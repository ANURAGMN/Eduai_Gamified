# How EduAI metrics are pulled (Firestore)

Use this file in another Cursor window so the chat does not have to rediscover sources.

**Project:** Firebase `eduai-e090e`  
**App filter:** `appName = eduai_app`  
**Timezone:** Asia/Kolkata (IST). A “day” is midnight–midnight IST, not UTC.  
**Auth:** `scripts/*.py` refresh a Google token from `.tools/firebase-ci-token.txt` (gitignored) and call the Firestore REST API.

Installs / downloads are **not** in Firestore. Put Play Console numbers in Excel yourself.

---

## Scripts

| Script | What it prints / writes |
|--------|-------------------------|
| `scripts/export-metrics-excel.py` | Last 15 IST days (or a date range) → `exports/eduai_metrics_<start>_to_<end>.xlsx` |
| `scripts/query-downloads-signins-by-day.py [YYYY-MM-DD]` | Daywise sign-in completes, DAU, sessions |
| `scripts/query-sim-chat-completions.py [YYYY-MM-DD]` | Daywise sim + chat/agent completions, by user, DONE/WIP |
| `scripts/query-user-coverage.py` | One user’s progress titles (edit `TARGETS` in the file) |

Run from repo root (PowerShell):

```powershell
$env:PYTHONIOENCODING='utf-8'
python scripts/export-metrics-excel.py
python scripts/export-metrics-excel.py 2026-09-22
python scripts/export-metrics-excel.py 2026-09-22 2026-10-06
python scripts/query-downloads-signins-by-day.py 2026-09-22
python scripts/query-sim-chat-completions.py 2026-09-22
```

`exports/` is gitignored (PII: phone, school, email).

---

## Collections and definitions

### Sign-in completes (new registrations)

- Collection: `users/{uid}`
- Keep docs where `appName == eduai_app`
- Bucket by `createdAt` (epoch ms) → IST date
- Profile fields on the same doc: `email`, `displayName`, `phoneNumber`, `schoolName`, `studentClass`, `language`, `id`, `updatedAt`, plus later `appVersionName` / `appVersionCode` / `appVersionFirstSeenName` when vc14+ has synced
- Doc id is sometimes the email, sometimes a Google numeric id — always read `email` and `id`

This is **not** Play installs. Users who install but never finish signup never appear here.

### DAU and sessions

- Parent: `sessions/eduai_app_<key>` where `<key>` is usually email, sometimes Google uid
- Child: `sessions/{parent}/records`
- Timestamp: `sessionStartTime` else `sessionDate`
- **DAU** = unique `<key>` with ≥1 record that IST day
- **Sessions** = count of those records

Join `<key>` to `users` by email, `id`, or document id (case-insensitive). If no match, Excel `Active_users.matched_user_doc = no`.

### Tasks (simulations + chat/agent)

- Parent: `progress/eduai_app_<key>/records`
- Status: `status` (`COMPLETED` vs anything else = WIP)
- Type: `itemType` (uppercased)

| Bucket in reports | `itemType` values |
|-------------------|-------------------|
| Sim done | `SIMULATION`, `SIMULATION_AGENT` |
| Chat/agent done | `CONCEPT`, `MATH_AGENT`, `SCIENCE_AGENT`, `REVISION_AGENT`, `STUDY`, `CHATBOT` |

Day for a record: `completedAt` else `lastAccessedAt` else `updatedAt` (IST).

Optional title fields: `itemTitle`, `title`, `conceptName`; chapter/subject: `chapterName`/`chapterId`, `subjectName`/`subjectId`.

WIP = status ≠ `COMPLETED` and the timestamp still falls in the window.

### Coverage for a named person

`scripts/query-user-coverage.py` also checks `chapterprogress/{parent}/records` (`status`, `overallPercentage`). Progress parents tried: `eduai_app_<email>`, `eduai_app_<uid>`, `eduai_app_<doc_id>`.

---

## Excel workbook (`export-metrics-excel.py`)

| Sheet | Contents |
|-------|----------|
| Notes | Window, definitions, generated time |
| Daywise | Installs blank, sign-in completes, DAU, sessions, sim done, chat/agent done, type breakdown |
| New_signups | PII for users with `createdAt` in the window |
| Active_users | Anyone with a session in the window + joined profile |
| Tasks_completed | COMPLETED progress rows in the window |
| Tasks_WIP | Non-completed progress rows in the window |

Default window: **today IST inclusive, going back 14 days** = 15 calendar days.

---

## What this stack cannot answer

| Ask | Where it actually lives |
|-----|-------------------------|
| Store installs / downloads | Play Console |
| Pre-signup funnel (first_open, login_view, gmail_tap) | GA4 (`eduai-e090e`, property `517375741`). Firestore analytics mirror is off |
| Ads conversions | Google Ads after `sign_up` Key Event import |

Do not treat DAU as installs. Do not treat sign-in completes as downloads.

---

## Prompt to reuse in a new window

Copy this:

> Read `docs/METRICS_FIRESTORE_PULL.md` and run `python scripts/export-metrics-excel.py` from the Eduapp repo. Report the Daywise totals and point me at the xlsx. Do not invent Play install counts. IST days only. PII stays in the Excel, summarize counts in chat.
