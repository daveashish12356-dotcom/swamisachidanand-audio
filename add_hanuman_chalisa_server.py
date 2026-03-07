#!/usr/bin/env python3
# Add શ્રી હનુમાન ચાલીસા to server: copy PDF to public/books, generate thumbnail in public/thumbnails.
# Run from repo root (f:\ss). Then push repo and release so app shows the book from server.
# App me kuch nahi – sirf server (public/ + books_server_list.json).

import os
import shutil
import sys

REPO_ROOT = os.path.dirname(os.path.abspath(__file__))
DESKTOP = os.path.join(os.path.expanduser("~"), "Desktop")
PUBLIC_BOOKS = os.path.join(REPO_ROOT, "public", "books")
PUBLIC_THUMB = os.path.join(REPO_ROOT, "public", "thumbnails")

PDF_NAME = "શ્રી હનુમાન ચાલીસા.pdf"
THUMB_SOURCE_PDF = "શ્રી હનુમાન ચાલીસા 1.pdf"  # optional: use this PDF's first page as thumbnail
THUMB_JPG = "શ્રી હનુમાન ચાલીસા.jpg"

def main():
    os.makedirs(PUBLIC_BOOKS, exist_ok=True)
    os.makedirs(PUBLIC_THUMB, exist_ok=True)

    # 1) Copy main PDF from Desktop to public/books/
    pdf_src = os.path.join(DESKTOP, PDF_NAME)
    pdf_dst = os.path.join(PUBLIC_BOOKS, PDF_NAME)
    if not os.path.isfile(pdf_src):
        print("PDF not found:", pdf_src)
        print("Put", PDF_NAME, "on Desktop and run again.")
        sys.exit(1)
    shutil.copy2(pdf_src, pdf_dst)
    print("Copied PDF -> public/books/ (Hanuman Chalisa)")

    # 2) Thumbnail: try "શ્રી હનુમાન ચાલીસા 1.pdf" first page, else main PDF first page
    thumb_dst = os.path.join(PUBLIC_THUMB, THUMB_JPG)
    pdf_for_thumb = os.path.join(DESKTOP, THUMB_SOURCE_PDF)
    if not os.path.isfile(pdf_for_thumb):
        pdf_for_thumb = pdf_dst
    try:
        import fitz
        doc = fitz.open(pdf_for_thumb)
        if len(doc) > 0:
            page = doc[0]
            w = min(400, page.rect.width)
            mat = fitz.Matrix(w / page.rect.width, w / page.rect.width)
            pix = page.get_pixmap(matrix=mat, alpha=False)
            pix.save(thumb_dst)
            print("Generated thumbnail -> public/thumbnails/ (Hanuman Chalisa.jpg)")
        doc.close()
    except ImportError:
        print("PyMuPDF not installed. Run: pip install pymupdf")
        print("Then run: python generate_thumbnails.py  (to create thumbnail from PDF)")
    except Exception as e:
        print("Thumbnail error:", e)
        print("Run generate_thumbnails.py after, or add thumbnail JPG manually to public/thumbnails/")

    print("Done. Book already added to public/books_server_list.json.")
    print("Next: git add public/books public/thumbnails public/books_server_list.json")
    print("      git commit -m 'Add Sri Hanuman Chalisa book'; git push (deploy runs on push).")

if __name__ == "__main__":
    main()
