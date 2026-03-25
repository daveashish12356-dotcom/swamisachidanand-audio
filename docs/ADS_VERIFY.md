# Ads verify karna (SwamiAds + AdMob)

## Jo fix ho chuka hai (code)

**Swami Info** screen: layout me `AdView` tha par `loadAd()` call nahi ho raha tha.  
Ab `SwamiInfoFragment.setupSwamiBottomBannerAd()` hai — logcat me placement: **`swami_info_bottom`**.

IDs: `app/src/main/res/values/admob_units.xml`  
Banner unit suffix console se match karo: **`5485911748`** (poora `ca-app-pub-7592986107944664/5485911748`).

---

## 1) Naya APK install karo

Debug build:

```powershell
cd F:\ss
.\gradlew :app:assembleDebug
```

APK path (usually):

`app\build\outputs\apk\debug\app-debug.apk`

Phone par install (USB debugging on):

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## 2) Logcat — saari ad logs ek tag

PC par (phone USB se laga ho):

```powershell
adb logcat -c
adb logcat -s SwamiAds
```

Phir **app kholo** aur ye screens par jao:

- Home, Videos, Books, About, Audio, Pravachan, **Swami Info**

### Samajhna

| Log line | Matlab |
|----------|--------|
| `[BANNER] … loadAd()` | Request bheji |
| `[BANNER] … onAdLoaded` | Banner fill mila |
| `[BANNER] … onAdFailedToLoad` | Fill nahi mila / network / galat ID — **code + msg** dekho |
| `[INTERSTITIAL] …` | Interstitial preload / show / throttle |
| `[REWARDED_PDF] …` | PDF rewarded flow |
| `[SDK] …` | Mobile Ads adapters init |

Zyada detail:

```powershell
adb logcat -s SwamiAds Ads:I
```

---

## 3) AdMob console check

- App ID: `ca-app-pub-7592986107944664~8167456866`
- Banner ad unit: suffix **`5485911748`** — XML se **bilkul same** hona chahiye.

---

## 4) Short note

Hum code se integration verify kar sakte hain; **ad screen par dikh rahi hai ya nahi** final test **device + install + logcat** se hi pakka hota hai.
