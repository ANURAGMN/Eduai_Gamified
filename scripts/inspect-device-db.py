import sqlite3
import sys
from pathlib import Path

path = Path(sys.argv[1])
print("size", path.stat().st_size)
con = sqlite3.connect(path)
cur = con.cursor()
tables = [r[0] for r in cur.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")]
print("tables", tables)
if "gamification_profile" in tables:
    print("profile", cur.execute("SELECT studentId, friendCode FROM gamification_profile").fetchall())
if "friend_connection" in tables:
    print("friends", cur.execute("SELECT studentId, friendStudentId, displayName FROM friend_connection").fetchall())
if "friend_feed_item" in tables:
    print(
        "feed",
        cur.execute(
            "SELECT fromDisplayName, message, cheers FROM friend_feed_item ORDER BY createdAt DESC LIMIT 5"
        ).fetchall(),
    )
