# Notifications — system, fixes, and verification

**Scope:** the local reminder/engagement notification engine in
`app/src/main/java/com/ncert7/aitutorandlab/notification/`. All notifications are **device-local**
(scheduled + evaluated on-device); there is no server push. Timezone is **Asia/Kolkata (IST)** throughout.

## How it works (pipeline)

1. **Schedule** — `NotificationScheduler` registers a daily **inexact** alarm (`DailyReminderReceiver`) and a
   WorkManager periodic sweep (`notification_eval_sweep`). `NotificationBootReceiver` re-registers both after
   reboot / app update.
2. **Evaluate** — the alarm/sweep runs `NotificationEvalWorker` → `NotificationEvaluator.eligibleCandidates()`,
   which builds a `NotificationSnapshot` (streak, tasks, plan, activity) and returns the eligible
   `NotificationType`s for *now*, filtered by reminder mode, quiet hours, and per-type rules.
3. **Gate & send** — `NotificationOrchestrator` checks the `NotificationLedger` (dedup + daily budget + min
   gap) and permission, then `NotificationHelper` posts the notification (avatar large-icon from
   `NotificationAvatarCache`, deep-link route attached).
4. **Tap** — the deep-link route is captured in `MainActivity` (`NotificationDeepLinkStore`) and routed to the
   right screen.

## Notification catalog (`NotificationType`)

| id | deep link | notes |
|---|---|---|
| `exam_countdown` | plan | only within `EXAM_COUNTDOWN_MAX_DAYS` (14) |
| `streak_at_risk` | trial | evening only (≥ 18:00) |
| `tasks_pending` | trial | |
| `chapter_progress` | chapter | |
| `daily_reminder` | trial | fires in a ±30-min window around the user's reminder time |
| `streak_saved` / `streak_comeback` | home / trial | |
| `inactivity_3` / `inactivity_7` / `inactivity_14` | home | tiered re-engagement |
| `weekly_xp_close` | progress | |
| `avatar_unlock_expiring` | avatar_studio | within 24h of weekly drop (`NotificationAvatarRules`) |

## Fixes / hardening in place

- **Play-policy-safe scheduling.** Inexact alarms only — *no* `SCHEDULE_EXACT_ALARM` (that permission is
  restricted to alarm-clock apps and triggers review rejections). See `NotificationScheduler` (“Inexact alarm
  only — no SCHEDULE_EXACT_ALARM (Play policy for non-alarm-clock apps)”).
- **Anti-spam budget.** `NotificationLedger` (Room-backed) enforces a **daily cap** and a **2-hour minimum
  gap**, plus a **dedup key** so the same reminder can't double-fire. Cap is mode-driven:
  `OFF=0, GENTLE=1, STANDARD=3` (default STANDARD), coerced to `DEFAULT_DAILY_CAP`.
- **Quiet hours.** `NotificationTimeRules.isQuietHours` suppresses engagement sends inside the user's
  window and is **overnight-aware** (handles `start > end`, e.g. 22:00–07:00). **Exception:** when the
  scheduled daily alarm fires (`NotificationEvalTrigger.DAILY_ALARM`), `DAILY_REMINDER` may still send
  so the user's explicit reminder time is not dropped by quiet hours or inexact-alarm drift. Streak /
  quest / inactivity / other types remain suppressed during quiet hours.
- **Reboot / update persistence.** `NotificationBootReceiver` reschedules on `BOOT_COMPLETED` (and package
  replace) so reminders survive a restart.
- **Android 13+ permission primer.** `NotificationPermissionGate` requests `POST_NOTIFICATIONS` only after a
  meaningful win (streak/quest/trial reward), **at most once/day, up to 3 times total**, each with a different
  framing, and stops entirely once granted or the OS dialog has shown — so it never nags.
- **Deep links.** Every type carries a `deepLinkRoute`; taps open the correct screen via
  `NotificationDeepLinkStore` captured in `MainActivity.onCreate`.
- **User control.** Settings expose master on/off + reminder mode (Off / Gentle / Standard) + quiet-hours
  start/end (`NotificationSettingsStore`).
- **Avatar icon.** `NotificationAvatarCache` supplies the user's avatar as the large icon (skips gracefully
  when no activity context).

## Test coverage (`app/src/test/.../notification/`)

`NotificationEvaluatorTest`, `NotificationLedgerTest`, `NotificationTimeRulesTest` /
`NotificationTokensTest`, `NotificationContentCatalogTest`, `NotificationDeepLinkMapperTest`,
`NotificationSettingsStoreTest`, `NotificationAvatarRulesTest`, `NotificationTypeTest`. These cover the pure
rules (eligibility, budget/dedup, quiet-hours math, content tokens, deep-link mapping, settings) on the JVM.

## Verify on device (QA checklist)

The pure logic is unit-tested, but scheduling/permission behaviour is device-specific — confirm on a real
device before relying on it:

1. **Reminder fires** at (±30 min of) the set daily time; not before/after the window.
2. **Quiet hours respected** — set an overnight window and confirm engagement types are suppressed
   (incl. across midnight). Separately: if reminder time falls inside quiet hours, the **daily alarm**
   may still deliver `daily_reminder` (by design); other types must not.
3. **No spam** — with STANDARD, never more than 3/day and never < 2h apart; the same type doesn't double-fire.
4. **Tap routing** — each notification opens its deep-link screen (plan/trial/chapter/home/progress/avatar).
5. **Permission primer** (Android 13+) — appears after a win, is capped/non-nagging, stops once granted.
6. **Survives reboot** — reminders still fire after a device restart and after an app update.
7. **Off mode** — master-off / mode OFF produces zero notifications.

## Pending / follow-ups

- Confirm the device-QA items above on Android 13/14 hardware (permission + inexact-alarm timing vary by OEM).
- If any specific QA item from device testing is still open (e.g. a reminder not firing on a particular OEM),
  capture the device model + Android version — OEM battery/doze policies are the usual cause for missed
  inexact alarms, and may need a WorkManager fallback tightening rather than a code-logic fix.

*This documents the notification subsystem as it stands in the current tree. It is a reference + QA checklist,
not a claim that every device-level item has been verified — items under “Verify on device” still need a
hardware pass.*
