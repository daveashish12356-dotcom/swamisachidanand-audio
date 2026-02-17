# ગીતાજીનું ચિતન – Server release steps

## Already done in this repo
- **Audio list**: ગીતાજીનું ચિતન (139 parts) added to:
  - `public/audio_list.json`
  - `audio_list.json`
  - `app/src/main/assets/audio_list_fallback.json`
- **App**: `ServerAudioFragment` shows thumbnail from  
  `https://raw.githubusercontent.com/.../gh-pages/thumbnails/geetaji_chintan.jpg`
- **Thumbnail**: `public/thumbnails/geetaji_chintan.jpg` created (run `python generate_geetaji_chintan_thumb.py` to regenerate).

## 1. MP3 and GitHub release (if not done yet)

If the 139 MP3 files are **not** yet on GitHub:

1. Put source path in `audio-repo/geetaji_source_path.txt` (one line):  
   `C:\Users\davea\Desktop\ગીતાજીનું ચિતન`
2. Open PowerShell:
   ```powershell
   cd f:\ss\audio-repo
   .\CREATE_GEETAJI_CHINTAN_RELEASE.ps1
   ```
   - Script uses WAV from that folder, converts to MP3 with ffmpeg (or copies existing MP3 as 1.mp3 … 139.mp3).
   - Creates/updates `geetaji_chintan_parts.json`.
   - Runs `gh release create geetaji_chintan` and uploads all MP3 (needs `gh` and repo access).

If MP3 are already in `audio-repo/geetaji_chintan_upload` and you only need the release:

```powershell
cd f:\ss\audio-repo\geetaji_chintan_upload
gh release create geetaji_chintan *.mp3 --repo daveashish12356-dotcom/swamisachidanand-audio --title "ગીતાજીનું ચિતન"
```

## 2. Thumbnail on gh-pages

App loads thumbnail from **swamisachidanand-audio** repo, gh-pages branch:

- URL:  
  `https://raw.githubusercontent.com/daveashish12356-dotcom/swamisachidanand-audio/gh-pages/thumbnails/geetaji_chintan.jpg`
- Copy `f:\ss\public\thumbnails\geetaji_chintan.jpg` into that repo under `thumbnails/geetaji_chintan.jpg` and push the **gh-pages** branch (or use Firebase/GitHub Pages deploy from that repo).

## 3. Push public/ for release

From `f:\ss`:

- Commit and push so `public/audio_list.json` and `public/thumbnails/geetaji_chintan.jpg` are on your server (e.g. gh-pages or hosting that serves `public/`).

## 4. App release

- Build release AAB/APK and upload to Play Store as usual.

---

**Summary**: MP3 → GitHub release `geetaji_chintan`; thumbnail → swamisachidanand-audio gh-pages `thumbnails/geetaji_chintan.jpg`; audio list and app already updated in this repo.
