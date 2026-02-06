# Hosting – Server se app connect

## 1. Git (code backup)

```bash
cd f:\ss
git init
git add .
git commit -m "Initial commit - app + Firebase hosting"
```

Remote add karne ke liye (GitHub/GitLab):

```bash
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
git push -u origin main
```

---

## 2. Firebase Hosting (server / URL)

Project mein Firebase pehle se hai. Hosting URL: **https://swami-sachidanand.web.app**

### Deploy karna

1. Firebase CLI install (ek baar):  
   `npm install -g firebase-tools`
2. Login:  
   `firebase login`
3. Deploy:  
   `firebase deploy`

Deploy ke baad ye URL kaam karega:

- **Audio list (JSON):**  
  `https://swami-sachidanand.web.app/audio_list.json`

App isi URL se list fetch karke naya audio dikha sakti hai (future me app code add karenge).

---

## 3. Naya audio add karna (server side)

### Option A: Sirf JSON update (audio kisi aur hosting pe ho)

`public/audio_list.json` edit karo, format:

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

Phir:

```bash
firebase deploy
```

App jab list fetch karegi to ye naya audio dikhega.

### Option B: Audio Firebase Storage pe upload karna

1. Firebase Console → Storage → file upload (e.g. `audio/xyz.mp3`)
2. File ka **download URL** copy karo
3. Wahi URL `public/audio_list.json` ke `audio` array mein add karo (jaise upar)
4. `firebase deploy` chala do

---

## 4. Summary

| Kaam              | Command / Step                                      |
|-------------------|-----------------------------------------------------|
| Git me sab commit | `git add .` → `git commit -m "..."`                 |
| Hosting deploy    | `firebase deploy`                                   |
| Audio list URL    | https://swami-sachidanand.web.app/audio_list.json   |
| Naya audio        | `audio_list.json` edit → `firebase deploy`          |

Ab app me kuch change nahi kiya – sirf Git + hosting setup aur ye instructions diye. Jab app me “server se audio list fetch” wala code add karoge tab isi `audio_list.json` URL se connect karna.
