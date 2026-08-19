# v1.0.11 — Local ship set (committed + uncommitted)

**Updated:** 2026-08-19  
**Branch:** `main` → tracks `gamified/main` (`https://github.com/ANURAGMN/Eduai_Gamified.git`)  
**Tip (committed):** `eb21044` (ship commit `f7de743`)  
**Remote:** **7 commits ahead, not pushed**  
**Working tree:** **clean** (Settings GA4, theme toggle, SubjectRowDBG cleanup committed)

`origin` is the simulations repo (`EduAI_app`). App ship set goes to **`gamified`**.

---

## Snapshot

| Layer | State |
|-------|--------|
| Committed locally | `c6eb69f` → … → `cca2cb0` → **Settings GA4 + theme + cleanup commit** |
| On GitHub (`gamified/main`) | **None of the six** |
| Uncommitted | **None** — safe to `bundleRelease` from tip |

Tip includes Made-for-kids reels, Settings GA4, `notification_opened`, and the Appearance (Light/Dark) toggle.

---

## A. Committed, not pushed (oldest → newest)

### `c6eb69f` — Improve login recovery and ship onboarding/reels polish

107 files. Main product commit.

- Login continues when Firestore is quota/slow (`UserViewModel`)
- Streak / garden restore hardening; garden celebration
- Friend code shown immediately; avatar save = 1 ad; subject rows / chapter rings
- Reels explore/player + analytics (behind `REELS_ENABLED`)
- Onboarding / ui-kit polish

### `02f504b` — Gitignore + science sim viewport

- `.gitignore` for temp shots, `.cursor/`, kotlin error logs, local DBs
- `Simulations/science_4_10.html` — WebView `--vh` never collapses to 0

### `db9935f` — Line-ending hygiene

- `.gitattributes` (`* text=auto eol=lf`)
- Untrack `.idea/` + `.kotlin/errors/*.log`
- Local `core.autocrlf=false`

### `2f80805` — Coach sandbox + example

- `coach-sim/` (7 files) + `docs/examples/edu-round-hook-math_1_8.html`
- Secret scan: public sim host only

### `cca2cb0` — Made-for-kids for 1.0.11

- `REQUIRE_MADE_FOR_KIDS_DEFAULT = true`
- `LanguageConsistencyTest.FakeChapterDao` stubs `getChapterCountsBySubjectFlow()`

Push (optional for AAB, recommended for backup):

```powershell
git push gamified main
```

---

## B. Sixth commit — Settings GA4, theme toggle, SubjectRowDBG cleanup

### 1. Settings + engagement analytics (GA4 only — no Room, no Firestore)

Read after launch in **Firebase Console → Analytics** (GA4), not Firestore / not `metrics-dashboard-html.py`.

| Event | When |
|--------|------|
| `theme_selected` | Light / Dark tap (`mode`) |
| `language_selected` | EN / KN tap (`language`) |
| `settings_tap` | Rows: `edit_profile`, `notifications` (+ `dest=in_app\|os`), `contact_us`, `privacy`, `terms`, `progress`, `quests`, `friends` |
| `edit_profile_saved` | Profile save **succeeds** |
| `contact_channel` | `email` / `whatsapp` / `web` |
| `logout` | Log out tap |
| `notif_pref_master` / `_mode` / `_category` / `_reminder_time` / `_quiet_hours` | In-app notification prefs |
| `notif_pref_open_system` | OS app/channel settings |
| `notification_opened` | User **taps** a local notification (`type`, `route`) — pairs with existing `notification_shown` |

Path: `EngagementAnalyticsTracker` / `GamificationAnalyticsTracker` → `FirebaseAnalyticsHelper.logEvent`.  
`AnalyticsFirestoreMirror.ENABLED` stays **false**.

Files: `EngagementAnalyticsTracker.kt`, `GamificationAnalyticsTracker.kt`, `SettingScreen.kt`, `NotificationSettingsScreen.kt`, `NotificationSettingsViewModel.kt`, `ContactSupportCard.kt`, `EditProfileSection.kt`, `MainActivity.kt`, `NotificationHelper.kt` (`EXTRA_TYPE`), `PRE_LAUNCH_CHECKLIST.md`, `P1_PRODUCT_GAPS_DETAILED.md`

### 2. Light / Dark appearance (product, not just analytics)

- New `ThemeModeStore.kt`
- Settings Appearance section; `EduAiTheme` / `AppTheme` follow saved mode
- Strings EN + KN: `settings_theme`, `theme_light`, `theme_dark`

Files: `ThemeModeStore.kt`, `EduAiTheme.kt`, `Theme.kt`, `SettingScreen.kt`, `values/strings.xml`, `values-kn/strings.xml`

### 3. Subject-row debug logs removed

`SubjectRowDBG` stripped from `HomeViewModel` / `GamifiedHomeMapper` (was `BuildConfig.DEBUG` only).

### 4. This note

`docs/RELEASE_v1.0.11_LOCAL_PENDING.md`

---

## C. Analytics after launch (GA4)

1. Firebase project **eduai-e090e** → Analytics → **Realtime / DebugView** (same day) or **Events** (~24h).
2. Register custom dimensions: `mode`, `language`, `item`, `channel`, `type`.
3. Light/Dark split = `theme_selected` broken down by `mode`.
4. Notification return rate = join `notification_shown` → `notification_opened` on `type`.

---

## D. Build config (this machine)

| Field | Value |
|-------|--------|
| version | **1.0.11** / **vc13** |
| targetSdk | **36** |
| `GAMIFIED_HOME_ENABLED` | `true` |
| `GARDEN_ENABLED` | `true` |
| `REELS_ENABLED` | `true` (AAB **will ship Reels**) |

Confirm Play does not already have vc13 (else bump 14). Signing: `../../Edu/Keys/android_sign_keys.jks`.

Unit tests (with SubjectRowDBG removed): reels/analytics **45/45**; full suite **215/225** (10 pre-existing):

| Class | Failures | Cause |
|-------|----------|--------|
| `SimulationIntroTtsSanitizerTest` | 7 | JVM Unicode regex / class init |
| `LanguageConsistencyTest` | 2 | Hero concept name not switching on locale change (`ComparisonFailure`) |
| `ResourceDecisionUseCaseTest` | 1 | Unmocked `Log` |

SubjectRowDBG removal fixed the third `LanguageConsistencyTest` failure (unmocked `Log` via `DebugLogger`); the two locale-switch assertions above remain.

---

## E. Before `bundleRelease`

- [x] Commit uncommitted set (Settings GA4 + theme + cleanup)
- [ ] **ColorOS light/dark smoke** — flip Appearance toggle; page Home / Chapter / Reels / Settings in both modes (no white-on-white or dark-on-dark)
- [ ] Confirm vc13 / Reels-in-or-out
- [ ] Optional: `git push gamified main`
- [ ] `.\gradlew.bat :app:bundleRelease`

Play/Console after AAB: ColorOS smoke, Data Safety, 512 icon, release notes. Firestore auth-gated rules **after** min-version adoption.

---

## F. Stack picture

```
f7de743  Settings GA4 + notif opened + Light/Dark + SubjectRowDBG removal + this doc
cca2cb0  Made-for-kids reels gating
2f80805  coach-sim + edu-round example
db9935f  .gitattributes / untrack .idea .kotlin
02f504b  gitignore + science_4_10 viewport
c6eb69f  login / onboarding / reels polish
         ↑ gamified/main (GitHub — not updated)
```
