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
        "SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'exam%'"
    )
]
print("exam tables:", tables)
for t in tables:
    n = cur.execute(f"SELECT COUNT(*) FROM {t}").fetchone()[0]
    print(f"{t}: {n} rows")
if "exam_plan" in tables:
    for row in cur.execute(
        "SELECT student_id, subject_id, exam_type, daily_minutes FROM exam_plan LIMIT 5"
    ):
        print("plan:", row)
if "exam_plan_day" in tables:
    for row in cur.execute(
        """
        SELECT day_index, day_type, status, label, estimated_minutes
        FROM exam_plan_day ORDER BY day_index LIMIT 15
        """
    ):
        print("day:", row)
con.close()
