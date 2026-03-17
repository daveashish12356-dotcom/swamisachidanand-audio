import io
import os


OLD = u"ગીતાજીનું ચિતન"
NEW = u"ગીતાજ્ઞાન ચિંતન"


def replace_in_file(path: str) -> None:
    if not os.path.isfile(path):
        return
    with io.open(path, "r", encoding="utf-8") as f:
        data = f.read()
    if OLD not in data:
        return
    data2 = data.replace(OLD, NEW)
    with io.open(path, "w", encoding="utf-8") as f:
        f.write(data2)
    print("Updated:", path)


def main() -> None:
    root = os.path.dirname(os.path.abspath(__file__))
    files = [
        os.path.join(root, "public", "audio_list.json"),
        os.path.join(root, "app", "src", "main", "assets", "audio_list_fallback.json"),
    ]
    for p in files:
        replace_in_file(p)


if __name__ == "__main__":
    main()

