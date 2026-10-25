# Pending work — before / with / after next release

**Last updated:** 2026-10-25  
**Next Play ship:** `v1.0.14` / versionCode **16**  
**In tree now:** `versionCode = 16`, `versionName = "1.0.14"`  
**Prior cut:** `v1.0.13` / versionCode **15** (whiteboard tutor + Notes/Doubts + bite trials — sideloaded; confirm whether it ever went to Play)  
**Live Play (historical):** `v1.0.11` / versionCode **13** — Broken Functionality rejection; appeal filed — **do not resubmit 13**  
**Ship tip (features):** `575ecdb` (whiteboard blank-board + timed reveal + Show all) + `28ade34` (home topic search) on `gamified/main`.  
**Ship checkboxes:** `docs/RELEASE_CHECKLIST_v1.0.14.md`  
**Store notes:** `docs/store-listing/release-notes-1.0.14.txt`

---

## Priority legend

| Tag | Meaning |
|-----|---------|
| **P0** | Blocks safe Play upload / review |
| **P1** | Must land with or immediately after upload |
| **P2** | Important follow-ups; not APK blockers |
| **P3** | Backlog / content / separate features |

---

## 1. What’s in 1.0.14 (this ship)

| Item | Commit / notes |
|------|----------------|
| Whiteboard blank Visual Board fix | AndroidSVG bitmaps, viewBox size, `orient` sanitize (`575ecdb`) |
| Timed SVG reveal + **Show all** | Opacity inject by `scene_part`; skip timing control |
| Home topic search | Notes / Whiteboard / Ask doubt from search (`28ade34`) |
| Carried from 1.0.13 | Whiteboard tutor, Notes, Doubts, bite-based chapter trials (`51ff85e`) |
| Gradle heap 4g | `gradle.properties` — stops release R8 OOM |

---

## 2. Git / build (P0)

| Item | Status | Action |
|------|--------|--------|
| Features on `gamified/main` | **Done** (`575ecdb`) | — |
| Version 16 / 1.0.14 + release docs | **In working tree** | Commit + `git push gamified main` |
| Release AAB | **Pending** | `./gradlew bundleRelease` → upload Closed testing |
| Release APK sideload smoke | **Pending** | Install after AAB/APK build; whiteboard + search |

---

## 3. Device / Play gates (P0)

| Item | Status |
|------|--------|
| Whiteboard Visual Board paints (Angle Validity / Photosynthesis) | Device-verified on pre-bump APK; **re-confirm on 1.0.14 release build** |
| Timed reveal + Show all | Same |
| Home search → Notes / Whiteboard / doubt | Confirm on release |
| Kannada class pick (signup + Edit Profile) | Regression gate (vc13 rejection) |
| Upload Closed testing → Pre-launch → Production as **16** | Pending |
| Do **not** resubmit versionCode **13** | Rule |

**Release notes (Play):** see `docs/store-listing/release-notes-1.0.14.txt`.

---

## 4. Google Ads / GA4 (P1 — carry-over)

| Step | Status |
|------|--------|
| Ads ↔ GA4 `517375741` link | Done |
| `sign_up` fires once on true new user | Done (code + device 2026-10-09) |
| Mark `sign_up` as Key event | Pending / confirm |
| Import `sign_up` in Ads | Pending |
| Switch campaign bid to `sign_up` | **Hold** until daily signup volume supports it |

---

## 5. Deploy re-checks (P1)

| Item | Action |
|------|--------|
| Pages `guide-coach.js` | `curl -I` → 200 |
| Coach coverage | Science **0 silent** |
| Kannada sim URLs | Dry-run → 0 pending |

---

## 6. Post-ship (P2)

| Item | Notes |
|------|--------|
| `feature/notif-reprompt` | Rebase onto post-1.0.14 main; device QA Android 13+ |
| Close stale PR #1 `notification_changes` | Superseded; has release binaries — do not merge |
| Close / archive PR #2 whiteboard | Feature already on main via 1.0.13+ |
| Math coach authoring | EN/KN silent chapters — content backlog |

---

## 7. Doc map

| Doc | Role |
|-----|------|
| **This file** | What’s left |
| `docs/RELEASE_CHECKLIST_v1.0.14.md` | Ship-gate checkboxes |
| `docs/store-listing/release-notes-1.0.14.txt` | Play “What’s new” |
| `docs/RELEASE_CHECKLIST_v1.0.12.md` | Historical vc14 gates |
| `docs/NOTIFICATIONS.md` | Notification QA |
| `docs/AUDIT_chat_auth_jwt_vc14.md` | Chat auth / JWT |

---

*When an item finishes, update this file and tick `RELEASE_CHECKLIST_v1.0.14.md`.*
