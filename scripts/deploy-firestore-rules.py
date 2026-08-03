#!/usr/bin/env python3
"""Deploy firestore.rules via Firebase Rules REST API."""
from __future__ import annotations

import base64
import hashlib
import json
import urllib.parse
import urllib.request
from pathlib import Path

PROJECT = "eduai-e090e"
ROOT = Path(__file__).resolve().parent.parent
RULES_PATH = ROOT / "firestore.rules"
TOKEN_PATH = ROOT / ".tools" / "firebase-ci-token.txt"
CLIENT_ID = "563584335869-fgrhgmd47bqnekij5i8b5pr03ho849e6.apps.googleusercontent.com"
CLIENT_SECRET = "j9iVZfS8kkCEFUPaAeJV0sAi"


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


def api_request(token: str, url: str, method: str = "GET", payload: dict | None = None) -> dict:
    data = None
    headers = {"Authorization": f"Bearer {token}"}
    if payload is not None:
        data = json.dumps(payload).encode()
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    with urllib.request.urlopen(req, timeout=120) as resp:
        return json.loads(resp.read().decode())


def main() -> None:
    content = RULES_PATH.read_text(encoding="utf-8")
    fingerprint = base64.b64encode(hashlib.sha256(content.encode()).digest()).decode()

    token = refresh_token()
    ruleset = api_request(
        token,
        f"https://firebaserules.googleapis.com/v1/projects/{PROJECT}/rulesets",
        method="POST",
        payload={
            "source": {
                "files": [
                    {
                        "name": "firestore.rules",
                        "content": content,
                        "fingerprint": fingerprint,
                    }
                ]
            }
        },
    )
    ruleset_name = ruleset["name"]
    print("Created ruleset:", ruleset_name)

    release = api_request(
        token,
        f"https://firebaserules.googleapis.com/v1/projects/{PROJECT}/releases/cloud.firestore?updateMask=rulesetName",
        method="PATCH",
        payload={
            "release": {
                "name": f"projects/{PROJECT}/releases/cloud.firestore",
                "rulesetName": ruleset_name,
            }
        },
    )
    print("Released to cloud.firestore:", release.get("name"))


if __name__ == "__main__":
    main()
