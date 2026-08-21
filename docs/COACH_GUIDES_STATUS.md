# Coach guides — implementation status

**Updated:** 2026-08-20 · counts generated from `app/src/main/assets/sim_guides/` vs `Simulations/`

The guided (V4) coach runs off an authored **`<simFile>.guide.json`** per simulation. The loader
(`SimGuideRepository`) resolves by the **exact sim filename**, so English and Kannada sims each need
their own guide: `science_4_1.html → science_4_1.guide.json`, `science_4_1_kn.html →
science_4_1_kn.guide.json`. Each guide has teach `steps` (with optional glow `target`) + `coach` banks
(mission / stuck / wrong / correct / done). A sim with no matching guide falls back to no handholding.

## Headline

- **Science — fully done, both languages.** English Ch.2–8 and Kannada Ch.1–8.
- **Math — barely started.** Only Chapter 1 English exists (pre-existing). All other Math is pending.
- Totals in repo: **83 English guides, 71 Kannada guides.**

---

## ✅ Implemented

### Science (complete)

| Chapter | Topic | English | Kannada |
|---|---|---|---|
| Ch.1 | Light & Shadow (KN-only sim) | — (no EN sim) | ☑ 1/1 |
| Ch.2 | Acids, Bases & Indicators | ☑ 10/10 | ☑ 10/10 |
| Ch.3 | Electricity & Circuits | ☑ 10/10 | ☑ 10/10 |
| Ch.4 | Metals vs Non-metals | ☑ 10/10 | ☑ 10/10 |
| Ch.5 | Physical & Chemical Changes | ☑ 10/10 | ☑ 10/10 |
| Ch.6 | Growing Up / Adolescence | ☑ 10/10 | ☑ 10/10 |
| Ch.7 | Heat Transfer & Water Cycle | ☑ 10/10 | ☑ 10/10 |
| Ch.8 | Time & Motion (Speed) | ☑ 10/10 | ☑ 10/10 |

- **English write-ups documented** in `docs/COACH_GUIDES_SCIENCE_WRITEUPS.md`.
- **Kannada** is a faithful translation of each English write-up; glow `target`s use the actual Kannada
  control labels where the sims exposed them, and are omitted where controls aren't extractable (coach
  still runs on steps + banks).
- Sensitive Ch.6 sims (puberty, menstruation) kept factual and respectful in both languages.
- EN and KN step counts aren't always 1:1 — a few KN guides have fewer steps than their EN counterpart
  (`science_2_2`, `2_4`, `2_5`, `2_6`, `science_4_1`), matching the KN sim's flow. All parse clean and are valid.

### Math (partial — pre-existing)

| Chapter | English | Kannada |
|---|---|---|
| Ch.1 | ◐ 13 guides (see note) | ☐ 0/5 KN sims |

**Ch.1 naming caveat:** the guides are `math_1_1_new … math_1_5_new` and `math_1_6 … math_1_13`. The
plain `math_1_1.html … math_1_5.html` sims have **no exact-match guide**, so — given the exact-filename
rule — the coach only fires on Chapter 1 if the app opens the `*_new` variants of 1_1–1_5. Confirm which
Ch.1 sim files the app actually launches.

3 of the Ch.1 Math sims (`math_1_1/1_3/1_7`) are *method-only* (combinatorial): hints, no answer-glow, by design.

---

## ❌ Not implemented (pending)

### Math — English (77 concepts)

| Chapter | Concepts | Status |
|---|---|---|
| Math Ch.2 | 14 | ☐ |
| Math Ch.3 | 10 | ☐ |
| Math Ch.4 | 13 | ☐ |
| Math Ch.5 | 7 | ☐ |
| Math Ch.6 | 7 | ☐ |
| Math Ch.7 | 9 | ☐ |
| Math Ch.8 | 9 | ☐ |
| Math Ch.9 | 8 | ☐ |

### Math — Kannada (all)

Math KN sims exist only for **Ch.1–4** (about 5 concepts each); none have guides. Ch.5–9 have no KN sims.

### Extra work Math needs (beyond writing guides)

- **`MathCoachSolver` extensions.** The per-round answer-glow + number-specific feedback come from the
  solver, which today only covers **Chapter-1 problem types** (round / compare / ratio / comma / pattern /
  speed / build). Any new math chapter introducing a *new* problem type needs a solver rule + a
  `MathCoachSolverTest` case. Chapters that reuse Ch.1 types are guide-only.
- Math benefits from an optional `practice{}` block in the guide (unlocks the guided practice phase).

---

## ⚠️ Caveats / still to do on what's done

1. **Native-Kannada review.** The Kannada is solid but human-unreviewed. For kid-facing science
   terminology, a native pass before ship is recommended (esp. Ch.6 adolescence content).
2. **On-device V4 verification.** These write-ups don't replace testing on a device: open each sim in V4,
   confirm the mission + steps show and the `target` controls glow. Free-form/observational sims
   (e.g. several in Ch.5/6/7) guide by text rather than glow, by design.
3. **Commit state (corrected).**
   - **Already on `gamified`** (commit `8682d5e`): Science **EN** Ch.3–8 (+ `science_1_1_kn`).
   - **Still untracked:** the **70 Science KN** guides (`science_2_*_kn … science_8_*_kn`) and the
     status/write-up docs. Commit these before treating the status board as ship-ready.
   - All additive assets under `sim_guides/` — no existing files changed.

---

## Reference docs
- `docs/COACH_GUIDES_SCIENCE_WRITEUPS.md` — the authored English write-ups (source for the KN pass).
- `docs/COACH_COVERAGE_CHECKLIST.md` — per-chapter checklist (update: Science now complete).
- `docs/CURSOR_NOTE_coach_guides_science_en.md` — commit note for the English batch.
- `docs/CURSOR_COACH_HANDOFF.md` — coach architecture (V1–V4), math solver, build/verify steps.
