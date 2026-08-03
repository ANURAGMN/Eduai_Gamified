import sqlite3
import subprocess
import sys
from pathlib import Path

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

adb = r"C:\Users\anurag.mn\AppData\Local\Android\Sdk\platform-tools\adb.exe"
serial = "192.168.0.100:36971"
pkg = "com.ncert7.aitutorandlab"
out = Path(r"c:\Users\anurag.mn\Desktop\Eduapp\_tmp_db_device")
out.mkdir(exist_ok=True)

for name in ["eduai_database", "eduai_database-wal", "eduai_database-shm"]:
    data = subprocess.check_output(
        [adb, "-s", serial, "exec-out", "run-as", pkg, "cat", f"databases/{name}"]
    )
    (out / name).write_bytes(data)

con = sqlite3.connect(out / "eduai_database")
cur = con.cursor()
print("interactions:", cur.execute("SELECT COUNT(*) FROM simulation_interactions").fetchone()[0])
print("unsynced:", cur.execute("SELECT COUNT(*) FROM simulation_interactions WHERE isSynced=0").fetchone()[0])
print("latest session:")
for row in cur.execute(
    "SELECT sessionId, durationMillis/1000, sessionEndTime IS NOT NULL FROM sessions ORDER BY sessionStartTime DESC LIMIT 2"
):
    print(" ", row)
print("latest interactions:")
for row in cur.execute(
    "SELECT interactionId, simulationTitle, elementClicked, isSynced FROM simulation_interactions ORDER BY interactionId DESC LIMIT 3"
):
    print(" ", row)
print("ad events:")
for row in cur.execute(
    "SELECT interactionType, eventType FROM app_analytics WHERE screenName='AD' ORDER BY analyticsId DESC LIMIT 5"
):
    print(" ", row)
con.close()
