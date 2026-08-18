# Reels (video lessons) — feature spec

Supersedes the primary approach in `INSTAGRAM_LESSONS_INTEGRATION.md`. Source is **YouTube Shorts** (auto-synced), shown in a **Reels** tab. Behind a feature flag, **post-release** (does not gate v1.0.11).

## Decisions locked
- **Bottom nav:** a **Reels** tab (film icon `ti-movie`) **replaces the Quests** tab in the bottom bar → Home · Plan · **Reels** · Leagues · Avatar · Profile. Tapping it opens the **Reels explore** screen. Separately, **Quests moves into Settings** as a row that opens the existing Quests screen (`QuestsRoute`), mirroring how **Progress** is linked from Settings today.
- **Videos also on Home:** a **Home rail** placed **above the Garden/Space section** (reuses/repositions the existing `YoutubeVideosSection`). Shows a horizontal preview (latest ~6); a **"See all →"** opens the same **Reels explore** screen. So Reels explore has two entry points — the bottom-nav Reels tab and the Home rail's See all.
- **Home top strip (related layout change):** merge **Today's focus** (hero) + **Your tutor** into a **single compact row** — tutor avatar + current task + Start CTA — targeting ~40% less vertical height than the two stacked sections. **New Home order:** header → merged focus/tutor strip → **Subjects** → Video lessons rail → Garden/Space → Friends update.
- **Reels explore screen (via See all):** fixed **3 columns × 4 rows = 12** grid + search. Rows 1–2 = **newest** (top 6 by `publishedAtMillis` desc). Rows 3–4 = **most-watched** (top 6 by `viewCount` desc), **deduped** against the newest 6. Backfill if <12 so no blanks (owner keeps ≥12 uploaded).
- **Source:** YouTube Shorts from your channel, **auto-synced** via the YouTube Data API into the existing Firestore `youtube_videos` collection (backend job). No app release needed to add videos.
- **Views:** show YouTube **`viewCount`** on each tile (fetched during sync).
- **Search:** type 2 — case-insensitive **contains + typo tolerance** (fuzzy/Levenshtein), over title + caption, client-side over the fetched list; filters the whole collection.
- **Player:** YouTube **privacy-enhanced embed** (see §Compliance). No custom overlay (that would require self-hosted MP4s).

## Firestore `youtube_videos` doc (extended)
```
videoId, title, titleKannada, publishedAtMillis, active,
viewCount,        // NEW — from Data API statistics.viewCount
caption,          // NEW — for search
madeForKids       // NEW — sanity flag; only sync true
```

## Backend sync job (backend dev)
- List your channel's uploads playlist → `videos.list` with `part=snippet,statistics,contentDetails,status`.
- **Hard filter (all must pass):** `status.madeForKids == true` **AND** duration ≤ 60s (Shorts) **AND** a marker (hashtag/playlist).
- `status.madeForKids` is readable with the **API key** (no OAuth). Skip — never upsert — anything where it isn't `true`.
- Store `madeForKids: true` on each doc; upsert with `viewCount`; refresh on a schedule.
- Only an **API key** — no OAuth/token refresh.

## Compliance config (Play Families) — REQUIRED
1. **`youtube-nocookie`** — embed via `https://www.youtube-nocookie.com/embed/...` (privacy-enhanced; no tracking cookies). Fixes the third-party-data concern.
2. **Disable share + all external links** — no "share", no "watch/open on YouTube", no channel/related links, no comments. Nothing leaves the app. Fixes the leaving-the-app concern.
3. **Made for kids + suppress ads/branding** — `youtube-nocookie` does NOT stop YouTube's own ads; so only sync videos marked **Made for kids** on YouTube, and set player params: `rel=0`, `modestbranding=1`, `fs=0`, `playsinline=1`, `iv_load_policy=3`, `disablekb=1`. Verify on device that no non-Families ad shows; if any do, fall back to **self-hosting the MP4s** (ExoPlayer) — the only way to fully guarantee no third-party ads to children.
4. **Data Safety + content rating** — update both before flipping the flag on (new streamed content + any data the embed collects).
5. **Curated content only** — the marker/playlist gate ensures only vetted lesson reels appear (auto-sync must not surface arbitrary posts).

## Android (app)
- Flag `REELS_ENABLED` (default off until backend + compliance ready).
- **Bottom nav:** swap the `Quests` item for a new `Reels` item (film icon) in the bar → its route opens the Reels explore screen. **Settings screen** gets a "Quests" row → `QuestsRoute` (like the existing Progress link).
- **Home:** merge the hero (Today's focus) + tutor sections into one compact row (~40% shorter); keep **Subjects** above the Videos rail; place the Videos rail **above** the Garden section (reuse/reposition `YoutubeVideosSection`); "See all →" → the same Reels explore screen. Order: header → focus/tutor strip → Subjects → Videos → Garden → Friends.
- **Reels explore screen:** search bar + the 3×4 grid (`LazyVerticalGrid`, 3 cols), two labeled sections (Newest / Most watched).
- Tap tile → in-app player screen using the `youtube-nocookie` embed with the params above (reuse/adjust the existing `YoutubePlayerDialog`/androidyoutubeplayer, WebView pointed at nocookie domain), share/links stripped.
- **Defensive filter:** the app renders only items with `madeForKids == true` (and hides any without it), so a hand-edited/non-kids doc can never surface even if the backend filter is bypassed.
- EN/KN labels. Reuse content-click analytics for impressions/plays.

## Still open
- Player = YouTube `youtube-nocookie` embed (current plan) vs self-hosted MP4 — decide only if Made-for-kids embeds still show non-Families ads on device.
