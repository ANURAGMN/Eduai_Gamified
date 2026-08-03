#!/usr/bin/env python3
"""Dump UI hierarchy and tap the center of a matching text node."""
from __future__ import annotations

import re
import subprocess
import sys
import time

ADB = r"C:\Users\anurag.mn\AppData\Local\Android\Sdk\platform-tools\adb.exe"
DEFAULT_SERIAL = "192.168.0.100:36971"


def adb(serial: str, *args: str) -> str:
    return subprocess.check_output(
        [ADB, "-s", serial, *args],
        text=True,
        errors="ignore",
    )


def dump_ui(serial: str, remote_path: str = "/sdcard/ui_auto.xml") -> str:
    adb(serial, "shell", "uiautomator", "dump", remote_path)
    time.sleep(0.5)
    return adb(serial, "shell", "cat", remote_path)


def parse_nodes(xml: str) -> list[tuple[str, int, int, int, int]]:
    nodes: list[tuple[str, int, int, int, int]] = []
    for m in re.finditer(
        r'text="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml
    ):
        text = m.group(1)
        x1, y1, x2, y2 = map(int, m.groups()[1:])
        nodes.append((text, x1, y1, x2, y2))
    return nodes


def find_nodes(xml: str, needle: str, exact: bool = False) -> list[tuple[str, int, int]]:
    matches: list[tuple[str, int, int]] = []
    for text, x1, y1, x2, y2 in parse_nodes(xml):
        ok = text == needle if exact else needle.lower() in text.lower()
        if ok:
            matches.append((text, (x1 + x2) // 2, (y1 + y2) // 2))
    return matches


def tap(serial: str, x: int, y: int) -> None:
    adb(serial, "shell", "input", "tap", str(x), str(y))


def main() -> None:
    serial = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_SERIAL
    needle = sys.argv[2] if len(sys.argv) > 2 else "View All Chapters"
    exact = "--exact" in sys.argv
    list_only = "--list" in sys.argv

    xml = dump_ui(serial)
    nodes = find_nodes(xml, needle, exact=exact)

    if list_only:
        for text, x1, y1, x2, y2 in parse_nodes(xml):
            print(f"{text!r:45} ({(x1+x2)//2},{(y1+y2)//2})")
        return

    if not nodes:
        print(f"No match for {needle!r}")
        sys.exit(1)

    text, cx, cy = nodes[0]
    print(f"Tapping {text!r} at ({cx},{cy})")
    tap(serial, cx, cy)


if __name__ == "__main__":
    main()
