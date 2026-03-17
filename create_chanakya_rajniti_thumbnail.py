# -*- coding: utf-8 -*-
"""Create simple thumbnail for ચાણક્યની રાજનીતિ and update audio_list.json"""
import json
import os
import sys

from PIL import Image, ImageDraw, ImageFont

THUMBNAIL_DEST = r"f:\ss\public\thumbnails\chanakya_rajniti.jpg"
AUDIO_LIST_JSON = r"f:\ss\public\audio_list.json"
BASE_URL = "https://daveashish12356-dotcom.github.io/swamisachidanand-audio/thumbnails/chanakya_rajniti.jpg"


def create_thumbnail():
    """Create a basic thumbnail image without needing poppler/pdf2image"""
    os.makedirs(os.path.dirname(THUMBNAIL_DEST), exist_ok=True)

    width, height = 600, 900
    bg_color = (240, 248, 255)  # light background
    accent_color = (34, 197, 94)  # Swamiji green
    text_color = (20, 20, 20)

    img = Image.new("RGB", (width, height), bg_color)
    draw = ImageDraw.Draw(img)

    # Simple border
    border = 12
    draw.rectangle(
        (border, border, width - border, height - border),
        outline=accent_color,
        width=8,
    )

    title = "ચાણક્યની\nરાજનીતિ"

    # Try a nicer font if available, else default
    try:
        # This path may not exist; Pillow will fall back if it fails
        font = ImageFont.truetype("arial.ttf", 64)
    except Exception:
        font = ImageFont.load_default()

    # Rough centering of text (fallback without multiline_textsize)
    x = width * 0.18
    y = height * 0.35

    draw.multiline_text(
        (x, y),
        title,
        font=font,
        fill=text_color,
        align="center",
        spacing=10,
    )

    img.save(THUMBNAIL_DEST, "JPEG", quality=90)
    print(f"Thumbnail created: {THUMBNAIL_DEST}")
    return True


def update_json():
    """Add thumbnailUrl to chanakya_rajniti book"""
    if not os.path.isfile(AUDIO_LIST_JSON):
        print("Error: audio_list.json not found")
        return False

    with open(AUDIO_LIST_JSON, "r", encoding="utf-8") as f:
        content = f.read()
        if content.startswith("\ufeff"):
            content = content[1:]
        data = json.loads(content)

    updated = False
    for book in data.get("books", []):
        if book.get("id") == "chanakya_rajniti":
            book["thumbnailUrl"] = BASE_URL
            updated = True
            print("Updated thumbnailUrl in JSON")
            break

    if not updated:
        print("Warning: chanakya_rajniti book not found in JSON")
        return False

    with open(AUDIO_LIST_JSON, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    return True


if __name__ == "__main__":
    print("Creating thumbnail and updating JSON...\n")

    if create_thumbnail():
        if update_json():
            print("\nSuccess! Thumbnail created and JSON updated.")
            print(f"\nThumbnail URL: {BASE_URL}")
            print("\nNext: Commit + push to GitHub Pages repo.")
        else:
            sys.exit(1)
    else:
        print("\nPlease create thumbnail manually or install pillow")
        sys.exit(1)

