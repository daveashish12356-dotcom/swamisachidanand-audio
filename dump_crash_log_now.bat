@echo off
REM Use this if the app ALREADY crashed - saves current logcat immediately.
cd /d "%~dp0"
echo Saving FULL logcat (no filter) to full_crash.txt ...
adb logcat -d > full_crash.txt
echo Saving AndroidRuntime only to crash_java.txt ...
adb logcat -d -s AndroidRuntime:E AndroidRuntime:W "*:S" > crash_java.txt
echo Saving app + FATAL to crash_app.txt ...
adb logcat -d | findstr /i "swamisachidanand FATAL Exception AndroidRuntime PdfViewerActivity PdfPageAdapter" > crash_app.txt
echo Done. Check: full_crash.txt (search FATAL, Exception, signal, backtrace)
pause
