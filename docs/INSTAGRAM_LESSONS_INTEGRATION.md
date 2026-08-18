# Instagram video lessons → Home rail (auto-sync) — integration spec

Goal: your Instagram video uploads appear automatically as a **"Video lessons"** rail on Home, played natively in-app. Educational content, auto-updating, no app release needed to add a video.

## Division of labour
| Part | Owner | Notes |
|------|-------|-------|
| Meta/Instagram account + app + token | **You** (Anurag) | One-time; steps in §1. Only *your* account → **no public App Review needed**. |
| Backend endpoint `/video-lessons` (holds token, calls IG, caches) | **Backend dev** | Contract in §2. Not in this repo. |
| Android Home rail + player (Media3/ExoPlayer) | **App** (I can build) | §3. Feature-flagged so it can't destabilise the release. |

## Key realities (read once)
- **Instagram Basic Display API is dead** (Meta shut it down Dec 2024). The only supported route is the **Instagram Graph API** (Business/Creator account).
- **Single-account = no App Review.** Because you only fetch *your own* media, you don't submit for Meta App Review. You create the app, add your IG/FB as a tester/admin, and generate a **long-lived token** (~60 days, refreshable). Public review is only needed to read *other people's* accounts.
- **`media_url` for video is a short-lived signed CDN URL** — it can 403 after a while. So the **backend caches/refreshes** it (or proxies the stream); the app also keeps `permalink` as a fallback ("Open in Instagram"). Don't hardcode `media_url` on the client for long-term storage.
- **Content filtering is required** (kids' app). Don't surface your whole feed — filter to lesson reels only (see §2, `media_product_type == "REELS"` + a marker hashtag like `#eduailesson`).

## §1 — Meta / Instagram setup (you, one-time)
1. Convert your IG account to **Business or Creator** (IG app → Settings → Account type).
2. Link it to a **Facebook Page** (Meta requires the IG↔Page link for Graph API).
3. Create a **Meta app** at developers.facebook.com → add the **Instagram Graph API** product.
4. Add yourself as **admin/tester**; grant permissions `instagram_basic`, `instagram_manage_insights` (optional), and the media read scope.
5. Generate a **long-lived user access token** for your IG user; note your **IG user id**.
6. Hand the backend dev: the **long-lived token**, **IG user id**, **app id/secret** (for refresh). Store server-side only — never in the app.
7. Token refresh: long-lived tokens expire ~60 days; the backend must refresh before expiry (a scheduled call). Document the expiry date.

## §2 — Backend contract (backend dev)
Add one endpoint the app calls (behind the app's existing API auth):

`GET {AGENTIC_AI_BASE_URL}/video-lessons?lang=en`

Backend does: call `GET https://graph.facebook.com/v20.0/{ig-user-id}/media` with
`fields=id,caption,media_type,media_product_type,media_url,thumbnail_url,permalink,timestamp`,
**filter** to `media_type=VIDEO` (and/or `media_product_type=REELS`) **and** a marker (e.g. caption contains `#eduailesson`), **cache** the result (e.g. 15–30 min) and **refresh `media_url`** as needed.

Response JSON (stable shape the app codes against):
```json
{
  "items": [
    {
      "id": "instagram-media-id",
      "title": "Photosynthesis in 60s",
      "caption": "How leaves make food …",
      "videoUrl": "https://.../video.mp4",     // fresh, playable
      "thumbnailUrl": "https://.../thumb.jpg",
      "permalink": "https://www.instagram.com/reel/XXXX/",
      "durationMs": 58000,                       // optional
      "postedAt": "2026-08-17T10:00:00Z",
      "lang": "en"                               // optional; omit if not tagged
    }
  ]
}
```
Notes: `title` = first line of caption (or a `Title:` marker you put in the caption); `videoUrl` must be fresh at response time; if you can't keep it fresh, omit it and the app uses `permalink` (opens the reel) instead of native playback.

## §3 — Android app (I build, feature-flagged)
- **Flag:** `INSTAGRAM_LESSONS_ENABLED` (BuildConfig / feature flag), default off until the endpoint is live.
- **Model:** `VideoLesson(id, title, caption, videoUrl, thumbnailUrl, permalink, durationMs, postedAt)`.
- **Data:** `VideoLessonsRepository` → Retrofit call to `/video-lessons` (reuse existing OkHttp/Retrofit + API key header); cache last good list in Room/prefs for offline.
- **Home rail:** a horizontal "Video lessons" section in `EduHomeScreen`/`GamifiedHomeRoute` (mirrors the existing subjects/garden rails), thumbnail + title cards, EN/KN label.
- **Playback:** tap → full-screen **Media3 ExoPlayer** screen playing `videoUrl` (portrait, tap-to-pause, mute toggle); if `videoUrl` is null/expired → open `permalink` in the in-app WebView / Instagram.
- **Analytics:** reuse the content-click tracker (impression + play).
- **Kids-safety:** only render items the backend already filtered; no comments/social chrome.

## Rollout
1. You do §1 → hand token/id to backend.
2. Backend ships §2 (`/video-lessons`).
3. I build §3 behind the flag; flip on once the endpoint returns data.
This keeps the current release unaffected (flag stays off until ready).

## Caveats / risks
- **Token expiry** (~60 days) — backend must refresh or the rail goes empty. Add a monitor/alert.
- **`media_url` expiry** — backend must serve fresh URLs (or the app falls back to `permalink`).
- **Meta policy / TOS** — automated fetch of *your own* media is fine; don't scrape. Music in reels is your copyright responsibility.
- **Release timing** — none of this should gate v1.0.11; it's a post-release feature behind the flag.
