#!/usr/bin/env python3
"""Daywise signups (sign-in completes) + session DAU from a start date (IST)."""

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

    signups: Counter[str] = Counter()
    signup_rows: list[tuple[str, str, str, str]] = []
    for doc in list_docs(token, "users"):
        f = doc.get("fields", {})
        if fv(f, "appName") != APP:
            continue
        ms = parse_ms(fv(f, "createdAt"))
        day = to_day(ms)
        if not day or day not in day_set:
            continue
        email = fv(f, "email") or doc["name"].split("/")[-1]
        name = fv(f, "displayName") or fv(f, "name") or "-"
        when = datetime.fromtimestamp(ms / 1000, tz=IST).strftime("%H:%M")
        signups[day] += 1
        signup_rows.append((day, when, email, name))

    dau: dict[str, set[str]] = {day: set() for day in days}
    sessions: Counter[str] = Counter()
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

    print(f"=== EduAI daywise since {since} (IST) through {today.isoformat()} ===")
    print(f"Generated: {datetime.now(IST).strftime('%Y-%m-%d %H:%M IST')}")
    print()
    print("Sign-in completes = new users/{uid} with createdAt that day (appName=eduai_app).")
    print("Downloads = Play Console only (not in Firestore) — column left blank.")
    print()
    print(f"{'Day':<12} {'Downloads':>10} {'Sign-in completes':>18} {'DAU':>5} {'Sessions':>8}")
    for day in days:
        print(
            f"{day:<12} {'n/a':>10} {signups[day]:>18} {len(dau[day]):>5} {sessions[day]:>8}"
        )
    print(
        f"{'TOTAL':<12} {'n/a':>10} {sum(signups.values()):>18} "
        f"{len(set().union(*dau.values()) if any(dau.values()) else set()):>5} "
        f"{sum(sessions.values()):>8}"
    )
    print()
    if signup_rows:
        print("--- Sign-in completes detail ---")
        for day, when, email, name in sorted(signup_rows):
            print(f"  {day} {when}  {email}  ({name})")
    else:
        print("--- Sign-in completes detail: (none) ---")
    print()
    print("--- Active users (sessions) ---")
    for day in days:
        users = sorted(dau[day])
        print(f"  {day}: {', '.join(users) if users else '(none)'}")


if __name__ == "__main__":
    main()
