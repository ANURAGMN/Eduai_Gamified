# Onboarding tutorial highlighter — add Subjects + Reels-tab spotlights

Goal: extend the first-run coach-mark tour to spotlight (1) the **Subjects** buttons and (2) the **Reels tab**. Plan only — implement after the Subjects-button + Reels-tab work lands.

## How the tour works today (two phases)
- **Phase 1 — home rails** (`ui-kit/.../screens/HomeScreen.kt`, `EduHomeScreen`). Each spotlighted rail is wrapped in a `Box(...onGloballyPositioned { railBounds = railBounds + (INDEX to it.boundsInRoot()) })`. The overlay spotlights `railBounds[coachStep]`; a `LaunchedEffect` scrolls that rect into view first. Steps live in the private `homeTourSteps` list. **Step index N must equal the rail's INDEX** (0-based). Today: 0 chips, 1 focus/tutor, 2 garden (3 = plan bounds captured but not stepped). Gate: `hasCompletedHomeTour()`.
- **Phase 2 — bottom-nav tour** (`app/.../navigation/BottomNavBar.kt` + `utils/NavTourCopy.kt`). Screen-level cards that navigate between tabs; `EduIntroTourOverlay(target = …)`. `EduBottomNavBar` can report each tab's on-screen rect via `onItemBounds(item, rect)` (currently unused). Gate: `hasCompletedNavTour()`.

Both use `EduIntroTourOverlay(step,total,target: Rect?,viewport,title,body,onBack/onNext/onSkip,labels)` — `target != null` = spotlight that rect; `null` = screen-level card. Overlay colours come from `EduAiTheme` → dark-mode safe.

---

## 1) Subjects spotlight — phase 1 (home rail)
Add a spotlight on the Subjects buttons ("Pick a subject to start — tap Math or Science").

**Changes in `EduHomeScreen`:**
1. Wrap `SubjectsRail(...)` in a bounds box: `Box(Modifier.fillMaxWidth().onGloballyPositioned { railBounds = railBounds + (2 to it.boundsInRoot()) }) { Entrance { SubjectsRail(...) } }`.
2. **Re-index** so step order == rail index. New map: `0 chips · 1 focus/tutor · 2 Subjects · 3 Video lessons · 4 garden`. So change the garden box capture `(2 → 4)` and give the (currently un-stepped) plan box a non-colliding index (e.g. `90`) or drop its wrapper.
3. Extend `homeTourSteps` to match: `[progress, focus, Subjects, (Video), garden]`.

**Copy (EN/KN):** Subjects → EN "Pick a subject to start" / "Tap Math or Science to open its chapters." KN equivalents. Put copy in the same place as `homeTourSteps` (or lift `homeTourSteps` to a param so the app supplies localized text).

Scroll-into-view already handles any index, so no extra work there.

## 2) Reels tab spotlight — phase 2 (bottom nav)
Spotlight the **Reels tab icon** while the explore screen shows ("Quick video lessons — watch and learn in under a minute").

**Changes:**
1. **Capture tab rects:** in `BottomNavBar`, pass `onItemBounds = { item, rect -> navTabBounds = navTabBounds + (item to rect) }` to `EduBottomNavBar` (add a `var navTabBounds by remember { mutableStateOf<Map<EduBottomNavItem, Rect>>(emptyMap()) }`).
2. **Add a Reels step** to `NavTourCopy` (`NavWalkStep(EduBottomNavItem.Reels.route, "Quick video lessons", "…", segment = null)`), placed where you want it in the sequence (e.g. right after Plan).
3. In the phase-2 overlay, for the Reels step set `target = navTabBounds[EduBottomNavItem.Reels]` (instead of the usual `null`) so it spotlights the tab icon; the tour already navigates to `step.route`.
4. **Gate on the flag:** only include the Reels step when `ReelsFeatureFlags.isReelsEnabled()` (the tab only exists then). Build the `NavTourCopy.steps(...)` list conditionally, or filter it in `BottomNavBar`.

## 3) (Optional) Video rail on Home — phase 1
If you also want to spotlight the Home **Video lessons rail** (not just the tab), wrap the `belowSubjectsContent()` Entrance in a bounds box at index `3` (per the re-index above) and add the matching step. Gate the step on the video rail being present (flag/`videoTourEnabled` param) so it isn't spotlighted when empty.

---

## Cross-cutting
- **Gating / re-trigger:** phase-1 = `hasCompletedHomeTour()`, phase-2 = `hasCompletedNavTour()`. A fresh install (or clearing the prefs) re-runs them. New steps ride the existing gates — no new flag needed (except the reels **feature** flag for including the reels step).
- **Conditional steps:** since `homeTourSteps` and `NavTourCopy` are currently static, make the Subjects/Video/Reels steps conditional by building the lists from params/flags. Keep step index == rail index after any conditional insert (recompute indices, don't hardcode).
- **Order matters:** recommended phase-1 sequence: progress → focus → **Subjects** → **Video** → garden. Phase-2: Plan → **Reels** → Avatar(Scene/Journey/Look) → Leaderboard → Home.
- **Analytics:** `onTourStep(step)` (phase 1) and `EngagementAnalyticsTracker.navWalkthroughStep` (phase 2) already fire per step — new steps report automatically.
- **Dark mode / contrast:** overlay + cards use `EduAiTheme` tokens, so no white-on-white risk; the spotlight cut-out is geometry, not colour.

## Effort / risk
- Subjects (phase 1): small, but the **index renumbering is the trap** — every `railBounds` key must line up with its step position, or the wrong rail gets spotlighted. Test by running the tour on a fresh install.
- Reels tab (phase 2): small; the only new bit is wiring `onItemBounds` and using the tab rect as the target for that one step.
- Both are `EduHomeScreen` / `BottomNavBar` edits (shared, live-Home files) — do them with the Subjects/Reels UI, and verify on device (can't preview the spotlight geometry statically).
