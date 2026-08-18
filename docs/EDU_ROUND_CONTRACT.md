# `window.__eduRound` — the sim → coach contract (V4)

The V4 "one-clock" coach can be driven **directly by the simulation** instead of scraping its DOM.
When a sim sets `window.__eduRound`, the coach renders it verbatim — glow + on-screen line + voice,
all from that one object, on its own ~300ms tick. Nothing to call, no events to wire.

This is the production path: the sim already knows its own answer, so publishing it is
**correct by construction** and removes all the per-sim DOM guesswork.

> **It's incremental.** If a sim does NOT set `window.__eduRound`, the coach falls back to its existing
> DOM scraping (math + science + expression handlers). So you can add the hook one sim at a time;
> nothing breaks in the meantime.

---

## The object

```js
window.__eduRound = {
  line:      "Tap the biggest button that still fits",  // REQUIRED — shown in the coach bar
  voice:     "Add one thousand",                         // optional — spoken (defaults to `line`)
  glow:      "#btn-1000",                                // optional — CSS selector (or element) → AMBER "tap this"
  submit:    "#lockBtn",                                 // optional — CSS selector → GREEN "now submit"
  input:     "#digitBox",                                // optional — CSS selector of an input → BLUE "type here"
  inputHint: 4,                                          // optional — placeholder shown in that input while empty
  key:       "r5-add-1000",                              // optional — STABLE id; the voice speaks once per key change
  glowKind:  "hint",                                     // optional — "hint" (amber, default) or "answer" (red)
};
```

To clear the coach (e.g. a pure "explore" screen with no guidance):

```js
window.__eduRound = null;
```

### Rules of thumb
- **Set it whenever the state changes** — a new round loads, the learner selects an option, the running
  total changes, a result appears. Just reassign the object.
- **`glow`/`submit`/`input`** are CSS selectors resolved with `document.querySelector` (or pass a DOM
  element directly). `glow` = amber (the thing to tap), `submit` = green (confirm/check/lock), `input`
  = blue (a box to type in, with optional `inputHint` placeholder).
- **`key` controls the voice.** The coach speaks `voice` once each time `key` changes. For anything with
  a rapidly-changing number (a running total, a countdown), key on the *move* — not the number — so the
  voice doesn't stutter. Example: while building, `key:"add1000"` stays constant across taps → the voice
  says "Add one thousand" once, even though `line` updates the total every tap.
- **Results/feedback:** just set `line` (and `voice`) to the feedback and drop `glow` — e.g.
  `window.__eduRound = { line:"Correct! 3 lakh is bigger.", key:"result-r5" }`.

---

## Copy-paste examples

**MCQ (compare / rounding / ratio / pattern):**
```js
// after picking / building the round:
window.__eduRound = {
  line:  '3 lakh is bigger — tap "<"',
  glow:  'button[data-v="<"]',
  submit:'#checkBtn',            // glows green once a choice is made
  key:   'round-' + roundNo
};
```

**Pattern with a digit-count input:**
```js
window.__eduRound = {
  line:  'Next term is 7777 (4 digits)',
  glow:  '.opt[data-v="7777"]',
  input: '#digitCount', inputHint: 4,
  key:   'round-' + roundNo
};
```

**Build / place-value (stateful — call on every tap):**
```js
function updateCoach(){
  if (current === target) {
    window.__eduRound = { line:'You matched the target — tap Lock Build', submit:'#lockBtn', key:'lock' };
  } else if (current > target) {
    window.__eduRound = { line:'Over the target — tap Reset', glow:'#resetBtn', key:'reset' };
  } else {
    var fit = biggestButtonThatFits();               // your existing logic
    window.__eduRound = {
      line:  'Add +' + fmt(fit) + ' (' + fmt(current) + ' of ' + fmt(target) + ')',
      voice: 'Add ' + fit,                            // comma-free number reads cleanly
      glow:  '#btn-' + fit,
      key:   'add-' + fit                             // stable across taps at the same place value
    };
  }
}
```

**Science (acid/base classification):**
```js
window.__eduRound = {
  line:  'Lemon is an acid — tap Dip Papers to test it',
  voice: 'Lemon is an acid',
  glow:  '#dipBtn',
  key:   'lemon'
};
```

**Expression evaluator:**
```js
window.__eduRound = {
  line:  '39 − 2×6 + 11 → 3 terms, value = 38',
  voice: '3 terms, value 38',
  key:   'expr-' + exprId
};
```

---

## Why this beats scraping
- **Correct by construction** — the sim publishes the answer it already computes; the coach never
  guesses selectors, parses "Current:" text, or matches option labels.
- **Every chapter, for free** — new sims/chapters need only these 4-ish lines; no coach changes and
  nothing for anyone to reverse-engineer.
- **Same one-clock guarantee** — glow, text, and voice still all come from one object on one tick, so
  they can never drift out of sync.

Consumer side is already shipped in V4 (`SimulationInteractionScript.kt` → `renderPublished`); the coach
prefers `window.__eduRound` when present and falls back to scraping otherwise. Verified live: setting
the object glows the selector and drives the bar.
