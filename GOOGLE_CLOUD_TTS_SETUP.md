# Google Cloud Text-to-Speech Setup Guide

## 🎙 Gujarati AI Voice (WaveNet) Integration Complete!

App में **Google Cloud Text-to-Speech (WaveNet Gujarati)** integrate हो गया है। यह human-like natural Gujarati voice provide करता है।

---

## 📋 Setup Steps

### STEP 1: Google Cloud Console में जाएं

1. **Google Cloud Console** खोलें: https://console.cloud.google.com
2. **New Project** बनाएं (या existing project select करें)
3. **APIs & Services** > **Library** में जाएं
4. **"Cloud Text-to-Speech API"** search करें
5. **Enable** button click करें

### STEP 2: API Key Generate करें

1. **APIs & Services** > **Credentials** में जाएं
2. **+ CREATE CREDENTIALS** > **API Key** click करें
3. API Key copy करें
4. (Optional) API Key को restrict करें:
   - **Application restrictions**: Android apps
   - **API restrictions**: Cloud Text-to-Speech API

### STEP 3: App में API Key Add करें

**File**: `app/src/main/res/values/api_config.xml`

```xml
<string name="google_cloud_tts_api_key">YOUR_API_KEY_HERE</string>
```

**Example:**
```xml
<string name="google_cloud_tts_api_key">AIzaSyAbCdEfGhIjKlMnOpQrStUvWxYz1234567</string>
```

---

## ✅ Features

- ✅ **Natural Gujarati Voice**: `gu-IN-Wavenet-A` (Human-like quality)
- ✅ **Automatic Fallback**: अगर Google Cloud TTS fail हो, तो system TTS use होगा
- ✅ **Sentence-by-Sentence Highlighting**: Text highlight होता रहेगा जैसे-जैसे बोला जाएगा
- ✅ **Error Handling**: API Key missing होने पर clear error message

---

## 🔧 How It Works

1. **OCR**: PDF page से text extract होता है (Tesseract OCR)
2. **Text Processing**: Text को sentences में split किया जाता है
3. **Google Cloud TTS**: हर sentence को Google Cloud API से natural voice में convert किया जाता है
4. **Playback**: Audio play होता है और text highlight होता रहता है

---

## 💰 Pricing

Google Cloud Text-to-Speech **free tier**:
- **0-4 million characters/month**: FREE
- **4+ million characters**: $4 per 1 million characters

Most users के लिए free tier काफी है।

---

## 🐛 Troubleshooting

### API Key Error
```
Error: API Key not configured
```
**Solution**: `api_config.xml` में API key add करें

### Network Error
```
Error: Network error
```
**Solution**: Internet connection check करें

### Fallback to System TTS
अगर Google Cloud TTS fail हो, app automatically system TTS use करेगा (Gujarati या English)

---

## 📝 Notes

- API Key को **secure** रखें
- Production app के लिए API key को **environment variables** या **secure storage** में store करें
- API key को **public repository** में commit न करें

---

## 🎯 Current Status

✅ Google Cloud TTS integration complete  
✅ Fallback system TTS ready  
✅ Sentence highlighting working  
✅ Error handling implemented  

**Next Step**: `api_config.xml` में अपना API key add करें और test करें!

