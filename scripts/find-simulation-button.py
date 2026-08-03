import re
import subprocess
import sys

adb = r"C:\Users\anurag.mn\AppData\Local\Android\Sdk\platform-tools\adb.exe"
serial = sys.argv[1]
xml_path = sys.argv[2]
anchor = sys.argv[3] if len(sys.argv) > 3 else "Conductors"

xml = subprocess.check_output([adb, "-s", serial, "shell", "cat", xml_path], text=True, errors="ignore")
nodes = []
for m in re.finditer(r'text="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
    text, x1, y1, x2, y2 = m.group(1), *map(int, m.groups()[1:])
    nodes.append((text, x1, y1, x2, y2))

anchor_y = None
for text, x1, y1, x2, y2 in nodes:
    if anchor in text:
        anchor_y = (y1 + y2) // 2
        print(f"anchor {text!r} at y={anchor_y}")
        break

if anchor_y is None:
    print("anchor not found")
    sys.exit(1)

best = None
for text, x1, y1, x2, y2 in nodes:
    if text == "Simulation":
        cy = (y1 + y2) // 2
        dist = abs(cy - anchor_y)
        if best is None or dist < best[0]:
            best = (dist, (x1 + x2) // 2, cy, x1, y1, x2, y2)

if best:
    print(f"nearest Simulation -> center=({best[1]},{best[2]}) bounds=[{best[3]},{best[4]}][{best[5]},{best[6]}]")
