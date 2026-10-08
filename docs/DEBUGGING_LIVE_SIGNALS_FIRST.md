# Incident debugging — live signals first

**One rule:** for anything happening in **production**, *observe before you theorize.* Pull the real signals
first; use the code only to **explain and fix** what those signals show — never to decide what's wrong.

This exists because we got burned twice doing the reverse: reasoning from code (and from the code's own
comments) into a confident-but-wrong verdict, when a 30-second look at the live signal said the opposite.

## The failure mode to avoid

- Reading source and concluding "it must be X" without checking whether X is what's actually happening.
- Trusting a code comment as evidence (e.g. "most 500s are quota") instead of the logs.
- Writing a "ruled out" list from inference. **Only rule something out with a signal that excludes it.**

## Step 0 — before any verdict, pull the signals

| Symptom | Look here FIRST |
|---|---|
| App **crashes** | Play Console → Android vitals → Crashes & ANRs (by version code); Firebase **Crashlytics** stack trace; the rejected release's **Pre-launch report**. |
| **"Server error"** / API failing | Backend **`GET /health`**; **Firestore `errors/eduai_app/logs`**; the actual **HTTP status + response body**; upstream provider dashboards (Gemini/Groq quota). |
| **Feature not showing** (coach, glow, content) | Reproduce on a **real device / the live URL**; `curl -I` the asset for **200**; read the runtime state (does it *render*), not just "the file exists". |
| **Release-only** breakage | It's almost always **R8/minify** — reproduce on the **release** build, check `mapping.txt` / missing keep rules. Debug won't show it. |
| **Notifications / alarms** | Device logs on the actual OEM + Android version; battery/doze settings. |
| **Data looks wrong** | Query the source of truth (Firestore) directly; don't infer from the mapper code. |

## Step 1 — reproduce; Step 2 — explain with code; Step 3 — fix; Step 4 — verify with a signal again

Only after a signal confirms the cause do you open the code to explain it and write the fix. Then confirm the
fix against the **same signal** (crash gone from Vitals, `/health`+logs clean, asset 200, feature renders on
device).

## When the signal isn't reachable from here

This environment often **can't** see the live signals — no Play Console, no server SSH, no Firebase console,
and git can be slow/timing-out on the mounted FS. In that case the correct move is **ask for the signal, don't
fill the gap with a theory.** Say exactly what's needed and where to get it, e.g.:

- "Paste the Crashlytics/Vitals stack trace for version code N."
- "What does `curl -sS https://<backend>/health` return, and the last 20 lines of `errors/eduai_app/logs`?"
- "`curl -I <asset-url>` — is it 200?"
- "Run `<script>` and paste the output."

A verdict without the signal is a hypothesis — label it as one ("likely, pending the trace"), never as a
conclusion, and never write a "ruled out" table from it.

## Scorecard from this project (why this doc exists)

- ✅ **Class-dropdown crash** — went straight to the Vitals trace → pinned `NumberFormatException` to the exact
  line. Fast and right *because* it started from the signal.
- ❌ **Ch.5–8 coach silent** — inferred "hosting the guide JSON fixes it" from code; the live behavior (V4
  needs the page to publish `__eduRound`; those pages didn't) said otherwise. Should have checked a device.
- ❌ **Chat "server errors"** — concluded "backend/LLM quota, not the app" from the client's retry comments;
  `/health` was 200 and Firestore logs were full of JWT/R8 auth failures. Should have read the logs first.

Same lesson each time: **the signal was available (or askable); the verdict shouldn't have run ahead of it.**
