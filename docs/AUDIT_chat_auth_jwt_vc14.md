# Audit — Chat auth / “Server error” package (vc14)

**Audited:** 2026-09-06 (working tree + live `/health` + Firestore error pattern)  
**Updated:** 2026-09-16 — live agent-chat “server error” wave **cleared by backend interventions**  
**Target ship:** `versionCode` **14** / `versionName` **1.0.12**  
**Live Play:** older build — JWT/R8 client package is **local only** until vc14 is published and installed

**Live status:** Agent chat is healthy again after **backend** fixes. Do not treat the store APK as still “broken chat” pending vc14.

**Verdict (client package):** The Sep 2026 wave also showed an **app-side Google ID-token decode + refresh** defect on **release/R8** builds (Firestore `TypeReference` / refresh thrash) — not “EC2 down.” Related gaps remain fixed in-tree for vc14 as hardening. Residual risk after both backend + client: real upstream LLM 500s only.

---

## 1. Evidence

| Source | Finding |
|--------|---------|
| Firestore app error logs (OnePlus, ~2026-09-05/06) | Flood: `JwtDecoder` `TypeReference constructed without actual type information`; `ProactiveTokenInterceptor` token still expired; `TokenManager` same-token warnings |
| `GET http://13.48.59.144:8000/health` (audit day) | `status=healthy`, agent `educational_agent_optimized_langsmith_v5` |
| Unauthenticated session start | `401` (auth gate alive) |
| Play vs laptop | Store APK unchanged; fix only in working tree until vc14 |

---

## 2. Chat auth wiring (as implemented)

| Piece | Location / detail |
|-------|-------------------|
| Package | `com.ncert7.aitutorandlab` |
| UI | `ui/screens/chatbotscreen/` (`ChatbotScreen`, `ChatViewModel`) |
| Client | `data/remote/AgenticAIClient.kt` → `AgenticAIService.kt` |
| Base URL | `BuildConfig.AGENTIC_AI_BASE_URL` ← `local.properties` → `http://13.48.59.144:8000` |
| Cleartext | `res/xml/network_security_config.xml` allowlists that IP |
| Attach token | `ProactiveTokenInterceptor` — `Authorization: Bearer` + `X-API-Key` |
| 401 retry | `TokenAuthenticator` (OkHttp `Authenticator`) |
| Client build | `RetrofitProvider` registers interceptor + authenticator |
| Expiry prefs | `SharedPreferenceUtils.isTokenExpiredOrExpiring()` |
| Study-agent copy | `ErrorHandler.getStudyAgentErrorMessage()` → `R.string.error_server_error` |
| Softened EN | “Tutor is busy right now. Please try again.” |
| Softened KN | `values-kn/strings.xml` `error_server_error` (localized) |
| `API_KEYS` | BuildConfig may be empty; chat uses **Firebase Google ID token**, not a static key |

---

## 3. Primary root cause

Release **R8** + dependency **auth0 `java-jwt`** used Jackson `TypeReference` paths that minify stripped. Decode failed → callers treated every token as **expired** → silent refresh thrash on agent calls → flaky chat + Firestore spam. Failed sessions / HTTP 500s surfaced as “Server error.”

EC2 `/health` staying green matches this: host up, app auth path broken.

---

## 4. In-tree fix package (audited present)

| # | Scenario | Status | Code |
|---|----------|--------|------|
| 1 | R8 breaks JWT decode | **Fixed** | `utils/JwtDecoder.kt` — Base64 URL + Gson; **no** `java-jwt` / auth0 in `app/build.gradle.kts` |
| 2 | Decode fail ⇒ expired ⇒ refresh storm | **Fixed** | `JwtDecoder.hasExpClaim()` + stored-expiry fallback in `SharedPreferenceUtils.isTokenExpiredOrExpiring()` |
| 3 | Same JWT after silent refresh treated as failure | **Fixed** | `TokenManager.refreshTokenSilently()`, `ErrorHandler`, `TokenAuthenticator` |
| 4 | No OkHttp 401 recovery | **Fixed** | `data/remote/TokenAuthenticator.kt` + `RetrofitProvider` |
| 5 | 401 refresh only if JWT “looked” expired | **Fixed** | `ErrorHandler` always attempts silent refresh on 401 (capped) |
| 6 | Firestore spam from routine token/HTTP | **Fixed** | Decode silent; routine paths `debugLog`; HTTP interceptor no longer Firestore `logError` |
| 7 | Harsh “Server error.” | **Fixed** | `values/strings.xml`, `values-kn/strings.xml` |
| 8 | Decoder + auth policy unit tests | **Fixed** | `JwtDecoderTest`, `AuthTokenPolicyTest`, `ErrorHandlerAuthTest` |

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.ncert7.aitutorandlab.utils.JwtDecoderTest \
  --tests com.ncert7.aitutorandlab.utils.AuthTokenPolicyTest \
  --tests com.ncert7.aitutorandlab.utils.ErrorHandlerAuthTest
# Last run: 35 tests, 0 failures (12 + 18 + 5)
```

---

## 5. Residual / out of scope

| Item | Notes |
|------|--------|
| Live chat-error wave | **Resolved (backend)** as of 2026-09-16 — not blocked on Play vc14 |
| Play until vc14 ships | Store users still run old JWT/R8 client path; ship package anyway for hardening |
| Real LLM 5xx | Quota / upstream SSL EOF can still 500 after auth is healthy |
| HTTP IP / single EC2 | Infra hardening separate from this bug |
| Deprecated Google Sign-In APIs | Compile warnings only; not this incident |

---

## 6. Ship / QA gates

1. Commit with vc14 ship set (class NFE, KN URL, this auth package, version 14).  
2. Install a **release** AAB (**R8 on**) — debug alone does not prove the JWT fix.  
3. Sign in → concept chat → send after idle (10–60+ min).  
4. Firestore: no new `TypeReference` / “still expired after refresh” storms.  
5. Closed testing → Production as **14**.

---

## 7. Doc index

| Doc | Role |
|-----|------|
| **`docs/AUDIT_chat_auth_jwt_vc14.md`** | **Canonical audit (this file)** |
| `docs/BUGFIX_chat_jwt_decoder_r8.md` | Short pointer → this audit |
| `docs/CHAT_SERVER_ERRORS.md` | Short pointer → this audit |
| `docs/STATUS_play_vc14_coach_language_notifications.md` | Release context (quick-ref links here) |
