package com.ncert7.aitutorandlab.domain.reels.analytics

/**
 * Pure, foreground-aware watch-time accumulator for the reel player. No Android, no clock — the
 * caller supplies timestamps so it is fully unit-testable.
 *
 * Usage: [onPlay] when the video becomes visible/foregrounded, [onPause] when it is hidden or the
 * app backgrounds, and [watchedMs] on dispose to read the total. Multiple play/pause cycles
 * accumulate. Backward or absurd time jumps (clock changes, stale events) are clamped to
 * `[0, maxSpanMs]` per span so a single glitch can't inflate the total.
 */
class ReelWatchTracker(
    private val maxSpanMs: Long = DEFAULT_MAX_SPAN_MS,
) {
    private var playingSince: Long? = null
    private var accumulatedMs: Long = 0L

    /** Idempotent — a second [onPlay] without an intervening [onPause] is ignored. */
    fun onPlay(atMs: Long) {
        if (playingSince == null) playingSince = atMs
    }

    /** No-op when not currently playing. Adds the clamped span to the running total. */
    fun onPause(atMs: Long) {
        val start = playingSince ?: return
        accumulatedMs += span(start, atMs)
        playingSince = null
    }

    /** Total watched so far, including the live span if still playing. Never negative. */
    fun watchedMs(nowMs: Long): Long {
        val live = playingSince?.let { span(it, nowMs) } ?: 0L
        return accumulatedMs + live
    }

    val isPlaying: Boolean get() = playingSince != null

    fun reset() {
        playingSince = null
        accumulatedMs = 0L
    }

    private fun span(startMs: Long, endMs: Long): Long =
        (endMs - startMs).coerceIn(0L, maxSpanMs)

    companion object {
        /** A single span longer than this is treated as a glitch and capped (6h). */
        const val DEFAULT_MAX_SPAN_MS: Long = 6L * 60 * 60 * 1000

        /**
         * Fraction of the video watched, `0f..1f`, or null when the duration is unknown
         * (the current player has no duration signal). Clamped so replays can't exceed 100%.
         */
        fun completionFraction(watchedMs: Long, durationMs: Long?): Float? {
            if (durationMs == null || durationMs <= 0L) return null
            if (watchedMs <= 0L) return 0f
            return (watchedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        }
    }
}
