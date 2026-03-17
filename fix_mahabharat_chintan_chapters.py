import json
from pathlib import Path

import fitz  # PyMuPDF

ROOT = Path(r"f:\ss")
PDF_PATH = ROOT / "public" / "books" / "mahabharat_chintan.pdf"
CHAPTERS_JSON = ROOT / "app" / "src" / "main" / "assets" / "book_chapters.json"


def generate_default_chapters(page_count: int):
    """Match PdfViewerActivity.createDefaultChaptersList logic for chapter pages."""
    num_chapters = min(50, max(5, (page_count + 4) // 5))
    chapter_size = max(1, page_count // num_chapters)
    chapters = []
    guj_digits = ["૦", "૧", "૨", "૩", "૪", "૫", "૬", "૭", "૮", "૯"]

    def to_guj(n: int) -> str:
        if n <= 0 or n > 999:
            return str(n)
        if n < 10:
            return guj_digits[n]
        out = []
        num = n
        div = 100
        started = False
        while div >= 1:
            d = num // div
            num = num % div
            div //= 10
            if d > 0 or started:
                out.append(guj_digits[d])
                started = True
        return "".join(out) if out else guj_digits[0]

    for i in range(num_chapters):
        page_num = i * chapter_size
        if page_num >= page_count:
            break
        title = f"અધ્યાય {to_guj(i + 1)}"
        chapters.append({"t": title, "p": page_num})
    if not chapters:
        chapters.append({"t": "અધ્યાય ૧", "p": 0})
    return chapters


def main():
    if not PDF_PATH.is_file():
        raise SystemExit(f"PDF not found: {PDF_PATH}")

    doc = fitz.open(PDF_PATH.as_posix())
    page_count = len(doc)
    doc.close()

    chapters = generate_default_chapters(page_count)

    data = {}
    if CHAPTERS_JSON.is_file():
        data = json.loads(CHAPTERS_JSON.read_text(encoding="utf-8"))

    # Key exactly as other Gujarati books use
    key = "મહાભારતનું ચિંતન.pdf"
    data[key] = chapters

    CHAPTERS_JSON.write_text(
        json.dumps(data, ensure_ascii=False, indent=None, separators=(",", ":")),
        encoding="utf-8",
    )
    print(f"Wrote {len(chapters)} chapters for {key} (pages={page_count})")


if __name__ == "__main__":
    main()

