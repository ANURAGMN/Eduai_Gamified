# Changes for review — 2026-08-16 (coworker session)

**Status (as of `ad380f2`):**

| Area | State |
|------|-------|
| App icon + sign-in logo (§1) | ✅ **committed** `33ba26a` on `gamified/main` |
| Garden growth via `ProgressEventTracker` — one-plant-per-concept-forever (§4, first cut) | ✅ **committed** `6f104c2` |
| Per-completion keys + 60 s window + Plan-DONE plant, celebration host, DAO `getLatestItemForTask`, onboarding scenes, Avatar tutorial | ✅ **committed** `ad380f2` on `gamified/main` (local; push when ready) |

**Rebuild required:** the APK last tested on device did **not** include the Plan-DONE plant path (that's why trials showed XP but 0 new `task:` rows). Install a build from `ad380f2` (or later) before re-testing.

**Not compiled here** — hand-checked against the code; the build machine must confirm. Two Hilt constructors change (see §4) — clean install before re-test if DI acts up.

Feature areas below, independent enough to commit separately.

---

## 1. New app icon + sign-in logo — ✅ committed `33ba26a` (gamified/main)

**What:** Replaced launcher icon + in-app sign-in logo with the EduAI Class-7 illustration.

**Files**
- `app/src/main/res/mipmap-*/ic_launcher.webp` + `ic_launcher_round.webp` (mdpi…xxxhdpi) — regenerated (illustration on orange, round = circle-cropped).
- `app/src/main/res/drawable-*/ic_launcher_foreground.png` (mdpi…xxxhdpi) — **new raster** adaptive foreground (illustration scaled ~76% and centered).
- `app/src/main/res/drawable-v24/ic_launcher_foreground.xml` — **deleted** (old vector foreground; raster now wins at every density).
- `app/src/main/res/drawable/logo.png` — replaced (sign-in logo used by `LoginScreen.kt`; black margin flood-filled to transparent).
- `app/src/main/res/values/colors.xml` — `ic_launcher_background` `#01194C` → `#FD7E01` (artwork orange).
- `app/src/main/ic_launcher-playstore.png` — **new 512px** store icon (NOT shipped in AAB; upload in Play Console → Store listing).

**Review focus:** `minSdk 28` → every device uses the **adaptive** icon (mipmap-anydpi-v26 XML unchanged: color bg + raster foreground). Confirm no leftover reference to the deleted `drawable-v24` vector. Verified visually under circle + squircle masks; full artwork ("EduAI / CLASS 7") survives both.

**Test:** fresh install → launcher icon + sign-in logo render correctly (incl. dark mode).

---

## 2. First-run world step renders real Garden/Space scenes

**What:** The onboarding "pick your world" step showed generic Eco/Rocket icon cards; now each card renders the actual `ThemeScene` (Garden = `GARDEN`, Space = `OUTPOST`). The pick already maps to the theme in `HomeViewModel.applyOnboardingPicksOnce` (`"Space"→OUTPOST`), so this is **visual only**.

**Files**
- `ui-kit/src/main/java/com/anurag/eduai/uikit/screens/OnboardingScreen.kt` — `WorldStep`/`WorldCard` rewritten to draw `ThemeScene` (cover, full `sceneAspect`); added private `worldTheme(key)` + `previewScene(theme)`; added garden imports + `rememberSceneTime`.

**Review focus:** `Eco`/`RocketLaunch`/`ImageVector` imports may now be unused (warning only). `previewScene` seeds a few planted rows so the card isn't an empty plot. Two live `ThemeScene` canvases on one screen — fine, but eyeball perf on low-end.

**Test:** fresh install → onboarding step 3 shows recognizable garden woodland + Mars outpost; selecting one still themes the garden.

---

## 3. First-run tutorial walkthrough → Avatar · Scene · Journey · Look

**What:** The tab walkthrough's single "Avatar" card is expanded into three cards that step the Avatar tab through its segments (Scene → Journey → Look), switching the live segment as each card shows. Sequence is now Plan → Avatar·Scene → Avatar·Journey → Avatar·Look → Leaderboard → Home.

**Files**
- `app/.../ui/screens/garden/AvatarTabNavigation.kt` — added `forcedSegment: StateFlow<AvatarGardenSegment?>` + `setForcedSegment()`.
- `app/.../ui/screens/setting/AvatarStudioRoute.kt` — collects `forcedSegment`; `LaunchedEffect` drives the tab's `segment`.
- `app/.../utils/NavTourCopy.kt` — `NavWalkStep` gains `segment: AvatarGardenSegment?`; EN + KN step lists expanded (6 steps each).
- `app/.../ui/navigation/BottomNavBar.kt` — tour `LaunchedEffect` calls `setForcedSegment(step.segment)`; `finishNavTour` + inactive branch clear it.

**Review focus:** gated by `hasCompletedNavTour()` — needs fresh install (or clear the pref) to re-trigger. Confirm the forced segment is released after the tour (no stuck Avatar segment). If you want to drop Plan/Leaderboard/Home and keep only the Avatar segments, it's a trim in `NavTourCopy.kt`.

**Test:** fresh install → after the home-rail tour, the tab tour walks Plan → Avatar (Scene→Journey→Look, segment visibly switching) → Leaderboard → Home.

---

## 4. Garden grows on every completion + app-wide celebration

**Root problem:** garden growth was wired **only** to the Plan-trial path, and even there a plant needed the chapter-completion engagement gate (~7 taps) — so trials finishing at `complete@2` paid XP but grew nothing, and free-browse sims grew nothing outside the Plan. Also the plant/space celebration only rendered on the Plan screen.

**What now:** any completed task grows a plant, however it completes; a new plant on **every** completion (repeats included); the celebration shows app-wide.

**Files**
- `app/.../repository/GardenRepository.kt` — `recordCompletion()` **rewritten to per-completion**: plants one item per completion, unique id `task:<concept>:<bucket>:<ts>`, dedup via `getLatestItemForTask` + `COMPLETION_DEDUP_WINDOW_MS = 60_000` (collapses the duplicate callbacks of one completion; a later re-do plants again). `taskKindBucket()` normalizes kind → STUDY/REVISION/SIM so Plan (`SIM_URL`) and free (`SIMULATION`) paths converge. KDoc updated to match. **`recordStep()` is unused in production but is still called by unit tests** — deleting it will break those tests, so leave it or update the tests.
- `app/.../data/local/dao/GardenDao.kt` — **new** `getLatestItemForTask(studentId, conceptId, kind)`.
- `app/.../domain/progress/ProgressEventTracker.kt` — **injects `GardenRepository` + `GardenMomentCoordinator`** (⚠ constructor change; this is the committed `6f104c2` cut); private `growGarden()` called from `markStudyCompleted` / `markSimulationAgentCompleted` / `markSimulationUrlCompleted` / `markRevisionCompleted` / `markMathAgentCompleted` / `markScienceAgentCompleted`.
- `app/.../domain/examplan/PlanTrialProgressTracker.kt` — plants on trial **DONE** via `recordCompletion` (⚠ re-added `GardenRepository` + `GardenMomentCoordinator` deps); old 7-step `recordStep` growth removed.
- `app/.../domain/garden/GardenMomentCoordinator.kt` — added `suppressGlobalHost` flow + `setGlobalHostSuppressed()`.
- `app/.../ui/garden/GardenCelebrationHost.kt` — **new**: global host + `GardenCelebrationViewModel`, mounted over the whole app; shows the pending plant/space moment via `TrialMomentHost`. **Gate is `suppressGlobalHost` ONLY.** (An earlier draft also skipped when `TrialSessionStore.activeTrialItemId != null`, but that id often stays set after leaving Plan and blocked the popup — it was removed. Do not re-add it.) Has a `GardenPlant` debug log on the pending path.
- `app/.../MainActivity.kt` — mounts `GardenCelebrationHost()` in a `Box` over `LoginNavigator`.
- `app/.../ui/screens/plan/viewmodel/PlanTrialViewModel.kt` — `onTrialScreenVisible/Hidden` toggle `setGlobalHostSuppressed(true/false)`.

**Review focus (please scrutinize):**
- **No double-plant:** free path + Plan-DONE for the same completion collapse via the 60 s window + shared bucket key. Verify the window is long enough for your device — Plan-DONE at tap 2 → chapter-completion at tap 7 can be ~30–60 s apart; if it exceeds 60 s you'd get two plants. One constant to tune.
- **No double-celebration:** the global host is suppressed **only while the Plan screen is visible** (`suppressGlobalHost`). A plan task completed on a sub-screen (sim/chat) will celebrate via the global host, and the Plan chain then finds `pending` already cleared — so still once. Confirm a Plan sim celebrates once and a free sim celebrates once.
- **Hilt:** two constructors changed — clean install if the graph complains (no cycles expected; both are existing `@Singleton`s).
- **Zones fill 12/plot:** 84 legacy items may already fill early zones; new plants land in the next unlocked zone, or no-op if every place is full — check the whole scene, not just zone 1.

**Test (on device, after installing a build from `ad380f2` — not the older APK):** free-browse a sim → new `task:…:<ts>` row + celebration (watch logcat `GardenPlant`). Redo after a minute → another new plant. Finish a Plan trial at a low bar (`complete@2`) → plant + celebration once. Do a Plan sim that also crosses the tap gate → still one plant.

---

## Docs touched (same session)
- `docs/RELEASE_READINESS_v1.0.11.md` — verdict + §0 table + §1 rows #16–#20 updated to reflect all of the above; #19 covers the final per-completion garden model.
- `docs/store-listing/release-notes-1.0.11.txt` — EN/KN "What's new" + internal notes (from earlier in session).

## Known open item (NOT in this change set)
Coach coverage gap: only 151/273 sims include `edu-coach.js`. **Science EN chapters 5–8 and ~80/91 Kannada sims have no coach** (that's why the coach "sometimes" doesn't appear — e.g. `science_6_3`). Fixing needs per-sim authoring like science 2–4; separate task.
