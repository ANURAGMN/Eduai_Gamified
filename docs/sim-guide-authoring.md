# Simulation guides — authoring & batch generation

Each simulation gets a small **guide JSON** that the app's coach uses to walk a student
through it, step by step. Guides are **pre-generated once** (model + review) and hosted
next to the sims. The app fetches one per sim and, if none exists yet, falls back to an
auto-harvested heuristic — so rollout can be gradual.

## Where to host

Next to the sim HTML, same name, `.guide.json` extension:

```
.../Simulations/science_2_6.html         → .../Simulations/science_2_6.guide.json
.../Simulations/science_2_6_kn.html      → .../Simulations/science_2_6_kn.guide.json
```

Language follows the sim file: the English sim gets an English guide; the `_kn` sim gets a
Kannada guide. (Start with English.)

## Schema

```json
{
  "simId": "science_2_6",
  "lang": "en",
  "title": "Olfactory Indicators",
  "steps": [
    { "text": "One short instruction.", "target": "Exact visible button/label text, or omit for observe steps" }
  ]
}
```

- `text` — one short, kid-friendly sentence.
- `target` — the **exact visible text** of the control the student should tap (the app
  matches it, case-insensitively, to what's on the button/card). **Omit `target`** (or set
  it to `null`) for intro, "observe/smell/notice", and conclusion steps — those show a
  Next button; steps *with* a target require the real tap (no skipping past the action).

### Rules for good guides
- 5–9 steps. Order: **1 intro → repeated (pick an option → trigger the action → observe) →
  1 conclusion.**
- Be **accurate to the experiment's science** — use the sim's own "Key Insight".
- `target` must be a control that actually exists on screen; use its visible label verbatim
  (e.g. `"Mix Together"`, `"Tamarind Water"`, `"Baking Soda"`).
- Conclusion step states the takeaway ("acids keep the smell, bases remove it").

## Example — `science_2_6.guide.json`

```json
{
  "simId": "science_2_6",
  "lang": "en",
  "title": "Olfactory Indicators",
  "steps": [
    { "text": "Onions smell strong — we'll use that smell to tell acids from bases!" },
    { "text": "Start with an acid — tap \"Tamarind Water\".", "target": "Tamarind Water" },
    { "text": "Now tap \"Mix Together\" to mix it with the onion.", "target": "Mix Together" },
    { "text": "Smell it 👃 — the strong onion smell stays. Acids keep the smell." },
    { "text": "Now try a base — tap \"Baking Soda\".", "target": "Baking Soda" },
    { "text": "Tap \"Mix Together\" again.", "target": "Mix Together" },
    { "text": "This time the smell fades — bases neutralise it. That's how you tell them apart!" }
  ]
}
```

## Batch generation prompt

For each sim, gather three inputs (the app already extracts the first two at runtime; for
offline generation, open the sim and read them, or reuse the harvested control labels the
app logs):

1. **Title** — the sim's page title.
2. **Concept text** — the intro copy + the "Key Insight" / takeaway block.
3. **Control labels** — the visible text of every interactive control (buttons, option
   cards, sliders).

Then prompt the model:

> **System:** You write short, accurate, kid-friendly guided walkthroughs for class-7
> science simulations. Output ONLY valid JSON matching this schema:
> `{ "simId": string, "lang": "en", "title": string, "steps": [ { "text": string, "target"?: string } ] }`.
> Each `text` is ONE short sentence. For `target`, use the EXACT visible control label
> provided; omit `target` for intro, observe, and conclusion steps. Follow the real
> pedagogy using the Key Insight. Structure: 1 intro → repeated (pick option → trigger
> action → observe) → 1 conclusion. 5–9 steps total. No prose outside the JSON.
>
> **User:**
> Title: `{title}`
> Key insight / concept: `{concept text}`
> Interactive controls (use these exact labels for target): `{comma-separated control labels}`
> Produce the guide JSON.

Review each output (5 seconds a sim), then host it. Because targets are matched by label,
the same guide keeps working even if the sim's internal ids change.

## How the app consumes it (already implemented)

- `SimGuideRepository.fetchGuide(simUrl)` derives the `.guide.json` URL and loads it.
- `SimGuideBuilder.buildFromDoc(doc, harvestedControls)` maps each step's `target` label to
  the live control's `data-edu-step` index (for highlight + tap-gating).
- If no guide is hosted, the coach uses the auto-harvested heuristic instead.
