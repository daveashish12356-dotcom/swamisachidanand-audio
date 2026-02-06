# Hosting – GitHub se (Firebase ki jagah, space limit nahi)

App ko server se connect karne ke liye **sirf GitHub + GitHub Pages** use karo. Free, space limit nahi.

---

## 1. Code GitHub pe push karna

```bash
cd f:\ss
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git
git branch -M main
git push -u origin main
```

- GitHub.com pe jao → **New repository** banao (e.g. `swami-app`)
- **YOUR_USERNAME** = apna GitHub username  
- **YOUR_REPO_NAME** = repo name (e.g. `swami-app`)

---

## 2. GitHub Pages on karna (free hosting = server URL)

1. GitHub pe apne repo kholo  
2. **Settings** → left side **Pages**  
3. **Source:** “Deploy from a branch”  
4. **Branch:** `main`  
5. **Folder:** `/ (root)` ya `public` (agar tum **public** folder serve karna chahte ho to “public” select karo)  
6. **Save**

Thodi der baad ye URL live ho jayega:

- **Agar root choose kiya:**  
  `https://YOUR_USERNAME.github.io/YOUR_REPO_NAME/`  
  - Is case me `audio_list.json` repo **root** me hona chahiye, tab URL:  
  `https://YOUR_USERNAME.github.io/YOUR_REPO_NAME/audio_list.json`

- **Agar “public” folder choose kiya:**  
  `https://YOUR_USERNAME.github.io/YOUR_REPO_NAME/public/`  
  - Tab audio list URL:  
  `https://YOUR_USERNAME.github.io/YOUR_REPO_NAME/public/audio_list.json`

App me jab “server se connect” wala code add karoge, isi URL se `audio_list.json` fetch karna.

---

## 3. Naya audio add karna (bina Firebase)

1. Repo me `public/audio_list.json` (ya root pe `audio_list.json`) edit karo. Format:

```json
{
  "version": 2,
  "updated": "2025-02-01T12:00:00Z",
  "audio": [
    {
      "id": "1",
      "title": "Audio 1",
      "url": "https://example.com/audio1.mp3"
    },
    {
      "id": "2",
      "title": "Audio 2",
      "url": "https://example.com/audio2.mp3"
    }
  ]
}
```

2. Audio file kahi bhi host ho sakti hai (GitHub repo me, ya koi free hosting). Sirf **url** sahi daalna hai.  
3. Git me commit + push:

```bash
git add public/audio_list.json
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
| Audio list URL    | `https://YOUR_USERNAME.github.io/YOUR_REPO_NAME/audio_list.json` (ya `.../public/audio_list.json`) |
| Naya audio        | `audio_list.json` edit → `git add` → `git commit` → `git push` |

**Firebase hosting ki zaroorat nahi – sirf GitHub use karo, space limit nahi.**
