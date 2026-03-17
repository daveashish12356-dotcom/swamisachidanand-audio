import os

try:
    import fitz  # PyMuPDF
except ImportError:
    print("PyMuPDF (fitz) not installed")
    raise SystemExit(1)


def main() -> None:
    # Source PDF provided by user (only cover page)
    pdf_path = r"C:\Users\davea\Desktop\ગીતાજ્ઞાન ચિંતન.pdf"

    if not os.path.isfile(pdf_path):
        print("Source PDF not found at:", pdf_path)
        return

    root = os.path.dirname(os.path.abspath(__file__))
    thumb_dir = os.path.join(root, "public", "thumbnails")
    out_file = os.path.join(thumb_dir, "geetaji_chintan.jpg")

    os.makedirs(thumb_dir, exist_ok=True)

    doc = fitz.open(pdf_path)
    if len(doc) == 0:
        print("PDF has no pages")
        return

    page = doc[0]

    # Scale so width is about 400px (similar to other thumbs)
    try:
        scale = 400.0 / float(page.rect.width or 1.0)
    except Exception:
        scale = 1.0

    mat = fitz.Matrix(scale, scale)
    pix = page.get_pixmap(matrix=mat, alpha=False)

    pix.save(out_file, "jpeg")
    doc.close()

    try:
        size = os.path.getsize(out_file)
    except OSError:
        size = -1

    print("Created geetaji_chintan thumbnail at:", out_file)
    print("Thumbnail size (bytes):", size)

    # Copy to ss-ghp (server) and app drawable
    import shutil
    root_path = os.path.dirname(os.path.abspath(__file__))
    ghp_thumb = os.path.join(root_path, "..", "ss-ghp", "thumbnails", "geetaji_chintan.jpg")
    app_thumb = os.path.join(root_path, "app", "src", "main", "res", "drawable", "geetaji_chintan_thumb.jpg")
    ghp_dir = os.path.dirname(ghp_thumb)
    app_dir = os.path.dirname(app_thumb)
    if os.path.isdir(ghp_dir):
        shutil.copy2(out_file, ghp_thumb)
        print("Copied to ss-ghp:", ghp_thumb)
    if os.path.isdir(app_dir):
        shutil.copy2(out_file, app_thumb)
        print("Copied to app drawable:", app_thumb)


if __name__ == "__main__":
    main()

