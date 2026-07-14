@echo off
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "PS_SCRIPT=%SCRIPT_DIR%update.ps1"

if "%1"=="release" (
    powershell.exe -ExecutionPolicy Bypass -File "%PS_SCRIPT%" -Release
) else (
    powershell.exe -ExecutionPolicy Bypass -File "%PS_SCRIPT%"
)

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Script failed with error code %ERRORLEVEL%.
    pause
)
exit /b %ERRORLEVEL%