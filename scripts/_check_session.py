import subprocess
import sqlite3
from datetime import datetime, timezone, timedelta
from pathlib import Path

adb = r"C:\Users\anurag.mn\AppData\Local\Android\Sdk\platform-tools\adb.exe"
serial = "192.168.0.100:36971"
pkg = "com.ncert7.aitutorandlab"
sid = "6eed5050-cb5b-46ff-8a06-c238f76d7f50"
out = Path(r"c:\Users\anurag.mn\Desktop\Eduapp\_tmp_db_device")

for name in ["eduai_database", "eduai_database-wal", "eduai_database-shm"]:
    data = subprocess.check_output(
        [adb, "-s", serial, "exec-out", "run-as", pkg, "cat", f"databases/{name}"]
    )
    (out / name).write_bytes(data)

ist = timezone(timedelta(hours=5, minutes=30))
con = sqlite3.connect(out / "eduai_database")
cur = con.cursor()
row = cur.execute(
    "SELECT sessionStartTime, sessionEndTime, durationMillis, isSynced FROM sessions WHERE sessionId=?",
    (sid,),
).fetchone()
start, end, dur, synced = row
st = datetime.fromtimestamp(start / 1000, tz=ist).strftime("%H:%M:%S")
en = datetime.fromtimestamp(end / 1000, tz=ist).strftime("%H:%M:%S") if end else "OPEN"
print(f"Session: {st} -> {en}")
print(f"Foreground duration: {dur // 1000}s ({dur / 60000:.1f} min) | synced={synced}")

sim = cur.execute(
    """
    SELECT durationMillis FROM app_analytics
    WHERE sessionId=? AND screenName='SIMULATION_VIEWER' AND eventType='EXIT'
    ORDER BY analyticsId DESC LIMIT 1
    """,
    (sid,),
).fetchone()
if sim:
    print(f"Simulation viewer (last visit): {sim[0] // 1000}s ({sim[0] / 60000:.1f} min)")

open_count = cur.execute("SELECT COUNT(*) FROM sessions WHERE sessionEndTime IS NULL").fetchone()[0]
print(f"Open sessions: {open_count}")
con.close()
