# Cursor: commit — Science coach guides (English) + Ch.1 Kannada

**Uncommitted, assets only. Additive — no existing files touched.** These are authored coach `guide.json`
files that unlock the guided (V4) coach on the Science simulations.

## What was authored this session (61 new files)

`app/src/main/assets/sim_guides/`

- **Science English — 60 guides** (10 per chapter): `science_3_*`, `science_4_*`, `science_5_*`,
  `science_6_*`, `science_7_*`, `science_8_*`.
- **Science Ch.1 Kannada — 1 guide**: `science_1_1_kn.guide.json` (Ch.1's only sim is Kannada-only).

Coverage after this: **Science EN complete for Ch.2–8** (Ch.2 was already present = 70 EN guides total).
Ch.1 has no English sim.

Chapters/topics:
- Ch.3 Electricity & circuits · Ch.4 Metals vs non-metals · Ch.5 Physical & chemical changes ·
  Ch.6 Growing up / adolescence · Ch.7 Heat transfer & water cycle · Ch.8 Time & motion (speed).

## Schema
Matches the existing `science_2_*.guide.json`: `simId`, `lang`, `title`, `steps[]` (each with `text` and an
optional `target` = control label to glow), and `coach{}` (`mission`, `whenStuck[]`, `whenWrong[]`,
`whenCorrect[]`, `done`). Targets were pulled from each sim's real controls (e.g. "🔨 Hammer the Material",
"Burn Mg", "Light Candle", "Start Race").

## ⚠️ Important — Kannada sims need their own `_kn.guide.json`
`SimGuideRepository` resolves the guide by **exact sim filename**: `science_4_1.html → science_4_1.guide.json`,
and `science_4_1_kn.html → science_4_1_kn.guide.json` (no `_kn` stripping). So:
- The 60 English guides serve **only the English sims**.
- Every Kannada sim (`science_X_N_kn.html`) still has **no coach** until a matching `science_X_N_kn.guide.json`
  (Kannada text) is authored. That's ~70 more files (Ch.2–8 KN) — the pending Kannada pass.

## Verify
- All 61 files JSON-valid; `simId` matches filename; all `coach{}` keys present (checked).
- Content review recommended before ship, especially the sensitive/observational sims:
  **`science_6_5` (menstruation)**, `6_3`, `6_4` (puberty) — kept factual/respectful; and the free-form
  sims (`5_3`, `5_4`, `5_9`, `7_x` observational) guide rather than glow.
- On device: open a Science sim in V4, confirm the mission + steps show and the `target` controls glow.

## Suggested commit
```
Add English coach guides for Science chapters 3–8 (+ Ch.1 Kannada)

app/src/main/assets/sim_guides/science_3_*.guide.json
app/src/main/assets/sim_guides/science_4_*.guide.json
app/src/main/assets/sim_guides/science_5_*.guide.json
app/src/main/assets/sim_guides/science_6_*.guide.json
app/src/main/assets/sim_guides/science_7_*.guide.json
app/src/main/assets/sim_guides/science_8_*.guide.json
app/src/main/assets/sim_guides/science_1_1_kn.guide.json
```

## Still pending (see docs/COACH_COVERAGE_CHECKLIST.md)
- Science **Kannada** guides (Ch.2–8, ~70 `_kn` files).
- **Math** guides entirely (Ch.2–9), EN + KN — plus `MathCoachSolver` extensions for new problem types.
