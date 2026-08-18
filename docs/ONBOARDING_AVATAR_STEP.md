# Onboarding — "Choose your tutor" avatar step

Add a tutor-avatar pick to first-run onboarding: choose one of ~10 named looks (Scholar, Nova, Ace, Sage, …). Plan only.

## Good news: the 10 presets already exist
`ui-kit/.../avatar/AvatarPresets.kt` → `AllAvatarPresets` already defines the exact set:
Scholar · Nova · Ace · Sage · Spark · Pulse · Quill · The Inventor · The Astronomer · The Naturalist · The Mathematician (11 total). Each is `AvatarPreset(id, name, tagline, config: TutorConfig)`.
And there's a renderer: `EduTutorAvatar(config = preset.config, …)` (`avatar/EduTutorAvatar.kt`). So the step is **reuse**, not new art.

Today these are *premium unlockables* (rewarded-ad gated via `AvatarUnlockStore`). For onboarding the chosen one should be **free** — unlock it on pick.

## The step (in `ui-kit/.../screens/OnboardingScreen.kt`)
Current flow: 3 slides → Subject → Chapter → World → `onFinish`. Add **Tutor** after World, before "Build my plan":
Subject → Chapter → World → **Tutor** → finish.

- New `TutorStep` composable: title "Meet your tutor" / sub "Pick who'll guide you — you can change it later.", then a scrollable **2-column grid** of cards. Each card = `EduTutorAvatar(config = preset.config)` (in a soft `forRole`-tinted circle/box) + `preset.name` (bold) + `preset.tagline`; selected card gets the 2px accent border + check (same pattern as `WorldCard`).
- Selection state `var tutor by rememberSaveable { mutableStateOf("") }` (preset id). "Build my plan" enabled once non-empty.

## Data + persistence
- `OnboardingResult` (line 69): add `val avatarPresetId: String`. `onFinish(OnboardingResult(subject, chapter, world, tutor))`.
- Add `onAvatarSelected: (String) -> Unit = {}` param (analytics), mirroring `onWorldSelected`.
- In `LoginNavigator` `onFinish`: besides `setFirstRunResult(...)`, persist the tutor:
  - resolve `AllAvatarPresets.first { it.id == result.avatarPresetId }`,
  - save its `config` as the user's tutor via the existing tutor-config path (`TutorConfigMapper` → `TutorConfigEntity` / the `TutorConfigViewModel.saveConfig(config, presetId)` used by Avatar studio),
  - `AvatarUnlockStore.unlock(context, presetId)` so the onboarding pick is free and appears unlocked in Avatar studio,
  - store `onboarding_avatar` in prefs (analytics/restore parity with `onboarding_world`).
- `SharedPreferenceUtils`: add `KEY_ONBOARDING_AVATAR` + getter (mirror `getOnboardingWorld`). `HomeViewModel.applyOnboardingPicksOnce` already applies world → apply avatar there too if you prefer a single apply point (instead of at finish).

## Copy (EN / KN)
Title/sub + "Build my plan" already localized. Preset `name`/`tagline` are in-code English today; add KN names/taglines to `AvatarPreset` (or a KN lookup) if you want them localized — otherwise keep the names as-is (proper-noun-ish) and localize only title/sub.

## Free vs premium
Onboarding = free pick (unlock the chosen). The **other** presets stay premium in Avatar studio (their unlock flow is unchanged). Optionally mark a small "starter" subset as always-free; simplest is: whatever they pick in onboarding is unlocked, the rest follow the existing weekly-drop/unlock rules.

## Cross-cutting
- **Dark mode:** `EduTutorAvatar` + card tokens (`forRole`, `colors.*`) → no contrast risk.
- **Perf:** ~10 vector avatars on one scrollable screen is fine; they're lightweight.
- **Analytics:** add `FunnelStep.ONBOARDING_AVATAR_SELECTED` + `EngagementAnalyticsTracker.onboardingAvatarSelected(id)` (mirror world).
- **Re-trigger:** rides the existing first-run gate (`hasCompletedFirstRun`); fresh install re-runs it.
- **Tour tie-in:** the Avatar tab tour (Scene/Journey/Look) already exists; this pick just seeds the starting look.

## Files touched
`OnboardingScreen.kt` (new step + `OnboardingResult` field + param), `OnboardingCopy.kt` (title/sub strings), `LoginNavigator.kt` (persist + unlock), `SharedPreferenceUtils.kt` (+ key), optionally `HomeViewModel` (apply), analytics enums/tracker. Reuses `AllAvatarPresets` + `EduTutorAvatar` as-is.

## Effort / risk
Small–medium. Main work is the `TutorStep` grid + persisting the chosen `TutorConfig` through the existing tutor-config store. No new avatar art. Verify on device (can't preview avatar rendering statically).
