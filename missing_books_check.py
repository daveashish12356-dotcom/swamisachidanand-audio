import json

with open("public/books_server_list.json", "r", encoding="utf-8") as f:
    repo = json.load(f)
repo_names = set(repo["fileNames"])

with open("missing_live_list.json", "r", encoding="utf-8") as f:
    live = json.load(f)
live_names = set(live["fileNames"])

missing = sorted(repo_names - live_names)
with open("missing_books_result.txt", "w", encoding="utf-8") as out:
    out.write("Repo: %d  |  Live (app): %d\n" % (len(repo_names), len(live_names)))
    out.write("App me NAHI aa rahi (live list me hi nahi): %d\n\n" % len(missing))
    for n in missing:
        out.write("  %s\n" % n)
