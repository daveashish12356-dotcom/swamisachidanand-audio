@echo off
REM Install app on connected device and open it
cd /d "%~dp0"
echo Installing app...
call gradlew.bat installDebug
if %ERRORLEVEL% neq 0 (
    echo Install failed.
    exit /b 1
)
echo Launching app...
adb shell am start -n com.swamisachidanand/.SplashActivity
echo Done. App should be open on device.
