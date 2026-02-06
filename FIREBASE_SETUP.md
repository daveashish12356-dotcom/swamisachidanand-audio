# Firebase setup – Shri Swami Sachchidanand Ji (Android)

Firebase is already added in the app. You only need to add your project config file.

## 1. Create a Firebase project (agar abhi nahi hai)

1. Open [Firebase Console](https://console.firebase.google.com/)
2. **Add project** (or use existing project)
3. Project name daalo (e.g. "Shri Swami Sachchidanand")
4. Google Analytics on/off karo, phir **Create project**

## 2. Android app add karo

1. Firebase project open karke **Project overview** → **Add app** → **Android** (Android icon)
2. **Android package name** ye exact daalo: `com.swamisachidanand`
3. App nickname optional (e.g. "Shri Swami Sachchidanand Ji")
4. **Register app** click karo
5. **Download google-services.json** click karo

## 3. File project mein lagao

- Downloaded **google-services.json** ko copy karke yahan paste karo:
  ```
  f:\ss\app\google-services.json
  ```
- Ye file `app` folder ke andar root par honi chahiye (build.gradle.kts ke saath).

## 4. Sync & run

- Android Studio mein **Sync Project with Gradle Files** chalao
- Phir app run karo – Firebase initialize ho jayega

---

**Note:** `google-services.json` ko git mein commit karna safe hai agar project public nahi hai; otherwise `.gitignore` mein add kar sakte ho aur team ko alag se share karo.
