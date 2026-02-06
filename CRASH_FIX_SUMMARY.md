# App crash fix — jo changes kiye

## Problem
App open hote hi crash ho raha hai (immediate crash).

## Fixes applied

### 1. MainActivity.onCreate()
- **Fragment transaction:** `commitNow()` ko try-catch me wrap kiya
- **Fallback:** Agar `commitNow()` fail ho to async `commit()` use karega
- **Reason:** `commitNow()` synchronous hai — agar HomeFragment crash kare to app crash ho jata hai

### 2. HomeFragment.onCreateView()
- **Full try-catch:** Pure `onCreateView()` ko try-catch me wrap kiya
- **Fallback view:** Agar crash ho to empty FrameLayout return karega (app crash nahi hoga)
- **getContext() null checks:** Line 98 aur 148 pe `getContext()` null check add kiye
- **Reason:** Fragment detach ho to `getContext()` null ho sakta hai → NPE

### 3. App.java (Firebase)
- **Firebase init disabled:** Temporarily comment out kiya (crash debug ke liye)
- **Reason:** Log me SIGABRT/SIGSEGV Firebase "FA" log ke baad dikha tha — test karne ke liye disable kiya

## Test karein

1. **Build & install:**
   ```bash
   .\gradlew installDebug
   ```

2. **App open karein:**
   - Agar ab crash nahi hota → Firebase ya HomeFragment issue tha
   - Agar phir bhi crash → fresh logs bhejna

3. **Agar crash phir bhi ho:**
   ```bash
   adb logcat -c
   adb shell am start -n com.swamisachidanand/.MainActivity
   Start-Sleep -Seconds 2
   adb logcat -d -t 300 > crash_log.txt
   ```
   Phir `crash_log.txt` yahan paste karein.

## Next steps

- Agar app ab open ho jaye → Firebase ko re-enable karein (delay ke saath)
- Agar phir bhi crash → exact error line dekh kar fix karein
