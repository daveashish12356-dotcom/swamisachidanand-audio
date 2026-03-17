#!/usr/bin/env python3
"""
Collage-style merge: central circular portrait, warm gradient background,
multiple panels with rounded corners. 16:9 output.
Usage: python merge_photos_collage_style.py [image1] [image2] [output.png]
"""
import sys
from pathlib import Path

try:
    from PIL import Image, ImageDraw
except ImportError:
    print("Pillow required. Run: pip install Pillow")
    sys.exit(1)

SCRIPT_DIR = Path(__file__).resolve().parent
OUTPUT_WIDTH = 1920
OUTPUT_HEIGHT = 1080
# Softer warm gradient (cream to light saffron) - premium look
GRADIENT_TOP = (255, 248, 238)       # cream
GRADIENT_MID = (255, 235, 210)       # soft peach
GRADIENT_BOTTOM = (255, 218, 185)    # light saffron
CIRCLE_RADIUS = 248
CIRCLE_BORDER = 8                    # white ring around circle
PANEL_RADIUS = 32
SHADOW_OFFSET = 6


def gradient_bg(w: int, h: int) -> Image.Image:
    """Soft cream-to-saffron vertical gradient."""
    img = Image.new("RGB", (w, h))
    draw = ImageDraw.Draw(img)
    for y in range(h):
        t = y / h
        if t < 0.5:
            u = t * 2
            r = int(GRADIENT_TOP[0] * (1 - u) + GRADIENT_MID[0] * u)
            g = int(GRADIENT_TOP[1] * (1 - u) + GRADIENT_MID[1] * u)
            b = int(GRADIENT_TOP[2] * (1 - u) + GRADIENT_MID[2] * u)
        else:
            u = (t - 0.5) * 2
            r = int(GRADIENT_MID[0] * (1 - u) + GRADIENT_BOTTOM[0] * u)
            g = int(GRADIENT_MID[1] * (1 - u) + GRADIENT_BOTTOM[1] * u)
            b = int(GRADIENT_MID[2] * (1 - u) + GRADIENT_BOTTOM[2] * u)
        draw.line([(0, y), (w, y)], fill=(r, g, b))
    return img


def crop_portrait_region(im: Image.Image, size: int) -> Image.Image:
    """Crop for circle: upper-center zone (face/portrait), not random center cut."""
    im = im.convert("RGB")
    iw, ih = im.size
    # Focus on upper 60% and center 80% width - where face usually is
    x_margin = int(iw * 0.10)
    top_crop = int(ih * 0.05)
    bottom_crop = int(ih * 0.35)  # use from 5% to 65% height
    w = iw - 2 * x_margin
    h = ih - top_crop - bottom_crop
    s = min(w, h, size * 2)
    left = x_margin + (w - s) // 2
    top = top_crop
    return im.crop((left, top, left + s, top + s)).resize(
        (size * 2, size * 2), Image.Resampling.LANCZOS
    )


def fit_in_panel(im: Image.Image, pw: int, ph: int, focus_top: bool = False) -> Image.Image:
    """Scale full photo to cover panel; center crop. focus_top=True keeps more of top (faces)."""
    im = im.convert("RGB")
    iw, ih = im.size
    scale = max(pw / iw, ph / ih)
    nw, nh = int(iw * scale), int(ih * scale)
    im = im.resize((nw, nh), Image.Resampling.LANCZOS)
    x = (nw - pw) // 2
    y = (nh - ph) // 2
    if focus_top and nh > ph:
        y = 0  # keep top of photo (faces)
    out = im.crop((x, y, x + pw, y + ph))
    # Subtle warm tint so photo blends with background (not raw paste)
    warm = Image.new("RGB", (pw, ph), (255, 242, 228))
    out = Image.blend(out, warm, alpha=0.06)
    return out


def circular_mask(size: int) -> Image.Image:
    """Black/white mask for circle (white = visible)."""
    m = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(m)
    d.ellipse((0, 0, size - 1, size - 1), fill=255)
    return m


def rounded_rect_mask(w: int, h: int, r: int) -> Image.Image:
    """Mask for rounded rectangle."""
    m = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(m)
    d.rounded_rectangle((0, 0, w - 1, h - 1), radius=r, fill=255)
    return m


def paste_rounded(base: Image.Image, im: Image.Image, x: int, y: int, r: int = PANEL_RADIUS) -> None:
    """Paste image onto base with rounded corners."""
    w, h = im.size
    mask = rounded_rect_mask(w, h, r)
    base.paste(im, (x, y), mask)


def draw_panel_shadow(draw: ImageDraw.Draw, x: int, y: int, w: int, h: int) -> None:
    """Draw subtle shadow under panel (rounded rect offset)."""
    sx, sy = x + SHADOW_OFFSET, y + SHADOW_OFFSET
    draw.rounded_rectangle(
        [sx, sy, sx + w, sy + h],
        radius=PANEL_RADIUS + 4,
        fill=(195, 192, 188),
    )


def draw_diagonal_bar(draw: ImageDraw.Draw, x: int, y: int, w: int, h: int, top_right: bool) -> None:
    """Diagonal grey bar on one corner (modern overlay)."""
    bar_w, bar_h = 140, 160
    if top_right:
        pts = [(x + w - bar_w, y), (x + w + 15, y), (x + w + 15, y + bar_h), (x + w - bar_w - 40, y + bar_h)]
    else:
        pts = [(x + w - bar_w, y), (x + w + 12, y), (x + w + 12, y + bar_h), (x + w - bar_w - 35, y + bar_h)]
    draw.polygon(pts, fill=(228, 226, 222))


def draw_panel_border(draw: ImageDraw.Draw, x: int, y: int, w: int, h: int) -> None:
    """Thin light border inside panel so it looks framed, not pasted."""
    draw.rounded_rectangle(
        [x + 1, y + 1, x + w - 2, y + h - 2],
        radius=PANEL_RADIUS - 1,
        outline=(255, 252, 248),
        width=1,
    )


def collage_16x9(path1: Path, path2: Path, out_path: Path) -> None:
    W, H = OUTPUT_WIDTH, OUTPUT_HEIGHT
    base = gradient_bg(W, H)
    draw = ImageDraw.Draw(base)

    # Balanced panel layout
    margin = 48
    gap = 28
    top_y = margin
    top_h = 400
    bot_y = top_y + top_h + gap
    bot_h = H - bot_y - margin

    cx = W // 2
    cy = top_y + top_h // 2
    circle_size = CIRCLE_RADIUS * 2

    top_left_w = (W - margin * 2 - gap * 2 - circle_size) // 2
    top_right_x = cx + CIRCLE_RADIUS + gap
    top_right_w = W - top_right_x - margin

    bot_left_w = (W - margin * 2 - gap) // 2
    bot_right_x = margin + bot_left_w + gap
    bot_right_w = W - bot_right_x - margin

    img1 = Image.open(path1).convert("RGB")
    img2 = Image.open(path2).convert("RGB")

    # 1) Subtle shadows under all panels (draw first)
    draw_panel_shadow(draw, margin, top_y, top_left_w, top_h)
    draw_panel_shadow(draw, top_right_x, top_y, top_right_w, top_h)
    draw_panel_shadow(draw, margin, bot_y, bot_left_w, bot_h)
    draw_panel_shadow(draw, bot_right_x, bot_y, bot_right_w, bot_h)

    # 2) Top left – full photo 1, focus top (faces)
    p1_tl = fit_in_panel(img1, top_left_w, top_h, focus_top=True)
    paste_rounded(base, p1_tl, margin, top_y)
    draw_panel_border(draw, margin, top_y, top_left_w, top_h)

    # 3) Top right – full photo 2, focus top
    p2_tr = fit_in_panel(img2, top_right_w, top_h, focus_top=True)
    paste_rounded(base, p2_tr, top_right_x, top_y)
    draw_panel_border(draw, top_right_x, top_y, top_right_w, top_h)

    # 4) Bottom left – full photo 1 (no cut), scaled to panel
    p1_bl = fit_in_panel(img1, bot_left_w, bot_h)
    paste_rounded(base, p1_bl, margin, bot_y)
    draw_panel_border(draw, margin, bot_y, bot_left_w, bot_h)

    # 5) Bottom right – full photo 2 (no cut), scaled to panel
    p2_br = fit_in_panel(img2, bot_right_w, bot_h)
    paste_rounded(base, p2_br, bot_right_x, bot_y)
    draw_panel_border(draw, bot_right_x, bot_y, bot_right_w, bot_h)

    # 6) Central circle – portrait crop (face zone), not random center cut
    circle_img = crop_portrait_region(img1, CIRCLE_RADIUS)
    mask = circular_mask(circle_size)
    base.paste(circle_img, (cx - CIRCLE_RADIUS, cy - CIRCLE_RADIUS), mask)

    # 7) White ring around circle (premium look)
    draw.ellipse(
        [
            cx - CIRCLE_RADIUS - CIRCLE_BORDER,
            cy - CIRCLE_RADIUS - CIRCLE_BORDER,
            cx + CIRCLE_RADIUS + CIRCLE_BORDER,
            cy + CIRCLE_RADIUS + CIRCLE_BORDER,
        ],
        outline=(255, 255, 255),
        width=CIRCLE_BORDER,
    )

    # 8) Diagonal grey bars on bottom panels (modern accent)
    draw_diagonal_bar(draw, margin, bot_y, bot_left_w, bot_h, top_right=True)
    draw_diagonal_bar(draw, bot_right_x, bot_y, bot_right_w, bot_h, top_right=True)

    # 9) Very subtle divider between top and bottom
    draw.line(
        [(margin, bot_y - gap // 2), (W - margin, bot_y - gap // 2)],
        fill=(250, 245, 238),
        width=1,
    )

    base.save(out_path, quality=95)
    print(f"Saved collage: {out_path} ({W}x{H}, 16:9)")


def main():
    if len(sys.argv) >= 4:
        path1, path2 = Path(sys.argv[1]), Path(sys.argv[2])
        out_path = Path(sys.argv[3])
    elif len(sys.argv) == 3:
        path1, path2 = Path(sys.argv[1]), Path(sys.argv[2])
        out_path = SCRIPT_DIR / "collage_16x9.png"
    else:
        path1 = Path(r"C:\Users\davea\Desktop\79221705486819.webp")
        path2 = Path(r"C:\Users\davea\Desktop\33391705486832.webp")
        out_path = SCRIPT_DIR / "collage_16x9.png"

    if not path1.is_file():
        print(f"Not found: {path1}")
        sys.exit(1)
    if not path2.is_file():
        print(f"Not found: {path2}")
        sys.exit(1)

    collage_16x9(path1, path2, out_path)


if __name__ == "__main__":
    main()
