@echo off
setlocal EnableExtensions
cd /d "%~dp0"
echo === CyberNpc local build (no GitHub Actions) ===
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\build-local.ps1" -GradleVersion "8.8" -ModName "CyberNpc" -JarPattern "CyberNpc-*.jar"
set "RESULT=%ERRORLEVEL%"
if not "%RESULT%"=="0" (
    echo.
    echo Build failed. Please share build-local.log if you need help.
) else (
    echo.
    echo The file CyberNpc.jar is ready for testing.
)
pause
exit /b %RESULT%
