# Hosting – GitHub se (Firebase ki jagah, space limit nahi)

App ko server se connect karne ke liye **sirf GitHub + GitHub Pages** use karo. Free, space limit nahi.

---

## 1. Code GitHub pe push karna

GitHub **username** bhi **swamisachidanand** rakho, repo naam **swamisachidanand-audio**. Is se URL: `swamisachidanand.github.io/swamisachidanand-audio`.

```bash
cd f:\ss
git remote add origin https://github.com/swamisachidanand/swamisachidanand-audio.git
git branch -M main
git push -u origin main
```

- GitHub.com pe **Sign up** (agar account nahi) → username: **swamisachidanand**  
- **New repository** → **Repository name:** `swamisachidanand-audio`

---

## 2. GitHub Pages on karna (free hosting = server URL)

1. GitHub pe apne repo kholo  
2. **Settings** → left side **Pages**  
3. **Source:** “Deploy from a branch”  
4. **Branch:** `main`  
5. **Folder:** `/ (root)` ya `public` (agar tum **public** folder serve karna chahte ho to “public” select karo)  
6. **Save**

Thodi der baad ye URL live ho jayega (username = **swamisachidanand**, repo = **swamisachidanand-audio**):

- **Agar root choose kiya:**  
  `https://swamisachidanand.github.io/swamisachidanand-audio/`  
  - Audio list URL:  
  `https://swamisachidanand.github.io/swamisachidanand-audio/audio_list.json`

- **Agar “public” folder choose kiya:**  
  `https://swamisachidanand.github.io/swamisachidanand-audio/public/`  
  - Audio list URL:  
  `https://swamisachidanand.github.io/swamisachidanand-audio/public/audio_list.json`

App me jab “server se connect” wala code add karoge, isi URL se `audio_list.json` fetch karna.

---

## 3. Bade audio (500 MB+) – GitHub **Releases** use karo

Repo me 500 MB+ audio mat daalo (limit). **Releases** pe file upload karo – har file 2 GB tak allowed.

### પહેલી book (અમરકંટક – 24 parts, ~571 MB) add karna

1. **Alag repo** banao: **swamisachidanand-audio** (app repo `f:\ss` me audio files mat daalo). Is naye repo me ye files push karo: `audio_list.json`, `public/audio_list.json`, `RELEASE_UPLOAD_MAP.txt`, aur ye README (copy from `f:\ss\HOSTING_README.md`). Matlab `f:\ss` se in files ko copy karke naye repo me commit + push karo.
2. GitHub pe repo kholo → **Releases** → **Create a new release**.
3. **Tag:** `amarakantak` (ya `v1.0`) type karo, **Publish release** click karo.
4. Release page pe **Assets** → **Attach binaries** se 24 WAV files upload karo.  
   **File names bilkul aise honi chahiye:** `part_01.wav`, `part_02.wav`, … `part_24.wav`.  
   Kaunsi Desktop file ka kaunsa naam hai wo **RELEASE_UPLOAD_MAP.txt** me hai.
5. Jab sab upload ho jaye, in URLs se audio chalegi:  
   `https://github.com/swamisachidanand/swamisachidanand-audio/releases/download/amarakantak/part_01.wav` … `part_24.wav`.  
   Ye URLs already `audio_list.json` me daal diye gaye hain.

### Baaki books / naya audio

1. Repo me `public/audio_list.json` (ya root pe `audio_list.json`) edit karo. Ab format **books** wala hai:

```json
{
  "version": 2,
  "updated": "2025-02-01T12:00:00Z",
  "books": [
    {
      "id": "book_id",
      "title": "Book title",
      "parts": [
        { "id": "1", "title": "Part 1", "url": "https://..../part_01.wav" },
        { "id": "2", "title": "Part 2", "url": "https://..../part_02.wav" }
      ]
    }
  ]
}
```

2. Audio files **GitHub Releases** pe upload karo (naya release tag banao), ya koi bhi public URL use karo. Sirf **url** sahi daalna hai.  
3. Git me commit + push:

```bash
git add public/audio_list.json audio_list.json
git commit -m "Add new audio to list"
git push
```

GitHub Pages thodi der me update ho jayega; app jab list fetch karegi to naya audio dikhega.

---

## 4. Short summary

| Kaam              | Kya karna hai                                      |
|-------------------|----------------------------------------------------|
| Code GitHub pe    | `git remote add origin ...` → `git push -u origin main` |
| Server URL        | GitHub repo → **Settings → Pages** → branch `main`, folder root ya `public` |
| Audio list URL    | `https://swamisachidanand.github.io/swamisachidanand-audio/audio_list.json` (ya `.../public/audio_list.json`) |
| Naya audio        | `audio_list.json` edit → `git add` → `git commit` → `git push` |

**Firebase hosting ki zaroorat nahi – sirf GitHub use karo, space limit nahi.**
