# v3 Coach + Maths Solver — Handoff / Build & Log Notes

## ✉️ QUICK NOTE TO CURSOR (2026-08-08)

**What landed:** a new coach mode **V4 = one-clock coach** (`SimCoachMode.ONE_CLOCK`), now the default.
It's the fix for "coach not in sync": one injected JS loop owns glow + text + voice; Kotlin is a
passive mirror. V1/V2/V3 untouched (fallback).

**Files changed (all committed to the working tree):**
- `SimGuide.kt` — added `ONE_CLOCK` enum + made it `DEFAULT`.
- `SimulationInteractionScript.kt` — injected `window.__eduCoachV4` loop (solver for all math sims +
  glow + `AndroidBridge.coachText/coachSpeak` on one tick). Syntax-checked OK.
- `SimulationWebViewBridge.kt` — `@JavascriptInterface coachSpeak(text)` + `coachText(text)`.
- `SimulationWebView.kt` — threaded `onCoachText/onCoachSpeak` + `coachV4Active` toggle.
- `ConceptSimulationViewer.kt` — V4 wiring: activate loop, mirror text into `SimAdaptiveCoachBar`,
  barge-in TTS; ALL V3 effects gated off in V4; added `ONE_CLOCK` to the 3 `when(coachMode)` blocks.

**Please do:**
1. **Clean build** (not incremental — history of stale APKs): `gradlew --stop` → delete `app/build`,
   `.gradle`, `build` → `gradlew clean assembleDebug --rerun-tasks` → `adb uninstall
   com.ncert7.aitutorandlab` → `gradlew installDebug`.
2. Open a math sim (opens in **V4** by default). Confirm logcat shows
   `CoachBuild: v4 one-clock ... (build 20260808)`.
3. Test the sync: glow + on-screen line + voice should move together and reset together each round.
   Try compare, rounding, pattern (option + digit box), and build.
4. If anything's off, capture a logcat filtered for `CoachBuild coachSpeak coachText` inside a math
   sim and share it.

**Not done yet:** the `window.__eduRound` native publish hook in the sim HTML (repo `EduAI_app`) —
optional robustness upgrade; the DOM scrape works today.

---



## 🆕 V4 — the one-clock coach (build 20260808)

New mode `SimCoachMode.ONE_CLOCK` ("V4"), now the DEFAULT. It fixes the "not in sync" problem
architecturally: instead of Kotlin driving glow/voice/text on separate clocks, a single page-side
loop owns all three and Kotlin is a passive mirror.

How it works:
- **One JS loop** (`window.__eduCoachV4` in `SimulationInteractionScript.kt`) reads each round, solves
  ALL math sims (the validated coach-all solver: compare/round/ratio/comma/pattern-incl-squares/speed/
  build), glows the right control (reusing the `.edu-g-*` classes), and on the SAME tick calls
  `AndroidBridge.coachText(line)` (display) and `AndroidBridge.coachSpeak(line)` (voice). Dormant until
  `setActive(true)`.
- **Kotlin is passive** (`ConceptSimulationViewer`): `coachV4Active = isV4 && guideUnlocked &&
  !dismissed` turns the loop on; `onCoachText` mirrors the line into a `SimAdaptiveCoachBar`;
  `onCoachSpeak` does barge-in TTS (`stop()` then `speak`, ad-mute + lakh-sanitizer inherited). No
  solving, no phase machine, no timers — so glow/text/voice can't drift.
- **V3 fully bypassed in V4:** verdict collector returns early, teach/practice/nudge/exploration
  effects gate false, highlight push = null, the V3 continuous loop is left inactive. V1/V2/V3 remain
  selectable and untouched (fallback).
- Bridge: `coachSpeak`/`coachText` `@JavascriptInterface` in `SimulationWebViewBridge`, threaded via
  `SimulationWebView`. Loader: `LaunchedEffect(coachV4Active) -> __eduCoachV4.setActive(...)`.

Validated: injected script syntax OK; the coach-all solver passed 11/11 offline cases (every sim type
+ build start/mid/done/overshoot). NEEDS a device build + real-user retest (this is the fix for
"coach not in sync"). Confirm the running APK with the `CoachBuild: v4 one-clock ... (build 20260808)`
log line. Not yet done: the `window.__eduRound` native publish hook in the sims (the DOM scrape works
now; native publish is the drop-in robustness upgrade later).

## ✅ STATUS 2026-08-07 (verified against `coach-check-20260807-1241.txt` + `new app math demo.mp4`)

Confirmed WORKING in V3 on device (correct glow + number-specific feedback, full rounds):
- Nearest-neighbour / rounding sims, Population Estimate Radar, **Ratio growth-factor** (7.4x/9.5x/11.5x),
  Pattern round 1, and the **BUILD sim per-tap glow** — log shows the glow tracking every tap for
  target 40,629 (+10,000 → +100 → +10 → +1 as the remainder shrank) and `move=null step=1` = **Lock
  Build glows exactly at current==target**. The build terminal-glow fix works.

### 🚨 "COACH NOT IN SYNC" — the four channels drifting (build 20260807d)
Real-user test: "worst experience, coach not in sync — text says one thing, voice another, sim
another, glow another." Root cause: the continuous loop made the GLOW live, but VOICE and TEXT stayed
on the OLD event/timer paths, so with a real learner moving at their own pace all four drifted apart.
Fix — put voice + text on the same clock as the glow:
- **Voice stopped on every round change** (`onMathProblem` roundKey change → `keyConceptTts.stop()`).
  The slow TTS queue narrating the previous round was the biggest "voice out of step" source.
- **Timer-driven generic narration suppressed while the loop leads.** `practiceActive` gains
  `&& !coachLoopActive` (no "tap Reset / tap the biggest button" step lines drifting from the glow),
  and the Phase-B lull nudge is skipped when `coachLoopActive` (no fourth "you're stuck" voice). Finish
  detection still runs.
- Net during a live math round: **glow leads (live) + only the current verdict feedback speaks**, and
  everything resets together on each round. Teach phase is unchanged (push glow + its own step
  narration are already the same step, so coherent).
- STILL long: verdict feedback lines are wordy/slow; they're now cut on round change so their lag is
  bounded, but if voice still feels behind, shorten the spoken feedback (keep the full text on screen).
- NEEDS A REAL-USER RETEST — this is the fix for the exact complaint; frame-by-frame video can't show
  voice timing.

### 🛠️ ARCHITECTURE-REVIEW FIXES (2026-08-07, build 20260807c) — P0/P1 from the gap list
1. **P0 — loop no longer runs during teach.** `coachLoopActive` narrowed to `inPhaseB && solvable`
   (`mathSolution != null || mathMoveActive`). Teach + method-only + pre-solve rounds fall back to the
   PUSH, so scripted `targetIndex` reaches the DOM (MCQ teach glows Check, not the answer) and
   combinatorial practice isn't left dark. (Reverts the earlier "loop during teach" change.)
2. **P1 — stale why-line cleared on round change.** `onMathProblem` computes a round key (prompt with
   the build `Current/Clicks` tail stripped) and, on change, clears `coachObserving` + `coachMessage`
   + `mathReteachStep` so the bar reflects the round on screen (fixes the Compare hold-over).
3. **P1 — JS build parse is `.mission`-only.** Reads BOTH Target and Current from the mission prompt
   (not `document.body`), requires an explicit `Target:` (dropped the `Math.max` fallback that grabbed
   the Current value on overshoot). Matches Kotlin. Verified: +10,000 / +100 / LOCK / RESET.
4. **P1 — Reset gone → glow Next Target.** The build reset action falls back to
   `Next Target / Next Round / Next` when no Reset button exists (after a failed Lock).
5. **P1 — speed `1_13` options harvested.** WebFetch confirmed the sim is client-rendered (options not
   in static DOM). Added an exact-match fallback (`^[\d,]+ km/day$`) to BOTH `reportMathProblem` and
   the loop reader, so the solver receives the options → `mathSolvable` true → loop glows.
6. **P1 — reteach auto-expires.** A 4s `LaunchedEffect` clears `mathReteachStep` so the red answer
   glow can't persist if the learner neither taps Continue nor changes rounds.

Not changed (P2, by design / accepted): method-only sims stay hint-only; ease-off vs 3-min overlay
independent clocks; no red reteach without a `practice` block.

### 🎥 RE-REVIEW `new app math demo 2.mp4` (2026-08-07 18:58) — continuous loop is LIVE
Build had the loop + pattern-fix (NOT yet: teach-glow, tts-lakh-fix, population-fix). Confirmed:
- **All pattern round types glow** — repunit 7777, **squares 99980001**, **geometric 12 24 48→96**,
  ×10 15000. The squares/geometric rounds (previously blank) now light up. Multi-target glow works:
  option (amber) + digit field (blue) + submit (green) together.

Real issues caught on the re-review:
1. **Digit-count not communicated (FIXED).** On the pattern sim the child got rounds 2 AND 6 WRONG
   even while the option was glowed — the loop highlighted the digit-count field (blue) but never said
   WHAT to type. Fix: the loop now puts the count in the field's placeholder ("Type 8 — that many
   digits") while empty, restored in `clearGlow`. The solver already had `s.digits`.
2. **`math_1_7` Number Cards Target Dash dead-ends.** ~40s stuck on "Step 1 of 5", no glow, coach just
   repeats "combine the cards" — it's the combinatorial method-only sim (no single answer to glow).
   Needs that sim to expose a scoring/step signal to be fully guided; otherwise it's the weak spot.
3. **Feedback-overlap on Compare (minor).** Round N's feedback ("Correct — 30,000 < 3,00,000") holds
   on screen with Continue while the sim already shows round N+1 ("500 lakh vs 5 million"). Numbers are
   correct for their round; the sim just auto-advances under the held card. Could tie feedback
   dismissal to the round change. Low severity.

### 🎥 VIDEO REVIEW `new app math demo 1.mp4` (2026-08-07 16:11)
Confirmed WORKING on device: amber hint colour (+1,000 glows amber, not red), build per-tap glow,
Lock-at-target ("Locked! …" at current==target), rounding sims, build success.

- **NEW BUG FIXED — reteach glow froze on the build sim.** On an overshoot (target 40,629, child at
  41,000→72,000) **+1,000 stayed glowing RED** while they kept tapping it. After a wrong Lock,
  `mathReteachStep = mathAnswerStep` froze the glow; for the stateful build sim that traps the learner
  on a stale button and overrides the correct "glow Reset on overshoot". Fix: build sims
  (`mathMoveActive`) no longer freeze the reteach glow — the live solver glow (Reset on overshoot /
  next button while building) stays in control. (`ConceptSimulationViewer.kt`)

- **Pattern rounds 2–3 (squares / geometric) still not glowing in THIS capture.** The solver rewrite
  is verified correct in isolation (81 9801 998001→99980001, 12 24 48→96), so an APK that still shows
  no glow here most likely did NOT recompile `MathCoachSolver.kt` (incremental-compile gap). Note the
  `CoachBuild` marker CAN'T detect this — it lives in `ConceptSimulationViewer` (which did recompile).
  → **Do a full clean build** and run `./gradlew testDebugUnitTest --tests "*MathCoachSolverTest"` to
  prove the solver logic before judging on device.

- **Speed sim `math_1_13` (Marathon Builder) doesn't glow.** `solveSpeed` is correct (42,000÷60=700),
  but its option buttons ("500 km/day"/"700 km/day") are likely NOT matched by the `reportMathProblem`
  option selector (`.opt, .choice, button[data-v]`), so the solver receives no options → no glow →
  exploration → concludes. Needs the sim's option DOM inspected to widen the selector (couldn't verify
  the exact markup from here).

- **Watch: rounding worked-feedback referenced off-screen numbers.** e.g. screen showed 1,24,42,373
  but the "why" said "6,72,85,183 rounds down…". Looks like `mathSolution` lagging the round change
  (verdict fired against a stale problem). Needs a log to confirm; scoring itself was correct.

### 🔧 PRIORITY-LIST FIXES (2026-08-07, round 2)
- **#2 Sticky V2 pref migration.** `getSimCoachMode()` now runs a one-time migration: if `ADAPTIVE`
  is stored from before v3 became the default (and the user never explicitly chose it), it's cleared
  once so the device picks up the new `GUIDED` default. An explicit `setSimCoachMode()` also marks the
  migration done, so a deliberate V2 choice is never second-guessed. (`SharedPreferenceUtils.kt`)
- **#3 Startup-race grace.** The safety wait only fired once `mathAnswerStep != null`; if the injected
  JS was slow to send the first `reportMathProblem`, the empty-`exploreOptions` path could still ease
  off. Added a guard: on a `math_*` sim, don't conclude within the first 6s of Phase B.
  (`ConceptSimulationViewer.kt`)
- **#4 Unit tests for the pattern rewrite.** Created `MathCoachSolverTest` with the two cases that
  failed on device — squares `81 9801 998001 → 99980001` and geometric-with-lead-in `12 24 48 → 96`
  — plus the repunit `7 77 777 → 7777`, the build solver (biggest-fit then null at target/overshoot),
  and a compare sanity check. Run: `./gradlew testDebugUnitTest --tests "*MathCoachSolverTest"`.

Still open by design (from the list):
- **#5 method-only sims (1_1/1_3/1_7)** have no Check → no verdict → ease off at the cap. The #3 grace
  gives them ≥6s; full V3 would need an on-tap/Submit scoring signal from those sims.
- **#6 Lock Build while not glowing** can still lock an empty build (following the glow avoids it).
  Disabling Lock until `current == target` means mutating the sim's own DOM — intrusive, left out.
- **#7 pattern digit-count field** isn't solver-driven (only the option is glowed). Needs a second
  simultaneous highlight; minor.

### 🎨 REVIEW-LIST FIXES (2026-08-07)
- **Distinct hint colour (was: red for both answer & next-move → "looks like an error").** The
  highlight overlay now takes a `kind`: **amber** for a proactive hint (next build move / suggested
  option) and **red** only for the answer the learner just missed (reteach). Threaded
  `highlightKind` viewer → `SimulationWebView` → `__eduHighlight(index, kind)`.
- **Stale button copy ("Tap Check Factor" while UI shows "Next").** These sims relabel the submit
  button after answering, so any hardcoded label goes stale. Reworded the step copy in `math_1_10`
  (ratio), `math_1_5_new` (pattern), `math_1_8` (compare) to "tap the highlighted check button"; the
  glow still points at the right control regardless of its current label.
- **Wrong-answer path (verified, not a bug).** On a wrong verdict the coach sets
  `mathReteachStep = mathAnswerStep` (glows the correct option, now RED via the `kind` split) and
  speaks the solver's `whyWrong`; it clears on Continue (`onPhaseBContinue`). It just wasn't captured
  on video — to see it, miss once on `math_1_6`/`math_1_8` then Check.
- **Teach card after "Correct" (intentional).** The card that stays is the coach bar itself carrying
  the practice-round prompt; Phase B has taken over (log shows `Adaptive coach entered`). No change.

### 🖐️ TAP-BLOCKING BUG — the glowing button couldn't be tapped (FIXED)
User: *"I wasn't able to tap when the buttons were glowing, e.g. +1,000 taps."* Root cause in
`SimulationInteractionScript.kt`: `window.__eduHighlight()` ran `el.scrollIntoView({behavior:'smooth',
block:'center'})` on **every** highlight. Each re-highlight smooth-scrolled the glowing button to
centre, moving it out from under the finger mid-tap (worst on the build sim, where you tap the same
+1,000 repeatedly). The overlay itself was never the problem (`pointer-events:none`) and the click
listener doesn't `preventDefault`. Fix: only `scrollIntoView` when the target is genuinely off-screen,
skip it entirely when the same element is re-highlighted, and scroll instantly (no smooth animation)
so nothing shifts mid-tap. This is likely the single biggest cause of the coach "not working" — a
correct glow is useless if the highlighted control can't be tapped.

Fixed THIS session (need the next rebuild):
1. **Pattern solver (`math_1_5_new`) rounds 2–3.** Log showed `no solution` for "12 24 48 ?" (the
   lead-in "2" from "multiplied by 2" corrupted the ratio) and for the squares round "81 9801 998001".
   `solvePattern` rewritten: tries progressively shorter trailing runs (discards lead-in) + added a
   squares rule (roots 9,99,999 → 9999² = 99980001). Verified: 7 77 777→7777, 81 9801 998001→99980001,
   12 24 48→96.
2. **Build teach step pushed an empty lock.** Teach "Step 4/5: tap Lock Build" glowed Lock Build while
   Current==0 (video). Reworded to a non-actionable beat ("the app highlights Lock Build when you
   match — no need to lock an empty build"); the solver drives the real Lock Build glow in practice.
3. Build marker bumped → `CoachBuild: ... build-terminal-glow + pattern-v2 (build 20260807)` so logs
   distinguish this from the 20260805 build.

Residual to watch: build round can still show "Not exact: built 0" if the child taps Lock Build while
it's NOT glowing (off-glow tap). Following the glow avoids it. Comma (`math_1_2`) and method-only sims
(`1_3`, `1_7`) ease off at the safety cap (~2×easeOff s) — acceptable, but if you want them fully
guided to round-completion, they need an on-tap-scoring round counter (no Check button → no verdict).

---


> Read this before building or debugging the guided simulation coach. The most common failure so
> far has been **testing a stale build** — a plain incremental build keeps missing the Kotlin
> changes (symptom: new guide text shows, but old runtime behaviour / auto-advance persists).

---

## 🟢 LOG-PROVEN ROOT CAUSE + FIX (2026-08-05, `coach-v3-math-20260805-190045.txt`)

The coach diagnostic log for `math_1_8.html` (Compare) shows the smoking gun:

```
19:01:45.014 CoachBuild: guided-coach v3 + math-solver + no-auto-advance (build 20260805)  ← APK is FRESH
19:01:45.973 reportMathProblem: {"prompt":"... 30 thousand 3 lakh", "options":[{"label":"<",...}]}  ← extract OK
19:01:45.979 MathCoach: solved ... answer=< step=9000 opts=3                                   ← solver OK
19:01:51.064 Adaptive coach entered ... interactions=1
19:01:53.102 Adaptive coach eased off                                                          ← BUG: 2s later
19:01:53.148 coach|1: "You can compare large numbers ... Keep going, or move on"               ← concluded
```

So build/extract/solver are all correct, but the coach **concluded ~2 seconds after teaching, before
the child answered** — no answer glow, no worked feedback. Two coupled defects, both now FIXED in
`ConceptSimulationViewer.kt`:

1. **Premature conclusion.** In the Phase-B `finished` `when`, the exploration branch treated
   `exploreOptions.isEmpty()` as "done". For MCQ math sims the answer options arrive via
   `reportMathProblem` (stamped step 9000+), NOT the harvested control list — so `exploreOptions` is
   empty and the coach concluded instantly. **Fix:** added a branch `mathAnswerStep != null ->
   safetyCap` (before the exploration `else`) so a pending, unanswered math problem keeps the coach
   waiting for the attempt instead of concluding.
2. **Answer glow gated on `sawVerdict`.** The highlight only showed the correct option *after* a
   first verdict — but a no-practice sim never got there. **Fix:** dropped `sawVerdict &&` from the
   `highlightStepIndex` math branch, so the correct option glows as soon as the child reaches Phase B
   with a solvable problem (still `inPhaseB`, so never during the lesson).

**Why some sims already worked in the video (rounding/pattern):** they ship an authored `practice{}`
block (`hasPractice == true`), which routes through the `sawVerdict` scoring path, not the empty
exploration path. The no-practice MCQ sims (Compare, etc.) fell into the bug. Both paths are now
covered.

**Verify after rebuild:** open `math_1_8` in V3 — the correct sign should glow immediately in Phase B,
and after Check the coach should give the number-specific line and continue rounds (no "eased off"
within 2s). The build sim (below) is a separate, still-open issue.

---

## 📹 VIDEO DIAGNOSIS (2026-08-05, `Guide_not_workign.mp4`) — build sim still needs work

Reviewed the full screen recording in V3. **Most of V3 works** — do NOT rewrite the coach:
- **Rounding** sim: correct option highlights; feedback is number-specific ("digit after the
  thousands place is 9 → 3,87,69,957 rounds up to 3,87,70,000"). ✅
- **Pattern Pulse**: correct option glows green, wrong distractor red (7777✓/77777✗; 99980001✓). ✅
- **Compare** (30 thousand vs 3 lakh) and the teach walkthroughs (Step x/5, Back/Replay/Next, no
  auto-advance). ✅

**The broken sim: `math_1_4_new` "Build target using place-value taps"** (stateful build sim).

FIXED (2026-08-05):
- **(c) Wrong-scale authored hint — ROOT of the "start with +1,00,000" bug.** `math_1_4_new.guide.json`
  hardcoded `"+1,00,000"` as the button (text + `target`) in teach step 2 AND practice step 2, so for
  a target of 5,072/8,300 it told the child to tap one lakh (overshoot) and force-highlighted the
  wrong button, fighting the solver. Both are now scale-agnostic ("tap the biggest button that still
  fits — the app highlights it"); the `+1,00,000` targets were removed so the solver's glow drives it.
  The `whenStuck` +1,00,000 reference was also genericised.
- **(b) Per-tap re-glow now works.** Verified: `reportMathProblem()` includes `current` in its payload
  and re-fires whenever `current` changes (each place-value tap), and `onMathProblem` re-runs
  `solveBuild`. Combined with the Phase-B glow fix above, the correct next button re-glows per tap.

- **Terminal-state glow — now matches the HTML sandbox (FIXED).** The sandbox
  (`coach-inject.js`) drives build purely from live state: glow the next place-value button while
  building, then glow the submit button the moment `current == target` (`buildDone = isBuild &&
  !s.ans`). The app now does the same in `onMathProblem`: `solveBuild` gives the next button while
  building; when `current == target` it resolves **Lock Build** from `allControls` and glows it; when
  `current > target` it glows **Reset**. Because the glow only points at Lock Build once the build is
  actually complete, a child who follows the glow no longer locks an empty/partial build (that was the
  "Not exact: built 0" path). Combined with the Phase-B glow fix, the whole build round now tracks
  per tap like the sandbox.

Residual (verify on device): the practice-step *narration* sequence (Reset→button→keep-adding→Lock)
is still tap-advanced and can drift from the live build, but the GLOW (the primary handholding) is now
state-driven and correct, so following the glow keeps the child in sync. If narration still feels off,
the clean follow-up is to drop the authored build practice steps entirely and let the solver glow +
the "Current n/target" line drive the round (exactly the sandbox model).

Leave round/pattern/compare as-is (they work).

---

## 🎯 REQUEST TO CURSOR — make V3 math handholding actually fire (2026-08-05)

**Scope: chapter-1 MATH sims only, coach mode V3 (GUIDED).**

**Expected behaviour (already works in the HTML sandbox `coach-sim/coach_simulator.html`):**
for each math problem the coach should (1) read the LIVE problem from the DOM, (2) solve it with
`MathCoachSolver`, (3) **glow the correct option**, and (4) on a verdict speak a **number-specific**
"why" line — round after round until the rounds are done.

**Status:** the Kotlin path is wired end-to-end and verified by inspection:
`SimulationInteractionScript.reportMathProblem()` → `SimulationWebViewBridge.reportMathProblem()`
(@JavascriptInterface, main-thread post) → `SimulationWebView` sets `bridge.onMathProblemReported`
→ `ConceptSimulationViewer.onMathProblem` (line ~482) parses JSON, calls `MathCoachSolver.solve()`,
sets `mathAnswerStep`. Default mode is now `GUIDED`.

**So if it still doesn't show, it's ONE of these runtime conditions — check in this order with a
logcat captured INSIDE a math sim in V3 (filter: `CoachBuild reportMathProblem MathCoach
ConceptSimulation`):**

1. **Is the latest APK running?** Look for `CoachBuild: guided-coach v3 ...`. If absent → stale build
   (section 1 force-clean). If present → skip to 2.
2. **Is the problem being extracted?** Look for `reportMathProblem` traffic. If absent, the injected
   JS isn't finding this sim's DOM — its `.mission` / option selectors differ. Add a `DebugLogger`
   in `SimulationWebViewBridge.reportMathProblem(json)` logging `json.take(160)` and tune the
   selectors in `reportMathProblem()` for that specific sim.
3. **Did the solver produce an answer?** In `onMathProblem`, `mathAnswerStep` must be non-null. If
   the JSON arrives but `mathAnswerStep` stays null → the option label→step match failed (log
   `sol.correctOptionLabel` vs the option labels).
4. **Is the glow gate open?** The proactive answer glow requires `inPhaseB && GUIDED && sawVerdict &&
   mathAnswerStep != null && !coachObserving`. Two gotchas: (a) **`sawVerdict` must be true** — the
   glow only appears AFTER the first Check/verdict, not on the very first look at a problem;
   (b) it only runs in **Phase B (practice)**, i.e. AFTER the teach walkthrough. If a sim's guide has
   **no authored `practice{}` block** (`hasPractice == false`), practice never starts — that sim
   needs a `practice` block added to its `*.guide.json`, or it falls back to `suggestSteps`.

**Most likely culprit given "I'm on V3 and it's still not right":** #2 (selectors differ per sim) or
#4b (no `practice{}` block → no practice phase → no per-round glow). A single logcat from one failing
math sim will tell us which. Please capture and attach it.

---

## ✅ LATEST (2026-08-05) — ROOT CAUSE FOUND (it was NOT a stale build)

The reported symptoms — "steps auto-advance by themselves" + "no answer glow / worked feedback" —
are **exactly what V2 (ADAPTIVE) is designed to do**, and **V2 was the default coach mode**. A fresh
install opened in V2, so none of the v3 handholding showed.

- Answer glow + number-specific worked feedback are **GUIDED (v3) only** — the verdict collector's
  solver-fed feedback is gated `coachMode == SimCoachMode.GUIDED` (`ConceptSimulationViewer.kt`),
  and the proactive answer glow is likewise v3-gated. In V2 the coach stays hands-off.
- In V2, `scriptedSteps = guideSteps.take(introStepCount)` — only a brief intro — then it calls
  `enterPhaseB()` and drops the walkthrough. That "run a couple of steps then stop guiding" reads
  as auto-advance.

**Fix applied:** `SimGuide.kt` → `SimCoachMode.DEFAULT` changed from `ADAPTIVE` to `GUIDED`, so the
app opens in the full v3 tutor. V1/V2 stay selectable in the header for comparison.

**Note:** the mode is persisted in SharedPreferences (`getSimCoachMode`). If the device already has
a stored value of `ADAPTIVE` from earlier testing, the new default won't apply until the user taps
**V3** once (or clears app data / reinstalls). Tell the tester to tap **V3** at least once.

**Also new this build:** a startup log marker in `ConceptSimulationViewer.kt` logs on every sim open —
`CoachBuild: guided-coach v3 + math-solver + no-auto-advance (build 20260805)` — so you can confirm
the running APK is current (`adb logcat | findstr CoachBuild`). Section 1 has the force-clean steps
if you ever DO need to rule out a stale build.

---

## 1. The build keeps shipping PARTIAL changes — force a FULL recompile

Observed symptom: the app shows the **V1·V2·V3 toggle** (an early change) but still **auto-advances
steps** and has **no answer glow** (later changes) — even though all three live in the SAME files
(`ConceptSimulationViewer.kt`, etc.). That means Gradle's incremental compiler is reusing stale
compiled classes for changed files, and is not compiling the new files (`MathCoachSolver.kt`).

### Verify what's actually running
A build marker was added. Open a sim and grep the log:
```bash
adb logcat -c && adb logcat | grep CoachBuild
# expect: CoachBuild: guided-coach v3 + math-solver + no-auto-advance (build 20260805)
```
If that line does NOT appear when you open a simulation, the running APK is stale — do the force-clean.

### Force a clean build (sledgehammer — do all of it)
```bash
./gradlew --stop
```
```powershell
# Windows PowerShell — from the repo root:
Remove-Item -Recurse -Force app\build, build, .gradle -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\caches\build-cache-1" -ErrorAction SilentlyContinue
```
- **Android Studio → File → Invalidate Caches / Restart → Invalidate and Restart.**
- Then **Build → Clean Project**, then **Build → Rebuild Project** (NOT "Apply Changes"/hot-swap).
```bash
.\gradlew clean assembleDebug --rerun-tasks
adb uninstall com.ncert7.aitutorandlab
.\gradlew installDebug
```
Run the app fresh (full Run ▶, not Apply Changes). Then re-check the `CoachBuild` log line.

### Also make sure the math handholding is visible
- The correct-answer glow + worked feedback are **V3 only** and only during the **practice rounds**
  (after the teach walkthrough). Tap **V3** in the header and get past the lesson into practice.

---

## 2. What changed (all in `app/src/main/java/.../ui/screens/conceptscreen/components/` unless noted)

Guided coach (v1/v2/v3), earlier work:
- `SimGuide.kt` — `SimCoachMode` (SCRIPTED/ADAPTIVE/GUIDED = V1/V2/V3), `SimCoachData` (mission,
  whenWrong/stuck/correct/deviate banks, `elements` inference, `SimPracticeDoc` practice rounds).
- `ConceptSimulationViewer.kt` — the whole coach state machine: teach → practice/exploration,
  no auto-advance (Next/Continue only), retry-same-problem on wrong, Back/Replay/Continue,
  close-guide-stops-voice, "next topic" after rounds, V1/V2/V3 selector in the header.
- `ConceptSimulationViewModel.kt` — coach session fields + `restartCoach`, `enterAdaptiveCoach`,
  `easeOffCoach`.
- `SimCoachOverlay.kt` — scripted bar + `SimAdaptiveCoachBar` + Back/Replay/Continue chips.
- `SimulationHeader.kt` — V1·V2·V3 segmented selector.
- `SimulationKeyConceptTts.kt` — `speakSimulationCoach`, `speakSimulationReplay`.
- `TextToSpeech.kt` (ui/viewModel) — global **ad-mute**: `onAdShown()/onAdDismissed()` stop all
  TTS while a fullscreen ad is up; `RewardedAdManager` calls them.
- `InteractionTracker.kt` (service/analytics) — `verdicts` SharedFlow.
- assets `app/src/main/assets/sim_guides/*.guide.json` — authored `coach{}` + `practice{}` blocks
  for chapter‑1 science + math.

Maths solver (NEW this session):
- `MathCoachSolver.kt` — pure solver for 10 of 13 chapter‑1 math sims (round, compare, ratio,
  comma, pattern, speed + build/calc). Verified LIVE against the real sim DOMs. **KDoc holds the
  verified DOM extraction contract.**
- `test/.../MathCoachSolverTest.kt` — 14 cases using the exact harvested prompts/options.
- `SimulationInteractionScript.kt` (simulation_agent/components) — added `reportMathProblem()`
  extractor: reads `.mission` (p + `.num`) + options (`.opt`/`.choice`/`button[data-v]`, stamps
  numeric options with `data-edu-step`) + "Current:" total; re-reads after each tap.
- `SimulationWebViewBridge.kt` — `reportMathProblem(json)` → `onMathProblemReported`.
- `SimulationWebView.kt` — threads `onMathProblemReported`.
- `ConceptSimulationViewer.kt` — `onMathProblem` handler: parse JSON → `MathCoachSolver.solve()`;
  on verdict use the solver's number-specific `whyCorrect`/`whyWrong` (fallback = authored hints,
  never guesses); on wrong, glow the correct option; build sims glow the next place-value button.

Design sandbox (not shipped): `coach-sim/coach_simulator.html` — the approved coach feel/flow.

---

## 3. Capturing a USEFUL logcat (must be INSIDE a math sim)

The earlier logs only covered app startup / Home. Capture while a **math simulation is open** in
**V3**, and get one answer wrong on purpose:

```bash
# clear, then record while you use a math sim, then Ctrl-C
adb logcat -c
adb logcat -v time | grep -E "ConceptSimulation|SimulationWebViewBridge|InteractionTracker|SimGuide|SimulationIntroTts|TTS:|MathCoach|reportMathProblem|Adaptive coach|Coach restarted|eased off" > coach_session.txt
```

What to confirm in the log:
- `SimulationWebViewBridge: reportGuideStructure: {...}` (controls harvested),
- the coach reaches **practice** (V3): look for the round narration / `Adaptive coach entered`,
- on a wrong answer, the coach speaks a **number-specific** line (rounding/compare/etc.).

Note: `reportMathProblem` isn't logged yet. If you want to trace it, add a `DebugLogger.debugLog`
in `SimulationWebViewBridge.reportMathProblem(json)` (log `json.take(160)`).

---

## 4. Live validation already done (so focus debugging on build/wiring, not the math)

The extractor + solver + highlight were run against the **real** sims via Chrome:
- `math_1_6` rounding: read `Round … nearest 1,000: 3,87,69,957` → answer `3,87,70,000`,
  highlighted the correct option, feedback "digit after the thousands place is 9 → rounds up".
- `math_1_8` compare: parsed `30 thousand`→30,000 and `3 lakh`→300,000 (the mixed-notation trap).

So the maths logic is correct against reality. If the app doesn't match, suspect (a) stale build,
(b) the injected script not re-injecting, or (c) `.mission`/option selectors differing on a
specific sim — send a log from that sim and tune `reportMathProblem()` selectors.

---

## 5. Coverage

- **Full solver (highlight + worked feedback):** 1_2, 1_4, 1_5, 1_6, 1_8, 1_9, 1_10, 1_12, 1_13
  (+1_11 partial).
- **Method-only (combinatorial):** 1_1 (arrange A+B), 1_3 (permutation vault), 1_7 (card
  expression) — these stay on authored method hints by design.

---

## 6. Tests

```bash
./gradlew testDebugUnitTest --tests "*MathCoachSolverTest" --tests "*SimCoachDataTest"
```
