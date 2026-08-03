import re
import subprocess
import sys

adb = r"C:\Users\anurag.mn\AppData\Local\Android\Sdk\platform-tools\adb.exe"
serial = sys.argv[1] if len(sys.argv) > 1 else "192.168.0.100:36971"
xml_path = sys.argv[2] if len(sys.argv) > 2 else "/sdcard/ch3.xml"
label = sys.argv[3] if len(sys.argv) > 3 else "Simulation"

xml = subprocess.check_output([adb, "-s", serial, "shell", "cat", xml_path], text=True, errors="ignore")
pattern = rf'text="{re.escape(label)}"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
for m in re.finditer(pattern, xml):
    x1, y1, x2, y2 = map(int, m.groups())
    print(f"{label}: center=({(x1+x2)//2},{(y1+y2)//2}) bounds=[{x1},{y1}][{x2},{y2}]")
