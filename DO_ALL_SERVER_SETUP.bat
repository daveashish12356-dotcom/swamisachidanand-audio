@echo off
chcp 65001 >nul
echo ========================================
echo  Server setup - Step 2 (push)
echo ========================================
echo.
echo Pehle GitHub pe repo banao:
echo   https://github.com/new
echo   Name: swamisachidanand-audio
echo   Public, README mat add karo.
echo.
echo Repo banane ke BAAD ye script run karo.
echo.
pause

cd /d "f:\ss\audio-repo"

git remote -v 2>nul | find "origin" >nul || (
  git remote add origin https://github.com/daveashish12356-dotcom/swamisachidanand-audio.git
)

git push -u origin main

echo.
echo ========================================
echo  Ab GitHub pe jao:
echo  1. Repo ^> Settings ^> Pages
echo     Source: Deploy from branch, Branch: main, Folder: / (root), Save
echo  2. 2 min wait, phir check: https://daveashish12356-dotcom.github.io/swamisachidanand-audio/audio_list.json
echo  3. Releases ^> Create release, Tag: amarakantak ^> 24 WAV upload (RELEASE_UPLOAD_MAP.txt dekho)
echo ========================================
pause
