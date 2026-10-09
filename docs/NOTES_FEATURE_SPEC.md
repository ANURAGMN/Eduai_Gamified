# Notes feature — build spec (Reader + Sections + PDF viewer)

**Date:** 2026-10-08 · **Status:** spec for implementation (Claude Code). **Source of truth:** per-chapter
**PDF hosted on GitHub Pages** (like the sims), optionally paired with a structured `notes.json`.
Chosen designs: **A (long-scroll reader)**, **B (sectioned tabs)**, **D (PDF viewer + download)** — mockups in
`notes_feature_designs.html`.

## 1. The core decision (read first)
A flat PDF gives you **D** for free. **A and B** render native callouts, reflow on phones, dark-mode, and
translate to Kannada — a PDF can't do any of those. So A/B need the notes as **structured data**.

Mirror the coach architecture the team already knows:

| Coach (existing) | Notes (this feature) |
|---|---|
| `Simulations/science_10_1.html` | `notes/science_10.pdf` (render + download → **D**) |
| `Simulations/science_10_1.guide.json` | `notes/science_10.notes.json` (structured → **A/B**) |
| `_kn.html` / `_kn.guide.json` | `science_10_kn.pdf` / `science_10_kn.notes.json` |
| `SimGuideRepository` (asset → network) | `NotesRepository` (same resolution order) |

**Phasing:** ship **D** first (PDF only, every chapter immediately). Light up **A/B** per chapter as its
`notes.json` is authored. A chapter with a PDF but no JSON simply shows D; the UI degrades gracefully.

## 2. Hosting layout (EduAI_app → GitHub Pages)
```
EduAI_app/Notes/
  science_10.pdf            science_10_kn.pdf
  science_10.notes.json     science_10_kn.notes.json   # optional; enables A/B
  math_3.pdf  …
  notes-manifest.json       # which chapters have notes, and in which languages/formats
```
Served at `https://anuragmn.github.io/EduAI_app/Notes/…`. Content changes ship by **git push to Pages**, no
Play update — same as coach guides.

`notes-manifest.json` (app reads this to know what exists):
```json
{
  "science_10": { "title":"Photosynthesis", "pdf":true, "pdf_kn":true,
                  "structured":true, "structured_kn":false }
}
```

## 3. `notes.json` schema (drives A + B)
Keep it close to `guide.json` so authoring/validation tooling is familiar. One file per chapter+language.
```json
{
  "chapterId": "science_10",
  "lang": "en",
  "title": "Photosynthesis",
  "subtitle": "Class 7 Science · Life Processes in Plants",
  "sections": [
    {
      "id": "overview", "heading": "The Big Question",
      "blocks": [
        { "type": "text", "text": "Green plants make their own food — photosynthesis." },
        { "type": "callout", "kind": "key",     "title": "Key Idea",       "text": "Plants use CO₂ + water with sunlight + chlorophyll to make glucose; O₂ is released." },
        { "type": "equation", "text": "CO₂ + Water → Glucose + O₂", "over": "sunlight", "under": "chlorophyll" },
        { "type": "callout", "kind": "trick",   "title": "Memory Trick",   "text": "S-W-C-C: Sunlight, Water, CO₂, Chlorophyll." },
        { "type": "callout", "kind": "mistake", "title": "Common Mistake", "text": "Roots don't make food; they absorb water & minerals." }
      ]
    },
    { "id": "experiments", "heading": "Key Experiments",
      "blocks": [ { "type":"callout","kind":"experiment","title":"Iodine test","text":"Starch turns blue-black." } ] }
  ],
  "quiz": [
    { "q": "Which gas do plants release in photosynthesis?", "options": ["Carbon dioxide","Oxygen","Nitrogen"], "answer": 1 }
  ]
}
```
- `callout.kind` ∈ `key | trick | mistake | experiment` → maps to the four colored box styles in the mockup.
- `block.type` ∈ `text | callout | equation | image | table | list`. Start with the first three; add
  `image`/`table` later (concept map, Q7 condition table).
- **B (tabs)** = the `sections[]` list; **A (reader)** = all sections concatenated with the progress bar.
- `quiz[]` powers the self-check (shared with design C later if wanted).

## 4. App side (Eduapp, Kotlin/Compose)
- **`NotesRepository`** — resolve by chapter+language, mirroring `SimGuideRepository`:
  bundled asset (optional) → hosted `notes-manifest.json` + `notes.json`/`pdf`. Cache in memory + disk.
- **Language**: pick `_kn` variants when app language is Kannada, falling back to EN (reuse the
  `SimulationLanguageUrl` pattern so behavior matches sims; don't re-parse localized strings).
- **UI** (one screen, format chosen by what's available + a view toggle):
  - **A – Reader**: `LazyColumn` of blocks; callout composable per `kind`; sticky bottom bar with
    **Download PDF** + **Save/bookmark**; top reading-progress.
  - **B – Sections**: `ScrollableTabRow`/chips from `sections[]` + pager; same bottom bar.
  - **D – PDF**: render `notes/<ch>.pdf`. Use Android `PdfRenderer` (no extra dep) or a known-good lib;
    **Download = save the PDF to the device** (FileProvider share/open).
- **Entry point**: a **"Notes"** action on the chapter screen (next to the sim/agent buttons). Also surface
  from concept/plan where it fits.
- **Download/offline**: downloaded PDFs cached locally; a chapter opened once is readable offline (PDF path).
  Structured notes are small JSON — cache them too.
- **Theme**: A/B honor the app's light/dark `EduAiTheme`; D is the PDF (no theming — call that out in UI).

## 5. Analytics (GA4, consistent with current setup)
Log (GA4 only — mirror is off): `notes_open {chapterId, lang, mode:reader|sections|pdf}`,
`notes_download {chapterId, lang}`, `notes_quiz_complete {chapterId, score}`. No Firestore mirror.

## 6. Build phases
1. **D now** — `NotesRepository` (PDF path) + PDF viewer + download + chapter entry point + manifest.
   Ships value for every chapter that has a PDF, EN + KN.
2. **A/B** — add `notes.json` rendering (reader + tabs). Light up per chapter as JSON is authored; UI falls
   back to D when a chapter has only a PDF.
3. **Quiz / revision (design C)** — later, reuses `quiz[]`.

## 7. Open questions (decide before/while building)
- **Who authors `notes.json`?** Hand-author (like guides), or a PDF→JSON extraction script (the PDFs are
  TikZ/LaTeX, so structure is extractable but not trivial). Recommendation: author the handful of priority
  chapters as JSON; everything else rides on D.
- **PDF render lib**: `PdfRenderer` (built-in, zero dep, fine for page-image rendering) vs a richer viewer
  (pinch-zoom/text-select). Start with built-in.
- **Diagrams in A/B**: the PDFs use TikZ figures (equation art, concept map). For A/B, either export those as
  images into `notes/` and reference via `image` blocks, or keep diagram-heavy sections as a "view in PDF"
  link. D always has them.

## 8. Related
- Architecture parallel: coach `SimGuideRepository` + `guide.json` + `check-coach-coverage.js` (consider a
  `check-notes-coverage.js` later). Language: `SimulationLanguageUrl`. See `CLAUDE.md` §7.
- Mockups: `notes_feature_designs.html` (A/B/C/D phone screens).
