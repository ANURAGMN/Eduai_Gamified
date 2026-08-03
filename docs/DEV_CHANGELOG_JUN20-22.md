# EduAI — Dev Changelog (Fri 20 Jun – Wed 25 Jun 2026)

Internal reference for changes made during the pre-launch / MVP test sprint and **Play Production resubmit (1.0.7)**.  
**Repo:** https://github.com/ANURAGMN/EduAI_app.git  
**Branch:** `main`  
**Related doc:** [APP_STRUCTURE.md](./APP_STRUCTURE.md)

### Ship status (25 Jun 2026)

| Item | Status |
|------|--------|
| **Play Production AAB** | **Uploaded** — `1.0.7` / versionCode **9** |
| **GitHub `main`** | **Pushed** — commit `51abd2d` |
| **AdMob + Play policy** | Child-safe SDK + console questionnaire / blocking updated |
| **Device tested** | Oppo CPH2661 — login, ads, funnel verified |

---

## Summary timeline

| Date | Commit | Theme |
|------|--------|--------|
| 2026-06-20 | `b8eaa4f` | Analytics, AdMob, Firestore tooling, SDK bump, progress/language fixes |
| 2026-06-20 | `181ef1e` | Team onboarding doc (`APP_STRUCTURE.md`, README) |
| 2026-06-22 | `d2c9812` | Locale-aware UI labels; remove agent debug overlay |
| 2026-06-22 | `e5f61c9` | Bilingual loading quotes on agent “thinking” states |
| 2026-06-25 | `51abd2d` | **Release 1.0.7** — Play resubmit: login fixes, child-safe ads, funnel analytics |

**Test device:** Oppo CPH2661 (`123249b7`)  
**Test user:** `mail2anuragmn@gmail.com`  
**Firebase project:** `eduai-e090e`

---

## Feature perspective (product view)

This section describes **what changed for users and product goals**. Technical implementation details follow in later sections.

### Feature map (Fri–Sun sprint)

| Feature area | User-visible outcome | Product goal |
|--------------|---------------------|--------------|
| **Ads** | First 5 learning taps/day free; banner on 6th+ | Monetize without blocking first sessions |
| **Analytics** | Every lesson/subject/chapter/sim tap tracked | MVP metrics for ~100-user test |
| **DB / progress** | Correct counts per language; faster reactive UI | Trustworthy home & progress screens |
| **Home screen** | Today’s lessons, streak, subject, sims — all locale-correct | Daily return habit, bilingual NCERT Class 7 |
| **Sync** | Progress & clicks appear in Firestore within ~100ms | Cross-device backup + team dashboards |
| **Agent UX** | No debug overlay; loading quotes while thinking | Polish for closed/production test |
| **Locale** | EN ↔ KN switch updates all lists and headings | Karnataka + English learners on one app |

### Analytics — bugs fixed (feature view)

| # | Bug (what users/devs saw) | Root cause | How we fixed it |
|---|---------------------------|------------|-----------------|
| 1 | **No visibility** into which lessons/simulations users opened during MVP test | Only basic screen tracking; no structured click events | **`ContentClickAnalyticsTracker`**, **`SimulationAnalyticsTracker`**, wired through **`ContentClickNavigation`** on all nav paths |
| 2 | **Simulation metrics incomplete** — opens vs completes not separated | No CLICK/COMPLETE distinction | **`SimulationInteraction`** enum + GA4 events via **`FirebaseAnalyticsHelper`** |
| 3 | **Firestore analytics docs** lacked item context | Room v1 `app_analytics` schema too thin | Room **v2 migration** + **`AnalyticsFirestorePayload`** with `conceptId`, `source`, `interactionType` |
| 4 | **Ad click count** out of sync with analytics | Separate simulation counter in prefs | Ad policy reads same **`CLICK`** events as analytics (`ClickAdGate` → **`AppAnalyticsDao`**) |

---

### Ads — feature view

#### What the student experiences

1. Student opens app and taps through lessons, chapters, simulations, subjects — **no ad** for the first **5 navigations per calendar day**.
2. On the **6th tap onward** (same day), a **banner ad dialog** appears **before** the next screen opens.
3. After dismissing the ad, navigation continues normally.
4. Counter **resets at midnight** (device local date).

#### What counts as a “click” (same counter as analytics)

All tracked **content navigation clicks**, not only simulation opens:

| User action | Counts toward daily limit? |
|-------------|---------------------------|
| Tap lesson on **Home** | Yes |
| Open **subject** | Yes |
| Open **chapter** (study / math / simulation list) | Yes |
| Open **concept / lesson** from list | Yes |
| Open **revision** | Yes |
| Open **simulation agent** or simulation URL | Yes (via simulation analytics CLICK) |
| Switch language, settings, scroll home | No |
| View ad itself | No |

This aligns **monetization with engagement**: active learners see ads after meaningful use, not on app open.

#### Where the ad appears

- **Navigation gate** — when user tries to go to the next learning screen (`NavigationAdGate` in `BottomNavBar` / `LearningNavigator`).
- **Not** inside the simulation WebView or mid-lesson — avoids interrupting an active simulation and fixed a bug where the WebView loaded before the ad check.

#### Why we chose 5 free clicks

- Enough for: pick subject → chapter → 2–3 lessons without friction.
- Closed-test feedback: simulation-only gating was too narrow; product wanted **any learning navigation** to count.
- Policy is **configurable** via `ClickAdPolicy.FREE_CLICKS_PER_DAY` (currently `5`).

#### Monetization stack (non-code)

- Production AdMob banner units in `local.properties`
- Firebase ↔ AdMob link for revenue analytics
- `app-ads.txt` on https://anuragmn.github.io/app-ads.txt
- Play Console production access obtained; closed test completed on earlier build

#### Ads timeline: before vs after Jun 20

**Partially yes** — ads were in the app before the Jun 20 sprint (`b8eaa4f`), but they were **simulation-only**, **inconsistent**, and had UX bugs. The sprint work is what made them reliable and aligned with product policy.

##### What existed before (pre–Jun 20 / closed-test builds)

Core AdMob stack was already in the repo:

| Component | Pre–Jun 20 state |
|-----------|------------------|
| `AdManager`, `BannerAdView`, `AdDialog` | Present |
| `MobileAds.initialize()` in `MainActivity` | Present |
| Ads SDK | `play-services-ads:22.6.0` |
| Ad unit IDs | Google **test** IDs in `local.properties` by default |

Banner ads **could load and display** (usually as test ads, unless production IDs were configured locally).

##### What triggered ads before (old behavior)

Ads only fired on **simulation URL** flows — not general learning navigation:

| Path | Ad behavior (before) |
|------|----------------------|
| **Concept list → Simulation URL** | SharedPrefs daily **open** count — first 5 opens free, 6th+ showed `AdDialog` |
| **Simulation WebView viewer** | Separate check on **today’s completed simulations** in DB (5 free, 6th+ ad) |
| **Simulation agent** | **No ad** from concept list — navigated straight through |
| **Home lessons, subjects, chapters, study concepts** | **No ads** |

So ads were **not** on “every 6th learning tap” — only on some simulation paths, with **two different counters**.

##### Why they felt broken or “not working”

1. **Dual gate logic** — concept list counted **opens** (SharedPrefs); viewer counted **completions** (Room DB).
2. **WebView race** — viewer started with `showAdBeforeSimulation = false`, so WebView often **loaded before** the async ad check finished.
3. **Home / practice sim cards** — could bypass concept-list ad logic depending on navigation path.
4. **Production vs test** — builds defaulted to Google test ad units; real revenue needed production IDs + AdMob console setup (done during sprint).
5. **SDK bump blocked builds** — Ads SDK 25.x failed on Kotlin 2.0 until dependency fixes in `b8eaa4f`.

##### What changed in the Jun 20 sprint (`b8eaa4f`)

| Change | Effect |
|--------|--------|
| **`ClickAdPolicy`** | Unified rule: 5 free **content clicks/day**, 6th+ shows ad |
| **`AppAnalyticsDao` click counter** | Same **`CLICK`** events as analytics — no separate SharedPrefs simulation counter for policy |
| **`NavigationAdGate`** | Ad **before** navigation, not inside WebView |
| **SDK 25.4.0** + **`MobileAdsInitializer`** | Test device config + clearer logging |
| **Setup scripts** | `admob-firebase-setup.md`, `verify-admob-config.ps1` |

##### Quick reference

| Question | Before Jun 20 | After Jun 20 |
|----------|---------------|--------------|
| AdMob integrated? | Yes | Yes |
| Test banners could show? | Yes, on simulation paths | Yes, on all gated navigation |
| Production/revenue ads in closed test? | Unlikely unless IDs configured locally | Production IDs in `local.properties` + app-ads.txt |
| Policy matches product intent? | No (simulation-only, dual counters) | Yes (5 free learning taps, unified counter) |

#### Bugs that existed & how we fixed them

| # | Bug (what users/devs saw) | Root cause | How we fixed it |
|---|---------------------------|------------|-----------------|
| 1 | Simulation WebView **loaded before** the ad dialog; user saw content then got interrupted | Ad gate ran inside `ConceptSimulationViewer` **after** WebView init | Moved gate to **`NavigationAdGate`** at nav time — ad shows **before** destination screen opens |
| 2 | Ad policy only counted **simulation opens**; tapping subjects/chapters/lessons never triggered ads | Early `SimulationAdPolicy` / simulation-only counter in SharedPreferences | Unified to **`ClickAdPolicy`** — counts all **`CLICK`** rows in `app_analytics` via `AppAnalyticsDao.getTodayClickCount()` |
| 3 | **Double ad checks** on some simulation paths (nav gate + viewer gate) | Both `NavigationAdGate` and `ConceptViewModel` / viewer ran `ClickAdGate` | Viewer gate removed; `ConceptSimulationViewModel.initializeSimulationWithAdCheck()` only prepares state; ad runs once at navigation |
| 4 | Ad could appear **mid-lesson** inside an active simulation | Gate tied to viewer lifecycle, not user intent to navigate | Gate only on **learning navigation** taps in `BottomNavBar` / `LearningNavigator` |
| 5 | **Build failed** when adding Ads SDK 25.x | Kotlin 2.0 metadata mismatch with Ads library | Bumped **Kotlin 2.2.21**, **KSP 2.2.21-2.0.5**, pinned **Hilt 2.57.1** |
| 6 | `verify-admob-config.ps1` failed on Windows | PowerShell encoding / Unicode in script | Rewrote script **ASCII-only** |

---

### DB optimization — feature view

#### Problems users felt before

- **Today’s Progress** on home showed **wrong language** progress after switching EN/Kannada.
- **Concept vs simulation counts** could disagree between Home and Progress tab.
- Old installs had progress rows labeled `English` / `Kannada` / empty — queries missed or double-counted.
- Home lists felt **stale** after locale change until app restart.

#### What we optimized (user outcome)

| Optimization | Benefit to user |
|--------------|-----------------|
| **Language-scoped progress** (`en` / `kn` per row) | English and Kannada progress tracked separately — switching language shows the right history |
| **Unique index** on `(studentId, itemType, itemId, language, appName)` | No duplicate progress rows; consistent completion state |
| **Reactive Flow queries** in `HomeViewModel` | Home updates live when a lesson is completed without manual refresh |
| **Shared totals with Progress tab** | Same DAO methods for “today completed” and all-time counts — numbers match across screens |
| **Room schema v2** (`app_analytics` + `conceptId`, `source`, `interactionType`) | Richer click analytics without losing old screen-tracking data |
| **Legacy migration on app start** | Existing testers keep progress; old rows mapped to `en`/`kn` |
| **Curated home lists** (in-progress first, then recent completed, max 4) | Home shows **actionable** next lessons, not random syllabus order |
| **Default first-unit content** for new users | Empty state still shows 4 starter concepts/simulations from chapter 1 |

#### Data model (conceptual)

```
Student
  └── Progress (per item + language + app)
        CONCEPT / SIMULATION / SIMULATION_AGENT / …
        status: NOT_STARTED | IN_PROGRESS | COMPLETED
        language: en | kn
```

Analytics clicks stored separately in `app_analytics` — used for **ads policy** and **Firestore dashboards**, not mixed into completion %.

#### Bugs that existed & how we fixed them

| # | Bug (what users/devs saw) | Root cause | How we fixed it |
|---|---------------------------|------------|-----------------|
| 1 | **Today’s Simulation count stayed 0** after completing simulations | Progress saved as `language = "English"` but Home queried `language = "en"` | **`normalizeLanguageCode()`** + **`resolveProgressLanguage()`** on all writes; DAO queries accept legacy aliases |
| 2 | **Today’s Progress wrong language** after EN ↔ KN switch | Queries used current UI code but rows mixed `English`/`Kannada`/`en`/`kn`/empty | Language-scoped rows; **`legacyProgressLanguageAlias()`** in `ProgressDao`; startup migration in **`EduAiApplication`** |
| 3 | **Home vs Progress tab numbers disagreed** | Different query paths; Home used today+language, Progress used all-time; some rows invisible to one query | Shared normalization; Home and Progress both filter by **active language**; documented that Progress cards are **all-time** vs Home **today-only** |
| 4 | **Duplicate progress rows** for same lesson in EN and KN | No unique constraint; updates could insert second row | Unique index on `(studentId, itemType, itemId, language, appName)`; **`updateProgressStatus`** merges legacy rows |
| 5 | **EN completion overwrote KN** (or vice versa) in cloud/local | Single doc key `{type}_{id}` without language | Firestore + Room treat **`en` and `kn` as separate progress** per item |
| 6 | Home lists **stale until app restart** after completing a lesson | One-shot loads; no reactive subscription | **`Flow`** queries + **`collectLatest`** on `_currentLanguage` in **`HomeViewModel`** |
| 7 | Old tester DB had **`English`/`Kannada` rows** that queries skipped | Pre-normalization schema | **`markDuplicateLegacyEnglishProgress()`** / Kannada equivalent; map to `legacy` or migrate to `en`/`kn` on update |
| 8 | **Room v1 analytics** lacked click context for dashboards | `app_analytics` had no `conceptId`, `source`, `interactionType` | **Migration 1→2** via **`DatabaseMigrations.kt`**; extended **`AppAnalyticsEntity`** |

---

### Home screen — feature view

#### Sections on Home (what each does now)

| UI block | Feature behavior |
|----------|------------------|
| **Greeting + name** | Time-based greeting in **current app language** (`Good Evening` / `ಶುಭ ಸಂಜೆ`) |
| **Current subject card** | Shows subject name in active locale; stored internally by **subject ID** (Science/Math UUID), not cached text |
| **Day streak** | Reactive streak from `StreakRepository` |
| **Today’s Progress** | Two counters: concepts completed **today** + simulations completed **today** (language-filtered) |
| **Lesson cards** | Up to 4 concepts: in-progress first, then recent completed; titles in **active language** |
| **Practice Simulations** | Up to 4 simulation items; correct **Kannada vs English** simulation URL/ID |
| **View all chapters** | Navigates using **subject ID** (no hardcoded English/Kannada name check) |

#### Related screens (same sprint)

Home fixes extended to **Subject list**, **Chapter list**, **Concept/Simulation lists**, and **Progress tab** chapter dropdown — same language-reload pattern.

#### Bugs that existed & how we fixed them

| # | Bug (what users/devs saw) | Root cause | How we fixed it |
|---|---------------------------|------------|-----------------|
| 1 | **Subject name stuck** in old language (e.g. “Math” in Kannada UI) | Subject stored as localized **display string** in SharedPreferences, not stable ID | **`setSubjectSelectionId()`** / **`getSubjectSelectionId()`** + **`SubjectIds`**; **`resolveStoredSubjectId()`** migrates legacy `"Science"`/`"ವಿಜ್ಞಾನ"` |
| 2 | **Chapter & concept titles** mixed EN/KN after locale switch | **`getLocalizedName()`** called **`isKannada()`** → not Compose state → no recomposition | **`getLocalizedName(languageCode)`** with explicit param; pass **`LocalConfiguration`** language into cards |
| 3 | **Greeting always English** (“Good Evening” in Kannada mode) | Hardcoded string in **`HomeViewModel`** | **`rememberTimeBasedGreeting()`** using **`R.string.good_morning`** etc. in **`values-kn`** |
| 4 | **“View all chapters”** broken or wrong subject after language change | Compared subject to hardcoded `"Science"` / `"ವಿಜ್ಞಾನ"` strings | Navigate using **`getSubjectSelectionId()`** only |
| 5 | **Lesson/simulation cards** did not refresh on Settings language change | ViewModels cached names at first load | **`LaunchedEffect(currentLanguage)`** on Home, Subject, Chapter, Concept, Progress; **`ChapterViewModel.loadChapters(id, lang)`** |
| 6 | **Practice Simulations** opened English URL in Kannada mode | **`ConceptViewModel`** used global **`isKannada()`** instead of active language | **`isKannadaLanguage(lang)`** + reload concept list on language Flow |
| 7 | **Today’s Progress counters** showed 0 or wrong totals | See DB bugs #1–2 — language mismatch on **`ProgressDao`** today queries | Fixed in shared DB layer; Home passes **`currentLanguage`** into **`TodaysProgressCard`** |
| 8 | **Progress tab chapter dropdown** labels wrong language | Same non-reactive localization pattern | **`SkillsProgressSection`** + **`LaunchedEffect(currentLanguage)`** on **`ProgressScreen`** |
| 9 | **Subject list screen** headings not updating | Subject/chapter entities read once without language param | **`SubjectViewModel`** + screen reload hooks same as Home |

---

### Sync — feature view

#### What syncs

| Data | User benefit |
|------|--------------|
| **Progress** (lesson/sim completion) | Resume on new device; team can see completion in Firestore |
| **Analytics clicks** | Ad policy audit trail + engagement metrics |
| **Sessions** | Session length / app opens for retention |
| **Streaks** | Streak preserved across reinstall if restored from cloud |
| **Chapter agent progress** | Agent session state per chapter |

#### How it behaves

1. **Local-first** — Room is source of truth on device; UI reads from DB immediately.
2. **Real-time push** — after write, `ProgressAnalyticsSessionSyncManager` syncs unsynced rows to Firestore (~100ms in testing).
3. **App isolation** — all paths use `AppConfig.APP_NAME` (`eduai_app`) so EduAI data does not collide with other apps on shared Firebase project `eduai-e090e`.
4. **Document IDs** — predictable keys e.g. `CONCEPT_{conceptId}_kn` for progress restore scripts.
5. **Offline** — rows marked `isSynced = false`; batch sync on next opportunity (WorkManager / sync manager).
6. **Batch size 100** — large backlogs upload in chunks without blocking UI.

#### Firestore layout (for ops / scripts)

```
progress/eduai_app_{email}/records/{type}_{id}_{en|kn}
analytics/eduai_app_{email}/events/{analyticsId}
sessions/eduai_app_{email}/records/...
```

#### Team tooling (same sprint)

- `query-firestore-analytics.js` — per-user click timeline
- `query-firestore-progress.js` — completion records
- `metrics-retention-dau.js --html` — DAU / retention dashboard for MVP review
- Migration scripts for legacy progress language cleanup in Firestore

#### Sync + language interaction

- New progress writes always include explicit `en` or `kn`.
- Restore from Firestore uses `resolveProgressLanguageFromFirestore` for old docs missing language field.
- Home and Progress screens read **only** the active language slice — sync does not merge languages in the UI.

#### Bugs that existed & how we fixed them

| # | Bug (what users/devs saw) | Root cause | How we fixed it |
|---|---------------------------|------------|-----------------|
| 1 | **`language` field missing** in Firestore progress docs | Batch upload map omitted language | All uploads use **`FirestoreSyncUtils.progressRecordPayload()`** with normalized **`en`/`kn`** |
| 2 | **English progress overwrote Kannada** (same lesson, two languages) | Doc ID was `{itemType}_{itemId}` with no language suffix | Doc ID now **`{itemType}_{itemId}_{en\|kn}`** via **`progressRecordDocId()`** |
| 3 | **Real-time sync wrote to wrong Firestore path** | `syncProgressUpdate` used `progress/{email}/...` instead of app-scoped path | All paths use **`studentAppDocId`** → `progress/eduai_app_{email}/...` |
| 4 | **Analytics & sessions** same wrong parent doc | Realtime sync used raw email as doc id | Fixed to **`eduai_app_{studentId}`** for analytics, sessions, streak, chapter progress |
| 5 | **Simulations missing after reinstall** | Data synced to wrong path; restore reads **`eduai_app_{email}`** only | Unified read/write paths in **`ProgressAnalyticsSessionSyncManager`** + **`FirebaseSyncManager`** |
| 6 | **New users did not restore cloud progress** on first login | **`submitNewUser()`** skipped **`syncUserProgress()`** | Restore/sync invoked for new and returning users on login |
| 7 | **Legacy Firestore docs** with `English`/`Kannada`/empty language | Pre-normalization uploads | **`resolveProgressLanguageFromFirestore()`** on restore; migration scripts for cloud cleanup |
| 8 | **Offline progress stuck** unsynced | No batch retry for large backlogs | **`isSynced = false`** flag + **`syncAllUnsyncedData()`** in chunks of **100** |
| 9 | **Repo `firestore.rules` deny-all** — risk if deployed | Template rules block client SDK | Documented: **do not deploy deny-all** while app uses client Firestore sync |

---

### Agent & loading UX (feature view)

| Change | User experience |
|--------|-----------------|
| Removed debug overlay | No “From node / To node” text on chatbot |
| Loading quotes (20, EN+KN) | Spinner + scientist/leader quote while agent thinks |
| Wired on chat, math, revision, simulation agents | Consistent “While you wait” moment across agents |

#### Bugs that existed & how we fixed them

| # | Bug (what users/devs saw) | Root cause | How we fixed it |
|---|---------------------------|------------|-----------------|
| 1 | **Debug overlay** on chatbot: “From node / To node / imager url / Concept Map” visible to testers | **`LogOverlay`** left enabled on **`ChatbotScreen`** for agent graph debugging | Removed **`LogOverlay`** from production chatbot UI (`d2c9812`) |
| 2 | Long **blank spinner** during agent “thinking” (2–10s) | Loading UI was generic status text only | Added **`LoadingInsightPanel`** + **`LoadingQuotes.kt`** (20 EN/KN quotes) on chat, math, revision, simulation agents (`e5f61c9`) |
| 3 | **`MathViewModel` build failure** blocked install during sprint | Missing comma in Hilt constructor parameter list | Fixed constructor syntax so debug APK could ship to Oppo for QA |

---

## Technical deep dive

The sections below document **implementation files, commits, and dev troubleshooting**.

---

## 1. Analytics & Firestore real-time sync (`b8eaa4f`)

### Why

- MVP test with ~100 users needed **click/simulation telemetry** without waiting for Play Console alone.
- Product wanted: simulation clicks, content clicks, viewer time, DAU/retention visibility.
- Progress and analytics should sync to Firestore in near real time for scripts/dashboards.

### What was added

| Area | Files / components |
|------|---------------------|
| Content clicks | `ContentClickAnalyticsTracker`, `ContentClickNavigation` |
| Simulation clicks | `SimulationAnalyticsTracker` (CLICK / COMPLETE) |
| Unified recorder | `AnalyticsEventRecorder` → Room + Firestore |
| GA4 | `FirebaseAnalyticsHelper` (`content_click`, `simulation_click`, `simulation_complete`) |
| Simulation viewer time | `ConceptSimulationViewer` ENTRY/EXIT + duration |
| Room v1→v2 | `DatabaseMigrations.kt`, extended `AppAnalyticsEntity` (`conceptId`, `source`, `interactionType`) |
| Navigation wiring | `BottomNavBar.kt`, `LearningNavigator.kt` |
| Firestore sync | `ProgressAnalyticsSessionSyncManager`, `FirebaseSyncManager`, `FirestoreSyncUtils.kt` |
| Config | `firebase.json`, `firestore.rules`, `firestore.indexes.json`, `.firebaserc` |

### Firestore paths

```
analytics/eduai_app_{email}/events/{analyticsId}
progress/eduai_app_{email}/records/{type}_{id}_{en|kn}
sessions/eduai_app_{email}/records/...
```

### Scripts (Node / PowerShell)

| Script | Purpose |
|--------|---------|
| `scripts/query-firestore-analytics.js` | Per-user click/simulation events |
| `scripts/query-firestore-progress.js` | Progress records |
| `scripts/metrics-retention-dau.js` | DAU, retention; `--html` for dashboard |
| `scripts/run-dashboard.ps1` | Wrapper for HTML dashboard |
| `scripts/migrate-legacy-firestore-progress.js` | Legacy progress language migration |
| `scripts/delete-legacy-firestore-progress.js` | Cleanup old docs |
| `scripts/delete-orphaned-firestore-progress.js` | Orphan cleanup |

### Unit tests added

- `ClickAdPolicyTest`
- `ContentClickNavigationTest`
- `AnalyticsFirestorePayloadTest`
- `FirestoreSyncUtilsTest`
- `ProgressLanguageRestoreTest`

---

## 2. AdMob monetization (`b8eaa4f`)

### Policy implemented

**First 5 content navigations per day are ad-free; 6th+ shows banner ad** before navigation proceeds.

| Component | Role |
|-----------|------|
| `ClickAdPolicy` | Daily threshold logic (`FREE_CLICKS_PER_DAY = 5`) |
| `ClickAdGate` | Reads today’s click count from **`AppAnalyticsDao`** (CLICK events) |
| `NavigationAdGate` | Gate at **navigation time** (not inside simulation viewer) |
| `AdDialog`, `BannerAdView`, `AdManager` | UI + load/show |
| `MobileAdsInitializer` | Init + test vs production ID logging |

### Why gate at navigation

Earlier approach gated inside `ConceptSimulationViewer` after WebView init → race where simulation loaded before ad check. Moving gate to nav time fixed UX and duplicate checks.

### Production IDs

Stored in **gitignored** `local.properties` (see `local.properties.example`).  
Publisher ID: `6484226294015492`  
Setup guide: `scripts/admob-firebase-setup.md`  
Verify script: `scripts/verify-admob-config.ps1`

### Manual console steps (not in code)

- Link AdMob ↔ Firebase `eduai-e090e`
- AdMob payments (payee, tax, bank)
- Play Console ↔ AdMob link
- **app-ads.txt** hosted at https://anuragmn.github.io/app-ads.txt (separate `ANURAGMN.github.io` repo)
- Google Search Console verification file on same Pages site

---

## 3. Progress & language sync fixes (`b8eaa4f`)

### Problems

- Today’s Progress on home could show wrong language or stale counts after locale switch.
- Firestore/legacy rows used mixed language codes (`English`, `Kannada`, empty, `en`, `kn`).
- Profile language from Firebase did not always match Room progress queries.

### Fixes

| File | Change |
|------|--------|
| `LocalizationUtils.kt` | `normalizeLanguageCode`, `resolveProgressLanguage`, legacy aliases |
| `ProgressEventTracker.kt` | Always write explicit `en`/`kn` on new progress |
| `ProgressDao.kt` | Language-aware queries; legacy alias support |
| `SharedPreferenceUtils.kt` | Normalized language preference |
| `EduAiApplication.kt` | Legacy progress language migration on startup |
| `UserViewModel.kt` | Language preference sync on login |

---

## 4. SDK & dependency bump (`b8eaa4f`)

| Package | From → To | Reason |
|---------|-----------|--------|
| Kotlin | 2.0.x → **2.2.21** | Ads SDK 25.x metadata mismatch |
| KSP | → **2.2.21-2.0.5** | Match Kotlin |
| Hilt | → **2.57.1** | KSP compatibility (2.59+ needs AGP 9) |
| play-services-ads | → **25.4.0** | Latest stable ads |

### Bugs hit during bump

1. **Kotlin metadata mismatch** with Ads 25.x on Kotlin 2.0 → resolved by full Kotlin bump.
2. **Hilt KSP** `Expected @HiltAndroidApp to have a value` on Hilt 2.59+ → pinned **2.57.1**.
3. **`verify-admob-config.ps1`** PowerShell encoding errors → rewritten ASCII-only.

---

## 5. Locale-aware UI labels (`d2c9812`)

### User-reported bug

After switching **English ↔ Kannada** in Settings:

- Home **subject** stayed in old language (e.g. “Math” when UI was Kannada).
- **Chapter names**, **concept titles**, **headings** sometimes mixed languages.
- **Greeting** stayed English (“Good Evening”) in Kannada mode.

### Root causes

1. **Subject stored as localized name string** in SharedPreferences (`setSubjectSelection("Science")` / `"ವಿಜ್ಞಾನ"`) instead of stable **subject ID**.
2. **`getLocalizedName()`** used `isKannada()` → reads `AppCompatDelegate`, which is **not a Compose state** → lists did not recompose on locale change.
3. **ViewModels** cached localized names at load time without reload on language change.
4. **Greeting** hardcoded in `HomeViewModel` instead of string resources.

### Fixes

| Change | Detail |
|--------|--------|
| Subject ID storage | `setSubjectSelectionId()` / `getSubjectSelectionId()` with migration from legacy name via `resolveStoredSubjectId()` |
| `SubjectIds` | Constants for Math / Science UUIDs |
| `getLocalizedName(languageCode)` | Explicit language param on Subject/Chapter/Concept entities |
| `HomeViewModel` | Resolves localized subject name from ID + `_currentLanguage` |
| Home greeting | `rememberTimeBasedGreeting()` using `R.string.good_morning` etc. |
| Screen reload hooks | `LaunchedEffect(currentLanguage)` on Home, Subject, Chapter, Concept, Progress screens |
| ViewModels | `ChapterViewModel.loadChapters(id, lang)`, `ConceptViewModel` uses `isKannadaLanguage(lang)` not global `isKannada()` |

### Screens touched

- `HomeScreen`, `HomeViewModel`, `TodaysProgressCard`, `PracticeSimulationCard`
- `SubjectScreen`, `SubjectViewModel`
- `ChapterScreen`, `ChapterViewModel`
- `ConceptViewModel` (headers + concept list)
- `ProgressScreen`, `SkillsProgressSection`

### Verified on device (2026-06-22)

- EN: `Good Evening`, `Current Subject`, `Science` / `Math`
- KN: `ಶುಭ ಸಂಜೆ`, `ಪ್ರಸ್ತುತ ವಿಷಯ`, `ಗಣಿತ`, Kannada lesson titles
- Logcat: `HomeViewModel: Language dynamically changed to: kn`

---

## 6. Remove agent debug overlay (`d2c9812`)

### Problem

`LogOverlay` on `ChatbotScreen` showed debug text when agent reached certain nodes (e.g. APK node):

- `From node: …`
- `To node: …`
- `imager url: …`
- `Concept Map: …`

Unacceptable for production / closed test users.

### Fix

Removed `LogOverlay` usage from `ChatbotScreen.kt`. Component file kept but unused (can delete later). Verified on device: no overlay on chatbot after opening lesson.

---

## 7. Bilingual loading quotes (`e5f61c9`)

### Why

Plain “Thinking…” / spinner during 2–10s agent waits felt empty. Goal: short science/education quotes (EN + KN) suitable for Class 7.

### Implementation

| File | Purpose |
|------|---------|
| `ui/components/LoadingQuotes.kt` | 20 quotes (Einstein, Curie, Kalam, Gandhi, Newton, Raman, light humor) + KN translations |
| `ui/components/LoadingInsightPanel.kt` | Spinner + status line + quote card (emoji + author) |
| `strings.xml` / `values-kn` | `teacher_thinking`, `loading_insight_label` (“While you wait” / “ನಿರೀಕ್ಷಿಸುವಾಗ”) |

### Wired into

- `ChatContentArea` — in-conversation loading
- `InitialAvatarView` — first session load (chat, math, revision)
- `SimulationConversationView` — teacher thinking + simulation loading

New quote picked when loading **status text changes** (new request).

### Verified on device (2026-06-22)

Install `lastUpdateTime=2026-06-22 19:01:29` on Oppo. User confirmed working.

---

## 8. Documentation & repo hygiene

| Item | Commit |
|------|--------|
| `docs/APP_STRUCTURE.md` | Full architecture reference |
| `README.md` | Quick start + links |
| `local.properties.example` | Secrets template for team |

**Intentionally not committed:** `local.properties`, `google-services.json`, Firebase tokens, `.tools/`, `app-ads.txt` (copy lives on GitHub Pages).

---

## 9. Play Store & launch context (manual, same sprint)

Not all in app repo; tracked for dev awareness:

- **Closed testing** completed on earlier build
- **Production access** approved on Play Console
- **app-ads.txt** + Search Console verification on https://anuragmn.github.io/
- **Production AAB uploaded:** `1.0.7` / versionCode **9** (25 Jun 2026)
- **AdMob / Play policy:** child-directed SDK + console questionnaire updated for ad rejection
- **Firestore rules:** deployed to `eduai-e090e` (1 Jul 2026) — see §18
- **Still optional / backlog:** Firebase Auth on login (tighter rules), Play App Signing SHA-1 in Firebase, in-app update flow (§14), remove full JWT log in `TokenManager.kt` if still present

---

## 10. Bugs & failures during development

| Issue | Symptom | Resolution |
|-------|---------|------------|
| Ads SDK 25 + Kotlin 2.0 | Build metadata errors | Bump Kotlin 2.2.21 + Hilt 2.57.1 |
| Hilt 2.59+ | KSP `@HiltAndroidApp` error | Stay on 2.57.1 until AGP 9 |
| Simulation viewer ad race | WebView loaded before ad | Move gate to `NavigationAdGate` |
| Language mix on home | Subject/chapters wrong lang | Subject ID + explicit `languageCode` in composables |
| `isKannada()` in Compose | No recomposition on locale change | Pass `LocalConfiguration` / `currentLanguage` into UI |
| `LogOverlay` on chatbot | Debug nodes visible to users | Removed from `ChatbotScreen` |
| Oppo adb disconnect | `device '123249b7' not found` mid-test | Reconnect USB; use `-s 123249b7` only (skip emulator for install) |
| Gradle install hang | Timeout on emulator during `installDebug` | `adb -s 123249b7 install -r` directly |
| Gradle daemon stop mid-install | `installDebug` exit 1 | Stop daemons, rebuild, install to single device |
| Firestore rules in repo | Deny-all client rules | **Do not deploy deny-all** if app uses client Firestore SDK for sync |
| `verify-admob-config.ps1` | Unicode/encoding parse errors | ASCII-only script |

---

## 11. Testing checklist (dev)

```powershell
# AdMob config
.\scripts\verify-admob-config.ps1

# Build
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug

# Install (Oppo)
adb -s 123249b7 install -r app\build\outputs\apk\debug\app-debug.apk

# Logcat filters
# MobileAdsInitializer, AdManager, ClickAdGate, ContentAnalytics, HomeViewModel

# Firestore scripts
node scripts/query-firestore-analytics.js
node scripts/metrics-retention-dau.js --html
.\scripts\run-dashboard.ps1
```

**Manual QA**

1. Switch EN ↔ KN in Settings → home subject, lessons, chapters, progress all match language.
2. Tap 6+ lesson/sim navigations in one day → ad on 6th (`ClickAdGate: showAd=true`).
3. Open agent lesson → loading quote visible; no `From node` overlay.
4. Firestore console → new analytics events within ~100ms of action.

---

## 12. Key files quick index (this sprint)

```
service/analytics/     → click + GA4 + Firestore analytics
service/ads/           → ClickAdPolicy, ClickAdGate, MobileAdsInitializer
ui/navigation/         → NavigationAdGate, LearningNavigator
utils/LocalizationUtils.kt → language codes, getLocalizedName(lang), SubjectIds
ui/components/         → LoadingInsightPanel, LoadingQuotes
data/local/SharedPreferenceUtils.kt → subject ID, language, ad click counters
scripts/               → Firestore queries, DAU dashboard, AdMob setup
docs/APP_STRUCTURE.md  → architecture
docs/DEV_CHANGELOG_JUN20-22.md → this file
```

---

## 13. Commits on `main` (pushed)

```
51abd2d Release 1.0.7: Play resubmit — login, child-safe ads, funnel analytics
e5f61c9 Show bilingual science quotes during agent loading states.
d2c9812 Fix locale-aware home labels and remove agent debug overlay.
181ef1e Add app structure documentation for team onboarding.
b8eaa4f Add analytics, AdMob monetization, and Firestore tooling for MVP test launch.
```

---

## 14. Next release backlog (keep in mind)

Items deferred from production launch **1.0.1 (versionCode 3)** — fix in the following release.

### Play rejections — resolved in 1.0.7

| Rejection | versionCode cited | Fix |
|-----------|-------------------|-----|
| **Broken Functionality** — infinite loading | 4 | Session validation, backup exclusions, home redirect (`1.0.2`+) |
| **Login credentials** — Gmail OTP / invalid | 5–6 | Institutional `@padaams.in` sign-in + Gmail account picker (`1.0.5`+) |
| **Ad Content** — ads vs content rating | 4 | Child-directed AdMob SDK (G rating) + AdMob/Play policy updates (`1.0.7`) |

### Play rejection (Jul 2026) — Metadata: featured graphic

**Issue:** Featured graphic (en-US) rejected — promotional / social-proof wording.

| Noncompliant (remove) | Why |
|----------------------|-----|
| **TRUSTED BY STUDENTS & PARENTS** | Reads like endorsement / popularity claim on a promotional asset |
| Review also | **IMPROVE UNDERSTANDING**, **BUILD STRONG CONCEPTS** — outcome promises; safer to use feature labels |

**Fix (store listing only — no app rebuild):**

1. Edit featured graphic (1024×500) — remove trust/ranking/outcome claims from footer and anywhere else on the image.
2. Use **factual feature text** only, e.g. footer: `NCERT ALIGNED` | `AI TUTOR & LABS` | `INTERACTIVE SIMULATIONS` | `ENGLISH & KANNADA`
3. Play Console → **Grow users → Store presence → Main store listing** → replace **Feature graphic** → **Publishing overview** → send for review (metadata-only; same AAB OK if binary unchanged).

Template: `docs/store-listing/feature-graphic-compliant.html` (export to PNG at 1024×500).

See §19 for full checklist.

### In-app update popup (Play Store updates)

**Current state:** `GooglePlayUpdateManager` + `InAppUpdateViewModel` exist; check runs on **login screen only**.

**Gaps to fix:**

| # | Issue | Fix |
|---|--------|-----|
| 1 | Login calls `checkForUpdate()` but never `startUpdate()` when an update is found | In `LoginScreen`, observe `updateState.updateAvailable` → call `updateViewModel.startUpdate(activity, UpdateType.FLEXIBLE)` |
| 2 | Logged-in users skip login → **no update check** on normal app open | Also check from `MainActivity` / home entry (or `BottomNavBar` once per session) |
| 3 | Flexible update downloaded but app not restarted | Show snackbar / dialog → `completeUpdate()` when `onUpdateInstalled` fires |
| 4 | Critical releases need forced update | Support `UpdateType.IMMEDIATE` (config or remote flag) for breaking changes |

**Files:** `LoginScreen.kt`, `InAppUpdateViewModel.kt`, `GooglePlayUpdateManager.kt`, optionally `LoginNavigator.kt` / `MainActivity.kt`

**Test:** Install **versionCode N** from Play internal/closed track → publish **N+1** → open app from Play → Google update UI should appear (not on sideloaded APK).

**Remember each release:** bump `versionCode` in `app/build.gradle.kts` (Play rejects reused codes).

### Other launch follow-ups (optional)

- ~~Deploy Firestore rules~~ — **done** 1 Jul 2026 (§18)
- **Firebase Auth on login** — wire `FirebaseAuth.signInWithCredential(Google)` so rules can use `request.auth.token.email` (§18)
- Add **Play App Signing certificate** SHA-1 to Firebase (for Play-installed builds)
- Privacy policy URL + data safety form complete in Play Console

---

## 15. Release 1.0.6 (versionCode 8) — 2026-06-25 *(local only)*

Build included funnel analytics; **not uploaded to Play** (superseded by 1.0.7 after ad-policy fix).

| Change | Detail |
|--------|--------|
| Funnel analytics | `FunnelAnalyticsTracker`, `onUserAuthenticated()` backfill |
| Query script | `scripts/query-firestore-analytics.py` |

---

## 16. Release 1.0.7 (versionCode 9) — **SHIPPED** 2026-06-25

**AAB:** `app/build/outputs/bundle/release/app-release.aab`  
**Git:** `51abd2d` on `main` → https://github.com/ANURAGMN/EduAI_app  
**Play:** Production upload submitted for review

### What’s in this release

| Area | Detail |
|------|--------|
| **Loading / session** | `hasValidLocalSession()`, backup exclusions, home invalid-session redirect |
| **Gmail sign-in** | Account picker via `signInIntent` when Credential Manager has no cached account |
| **Institutional login** | Collapsed **Institutional sign in** → `@padaams.in` + password (Play reviewers) |
| **Child-safe ads** | `MobileAdsInitializer`: child-directed, under-age consent, **max rating G** |
| **Funnel analytics** | `login_view`, `gmail_tap`, `institutional_expand`, `institutional_sign_in`, `profile_submit`, `home_view` → Firestore + GA4 |
| **Pre-login sync** | `DataSyncService.onUserAuthenticated()` backfills analytics/sessions after sign-in |
| **Firestore rules** | Updated rules in repo (`firestore.rules`) |
| **Scripts** | `query-firestore-analytics.py`, `test-gmail-signin.ps1`, `test-child-safe-ads.ps1` |

### Play Console — reviewer sign-in

| Field | Value |
|-------|--------|
| **Username** | `check@padaams.in` |
| **Password** | Same as `PADAAMS_SIGNIN_PASSWORD` in `local.properties` |
| **Instructions** | Tap **Institutional sign in** (not Continue with Gmail). Internet required. |

### AdMob / Play policy (required for ad rejection)

Code enforces G-rated child-directed requests on **every** build (debug + release). Also completed in console:

- AdMob → app **child-directed** + **block sensitive categories**
- Play → **Content rating** questionnaire updated for child-safe ads
- Guide: `scripts/admob-firebase-setup.md` § Step 4

### Verification (25 Jun 2026)

| Test | Result |
|------|--------|
| Gmail → account picker → home | Pass (Oppo CPH2661) |
| Institutional expand + sign-in path | Pass |
| Ad gate after 5+ clicks | Pass — education banner (e.g. tuition) |
| Funnel events in Firestore | Pass — `query-firestore-analytics.py mail2anuragmn@gmail.com` |

### Version history (this sprint)

| versionName | versionCode | Play |
|-------------|-------------|------|
| 1.0.2 | 4 | Rejected — loading |
| 1.0.3–1.0.5 | 5–7 | Login / Gmail fixes |
| 1.0.6 | 8 | Not uploaded |
| **1.0.7** | **9** | **Uploaded** |

---

## 17. Backend API (AWS EC2) — ops reference

Full detail: **[APP_STRUCTURE.md §11](./APP_STRUCTURE.md#11-backend-api-aws-ec2)**.

| Item | Value |
|------|--------|
| **API base URL** | `http://13.48.59.144:8000` |
| **Public DNS** | `ec2-13-48-59-144.eu-north-1.compute.amazonaws.com` |
| **Region** | `eu-north-1` |
| **SSH user** | `ubuntu` |
| **Shell access** | EC2 Instance Connect in AWS Console, or `ssh -i key.pem ubuntu@13.48.59.144` |
| **Health** | `Invoke-RestMethod http://13.48.59.144:8000/health` |
| **Docs** | http://13.48.59.144:8000/docs |

`local.properties` → `AGENTIC_AI_BASE_URL=http://13.48.59.144:8000`. No `.pem` in repo.

---

## 18. Firestore security rules — deployed 2026-07-01

**Trigger:** Firebase email — Test Mode 30-day window expiring; client access would be denied without production rules.

| Item | Detail |
|------|--------|
| **Rules file** | `firestore.rules` (in repo since 1.0.7 prep) |
| **Deploy** | `firebase deploy --only firestore` → project `eduai-e090e` |
| **Status** | **Live** — validated + deployed successfully |

### What the rules allow

| Path | Access |
|------|--------|
| `Concept/*` | Read-only (syllabus sync) |
| `users/*` | Read all; create/update if `appName == eduai_app` |
| `progress|analytics|sessions|streak|chapterprogress/eduai_app_{email}/…` | Read/write if `studentId` matches parent doc |
| `errors/eduai_app/logs/*` | Create only |
| Everything else | Deny |

### Verify after deploy

Sign in on device → home loads, progress syncs, no `PERMISSION_DENIED` in logcat for Firestore.

### Follow-up: Firebase Auth (stronger security)

**Current gap:** App uses Google Sign-In for the **backend API** only — not `FirebaseAuth`. Rules rely on path + `appName` / `studentId` fields, not `request.auth`.

**Next release task:**

1. On successful Google login → `FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))`
2. Tighten `firestore.rules` — e.g. `request.auth != null && request.auth.token.email == studentId`
3. Restrict `users` reads to authenticated owner or admin scripts only
4. Re-deploy: `firebase deploy --only firestore`

**Files:** `GoogleLoginButton.kt` / login flow, `PadaamsEmailAuth` path (Firebase Auth email link or custom token if needed), `firestore.rules`

---

## 19. Play Metadata rejection — featured graphic (Jul 2026)

**Status:** Listing rejected; **1.0.7 AAB can stay** — fix is store assets only.

### What Google flagged

Featured graphic contained wording that implies **performance, popularity, or endorsement** — not allowed on feature graphics (stricter than screenshots).

**Primary offender:** footer pill **“TRUSTED BY STUDENTS & PARENTS”** on the **featured graphic**.

**In-app (login screen):** footer had **“1M+ Students”** / **“Trusted Globally”** — same policy risk in store screenshots. Fixed in code: `FooterCard.kt` → **Interactive Labs** / **Math & Science** (§19, release 1.0.8+).

**Also risky on the same banner:**

| Text | Risk |
|------|------|
| TRUSTED BY STUDENTS & PARENTS | Social proof / trust claim — **remove** |
| IMPROVE UNDERSTANDING | Outcome promise |
| BUILD STRONG CONCEPTS | Outcome promise |

**Keep (factual):** NCERT ALIGNED, AI TUTOR & LABS, MATH/SCIENCE, ENGLISH & KANNADA, SIMULATIONS — these describe what the app is.

### Compliant footer (suggested)

```
NCERT ALIGNED  |  AI TUTOR & LABS  |  INTERACTIVE SIMULATIONS  |  ENGLISH & KANNADA
```

### Resubmit steps

1. Open your design source (Canva/Figma) or `docs/store-listing/feature-graphic-compliant.html`
2. Export **1024 × 500 PNG** (max 15 MB)
3. Play Console → **Main store listing** → **Feature graphic** → upload
4. Scan **title, short description, full description, icon** for: Best, #1, Top, Trusted, Popular, free/discount, awards
5. **Publishing overview** → submit listing changes for review

### Metadata checklist (all locales)

- [ ] Feature graphic — no trust/ranking/price claims
- [ ] App icon — no badges (“#1”, “Free”)
- [ ] Title ≤ 30 chars, no promotional caps spam
- [ ] Descriptions — factual features only; no unattributed testimonials

---

*Last updated: 2026-07-02 (featured graphic metadata rejection). Each feature section includes **Bugs that existed & how we fixed them**. Use technical sections below for file-level debugging.*
