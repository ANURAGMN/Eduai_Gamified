#!/usr/bin/env python3
"""User / subject / day / concept study-agent progress report (IST)."""

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

STUDY_TYPES = {"CONCEPT", "MATH_AGENT", "REVISION_AGENT", "SCIENCE_AGENT"}


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


def norm_lang(raw: str | None) -> str:
    if not raw:
        return "unknown"
    raw = raw.lower()
    if raw in ("en", "english"):
        return "en"
    if raw in ("kn", "kannada", "kannada medium"):
        return "kn"
    return raw


def record_ts(fields: dict) -> int | None:
    for key in ("completedAt", "updatedAt", "lastAccessedAt", "createdAt"):
        ms = parse_ms(fv(fields, key))
        if ms:
            return ms
    return None


def load_catalog(token: str) -> dict[str, dict]:
    catalog: dict[str, dict] = {}
    for doc in list_collection(token, "Concept"):
        f = doc.get("fields", {})
        concept_id = doc["name"].split("/")[-1]
        catalog[concept_id] = {
            "conceptName": fv(f, "concept_name") or concept_id[:8],
            "conceptType": fv(f, "type") or "?",
            "chapterId": fv(f, "chapter_id") or "?",
            "chapterName": fv(f, "unit_name") or "Unknown chapter",
            "subjectId": fv(f, "subject_id") or "?",
            "subjectName": fv(f, "subject_name") or "Unknown subject",
        }
    return catalog


def agent_label(item_type: str, concept_type: str | None) -> str:
    t = item_type.upper()
    if t == "MATH_AGENT":
        return "math_agent"
    if t == "REVISION_AGENT":
        return "revision_agent"
    if t == "SCIENCE_AGENT":
        return "science_agent"
    if t == "CONCEPT":
        if concept_type and "MATH" in concept_type.upper():
            return "study_concept_via_math"
        return "study_agent"
    return t.lower()


def main() -> None:
    since = sys.argv[1] if len(sys.argv) > 1 else "2026-07-09"
    since_ms = int(datetime.strptime(since, "%Y-%m-%d").replace(tzinfo=IST).timestamp() * 1000)

    token = refresh_token()
    catalog = load_catalog(token)
    rows: list[dict] = []

    for doc in list_collection(token, "progress"):
        cid = doc["name"].split("/")[-1]
        if not cid.startswith(f"{APP}_"):
            continue
        email = email_from_container(cid)
        for rec in list_sub(token, f"progress/{cid}", "records"):
            f = rec.get("fields", {})
            item_type = (fv(f, "itemType") or "?").upper()
            if item_type not in STUDY_TYPES:
                continue
            ts = record_ts(f)
            if ts is None or ts < since_ms:
                continue
            item_id = fv(f, "itemId") or "?"
            meta = catalog.get(item_id, {})
            rows.append(
                {
                    "email": email,
                    "day": to_day_ist(ts),
                    "language": norm_lang(fv(f, "language")),
                    "itemType": item_type,
                    "itemId": item_id,
                    "status": (fv(f, "status") or "?").upper(),
                    "subject": meta.get("subjectName") or "Unknown subject",
                    "chapter": meta.get("chapterName") or "Unknown chapter",
                    "conceptName": meta.get("conceptName") or item_id[:8],
                    "conceptType": meta.get("conceptType"),
                    "agent": agent_label(item_type, meta.get("conceptType")),
                }
            )

    completed = [r for r in rows if r["status"] == "COMPLETED"]
    in_progress = [r for r in rows if r["status"] == "IN_PROGRESS"]

    print(f"=== Study agent report since {since} (IST) ===")
    print(f"Generated: {datetime.now(IST).strftime('%Y-%m-%d %H:%M IST')}")
    print(f"Catalog concepts mapped: {len(catalog)}")
    print(
        f"Records: {len(rows)} total | {len(completed)} completed | {len(in_progress)} in_progress\n"
    )

    print("--- Agent split (completed) ---")
    for agent, n in Counter(r["agent"] for r in completed).most_common():
        print(f"  {agent}: {n}")

    print("\n--- Day-wise (completed) ---")
    for day in sorted({r["day"] for r in completed}):
        day_rows = [r for r in completed if r["day"] == day]
        agents = Counter(r["agent"] for r in day_rows)
        subs = Counter(r["subject"] for r in day_rows)
        print(f"\n{day}: {len(day_rows)} completions | users={len({r['email'] for r in day_rows})}")
        print(f"  agents: {dict(agents)}")
        print(f"  subjects: {dict(subs)}")

    print("\n--- Subject-wise (completed) ---")
    for subject in sorted({r["subject"] for r in completed}):
        srows = [r for r in completed if r["subject"] == subject]
        agents = Counter(r["agent"] for r in srows)
        print(f"\n{subject}: {len(srows)} completions | users={len({r['email'] for r in srows})}")
        print(f"  agents: {dict(agents)}")
        by_day = Counter(r["day"] for r in srows)
        print(f"  by day: {dict(sorted(by_day.items()))}")

    print("\n--- User-wise (completed) ---")
    for email in sorted({r["email"] for r in completed}):
        urows = [r for r in completed if r["email"] == email]
        agents = Counter(r["agent"] for r in urows)
        subs = Counter(r["subject"] for r in urows)
        days = Counter(r["day"] for r in urows)
        print(f"\n{email}: {len(urows)} completions")
        print(f"  agents: {dict(agents)}")
        print(f"  subjects: {dict(subs)}")
        print(f"  days: {dict(sorted(days.items()))}")
        print("  concepts:")
        seen = set()
        for r in sorted(urows, key=lambda x: (x["day"], x["subject"], x["conceptName"])):
            key = (r["itemId"], r["agent"], r["day"])
            if key in seen:
                continue
            seen.add(key)
            lang = r["language"]
            print(
                f"    [{r['day']}] {r['subject']} / {r['chapter']} / "
                f"{r['conceptName']} ({r['agent']}, {lang})"
            )

    print("\n--- Concept-wise (completed, aggregated) ---")
    concept_groups: dict[str, list[dict]] = defaultdict(list)
    for r in completed:
        concept_groups[r["itemId"]].append(r)
    for item_id, crows in sorted(
        concept_groups.items(), key=lambda kv: (-len(kv[1]), kv[1][0]["conceptName"])
    ):
        sample = crows[0]
        users = sorted({r["email"] for r in crows})
        agents = Counter(r["agent"] for r in crows)
        days = Counter(r["day"] for r in crows)
        langs = Counter(r["language"] for r in crows)
        print(
            f"\n{sample['conceptName']} [{sample['subject']} / {sample['chapter']}]"
        )
        print(f"  id: {item_id}")
        print(f"  completions: {len(crows)} | users ({len(users)}): {', '.join(users)}")
        print(f"  agents: {dict(agents)} | langs: {dict(langs)} | days: {dict(sorted(days.items()))}")


if __name__ == "__main__":
    main()
