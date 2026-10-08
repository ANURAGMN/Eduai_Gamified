# Pending work — before / with / after next release

**Last updated:** 2026-10-08  
**Next Play ship:** `v1.0.12` / versionCode **14** (vc14)  
**Live Play:** `v1.0.11` / versionCode **13** (Broken Functionality rejection; appeal filed — **do not resubmit 13**)  
**Ship commit (local):** `b66f5c0` — *Ship vc14: class NFE fix, Kannada sim URLs, auth hardening, Ads sign_up.*  
**Tracking branch tip vs remote:** `main` is **ahead of** `gamified/main` by that commit — **push / open PR still pending** unless already done elsewhere.

This doc is the single “what’s left” list. Day-to-day checkboxes for the Play build stay in  
`docs/RELEASE_CHECKLIST_v1.0.12.md`. Context lives in  
`docs/STATUS_play_vc14_coach_language_notifications.md`.

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
| vc14 ship set committed | **Done** (`b66f5c0`) | — |
| Push to remote (`gamified` / origin as you use for Play) | **Pending** | `git push` the commit that tracks Play upload |
| Avatar Studio / ui-kit drive-bys still dirty locally | Left out of ship commit on purpose | Separate commit later or discard |
| `_tmp_*` screenshots / UI dumps / `.codex/` | Untracked junk | Do **not** commit |

**Do not** fold whiteboard MR or release APKs into the vc14 push.

---

## 2. Build & device gates (P0)

| Item | Status | Action |
|------|--------|--------|
| Release AAB (`bundleRelease`) for vc14 | **Not done** | Build signed release AAB |
| Install **release** build (not debug) | **Not done** | Sideload / Closed testing |
| Kannada onboarding + class pick (user-detail **and** Edit Profile) | **Not done** | Must not crash — this is the Play rejection fix |
| R8 smoke: onboarding → home → concept → sim → settings | **Not done** | Minify is on; Room keeps already in ProGuard |
| Full unit test suite | Partial | Ran targeted tests (`SignUpAnalyticsTest`, JWT/auth, `SimulationUrlResolverTest`). Run `./gradlew testDebugUnitTest` — no *new* failures vs known baseline |
| Login / onboarding EN + KN | **Not done** | Fresh install path |
| Science sims EN + KN (HTML language + voice + coach) | **Not done** | Agent + concept paths |
| Notifications device QA | **Not done** | Per `docs/NOTIFICATIONS.md` |
| Institute login / settings / bookmarks / badges | **Not done** | Release build |

**Release notes (Play):**  
*Fixed class-selection crash (NumberFormatException) affecting Kannada profile setup.*

---

## 3. Play Console upload path (P0 → P1)

| Step | Status |
|------|--------|
| Upload AAB to **Closed testing** (generates Pre-launch) | Pending |
| Review Pre-launch / vitals | Pending |
| Promote to Production / submit for review as **14** | Pending |
| Do **not** resubmit versionCode **13** | Rule |

---

## 4. Google Ads — optimize for signups, not installs only (P1)

Code is in `b66f5c0`. Console work still open.

### Plumbing (confirm once)

| Check | Correct value |
|-------|----------------|
| Firebase | `eduai-e090e` (`515723957976`) |
| GA4 property | **`517375741`** (`eduai-e090e`, stream `com.ncert7.aitutorandlab`) |
| Ads account | `(263-372-1466)` |
| Do **not** use | `eduai-5e78b` / `517333707`, memory-lane, demopam |
| Access | Ads operator (`mail2anuragmn@gmail.com`) has Property access on `517375741` |

### After vc14 is installed

| Step | Status |
|------|--------|
| One **new** registration on **this** build | Pending |
| Confirm `sign_up` in GA4 Realtime / Events | Pending |
| Star **`sign_up` as Key event** (do not invent screen-based `sign_up`) | Pending |
| Ads → Goals → Conversions → App → **import `sign_up`** from Firebase/GA4 (~24h) | Pending |
| Point App campaigns at signup conversion (else stays install-optimized) | Pending — **gate on volume, see below** |

**Code contract:** `logSignUp` only when `CreateUserResult.Created` (`SignUpAnalytics` + `SignUpAnalyticsTest`).

### Volume caveat before switching bidding to `sign_up`

- Current signups are low: Firestore shows **~23 in 15 days (~1.5/day)** across all sources (22 Sep–6 Oct).
- App campaigns bidding on an in-app action need steady daily conversion volume (check Google Ads Help for
  the current recommendation). At ~1.5/day the campaign may stay in learning or under-deliver.
- Switching the bid target **resets learning** — expect a temporary dip.
- Options: keep install-optimized while `sign_up` accumulates as a secondary/observed conversion; run a
  separate signup-optimized campaign alongside; switch only once daily signups are consistently higher.
- `allow_ad_personalization_signals = false` (kids compliance) — after import, confirm `sign_up`
  conversions actually appear in Ads.

---

## 5. Deploy re-checks with the release (P1)

No new APK code required if already live; **re-verify on upload day**.

| Item | Action |
|------|--------|
| GitHub Pages `guide-coach.js` | `curl -I` → **200** |
| Coach coverage | `node scripts/check-coach-coverage.js` → Science **0 silent** |
| Kannada sim URLs in Firestore | Dry-run `set-kannada-sim-urls.js` → **0 pending** |
| Bundled `sim_guides` (~154) inside release AAB | Confirm not stripped |

---

## 6. Open MRs — Rabinarayan / Rabi2007 (P2 — **not** vc14)

Repo: [`ANURAGMN/Eduai_Gamified`](https://github.com/ANURAGMN/Eduai_Gamified)

### PR [#2 whiteboard_implemented](https://github.com/ANURAGMN/Eduai_Gamified/pull/2)

| | |
|--|--|
| Tip | `d240c26` (*added subject filter*, 2026-10-07) |
| Scope | Whiteboard tutor UI + `/whiteboard-tutor/*` + chapter entry + Science/Math filter |
| Mergeable | Yes (GitHub) |
| Device QA | Started once; wireless adb never completed — **still needs local smoke** |

**Before merge:**
- Rebase onto post-vc14 `main` (shared files: `BottomNavBar`, `LearningNavigator`, `ChapterScreen`, `AgenticAI*`).
- Strip / avoid shipping the **BottomNavBar** large reformat (+711/−703) and **duplicate** whiteboard routes vs `LearningNavigator`.
- Localize hard-coded English strings (or accept EN-only for a first cut).
- Device test path: Science → chapter list → Interactive Whiteboard → concept → TTS/SVG/flow → ask follow-up.

### PR [#1 notification_changes](https://github.com/ANURAGMN/Eduai_Gamified/pull/1)

| | |
|--|--|
| Last update | 2026-09-01 (stale vs `21dbd20` notifications work on main) |
| Code | `NotificationEvaluator` / `Orchestrator` / `MainActivity` |

**Blocker:** includes **`app/release/*.apk`** and baselineProfiles — **must not merge** until binaries are removed. Prefer close-as-superseded or a clean rebased diff.

---

## 7. Local dirty tree (P2 hygiene)

Still modified / untracked after the ship commit (as of 2026-10-08):

- `AvatarStudioCopyFactory.kt`, `AvatarPresets.kt`, `AvatarStudioCopy.kt`, `AvatarStudioScreen.kt`
- `_tmp_*.png`, `_tmp_ui*.xml`, `_tmp_v*.png`
- `.codex/`

Decide: separate feature commit, stash, or delete. Keep out of Play AAB source of truth unless intentional.

---

## 8. Content / product backlog (P3 — does **not** block vc14)

| Item | Notes |
|------|--------|
| Math coach | EN Ch.2–9 (~92) + KN Ch.1–4 (~20) silent; authoring + `MathCoachSolver` |
| Glow `target` density | Sparse (e.g. KN Ch.3 ~2/40; EN Ch.6/Ch.8); hint already works |
| EN Ch.3–4 native coach audit | Hand-authored pages (not guide-coach) |
| Coverage gate extensions | Flag missing `whenStuck` / `target` |
| Math Ch.5+ Kannada HTML | No files yet — cannot invent URLs |
| Analytics Firestore mirror | **OFF** — funnels live in GA4 only |
| Play install counts in ops Excel | Not in Firestore; fill from Play Console (`docs/METRICS_FIRESTORE_PULL.md`) |

---

## 9. Already done (do not re-open as blockers)

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
| Empty-graph guard (`GraphRenderLogic`) + R8 enum keeps (`TutorCharacter`, `NotificationEvalTrigger`) | In `b66f5c0` — enum keeps are defense-in-depth; confirm on release smoke |

---

## 10. Suggested order of operations

1. **Push** `b66f5c0` (or the PR that carries it) to the remote used for release.  
2. **`testDebugUnitTest`** full suite — confirm no new failures.  
3. **`bundleRelease`** → install release → **Kannada class pick** (must pass).  
4. Device smoke (sims EN+KN, notifications, login).  
5. Pages + coverage + KN URL dry-run.  
6. Closed testing → Pre-launch → Production as **vc14**.  
7. Ads: one new reg → Key Event `sign_up` → import on **517375741**.  
8. In parallel / after: whiteboard PR #2 rebase + device QA; close or clean PR #1; Avatar Studio / junk cleanup; content backlog.

---

## 11. Doc map

| Doc | Role |
|-----|------|
| **This file** | All pending work (release + Ads + MRs + backlog) |
| `docs/RELEASE_CHECKLIST_v1.0.12.md` | Ship gates checkboxes |
| `docs/STATUS_play_vc14_coach_language_notifications.md` | Live vs in-tree context |
| `docs/METRICS_FIRESTORE_PULL.md` | How to pull ops metrics / Excel |
| `docs/NOTIFICATIONS.md` | Notification device QA |
| `docs/AUDIT_chat_auth_jwt_vc14.md` | Chat auth / JWT package |
| Whiteboard / notification PRs | GitHub `Eduai_Gamified` #2 / #1 |

---

*When an item above is finished, update this file and the matching checkbox in `RELEASE_CHECKLIST_v1.0.12.md` so the next window does not rediscover status.*
