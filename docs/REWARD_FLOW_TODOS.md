# Reward / plant-celebration flow — TODOs to fix

Observed (device, Oppo): plant popup appears **mid-sim** after ~7 clicks; **again** when going back to Plan; **again** on entering the next sim with no clicks; and a stray "you're on fire" with no action. Root causes and fixes below. Target end-state: **one** plant/component celebration per completion, shown **once**, **after** you leave the sim (on Plan/home), with XP — never mid-sim, never re-shown, never on a fresh sim entry.

## Root causes (from code + logs)
- The global celebration host (`GardenCelebrationHost`) is suppressed **only while the Plan screen is visible** (`suppressGlobalHost`). During a **sim** it is NOT suppressed → the chapter-completion path (`markSimulationUrlCompleted` → `growGarden`, fires at the 7-tap gate) pops the plant **mid-sim**.
- `queueCelebrationForUnshownPlant` is called from **two** places in `PlanTrialViewModel` and **suspends** between its `pending == null` check and `notifyPlanted` → both pass the check → **double `notifyPlanted`** (seen at 11:24:00.684 & .698). A late second notify re-arms `pending` after Plan cleared it → **popup on next sim with no clicks**.
- `lastCelebratedPlantTotal` is only written by the Plan path. When the **global host** shows a plant (mid-sim), it does **not** write it → Plan re-queues the same plant → **duplicate on return**.
- "You're on fire" is a **separate** gamification/streak moment, not garden — likely `pendingReward`/streak re-emitting on navigation.

## TODO — status
1. ✅ **DONE. Suppress the global host during a sim.** `ConceptSimulationViewModel.onViewerVisible/onViewerHidden` toggle `setGlobalHostSuppressed`, wired from a `DisposableEffect` in `ConceptSimulationViewer`. Gated to `activeTrialItemId == null` (free-browse only) so it never fights the Plan screen's own suppression during a plan-sim → Plan handoff. *(Sim Agent screen: apply the same pattern if agent sims can plant mid-run.)*

2. ✅ **DONE. `notifyPlanted` is idempotent.** `GardenMomentCoordinator` tracks `lastCelebratedTotal`; a `notifyPlanted` whose `totalPlanted <= lastCelebratedTotal` is ignored (logged `notifyPlanted IGNORED`). Method is `@Synchronized`. This single guard kills both the double-notify **and** the re-arm-on-next-screen (the second/third pop-ups).

3. ✅ **DONE. Single writer for `lastCelebratedPlantTotal`.** `GardenCelebrationHost.markCelebrationShown()` persists it on show + dismiss (Cursor); the coordinator's in-memory guard is **seeded from prefs** on VM init (`syncCelebratedTotal`) so it survives an app restart.

4. ✅ **DONE (via 2).** The in-memory guard means a re-notify of an already-celebrated total is dropped, so `pending` can't be re-armed on the next screen.

5. ✅ **Largely DE-RISKED (was: verify 60 s window).** The old double-plant worry was that Plan-DONE (~2 taps) and the chapter-completion path (~7 taps) could plant twice if the gap exceeded the 60 s dedup window. Two later changes closed this:
   - **Free-browse chapter progress is disabled** (`CHAPTER_PROGRESS_FROM_FREE_BROWSE_ENABLED = false` in `ConceptSimulationViewModel`) — for free browsing there is no second path, so no double.
   - **For Plan sims,** chapter-credit only fires once the trial is already DONE, and the **knowledge-bite cap was raised 7 → 15** (`SimulationTrialThresholds.DEFAULT_GOAL`), so DONE and the chapter-credit are now **co-located** (~15 taps, moments apart), comfortably inside the 60 s window. The old "2 vs 7" spread — and the "gate=7 vs cap=15" mismatch — no longer apply (credit waits for DONE regardless of the 7 gate).
   - **Remaining check (low priority):** finish one Plan sim → confirm exactly **one** plant (logcat: one `recordCompletion` insert; the other attempt logged as skipped / `notifyPlanted IGNORED`). If two ever appear, widen `COMPLETION_DEDUP_WINDOW_MS`.

6. ✅ **DONE (via 1).** Free-sim plant stays `pending` while the viewer is up and surfaces once on the next safe screen. Plan sims already defer via `growGarden` + Plan's chain.

7. 🟠 **MONITOR.** "You're on fire!" is a `sim_done` **moment variant** (`MomentVariants.kt`), i.e. a celebration headline — the same stale-re-show class, so TODOs 1–2 should stop it appearing "without doing anything." If it still fires on plain navigation, check `RewardOverlayViewModel.pendingReward` isn't re-emitting.

8. **Test matrix (device, this working tree):**
   - In-sim 7 clicks → **no** popup.
   - Back to Plan → **one** plant + XP.
   - Open next sim, no clicks → **nothing**.
   - Back again → **nothing**.
   - Redo same sim after >window → **new** plant.
   - Watch logcat `GardenPlant`: expect a **single** `notifyPlanted` + `showingMoment=true` per completion.

## Notes
- These files are being edited live by Cursor; coordinate before applying (esp. TODO 1–3 touch `GardenMomentCoordinator`, `GardenCelebrationHost`, `PlanTrialViewModel`, `ConceptSimulationViewer`).
- Keep the `GardenPlant` debug logs until the matrix passes; remove before release.
