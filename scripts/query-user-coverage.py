#!/usr/bin/env python3
"""What content three users have covered (progress + chapterprogress)."""

from __future__ import annotations

import json
import urllib.parse
import urllib.request
from datetime import datetime, timedelta, timezone
from pathlib import Path

PROJECT = "eduai-e090e"
APP = "eduai_app"
TOKEN_PATH = Path(__file__).resolve().parent.parent / ".tools" / "firebase-ci-token.txt"
CLIENT_ID = "563584335869-fgrhgmd47bqnekij5i8b5pr03ho849e6.apps.googleusercontent.com"
CLIENT_SECRET = "j9iVZfS8kkCEFUPaAeJV0sAi"
IST = timezone(timedelta(hours=5, minutes=30))

TARGETS = [
    "kaurv2191@gmail.com",
    "chavhanm284@gmail.com",
    "methav85@gmail.com",
]


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


def main() -> None:
    token = refresh_token()
    email_meta: dict[str, dict] = {}

    url = f"https://firestore.googleapis.com/v1/projects/{PROJECT}/databases/(default)/documents/users?pageSize=100"
    while url:
        data = api_get(token, url)
        for doc in data.get("documents", []):
            f = doc.get("fields", {})
            email = (fv(f, "email") or "").lower()
            if email not in TARGETS:
                continue
            doc_id = doc["name"].split("/")[-1]
            uid = fv(f, "id") or doc_id
            email_meta[email] = {
                "name": fv(f, "displayName") or fv(f, "name") or "-",
                "uid": uid,
                "doc_id": doc_id,
                "parents": list(
                    dict.fromkeys(
                        [
                            f"{APP}_{email}",
                            f"{APP}_{uid}",
                            f"{APP}_{doc_id}",
                        ]
                    )
                ),
            }
        page = data.get("nextPageToken")
        url = (
            f"https://firestore.googleapis.com/v1/projects/{PROJECT}/databases/(default)/documents/users?pageSize=100&pageToken={page}"
            if page
            else None
        )

    for email in TARGETS:
        meta = email_meta.get(email)
        print("=" * 64)
        if not meta:
            print(email, "NOT FOUND in users")
            continue
        print(f"{meta['name']} <{email}>")
        print(f"uid={meta['uid']} doc={meta['doc_id']}")

        rows = []
        seen = set()
        for cid in meta["parents"]:
            docs = list_docs(token, f"progress/{cid}/records")
            if not docs:
                continue
            print(f"progress/{cid}/records -> {len(docs)}")
            for p in docs:
                f = p.get("fields", {})
                rid = p["name"].split("/")[-1]
                if rid in seen:
                    continue
                seen.add(rid)
                ms = (
                    parse_ms(fv(f, "completedAt"))
                    or parse_ms(fv(f, "lastAccessedAt"))
                    or parse_ms(fv(f, "updatedAt"))
                )
                when = (
                    datetime.fromtimestamp(ms / 1000, tz=IST).strftime("%Y-%m-%d %H:%M")
                    if ms
                    else "-"
                )
                rows.append(
                    {
                        "when": when,
                        "status": (fv(f, "status") or "?").upper(),
                        "type": (fv(f, "itemType") or "?").upper(),
                        "itemId": fv(f, "itemId") or fv(f, "conceptId") or rid,
                        "title": fv(f, "itemTitle")
                        or fv(f, "title")
                        or fv(f, "conceptName")
                        or fv(f, "name")
                        or "-",
                        "chapter": fv(f, "chapterId") or fv(f, "chapterName") or "-",
                        "subject": fv(f, "subjectId") or fv(f, "subjectName") or "-",
                        "lang": fv(f, "language") or "-",
                    }
                )

        chap = []
        for cid in meta["parents"]:
            for p in list_docs(token, f"chapterprogress/{cid}/records"):
                f = p.get("fields", {})
                chap.append(
                    {
                        "chapter": fv(f, "chapterId") or p["name"].split("/")[-1],
                        "status": fv(f, "status"),
                        "pct": fv(f, "overallPercentage"),
                        "lang": fv(f, "language"),
                    }
                )

        if not rows:
            print("(no progress records)")
        else:
            rows.sort(key=lambda r: r["when"])
            done = [r for r in rows if r["status"] == "COMPLETED"]
            wip = [r for r in rows if r["status"] != "COMPLETED"]
            print(f"completed={len(done)}  wip={len(wip)}")
            print("--- COMPLETED ---")
            for r in done:
                print(
                    f"  {r['when']}  {r['type']:16}  lang={r['lang']:4}  "
                    f"title={r['title']}  ch={r['chapter']}  item={r['itemId']}"
                )
            if wip:
                print("--- WIP ---")
                for r in wip:
                    print(
                        f"  {r['when']}  {r['type']:16}  {r['status']:12}  "
                        f"title={r['title']}  ch={r['chapter']}  item={r['itemId']}"
                    )
        if chap:
            print("--- chapterprogress ---")
            for c in chap:
                print(f"  {c}")
        print()


if __name__ == "__main__":
    main()
