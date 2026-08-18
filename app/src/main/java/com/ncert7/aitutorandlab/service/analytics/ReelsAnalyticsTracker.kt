package com.ncert7.aitutorandlab.service.analytics

import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.domain.reels.analytics.ReelSection
import com.ncert7.aitutorandlab.domain.reels.analytics.ReelsAnalytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Records reels engagement to local Room (via [AnalyticsEventRecorder], which mirrors to Firestore
 * when [AnalyticsFirestoreMirror] is on) and to GA4. Fire-and-forget on an IO scope — mirrors
 * [NavClickAnalyticsTracker]. The string encodings live in the pure, unit-tested [ReelsAnalytics].
 *
 * Screen dwell (time in the reels tab / per-video watch screen) is captured separately by
 * `TrackScreenEvent(ScreenName.REELS / REELS_PLAYER)`; this tracker adds the richer discrete events.
 */
object ReelsAnalyticsTracker {

    private const val TAG = "ReelsAnalytics"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** A reel was opened from [section] at 0-based [position]; [query] set only for search opens. */
    fun trackOpen(videoId: String, section: ReelSection, position: Int, query: String?) {
        if (videoId.isBlank()) return
        scope.launch {
            AnalyticsEventRecorder.recordClick(
                screenName = ScreenName.REELS.displayName,
                itemId = videoId,
                source = section.value,
                interactionType = ReelsAnalytics.openInteraction(position, query),
                eventType = EventType.CLICK.type,
            )
            FirebaseAnalyticsHelper.logEvent(
                eventName = ReelsAnalytics.EVENT_OPEN,
                screen = ScreenName.REELS,
                params = ReelsAnalytics.openParams(videoId, section, position, query),
            )
            DebugLogger.debugLog(TAG, "open $videoId (${section.value}, pos=$position)")
        }
    }

    /** The player closed after [watchedMs] of foreground watch. [completion] null when unknown. */
    fun trackWatch(videoId: String, watchedMs: Long, completion: Float?) {
        if (videoId.isBlank() || watchedMs <= 0L) return
        scope.launch {
            AnalyticsEventRecorder.recordClick(
                screenName = ScreenName.REELS_PLAYER.displayName,
                itemId = videoId,
                source = null,
                interactionType = ReelsAnalytics.watchInteraction(watchedMs, completion),
                eventType = EventType.COMPLETE.type,
            )
            FirebaseAnalyticsHelper.logEvent(
                eventName = ReelsAnalytics.EVENT_WATCH,
                screen = ScreenName.REELS_PLAYER,
                params = ReelsAnalytics.watchParams(videoId, watchedMs, completion),
            )
            DebugLogger.debugLog(TAG, "watch $videoId ${watchedMs}ms pct=$completion")
        }
    }

    /** A search was committed (IME action) yielding [resultCount] results. */
    fun trackSearch(query: String, resultCount: Int) {
        val q = ReelsAnalytics.sanitizedQuery(query) ?: return
        scope.launch {
            AnalyticsEventRecorder.recordClick(
                screenName = ScreenName.REELS.displayName,
                itemId = q,
                source = ReelSection.SEARCH.value,
                interactionType = ReelsAnalytics.searchInteraction(resultCount),
                eventType = EventType.CLICK.type,
            )
            FirebaseAnalyticsHelper.logEvent(
                eventName = ReelsAnalytics.EVENT_SEARCH,
                screen = ScreenName.REELS,
                params = ReelsAnalytics.searchParams(query, resultCount),
            )
            DebugLogger.debugLog(TAG, "search '$q' → $resultCount")
        }
    }
}
