# Coach coverage — pending chapters checklist

**Generated:** 2026-08-20 · **Updated:** 2026-08-20 (Science EN authored) · from `Simulations/*.html`
vs authored `app/src/main/assets/sim_guides/*.guide.json`

The guided coach (V4 one-clock) runs off an authored **`<simFile>.guide.json`** per simulation. A sim
with no matching guide falls back to hints-only. `SimGuideRepository` resolves the guide by the **exact
sim filename**, so `science_4_1.html → science_4_1.guide.json` and `science_4_1_kn.html →
science_4_1_kn.guide.json` — **English and Kannada sims each need their own guide file.**

## Summary (English guides)

| | Concepts |
|---|---|
| Total distinct EN sim concepts | **161** |
| Coach done (EN guide authored) | **84** |
| **Pending (EN)** | **77** — Math Ch.2–9 |

**Done (EN):** Math Ch.1 (13) · Science Ch.2 (10, pre-existing) · **Science Ch.3–8 (60, this batch)** ·
Science Ch.1 (1, its only sim is Kannada → `science_1_1_kn`).
(3 Math Ch.1 sims — `1_1/1_3/1_7` — are *method-only*: hints, no answer-glow, by design.)

---

## Per chapter

### Science — English ✅ COMPLETE

| Chapter | Concepts | Count | Done |
|---|---|---|---|
| Science Ch.1 | `science_1_1_kn` (KN-only sim) | 1 | ☑ |
| Science Ch.2 | `science_2_1 … 2_10` (pre-existing) | 10 | ☑ |
| Science Ch.3 | `science_3_1 … 3_10` | 10 | ☑ |
| Science Ch.4 | `science_4_1 … 4_10` | 10 | ☑ |
| Science Ch.5 | `science_5_1 … 5_10` | 10 | ☑ |
| Science Ch.6 | `science_6_1 … 6_10` | 10 | ☑ |
| Science Ch.7 | `science_7_1 … 7_10` | 10 | ☑ |
| Science Ch.8 | `science_8_1 … 8_10` | 10 | ☑ |

### Math — English (77 concepts, pending)

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

### Kannada `_kn` guides — pending (separate files required)

The KN sims have **no coach** until a matching `science_X_N_kn.guide.json` / `math_X_N_kn.guide.json`
(Kannada text) is authored.

| Set | Files | Done |
|---|---|---|
| Science KN — Ch.2–8 | 70 | ☑ (authored 2026-08-20; untracked — commit pending) |
| Science Ch.1 KN | 1 | ☑ (`science_1_1_kn`) |
| Math KN — Ch.1–4 (only these have KN sims) | ~20 | ☐ |

---

## What "authoring a guide" involves (per sim)

1. **`<simFile>.guide.json`** in `app/src/main/assets/sim_guides/` —
   - **Science (this batch):** `simId`, `lang`, `title`, `steps[]` (each `text` + optional glow `target`),
     and `coach{}` (`mission`, `whenStuck[]`, `whenWrong[]`, `whenCorrect[]`, `done`). **No `practice{}`
     needed** — science sims are explore/observe, guided by steps + coach banks.
   - **Math:** additionally benefits from a `practice{}` block, which unlocks the per-round answer-glow +
     number-specific worked feedback via `MathCoachSolver`.
2. **DOM contract** — the injected reader must find the sim's `.mission` / option / control selectors. New
   markup may need selector tuning. See `docs/EDU_ROUND_CONTRACT.md`.
3. **Math only — solver support.** `MathCoachSolver` currently covers **Chapter-1 math problem types**
   (round / compare / ratio / comma / pattern / speed / build). Each new math chapter with a *new* problem
   type needs a solver rule + `MathCoachSolverTest` case. Chapters reusing Ch.1 types are guide-only.
4. **Kannada — separate file.** `_kn` sims resolve to `<simFile>_kn.guide.json`; they do **not** share the
   English guide. Author Kannada `steps`/`coach` text and use the Kannada control labels for `target`s.
5. **Verify on device** in V4 (default): confirm `CoachBuild: v4 one-clock …` in logcat; mission + steps
   show and the `target` controls glow.

## Suggested order (remaining)

Science EN **and** KN are **complete**. Only Math remains:

1. **Math Ch.2–4** EN (biggest blocks, next after covered Ch.1) — with any needed `MathCoachSolver` rules + tests.
2. Remaining **Math Ch.5–9** EN.
3. **Math KN** (Ch.1–4 sims exist) — translate the EN math write-ups.

Also confirm the Ch.1 Math sim naming (`math_1_1_new …` vs plain `math_1_1.html`) so the coach fires on the sims the app actually opens.

## Method-only caveat
Combinatorial math sims (like Ch.1's `1_1/1_3/1_7`) have no single answer to glow — hints-only by design.

## Reference docs
- `docs/COACH_GUIDES_SCIENCE_WRITEUPS.md` — the authored Science write-ups (source for the KN pass).
- `docs/CURSOR_NOTE_coach_guides_science_en.md` — commit note for this batch.
- `docs/CURSOR_COACH_HANDOFF.md` — coach architecture (V1–V4), math solver, build/verify steps.
- `docs/EDU_ROUND_CONTRACT.md` — the sim→coach DOM/publish contract.
- `coach-sim/coach_simulator.html` — the approved coach feel/flow sandbox.
