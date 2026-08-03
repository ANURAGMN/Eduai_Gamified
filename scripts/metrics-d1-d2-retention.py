#!/usr/bin/env python3
"""D1 and D2 retention by signup cohort (IST)."""

from __future__ import annotations

import json
import urllib.parse
import urllib.request
from collections import defaultdict
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


def offset_day(cohort_day: str, n: int) -> str:
    return (datetime.strptime(cohort_day, "%Y-%m-%d") + timedelta(days=n)).strftime("%Y-%m-%d")


def main() -> None:
    token = refresh_token()
    activity: dict[str, set[str]] = defaultdict(set)
    cohort: dict[str, list[str]] = defaultdict(list)

    for doc in list_collection(token, "users"):
        f = doc.get("fields", {})
        if fv(f, "appName") != APP:
            continue
        email = fv(f, "email") or doc["name"].split("/")[-1]
        day = to_day_ist(parse_ms(fv(f, "createdAt")))
        if day:
            cohort[day].append(email)

    for coll, sub, ts_keys in [
        ("sessions", "records", ["sessionStartTime", "sessionDate"]),
        ("progress", "records", ["lastAccessedAt", "updatedAt", "completedAt"]),
        ("analytics", "events", ["entryTime"]),
    ]:
        for doc in list_collection(token, coll):
            cid = doc["name"].split("/")[-1]
            if not cid.startswith(f"{APP}_"):
                continue
            email = email_from_container(cid)
            for rec in list_sub(token, f"{coll}/{cid}", sub):
                f = rec.get("fields", {})
                ms = None
                for k in ts_keys:
                    ms = parse_ms(fv(f, k))
                    if ms:
                        break
                day = to_day_ist(ms)
                if day:
                    activity[email].add(day)

    today = datetime.now(IST).date().isoformat()
    print("=== D1 & D2 retention by signup cohort (IST) ===")
    print(f"Generated: {datetime.now(IST).strftime('%Y-%m-%d %H:%M IST')}\n")
    print(
        f"{'Cohort':<12} {'Users':>5}  {'D1':>12}  {'D2':>12}  Notes"
    )
    print("-" * 62)

    for day in sorted(cohort.keys()):
        if day < "2026-06-24":
            continue
        emails = cohort[day]
        n = len(emails)
        d1_day = offset_day(day, 1)
        d2_day = offset_day(day, 2)
        d1_count = sum(1 for e in emails if d1_day in activity.get(e, set()))
        d2_count = sum(1 for e in emails if d2_day in activity.get(e, set()))
        d1_pct = (d1_count / n * 100) if n else 0
        d2_pct = (d2_count / n * 100) if n else 0

        notes = []
        if d1_day > today:
            notes.append("D1 pending")
        if d2_day > today:
            notes.append("D2 pending")
        note = ", ".join(notes) if notes else "complete"

        print(
            f"{day:<12} {n:>5}  {d1_count:>3}/{n} ({d1_pct:4.1f}%)  "
            f"{d2_count:>3}/{n} ({d2_pct:4.1f}%)  {note}"
        )

    print("\n--- Launch-week cohorts (detail) ---")
    for day in sorted(c for c in cohort if c >= "2026-07-07"):
        emails = cohort[day]
        d1_day = offset_day(day, 1)
        d2_day = offset_day(day, 2)
        d1_users = [e for e in emails if d1_day in activity.get(e, set())]
        d2_users = [e for e in emails if d2_day in activity.get(e, set())]
        print(f"\nSignup {day} (n={len(emails)}):")
        print(f"  D1 on {d1_day}: {len(d1_users)} — {', '.join(d1_users) or 'none'}")
        if d2_day <= today:
            print(f"  D2 on {d2_day}: {len(d2_users)} — {', '.join(d2_users) or 'none'}")
        else:
            print(f"  D2 on {d2_day}: pending")

    eligible_d1 = [d for d in cohort if offset_day(d, 1) <= today]
    eligible_d2 = [d for d in cohort if offset_day(d, 2) <= today]
    d1_num = sum(
        sum(1 for e in cohort[d] if offset_day(d, 1) in activity.get(e, set()))
        for d in eligible_d1
    )
    d1_den = sum(len(cohort[d]) for d in eligible_d1)
    d2_num = sum(
        sum(1 for e in cohort[d] if offset_day(d, 2) in activity.get(e, set()))
        for d in eligible_d2
    )
    d2_den = sum(len(cohort[d]) for d in eligible_d2)

    print("\n--- Overall ---")
    if d1_den:
        print(f"D1: {d1_num}/{d1_den} users returned next day ({d1_num / d1_den * 100:.1f}%)")
    if d2_den:
        print(f"D2: {d2_num}/{d2_den} users returned 2 days later ({d2_num / d2_den * 100:.1f}%)")


if __name__ == "__main__":
    main()
