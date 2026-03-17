#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Scan ALL local PDF books and build original-looking chapter lists
into app/src/main/assets/book_chapters.json.

Logic per book:
1. Prefer PDF TOC / outline (if present)
2. Else, infer headings from text patterns (Gujarati/numbered headings)

You can re‑run this script any time after adding new books.
"""

from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Dict, List

import fitz  # PyMuPDF

ROOT = Path(r"f:\ss")
ASSETS_BOOKS = [
    ROOT / "app" / "src" / "main" / "assets" / "ramayan_chintan.pdf",
]
PUBLIC_BOOKS_DIR = ROOT / "public" / "books"
CHAPTERS_JSON = ROOT / "app" / "src" / "main" / "assets" / "book_chapters.json"


def iter_all_pdfs() -> Dict[str, Path]:
    """Return mapping from display name (file name) to absolute PDF path."""
    mapping: Dict[str, Path] = {}
    # Assets PDFs
    for p in ASSETS_BOOKS:
        if p.is_file():
            mapping[p.name] = p
    # Public books
    if PUBLIC_BOOKS_DIR.is_dir():
        for p in PUBLIC_BOOKS_DIR.glob("*.pdf"):
            mapping[p.name] = p
    return mapping


def extract_outline(doc: fitz.Document) -> List[dict]:
    """Use PDF's own TOC if present."""
    toc = doc.get_toc()
    if not toc:
        return []
    chapters: List[dict] = []
    for level, title, page_no in toc:
        if level > 2:  # ignore deep subsections
            continue
        title = (title or "").strip()
        if not title:
            continue
        # page_no is 1‑based, convert to 0‑based index
        chapters.append({"t": title, "p": max(page_no - 1, 0)})
    return chapters


HEADING_PAT = re.compile(r"^(\d+)[\.\)]\s+(.+)")


def extract_headings_from_text(doc: fitz.Document) -> List[dict]:
    """
    Fallback when no TOC:
    - Scan each page, look for lines that look like numbered or obvious headings.
    - Gujarati words like 'અધ્યાય', 'ભૂમિકા', 'પ્રસ્તાવના' are also treated as headings.
    """
    chapters: List[dict] = []
    seen_titles = set()

    for page_index in range(len(doc)):
        page = doc[page_index]
        page_no = page_index + 1
        text = page.get_text("text") or ""
        if not text:
            continue

        best_line = None
        for raw in text.splitlines():
            line = raw.strip()
            if not line:
                continue

            # 1) Numbered pattern: "12. શકુંતલાનો જન્મ"
            m = HEADING_PAT.match(line)
            if m and len(line) > 4:
                best_line = line
                break

            # 2) Gujarati/heading keywords
            if any(
                kw in line
                for kw in ("અધ્યાય", "ભૂમિકા", "પ્રસ્તાવના", "ઉપસંહાર", "અર્પણ")
            ):
                # Avoid picking long paragraph lines – limit length
                if len(line) <= 60:
                    best_line = line
                    break

        if not best_line:
            continue

        # Skip very early pages (front‑matter) – heuristic
        if page_no <= 3 and (
            "અધ્યાય" not in best_line and "Chapter" not in best_line
        ):
            continue

        if best_line in seen_titles:
            continue
        seen_titles.add(best_line)
        chapters.append({"t": best_line, "p": page_index})

    return chapters


def extract_chapters_for_pdf(pdf_path: Path) -> List[dict]:
    # Use ASCII‑safe printing so Windows console encoding does not explode.
    try:
        print("Scanning chapters from:", pdf_path.name)
    except Exception:
        pass
    doc = fitz.open(pdf_path.as_posix())
    try:
        chapters = extract_outline(doc)
        if not chapters:
            chapters = extract_headings_from_text(doc)
        if not chapters:
            # Fallback: at least a single entry for page 0
            chapters = [{"t": "અધ્યાય ૧", "p": 0}]
        return chapters
    finally:
        doc.close()


def main() -> None:
    mapping = iter_all_pdfs()
    if not mapping:
        print("No PDFs found.")
        return

    # Load existing JSON so we can merge/override per key.
    data: dict = {}
    if CHAPTERS_JSON.is_file():
        try:
            data = json.loads(CHAPTERS_JSON.read_text(encoding="utf-8"))
        except Exception:
            data = {}

    total = 0
    for display_name, pdf_path in mapping.items():
        chapters = extract_chapters_for_pdf(pdf_path)
        data[display_name] = chapters
        total += 1

    CHAPTERS_JSON.write_text(
        json.dumps(data, ensure_ascii=False, indent=None, separators=(",", ":")),
        encoding="utf-8",
    )
    print(f"\nUpdated chapters for {total} books in {CHAPTERS_JSON}")


if __name__ == "__main__":
    main()

