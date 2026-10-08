#!/usr/bin/env python3
"""Usage + users since a start date (IST). Default: 2026-09-01."""

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
    with urllib.request.urlopen(req, timeout=120) as resp:
        return json.loads(resp.read().decode())


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


def main() -> None:
    since = sys.argv[1] if len(sys.argv) > 1 else "2026-09-01"
    token = refresh_token()
    today = datetime.now(IST).date()
    start = datetime.fromisoformat(since).date()
    days: list[str] = []
    d = start
    while d <= today:
        days.append(d.isoformat())
        d += timedelta(days=1)
    day_set = set(days)

    users = []
    for doc in list_docs(token, "users"):
        f = doc.get("fields", {})
        if fv(f, "appName") != APP:
            continue
        ms = parse_ms(fv(f, "createdAt"))
        users.append(
            {
                "email": fv(f, "email") or doc["name"].split("/")[-1],
                "name": fv(f, "name") or fv(f, "displayName") or "-",
                "day": to_day(ms),
                "lang": fv(f, "language") or fv(f, "preferredLanguage") or "-",
                "school": fv(f, "schoolName") or fv(f, "school") or "-",
                "klass": fv(f, "grade") or fv(f, "studentClass") or fv(f, "class") or "-",
                "ms": ms,
            }
        )

    signups = [u for u in users if u["day"] and u["day"] >= since]
    total_users = len(users)

    dau: dict[str, set[str]] = {day: set() for day in days}
    sessions: Counter[str] = Counter()
    clicks: Counter[str] = Counter()
    concept_done: Counter[str] = Counter()
    sim_done: Counter[str] = Counter()
    user_activity: dict[str, Counter[str]] = defaultdict(Counter)

    for doc in list_docs(token, "sessions"):
        cid = doc["name"].split("/")[-1]
        if not cid.startswith(f"{APP}_"):
            continue
        email = cid.replace(f"{APP}_", "")
        for s in list_docs(token, f"sessions/{cid}/records"):
            f = s.get("fields", {})
            ms = parse_ms(fv(f, "sessionStartTime")) or parse_ms(fv(f, "sessionDate"))
            day = to_day(ms)
            if day in day_set:
                dau[day].add(email)
                sessions[day] += 1
                user_activity[email]["sessions"] += 1

    for doc in list_docs(token, "analytics"):
        cid = doc["name"].split("/")[-1]
        if not cid.startswith(f"{APP}_"):
            continue
        email = cid.replace(f"{APP}_", "")
        for e in list_docs(token, f"analytics/{cid}/events"):
            f = e.get("fields", {})
            ms = parse_ms(fv(f, "entryTime"))
            day = to_day(ms)
            if day not in day_set:
                continue
            dau[day].add(email)
            if fv(f, "eventType") == "CLICK":
                clicks[day] += 1
                user_activity[email]["clicks"] += 1

    for doc in list_docs(token, "progress"):
        cid = doc["name"].split("/")[-1]
        if not cid.startswith(f"{APP}_"):
            continue
        email = cid.replace(f"{APP}_", "")
        for p in list_docs(token, f"progress/{cid}/records"):
            f = p.get("fields", {})
            ms = (
                parse_ms(fv(f, "completedAt"))
                or parse_ms(fv(f, "lastAccessedAt"))
                or parse_ms(fv(f, "updatedAt"))
            )
            day = to_day(ms)
            if day not in day_set:
                continue
            dau[day].add(email)
            user_activity[email]["progress"] += 1
            status = (fv(f, "status") or "").upper()
            itype = (fv(f, "itemType") or "").upper()
            if status == "COMPLETED":
                if itype in ("CONCEPT", "STUDY", "CHATBOT"):
                    concept_done[day] += 1
                    user_activity[email]["concepts"] += 1
                elif itype in ("SIMULATION", "SIMULATION_AGENT"):
                    sim_done[day] += 1
                    user_activity[email]["sims"] += 1

    unique: set[str] = set()
    for s in dau.values():
        unique |= s

    print(f"=== EduAI usage since {since} (IST) through {today.isoformat()} ===")
    print(f"Generated: {datetime.now(IST).strftime('%Y-%m-%d %H:%M IST')}")
    print(f"Total registered users (all time): {total_users}")
    print(f"Signups since {since}: {len(signups)}")
    print(f"Unique visitors since {since}: {len(unique)}")
    print(
        f"Sessions: {sum(sessions.values())} | Clicks: {sum(clicks.values())} | "
        f"Concept completions: {sum(concept_done.values())} | "
        f"Simulation completions: {sum(sim_done.values())}"
    )
    print()
    print("--- Daily ---")
    print(f"{'Day':<12} {'DAU':>4} {'Sess':>5} {'Click':>5} {'Concept':>7} {'Sim':>4}  Users")
    for day in days:
        users_d = sorted(dau[day])
        names = ", ".join(users_d) if users_d else "-"
        print(
            f"{day:<12} {len(users_d):>4} {sessions[day]:>5} {clicks[day]:>5} "
            f"{concept_done[day]:>7} {sim_done[day]:>4}  {names}"
        )

    print()
    print(f"--- Signups since {since} ---")
    if not signups:
        print("  (none)")
    else:
        for u in sorted(signups, key=lambda x: x["day"] or ""):
            when = (
                datetime.fromtimestamp(u["ms"] / 1000, tz=IST).strftime("%Y-%m-%d %H:%M")
                if u["ms"]
                else u["day"]
            )
            print(
                f"  {u['day']} | {u['email']} | {u['name']} | class {u['klass']} | "
                f"{u['lang']} | {u['school']} | {when}"
            )

    print()
    print(f"--- Active users ({since}+) ---")
    for email, c in sorted(user_activity.items(), key=lambda x: (-sum(x[1].values()), x[0])):
        parts = ", ".join(f"{k}={v}" for k, v in sorted(c.items()))
        print(f"  {email}: {parts}")


if __name__ == "__main__":
    main()
