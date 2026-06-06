@echo off
REM Quick-start script for TriggerBot with Screen Check Disabled
REM This bypasses the "screen open" blocking issue

setlocal enabledelayedexpansion

echo.
echo [TriggerBot] Screen Open Fix - Quick Start
echo ======================================
echo.

REM Check if Maven is available
where mvn >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ERROR: Maven not found in PATH
    echo.
    echo Please install Maven from: https://maven.apache.org/download.cgi
    echo Then add Maven bin directory to your PATH
    echo.
    pause
    exit /b 1
)

echo [*] Cleaning previous builds...
call mvn clean

echo.
echo [*] Compiling project...
call mvn compile

echo.
echo [*] Building JAR with shade plugin...
call mvn package

echo.
if exist "target\sentaihex-1.0.0.jar" (
    echo [SUCCESS] Build completed!
    echo [JAR] target\sentaihex-1.0.0.jar
    echo.
    echo [INFO] To run with Screen Check Disabled:
    echo        Add to Minecraft JVM Arguments:
    echo        -Dtriggerbot.skip.screen.check
    echo.
    echo [INFO] To enable verbose debugging:
    echo        -Dtriggerbot.skip.screen.check -Dtriggerbot.debug -Dtriggerbot.verbose.screen
) else (
    echo [ERROR] Build failed - JAR not found
)

echo.
pause
