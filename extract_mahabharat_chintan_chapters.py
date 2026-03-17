import json
from pathlib import Path

import fitz  # PyMuPDF

ROOT = Path(r"f:\ss")
PDF_PATH = ROOT / "public" / "books" / "mahabharat_chintan.pdf"
CHAPTERS_JSON = ROOT / "app" / "src" / "main" / "assets" / "book_chapters.json"


def extract_chapters() -> list[dict]:
    """
    Very simple rule:
    - On each page, take the FIRST line that looks like '12. શકુંતલાનો જન્મ'.
    - Completely skip early intro pages (<= 9) where headings are not chapters.
    """
    if not PDF_PATH.is_file():
        raise SystemExit(f"PDF not found: {PDF_PATH}")

    doc = fitz.open(PDF_PATH.as_posix())
    chapters: list[dict] = []

    for page_index in range(len(doc)):
        page = doc[page_index]
        page_no = page_index + 1
        text = page.get_text("text")
        if not text:
            continue

        first_heading = None
        for raw_line in text.splitlines():
            line = raw_line.strip()
            if not line:
                continue
            if len(line) > 3 and line[0].isdigit() and line[1] == "." and line[2] == " ":
                first_heading = line
                break

        if not first_heading:
            continue

        # skip philosophy bullets at start
        if page_no <= 9:
            continue

        if any(c["t"] == first_heading for c in chapters):
            continue
        chapters.append({"t": first_heading, "p": page_index})

    doc.close()

    if not chapters:
        chapters.append({"t": "અધ્યાય ૧", "p": 0})
    return chapters


def main():
    chapters = extract_chapters()
    print(f"Found {len(chapters)} chapter headings")

    data: dict = {}
    if CHAPTERS_JSON.is_file():
        data = json.loads(CHAPTERS_JSON.read_text(encoding="utf-8"))

    key = "મહાભારતનું ચિંતન.pdf"
    data[key] = chapters

    CHAPTERS_JSON.write_text(
        json.dumps(data, ensure_ascii=False, indent=None, separators=(",", ":")),
        encoding="utf-8",
    )
    print(f"Wrote {len(chapters)} chapters into {CHAPTERS_JSON} for key {key!r}")


if __name__ == "__main__":
    main()

