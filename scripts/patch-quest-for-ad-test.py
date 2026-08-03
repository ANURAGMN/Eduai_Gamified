#!/usr/bin/env python3
"""Debug-only: patch quest_daily on device (WAL-safe) for ad-claim testing."""
from __future__ import annotations

import sqlite3
import subprocess
import sys
from pathlib import Path

ADB = r"C:\Users\anurag.mn\AppData\Local\Android\Sdk\platform-tools\adb.exe"
PKG = "com.ncert7.aitutorandlab"
DEFAULT_SERIAL = "adb-123249b7-RrRA4J (2)._adb-tls-connect._tcp"
QUEST_DATE = "2026-07-24"
TMP_DIR = Path(__file__).resolve().parents[1] / "_tmp_db"
DB = TMP_DIR / "device_eduai_database.bin"
WAL = TMP_DIR / "device_eduai_database.bin-wal"
SHM = TMP_DIR / "device_eduai_database.bin-shm"


def adb(serial: str, *args: str, **kwargs) -> subprocess.CompletedProcess:
    return subprocess.run([ADB, "-s", serial, *args], **kwargs)


def stop_app(serial: str) -> None:
    adb(serial, "shell", "am", "force-stop", PKG, check=False)


def pull_file(serial: str, remote: str, local: Path) -> None:
    local.parent.mkdir(parents=True, exist_ok=True)
    with local.open("wb") as out:
        subprocess.run(
            [ADB, "-s", serial, "exec-out", "run-as", PKG, "cat", remote],
            stdout=out,
            check=True,
            timeout=120,
        )


def push_file(serial: str, local: Path, remote: str) -> None:
    proc = subprocess.Popen(
        [ADB, "-s", serial, "shell", "run-as", PKG, "sh", "-c", f"cat > {remote}"],
        stdin=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    _, err = proc.communicate(local.read_bytes(), timeout=120)
    if proc.returncode != 0:
        raise RuntimeError(err.decode(errors="ignore").strip() or f"push failed: {remote}")


def pull_db_bundle(serial: str) -> None:
    for local, remote in [
        (DB, "databases/eduai_database"),
        (WAL, "databases/eduai_database-wal"),
        (SHM, "databases/eduai_database-shm"),
    ]:
        try:
            pull_file(serial, remote, local)
            if local.stat().st_size == 0 and remote.endswith("-wal"):
                local.unlink(missing_ok=True)
            if local.stat().st_size == 0 and remote.endswith("-shm"):
                local.unlink(missing_ok=True)
        except subprocess.CalledProcessError:
            local.unlink(missing_ok=True)


def patch_quest() -> str:
    if DB.stat().st_size < 100:
        raise RuntimeError("main database file is empty — open the app once, then retry")

    # Point sqlite at WAL sidecar files copied beside the main db.
    if WAL.exists() and WAL.stat().st_size > 0:
        WAL.rename(DB.with_suffix(".bin-wal"))
    if SHM.exists() and SHM.stat().st_size > 0:
        SHM.rename(DB.with_suffix(".bin-shm"))

    con = sqlite3.connect(DB)
    try:
        con.execute("PRAGMA wal_checkpoint(FULL)")
        cur = con.cursor()
        cur.execute(
            """
            UPDATE quest_daily
            SET simsClaimed = 0,
                studyDone = 1,
                studyClaimed = 0,
                bonusClaimed = 0
            WHERE questDate = ?
            """,
            (QUEST_DATE,),
        )
        row = cur.execute(
            """
            SELECT simsDone, simsTotal, simsClaimed, studyDone, studyTotal, studyClaimed, bonusClaimed
            FROM quest_daily WHERE questDate = ?
            """,
            (QUEST_DATE,),
        ).fetchone()
        con.commit()
        con.execute("PRAGMA wal_checkpoint(FULL)")
    finally:
        con.close()

    for suffix in ("-wal", "-shm"):
        sidecar = DB.with_suffix(f".bin{suffix}")
        sidecar.unlink(missing_ok=True)

    return str(row)


def push_db_bundle(serial: str) -> None:
    push_file(serial, DB, "databases/eduai_database")
    # Clear stale WAL on device so Room rebuilds from merged main file.
    adb(
        serial,
        "shell",
        "run-as",
        PKG,
        "rm",
        "-f",
        "databases/eduai_database-wal",
        "databases/eduai_database-shm",
        check=False,
    )


def main() -> int:
    serial = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_SERIAL
    stop_app(serial)
    pull_db_bundle(serial)
    row = patch_quest()
    push_db_bundle(serial)
    adb(serial, "shell", "am", "start", "-n", f"{PKG}/.MainActivity", check=False)
    print("patched quest_daily:", row)
    print("On Home: tap sims (3/3) OR study (1/1) → Watch ad → +15 gems")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
