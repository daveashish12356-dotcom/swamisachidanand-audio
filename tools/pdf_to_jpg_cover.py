import sys
from pathlib import Path

import fitz  # PyMuPDF


def pdf_first_page_to_jpg(src: Path, dst: Path, dpi: int = 200) -> None:
    doc = fitz.open(src)
    try:
        page = doc[0]
        pix = page.get_pixmap(dpi=dpi)
        dst.parent.mkdir(parents=True, exist_ok=True)
        pix.save(dst.as_posix())
    finally:
        doc.close()


def main(argv: list[str]) -> None:
    if len(argv) != 3:
        print("Usage: pdf_to_jpg_cover.py <src_pdf> <dst_jpg>", file=sys.stderr)
        sys.exit(1)
    src = Path(argv[1])
    dst = Path(argv[2])
    pdf_first_page_to_jpg(src, dst)


if __name__ == "__main__":
    main(sys.argv)

