# Cursor: rebuild + review — reward flow, knowledge-bite cap, chapter-progress disable

**Action: pull the working tree, rebuild, and review the areas below.** All uncommitted. These sit **on top of** the earlier set in `CHANGES_FOR_REVIEW_2026-08-16.md` (icon, onboarding, tutorial, garden-grow + celebration host). Some files overlap with your on-device edits — I built on the current tree (e.g. your `markCelebrationShown`), but re-verify after pulling.

## A. Reward-flow fixes (detail + status in `REWARD_FLOW_TODOS.md`)
- **`GardenMomentCoordinator`** — `notifyPlanted` is now idempotent: ignores any total ≤ `lastCelebratedTotal` (logs `notifyPlanted IGNORED`), `@Synchronized`; `syncCelebratedTotal(total)` seeds the guard from prefs. **This is the single guard that kills the double pop-up and the re-show-on-next-screen.**
- **`GardenCelebrationHost` / VM** — seeds the guard from `lastGardenCelebrationPlantTotal` on init; `markCelebrationShown()` persists it on show + dismiss (your change, kept).
- **`ConceptSimulationViewModel` + `ConceptSimulationViewer`** — `onViewerVisible/Hidden` mute the app-wide host while a **free-browse** sim is on screen (gated to `activeTrialItemId == null`, so it never fights the Plan screen's own suppression). Wired via a `DisposableEffect`.
- **`PlanTrialProgressTracker`** — plants once on the DONE transition, no `notifyPlanted` (Plan owns its celebration on return).

**Review focus:** confirm one plant + one celebration per completion; no popup mid-sim; none on the next sim without new clicks.

## B. Knowledge-bite cap 7 → 15
- **`SimulationTrialThresholds.DEFAULT_GOAL` 7 → 15** and **`PlanTrialMaterializer`** SIM_URL `requiredCount` now references that constant (single source of truth). Bite completes at `min(15, htmlClickBudget)`; study/math/revision/agent unchanged.
- **Caveat:** existing materialized plan items keep `requiredCount = 7` — only newly materialized plans get 15. Re-materialize (or fresh plan) to test.

## C. Free-browse chapter-progress DISABLED (kill-switch)
- **`ConceptSimulationViewModel.CHAPTER_PROGRESS_FROM_FREE_BROWSE_ENABLED = false`** gates both `markSimulationCompleted` and `markSimulationUrlCompleted`: a sim finished **outside the Plan** does nothing — no chapter %, no XP, no plant/celebration. Streak-on-open and **Plan tasks + free-browse chat/revision** are unaffected (task-based rewards stay). Flip the flag to re-enable.

## Interactions (why TODO 5 is now low-risk)
Free-browse has no second plant path (C), and for Plan sims the chapter-credit only fires after DONE — which is now ~15 taps (B) — so it's co-located with the Plan-DONE plant and safely inside the 60 s `recordCompletion` dedup window. See `REWARD_FLOW_TODOS.md` #5.

## Build + test matrix (device, this tree)
1. **Plan sim, finish** → back to Plan → **one** plant + XP; no mid-sim popup; nothing on the next sim.
2. **Free-browse sim, finish** → logcat `Free-browse chapter progress DISABLED …`; **no** chapter %, XP, or plant.
3. **Free-browse chat / revision** → still completes + rewards (task-based).
4. **Bite cap:** a fresh-plan 15-budget sim shows `complete@15` (old plans still `@7` until re-materialized).
5. Watch logcat `GardenPlant`: one `notifyPlanted` (+ any `IGNORED`) per plant.

## Not compiled here
Hand-checked against the tree; the build machine must confirm. Hilt constructors changed in `ProgressEventTracker`, `PlanTrialProgressTracker`, `ConceptSimulationViewModel`, `GardenCelebrationViewModel` — clean build if DI complains.
