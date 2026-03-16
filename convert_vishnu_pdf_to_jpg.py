import fitz  # PyMuPDF
from pathlib import Path


def main():
    # Source PDF on your desktop
    pdf_path = Path(r"C:\Users\davea\Desktop\વિષ્ણુસહસ્રનામ.pdf")

    # Output JPG in this repo (change if you want a different folder)
    out_path = Path(r"f:\ss\public\vishnusahasranam_part1.jpg")

    if not pdf_path.exists():
        raise SystemExit(f"PDF not found: {pdf_path}")

    out_path.parent.mkdir(parents=True, exist_ok=True)

    doc = fitz.open(pdf_path)
    try:
        page = doc.load_page(0)  # first page
        pix = page.get_pixmap(dpi=200)
        pix.save(out_path)
    finally:
        doc.close()

    print(f"Saved JPG: {out_path}")


if __name__ == "__main__":
    main()

