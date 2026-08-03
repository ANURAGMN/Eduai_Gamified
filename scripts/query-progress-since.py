#!/usr/bin/env python3
"""Progress counts for English concepts and simulations since a date (IST)."""

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


def to_day_ist(ms: int | None) -> str | None:
    if ms is None:
        return None
    return datetime.fromtimestamp(ms / 1000, tz=IST).strftime("%Y-%m-%d")


def fmt_ist(ms: int | None) -> str:
    if ms is None:
        return "-"
    return datetime.fromtimestamp(ms / 1000, tz=IST).strftime("%Y-%m-%d %H:%M IST")


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


def record_ts(fields: dict) -> int | None:
    for key in ("completedAt", "updatedAt", "lastAccessedAt", "createdAt"):
        ms = parse_ms(fv(fields, key))
        if ms:
            return ms
    return None


def norm_lang(raw: str | None) -> str:
    if not raw:
        return "unknown"
    raw = raw.lower()
    if raw in ("en", "english"):
        return "en"
    if raw in ("kn", "kannada", "kannada medium"):
        return "kn"
    return raw


def main() -> None:
    since = sys.argv[1] if len(sys.argv) > 1 else "2026-07-09"
    since_ms = int(
        datetime.strptime(since, "%Y-%m-%d")
        .replace(tzinfo=IST)
        .timestamp()
        * 1000
    )

    token = refresh_token()
    rows: list[dict] = []

    for doc in list_collection(token, "progress"):
        cid = doc["name"].split("/")[-1]
        if not cid.startswith(f"{APP}_"):
            continue
        email = email_from_container(cid)
        for rec in list_sub(token, f"progress/{cid}", "records"):
            f = rec.get("fields", {})
            ts = record_ts(f)
            if ts is None or ts < since_ms:
                continue
            rows.append(
                {
                    "email": email,
                    "itemType": (fv(f, "itemType") or "?").upper(),
                    "itemId": fv(f, "itemId") or "?",
                    "language": norm_lang(fv(f, "language")),
                    "status": (fv(f, "status") or "?").upper(),
                    "ts": ts,
                    "day": to_day_ist(ts),
                }
            )

    concept_en = [r for r in rows if r["itemType"] == "CONCEPT" and r["language"] == "en"]
    sim_en = [
        r
        for r in rows
        if r["itemType"] in ("SIMULATION", "SIMULATION_AGENT") and r["language"] == "en"
    ]

    def summarize(items: list[dict], label: str) -> None:
        completed = [r for r in items if r["status"] == "COMPLETED"]
        in_progress = [r for r in items if r["status"] == "IN_PROGRESS"]
        unique_completed = {(r["email"], r["itemId"]) for r in completed}
        unique_any = {(r["email"], r["itemId"]) for r in items}
        unique_users = {r["email"] for r in items}

        print(f"--- {label} ---")
        print(f"Records since {since} IST: {len(items)}")
        print(f"  COMPLETED records: {len(completed)}")
        print(f"  IN_PROGRESS records: {len(in_progress)}")
        print(f"  Unique concept/sim IDs completed: {len({r['itemId'] for r in completed})}")
        print(f"  Unique user+concept completions: {len(unique_completed)}")
        print(f"  Unique user+concept touched (any status): {len(unique_any)}")
        print(f"  Unique users: {len(unique_users)}")

        by_day = Counter(r["day"] for r in completed)
        if by_day:
            print("  Completions by day:")
            for day in sorted(by_day):
                print(f"    {day}: {by_day[day]}")

        by_user = Counter(r["email"] for r in completed)
        if by_user:
            print("  Top users (completions):")
            for email, n in by_user.most_common(8):
                print(f"    {email}: {n}")

    print(f"=== English progress since {since} (IST) ===")
    print(f"Generated: {datetime.now(IST).strftime('%Y-%m-%d %H:%M IST')}\n")
    summarize(concept_en, "English agentic concepts (itemType=CONCEPT)")
    print()
    summarize(sim_en, "English simulations (SIMULATION + SIMULATION_AGENT)")

    sim_agent = [r for r in sim_en if r["itemType"] == "SIMULATION_AGENT"]
    sim_url = [r for r in sim_en if r["itemType"] == "SIMULATION"]
    print("\n--- Simulation split ---")
    print(f"SIMULATION_AGENT completed: {sum(1 for r in sim_agent if r['status']=='COMPLETED')}")
    print(f"SIMULATION (URL) completed: {sum(1 for r in sim_url if r['status']=='COMPLETED')}")

    print("\n--- Recent English concept completions ---")
    for r in sorted(
        [x for x in concept_en if x["status"] == "COMPLETED"],
        key=lambda x: x["ts"],
        reverse=True,
    )[:12]:
        print(f"  {fmt_ist(r['ts'])} | {r['email']} | {r['itemId']}")

    print("\n--- Recent English simulation completions ---")
    for r in sorted(
        [x for x in sim_en if x["status"] == "COMPLETED"],
        key=lambda x: x["ts"],
        reverse=True,
    )[:12]:
        print(f"  {fmt_ist(r['ts'])} | {r['email']} | {r['itemType']} | {r['itemId']}")


if __name__ == "__main__":
    main()
