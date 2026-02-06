# App crash – log summary

## Log se jo mila

1. **Fatal signal 6 (SIGABRT)**  
   - Thread: `queued-work-loo` (queued-work-loop)  
   - Firebase "FA" log ke turant baad  
   - Process: `com.swamisachidanand` (pid 29420, 29473, …)

2. **Fatal signal 11 (SIGSEGV)**  
   - Main thread  
   - `fault addr 0x4e8` → null/bad pointer jaisa  
   - Process: `com.swamisachidanand` (pid 29544)

3. **Flow**  
   - App start → `handleBindApplication` → Firebase FA log → crash  
   - Crash app start ke ~1 sec ke andar  
   - Process bar‑bar restart ho raha hai

4. **Baki warnings (crash cause nahi)**  
   - `ResourcesOffloading: java.io.EOFException`  
   - `ClassLoaderContext classpath size mismatch`  
   - `PhenotypeResourceReader: unable to find...`  
   - `E USNET : USNET: appName: com.swamisachidanand` (shayad kisi library ka log)

## Jo fix kiya

- **App.java**  
  Firebase init ab **2 second delay** se ho raha hai taaki pehle UI dikhe; agar crash Firebase ki wajah se hai to kam se kam splash/main 2 sec dikh jaye.

## Aap kya kar sakte ho

1. **Naya build chala ke dekho**  
   - `.\gradlew installDebug`  
   - App kholo – ab 2 sec tak white/splash dikhna chahiye, phir agar crash aata hai to time note karo.

2. **Crash phir bhi 2 sec ke baad aaye to**  
   - Firebase bilkul band karne ke liye `App.java` me `FirebaseApp.initializeApp(this);` wala block comment kar do (ya delete) aur phir run karke dekho. Agar crash band ho jaye to problem Firebase init/analytics side hai.

3. **Fresh logs bhejna**  
   - Crash ke turant baad ye chala ke last 100–200 lines bhejna:  
     `adb logcat -d -t 300 > crash_log.txt`  
   - Ya filter karke:  
     `adb logcat -d -t 300 *:E > errors.txt`

## Crash fir bhi aaye to

- `adb logcat -d -t 500` ka full output (ya `crash_log.txt`) yahan paste karo, phir exact line/stack dekh sakte hain.
