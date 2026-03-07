#!/usr/bin/env python3
# Add સરદારસાહેબ મારી નજરે to server: copy PDF to public/books, thumbnail to public/thumbnails.
# Run from repo root. PDF path: C:\Users\davea\Desktop\સરદારસાહેબ મારી નજરે.pdf

import os
import shutil
import sys

REPO_ROOT = os.path.dirname(os.path.abspath(__file__))
DESKTOP = os.path.join(os.path.expanduser("~"), "Desktop")
PUBLIC_BOOKS = os.path.join(REPO_ROOT, "public", "books")
PUBLIC_THUMB = os.path.join(REPO_ROOT, "public", "thumbnails")

PDF_NAME = "સરદારસાહેબ મારી નજરે.pdf"
THUMB_JPG = "સરદારસાહેબ મારી નજરે.jpg"

def main():
    os.makedirs(PUBLIC_BOOKS, exist_ok=True)
    os.makedirs(PUBLIC_THUMB, exist_ok=True)

    # 1) Copy PDF from Desktop to public/books/
    pdf_src = os.path.join(DESKTOP, PDF_NAME)
    pdf_dst = os.path.join(PUBLIC_BOOKS, PDF_NAME)
    if not os.path.isfile(pdf_src):
        # Try alternate name (double dot)
        pdf_src_alt = os.path.join(DESKTOP, "સરદારસાહેબ મારી નજરે..pdf")
        if os.path.isfile(pdf_src_alt):
            pdf_src = pdf_src_alt
            shutil.copy2(pdf_src, pdf_dst)
        else:
            print("PDF not found:", pdf_src)
            print("Put", PDF_NAME, "on Desktop and run again.")
            sys.exit(1)
    else:
        shutil.copy2(pdf_src, pdf_dst)
    print("Copied PDF -> public/books/")

    # 2) Thumbnail from first page of PDF (PyMuPDF)
    thumb_dst = os.path.join(PUBLIC_THUMB, THUMB_JPG)
    try:
        import fitz
        doc = fitz.open(pdf_dst)
        if len(doc) > 0:
            page = doc[0]
            w = min(400, page.rect.width)
            mat = fitz.Matrix(w / page.rect.width, w / page.rect.width)
            pix = page.get_pixmap(matrix=mat, alpha=False)
            pix.save(thumb_dst)
            print("Generated thumbnail -> public/thumbnails/")
        doc.close()
    except ImportError:
        print("PyMuPDF not installed. pip install pymupdf then re-run for thumbnail.")
    except Exception as e:
        print("Thumbnail error:", e)

    print("Done. Add entry to books_server_list.json if not already added.")

if __name__ == "__main__":
    main()
