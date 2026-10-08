# Release checklist — v1.0.12 (versionCode 14)

Consolidated "must-do before we ship the next version." Grouped by **blocker** (ship is not safe without
it), **deploy** (Pages/Firestore — takes effect on live installs, land it with the release), **verify**, and
**deferred** (explicitly NOT blocking this release).

Status legend: ☑ done in tree · ◻ to do · ⚠ verify

---

## 1. Blockers — must be in the build

- ☑ **Crash fix: class-dropdown `NumberFormatException`** (the Play v13 rejection). Locale-proof index-based
  parse applied in **both** `UserDetailEntryScreen.kt` and `EditProfileSection.kt`. — `classOptions.indexOf` present in both.
- ☑ **Version bumped** — `versionCode = 14`, `versionName = "1.0.12"` in `app/build.gradle.kts`.
- ☑ **Kannada sim shows English (Bug 1) — client fix** — `SimulationLanguageUrl` + `preferKannadaOverAgentUrl`
  wired into the agent path + `ConceptSimulationViewModel` + `PlanTrialMaterializer`.
- ☑ **Reels made-for-kids gating on by default** — `REQUIRE_MADE_FOR_KIDS_DEFAULT = true` (child-safety).
- ☑ **GA4 `sign_up` event (Ads signup conversion)** — `FirebaseAnalyticsHelper.logSignUp()` fired from
  `UserViewModel.submitNewUser` only when `createNewUser` returns `CreateUserResult.Created` (not
  Updated/retry merge, not returning login). Method via `SignUpAnalytics`. Required so Google Ads can
  optimize for signups, not installs-only. **Do not** invent `sign_up` via Analytics “Create event
  without code.” Unit: `SignUpAnalyticsTest`.
- ◻ **Commit the ship set** — §1 ☑ items are in the **working tree**, not on `main` yet. Commit crash +
  language/coach Kotlin + JWT/auth package + `appVersion*` sync + `logSignUp` (+ docs as needed). **Do not**
  fold Avatar Studio / `_tmp_*` junk into the release commit.
- ◻ **Build the release AAB and confirm it does NOT crash on open** — install the actual release build, run
  onboarding in **Kannada** (the crash locale), pick a class on the user-detail screen and in Edit Profile.
  This is the single most important gate — it's what Play rejected.
- ⚠ **R8/minify is on** (`isMinifyEnabled = true`). After building, smoke-test the *release* build (not just
  debug) across onboarding → home → concept → sim → settings, since R8-only breakage only shows there.
  Room keep rules for `data.local.**` are already in `proguard-rules.pro` (post-1.0.11).

## 1b. Google Ads / Analytics — before or right after Play upload (signup tracking)

Ads account `(263-372-1466)` must use the GA4 property that receives this app’s events. Wrong property =
installs with no signup signal.

| Check | Detail |
|-------|--------|
| Firebase project | **`eduai-e090e`** (project number `515723957976`) |
| Correct GA4 property | **`eduai-e090e` / Property ID `517375741`** (stream `com.ncert7.aitutorandlab`) |
| Do **not** use | `eduai-5e78b` (`517333707`), `memory-lane-*`, or `demopam*` |
| Ads ↔ GA4 link | Linked accounts → GA4 & Firebase → **`517375741` only** (unlink `5e78b` if still present) |
| Access | Firebase owner `check@padaams.in`; Ads often `mail2anuragmn@gmail.com` — grant **Property access** on `517375741` to the Ads operator and accept the invite |

**Post-build / pre-optimize for signups (console):**

- ◻ Install **this** release (or Closed testing) → complete **one new** registration → confirm `sign_up` in
  GA4 Realtime / Events (last 28 days). Event is absent until the new build fires it.
- ◻ Analytics → Events → star **`sign_up` as Key event** (once per event). Do **not** create a fake
  screen-based `sign_up`.
- ◻ Google Ads → Goals → Conversions → App → import **`sign_up`** from Firebase/GA4 (can take up to ~24h
  after link + Key Event). Until imported, campaigns stay **install-optimized** / Learning on installs only.

## 2. Deploy with the release (Pages / Firestore — no app update needed, but land them together)

- ☑ **Coach guides reachable on Pages** — `guide-coach.js` returns **200** on GitHub Pages; Ch.5–8 wiring +
  Heat multi-target glow already on `EduAI_app` `main` (tip ~`4ed8e30`). Ignore local `Simulations/.fuse_hidden*`
  junk — do **not** commit those. Re-check: `curl -I …/guide-coach.js` → 200 before upload day.
- ☑ **Bundle guides in the APK** — `app/src/main/assets/sim_guides/` has **~154** guide JSONs in tree.
  ⚠ Still confirm they land inside the release AAB (not stripped).
- ⚠ **Kannada sim URLs in Firestore** — `simulation_url_kannada` filled where a `_kn.html` exists (5 Science
  done; Math Ch.1–4 already set). Re-run `EduAI_app/scripts/set-kannada-sim-urls.js` (dry-run) to confirm 0 pending.
- ⚠ **Coach coverage gate green** — from EduAI_app: `node scripts/check-coach-coverage.js` → **Science 0 silent** before ship.

## 3. Verify (tests + device QA)

- ◻ **Unit tests** — `./gradlew testDebugUnitTest`; expect the known baseline (was 225 total / 8 pre-existing
  failures unrelated). No *new* failures. Include the new `SimulationUrlResolverTest`.
- ◻ **Login / onboarding** — fresh install, sign in, complete user-detail (EN **and** KN), no crash.
- ◻ **Simulations** — EN + KN load the correct-language HTML with matching voice; coach appears (mission →
  steps; hint; glow where targets exist) on Science Ch.2–8.
- ◻ **Notifications** — device QA per `docs/NOTIFICATIONS.md` (reminder fires in-window, quiet hours, no spam,
  tap routing, permission primer, survives reboot).
- ◻ **Institute login / settings / bookmarks / badges** — re-run the earlier device-QA list items that were
  open, on a release build.

## 4. Deferred — explicitly NOT blocking v1.0.12

- **Math coach** — EN Ch.2–9 (92 sims) + KN Ch.1–4 (20 sims) have no guides; needs authoring + `MathCoachSolver`
  rules. Large, separate effort.
- **Glow `target` coverage** — sparse, esp. KN Ch.3–6 (Ch.3 = 2/40) and EN Ch.6/Ch.8. Content task to add
  `target`s; hint already works everywhere.
- **EN Ch.3–4 native coach audit** — the flagged glow/hint on the hand-authored (non-guide-coach) pages.
- **Coverage gate parity** — extend `check-coach-coverage.js` to flag missing `whenStuck` / missing `target`.
- **Metrics dashboard** — `metrics-dashboard.js` is an internal tool (not shipped in the app); commit when
  convenient. GA4 funnels need the Firestore mirror on or a GA4 export.

---

## Minimal path to ship
1. **Commit** ship set (crash + KN URL reconcile + version + `logSignUp` + related). 2. Build release AAB (v14).
3. Install it, run onboarding + class-pick in **Kannada** → confirm no crash.
4. Confirm Pages still 200 + coverage gate green (coach already live — re-verify only).
5. Run unit tests (baseline + `SimulationUrlResolverTest`). 6. Quick device pass (login, Science sim EN+KN, notifications per `NOTIFICATIONS.md`).
7. Upload AAB to **Closed testing** first (generates Pre-launch) → then Production / review.
8. **§1b signup tracking:** one new registration on this build → Key Event `sign_up` → Ads import (property **517375741**).
9. Release notes: "Fixed class-selection crash (NumberFormatException) affecting Kannada profile setup."

*Appeal for vc13 was filed 2026-08-31; still ship vc14 — do not resubmit 13. See also
`docs/STATUS_play_vc14_coach_language_notifications.md`.*

*Everything in §1 marked ☑ is already in the working tree; remaining ◻ items are **commit** + build/verify
actions, not new feature code — except any fixes surfaced by the device pass. §1b console steps need the
shipped build before `sign_up` appears for Key Event / Ads import.*
