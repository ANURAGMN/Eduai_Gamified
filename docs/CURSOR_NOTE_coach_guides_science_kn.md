# Cursor: commit — Science coach guides (Kannada) + status docs

**Uncommitted, assets + docs only. Additive — no existing files changed.** Follows the English batch
already on `gamified` (`8682d5e`).

## What to commit (73 files)

`app/src/main/assets/sim_guides/`
- **70 Science Kannada guides** — `science_2_*_kn.guide.json … science_8_*_kn.guide.json` (10 per chapter,
  Ch.2–8). (`science_1_1_kn.guide.json` was already committed with the EN batch.)

`docs/`
- `COACH_GUIDES_STATUS.md` — implemented vs pending status board (new).
- `COACH_GUIDES_SCIENCE_WRITEUPS.md` — authored English write-ups (source for translation) (new).
- `COACH_COVERAGE_CHECKLIST.md` — updated: Science EN **and** KN marked complete; Math-only remaining.

## How resolution works (why KN needs its own files)
`SimGuideRepository` resolves the guide by **exact sim filename**: `science_4_1_kn.html →
science_4_1_kn.guide.json`. So KN sims don't share the EN guide — these 70 files are what turn the coach
on for the Kannada Science sims.

## Verify
- All 70 KN files JSON-valid; `lang: "kn"`; `simId` matches filename; `coach` banks present (checked here).
- Kannada is a faithful translation of the EN write-ups. Glow `target`s use the **Kannada** control labels
  where the sims exposed them (e.g. `ಸಿಂಪಡಿಸಿ`, `ಮೇಣ ಬೆಳಗಿಸಿ`, `ಓಟ ಪ್ರಾರಂಭಿಸಿ`, and the sim-native
  `Burn Mg`/`Test pH`); omitted where controls weren't extractable (coach still runs on steps + banks).
- A few KN guides have fewer steps than their EN counterpart (`science_2_2/2_4/2_5/2_6`, `science_4_1`) —
  matches the KN sim flow; still valid.

## Suggested commit
```
Add Kannada coach guides for Science chapters 2–8 + coverage docs

app/src/main/assets/sim_guides/science_2_*_kn.guide.json
app/src/main/assets/sim_guides/science_3_*_kn.guide.json
app/src/main/assets/sim_guides/science_4_*_kn.guide.json
app/src/main/assets/sim_guides/science_5_*_kn.guide.json
app/src/main/assets/sim_guides/science_6_*_kn.guide.json
app/src/main/assets/sim_guides/science_7_*_kn.guide.json
app/src/main/assets/sim_guides/science_8_*_kn.guide.json
docs/COACH_GUIDES_STATUS.md
docs/COACH_GUIDES_SCIENCE_WRITEUPS.md
docs/COACH_COVERAGE_CHECKLIST.md
```

## Before trusting as ship-ready (not code)
1. **Native-Kannada review** — esp. Ch.6 (adolescence: puberty, menstruation) kept factual/respectful.
2. **On-device V4 pass** — confirm mission + steps show and `target` controls glow; free-form sims guide by text.

## Still pending (Math)
- Math **EN** Ch.2–9 (77 concepts) — plus `MathCoachSolver` rules + tests for any new problem types.
- Math **KN** — Ch.1–4 sims exist (~20); none have guides.
- **Confirm Ch.1 Math sim naming:** guides are `math_1_1_new … math_1_5_new`; plain `math_1_1.html …
  math_1_5.html` have no exact-match guide, so the coach fires only if the app opens the `*_new` sims.
