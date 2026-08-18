#!/usr/bin/env python3
"""Generate detailed EduAI metrics HTML dashboard from Firestore."""

from __future__ import annotations

import html
import json
import sys
import time
import urllib.error
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
SIM_TYPES = {"SIMULATION", "SIMULATION_AGENT"}
DEFAULT_SINCE = "2026-07-09"
DEFAULT_OUT = Path(__file__).resolve().parent.parent / "reports" / "dashboard.html"
DEFAULT_SNAPSHOT = Path(__file__).resolve().parent.parent / "reports" / "firestore-snapshot.json"

# Internal team — excluded when "Exclude internal team" is checked in the dashboard
INTERNAL_EMAILS = frozenset(
    {
        "jeecounsela@gmail.com",
        "nkb.rgp@gmail.com",
        "check@padaams.in",
    }
)

# Cap inflated orphan sessions / screen exits in reports (matches app cleanup cap)
# Orphan sessions (app killed / left open) can span days in Firestore; cap for reporting.
MAX_REPORTED_SESSION_MS = 45 * 60 * 1000
MAX_REPORTED_SCREEN_MS = 45 * 60 * 1000

BUCKETS = [
    ("math_study", "Math study agent"),
    ("science_study", "Science study agent"),
    ("math_sim_url", "Math simulation (URL viewer)"),
    ("math_sim_agent", "Math simulation agent"),
    ("science_sim_url", "Science simulation (URL viewer)"),
    ("science_sim_agent", "Science simulation agent"),
]

FUNNEL_STEP_ORDER = [
    "login_view",
    "gmail_tap",
    "institutional_expand",
    "institutional_sign_in",
    "profile_submit",
    "home_view",
]

PAGE_SCREEN_ORDER = [
    "LOGIN",
    "USER_DETAIL_ENTRY",
    "HOME",
    "SUBJECT",
    "CHAPTER",
    "CONCEPT",
    "CONCEPT_DETAIL",
    "PROGRESS",
    "SETTINGS",
    "CHATBOT",
    "SIMULATION_LIST",
    "SIMULATION_VIEWER",
    "SIMULATION_AGENT",
    "REVISION",
    "MATH_AGENT",
    "CONTENT",
    "SIMULATION",
    "FUNNEL",
    "AD",
]

AD_INTERACTION_ORDER = [
    "SHOWN",
    "LOADED",
    "IMPRESSION",
    "CLICK",
    "OPENED",
    "CLOSED",
    "FAILED",
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


REQUEST_DELAY_SEC = 0.35


def api_get(token: str, url: str, retries: int = 12) -> dict:
    req = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})
    for attempt in range(retries):
        try:
            with urllib.request.urlopen(req, timeout=120) as resp:
                data = json.loads(resp.read().decode())
            time.sleep(REQUEST_DELAY_SEC)
            return data
        except urllib.error.HTTPError as e:
            if e.code == 429 and attempt < retries - 1:
                wait = min(120, 20 * (attempt + 1))
                print(f"Rate limited, retrying in {wait}s ({attempt + 1}/{retries})...", file=sys.stderr)
                time.sleep(wait)
                continue
            raise
    raise RuntimeError("api_get exhausted retries")


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
        if len(val) == 10 and val[4] == "-":
            return int(datetime.fromisoformat(val + "T12:00:00+00:00").timestamp() * 1000)
        try:
            return int(datetime.fromisoformat(val.replace("Z", "+00:00")).timestamp() * 1000)
        except ValueError:
            return None
    return None


def to_day_ist(ms: int | None) -> str | None:
    if ms is None:
        return None
    return datetime.fromtimestamp(ms / 1000, tz=IST).strftime("%Y-%m-%d")


def fmt_ist(ms: int | None) -> str:
    if ms is None:
        return "-"
    return datetime.fromtimestamp(ms / 1000, tz=IST).strftime("%Y-%m-%d %H:%M")


def fmt_duration(ms: int | float | None) -> str:
    if not ms or ms <= 0:
        return "0s"
    s = int(ms // 1000)
    if s < 60:
        return f"{s}s"
    m, s = divmod(s, 60)
    if m < 60:
        return f"{m}m {s}s"
    h, m = divmod(m, 60)
    return f"{h}h {m}m"


def cap_reported_duration(raw_ms: int, max_ms: int) -> tuple[int, bool]:
    if raw_ms <= max_ms:
        return raw_ms, False
    return max_ms, True


def cap_screen_exit_ms(screen: str | None, raw_ms: int) -> tuple[int, bool]:
    """Per-screen caps — simulation WebViews left open inflate duration the most."""
    screen_key = (screen or "").upper()
    if screen_key in ("SIMULATION_VIEWER", "SIMULATIONVIEWER"):
        return cap_reported_duration(raw_ms, 15 * 60 * 1000)
    if screen_key in ("CHATBOT", "MATH_AGENT", "SIMULATION_AGENT", "SIMULATIONAGENT"):
        return cap_reported_duration(raw_ms, MAX_REPORTED_SCREEN_MS)
    return cap_reported_duration(raw_ms, 20 * 60 * 1000)


def day_end_ms(day: str) -> int:
    """Exclusive end of an IST calendar day, in epoch ms."""
    start = datetime.strptime(day, "%Y-%m-%d").replace(tzinfo=IST)
    return int((start + timedelta(days=1)).timestamp() * 1000)


def resolve_closed_session_ms(
    start_ms: int | None, end_ms: int | None, duration_field: int
) -> tuple[int | None, bool]:
    """Return measurable closed-session duration, or None for orphan open sessions."""
    if not start_ms:
        return None, False
    if end_ms is None and duration_field <= 0:
        return None, False
    dur = duration_field
    if dur <= 0 and end_ms and end_ms > start_ms:
        dur = end_ms - start_ms
    if dur <= 0:
        return None, False
    return cap_reported_duration(dur, MAX_REPORTED_SESSION_MS)


def clip_session_to_start_day(start_ms: int, duration_ms: int) -> tuple[int, bool]:
    """Attribute session time only to the IST day the session started (no spill past midnight)."""
    day = to_day_ist(start_ms)
    if not day:
        return duration_ms, False
    max_in_day = max(0, day_end_ms(day) - start_ms)
    if duration_ms <= max_in_day:
        return duration_ms, False
    return max_in_day, True


def entry_users_by_day(analytics_events: list[dict]) -> dict[str, set[str]]:
    by_day: dict[str, set[str]] = defaultdict(set)
    for e in analytics_events:
        if e.get("eventType") != "ENTRY":
            continue
        day = e.get("day")
        email = e.get("email")
        if day and email:
            by_day[day].add(email)
    return by_day


def rebuild_engagement_metrics(
    analytics_events: list[dict],
    session_events: list[dict],
    since_ms: int,
    today: str,
    cohort_by_day: dict[str, list[str]] | None = None,
) -> dict:
    """Strict DAU + session time from closed sessions and screen ENTRY events only."""
    entries = entry_users_by_day(analytics_events)
    activity: dict[str, set[str]] = defaultdict(set)
    dau_detail: dict[str, dict] = defaultdict(
        lambda: {"sessions": 0, "clicks": 0, "users": set(), "sessionMs": 0, "screenMs": 0}
    )
    user_session_ms: dict[str, int] = defaultdict(int)
    day_session_ms: dict[str, int] = defaultdict(int)

    for s in session_events:
        email = s["email"]
        day = s["day"]
        dur = s.get("durationMs") or 0
        if not day or dur <= 0:
            continue
        if s.get("startMs") and s["startMs"] < since_ms:
            continue
        user_session_ms[email] += dur
        day_session_ms[day] += dur
        dau_detail[day]["sessionMs"] += dur
        dau_detail[day]["sessions"] += 1

    for e in analytics_events:
        day = e.get("day")
        email = e.get("email")
        if not day or not email:
            continue
        if e.get("sinceWindow") and e.get("eventType") == "CLICK":
            dau_detail[day]["clicks"] += 1

    all_days = set(dau_detail.keys()) | set(entries.keys())
    for day in all_days:
        users = set(entries.get(day, set()))
        for s in session_events:
            if s.get("day") == day and (s.get("durationMs") or 0) > 0:
                users.add(s["email"])
        dau_detail[day]["users"] = users
        for email in users:
            activity[email].add(day)

    dau_days = []
    for day in sorted(all_days)[-30:]:
        info = dau_detail[day]
        dau_days.append(
            {
                "day": day,
                "users": len(info["users"]),
                "userList": sorted(info["users"]),
                "sessions": info["sessions"],
                "clicks": info["clicks"],
                "sessionMs": info["sessionMs"],
                "screenMs": info["screenMs"],
            }
        )

    cohorts = []
    if cohort_by_day:
        for day in sorted(cohort_by_day.keys())[-20:]:
            emails = cohort_by_day[day]
            ret = {}
            for off in (1, 2, 7, 30):
                target = offset_day(day, off)
                active = sum(1 for e in emails if target in activity.get(e, set()))
                ret[f"d{off}"] = {
                    "active": active,
                    "pct": round(active / len(emails) * 100, 1) if emails else 0,
                    "pending": target > today,
                }
            cohorts.append({"day": day, "n": len(emails), "emails": sorted(emails), "ret": ret})

    active_emails = set(activity.keys())
    return {
        "activity": {email: sorted(days) for email, days in activity.items()},
        "dauDays": dau_days,
        "userSessionMs": dict(user_session_ms),
        "daySessionMs": dict(day_session_ms),
        "todayDau": len(dau_detail.get(today, {}).get("users", set())),
        "activeUsers": len(active_emails),
        "cohorts": cohorts,
    }


def ad_action(raw: str) -> str:
    return (raw or "?").split("|")[0]


def summarize_analytics(events: list[dict]) -> dict:
    page_visits: Counter[str] = Counter()
    page_clicks: Counter[str] = Counter()
    page_users: dict[str, set[str]] = defaultdict(set)
    funnel_events: Counter[str] = Counter()
    funnel_user_sets: dict[str, set[str]] = defaultdict(set)
    user_funnel: dict[str, Counter[str]] = defaultdict(Counter)
    user_page_visits: dict[str, Counter[str]] = defaultdict(Counter)
    user_page_clicks: dict[str, Counter[str]] = defaultdict(Counter)
    click_types: Counter[str] = Counter()
    ad_events: Counter[tuple[str, str, str]] = Counter()  # (ad_type, placement, action)
    ad_users: dict[tuple[str, str, str], set[str]] = defaultdict(set)
    user_ads: dict[str, Counter[tuple[str, str, str]]] = defaultdict(Counter)

    for e in events:
        if not e.get("sinceWindow"):
            continue
        email = e["email"]
        et = e.get("eventType") or ""
        screen = e.get("screenName") or "?"
        interaction = e.get("interactionType") or ""
        ad_type = e.get("adType") or "?"
        placement = e.get("adPlacement") or "?"

        if et == "AD" or screen == "AD":
            action = ad_action(interaction)
            key = (ad_type, placement, action)
            ad_events[key] += 1
            ad_users[key].add(email)
            user_ads[email][key] += 1
        elif et == "CLICK":
            click_types[f"{screen}/{interaction or '?'}"] += 1
            page_clicks[screen] += 1
            user_page_clicks[email][screen] += 1
        elif et == "FUNNEL":
            step = interaction or "?"
            funnel_events[step] += 1
            funnel_user_sets[step].add(email)
            user_funnel[email][step] += 1
        elif et == "ENTRY":
            page_visits[screen] += 1
            page_users[screen].add(email)
            user_page_visits[email][screen] += 1

    return {
        "pageVisits": page_visits,
        "pageClicks": page_clicks,
        "pageUserCounts": {k: len(v) for k, v in page_users.items()},
        "funnelEvents": funnel_events,
        "funnelUserCounts": {k: len(v) for k, v in funnel_user_sets.items()},
        "userFunnel": {k: dict(v) for k, v in user_funnel.items()},
        "userPageVisits": {k: dict(v) for k, v in user_page_visits.items()},
        "userPageClicks": {k: dict(v) for k, v in user_page_clicks.items()},
        "adEvents": {f"{a}|{p}|{i}": c for (a, p, i), c in ad_events.items()},
        "adUserCounts": {f"{a}|{p}|{i}": len(v) for (a, p, i), v in ad_users.items()},
        "userAds": {
            email: {f"{a}|{p}|{i}": cnt for (a, p, i), cnt in counts.items()}
            for email, counts in user_ads.items()
        },
        "clickTypes": click_types.most_common(20),
        "funnelSteps": funnel_events.most_common(),
    }


def ordered_keys(counter: Counter[str], preferred: list[str]) -> list[str]:
    keys = set(counter.keys())
    ordered = [k for k in preferred if k in keys]
    ordered.extend(sorted(keys - set(ordered)))
    return ordered


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
    for key in (
        "completedAt",
        "updatedAt",
        "lastAccessedAt",
        "createdAt",
        "entryTime",
        "sessionStartTime",
        "sessionDate",
    ):
        ms = parse_ms(fv(fields, key))
        if ms:
            return ms
    return None


def offset_day(day: str, n: int) -> str:
    return (datetime.strptime(day, "%Y-%m-%d") + timedelta(days=n)).strftime("%Y-%m-%d")


def is_science(meta: dict) -> bool:
    return "science" in (meta.get("subjectName") or "").lower()


def is_math(meta: dict) -> bool:
    return "math" in (meta.get("subjectName") or "").lower()


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
    if t == "SIMULATION":
        return "simulation_url"
    if t == "SIMULATION_AGENT":
        return "simulation_agent"
    return t.lower()


def progress_bucket(row: dict) -> str | None:
    item_type = row["itemType"]
    science = row["science"]
    math = is_math(row)
    if item_type in STUDY_TYPES:
        if science:
            return "science_study"
        if math:
            return "math_study"
        return None
    if item_type in SIM_TYPES:
        if science:
            return "science_sim_agent" if item_type == "SIMULATION_AGENT" else "science_sim_url"
        if math:
            return "math_sim_agent" if item_type == "SIMULATION_AGENT" else "math_sim_url"
    return None


def time_bucket(screen: str | None, meta: dict) -> str | None:
    screen = (screen or "").upper()
    science = is_science(meta)
    math = is_math(meta)
    if screen == "MATH_AGENT":
        return "math_study"
    if screen == "CHATBOT":
        if science:
            return "science_study"
        if math:
            return "math_study"
        return None
    if screen in ("SIMULATION_VIEWER", "SIMULATIONVIEWER"):
        if science:
            return "science_sim_url"
        if math:
            return "math_sim_url"
    if screen in ("SIMULATION_AGENT", "SIMULATIONAGENT"):
        if science:
            return "science_sim_agent"
        if math:
            return "math_sim_agent"
    return None


def esc(s) -> str:
    return html.escape(str(s) if s is not None else "")


def status_pill(status: str) -> str:
    cls = "pill ok" if status == "COMPLETED" else "pill warn"
    return f'<span class="{cls}">{esc(status)}</span>'


def table(headers: list[str], rows: list[list], empty: str = "No data") -> str:
    if not rows:
        return f'<p class="empty">{esc(empty)}</p>'
    head = "".join(f"<th>{esc(h)}</th>" for h in headers)
    body = "".join("<tr>" + "".join(f"<td>{cell}</td>" for cell in row) + "</tr>" for row in rows)
    return f"<table><thead><tr>{head}</tr></thead><tbody>{body}</tbody></table>"


def card(val, label) -> str:
    return f'<div class="card"><div class="val">{esc(val)}</div><div class="lbl">{esc(label)}</div></div>'


def section(title: str, body: str, note: str = "", anchor: str = "") -> str:
    note_html = f'<p class="note">{note}</p>' if note else ""
    id_attr = f' id="{anchor}"' if anchor else ""
    return f"<section{id_attr}><h2>{esc(title)}</h2>{note_html}{body}</section>"


def details(summary: str, body: str) -> str:
    return f"<details><summary>{esc(summary)}</summary><div class='details-body'>{body}</div></details>"


def load_catalog(token: str) -> dict[str, dict]:
    catalog: dict[str, dict] = {}
    for doc in list_collection(token, "Concept"):
        f = doc.get("fields", {})
        concept_id = doc["name"].split("/")[-1]
        catalog[concept_id] = {
            "conceptName": fv(f, "concept_name") or concept_id[:8],
            "conceptType": fv(f, "type") or "?",
            "chapterName": fv(f, "unit_name") or "Unknown chapter",
            "subjectName": fv(f, "subject_name") or "Unknown subject",
        }
    return catalog


def collect_data(token: str, since: str) -> dict:
    since_ms = int(datetime.strptime(since, "%Y-%m-%d").replace(tzinfo=IST).timestamp() * 1000)
    today = datetime.now(IST).date().isoformat()
    catalog = load_catalog(token)
    empty_meta = {"conceptName": "-", "chapterName": "-", "subjectName": "-", "conceptType": "?"}

    users: list[dict] = []
    user_profiles: dict[str, dict] = {}
    cohort_by_day: dict[str, list[str]] = defaultdict(list)
    for doc in list_collection(token, "users"):
        f = doc.get("fields", {})
        email = fv(f, "email") or doc["name"].split("/")[-1]
        created_ms = parse_ms(fv(f, "createdAt"))
        day = to_day_ist(created_ms)
        profile = {
            "email": email,
            "name": fv(f, "name") or fv(f, "displayName") or "-",
            "schoolName": fv(f, "schoolName") or "-",
            "studentClass": fv(f, "studentClass") or "-",
            "phoneNumber": fv(f, "phoneNumber") or "-",
            "language": norm_lang(fv(f, "language")),
            "signupDay": day,
            "signupAt": fmt_ist(created_ms),
        }
        user_profiles[email] = profile
        if fv(f, "appName") != APP:
            continue
        users.append(profile)
        if day:
            cohort_by_day[day].append(email)

    sessions_total = 0
    skipped_open_sessions = 0
    clicks_total = 0
    funnel_steps: Counter[str] = Counter()
    click_types: Counter[str] = Counter()
    analytics_events: list[dict] = []

    progress_rows: list[dict] = []
    time_rows: list[dict] = []
    session_events: list[dict] = []
    capped_sessions = 0
    day_clipped_sessions = 0
    capped_screen_events = 0

    for doc in list_collection(token, "sessions"):
        cid = doc["name"].split("/")[-1]
        if not cid.startswith(f"{APP}_"):
            continue
        email = email_from_container(cid)
        for s in list_sub(token, f"sessions/{cid}", "records"):
            f = s.get("fields", {})
            start_ms = parse_ms(fv(f, "sessionStartTime")) or parse_ms(fv(f, "sessionDate"))
            end_ms = parse_ms(fv(f, "sessionEndTime"))
            dur_field = parse_ms(fv(f, "durationMillis")) or 0
            sessions_total += 1
            dur, was_capped = resolve_closed_session_ms(start_ms, end_ms, dur_field)
            if dur is None:
                skipped_open_sessions += 1
                continue
            if was_capped:
                capped_sessions += 1
            dur, was_day_clipped = clip_session_to_start_day(start_ms, dur)
            if was_day_clipped:
                day_clipped_sessions += 1
            day = to_day_ist(start_ms)
            if start_ms and start_ms >= since_ms and dur > 0 and day:
                session_events.append(
                    {
                        "email": email,
                        "day": day,
                        "startMs": start_ms,
                        "durationMs": dur,
                    }
                )

    for doc in list_collection(token, "analytics"):
        cid = doc["name"].split("/")[-1]
        if not cid.startswith(f"{APP}_"):
            continue
        email = email_from_container(cid)
        for e in list_sub(token, f"analytics/{cid}", "events"):
            f = e.get("fields", {})
            ms = parse_ms(fv(f, "entryTime"))
            day = to_day_ist(ms)
            et = fv(f, "eventType")
            screen = fv(f, "screenName")
            dur = parse_ms(fv(f, "durationMillis")) or 0
            interaction = fv(f, "interactionType") or ""
            in_window = ms is not None and ms >= since_ms
            analytics_events.append(
                {
                    "email": email,
                    "day": day,
                    "ms": ms,
                    "eventType": et or "",
                    "screenName": screen or "",
                    "interactionType": interaction,
                    "adType": fv(f, "conceptId") or "",
                    "adPlacement": fv(f, "source") or "",
                    "sinceWindow": in_window,
                }
            )
            if in_window and et == "CLICK":
                clicks_total += 1
                click_types[f"{screen}/{interaction or '?'}"] += 1
            if in_window and et == "FUNNEL":
                funnel_steps[interaction or fv(f, "conceptId") or "?"] += 1
            if in_window and dur > 0 and et == "EXIT":
                dur, was_capped = cap_screen_exit_ms(screen, dur)
                if was_capped:
                    capped_screen_events += 1
                concept_id = fv(f, "conceptId")
                meta = catalog.get(concept_id, empty_meta) if concept_id else empty_meta
                bucket = time_bucket(screen, meta)
                if bucket:
                    time_rows.append(
                        {
                            "email": email,
                            "day": day,
                            "durationMs": dur,
                            "bucket": bucket,
                            "screenName": screen,
                            "conceptId": concept_id or "-",
                            "conceptName": meta.get("conceptName", "-"),
                            "chapterName": meta.get("chapterName", "-"),
                            "subjectName": meta.get("subjectName", "-"),
                        }
                    )

    for doc in list_collection(token, "progress"):
        cid = doc["name"].split("/")[-1]
        if not cid.startswith(f"{APP}_"):
            continue
        email = email_from_container(cid)
        for p in list_sub(token, f"progress/{cid}", "records"):
            f = p.get("fields", {})
            ms = record_ts(f)
            day = to_day_ist(ms)
            item_type = (fv(f, "itemType") or "?").upper()
            item_id = fv(f, "itemId") or "?"
            meta = catalog.get(item_id, empty_meta)
            row = {
                "email": email,
                "day": day,
                "ts": ms,
                "language": norm_lang(fv(f, "language")),
                "itemType": item_type,
                "status": (fv(f, "status") or "?").upper(),
                "itemId": item_id,
                "conceptName": meta.get("conceptName", item_id[:8]),
                "chapterName": meta.get("chapterName", "?"),
                "subjectName": meta.get("subjectName", "?"),
                "conceptType": meta.get("conceptType", "?"),
                "science": is_science(meta),
                "agent": agent_label(item_type, meta.get("conceptType")),
                "sinceWindow": ms is not None and ms >= since_ms,
            }
            row["bucket"] = progress_bucket(row)
            progress_rows.append(row)

    since_progress = [r for r in progress_rows if r["sinceWindow"] and r["bucket"]]

    screen_ms_by_day: dict[str, int] = defaultdict(int)
    for row in time_rows:
        if row.get("day"):
            screen_ms_by_day[row["day"]] += row.get("durationMs") or 0

    engagement = rebuild_engagement_metrics(
        analytics_events, session_events, since_ms, today, cohort_by_day
    )
    for day, ms in screen_ms_by_day.items():
        for d in engagement["dauDays"]:
            if d["day"] == day:
                d["screenMs"] = ms
                break
        else:
            engagement["dauDays"].append(
                {
                    "day": day,
                    "users": 0,
                    "userList": [],
                    "sessions": 0,
                    "clicks": 0,
                    "sessionMs": 0,
                    "screenMs": ms,
                }
            )
    engagement["dauDays"] = sorted(engagement["dauDays"], key=lambda x: x["day"])[-30:]

    analytics_summary = summarize_analytics(analytics_events)

    return {
        "generatedAt": datetime.now(IST).strftime("%Y-%m-%d %H:%M IST"),
        "since": since,
        "today": today,
        "totalUsers": len(users),
        "activeUsers": engagement["activeUsers"],
        "todayDau": engagement["todayDau"],
        "sessionsTotal": sessions_total,
        "skippedOpenSessions": skipped_open_sessions,
        "clicksTotal": clicks_total,
        "users": sorted(users, key=lambda u: u.get("signupDay") or ""),
        "userProfiles": user_profiles,
        "dauDays": engagement["dauDays"],
        "cohorts": engagement["cohorts"],
        "clickTypes": analytics_summary["clickTypes"],
        "funnelSteps": analytics_summary["funnelSteps"],
        "analyticsEvents": analytics_events,
        "pageVisits": dict(analytics_summary["pageVisits"]),
        "pageClicks": dict(analytics_summary["pageClicks"]),
        "pageUserCounts": analytics_summary["pageUserCounts"],
        "funnelEvents": dict(analytics_summary["funnelEvents"]),
        "funnelUserCounts": analytics_summary["funnelUserCounts"],
        "userFunnel": analytics_summary["userFunnel"],
        "userPageVisits": analytics_summary["userPageVisits"],
        "userPageClicks": analytics_summary["userPageClicks"],
        "adEvents": analytics_summary["adEvents"],
        "adUserCounts": analytics_summary["adUserCounts"],
        "userAds": analytics_summary["userAds"],
        "progressRows": since_progress,
        "timeRows": time_rows,
        "sessionEvents": session_events,
        "userSessionMs": engagement["userSessionMs"],
        "daySessionMs": engagement["daySessionMs"],
        "activity": engagement["activity"],
        "catalogSize": len(catalog),
        "internalEmails": sorted(INTERNAL_EMAILS),
        "cappedSessions": capped_sessions,
        "dayClippedSessions": day_clipped_sessions,
        "cappedScreenEvents": capped_screen_events,
    }


def collect_data_from_snapshot(raw: dict, since: str) -> dict:
    data = dict(raw)
    data["since"] = since
    data["generatedAt"] = datetime.now(IST).strftime("%Y-%m-%d %H:%M IST")
    since_ms = int(datetime.strptime(since, "%Y-%m-%d").replace(tzinfo=IST).timestamp() * 1000)
    today = data.get("today") or datetime.now(IST).date().isoformat()
    cohort_by_day: dict[str, list[str]] = defaultdict(list)
    for u in data.get("users", []):
        day = u.get("signupDay")
        email = u.get("email")
        if day and email:
            cohort_by_day[day].append(email)
    engagement = rebuild_engagement_metrics(
        data.get("analyticsEvents", []),
        data.get("sessionEvents", []),
        since_ms,
        today,
        dict(cohort_by_day),
    )
    screen_ms_by_day: dict[str, int] = defaultdict(int)
    for row in data.get("timeRows", []):
        if row.get("day"):
            screen_ms_by_day[row["day"]] += row.get("durationMs") or 0
    for d in engagement["dauDays"]:
        d["screenMs"] = screen_ms_by_day.get(d["day"], d.get("screenMs", 0))
    data.update(engagement)
    return data


def is_external(email: str, exclude_internal: bool) -> bool:
    if not exclude_internal:
        return True
    return email not in INTERNAL_EMAILS


def filter_data(data: dict, exclude_internal: bool) -> dict:
    if not exclude_internal:
        return data

    progress = [r for r in data["progressRows"] if is_external(r["email"], True)]
    times = [r for r in data["timeRows"] if is_external(r["email"], True)]
    sessions = [s for s in data["sessionEvents"] if is_external(s["email"], True)]
    user_sess = {k: v for k, v in data["userSessionMs"].items() if is_external(k, True)}
    analytics_events = [e for e in data.get("analyticsEvents", []) if is_external(e["email"], True)]
    analytics_summary = summarize_analytics(analytics_events)

    since_ms = int(
        datetime.strptime(data["since"], "%Y-%m-%d").replace(tzinfo=IST).timestamp() * 1000
    )
    cohort_by_day: dict[str, list[str]] = defaultdict(list)
    for u in data.get("users", []):
        if is_external(u["email"], True) and u.get("signupDay"):
            cohort_by_day[u["signupDay"]].append(u["email"])

    engagement = rebuild_engagement_metrics(
        analytics_events,
        sessions,
        since_ms,
        data["today"],
        dict(cohort_by_day),
    )
    screen_ms_by_day: dict[str, int] = defaultdict(int)
    for row in times:
        if row.get("day"):
            screen_ms_by_day[row["day"]] += row.get("durationMs") or 0
    for d in engagement["dauDays"]:
        d["screenMs"] = screen_ms_by_day.get(d["day"], 0)

    dau_days = engagement["dauDays"]
    today = data["today"]
    today_users = next((x["userList"] for x in dau_days if x["day"] == today), [])

    signup_users = [u for u in data["users"] if is_external(u["email"], True)]
    user_profiles = {
        k: v for k, v in data.get("userProfiles", {}).items() if is_external(k, True)
    }
    active_emails = set(engagement["activity"].keys())

    return {
        **data,
        "progressRows": progress,
        "timeRows": times,
        "sessionEvents": sessions,
        "userSessionMs": user_sess,
        "dauDays": dau_days,
        "cohorts": engagement["cohorts"],
        "users": signup_users,
        "userProfiles": user_profiles,
        "totalUsers": len(signup_users),
        "activeUsers": len(active_emails),
        "todayDau": len(today_users),
        "clicksTotal": sum(d["clicks"] for d in dau_days if d["day"] >= data["since"]),
        "clickTypes": analytics_summary["clickTypes"],
        "funnelSteps": analytics_summary["funnelSteps"],
        "analyticsEvents": analytics_events,
        "pageVisits": dict(analytics_summary["pageVisits"]),
        "pageClicks": dict(analytics_summary["pageClicks"]),
        "pageUserCounts": analytics_summary["pageUserCounts"],
        "funnelEvents": dict(analytics_summary["funnelEvents"]),
        "funnelUserCounts": analytics_summary["funnelUserCounts"],
        "userFunnel": analytics_summary["userFunnel"],
        "userPageVisits": analytics_summary["userPageVisits"],
        "userPageClicks": analytics_summary["userPageClicks"],
        "adEvents": analytics_summary["adEvents"],
        "adUserCounts": analytics_summary["adUserCounts"],
        "userAds": analytics_summary["userAds"],
        "activity": engagement["activity"],
        "daySessionMs": engagement["daySessionMs"],
        "filteredExternal": True,
    }


def sum_time(rows: list[dict]) -> int:
    return sum(r.get("durationMs", 0) for r in rows)


def profile_for(data: dict, email: str) -> dict:
    profiles = data.get("userProfiles") or {}
    if email in profiles:
        return profiles[email]
    for u in data.get("users", []):
        if u["email"] == email:
            return u
    return {
        "email": email,
        "name": "-",
        "schoolName": "-",
        "studentClass": "-",
        "phoneNumber": "-",
        "language": "-",
        "signupDay": "-",
        "signupAt": "-",
    }


def render_page_funnel_sections(data: dict) -> str:
    page_visits = Counter(data.get("pageVisits") or {})
    page_clicks = Counter(data.get("pageClicks") or {})
    page_user_counts = data.get("pageUserCounts") or {}
    all_screens = ordered_keys(page_visits + page_clicks, PAGE_SCREEN_ORDER)

    page_rows = []
    for screen in all_screens:
        if not page_visits[screen] and not page_clicks[screen]:
            continue
        page_rows.append(
            [
                esc(screen),
                str(page_visits[screen]),
                str(page_clicks[screen]),
                str(page_user_counts.get(screen, 0)),
            ]
        )

    funnel_events = Counter(data.get("funnelEvents") or {})
    funnel_user_counts = data.get("funnelUserCounts") or {}
    funnel_rows = []
    for step in ordered_keys(funnel_events, FUNNEL_STEP_ORDER):
        funnel_rows.append(
            [
                esc(step),
                str(funnel_events[step]),
                str(funnel_user_counts.get(step, 0)),
            ]
        )

    return (
        section(
            "Page visits & clicks",
            table(["Screen / page", "Visits (ENTRY)", "Clicks (CLICK)", "Unique users (visits)"], page_rows),
            "Visits = screen ENTRY events. Clicks = CLICK events on that screen (CONTENT, SIMULATION, etc.).",
            "pages",
        )
        + section(
            "Funnel visits & clicks",
            table(["Funnel step", "Events", "Unique users"], funnel_rows),
            "Onboarding funnel (screenName=FUNNEL). Each row is a funnel step event since the launch window.",
            "funnel",
        )
    )


def render_ad_sections(data: dict) -> str:
    ad_events = data.get("adEvents") or {}
    ad_user_counts = data.get("adUserCounts") or {}
    user_ads = data.get("userAds") or {}

    if not ad_events:
        return section(
            "Ads (views, clicks, type)",
            '<p class="empty">No ad events in this window yet. Ad tracking ships in the next app build.</p>',
            "Tracks banner SHOWN, LOADED, IMPRESSION, CLICK, OPENED, CLOSED, FAILED via screenName=AD.",
            "ads",
        )

    summary_rows = []
    keys = sorted(ad_events.keys(), key=lambda k: (-ad_events[k], k))
    for key in keys:
        ad_type, placement, action = key.split("|", 2)
        summary_rows.append(
            [
                esc(ad_type),
                esc(placement),
                esc(action),
                str(ad_events[key]),
                str(ad_user_counts.get(key, 0)),
            ]
        )

    cards = "".join(
        [
            card(sum(v for k, v in ad_events.items() if k.endswith("|IMPRESSION")), "Impressions"),
            card(sum(v for k, v in ad_events.items() if k.endswith("|CLICK")), "Ad clicks"),
            card(sum(v for k, v in ad_events.items() if k.endswith("|SHOWN")), "Dialogs shown"),
            card(sum(v for k, v in ad_events.items() if k.endswith("|FAILED")), "Load failures"),
        ]
    )

    user_blocks = []
    for email in sorted(user_ads, key=str.lower):
        p = profile_for(data, email)
        rows = []
        for key in sorted(user_ads[email], key=lambda k: (-user_ads[email][k], k)):
            ad_type, placement, action = key.split("|", 2)
            rows.append(
                [
                    esc(ad_type),
                    esc(placement),
                    esc(action),
                    str(user_ads[email][key]),
                ]
            )
        user_blocks.append(
            details(
                f"{email} · {p.get('name', '-')} · class {p.get('studentClass', '-')}",
                table(["Ad type", "Placement", "Event", "Count"], rows),
            )
        )

    return section(
        "Ads (views, clicks, type)",
        f'<div class="grid">{cards}</div>'
        + table(
            ["Ad type", "Placement", "Event", "Total", "Unique users"],
            summary_rows,
        )
        + "<h4>Email-wise ad activity</h4>"
        + ("".join(user_blocks) or '<p class="empty">No per-user ad data.</p>'),
        "Banner ads in the click-gate dialog. IMPRESSION = AdMob view; CLICK = user tapped the ad.",
        "ads",
    )


def render_user_directory(data: dict) -> str:
    profiles = data.get("userProfiles") or {u["email"]: u for u in data.get("users", [])}
    emails = sorted(
        set(profiles.keys())
        | set(data.get("userSessionMs", {}).keys())
        | set(data.get("userFunnel", {}).keys())
        | set(data.get("userPageVisits", {}).keys())
        | set(data.get("userAds", {}).keys())
        | {u["email"] for u in data.get("users", [])},
        key=str.lower,
    )

    summary_rows = []
    detail_blocks = []
    user_funnel = data.get("userFunnel") or {}
    user_page_visits = data.get("userPageVisits") or {}
    user_page_clicks = data.get("userPageClicks") or {}
    user_ads = data.get("userAds") or {}

    for email in emails:
        p = profile_for(data, email)
        if (p.get("signupDay") or "-") < data["since"] and email not in data.get("userSessionMs", {}):
            if email not in user_funnel and email not in user_page_visits:
                continue

        funnel_hits = sum(user_funnel.get(email, {}).values())
        page_v = sum(user_page_visits.get(email, {}).values())
        page_c = sum(user_page_clicks.get(email, {}).values())
        ad_impressions = sum(
            v for k, v in user_ads.get(email, {}).items() if k.endswith("|IMPRESSION")
        )
        ad_clicks = sum(v for k, v in user_ads.get(email, {}).items() if k.endswith("|CLICK"))
        summary_rows.append(
            [
                esc(email),
                esc(p.get("name", "-")),
                esc(p.get("schoolName", "-")),
                esc(str(p.get("studentClass", "-"))),
                esc(p.get("phoneNumber", "-")),
                esc(p.get("signupDay", "-")),
                str(funnel_hits),
                str(page_v),
                str(page_c),
                str(ad_impressions),
                str(ad_clicks),
                fmt_duration(data.get("userSessionMs", {}).get(email, 0)),
            ]
        )

        funnel_detail = []
        for step in ordered_keys(Counter(user_funnel.get(email, {})), FUNNEL_STEP_ORDER):
            funnel_detail.append([esc(step), str(user_funnel[email][step])])
        page_detail = []
        screens = set(user_page_visits.get(email, {})) | set(user_page_clicks.get(email, {}))
        for screen in ordered_keys(
            Counter({s: user_page_visits.get(email, {}).get(s, 0) for s in screens}),
            PAGE_SCREEN_ORDER,
        ):
            page_detail.append(
                [
                    esc(screen),
                    str(user_page_visits.get(email, {}).get(screen, 0)),
                    str(user_page_clicks.get(email, {}).get(screen, 0)),
                ]
            )

        ad_detail = []
        for key in sorted(user_ads.get(email, {}), key=lambda k: (-user_ads[email][k], k)):
            ad_type, placement, action = key.split("|", 2)
            ad_detail.append(
                [
                    esc(ad_type),
                    esc(placement),
                    esc(action),
                    str(user_ads[email][key]),
                ]
            )

        body = (
            f"<p><strong>School:</strong> {esc(p.get('schoolName', '-'))} · "
            f"<strong>Class:</strong> {esc(str(p.get('studentClass', '-')))} · "
            f"<strong>Phone:</strong> {esc(p.get('phoneNumber', '-'))} · "
            f"<strong>Lang:</strong> {esc(p.get('language', '-'))} · "
            f"<strong>Signed up:</strong> {esc(p.get('signupAt', '-'))}</p>"
            + "<h4>Funnel steps</h4>"
            + table(["Step", "Events"], funnel_detail, "No funnel events in window.")
            + "<h4>Page visits / clicks</h4>"
            + table(["Screen", "Visits", "Clicks"], page_detail, "No page events in window.")
            + "<h4>Ad events</h4>"
            + table(["Ad type", "Placement", "Event", "Count"], ad_detail, "No ad events in window.")
        )
        detail_blocks.append(
            details(
                f"{email} · {p.get('name', '-')} · class {p.get('studentClass', '-')} · {p.get('schoolName', '-')}",
                body,
            )
        )

    return section(
        "Users (email-wise profile + funnel/pages)",
        table(
            [
                "Email",
                "Name",
                "School",
                "Class",
                "Phone",
                "Signup day",
                "Funnel events",
                "Page visits",
                "Page clicks",
                "Ad views",
                "Ad clicks",
                "Session time",
            ],
            summary_rows,
        )
        + "".join(detail_blocks),
        "Profile from Firestore users collection. Expand a row for funnel step and page breakdown per user.",
        "users",
    )


def render_bucket_section(
    bucket_key: str,
    bucket_label: str,
    progress_rows: list[dict],
    time_rows: list[dict],
) -> str:
    rows = [r for r in progress_rows if r["bucket"] == bucket_key]
    times = [r for r in time_rows if r["bucket"] == bucket_key]
    completed = [r for r in rows if r["status"] == "COMPLETED"]
    in_prog = [r for r in rows if r["status"] == "IN_PROGRESS"]

    cards = "".join(
        [
            card(len(completed), "Completed"),
            card(len(in_prog), "In progress"),
            card(len({r["email"] for r in rows}), "Users"),
            card(fmt_duration(sum_time(times)), "Screen time"),
        ]
    )

    day_rows = []
    for day in sorted({r["day"] for r in rows} | {r["day"] for r in times if r["day"]}):
        d_rows = [r for r in rows if r["day"] == day]
        d_times = [r for r in times if r["day"] == day]
        day_rows.append(
            [
                esc(day),
                str(sum(1 for r in d_rows if r["status"] == "COMPLETED")),
                str(sum(1 for r in d_rows if r["status"] == "IN_PROGRESS")),
                str(len({r["email"] for r in d_rows})),
                fmt_duration(sum_time(d_times)),
                esc(
                    ", ".join(
                        sorted({r["conceptName"] for r in d_rows})[:8]
                    )
                    + ("…" if len({r["conceptName"] for r in d_rows}) > 8 else "")
                ),
            ]
        )

    user_map: dict[str, list[dict]] = defaultdict(list)
    for r in rows:
        user_map[r["email"]].append(r)
    user_time: dict[str, int] = defaultdict(int)
    for t in times:
        user_time[t["email"]] += t["durationMs"]

    user_detail_blocks = []
    for email in sorted(user_map, key=lambda e: (-len(user_map[e]), e)):
        urows = user_map[email]
        detail_rows = []
        seen = set()
        for r in sorted(urows, key=lambda x: (x["day"], x["conceptName"], x["status"])):
            key = (r["day"], r["itemId"], r["status"], r["agent"])
            if key in seen:
                continue
            seen.add(key)
            concept_times = [
                t for t in times if t["email"] == email and t.get("conceptId") == r["itemId"]
            ]
            detail_rows.append(
                [
                    esc(r["day"]),
                    status_pill(r["status"]),
                    esc(r["chapterName"]),
                    esc(r["conceptName"]),
                    f'<span class="pill">{esc(r["agent"])}</span>',
                    esc(r["language"]),
                    fmt_duration(sum_time(concept_times)),
                ]
            )
        body = (
            f"<p>Completed: {sum(1 for x in urows if x['status']=='COMPLETED')} · "
            f"In progress: {sum(1 for x in urows if x['status']=='IN_PROGRESS')} · "
            f"Time: {fmt_duration(user_time.get(email, 0))}</p>"
            + table(
                ["Day", "Status", "Chapter", "Concept", "Agent", "Lang", "Time on screen"],
                detail_rows,
            )
        )
        user_detail_blocks.append(
            details(
                f"{email} — {len(urows)} records · {fmt_duration(user_time.get(email, 0))}",
                body,
            )
        )

    concept_time: dict[str, int] = defaultdict(int)
    for t in times:
        concept_time[t.get("conceptId", "-")] += t["durationMs"]

    concept_rows = []
    groups: dict[str, list[dict]] = defaultdict(list)
    for r in rows:
        groups[r["itemId"]].append(r)
    for item_id, crows in sorted(groups.items(), key=lambda kv: (-len(kv[1]), kv[1][0]["conceptName"])):
        s = crows[0]
        concept_rows.append(
            [
                esc(s["conceptName"]),
                esc(s["chapterName"]),
                str(sum(1 for x in crows if x["status"] == "COMPLETED")),
                str(sum(1 for x in crows if x["status"] == "IN_PROGRESS")),
                esc(", ".join(sorted({x["email"] for x in crows}))),
                fmt_duration(concept_time.get(item_id, 0)),
            ]
        )

    return (
        f'<div class="bucket-block" id="{bucket_key}">'
        f"<h3>{esc(bucket_label)}</h3>"
        f'<div class="grid">{cards}</div>'
        f"<h4>Day-wise</h4>"
        + table(
            ["Day", "Completed", "In progress", "Users", "Screen time", "Concepts touched"],
            day_rows,
        )
        + f"<h4>User-wise</h4>"
        + ("".join(user_detail_blocks) or '<p class="empty">No user records.</p>')
        + f"<h4>Concept-wise</h4>"
        + table(
            ["Concept", "Chapter", "Completed", "In progress", "Users", "Screen time"],
            concept_rows,
        )
        + "</div>"
    )


def render_main_content(data: dict) -> str:
    progress_rows = data["progressRows"]
    time_rows = data["timeRows"]

    total_session_ms = sum(data["userSessionMs"].values())
    total_learning_ms = sum_time(time_rows)
    session_users = len(data["userSessionMs"])
    learning_users = len({t["email"] for t in time_rows})
    avg_learning_ms = total_learning_ms // learning_users if learning_users else 0
    cap_min = MAX_REPORTED_SESSION_MS // 60000
    capped_note = ""
    if data.get("cappedSessions") or data.get("cappedScreenEvents") or data.get("dayClippedSessions"):
        capped_note = (
            f" Caps: {cap_min} min/session; 45 min tutor/sim exit; 15 min simulation WebView exit."
            f" Skipped {data.get('skippedOpenSessions', 0)} open orphan sessions;"
            f" {data.get('cappedSessions', 0)} session caps,"
            f" {data.get('cappedScreenEvents', 0)} screen caps."
        )
    time_note = (
        "Active learning time = tutor + simulation screen EXIT events only (best engagement signal). "
        "App open time uses session records and can over-count when the app was left open in the background."
        + capped_note
    )
    dau_note = (
        "DAU = unique users with a closed session that day OR at least one screen ENTRY that day. "
        "Open orphan sessions, progress sync timestamps, and click/funnel-only pings do not count."
    )

    dau_rows = []
    for d in data["dauDays"]:
        if d["day"] < data["since"]:
            continue
        user_details = details(
            f"View {d['users']} users",
            "<ul>" + "".join(f"<li>{esc(u)}</li>" for u in d["userList"]) + "</ul>",
        )
        dau_rows.append(
            [
                esc(d["day"]),
                str(d["users"]),
                user_details,
                str(d["sessions"]),
                str(d["clicks"]),
                fmt_duration(d["sessionMs"]),
                fmt_duration(d["screenMs"]),
            ]
        )

    learning_by_user: dict[str, int] = defaultdict(int)
    for t in time_rows:
        learning_by_user[t["email"]] += t["durationMs"]

    time_user_rows = []
    for email in sorted(
        set(data["userSessionMs"]) | set(learning_by_user),
        key=lambda e: (-learning_by_user.get(e, 0), -data["userSessionMs"].get(e, 0)),
    ):
        time_user_rows.append(
            [
                esc(email),
                fmt_duration(learning_by_user.get(email, 0)),
                fmt_duration(data["userSessionMs"].get(email, 0)),
            ]
        )

    nav_links = "".join(
        f'<a href="#{k}">{esc(label)}</a>' for k, label in BUCKETS
    )

    bucket_sections = "".join(
        render_bucket_section(k, label, progress_rows, time_rows) for k, label in BUCKETS
    )

    cohort_rows = []
    for c in data["cohorts"]:
        if c["day"] < data["since"]:
            continue
        r = c["ret"]
        d2 = "pending" if r["d2"]["pending"] else f"{r['d2']['pct']}% ({r['d2']['active']})"
        cohort_rows.append(
            [
                esc(c["day"]),
                str(c["n"]),
                f"{r['d1']['pct']}% ({r['d1']['active']})",
                d2,
                f"{r['d7']['pct']}% ({r['d7']['active']})",
            ]
        )

    signup_rows = [
        [
            esc(u["signupDay"]),
            esc(u["email"]),
            esc(u["name"]),
            esc(u.get("schoolName", "-")),
            esc(str(u.get("studentClass", "-"))),
            esc(u.get("phoneNumber", "-")),
            esc(u["language"]),
            esc(u["signupAt"]),
        ]
        for u in data["users"]
        if (u.get("signupDay") or "") >= data["since"]
    ]

    user_directory = render_user_directory(data)
    page_funnel = render_page_funnel_sections(data)
    ad_sections = render_ad_sections(data)

    return f"""
    {section("Overview", '<div class="grid">' + "".join([
        card(data["totalUsers"], "Registered users"),
        card(data["todayDau"], "DAU today"),
        card(fmt_duration(total_learning_ms), "Active learning time (tutor/sim)"),
        card(fmt_duration(avg_learning_ms), "Avg learning time per active user"),
        card(fmt_duration(total_session_ms), "App open time (session records)"),
        card(len(progress_rows), "Progress records (window)"),
        card(len([r for r in progress_rows if r["status"]=="COMPLETED"]), "Completed (window)"),
        card(len([r for r in progress_rows if r["status"]=="IN_PROGRESS"]), "In progress (window)"),
    ]) + '</div>', time_note)}

    {section("Time spent on app", table(["User", "Learning time (tutor/sim)", "App open time (est.)"], time_user_rows), "Use learning time for engagement. App open time can over-count background orphans.", "time")}

    {section("Daily visitors", table(["Day", "DAU", "Users", "Sessions", "Clicks", "Session time", "Screen time"], dau_rows), dau_note, "dau")}

    {section("Retention (signup cohorts)", table(["Signup", "Users", "D1", "D2", "D7"], cohort_rows))}

    {section("Signups since launch window", table(["Day", "Email", "Name", "School", "Class", "Phone", "Lang", "Signed up"], signup_rows))}

    {user_directory}

    {page_funnel}

    {ad_sections}

    {section("Activity splits — completed / in progress / time", bucket_sections, "Each bucket: day-wise, user-wise (expandable), concept-wise. Screen time = tutor/sim EXIT duration, capped at 45 min per visit.")}

    <div class="cols2">
      {section("Top content/sim clicks", table(["Type", "Count"], [[esc(k), str(v)] for k, v in data["clickTypes"]]))}
    </div>
    """


def render_dashboard(data: dict) -> str:
    content_all = render_main_content(filter_data(data, False))
    content_ext = render_main_content(filter_data(data, True))
    internal_list = ", ".join(data["internalEmails"])
    nav_links = "".join(f'<a href="#{k}">{esc(label)}</a>' for k, label in BUCKETS)

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <title>EduAI Metrics Dashboard</title>
  <style>
    :root {{
      --bg: #f4f6fb; --card: #fff; --text: #1a1a2e; --muted: #5c6478;
      --accent: #4f46e5; --border: #e5e8f0; --warn: #b45309; --ok: #047857;
    }}
    * {{ box-sizing: border-box; }}
    body {{ font-family: "Segoe UI", system-ui, sans-serif; margin: 0; background: var(--bg); color: var(--text); }}
    header {{ background: linear-gradient(135deg, #312e81, #4f46e5); color: #fff; padding: 28px 32px; }}
    header h1 {{ margin: 0 0 6px; font-size: 28px; }}
    header p {{ margin: 0; opacity: .9; }}
    nav.bucket-nav {{ background: #fff; padding: 12px 32px; border-bottom: 1px solid var(--border); display: flex; flex-wrap: wrap; gap: 10px; position: sticky; top: 0; z-index: 10; }}
    nav.bucket-nav a {{ color: var(--accent); text-decoration: none; font-size: 13px; padding: 4px 10px; background: #eef2ff; border-radius: 999px; }}
    .filter-bar {{ background: #fffbeb; border-bottom: 1px solid #fcd34d; padding: 12px 32px; display: flex; flex-wrap: wrap; align-items: center; gap: 16px; font-size: 14px; }}
    .filter-bar label {{ display: flex; align-items: center; gap: 8px; cursor: pointer; font-weight: 600; }}
    .filter-bar .hint {{ color: var(--muted); font-size: 12px; }}
    .view-label {{ font-size: 12px; font-weight: 700; color: var(--accent); text-transform: uppercase; letter-spacing: .05em; margin-bottom: 8px; }}
    main {{ max-width: 1280px; margin: 0 auto; padding: 24px; }}
    section {{ margin-bottom: 36px; }}
    h2 {{ font-size: 20px; margin: 0 0 8px; color: #312e81; }}
    h3 {{ font-size: 17px; margin: 24px 0 10px; color: #3730a3; }}
    h4 {{ font-size: 14px; margin: 18px 0 8px; color: var(--muted); text-transform: uppercase; letter-spacing: .04em; }}
    .note {{ color: var(--muted); font-size: 13px; margin: 0 0 12px; line-height: 1.5; }}
    .grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 12px; margin: 12px 0 20px; }}
    .card {{ background: var(--card); border-radius: 12px; padding: 14px; box-shadow: 0 1px 3px rgba(0,0,0,.06); }}
    .card .val {{ font-size: 24px; font-weight: 700; color: var(--accent); }}
    .card .lbl {{ font-size: 11px; color: var(--muted); margin-top: 4px; }}
    table {{ width: 100%; border-collapse: collapse; background: var(--card); border-radius: 12px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,.06); font-size: 13px; margin-bottom: 16px; }}
    th, td {{ padding: 10px 12px; text-align: left; border-bottom: 1px solid var(--border); vertical-align: top; }}
    th {{ background: #eef2ff; font-weight: 600; }}
    tr:last-child td {{ border-bottom: none; }}
    details {{ background: var(--card); border-radius: 10px; margin-bottom: 10px; box-shadow: 0 1px 3px rgba(0,0,0,.05); }}
    summary {{ cursor: pointer; padding: 12px 14px; font-weight: 600; }}
    .details-body {{ padding: 0 14px 14px; }}
    .pill {{ display: inline-block; background: #eef2ff; color: #3730a3; border-radius: 999px; padding: 2px 8px; font-size: 11px; }}
    .pill.warn {{ background: #fef3c7; color: var(--warn); }}
    .pill.ok {{ background: #d1fae5; color: var(--ok); }}
    .empty {{ color: var(--muted); font-style: italic; }}
    .bucket-block {{ background: #fff; border-radius: 14px; padding: 16px 18px 22px; margin-bottom: 28px; box-shadow: 0 1px 4px rgba(0,0,0,.06); }}
    footer {{ text-align: center; color: var(--muted); font-size: 12px; padding: 24px; }}
    code {{ background: #eef2ff; padding: 2px 6px; border-radius: 4px; }}
  </style>
</head>
<body>
  <header>
    <h1>EduAI Metrics Dashboard</h1>
    <p>Generated {esc(data["generatedAt"])} · Since {esc(data["since"])} (IST) · Firebase {esc(PROJECT)}</p>
  </header>
  <div class="filter-bar">
    <label><input type="checkbox" id="exclude-internal"/> Exclude internal team</label>
    <span class="hint">Internal: {esc(internal_list)}</span>
  </div>
  <nav class="bucket-nav">{nav_links}<a href="#users">Users</a><a href="#pages">Pages</a><a href="#funnel">Funnel</a><a href="#ads">Ads</a><a href="#time">Time spent</a><a href="#dau">Daily visitors</a></nav>
  <main>
    <div id="view-all">
      <div class="view-label">All users</div>
      {content_all}
    </div>
    <div id="view-external" hidden>
      <div class="view-label">External users only (internal team excluded)</div>
      {content_ext}
    </div>
  </main>
  <footer>Re-run: <code>python scripts/metrics-dashboard-html.py</code> or <code>.\\scripts\\run-dashboard.ps1</code></footer>
  <script>
    (function() {{
      const cb = document.getElementById('exclude-internal');
      const all = document.getElementById('view-all');
      const ext = document.getElementById('view-external');
      function sync() {{
        const on = cb.checked;
        all.hidden = on;
        ext.hidden = !on;
      }}
      cb.addEventListener('change', sync);
      sync();
    }})();
  </script>
</body>
</html>"""


def main() -> None:
    since = DEFAULT_SINCE
    out = DEFAULT_OUT
    snapshot = DEFAULT_SNAPSHOT
    args = sys.argv[1:]
    if args and not args[0].startswith("-"):
        since = args[0]
        args = args[1:]
    if "--out" in args:
        out = Path(args[args.index("--out") + 1])
    if "--snapshot" in args:
        snapshot = Path(args[args.index("--snapshot") + 1])

    if "--from-snapshot" in args:
        snap_path = Path(args[args.index("--from-snapshot") + 1])
        print(f"Rendering from snapshot {snap_path}...")
        raw = json.loads(snap_path.read_text(encoding="utf-8"))
        data = collect_data_from_snapshot(raw, since)
    else:
        print(f"Fetching Firestore metrics since {since} (IST)...")
        token = refresh_token()
        data = collect_data(token, since)
        snapshot.parent.mkdir(parents=True, exist_ok=True)
        snapshot.write_text(json.dumps(data, default=str), encoding="utf-8")
        print(f"Snapshot saved: {snapshot}")

    html_out = render_dashboard(data)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(html_out, encoding="utf-8")
    print(f"Dashboard written: {out}")


if __name__ == "__main__":
    main()
