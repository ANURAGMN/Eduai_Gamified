package com.ncert7.aitutorandlab.domain.reels.analytics

import kotlin.math.roundToInt

/**
 * Pure builders for reels analytics — GA4 event names, the `interactionType` strings persisted in
 * `app_analytics` (mirrors the `"value|detail"` convention of ad events), and the GA4 param maps.
 * No Android or DB dependency so every encoding is unit-testable.
 */
object ReelsAnalytics {

    // GA4 event names.
    const val EVENT_OPEN = "reel_open"
    const val EVENT_WATCH = "reel_watch"
    const val EVENT_SEARCH = "reel_search"

    /** Longest query we keep, to bound row size / GA4 param length. */
    const val MAX_QUERY_LEN = 64

    // ---- interactionType encoders (persisted in AppAnalyticsEntity.interactionType) ----

    /** `open|pos=<n>` and, when searching, `|q=<query>`. Position is 0-based; unknown = -1. */
    fun openInteraction(position: Int, query: String?): String =
        buildString {
            append("open|pos=").append(position.coerceAtLeast(-1))
            sanitizedQuery(query)?.let { append("|q=").append(it) }
        }

    /** `watch|ms=<watchedMs>` and, when the duration is known, `|pct=<0..100>`. */
    fun watchInteraction(watchedMs: Long, completion: Float?): String =
        buildString {
            append("watch|ms=").append(watchedMs.coerceAtLeast(0L))
            completion?.let { append("|pct=").append(percent(it)) }
        }

    /** `search|results=<n>` for a committed query. */
    fun searchInteraction(resultCount: Int): String =
        "search|results=" + resultCount.coerceAtLeast(0)

    // ---- GA4 param maps ----

    fun openParams(videoId: String, section: ReelSection, position: Int, query: String?): Map<String, Any?> =
        mapOf(
            "video_id" to videoId,
            "section" to section.value,
            "position" to position.coerceAtLeast(-1),
            "query" to sanitizedQuery(query),
        )

    fun watchParams(videoId: String, watchedMs: Long, completion: Float?): Map<String, Any?> =
        mapOf(
            "video_id" to videoId,
            "watched_ms" to watchedMs.coerceAtLeast(0L),
            "completion_pct" to completion?.let { percent(it) },
        )

    fun searchParams(query: String, resultCount: Int): Map<String, Any?> =
        mapOf(
            "query" to sanitizedQuery(query),
            "result_count" to resultCount.coerceAtLeast(0),
        )

    // ---- helpers ----

    /** Trimmed + length-bounded query, or null when blank (so callers can omit it). */
    fun sanitizedQuery(query: String?): String? =
        query?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_QUERY_LEN)

    /** Fraction 0f..1f → integer percent 0..100. */
    fun percent(fraction: Float): Int = (fraction.coerceIn(0f, 1f) * 100f).roundToInt()
}
