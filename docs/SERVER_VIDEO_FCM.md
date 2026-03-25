# સર્વર પર નવું YouTube વિડિયો → FCM (એપ બંધ / ખુલી)

આ પૂરું **Firebase Cloud Functions** પર ચાલે છે — ફોન બંધ હોય ત્યારે પણ **સિસ્ટમ નોટિફિકેશન** આવી શકે (topic `new_video`).

## શું થાય છે?

1. **`pollYouTubeNewVideos`** — દર **૩૦ મિનિટે** (Asia/Kolkata) ચાલે છે.
2. **YouTube Data API** થી આ હેન્ડલ્સ રિઝોલ્વ થાય છે (એપના `VideosFragment` જેવા જ):
   - `Sachchidanand-Dantali`
   - `swamisachchidanandji`
   - `SwamiSachchidanand`
3. દર ચેનલ પર નવા વિડિયો `yt_channels/{channelId}` માં સાચવેલા `lastPublishedAt` કરતાં નવા હોય તો **FCM topic `new_video`** પર મોકલાય છે.
4. **`yt_feed/latest`** માં બધી ચેનલોનો **merge** થયેલ લિસ્ટ (max 50) લખાય છે — એપનું Firestore feed અહીંથી અપડેટ થાય છે.
5. FCM માં **`data.title`**, **`data.text`** (વિડિયો શીર્ષક), **`thumbUrl`** — એપ `MyFirebaseService` + `ContentUpdateNotificationHelper` થી સાચું શીર્ષક/બોડી બતાવે.

## એક વાર સેટઅપ (જરૂરી)

```bash
cd functions
npm install
cd ..

# YouTube Data API v3 key (Google Cloud Console → APIs → YouTube Data API v3)
npx firebase-tools functions:config:set youtube.key="AIza..."

# (વૈકલ્પિક) હેન્ડલ લિસ્ટ ઓવરરાઇડ
# npx firebase-tools functions:config:set youtube.handles="Handle1,Handle2"

# સુવિચાર સાથે જ admin key (trigger URL માટે)
npx firebase-tools functions:config:set admin.key="તમારો_સિક્રેટ"
```

- **Blaze** પ્લાન જોઈએ (શેડ્યૂલ્ડ functions માટે).
- Deploy: `npx firebase-tools deploy --only functions`

## ચેક કરો કે કામ કરે છે

1. Firebase Console → **Functions** → **Logs** — `runYouTubePollOnce` / `pollYouTubeNewVideos` માં error નહીં.
2. જો `youtube.key not set` દેખાય તો ઉપરનો `config:set` ફરી ચલાવો.
3. **મેન્યુઅલ ટ્રિગર** (ટેસ્ટ):
   ```
   https://REGION-PROJECT.cloudfunctions.net/triggerYouTubePollNow?key=તમારો_admin.key
   ```
   (REGION/URL deploy પછી Console માંથી કોપી કરો.)  
   જવાબ JSON: `{ ok: true, channels: 3, fcmNotifications: N }`

## એપ તરફથી

- `App.java` topic **`new_video`** subscribe કરે છે.
- Android 13+ પર **નોટિફિકેશન પરમિશન** ચાલુ હોવી જોઈએ.

## સ્થાનિક નોટિફિકેશન (ફક્ત એપ ખુલી હોય ત્યારે)

`VideosFragment` નવી લિસ્ટ લોડ થાય ત્યારે પણ ટોપ વિડિયો બદલાય તો લોકલ નોટિફિકેશન આવે — આ સર્વરનો વિકલ્પ નથી, extra છે.
