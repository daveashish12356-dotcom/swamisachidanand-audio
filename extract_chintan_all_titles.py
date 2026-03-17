# -*- coding: utf-8 -*-
"""Extract all chapter titles from mahabharat_chintan PDF"""
import json
from pathlib import Path
import fitz  # PyMuPDF

ROOT = Path(r"f:\ss")
PDF_PATH = ROOT / "public" / "books" / "mahabharat_chintan.pdf"
CHAPTERS_JSON = ROOT / "app" / "src" / "main" / "assets" / "book_chapters.json"

def extract_all_chapters():
    """Extract all chapter headings from PDF"""
    if not PDF_PATH.is_file():
        print(f"PDF not found: {PDF_PATH}")
        return []
    
    doc = fitz.open(PDF_PATH.as_posix())
    chapters = []
    
    for page_index in range(len(doc)):
        page = doc[page_index]
        page_no = page_index + 1
        text = page.get_text("text")
        if not text:
            continue
        
        # Find first heading that looks like "12. શકુંતલાનો જન્મ"
        first_heading = None
        for raw_line in text.splitlines():
            line = raw_line.strip()
            if not line:
                continue
            # Pattern: starts with number, dot, space
            if len(line) > 3 and line[0].isdigit() and line[1] == "." and line[2] == " ":
                first_heading = line
                break
        
        if not first_heading:
            continue
        
        # Skip early intro pages
        if page_no <= 9:
            continue
        
        # Avoid duplicates
        if any(c["t"] == first_heading for c in chapters):
            continue
        
        chapters.append({"t": first_heading, "p": page_index, "page": page_no})
    
    doc.close()
    return chapters

def main():
    chapters = extract_all_chapters()
    print(f"Found {len(chapters)} chapter headings")
    
    # Print all chapters
    for i, ch in enumerate(chapters, 1):
        print(f"{i:3d}. Page {ch['page']:3d}: {ch['t']}")
    
    # Save to JSON
    data = {}
    if CHAPTERS_JSON.is_file():
        data = json.loads(CHAPTERS_JSON.read_text(encoding="utf-8"))
    
    key = "મહાભારતનું ચિંતન.pdf"
    data[key] = [{"t": ch["t"], "p": ch["p"]} for ch in chapters]
    
    CHAPTERS_JSON.write_text(
        json.dumps(data, ensure_ascii=False, indent=None, separators=(",", ":")),
        encoding="utf-8",
    )
    print(f"\nSaved {len(chapters)} chapters to {CHAPTERS_JSON}")

if __name__ == "__main__":
    main()
