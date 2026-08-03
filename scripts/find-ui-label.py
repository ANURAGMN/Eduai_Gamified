import re
import subprocess
import sys

adb = r"C:\Users\anurag.mn\AppData\Local\Android\Sdk\platform-tools\adb.exe"
serial = sys.argv[1]
xml_path = sys.argv[2]
labels = sys.argv[3:] or ["View All Chapters", "Conductors", "Simulation"]

xml = subprocess.check_output([adb, "-s", serial, "shell", "cat", xml_path], text=True, errors="ignore")
for label in labels:
    m = re.search(rf'text="{re.escape(label)}"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
    if m:
        x1, y1, x2, y2 = map(int, m.groups())
        print(f"{label}: ({(x1+x2)//2},{(y1+y2)//2})")
    else:
        print(f"{label}: not found")
