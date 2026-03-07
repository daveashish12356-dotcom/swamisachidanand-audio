#!/usr/bin/env python3
# Add ચાણક્યની વ્યવહારનીતિ to server: copy PDF, generate thumbnail, update list.
# Run from f:\ss. PDF: F:\52\pdf\ or Desktop.

import os
import shutil
import json

REPO = os.path.dirname(os.path.abspath(__file__))
PDF_NAME = "ચાણક્યની વ્યવહારનીતિ.pdf"
JPG_NAME = "ચાણક્યની વ્યવહારનીતિ.jpg"
PDF_SOURCES = [
    r"F:\52\pdf\ચાણક્યની વ્યવહારનીતિ.pdf",
    r"C:\Users\davea\Desktop\ચાણક્યની વ્યવહારનીતિ.pdf",
]

PUBLIC_BOOKS = os.path.join(REPO, "public", "books")
PUBLIC_THUMB = os.path.join(REPO, "public", "thumbnails")
LIST_PUBLIC = os.path.join(REPO, "public", "books_server_list.json")
LIST_APP = os.path.join(REPO, "app", "src", "main", "assets", "books_server_list.json")

def main():
    os.makedirs(PUBLIC_BOOKS, exist_ok=True)
    os.makedirs(PUBLIC_THUMB, exist_ok=True)

    # 1) Copy PDF from F:\52\pdf or Desktop to public/books
    pdf_src = None
    for p in PDF_SOURCES:
        if os.path.isfile(p):
            pdf_src = p
            break
    if not pdf_src:
        print("PDF not found. Tried:", PDF_SOURCES)
        return 1
    pdf_dst = os.path.join(PUBLIC_BOOKS, PDF_NAME)
    shutil.copy2(pdf_src, pdf_dst)
    print("Copied PDF -> public/books/" + PDF_NAME)

    # 2) Generate thumbnail from PDF first page
    try:
        import fitz
        doc = fitz.open(pdf_dst)
        if len(doc) > 0:
            page = doc[0]
            w = min(400, page.rect.width)
            mat = fitz.Matrix(w / page.rect.width, w / page.rect.width)
            pix = page.get_pixmap(matrix=mat, alpha=False)
            thumb_dst = os.path.join(PUBLIC_THUMB, JPG_NAME)
            pix.save(thumb_dst)
            print("Generated thumbnail -> public/thumbnails/" + JPG_NAME)
        doc.close()
    except Exception as e:
        print("Thumbnail error:", e)
        return 1

    # 3) Add to list after ચાણક્યની રાજનીતિ.pdf
    for list_path in [LIST_PUBLIC, LIST_APP]:
        if not os.path.isfile(list_path):
            continue
        with open(list_path, "r", encoding="utf-8") as f:
            data = json.load(f)
        names = data.get("fileNames", [])
        if PDF_NAME in names:
            print("Already in list:", list_path)
            continue
        idx = -1
        for i, n in enumerate(names):
            if n == "ચાણક્યની રાજનીતિ.pdf":
                idx = i + 1
                break
        if idx < 0:
            names.append(PDF_NAME)
        else:
            names.insert(idx, PDF_NAME)
        data["fileNames"] = names
        with open(list_path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=None)
        print("Updated list:", list_path)
    print("Done.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
