#!/usr/bin/env python3
"""Last N days: DAU users, usage time, tasks completed (IST)."""

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
MAX_SESSION_MS = 45 * 60 * 1000
MAX_SCREEN_MS = 45 * 60 * 1000
LEARNING_SCREENS = {
    "CHATBOT",
    "SIMULATION_VIEWER",
    "MATH_AGENT",
    "SIMULATION",
    "CONTENT",
    "PLAN_TRIAL",
}


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


def fmt_dur(ms: int) -> str:
    if ms <= 0:
        return "0s"
    s = ms // 1000
    h, rem = divmod(s, 3600)
    m, sec = divmod(rem, 60)
    if h:
        return f"{h}h {m}m"
    if m:
        return f"{m}m {sec}s"
    return f"{sec}s"


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


def resolve_email(cid: str, id_to_email: dict[str, str]) -> str:
    raw = cid.replace(f"{APP}_", "", 1) if cid.startswith(f"{APP}_") else cid
    return id_to_email.get(raw, raw)


def main() -> None:
    days_n = int(sys.argv[1]) if len(sys.argv) > 1 else 7
    token = refresh_token()
    today = datetime.now(IST).date()
    start = today - timedelta(days=days_n - 1)
    days = [(start + timedelta(days=i)).isoformat() for i in range(days_n)]
    day_set = set(days)

    # email map for numeric Google IDs
    id_to_email: dict[str, str] = {}
    for doc in list_docs(token, "users"):
        f = doc.get("fields", {})
        if fv(f, "appName") != APP:
            continue
        email = fv(f, "email") or ""
        docid = doc["name"].split("/")[-1]
        sid = fv(f, "id") or docid
        if email:
            id_to_email[docid] = email
            id_to_email[sid] = email
            id_to_email[email] = email

    dau: dict[str, set[str]] = {d: set() for d in days}
    session_ms: dict[str, int] = Counter()
    session_ms_user: dict[str, Counter[str]] = defaultdict(Counter)
    closed_sessions: Counter[str] = Counter()
    screen_ms: dict[str, int] = Counter()
    screen_ms_user: dict[str, Counter[str]] = defaultdict(Counter)
    concepts: dict[str, list[tuple[str, str]]] = defaultdict(list)
    sims: dict[str, list[tuple[str, str]]] = defaultdict(list)
    concept_cache: dict[str, str] = {}

    def concept_name(item_id: str) -> str:
        if item_id in concept_cache:
            return concept_cache[item_id]
        try:
            d = api_get(
                token,
                f"https://firestore.googleapis.com/v1/projects/{PROJECT}/databases/(default)/documents/Concept/{item_id}",
            )
            name = fv(d.get("fields", {}), "concept_name") or item_id
        except Exception:
            name = item_id
        concept_cache[item_id] = name
        return name

    for doc in list_docs(token, "sessions"):
        cid = doc["name"].split("/")[-1]
        if not cid.startswith(f"{APP}_"):
            continue
        email = resolve_email(cid, id_to_email)
        for s in list_docs(token, f"sessions/{cid}/records"):
            f = s.get("fields", {})
            ms = parse_ms(fv(f, "sessionStartTime")) or parse_ms(fv(f, "sessionDate"))
            day = to_day(ms)
            if day not in day_set:
                continue
            dau[day].add(email)
            dur = parse_ms(fv(f, "durationMillis")) or 0
            end = parse_ms(fv(f, "sessionEndTime"))
            if dur > 0 and end:
                dur = min(dur, MAX_SESSION_MS)
                session_ms[day] += dur
                session_ms_user[day][email] += dur
                closed_sessions[day] += 1

    for doc in list_docs(token, "analytics"):
        cid = doc["name"].split("/")[-1]
        if not cid.startswith(f"{APP}_"):
            continue
        email = resolve_email(cid, id_to_email)
        for e in list_docs(token, f"analytics/{cid}/events"):
            f = e.get("fields", {})
            ms = parse_ms(fv(f, "entryTime"))
            day = to_day(ms)
            if day not in day_set:
                continue
            dau[day].add(email)
            et = fv(f, "eventType")
            sn = (fv(f, "screenName") or "").upper()
            dur = parse_ms(fv(f, "durationMillis")) or 0
            if et == "EXIT" and dur > 0 and sn in LEARNING_SCREENS:
                dur = min(dur, MAX_SCREEN_MS)
                screen_ms[day] += dur
                screen_ms_user[day][email] += dur

    for doc in list_docs(token, "progress"):
        cid = doc["name"].split("/")[-1]
        if not cid.startswith(f"{APP}_"):
            continue
        email = resolve_email(cid, id_to_email)
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
            if (fv(f, "status") or "").upper() != "COMPLETED":
                continue
            itype = (fv(f, "itemType") or "").upper()
            item = fv(f, "itemId") or "?"
            name = concept_name(item)
            if itype in ("CONCEPT", "STUDY", "CHATBOT"):
                concepts[day].append((email, name))
            elif itype in ("SIMULATION", "SIMULATION_AGENT"):
                sims[day].append((email, name))

    print(f"=== DAU past {days_n} days ({days[0]} → {days[-1]}, IST) ===")
    print(f"Generated: {datetime.now(IST).strftime('%Y-%m-%d %H:%M IST')}")
    print(
        "Notes: session time = closed sessions only (cap 45m); "
        "learning time = EXIT on chatbot/sim/math/plan-trial/content (cap 45m)."
    )
    print()

    unique: set[str] = set()
    for s in dau.values():
        unique |= s
    print(
        f"Unique visitors: {len(unique)} | "
        f"Concepts completed: {sum(len(v) for v in concepts.values())} | "
        f"Sims completed: {sum(len(v) for v in sims.values())}"
    )
    print()

    for day in days:
        users = sorted(dau[day])
        print("=" * 72)
        print(
            f"{day}  DAU={len(users)}  closedSessions={closed_sessions[day]}  "
            f"sessionTime={fmt_dur(session_ms[day])}  "
            f"learningTime={fmt_dur(screen_ms[day])}  "
            f"concepts={len(concepts[day])}  sims={len(sims[day])}"
        )
        if not users:
            print("  (no users)")
            continue
        print("  Users:")
        for email in users:
            st = session_ms_user[day].get(email, 0)
            lt = screen_ms_user[day].get(email, 0)
            print(f"    - {email}  session={fmt_dur(st)}  learning={fmt_dur(lt)}")
        if concepts[day]:
            print("  Concepts completed:")
            for email, name in concepts[day]:
                print(f"    - {email}: {name}")
        if sims[day]:
            print("  Sims completed:")
            for email, name in sims[day]:
                print(f"    - {email}: {name}")
        if not concepts[day] and not sims[day]:
            print("  Tasks completed: none")


if __name__ == "__main__":
    main()
