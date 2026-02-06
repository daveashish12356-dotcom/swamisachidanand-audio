#!/usr/bin/env python3
"""
Convert all scanned PDFs (photos) to PDFs with OCR text layer (Gujarati).
Run this on PC first, then copy converted PDFs to app/src/main/assets/ to replace originals.
Requires: pip install ocrmypdf, and Tesseract with Gujarati (guj) installed.
"""
import os
import subprocess
import sys

ASSETS_DIR = os.path.join(os.path.dirname(__file__), "app", "src", "main", "assets")
OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "converted_pdfs")

def main():
    if not os.path.isdir(ASSETS_DIR):
        print("Assets folder not found:", ASSETS_DIR)
        sys.exit(1)
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    pdfs = [f for f in os.listdir(ASSETS_DIR) if f.lower().endswith(".pdf")]
    if not pdfs:
        print("No PDFs found in", ASSETS_DIR)
        sys.exit(1)
    print("Found", len(pdfs), "PDFs. Converting with OCR (Gujarati)...")
    ok = 0
    fail = 0
    for i, name in enumerate(sorted(pdfs), 1):
        src = os.path.join(ASSETS_DIR, name)
        dst = os.path.join(OUTPUT_DIR, name)
        print("[%d/%d] %s" % (i, len(pdfs), name))
        try:
            cmd = [sys.executable, "-m", "ocrmypdf", "-l", "guj", "--skip-text", src, dst]
            subprocess.run(cmd, check=True, capture_output=True, timeout=600)
            ok += 1
        except subprocess.CalledProcessError as e:
            print("  FAIL:", e.stderr.decode(errors="ignore") if e.stderr else e)
            fail += 1
        except FileNotFoundError:
            print("  ocrmypdf not found. Install: pip install ocrmypdf")
            print("  Also install Tesseract with Gujarati (guj.traineddata) in Tesseract tessdata folder.")
            sys.exit(1)
        except subprocess.TimeoutExpired:
            print("  Timeout (10 min). Skipped.")
            fail += 1
    print("Done. OK:", ok, "Failed:", fail)
    print("Copy files from", OUTPUT_DIR, "to", ASSETS_DIR, "to replace originals (backup assets first).")

if __name__ == "__main__":
    main()
