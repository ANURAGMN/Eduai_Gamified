# CLAUDE.md — EduAI (NCERT Class-7 AI Tutor)

Project context for Claude Code. Read this first; it's the map. Deep detail lives in `docs/` (see §10).

## 1. What this is
Android app: NCERT Class-7 AI tutor + interactive lab (Science & Math simulations with a guided "coach").
**Live on Google Play.** Languages: **English + Kannada (kn)** — Kannada is first-class, not an afterthought,
and is where most locale bugs hide. Package: `com.ncert7.aitutorandlab`.

## 2. Repos / where things live
- **`Eduapp/`** (this repo) — the Android app (Kotlin, Compose, Hilt, Room, Firebase).
- **`EduAI_app/`** (sibling repo) — the **simulations** (HTML/JS) served via **GitHub Pages**
  (`https://anuragmn.github.io/EduAI_app/Simulations/`), plus Firebase/ops **scripts/** and `reports/`.
  The in-app coach is driven from here (`edu-coach.js`, `guide-coach.js`, `*.guide.json`), so coach/sim
  content changes ship by **git push to Pages**, not a Play update.
- GitHub (fork for MRs): `ANURAGMN/Eduai_Gamified` (remote often referred to as `gamified`).

## 3. Versions / release state (as of 2026-10-09)
- **Live Play:** v1.0.11 / **versionCode 13** — got a Broken-Functionality rejection; **appeal filed; do NOT
 resubmit 13.**
- **Shipping next:** v1.0.12 / **versionCode 14** — `b66f5c0` pushed to `gamified/main`; Avatar unlock fix
 `9c34206` committed on top (in the AAB; push pending). Release AAB built 2026-10-08; device test (Kannada
 class pick) + Play closed testing still pending.
- **Post-vc14 branches:** `feature/notif-reprompt` (local); whiteboard PR #2.
- The whole "what's left" list is `docs/PENDING_BEFORE_NEXT_RELEASE.md`; ship checkboxes in
  `docs/RELEASE_CHECKLIST_v1.0.12.md`.

## 4. Build / test / verify
```bash
./gradlew testDebugUnitTest          # baseline ~298 tests, 9 known-unrelated failures (android.util.Log not mocked: MobileAdsInitializer, ResourceDecisionUseCase, SimulationIntroTtsSanitizer x7); no NEW failures
./gradlew :app:compileDebugKotlin    # quick compile check
./gradlew bundleRelease              # signed release AAB (R8/minify ON) — the build Play gets
```
- **R8/minify is ON for release only.** Release-only breakage (stripped reflection/enum names, etc.) will NOT
  show in debug. Always smoke-test the **release** build, not debug.
- Keep rules: `app/proguard-rules.pro` (Room `data.local.**`, Firebase, Hilt, Gson, Retrofit, and enums used
  with `valueOf` are kept).

## 5. The two bug *classes* that have bitten us (check these first on any change)
1. **Locale-fragile parsing.** Never parse a localized display string back to a value. The vc13 crash was
   `"Class %d"` → `removePrefix("Class ").toInt()` which threw `NumberFormatException` in Kannada
   (`ತರಗತಿ 7`). Rule: map by **option index / stable id**, never by scraping localized text.
2. **R8-stripped reflection.** The Sep chat outage was auth0 `java-jwt` using Jackson `TypeReference` paths
   R8 stripped → tokens read as expired → refresh storm. Rule: avoid reflection-based JSON on release; if an
   enum/class is resolved by name at runtime, add a ProGuard keep. Fixed via Base64+Gson `JwtDecoder` +
   `TokenAuthenticator` (see `docs/AUDIT_chat_auth_jwt_vc14.md`).

## 6. Debugging rule — LIVE SIGNALS FIRST
For anything happening in production, **observe before theorizing.** Pull the real signal, then use code to
explain it — never the reverse. Full playbook: `docs/DEBUGGING_LIVE_SIGNALS_FIRST.md`.
- Crash → Play **Vitals** / **Crashlytics** stack trace / **Pre-launch report**.
- "Server error" in chat → backend `GET /health` + **Firestore `errors/eduai_app/logs`** + the real HTTP body
  (not the client's retry comments).
- Feature not showing → reproduce on a **real device / live URL** (does it *render*), not "the file exists".
Don't write "ruled out" conclusions from inference; label unconfirmed things as hypotheses.

## 7. Key subsystems (one line each + where)
- **Coach:** V4 "one-clock". Coach line comes from the **page** publishing `window.__eduRound` via
  `edu-coach.js`; `guide-coach.js` drives it from a hosted `<sim>.guide.json` (mission→steps→done, hint from
  `whenStuck`, best-effort glow from step `target`). App loader: `SimGuideRepository` (bundled asset →
  hosted fallback). Coverage gate: `EduAI_app/scripts/check-coach-coverage.js` (Science must be 0 silent).
- **Simulations language:** concept has `simulation_url` + `simulation_url_kannada`; resolver prefers KN,
  `SimulationLanguageUrl` / `preferKannadaOverAgentUrl` reconcile the **agent** path. Firestore KN URLs set by
  `EduAI_app/scripts/set-kannada-sim-urls.js`.
- **Chat / tutor:** `AgenticAIClient` → agent backend at `http://13.48.59.144:8000` (single EC2, cleartext
  allowlisted). Auth = **Firebase ID token** via `ProactiveTokenInterceptor` + `TokenAuthenticator` (NOT
  `API_KEYS`, which is often empty). "Server error" = HTTP 500 or `success=false`.
- **Analytics:** GA4 only — **`AnalyticsFirestoreMirror.ENABLED = false`**, so event funnels are NOT in
  Firestore. GA4 property `517375741`. `sign_up` fires once on `CreateUserResult.Created` only
  (`SignUpAnalytics` + `FirebaseAnalyticsHelper.logSignUp`). Ads: see `docs/ADS_SIGNUP_VS_INSTALL_REVIEW.md`.
- **Notifications:** device-local engine (`notification/`), inexact alarms (no `SCHEDULE_EXACT_ALARM`),
  Room-backed budget/dedup, quiet hours, Android-13 permission primer. See `docs/NOTIFICATIONS.md`.
- **Data:** Room (local-first) synced from Firestore collections `Concept`, `progress`, `chapterprogress`,
  `sessions`, `streak`, `analytics`, `users`.

## 8. Conventions / gotchas
- **Don't commit junk:** `_tmp_*` and `.codex/` (gitignored), `app/release/*.apk`, baseline-profile binaries.
 Keep **docs commits separate** from code commits. Gradle builds the **working tree** — commit (or stash)
 drive-bys before a release build so the AAB matches a commit.
- **git on the Cowork mount was slow** (full-tree `status`/`log` timed out); in Claude Code's real terminal
  this is a non-issue — normal git works.
- Firestore writes must **merge** for existing users (bare `set` wipes onboarding/tutorConfig) — see
  `FirebaseRepository.createNewUser` (returns `Created` vs `Updated`).
- Math coach is **not** authored yet (EN Ch.2–9, KN Ch.1–4 silent) — known backlog, not a regression.

## 9. Current open threads (see PENDING doc for the live list)
- vc14: push `9c34206`, device-test the **release** APK (**Kannada class pick** = the rejection fix), closed
 testing → Pre-launch → Production. Do not resubmit 13. Release build OOMs at the default 2g heap — use
 `-Dorg.gradle.jvmargs=-Xmx4g`.
- Ads: `sign_up` is correct in code; switch campaign bid to it **only once daily signup volume supports it**
  (~1.5/day now is too low). Console steps in the Ads doc.
- MRs on `Eduai_Gamified`: **#2 whiteboard** (rebase onto post-vc14 main; strip the BottomNavBar reformat;
  check it reuses the hardened `AgenticAIClient`/auth, not a new client); **#1 notifications** (close/clean —
  has release binaries; likely superseded by `21dbd20`).

## 10. Doc index (authoritative detail)
- `docs/PENDING_BEFORE_NEXT_RELEASE.md` — single "what's left" list (release + Ads + MRs + backlog).
- `docs/RELEASE_CHECKLIST_v1.0.12.md` — ship-gate checkboxes.
- `docs/STATUS_play_vc14_coach_language_notifications.md` — live-vs-in-tree context.
- `docs/AUDIT_chat_auth_jwt_vc14.md` — chat auth / JWT-R8 package (canonical).
- `docs/CHAT_SERVER_ERRORS.md` — pointer to the audit.
- `docs/ADS_SIGNUP_VS_INSTALL_REVIEW.md` — install-vs-signup tracking for Ads.
- `docs/NOTIFICATIONS.md` — notification system + device QA.
- `docs/DEBUGGING_LIVE_SIGNALS_FIRST.md` — incident-debugging playbook (read §6 above as the summary).
- `docs/METRICS_FIRESTORE_PULL.md` — ops metrics / Excel.
- Coach coverage: `EduAI_app/scripts/check-coach-coverage.js`; dashboard: `metrics-dashboard.js`.

---
*If a request touches a live/production symptom, get the signal (Vitals/Crashlytics/logs/`/health`) before
proposing a fix. If it touches release behavior, verify on an R8 build. Keep this file updated as state
changes so the next session doesn't re-derive it.*
