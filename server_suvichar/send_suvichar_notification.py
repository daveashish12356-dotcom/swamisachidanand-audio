"""
Simple helper script to send a new Suvichar push notification via Firebase Cloud Messaging (FCM).

Usage:
1) Install dependencies (once):
   pip install requests

2) Set your Firebase server key safely (do NOT commit the real key):
   - Option A (recommended): environment variable
       set FCM_SERVER_KEY=AAAA...your-server-key...
   - Option B: paste into SERVER_KEY below (only on your local machine)

3) Run:
   python send_suvichar_notification.py
   (it will ask you for text and author)
"""

import os
import sys
import json
from getpass import getpass

import requests


# Do NOT commit real key to git. Prefer environment variable FCM_SERVER_KEY.
SERVER_KEY = os.environ.get("FCM_SERVER_KEY", "").strip()

FCM_URL = "https://fcm.googleapis.com/fcm/send"
TOPIC = "/topics/suvichar"


def read_input(prompt: str) -> str:
    try:
        return input(prompt)
    except EOFError:
        return ""


def main() -> None:
    global SERVER_KEY

    if not SERVER_KEY:
        print("⚠ FCM_SERVER_KEY env var not set.")
        raw = getpass("Paste your Firebase server key (will not echo): ").strip()
        if not raw:
            print("No server key provided. Aborting.")
            sys.exit(1)
        SERVER_KEY = raw

    print("=== New Suvichar Notification ===")
    text = read_input("Suvichar text: ").strip()
    if not text:
        print("Suvichar text is required.")
        sys.exit(1)

    author = read_input("Author (optional, default: સ્વામી શ્રીસચ્ચિદાનંદજી): ").strip()
    if not author:
        author = "સ્વામી શ્રીસચ્ચિદાનંદજી"

    payload = {
        "to": TOPIC,
        "data": {
            "text": text,
            "author": author,
        },
    }

    headers = {
        "Content-Type": "application/json; charset=utf-8",
        "Authorization": "key=" + SERVER_KEY,
    }

    print("\nSending to FCM...")
    try:
        resp = requests.post(FCM_URL, headers=headers, data=json.dumps(payload), timeout=10)
    except Exception as e:
        print("Request failed:", e)
        sys.exit(1)

    print("Status:", resp.status_code)
    try:
        print("Response:", resp.json())
    except Exception:
        print("Raw response:", resp.text)

    if resp.status_code == 200:
        print("\n✅ Suvichar push request sent. If app is installed & subscribed to topic 'suvichar',")
        print("   users should receive the notification (even if app is closed).")
    else:
        print("\n❌ Something went wrong. Check server key and internet, then try again.")


if __name__ == "__main__":
    main()

