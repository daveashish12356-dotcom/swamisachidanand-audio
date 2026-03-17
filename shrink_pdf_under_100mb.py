#!/usr/bin/env python3
# Shrink PDF in public/books to under 100 MB for GitHub (render pages at lower DPI).
import os
import sys

REPO = os.path.dirname(os.path.abspath(__file__))
BOOKS = os.path.join(REPO, "public", "books")
PDF_NAME = "ચાણક્યની રાજનીતિ.pdf"

def main():
    src = os.path.join(BOOKS, PDF_NAME)
    if not os.path.isfile(src):
        print("Not found:", src)
        sys.exit(1)
    size_mb = os.path.getsize(src) / (1024 * 1024)
    if size_mb < 100:
        print("Already under 100 MB:", round(size_mb, 2))
        return
    tmp = os.path.join(BOOKS, "chanakya_shrink_tmp.pdf")
    try:
        import fitz
        doc = fitz.open(src)
        new_doc = fitz.open()
        # 96 DPI for smaller size (was likely 150-300)
        dpi = 96
        for i in range(len(doc)):
            page = doc[i]
            mat = fitz.Matrix(dpi / 72, dpi / 72)
            pix = page.get_pixmap(matrix=mat, alpha=False)
            img_doc = fitz.open()
            img_page = img_doc.new_page(width=pix.width, height=pix.height)
            img_page.insert_image(img_page.rect, pixmap=pix)
            new_doc.insert_pdf(img_doc)
            img_doc.close()
        doc.close()
        new_doc.save(tmp, garbage=4, deflate=True, clean=True)
        new_doc.close()
        new_mb = os.path.getsize(tmp) / (1024 * 1024)
        if new_mb < 100:
            os.replace(tmp, src)
            print("Shrunk: {} MB -> {} MB".format(round(size_mb, 2), round(new_mb, 2)))
        else:
            os.remove(tmp)
            print("Still too large:", round(new_mb, 2), "MB. Try lower DPI.")
            sys.exit(1)
    except Exception as e:
        if os.path.isfile(tmp):
            os.remove(tmp)
        print("Error:", e)
        sys.exit(1)

if __name__ == "__main__":
    main()
