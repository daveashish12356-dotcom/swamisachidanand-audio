import json
from pathlib import Path

ROOT = Path(r"f:\ss")


def swap_bhumika_urls(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    data = json.loads(text)

    books = data.get("books", [])
    for book in books:
        if book.get("id") != "mahabharat_jeevankathao":
            continue
        parts = book.get("parts", [])
        p1 = p2 = None
        for p in parts:
            if p.get("title") == "2.ભૂમિકા 1":
                p1 = p
            elif p.get("title") == "2.ભૂમિકા 2":
                p2 = p
        if p1 and p2:
            p1["url"], p2["url"] = p2.get("url"), p1.get("url")
        break

    path.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")


if __name__ == "__main__":
    files = [
        ROOT / "app" / "src" / "main" / "assets" / "audio_list_fallback.json",
        ROOT / "audio_list.json",
        ROOT / "public" / "audio_list.json",
    ]
    for f in files:
        if f.is_file():
            swap_bhumika_urls(f)

