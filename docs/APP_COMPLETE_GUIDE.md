# સ્વામી સચ્ચિદાનંદ એપ — સંપૂર્ણ ગાઇડ (યુઝર + એડમિન + ડેવલપર)

આ ડૉક્યુમેન્ટ રિપો `f:\ss` અને એપ `com.swamisachidanand` પર આધારિત છે. નવા એડમિન / ડેવલપર માટે **એક જ જગ્યાએ** બધું.

---

## 1. એપ શું છે?

| વિગત | માહિતી |
|--------|---------|
| પેકેજ | `com.swamisachidanand` |
| ભાષા UI | મુખ્યત્વે **ગુજરાતી** |
| લોન્ચ | `SplashActivity` → **પહેલી વાર** `AppTourActivity` (ટૂર) → `MainActivity` · પછી સીધું `MainActivity` |
| ડેટા | ઇન્ટરનેટ + **Firebase** (Firestore, FCM) + GitHub Pages URL |

---

## 2. યુઝર — નીચેનું મેનુ (Bottom navigation)

| ટેબ | સ્ક્રીન | શું મળે છે |
|-----|---------|------------|
| **હોમ** | `HomeFragment` | સુવિચાર/હીરો વિડિઓ, શોધ, ઝડપી લિંકો (પુસ્તક/ઓડિયો/પ્રવચન/વિડિઓ), નવા સેક્શન |
| **પુસ્તક** | `BooksFragment` | પીડીએફ પુસ્તકો, કેટેગરી, `ModernPdfActivity` માં વાંચન |
| **ઓડિયોબુક** | `ServerAudioFragment` | સર્વર લિસ્ટ (`books_server_list.json` / સર્વર URL) થી ઓડિયો પુસ્તકો |
| **પ્રવચન** | `AudioPravachanFragment` | **Firestore** `pravachan` — ઓડિયો સ્ટ્રીમ, શોધ, pagination |
| **વિડિઓ** | `VideosFragment` | YouTube (API/RSS/Invidious વગેરે) + **Firestore `yt_feed/latest`** |
| **સંપર્ક** | `AboutFragment` | સંપર્ક, લિંક્સ; છુપો સુવિચાર એડમિન (ફોટા પર 10 ટેપ) |

**અન્ય એક્ટિવિટી (મેનુ બહારથી ખુલે):**

- `BookStoreActivity` — પુસ્તકાલય કાર્ડ, ઓર્ડર લિંક
- `YouTubePlayerActivity` — ઇન-એપ YouTube
- `SearchResultActivity` / `SearchResultFragment` — ગ્લોબલ શોધ પરિણામ
- `SwamiInfoFragment` — સ્વામી માહિતી (મુખ્ય એક્ટિવિટીમાં ફ્રેગમેન્ટ)
- `PhotoGalleryActivity` — ફોટો ગેલેરી
- `AppTourActivity` — પ્રથમ વખત ટૂર

---

## 3. ડેટા ક્યાંથી આવે છે? (સર્વર / સોર્સ)

### 3.1 પુસ્તક (PDF)

- લિસ્ટ / મેટાડેટા: મુખ્યત્વે **`books_server_list.json`** અને સંબંધિત એસેટ્સ (બેઝ URL `strings.xml` માં `server_books_base_url`).
- ઓફલાઇન / કેશ લોજિક `BooksFragment` અને સંબંધિત હેલ્પરમાં.

### 3.2 ઓડિયો પુસ્તકો (ઓડિયોબુક ટેબ)

- **`ServerAudioFragment`** — સર્વર JSON + ઓડિયો URL.

### 3.3 પ્રવચન (ઓડિયો)

| સ્ત્રોત | વર્ણન |
|---------|--------|
| **Firestore** `pravachan` | દર ડૉક: `title`, `audioUrl` અથવા `audio_url`, `createdAt`, `speaker` વગેરે |
| **Telegram** | `functions/index.js` → `telegramPravachanWebhook` નવું ઓડિયો આવે ત્યારે Firestoreમાં લખે + FCM `new_pravachan` |

એપમાં **`AudioPravachanFragment`**: પેજ સાઇઝ ~100, `orderBy("createdAt")`, pagination.

### 3.4 વિડિઓ

| સ્ત્રોત | વર્ણન |
|---------|--------|
| **Firestore** `yt_feed` / `latest` | Cloud Function **`pollYouTubeNewVideos`** merged લિસ્ટ લખે છે |
| **YouTube સીધું** | API key (`local.properties` / બિલ્ડ કોન્ફિગ), Invidious, RSS, HTML fallback — `VideosFragment` |

ચેનલ હેન્ડલ્સ એપમાં: `Sachchidanand-Dantali`, `swamisachchidanandji`, `SwamiSachchidanand` (એક ચેનલ જાણીજોઈને બાકાત).

### 3.5 સુવિચાર (આજનું ચિંતન)

- રિમોટ: `suvichar_config.json` (GitHub / Firebase URL — `HomeFragment`).
- Push: FCM topic **`suvichar`** + Cloud Function **`sendSuvichar`** (વિગત `FIREBASE_SETUP.md`).

### 3.6 પુસ્તકાલય સરનામું (કાર્ડ)

- `layout_book_store_info_card.xml` + `public/books_store.json` (`address` વગેરે).

---

## 4. નોટિફિકેશન (FCM)

એપ `App.java` માં આ ટોપિક્સ subscribe કરે છે: `suvichar`, `new_book`, `new_audio`, `new_video`.

| ટોપિક / `kind` | શું થાય | હેન્ડલર |
|----------------|---------|---------|
| `suvichar` | આજનું ચિંતન | `MyFirebaseService` → `SuvicharNotificationHelper` |
| `new_video` | નવું YouTube | `ContentUpdateNotificationHelper` → વિડિઓ ટેબ |
| `new_book` / `new_audio` / `new_pravachan` | સામાન્ય કન્ટેન્ટ | `ContentUpdateNotificationHelper` |

**સર્વર પર વિડિઓ:** `pollYouTubeNewVideos` (૩૦ મિનિટ) + manual **`triggerYouTubePollNow`** — સંપૂર્ણ સ્ટેપ **`docs/SERVER_VIDEO_FCM.md`**.

**લોકલ (ફક્ત એપ ખુલી હોય ત્યારે):** `VideosFragment` નવી લિસ્ટ પર ટોપ વિડિયો બદલાય ત્યારે — `docs/VIDEO_NOTIFICATIONS.md`.

**Android 13+:** `POST_NOTIFICATIONS` પરમિશન જરૂરી.

---

## 5. Cloud Functions (Firebase)

પ્રોજેક્ટ: **`swami-sachidanand`** (`.firebaserc`).

| Function | ઉપયોગ |
|----------|--------|
| `sendSuvichar` | સુવિચાર POST + FCM `suvichar` |
| `pollYouTubeNewVideos` | YouTube પોલ → `yt_feed` + FCM `new_video` |
| `triggerYouTubePollNow` | તરત પોલ (કી સાથે) |
| `telegramPravachanWebhook` | Telegram ઓડિયો → Firestore `pravachan` + FCM |

કોન્ફિગ: `youtube.key`, `admin.key` — `firebase functions:config:get`

ડિપ્લોય: `cd functions && npm install` પછી `npx firebase-tools deploy --only functions`

---

## 6. એડ્સ (AdMob)

- એપ ID / યુનિટ્સ `res/values` / `admob_units.xml` વગેરે.
- ચકાસણી: **`docs/ADS_VERIFY.md`**.

---

## 7. બિલ્ડ અને ઇન્સ્ટોલ (ડેવલપર)

```bash
cd f:\ss
.\gradlew :app:assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

- YouTube API key (વિડિઓ ટેબ): `local.properties` માં `YOUTUBE_API_KEY=...` (પ્રોજેક્ટ પ્રમાણે).

---

## 8. મહત્વની ફાઇલો (નકશો)

| વિસ્તાર | ફાઇલો |
|---------|--------|
| નેવિગેશન | `MainActivity.java`, `bottom_navigation_menu.xml` |
| હોમ | `HomeFragment.java` |
| પુસ્તક | `BooksFragment.java`, `ModernPdfActivity.java` |
| ઓડિયોબુક | `ServerAudioFragment.java` |
| પ્રવચન | `AudioPravachanFragment.java`, `PravachanAdapter.java` |
| વિડિઓ | `VideosFragment.java` |
| FCM | `App.java`, `MyFirebaseService.java`, `ContentUpdateNotificationHelper.java` |
| સર્વર લોજિક | `functions/index.js` |
| Firebase સેટઅપ લેખ | `FIREBASE_SETUP.md` |

---

## 9. સંબંધિત ડૉક્યુમેન્ટ્સ (આ રિપોમાં)

| ફાઇલ | વિષય |
|------|------|
| `FIREBASE_SETUP.md` | સુવિચાર, deploy, વેબ નોટિફાય |
| `docs/SERVER_VIDEO_FCM.md` | YouTube સર્વર FCM + `youtube.key` |
| `docs/VIDEO_NOTIFICATIONS.md` | લોકલ vs FCM વિડિઓ |
| `docs/ADS_VERIFY.md` | AdMob ચેકલિસ્ટ |
| `APP_ADS_TXT_SETUP.md` | ads.txt / હોસ્ટિંગ |

---

## 10. ટૂંકો જવાબ — “સબ સર્વરથી નવું મળશે?”

- **પ્રવચન:** હા — મુખ્યત્વે **Firestore** (+ Telegram webhook થી નવું ઉમેરાય).
- **વિડિઓ:** **Firestore `yt_feed`** (ફંક્શન) **અને** એપનું **સીધું YouTube fetch** — બંને.
- **ઓડિયોબુક / પુસ્તક:** સર્વર JSON / URLs પ્રમાણે.

આ ગાઇડ સમયાંતરે અપડેટ રાખો જ્યારે નવી ટેબ કે નવો સર્વર રસ્તો ઉમેરો.
