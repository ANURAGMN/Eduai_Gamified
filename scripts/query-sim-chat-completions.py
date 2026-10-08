#!/usr/bin/env python3
"""Simulation + chat/agent completions (and WIP) since a start date, by user (IST)."""

from __future__ import annotations

import json
import sys
import urllib.parse
import urllib.request
from collections import Counter, defaultdict
from datetime import datetime, timedelta, timezone
from pathlib import Path

PROJECT = "eduai-e090e"
APP = "eduai_app"
TOKEN_PATH = Path(__file__).resolve().parent.parent / ".tools" / "firebase-ci-token.txt"
CLIENT_ID = "563584335869-fgrhgmd47bqnekij5i8b5pr03ho849e6.apps.googleusercontent.com"
CLIENT_SECRET = "j9iVZfS8kkCEFUPaAeJV0sAi"
IST = timezone(timedelta(hours=5, minutes=30))

STUDY = {"CONCEPT", "MATH_AGENT", "SCIENCE_AGENT", "REVISION_AGENT", "STUDY", "CHATBOT"}
SIM = {"SIMULATION", "SIMULATION_AGENT"}


def refresh_token() -> str:
    body = urllib.parse.urlencode(
        {
            "refresh_token": TOKEN_PATH.read_text(encoding="utf-8").strip(),
            "client_id": CLIENT_ID,
            "client_secret": CLIENT_SECRET,
            "grant_type": "refresh_token",
        }
    ).encode()
    req = urllib.request.Request(
        "https://oauth2.googleapis.com/token",
        data=body,
        method="POST",
        headers={"Content-Type": "application/x-www-form-urlencoded"},
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.loads(resp.read().decode())["access_token"]


def api_get(token: str, url: str) -> dict:
    req = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})
    with urllib.request.urlopen(req, timeout=180) as resp:
        return json.loads(resp.read().decode())


def list_docs(token: str, path: str) -> list[dict]:
    parent = f"projects/{PROJECT}/databases/(default)/documents/{path}"
    url = f"https://firestore.googleapis.com/v1/{parent}?pageSize=100&showMissing=true"
    docs: list[dict] = []
    while url:
        data = api_get(token, url)
        docs.extend(data.get("documents", []))
        tok = data.get("nextPageToken")
        url = (
            f"https://firestore.googleapis.com/v1/{parent}?pageSize=100&showMissing=true&pageToken={tok}"
            if tok
            else None
        )
    return docs


def fv(fields: dict, key: str):
    v = fields.get(key, {})
    for k in ("stringValue", "integerValue", "doubleValue", "booleanValue"):
        if k in v:
            return v[k]
    return None


def parse_ms(val):
    if val is None:
        return None
    if isinstance(val, (int, float)):
        return int(val)
    if isinstance(val, str):
        if val.isdigit():
            return int(val)
        try:
            return int(datetime.fromisoformat(val.replace("Z", "+00:00")).timestamp() * 1000)
        except ValueError:
            return None
    return None


def to_day(ms: int | None) -> str | None:
    if ms is None:
        return None
    return datetime.fromtimestamp(ms / 1000, tz=IST).strftime("%Y-%m-%d")


def main() -> None:
    since = sys.argv[1] if len(sys.argv) > 1 else "2026-09-15"
    token = refresh_token()
    today = datetime.now(IST).date()
    start = datetime.fromisoformat(since).date()
    days: list[str] = []
    d = start
    while d <= today:
        days.append(d.isoformat())
        d += timedelta(days=1)
    day_set = set(days)

    by_day_sim: Counter[str] = Counter()
    by_day_chat: Counter[str] = Counter()
    by_day_type: dict[str, Counter[str]] = defaultdict(Counter)
    by_user: dict[str, Counter[str]] = defaultdict(Counter)
    detail: list[tuple] = []

    for doc in list_docs(token, "progress"):
        cid = doc["name"].split("/")[-1]
        if not cid.startswith(f"{APP}_"):
            continue
        email = cid.replace(f"{APP}_", "")
        for p in list_docs(token, f"progress/{cid}/records"):
            f = p.get("fields", {})
            status = (fv(f, "status") or "").upper()
            itype = (fv(f, "itemType") or "").upper()
            ms = (
                parse_ms(fv(f, "completedAt"))
                or parse_ms(fv(f, "lastAccessedAt"))
                or parse_ms(fv(f, "updatedAt"))
            )
            day = to_day(ms)
            if day not in day_set:
                continue
            item = fv(f, "itemId") or fv(f, "conceptId") or p["name"].split("/")[-1]
            when = (
                datetime.fromtimestamp(ms / 1000, tz=IST).strftime("%H:%M") if ms else "-"
            )
            if status == "COMPLETED":
                if itype in SIM:
                    by_day_sim[day] += 1
                    by_user[email]["sim_done"] += 1
                    by_day_type[day][itype] += 1
                elif itype in STUDY:
                    by_day_chat[day] += 1
                    by_user[email]["chat_done"] += 1
                    by_day_type[day][itype] += 1
                else:
                    by_user[email][f"other_done:{itype or '?'}"] += 1
                detail.append((day, when, email, "DONE", itype, item))
            else:
                if itype in SIM:
                    by_user[email]["sim_wip"] += 1
                elif itype in STUDY:
                    by_user[email]["chat_wip"] += 1
                else:
                    label = itype or status or "?"
                    by_user[email][f"other_wip:{label}"] += 1
                detail.append((day, when, email, "WIP", itype or status, item))

    print(f"=== Sim + chat/agent since {since} (IST) through {today.isoformat()} ===")
    print(f"Generated: {datetime.now(IST).strftime('%Y-%m-%d %H:%M IST')}")
    print(f"Chat/study itemTypes: {sorted(STUDY)}")
    print(f"Sim itemTypes: {sorted(SIM)}")
    print()
    print(f"{'Day':<12} {'Sim done':>8} {'Chat done':>9}  types")
    for day in days:
        parts = ", ".join(f"{k}={v}" for k, v in sorted(by_day_type[day].items())) or "-"
        print(f"{day:<12} {by_day_sim[day]:>8} {by_day_chat[day]:>9}  {parts}")
    print(
        f"{'TOTAL':<12} {sum(by_day_sim.values()):>8} {sum(by_day_chat.values()):>9}"
    )
    print()
    print("--- By user ---")
    for email, c in sorted(
        by_user.items(),
        key=lambda x: (
            -(x[1].get("sim_done", 0) + x[1].get("chat_done", 0)),
            -(x[1].get("sim_wip", 0) + x[1].get("chat_wip", 0)),
            x[0],
        ),
    ):
        parts = ", ".join(f"{k}={v}" for k, v in sorted(c.items()))
        print(f"  {email}: {parts}")
    print()
    print("--- DONE detail ---")
    dones = [r for r in detail if r[3] == "DONE"]
    if not dones:
        print("  (none)")
    else:
        for row in sorted(dones):
            print(f"  {row[0]} {row[1]}  {row[2]}  {row[4]}  {row[5]}")
    print()
    print("--- WIP summary (records touched in window, status != COMPLETED) ---")
    wips = [r for r in detail if r[3] == "WIP"]
    wsum: dict[str, Counter[str]] = defaultdict(Counter)
    for _day, _when, email, _st, itype, _item in wips:
        wsum[email][itype] += 1
    print(f"  WIP records: {len(wips)}")
    for email, c in sorted(wsum.items(), key=lambda x: -sum(x[1].values())):
        parts = ", ".join(f"{k}={v}" for k, v in sorted(c.items()))
        print(f"  {email}: {parts}")


if __name__ == "__main__":
    main()
