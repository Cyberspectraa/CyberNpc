@echo off
setlocal EnableExtensions
cd /d "%~dp0"
echo === CyberNpc local Forge build ===
echo Required: JDK 17 and Gradle 8.8 on PATH.
where java >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java not found. Install JDK 17.
    exit /b 1
)
where gradle >nul 2>&1
if errorlevel 1 (
    echo ERROR: Gradle not found. Install Gradle 8.8.
    exit /b 1
)
call gradle clean build --no-daemon --stacktrace
if errorlevel 1 (
    echo BUILD FAILED. Nothing was copied to CyberNpc.jar.
    exit /b 1
)
set "BUILT="
for %%F in ("build\libs\CyberNpc-*.jar") do (
    if exist "%%~fF" (
        copy /Y "%%~fF" "CyberNpc.jar" >nul
        set "BUILT=1"
    )
)
if not defined BUILT (
    echo ERROR: Build passed but output JAR was not found in build\libs.
    exit /b 1
)
echo SUCCESS: CyberNpc.jar is in this folder.
endlocal
