#!/usr/bin/env python3
"""List users who signed up on a given calendar day (IST)."""

from __future__ import annotations

import json
import sys
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


def fmt_ist(ms: int) -> str:
    return datetime.fromtimestamp(ms / 1000, tz=IST).strftime("%Y-%m-%d %H:%M IST")


def main() -> None:
    target = sys.argv[1] if len(sys.argv) > 1 else datetime.now(IST).date().isoformat()
    token = refresh_token()
    url = f"https://firestore.googleapis.com/v1/projects/{PROJECT}/databases/(default)/documents/users?pageSize=100"
    rows: list[dict] = []

    while url:
        data = api_get(token, url)
        for doc in data.get("documents", []):
            f = doc.get("fields", {})
            if fv(f, "appName") != APP:
                continue
            ms = parse_ms(fv(f, "createdAt"))
            if ms is None:
                continue
            day = datetime.fromtimestamp(ms / 1000, tz=IST).strftime("%Y-%m-%d")
            if day != target:
                continue
            rows.append(
                {
                    "email": fv(f, "email") or doc["name"].split("/")[-1],
                    "name": fv(f, "name") or fv(f, "displayName") or "-",
                    "signed_up": fmt_ist(ms),
                    "grade": fv(f, "grade") or fv(f, "class") or "-",
                    "language": fv(f, "language") or fv(f, "preferredLanguage") or "-",
                }
            )
        page = data.get("nextPageToken")
        url = (
            f"https://firestore.googleapis.com/v1/projects/{PROJECT}/databases/(default)/documents/users?pageSize=100&pageToken={page}"
            if page
            else None
        )

    rows.sort(key=lambda r: r["signed_up"])
    print(f"=== Signups on {target} (IST) ===")
    print(f"Total: {len(rows)}\n")
    for i, r in enumerate(rows, 1):
        print(f"{i}. {r['email']}")
        print(
            f"   Name: {r['name']}  |  Signed up: {r['signed_up']}  |  "
            f"Grade: {r['grade']}  |  Lang: {r['language']}"
        )


if __name__ == "__main__":
    main()
