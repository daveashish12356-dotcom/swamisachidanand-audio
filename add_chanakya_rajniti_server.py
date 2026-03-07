#!/usr/bin/env python3
# Add ચાણક્યની રાજનીતિ to server: copy PDF, generate thumbnail, update list.
# Remove ramayan_chintan (keep only Gujarati-named રામાયણનું ચિંતન).
# Run from f:\ss.

import os
import shutil
import json

REPO = os.path.dirname(os.path.abspath(__file__))
DESKTOP_PDF = r"C:\Users\davea\Desktop\ચાણક્યની રાજનીતિ.pdf"
PDF_NAME = "ચાણક્યની રાજનીતિ.pdf"
JPG_NAME = "ચાણક્યની રાજનીતિ.jpg"

PUBLIC_BOOKS = os.path.join(REPO, "public", "books")
PUBLIC_THUMB = os.path.join(REPO, "public", "thumbnails")
LIST_PUBLIC = os.path.join(REPO, "public", "books_server_list.json")
LIST_APP = os.path.join(REPO, "app", "src", "main", "assets", "books_server_list.json")

def main():
    os.makedirs(PUBLIC_BOOKS, exist_ok=True)
    os.makedirs(PUBLIC_THUMB, exist_ok=True)

    # 1) Copy PDF from Desktop to public/books
    if not os.path.isfile(DESKTOP_PDF):
        print("PDF not found:", DESKTOP_PDF)
        return 1
    pdf_dst = os.path.join(PUBLIC_BOOKS, PDF_NAME)
    shutil.copy2(DESKTOP_PDF, pdf_dst)
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

    # 3) Remove ramayan_chintan.pdf and .jpg (keep only Gujarati-named Ramayan)
    for name, folder in [("ramayan_chintan.pdf", PUBLIC_BOOKS), ("ramayan_chintan.jpg", PUBLIC_THUMB)]:
        path = os.path.join(folder, name)
        if os.path.isfile(path):
            os.remove(path)
            print("Removed:", path)

    # 4) Add ચાણક્યની રાજનીતિ.pdf to list (after ગીતાજ્ઞાન ચિંતન.pdf)
    for list_path in [LIST_PUBLIC, LIST_APP]:
        if not os.path.isfile(list_path):
            continue
        with open(list_path, "r", encoding="utf-8") as f:
            data = json.load(f)
        names = data.get("fileNames", [])
        if PDF_NAME in names:
            print("Already in list:", list_path)
            continue
        # Insert after ગીતાજ્ઞાન ચિંતન.pdf
        idx = -1
        for i, n in enumerate(names):
            if n == "ગીતાજ્ઞાન ચિંતન.pdf":
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
