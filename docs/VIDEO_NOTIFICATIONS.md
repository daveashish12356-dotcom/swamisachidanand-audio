# YouTube / વિડિઓ નોટિફિકેશન

## પહેલાં શું થતું હતું?

- એપ `new_video` FCM ટોપિક પર subscribe છે (`App.java`).
- `MyFirebaseService` માં `kind=new_video` (અથવા `new_book`, `new_audio`) સાથે **server થી push આવે** ત્યારે જ `ContentUpdateNotificationHelper` નોટિફિકેશન બતાવે.
- **YouTube API / RSS / Firebase feed થી લિસ્ટ અપડેટ થાય ત્યારે કોઈ push મોકલાતું નહોતું** — એટલે નવું વિડિયો એપમાં દેખાય પણ નોટિફિકેશન ન આવે.

## હવે શું ઉમેર્યું?

- `VideosFragment` નવી લિસ્ટ લોડ થયા પછી (YouTube fetch **અને** Firestore feed) સૌથી ઉપરનું `videoId` પહેલાં સાચવેલા id સાથે સરખાવે છે.
- જો **ટોપ વિડિયો બદલાયો** હોય તો લોકલ નોટિફિકેશન (`નવું YouTube વિડિયો` + શીર્ષક, થંબનેઇલ) — ટેપ કરતાં વિડિઓ ટેબ ખુલે છે.
- પહેલી વાર લોડ (baseline): માત્ર id સેવ થાય, નોટિફિકેશન નહીં.

## Android 13+

- નોટિફિકેશન માટે **POST_NOTIFICATIONS** પરમિશન જોઈએ. ન હોય તો લોગમાં warning આવશે; id અપડેટ થઈ જશે જેથી પરમિશન મળ્યા પછી ડુપ્લિકેટ ન થાય.

## સર્વર (એપ બંધ હોય ત્યારે પણ)

વિગતો: **`docs/SERVER_VIDEO_FCM.md`**

સંક્ષિપ્ત: `functions/index.js` માં **`pollYouTubeNewVideos`** (૩૦ મિનિટ) + **`youtube.key`** કોન્ફિગ + **`firebase deploy --only functions`**.

## હજુ પણ FCM થી manual push

- Firebase Console થી ટોપિક `new_video` પર મેસેજ મોકલો તો પણ કામ કરશે (`data.kind=new_video`, `title`, `text`, optional `thumbUrl`).
