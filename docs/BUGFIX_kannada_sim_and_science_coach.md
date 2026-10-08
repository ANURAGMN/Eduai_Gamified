# Bugfix — Kannada English sim + Science EN coach missing (Ch.3+)

**Date:** 2026-08-26  
**Reporter:** Live Play Store (`v1.0.11` / versionCode `13`)  
**Next ship:** `v1.0.12` / versionCode `14` (bumped in `app/build.gradle.kts`)  
**Compile:** `:app:compileDebugKotlin` succeeded after these changes

---

## Live fixes applied — 2026-08-26 (no Kotlin, no new KN HTML)

Shipped to the live field **without a Play update**, via Pages + Firestore only:

- **Science EN coach (Ch.5–8).** In `EduAI_app` (`main` / GitHub Pages): hosted ~154 `.guide.json` files
  next to the sims (they had never shipped before), and added `edu-coach.js` to **all** Science EN Ch.5–8
  HTML pages. Older 1.0.11 installs can now get the coach over the network — no app update.
- **Kannada sim URLs.** In Firestore `Concept`: filled the last empty `simulation_url_kannada` fields
  **only where a `*_kn.html` already exists** (5 Science concepts). Math Ch.1–4 were already set; Math Ch.5+
  left alone (no KN HTML exists to point at). Script: `EduAI_app/scripts/set-kannada-sim-urls.js` (now reads
  the KN file set live from `Simulations/` and maps Math `*_new.html` ↔ `*_kn.html`).

**Deferred to v1.0.12 / later (needs code or new content):**
- **Agent path** may still serve the English `html_url` until the 1.0.12 Kotlin reconcile (or an agent-API
  fix). This is the one part of Bug 1 that data/hosting cannot cover — see `BUGFIX_no_kotlin_feasibility.md`.
- **Math Ch.5+ Kannada** needs new `*_kn.html` sims authored before its KN URLs / coach can exist.

**QA for this pass:** EN Science Ch.5–8 coach appears; KN Science and KN Math load Kannada where a KN URL
exists (force a concept sync if the catalog looks stale). Bundling the guides offline + the agent reconcile
land with v1.0.12.

### Follow-up (2026-08-26, later) — coach was still silent on Ch.5–8; root cause + fix

The first pass added the `<script src="edu-coach.js">` **include** to Ch.5–8 but nothing **published**
`window.__eduRound`. In V4 the coach line only appears when the page publishes a round (Ch.2–4 do; Ch.5–8
published 0/10), so the coach stayed silent (reported as "no coach for Heat / Ch.7"). A script tag with no
publisher is inert — this should have been caught in QA before shipping.

Fix (still no Kotlin, live): added a shared **`Simulations/guide-coach.js`** that reads each page's hosted
`.guide.json` and drives the coach (`mission` on load → one `step` per interaction → `done`), standing down
if the page publishes its own round. Wired into all **80** Ch.5–8 pages:
- EN Ch.5–8 (40): added `guide-coach.js` after the existing `edu-coach.js`.
- KN Ch.5–8 (40): added **both** `edu-coach.js` and `guide-coach.js` (they previously had neither), so KN
  Ch.5–8 get the coach too.

Verified: 80/80 pages reference `guide-coach.js`, every guide has `mission` + `steps`, and the load→steps→done
sequence renders in EN and KN. Ch.2–4 are untouched (they publish their own rounds; `guide-coach.js` defers).
Deploy: commit `Simulations/guide-coach.js` + the 80 edited HTML pages and push to Pages. Still pending: Math
coach (only Ch.1 guides authored) and glow-targeted rounds (guide-coach is text+voice, no per-control glow).

---

## What was reported

1. **Kannada language → English simulation HTML, Kannada voice**  
   With app language set to Kannada, Math and Science simulations showed the English interactive page, while TTS / teacher voice spoke Kannada.

2. **Science English coach missing from Chapter 3 onwards**  
   Chapter 2 Science (EN) still had the floating coach; from Chapter 3 up, no coach appeared.

---

## Bug 1 — Kannada shows English sim + Kannada voice

### Why it happened (two layers)

| Layer | What goes wrong |
|--------|------------------|
| **Client (app)** | Simulation **agent** sessions trusted the API’s non-blank `html_url`. That URL is often the **English** HTML even when the session was started with `language=kannada`. Catalog Kannada URLs (`simulation_url_kannada` / `simulationUrlKannada`) were only applied when `html_url` was **blank**. TTS independently follows app language → Kannada voice over English HTML. |
| **Data (Firestore)** | Many concepts have empty / missing `simulation_url_kannada`, so even correct client routing falls back to English. Kannada HTML files often exist on GitHub Pages (`*_kn.html`), but the catalog field was never filled. |

Direct WebView opens (concept list) already preferred `simulationUrlKannada` when set; the agent path and several fallbacks did not.

### Why we fixed it in the app (not only Firestore)

Firestore-only fill fixes installs after sync **if** the client prefers KN over a non-blank English agent URL. Without the client change, a filled catalog still loses to the agent’s English `html_url`. Both layers are required for a durable fix.

### What we changed

#### New helper — `SimulationLanguageUrl`
`app/src/main/java/com/ncert7/aitutorandlab/utils/SimulationLanguageUrl.kt`

Central rules for Kannada:

1. Use explicit Kannada catalog URL if usable  
2. Else derive a twin: `science_3_1.html` → `science_3_1_kn.html` (keeps query/fragment)  
3. Else optionally fall back to English (so plans / lists don’t go empty)

Also: `preferKannadaOverAgentUrl(...)` — **replace** a non-blank English agent `html_url` when a better KN URL exists.

Related pure helper (same intent, for domain/tests):  
`app/src/main/java/com/ncert7/aitutorandlab/domain/simulation/SimulationUrlResolver.kt`

#### Call sites wired

| File | Change | Why |
|------|--------|-----|
| `SimulationAgentViewModel.kt` | `applyConceptHtmlFallback` no longer returns early on non-blank `html_url`; always reconciles with catalog + language. Also applied on **resume**. | Agent was the main path for “EN HTML + KN voice”. Resume previously skipped fallback entirely. |
| `ConceptSimulationViewModel.kt` | `simRoute` / `getSelectedSimulationUrl` use `SimulationLanguageUrl.resolve` | Plan/next-sim and ad-check init must not silently prefer English when KN twin/catalog exists. |
| `PlanTrialMaterializer.kt` | `resolvedSimulationUrl` uses the same resolver | Exam-plan trial days were materializing English URLs for Kannada learners. |

### What still needs data / hosting

- Populate Firestore `simulation_url_kannada` where KN HTML exists (especially Math naming: `math_*_new.html` ↔ `math_*_kn.html` — do not naïvely insert `_kn` without normalizing `_new`).  
- Confirm Math Ch.5–9 (and any sim with no KN twin) stay English on purpose rather than 404.

### How to verify

1. App language = Kannada.  
2. Open Science + Math sims via agent and via concept/WebView.  
3. Confirm HTML UI is Kannada (labels/copy), voice remains Kannada.  
4. Logcat: language-reconcile / replace-html_url messages when agent returned English.

---

## Bug 2 — Science English coach missing from Chapter 3+

### How the V4 (“one-clock”) coach works

Default mode is **ONE_CLOCK (V4)**:

- Floating coach UI shows only when `coachV4Active` **and** `v4Line` is non-blank.  
- `v4Line` normally comes from the **page** via `edu-coach.js` → `window.__eduRound` / bridge `coachText`.  
- Bundled `assets/sim_guides/*.guide.json` steps are **not** walked in V4 (steps list is emptied for ONE_CLOCK). Guides still matter for mission text and as a fallback seed.

So: **guide JSON alone ≠ visible coach** unless the page publishes lines **or** the app seeds a line from the guide.

### Why Chapter 3+ looked broken on live 1.0.11

| Factor | Detail |
|--------|--------|
| **APK assets** | Ship commit for 1.0.11 (`f7de743` era) bundled only ~10 Science guides (mostly Ch.2). Ch.3–8 EN guides landed later in tip (`8682d5e` / `3b585da`) and were **not** in the Play APK. |
| **Hosted HTML (`EduAI_app` / GitHub Pages)** | Live scan of `science_*_*.html`: **Ch.2–3** all include `edu-coach.js`; **Ch.4** 9/10 (missing `science_4_10`); **Ch.5–8** **0/40** have `edu-coach.js`. Without page coach, `v4Line` stays blank → floating coach never appears. |
| **User wording “from Ch.3”** | Ch.3–4 *should* get page coach if Pages + unlock work; Ch.5–8 cannot without page wiring or an app fallback. Missing guides in 1.0.11 + silent pages explain “no coach” for most of Ch.3+. |

### Why we fixed it in the app

Waiting only on Pages authoring leaves Ch.5–8 dead. Tip already has full Science EN/KN guide JSON. Seeding the floating coach from the guide when the page stays silent restores a visible coach after unlock, without blocking on `edu-coach.js` for every HTML file.

### What we changed

#### `ConceptSimulationViewer.kt`

After V4 unlock + page ready, wait ~1.8s. If `v4Line` is still blank, seed from:

1. Guide `coach.mission`, else  
2. First guide step `text`

Ch.2–4 that already publish via `edu-coach.js` keep live lines (they usually arrive before the delay).

### What still needs hosting (full glow / voice sync)

App fallback shows **text** coach; full glow + per-round voice still needs page-side wiring:

1. In **EduAI_app** Pages repo, add the same pattern as Ch.2–4:  
   `<script src="edu-coach.js"></script>` + `__eduRound` (or equivalent) on `science_5_*`–`science_8_*` and `science_4_10`.  
2. Optionally host `*.guide.json` next to sims so older 1.0.11 installs can network-fetch guides (loader already supports that fallback when assets are missing).  
3. Ship **v1.0.12** from tip so Ch.3–8 guides are **bundled** offline.

### How to verify

1. English, Science Ch.2 — coach still works (page-driven).  
2. English, Science Ch.3–4 — coach line appears (page or seed).  
3. English, Science Ch.5–8 — after ~2s, floating coach shows mission / first step from bundled guide (on a build that includes those assets).  
4. Confirm 1.0.11 without new APK still won’t have Ch.3–8 **bundled** guides until either network-hosted JSON or Play update.

---

## Version / release

| Field | Before (live) | After (this work) |
|-------|----------------|-------------------|
| `versionName` | `1.0.11` | `1.0.12` |
| `versionCode` | `13` | `14` |

File: `app/build.gradle.kts`

---

## Files touched (this fix set)

| Path | Role |
|------|------|
| `app/build.gradle.kts` | Version bump 1.0.12 / 14 |
| `utils/SimulationLanguageUrl.kt` | **New** — language-aware sim URL selection |
| `domain/simulation/SimulationUrlResolver.kt` | **New** — pure resolver (tests / domain) |
| `simulation_agent/.../SimulationAgentViewModel.kt` | Prefer KN HTML over agent English `html_url` (start + resume) |
| `conceptscreen/.../ConceptSimulationViewModel.kt` | Route / selected URL via resolver |
| `domain/examplan/PlanTrialMaterializer.kt` | Plan trial URLs via resolver |
| `conceptscreen/.../ConceptSimulationViewer.kt` | V4 coach seed from guide when page silent |
| `docs/BUGFIX_kannada_sim_and_science_coach.md` | This document |

*(Working tree may also include unrelated Avatar Studio edits from earlier work — not part of these two bugs.)*

---

## Done without Kotlin (2026-08-26) — live for existing installs

Pushed to **EduAI_app** `main` (`3175530`) and GitHub Pages:

1. **Hosted all `.guide.json`** next to sims (were local-only / untracked before → network fallback 404).  
2. **Wired `edu-coach.js`** into Science EN **Ch.5–8** HTML (40 files).  
3. **Firestore:** filled the remaining empty `simulation_url_kannada` gaps where `*_kn.html` already exists (5 Science concepts). Math Ch.1–4 KN URLs were already populated; Math Ch.5+ still have **no** KN HTML (not inventable here).

### Still needs app/backend (not done in this pass)
- Simulation **agent** may still return English `html_url` until Kotlin reconcile ships or the agent API is fixed.  
- **Concept / WebView** Kannada path should pick up Firestore KN URLs after sync.

## Rollout checklist

1. **QA now (no Play update):** English Science Ch.5–8 coach; Kannada Science/Math where KN URL exists (force concept sync / clear app data if stale).  
2. **Play (optional hardening):** Build & upload **1.0.12** (client URL reconcile + coach seed + bundled guides).  
3. **Invent later:** Math Ch.5–9 `*_kn.html` (and any Science gaps) — out of scope for “no invent”.

---

## Root cause in one line each

1. **Kannada:** TTS followed app language; **HTML followed agent/catalog English** because a non-blank English `html_url` was never overridden.  
2. **Coach Ch.3+:** Live APK lacked Ch.3–8 guides; V4 UI needs page `edu-coach.js` (absent Ch.5–8) or a guide-seeded line — without either, the floating coach never appears.
