import json
import re
from pathlib import Path

import fitz  # PyMuPDF

ROOT = Path(r"f:\ss")
# Current repo stores this book with its original Gujarati filename.
PDF_PATH = ROOT / "public" / "books" / "'મહાભારત'નું ચિંતન.pdf"
CHAPTERS_JSON = ROOT / "app" / "src" / "main" / "assets" / "book_chapters.json"


def extract_numbered_headings():
    """
    Extract monotonic 1..108 headings from the PDF.

    This mirrors the python -c logic that printed the list you liked:
      1. ... (page 8)
      ...
      108. ... (page 232)
    """
    if not PDF_PATH.is_file():
        raise SystemExit(f"PDF not found: {PDF_PATH}")

    doc = fitz.open(PDF_PATH.as_posix())
    pat = re.compile(r"^(\d+)[\.\)]\s+(.+)")
    chapters = {}
    current = 0
    for i in range(len(doc)):
        text = doc[i].get_text("text") or ""
        for raw in text.splitlines():
            line = raw.strip()
            if not line:
                continue
            m = pat.match(line)
            if not m:
                continue
            num = int(m.group(1))
            if num <= current or num > 108:
                continue
            current = num
            chapters[num] = (m.group(0), i + 1)  # full title, 1-based page
    doc.close()
    return chapters


def main():
    numbered = extract_numbered_headings()
    print("Found numbered headings:", len(numbered))

    chapters = []
    # 1) Book name (approx page 2)
    chapters.append({"t": "મહાભારતનું ચિંતન – પુસ્તક નામ", "p": 2})
    # 2) Arpan (page 4)
    chapters.append({"t": "અર્પણ", "p": 4})
    # 3) Bhumika (page 5)
    chapters.append({"t": "ભૂમિકા", "p": 5})

    # 4) All 1..108 extracted headings in order
    for n in sorted(numbered):
        title, page = numbered[n]
        chapters.append({"t": title, "p": page - 1})  # convert to 0-based index

    print("Total chapters to write:", len(chapters))

    data = {}
    if CHAPTERS_JSON.is_file():
        data = json.loads(CHAPTERS_JSON.read_text(encoding="utf-8"))

    key = "મહાભારતનું ચિંતન.pdf"
    data[key] = chapters

    CHAPTERS_JSON.write_text(
        json.dumps(data, ensure_ascii=False, indent=None, separators=(",", ":")),
        encoding="utf-8",
    )
    print(f"Wrote {len(chapters)} chapters for {key}")


if __name__ == "__main__":
    main()

