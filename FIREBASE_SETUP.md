# Firebase – આજનું ચિંતન Notification Setup

App already subscribes to Firebase topic **`suvichar`**. When you send a message to this topic, all installed apps get the notification **"આજનું ચિંતન"** (even when app is closed). Suvichar content stays only in Firebase; no need to show it on GitHub website.

---

## 1. Firebase project

- Go to [Firebase Console](https://console.firebase.google.com/)
- Select your project (the one used in `app/google-services.json`)
- If new: Add project → Add Android app with package `com.swamisachidanand` → download `google-services.json` and put it in **`app/google-services.json`**

---

## 2. Send "આજનું ચિંતન" from Firebase Console

1. In Firebase Console: **Engage** → **Messaging** (or **Build** → **Cloud Messaging**).
2. Click **Create your first campaign** or **New campaign** → **Firebase Notification messages**.
3. **Notification:**
   - Title: `આજનું ચિંતન` (or leave blank; app uses this title from strings).
   - Body: type today’s suvichar text here (optional if you use Custom data).
4. Click **Send test message** to try, or **Next**.
5. **Target:**
   - Choose **Topic**.
   - Topic name: **`suvichar`** (exactly this).
6. **Schedule:** Now or set time.
7. **Additional options** (optional): set expiry, etc.
8. **Review** → **Publish**.

All devices that have the app and are subscribed to topic `suvichar` will receive the notification.

---

## 3. Using Custom data (recommended for long text)

So the full suvichar shows in the notification body and in BigTextStyle:

1. In the same **Compose notification** screen, expand **Additional options**.
2. Under **Custom data** add:
   - Key: `text`  → Value: full suvichar text (Gujarati).
   - Key: `author` → Value: author name (optional, e.g. સ્વામી સચ્ચિદાનંદ).
3. **Notification title** can be: `આજનું ચિંતન`.
4. **Notification body** can be short (e.g. first line) or leave blank; app will show `text` in the big notification.

App’s `MyFirebaseService` reads `text` and `author` from data and shows them in the notification.

---

## 4. First-time permission (Android 13+)

On first open, the app asks for **Notifications** permission. User must allow so that "આજનું ચિંતન" notifications appear.

---

## Summary

| Item        | Value                          |
|------------|---------------------------------|
| Topic      | `suvichar`                      |
| Data keys  | `text` (suvichar), `author` (optional) |
| Channel    | આજનું ચિંતન (in app)           |

No need to put suvichar on the GitHub website; only Firebase is used for notifications.

---

## 5. Firebase website (GitHub જેવી જ – અહીંથી નોટિફિકેશન ભેજો)

Firebase પર એક પેજ છે જ્યાંથી તમે સુવિચાર લખીને એક ક્લિકમાં નોટિફિકેશન ભેજી શકો (Console માં જવાની જરૂર નથી).

### Deploy (એક વાર)

1. **Node.js 18** install હોવું જોઈએ. પછી terminal માં:
   ```bash
   cd f:\ss
   cd functions
   npm install
   cd ..
   ```

2. **Secret key** પહેલેથી સેટ છે: <code>suvichar_secret_2024</code>. બદલવી હોય તો:
   ```bash
   npx firebase-tools functions:config:set admin.key="તમારો_ગુપ્ત_કી"
   ```

3. **Blaze upgrade (એક વાર):** Firebase Console → Project → Upgrade to Blaze (pay-as-you-go). Free quota માં જ ચાલશે; કાર્ડ લગાવવું પડશે પણ charge નહીં થાય જ્યાં સુધી limit ઓળંગો નહીં.

4. **Deploy** કરો (Hosting + Functions બંને):
   ```bash
   cd f:\ss
   npx firebase-tools deploy
   ```
   (માત્ર Hosting: <code>npx firebase-tools deploy --only hosting</code>)

### વેબસાઇટ લિંક

Deploy પછી:
- **Website:** `https://swami-sachidanand.web.app/suvichar-notify.html`
- આ પેજ ખોલો → સુવિચાર લખો → Secret key (ઉપર set કરેલ) એક વાર લગાવો → **Notification Bhejo** દબાવો. સૌને મોબાઇલ પર "આજનું ચિંતન" નોટિફિકેશન જશે.

GitHub જેવી જ – ફક્ત અહીં Firebase પર છે, અને નોટિફિકેશન સીધું જ ભેજાય છે.
