# Pravachan + Telegram (batch / album)

## Kyon ek saath bhejne par problem hoti thi

1. **Firestore list query** sirf `orderBy("createdAt")` thi. Album ke saari files ka `createdAt` **ek hi second** me aa sakta hai — `startAfter` pagination par **kuch docs skip** ho sakte the. Ab query: `createdAt` + `documentId` (composite index).

2. **Cloud Function** har file ke liye Telegram ko `sendMessage` karti hai. Bahut messages = **429 Too Many Requests** — ab **retry + wait** hai.

3. **Python polling bot** (`telegram_pravachan_bot.py`) pehle **ek thread** par tha: pehli 100MB upload chal rahi ho to baaki files queue me atakti thi. Ab **6 parallel** workers.

## Deploy (function)

```bash
npx firebase-tools deploy --only functions:telegramPravachanWebhook
```

Firestore ne `createdAt` + `__name__` composite deploy reject kiya (“not necessary”) — agar app me query error aaye to Console me diye gaye link se index bana lena.

## Storage

- **Production webhook** (`functions/index.js`): file Firebase Storage me **copy nahi** — sirf **Telegram `file` URL** Firestore `audioUrl` me save.
- **Python tool**: Storage `pravachan/<filename>` par upload.

## Screenshot wala “31–40 chapter missing”

Wo batch me **wo file select hi nahi hui** hogi (8 files me gap) — code usko Telegram se receive nahi karta jab tak bhejo hi na.
