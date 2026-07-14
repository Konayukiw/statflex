param(
    [switch]$Release
)

$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
if (-not $projectRoot) { $projectRoot = Get-Location }

function Increment-Version {
    param([string]$versionStr)
    $ver = [double]::Parse($versionStr, [System.Globalization.CultureInfo]::InvariantCulture)
    $newVer = $ver + 0.01
    return $newVer.ToString("0.00", [System.Globalization.CultureInfo]::InvariantCulture)
}

function Update-FileVersion {
    param(
        [string]$filePath,
        [string]$pattern,
        [string]$newVersion,
        [string]$replacementFormat
    )
    if (-not (Test-Path $filePath)) {
        Write-Warning "File not found: $filePath"
        return $false
    }
    $content = Get-Content $filePath -Raw
    $regex = $pattern
    $match = [regex]::Match($content, $regex)
    if ($match.Success) {
        $newContent = $content -replace $regex, $replacementFormat
        Set-Content -Path $filePath -Value $newContent -NoNewline
        Write-Host "Updated $filePath"
        return $true
    } else {
        Write-Warning "Pattern not found in $filePath"
        return $false
    }
}

function Get-VersionFromFile {
    param([string]$filePath, [string]$pattern)
    $content = Get-Content $filePath -Raw
    if ($content -match $pattern) {
        return $matches[1]
    }
    throw "Version not found in $filePath"
}

function Get-ContentBetweenQuotes {
    param([string]$line)
    if ($line -match ':\s*"([^"]*)"') {
        return $matches[1]
    }
    return ""
}

function Strip-Whitespace {
    param([string]$text)
    return $text -replace '\s+', ''
}

function Revert-Version {
    param(
        [string]$filePath,
        [string]$pattern,
        [string]$oldVersion,
        [string]$newVersion
    )
    if (-not (Test-Path $filePath)) {
        Write-Warning "File not found: $filePath"
        return $false
    }
    $content = Get-Content $filePath -Raw
    $regex = $pattern
    $match = [regex]::Match($content, $regex)
    if ($match.Success) {
        $replacement = $match.Value -replace [regex]::Escape($newVersion), $oldVersion
        $newContent = $content -replace $regex, $replacement
        Set-Content -Path $filePath -Value $newContent -NoNewline
        Write-Host "Reverted $filePath"
        return $true
    } else {
        Write-Warning "Pattern not found in $filePath"
        return $false
    }
}

$javaFile = Join-Path $projectRoot "src/main/java/com/konayuki/statflex/statflex.java"
$versionPattern = 'public static final String VERSION = "([^"]+)"'
$currentVersion = Get-VersionFromFile $javaFile $versionPattern
Write-Host "Current version: $currentVersion"

$newVersion = Increment-Version $currentVersion
Write-Host "New version: $newVersion"

$updated = $false

# statflex.java
$pattern = '(public static final String VERSION = ")[^"]+(")'
$replacement = '${1}' + $newVersion + '${2}'
if (Update-FileVersion $javaFile $pattern $newVersion $replacement) { $updated = $true }

# mcmod.info
$mcmodFile = Join-Path $projectRoot "src/main/resources/mcmod.info"
$pattern = '("version":\s*")[^"]+(")'
$replacement = '${1}' + $newVersion + '${2}'
if (Update-FileVersion $mcmodFile $pattern $newVersion $replacement) { $updated = $true }

# build.gradle
$gradleFile = Join-Path $projectRoot "build.gradle"
$pattern = '(?m)^(version\s*=\s*")[^"]+(")'
$replacement = '${1}' + $newVersion + '${2}'
if (Update-FileVersion $gradleFile $pattern $newVersion $replacement) { $updated = $true }

# main.md
$mainMdFile = Join-Path $projectRoot "main.md"
$pattern = '(# version:\s*)[\d.]+'
$replacement = '${1}' + $newVersion
if (Update-FileVersion $mainMdFile $pattern $newVersion $replacement) { $updated = $true }

# git add .
Write-Host "Running git add . ..."
Push-Location $projectRoot
git add .
if ($LASTEXITCODE -ne 0) {
    throw "Add failed"
}

$diffResult = git diff --cached --quiet 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "Repository is up-to-date!"
    Write-Host "Reverting version changes..."
    # statflex.java
    $pattern = '(public static final String VERSION = ")[^"]+(")'
    Revert-Version $javaFile $pattern $currentVersion $newVersion
    # mcmod.info
    $pattern = '("version":\s*")[^"]+(")'
    Revert-Version $mcmodFile $pattern $currentVersion $newVersion
    # build.gradle
    $pattern = '(?m)^(version\s*=\s*")[^"]+(")'
    Revert-Version $gradleFile $pattern $currentVersion $newVersion
    # main.md
    $pattern = '(# version:\s*)[\d.]+'
    Revert-Version $mainMdFile $pattern $currentVersion $newVersion
    Pop-Location
    exit 0
}

# Compare with latest commit on origin/main
$logOutput = git log origin/main -1 --pretty=full
if ($LASTEXITCODE -ne 0) {
    Write-Warning "Could not fetch latest commit. Skipping checking duplicated commit."
} else {
    $lines = $logOutput -split "`r`n|`n"
    $body = if ($lines.Count -gt 3) {
        $lines[3..($lines.Count-1)] -join "`n"
    } else {
        ""
    }
    $strippedCommit = Strip-Whitespace $body

    $mainMdContent = Get-Content $mainMdFile -Raw
    $changelogLine = ($mainMdContent -split "`r`n|`n" | Where-Object { $_ -match '^# changelog:' })
    if ($changelogLine) {
        $changelog = Get-ContentBetweenQuotes $changelogLine
    } else {
        $changelog = ""
    }
    $strippedChangelog = Strip-Whitespace $changelog

    if ($strippedCommit -eq $strippedChangelog) {
        Write-Host "The same text was already committed. Will you continue? (Y/N)" -ForegroundColor Yellow
        $response = Read-Host
        if ($response -ne 'y') {
            Write-Host "Aborted."
            Write-Host "Reverting version changes..."
            # statflex.java
            $pattern = '(public static final String VERSION = ")[^"]+(")'
            Revert-Version $javaFile $pattern $currentVersion $newVersion
            # mcmod.info
            $pattern = '("version":\s*")[^"]+(")'
            Revert-Version $mcmodFile $pattern $currentVersion $newVersion
            # build.gradle
            $pattern = '(?m)^(version\s*=\s*")[^"]+(")'
            Revert-Version $gradleFile $pattern $currentVersion $newVersion
            # main.md
            $pattern = '(# version:\s*)[\d.]+'
            Revert-Version $mainMdFile $pattern $currentVersion $newVersion
            Pop-Location
            exit 0
        }
    }
}

# Commit / push
$commitMessage = $changelog
if (-not $commitMessage) {
    $commitMessage = "Version update to $newVersion"
    Write-Warning "No changelog found. using default message: '$commitMessage'"
}

Write-Host "Committing with message: $commitMessage"
git commit -m $commitMessage
if ($LASTEXITCODE -ne 0) {
    throw "Commit failed"
}

Write-Host "Pushing..."
git push origin main --force
if ($LASTEXITCODE -ne 0) {
    Write-Host "Push failed"
    Write-Host "Reverting version changes..."
    # statflex.java
    $pattern = '(public static final String VERSION = ")[^"]+(")'
    Revert-Version $javaFile $pattern $currentVersion $newVersion
    # mcmod.info
    $pattern = '("version":\s*")[^"]+(")'
    Revert-Version $mcmodFile $pattern $currentVersion $newVersion
    # build.gradle
    $pattern = '(?m)^(version\s*=\s*")[^"]+(")'
    Revert-Version $gradleFile $pattern $currentVersion $newVersion
    # main.md
    $pattern = '(# version:\s*)[\d.]+'
    Revert-Version $mainMdFile $pattern $currentVersion $newVersion
    Pop-Location
    exit 0
}

# Build
.\gradlew.bat build
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed"
    Write-Host "Reverting version changes..."
    # statflex.java
    $pattern = '(public static final String VERSION = ")[^"]+(")'
    Revert-Version $javaFile $pattern $currentVersion $newVersion
    # mcmod.info
    $pattern = '("version":\s*")[^"]+(")'
    Revert-Version $mcmodFile $pattern $currentVersion $newVersion
    # build.gradle
    $pattern = '(?m)^(version\s*=\s*")[^"]+(")'
    Revert-Version $gradleFile $pattern $currentVersion $newVersion
    # main.md
    $pattern = '(# version:\s*)[\d.]+'
    Revert-Version $mainMdFile $pattern $currentVersion $newVersion
    Pop-Location
    exit 0
}

# Release
if ($Release) {
    Write-Host "Releasing..."

    # Determine project name
    $gradleContent = Get-Content $gradleFile -Raw
    $projectName = ""
    if ($gradleContent -match 'archivesBaseName\s*=\s*"([^"]+)"') {
        $projectName = $matches[1]
    } elseif ($gradleContent -match 'baseName\s*=\s*"([^"]+)"') {
        $projectName = $matches[1]
    } else {
        # Fallback to directory name
        $projectName = Split-Path $projectRoot -Leaf
    }
    Write-Host "Project name: $projectName"

    $title = "$projectName-$newVersion"
    Write-Host "Release title: $title"

    # Add tag and push
    git tag -a $title -m $commitMessage
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Tag failed"
        Write-Host "Reverting version changes..."
        # statflex.java
        $pattern = '(public static final String VERSION = ")[^"]+(")'
        Revert-Version $javaFile $pattern $currentVersion $newVersion
        # mcmod.info
        $pattern = '("version":\s*")[^"]+(")'
        Revert-Version $mcmodFile $pattern $currentVersion $newVersion
        # build.gradle
        $pattern = '(?m)^(version\s*=\s*")[^"]+(")'
        Revert-Version $gradleFile $pattern $currentVersion $newVersion
        # main.md
        $pattern = '(# version:\s*)[\d.]+'
        Revert-Version $mainMdFile $pattern $currentVersion $newVersion
        Pop-Location
        exit 0
    }
    git push origin $title
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Git push tag failed"
        Write-Host "Reverting version changes..."
        # statflex.java
        $pattern = '(public static final String VERSION = ")[^"]+(")'
        Revert-Version $javaFile $pattern $currentVersion $newVersion
        # mcmod.info
        $pattern = '("version":\s*")[^"]+(")'
        Revert-Version $mcmodFile $pattern $currentVersion $newVersion
        # build.gradle
        $pattern = '(?m)^(version\s*=\s*")[^"]+(")'
        Revert-Version $gradleFile $pattern $currentVersion $newVersion
        # main.md
        $pattern = '(# version:\s*)[\d.]+'
        Revert-Version $mainMdFile $pattern $currentVersion $newVersion
        Pop-Location
        exit 0
    }

    # Copy description from main.md
    $descMatch = [regex]::Match(
        $mainMdContent, '(?ms)^desc_s\s*\r?\n(.*?)(?=^desc_e$)')
    if ($descMatch.Success) {
        $description = $descMatch.Groups[1].Value.TrimEnd()
    } else {
        $description = ""
    }
    $releaseNoteFile = Join-Path $projectRoot "release.md"
    Set-Content -Path $releaseNoteFile -Value $description -NoNewline
    Write-Host "release.md could not found, new one created"

    # Create GitHub release
    $jarFile = Join-Path $projectRoot "build/libs/statflex-$newVersion.jar"
    $injectorExe = "C:\Users\Konayuki\OneDrive\Desktop\statflex-injector.exe"
    if (-not (Test-Path $jarFile)) {
        Write-Warning "Jar file not found: $jarFile. Skipping release creation."
    } else {
        $ghArgs = @(
            "release", "create", $title,
            $jarFile,
            $injectorExe,
            "--title", $title,
            "--notes-file", $releaseNoteFile
        )
        Write-Host "Running: gh $($ghArgs -join ' ')"
        gh $ghArgs
        if ($LASTEXITCODE -ne 0) {
            Write-Warning "gh release create failed with exit code $LASTEXITCODE"
        }
    }
}

Pop-Location
Write-Host "Update process done!"