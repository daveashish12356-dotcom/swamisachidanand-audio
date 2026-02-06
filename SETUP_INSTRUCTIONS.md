# TTS और OCR Setup Instructions

## 📋 App में TTS और OCR Install करने के लिए

### ✅ TTS (Text-to-Speech) - Already Installed
- Android system TTS engine app में automatically initialize होता है
- Gujarati language support phone में होना चाहिए
- अगर Gujarati नहीं है, तो English TTS use होगा (fallback)

### 📝 OCR (Optical Character Recognition) - Setup Required

#### Step 1: Gujarati OCR Data File Download करें

1. **Download Link**: 
   ```
   https://github.com/tesseract-ocr/tessdata/raw/main/guj.traineddata
   ```

2. **File Size**: ~2-3 MB

#### Step 2: File को App में Add करें

1. Download की हुई `guj.traineddata` file को copy करें
2. Paste करें इस folder में:
   ```
   app/src/main/assets/tessdata/guj.traineddata
   ```

#### Step 3: Folder Structure

```
app/src/main/assets/
  └── tessdata/
      ├── README.txt
      └── guj.traineddata  ← यह file add करें
```

#### Step 4: App Rebuild करें

```bash
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

#### Step 5: App Install करें

```bash
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## ✅ Verification

App open करने के बाद:

1. **TTS Status**: 
   - Toast message दिखेगा: "Gujarati TTS ready!" या "TTS ready (English)"
   - अगर error आए, तो phone settings में TTS check करें

2. **OCR Status**:
   - Toast message दिखेगा: "OCR ready for Gujarati!"
   - अगर error आए, तो `guj.traineddata` file check करें

---

## 🔧 Troubleshooting

### TTS नहीं बोल रहा:
- Phone settings > Language & Input > Text-to-Speech check करें
- Gujarati language pack install करें (अगर available हो)
- App restart करें

### OCR नहीं काम कर रहा:
- `guj.traineddata` file `app/src/main/assets/tessdata/` में है या नहीं check करें
- File size ~2-3 MB होनी चाहिए
- App clean build करें और फिर install करें

---

## 📦 Files Required

1. ✅ **TTS**: Android system TTS (already available)
2. ⚠️ **OCR**: `guj.traineddata` file (manual download required)

---

## 🎯 Current Status

- ✅ TTS initialization code ready
- ✅ OCR initialization code ready  
- ⚠️ `guj.traineddata` file needs to be added manually

**Next Step**: `guj.traineddata` file download करें और `app/src/main/assets/tessdata/` में add करें!

