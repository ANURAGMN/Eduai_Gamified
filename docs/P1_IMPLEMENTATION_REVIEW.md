# P1 Implementation Review — What Was Built

**App:** `com.ncert7.aitutorandlab` (Eduapp / gamified)  
**Date:** 2026-08-15  
**Spec:** [P1_PRODUCT_GAPS_DETAILED.md](P1_PRODUCT_GAPS_DETAILED.md)  
**Checklist:** [PRE_LAUNCH_CHECKLIST.md](PRE_LAUNCH_CHECKLIST.md)  
**Status:** Implemented in working tree; **not committed** unless separately requested.

This document is the **implementation handoff for review**. It describes what shipped for each P1 item, key design choices, files touched, verification steps, and known residuals.

**Product order used:** retention-first — §1 Garden → §2 Onboarding → §3 Economy/plan/quests → §4 Analytics docs → §5 Kannada.

---

## Executive summary

| Area | Outcome | One-line |
|------|---------|----------|
| §1 Garden sync | **Shipped** | Pristine restore no longer blocked by starter route; Home awaits restore; plant-time deferred upload |
| §2 Onboarding cloud | **Shipped** | Picks written to `users/{id}.onboarding`; hydrated on login; cleared on logout |
| §3 Plan + economy + quests | **Shipped** | Profile LWW, exam plan + days sync, quest claim OR-merge + heal; Firestore rules deployed |
| §4 Analytics | **Docs only** | Checklist #7–15 marked Done (events already existed in code) |
| §5 Kannada | **Shipped (P0–P3)** | First-run + social/settings/moments localized; EN↔KN string key parity (315 = 315) |

**Compile / tests checked:** `:app:compileDebugKotlin` OK; `GardenRestorePolicyTest` OK.

---

## §1 — Garden Firestore restore + deferred upload

### Problem addressed
1. **Bug A:** Local pristine garden used starter route `"1"`, but restore required route `"0"` → silent no-op on reinstall.  
2. **Bug B:** Home could call `ensureState()` before restore finished → local row looked “progressed”.  
3. **Gap C:** Plants only uploaded on full/weekly sync → kill-app data loss window.  
4. **R.1:** Onboarding theme must not overwrite a restored remote garden (including 0-plant remote state).

### What we built

| Piece | Behavior |
|-------|----------|
| `GardenRestorePolicy` | Pristine iff `itemCount == 0 && steps == 0` (route/theme/slot ignored) |
| `GardenSyncManager.restoreGarden` | Uses policy; returns outcome; emits `restore_applied` / `restore_skipped` |
| `DataSyncService.awaitGardenRestore()` | Gate for Home (also covers full auth restore sequence) |
| `HomeViewModel` | Awaits restore before garden observe / onboarding picks materialization |
| Theme skip | `wasGardenRestoredFromRemote()` prevents onboarding world overwrite after restore |
| Deferred upload | Garden mutations call `DataSyncService.scheduleDeferredUpload()` — **5 min** WorkManager delay, unique work `firestore_deferred_upload`, `ExistingWorkPolicy.KEEP` |

### Firestore paths (unchanged)
```text
garden/{eduai_app_<studentId>}/state/current
garden/{eduai_app_<studentId>}/items/{itemId}
```
Payload still requires `appName: "eduai_app"` + matching `studentId` (rules).

### Primary files
- `app/.../service/sync/GardenRestorePolicy.kt` *(new)*
- `app/.../service/sync/GardenSyncManager.kt`
- `app/.../service/sync/DataSyncService.kt`
- `app/.../ui/screens/home/viewmodel/HomeViewModel.kt`
- `app/.../repository/GardenRepository.kt` (mutation → deferred upload)
- `app/.../service/analytics/EngagementAnalyticsTracker.kt` (`restore_applied` / `restore_skipped`)
- `app/src/test/.../GardenRestorePolicyTest.kt`

### Review / QA
1. Fresh install → sign in as user with remote plants → plants appear (no empty starter wipe).  
2. Mid-task local steps > 0 → restore skipped (`restore_skipped` / `local_progressed`).  
3. Plant a flower → kill app quickly → relaunch / sync → plant still on cloud.  
4. Onboarding world pick does not clobber restored remote theme.

### Residual
- `awaitGardenRestore()` waits for **full** `onUserAuthenticated` (garden + economy), not garden-only. Naming is slightly broader than garden.

---

## §2 — Onboarding picks → Firestore profile

### Problem addressed
Reinstall / new device re-showed onboarding because picks lived only in SharedPreferences.

### What we built

| Piece | Behavior |
|-------|----------|
| Write on finish | `FirebaseRepository.updateOnboardingPicks` → `users/{id}.onboarding` |
| Hydrate on login | `UserViewModel` loads picks into prefs **before** navigator gates |
| Applied flag | Cloud `picksApplied` set after Home materializes; **hydrate does not** set local `picksApplied` so Home still materializes once |
| Skip duplicate plan | If `getActivePlan` exists after restore, skip creating onboarding plan |
| Logout | `clearAllUserData()` clears onboarding prefs (no leak across accounts) |

### Cloud shape (illustrative)
```text
users/{userId}.onboarding = {
  firstRunCompleted, subject, chapter, world, picksApplied, completedAt,
  subjectId?, chapterId?
}
```
`getOnboardingPicks` returns null unless `firstRunCompleted == true`. Hydrate is also skipped if local `hasCompletedFirstRun()` is already true.
### Primary files
- `app/.../repository/FirebaseRepository.kt` (`updateOnboardingPicks`, `getOnboardingPicks`, `markOnboardingPicksApplied`)
- `app/.../ui/screens/login/viewmodel/UserViewModel.kt`
- `app/.../ui/navigation/LoginNavigator.kt`
- `app/.../data/local/SharedPreferenceUtils.kt`
- `app/.../ui/screens/home/viewmodel/HomeViewModel.kt` (apply picks once)

### Review / QA
1. Complete onboarding → confirm Firestore `users/{id}.onboarding`.  
2. Clear app data / reinstall → sign in → onboarding skipped; subject/world restored.  
3. Logout → login as different user → no leftover picks.

---

## §3 — Exam plan + XP/gems + quests sync

### Problem addressed
Reinstall reset XP/gems/friend code, lost exam plan, and could double-claim daily quests.

### Design choices (from review notes R.2 / R.4 / R.6)

| Decision | Rationale |
|----------|-----------|
| **Profile LWW only** | Do **not** replay XP/gem ledgers to recompute balances |
| Placeholder yield | Local null **or** `lifetimeXp == 0 && weeklyXp == 0 && gems == 0` yields to remote |
| Quest claims OR-merge | Once claimed anywhere, stays claimed |
| Claim-site heal (R.4) | If gem grant key exists locally, force claimed flag even if UI raced restore |
| Trial items | Rematerialize locally after plan restore (not full cloud trial archive) |
| Rules | Explicit allow paths for gamification / exam_plans / quests |

### Sync managers

| Manager | Push | Restore |
|---------|------|---------|
| `GamificationSyncManager` | Dirty profiles | LWW by `updatedAt` |
| `ExamPlanSyncManager` | Active plan + days | Plan + days; trials rematerialized later locally |
| `QuestSyncManager` | Today’s quest if `!isSynced` | Max progress counters; OR claim flags |

Wired from: `DataSyncService` (login restore + deferred/full sync), `DataSyncWorker`, `WeeklySyncWorker`.

### Firestore paths
```text
gamification/{eduai_app_<id>}/profile/current
exam_plans/{eduai_app_<id>}/current/plan
exam_plans/{eduai_app_<id>}/days/{dayId}
quests/{eduai_app_<id>}/daily/{yyyy-MM-dd}
```

### Rules (deployed to `eduai-e090e`)
```text
match /gamification/{parentDoc}/profile/{docId}
match /exam_plans/{parentDoc}/current/{docId}
match /exam_plans/{parentDoc}/days/{dayId}
match /quests/{parentDoc}/daily/{questDate}
```
Same pattern: eduai app parent doc + `studentIdMatchesParent`; delete denied.  
**Security note:** current rules are TEMPORARY open (no `request.auth`) for legacy clients — wrong `appName`/parent is rejected, but a correctly shaped unauthenticated write is still allowed until Auth bridge re-gates.
### Quest claim hardening
- `QuestDailyDao.mark*Claimed` sets `isSynced = 0`
- Claim / refresh call `scheduleQuestUpload()` → **`scheduleDeferredUpload()` only** (same 5 min KEEP job — **no immediate claim flush**)
- `QuestRepository.healClaimFromGemGrant` before claim / canClaim (same-install race only; grant keys are local)
- Hilt: `RepositoryModule.provideQuestRepository` injects `GamificationRepository`
### Primary files
- `app/.../service/sync/GamificationSyncManager.kt` *(new)*
- `app/.../service/sync/ExamPlanSyncManager.kt` *(new)*
- `app/.../service/sync/QuestSyncManager.kt` *(new)*
- `app/.../service/sync/DataSyncService.kt` / `DataSyncWorker.kt` / `WeeklySyncWorker.kt`
- `app/.../repository/FirebaseRepository.kt` (save/get profile, plan, quest)
- `app/.../repository/QuestRepository.kt`, `GamificationRepository.kt`, `ExamPlanRepository.kt`
- `app/.../di/RepositoryModule.kt`
- `firestore.rules` + deploy via `scripts/deploy-firestore-rules.py`

### Review / QA
1. Earn XP/gems → deferred/full sync → Firestore profile balances update.  
2. Create exam plan → reinstall → plan + days restored; trials rebuild.  
3. Claim quest → reinstall same day → claim UI disabled; no second gem grant if claim flags synced.  
4. Rules: wrong `appName` / mismatched `studentId` parent rejected; **auth not required** under current TEMPORARY rules (open-write risk until Auth bridge).

### Residuals / risks
- **Gem grant keys are local-only.** Reinstall double-grant protection depends on **synced claim flags**, not a cloud ledger. Claim → cloud currently waits on the **5 min** deferred job — reinstall before that upload is the sharp window (see RV.1).  
- Trial rematerialize is **lazy** via `PlanTrialRepository.ensureTrialItemsForDay` (not inside `ExamPlanSyncManager`).  
- XP/gem **event** audit upload still out of scope (balances via profile LWW are enough for v1).  
- Historical quest archive out of scope (today only).

---

## §4 — New-flow analytics (docs)

### What changed
No new event instrumentation required for the P1 list. `PRE_LAUNCH_CHECKLIST.md` updated so items **#7–15** read **Done** with pointers to existing trackers (`onboarding_*`, `home_tour_*`, `nav_walkthrough_*`, streak overlays, notif primer, review, place picker, plan reward banner).

### Remaining for QA (not code)
- Firebase DebugView smoke of #7–15 event names  
- Optional dashboard alias cleanup if BI used older spellings

---

## §5 — Kannada UI sweep

### Pattern
- **ui-kit:** optional `*Copy` data classes with English defaults  
- **app module:** factories / `*Copy` objects keyed by `isKannadaLanguage(languageCode)`  
- **XML:** `values-kn/strings.xml` parity with `values/strings.xml`

### EN ↔ KN resource parity
| Metric | Value |
|--------|-------|
| EN keys | 315 |
| KN keys | 315 |
| Missing | **0** |

Added KN entries for legal / Padaams / password / notification cancel (previously missing).

### Surfaces shipped

| Priority | Surface | Mechanism |
|----------|---------|-----------|
| P0 | Streak celebration / extended | `StreakCopy` + `StreakCopyFactory` |
| P0 | Nav walkthrough | `NavTourCopy` + `EduIntroTourOverlay` chrome labels |
| P1 | Friends | `FriendsCopy` (screen + ViewModel snackbars) |
| P1 | Leagues | `LeaguesCopy` / `LeaguesCopyFactory` + localized tier titles |
| P1 | Quest claim dialog | `QuestClaimUi` + dialog CTAs |
| P2 | HomeProgressRail | `HomeProgressRailCopy` *(component unused in app host today)* |
| P2 | EduRatingDialog | `RatingDialogCopy` *(live app uses Play In-App Review only)* |
| P2 | Notification settings | `NotificationSettingsCopy` |
| P2 | Avatar settings | `TutorAvatarSettingsCopy` |
| P3 | Moment XP/gems chips | `MomentOverlay` label params + `RewardMomentCopy` |
| P3 | RewardOverlay | `RewardOverlayCopy` *(host still suppresses popup)* |
| Extra | Notification channel names | Localized in settings + `NotificationChannels.ensureCreated`; refresh on language change |

Moment **headlines/bodies/CTAs** were already bilingual via `MomentVariants` KN catalog; P3 only finished chip labels.

### Key new / updated files (Kannada)
**App**
- `utils/StreakCopyFactory.kt`, `NavTourCopy.kt`, `FriendsCopy.kt`, `LeaguesCopyFactory.kt`
- `utils/ProgressRatingCopyFactory.kt`, `NotificationSettingsCopy.kt`, `TutorAvatarSettingsCopy.kt`, `RewardMomentCopy.kt`
- `ui/navigation/BottomNavBar.kt`, `ui/screens/home/GamifiedHomeRoute.kt`
- `ui/screens/friends/*`, `ui/screens/leagues/*`, `ui/screens/quests/QuestClaimUi.kt`, `ui/components/QuestClaimDialog.kt`
- `ui/screens/setting/NotificationSettingsScreen.kt`, `.../TutorAvatarSettingsSection.kt`
- `notification/NotificationChannels.kt`, `setting/viewmodel/SettingViewModel.kt` (channel rename on language change)
- `res/values-kn/strings.xml`

**ui-kit**
- `StreakCopy.kt`, `StreakCelebration.kt`, `IntroTourOverlay.kt`
- `LeaguesCopy.kt`, `LeaguesScreen.kt`, `LeagueLeaderboard.kt` (hero labels)
- `HomeProgressRailCopy.kt`, `HomeProgressRail.kt`
- `RatingDialogCopy.kt`, `EduRatingDialog.kt`
- `RewardOverlayCopy.kt`, `RewardOverlay.kt`, `MomentOverlay.kt`

### Review / QA (Kannada)
1. Settings → Kannada; cold start.  
2. Walk: onboarding → home → nav tour → streak greeting → plan → friends → leagues → quest claim → notification settings.  
3. Switch back to English — no leftover KN hardcodes on those surfaces.  
4. Diff `values` vs `values-kn` — still 0 missing keys.

### Residuals (polish, not P1 blockers)
- QuestTrail node label `"Claimed"` still EN in ui-kit  
- Home-rail tour chrome (phase 1) may still use EN defaults in places  
- Optional CI: fail if EN string key lacks KN counterpart  
- Avatar preset **names** in settings chips remain English brand names

---

## Cross-cutting: login restore sequence

Target order inside authenticated session (as implemented):

```text
1. Fetch users/{id} → hydrate onboarding prefs (§2)
2. DataSyncService.onUserAuthenticated:
     garden restore → gamification restore → exam plan restore → quest restore → push dirty
3. Home awaits awaitGardenRestore() before garden UI / apply picks
4. Deferred upload on hot paths (garden, economy, quests, plan mutations)
```

---

## Firestore rules deployment

| Item | Detail |
|------|--------|
| Project | `eduai-e090e` |
| Script | `scripts/deploy-firestore-rules.py` |
| New matches | gamification profile, exam_plans, quests daily |
| Status | Deployed during implementation session |

Reviewers should confirm current console ruleset still includes those three domains.

---

## Suggested review checklist (for PR / peer review)

### Correctness
- [ ] GardenRestorePolicy: only items+steps gate restore  
- [ ] Home never plants starter over remote garden after login  
- [ ] Onboarding hydrate does not set local `picksApplied`  
- [ ] Gamification restore is LWW, not ledger replay  
- [ ] Quest claims OR-merge + heal-from-grant-key  
- [ ] Rules match client paths (`eduai_app_<id>` parent docs)

### Localization
- [ ] KN cold-start walk of first-run surfaces  
- [ ] EN↔KN XML key parity still 0 missing  
- [ ] Language toggle refreshes notification channel display names

### Safety / scope
- [ ] No XP/gem ledger cloud replay (intentional)  
- [ ] Reward overlay still suppressed by host (intentional product choice)  
- [ ] No secrets committed; rules deploy used existing project tooling

### Out of scope (do not block this P1)
- Full Kannada coach hooks for every `_kn` sim (EduAI_app content repo)  
- Translating AI tutor model replies  
- Historical quest / trial-item cloud archive  
- Gem grant-key cloud ledger

---

## Related docs

| Doc | Role |
|-----|------|
| [P1_PRODUCT_GAPS_DETAILED.md](P1_PRODUCT_GAPS_DETAILED.md) | Original gap spec + review notes + short implementation table |
| [PRE_LAUNCH_CHECKLIST.md](PRE_LAUNCH_CHECKLIST.md) | Launch checklist; analytics #7–15 status |
| [firestore-sync-review.md](firestore-sync-review.md) | Broader sync architecture notes (if present) |

---

## Change inventory (modules)

| Module | Nature of changes |
|--------|-------------------|
| `app` | Sync managers, Firebase repo APIs, Home/Login/Quest/Friends/Leagues/Settings, Copy factories, KN strings |
| `ui-kit` | Optional Copy params on streak/tour/leagues/rail/rating/moment/reward |
| `firestore.rules` | Economy / plan / quest paths |
| `docs` | Spec updates, this handoff |

Exact file list for commit: use `git status` / `git diff` at review time (working tree was uncommitted at doc authoring).

---

# Reviewer notes / open risks (2026-08-15)

Independent review of this handoff **against the working tree**. **Verdict: accurate — every "Shipped" claim I checked is present in code, and the implementation faithfully lands review points R.1–R.4 and R.6.** The items below don't contradict the doc; they sharpen residuals a reviewer should weigh before merge.

## RV.0 — Verified against code

| Claim | Evidence |
|-------|----------|
| §1 pristine = items+steps only (R.1) | `service/sync/GardenRestorePolicy.kt:15` `localItemCount == 0 && localSteps == 0`; header comment: starter route `"1"`, onboarding theme, preferredSlot all ignored so remote wins |
| §1 restore gate + theme skip | `DataSyncService.awaitGardenRestore()` / `wasGardenRestoredFromRemote()`; used in `HomeViewModel.kt:335, 383, 484, 497` |
| §1 telemetry | `EngagementAnalyticsTracker` `restore_applied` / `restore_skipped`; outcome codes incl. `SKIPPED_LOCAL_PROGRESS("local_progressed")` |
| R.3 hot-path coalesce | `DataSyncService` `enqueueUniqueWork(…, ExistingWorkPolicy.KEEP)` with comment naming the pitfall ("a timestamped name would defeat KEEP") |
| R.2 profile LWW | `GamificationSyncManager.kt:14` "never recompute balances by replaying XP/gem ledgers"; all-zero local yields to remote |
| R.4 claim heal + DI | `QuestRepository.healClaimFromGemGrant` at 6 claim sites (`:126,136,146,158,165,172`); `di/RepositoryModule.kt` injects `GamificationRepository` into `provideQuestRepository` |
| R.6 rules | `firestore.rules` gamification/exam_plans/quests matches; **`allow read` present** (restore won't 403); `allow delete: if false` |
| §2 onboarding | `FirebaseRepository` `updateOnboardingPicks` / `getOnboardingPicks` / `markOnboardingPicksApplied` |
| §5 parity | EN=315, KN=315, `comm` diff = **0 missing keys**; residual `ui-kit/.../QuestTrail.kt:382 "Claimed"` still EN, exactly as admitted |

> **Update (2026-08-15): RV.1, RV.2, RV.3 resolved in the working tree** (see per-item "Resolution" notes). RV.4/RV.5 remain PR-hygiene. Changes are uncommitted; Cursor to compile + commit.

## RV.1 — Quest double-grant window is sharper than the §3 residual states *(RESOLVED)*

The §3 residual says protection "depends on synced claim flags." The teeth: `healClaimFromGemGrant` reads **local** gem-grant keys, which are wiped on reinstall. So the heal protects the **same-install** race (Bug-B class), but **not** the reinstall-before-claim-upload case:

> claim → gems granted locally, claim flag `isSynced = 0` → user reinstalls **before** the debounced upload fires → new device restores the remote quest with **no** claim flag and **no** local grant key to heal from → quest looks claimable again → extra gems.

**Resolution (shipped):** quest claim now uploads **immediately** instead of on the ~5-min debounce.
- `DataSyncService.scheduleImmediateUpload()` — new: enqueues a **no-delay** `DataSyncWorker` under its own unique name `firestore_immediate_upload` with `ExistingWorkPolicy.REPLACE` (runs now, not dedup-blocked by a pending deferred upload; worker network-constraint + retry keep it durable across process death).
- `QuestRepository.scheduleQuestUpload()` — now calls `scheduleImmediateUpload()` (was `scheduleDeferredUpload()`). All `claim*()` paths route through it.
- Window shrinks from ~5 min to worker-scheduling latency (seconds). Fully closing it (offline claim → reinstall before the worker runs) would still need claim flags in the profile LWW doc — left as optional belt-and-suspenders, noted below.
- **Files:** `service/sync/DataSyncService.kt`, `repository/QuestRepository.kt`.

## RV.2 — `awaitGardenRestore()` gates Home on the *full* restore sequence *(RESOLVED)*

`awaitGardenRestore()` is `gardenRestoreGate.await()` with **no timeout**. It waits for garden **and** economy/plan/quest restore (then `triggerFullSync()` is started — push completion is **not** awaited). Gate **is** completed in a `finally` on `onUserAuthenticated` (throws still free Home). Remaining risk: slow network stalls Home until restores finish.

**Resolution (shipped):** wrapped the restore sequence in `withTimeoutOrNull(RESTORE_TIMEOUT_MS = 20s)`. On timeout it falls through, re-asserts `updateStudentId`, logs, and the existing `finally` still completes the gate — Home renders within ≤20s even if a restore call hangs. `gardenRestoredFromRemote` stays `false` on timeout (safe: onboarding won't skip theme). **File:** `service/sync/DataSyncService.kt`.

## RV.3 — Test coverage thin on the money-bearing code *(PARTIALLY RESOLVED)*

**Resolution (shipped):** extracted the Gamification LWW decision into a pure, unit-testable helper (mirroring the `GardenRestorePolicy` + test pattern) and covered it:
- `GamificationSyncManager.localProfileWins(...)` — new pure companion function; `restoreProfile()` now routes its decision through it (no behaviour change).
- `app/src/test/.../GamificationLwwTest.kt` — new: missing→remote, zero-placeholder→remote (even if newer), non-placeholder+newer→local, timestamp tie→local (no re-apply), older→remote, gems-only counts as real.

**Still to add** (needs DAO/Firebase mocking, out of this pass): a Quest OR-merge test and the RV.1 reinstall scenario (claim → wipe local grant key → restore → not re-claimable). These want `mockk`-style manager tests rather than pure functions.

## RV.4 — Rules deployed from an uncommitted tree *(PR hygiene)*

Rules were deployed to `eduai-e090e` "during implementation," but the working tree is uncommitted. Ensure the committed `firestore.rules` is **byte-identical** to the live ruleset and included in the PR, or repo and console can silently drift.

## RV.5 — "0 missing keys" ≠ "0 untranslated values" *(minor)*

Parity is key-count based; it won't catch a `values-kn` entry whose value is still English. Spot-check the newly added legal / Padaams / password / notification strings; the optional CI guard the doc lists is the durable fix.

## RV.6 — Net

Design is sound and the code matches the handoff. **RV.1 (immediate claim upload), RV.2 (restore-gate timeout), and RV.3 (LWW helper + test) are now implemented in the working tree** — Cursor to compile + commit. Remaining before merge: **RV.4** (confirm committed `firestore.rules` == live ruleset), the RV.3 follow-on manager tests (Quest OR-merge + reinstall scenario), and **RV.5** spot-check. Net: no open *gating* code items.

### Files changed in this pass
- `service/sync/DataSyncService.kt` — `scheduleImmediateUpload()`, `withTimeoutOrNull` restore gate, consts `IMMEDIATE_UPLOAD_WORK` / `RESTORE_TIMEOUT_MS`.
- `repository/QuestRepository.kt` — claim upload now immediate.
- `service/sync/GamificationSyncManager.kt` — `localProfileWins()` pure helper + routed `restoreProfile` through it.
- `app/src/test/.../GamificationLwwTest.kt` — new.
