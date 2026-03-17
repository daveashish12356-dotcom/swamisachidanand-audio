#!/usr/bin/env python3
"""
Generate thumbnail images from first page of each PDF in public/books/.
Saves to public/thumbnails/ with same filename but .jpg extension.
Requires: pip install pymupdf
"""
import os
import sys

try:
    import fitz  # PyMuPDF
except ImportError:
    print("Install PyMuPDF: pip install pymupdf")
    sys.exit(1)

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
BOOKS_DIR = os.path.join(SCRIPT_DIR, "public", "books")
THUMB_DIR = os.path.join(SCRIPT_DIR, "public", "thumbnails")
# Thumbnail max width in pixels
THUMB_WIDTH = 400

def main():
    if not os.path.isdir(BOOKS_DIR):
        print("Books folder not found:", BOOKS_DIR)
        sys.exit(1)
    os.makedirs(THUMB_DIR, exist_ok=True)
    pdfs = [f for f in os.listdir(BOOKS_DIR) if f.lower().endswith(".pdf")]
    if not pdfs:
        print("No PDFs in", BOOKS_DIR)
        sys.exit(1)
    print("Generating thumbnails for", len(pdfs), "PDFs...")
    ok = 0
    fail = 0
    for i, name in enumerate(sorted(pdfs), 1):
        src = os.path.join(BOOKS_DIR, name)
        base = os.path.splitext(name)[0]
        dst = os.path.join(THUMB_DIR, base + ".jpg")
        try:
            doc = fitz.open(src)
            if len(doc) == 0:
                doc.close()
                fail += 1
                continue
            page = doc[0]
            mat = fitz.Matrix(THUMB_WIDTH / page.rect.width, THUMB_WIDTH / page.rect.width)
            pix = page.get_pixmap(matrix=mat, alpha=False)
            pix.save(dst)
            doc.close()
            ok += 1
            print("[%d/%d] OK" % (i, len(pdfs)))
        except Exception as e:
            print("[%d/%d] FAIL: %s" % (i, len(pdfs), str(e)[:80]))
            fail += 1
    print("Done. OK:", ok, "Failed:", fail)

if __name__ == "__main__":
    main()
