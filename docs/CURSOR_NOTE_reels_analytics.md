# Cursor: rebuild + review — Reels/Video analytics

All **uncommitted**; pull latest. **Not compiled here** — build + run tests. Behind `REELS_ENABLED` (the reels tab/player are already flag-gated). Capture reuses the existing `AnalyticsEventRecorder` → `app_analytics` (Room) → GA4 pipeline (same as `NavClickAnalyticsTracker`). **No Firestore** — `AnalyticsFirestoreMirror.ENABLED` stays `false` by design (GA4-only for high-frequency events); events write to Room + GA4, not Firestore.

## What's captured
- **Screen dwell** — `TrackScreenEvent(ScreenName.REELS)` on explore; `TrackScreenEvent(ScreenName.REELS_PLAYER, conceptId = videoId)` on the player (per-video watch screen row).
- **Open** (`reel_open`, `EventType.CLICK`) — video id + section (`newest` / `most_watched` / `search`) + 0-based grid position + query (search only).
- **Watch** (`reel_watch`, `EventType.COMPLETE`) — foreground-aware watched-ms via `ReelWatchTracker`; completion null (nocookie embed has no duration signal).
- **Search** (`reel_search`) — committed query (IME action) + result count.

## Files
New (pure, unit-tested — no Android deps):
- `domain/reels/analytics/ReelSection.kt` — enum newest/most_watched/search.
- `domain/reels/analytics/ReelWatchTracker.kt` — watch-time accumulator (idempotent play, no-op pause, backward-time clamp, per-span cap, live span) + `completionFraction`.
- `domain/reels/analytics/ReelsAnalytics.kt` — `interactionType` encoders + GA4 param maps + query sanitize / percent.

New (thin I/O — mirrors `NavClickAnalyticsTracker`):
- `service/analytics/ReelsAnalyticsTracker.kt` — `trackOpen` / `trackWatch` / `trackSearch` (IO scope → `AnalyticsEventRecorder.recordClick` + `FirebaseAnalyticsHelper.logEvent`).

Edited:
- `service/analytics/AnalyticsEnums.kt` — `ScreenName.REELS`, `ScreenName.REELS_PLAYER`.
- `ui/screens/reels/ReelsViewModel.kt` — `onReelOpened(video, section, position)`, `onSearchCommitted(resultCount)`.
- `ui/screens/reels/ReelsExploreScreen.kt` — `TrackScreenEvent(REELS)`; grid → `itemsIndexed` passing section+position; IME `onSearch` → `onSearchCommitted`.
- `ui/screens/reels/ReelsPlayerScreen.kt` — `TrackScreenEvent(REELS_PLAYER, videoId)`; `DisposableEffect` drives `ReelWatchTracker` (ProcessLifecycle ON_START/ON_STOP) → `trackWatch` on dispose.

Tests:
- `test/.../domain/reels/analytics/ReelWatchTrackerTest.kt` (12) + `ReelsAnalyticsTest.kt` (13). Pure logic cross-checked off-device: 0 failures. Run `./gradlew :app:testDebugUnitTest`.

## Notes
- `AnalyticsEventRecorder` must be `initialize(context)`-d at app start (already is — used by existing trackers).
- To ever send reels events to Firestore, flip `AnalyticsFirestoreMirror.ENABLED` (affects **all** event analytics, not just reels) — not needed for this ask.
