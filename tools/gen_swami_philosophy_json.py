import json
import re
from pathlib import Path

xml = Path("app/src/main/res/values/swami_content.xml").read_text(encoding="utf-8")
m = re.search(
    r'<string name="swami_philosophy">(.*?)</string>', xml, re.DOTALL
)
if not m:
    raise SystemExit("swami_philosophy not found")
s = m.group(1).replace(r"\n", "\n")
out = Path("public/swami_philosophy.json")
out.write_text(
    json.dumps({"philosophy": s}, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)
print("Wrote", out)
