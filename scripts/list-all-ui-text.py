#!/usr/bin/env python3
import re
import subprocess
import sys

adb = r"C:\Users\anurag.mn\AppData\Local\Android\Sdk\platform-tools\adb.exe"
serial = sys.argv[1] if len(sys.argv) > 1 else "192.168.0.100:36971"
xml_path = sys.argv[2] if len(sys.argv) > 2 else "/sdcard/chapters.xml"
skip = {"Home", "Profile", "Settings", "Back"}

xml = subprocess.check_output([adb, "-s", serial, "shell", "cat", xml_path], text=True, errors="ignore")
for m in re.finditer(r'text="([^"]+)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
    t = m.group(1).strip()
    if not t or t in skip or t.isdigit() and len(t) == 1:
        continue
    x1, y1, x2, y2 = map(int, m.groups()[1:])
    print(f"{t!r:55} ({(x1+x2)//2},{(y1+y2)//2})")
