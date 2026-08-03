# EduAI Gamification Integration Plan

**Status:** Planning (not implemented)  
**Last updated:** 2026-07-23  
**Live app:** `com.ncert7.aitutorandlab` — [Eduapp repo](.) / Firebase `eduai-e090e`  
**Source UI kit:** [ANURAGMN/Gamification](https://github.com/ANURAGMN/Gamification) — local path `C:\Users\anurag.mn\Desktop\Gamification`

This document is the **master / single source of truth** for merging the Gamification prototype into the production Eduapp. It covers product truth, phasing, agents, exam planner, quests, leagues, friends, XP/gems, ads, database design, Firestore limits, avatar migration, app size, rollout, and testing.

### Document map

The plan spans three files. This one is canonical for **product decisions**; the two Gamification-repo files carry implementation detail:

| Doc | Role | Owns |
|-----|------|------|
| **This file** (`docs/GAMIFICATION_INTEGRATION_PLAN.md`) | **Master** | Product truth, phasing, economy tables, DB schema, testing |
| `Gamification/INTEGRATION_PLAN.md` | **Part A — UI & navigation** | ui-kit module merge, `GamifiedShell`, nav wiring, theme, `NavigationAdGate` reuse, "Home-only first" Phase 0/1 steps |
| `Gamification/INTEGRATION_PLAN_PART2.md` | **Part B — backend, ops & compliance** | Cloud Functions (`leagueRollover`/`claimReferral`/`admobSsv`), Blaze cost math, aggregated league doc, GA4 analytics spec, kids-compliance checklist, gap analysis (FDL, migration, clock), `HomeProgressRail` |

**On conflicts, this master wins for product/economy; Part A wins for UI/nav specifics; Part B wins for backend/ops specifics.** Where a value must be single-sourced (XP/gem amounts, promotion counts), it lives in **Remote Config** and is referenced — not duplicated — across docs.

---

## Table of contents

1. [Executive summary](#1-executive-summary)
2. [What the Gamification repo contains](#2-what-the-gamification-repo-contains)
3. [What the live app has today](#3-what-the-live-app-has-today)
4. [Architecture & module merge](#4-architecture--module-merge)
5. [Navigation redesign](#5-navigation-redesign)
6. [Theme & localization](#6-theme--localization)
7. [Learning agents (no quiz)](#7-learning-agents-no-quiz)
8. [Exam planner](#8-exam-planner)
9. [Daily quests](#9-daily-quests)
10. [XP & gems economy](#10-xp--gems-economy)
11. [Leagues](#11-leagues)
12. [Friends & social feed](#12-friends--social-feed)
13. [Avatar migration (boy/girl → Free tutor)](#13-avatar-migration-boygirl--free-tutor)
14. [Ads: banner & rewarded video](#14-ads-banner--rewarded-video)
15. [Database design (Room + Firestore)](#15-database-design-room--firestore)
16. [Firestore free tier & cost control](#16-firestore-free-tier--cost-control)
17. [Analytics](#17-analytics)
18. [App size estimate](#18-app-size-estimate)
19. [Phased rollout](#19-phased-rollout)
20. [Testing checklist](#20-testing-checklist)
21. [What not to do](#21-what-not-to-do)
22. [Open decisions](#22-open-decisions)
23. [Related docs & files](#23-related-docs--files)

---

## 1. Executive summary

The Gamification repo delivers a **native Compose UI kit** (`phase0-native/ui-kit`) with Home, Leagues, Avatar Studio, onboarding, design tokens, quest/plan trails, and reward overlays. **Phase 0 is UI-ready; backend wiring is not done.**

Integration work is primarily:

| Area | Work |
|------|------|
| **UI** | Import `:ui-kit` module; replace Home; add tabs incrementally |
| **Data** | Wire mock UI to real progress, streak, auth, new gamification tables |
| **Exam planner** | Build scheduler (prototype only stubs `ps_generate()`) |
| **Quests** | Derive daily targets from plan + agent progress — **no quiz** |
| **Economy** | XP on agent completion; gems via **rewarded ads** + invites |
| **Social** | Friend codes, feed, cheers; leagues with weekly XP ranking |
| **Avatar** | Replace WebView boy/girl lip-sync with native `EduTutorAvatar` |
| **Ads** | Add **Rewarded** AdMob unit; keep existing banner click-gate |

Recommended strategy: **feature-flagged phased rollout** — new Home first, then economy, then anonymous leagues, then friends by code (Path A). The invite **reward** loop (Path B, +50/+50 gems) ships last since it needs install attribution (Firebase Dynamic Links is shut down).

---

## 2. What the Gamification repo contains

Three layers — only one belongs in production Eduapp:

| Layer | Location | Use in Eduapp |
|-------|----------|---------------|
| **Native UI kit (primary)** | `phase0-native/ui-kit` | **Merge as Gradle module** — design system, screens, components |
| **HTML prototype** | `EduAI_Master_Prototype_latest.html`, `www/` | Design reference only |
| **WebView demo app** | `android/` | Standalone demo APK — **do not ship inside Eduapp** |

### UI kit inventory (Phase 0 — mostly done)

| Component | Status | Notes |
|-----------|--------|-------|
| Design tokens, `EduAiTheme` | Done | May bridge with existing `BrandPrimary` / `LocalDimensions` |
| `EduBottomNavItem` — 6 tabs | Done | Home, Plan, Quests, Leagues, Avatar, Profile |
| `HomeScreen`, `HeroFocusCard`, rails | Done | Mock data today |
| `QuestTrail`, `PlanTrail` | Done | Quiz node must be replaced |
| `EduLeaguesScreen`, leaderboard | Done | Mock 15-player board |
| `FriendsUpdatesRail` | Done | Mock friends |
| `AvatarStudioScreen`, `EduTutorAvatar` | Done | Simulated rewarded ads |
| `OnboardingScreen` | Done | Conflict with existing login flow — gate once |
| Plan / Quests full screens | Placeholder | Phase 1+ |
| Backend / event bus | Mock only | `GamificationEvent` not wired to Firestore |

See [PHASE0_TASKS.md](https://github.com/ANURAGMN/Gamification/blob/main/PHASE0_TASKS.md) in the Gamification repo.

---

## 3. What the live app has today

| Feature | Implementation |
|---------|----------------|
| Package | `com.ncert7.aitutorandlab` |
| Navigation | **3 tabs:** Home, Progress, Settings |
| Progress | `ProgressEventTracker`, Room, Firestore sync |
| Streak | `StreakRepository` + Firestore `streak/` |
| Agents | Study, Simulation, Simulation Agent, Math Agent, Science Agent, Revision Agent |
| Quiz | **None** |
| Ads | **Banner only** — `ClickAdGate` (5 free clicks/day → ad dialog) |
| Avatar | WebView `LipSync.html` — boy / girl / disable |
| Gamification | Not present |

### Progress item types (keep in sync everywhere)

From `ProgressEventTracker.kt`:

| `itemType` | Agent / feature |
|------------|-----------------|
| `CONCEPT` | Study agent (Science / general) |
| `SIMULATION` | Simulation HTML load/complete |
| `SIMULATION_AGENT` | Simulation agent session |
| `MATH_AGENT` | Math simulation agent |
| `SCIENCE_AGENT` | Science study agent (100% nodes) |
| `REVISION_AGENT` | Revision agent |

Chapter completion rules: `ChapterCompletionUseCase.kt`.

---

## 4. Architecture & module merge

### Gradle module

1. Copy or submodule `Gamification/phase0-native/ui-kit` → `Eduapp/ui-kit` (or `modules/ui-kit`).
2. Add to `settings.gradle.kts`: `include(":ui-kit")`.
3. App dependency: `implementation(project(":ui-kit"))`.
4. Align `compileSdk` — ui-kit uses 35; Eduapp uses **36** — bump ui-kit.
5. New dependency from ui-kit: **Lottie Compose** + **VIBRATE** permission (reward haptics).

### Namespace

- Keep ui-kit package: `com.anurag.eduai.uikit` (separate module).
- App package stays: `com.ncert7.aitutorandlab`.
- Bridge via interfaces in app layer (ViewModels, repositories) — avoid renaming until stable.

### Integration pattern

```
:app
  ├── domain/gamification/     ← new: reward service, quest engine, league repo
  ├── data/local/              ← new entities + DAOs
  ├── service/sync/            ← extend DataSyncService
  └── ui/                      ← hosts ui-kit composables, passes real state

:ui-kit
  └── pure UI + theme + avatar renderer (no Hilt, no Firestore)
```

### Feature flag

Use `BuildConfig.GAMIFICATION_HOME_ENABLED` or Firebase Remote Config so Play users can fall back to old Home during rollout.

---

## 5. Navigation redesign

### Current vs target

| Eduapp today | Gamification prototype |
|--------------|------------------------|
| Home | Home (gamified) |
| Progress | Plan / Quests (partial replacement) |
| Settings | Profile (+ merge Settings into Profile tab) |
| — | Leagues |
| — | Avatar |

### Suggested mapping (v1)

| Tab | Content |
|-----|---------|
| **Home** | New gamified Home — hero, quests, plan trail, friends rail |
| **Plan** | Exam prep plan full screen |
| **Quests** | Daily + bonus quests |
| **Leagues** | Weekly leaderboard + friends feed tab |
| **Avatar** | Avatar Studio |
| **Profile** | Old Settings + stats + friends list + invite |

**Preserve all deep links:** chapters → concepts → study/simulation/agents must still work from gamified Home.

### Rollout option

Start with **3-tab shell** + new Home only; add tabs as features ship.

---

## 6. Theme & localization

- Gamification: `EduAiTheme`, own colors/dimens/motion.
- Eduapp: `BrandPrimary`, `LocalDimensions`, Material theme.
- **Choose one path:**
  - **A (recommended long-term):** Adopt ui-kit tokens app-wide incrementally.
  - **B (short-term):** Bridge themes during migration; no two themes on one screen.
- ui-kit strings are mostly hardcoded English — extract to `strings.xml` + `values-kn` to match Eduapp.

---

## 7. Learning agents (no quiz)

There is **no quiz** in Eduapp. Do not ship quiz UI or quest nodes labeled "Quiz 80%+".

### Agent → progress → gamification hook

Every completion already flows through `ProgressEventTracker`. Extend it (or call `GamificationRewardService` from it):

```
Agent completes
  → ProgressRepository (existing)
  → StreakRepository.recordActivity (existing)
  → ChapterProgressService (existing)
  → GamificationRewardService.awardXpIfEligible(...)
  → QuestEngine.onProgressEvent(...)
  → Optional: RewardOverlay UI event
  → Queue Firestore sync on background
```

### Concept session model (replaces prototype's concept + sim + quiz)

For a plan **Lesson** day, "topic complete" = all **required agents** for that concept (same rules as `ChapterCompletionUseCase`):

- Study (`CONCEPT`) if applicable
- Simulation (`SIMULATION`) if concept has simulation
- Simulation agent (`SIMULATION_AGENT`) if applicable
- Math agent (`MATH_AGENT`) if concept type is math
- Science agent (`SCIENCE_AGENT`) if applicable — 100% nodes

Revision days use `REVISION_AGENT` only.

---

## 8. Exam planner

### Prototype inputs (from `www/index.html`)

```javascript
planSetup: {
  examType: 'Unit Test' | 'Mid-term' | 'Final',
  subject: 'Math',
  chapters: { 'Integers': true, 'Fractions & Decimals': true, ... },
  days: 10,
  minutes: 40  // daily budget
}
```

Plan output: list of `PlanDayNode` with types:

| `PlanDayType` | Meaning |
|---------------|---------|
| `Lesson` | Concept + agents for assigned topics |
| `Revise` | Revision agent for weak topics |
| `Mock` | Full agent checklist for selected chapters (not a quiz) |
| `Exam` | Exam day — encouragement only, no quests |

### `ExamPlanGenerator` (to build)

**Inputs:** exam date, subject, selected chapters, daily minutes, exam type.

**Algorithm (high level):**

1. Expand selected chapters → concepts from Room/NCERT syllabus.
2. Estimate minutes per concept (study + sim + agents).
3. Pack concepts into lesson days within daily minute budget.
4. Insert revision block before exam.
5. Insert one `Mock` day (agent checklist).
6. Final day: `Exam`.
7. Mark `Today` / `Done` / `Upcoming` from calendar.
8. Persist one active plan per student.

**Outputs feed:**

- `HeroFocusCard` — today's focus + minutes
- `PlanTrail` on Home
- Quest node 2 targets (study/plan task)
- Navigation into existing learning flows

### Storage

- Room: `exam_plan` + `exam_plan_day` tables
- Firestore: `exam_plans/eduai_app_{studentId}` (single active doc or subcollection)

### Exam trial (detailed spec)

See **[docs/exam-plan/EXAM_TRIAL_SPEC.md](exam-plan/EXAM_TRIAL_SPEC.md)** for phased trial flow: task ordering (~10 sim + ~10 study per concept, revision on REVISE days only), completion (GE node / 7 clicks / 7 turns, deferred celebration), 3s happy-avatar advance, rollover, deadline delete + feed entry, plan feasibility checks, and Phase E meaningful interaction counts.

---

## 9. Daily quests

### Prototype vs Eduapp

| Node | Prototype | Eduapp (no quiz) |
|------|-----------|------------------|
| **1 — Sims** | Complete 3 simulations | Count `SIMULATION` (+ optional `SIMULATION_AGENT`) completions **today**, scoped to plan concepts |
| **2 — Study** | Quiz 80%+ | **Replace:** complete today's plan task (see below) |
| **3 — Bonus** | +30 gems after 1+2 | Same — both dailies done → claim via rewarded ad |

**UI change:** Rename `QuestTrailState.quizDone/quizTotal` → `studyDone/studyTotal`; update labels and Lottie/icons in `QuestTrail.kt`.

### Node 2 by plan day type

| `PlanDayType` | Completion rule |
|---------------|-----------------|
| **Lesson** | Required agents for today's concept(s) complete |
| **Revise** | 1 `REVISION_AGENT` session for queued topic |
| **Mock** | All pending agents for plan chapters complete |
| **Exam** | No quests |

### Quest reset

- Local midnight → recompute targets from `plan[today]`.
- Count progress from Room (today's rows) — same pattern as streak queries.
- Claim state: `simsClaimed`, `studyClaimed`, `bonusClaimed` per date.

### Gems on quest claim

- **No free gem claim** — prototype rule: gems only after watching ad(s).
- 1 ad → base gems; 2 ads → 2× gems (optional UI from prototype).

---

## 10. XP & gems economy

### Two currencies, different rules

| Currency | Purpose | How earned |
|----------|---------|------------|
| **XP** | Effort, league rank, profile total | Agent completions (automatic) |
| **Gems** | Premium unlocks, streak repair, merch | **Rewarded ads**, invites, spends |

**Prototype rule to keep:** Ad-doubled session XP counts toward **lifetime XP only**, **not** weekly league XP.

### XP award table

| Event | `itemType` / source | XP | League counts? |
|-------|---------------------|-----|----------------|
| Study agent done | `CONCEPT` | 10 | Yes |
| Simulation done | `SIMULATION` | 10 | Yes |
| Simulation agent done | `SIMULATION_AGENT` | 15 | Yes |
| Math agent done | `MATH_AGENT` | 15 | Yes |
| Science agent 100% | `SCIENCE_AGENT` | 20 | Yes |
| Revision agent done | `REVISION_AGENT` | 12 | Yes |
| Session bonus (all required agents for concept) | Derived | +10 | Yes |
| Quest claim | — | **0** | — (XP already comes from the completions the quest wraps — never double-count) |
| Ad double XP | Rewarded ad | +session XP | **No** (lifetime only) |
| Streak day kept | Streak | +5 | Yes |
| Streak milestone (7 / 30 / 50 d) | Streak | +25 / +75 / +150 | Yes — **also pays gems** (see below) |

### Gems award / spend table

| Event | Gems | Mechanism |
|-------|------|-----------|
| Quest claim (sims / study) | 15–20 each | 1 rewarded ad |
| Quest claim 2× option | 2× base | 2 rewarded ads |
| Bonus quest | 30 | 1 rewarded ad |
| Streak milestone (7 / 30 / 50 d) | 25 / 100 / 200 | **No ad** — habit reward (the non-ad gem faucet) |
| Invite reward | 50 + 50 | Both users when invitee completes **first CONCEPT** — no ad (Path B, needs attribution) |
| Streak repair | Spend 150 → 250 → 400 | Gem sink |
| Avatar preset unlock | — | 2 rewarded ads or gems |
| Merch (future) | 300+ | Streak gate + parent approval |

> **Single source of truth.** These XP/gem values are **Remote Config defaults** (`economy_config`) — tune without a release, A/B later. This table is canonical; **Part B does not re-declare it** — it points here. Exact numbers are a balance-pass, not a doc decision.
>
> **Reward integrity (decided):** ad-based gem grants are **server-verified via AdMob SSV** (`admobSsv` + `grantAdReward` Cloud Functions) **from day one** — the main abuse vector. Non-ad grants (streak / invite) are client-side for the pilot but written through **idempotency keys** and the gem balance is **excluded from Android auto-backup** (so a restore can't re-mint). Move all grants server-authoritative before gems become purchasable.

### Idempotency

**XP ledger unique key:** `(studentId, itemType, itemId, language)` — never double-award on replay.

**Gem grant keys:** e.g. `quest_sims_2026-07-23`, `invite_reward_{inviteeId}`.

### Service sketch

```kotlin
// domain/gamification/GamificationRewardService.kt
suspend fun awardXpIfEligible(studentId, itemType, itemId, language): XpAwardResult?
suspend fun awardSessionBonusIfEligible(studentId, conceptId, language): XpAwardResult?
suspend fun grantGems(studentId, amount, reason, idempotencyKey)
suspend fun doubleSessionXp(studentId, sessionKey): XpAwardResult?  // countsForLeague = false
```

Hook from `ProgressEventTracker` after each `mark*Completed` call.

---

## 11. Leagues

### Design (from prototype + ui-kit)

- **Ranked by weekly XP only** — not grades, not lifetime XP, not gems.
- **Tiers:** Bronze → Silver → Gold → (extend later).
- **Board size:** ~15–30 players per cohort.
- **Zones:** Top N promote, bottom N demote, middle safe.
  - ui-kit sample: top **5** promote, bottom **3** demote.
  - HTML prototype: top **3** / bottom **3** — pick one and document in config.

### Weekly cycle

- **Week key:** ISO week e.g. `2026-W29` (define timezone: IST recommended).
- **Reset:** Monday 00:00 or Sunday midnight — pick one globally.
- On new week: assign cohort, reset `weeklyXp = 0`, run promotion/demotion.

### Cohort assignment

1. Read user's `leagueTier` from profile.
2. Find or create cohort: `leagues/{weekKey}/{tier}/cohorts/{cohortId}` with &lt; 30 members.
3. Write member doc: `{ studentId, displayName, weeklyXp, streak }`.

### Low user count (early launch)

Fill empty slots with **soft bots** (deterministic names, XP ~60–80% of median human). Mark `isBot: true` in data. **Do not** use bots in friends feed.

### End-of-week processing

**Recommended:** Cloud Function (scheduled) — not client-only.

1. Sort cohort by `weeklyXp`.
2. Promote top N, demote bottom N.
3. Write `LEAGUE_PROMOTED` feed events for friends.
4. Reset weekly XP for new week.

### Client read strategy (cost control)

| Surface | Read pattern |
|---------|--------------|
| Home top bar (rank chip) | Cached from Room, stale ≤ 1 hour |
| Leagues tab | Fetch cohort once on open (~≤30 docs) |
| Background sync | Write own member doc only |

**Do not** poll full leaderboard on every Home load.

---

## 12. Friends & social feed

### Friend model

- Bidirectional, accepted connections.
- **Invite code** on profile (e.g. `AANYA7X2`) + deep link / QR.
- Add flow: enter code → `PENDING` → `ACCEPTED` (or auto-accept).

**Invite reward:** when invitee completes first `CONCEPT`, both get **50 gems** once (`inviteRewardGranted` flag).

### Phasing — linking (Path A) vs install reward (Path B)

Two **separable** problems that ship in different phases — don't bundle them:

- **Path A — add/scan friend code → link.** Code lookup → symmetric edges (`friends/.../connections/...`), cached in Room; **auto-accept** on a valid unguessable code (8 chars, non-sequential) is the recommended Class-7 MVP. **Needs no install attribution**, so friend-linking + feed + cheers can ship as soon as the friends feature lands — it does **not** wait on deep-link infra.
- **Path B — invite → install → +50/+50 gems.** Requires **install attribution** (which code was used at install). **Firebase Dynamic Links shut down (Aug 25, 2025)**, so this needs **Play Install Referrer** (`…&referrer=CODE`) or **App Links** (`https://padaams.in/join?code=AANYA7X2`) + a landing endpoint before the reward can be granted. Until attribution exists, Path A works but the automatic +50/+50 does **not** — fall back to the invitee entering the code once during onboarding.
- Grant invite gems **server-side** (`claimReferral` Cloud Function) with anti-abuse: no self-referral, device/account de-dup, once per pair. Invite gems are the one economy exception not tied to a rewarded ad.

### Empty state (0 friends)

| Surface | UX |
|---------|-----|
| `FriendsUpdatesRail` | "Invite a friend — you both earn 50 gems" + CTA |
| Leagues → Feed tab | Same empty CTA |
| Profile | "Friends · 0" + Add (code / QR) |

Never show fake friends in production feed.

### Friend feed events

Write on milestones:

| `eventType` | Example |
|-------------|---------|
| `STREAK_MILESTONE` | "Reached a 30 day streak" |
| `LEAGUE_PROMOTED` | "Promoted to Silver League" |
| `CONCEPT_MILESTONE` | "Completed 10 concepts" |
| `BADGE_EARNED` | Future |

**Cheer:** one cheer per user per event; increment counter; no XP/gems for cheering.

**Seen state:** `seen = false` until user opens feed/card.

### Leagues screen tabs

- **Leaderboard** — cohort ranking (ui-kit `EduLeaguesScreen`).
- **Feed** — friend updates (prototype `screenLeagues` feed mode).

---

## 13. Avatar migration (boy/girl → Free tutor)

### Today (Eduapp)

- WebView loads `assets/LipSync.html` with boy/girl PNG + SVG visemes.
- `AvatarChangeUseCase`: `"boy" | "girl" | "disable"`.
- `TextToSpeech.switchCharacter()` → JS bridge.
- Used in: Chatbot, Simulation Agent, Revision, Math Agent (`InitialAvatarView`).

### Target (Gamification ui-kit)

- `EduTutorAvatar` + `TutorCharacter.Free` — **pure Compose**, no bitmap assets.
- `EduappVisemeMapper` — ported from same `LipSync.html` logic.
- `LipSyncController` with `EduappViseme` mode for TTS word boundaries.
- Avatar Studio for customization; Orb + Free characters; premium presets via ads/gems.

### Migration steps

1. Default tutor → `TutorCharacter.Free` (not boy/girl).
2. Replace `InitialAvatarView` WebView with `EduTutorAvatar` + lip-sync controller on **one agent screen** (pilot), then roll out.
3. Wire TTS speaking state → `LipSyncController`.
4. Replace settings boy/girl dropdown with avatar picker / link to Avatar Studio.
5. Persist `TutorConfig` in Room/Firestore (not ui-kit SharedPreferences alone).
6. Remap voice filters (today keyed on boy/girl) to neutral or preset-based defaults.
7. Remove `LipSync.html` + WebView path when all agents migrated.

---

## 14. Ads: banner & rewarded video

### Today (Eduapp)

| Piece | Role |
|-------|------|
| `MobileAdsInitializer` | Child-safe: COPPA, max rating **G** |
| `AdManager` + `BannerAdView` | Banner only |
| `ClickAdGate` | After **5 free clicks/day**, navigation shows ad dialog |
| `NavigationAdGate` + `AdDialog` | Banner in modal before continue |
| `AdAnalyticsTracker` | Banner → Firestore + GA4 |
| AdMob units | **Banner only** — see `scripts/admob-firebase-setup.md` |

### Gamification today

- `AdRewardOverlay` in ui-kit = **simulated countdown** — replace with real AdMob rewarded callbacks.

### Ad format strategy

| Format | Use |
|--------|-----|
| **Banner** | Keep — passive, click-gate on learning navigation |
| **Rewarded video** | **Add** — all gem grants, avatar unlock, XP double |
| **Interstitial** | **Skip v1** — forced full-screen; poor fit for child-directed app |

User must **opt in** ("Watch short video") for all gem/XP boosts.

### Rewarded placements

| # | Trigger | Ads | Reward |
|---|---------|-----|--------|
| 1 | Quest claim (sims) | 1 | +15–20 gems |
| 2 | Quest claim (study) | 1 | +15–20 gems |
| 3 | Quest claim 2× option | 2 | 2× gems |
| 4 | Bonus quest | 1 | +30 gems |
| 5 | Session complete — double XP | 1 | 2× session XP (lifetime only) |
| 6 | Avatar preset unlock | 2 | Unlock preset |
| 7 | Save custom avatar | 2 | Persist custom look |

### `RewardedAdManager` (to build)

```
RewardedAdManager
├── preload() on app start + after each show
├── show(placement, onReward, onDismiss(earned))
├── showSequence(count, ...)  // "watch 2 ads"
└── isReady(): Boolean
```

**Grant rule:** only in `OnUserEarnedRewardListener` — never on open, timer, or dismiss without earn.

Replace ui-kit fake overlay with loading UI → full-screen AdMob rewarded ad.

### AdMob setup

1. AdMob Console → Create ad unit → **Rewarded**.
2. Add to `local.properties`:

```properties
REWARDED_AD_UNIT_ID=ca-app-pub-6484226294015492/XXXXXXXX
```

3. Debug test ID: `ca-app-pub-3940256099942544/5224354917`
4. Same child-directed console settings as banner.

### Analytics extensions

```kotlin
enum class AdType { BANNER, REWARDED }

enum class AdPlacement {
    AD_DIALOG,
    QUEST_CLAIM,
    QUEST_BONUS,
    SESSION_DOUBLE_XP,
    AVATAR_UNLOCK,
    AVATAR_SAVE,
}

enum class AdInteraction {
    // existing LOADED, IMPRESSION, ...
    REWARD_EARNED,
    REWARD_SKIPPED,
    NOT_READY,
}
```

### Frequency caps (recommended)

| Placement | Cap |
|-----------|-----|
| Quest claims | Max 3 rewarded/day |
| Double XP | Once per session summary |
| Avatar unlock | User-initiated, no daily cap |

### Failure handling

| Case | Behavior |
|------|----------|
| Not loaded | Preload on Home; retry once |
| User closes early | No reward; quest stays unclaimed |
| No fill | Message + retry later; no grant |
| Offline | Disable "Watch ad" buttons |
| 2-ad sequence | Second ad only after first earn; partial grant if second fails |

---

## 15. Database design (Room + Firestore)

Follow existing pattern: **Room first, sync on background** (`DataSyncService`, `AppLifecycleObserver.onStop`).

### Room tables (new)

#### `gamification_profile`

| Column | Type | Notes |
|--------|------|-------|
| studentId | PK | |
| lifetimeXp | Int | |
| weeklyXp | Int | League metric |
| gems | Int | |
| leagueTier | String | BRONZE, SILVER, GOLD |
| currentWeekKey | String | e.g. 2026-W29 |
| cohortId | String? | |
| friendCode | String | Unique, generated once |
| invitedByCode | String? | |
| inviteRewardGranted | Boolean | |
| updatedAt | Long | |
| isSynced | Boolean | |

#### `xp_event` (ledger)

| Column | Notes |
|--------|-------|
| id | PK |
| studentId, itemType, itemId, language | UNIQUE together |
| xpAmount | |
| weekKey | |
| countsForLeague | false for ad-bonus XP |
| createdAt, isSynced | |

#### `quest_daily`

| Column | Notes |
|--------|-------|
| studentId + date | Composite PK |
| simsDone, simsTotal, studyDone, studyTotal | |
| simsClaimed, studyClaimed, bonusClaimed | |
| isSynced | |

#### `exam_plan` / `exam_plan_day`

Plan metadata + ordered days (`PlanDayType`, label, status, conceptIds).

#### `friend_connection`

| Column | Notes |
|--------|-------|
| studentId + friendStudentId | PK |
| status | PENDING, ACCEPTED |
| displayName | |
| createdAt, isSynced | |

#### `friend_feed_item`

| Column | Notes |
|--------|-------|
| id | PK |
| fromStudentId, eventType, message | |
| cheers, cheeredByMe, seen | |
| createdAt, isSynced | |

#### `league_cache` (optional)

Cache last cohort fetch: weekKey, cohortId, rank, participantsJson, fetchedAt.

### Firestore collections (extend `firestore.rules`)

```
gamification/eduai_app_{studentId}
  → profile fields, appName: eduai_app

gamification/eduai_app_{studentId}/xp_events/{eventId}
  → optional audit mirror

exam_plans/eduai_app_{studentId}
  → active plan snapshot

quests/eduai_app_{studentId}/daily/{yyyy-MM-dd}
  → quest progress + claim flags

leagues/{weekKey}/{tier}/cohorts/{cohortId}/members/{studentId}
  → weeklyXp, displayName, streak, isBot?

friends/eduai_app_{studentId}/connections/{friendStudentId}
  → status, displayName

social_feed/{eventId}
  → fromStudentId, eventType, message, cheers, createdAt
  → read: friend connection exists
```

Reuse existing helpers: `FirestoreSyncUtils.studentAppDocId()`, `appName == eduai_app`, same security pattern as `progress/`, `streak/`.

### Sync triggers

Same as today:

- App background (`onStop`)
- Network reconnect
- Login
- Optional: WorkManager retry for gamification profile (not required for v1 if profile merges on background)

---

## 16. Firestore free tier & cost control

Gamification **does not increase** Spark plan quotas. Same project limits for **all** collections:

| Quota | Spark (free) / day |
|-------|---------------------|
| Document reads | 50,000 |
| Document writes | 20,000 |
| Document deletes | 20,000 |
| Storage | 1 GiB total |

References: [Firestore pricing](https://firebase.google.com/docs/firestore/pricing), [Firebase pricing](https://firebase.google.com/pricing/).

### Incremental usage (well-designed)

| Per active student / day | Approx. |
|--------------------------|---------|
| XP events + profile merge | 1–2 writes (**batched at session end**) |
| Quest daily doc | 1–2 writes |
| League member update | 1 write (`increment`, batched) |
| Friend cheer / feed | 0–2 writes |
| **Leagues tab open** | **1 read** (aggregated standings doc — see below) |

**Reality check on the free tier.** **Writes are the binding constraint** (20K/day). At ~5–8 writes/student/day, gamification alone starts exceeding Spark writes around **~2.5–3K DAU** — and that's *before* the app's existing Firestore usage. So:

- **Spark is fine for dev + small pilots**, not production DAU.
- **Plan for Blaze (pay-as-you-go)** at real scale — it keeps the same free daily quotas, then bills per op (~$0.06/100K reads, ~$0.18/100K writes). With batched/aggregated writes, gamification cost at 20K DAU is ~$15–30/mo — negligible.
- **Batch writes regardless of plan** (accumulate XP/gems/quest progress locally in Room, flush once per session), and use **one aggregated standings doc per cohort** (1 read renders the whole board) instead of per-member reads.

### Highest risk

- **League leaderboard reads** if polled frequently — use the **aggregated standings doc** (1 read) + cache in Room, fetch on Leagues tab only. Do **not** read ~30 member docs per open.
- **Duplicating progress** in Firestore — derive quest completion from existing `progress` rows where possible.
- **Never write per-tap/analytics events to Firestore** — those go to GA4 (see analytics), or they'll blow the write budget.

### If limits exceeded

Spark: Firestore stops until midnight Pacific — upgrade to Blaze for overages.

---

## 17. Analytics

### Existing

- Screen events, funnel, session tracking, banner ad analytics.

### Add for gamification

| Event | When |
|-------|------|
| `xp_earned` | Agent completion |
| `gems_earned` / `gems_spent` | Ad reward, invite, repair |
| `quest_completed` / `quest_claimed` | Daily quests |
| `league_promoted` / `league_demoted` | Week rollover |
| `friend_added` / `friend_cheered` | Social |
| `rewarded_ad_*` | Extend `AdAnalyticsTracker` |

Map to GA4 + Firestore analytics collection (same pattern as `AdAnalyticsTracker`).

---

## 18. App size estimate

Measured ui-kit source ~**0.59 MB**; Lottie JSON ~**0.31 MB**. Eduapp has no Lottie today.

| Addition | Release APK impact |
|----------|-------------------|
| ui-kit Compose (DEX, R8) | +400 KB – 1 MB |
| Lottie Compose library | +300 – 500 KB |
| 7 Lottie JSON files | ~+300 KB |
| Free avatar (Compose, no PNGs) | Negligible |
| Remove WebView lip-sync later | −50 – 200 KB |

**Net estimate: +1.5 – 2.5 MB** on release build.

Do **not** bundle `www/` HTML prototype inside Eduapp.

---

## 19. Phased rollout

| Phase | Scope | Exit criteria |
|-------|--------|---------------|
| **0** | ui-kit module in repo; sample compiles | Gradle merge clean |
| **1** | New Home behind feature flag; real streak/progress in hero | Old Home fallback works |
| **2** | `GamificationRewardService` + XP ledger; all 6 agent types | RewardOverlay on completion |
| **3** | Exam planner generator + Plan tab | Today focus from real plan |
| **4** | Daily quests (no quiz) + quest UI | Claims gated on progress |
| **5** | Rewarded ads + gem economy (+ AdMob SSV for ad gems) | AdMob earn callback before grant |
| **6** | Leagues + weekly reset job (anonymous cohorts — no friends needed) | Board + promotion works |
| **7** | Friends **Path A** — add/scan code (auto-accept) + feed + cheers | Feed + cheers live; **no install attribution needed** |
| **8** | Avatar pilot on one agent → full rollout | WebView removed |
| **8b** | Friends **Path B** — install attribution + invite +50/+50 gems | Play Install Referrer / App Link captured; **server-side** grant (FDL is dead) |
| **9** | Full 6-tab nav; deprecate old Progress tab | Play rollout 100% |

> **Leagues (6) and Path A friends (7) are not deferred** — they ship in the same MVP bucket as quests/ads (Part B §16 Phase 1), not after avatar. Avatar (8) can parallelize once lip-sync is ready.
>
> Path A (linking) and Path B (install reward) stay split: 7 needs no install attribution; 8b is gated on post-FDL attribution infra and must not block friend-linking.

### Launch buckets (aligns with Part B §16)

| Bucket | Master phases | Notes |
|--------|---------------|-------|
| **MVP (Part B Phase 1)** | **0–7** (+ **8** avatar when ready) | Flagged production slice: gamified Home, economy, quests, ads, **leagues**, **friends by code**. Path B gems **not** required. |
| **Retention (Part B Phase 2)** | **8b** + FCM + localization | Referral +50/+50 gems; push re-engagement |
| **Scale (Part B Phase 3)** | **9** at 100% + ops / optional IAP | Full nav; admin dashboard if needed |

---

## 20. Testing checklist

### Learning (no regression)

- [ ] Login → existing flows: chapter → concept → study agent
- [ ] Simulation HTML + interaction tracking still syncs
- [ ] Simulation agent, math agent, science agent, revision agent complete
- [ ] Progress + streak + chapter progress unchanged
- [ ] Kannada / English progress language split intact

### Gamification

- [ ] XP awarded once per agent completion (replay does not double)
- [ ] Session bonus when all required agents for concept done
- [ ] Weekly XP increments; ad-double XP does **not** affect league
- [ ] Quest progress from today's plan + agent events
- [ ] Quest gems only after rewarded ad earn callback
- [ ] Bonus quest after both dailies
- [ ] Exam plan generates sensible day list
- [ ] League board loads; rank matches weekly XP
- [ ] Week rollover promotes/demotes correctly
- [ ] Friend code add; invite gems on first concept
- [ ] 0 friends shows invite CTA, not mock data
- [ ] Cheer once per feed item
- [ ] Avatar lip-sync on TTS in agent screen
- [ ] Avatar unlock after 2 rewarded ads

### Ads & policy

- [ ] Banner click-gate still works (5 free clicks)
- [ ] Rewarded test ads on debug device
- [ ] No reward if user skips video
- [ ] Child-directed / G-rated ad config unchanged

### Offline / edge

- [ ] XP/quests work offline; sync on background
- [ ] Force-stop retains local data until next sync

---

## 21. What not to do

- Do **not** ship WebView HTML prototype (`www/`) as main Eduapp UI.
- Do **not** add quiz flows or "Quiz 80%+" quest nodes.
- Do **not** replace bottom nav with 6 tabs on day one without mapping Settings/Progress.
- Do **not** store gamification state only in SharedPreferences.
- Do **not** fork streak/XP — extend `ProgressEventTracker` / `StreakRepository`.
- Do **not** grant gems without rewarded ad completion (except **streak milestones** and **invite reward** — see §10).
- Do **not** poll full league board on every Home open.
- Do **not** show fake friends in production feed.
- Do **not** block learning behind ads.

---

## 22. Open decisions

| # | Decision | Options | When to lock |
|---|----------|---------|--------------|
| 1 | Week reset timezone | IST vs UTC | Before league rollover Function |
| 2 | Promotion/demotion counts | Top/bottom 3 vs 5 (ui-kit vs HTML) — **Remote Config defaults**, not a release blocker | Before leagues ship (Phase 6) |
| 3 | Tab rollout | Big-bang 6-tab vs incremental | Phase 9 |
| 4 | Theme | Adopt ui-kit tokens app-wide vs bridge | Phase 1 |
| 5 | Onboarding | Gamification 3-slide vs existing login/user-detail only | Phase 1 |
| 6 | League bots | Enable until N real users per cohort | Phase 6 |
| 7 | Cloud Functions | Firebase Functions vs external cron for week rollover | Phase 6 (Functions recommended) |
| 8 | Progress tab | Keep alongside Plan or merge | Phase 9 |
| 9 | Path B referral attribution | Play Install Referrer vs `https://padaams.in/join?code=` App Links vs third-party (Branch, etc.) | **Phase 8b only** — does not block Path A (Phase 7) |

---

## 23. Related docs & files

### Eduapp

| Path | Topic |
|------|-------|
| `docs/APP_STRUCTURE.md` | App architecture, existing AdMob |
| `docs/DEV_CHANGELOG_JUN20-22.md` | Recent sync, ads, simulation tracking |
| `scripts/admob-firebase-setup.md` | AdMob + Firebase checklist |
| `firestore.rules` | Current security rules |
| `app/.../ProgressEventTracker.kt` | Agent completion hooks |
| `app/.../StreakRepository.kt` | Streak pattern to mirror |
| `app/.../service/ads/` | Banner ads today |
| `app/.../service/sync/DataSyncService.kt` | Sync pattern |

### Gamification repo

| Path | Topic |
|------|-------|
| `phase0-native/ui-kit/` | Compose UI kit to merge |
| `PHASE0_TASKS.md` | Phase 0 task tracker |
| `www/index.html` | Product behavior reference (exam plan, quests, ads, leagues) |
| `phase0-native/ui-kit/.../QuestTrail.kt` | Quest UI (replace quiz node) |
| `phase0-native/ui-kit/.../AdReward.kt` | Simulated rewarded ads — replace |
| `phase0-native/ui-kit/.../EduTutorAvatar.kt` | Target avatar renderer |

### External

- [Gamification GitHub](https://github.com/ANURAGMN/Gamification)
- [Firestore pricing](https://firebase.google.com/docs/firestore/pricing)
- [AdMob rewarded ads (Android)](https://developers.google.com/admob/android/rewarded)

---

*End of plan. Update this doc as phases complete or decisions are made.*
