@echo off
setlocal

echo [SentaiHex] Building...

:: Build jar truoc
call mvn clean package -q
if errorlevel 1 (
    echo [ERROR] Maven build failed!
    pause
    exit /b 1
)

:: Tao thu muc output
if not exist "dist" mkdir dist

:: Chay jpackage de tao exe
:: jpackage co san trong JDK 21, khong can cai them gi
jpackage ^
  --input target ^
  --name SentaiHex ^
  --main-jar SentaiHex-Launcher.jar ^
  --main-class me.sentaihex.launcher.Launcher ^
  --type exe ^
  --dest dist ^
  --win-console ^
  --win-shortcut ^
  --app-version 1.0.0 ^
  --description "SentaiHex Minecraft Macro Client" ^
  --vendor "SentaiHex" ^
  --java-options "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED" ^
  --java-options "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED" ^
  --java-options "-Djdk.attach.allowAttachSelf=true"

if errorlevel 1 (
    echo [ERROR] jpackage failed!
    pause
    exit /b 1
)

echo.
echo [OK] Done! File exe nam o: dist\SentaiHex-1.0.0.exe
echo Chia se file do cho ban, chay thang khong can cai Java.
echo.
pause
