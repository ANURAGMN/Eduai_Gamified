#!/usr/bin/env python3
"""Post-mailer opens + task completions among signed-up users."""

from __future__ import annotations

import json
import urllib.parse
import urllib.request
from collections import Counter, defaultdict
from datetime import datetime, timedelta, timezone
from pathlib import Path

PROJECT = "eduai-e090e"
APP = "eduai_app"
IST = timezone(timedelta(hours=5, minutes=30))
# Mail roughly sent Sep 15 ~12:18 IST
SINCE_MS = int(datetime(2026, 9, 15, 12, 0, tzinfo=IST).timestamp() * 1000)
TOKEN_PATH = Path(__file__).resolve().parent.parent / ".tools" / "firebase-ci-token.txt"
CLIENT_ID = "563584335869-fgrhgmd47bqnekij5i8b5pr03ho849e6.apps.googleusercontent.com"
CLIENT_SECRET = "j9iVZfS8kkCEFUPaAeJV0sAi"
MAX_MS = 45 * 60 * 1000
LEARNING = {
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


def fmt(ms: int | None) -> str:
    if not ms:
        return "-"
    return datetime.fromtimestamp(ms / 1000, tz=IST).strftime("%Y-%m-%d %H:%M")


def fmt_dur(ms: int) -> str:
    if not ms:
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


def main() -> None:
    token = refresh_token()

    signed: set[str] = set()
    meta: dict[str, dict] = {}
    id_to_email: dict[str, str] = {}
    for doc in list_docs(token, "users"):
        f = doc.get("fields", {})
        if fv(f, "appName") != APP:
            continue
        email = (fv(f, "email") or "").strip().lower()
        if not email or "@" not in email:
            continue
        signed.add(email)
        docid = doc["name"].split("/")[-1]
        sid = fv(f, "id")
        meta[email] = {"name": fv(f, "displayName") or fv(f, "name") or "-"}
        id_to_email[email] = email
        id_to_email[docid] = email
        if sid:
            id_to_email[str(sid)] = email

    def resolve(cid: str) -> str:
        raw = cid.replace(f"{APP}_", "", 1) if cid.startswith(f"{APP}_") else cid
        return (id_to_email.get(raw) or raw).lower()

    opened: set[str] = set()
    first_open: dict[str, int] = {}
    by_day_open: dict[str, set[str]] = defaultdict(set)
    sess_ms: Counter[str] = Counter()
    learn_ms: Counter[str] = Counter()
    concepts: list[tuple] = []
    sims: list[tuple] = []
    inprog: list[tuple] = []
    concept_cache: dict[str, str] = {}

    def cname(item: str) -> str:
        if item in concept_cache:
            return concept_cache[item]
        try:
            d = api_get(
                token,
                f"https://firestore.googleapis.com/v1/projects/{PROJECT}/databases/(default)/documents/Concept/{item}",
            )
            concept_cache[item] = fv(d.get("fields", {}), "concept_name") or item
        except Exception:
            concept_cache[item] = item
        return concept_cache[item]

    def note_open(email: str, ms: int | None) -> None:
        if email not in signed or ms is None or ms < SINCE_MS:
            return
        opened.add(email)
        day = datetime.fromtimestamp(ms / 1000, tz=IST).strftime("%Y-%m-%d")
        by_day_open[day].add(email)
        if email not in first_open or ms < first_open[email]:
            first_open[email] = ms

    for doc in list_docs(token, "sessions"):
        cid = doc["name"].split("/")[-1]
        if not cid.startswith(f"{APP}_"):
            continue
        email = resolve(cid)
        for s in list_docs(token, f"sessions/{cid}/records"):
            f = s.get("fields", {})
            ms = parse_ms(fv(f, "sessionStartTime")) or parse_ms(fv(f, "sessionDate"))
            note_open(email, ms)
            if ms and ms >= SINCE_MS and email in signed:
                dur = parse_ms(fv(f, "durationMillis")) or 0
                end = parse_ms(fv(f, "sessionEndTime"))
                if dur > 0 and end:
                    sess_ms[email] += min(dur, MAX_MS)

    for doc in list_docs(token, "analytics"):
        cid = doc["name"].split("/")[-1]
        if not cid.startswith(f"{APP}_"):
            continue
        email = resolve(cid)
        for e in list_docs(token, f"analytics/{cid}/events"):
            f = e.get("fields", {})
            ms = parse_ms(fv(f, "entryTime"))
            note_open(email, ms)
            if not (ms and ms >= SINCE_MS and email in signed):
                continue
            et = fv(f, "eventType")
            sn = (fv(f, "screenName") or "").upper()
            dur = parse_ms(fv(f, "durationMillis")) or 0
            if et == "EXIT" and dur > 0 and sn in LEARNING:
                learn_ms[email] += min(dur, MAX_MS)

    for doc in list_docs(token, "progress"):
        cid = doc["name"].split("/")[-1]
        if not cid.startswith(f"{APP}_"):
            continue
        email = resolve(cid)
        if email not in signed:
            continue
        for p in list_docs(token, f"progress/{cid}/records"):
            f = p.get("fields", {})
            ms = (
                parse_ms(fv(f, "completedAt"))
                or parse_ms(fv(f, "lastAccessedAt"))
                or parse_ms(fv(f, "updatedAt"))
            )
            if not ms or ms < SINCE_MS:
                continue
            note_open(email, ms)
            status = (fv(f, "status") or "").upper()
            itype = (fv(f, "itemType") or "").upper()
            item = fv(f, "itemId") or "?"
            name = cname(item)
            pct = fv(f, "progressPercentage")
            if status == "COMPLETED":
                if itype in ("CONCEPT", "STUDY", "CHATBOT"):
                    concepts.append((email, name, pct, ms))
                elif itype in ("SIMULATION", "SIMULATION_AGENT"):
                    sims.append((email, name, pct, ms))
            else:
                inprog.append((email, status, itype, name, pct, ms))

    print("=== Post-mailer engagement (since 2026-09-15 12:00 IST) ===")
    print(f"Generated: {datetime.now(IST).strftime('%Y-%m-%d %H:%M IST')}")
    print(f"Signed-up mail list size: {len(signed)}")
    print(f"Opened app after mail: {len(opened)}")
    print(
        f"Concept completions: {len(concepts)} | "
        f"Sim completions: {len(sims)} | "
        f"In-progress touches: {len(inprog)}"
    )
    print()
    print("--- Opens by day ---")
    for day in sorted(by_day_open):
        users = ", ".join(sorted(by_day_open[day]))
        print(f"{day}: {len(by_day_open[day])} -- {users}")
    print()
    print("--- Who opened (detail) ---")
    for email in sorted(opened, key=lambda e: first_open.get(e, 0)):
        print(email)
        print(
            f"  name={meta.get(email, {}).get('name', '-')} | "
            f"firstOpen={fmt(first_open.get(email))} | "
            f"session={fmt_dur(sess_ms.get(email, 0))} | "
            f"learning={fmt_dur(learn_ms.get(email, 0))}"
        )
    print()
    print("--- Tasks completed after mail ---")
    if not concepts and not sims:
        print("  (none)")
    for e, n, p, ms in sorted(concepts, key=lambda x: x[3]):
        print(f"  CONCEPT | {fmt(ms)} | {e}: {n} ({p}%)")
    for e, n, p, ms in sorted(sims, key=lambda x: x[3]):
        print(f"  SIM | {fmt(ms)} | {e}: {n} ({p}%)")
    if inprog:
        print()
        print("--- In progress after mail ---")
        for e, st, it, n, p, ms in sorted(inprog, key=lambda x: x[5]):
            print(f"  {fmt(ms)} | {st} {it} | {e}: {n} ({p}%)")


if __name__ == "__main__":
    main()
