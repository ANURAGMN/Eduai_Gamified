# Cursor: gem chip — icon swap (badge/medal → diamond)

**Uncommitted, ui-kit only. Not compiled here — build to confirm.** Cosmetic; fold into the QA-fixes commit.

## Context
The top-bar "gems" chip already showed the **real** gem count (`gamificationProfile.gems`) and was re-captioned **Gems** (`HomeCopy.gemsCaption`). The only thing still reading as "badges" was the **icon** — `Icons.Outlined.WorkspacePremium` (a medal/rosette). Swapped to `Icons.Outlined.Diamond` everywhere a gem value is shown.

## Changes (3 files)
- `ui-kit .../components/TopBarChips.kt` — gems chip leading icon `WorkspacePremium` → `Diamond` (import + usage).
- `ui-kit .../components/RewardOverlay.kt` — `RewardStat` gems leading icon → `Diamond`. Verified the icon renders **only** on the gems stat (`leadingIcon = true`); the XP stat passes no leading icon, so no XP was mislabeled.
- `ui-kit .../components/PlanTrialAdvanceOverlay.kt` — `TrialRewardStat` gems leading icon → `Diamond`. Same check: `leadingIcon = true` is only on the `gemsEarned` stat; XP / bonus-XP stats have none.

## Verify
- `grep -rc WorkspacePremium app/src/main/java ui-kit/src/main/java` → **0** (clean everywhere).
- All 3 files brace/paren balanced.
- No behavior/logic change — icon vector only. Reward overlays: `RewardOverlay` is currently suppressed (per `GamificationRewardHost`); `PlanTrialAdvanceOverlay` still shows.

## Not changed (intentionally)
- `EmojiEvents` (trophy) usages — those are leagues / friends / achievements, **not** gems. Left as-is.
