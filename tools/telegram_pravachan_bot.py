"""
Simple Telegram bot to upload MP3 pravachans to Firebase.

Flow:
- User sends MP3 (audio or document) to bot, with optional caption as title.
- Bot uploads file to Firebase Storage at pravachan/<filename>.
- Bot creates a document in Firestore collection "pravachan".

Album / kai file ek saath: Telegram alag-alag updates bhejta hai. Polling bot me
pehle sequential tha — bada file 1 chal raha ho to 2..8 queue me atke. Ab har
message alag thread par process hota hai (max 6 parallel).
"""

import os
import tempfile
import concurrent.futures

import firebase_admin
from firebase_admin import credentials, storage, firestore
from telegram.ext import Updater, MessageHandler, Filters


def init_firebase():
    # Path to service account JSON (download from Firebase console)
    service_path = os.environ.get("FIREBASE_SERVICE_JSON", "firebase-service-account.json")
    project_bucket = os.environ.get("FIREBASE_BUCKET", "YOUR_PROJECT_ID.appspot.com")

    cred = credentials.Certificate(service_path)
    firebase_admin.initialize_app(cred, {"storageBucket": project_bucket})
    db = firestore.client()
    bucket = storage.bucket()
    return db, bucket


def process_one_audio(db, bucket, update, context):
    msg = update.effective_message
    if not msg:
        return

    file_obj = None
    filename = None

    if msg.audio:
        file_obj = msg.audio.get_file()
        filename = msg.audio.file_name or f"audio_{msg.audio.file_unique_id}.mp3"
    elif msg.document and msg.document.mime_type and msg.document.mime_type.startswith("audio/"):
        file_obj = msg.document.get_file()
        filename = msg.document.file_name or f"audio_{msg.document.file_unique_id}.mp3"

    if not file_obj:
        try:
            msg.reply_text("કૃપા કરીને MP3 ફાઇલ (audio/document) મોકલો.")
        except Exception:
            pass
        return

    try:
        msg.reply_text("Upload ચાલી રહ્યું છે…")
    except Exception:
        pass

    with tempfile.TemporaryDirectory() as tmpdir:
        local_path = os.path.join(tmpdir, filename)
        file_obj.download(custom_path=local_path)

        blob_path = f"pravachan/{filename}"
        blob = bucket.blob(blob_path)
        blob.upload_from_filename(local_path, content_type="audio/mpeg")
        blob.make_public()
        audio_url = blob.public_url

    title = (msg.caption or filename).strip()
    if not title:
        title = filename

    doc = {
        "title": title,
        "speaker": "સ્વામી સચ્ચિદાનંદ",
        "audioUrl": audio_url,
        "durationSec": 0,
        "tags": ["pravachan"],
        "createdAt": firestore.SERVER_TIMESTAMP,
    }
    db.collection("pravachan").add(doc)

    try:
        msg.reply_text("પ્રવચન સફળતાપૂર્વક ઉમેરાઈ ગયું. App refresh પછી દેખાશે.")
    except Exception:
        pass


def make_bot(db, bucket):
    executor = concurrent.futures.ThreadPoolExecutor(max_workers=6)

    def handle_audio(update, context):
        executor.submit(process_one_audio, db, bucket, update, context)

    return handle_audio


def main():
    telegram_token = os.environ.get("TELEGRAM_BOT_TOKEN", "")
    if not telegram_token:
        raise SystemExit("Set TELEGRAM_BOT_TOKEN env var first.")

    db, bucket = init_firebase()
    updater = Updater(telegram_token, use_context=True)
    handler = make_bot(db, bucket)

    dp = updater.dispatcher
    dp.add_handler(MessageHandler(Filters.audio | Filters.document, handler))

    updater.start_polling()
    updater.idle()


if __name__ == "__main__":
    main()
