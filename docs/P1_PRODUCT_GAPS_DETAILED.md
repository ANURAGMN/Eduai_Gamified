# P1 Product Gaps — Detailed Change Spec

**App:** `com.ncert7.aitutorandlab` (Eduapp / gamified)  
**Date:** 2026-08-14  
**Related:** [PRE_LAUNCH_CHECKLIST.md](PRE_LAUNCH_CHECKLIST.md) · [COMPLIANCE_BLOCKERS.md](store-listing/COMPLIANCE_BLOCKERS.md) · [firestore-sync-review.md](firestore-sync-review.md) · **[P1_IMPLEMENTATION_REVIEW.md](P1_IMPLEMENTATION_REVIEW.md)** (2026-08-15 — what was built, for peer review)

This document expands the five **strong P1** first-impression gaps into implementable specs: current state, user impact, exact files, and change steps. Work them **in order** below unless product re-prioritizes.

> **Checklist note (2026-08):** `PRE_LAUNCH_CHECKLIST.md` (Jun 20) is **partially stale**. Garden sync and most new-flow analytics **already exist in code**; what remains is correctness / polish / still-missing sync domains. This doc is the source of truth for these five items until the checklist is updated.

---

## Recommended build order (within P1)

| Step | Item | Effort (rough) | Why first |
|------|------|----------------|-----------|
| 1 | §1 Garden sync hardening | 0.5–1 day | **Retention first** — silent restore no-op is live data loss (R.8) |
| 2 | §2 Onboarding picks → Firestore profile | 1 day | Reinstall / new device re-shows onboarding |
| 3 | §3 Exam plan + XP / gems / quests sync | ~1 week | Highest remaining reinstall pain after garden |
| 4 | §4 New-flow analytics | Hours | Mostly done — docs + DebugView smoke |
| 5 | §5 Kannada UI sweep (streak + nav tour first) | 1–2 days | Polished first KN session (conversion) |

---

# 1. Garden Firestore sync — harden restore + plant-time upload

## 1.1 User impact

| Scenario | Today |
|----------|--------|
| Reinstall / new phone, empty local DB | Restore **intended** via `GardenSyncManager.restoreGarden` on login |
| Login then Home opens before restore finishes | `ensureState()` may insert a local starter row → pristine check fails → **remote garden never applied** |
| Plant a flower mid-session, kill app before background sync | Push only on full/weekly sync — short window of data loss |
| Checklist #16 (“Local Room only”) | **Wrong** — sync is wired; bugs remain |

## 1.2 Current architecture (what already exists)

| Layer | Location | Role |
|-------|----------|------|
| Room | `GardenStateEntity`, `GrownItemEntity`, `GardenDao` | Local source of truth |
| Repo | `GardenRepository` | `recordStep`, `ensureState`, theme/slot/route |
| Cloud API | `FirebaseRepository.saveGardenState/Items`, `getGardenState/Items` | Paths under `garden/` |
| Sync | `GardenSyncManager` | `pushGarden`, `restoreGarden` |
| Hooks | `DataSyncService.onUserAuthenticated` → restore then full sync; `DataSyncWorker` / `WeeklySyncWorker` / `triggerFullSync` → push |

**Firestore paths** (do not move under `users/`):

```text
garden/{eduai_app_<studentId>}/state/current
garden/{eduai_app_<studentId>}/items/{itemId}
```

Payload must keep `appName: "eduai_app"` + matching `studentId` (see `firestore.rules`).

**Room fields today**

| Entity | Fields | Missing vs progress/streak |
|--------|--------|----------------------------|
| `garden_state` | `studentId`, `theme`, `route`, `steps`, `preferredSlot` | No `isSynced`, no `updatedAt` |
| `garden_item` | `id`, `studentId`, `zone`, `plot`, `slot`, `conceptId`, `chapterId`, `kind`, `completedAt` | No `isSynced` |

## 1.3 Bugs / gaps to fix

### Bug A — Pristine detection vs starter route (restore no-op)

- `GardenRepository.ensureState` inserts `route = STARTER_GARDEN_ZONE` (`"1"`).
- `GardenSyncManager.isPristinePlaceholder` requires `route == "0"`.
- Any early `getProgress` / `observeProgress` → local row looks “progressed” → `canRestoreFromRemote` returns false.

**Change:** Treat as pristine when **no grown items** and `steps == 0`, and route is blank / `"0"` / starter garden / starter outpost (or simply `countItems == 0 && steps == 0`). Align with `ensureState` defaults.

**File:** `app/src/main/java/com/ncert7/aitutorandlab/service/sync/GardenSyncManager.kt`

### Bug B — Race: Home `ensureState` before restore completes

**Change (pick one, prefer combining 1+2):**

1. `HomeViewModel.observeGardenProgress` / first garden read → `awaitGardenRestore()` first (same gate as `applyOnboardingPicksOnce`).
2. Allow restore to **overwrite** pristine-but-starter-route rows.
3. Optionally: don’t insert placeholder until restore has finished for this session.

**Files:** `HomeViewModel.kt`, `GardenSyncManager.kt`, optionally `GardenRepository.kt`

### Gap C — No plant-time / preference dirty upload

Hot paths (`recordStep`, `setTheme`, `setPreferredSlot`, `unlockZoneIfNeeded`, onboarding scene apply) write Room only.

**Change — Option A (minimal, recommended for launch):**  
After those mutations, call `DataSyncService.scheduleDeferredUpload()` (worker already `pushGarden`s).

**Change — Option B (proper, post-launch OK):**  
Add `isSynced` + `updatedAt` on state/items; DAO `getUnsynced*`; mark dirty on write; push only dirty; mark synced after success (mirror streak).

**Files:** `GardenRepository.kt`, optionally entities + `GardenDao.kt` + migration, `DataSyncService.kt`

### Gap D — Optional LWW for state

Today restore refuses if any local plant exists. Fine for v1 if Bug A/B fixed. Later: compare remote `updatedAt` vs local before overwrite.

### Gap E — Docs

Update `PRE_LAUNCH_CHECKLIST.md` #16 to: *Implemented; harden pristine + plant deferred upload.*

## 1.4 Verification

1. Plant 2–3 items on device A → background app → confirm Firestore `garden/.../items` + `state/current`.
2. Clear app data / install on device B → same Google account → garden plants + theme restored.
3. Force race: open Home immediately after login with remote garden present → still restores (Bug A/B).
4. Offline plant → come online → deferred upload pushes within worker window.

## 1.5 Out of scope

- Moving garden under `users/{id}`
- Syncing tour / primer flags

---

# 2. Onboarding picks → user profile (Firestore)

## 2.1 User impact

| Scenario | Today |
|----------|--------|
| Fresh install, same Google account | Sees full onboarding again (`first_run_completed` is device SharedPreferences only) |
| Completes onboarding on phone A, opens phone B | Phone B may force picks that **disagree** with restored garden theme |
| Logout / switch account on same device | First-run prefs are **not** cleared in `clearAllUserData()` — sticky across accounts |

Checklist #17 is still accurate: picks are **not** on the user profile.

## 2.2 Current architecture

| Data | Storage | API |
|------|---------|-----|
| First-run done | prefs `first_run_completed` | `hasCompletedFirstRun()` / `setFirstRunResult` |
| Subject / chapter / world | prefs `onboarding_subject`, `onboarding_chapter`, `onboarding_world` | getters via `SharedPreferenceUtils` |
| Picks applied once | prefs `onboarding_picks_applied` | `hasAppliedOnboardingPicks` / `setOnboardingPicksApplied` |
| Applied subject id | prefs `selected_subject_id` | separate from onboarding keys |

**Write today:** `LoginNavigator` `onFinish` → `setFirstRunResult(subject, chapter, world)` only (+ analytics). **No Firestore.**

**Apply today:** `HomeViewModel.applyOnboardingPicksOnce` → await garden restore → set theme / subject / exam plan → `setOnboardingPicksApplied()`.

**User model** (`User.kt` / `FirebaseRepository.createNewUser`): identity + class + language + `appName`. **No onboarding fields.**

**Gate:** `LoginNavigator` `"main"` → if `!hasCompletedFirstRun()` show `EduOnboardingScreen`.

## 2.3 Target schema (`users/{userId}`)

Merge into existing user doc (keep `appName: "eduai_app"` for rules):

```json
{
  "appName": "eduai_app",
  "onboarding": {
    "firstRunCompleted": true,
    "subject": "Math",
    "chapter": "<display label as today>",
    "world": "Garden",
    "picksApplied": true,
    "subjectId": "<optional resolved id>",
    "chapterId": "<optional resolved id>",
    "completedAt": 1710000000000
  }
}
```

Flat fields (`onboardingSubject`, …) are fine if you prefer fewer nested maps — pick one shape and stick to it.

## 2.4 Changes (step by step)

### Step 1 — Repository write/read

**File:** `FirebaseRepository.kt` (+ optional fields on `User.kt`)

- `updateOnboardingPicks(userId, subject, chapter, world, completedAt)` → `usersCollection.document(userId).set(..., SetOptions.merge())`
- `getOnboardingPicks(userId): OnboardingPicks?` from user doc (or reuse login fetch)

### Step 2 — Write on finish

**File:** `LoginNavigator.kt` (or a small `OnboardingPrefsWriter` / ViewModel method)

After `setFirstRunResult(...)`:

1. Resolve current `studentId` / Firebase user id (must be logged in before `"main"`).
2. Call `updateOnboardingPicks(...)`.
3. Keep existing analytics (`EngagementAnalyticsTracker` / funnel) unchanged.

### Step 3 — Mark applied in cloud

**File:** `HomeViewModel.applyOnboardingPicksOnce`

After local apply succeeds, merge `onboarding.picksApplied = true` (+ resolved `subjectId` / `chapterId` if available).

### Step 4 — Hydrate on sign-in (before first-run gate)

**File:** `UserViewModel.saveExistingUserLocally` / `submitNewUser`

After user doc is known / fetched:

1. If remote `onboarding.firstRunCompleted == true` → `setFirstRunResult(subject, chapter, world)`.
2. If remote `picksApplied` → optionally `setOnboardingPicksApplied()` **or** leave false so this device still applies subject/plan once.
3. Ensure this runs **before** navigation lands on `LoginNavigator` reading `hasCompletedFirstRun()`.

### Step 5 — Logout / multi-account

**File:** `SharedPreferenceUtils.clearAllUserData` (or logout path)

**Decision (recommend):** clear onboarding first-run + picks on logout so the next account hydrates from Firestore instead of inheriting the previous user’s sticky prefs.

### Step 6 — Do not sync (intentional)

Leave local-only: `home_tour_completed`, `nav_tour_completed`, notification primer counters, rating throttle prefs (see checklist “Keep local intentionally”).

## 2.5 Verification

1. Complete onboarding on device A → Firestore `users/{id}.onboarding` present.
2. Clear data / device B → sign in → **skip** onboarding UI; subject/world match; garden theme consistent when empty.
3. Logout → different account → no leaked first-run skip from previous account.
4. Offline finish → queue or retry merge when online (at least succeed on next full sync / login).

## 2.6 Out of scope

- Replacing GA4 onboarding events with profile fields (analytics stay; profile is persistence)
- Syncing walkthrough completion flags

---

# 3. Exam plan / XP / gems / quests sync

## 3.1 User impact

| Data | Reinstall today |
|------|-----------------|
| Progress / streak / chapter agent | Restored (existing outbox) |
| Garden | Intended restore (see §1) |
| **Active exam plan + trial day items** | **Lost** — user rebuilds plan |
| **XP, gems, league tier, friend code** | **Lost** — economy resets |
| **Daily quest claim state** | **Lost** — can re-claim / wrong UI |

`isSynced` exists on several entities but **nothing uploads them** (`ProgressAnalyticsSessionSyncManager` only handles progress, analytics, sessions, streak, chapterprogress).

## 3.2 Current inventory

| Entity | `isSynced`? | Mutations set dirty? | DAO unsynced query? | Upload / restore |
|--------|-------------|----------------------|---------------------|------------------|
| `ExamPlanEntity` | Yes | Default false; never marked true | No | None |
| `ExamPlanDayEntity` | **No** | — | No | None |
| `PlanTrialItemEntity` | **No** | — | No | None |
| `GamificationProfileEntity` | Yes | Set `false` on awards | No | None |
| `XpEventEntity` | Yes | Inserts dirty | No | None |
| `GemEventEntity` | Yes | Inserts dirty | No | None |
| `QuestDailyEntity` | Yes | Claims set `isSynced = 0` | No | None |

**Reference pattern to copy:** progress/streak outbox in `ProgressAnalyticsSessionSyncManager` + `DataSyncService.scheduleDeferredUpload` + login restore in `FirebaseSyncManager` / `GardenSyncManager`.

## 3.3 Part A — Exam plan + trial (checklist #18)

### Target Firestore shape

```text
exam_plans/{eduai_app_<studentId>}/current          # plan header
exam_plans/{eduai_app_<studentId>}/days/{dayId}
exam_plans/{eduai_app_<studentId>}/trial_items/{itemId}
```

(Or nest days/items as arrays on one doc if payload stays small — prefer subcollections if trial lists grow.)

Include: `appName`, `studentId`, `updatedAt`, plan metadata (exam date, subject, status), day status, trial item fields needed to rematerialize UI.

### Code changes

1. **Schema:** Add `isSynced` (+ `updatedAt` if LWW) to `ExamPlanDayEntity` / `PlanTrialItemEntity` (migration), **or** nest under plan and only dirty the plan row.
2. **DAO:** `ExamPlanDao` — `getUnsyncedPlans()`, mark helpers; same for days/items as needed.
3. **Repos:** Every `ExamPlanRepository` / `PlanTrialRepository` mutation → `isSynced = false` + `scheduleDeferredUpload()`.
4. **New** `ExamPlanSyncManager` (or extend progress manager carefully):
   - `pushExamPlan(studentId)`
   - `restoreExamPlan(studentId)` — if no local active plan **or** remote `updatedAt` wins → hydrate Room; rematerialize trial items if required.
5. **Hooks:** `DataSyncService.triggerFullSync`, `DataSyncWorker`, `WeeklySyncWorker`, `onUserAuthenticated` (restore before home applies onboarding plan).
6. **Rules:** Mirror garden/progress owner checks.

### Ordering with onboarding (§2)

On new device: restore exam plan **before** `applyOnboardingPicksOnce` creates a fresh onboarding plan, or skip local create if remote plan exists.

## 3.4 Part B — Gamification profile / XP / gems / quests (checklist #19)

### Target Firestore shape

```text
gamification/{eduai_app_<studentId>}/profile/current
gamification/{eduai_app_<studentId>}/xp_events/{eventId}
gamification/{eduai_app_<studentId>}/gem_events/{eventId}
gamification/{eduai_app_<studentId>}/quests/{questDate}
```

**Profile (authoritative balances):** `lifetimeXp`, `weeklyXp`, `gems`, `leagueTier`, `currentWeekKey`, `friendCode`, `cohortId`, `updatedAt`, `appName`, `studentId`.

**Events:** upload by stable unique keys so restore doesn’t double-grant. Prefer **profile LWW for balances** + **idempotent event ledger** for audit.

**Quests:** sync **current day** (or last N days) only — full history is low value.

### Code changes

1. **DAO:** `GamificationDao` — `getUnsyncedProfiles/XpEvents/GemEvents` + mark synced.  
   `QuestDailyDao` — `getUnsyncedQuests` + mark synced.
2. **New** `GamificationSyncManager`:
   - Push dirty profile + events + quests.
   - Restore on login: if local profile missing/zero and remote newer → apply profile; merge events by id before any replay.
3. **Hot path:** After `GamificationRepository.recordXpAward` / `grantGemsIfEligible` / quest claims → `scheduleDeferredUpload()`.
4. **Hooks:** same workers + `onUserAuthenticated`.
5. **Rules:** owner-only under `gamification/{eduai_app_*}/…`.
6. **Product call:** League **bots** stay local; restoring `weeklyXp` + `leagueTier` + `friendCode` is the user-visible win. `LeagueRepository.syncUserWeeklyXp` today updates **local** league DAO only — fold into profile sync or leave as display cache.

## 3.5 Verification

1. Earn XP / gems / claim quest → Firestore profile + events update after deferred upload.
2. Create exam plan, complete a trial day → remote days/items present.
3. Reinstall → sign in → XP/gems/friend code + active plan restored; quests not double-claimable for same day.
4. Conflict: offline award then restore — no double XP (idempotent event ids).

## 3.6 Out of scope for v1 (unless requested)

- Full historical quest archive
- Syncing league bot tables
- Analytics event mirror to Firestore (`AnalyticsFirestoreMirror` stays off)

---

# 4. New-flow analytics (checklist #7–15)

## 4.1 User / launch impact

GA4 funnels for onboarding, tours, streak UI, primer, review, place picker were listed as missing in Jun checklist. **Code now wires most of them** via `EngagementAnalyticsTracker` + `FunnelAnalyticsTracker` + `TrackScreenEvent`.

**Remaining work:** update docs, optionally dual-log aliases if dashboards used checklist names, smoke-check DebugView.

## 4.2 Status matrix (code reality)

| # | Checklist name | Status | Actual event name(s) | Wire site |
|---|----------------|--------|----------------------|-----------|
| 7 | `ScreenName.ONBOARDING` | Done | `TrackScreenEvent(ONBOARDING)` | `LoginNavigator.kt` |
| 8 | Onboarding funnel | Done (name diffs) | Funnel: `onboarding_start`, `onboarding_subject_selected`, `onboarding_chapter_selected`, `onboarding_world_selected`, `onboarding_complete`. Engagement: `onboarding_slide_view`, `onboarding_skip`, `onboarding_*_selected`, `onboarding_picks` | `LoginNavigator.kt` |
| 9 | Home tour | Done (name diffs) | `home_tour_start`, `home_tour_step` (not `*_step_view`), `home_tour_skip`, `home_tour_complete` | `GamifiedHomeRoute.kt`, `BottomNavBar.kt` |
| 10 | Nav walkthrough | Done (name diffs) | `nav_walkthrough_step` (not `*_step_view`), skip/complete | `BottomNavBar.kt` |
| 11 | Streak UI | Done | `streak_greeting_shown/continue`, `streak_extended_shown/done` | `GamifiedHomeRoute.kt` |
| 12 | Notif primer | Done (name diffs) | `notif_primer_shown/accepted/declined`, `notif_permission_result` | `NotificationPermissionGate.kt`, `NotificationPermissionHost.kt` |
| 13 | In-app review | Done | `review_requested{trigger}`, `review_throttled{reason}` | `GamifiedHomeRoute.kt` |
| 14 | Place picker | Done | `place_completed`, `next_place_offered/picked/surprise` | `PlanTrialViewModel.kt` |
| 15 | Plan reward banner | Done | `plan_reward_banner_tap{day_count}` | `PlanOverviewScreen.kt` |

**Already covered separately (no action for #7–15):** `GamificationAnalyticsTracker` — plan/trial, XP/gems, DB streak, quests, leagues, friends, avatar, etc.

## 4.3 Changes needed

### A. Documentation (required)

Update `PRE_LAUNCH_CHECKLIST.md` § P1 Analytics:

- Mark #7–15 **Done**.
- Document **actual** event names (table above).
- Note: keep DB `streak_extended` distinct from UI `streak_extended_shown/done`.

### B. Optional GA4 alias dual-log

Only if existing dashboards expect checklist names (`home_tour_step_view`, `primer_shown`, `os_permission_result`, …):

- In `EngagementAnalyticsTracker`, log **both** names once, or remap in BigQuery.
- Do **not** silently rename production events without a migration note.

### C. Smoke test (required before calling analytics “done”)

Firebase DebugView / Analytics Debug:

1. Fresh install → onboarding slides → picks → complete.
2. Home tour → nav walkthrough.
3. Streak greeting + extended overlay.
4. Notification primer accept/decline + OS permission result.
5. Complete a place → next-place picker.
6. Tap plan reward banner.
7. Trigger review gate (or throttle path).

### D. No new Compose wiring expected

Unless smoke test finds a call site that never fires — then fix that one site; don’t re-implement the tracker.

## 4.4 Param reference (keep stable)

| Event | Params |
|-------|--------|
| `onboarding_slide_view` / skip | `index` |
| subject / chapter / world | `subject` / `chapter` / `world` |
| `onboarding_picks` | `subject`, `chapter`, `world` |
| `home_tour_step` / skip | `step` (0..2) |
| `nav_walkthrough_step` | `route`, `step` |
| streak UI | `streak` |
| primer shown | `variant`, `attempt` |
| permission | `granted` |
| review | `trigger`, throttle `reason` |
| place flow | `zone` / `candidates` |
| banner | `day_count` |

---

# 5. Kannada UI sweep

## 5.1 User impact

Home chrome EN↔KN was fixed (plan/revision/friends/bookmarks/textbooks/hero). First-run and secondary surfaces still show **English** when language is Kannada — especially streak overlays and nav walkthrough.

## 5.2 How language works today

| Layer | Mechanism |
|-------|-----------|
| System locale | `LanguageHelper.setLanguage("en"|"kn")` → AppCompat locales → `values` / `values-kn` |
| Settings | `SettingViewModel.setLanguage` → prefs + student language dirty |
| Syllabus content | Entity `*Kannada` fields via mappers |
| Gamified chrome | `*Copy` objects / factories pass localized strings into ui-kit (ui-kit defaults stay English) |

**Pattern to reuse:** `HomeCopy.kt`, `ExamPlanCopy.kt`, `TrialCopy.kt`, `GardenCopyFactory.kt`, `OnboardingCopy.onboardingStrings(languageCode)`.

## 5.3 Missing `values-kn` string keys

Diff `app/src/main/res/values/strings.xml` vs `values-kn/strings.xml` — add KN for at least:

- `policy_msg_prefix`, `policy_msg_suffix`
- `terms_of_service_*`, `privacy_policy_*`, `legal_section_title`
- `padaams_*`
- `password_label`, `sign_in_with_email`
- `notification_action_cancel`

(Re-diff before shipping — list may grow.)

## 5.4 Hardcoded English offenders (priority order)

| Priority | Surface | File(s) | Examples |
|----------|---------|---------|----------|
| P0 | Streak overlays | `ui-kit/.../StreakCelebration.kt` | `"day streak"`, `"Let's go"`, `"Streak extended!"`, weekday letters |
| P0 | Nav walkthrough | `app/.../BottomNavBar.kt` | `"Your exam planner"`, step bodies |
| P1 | Friends | `FriendsScreen.kt` | `"Friends"`, `"Accept"`, `"Copy code"`, `"Add friend"` |
| P1 | Leagues | `LeaguesScreen.kt` | `"Leagues"` |
| P1 | Quest claim | `QuestClaimDialog.kt` | `"Watch short video · +N gems"`, `"Not now"` |
| P2 | Home progress rail | `ui-kit/.../HomeProgressRail.kt` | `"Your week"`, `"See all"` |
| P2 | Rating dialog | `ui-kit/.../EduRatingDialog.kt` | `"What can we improve?"`, `"Not now"` |
| P2 | Notification settings | `NotificationSettingsScreen.kt` | `"Enable in system settings"`, `"OK"` |
| P2 | Avatar settings | `TutorAvatarSettingsSection.kt` | `"Customize in Avatar Studio"` |
| P3 | Moments / rewards | `MomentOverlay.kt`, `RewardOverlay.kt` | `"XP"`, `"gems"` labels |
| Guard | ui-kit defaults | `PlanTrail`, `QuestTrail`, `HomeRails`, `HomeScreen` defaults | Safe only if every call site passes Copy |

## 5.5 Changes (systematic sweep)

### Step 1 — Inventory

1. Diff XML EN vs KN keys → checklist.
2. Grep ui-kit + app UI: `Text("…")`, English default params, `navWalkthroughSteps`.
3. Audit every call site that constructs Home / Plan / Quest / Streak / Nav tour UI.

### Step 2 — Patterns (match Home)

| Kind of UI | Approach |
|------------|----------|
| Login / legal / settings using `stringResource` | Add `values-kn` entries |
| Compose chrome with many labels | New `StreakCopy`, `NavTourCopy`, or extend `HomeCopy`; pass from app module |
| ui-kit components | Optional label params; no new hardcoded EN once callers exist |
| Garden | Extend `GardenCopyFactory` |

### Step 3 — Implement in priority order

1. Streak overlays + nav walkthrough  
2. Friends / Leagues / Quest claim  
3. Missing legal / Padaams XML keys  
4. HomeProgressRail / rating dialog  
5. Low-priority settings / debug strings  

### Step 4 — Verification

1. Settings → Kannada; cold start with `kn`.
2. Walk: onboarding → home tour → nav tour → streak greeting → plan → friends → leagues → quest claim.
3. Switch back to English — no leftover KN hardcodes.
4. Keep / extend `LanguageConsistencyTest`-style coverage for plan/trial labels.

## 5.6 Out of scope

- Full Kannada coach hooks for every science/math `_kn` sim (content repo `EduAI_app` — separate track)
- Translating AI tutor model replies (backend / prompt concern)

---

# Cross-cutting: login restore sequence (after §1–§3)

Target order inside `UserViewModel` auth success → `"main"`:

```text
1. Fetch users/{id} → hydrate onboarding prefs (§2)
2. Restore progress / streak / chapter agent (existing)
3. await / restore garden with fixed pristine rules (§1)
4. Restore gamification profile + exam plan (§3)
5. triggerFullSync (push any local dirty)
6. Navigate main
   → LoginNavigator respects hydrated first_run
   → Home applyOnboardingPicksOnce only if needed (skip plan create if remote plan exists)
```

---

# File index (quick)

| Area | Key paths under `app/src/main/java/.../aitutorandlab/` |
|------|--------------------------------------------------------|
| Garden | `data/local/entities/Garden*.kt`, `repository/GardenRepository.kt`, `service/sync/GardenSyncManager.kt`, `service/sync/DataSyncService.kt` |
| Prefs / onboarding | `data/local/SharedPreferenceUtils.kt`, `ui/navigation/LoginNavigator.kt`, `ui/screens/home/viewmodel/HomeViewModel.kt`, `ui/screens/login/viewmodel/UserViewModel.kt`, `data/firebase/model/User.kt`, `service/.../FirebaseRepository.kt` |
| Plan / economy | `entities/ExamPlan*.kt`, `PlanTrialItemEntity.kt`, `Gamification*.kt`, `QuestDailyEntity.kt`, `repository/ExamPlanRepository.kt`, `GamificationRepository.kt`, `QuestRepository.kt`, `service/sync/ProgressAnalyticsSessionSyncManager.kt` |
| Analytics | `service/analytics/EngagementAnalyticsTracker.kt`, `FunnelAnalyticsTracker.kt`, `GamificationAnalyticsTracker.kt`, `AnalyticsEnums.kt` |
| Kannada | `ui-kit` Streak/Home/Plan components; app `HomeCopy.kt`, `BottomNavBar.kt`, `FriendsScreen.kt`, `LeaguesScreen.kt`; `res/values-kn/strings.xml` |
| Rules | repo root `firestore.rules` |

---

# Definition of done (all five)

- [ ] §1 Garden: reinstall restores plants; race + pristine fixed; plant triggers deferred upload  
- [ ] §2 Onboarding: reinstall skips onboarding; prefs hydrated from `users/{id}`; logout doesn’t leak picks  
- [ ] §3 Plan + economy: reinstall restores plan + XP/gems/friend code; quests don’t double-claim  
- [ ] §4 Analytics: checklist updated; DebugView smoke for #7–15 passes  
- [x] §5 Kannada: streak + nav tour + Friends/Leagues/quest claim + missing XML keys localized; EN↔KN cold-start walk clean
  - Residual polish: QuestTrail “Claimed”, home-rail tour chrome, optional string-parity CI  

When this checklist is green, update `PRE_LAUNCH_CHECKLIST.md` statuses to match and link here.

---

# Review notes (2026-08-15)

Technical review of this spec against the current `Eduapp` codebase. **Verdict: accurate and buildable.** Every load-bearing claim checked below matched the code. The items after that are issues, risks, and gaps to resolve *before* building — they don't invalidate the spec, they tighten it.

## R.0 Claims verified against code

| Claim in spec | Verified? | Evidence |
|---------------|-----------|----------|
| §1 Bug A: `ensureState` writes starter route, pristine check wants `"0"` | ✅ Confirmed | `STARTER_GARDEN_ZONE = 1` (`ui-kit/.../garden/quest/QuestZones.kt:39`); `GardenRepository.ensureState` sets `route = STARTER_GARDEN_ZONE.toString()` → `"1"` (`GardenRepository.kt:245`); `GardenSyncManager.isPristinePlaceholder` requires `state.route == "0"` (`GardenSyncManager.kt:88`). A pristine starter row therefore never passes → `canRestoreFromRemote` returns false → restore no-ops. |
| §1 pristine also gates on theme/slot | ✅ (see R.1) | `isPristinePlaceholder` additionally requires `theme == GardenTheme.GARDEN` **and** `preferredSlot == -1` (`GardenSyncManager.kt:86–89`). |
| §3.2 `isSynced` inventory | ✅ Exact | `ExamPlanEntity` yes, `ExamPlanDayEntity` no, `PlanTrialItemEntity` no, `GamificationProfileEntity`/`XpEventEntity`/`GemEventEntity`/`QuestDailyEntity` yes. |
| §3.1 "nothing uploads economy" | ✅ Confirmed | `ProgressAnalyticsSessionSyncManager.kt` has **zero** references to ExamPlan/Gamification/Xp/Gem/Quest. |
| §5 P0 hardcoded EN in streak | ✅ Confirmed | `ui-kit/.../components/StreakCelebration.kt` hardcodes `"day streak"` (:83, :143), `"Let's go"` (:102), `"Streak extended!"` (:140). No `StreakCopy.kt` / `NavTourCopy.kt` exists yet. |

## R.1 Bug A fix drops two guards that §2 relies on (blocking — resolve before build)

`isPristinePlaceholder` today is `steps == 0 && theme == GARDEN && route == "0" && preferredSlot == -1`. The proposed relaxation to *"`countItems == 0 && steps == 0`"* silently **removes the theme and preferredSlot conditions**.

Why it matters: §2 (onboarding) applies a **theme** (`world`) on the new device. §1 and §2 both write `garden_state.theme`, so they are coupled — a fresh device can have `theme != GARDEN` and `preferredSlot != -1` *before* garden restore runs. Under the relaxed rule that row is "pristine" and gets overwritten by remote; under the current rule restore is blocked. Either way the onboarding-theme-vs-remote-garden conflict is unresolved and the two sections treat the same field independently.

**Resolution required:** state the precedence explicitly. Recommended: pristine = `countItems == 0 && steps == 0` (drop route only), **but** on restore, reconcile theme with a rule — remote garden theme wins if a remote garden exists; otherwise keep the onboarding pick. Sequence restore-garden (cross-cutting step 3) strictly before onboarding theme apply, and make onboarding theme a no-op when a remote garden was restored.

## R.2 "Replay events" contradicts "profile LWW is authoritative" (correctness — §3.4)

§3.4 correctly prefers **profile LWW for balances + idempotent ledger for audit**, but the code steps then say *"merge events by id before any replay."* If the profile is the source of truth for balances, restore must **never recompute balances from the event ledger** — restore the profile numbers directly and treat `xp_events` / `gem_events` as append-only audit.

Risk if left as written: a device with local XP/gem events **plus** a restored remote profile double-counts (profile balance + replayed events). Also note whether `XpEventEntity`/`GemEventEntity` ids are globally stable or local autoincrement — if local, cross-device "merge by id" is unsafe.

**Resolution:** remove "replay." Restore = apply remote profile balances (LWW on `updatedAt`); ledger is audit only, deduped by a globally unique event key (e.g. `studentId + source + serverTimestamp`), never summed to derive balances.

## R.3 `scheduleDeferredUpload()` on `recordStep` is a hot-path enqueue risk (§1 Gap C / §3.3)

`recordStep` fires on **every learning step**; calling `scheduleDeferredUpload()` each time can churn WorkManager with redundant enqueues. §1 labels Option A "minimal, recommended for launch," but the dirty-flag approach (Option B) is actually **safer** even for launch on this path.

**Resolution:** if keeping Option A, enqueue as **unique work with `ExistingWorkPolicy.REPLACE`** (or a short debounce) so rapid steps coalesce into one push. Prefer Option B (`isSynced` + `updatedAt` + push-only-dirty) for anything on a per-step path.

## R.4 Quest double-claim needs a guard at the claim site, not just restore timing (§3.4 / DoD)

Relying on "restore marks quest claimed before the UI reads it" is the **same race class as Bug B**. A mid-day new device can re-claim if restore hasn't landed.

**Resolution:** enforce an idempotent claimed-guard at the claim site (server-authoritative claim, or a local `claimed(questDate)` check that the restore populates transactionally), so re-claim is impossible regardless of restore timing. Keep the login-sequence ordering as a second line of defence, not the only one.

## R.5 No observability for the exact failure mode this doc fixes (cross-cutting — add)

Every section targets **silent** data loss / no-ops, yet there is no telemetry to catch them in production — which is how Bug A shipped unnoticed.

**Resolution:** add lightweight per-domain restore logging: `restore_applied{domain, itemCount}` vs `restore_skipped{domain, reason}` (reason = `local_progressed` / `remote_empty` / `race_not_ready` / …). Cheap, and it turns the next silent no-op into a dashboard signal. This should be a first-class checklist item, not optional.

## R.6 Firestore rules should be one consolidated, gated unit (cross-cutting — add)

§3 adds new top-level collections (`exam_plans/`, `gamification/`), each section saying only "mirror owner checks." A missing rule makes writes **silently fail** (fits the silent-loss theme).

**Resolution:** add a dedicated "Rules changes" subsection enumerating **every** new path (`exam_plans/{eduai_app_*}/…`, `gamification/{eduai_app_*}/…`) with owner + `appName` checks, and a single DoD checkbox "rules deployed and write-tested" so nothing ships without its matching rule.

## R.7 Smaller notes

- **Misleading code comment:** `GardenSyncManager.kt:84` says *"Default row created by GardenRepository.ensureState"* above a `route == "0"` check, but `ensureState` writes `"1"`. Fix the comment as part of Bug A so the next reader isn't misled.
- **Kannada regression guard:** §5's durable fix is a **CI check that fails when a `values/strings.xml` key lacks a `values-kn` counterpart** — add it alongside the `LanguageConsistencyTest` note; manual re-diff won't hold.
- **Bug B is not deterministically testable** as written ("open Home immediately after login"). Add an injectable hook / unit test around `awaitGardenRestore()` instead of relying on manual timing.
- **Effort:** §3's "2–4 days" is optimistic for two migrations + two sync managers + hot-path wiring + rules + idempotency + reinstall testing on the money-bearing path — plan closer to a week; treat §3 as the highest-risk item.
- **Cosmetic:** §2.3 example `completedAt` uses a 2024-era epoch; refresh or mark as illustrative.

## R.8 Prioritization — one product call

The build order leads with §4 (cheap, mostly done — reasonable) but places §5 Kannada at #2 (first-impression) and §1 garden at #3. The two genuine **data-loss** bugs are §1 (silent restore no-op) and §3 (economy reset). §1 Bug A/B is a *small* change fixing silent loss.

**Decision to make:** if the near-term goal is *"no data loss on reinstall"* (retention), §1 Bug A/B should move ahead of §5's cosmetic sweep. If the goal is *"polished first session"* (conversion), the current order stands. Make this explicit rather than implicit in the effort table.

## R.9 Net

Ship-ready as a plan once **R.1, R.2, R.4, R.6** are folded in (they change what gets built), with **R.3, R.5** strongly recommended for launch and **R.7–R.8** as polish/decisions. Accuracy of the spec itself is excellent — no claim I checked was wrong.

---

# Second-pass notes on Review notes (2026-08-15)

Follow-up review of the **Review notes** section against the same codebase. **Verdict: Review notes are strong and signed-off** — R.0 matches code; fold **R.1, R.2, R.4, R.6** into the main §§ before build. The items below are corrections / nuances only (they do not reopen the R.0 claims).

## S.1 R.3 overstated — deferred upload already coalesces

`DataSyncService.scheduleDeferredUpload()` already uses **unique work** + `ExistingWorkPolicy.KEEP` + a delayed coalesce window. Calling it from `recordStep` does **not** spam a new worker per step.

**Residual risk (keep in mind for Option A):** if a push is **already running**, KEEP will not enqueue a follow-up for plants made during that run — the last plant can wait until the next dirty schedule. Prefer Option A for launch with that caveat documented; Option B (`isSynced` + push-only-dirty) if dirty-row certainty matters more.

**Soft-edit to R.3:** do not require inventing REPLACE/debounce from scratch — KEEP coalescing exists; document the in-flight-work gap instead.

## S.2 R.2 confirmed — never merge XP/gem ledgers by local Room id

`XpEventEntity.id` is `@PrimaryKey(autoGenerate = true)`. Cross-device “merge by id” is unsafe. Use the existing unique business key for ledger dedupe: `studentId + itemType + itemId + language` (and an equivalent stable key for gem events). Profile balances remain LWW-only; ledgers are append-only audit.

## S.3 R.1 nuance — theme apply already gated on plants

`HomeViewModel.applyOnboardingPicksOnce` already `awaitGardenRestore()`s and **skips** onboarding theme when `totalPlanted > 0`. The real edge case R.1 still correctly flags is **empty remote plants + remote theme** vs local onboarding world after pristine is relaxed. Keep the stated precedence: restore garden first; onboarding theme is a no-op when a remote garden was restored (even if item count is zero but remote state exists).

## S.4 R.8 — product call stands

If near-term goal is **no data loss on reinstall** (retention), bump §1 Bug A/B ahead of §5 Kannada. If goal is **polished first session** (conversion), keep the current build-order table. Make the choice explicit in the effort table when folding reviews in.

## S.5 Net (second pass)

| Review note | Second-pass action |
|-------------|--------------------|
| R.1, R.2, R.4, R.6 | Fold into main §§ before coding (unchanged) |
| R.3 | Soft-edit: KEEP already coalesces; note in-flight gap |
| R.5, R.7 | Still recommended / polish |
| R.8 | Explicit product call when scheduling work |
| R.0 / R.9 | Confirmed — no claim reopened |

---

# Implementation review (2026-08-15, post first build)

Re-checked the code shipped in this session against R.1–R.6 / S.1–S.3.

| Area | Verdict | Notes |
|------|---------|--------|
| §1 Bug A pristine | ✅ | `GardenRestorePolicy.canRestoreFromRemote(items, steps)` — route/theme no longer block |
| §1 Bug B race | ✅ | `HomeViewModel` awaits restore before garden observe / picks |
| §1 R.1 theme precedence | ✅ | `wasGardenRestoredFromRemote()` gates onboarding theme (incl. 0-plant remote state) |
| §1 Gap C deferred upload | ✅ | KEEP coalesce; called from garden mutations |
| §1 R.5 telemetry | ✅ | `restore_applied` / `restore_skipped` |
| §2 onboarding cloud | ✅ | write on finish, hydrate on login, clear on logout |
| §2 hydrate vs picksApplied | ✅ | hydrate does **not** set local picksApplied — Home still materializes once |
| §3 profile LWW (R.2) | ✅ | No ledger replay; placeholder local (0 xp/gems) yields to remote |
| §3 rules (R.6) | ✅ | Deployed: gamification profile + exam_plans + `quests/{parent}/daily/{date}` |
| §3 exam plan sync | ✅ | `ExamPlanSyncManager` restore/push wired; trials rematerialize locally |
| §3 quest sync (R.4) | ✅ | `QuestSyncManager` OR-merges claim flags; claim sites heal from gem grant keys; deferred upload on claim/refresh |
| R.4 residual risk | note | Gem **grant keys** stay local — reinstall double-grant is blocked by synced claim flags, not by ledger. Push after claim is the hard dependency. |
| Gate naming | note | `awaitGardenRestore()` waits for **full** `onUserAuthenticated` (garden + economy restore) |
| §5 P0 streak + nav tour | ✅ | `StreakCopy` / `StreakCopyFactory`; `NavTourCopy` + tour chrome labels on `EduIntroTourOverlay` |
| §5 P1 Friends | ✅ | `FriendsCopy` + screen/VM snackbars; reuses `HomeCopy` / `FriendFeedCopy` where possible |
| §5 P1 Leagues | ✅ | `LeaguesCopy` + factory; localized tier titles via `LeagueUiMapper`; hero + zone labels |
| §5 P1 Quest claim | ✅ | Dialog titles/bodies/CTAs + result snackbars EN/KN (`QuestClaimUi` / `QuestClaimDialog`) |
| §5 P2 `values-kn` | ✅ | 15 missing keys added (legal / Padaams / password / cancel) — EN↔KN key parity |
| §5 P2 HomeProgressRail | ✅ | `HomeProgressRailCopy` + `ProgressRatingCopyFactory` (component unused in app today) |
| §5 P2 EduRatingDialog | ✅ / N/A host | Copy wired; live app uses Play In-App Review only (`AppRatingHost`) |
| §5 P2 Notification settings | ✅ | `NotificationSettingsCopy` for screen chrome + pickers |
| §5 P2 Avatar settings | ✅ | `TutorAvatarSettingsCopy` |
| §5 P3 Moment chips | ✅ | `xpLabel` / `gemsLabel` on `MomentOverlay`; headlines already KN via `MomentVariants` |
| §5 P3 RewardOverlay | ✅ / suppressed host | `RewardOverlayCopy` ready; live host still drains rewards without showing overlay |
| §5 channel labels | ✅ | `RewardMomentCopy.categoryLabel` in settings UI + `NotificationChannels.ensureCreated` |

**Product call applied:** retention-first order (§1 → §2 → §3 → §4 docs → §5).
