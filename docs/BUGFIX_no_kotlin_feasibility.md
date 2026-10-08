# Can these be fixed without touching Kotlin? — feasibility analysis

**Date:** 2026-08-26 · Companion to `BUGFIX_kannada_sim_and_science_coach.md`.
**Question asked:** can Bug 1 (Kannada sim shows English) and Bug 2 (Science coach missing Ch.3+) be
fixed with **data / hosting only**, no app (Kotlin) change and no Play update?

**Short answer:**

| Bug | Fixable without Kotlin? | How |
|---|---|---|
| **Bug 2 — Science coach (all chapters)** | **Yes** | Add `edu-coach.js` + coach wiring to the Ch.5–8 pages on GitHub Pages (Ch.2–4 already have it). Pure content. |
| **Bug 1 — Kannada Science/Math sim** | **No** | The app hands the viewer the **English** URL directly on every open path. Filling Firestore is necessary but not sufficient — the client must be told to prefer the Kannada URL (the reconcile Cursor added in Kotlin), or the agent backend must return Kannada URLs. |

This corrects an earlier claim that "hosting the guide JSON" fixes Bug 2 and that "filling Firestore"
fixes Bug 1 on the WebView path. The code below shows why both were wrong.

---

## Bug 2 — coach: **yes, no Kotlin (Pages content)**

### Why guide JSON alone is not enough
The coach runs in **V4 / ONE_CLOCK**, which is the forced default:

- `ConceptSimulationViewer.kt` line ~432: `val coachMode = SimCoachMode.ONE_CLOCK`.
- Line ~473: `SimCoachMode.ONE_CLOCK -> emptyList()` — the bundled guide **steps are dropped** in V4.
- Line ~272 comment: *"ONE_CLOCK (V4/V5): the page owns coaching via edu-coach.js / __eduRound."*

So the visible coach line comes from the **page's** `edu-coach.js`, not from `sim_guides/*.guide.json`.
Hosting the guide JSON only helps mission/activation and older-install fallbacks — it does **not** make
the V4 coach line appear on a page that has no `edu-coach.js`.

### The real gap (verified against the hosted pages, 2026-08-26)
| Set | `edu-coach.js` present |
|---|---|
| Science Ch.2 EN | 10/10 |
| Science Ch.3 EN | 10/10 |
| Science Ch.4 EN | 10/10 (`science_4_10` was the one gap; already patched) |
| **Science Ch.5–8 EN** | **0/40** |
| Science Ch.5–8 KN | 0/40 |

That 0/40 is exactly the "no coach from Ch.3+" the user sees (Ch.3–4 work, Ch.5–8 dead).

### No-Kotlin fix
On the **EduAI_app** Pages repo, wire the Ch.5–8 (EN + KN) Science pages the same way as Ch.2–4:
`<script src="edu-coach.js"></script>` + the per-sim `__eduRound` / coach-line publish. The coach content
is already authored in `sim_guides/science_5_*..science_8_*` (EN) and `_kn` (KN) — reuse it to drive the
page. This restores the full coach (glow + voice), no app change.

Cursor's alternative (app-side guide-seed) is a Kotlin fallback that shows a **text-only** coach even on
un-wired pages; not required if the pages are wired.

---

## Bug 1 — Kannada sim: **no, needs the client change**

A Science/Math concept card exposes **two** open buttons (`ConceptCard.kt` ~309–390), and **both** feed the
viewer the English URL:

### Path A — "Simulation" button (WebView)
```kotlin
// ConceptCard.kt
val validSimulationUrl = concept.simulationUrl?.takeIf { it.isNotBlank() && it != "null" }  // ENGLISH
...
onSimulationClick(concept.name, validSimulationUrl, concept.id)                              // passes English
```
```kotlin
// ConceptSimulationViewModel.initializeSimulationWithAdCheck (line ~453)
if (simulationUrl != null && simulationTitle != null) {
    _simulationUrl.value = simulationUrl   // uses the passed ENGLISH url as-is
    return                                 // ← language / Kannada catalog never consulted
}
```
The Kannada-aware branch (`getSelectedSimulationUrl`, line ~476) only runs when **no** URL is passed. The
button always passes one, so `simulation_url_kannada` in Firestore is never looked at on this path.

### Path B — "Agent" button
`SimulationAgentViewModel` (at ship commit `f7de743`, lines ~453/456) set the sim URL to
`response.simulation.htmlUrl` — the tutor API's URL, which is **English** even for a `language=kannada`
session. No Kannada reconciliation existed.

### Consequence
Filling Firestore `simulation_url_kannada` (data) is **necessary but not sufficient**: on both live paths
the app uses the English URL directly, so the Kannada sim never loads. The durable fix requires the client
to prefer the Kannada URL — i.e. the Kotlin reconcile Cursor added:

- `SimulationLanguageUrl` / `SimulationUrlResolver` used by `simRoute`, `getSelectedSimulationUrl`,
  `PlanTrialMaterializer.resolvedSimulationUrl`, and
- `preferKannadaOverAgentUrl(...)` to override a non-blank English agent `html_url`.

The only non-Kotlin way to fix Path B specifically would be a **server-side** change so the agent API
returns the Kannada `html_url` for Kannada sessions — outside this repo, and it would not fix Path A.

---

## What each fix actually requires

| Item | Kotlin? | Play update? | Where |
|---|---|---|---|
| Bug 2 — coach Ch.5–8 | No | No | Add `edu-coach.js` + round wiring to Ch.5–8 pages (EduAI_app Pages) |
| Bug 2 — bundle guides offline | No (assets only) | Yes (new build) | Ship v1.0.12 from tip |
| Bug 1 — Firestore KN URLs | No | No | `scripts/set-kannada-sim-urls.js` (necessary, not sufficient) |
| Bug 1 — prefer KN in app | **Yes** | Yes (new build) | `SimulationLanguageUrl` + 3 call sites + agent reconcile |
| Bug 1 — agent returns KN url | No (server) | No | Agent backend (not in this repo) |

## Bottom line
- **Science coach (Bug 2): fully fixable with zero Kotlin** — wire `edu-coach.js` into the Ch.5–8 pages.
- **Kannada Science/Math sim (Bug 1): not fixable without Kotlin** (or an agent-backend change) — the app
  passes the English URL directly on every open path, so data alone can't redirect it.

### Correction to earlier note
`BUGFIX_kannada_sim_and_science_coach.md` (first version) implied hosting `guide.json` fixes the Ch.5–8
coach and that filling Firestore fixes the WebView path. Both are wrong for the reasons above: V4 ignores
guide steps (needs page `edu-coach.js`), and the WebView "external" init branch uses the passed English URL
without consulting the Kannada catalog.
