@echo off
REM Run this AFTER the app crashes. Phone must be connected via USB.
cd /d "%~dp0"
echo Clearing logcat...
adb logcat -c
echo.
echo Now open the app on phone, open the book that crashes, let it crash.
echo Then press any key here to save the log...
pause >nul
echo Saving log...
adb logcat -d > full_crash.txt
adb logcat -d -s AndroidRuntime:E "*:S" > crash_java.txt
echo Done. Check: full_crash.txt and crash_java.txt
pause
