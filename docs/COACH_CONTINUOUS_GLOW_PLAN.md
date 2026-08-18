# Plan — move the V3 coach glow to a continuous page-side loop (the sandbox model)

> ## ✅ IMPLEMENTED (2026-08-07, behind the V3-math gate — needs a clean build to test)
> Phase 1–3 of this plan are done:
> - **Loop added** to `SimulationInteractionScript.kt`: `window.__eduCoach` runs `setInterval(tick,
>   400)`, re-reads `.mission` + options every tick, solves (ported from `coach-inject.js`, incl. the
>   pattern squares/lead-in fix), and outlines the right control with CSS classes — amber hint, green
>   submit, blue digit-input, red reteach. Dormant until `setActive(true)`.
> - **Toggles wired**: `SimulationWebView` gained `coachLoopActive` / `coachReteach`; it calls
>   `__eduCoach.setActive/​setReteach`. `ConceptSimulationViewer` sets
>   `coachLoopActive = inPhaseB && GUIDED && url~"math_" && !coachObserving`.
> - **Single owner**: when `coachLoopActive`, the old single-push `highlightStepIndex` is forced to
>   `null` (overlay hidden), so the two never fight. The old push still drives scripted teach, V1/V2,
>   and non-math sims.
> - **Bonus bug fixed in both solvers**: build `target` was `max(all numbers in prompt)`, which on an
>   overshoot picked up the "Current: N" value and broke Reset detection. Now parsed from "Target: N".
> - **Parity checked** (Node) against the vectors: compare `<`, round, pattern (7777 / 99980001 / 96),
>   build per-tap (+10,000→+100), Lock at target, **Reset on overshoot**.
>
> NOT yet done: retiring the old push path entirely (step 4 — leave until the loop is proven on
> device), the speed-sim option-selector widening (needs that sim's DOM), and the JS↔Kotlin solver
> parity test in `coach-sim/`. **Test: clean build, open a math sim in V3, confirm the glow tracks per
> tap and never freezes; the `CoachBuild` marker confirms the APK is current.**



## Why
Every recurring sync bug (frozen reteach glow, stale target, glow points at the wrong option after a
re-render, per-tap build tracking, the race before the first `reportMathProblem`) is the same root
cause: **the glow is computed in Kotlin from pushed state, and that state can disagree with the live
DOM for a moment.** The `coach-sim/coach-inject.js` sandbox never has this problem because it re-reads
the DOM, re-solves, and re-applies the glow every 500 ms — it's stateless and self-correcting.

This plan makes the in-app glow work the same way: a small **page-side loop owns the highlight**;
Kotlin keeps narration, verdicts, round flow, and the V1/V2/V3 selector. It's a scoped refactor of the
highlight path only, gated behind V3 so it can be A/B'd against the current build.

Principle: **one owner for the glow, and that owner reads ground truth on a timer.**

---

## Before → after

**Before (push model)**
```
tap → reportMathProblem → onMathProblem (Kotlin: MathCoachSolver) → sets mathAnswerStep /
  mathReteachStep / … → highlightStepIndex `when {}` recomputed → SimulationWebView
  LaunchedEffect → evaluateJavascript(__eduHighlight(index, kind)) → overlay positioned
```
Glow correctness depends on ~6 state vars all being consistent at the instant Kotlin pushes.

**After (pull model)**
```
setInterval(400ms) in page:  read live DOM → solve → outline the right control(s)
Kotlin: still gets reportMathProblem (for spoken "why"), still tracks verdicts/rounds,
         but NO LONGER pushes the glow. It only toggles the loop on/off and its mode.
```
Glow is re-derived from the DOM every tick, so it cannot freeze or go stale.

---

## What gets RETIRED vs KEPT

Retired (glow decision moves out of Kotlin):
- `ConceptSimulationViewer.kt` — the `highlightStepIndex` `when {}` block and `highlightKind`
  computation (the whole push path). Keep the *state* it read (`mathAnswerStep` etc.) only if still
  needed for TTS; otherwise drop.
- `SimulationWebView.kt` — the `LaunchedEffect(highlightStepIndex, highlightKind)` that calls
  `__eduHighlight`. Replaced by a one-time "start the loop" + small on/off toggles.
- `SimulationInteractionScript.kt` — `__eduHighlight(index, kind)` as the *primary* mechanism. The
  overlay drawing/positioning code is REUSED by the loop; only the "who calls it" changes.

Kept (unchanged):
- `MathCoachSolver.kt` — stays as the brain for the **spoken** number-specific feedback on verdicts.
  (See "solver source of truth" for keeping it from drifting from the JS solver.)
- Verdict flow (`InteractionTracker.verdicts`), round counting, practice `onCorrect/onWrong`, teach
  walkthrough narration, V1/V2/V3 selector, TTS ad-mute, Back/Replay/Continue.
- `reportMathProblem` bridge → Kotlin (still needed so Kotlin can speak the "why" line).

---

## The new component — an injected glow loop

Add to `SimulationInteractionScript.injectionScript` a self-contained loop, ported from
`coach-sim/coach-inject.js` (it already works on these exact sims). Shape:

```js
window.__eduCoach = (function () {
  var active = false;      // Kotlin toggles this per coach phase
  var reteach = false;     // Kotlin sets true briefly after a wrong verdict → red
  function tick() {
    if (!active) { window.__eduHideOverlay(); return; }
    var r = eduReadProblem();            // reuse reportMathProblem's reader (.mission + options)
    if (!r) { window.__eduHideOverlay(); return; }
    var s = eduSolve(r.prompt, r.opts);  // the SAME rules as coach-inject.js solve()
    // choose control + colour, then draw via the existing overlay code:
    //   building  → next place-value button   (amber)
    //   build done→ Lock/Submit               (green)
    //   overshoot → Reset                      (amber)
    //   MCQ       → correct option             (amber, or red while reteach)
    //   pattern   → option (amber) + digit input (blue)   ← multi-target, see below
    eduApplyGlow(target, colour);
  }
  return {
    start: function () { if (!window.__eduIv) window.__eduIv = setInterval(tick, 400); },
    setActive: function (v) { active = v; },
    setReteach: function (v) { reteach = v; },
  };
})();
```

Reuse what already exists in the script: `eduHarvest`, the `#__edu_hl_overlay` element,
`positionEduOverlay`, and the `.mission`/options reader from `reportMathProblem`. The solver rules are
a straight lift of `coach-inject.js` `solve()` (round/compare/ratio/comma/pattern/speed/build) — that
file is the reference implementation and is already validated live.

Multi-target (pattern needs option + digit field; build could show next-move + a faint Lock): allow up
to ~3 overlays instead of one (the sandbox already tracks `__gA/__gB/__gC`). This also finally covers
the pattern digit-count field (open item #7).

---

## Coordination API (Kotlin ↔ page)

Kotlin calls these via `evaluateJavascript` — tiny, infrequent, not per-frame:
- `window.__eduCoach.setActive(true|false)` — on when the coach is in a glow phase (V3 practice /
  guided teach step that points at a control), off during pure teach text, ads, or when the guide is
  dismissed.
- `window.__eduCoach.setReteach(true)` for ~4 s after a wrong verdict (colour the answer red), then
  `false`. This is the ONLY place red is used.

That's the whole surface. No per-tap pushing, no `highlightStepIndex`.

Add a bridge method if the loop needs to *tell Kotlin* the current answer for TTS (optional — Kotlin
already gets `reportMathProblem` and runs `MathCoachSolver` for the spoken line).

---

## Solver: one source of truth (avoid drift)

Two solvers exist today: `MathCoachSolver.kt` (spoken feedback) and `coach-inject.js` `solve()` (glow).
To stop them drifting:
- **Recommended:** the **JS loop is canonical for the glow**; Kotlin's `MathCoachSolver` is used ONLY
  for the spoken "why" on a verdict. Keep the two rule-sets byte-for-byte equivalent by driving both
  from the same test vectors (`MathCoachSolverTest` cases mirrored in a JS test in `coach-sim/`).
- Alternative (more work, less drift risk): Kotlin computes the answer and hands the *control
  selector/label* to the page via a bridge call; the loop only positions the overlay. Rejected for now
  because it reintroduces a Kotlin→page dependency (smaller, but still a push).

---

## Colours / states (final)
- **Amber** — proactive hint: next build move, suggested MCQ option, ratio/round/compare/speed answer.
- **Green** — the submit/Lock button, shown only when it's actually time (build `current==target`;
  MCQ after a selection; calc always) — mirrors the sandbox `showSubmit` rule.
- **Blue** — a secondary input the round also needs (pattern digit count).
- **Red** — only while `reteach` is set (the answer just missed).

---

## Migration steps (phased, behind the V3 flag)
1. **Land the loop dormant.** Add `window.__eduCoach` (active=false by default) + the ported solver to
   `SimulationInteractionScript`. No behaviour change yet. Verify the script still injects.
2. **Wire the toggles.** From `ConceptSimulationViewer`, call `setActive(true)` when entering a glow
   phase and `setActive(false)` on teach-text/ads/dismiss; `setReteach` around wrong verdicts.
3. **Switch ownership for math sims in V3.** When the loop is active, STOP the Kotlin push: make
   `SimulationWebView`'s highlight `LaunchedEffect` a no-op for V3 math (or remove it). One owner.
4. **Delete the dead push path** once the loop is proven (the `highlightStepIndex` `when {}` and
   `highlightKind`).
5. Keep V1/V2 exactly as-is (they can keep the old scripted push, or also use `setActive` for their
   scripted-step targets — decide after step 3).

Ship steps 1–3 behind V3 so you can flip between old/new by mode during QA.

---

## Edge cases / risks
- **Two systems fighting** — the #1 risk. Enforce single ownership (step 3): if the loop is active,
  Kotlin must not call `__eduHighlight`.
- **Sim re-creates option buttons on tap** — already handled: the loop re-reads + `eduHarvest`
  re-stamps every tick, so re-render can't strand the glow (this is *why* the loop is more robust).
- **Speed sim options not harvested** (`math_1_13`) — the loop's reader must match those buttons;
  widen the option selector once that sim's DOM is inspected. Same fix helps push or pull.
- **Performance** — a 400 ms `setInterval` doing a DOM read + a tiny solve is negligible; the sandbox
  runs at 500 ms with no jank. Stop the interval on page unload.
- **Tap jitter** — keep the "only scroll when off-screen" rule already added; the loop must NOT
  `scrollIntoView` on every tick (position the overlay in place; only scroll if the target left view).

---

## Test plan
- **JS solver parity:** a small test in `coach-sim/` running the same vectors as
  `MathCoachSolverTest` (round/compare/ratio/comma/pattern incl. squares+lead-in/speed/build) so the
  two solvers can't drift.
- **Loop behaviour on the real sims (browser):** open each `math_1_*` with the loop, tap through a
  round, confirm the glow tracks (build per-tap, Lock at target, Reset on overshoot, pattern option+
  digit, no freeze after a wrong answer).
- **On device (V3):** one clean build; capture a logcat with the build-sim overshoot + a pattern
  round-2 + the speed sim; confirm glow tracks and never freezes.
- Regression: V1/V2 unchanged.

---

## Rollback
Everything is behind V3 + the `setActive` toggle. To roll back: leave `active=false` and re-enable the
old push `LaunchedEffect`. No data or schema changes, so rollback is a flag flip.

## Effort
~½–1 focused day: porting the loop (mostly a lift of `coach-inject.js`), wiring two toggles, disabling
the old push, and the parity test. The payoff is that the whole "pushed state disagreed with the DOM"
bug class stops recurring.
