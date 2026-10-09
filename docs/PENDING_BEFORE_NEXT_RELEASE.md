# Pending work — before / with / after next release

**Last updated:** 2026-10-09  
**Next Play ship:** `v1.0.12` / versionCode **14** (vc14)  
**Live Play:** `v1.0.11` / versionCode **13** (Broken Functionality rejection; appeal filed — **do not resubmit 13**)  
**Ship code:** `b66f5c0` (vc14 set) + `9c34206` (Avatar Studio unlock fix) on `main`.  
**Remote:** `gamified/main` = `1d7b12c` (vc14 pushed 2026-10-08). `9c34206` + the docs commit after it are **local — push pending**.

This doc is the single “what’s left” list. Ship-gate checkboxes stay in `docs/RELEASE_CHECKLIST_v1.0.12.md`.
Context lives in `docs/STATUS_play_vc14_coach_language_notifications.md`.

---

## Priority legend

| Tag | Meaning |
|-----|---------|
| **P0** | Blocks safe Play upload / review |
| **P1** | Must land with or immediately after upload (Ads, Pages re-check) |
| **P2** | Important follow-ups; not APK blockers |
| **P3** | Backlog / content / separate features |

---

## 1. Git / remote (P0)

| Item | Status | Action |
|------|--------|--------|
| vc14 ship set committed + pushed | **Done** (`b66f5c0`, pushed with `1d7b12c` to `gamified/main`) | — |
| Avatar Studio unlock fix | **Committed** `9c34206` (was a dirty-tree drive-by; it **is in the vc14 AAB** built 2026-10-08) | `git push gamified main` so remote = what ships |
| `_tmp_*` screenshots / UI dumps / `.codex/` | Now **gitignored** | Delete locally whenever |

**Do not** fold whiteboard PR #2 or the notification re-prompt branch into the vc14 push.

---

## 2. Build & device gates (P0)

| Item | Status | Action |
|------|--------|--------|
| Release AAB (`bundleRelease`) for vc14 | **Done** 2026-10-08 — `app/build/outputs/bundle/release/app-release.aab` (1.0.12 / 14, upload key SHA-1 `db4406fd…`, registered in Firebase) | Rebuild only if code changes. If Gradle OOMs: `-Dorg.gradle.jvmargs=-Xmx4g` (default heap is 2g) |
| Release APK for sideload | **Done** — `app/build/outputs/apk/release/app-release.apk` | — |
| Install **release** build on device | **Blocked** — wireless adb dropped | Re-pair phone; uninstall debug/whiteboard build first (signature differs) |
| Kannada onboarding + class pick (user-detail **and** Edit Profile) | **Not done** | **Must not crash** — this is the Play rejection fix |
| R8 smoke: onboarding → home → concept → sim → settings | **Not done** | On the release APK |
| Full unit test suite | **Done** — 298 tests, 9 failures, **same 9 on `main` baseline** (`android.util.Log` not mocked: MobileAdsInitializer, ResourceDecisionUseCase, SimulationIntroTtsSanitizer ×7) | — |
| Login / onboarding EN + KN | **Not done** | Fresh install |
| Science sims EN + KN (HTML language + voice + coach) | **Not done** | Agent + concept paths |
| Notifications device QA | **Not done** | Per `docs/NOTIFICATIONS.md` |
| Institute login / settings / bookmarks / badges | **Not done** | Release build |
| Avatar Studio: watch ad → unlock applies immediately | **Not done** | New in `9c34206` |

**Release notes (Play):**  
*Fixed class-selection crash (NumberFormatException) affecting Kannada profile setup.*

---

## 3. Play Console upload path (P0 → P1)

| Step | Status |
|------|--------|
| Upload AAB to **Closed testing** (generates Pre-launch) | Pending — can run in parallel with device testing |
| Review Pre-launch / vitals | Pending |
| Promote to Production / submit for review as **14** | Pending |
| Do **not** resubmit versionCode **13** | Rule |

---

## 4. Google Ads — optimize for signups, not installs only (P1)

Code is in `b66f5c0`. Console steps are gated on vc14 being on a device.

### Plumbing (confirm once)

| Check | Correct value |
|-------|----------------|
| Firebase | `eduai-e090e` (`515723957976`) |
| GA4 property | **`517375741`** (`eduai-e090e`, stream `com.ncert7.aitutorandlab`) |
| Ads account | `(263-372-1466)` |
| Do **not** use | `eduai-5e78b` / `517333707`, memory-lane, demopam |
| Access | Ads operator (`mail2anuragmn@gmail.com`) has Property access on `517375741` |

### Steps

| Step | Status | Gate |
|------|--------|------|
| 1. Ads ↔ GA4 `517375741` link | Done | — |
| 2. One **new** registration on vc14 → `sign_up` in GA4 DebugView / Realtime (`method=google`) | **Blocked** | vc14 on device. Enable DebugView: `adb shell setprop debug.firebase.analytics.app com.ncert7.aitutorandlab`. Existing accounts do **not** fire `sign_up` (by design) |
| 3. Mark **`sign_up` as Key event** | Pre-stageable now | Safe/reversible; useful only after step 2 |
| 4. Ads → Goals → Conversions → App → import `sign_up` | **Blocked** | ~24h after first real event |
| 5. Point App campaign bidding at `sign_up` | **Hold** | Volume (below) |

**Code contract:** `logSignUp` only when `CreateUserResult.Created` (`SignUpAnalytics` + `SignUpAnalyticsTest`).

### Volume caveat before switching bidding to `sign_up`

- Signups are low: Firestore shows **~23 in 15 days (~1.5/day)** (22 Sep–6 Oct).
- App campaigns bidding on an in-app action need steady daily conversion volume; at ~1.5/day the campaign may
  stay in learning or under-deliver. Switching **resets learning**.
- Options: keep install-optimized while `sign_up` accumulates as a secondary conversion; or run a separate
  signup-optimized campaign; switch only once daily signups are consistently higher.
- `allow_ad_personalization_signals = false` (kids compliance) — after import, confirm conversions appear.

---

## 5. Deploy re-checks with the release (P1)

No new APK code required; **re-verify on upload day**.

| Item | Action |
|------|--------|
| GitHub Pages `guide-coach.js` | `curl -I` → **200** |
| Coach coverage | `node scripts/check-coach-coverage.js` → Science **0 silent** |
| Kannada sim URLs in Firestore | Dry-run `set-kannada-sim-urls.js` → **0 pending** |
| Bundled `sim_guides` (~154) inside release AAB | Confirm not stripped |

---

## 6. Post-vc14 feature work (P2 — **not** in vc14)

### Notification re-prompt — branch `feature/notif-reprompt` (local, not pushed)

| | |
|--|--|
| Commits | `b5bbd81` (code) + `bfdcc16` (docs), on top of `1d7b12c` |
| What | In-app primer always before the OS dialog; re-offers after denial (2nd system dialog → then deep-link to Settings); moments: end of home tour, chapter list, app open, wins; 48h cooldown, every 3rd open, 1 per session, stop after 4 declines; respects in-app master switch; Settings return schedules reminders; Kannada QUEST copy typo fixed |
| Tests | `NotificationPromptPolicyTest` (15) + all notification tests green |
| Product decisions to confirm | (a) no more cold-start OS dialog; (b) students who already denied get re-asked (within caps) |
| Before merge | Rebase onto `main` after vc14 ships; device QA on Android 13+ (deny → re-ask → Settings path, EN + KN) |

### Whiteboard — PR [#2 whiteboard_implemented](https://github.com/ANURAGMN/Eduai_Gamified/pull/2)

| | |
|--|--|
| Tip | `d240c26` (*added subject filter*, 2026-10-07); based on pre-vc14 `3b585da` |
| Device QA | Debug build installed 2026-10-08; smoke not completed before swapping to vc14 testing |
| Before merge | Rebase onto post-vc14 `main` (shared: `BottomNavBar`, `LearningNavigator`, `ChapterScreen`, `AgenticAI*`); drop the **BottomNavBar** reformat (+711/−703) and duplicate routes; reuse hardened `AgenticAIClient`/auth; localize EN strings (or accept EN-only first cut) |
| Test path | Science → chapter list → Try Interactive Whiteboard → filter chips → concept → TTS/SVG/flow → follow-up; Replay/Stop/voice settings/back |

### PR [#1 notification_changes](https://github.com/ANURAGMN/Eduai_Gamified/pull/1)

Stale (2026-09-01), superseded by `21dbd20` on main + the re-prompt branch. Contains **`app/release/*.apk`** and
baselineProfiles — **must not merge**. Close as superseded.

### Notes feature (spec only)

`docs/NOTES_FEATURE_SPEC.md` — Reader / Sections / PDF viewer backed by Pages-hosted PDFs + optional
`notes.json`. Ship PDF viewer (D) first. Not started.

---

## 7. Content / product backlog (P3 — does **not** block vc14)

| Item | Notes |
|------|--------|
| Math coach | EN Ch.2–9 (~92) + KN Ch.1–4 (~20) silent; authoring + `MathCoachSolver` |
| Glow `target` density | Sparse (e.g. KN Ch.3 ~2/40; EN Ch.6/Ch.8); hint already works |
| EN Ch.3–4 native coach audit | Hand-authored pages (not guide-coach) |
| Coverage gate extensions | Flag missing `whenStuck` / `target` |
| Math Ch.5+ Kannada HTML | No files yet — cannot invent URLs |
| Analytics Firestore mirror | **OFF** — funnels live in GA4 only |
| Play install counts in ops Excel | Not in Firestore; fill from Play Console (`docs/METRICS_FIRESTORE_PULL.md`) |
| Gradle heap | `gradle.properties` has `-Xmx2048m`; release R8 + lint OOM'd once. Consider raising to 4g |

---

## 8. Already done (do not re-open as blockers)

| Item | Where |
|------|--------|
| Class dropdown NFE fix | `UserDetailEntryScreen` + `EditProfileSection` |
| Version 14 / 1.0.12 | `app/build.gradle.kts` |
| Kannada sim URL client reconcile | `SimulationLanguageUrl` / resolver / agent + concept + plan |
| JWT/R8 + refresh/401 hardening | `JwtDecoder`, `AuthTokenPolicy`, `TokenAuthenticator`, … |
| Firestore `appVersion*` sync | `FirebaseRepository` + debounce |
| GA4 `sign_up` on true new user | `CreateUserResult.Created` + `SignUpAnalytics` |
| Reels made-for-kids default on | Feature flag default |
| Live agent-chat “server error” wave | Cleared by **backend** (2026-09-16); client package still ships for defense |
| Science coach on Pages + bundled guides | Live / in tree — re-verify only |
| Metrics export scripts + `METRICS_FIRESTORE_PULL.md` | In `b66f5c0` |
| Empty-graph guard + R8 enum keeps | In `b66f5c0` — confirm on release smoke |
| vc14 pushed; release AAB + APK built; full unit suite at baseline | 2026-10-08 |

---

## 9. Suggested order of operations

1. **Push** `main` (`9c34206` + docs) so the remote matches the AAB.  
2. Re-pair phone → install **release APK** → **Kannada class pick** (must pass) → R8 smoke → Avatar unlock.  
3. Same session: register **one new account** → `sign_up` in GA4 DebugView (Ads step 2).  
4. Upload AAB to **Closed testing** (can start now, in parallel) → Pre-launch report.  
5. Pages + coverage + KN URL dry-run.  
6. Promote to Production as **vc14**.  
7. Ads: Key Event `sign_up` → import (~24h) → hold bid switch until volume supports it.  
8. After vc14 is live: rebase + QA `feature/notif-reprompt`; rebase + QA whiteboard PR #2; close PR #1; content backlog.

---

## 10. Doc map

| Doc | Role |
|-----|------|
| **This file** | All pending work (release + Ads + branches/PRs + backlog) |
| `docs/RELEASE_CHECKLIST_v1.0.12.md` | Ship gates checkboxes |
| `docs/STATUS_play_vc14_coach_language_notifications.md` | Live vs in-tree context |
| `docs/NOTIFICATIONS.md` | Notification system + device QA (re-prompt rules on the feature branch) |
| `docs/NOTES_FEATURE_SPEC.md` | Notes feature spec |
| `docs/METRICS_FIRESTORE_PULL.md` | How to pull ops metrics / Excel |
| `docs/AUDIT_chat_auth_jwt_vc14.md` | Chat auth / JWT package |

---

*When an item above is finished, update this file and the matching checkbox in `RELEASE_CHECKLIST_v1.0.12.md` so the next window does not rediscover status.*
