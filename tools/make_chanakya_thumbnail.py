import os

import fitz  # PyMuPDF


def make_thumbnail():
    # Input PDF on Desktop
    pdf_path = r"C:\Users\davea\Desktop\ચાણક્યની રાજનીતિ.pdf"

    # Output JPG inside repo, used by server/app
    output_jpg = r"f:\ss\audio-repo\public\thumbnails\ચાણક્યની રાજનીતિ.jpg"

    if not os.path.isfile(pdf_path):
        raise FileNotFoundError(f"PDF not found at {pdf_path!r}")

    os.makedirs(os.path.dirname(output_jpg), exist_ok=True)

    # Open PDF and render first page as image
    doc = fitz.open(pdf_path)
    if doc.page_count == 0:
        raise RuntimeError("PDF has no pages")

    page = doc.load_page(0)
    # Zoom a bit for better quality
    zoom = 2.0
    mat = fitz.Matrix(zoom, zoom)
    pix = page.get_pixmap(matrix=mat, alpha=False)

    pix.save(output_jpg, output="jpeg")
    doc.close()

    # Avoid Unicode print issues on Windows console; simple ASCII message is enough
    print("Saved thumbnail JPG.")


if __name__ == "__main__":
    make_thumbnail()

