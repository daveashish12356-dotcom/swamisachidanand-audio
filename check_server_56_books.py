#!/usr/bin/env python3
"""
Check if all 56 books and thumbnails are online at server.
Uses same base URL and encoding as app (ServerBookLoader).
Run: python check_server_56_books.py
"""
import json
import urllib.request
import urllib.parse
import ssl

BASE_URL = "https://daveashish12356-dotcom.github.io/swamisachidanand-audio/"
JSON_PATH = "app/src/main/assets/books_server_list.json"

def main():
    base = BASE_URL.rstrip("/") + "/"
    books_base = base + "books/"
    thumb_base = base + "thumbnails/"

    with open(JSON_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)
    names = data.get("fileNames", [])
    pdf_names = [n.strip() for n in names if n.strip().lower().endswith(".pdf")]
    print(f"Total PDF names in JSON: {len(pdf_names)}")
    print(f"Base URL: {base}\n")

    ctx = ssl.create_default_context()
    thumb_ok = 0
    book_ok = 0
    thumb_fail = []
    book_fail = []

    for i, fileName in enumerate(pdf_names):
        thumb_name = fileName.replace(".pdf", ".jpg").replace(".PDF", ".jpg")
        enc_pdf = urllib.parse.quote(fileName, safe="").replace("+", "%20")
        enc_thumb = urllib.parse.quote(thumb_name, safe="").replace("+", "%20")
        pdf_url = books_base + enc_pdf
        thumb_url = thumb_base + enc_thumb

        try:
            req = urllib.request.Request(thumb_url, method="HEAD")
            with urllib.request.urlopen(req, timeout=10, context=ctx) as r:
                if 200 <= r.status < 400:
                    thumb_ok += 1
                else:
                    thumb_fail.append((fileName, r.status))
        except Exception as e:
            thumb_fail.append((fileName, str(e)))

        try:
            req = urllib.request.Request(pdf_url, method="HEAD")
            with urllib.request.urlopen(req, timeout=10, context=ctx) as r:
                if 200 <= r.status < 400:
                    book_ok += 1
                else:
                    book_fail.append((fileName, r.status))
        except Exception as e:
            book_fail.append((fileName, str(e)))

    total = len(pdf_names)
    print("--- Result ---")
    print(f"Thumbnails: {thumb_ok}/{total} OK")
    print(f"Books (PDF): {book_ok}/{total} OK")
    if thumb_fail:
        print(f"\nThumbnail failed count: {len(thumb_fail)} (first status: {thumb_fail[0][1]!r})")
    if book_fail:
        print(f"PDF failed count: {len(book_fail)} (first status: {book_fail[0][1]!r})")
    if thumb_ok == total and book_ok == total:
        print("\n56 books server par ready / online.")
    else:
        print("\nKuch files 404 - repo me books/ aur thumbnails/ upload karo (same file names).")

if __name__ == "__main__":
    main()
