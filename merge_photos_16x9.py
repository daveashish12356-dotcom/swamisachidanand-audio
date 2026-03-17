#!/usr/bin/env python3
"""
Merge two photos into one image with 16:9 aspect ratio.
Usage: python merge_photos_16x9.py [image1.png] [image2.png] [output.png]
Or place photo1.png and photo2.png in assets/ and run without arguments.
"""
import sys
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    print("Pillow required. Run: pip install Pillow")
    sys.exit(1)

# Default paths (relative to script)
SCRIPT_DIR = Path(__file__).resolve().parent
ASSETS = SCRIPT_DIR / "assets"
DEFAULT_IMG1 = ASSETS / "photo1.png"
DEFAULT_IMG2 = ASSETS / "photo2.png"
DEFAULT_OUT = SCRIPT_DIR / "merged_16x9.png"

# 16:9 dimensions (e.g. 1920x1080)
OUTPUT_WIDTH = 1920
OUTPUT_HEIGHT = 1080


def load_and_fit(img_path: Path, target_w: int, target_h: int) -> Image.Image:
    """Load image and scale to cover target size, then crop to exact size."""
    img = Image.open(img_path).convert("RGB")
    iw, ih = img.size
    scale = max(target_w / iw, target_h / ih)
    new_w = int(iw * scale)
    new_h = int(ih * scale)
    img = img.resize((new_w, new_h), Image.Resampling.LANCZOS)
    # Center crop
    left = (new_w - target_w) // 2
    top = (new_h - target_h) // 2
    return img.crop((left, top, left + target_w, top + target_h))


def merge_16x9(path1: Path, path2: Path, out_path: Path) -> None:
    half_w = OUTPUT_WIDTH // 2
    # Left half from image1, right half from image2
    left = load_and_fit(path1, half_w, OUTPUT_HEIGHT)
    right = load_and_fit(path2, half_w, OUTPUT_HEIGHT)
    merged = Image.new("RGB", (OUTPUT_WIDTH, OUTPUT_HEIGHT))
    merged.paste(left, (0, 0))
    merged.paste(right, (half_w, 0))
    merged.save(out_path, quality=95)
    print(f"Saved: {out_path} ({OUTPUT_WIDTH}x{OUTPUT_HEIGHT}, 16:9)")


def main():
    if len(sys.argv) >= 4:
        path1 = Path(sys.argv[1])
        path2 = Path(sys.argv[2])
        out_path = Path(sys.argv[3])
    elif len(sys.argv) == 3:
        path1 = Path(sys.argv[1])
        path2 = Path(sys.argv[2])
        out_path = DEFAULT_OUT
    else:
        path1 = DEFAULT_IMG1
        path2 = DEFAULT_IMG2
        out_path = DEFAULT_OUT

    if not path1.is_file():
        print(f"Image 1 not found: {path1}")
        print("Either pass two image paths: python merge_photos_16x9.py img1.png img2.png [out.png]")
        print("Or put photo1.png and photo2.png in the assets/ folder.")
        sys.exit(1)
    if not path2.is_file():
        print(f"Image 2 not found: {path2}")
        sys.exit(1)

    merge_16x9(path1, path2, out_path)


if __name__ == "__main__":
    main()
