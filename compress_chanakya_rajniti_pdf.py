#!/usr/bin/env python3
# Compress PDF to smaller size. Input: Desktop\ચાણક્યની રાજનીતિ.pdf
# Output: same path, smaller file (overwrites after success).

import os
import sys

DESKTOP = os.path.join(os.path.expanduser("~"), "Desktop")
PDF_NAME = "ચાણક્યની રાજનીતિ.pdf"
PDF_ALT = "ચાણક્યની રાજનીતિ..pdf"

def main():
    pdf_path = os.path.join(DESKTOP, PDF_NAME)
    if not os.path.isfile(pdf_path):
        pdf_path = os.path.join(DESKTOP, PDF_ALT)
    if not os.path.isfile(pdf_path):
        print("PDF not found on Desktop")
        sys.exit(1)

    size_before = os.path.getsize(pdf_path)
    tmp_path = pdf_path + ".tmp"

    try:
        import fitz
        doc = fitz.open(pdf_path)
        # Compress: garbage=4 (max), deflate=True, clean=True
        doc.save(tmp_path, garbage=4, deflate=True, clean=True)
        doc.close()
        size_after = os.path.getsize(tmp_path)
        os.replace(tmp_path, pdf_path)
        print("Done. Size: {} MB -> {} MB".format(
            round(size_before / (1024*1024), 2),
            round(size_after / (1024*1024), 2)))
    except Exception as e:
        if os.path.isfile(tmp_path):
            try:
                os.remove(tmp_path)
            except Exception:
                pass
        print("Error:", e)
        sys.exit(1)

if __name__ == "__main__":
    main()
