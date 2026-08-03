import re
import sys
from pathlib import Path

xml = Path(sys.argv[1]).read_text(encoding="utf-8", errors="ignore")
texts = re.findall(r'text="([^"]{1,120})"', xml)
descs = re.findall(r'content-desc="([^"]{1,120})"', xml)
print("=== text nodes ===")
seen = set()
for t in texts:
    t = t.strip()
    if t and t not in seen:
        seen.add(t)
        print(t)
print("=== content-desc ===")
seen2 = set()
for d in descs:
    d = d.strip()
    if d and d not in seen2:
        seen2.add(d)
        print(d)
