#!/usr/bin/env python3
"""
Generate ગીતાજીનું ચિતન thumbnail: public/thumbnails/geetaji_chintan.jpg
For server: copy this file to swamisachidanand-audio repo gh-pages/thumbnails/geetaji_chintan.jpg
"""
import os

ROOT = os.path.dirname(os.path.abspath(__file__))
THUMB_DIR = os.path.join(ROOT, "public", "thumbnails")
OUT_ASCII = os.path.join(THUMB_DIR, "geetaji_chintan.jpg")
W, H = 400, 560  # portrait like other chintan thumbs

def main():
    os.makedirs(THUMB_DIR, exist_ok=True)
    try:
        from PIL import Image, ImageDraw, ImageFont
    except ImportError:
        print("pip install Pillow")
        print("Or copy an existing thumbnail to public/thumbnails/geetaji_chintan.jpg")
        raise SystemExit(1)

    # Saffron/orange gradient style
    img = Image.new("RGB", (W, H), (0, 0, 0))
    draw = ImageDraw.Draw(img)
    for y in range(H):
        r = int(255 * (1 - 0.6 * y / H))
        g = int(140 * (1 - 0.4 * y / H))
        b = int(50 * (1 - 0.5 * y / H))
        draw.line([(0, y), (W, y)], fill=(r, g, b))

    text = "ગીતાજીનું ચિતન"
    try:
        font = ImageFont.truetype("C:\\Windows\\Fonts\\shruti.ttf", 52)
    except Exception:
        try:
            font = ImageFont.truetype("C:\\Windows\\Fonts\\Nirmala.ttf", 48)
        except Exception:
            font = ImageFont.load_default()

    bbox = draw.textbbox((0, 0), text, font=font)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    x = (W - tw) // 2
    y = (H - th) // 2 - 20
    draw.text((x + 1, y + 1), text, fill=(0, 0, 0), font=font)
    draw.text((x, y), text, fill=(255, 255, 220), font=font)

    img.save(OUT_ASCII, "JPEG", quality=88)
    print("Created:", OUT_ASCII)
    print("For gh-pages: copy to swamisachidanand-audio repo thumbnails/geetaji_chintan.jpg and push gh-pages branch.")

if __name__ == "__main__":
    main()
