# Note: Capture app version on Firestore user docs

**Date:** 2026-09-16  
**Status:** Implemented in working tree (ship with vc14)

**Why:** Ops cannot answer “how many users are on latest (1.0.12 / vc14)?” from Firestore today. `app_version` exists only as a GA4 user property / event param (`FirebaseAnalyticsHelper`). Garden/Space (`onboarding.world`) is only a weak proxy for “saw new onboarding,” not true version.

**Goal:** Persist current install version on `users/{uid}` so scripts and dashboards can count upgrades without Play Console / GA4 scopes.

---

## Schema (on `users/{uid}`)

```text
appVersionName: "1.0.12"          // string — BuildConfig.VERSION_NAME
appVersionCode: 14                // number — BuildConfig.VERSION_CODE
appVersionUpdatedAt: <millis>     // number — when we last wrote these fields
appVersionFirstSeenName: "…"      // set once if missing
appVersionFirstSeenCode: N        // set once if missing
```

Flat fields; always filter `appName == "eduai_app"`.

---

## Implementation

| Piece | Location |
|-------|----------|
| Merge write | `FirebaseRepository.syncAppVersion(userId)` |
| First create stamp | `createNewUser` includes version fields; **existing-user path uses `SetOptions.merge()`** (no bare overwrite) |
| Debounce | `SharedPreferenceUtils` `last_synced_app_version_code` — skip write if == `VERSION_CODE`; cleared on logout |
| Auth | `DataSyncService.onUserAuthenticated` → `syncAppVersionIfNeeded` |
| Foreground | `AppLifecycleObserver.onStart` → `DataSyncService.syncAppVersionIfNeeded()` |

---

## How ops will query after ship

```text
users where appName == eduai_app AND appVersionCode == 14   → on latest
users where appName == eduai_app AND (missing appVersionCode OR appVersionCode < 14)
  → not yet upgraded OR never opened a build that writes version
```

Caveat until adoption: users who never open the new build keep a blank version — count those as “unknown / pre-instrumentation,” not “definitely old.”

---

## Verification checklist

1. Install build with the change → sign in → Firestore user doc shows `appVersionName` / `appVersionCode`.
2. Bump `versionCode` locally → reopen app → fields update; `appVersionUpdatedAt` moves.
3. Confirm merge did not clear `onboarding` / `tutorConfig`.
4. Script: count by `appVersionCode` for `eduai_app`.
5. Play Data Safety: version is app metadata already implied by analytics; no new PII.
