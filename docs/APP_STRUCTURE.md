# EduAI — App Structure & Key Items

Reference for developers joining the project.  
**Repo:** https://github.com/ANURAGMN/EduAI_app.git  
**Package:** `com.ncert7.aitutorandlab`  
**Firebase project:** `eduai-e090e`  
**App identifier (Firestore):** `eduai_app` (`AppConfig.APP_NAME`)

---

## 1. Tech stack

| Layer | Technology |
|-------|------------|
| UI | Jetpack Compose, Material 3, Navigation Compose |
| DI | Dagger Hilt 2.57 |
| Local DB | Room 2.8 (schema v2) |
| Backend sync | Firebase Firestore, Firebase Analytics, Crashlytics |
| Ads | Google Mobile Ads SDK 25.4.0 (AdMob) |
| Network | Retrofit, OkHttp, Kotlin Serialization |
| Async | Kotlin Coroutines, Flow |
| Background | WorkManager (daily sync) |
| Language | Kotlin 2.2.21 |

---

## 2. High-level architecture

```
MainActivity
  └── LoginNavigator          (auth gate)
        └── BottomNavBar      (main app shell)
              ├── Home
              ├── Progress
              ├── Settings
              └── LearningNavigator (subjects → chapters → concepts → agents)
```

**Pattern:** UI (Compose screens) → ViewModels → Repositories / UseCases → DAOs / Remote APIs → Room / Firestore

---

## 3. Project layout

```
Eduapp/
├── app/src/main/java/com/ncert7/aitutorandlab/
│   ├── MainActivity.kt              # Entry, Mobile Ads init
│   ├── EduAiApplication.kt          # SessionManager, analytics, sync bootstrap
│   ├── config/                      # AppConfig (APP_NAME)
│   ├── data/
│   │   ├── local/                   # Room entities, DAOs, database, migrations
│   │   └── remote/                  # Retrofit, LLM/Gemini clients
│   ├── domain/                      # Use cases (chatbot, math, simulation, progress)
│   ├── repository/                  # Data access facades
│   ├── service/
│   │   ├── analytics/               # Click tracking, GA4, Firestore analytics sync
│   │   ├── ads/                     # AdMob policy, gate, initializer
│   │   ├── sync/                    # Firestore sync workers & managers
│   │   ├── logging/                 # Firestore error logger
│   │   └── update/                  # Play in-app updates
│   ├── ui/
│   │   ├── navigation/              # BottomNavBar, LearningNavigator, ad gate
│   │   ├── screens/                 # Feature screens (home, chatbot, etc.)
│   │   ├── components/              # Shared UI (AdDialog, BannerAdView)
│   │   └── theme/
│   └── di/                          # Hilt modules
├── app/src/test/                    # Unit tests
├── docs/                            # This file
├── scripts/                         # Firestore queries, metrics, AdMob/Firebase setup
├── firebase.json                    # Firebase project config
├── firestore.rules                  # Deny-all client rules (deploy carefully)
├── local.properties.example         # Secrets template (copy → local.properties)
└── gradle/libs.versions.toml        # Version catalog
```

---

## 4. Navigation & user flow

### Main tabs (`BottomNavBar`)
| Tab | Screen | Purpose |
|-----|--------|---------|
| Home | `HomeScreen` | Today's progress, lesson cards, quick simulations |
| Progress | `ProgressScreen` | Skills/chapter progress overview |
| Settings | `SettingScreen` | Profile, language, support |

### Learning flow (`LearningNavigator`)
```
Subjects → Chapters → Concepts (by type: STUDY / MATH PROBLEM / SIMULATION)
                              ├── ChatbotScreen      (study lessons)
                              ├── MathAgentScreen    (math problems)
                              ├── SimulationAgentScreen (AI simulation chat)
                              ├── ConceptSimulationViewer (HTML WebView)
                              └── RevisionScreen
```

### Ad gate
All **tracked navigation clicks** go through `NavigationAdGate` / `GatedNavigationAction`.  
After **5 clicks per calendar day**, the **6th+** shows `AdDialog` (banner) before opening the destination.

---

## 5. Key screens

| Screen | Path | ViewModel / notes |
|--------|------|-------------------|
| Login | `ui/screens/login/` | Google Sign-In |
| Home | `ui/screens/home/` | `HomeViewModel` — streak, today's counts |
| Subject | `ui/screens/subjectscreen/` | Subject list |
| Chapter | `ui/screens/chapterscreen/` | Study / Math / Simulation entry |
| Concept | `ui/screens/conceptscreen/` | `ConceptViewModel`, `ConceptSimulationViewModel` |
| Chatbot | `ui/screens/chatbotscreen/` | AI tutor, TTS, concept map |
| Math agent | `ui/screens/mathagentscreen/` | Step-by-step math help |
| Simulation agent | `ui/screens/simulation_agent/` | Conversational simulation |
| Simulation viewer | `conceptscreen/components/ConceptSimulationViewer.kt` | WebView HTML sims |
| Revision | `ui/screens/revisionscreen/` | Chapter revision |
| Progress | `ui/screens/progess/` | Progress dashboard |

---

## 6. Data layer

### Room database (`EduAiDatabase`, version **2**)

| Entity | DAO | Purpose |
|--------|-----|---------|
| `StudentEntity` | `StudentDao` | Logged-in user profile |
| `SubjectEntity` | `SubjectDao` | NCERT subjects |
| `ChapterEntity` | `ChapterDao` | Chapters per subject |
| `ConceptEntity` | `ConceptDao` | Lessons / problems / sim metadata |
| `ProgressEntity` | `ProgressDao` | Per-item completion (STUDY, SIMULATION, etc.) |
| `SessionEntity` | `SessionDao` | App session tracking |
| `AppAnalyticsEntity` | `AppAnalyticsDao` | Click/entry/exit analytics events |
| `StreakEntity` | `StreakDao` | Daily streak |
| `ChapterAgentProgressEntity` | `ChapterAgentProgressDao` | Agent session progress |

**Migration:** `data/local/database/DatabaseMigrations.kt` (v1→v2 adds analytics fields).

### Firestore paths (synced from device)

| Collection | Document pattern | Contents |
|------------|------------------|----------|
| `users/` | per user | Profile, `appName=eduai_app` |
| `progress/eduai_app_{email}/records/` | `{type}_{id}_{en\|kn}` | Completion status |
| `analytics/eduai_app_{email}/events/` | `{analyticsId}` | Click, entry, exit events |
| `sessions/eduai_app_{email}/records/` | session records | Session start/end |

---

## 7. Analytics

### Trackers
| Class | Role |
|-------|------|
| `ContentClickAnalyticsTracker` | Non-simulation taps (lessons, subjects, chapters, etc.) |
| `SimulationAnalyticsTracker` | Simulation CLICK + COMPLETE |
| `AnalyticsEventRecorder` | Writes to Room + triggers Firestore sync |
| `SessionManager` | Screen ENTRY/EXIT + session lifecycle |
| `TrackScreenEvent` | Compose helper for screen analytics |
| `FirebaseAnalyticsHelper` | GA4 event logging |

### Content click types (`ContentClickType`)
`LESSON`, `STUDY`, `MATH_PROBLEM`, `CHAPTER_STUDY`, `CHAPTER_MATH`, `CHAPTER_SIMULATION`, `SUBJECT`, `REVISION`

### GA4 events
- `content_click` — item_id, content_type, source
- `simulation_click` — conceptId, interaction, source
- `simulation_complete` — conceptId, interaction

### Query script
```powershell
node scripts/query-firestore-analytics.js mail2anuragmn@gmail.com
```

---

## 8. Ads (AdMob)

| Class | Role |
|-------|------|
| `ClickAdPolicy` | 5 free clicks/day; 6th onward shows ad |
| `ClickAdGate` | Reads today's CLICK count from `AppAnalyticsDao` |
| `NavigationAdGate` | Composable wrapper; shows `AdDialog` before nav |
| `AdManager` | Banner load lifecycle |
| `BannerAdView` | Compose banner |
| `AdDialog` | Full-screen ad + Close button |
| `MobileAdsInitializer` | SDK init, test device IDs |

**Config (local.properties, gitignored):**
```properties
ADMOB_APP_ID=ca-app-pub-...
BANNER_AD_UNIT_ID=ca-app-pub-.../...
ADMOB_TEST_DEVICE_ID=...   # optional, debug only
```

**Setup doc:** `scripts/admob-firebase-setup.md`  
**Verify:** `.\scripts\verify-admob-config.ps1`

**Logcat tags:** `MobileAdsInitializer`, `ClickAdGate`, `AdManager`, `ContentAnalytics`

---

## 9. Sync & background jobs

| Component | Role |
|-----------|------|
| `DataSyncService` | Real-time sync trigger on analytics/progress updates |
| `ProgressAnalyticsSessionSyncManager` | Batch sync orchestration |
| `FirebaseSyncManager` | Firestore write/read |
| `WeeklySyncWorker` / `DataSyncWorker` | Periodic WorkManager jobs |
| `EduAiApplication` | Starts session, schedules daily sync, legacy migration |

---

## 10. Configuration & secrets

### Required per developer (not in git)
| File | Purpose |
|------|---------|
| `local.properties` | SDK path, API keys, AdMob IDs — copy from `local.properties.example` |
| `app/google-services.json` | Firebase Android config — download from Firebase Console |

### BuildConfig fields (from local.properties)
`AUTH_KEY`, `GEMINI_API_KEY`, `GROQ_API_KEY`, `AGENTIC_AI_BASE_URL`, `SIMULATION_BASE_URL`, `ADMOB_APP_ID`, `BANNER_AD_UNIT_ID`, `ADMOB_TEST_DEVICE_ID`

### Firestore security rules

| Item | Detail |
|------|--------|
| **Rules file** | `firestore.rules` |
| **Project** | `eduai-e090e` |
| **Deployed** | 2026-07-01 (replaces expired Test Mode open access) |

**Deploy (admin):**

```powershell
cd Eduapp
firebase deploy --only firestore
```

Or via Firebase MCP in Cursor (`firebase_deploy` with `only: firestore`).

**Collections used by the app:** `Concept`, `users`, `progress`, `analytics`, `sessions`, `streak`, `chapterprogress`, `errors`. All other paths are denied. See `firestore.rules` for path-scoped read/write checks (`appName`, `studentId`).

**Follow-up (backlog):** Add **Firebase Auth** on Google login so rules can bind to `request.auth.token.email` instead of field-only checks. Details in [DEV_CHANGELOG §18](./DEV_CHANGELOG_JUN20-22.md#18-firestore-security-rules--deployed-2026-07-01).

---

## 11. Backend API (AWS EC2)

FastAPI agent server for chatbot, math, simulation, and revision flows. The Android app reaches it via `AGENTIC_AI_BASE_URL` (Retrofit / OkHttp).

### Public URL

| Item | Value |
|------|--------|
| **Base URL** | `http://13.48.59.144:8000` |
| **Public DNS** | `ec2-13-48-59-144.eu-north-1.compute.amazonaws.com` |
| **AWS region** | `eu-north-1` (Stockholm) |
| **Port** | **8000** — HTTP only (no HTTPS on this host) |

**Health check (PowerShell):**

```powershell
Invoke-RestMethod "http://13.48.59.144:8000/health"
```

**API docs (browser):** http://13.48.59.144:8000/docs

### App wiring

| Location | Purpose |
|----------|---------|
| `local.properties` → `AGENTIC_AI_BASE_URL` | Backend base URL baked into debug/release builds |
| `app/src/main/res/xml/network_security_config.xml` | Whitelists `13.48.59.144` for cleartext HTTP |

Set in `local.properties`:

```properties
AGENTIC_AI_BASE_URL=http://13.48.59.144:8000
```

Most endpoints require Google ID token or API key auth; `/health` and `/simulation` are public.

### SSH / shell access

No EC2 private key (`.pem`) is stored in this repo. Use one of:

**Option A — EC2 Instance Connect (recommended, no key file)**

1. AWS Console → **EC2** → **Instances**
2. Select instance with public IP **13.48.59.144**
3. **Connect** → **EC2 Instance Connect** → **Connect**
4. Browser shell opens as user **`ubuntu`** (Ubuntu 22.04/24.04 AMI)

**Option B — SSH from your machine**

Requires the `.pem` key pair downloaded when the instance was launched, and security group **TCP 22** open to your IP:

```powershell
ssh -i "C:\path\to\your-key.pem" ubuntu@13.48.59.144
```

Or:

```powershell
ssh -i "C:\path\to\your-key.pem" ubuntu@ec2-13-48-59-144.eu-north-1.compute.amazonaws.com
```

Full AWS setup (Docker, SSM secrets, security group): [AgenticDeploymentEC2 — AWS_Hosting_Instructions.pdf](https://github.com/ANURAGMN/AgenticDeploymentEC2/blob/main/AWS_Hosting_Instructions.pdf)

### On-server ops

Container name is typically **`fastapi`**:

```bash
docker ps
docker logs -f fastapi
curl http://localhost:8000/health
```

Image source: [EduAgent](https://github.com/ANURAGMN/AgenticInteractiveTutor) (`aloofzebra03/educational-api` on Docker Hub).

### Region note

The deployment PDF uses **ap-south-1** (Mumbai) for SSM Parameter Store examples. The **live instance is in eu-north-1** — match the AWS region in the console/CLI to wherever SSM params and the instance actually live.

---

## 12. Scripts (ops & metrics)

| Script | Purpose |
|--------|---------|
| `scripts/admob-firebase-setup.md` | AdMob ↔ Firebase linking + payments checklist |
| `scripts/verify-admob-config.ps1` | Validate local AdMob/Firebase config |
| `scripts/query-firestore-analytics.js` | Per-user analytics from Firestore |
| `scripts/query-firestore-progress.js` | Per-user progress from Firestore |
| `scripts/metrics-retention-dau.js` | DAU, retention, click breakdown; `--html` for dashboard |
| `scripts/run-dashboard.ps1` | Generate `reports/dashboard.html` |
| `scripts/setup-admob-ids.ps1` | Helper to set AdMob IDs in local.properties |
| `scripts/setup-firebase-mcp.ps1` | Firebase MCP for Cursor |
| `firestore.rules` + `firebase deploy --only firestore` | Production Firestore security rules (see §10) |

Firestore scripts require `.tools/firebase-ci-token.txt` (gitignored).

---

## 13. Build & run

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
cd Eduapp
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Unit tests
```powershell
.\gradlew.bat testDebugUnitTest
```

---

## 14. Team onboarding checklist

1. Clone: `git clone https://github.com/ANURAGMN/EduAI_app.git`
2. Copy `local.properties.example` → `local.properties` and fill values
3. Download `google-services.json` → `app/google-services.json`
4. Run `.\scripts\verify-admob-config.ps1`
5. Build & install debug APK
6. (Admin) Link AdMob to Firebase — see `scripts/admob-firebase-setup.md`
7. (Admin) Complete AdMob payments profile

---

## 15. Recent MVP additions (commit `b8eaa4f`)

- Full click analytics (content + simulation) with Firestore + GA4
- AdMob banner after 5 daily clicks
- Kotlin 2.2.21 + Ads SDK 25.4.0
- Progress/language sync fixes + Room v2 migration
- DAU/retention dashboard scripts

---

*Last updated: 2026-07-01*
