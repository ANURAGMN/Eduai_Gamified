# Pre–Play Console checklist (EduAI)

**App:** `com.ncert7.aitutorandlab` · **NCERT Class 7 AI Tutor & Labs**  
**Last updated:** 2026-06-20  
**Related:** [COMPLIANCE_BLOCKERS.md](store-listing/COMPLIANCE_BLOCKERS.md) · [PLAY_DATA_SAFETY.md](store-listing/PLAY_DATA_SAFETY.md) · [P1_PRODUCT_GAPS_DETAILED.md](P1_PRODUCT_GAPS_DETAILED.md) (Aug 2026 — detailed change specs for garden/onboarding/economy sync, analytics, Kannada; supersedes stale #7–16 notes where noted) · [P1_IMPLEMENTATION_REVIEW.md](P1_IMPLEMENTATION_REVIEW.md) (implementation handoff for review)

Consolidated checklist for analytics gaps, Firestore sync, Play Console policy, and release QA. Items are ordered by recommended implementation priority.

---

## Recommended build order

1. **AI report control** (P0 #1)
2. **Analytics for new flows** (P1 #7–15)
3. **Garden Firestore sync** (P1 #16)
4. **Onboarding picks → user profile** (P1 #17)
5. **Release hygiene + QA** (P0 #5–6, P2 #20–26)
6. **Exam plan + gamification sync** (P1 #18–19) — if time before launch

---

## P0 — Ship blockers (do before upload)

| # | Item | Status | Action |
|---|------|--------|--------|
| 1 | **AI content report control** | Missing | Add **Report** (long-press or ⋮ overflow) on **AI assistant messages** in shared `ConversationView` (covers chatbot + math agent). Log report event + optional Firestore doc. Required by Play **Generative AI / AI-Generated Content** policy — Settings → Contact Support is not sufficient. |
| 2 | **Play Families / ads compliance** | Mostly done in code | Confirm Play Console: child-directed audience, Families self-certified ads SDK, **non-personalized ads only**. Verify `MobileAdsInitializer` flags (`childDirected=true`, `underAge=true`, `maxRating=G`) match questionnaire. |
| 3 | **Data safety form** | Needs manual fill | Declare: Firebase Analytics, AdMob (non-personalized), account email, progress sync, device identifiers. Must match `allow_ad_personalization_signals=false` in `FirebaseAnalyticsHelper`. See [PLAY_DATA_SAFETY.md](store-listing/PLAY_DATA_SAFETY.md). |
| 4 | **Store metadata accuracy** | Needs review | Listing title, description, screenshots, and content-rating questionnaire must describe the **current rebrand** (NCERT Class 7 / EduAI). |
| 5 | **Support email** | Personal Gmail today | Change `contact_email` in `strings.xml` from `mail2anuragmn@gmail.com` → production support address (e.g. `support@padaams.in` or `contact@padaams.in`). |
| 6 | **App display name consistency** | Inconsistent | EN `app_name` has a leading space and differs from Kannada (`EduAI`). Align `values/strings.xml`, `values-kn/strings.xml`, and Play listing. |

---

## P1 — Analytics (new flows, GA4-only)

**Status (Aug 2026):** **Done in code** — see [P1_PRODUCT_GAPS_DETAILED.md](P1_PRODUCT_GAPS_DETAILED.md) §4. Remaining: DebugView smoke + optional name aliases if dashboards used checklist spellings.

**Routing:** All events via `FirebaseAnalyticsHelper` / `GamificationAnalyticsTracker` / `EngagementAnalyticsTracker`.  
**Mirror:** `AnalyticsFirestoreMirror.ENABLED = false` is intentional — GA4 is the system of record for high-frequency events.  
**Compliance:** `allow_ad_personalization_signals=false` is already set in `FirebaseAnalyticsHelper`.

| # | Event(s) | Status |
|---|----------|--------|
| 7 | `ScreenName.ONBOARDING` + screen view | Done (`LoginNavigator`) |
| 8 | onboarding funnel + picks | Done (`onboarding_*` / funnel steps) |
| 9 | home tour | Done (`home_tour_*`) |
| 10 | nav walkthrough | Done (`nav_walkthrough_*`) |
| 11 | streak UI celebrations | Done (`streak_greeting_*` / `streak_extended_*`) |
| 12 | notification primer | Done (`notif_primer_*` / `notif_permission_result`) |
| 13 | review | Done (`review_requested` / `review_throttled`) |
| 14 | place picker | Done (`place_completed` / `next_place_*`) |
| 15 | plan reward banner | Done (`plan_reward_banner_tap`) |

### Analytics notes

- **Distinct streak events:** Keep existing DB-level `streak_extended` (logged when streak count increments in `StreakRepository`) separate from UI celebration events (`streak_extended_shown` / `streak_extended_done`).
- **In-app review:** `AppReviewManager` + throttle in `AppRatingGate` exist; logging the request (and throttle hits) is still missing.
- **Background flush:** GA4 batches internally. Explicit flush on `ON_STOP` is optional, not blocking.

### Already tracked (no action)

- Main screens via `TrackScreenEvent` / `ScreenName`
- Login funnel via `FunnelAnalyticsTracker` / `FunnelStep`
- Trial, plan, quests, leagues, friends, avatar, economy via `GamificationAnalyticsTracker`
- Nav clicks via `NavClickAnalyticsTracker`

---

## P1 — Firestore sync (cross-device / reinstall)

| # | Data | Status | Action |
|---|------|--------|--------|
| 16 | **Garden** (`GardenStateEntity`, `GrownItemEntity`) | Hardened (Aug 2026) | Sync existed; Bug A pristine/`route` mismatch + Bug B race fixed; deferred upload on plant/theme; restore telemetry. See [P1_PRODUCT_GAPS_DETAILED.md](P1_PRODUCT_GAPS_DETAILED.md) §1. |
| 17 | **Onboarding picks** (subject, chapter, world, `first_run_completed`) | In progress (Aug 2026) | Write/hydrate `users/{id}.onboarding`; clear prefs on logout. See P1 §2. |
| 18 | **Exam plan + trial state** | Local Room only | Add sync if mid-prep device switch matters for v1. |
| 19 | **Gamification profile / XP / gems / quests** | Local Room only (`isSynced` fields exist but no upload path) | Add sync post-garden if league/XP loss on reinstall is a concern. |

### Already synced (verify unchanged)

- Progress, chapter agent progress, streak, sessions, analytics entities (`ProgressAnalyticsSessionSyncManager` / `WeeklySyncWorker`)
- Syllabus content pull (`FirebaseSyncManager`)
- Tutor config push/pull
- Friends (partial)

### Keep local intentionally

Do **not** sync unless product decides otherwise:

- `home_tour_completed`, `nav_tour_completed`
- Notification primer show-count / last-shown-day
- Rating prompt show-count / last-shown-day
- `has_completed_any_task`

Re-showing tours or primer on a new device is acceptable; syncing adds complexity for little gain.

---

## P2 — Release hygiene & QA

| # | Item | Notes |
|---|------|-------|
| 20 | **Kannada completeness** | Final sweep of ui-kit hardcoded English literals + missing `values-kn` strings. |
| 21 | **Clean release build** | `.\gradlew.bat clean assembleRelease` (or `bundleRelease`). Play Review deps already in `app/build.gradle.kts` (`review:2.0.2`, `review-ktx:2.0.2`). |
| 22 | **In-app review testing** | Play In-App Review **will not show** on debug/sideloaded APK — test on **internal testing track** only. |
| 23 | **End-to-end smoke test** | Onboarding → home tour → nav walkthrough → streaks → scene-unlock picker → celebration/ad sequencing. Test 2+ screen sizes + **font scale 2.0** (Canvas/overlay-heavy flows). |
| 24 | **targetSdk 35** | ✅ Already set in `app/build.gradle.kts`. |
| 25 | **SCHEDULE_EXACT_ALARM** | ✅ Not declared. `NotificationScheduler` uses inexact `setAndAllowWhileIdle` only. |
| 26 | **Release AdMob IDs** | Confirm release build uses production `ADMOB_APP_ID` / ad unit IDs in `local.properties`, not Google sample IDs. `MobileAdsInitializer` logs a warning if sample IDs are detected in release. |

---

## Out of scope for v1 (unless explicitly requested)

| Item | Rationale |
|------|-----------|
| Firestore mirror for analytics events | GA4 remains system of record (`AnalyticsFirestoreMirror.ENABLED = false`) |
| Syncing tour / primer / rating device flags | Low value vs complexity; acceptable to re-show on new device |
| Custom rating dialog analytics | App uses Play In-App Review only (policy-compliant); no custom sentiment gate |
| Explicit GA4 flush on background | Nice-to-have; one-shot funnel events rarely lost |

---

## Code references

| Area | Key files |
|------|-----------|
| Analytics plumbing | `AnalyticsEventRecorder.kt`, `FirebaseAnalyticsHelper.kt`, `GamificationAnalyticsTracker.kt`, `AnalyticsEnums.kt`, `TrackScreenEvent.kt` |
| Onboarding | `ui-kit/.../OnboardingScreen.kt`, `LoginNavigator.kt`, `SharedPreferenceUtils.kt` |
| Home / nav tours | `ui-kit/.../HomeScreen.kt`, `BottomNavBar.kt` |
| Streak UI | `GamifiedHomeRoute.kt`, `StreakCelebration.kt`, `StreakRepository.kt` |
| Notification primer | `NotificationPermissionGate.kt`, `NotificationPermissionHost.kt` |
| In-app review | `AppReviewManager.kt`, `AppRatingGate.kt`, `GamifiedHomeRoute.kt` |
| Garden / place picker | `GardenRepository.kt`, `GardenNextPlacePicker.kt`, `PlanTrialViewModel.kt` |
| AI chat (report target) | `ConversationView` (shared by chatbot + math agent) |
| Sync worker | `WeeklySyncWorker.kt`, `ProgressAnalyticsSessionSyncManager.kt` |
| Ads compliance | `MobileAdsInitializer.kt` |
| Support contact | `ContactSupportCard.kt`, `strings.xml` → `contact_email` |

---

## Verification commands

```powershell
# Release build
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat clean bundleRelease

# AdMob / release config (if scripts exist)
.\scripts\verify-admob-config.ps1
.\scripts\verify-release-config.ps1

# Deploy Firestore rules (after auth + rules changes)
python scripts/deploy-firestore-rules.py
```

---

## Status legend

| Symbol | Meaning |
|--------|---------|
| Missing | Not implemented in codebase |
| Mostly done | Code in place; Console/manual steps remain |
| Needs review | Exists but must be verified before upload |
| ✅ | Verified OK in codebase |
