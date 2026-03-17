# InfinityFree – Suvichar Bot Setup Steps

## 2️⃣ Password & keys fill karo (online editor me)

1. File Manager me **`bot_upload.php`** pe right‑click → **Edit** (ya pencil icon).
2. Upar ke paas ye line dikhni chahiye:
   ```php
   $dbPass = "YOUR_DB_PASSWORD";    // yahan apna real password dalna hai
   ```
3. **YOUR_DB_PASSWORD** ko MySQL screen wale password se replace karo (InfinityFree → MySQL Connection Details → MYSQL PASSWORD) → **Save**.
4. Ab **`webhook.php`** ko Edit karo. Upar 2 lines me apne real keys daalo:
   ```php
   $botToken    = "YOUR_BOT_TOKEN_HERE";
   $ocrApiKey   = "YOUR_OCR_API_KEY_HERE";
   ```
   - Pehle me **Telegram bot ka token** (BotFather wala).
   - Doosre me **OCR API key** (e.g. ocr.space).
5. Dono change karke **Save**.

---

## 3️⃣ Telegram webhook set karo

Browser ke address bar me ye URL likho (apna token daal ke):

```
https://api.telegram.org/botYOUR_BOT_TOKEN_HERE/setWebhook?url=https://ashish-dave.infinityfree.me/webhook.php
```

- **YOUR_BOT_TOKEN_HERE** ko real Telegram bot token se replace karo.
- Enter dabao → agar sab sahi hai to JSON jaisa text aayega: `{"ok":true,...}`.

---

## 4️⃣ Test

1. Telegram me apne bot ko **ek suvichar wali photo** bhejo.
2. Fir **phpMyAdmin** me: **if0_41315067_suvichar** DB → left me **suvichar** table → **Browse** pe click karo.
3. Agar nayi row me text dikh raha hai to system chal raha hai.

---

Jahan pe atak jao (File Manager ka screen, edit ka button, ya webhook ka result), uska screenshot bhej do, ussi ke hisaab se next step bata dunga.
