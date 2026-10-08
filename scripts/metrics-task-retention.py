#!/usr/bin/env python3
"""D0–D10 retention by signup cohort, activity = COMPLETED tasks (IST).

Usage:
  python scripts/metrics-task-retention.py
  python scripts/metrics-task-retention.py 2026-09-01 2026-10-06

Cohort = users with createdAt on that IST day (appName=eduai_app).
Dn retained = user completed ≥1 progress task on cohort_day + n.
Task day = completedAt else lastAccessedAt else updatedAt (IST).
"""

from __future__ import annotations

import json
import sys
import urllib.parse
import urllib.request
import zipfile
from collections import defaultdict
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
MAX_N = 10


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


def offset_day(day: str, n: int) -> str:
    return (datetime.strptime(day, "%Y-%m-%d") + timedelta(days=n)).strftime("%Y-%m-%d")


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


def col_letter(n: int) -> str:
    s = ""
    while n:
        n, r = divmod(n - 1, 26)
        s = chr(65 + r) + s
    return s


def cell_xml(value, col: int, row: int) -> str:
    ref = f"{col_letter(col)}{row}"
    if value is None:
        value = ""
    if isinstance(value, (int, float)) and not isinstance(value, bool):
        return f'<c r="{ref}"><v>{value}</v></c>'
    text = escape(str(value), {"'": "&apos;", '"': "&quot;"})
    return f'<c r="{ref}" t="inlineStr"><is><t xml:space="preserve">{text}</t></is></c>'


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
        sheets_xml.append(f'<sheet name="{escape(title[:31])}" sheetId="{i}" r:id="rId{i}"/>')
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
        start = today - timedelta(days=35)

    today_s = today.isoformat()
    generated = datetime.now(IST).strftime("%Y-%m-%d %H:%M IST")
    token = refresh_token()

    users: list[dict] = []
    cohort: dict[str, list[str]] = defaultdict(list)
    for doc in list_docs(token, "users"):
        f = doc.get("fields", {}) or {}
        if fv(f, "appName") != APP:
            continue
        doc_id = doc["name"].split("/")[-1]
        email = fv(f, "email") or ""
        uid = fv(f, "id") or doc_id
        day = to_day(parse_ms(fv(f, "createdAt")))
        canonical = email or uid or doc_id
        u = {"doc_id": doc_id, "uid": uid, "email": email, "canonical": canonical, "createdDay": day or ""}
        users.append(u)
        if day and start.isoformat() <= day <= end.isoformat():
            cohort[day].append(canonical)

    by_user = index_users(users)

    # user_key -> set of IST days with ≥1 COMPLETED task
    task_days: dict[str, set[str]] = defaultdict(set)
    completed_rows = 0
    for doc in list_docs(token, "progress"):
        cid = doc["name"].split("/")[-1]
        if not cid.startswith(f"{APP}_"):
            continue
        key = cid.replace(f"{APP}_", "", 1)
        u = lookup_user(by_user, key)
        user_key = (u["canonical"] if u else "") or key
        for p in list_docs(token, f"progress/{cid}/records"):
            f = p.get("fields", {}) or {}
            if (fv(f, "status") or "").upper() != "COMPLETED":
                continue
            ms = (
                parse_ms(fv(f, "completedAt"))
                or parse_ms(fv(f, "lastAccessedAt"))
                or parse_ms(fv(f, "updatedAt"))
            )
            day = to_day(ms)
            if day:
                task_days[user_key].add(day)
                completed_rows += 1

    # Also index task_days under email/uid aliases for cohort matching
    task_by_canonical: dict[str, set[str]] = defaultdict(set)
    for key, days in task_days.items():
        u = lookup_user(by_user, key)
        canon = (u["canonical"] if u else "") or key
        task_by_canonical[canon] |= days
        if u:
            for alt in (u["email"], u["uid"], u["doc_id"]):
                if alt:
                    task_by_canonical[alt] |= days
                    task_by_canonical[str(alt).lower()] |= days

    def days_for(user: str) -> set[str]:
        return task_by_canonical.get(user) or task_by_canonical.get(user.lower()) or set()

    print("=== D0–D10 retention (COMPLETED tasks) by signup cohort (IST) ===")
    print(f"Generated: {generated}")
    print(f"Cohort window: {start.isoformat()} → {end.isoformat()}")
    print(f"Completed progress rows scanned: {completed_rows}")
    print(f"Users with ≥1 completed task (any day): {len(task_days)}\n")

    header = ["Cohort", "Users"] + [f"D{n}" for n in range(MAX_N + 1)] + ["Notes"]
    print(
        f"{'Cohort':<12} {'Users':>5}  "
        + "  ".join(f"{'D'+str(n):>10}" for n in range(MAX_N + 1))
        + "  Notes"
    )
    print("-" * 150)

    table_rows: list[list] = [header]
    overall_num = [0] * (MAX_N + 1)
    overall_den = [0] * (MAX_N + 1)

    for day in sorted(cohort.keys()):
        emails = cohort[day]
        n = len(emails)
        cells: list = [day, n]
        pending = []
        line_parts = []
        for dn in range(MAX_N + 1):
            target = offset_day(day, dn)
            if target > today_s:
                cells.append("pending")
                line_parts.append(f"{'—':>10}")
                pending.append(f"D{dn}")
                continue
            count = sum(1 for e in emails if target in days_for(e))
            pct = count / n * 100 if n else 0
            cells.append(f"{count}/{n} ({pct:.1f}%)")
            line_parts.append(f"{count}/{n} ({pct:4.1f}%)".rjust(10))
            overall_num[dn] += count
            overall_den[dn] += n
        note = ("pending: " + ",".join(pending)) if pending else "complete"
        cells.append(note)
        table_rows.append(cells)
        print(f"{day:<12} {n:>5}  " + "  ".join(line_parts) + f"  {note}")

    print("\n--- Overall (eligible cohorts only; Dn day ≤ today) ---")
    overall_row = ["OVERALL", sum(len(v) for v in cohort.values())]
    for dn in range(MAX_N + 1):
        den = overall_den[dn]
        num = overall_num[dn]
        if den:
            pct = num / den * 100
            s = f"{num}/{den} ({pct:.1f}%)"
            print(f"D{dn}: {s}")
            overall_row.append(s)
        else:
            print(f"D{dn}: n/a")
            overall_row.append("n/a")
    overall_row.append("")
    table_rows.append(overall_row)

    notes = [
        ["Field", "Value"],
        ["Generated", generated],
        ["Timezone", "Asia/Kolkata (IST)"],
        ["Cohort window start", start.isoformat()],
        ["Cohort window end", end.isoformat()],
        ["Retention days", "D0..D10"],
        ["Cohort definition", "users createdAt IST day, appName=eduai_app"],
        [
            "Retention event",
            "≥1 progress record with status=COMPLETED on cohort_day+n",
        ],
        [
            "Task day",
            "completedAt else lastAccessedAt else updatedAt (IST)",
        ],
        ["Firebase project", PROJECT],
    ]

    out = OUT_DIR / f"eduai_task_retention_D0-D10_{start.isoformat()}_to_{end.isoformat()}.xlsx"
    write_xlsx(out, [("Notes", notes), ("Retention", table_rows)])
    print(f"\n{out}")


if __name__ == "__main__":
    main()
