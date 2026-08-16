# Release Readiness — v1.0.11 (versionCode 13)

**App:** `com.ncert7.aitutorandlab` · NCERT Class-7 AI Tutor & Labs
**Date:** 2026-08-16 · **Prepared for:** Play Console update
**Verdict: CONDITIONAL GO — clear the release-path blockers first.** For **this AAB upload:** commit WebView/coach/theme fixes (#12), listing strings (#14), AI-Report decision (#13), clean ship set (#15), bump **targetSdk 36** (#10), ColorOS retest, Data-Safety refresh (#4). **Firestore rules (#1/#2) do not gate this cut** — interim rollback stays live; switch after min-version adoption (separate calendar). Everything else is release-ready or an accepted standing risk.

> Scope of this doc: (1) go/no-go readiness, (2) compliance / Data-Safety refresh vs the last release, (3) Firestore rules **old vs new**. Not legal advice; the under-13 consent item needs product/legal sign-off.

---

## 0. What actually ships in this build (read first)

The recent coaching work is **content, not app code** — it lives in the **`EduAI_app`** repo and is served to the in-app WebView from GitHub Pages. It is **already live** and does **not** require an APK release.

| Change | Where | Ships via |
|--------|-------|-----------|
| Coach hooks for science_2 / 3 / 4 (EN) + walkthroughs | `EduAI_app/Simulations/*` | **GitHub Pages (already live)** |
| Sim contrast CSS fallbacks (solid `background-color`) | `EduAI_app` sims | GitHub Pages (already live) |
| WebView **force-dark OFF** (`c66088e`) | `Eduapp/SimulationWebView.kt` | APK — committed ✅ |
| **⚠ ColorOS/OEM contrast rescue + theme `isLightTheme`/`forceDarkAllowed` + coach unlock-on-url fix** | **5 files:** `SimulationWebView.kt`, `SimulationInteractionScript.kt`, `ConceptSimulationViewer.kt`, `res/values/themes.xml`, `res/values-night/themes.xml` | APK — **UNCOMMITTED (dirty)** — see §1 #12 |
| P1 sync fixes (garden restore, onboarding, economy, quests) | `Eduapp/app` | APK — committed ✅ |
| P1 review fixes RV.1/RV.2/RV.3 | `Eduapp/app` | APK — **committed `3e1f90e`** ✅ |
| Textbook section, header Next button, Kannada UI sweep | `Eduapp/app` | APK — this release |

**Implication:** the coach chapters are already usable in-app, but on dark-mode devices they render white-on-white until the WebView fix ships. **Committed HEAD alone is only a *partial* fix** — it has `FORCE_DARK_OFF` (`c66088e`) but **not** the ColorOS/OEM contrast rescue, the theme `isLightTheme`/`forceDarkAllowed` handling, or the coach unlock-on-url fix, which are still **dirty across five files** (the three Kotlin files plus `values/themes.xml` and `values-night/themes.xml`). A release cut from clean HEAD may still show white-on-white / missing coach / mis-themed WebView on ColorOS (Oppo/Realme). **Commit all five before this cut**, or retest HEAD on a ColorOS device and downgrade the claim to "partial fix."

---

## 1. Go / No-Go checklist

| # | Item | Status | Blocking? |
|---|------|--------|-----------|
| 1 | **Firestore rules transition** (auth-gated vs rollback) | 🔴 decision needed | Blocks **rules re-deploy only** — **not** this AAB (§3 / §7) |
| 2 | **Auth-gated ruleset missing economy domains** | 🔴 must fix before any auth-gated re-deploy | Blocks **rules re-deploy only** — **not** this AAB (§3 / Appendix A) |
| 3 | P1 RV.1/RV.2/RV.3 fixes | 🟢 committed `3e1f90e` | no |
| 4 | Data-Safety form refreshed for this build | 🟠 this doc §2 | before submit |
| 5 | Contrast/coach verified on **ColorOS / Oppo** (primary); optionally stock Android 10–13 | 🟠 on-device only | strongly rec. — after ship-commit AAB (§7) |
| 6 | Under-13 Google Sign-In / parental consent | 🟡 standing risk | product/legal call |
| 7 | Build config (signing, ProGuard, `isDebuggable=false`, targetSdk 35) | 🟢 configured | no |
| 8 | Signed AAB builds + smoke test on release build | 🟠 you run it | before submit |
| 9 | Version + release notes | 🟠 config is vc13/1.0.11; **confirm Play doesn't already have vc13** — if it does, bump to vc14 for this update; then write release notes | before submit |
| 10 | **targetSdk 36 by Aug 31, 2026** (was 35) | 🟢 bumped to 36 — still need API-36 behavior pass (§5.1) | test before submit |
| 11 | Android developer verification registered (Sep 30, 2026) | 🟡 account-level, likely auto-registered | no (not a build gate) — see §5.2 |
| 12 | **Uncommitted WebView/coach/theme fixes** (ColorOS contrast rescue + `isLightTheme`/`forceDarkAllowed` + unlock-on-url) | 🟢 committed `a9f480a` | no |
| 13 | **AI "Report" control on assistant messages** (Play GenAI policy P0) | 🟡 **accepted risk for this cut** (§6.1) | no for this upload — ship Report in a follow-up |
| 14 | **Store-listing hygiene** (EN app_name leading space; KN name mismatch; contact email = personal Gmail) | 🟢 committed `93d8080` — still update Console contact | Console residual (§6.2) |
| 15 | **Clean, explicit committed ship set** before `bundleRelease` (tree is dirty ~61 files; ignore `ui-kit/build`) | 🟢 ship commit `93592d9` (+ `e9b57e5` untracked `ui-kit/build`) | build AAB from this tip |

**Recommendation (order):** (1) commit the 5 WebView/coach/theme files (#12); (2) fix listing strings (#14); (3) AI-Report decision — a 5-min accepted-risk note *or* implement (#13); (4) pin a clean committed ship set (#15) + bump `targetSdk 36` (#10); (5) build AAB → retest on **ColorOS** → refresh **Data-Safety** (§2) → upload. **The Firestore rules switch (#1/#2) is a separate, post-adoption step — the interim rollback rules stay live meanwhile, so it does NOT gate this release.** See §7 for the full plan.

---

## 2. Compliance / Data-Safety refresh (delta since v1.0.10 / vc12)

`PLAY_DATA_SAFETY.md` was last verified at **vc12 (2026-07-30)**. Since then the P1 work added new synced data and there are two permission nuances to reconcile before re-submitting the form.

### 2.1 New data now collected/synced (P1) — update the form
| New Firestore domain | Data | Play data-type mapping |
|----------------------|------|------------------------|
| `gamification/…/profile` | XP, gems, league tier, **friendCode**, cohortId | "App activity" / "In-app actions". friendCode = studentId only, **no PII** (good). |
| `exam_plans/…` | plan header + day items | "App activity". |
| `quests/…/daily` | daily quest progress/claims | "App activity". |

None of these add a **new PII category** beyond what's already declared — they're app-activity/progress. But the Data-Safety form should note the expanded app-activity collection so it matches behavior.

### 2.2 Microphone / audio — **confirm the declaration**
- `RECORD_AUDIO` is declared and **used** (SpeechToText via `SpeechRecognizer` in chatbot, math agent, revision, simulation agent, and the 4_10 voice-answer).
- Android `SpeechRecognizer` typically routes audio to Google's on-device/cloud recognizer. **Data Safety must either declare "Audio → Voice or sound recordings" (collected/shared) or document that recognition is on-device and audio is not retained.** This is a common rejection point — do not leave it implicit.

### 2.3 Advertising ID — **decide intentionally**
- `com.google.android.gms.permission.AD_ID` is **not** in the manifest (grep = 0).
- For a **child-directed** app this is often **correct** (non-personalized ads, no ad ID). If that's the intent: Data Safety "Device or other IDs" can be **No**, and confirm AdMob is set to non-personalized/child-directed (tag for child treatment).
- If AdMob's SDK auto-merges AD_ID (newer play-services-ads), you may need to **explicitly remove** it with `tools:node="remove"` to keep a kids app clean. **Verify the merged manifest of the release build.**

### 2.4 Standing items (unchanged, from `COMPLIANCE_BLOCKERS.md`)
- **Under-13 Google Sign-In + parental consent** — still 🟡 potentially blocking; needs product/legal. Not introduced by this release.
- **In-app "Request account deletion"** — still email-only; acceptable for now, add before scale. Deletion contact is **`contact@padaams.in`** in-app (`93d8080`); keep listing + Data Safety aligned.
- Encryption in transit ✅; cleartext limited to the AI backend IP in `network_security_config.xml` ✅.

---

## 3. Firestore rules — OLD vs NEW (the key release decision)

There are **two rulesets** in play, and they diverge. The committed file and the deployed file are not the same posture.

| | **Committed (HEAD)** | **Working tree = what was deployed** |
|---|---|---|
| Header | "child-data rules — **requires Firebase Auth**" | "**rollback** rules — TEMPORARY, restores pre-Auth access for 1.0.8 clients" |
| Gate | `request.auth != null` + `auth_index/{uid}` → studentId + owner-only | `appName == 'eduai_app'` + `studentId` matches parent doc (**no auth**) |
| `users/` read | owner-only | `allow read: if true` (**open**) |
| `auth_index/` | present | removed |
| **Economy domains** (`gamification`, `exam_plans`, `quests`) | ❌ **absent (0)** | ✅ present (4 matches) |

**Why the rollback exists:** 1.0.8/1.0.9 clients in the wild have **no** `FirebaseAuthBridge`, so auth-gated rules would lock them out of sync. The file's own TODO: *re-deploy auth-gated rules once Play min version ≥ 1.0.11 / vc13.* **This release is vc13** — so that transition becomes possible after adoption.

### 🔴 The two things to fix before re-deploying auth-gated rules
1. **The auth-gated ruleset is stale** — it predates the P1 economy work and **lacks** `gamification` / `exam_plans` / `quests`. Re-deploying it as-is would **silently break** XP/gems/plan/quest sync (writes denied). It must be updated to include those three domains (owner-scoped) **before** it's ever deployed. (This is review-note **RV.6** made concrete, and **RV.4** — committed ≠ deployed.)
2. **Confirm what's actually live** in the `eduai-e090e` console right now. Per the P1 handoff the rollback (open) set was deployed. If so, the 🔴 child-data exposure item in `COMPLIANCE_BLOCKERS.md` (world-readable `users`, `simulation_interactions`, `friend_codes`) is **currently open in production** — a real DPDP/COPPA/GDPR-K concern for a children's app.

### Recommended transition plan
1. Ship **1.0.11 / vc13** (adds FirebaseAuthBridge to the client — already in this build).
2. In Play Console, set **minimum version** (or a forced-update prompt) so vc≤12 clients update. Allow adoption time.
3. **Reconcile the auth-gated ruleset**: add the `gamification` / `exam_plans` / `quests` matches using the **auth-gated owner-scoped pattern** — see **Appendix A** for the ready-to-paste blocks (do **not** copy the looser rollback pattern).
4. Deploy the reconciled auth-gated rules via `scripts/deploy-firestore-rules.py`; smoke-test login → progress → garden → economy → quests → friends on a **release** build.
5. Commit the deployed rules so repo == console (closes RV.4 drift).

**Interim (if you ship vc13 now, before the switch):** the open rollback rules stay live → old clients keep working, but document the child-data exposure as an **accepted, time-boxed risk** with the switch scheduled, since this is a kids app.

---

## 4. Pre-submit runbook (you execute — I can't build/upload from here)

1. **Commit the 5 WebView/coach/theme files (#12)** — without these the contrast/coach fix is only partial on ColorOS.
2. **AI Report decision (#13)** — implement the Report control on assistant messages, or record the accepted GenAI-policy risk in this doc.
3. **Store-listing hygiene (#14)** — fix EN `app_name` leading space, KN name, and `contact_email` → `contact@padaams.in` (must match Data Safety).
4. **Pin a clean committed ship set (#15)** — git-ignore `ui-kit/build`; the tree is dirty (~61 files). Build the AAB from a known commit, not the working folder. (P1 RV fixes already committed `3e1f90e`.)
5. **targetSdk 36 (#10)** — bump in `build.gradle.kts` and run the API-36 behavior test pass (§5.1).
6. Version: confirm Play doesn't already have vc13; if it does, `versionCode 13 → 14`. Write `store-listing/release-notes-1.0.11.txt`.
7. Compile + unit test: `:app:compileDebugKotlin` + `:app:testDebugUnitTest` (incl. `GamificationLwwTest`).
8. Build signed AAB: `./gradlew :app:bundleRelease` (signing + ProGuard already configured via `local.properties`).
9. **On-device (dark mode):** primary target **ColorOS / Oppo** (the known break); optionally also stock Android 10–13. Open any Acids/Electricity/Metals sim → confirm **no white-on-white**, WebView themed correctly, and the coach bar renders.
10. Play Console → refresh **Data Safety** per §2 (app-activity expansion, audio decision, AD_ID decision).
11. Upload AAB → internal testing → verify sync end-to-end on the release build → promote.
12. Execute the §3 rules transition (reconciled auth-gated rules, Appendix A) **after** min-version adoption — not at upload time.

---

## 5. Platform & account deadlines (external — verified Aug 2026)

### 5.1 🔴 targetSdk 36 (Android 16) — Aug 31, 2026 *(time-sensitive)*
- **Policy:** from **Aug 31, 2026**, new apps **and updates** must **target API 36** to be submitted; updates targeting API 35 or lower are **rejected**. Untouched existing apps only need API 35 to stay available to new users. Extension to **Nov 1, 2026** requestable in Console.
- **Current state:** `targetSdk = 35`, `compileSdk = 36`, minSdk 28. Today is ~2 weeks from the cutoff.
- **Decision:**
  - Ship at **35 before Aug 31** → accepted, but tight; any slip → rejected, and a 36 bump still needed within weeks.
  - **Bump to 36 now (recommended)** → `compileSdk` is already 36, so it's a one-line change; ship once instead of twice.
- **API 36 behavior changes to test before shipping** (on top of API 35 edge-to-edge already handled):
  - Edge-to-edge **fully enforced** (no opt-out) → verify `WindowInsets` on Compose screens, the WebView sim host, and dialogs (nothing hidden behind status/nav bars).
  - Foreground-service / background-launch tightening → WorkManager path is fine; re-check the `RECEIVE_BOOT_COMPLETED` receiver.
  - **16 KB page size** → confirm native libs (AdMob, Firebase) in the AAB are 16 KB-aligned.
  - Predictive back; large-screen/adaptive (mostly cosmetic, phone-first).
- **Action:** bump `targetSdk = 36` in `app/build.gradle.kts`, run the behavior-change test pass, then this becomes 🟢.

### 5.2 🟡 Android developer verification — Sep 30, 2026 *(account-level, not a build gate)*
- **Policy:** all Play apps must be **registered** by **Sep 30, 2026** (enforcement starts in Brazil/Indonesia/Singapore/Thailand first; global 2027). ~99% of Play apps were **auto-registered** (package name + Play App Signing key) in March 2026.
- **This app:** Play-distributed with **Play App Signing** → almost certainly auto-registered. Only **confirm**.
- **Action (in Console, not via email links — verify authenticity):** Play Console → **Home** → check the package-name status next to the app / filter **unregistered**. Register only if it shows unregistered. Add extra keys only if you also sign/distribute this app **outside** Play (you don't appear to).
- **Not a blocker for the 1.0.11 upload** — it's an account requirement, tracked here so it isn't missed.

## 6. Additional blockers (from checklist + code review)

### 6.1 🟡 AI "Report" control on assistant messages — Play Generative AI policy (P0)
- `PRE_LAUNCH_CHECKLIST.md` P0 #1: add a **Report** action (long-press or ⋮ overflow) on **AI assistant messages** in the shared `ConversationView` (covers chatbot + math agent); log a report event + optional Firestore doc. Settings → Contact Support is **not** sufficient.
- **Currently absent in code** (no report/flag on messages found).
- **Decision for v1.0.11 / vc13 (2026-08-16):** **Accepted GenAI-policy risk for this cut.** Possible Play rejection or post-launch policy action until Report ships. Mitigations meanwhile: in-app Contact Support (`contact@padaams.in`), content moderation on the AI backend, and staged rollout. **Follow-up (next release):** Report on assistant messages in `ConversationView` + analytics/Firestore event. Same decision recorded in `COMPLIANCE_BLOCKERS.md`.

### 6.2 🟢 Store-listing hygiene (`strings.xml`) — fixed in `93d8080`
| Item | Was | Now (in app) |
|------|-----|--------------|
| EN `app_name` | leading space | `NCERT Class 7 AI Tutor & Labs` |
| KN `app_name` | `EduAI` | `NCERT ತರಗತಿ 7 AI ಟ್ಯೂಟರ್ & ಲ್ಯಾಬ್‌ಗಳು` |
| `contact_email` | personal Gmail | `contact@padaams.in` |
- **Still you (Console):** Play listing + Data Safety deletion contact must match `contact@padaams.in`.

### 6.3 🔴 Clean, explicit ship set before `bundleRelease`
- Working tree is **dirty (~61 app Kotlin files + ui-kit build noise)**. Do **not** build the release AAB from "whatever is in the folder."
- Pin an explicit **committed ship set**, build from that commit, and git-ignore `ui-kit/build/` (+ other build output) so it isn't mistaken for source.
- Ensure the **five** uncommitted WebView/coach/theme files (§0) are in the ship set.

## 7. Resolution plan (per blocker)

**Critical path (in order):** #12 commit → #14 strings → #13 decision → #15 ship-set commit(s) → #10 targetSdk → compile/test → build AAB → **on-device (ColorOS)** → **Data-Safety refresh (#4)** → upload. The Firestore rules switch (**#1/#2**) is on a **separate calendar, after min-version adoption** — the interim rollback rules stay live, so it does **not** gate this release.

| # | Blocker | Resolution steps | Owner | Effort |
|---|---------|------------------|-------|--------|
| **12** | Uncommitted WebView/coach/theme (5 files) | `git add` the 5 files (3 Kotlin + 2 `themes.xml`) → commit as one "ColorOS contrast + coach unlock" commit. **Do not** `bundleRelease` yet — the tree is still dirty; ColorOS proof comes from the **ship-commit AAB** after #15/#10. | You / Cursor | 20 min |
| **14** | Store-listing hygiene | Separate step after #12: `strings.xml` — trim EN `app_name` leading space; set KN name; `contact_email` → `contact@padaams.in`. Commit on its own (or fold into the next ship-set commit). Update Play listing + Data-Safety contact to match. | I can edit strings; you update Console | 15 min |
| **13** | AI "Report" control (GenAI P0) | **Fast path:** record an explicit accepted-risk note here + in `COMPLIANCE_BLOCKERS.md` (≈5 min) — do this so it does **not** block #12/#14. **Full fix (if you want it in this cut):** add a **Report** action (long-press/⋮) on assistant messages in shared `ConversationView` + log event (+ optional Firestore `reports/`). | You decide — I can draft the code | 5 min (accept-risk) / ~0.5–1 day (impl) |
| **15** | Clean ship set | Two parts: (a) *quick* — add `ui-kit/build/` (+ build output) to `.gitignore`, `git rm --cached` if tracked; (b) *the real work* — after #12/#14 are committed, **explicitly decide in/out for the remaining ~61 dirty app files** (a product cut, not a chore). Then `bundleRelease` **from that commit** and run the ColorOS retest (#5) on that AAB/APK. | You / Cursor | (a) 10 min · (b) 0.5–1 day review |
| **10** | targetSdk 36 (by Aug 31) | Bump `targetSdk = 36` (`compileSdk` already 36); run the API-36 behavior pass (edge-to-edge insets, boot receiver, 16 KB native libs — §5.1); fix insets if any overlap. | I can bump; you test | one-line + ~0.5 day test |
| **4** | Data-Safety form refresh | Before upload: update the Console Data-Safety form per §2 (app-activity expansion for economy domains, mic/audio declaration, AD_ID decision); match the deletion contact to #14. | You (Console) | 20 min |
| **1 / 2** | Firestore rules transition + economy gap *(post-adoption, not a release gate)* | Confirm old-version users negligible + Auth adoption (Console); add **Appendix A** blocks to the auth-gated ruleset; emulator/playground test; deploy via `deploy-firestore-rules.py`; commit so repo == console. | I can produce the reconciled rules file; you deploy | ~0.5 day, **on a separate calendar** |
| 9 | Version + release notes | Confirm Play doesn't already have vc13; bump to vc14 if so; write `release-notes-1.0.11.txt`. | You | 15 min |
| 11 | Developer verification | Confirm the app shows registered (Console Home) — likely auto-done. | You | 5 min |
| 6 | Under-13 consent (standing) | Product/legal review; not introduced by this release. | Product / legal | — |

**What I can do now (no build/console access):** (a) draft the AI-Report control code for `ConversationView`; (b) fix the listing `strings.xml`; (c) produce the reconciled auth-gated `firestore.rules` (Appendix A merged in); (d) bump `targetSdk = 36`; (e) add the build-output `.gitignore` entries. Tell me which to start.

## 8. Summary

- **Most time-sensitive:** **targetSdk 36 by Aug 31, 2026** (~2 weeks). Recommend bumping 35→36 now (`compileSdk` already 36) and shipping once.
- **Commit the WebView/coach/theme fixes first.** Committed HEAD is only a **partial** contrast fix (`FORCE_DARK_OFF` only); the ColorOS rescue + `isLightTheme`/`forceDarkAllowed` + coach unlock-on-url are **dirty across 5 files** (3 Kotlin + 2 `themes.xml`) — commit them or a clean-HEAD build can still be white-on-white / mis-themed on ColorOS.
- **AI "Report" control** is a **P0 GenAI-policy blocker** and is missing — implement or explicitly accept the risk; don't leave it out.
- **Build from a clean, committed ship set** (tree is dirty ~61 files) — not the working folder; git-ignore `ui-kit/build`.
- **Firestore rules (#1/#2):** auth-gated transition + stale economy domains — **post-adoption**, not an AAB gate. Interim rollback stays live for this cut; reconcile via Appendix A before any auth-gated re-deploy. (Confirm old clients negligible in Console before switching.)
- **Store-listing hygiene:** app_name leading space, KN name, and the personal-Gmail contact email must be reconciled with the listing + Data Safety.
- **Data-Safety refresh** (app-activity expansion, mic/audio declaration, AD_ID decision) to match the shipped build.
- **Developer verification** (Sep 30, 2026): confirm the app shows registered in Console — likely auto-done, not a build gate.
- **Content coach work is already live** via Pages (not the APK).
- **Bottom line (this release):** clear #12 → #14 → #13 → #15 → #10, build AAB from the ship commit, **retest on ColorOS**, refresh Data-Safety (#4) + listing, upload. **Rules (#1/#2) later.** Then it's a GO.

---

## Appendix A — Economy domains to add to the auth-gated ruleset (closes §3 #1)

The committed auth-gated ruleset is missing `gamification` / `exam_plans` / `quests`. Add these blocks (they mirror the auth-gated `garden` pattern exactly — `canReadStudentParent` for owner-only read, `studentIdMatchesParent` for owner-only write, `delete` denied). Paste alongside the other student-scoped matches, **before** the catch-all `match /{document=**}` deny.

```
match /gamification/{parentDoc}/profile/{docId} {
  allow read: if canReadStudentParent(parentDoc);
  allow create, update: if studentIdMatchesParent(parentDoc);
  allow delete: if false;
}

match /exam_plans/{parentDoc}/current/{docId} {
  allow read: if canReadStudentParent(parentDoc);
  allow create, update: if studentIdMatchesParent(parentDoc);
  allow delete: if false;
}

match /exam_plans/{parentDoc}/days/{dayId} {
  allow read: if canReadStudentParent(parentDoc);
  allow create, update: if studentIdMatchesParent(parentDoc);
  allow delete: if false;
}

match /quests/{parentDoc}/daily/{questDate} {
  allow read: if canReadStudentParent(parentDoc);
  allow create, update: if studentIdMatchesParent(parentDoc);
  allow delete: if false;
}
```

**Notes:**
- In the auth-gated ruleset, `studentIdMatchesParent` already includes `ownsStudent(...)` (Firebase-Auth uid → studentId via `auth_index`), so these are owner-only — matching how the client writes them.
- The **payloads must carry `appName: "eduai_app"` + `studentId`** (they already do in `GamificationSyncManager` / `ExamPlanSyncManager` / `QuestSyncManager`).
- After adding: emulator/playground test login → economy sync (XP/gems, plan, quests) → no `permission-denied`, then `deploy-firestore-rules.py`, then commit so repo == console.
