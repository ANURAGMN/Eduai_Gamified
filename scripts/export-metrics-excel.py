#!/usr/bin/env python3
"""Export daywise EduAI metrics + user PII to Excel (IST).

Usage:
  python scripts/export-metrics-excel.py              # last 15 IST days incl. today
  python scripts/export-metrics-excel.py 2026-09-22   # since this date through today
  python scripts/export-metrics-excel.py 2026-09-22 2026-10-06

Installs/downloads are NOT in Firestore (Play Console only) — column left blank.
"""

from __future__ import annotations

import json
import sys
import urllib.parse
import urllib.request
import zipfile
from collections import Counter, defaultdict
from datetime import datetime, timedelta, timezone
from io import BytesIO
from pathlib import Path
from xml.sax.saxutils import escape

PROJECT = "eduai-e090e"
APP = "eduai_app"
TOKEN_PATH = Path(__file__).resolve().parent.parent / ".tools" / "firebase-ci-token.txt"
CLIENT_ID = "563584335869-fgrhgmd47bqnekij5i8b5pr03ho849e6.apps.googleusercontent.com"
CLIENT_SECRET = "j9iVZfS8kkCEFUPaAeJV0sAi"
IST = timezone(timedelta(hours=5, minutes=30))
OUT_DIR = Path(__file__).resolve().parent.parent / "exports"

STUDY = {"CONCEPT", "MATH_AGENT", "SCIENCE_AGENT", "REVISION_AGENT", "STUDY", "CHATBOT"}
SIM = {"SIMULATION", "SIMULATION_AGENT"}

USER_PROFILE_KEYS = [
    "email",
    "displayName",
    "phoneNumber",
    "schoolName",
    "studentClass",
    "language",
    "appName",
    "appVersionName",
    "appVersionCode",
    "appVersionFirstSeenName",
    "createdAt",
    "updatedAt",
    "id",
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


def to_day(ms: int | None) -> str | None:
    if ms is None:
        return None
    return datetime.fromtimestamp(ms / 1000, tz=IST).strftime("%Y-%m-%d")


def fmt_ist(ms) -> str:
    n = parse_ms(ms)
    if n is None:
        return ""
    return datetime.fromtimestamp(n / 1000, tz=IST).strftime("%Y-%m-%d %H:%M")


def ist_days(start, end) -> list[str]:
    days: list[str] = []
    d = start
    while d <= end:
        days.append(d.isoformat())
        d += timedelta(days=1)
    return days


def user_row(doc: dict) -> dict:
    f = doc.get("fields", {}) or {}
    doc_id = doc["name"].split("/")[-1]
    email = fv(f, "email") or ""
    uid = fv(f, "id") or doc_id
    created_ms = parse_ms(fv(f, "createdAt"))
    return {
        "doc_id": doc_id,
        "uid": uid,
        "email": email,
        "name": fv(f, "displayName") or fv(f, "name") or "",
        "phone": fv(f, "phoneNumber") or "",
        "school": fv(f, "schoolName") or "",
        "class": fv(f, "studentClass") or "",
        "language": fv(f, "language") or "",
        "appName": fv(f, "appName") or "",
        "appVersionName": fv(f, "appVersionName") or "",
        "appVersionCode": fv(f, "appVersionCode") or "",
        "appVersionFirstSeenName": fv(f, "appVersionFirstSeenName") or "",
        "createdAt": fmt_ist(created_ms),
        "createdDay": to_day(created_ms) or "",
        "updatedAt": fmt_ist(fv(f, "updatedAt")),
        "keys": ",".join(sorted(f.keys())),
    }


def index_users(users: list[dict]) -> dict[str, dict]:
    by: dict[str, dict] = {}
    for u in users:
        for k in (u["email"], u["uid"], u["doc_id"]):
            if k:
                by[str(k).lower()] = u
                by[str(k)] = u
    return by


def lookup_user(by: dict[str, dict], key: str) -> dict | None:
    if not key:
        return None
    return by.get(key) or by.get(key.lower())


def cell_xml(value, col: int, row: int) -> str:
    ref = f"{col_letter(col)}{row}"
    if value is None:
        value = ""
    if isinstance(value, bool):
        return f'<c r="{ref}" t="b"><v>{1 if value else 0}</v></c>'
    if isinstance(value, (int, float)) and not isinstance(value, bool):
        return f'<c r="{ref}"><v>{value}</v></c>'
    text = escape(str(value), {"'": "&apos;", '"': "&quot;"})
    return f'<c r="{ref}" t="inlineStr"><is><t xml:space="preserve">{text}</t></is></c>'


def col_letter(n: int) -> str:
    s = ""
    while n:
        n, r = divmod(n - 1, 26)
        s = chr(65 + r) + s
    return s


def sheet_xml(name: str, rows: list[list]) -> str:
    parts = [
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
        '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">',
        "<sheetData>",
    ]
    for r_i, row in enumerate(rows, start=1):
        cells = "".join(cell_xml(v, c_i, r_i) for c_i, v in enumerate(row, start=1))
        parts.append(f'<row r="{r_i}">{cells}</row>')
    parts.append("</sheetData></worksheet>")
    return "".join(parts)


def write_xlsx(path: Path, sheets: list[tuple[str, list[list]]]) -> None:
    ns = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    rel_ns = "http://schemas.openxmlformats.org/package/2006/relationships"
    od_rel = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    wb_rels = [
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
        f'<Relationships xmlns="{rel_ns}">',
    ]
    sheets_xml = []
    for i, (title, _) in enumerate(sheets, start=1):
        safe = title[:31]
        sheets_xml.append(f'<sheet name="{escape(safe)}" sheetId="{i}" r:id="rId{i}"/>')
        wb_rels.append(
            f'<Relationship Id="rId{i}" Type="{od_rel}/worksheet" Target="worksheets/sheet{i}.xml"/>'
        )
    wb_rels.append("</Relationships>")
    workbook = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        f'<workbook xmlns="{ns}" xmlns:r="{od_rel}">'
        f'<sheets>{"".join(sheets_xml)}</sheets></workbook>'
    )
    content_types = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
        '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
        '<Default Extension="xml" ContentType="application/xml"/>'
        '<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>'
        + "".join(
            f'<Override PartName="/xl/worksheets/sheet{i}.xml" '
            'ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>'
            for i in range(1, len(sheets) + 1)
        )
        + "</Types>"
    )
    pkg_rels = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        f'<Relationships xmlns="{rel_ns}">'
        f'<Relationship Id="rId1" Type="{od_rel}/officeDocument" Target="xl/workbook.xml"/>'
        "</Relationships>"
    )
    path.parent.mkdir(parents=True, exist_ok=True)
    buf = BytesIO()
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("[Content_Types].xml", content_types)
        z.writestr("_rels/.rels", pkg_rels)
        z.writestr("xl/workbook.xml", workbook)
        z.writestr("xl/_rels/workbook.xml.rels", "".join(wb_rels))
        for i, (_, rows) in enumerate(sheets, start=1):
            z.writestr(f"xl/worksheets/sheet{i}.xml", sheet_xml(sheets[i - 1][0], rows))
    path.write_bytes(buf.getvalue())


def main() -> None:
    today = datetime.now(IST).date()
    if len(sys.argv) >= 3:
        start = datetime.fromisoformat(sys.argv[1]).date()
        end = datetime.fromisoformat(sys.argv[2]).date()
    elif len(sys.argv) == 2:
        start = datetime.fromisoformat(sys.argv[1]).date()
        end = today
    else:
        end = today
        start = today - timedelta(days=14)

    days = ist_days(start, end)
    day_set = set(days)
    generated = datetime.now(IST).strftime("%Y-%m-%d %H:%M IST")
    token = refresh_token()

    users_raw = []
    for doc in list_docs(token, "users"):
        u = user_row(doc)
        if u["appName"] and u["appName"] != APP:
            continue
        users_raw.append(u)
    by_user = index_users(users_raw)

    signups: Counter[str] = Counter()
    signup_users: list[dict] = []
    for u in users_raw:
        if u["createdDay"] in day_set:
            signups[u["createdDay"]] += 1
            signup_users.append(u)

    dau: dict[str, set[str]] = {d: set() for d in days}
    sessions: Counter[str] = Counter()
    active_keys: set[str] = set()
    for doc in list_docs(token, "sessions"):
        cid = doc["name"].split("/")[-1]
        if not cid.startswith(f"{APP}_"):
            continue
        key = cid.replace(f"{APP}_", "", 1)
        for s in list_docs(token, f"sessions/{cid}/records"):
            f = s.get("fields", {})
            ms = parse_ms(fv(f, "sessionStartTime")) or parse_ms(fv(f, "sessionDate"))
            day = to_day(ms)
            if day in day_set:
                dau[day].add(key)
                sessions[day] += 1
                active_keys.add(key)

    by_day_sim: Counter[str] = Counter()
    by_day_chat: Counter[str] = Counter()
    by_day_type: dict[str, Counter[str]] = defaultdict(Counter)
    tasks_done: list[list] = []
    tasks_wip: list[list] = []

    for doc in list_docs(token, "progress"):
        cid = doc["name"].split("/")[-1]
        if not cid.startswith(f"{APP}_"):
            continue
        key = cid.replace(f"{APP}_", "", 1)
        u = lookup_user(by_user, key)
        email = (u["email"] if u else "") or key
        name = u["name"] if u else ""
        for p in list_docs(token, f"progress/{cid}/records"):
            f = p.get("fields", {})
            status = (fv(f, "status") or "").upper()
            itype = (fv(f, "itemType") or "").upper()
            ms = (
                parse_ms(fv(f, "completedAt"))
                or parse_ms(fv(f, "lastAccessedAt"))
                or parse_ms(fv(f, "updatedAt"))
            )
            day = to_day(ms)
            if day not in day_set:
                continue
            item = fv(f, "itemId") or fv(f, "conceptId") or p["name"].split("/")[-1]
            title = fv(f, "itemTitle") or fv(f, "title") or fv(f, "conceptName") or ""
            chapter = fv(f, "chapterName") or fv(f, "chapterId") or ""
            subject = fv(f, "subjectName") or fv(f, "subjectId") or ""
            lang = fv(f, "language") or ""
            when = fmt_ist(ms)
            row = [day, when, email, name, status, itype, title, chapter, subject, lang, item]
            if status == "COMPLETED":
                tasks_done.append(row)
                if itype in SIM:
                    by_day_sim[day] += 1
                    by_day_type[day][itype] += 1
                elif itype in STUDY:
                    by_day_chat[day] += 1
                    by_day_type[day][itype] += 1
                else:
                    by_day_type[day][itype or "OTHER"] += 1
            else:
                tasks_wip.append(row)

    unique_dau = set().union(*dau.values()) if any(dau.values()) else set()

    notes = [
        ["Field", "Value"],
        ["Generated", generated],
        ["Timezone", "Asia/Kolkata (IST)"],
        ["Window start", start.isoformat()],
        ["Window end", end.isoformat()],
        ["Days", len(days)],
        ["Firebase project", PROJECT],
        ["appName filter", APP],
        [
            "Installs",
            "Not in Firestore. Use Play Console. Daywise Downloads column is blank.",
        ],
        [
            "Sign-in completes",
            "users/{uid} with appName=eduai_app and createdAt on that IST day.",
        ],
        ["DAU", "Unique session parent keys with a session record that IST day."],
        [
            "Sim done",
            "progress records COMPLETED with itemType SIMULATION or SIMULATION_AGENT.",
        ],
        [
            "Chat/agent done",
            "progress COMPLETED with CONCEPT, MATH_AGENT, SCIENCE_AGENT, REVISION_AGENT, STUDY, CHATBOT.",
        ],
        ["See also", "docs/METRICS_FIRESTORE_PULL.md"],
    ]

    daywise = [
        [
            "Day",
            "Installs (Play Console)",
            "Sign-in completes",
            "DAU",
            "Sessions",
            "Sim done",
            "Chat/agent done",
            "Tasks completed total",
            "Completed types",
        ]
    ]
    for day in days:
        types = ", ".join(f"{k}={v}" for k, v in sorted(by_day_type[day].items()))
        sim = by_day_sim[day]
        chat = by_day_chat[day]
        daywise.append(
            [
                day,
                "",
                signups[day],
                len(dau[day]),
                sessions[day],
                sim,
                chat,
                sim + chat,
                types,
            ]
        )
    daywise.append(
        [
            "TOTAL",
            "",
            sum(signups.values()),
            len(unique_dau),
            sum(sessions.values()),
            sum(by_day_sim.values()),
            sum(by_day_chat.values()),
            sum(by_day_sim.values()) + sum(by_day_chat.values()),
            "",
        ]
    )

    profile_headers = [
        "signup_day",
        "createdAt_IST",
        "email",
        "name",
        "phone",
        "school",
        "class",
        "language",
        "uid",
        "doc_id",
        "appVersionName",
        "appVersionCode",
        "appVersionFirstSeenName",
        "updatedAt_IST",
        "user_fields",
    ]

    def profile_line(u: dict) -> list:
        return [
            u.get("createdDay", ""),
            u.get("createdAt", ""),
            u.get("email", ""),
            u.get("name", ""),
            u.get("phone", ""),
            u.get("school", ""),
            u.get("class", ""),
            u.get("language", ""),
            u.get("uid", ""),
            u.get("doc_id", ""),
            u.get("appVersionName", ""),
            u.get("appVersionCode", ""),
            u.get("appVersionFirstSeenName", ""),
            u.get("updatedAt", ""),
            u.get("keys", ""),
        ]

    new_sheet = [profile_headers]
    for u in sorted(signup_users, key=lambda x: (x["createdDay"], x["createdAt"], x["email"])):
        new_sheet.append(profile_line(u))

    active_sheet = [
        [
            "session_key",
            "email",
            "name",
            "phone",
            "school",
            "class",
            "language",
            "signup_day",
            "createdAt_IST",
            "uid",
            "doc_id",
            "days_active_in_window",
            "session_days",
            "appVersionName",
            "matched_user_doc",
        ]
    ]
    for key in sorted(active_keys, key=lambda k: k.lower()):
        u = lookup_user(by_user, key)
        days_active = sorted(d for d in days if key in dau[d])
        if u:
            active_sheet.append(
                [
                    key,
                    u["email"] or key,
                    u["name"],
                    u["phone"],
                    u["school"],
                    u["class"],
                    u["language"],
                    u["createdDay"],
                    u["createdAt"],
                    u["uid"],
                    u["doc_id"],
                    len(days_active),
                    ", ".join(days_active),
                    u["appVersionName"],
                    "yes",
                ]
            )
        else:
            active_sheet.append(
                [
                    key,
                    key,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    len(days_active),
                    ", ".join(days_active),
                    "",
                    "no",
                ]
            )

    task_headers = [
        "day",
        "when_IST",
        "email",
        "name",
        "status",
        "itemType",
        "title",
        "chapter",
        "subject",
        "language",
        "itemId",
    ]
    done_sheet = [task_headers] + sorted(tasks_done, key=lambda r: (r[0], r[1], r[2]))
    wip_sheet = [task_headers] + sorted(tasks_wip, key=lambda r: (r[0], r[1], r[2]))

    out = OUT_DIR / f"eduai_metrics_{start.isoformat()}_to_{end.isoformat()}.xlsx"
    write_xlsx(
        out,
        [
            ("Notes", notes),
            ("Daywise", daywise),
            ("New_signups", new_sheet),
            ("Active_users", active_sheet),
            ("Tasks_completed", done_sheet),
            ("Tasks_WIP", wip_sheet),
        ],
    )
    print(out)
    print(f"days={len(days)} signups={sum(signups.values())} dau={len(unique_dau)} "
          f"sessions={sum(sessions.values())} sim={sum(by_day_sim.values())} "
          f"chat={sum(by_day_chat.values())} wip={len(tasks_wip)}")


if __name__ == "__main__":
    main()
