param(
    [switch]$Release,
    [switch]$Revert
)

$ErrorActionPreference = "Stop"

$projectRoot = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }
$utf8NoBom = New-Object System.Text.UTF8Encoding $false

function Read-TextFile {
    param([string]$Path)
    return [System.IO.File]::ReadAllText($Path, $utf8NoBom)
}

function Write-TextFile {
    param([string]$Path, [string]$Content)
    [System.IO.File]::WriteAllText($Path, $Content, $utf8NoBom)
}

function Increment-Version {
    param([string]$versionStr)
    # decimal avoids floating-point drift (e.g. 2.29 + 0.01)
    $ver = [decimal]::Parse($versionStr, [System.Globalization.CultureInfo]::InvariantCulture)
    $newVer = $ver + [decimal]"0.01"
    return $newVer.ToString("0.00", [System.Globalization.CultureInfo]::InvariantCulture)
}

function Update-FileVersion {
    param(
        [string]$filePath,
        [string]$pattern,
        [string]$replacementFormat
    )
    if (-not (Test-Path -LiteralPath $filePath)) {
        Write-Warning "File not found: $filePath"
        return $false
    }
    $content = Read-TextFile -Path $filePath
    $match = [regex]::Match($content, $pattern)
    if (-not $match.Success) {
        Write-Warning "Pattern not found in $filePath"
        return $false
    }
    $newContent = [regex]::Replace($content, $pattern, $replacementFormat, 1)
    Write-TextFile -Path $filePath -Content $newContent
    Write-Host "Updated $filePath"
    return $true
}

function Get-VersionFromFile {
    param([string]$filePath, [string]$pattern)
    $content = Read-TextFile -Path $filePath
    $match = [regex]::Match($content, $pattern)
    if (-not $match.Success) {
        throw "Version not found in $filePath"
    }
    return $match.Groups[1].Value
}

function Get-ContentBetweenQuotes {
    param([string]$line)
    if ($line -match ':\s*"([^"]*)"') {
        return $Matches[1]
    }
    return ""
}

function Strip-Whitespace {
    param([string]$text)
    return $text -replace '\s+', ''
}

function Get-VersionReplacement {
    param(
        [hashtable]$Target,
        [string]$Version
    )
    return $Target.Replacement.Replace('{VERSION}', $Version)
}

function Update-AllVersions {
    param([string]$Version)
    $any = $false
    foreach ($target in $script:versionTargets) {
        $replacement = Get-VersionReplacement -Target $target -Version $Version
        if (Update-FileVersion -filePath $target.File -pattern $target.Pattern -replacementFormat $replacement) {
            $any = $true
        }
    }
    return $any
}

function Revert-AllVersions {
    param(
        [string]$OldVersion,
        [string]$Reason
    )
    if ($Reason) {
        Write-Host $Reason
    }
    Write-Host "Reverting version changes..."
    [void](Update-AllVersions -Version $OldVersion)
}

function Exit-WithRevert {
    param(
        [string]$OldVersion,
        [string]$Reason,
        [switch]$Restage
    )
    Revert-AllVersions -OldVersion $OldVersion -Reason $Reason
    if ($Restage) {
        $paths = $script:versionTargets | ForEach-Object { $_.File }
        git add -- $paths 2>$null | Out-Null
    }
    exit 0
}

$javaFile   = Join-Path $projectRoot "src/main/java/com/konayuki/statflex/statflex.java"
$mcmodFile  = Join-Path $projectRoot "src/main/resources/mcmod.info"
$gradleFile = Join-Path $projectRoot "build.gradle"
$mainMdFile = Join-Path $projectRoot "main.md"

$script:versionTargets = @(
    @{
        File        = $javaFile
        Pattern     = '(public static final String VERSION = ")[^"]+(")'
        Replacement = '${1}{VERSION}${2}'
    },
    @{
        File        = $mcmodFile
        Pattern     = '("version":\s*")[^"]+(")'
        Replacement = '${1}{VERSION}${2}'
    },
    @{
        File        = $gradleFile
        Pattern     = '(?m)^(version\s*=\s*")[^"]+(")'
        Replacement = '${1}{VERSION}${2}'
    },
    @{
        File        = $mainMdFile
        Pattern     = '(# version:\s*)[\d.]+'
        Replacement = '${1}{VERSION}'
    }
)

$versionPattern = 'public static final String VERSION = "([^"]+)"'
$currentVersion = Get-VersionFromFile -filePath $javaFile -pattern $versionPattern
Write-Host "Current version: $currentVersion"

if ($Revert) {
    $previousVersion = [decimal]::Parse($currentVersion, [System.Globalization.CultureInfo]::InvariantCulture) - [decimal]"0.01"
    $previousVersionStr = $previousVersion.ToString("0.00", [System.Globalization.CultureInfo]::InvariantCulture)
    Write-Host "Reverting to previous version: $previousVersionStr"
    Revert-AllVersions -OldVersion $previousVersionStr
    Write-Host "Revert completed!"
    exit 0
}

$newVersion = Increment-Version -versionStr $currentVersion
Write-Host "New version: $newVersion"

[void](Update-AllVersions -Version $newVersion)

# Always load main.md (used for changelog + release notes)
$mainMdContent = Read-TextFile -Path $mainMdFile
$changelogLine = ($mainMdContent -split "`r?`n" | Where-Object { $_ -match '^# changelog:' } | Select-Object -First 1)
$changelog = if ($changelogLine) { Get-ContentBetweenQuotes -line $changelogLine } else { "" }

Push-Location $projectRoot
try {
    Write-Host "Running git add . ..."
    git add .
    if ($LASTEXITCODE -ne 0) {
        throw "Add failed"
    }

    git diff --cached --quiet 2>$null
    if ($LASTEXITCODE -eq 0) {
        Exit-WithRevert -OldVersion $currentVersion -Reason "Repository is up-to-date!" -Restage
    }

    # Compare with latest commit on origin/main
    $logOutput = git log origin/main -1 --pretty=full 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Could not fetch latest commit. Skipping checking duplicated commit."
    } else {
        $lines = @($logOutput -split "`r?`n")
        $body = if ($lines.Count -gt 3) {
            ($lines[3..($lines.Count - 1)] -join "`n")
        } else {
            ""
        }
        $strippedCommit = Strip-Whitespace -text $body
        $strippedChangelog = Strip-Whitespace -text $changelog

        if ($strippedCommit -and ($strippedCommit -eq $strippedChangelog)) {
            Write-Host "The same text was already committed. Will you continue? (Y/N)" -ForegroundColor Yellow
            $response = Read-Host
            if ($response -notmatch '^[yY]$') {
                Exit-WithRevert -OldVersion $currentVersion -Reason "Aborted." -Restage
            }
        }
    }

    $commitMessage = if ($changelog) { $changelog } else {
        $msg = "Version update to $newVersion"
        Write-Warning "No changelog found. using default message: '$msg'"
        $msg
    }

    Write-Host "Committing with message: $commitMessage"
    git commit -m $commitMessage
    if ($LASTEXITCODE -ne 0) {
        throw "Commit failed"
    }

    Write-Host "Pushing..."
    git push origin main --force
    if ($LASTEXITCODE -ne 0) {
        Exit-WithRevert -OldVersion $currentVersion -Reason "Push failed"
    }

    Write-Host "Building..."
    & .\gradlew.bat build
    if ($LASTEXITCODE -ne 0) {
        Exit-WithRevert -OldVersion $currentVersion -Reason "Build failed"
    }

    if ($Release) {
        Write-Host "Releasing..."

        $gradleContent = Read-TextFile -Path $gradleFile
        if ($gradleContent -match 'archivesBaseName\s*=\s*"([^"]+)"') {
            $projectName = $Matches[1]
        } elseif ($gradleContent -match 'baseName\s*=\s*"([^"]+)"') {
            $projectName = $Matches[1]
        } else {
            $projectName = Split-Path $projectRoot -Leaf
        }
        Write-Host "Project name: $projectName"

        $title = "$projectName-$newVersion"
        Write-Host "Release title: $title"

        git tag -a $title -m $commitMessage
        if ($LASTEXITCODE -ne 0) {
            Exit-WithRevert -OldVersion $currentVersion -Reason "Tag failed"
        }

        git push origin $title
        if ($LASTEXITCODE -ne 0) {
            Exit-WithRevert -OldVersion $currentVersion -Reason "Git push tag failed"
        }

        $descMatch = [regex]::Match($mainMdContent, '(?ms)^desc_s\s*\r?\n(.*?)(?=^desc_e$)')
        $description = if ($descMatch.Success) { $descMatch.Groups[1].Value.TrimEnd() } else { "" }

        $releaseNoteFile = Join-Path $projectRoot "release.md"
        Write-TextFile -Path $releaseNoteFile -Content $description
        if (Test-Path -LiteralPath $releaseNoteFile) {
            Write-Host "Wrote release notes to release.md"
        }

        $jarFile = Join-Path $projectRoot "build/libs/statflex-$newVersion.jar"
        $injectorExe = "C:\Users\Konayuki\OneDrive\Desktop\statflex-injector.exe"
        if (-not (Test-Path -LiteralPath $jarFile)) {
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
            & gh @ghArgs
            if ($LASTEXITCODE -ne 0) {
                Write-Warning "gh release create failed with exit code $LASTEXITCODE"
            }
        }
    }

    Write-Host "Update process done!"
}
finally {
    Pop-Location
}
