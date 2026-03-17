# App size aur assets – summary

## App (APK) total size

| Item | Size |
|------|------|
| **app-debug.apk** | **~113 MB** |

*(Path: `app/build/outputs/apk/debug/app-debug.apk`)*

---

## App ke andar kitna asset pada hai

### 1. Assets folder (`app/src/main/assets`) — **~2.79 MB**

| File | Size |
|------|------|
| **tessdata/guj.traineddata** (OCR – Gujarati) | 1.87 MB |
| home_photo2.jpg | 0.17 MB |
| home_photo3.jpg | 0.12 MB |
| 6f93aa5c-...-md.jpg | 0.12 MB |
| home_photo1.jpg | 0.09 MB |
| e18b218e-...-md.jpg | 0.10 MB |
| home_photo4.webp | 0.11 MB |
| audio_list_fallback.json | 0.08 MB |
| swamiji.jpg | 0.08 MB |
| F0GoDmQacAAaPKX.jpg | 0.04 MB |
| book_chapters.json | 0.01 MB |
| books_server_list.json | &lt; 0.01 MB |

**Breakdown by type:**

| Type | Count | Total size |
|------|-------|------------|
| .traineddata (OCR) | 1 | 1.87 MB |
| .jpg | 7 | ~0.72 MB |
| .webp | 1 | 0.11 MB |
| .json | 3 | ~0.09 MB |
| **Assets total** | **12 files** | **~2.79 MB** |

### 2. Res folder (`app/src/main/res`) — **~3.83 MB**

- drawable (icons, backgrounds, XML)
- mipmap (launcher icons – hdpi, mdpi, xhdpi, xxhdpi, xxxhdpi)
- layout, menu, values, xml  
*Res total: ~3.83 MB*

---

## Grand total – app ke andar bundled content

| Source | Size |
|--------|------|
| **assets** | ~2.79 MB |
| **res** | ~3.83 MB |
| **Bundled content total** | **~6.6 MB** |

Baaki **~106 MB** APK me: Java/Kotlin code, native libraries (CMake), Android framework, dependencies, compiled resources – ye sab mila ke APK ko **~113 MB** banata hai.

---

## Note

- **Video** ab app me nahi hai – server se online chal raha hai (isliye pehle jitna size tha utna ab nahi).
- **PDF / audiobook** files app me embed nahi hai – wo server se load hote hai, isliye unka size is summary me nahi aata.
