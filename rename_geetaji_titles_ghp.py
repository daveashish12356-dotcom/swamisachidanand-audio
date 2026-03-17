import io
import os


OLD = u"ગીતાજીનું ચિતન"
NEW = u"ગીતાજ્ઞાન ચિંતન"


def main() -> None:
    path = r"F:\ss-ghp\audio_list.json"
    if not os.path.isfile(path):
        print("audio_list.json not found in gh-pages repo")
        return
    with io.open(path, "r", encoding="utf-8") as f:
        data = f.read()
    if OLD not in data:
        print("Old title not found in gh-pages audio_list.json")
        return
    data2 = data.replace(OLD, NEW)
    with io.open(path, "w", encoding="utf-8") as f:
        f.write(data2)
    print("Updated gh-pages audio_list.json")


if __name__ == "__main__":
    main()

