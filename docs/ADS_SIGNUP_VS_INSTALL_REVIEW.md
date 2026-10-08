# Review — install-vs-signup tracking for Google Ads optimization

**Date:** 2026-10-08 · **Scope:** the GA4 `sign_up` integration that lets Ads optimize for registrations
instead of installs. **Verdict:** the **app code is correct and well-designed**; the remaining work is
GA4/Ads **console configuration**, not code.

---

## 1. How it's wired (verified in code)

| Piece | Location | Detail |
|------|----------|--------|
| Event name + method | `service/analytics/SignUpAnalytics.kt` | `EVENT_NAME = "sign_up"` (standard GA4 event), `method` → `google` / `email` / `unknown` |
| Emit | `service/analytics/FirebaseAnalyticsHelper.logSignUp(method)` | logs the standard `sign_up` with `method` param |
| New-user gate | `repository/FirebaseRepository.createNewUser` | queries `email + appName`; returns **`Created`** only when no doc exists, else **`Updated`** (merge) |
| Single call site | `ui/screens/login/viewmodel/UserViewModel.kt:419` | fires `logSignUp` **only** in the `Created` branch; `Updated` explicitly skips |

Grep of the whole source: the `sign_up` string lives only in the centralized helper + the one call site —
**no stray `logEvent("sign_up")`, no screen-based proxy, no double-fire path.**

## 2. Install vs signup — the split is correct

- **Install** — tracked automatically by Play's install conversion + the Firebase SDK's auto `first_open`.
  No app code needed.
- **Signup** — the custom in-app signal, emitted **once per genuine new account**. This is the deeper event
  App campaigns should bid toward instead of installs.

Firing only on `CreateUserResult.Created` means returning logins, profile merges, and reinstalls with the
same Google account do **not** re-fire `sign_up` — so Ads signup conversions won't be inflated.

## 3. Still required to actually optimize (console — not code)

Mirrors `PENDING_BEFORE_NEXT_RELEASE.md` §4. **Do in order:**

1. **Linkage:** GA4 property `517375741` ↔ Firebase (`eduai-e090e`) ↔ Ads account `263-372-1466` all linked.
2. **Validate the event** on the shipped vc14 build: one new registration → confirm `sign_up` in GA4
   Realtime / Events.
3. **Mark `sign_up` as a Key Event** in GA4 (counting "once per event" is fine — it fires once per new user).
4. **Import** in Ads → Goals → Conversions → App → import `sign_up` from Firebase/GA4 (appears ~24h after
   the first real event).
5. **Point the App campaign's bid/optimization target at the `sign_up` conversion.** ← the step most often
   missed; without it the campaign stays install-optimized even after the event is imported.

## 4. Flags / risks

- **Needs a real install→signup chain on the shipped build.** The event + attribution only prove out
  post-vc14 on a device with the GA4↔Ads link live — hence the "register → Realtime → import" order above.
- **Minor race (low risk):** the `Updated` guard protects *sequential* retries, not two *concurrent*
  submits (both could read "no doc" and return `Created`). UI is a single submit, so unlikely — but if
  signup counts ever run slightly above new-account counts, this is the cause. Fix later only if observed
  (e.g. a transaction / unique-doc-id write keyed on uid).
- **Collection consent:** if a user has analytics collection disabled, `sign_up` won't send — expected, not
  a bug.
- **Signup volume is likely too low to bid on yet.** Firestore shows ~23 signups in 15 days (~1.5/day,
  all sources, 22 Sep–6 Oct). App campaigns optimizing for an in-app action need steady daily conversion
  volume; at this rate the campaign may stay in learning or under-deliver. Switching the bid target also
  resets learning. Prefer: keep install-optimized while `sign_up` accumulates as an observed conversion, or
  run a separate signup-optimized campaign, and switch once daily signups are consistently higher.
- **Kids compliance:** `FirebaseAnalyticsHelper` sets `allow_ad_personalization_signals = false`. This
  should not block conversion measurement, but confirm `sign_up` conversions actually appear in Ads after
  import.

## 5. Forward suggestion (not a gap)

Once signup volume is healthy, App campaigns optimize even better with a **downstream engaged-user signal**
(e.g. `first_lesson_complete` or `trial_start`) layered in as a secondary conversion. `sign_up` is the
correct first step; this is a later refinement.

---

## Bottom line
Code: correct, single-fire, gated on true new users — nothing to change. Optimization now depends on the
§3 console steps, the key one being **retargeting the campaign bid to the `sign_up` conversion** — but only
once signup volume can support it (§4).

**Related:** `PENDING_BEFORE_NEXT_RELEASE.md` §4 (Ads), `docs/METRICS_FIRESTORE_PULL.md` (ops metrics).
