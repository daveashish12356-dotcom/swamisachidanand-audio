# App-ads.txt verification – Shri Swami Sachchidanand Ji (Android)

AdMob verification ke liye `app-ads.txt` file ready hai. Ab ise **developer website** par host karna hai.

## File kahan hai

- **Project mein:** `f:\ss\app-ads.txt`
- **Hosting ke liye copy:** `f:\ss\public\app-ads.txt` (Firebase Hosting use karenge to yahi use hoga)

Line (exact):  
`google.com, pub-7592986107944664, DIRECT, f08c47fec0942fa0`

---

## Option A: Firebase Hosting se host karna (recommended)

Aapka Firebase project **swami-sachidanand** hai. Isi par ek chhota site host karke sirf `app-ads.txt` serve karenge.

### 1. Deploy karo

Project root (`f:\ss`) par terminal mein:

```powershell
npx firebase-tools deploy --only hosting
```

Deploy ke baad URL milega, jaise:  
**https://swami-sachidanand.web.app**

### 2. Google Play Console – Developer website

1. [Google Play Console](https://play.google.com/console) → **Shri Swami Sachchidanand Ji** app
2. **Policy** → **App content** (ya **Store listing** / **Main store listing**)
3. **Developer contact** / **Website** wahan **Developer website** URL daalo
4. URL **exact** daalo: `https://swami-sachidanand.web.app` (trailing slash ke bina, https)
5. Save karo

Is URL par file address hogi:  
**https://swami-sachidanand.web.app/app-ads.txt**

### 3. AdMob – Verify

1. AdMob → **Apps** → **Shri Swami Sachchidanand Ji (Android)**
2. **Check for updates** (ya “Verify app”) click karo
3. Thodi der wait karo – verification ho jani chahiye

---

## Option B: Apni existing website par upload

Agar aapke paas pehle se developer website hai (jo Play Console mein daali hai):

1. Us site ke **root** par `app-ads.txt` upload karo  
   (matlab: `https://yourdomain.com/app-ads.txt` open hone par file dikhe)
2. File ka content yehi hona chahiye (ek line):  
   `google.com, pub-7592986107944664, DIRECT, f08c47fec0942fa0`
3. Play Console mein **Developer website** wahi domain hona chahiye (exact match)
4. AdMob mein **Check for updates** / Verify karo

---

## Important

- **Domain match:** Play Console ka “Developer website” aur jis domain par `app-ads.txt` hai, dono same hone chahiye (exact).
- **File name:** Sirf `app-ads.txt` (small letters, hyphen).
- **Content:** Line ke beech extra space mat daalna; copy-paste `app-ads.txt` se hi karo.

Verification fail ho to:
- Browser mein direct open karke check karo: `https://your-website.com/app-ads.txt`
- AdMob account (pub-7592986107944664) aur Play Console ka developer account same Google account se hona chahiye.
