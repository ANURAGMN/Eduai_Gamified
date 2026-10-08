# Cursor: two small hardening changes for vc14 + audit sign-off

**Date:** 2026-09-06 · uncommitted, in working tree. Fold into the vc14 ship set.

## 1. Chat-auth audit — reviewed, accurate
`docs/AUDIT_chat_auth_jwt_vc14.md` checks out against the code: `TokenAuthenticator` wired in
`RetrofitProvider`, `JwtDecoder.hasExpClaim()` + `SharedPreferenceUtils.isTokenExpiredOrExpiring()`
present, softened `error_server_error` (EN + KN), no `auth0`/`java-jwt` in `build.gradle.kts`. Ship-ready
as the canonical reference. Only real caveat: the fix is release/R8-behavioural, so the §6 QA (install a
**release** AAB, sign in, chat after idle, confirm no `TypeReference` / still-expired-after-refresh storms)
must actually run before promoting — debug won't prove it.

## 2. Two targeted hardening edits I made (safe, additive)
From the app-wide sweep for the two proven bug-classes (locale parse, R8/reflection). The sweep found the
code otherwise well-guarded — these are the only two worth acting on:

- **`ui/screens/chatbotscreen/utility/GraphRenderLogic.kt`** — the one genuinely-unguarded collection access.
  `graphData.nodes.first()` (empty-graph → `NoSuchElementException`) → `rootNodes.ifEmpty { graphData.nodes.take(1) }`.
  Same behaviour for non-empty graphs; can't crash on an empty one.
- **`app/proguard-rules.pro`** — keep enum constant names for the two enums read back via `valueOf(name)`:
  `com.anurag.eduai.uikit.avatar.core.TutorCharacter` (Firestore tutor config) and
  `com.ncert7.aitutorandlab.notification.NotificationEvalTrigger` (WorkManager inputData). Both call sites
  use `runCatching`, so this fixes silent release-only *fallback*, not a crash.

## Not done (deliberately)
Did **not** touch the other ~90 `!!` / `.first()` sites — verified the top ones are guarded
(`isNotEmpty()`/`ifEmpty`/`size` checks/derived-filters/try-catch) or are `Flow.first()`. Mass-editing them
is churn/regression risk with no signal they fire; let Crashlytics rank any real ones. Broader resilience
audit (offline, cold-state, low-end, Play data-safety) not run — available on request.

## Verify
- `./gradlew :app:compileDebugKotlin` (couldn't compile in the doc environment).
- Both edits are brace/paren balanced.
