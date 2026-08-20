# Coach coverage — pending chapters checklist

**Generated:** 2026-08-20 · from `Simulations/*.html` vs authored `app/src/main/assets/sim_guides/*.guide.json`

The guided coach (V4 one-clock) runs off an authored **`<concept>.guide.json`** per simulation
(mission + `coach{}` + `practice{}` blocks). A sim with no guide falls back to hints-only / no
handholding. This is the list of what still needs a guide authored.

## Summary

| | Concepts |
|---|---|
| Total distinct sim concepts | **161** |
| Coach done (guide authored) | **23** |
| **Pending (no guide)** | **138** across **15 chapters** |

**Done:** Math Ch.1 (13) · Science Ch.2 (10).
(3 of the Math Ch.1 sims — `math_1_1`, `1_3`, `1_7` — are *method-only*: hints, no answer-glow, by design.)

---

## Pending — per chapter

Tick each concept as its `guide.json` is authored + verified on device.

### Math (77 concepts, 8 chapters)

| Chapter | Concepts | Count | Done |
|---|---|---|---|
| Math Ch.2 | `math_2_1 … math_2_14` | 14 | ☐ |
| Math Ch.3 | `math_3_1 … math_3_10` | 10 | ☐ |
| Math Ch.4 | `math_4_1 … math_4_13` | 13 | ☐ |
| Math Ch.5 | `math_5_1 … math_5_7` | 7 | ☐ |
| Math Ch.6 | `math_6_1 … math_6_7` | 7 | ☐ |
| Math Ch.7 | `math_7_1 … math_7_9` | 9 | ☐ |
| Math Ch.8 | `math_8_1 … math_8_9` | 9 | ☐ |
| Math Ch.9 | `math_9_1 … math_9_8` | 8 | ☐ |

### Science (61 concepts, 7 chapters)

| Chapter | Concepts | Count | Done |
|---|---|---|---|
| Science Ch.1 | `science_1_1` | 1 | ☐ |
| Science Ch.3 | `science_3_1 … science_3_10` | 10 | ☐ |
| Science Ch.4 | `science_4_1 … science_4_10` | 10 | ☐ |
| Science Ch.5 | `science_5_1 … science_5_10` | 10 | ☐ |
| Science Ch.6 | `science_6_1 … science_6_10` | 10 | ☐ |
| Science Ch.7 | `science_7_1 … science_7_10` | 10 | ☐ |
| Science Ch.8 | `science_8_1 … science_8_10` | 10 | ☐ |

---

## What "authoring a guide" involves (per concept)

1. **`<concept>.guide.json`** in `app/src/main/assets/sim_guides/` — mission line, `coach{}` (whenWrong /
   whenStuck / whenCorrect / whenDeviate banks), and a **`practice{}`** block (the practice rounds are
   what unlock the per-round answer-glow + worked feedback; a sim with no `practice{}` never enters the
   guided phase).
2. **DOM contract** — the injected reader (`SimulationInteractionScript.reportMathProblem` / guide
   structure) must find the sim's `.mission` / option selectors. New sims with different markup may need
   selector tuning. See `docs/EDU_ROUND_CONTRACT.md`.
3. **Math only — solver support.** The per-round glow + number-specific feedback comes from
   `MathCoachSolver`, which today only covers **Chapter-1 math problem types** (round / compare / ratio /
   comma / pattern / speed / build). Each new math chapter that introduces a *new* problem type needs a
   solver rule + `MathCoachSolverTest` case. Chapters reusing Ch.1 types are guide-only.
4. **Kannada** — `_kn` sims share the concept's guide (guide text is language-resolved), so no separate
   KN guide file is needed. Verify the KN sim's DOM matches.
5. **Verify on device** in V4 (default): confirm `CoachBuild: v4 one-clock …` in logcat, then glow +
   text + voice move and reset together each round.

## Suggested order

1. **Math Ch.2–4** (14 + 13 + 10 = biggest math blocks, next in sequence after the covered Ch.1).
2. **Science Ch.3–4** (next after the covered Ch.2).
3. Remaining math (5–9) and science (5–8).
4. `science_1_1` is a one-off (Science Ch.1 has only 1 sim) — quick win, do anytime.

## Method-only caveat
Combinatorial sims (like Ch.1's `1_1/1_3/1_7`) have no single answer to glow — they stay hints-only by
design. Flag any such sims per chapter so authors don't chase an answer-glow that can't exist.

## Reference docs
- `docs/CURSOR_COACH_HANDOFF.md` — coach architecture (V1–V4), math solver, build/verify steps.
- `docs/EDU_ROUND_CONTRACT.md` — the sim→coach DOM/publish contract.
- `docs/COACH_CONTINUOUS_GLOW_PLAN.md` — glow behaviour.
- `coach-sim/coach_simulator.html` — the approved coach feel/flow sandbox.
