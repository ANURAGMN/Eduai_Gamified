# Play Console — Data Safety form (EduAI)

**App:** `com.ncert7.aitutorandlab` · **NCERT Class 7 AI Tutor & Labs**  
**Operator:** PADAAMS · **Audience:** Children (under 13 / under 18 per India DPDP)  
**Last verified against codebase:** 2026-07-30 (v1.0.10 / versionCode 12)

Use this as a **line-by-line draft** when completing [Play Console → App content → Data safety](https://play.google.com/console). Answers must match **actual app behavior** — mismatches are a common rejection/takedown cause.

> **Not legal advice.** Parental consent for Google Sign-In + personal data from minors may require a **product/legal review** (see `COMPLIANCE_BLOCKERS.md`).

---

## 1. Does your app collect or share any of the required user data types?

**Answer: Yes**

---

## 2. Is all of the user data collected by your app encrypted in transit?

**Answer: Yes**

- HTTPS/TLS for Firebase, AdMob, Google Sign-In, and the AI backend (`AGENTIC_AI_BASE_URL`).
- Cleartext HTTP is limited to the configured agent backend IP in `network_security_config.xml` (learning API only).

---

## 3. Do you provide a way for users to request that their data is deleted?

**Answer: Yes** (with caveat)

- Privacy policy: email **contact@padaams.in** for deletion requests.
- Uninstall removes local Room database and SharedPreferences (except what Google/Firebase retain per their policies).
- **Improvement:** add in-app “Request account deletion” before scale.

---

## 4. Data types — declare each that applies

### Personal info

| Play data type | Collected? | Shared? | Purpose | Required or optional | Processed ephemerally? |
|----------------|------------|---------|---------|----------------------|-------------------------|
| **Name** | Yes | Yes (Firebase, AI backend) | Account, display in app, friend features | Required for sign-in | No |
| **Email address** | Yes | Yes (Firebase, Google Sign-In, AI backend auth) | Account identification, sync, support | Required for Google / @padaams.in sign-in | No |
| **User IDs** | Yes | Yes (Firebase, Crashlytics, Analytics, AdMob) | Account, progress sync, debugging, child-safe ads | Required | No |
| **Phone number** | Optional | Yes (Firebase if user enters in profile) | Profile / support | Optional (profile form) | No |

**Notes for reviewer:**
- Google Sign-In provides name, email, profile photo URL.
- @padaams.in in-app sign-in uses email + shared review password (Play testing).
- Stored in Firestore `users/{studentId}` and local Room `student` table.

### Financial info

**None**

### Health and fitness

**None**

### Messages

| Type | Collected? | Notes |
|------|------------|-------|
| Other in-app messages | Yes | Tutor chat text sent to **your AI backend** for educational responses; stored in session history locally and synced per backend/Firestore session paths where applicable. **Not human-to-human messaging.** |

### Photos and videos

| Type | Collected? | Notes |
|------|------------|-------|
| Photos | Optional | Profile photo **URL** from Google account (not uploaded gallery photos by default). |

### Audio files

| Type | Collected? | Notes |
|------|------------|-------|
| **Voice or sound recordings** | **No (stored)** | See **Microphone** below — important distinction. |

### Files and docs

**None** (NCERT content is app-delivered / Firestore syllabus, not user uploads)

### Calendar / Contacts / Location

**None** — app does **not** request location permissions.

### App activity

| Play data type | Collected? | Shared? | Purpose |
|----------------|------------|---------|---------|
| App interactions | Yes | Yes (Firebase Analytics GA4) | Product analytics: screens, clicks, funnel, gamification events |
| In-app search history | No | — | — |
| Installed apps | No | — | — |
| Other user-generated content | Yes | Yes (Firebase, backend) | Simulation interaction logs, progress, quest/plan state |
| Other actions | Yes | Yes | Ad impressions/clicks (AdMob), crash events (Crashlytics) |

**Analytics detail:**
- GA4 events via `FirebaseAnalyticsHelper` (screen time, nav, quests, ads, etc.).
- `allow_ad_personalization_signals=false` user property set in code.
- High-frequency analytics **not** mirrored to Firestore (`AnalyticsFirestoreMirror.ENABLED = false`).

### Web browsing

**None**

### App info and performance

| Type | Collected? | Shared? | Purpose |
|------|------------|---------|
| Crash logs | Yes | Yes (Firebase Crashlytics) | Stability |
| Diagnostics | Yes | Yes (Crashlytics, optional Firestore `errors/` logs) | Debug production issues |
| Other app performance data | Yes | Yes (Analytics) | Usage patterns |

### Device or other IDs

| Type | Collected? | Shared? | Purpose |
|------|------------|---------|
| **Device or other IDs** | **Yes** | **Yes (Google/AdMob/Firebase)** | **Child-safe ads (non-personalized)**, analytics, Firebase installations |

**Declare honestly:** AdMob and Firebase Analytics may use advertising/analytics identifiers even when ads are tagged **child-directed (TFCD)** and **non-personalized**. Do **not** answer “No” for device IDs if AdMob is enabled.

---

## 5. Microphone (`RECORD_AUDIO`) — declare carefully

| Question | Answer |
|----------|--------|
| Permission used? | **Yes** — `RECORD_AUDIO` in manifest |
| Audio transmitted off-device? | **No raw audio uploads** in current code |
| How it works | Android **`SpeechRecognizer`** (on-device/OS speech service) converts speech to **text**; only the **transcript string** is sent to the tutor chat / AI backend |
| Stored? | Transcript in chat session locally; may be included in backend session logs per agent API |
| Data Safety type | Declare **microphone** permission under **App permissions**; for “Audio” collection select **No** for stored recordings if you only send derived text — **confirm with Play form wording** (some reviewers expect “Audio” if mic permission exists; add explanation in “Security practices” notes) |

**Reviewer note text (paste in Data safety → Details):**  
> Voice input uses the system speech recognizer. Audio is processed to text on the device/OS layer; EduAI does not upload raw audio recordings. Only the resulting text is sent to our tutoring backend.

---

## 6. Third parties — data is shared with

| Third party | Data shared | Purpose |
|-------------|-------------|---------|
| **Google Firebase** (Auth, Firestore, Analytics, Crashlytics) | Account, progress, analytics, crashes | Backend sync, metrics, stability |
| **Google AdMob** | Device/ad identifiers, ad interaction signals | Child-directed **non-personalized** banner & rewarded ads |
| **Google Sign-In** | OAuth tokens, email, name, photo URL | Authentication |
| **PADAAMS AI backend** (EC2 agent API) | Google ID token, chat text, session metadata, progress-related context | Tutoring, simulations, math/revision agents |

**Not sold:** Select **No** for “sale of data” (unless business model changes).

---

## 7. Target audience & Families

| Console section | Recommended answer |
|-----------------|-------------------|
| Target age groups | Include **children** (under 13) and applicable teen bands per your marketing |
| Designed for Families | **Yes** / comply with Families policy |
| Ads | **Yes, contains ads** |
| Ad personalization | **No** (TFCD + under-age tags in `MobileAdsInitializer`; GA4 `allow_ad_personalization_signals=false`) |
| Sign-in | **Yes** — Google Sign-In and institutional email |

---

## 8. Security practices (short text for Console)

- Data encrypted in transit (TLS).
- Child-directed ad configuration (TFCD, max content rating G).
- Firestore rules require Firebase Authentication + per-user `auth_index` mapping (see `firestore.rules`).
- Gem/progress Room DB excluded from Android auto-backup.
- Privacy policy: https://anuragmn.github.io/privacy-policy.html

---

## 9. Common mistakes to avoid

1. Saying **“No device IDs”** while AdMob + Firebase Analytics are enabled.  
2. Omitting **email/name** while Google Sign-In is required.  
3. Omitting **microphone** while `RECORD_AUDIO` is declared.  
4. Target audience **not** including children while marketing an NCERT Class 7 app.  
5. Privacy policy URL in Console **different** from in-app `strings.xml` URL.

---

## 10. Pre-submit checklist

- [ ] Every “Yes” above matches a row in Play’s Data safety wizard  
- [ ] Privacy policy updated to mention mic → text behavior (already in policy; verify)  
- [ ] Ads declaration = Contains ads  
- [ ] Account deletion contact visible in policy  
- [ ] Counsel review for **parental consent + Google Sign-In for under-13** (see compliance doc)
