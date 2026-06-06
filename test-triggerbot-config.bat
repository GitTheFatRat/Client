@echo off
REM TriggerBot Configuration Test & Validation
REM Windows batch script to verify file I/O fix

setlocal enabledelayedexpansion

echo.
echo ╔═══════════════════════════════════════════════════════════════╗
echo ║     TriggerBot - Configuration File Verification              ║
echo ╚═══════════════════════════════════════════════════════════════╝
echo.

REM Config file location
set CONFIG_FILE=%USERPROFILE%\.sentaihex\triggerbot.txt

echo [1] Checking configuration file...
if exist "%CONFIG_FILE%" (
    echo     ✓ File exists: %CONFIG_FILE%
    
    REM Show file content
    echo.
    echo [2] Current content:
    echo     ---
    for /f "delims=" %%A in (%CONFIG_FILE%) do (
        echo     %%A
    )
    echo     ---
    echo.
    
    REM Check for known issues
    echo [3] Checking for common issues...
    
    REM Read file into variable
    set /p FILE_CONTENT=<%CONFIG_FILE%
    
    if "!FILE_CONTENT!"=="" (
        echo     ⚠ WARNING: File is empty!
    ) else (
        echo     ✓ File has content: !FILE_CONTENT!
    )
    
    REM Check format
    echo !FILE_CONTENT! | findstr /R "^[a-z]*|.*|.*$" >nul
    if !errorlevel! equ 0 (
        echo     ✓ Format looks correct (has pipes: ^|)
    ) else (
        echo     ✗ Format issue - should be: enabled^|slot^|delay
    )
    
) else (
    echo     ✗ File NOT found: %CONFIG_FILE%
    echo.
    echo [Action] Creating directory and test file...
    
    if not exist "%USERPROFILE%\.sentaihex" (
        mkdir "%USERPROFILE%\.sentaihex"
        echo     ✓ Created directory
    )
    
    REM Create test config
    echo true^|-1^|100 > "%CONFIG_FILE%"
    echo     ✓ Created test config: %CONFIG_FILE%
    echo     Content: true^|-1^|100
)

echo.
echo [4] Next steps:
echo     1. Run TriggerBotDebugger for detailed analysis:
echo        cd "%~dp0"
echo        javac -encoding UTF-8 src\main\java\me\sentaihex\client\util\TriggerBotDebugger.java
echo        java -cp src\main\java me.sentaihex.client.util.TriggerBotDebugger
echo.
echo     2. Start Minecraft with debug flag:
echo        java -Dtriggerbot.debug -jar minecraft.jar
echo.
echo     3. Check that TriggerBot loop recognizes enabled state
echo.

echo ═══════════════════════════════════════════════════════════════
pause
