# ચાણક્યની વ્યવહારનીતિ - Release Steps

## ✅ Completed
- ✓ 52 audio files prepared in `f:\ss\chanakya_upload\` (1.wav to 52.wav)
- ✓ Book added to `public/audio_list.json` with thumbnailUrl
- ✓ JSON entry has 52 parts with correct URLs

## 📋 Remaining Steps

### 1. Create Thumbnail
**Extract first page from PDF as JPG:**
- Source: `C:\Users\davea\Desktop\ચાણક્યની વ્યવહારનીતિ.pdf`
- Save as: `f:\ss\public\thumbnails\chanakya_vyavaharniti.jpg`

**Methods:**
- Use PDF viewer (Adobe, Edge) → Print/Save first page as JPG
- Or install: `pip install pdf2image pillow` then run `create_chanakya_thumbnail.py`

### 2. Create GitHub Release
**Option A: Using GitHub CLI (if installed):**
```bash
cd f:\ss\chanakya_upload
gh release create chanakya_vyavaharniti *.wav --repo daveashish12356-dotcom/swamisachidanand-audio --title "Chanakya Vyavaharniti"
```

**Option B: Via Browser:**
1. Go to: https://github.com/daveashish12356-dotcom/swamisachidanand-audio/releases/new
2. **Tag:** `chanakya_vyavaharniti`
3. **Title:** `Chanakya Vyavaharniti`
4. **Upload files:** Select all 52 WAV files from `f:\ss\chanakya_upload\`
5. Click **Publish release**

### 3. Push to GitHub
```bash
cd f:\ss
git add public/audio_list.json
git add public/thumbnails/chanakya_vyavaharniti.jpg
git commit -m "Add Chanakya Vyavaharniti audio book"
git push
```

### 4. Verify in App
1. Open app → **Audio** tab
2. Check if book appears: **ચાણક્યની વ્યવહારનીતિ**
3. Verify thumbnail is visible on card
4. Test audio playback (should load from GitHub release)

## 📝 Notes
- Thumbnail URL in JSON: `https://daveashish12356-dotcom.github.io/swamisachidanand-audio/thumbnails/chanakya_vyavaharniti.jpg`
- Release tag must be: `chanakya_vyavaharniti` (matches JSON URLs)
- Audio files must be named: `1.wav`, `2.wav`, ... `52.wav` (already done in upload folder)
