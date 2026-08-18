#!/usr/bin/env python3
"""Enable Email/Password sign-in for Firebase Auth (Play institutional login)."""
from __future__ import annotations

import json
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

PROJECT = "eduai-e090e"
ROOT = Path(__file__).resolve().parent.parent
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


def api(token: str, url: str, method: str = "GET", payload: dict | None = None):
    data = None
    headers = {"Authorization": f"Bearer {token}"}
    if payload is not None:
        data = json.dumps(payload).encode()
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            raw = resp.read().decode()
            return resp.status, json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()


def main() -> None:
    token = refresh_token()
    config_url = f"https://identitytoolkit.googleapis.com/admin/v2/projects/{PROJECT}/config"

    status, before = api(token, config_url)
    print("GET config:", status)
    print(json.dumps(before if isinstance(before, dict) else {"raw": before}, indent=2)[:2000])

    # Enable Email/Password (password required).
    payload = {
        "signIn": {
            "email": {
                "enabled": True,
                "passwordRequired": True,
            }
        }
    }
    status, after = api(
        token,
        f"{config_url}?updateMask=signIn.email",
        method="PATCH",
        payload=payload,
    )
    print("PATCH signIn.email:", status)
    print(json.dumps(after if isinstance(after, dict) else {"raw": after}, indent=2)[:2000])
    if status >= 400:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
