# Status — Play vc14 resubmit (coach / language / notifications)

**Status:** FINAL (code- and live-verified 2026-08-31; chat live-status updated 2026-09-16)  
**Live Play:** `v1.0.11` / versionCode **13** — Broken Functionality rejection; appeal filed  
**Next ship:** `v1.0.12` / versionCode **14**

| Doc role | Path |
|----------|------|
| **This file** | Context — what is live vs in-tree vs backlog |
| **Ship gates** | `docs/RELEASE_CHECKLIST_v1.0.12.md` |

---

## 1. Play rejection (vc13)

| Item | Detail |
|------|--------|
| Policy | Broken Functionality — “crashes after opening” / In-app experience |
| Android Vitals | **No** crash cluster for **13 / 1.0.11**. Only crash: `NumberFormatException` on `UserDetailEntryScreen`, tagged **10 / 1.0.8** (2 users / 6 events) |
| Vitals “R8 40%” tip | Optimization advice only — **not** the rejection cause |
| Pre-launch | Empty for that artifact (no device stack) |
| Appeal | 2026-08-31 — “I believe this is incorrect” (vitals evidence). **Still ship vc14**; do not wait on appeal alone |
| Likely cause | Class picker: EN `Class %d` / KN `ತರಗತಿ %d`. Shipped code used `removePrefix("Class ").toInt()` → NFE on Kannada. Hits on **user-detail after sign-in** (often labeled “after opening”) |

**Fix (in working tree — commit into vc14):** `docs/CURSOR_NOTE_crash_class_dropdown_nfe.md`

- `UserDetailEntryScreen.kt` — option **index** (+ digit fallback); localized `selectedValue`
- `EditProfileSection.kt` — same pattern

**Do not resubmit vc13.** Path: Closed testing → Pre-launch → Production as **14**.

Release notes: *Fixed class-selection crash (NumberFormatException) affecting Kannada profile setup.*

---

## 2. Science coach — glow / hint / Pages

### Live (verified)

| Area | Status |
|------|--------|
| Coach on Science sims | Live EN+KN via `edu-coach.js` + `guide-coach.js`. Coverage gate: **Science 0 silent / PASS** (broader than Ch.5–8 alone — KN Ch.3–4 wired too) |
| `guide-coach.js` on Pages | HTTP **200**; includes `whenStuck`, lookahead, `fromGuide`. Repo tip ~`4ed8e30`. CDN bytes can lag `origin/main` — **re-curl + spot-check Heat glow before upload day** |
| Hosted `.guide.json` | Live (network fallback for older APKs) |
| Hint | Works wherever guides define `whenStuck` (even if glow `target`s are sparse) |
| Glow engine | Stand-down / progression / hint-phase + lookahead fixed |
| Heat multi-step targets | EN + KN guides enriched (reference pattern for other chapters) |

**Hygiene:** do not commit EduAI_app `Simulations/.fuse_hidden*`.  
**APK:** `assets/sim_guides/` ≈ **154** JSONs in tree — confirm present inside the release AAB.

### Backlog (does **not** block vc14)

| Item | Notes |
|------|--------|
| Glow `target` density | Sparse in places (e.g. KN Ch.3 ~2/40; EN Ch.6/Ch.8). Hint still works |
| EN Ch.3–4 native coach audit | Hand-authored pages (not guide-coach) |
| Math coach | Coverage gate: **~28 silent** Math sims — authoring + `MathCoachSolver` |
| Gate extensions | Flag missing `whenStuck` / `target` in `check-coach-coverage.js` |

**No-APK QA:** Heat — glow advances across steps; Hint when stuck.

---

## 3. Language — Kannada vs English sim HTML

### Live without a Play update

| Item | Status |
|------|--------|
| Firestore `simulation_url_kannada` | Filled where `*_kn.html` exists (Science gaps; Math Ch.1–4 already set) |
| Pre-ship dry-run | `EduAI_app/scripts/set-kannada-sim-urls.js` → expect **0 pending** |
| Math Ch.5+ KN HTML | Not inventable — no files yet |

### Must ship in vc14 (Kotlin — **uncommitted**)

| Item | Role |
|------|------|
| `SimulationLanguageUrl.kt` | Prefer KN catalog / twin URL |
| `SimulationUrlResolver.kt` + test | Pure resolver |
| `SimulationAgentViewModel.kt` | Reconcile agent `html_url` on start **and** resume |
| `ConceptSimulationViewModel.kt` | Plan / selected URL |
| `PlanTrialMaterializer.kt` | Exam-plan trial URLs |
| `ConceptSimulationViewer.kt` | V4 text seed if page coach silent |
| Bundled `sim_guides` | Offline coach |

Without this reconcile, the **agent path** can still show **English HTML + Kannada voice**. WebView/concept path improves after Firestore sync alone.

Also in ship set: **Reels** `REQUIRE_MADE_FOR_KIDS_DEFAULT = true`.

Refs: `BUGFIX_kannada_sim_and_science_coach.md`, `BUGFIX_no_kotlin_feasibility.md`.

---

## 4. Notifications

Implemented and QA-hardened — **ship with vc14**, not a content backlog.

| Piece | Status |
|--------|--------|
| Channels, scheduler, types | Done |
| Primer + Android 13+ `POST_NOTIFICATIONS` | Done |
| Settings (master / categories / reminder / quiet hours) | Done |
| Analytics shown → opened | Done |
| QA (`21dbd20`) | Grant → enable + reschedule; cold-start ask after login; daily-alarm exception below |

**Quiet hours (explicit rule):** Quiet hours suppress engagement notifications (streak, quests, inactivity, etc.). **Exception:** when the scheduled daily alarm fires (`DAILY_ALARM`), `DAILY_REMINDER` may still send so the user’s chosen reminder time is not dropped by quiet hours or inexact-alarm drift. Other types still respect quiet hours. (`NotificationEvaluator` · `docs/NOTIFICATIONS.md`)

Device pass: `NOTIFICATIONS.md` checklist (window, quiet hours, spam caps, deep links, primer, reboot).

Optional later: KN channel labels on language toggle; GA4 shown→opened by type.

---

## 5. Agent chat — “server error” wave

| Item | Status |
|------|--------|
| Live agent-chat “server error” | **Resolved (2026-09-16) via backend interventions** — not waiting on Play vc14 |
| Client JWT/R8 package | Still in-tree for vc14 (defense-in-depth). Canonical: `docs/AUDIT_chat_auth_jwt_vc14.md` |

---

## 5b. Firestore user `appVersion*` (ops)

| Item | Status |
|------|--------|
| `syncAppVersion` + debounce | **In tree** — `FirebaseRepository` / `DataSyncService.syncAppVersionIfNeeded` / foreground + auth |
| Fields | `appVersionName`, `appVersionCode`, `appVersionUpdatedAt`, first-seen pair |
| Note | `docs/CURSOR_NOTE_firestore_app_version.md` |

---

## 5c. Google Ads signup conversion (before/with vc14)

| Item | Status |
|------|--------|
| Code `sign_up` | **In tree** — `logSignUp()` only on `CreateUserResult.Created`; `SignUpAnalytics` + unit tests |
| Correct GA4 | **`eduai-e090e` / `517375741`** (not `eduai-5e78b` / `517333707`) |
| Ads link | Confirm Ads ↔ **517375741**; then Key Event + import after first fire on this release |
| Checklist | `docs/RELEASE_CHECKLIST_v1.0.12.md` §1b |

---

## 6. Internal / deferred (not APK blockers)

| Item | Notes |
|------|--------|
| `scripts/metrics-dashboard.js` | Internal; 45m session cap + IST day-clip; commit when convenient |
| Analytics Firestore mirror | **OFF** — clicks/funnels stay in GA4 |
| R8 release smoke | Mandatory on **release** AAB. Room `data.local.**` keeps already in ProGuard |

---

## 7. Ship order

1. **Commit ship set only** — class NFE + KN URL reconcile + chat auth package (JWT/R8 + refresh/401 + log spam) + `logSignUp` + `appVersion*` + version 14 (+ these docs).  
   Exclude: Avatar Studio drive-bys, `_tmp_*`, `.fuse_hidden*`.
2. `./gradlew testDebugUnitTest` — no *new* failures; include `SimulationUrlResolverTest`, `JwtDecoderTest`.
3. **`bundleRelease`** → install **release** AAB → onboarding + class pick in **Kannada** (must not crash).
4. Device: EN+KN Science sim (HTML language + coach), notifications smoke.
5. Pages: `guide-coach.js` 200 + coverage gate Science 0 silent + Heat spot-check.
6. Upload **Closed testing** (Pre-launch) → if clean → Production / review as **vc14**.
7. **Ads signup:** one new reg on this build → GA4 Key Event `sign_up` → Ads import (property **517375741**).
8. Content backlog (glow targets, Math coach, Math KN HTML) in parallel — **does not gate** this release.

Checkboxes: `docs/RELEASE_CHECKLIST_v1.0.12.md`.

---

## 8. Quick reference

| Topic | Pointer |
|-------|---------|
| Ship checklist | `docs/RELEASE_CHECKLIST_v1.0.12.md` |
| Class NFE | `docs/CURSOR_NOTE_crash_class_dropdown_nfe.md` |
| Chat auth audit (JWT/R8) | `docs/AUDIT_chat_auth_jwt_vc14.md` · live wave cleared by backend |
| Firestore app version | `docs/CURSOR_NOTE_firestore_app_version.md` |
| Ads `sign_up` / GA4 | Checklist §1b — property **517375741** only |
| Kannada + coach bugs | `docs/BUGFIX_kannada_sim_and_science_coach.md` |
| No-Kotlin feasibility | `docs/BUGFIX_no_kotlin_feasibility.md` |
| Notifications | `docs/NOTIFICATIONS.md` · `21dbd20` |
| Pages glow/hint | EduAI_app `693669b` … `4ed8e30` |
| Metrics (internal) | `scripts/metrics-dashboard.js` |
