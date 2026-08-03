#!/usr/bin/env python3
import sqlite3
import sys
from pathlib import Path

path = Path(sys.argv[1] if len(sys.argv) > 1 else "_tmp_db/device_eduai_database")
if not path.exists() or path.stat().st_size < 1000:
    print(f"missing or tiny db: {path}")
    sys.exit(1)

con = sqlite3.connect(path)
cur = con.cursor()
tables = [
    r[0]
    for r in cur.execute(
        "SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'quest%'"
    )
]
print("quest tables:", tables)
if "quest_daily" in [t.lower() for t in tables] or "quest_daily" in tables:
    cols = [r[1] for r in cur.execute("PRAGMA table_info(quest_daily)")]
    print("quest_daily cols:", cols)
    for row in cur.execute(
        "SELECT studentId, questDate, simsDone, simsTotal, studyDone, studyTotal, "
        "simsClaimed, studyClaimed, bonusClaimed FROM quest_daily "
        "ORDER BY questDate DESC LIMIT 5"
    ):
        print("quest:", row)
try:
    for row in cur.execute(
        "SELECT dayIndex, dayType, status, label FROM exam_plan_day "
        "WHERE status IN ('TODAY','UPCOMING') ORDER BY dayIndex LIMIT 5"
    ):
        print("plan day:", row)
except Exception as exc:
    print("plan error:", exc)
try:
    uid = cur.execute("SELECT studentId FROM gamification_profile LIMIT 1").fetchone()
    print("studentId:", uid[0] if uid else None)
except Exception as exc:
    print("profile error:", exc)
con.close()
