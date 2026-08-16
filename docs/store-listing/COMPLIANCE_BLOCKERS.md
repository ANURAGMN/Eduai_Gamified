# Compliance blockers — re-weighted (not “MVP OK”)

**App:** EduAI · `com.ncert7.aitutorandlab` · v1.0.11 (versionCode **13**)  
**Checklist source:** `Gamification/PLAY_RELEASE_CHECKLIST.md`

This document supersedes the lenient audit. Items here are **fix-before-production** unless marked “defer with accepted risk.”

---

## 🟡 ACCEPTED RISK — this cut (v1.0.11)

### AI “Report” on assistant messages (Play GenAI P0)

**Status:** **Deferred with accepted risk** for the v1.0.11 / vc13 upload (decision **2026-08-16**).

**Gap:** No Report / flag control on AI assistant messages in shared `ConversationView` (chatbot + math agent). Contact Support alone does not satisfy the GenAI policy checklist item.

**Accepted risk:** Possible Play rejection, delayed review, or post-launch policy action until Report ships.

**Mitigations until next release:** monitored `contact@padaams.in` support path; AI-backend content controls; staged rollout with halt if needed.

**Follow-up (required next cut):** long-press or ⋮ **Report** on assistant messages + log event (optional Firestore `reports/`). See `RELEASE_READINESS_v1.0.11.md` §6.1.

---

## 🔴 FIX BEFORE LAUNCH (code + policy)

### 1. Firestore child-data exposure — **FIX IN PROGRESS**

**Was wrongly rated:** ⚠️ “OK for MVP”  
**Correct severity:** 🔴 **Child-data exposure / DPDP-COPPA-GDPR-K risk**

| Collection | Old rule | Risk |
|------------|----------|------|
| `users/{userId}` | `allow read: if true` | World-readable email, name, phone, school, class |
| `simulation_interactions/{studentId}/…` | `allow read: if true` | Student answers, titles, timestamps |
| `friend_codes/{code}` | `allow read: if true` + displayName | PII in public lookup |

**Fix shipped in repo:**
- `firestore.rules` — auth required; `auth_index/{firebaseUid}` maps uid → studentId; owner-only reads/writes.
- `FirebaseAuthBridge.kt` — Google + email Firebase Auth on sign-in.
- `FirebaseRepository.ensureAuthIndex()` — written on login.
- `friend_codes` — **no displayName** stored (code → studentId only).

**Deploy order (mandatory):**
1. Ship app **1.0.10+** with Firebase Auth + `ensureAuthIndex`.
2. Deploy rules: `python scripts/deploy-firestore-rules.py`.
3. Smoke-test login → progress sync → friends → simulation sync on **release** build.

**Residual gap:** Authenticated users can still **read any friend code** (needed for add-friend). Document contains **studentId only**, not email.

---

### 2. Parental consent + Google Sign-In for under-13 — **POTENTIALLY BLOCKING**

**Was wrongly rated:** 🔲 “fill a form”  
**Correct severity:** 🔴 **May require product/legal design change**

The app targets **children**, collects **email, name, profile photo** (Google Sign-In), **analytics**, **AdMob data**, and **chat text** sent to your backend. Under **COPPA (US)** and **India DPDP (under-18 = child)**, verifiable **parental consent** is generally required before collecting personal information from children — unless you qualify for a narrow exception.

Many Families apps **avoid Google Sign-In for child accounts** for this reason and use:
- Parent-gated account creation, or  
- Anonymous / limited local mode, or  
- School/institutional consent (@padaams.in reviewer path is not a general solution)

**Action:** Consult counsel **before** production. Play Console forms alone do not satisfy COPPA/DPDP.

**Data Safety draft:** `docs/store-listing/PLAY_DATA_SAFETY.md`

---

### 3. Microphone — **DECLARE ACCURATELY**

**Finding:** `RECORD_AUDIO` is declared. `SpeechToText` uses **`SpeechRecognizer`** — audio is **not uploaded as a file** by app code; **text transcript** goes to chat/backend.

**Action:**
- Data Safety: declare mic permission; clarify **text derived from speech**, not stored raw audio (see PLAY_DATA_SAFETY §5).
- Extra reviewer scrutiny on kids apps — keep permission request **in context** (only when user taps voice input).

---

### 4. Concept maps without API key — **FIX SHIPPED**

**Was wrongly rated:** ⚠️ optional  
**Correct severity:** 🔴 **Broken UX on a kids’ app**

**Fix:** `ConceptMapFeatureAvailability` + `ResourceDecisionUseCase` — CI→SIM_CC concept map transition **skipped** when `GEMINI_API_KEY` is empty. Tutor chat/sim flow continues without a dead button.

**Alternative for launch:** Add `GEMINI_API_KEY` to release `local.properties` if you want concept maps live.

---

### 5. versionCode must exceed Play — **FIX SHIPPED**

**Was missed:** versionCode **11** may already be uploaded.

**Fix:** **versionCode 12** / **versionName 1.0.10** in `app/build.gradle.kts`.

**Before upload:** Play Console → Release → confirm **latest production versionCode**; bump again if ≥ 12.

---

## 🟡 PLAY CONSOLE / OPS (still blocking upload)

| Item | Status |
|------|--------|
| Closed testing **14 days** (personal dev account) | 🔲 Start now |
| Data Safety form matches app | 🔲 Use PLAY_DATA_SAFETY.md |
| Target audience includes **children** | 🔲 |
| `app-ads.txt` on **same domain** as Play developer website | 🔲 Verify AdMob crawler |
| Release smoke test (signed APK/AAB) | 🔲 Device not connected during audit |
| Pre-launch report | 🔲 After Internal track upload |

---

## 🟢 VERIFIED OK (unchanged)

- TFCD / max rating G / under-age consent (`MobileAdsInitializer`)
- GA4 `allow_ad_personalization_signals=false`
- Gem balance excluded from backup (`backup_rules.xml`)
- Minimal permissions (no location; no QUERY_ALL_PACKAGES)
- AdMob SSV — **defer** with documented client-side gem risk
- Remote Config kill-switch — **nice-to-have**; use staged rollout + halt instead

---

## Deploy commands

```powershell
# Verify config
.\scripts\verify-admob-config.ps1
.\scripts\verify-release-config.ps1

# Build (after local.properties + signing)
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat bundleRelease

# Deploy rules AFTER app with Firebase Auth is live
python scripts/deploy-firestore-rules.py
```

**Upload artifact:** `EduAI-1.0.10-release.aab` (rebuild after version bump)
