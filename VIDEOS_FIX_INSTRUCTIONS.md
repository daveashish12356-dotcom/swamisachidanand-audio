# Videos Page Fix – Channel Videos Nahi Dikh Rahe

## Option 1: API Key Fix (Sabse Aasan)

Google Cloud Console me jao aur API key me ye add karo:

1. https://console.cloud.google.com/apis/credentials
2. Apna YouTube API key select karo (edit)
3. **Application restrictions** → **Android apps**
4. **Add** click karo:
   - Package name: `com.swamisachidanand`
   - SHA-1: `46:2D:B6:62:82:A6:F7:36:16:78:C2:01:CB:C5:ED:4E:32:7A:10:DB`
5. Save karo

Phir app restart karo – videos load honi chahiye.

---

## Option 2: API Key "None" Restriction

Agar Option 1 kaam na kare:

1. Google Cloud Console → Credentials
2. Naya API key banao (ya existing edit karo)
3. **Application restrictions** → **None** select karo
4. **API restrictions** → YouTube Data API v3
5. `gradle.properties` me `YOUTUBE_API_KEY` ko naya key se replace karo
6. Rebuild: `.\gradlew assembleDebug`

---

## Option 3: Proxy Deploy (100% Kaam Karega)

1. `youtube_proxy/` folder ko Vercel pe deploy karo (https://vercel.com)
2. Environment variable: `YOUTUBE_API_KEY` = naya key (None restriction)
3. `gradle.properties` me add karo:
   ```
   YOUTUBE_PROXY_URL=https://YOUR-PROJECT.vercel.app/api/youtube-videos
   ```
4. Rebuild

---

## Kya Change Kiya

- Sachchidanand-Dantali pehla channel
- 10 Piped + 4 Invidious instances
- 403 pe API key "None" retry
- Proxy support (optional)
- Debug SHA-1 script: `get_debug_sha1.ps1`
