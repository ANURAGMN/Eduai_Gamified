#!/usr/bin/env python3
"""Science agent / concept activity report."""

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
SCIENCE_SUBJECT_IDS = {
    "a8d0e6b1-9c2f-4a1b-8e3d-1f2a3b4c5d6e",  # fallback if name match fails
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


def record_ts(fields: dict) -> int | None:
    for key in ("completedAt", "updatedAt", "lastAccessedAt", "createdAt", "entryTime"):
        ms = parse_ms(fv(fields, key))
        if ms:
            return ms
    return None


def is_science(meta: dict) -> bool:
    subject = (meta.get("subjectName") or "").lower()
    subject_id = meta.get("subjectId") or ""
    return "science" in subject or subject_id in SCIENCE_SUBJECT_IDS


def load_catalog(token: str) -> dict[str, dict]:
    catalog: dict[str, dict] = {}
    science_ids: set[str] = set()
    for doc in list_collection(token, "Concept"):
        f = doc.get("fields", {})
        concept_id = doc["name"].split("/")[-1]
        meta = {
            "conceptName": fv(f, "concept_name") or concept_id[:8],
            "conceptType": fv(f, "type") or "?",
            "chapterId": fv(f, "chapter_id") or "?",
            "chapterName": fv(f, "unit_name") or "?",
            "subjectId": fv(f, "subject_id") or "?",
            "subjectName": fv(f, "subject_name") or "?",
        }
        catalog[concept_id] = meta
        if is_science(meta):
            science_ids.add(concept_id)
    return catalog, science_ids


def main() -> None:
    since = sys.argv[1] if len(sys.argv) > 1 else "2026-07-09"
    since_ms = int(datetime.strptime(since, "%Y-%m-%d").replace(tzinfo=IST).timestamp() * 1000)

    token = refresh_token()
    catalog, science_ids = load_catalog(token)

    science_catalog = {cid: catalog[cid] for cid in science_ids}
    print(f"=== Science concepts activity since {since} (IST) ===")
    print(f"Generated: {datetime.now(IST).strftime('%Y-%m-%d %H:%M IST')}")
    print(f"Science concepts in catalog: {len(science_catalog)}")
    by_type = Counter(m["conceptType"] for m in science_catalog.values())
    print(f"Catalog by type: {dict(by_type)}\n")

    progress_rows: list[dict] = []
    analytics_rows: list[dict] = []

    for doc in list_collection(token, "progress"):
        cid = doc["name"].split("/")[-1]
        if not cid.startswith(f"{APP}_"):
            continue
        email = email_from_container(cid)
        for rec in list_sub(token, f"progress/{cid}", "records"):
            f = rec.get("fields", {})
            item_id = fv(f, "itemId") or "?"
            if item_id not in science_ids:
                continue
            ts = record_ts(f)
            if ts is None or ts < since_ms:
                continue
            meta = catalog[item_id]
            progress_rows.append(
                {
                    "email": email,
                    "day": to_day_ist(ts),
                    "itemType": (fv(f, "itemType") or "?").upper(),
                    "status": (fv(f, "status") or "?").upper(),
                    "language": (fv(f, "language") or "?").lower(),
                    "itemId": item_id,
                    **meta,
                }
            )

    for doc in list_collection(token, "analytics"):
        cid = doc["name"].split("/")[-1]
        if not cid.startswith(f"{APP}_"):
            continue
        email = email_from_container(cid)
        for e in list_sub(token, f"analytics/{cid}", "events"):
            f = e.get("fields", {})
            concept_id = fv(f, "conceptId")
            if not concept_id or concept_id not in science_ids:
                continue
            ts = record_ts(f)
            if ts is None or ts < since_ms:
                continue
            meta = catalog[concept_id]
            analytics_rows.append(
                {
                    "email": email,
                    "day": to_day_ist(ts),
                    "eventType": fv(f, "eventType"),
                    "screenName": fv(f, "screenName"),
                    "interactionType": fv(f, "interactionType"),
                    "itemId": concept_id,
                    **meta,
                }
            )

    study_types = {"CONCEPT", "SCIENCE_AGENT", "REVISION_AGENT"}
    sim_types = {"SIMULATION", "SIMULATION_AGENT"}

    study_progress = [r for r in progress_rows if r["itemType"] in study_types]
    sim_progress = [r for r in progress_rows if r["itemType"] in sim_types]
    study_completed = [r for r in study_progress if r["status"] == "COMPLETED"]
    sim_completed = [r for r in sim_progress if r["status"] == "COMPLETED"]
    study_started = [r for r in study_progress if r["status"] == "IN_PROGRESS"]

    print("--- Progress (Science concepts) ---")
    print(f"Study/agent records: {len(study_progress)} | completed: {len(study_completed)} | in_progress: {len(study_started)}")
    print(f"Simulation records: {len(sim_progress)} | completed: {len(sim_completed)}")
    print(f"Unique science concepts touched (any agent): {len({r['itemId'] for r in progress_rows})}")
    print(f"Unique users: {len({r['email'] for r in progress_rows})}")

    if not progress_rows and not analytics_rows:
        print("\nNo Science concept progress or analytics since Jul 9.")
        print("\n--- Science catalog sample (STUDY type) ---")
        study_concepts = [
            (cid, m)
            for cid, m in sorted(science_catalog.items(), key=lambda x: (x[1]["chapterName"], x[1]["conceptName"]))
            if (m["conceptType"] or "").upper() == "STUDY"
        ]
        for cid, m in study_concepts[:15]:
            print(f"  {m['chapterName']} / {m['conceptName']} [{m['conceptType']}]")
        if len(study_concepts) > 15:
            print(f"  ... and {len(study_concepts) - 15} more STUDY science concepts")
        return

    if study_completed:
        print("\n--- Science study/agent completions ---")
        for r in sorted(study_completed, key=lambda x: (x["day"], x["email"], x["conceptName"])):
            print(
                f"  [{r['day']}] {r['email']} | {r['chapterName']} / {r['conceptName']} "
                f"| {r['itemType']} | {r['language']}"
            )
    else:
        print("\nScience study/agent completions: **0**")

    if sim_completed:
        print("\n--- Science simulation completions ---")
        for r in sorted(sim_completed, key=lambda x: (x["day"], x["email"], x["conceptName"])):
            print(
                f"  [{r['day']}] {r['email']} | {r['chapterName']} / {r['conceptName']} "
                f"| {r['itemType']} | {r['language']}"
            )

    if analytics_rows:
        print("\n--- Science analytics (clicks/opens) ---")
        click_types = Counter(
            f"{r['screenName']}/{r['interactionType'] or r['eventType']}" for r in analytics_rows
        )
        for k, v in click_types.most_common():
            print(f"  {k}: {v}")
        print("\nRecent events:")
        for r in sorted(analytics_rows, key=lambda x: x["day"], reverse=True)[:15]:
            print(
                f"  [{r['day']}] {r['email']} | {r['screenName']}/{r['interactionType']} "
                f"| {r['chapterName']} / {r['conceptName']}"
            )


if __name__ == "__main__":
    main()
