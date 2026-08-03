import re
import subprocess
import sys

adb = r"C:\Users\anurag.mn\AppData\Local\Android\Sdk\platform-tools\adb.exe"
serial = sys.argv[1] if len(sys.argv) > 1 else "192.168.0.100:36971"
xml_path = sys.argv[2] if len(sys.argv) > 2 else "/sdcard/con3.xml"

xml = subprocess.check_output([adb, "-s", serial, "shell", "cat", xml_path], text=True, errors="ignore")
for m in re.finditer(r'text="([^"]+)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
    text = m.group(1)
    x1, y1, x2, y2 = map(int, m.groups()[1:])
    cx, cy = (x1 + x2) // 2, (y1 + y2) // 2
    if any(k in text for k in ("Non", "Properties", "Real", "Metal", "Simulation")) or text.isdigit():
        print(f"{text!r:40} -> ({cx},{cy})")
