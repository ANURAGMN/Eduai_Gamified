#!/usr/bin/env python3
"""Firestore usage metrics for a calendar day window (IST)."""

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
    for k in ("stringValue", "integerValue", "doubleValue"):
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
        if len(val) == 10 and val[4] == "-":
            return int(datetime.fromisoformat(val + "T12:00:00+00:00").timestamp() * 1000)
        try:
            return int(datetime.fromisoformat(val.replace("Z", "+00:00")).timestamp() * 1000)
        except ValueError:
            return None
    return None


def to_day_ist(ms: int | None) -> str | None:
    if ms is None:
        return None
    return datetime.fromtimestamp(ms / 1000, tz=IST).strftime("%Y-%m-%d")


def list_collection(token: str, coll: str) -> list[dict]:
    parent = f"projects/{PROJECT}/databases/(default)/documents/{coll}"
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


def list_sub(token: str, parent_path: str, sub: str) -> list[dict]:
    base = f"projects/{PROJECT}/databases/(default)/documents/{parent_path}/{sub}"
    url = f"https://firestore.googleapis.com/v1/{base}?pageSize=100&showMissing=true"
    docs: list[dict] = []
    while url:
        data = api_get(token, url)
        docs.extend(data.get("documents", []))
        tok = data.get("nextPageToken")
        url = (
            f"https://firestore.googleapis.com/v1/{base}?pageSize=100&showMissing=true&pageToken={tok}"
            if tok
            else None
        )
    return docs


def email_from_container(cid: str) -> str:
    return cid.replace(f"{APP}_", "")


def main() -> None:
    # Optional: metrics-date-window.py 2026-06-30 2026-07-01
    now_ist = datetime.now(IST).date()
    if len(sys.argv) >= 3:
        days = [sys.argv[1], sys.argv[2]]
    else:
        yesterday = (now_ist - timedelta(days=1)).isoformat()
        today = now_ist.isoformat()
        days = [yesterday, today]

    day_set = set(days)
    token = refresh_token()

    dau_users: dict[str, set[str]] = {d: set() for d in days}
    sessions_by_day: Counter[str] = Counter()
    clicks_by_day: Counter[str] = Counter()
    funnel_by_day: Counter[str] = Counter()
    click_types: Counter[str] = Counter()
    funnel_steps: Counter[str] = Counter()
    user_events: dict[str, Counter[str]] = defaultdict(Counter)

    for doc in list_collection(token, "sessions"):
        cid = doc["name"].split("/")[-1]
        if not cid.startswith(f"{APP}_"):
            continue
        email = email_from_container(cid)
        for s in list_sub(token, f"sessions/{cid}", "records"):
            f = s.get("fields", {})
            ms = parse_ms(fv(f, "sessionStartTime")) or parse_ms(fv(f, "sessionDate"))
            day = to_day_ist(ms)
            if day in day_set:
                dau_users[day].add(email)
                sessions_by_day[day] += 1
                user_events[email][f"sessions@{day}"] += 1

    for doc in list_collection(token, "analytics"):
        cid = doc["name"].split("/")[-1]
        if not cid.startswith(f"{APP}_"):
            continue
        email = email_from_container(cid)
        for e in list_sub(token, f"analytics/{cid}", "events"):
            f = e.get("fields", {})
            ms = parse_ms(fv(f, "entryTime"))
            day = to_day_ist(ms)
            if day not in day_set:
                continue
            dau_users[day].add(email)
            et = fv(f, "eventType")
            if et == "CLICK":
                clicks_by_day[day] += 1
                user_events[email][f"clicks@{day}"] += 1
                typ = f"{fv(f, 'screenName')}/{fv(f, 'interactionType') or '?'}"
                click_types[typ] += 1
            elif et == "FUNNEL":
                funnel_by_day[day] += 1
                step = fv(f, "interactionType") or fv(f, "conceptId") or "?"
                funnel_steps[step] += 1
                user_events[email][f"funnel:{step}@{day}"] += 1

    for doc in list_collection(token, "progress"):
        cid = doc["name"].split("/")[-1]
        if not cid.startswith(f"{APP}_"):
            continue
        email = email_from_container(cid)
        for p in list_sub(token, f"progress/{cid}", "records"):
            f = p.get("fields", {})
            ms = (
                parse_ms(fv(f, "lastAccessedAt"))
                or parse_ms(fv(f, "updatedAt"))
                or parse_ms(fv(f, "completedAt"))
            )
            day = to_day_ist(ms)
            if day in day_set:
                dau_users[day].add(email)
                user_events[email][f"progress@{day}"] += 1

    generated = datetime.now(IST).strftime("%Y-%m-%d %H:%M IST")
    print(f"=== EduAI visits ({days[0]} to {days[-1]}, IST) ===")
    print(f"Generated: {generated}\n")

    total_dau = set()
    for d in days:
        total_dau |= dau_users[d]

    print("--- Daily summary ---")
    for d in days:
        users = sorted(dau_users[d])
        label = "today so far" if d == days[-1] else "yesterday"
        print(
            f"{d} ({label}): DAU={len(users)} | sessions={sessions_by_day[d]} | "
            f"clicks={clicks_by_day[d]} | funnel={funnel_by_day[d]}"
        )
        if users:
            print(f"  users: {', '.join(users)}")

    print(f"\n--- Window totals ---")
    print(f"Unique visitors: {len(total_dau)}")
    print(f"Sessions: {sum(sessions_by_day.values())}")
    print(f"Clicks: {sum(clicks_by_day.values())}")
    print(f"Funnel events: {sum(funnel_by_day.values())}")

    if user_events:
        print("\n--- Per-user activity ---")
        for email, counts in sorted(user_events.items(), key=lambda x: (-sum(x[1].values()), x[0])):
            parts = ", ".join(f"{k}={v}" for k, v in sorted(counts.items()))
            print(f"  {email}: {parts}")

    if click_types:
        print("\n--- Clicks by type ---")
        for k, v in click_types.most_common():
            print(f"  {k}: {v}")

    if funnel_steps:
        print("\n--- Funnel steps ---")
        for k, v in sorted(funnel_steps.items()):
            print(f"  {k}: {v}")


if __name__ == "__main__":
    main()
