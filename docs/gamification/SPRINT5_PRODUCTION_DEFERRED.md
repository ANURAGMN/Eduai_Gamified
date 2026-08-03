# Sprint 5 — deferred until production release

**Status:** Client quest → rewarded ad → gems flow is **verified on device** (debug build, Google test ad unit).

Return to this checklist **before Play production release** (not required for Sprint 6+ feature work).

**Canonical plan:** [GAMIFICATION_INTEGRATION_PLAN.md](../GAMIFICATION_INTEGRATION_PLAN.md) §10 (economy), §14 (ads), §19 (sprints).

---

## 1. AdMob SSV + Cloud Functions (server-verified gem grants)

### Current (MVP / debug)

- App grants gems locally after `RewardedAdManager` earn callback.
- Idempotency via Room `gem_event` grant keys (e.g. `quest_sims_2026-07-24`).
- **Risk:** Modified clients can fake earn callbacks; gems are client-authoritative.

### Production requirement

- **AdMob Server-Side Verification (SSV):** Google POSTs a signed reward payload to a backend URL on real ad completion.
- **Cloud Functions:** e.g. `admobSsv` webhook → verify signature → `grantAdReward` with idempotency keys.
- Client should request grant from server (or apply server-confirmed result); Room remains cache/offline queue.

### References in repo

- `QuestGemRewardService.kt` — post-ad grant path to replace/wrap
- `GamificationRepository.grantGemsIfEligible()` — local grant today
- Plan: reward integrity note in §10 — SSV **from day one** for ad gems

---

## 2. 2× gems option (two rewarded ads)

### Current

- One ad per quest claim → fixed gems (`EconomyConfig`: 15 / 15 / 30).
- `QuestClaimDialog` — single “Watch short video · +N gems” button.

### Production requirement (prototype rule)

- Optional **2×** payout if user watches **two** ads in sequence (e.g. 15 → 30, 30 → 60 bonus).
- UI: base claim vs 2× option on claim dialog.
- `RewardedAdManager.showSequence()` (or equivalent) — grant only after both earns; partial failure = base grant only per plan.

### References

- Plan §9–10 gem tables; `QuestClaimType.gemAmount()` / `EconomyConfig`

---

## 3. Production rewarded ad unit ID

### Current

- Debug default in `app/build.gradle.kts`:
  `ca-app-pub-3940256099942544/5224354917` (Google **test** unit)
- Prod **app ID** + **banner** exist in `local.properties` / `scripts/admob-firebase-setup.md`
- **Rewarded unit:** not yet created/documented for production

### Production requirement

1. Create **Rewarded** ad unit in AdMob console (app `eduai-e090e` / `ca-app-pub-6484226294015492~5849133177`).
2. Add to `local.properties` (and `local.properties.example`):
   ```properties
   REWARDED_AD_UNIT_ID=ca-app-pub-6484226294015492/XXXXXXXX
   ```
3. Update `scripts/admob-firebase-setup.md` with rewarded unit steps.
4. Verify release build does **not** use sample IDs (`MobileAdsInitializer` warning path).

---

## Verified for Sprint 5 (keep as regression baseline)

- [x] Quest complete → claim dialog → rewarded ad → gems + overlay
- [x] Daily cap (3 quest ads / day) + grant-key idempotency in Room
- [x] Sims / study / bonus claim amounts (15 / 15 / 30)
- [x] Debug **Prepare quest ad test** (Settings → Developer) for QA without full study flow

---

## Suggested order when returning for release

1. Production rewarded ad unit + release config
2. AdMob SSV + Cloud Functions + client migration off local-only grants
3. 2× gems UI + sequential ad flow (optional polish before or shortly after launch)

---

*Last updated: 2026-07-24 — after on-device Sprint 5 verification (CPH2661).*
