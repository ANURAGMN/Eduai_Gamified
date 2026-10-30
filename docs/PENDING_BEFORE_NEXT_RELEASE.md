# Pending work — before / with / after next release

**Last updated:** 2026-10-30  
**Live Play:** `v1.0.14` / versionCode **16** — **live on Google Play** (2026-10-30)  
**In tree now:** `versionCode = 16`, `versionName = "1.0.14"` (matches live)  
**Do NOT resubmit versionCode 13** (v1.0.11 Broken-Functionality rejection; appeal filed).  
**Shipped tip:** `de07f80` / features `575ecdb` (whiteboard blank-board + timed reveal + Show all) + `28ade34` (home topic search) on `gamified/main`.  
**Historical checklist:** `docs/RELEASE_CHECKLIST_v1.0.14.md`  
**Store notes:** `docs/store-listing/release-notes-1.0.14.txt`

---

## Priority legend

| Tag | Meaning |
|-----|---------|
| **P0** | Blocks next Play upload |
| **P1** | Land soon after live (Ads, Pages) |
| **P2** | Follow-ups; not APK blockers |
| **P3** | Backlog / content |

---

## 1. Shipped in 1.0.14 (live)

| Item | Notes |
|------|--------|
| Whiteboard blank Visual Board fix | AndroidSVG bitmaps, viewBox size, `orient` sanitize |
| Timed SVG reveal + **Show all** | Opacity inject by `scene_part` |
| Home topic search | Notes / Whiteboard / Ask doubt |
| From 1.0.13 carry | Whiteboard tutor, Notes, Doubts, bite-based chapter trials |

---

## 2. Post-live (P1)

| Item | Status |
|------|--------|
| Watch Play Vitals / Crashlytics for new crashes on 16 | Ongoing |
| GA4: mark `sign_up` as Key event | Confirm if not done |
| Ads: import `sign_up`; **hold** bid switch until daily signup volume supports it | Pending |
| Pages `guide-coach.js` / coach coverage / KN sim URL dry-run | Re-check when touching coach content |

---

## 3. Next feature work (P2 — not in 1.0.14)

| Item | Notes |
|------|--------|
| `feature/notif-reprompt` | Rebase onto current `main`; device QA Android 13+ |
| Close stale PR #1 `notification_changes` | Superseded; has release binaries — do not merge |
| Close / archive PR #2 whiteboard | Feature already on main |
| Math coach authoring | EN/KN silent chapters — content backlog |

---

## 4. Doc map

| Doc | Role |
|-----|------|
| **This file** | What’s left after live |
| `docs/RELEASE_CHECKLIST_v1.0.14.md` | 1.0.14 ship gates (historical) |
| `docs/store-listing/release-notes-1.0.14.txt` | Play “What’s new” for 1.0.14 |
| `docs/NOTIFICATIONS.md` | Notification QA |
| `docs/AUDIT_chat_auth_jwt_vc14.md` | Chat auth / JWT |

---

*When starting the next cut, bump versionCode past 16 and add a new `RELEASE_CHECKLIST_v1.0.x.md`.*
