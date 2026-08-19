# Cursor: rebuild + review — onboarding tutor step + Reels-tab tour

All **uncommitted**; pull latest first. **Not compiled here** — build + run to confirm Compose/ui-kit APIs. Layers on top of `REELS_BUILD_HANDOFF.md`.

## 1. Onboarding "Meet your tutor" step (NOT flag-gated — first-run flow for all)
New 4th onboarding pick after World: choose 1 of 10 tutor looks (reuses existing presets + `EduTutorAvatar`).
- `ui-kit .../avatar/AvatarPresets.kt` — new `OnboardingTutorPresets` = **8 distinct** presets (drops `quill`, `naturalist` ≈ scholar, and `pulse` ≈ nova). `AllAvatarPresets` (premium pool) untouched.
- `ui-kit .../screens/OnboardingScreen.kt` — `TutorStep` + `TutorCard`: **`LazyVerticalGrid(2)` → clean 2 col × 4 row** for the 8 presets, **equal square** avatar frame + `avatarFaceZoom(1.9f, 0.12f)` on Free faces so sizes match the Orb, **selected check badge**, **"Surprise me"** (random pick). `OnboardingResult` gained `avatarPresetId`; World's button is now "Continue".
  - **Nova/Orb visibility fix:** `TutorCard` now takes `avatarBg: Color`; Orb characters (Nova/Pulse) render on a **dark frame `Color(0xFF17263A)`** so the glow shows, Free faces stay on `colors.surface1`. (Nova was invisible before — pale orb on a light tile.)
- `ui-kit .../screens/OnboardingCopy.kt` — EN/KN for `pickTutorTitle/Sub`, `backWorld`, `surpriseMe`; step labels now "of 4".
- `app .../ui/navigation/LoginNavigator.kt` — on finish: `TutorConfigStore.save(context, preset.config)` + `AvatarUnlockStore.unlock(...)` + `setOnboardingAvatar(id)`.
- `app .../data/local/SharedPreferenceUtils.kt` — `KEY_ONBOARDING_AVATAR` + get/set + cleared on reset.

**Note:** persistence is **prefs-local** (`TutorConfigStore`) — renders everywhere but does NOT sync to Room/cloud, so the pick won't restore on a new device. Route through `TutorConfigRepository` if you want cloud sync (deferred).
**Tune:** the `avatarFaceZoom` values are a sensible default — may need a small nudge on device to frame faces.

## 2. Reels-tab nav-tour spotlight (flag-gated by REELS_ENABLED)
- `app .../utils/NavTourCopy.kt` — new **Reels** step (EN/KN) after Plan.
- `app .../ui/navigation/BottomNavBar.kt` — captures each tab's rect via `EduBottomNavBar.onItemBounds` → `navTabBounds`; the tour spotlights `navTabBounds[Reels]` on that step; the step is **filtered out when the flag is off** (reuses `reelsEnabled`). Sequence (flag on): Plan → **Reels** → Avatar(Scene/Journey/Look) → Leaderboard → Home.

## Test / verify
- **Fresh install** (or clear `hasCompletedFirstRun` / `hasCompletedNavTour`) to see the onboarding step and the nav tour.
- `./gradlew :app:testDebugUnitTest` — reels logic tests still valid.
- Onboarding: Subject → Chapter → World → **Meet your tutor** → Build my plan; chosen avatar shows as Home mascot.

## Now built (this pass)
- **Subject list rows (design "Option B")** — `ui-kit .../components/HomeRails.kt`: `SubjectsRail` no longer a 110dp square `HorizontalRail`; now **full-width stacked rows** via new private `SubjectRow`. Each row = role-tinted icon chip (`forRole.bg` fill, glyph tinted `fg`) · name (`colors.text`) + detail line (`colors.textSecondary`) · trailing chevron (`colors.textMuted`), on a `colors.surface2` card with a `colors.border` hairline. Whole row is one `pressScaleClickable` → `onOpen` → chapter selection. All theme tokens → dark-mode safe.
  - `SubjectTile` gained optional **`subtitle: String?`** (the detail line, e.g. "14 chapters"). When null it falls back to `ctaLabel` (default "Continue").
  - `SubjectsRail` also gained optional `ctaLabel` param (default "Continue") used as the subtitle fallback; pass localized copy from Home if wanted.

## Chapter counts wired into the subject rows (this pass)
- `app .../data/local/dao/ChapterDao.kt` — new `getChapterCountsBySubject(): List<SubjectChapterCount>` (grouped `COUNT(*)`), plus a `SubjectChapterCount(subjectId, chapterCount)` Room projection.
- `app .../ui/screens/home/viewmodel/HomeViewModel.kt` — new `chapterCounts: StateFlow<Map<String,Int>>`, populated by `loadChapterCounts()` (best-effort, `runCatching`) after subjects load in both `refreshAvailableSubjects()` and `observeAvailableSubjects()`.
- `app .../ui/screens/home/GamifiedHomeMapper.kt` — `map(...)` gained `chapterCountsBySubject: Map<String,Int> = emptyMap()`; `mapSubjectTiles` sets `subtitle = HomeCopy.chapterCount(lang, n)` when `n > 0`.
- `app .../utils/HomeCopy.kt` — new `chapterCount(lang, count)` → "N chapters" / "1 chapter" (+ Kannada "N ಅಧ್ಯಾಯಗಳು" / "1 ಅಧ್ಯಾಯ").
- `app .../ui/screens/home/GamifiedHomeRoute.kt` — collects `chapterCounts`, adds it to the `remember` keys, passes `chapterCountsBySubject = chapterCounts` into `map(...)`.
- Other `GamifiedHomeMapper.map(` callers (`QuestsRoute`, `LanguageConsistencyTest`) are unaffected — new param defaults to `emptyMap()`.

## Subject-row completion ring (this pass)
Metric chosen: **completed chapters ÷ total**. Placement: **trailing ring, before the chevron**.
- `app .../data/local/dao/ChapterAgentProgressDao.kt` — new `getCompletedChapterCountsBySubject(studentId, language, appName, completedStatus="COMPLETED"): List<SubjectChapterCount>` (grouped COUNT of COMPLETED chapters, joined to `chapters` for subjectId). Reuses the `SubjectChapterCount` projection from `ChapterDao.kt` (same package, no import needed).
- `app .../ui/screens/home/viewmodel/HomeViewModel.kt` — injected `ChapterAgentProgressDao`; new `completedChapterCounts: StateFlow<Map<String,Int>>`, populated inside `loadChapterCounts()` (best-effort, keyed on `userId` + `_currentLanguage` + `AppConfig.APP_NAME`). Refreshes when subjects load (app open / language / class change) — not live mid-session.
- `app .../ui/screens/home/GamifiedHomeMapper.kt` — `map(...)` gained `completedChapterCountsBySubject`; `mapSubjectTiles` sets `SubjectTile.progress = (completed / total).coerceIn(0,1)` when `total > 0`, else null.
- `app .../ui/screens/home/GamifiedHomeRoute.kt` — collects `completedChapterCounts`, adds to `remember` keys, passes `completedChapterCountsBySubject`.
- `ui-kit .../components/HomeRails.kt` — `SubjectTile` gained `progress: Float?`. New private `SubjectProgressRing` (Canvas: `colors.border` track + role-`fg` arc, centred `"$percent%"` in `colors.text`, 38dp, 4dp stroke, rounded caps). `SubjectRow` renders it before the chevron only when `progress != null`. Dark-mode safe (all tokens).

### Fix — "no count / % showing"
Two gaps found and fixed:
1. **Default tiles had no counts.** When the home renders the fallback `defaultSubjectTiles` (Math/Science — used whenever `availableSubjects` is empty), neither `subtitle` nor `progress` was set → both rows showed nothing. `mapSubjectTiles` now shares `subjectSubtitle()` / `subjectProgress()` helpers and `defaultSubjectTiles(...)` takes the count maps too (SubjectIds.MATH/SCIENCE match the `chapters.subjectId`s). Ring shows even at 0% (grey track + "0%") once a subject has chapters.
2. **Counts loaded too early.** They were fetched once when subjects loaded; if chapter sync finished afterwards they never appeared. Totals now come from a reactive Room Flow — `ChapterDao.getChapterCountsBySubjectFlow()` observed in `HomeViewModel.observeChapterCounts()` — so they populate as soon as chapters exist, and completed counts refresh in the same collector (`refreshCompletedChapterCounts()`). Still 100% local Room, no Firebase.

## Still pending (not built)
- **Subjects home-rail tour spotlight** — see `ONBOARDING_TOUR_ADDITIONS.md` (index re-alignment is the trap).
- **Video coach** (primer + checkpoint questions) — spec only.

## Heads-up
`ReelsGridSelector.REQUIRE_MADE_FOR_KIDS_DEFAULT` is **true** (Families / v1.0.11) so only Made-for-kids videos surface.
