package com.ncert7.aitutorandlab.service.analytics

/**
 * Pure helpers for capping orphan session/screen durations (testable without Room).
 */
object AnalyticsDurationUtils {

    const val MAX_ORPHAN_MS = 2L * 60 * 60 * 1000

    data class CappedDuration(
        val durationMillis: Long,
        val endTime: Long,
        val wasCapped: Boolean,
    )

    fun capDuration(startTime: Long, endTime: Long, maxMs: Long = MAX_ORPHAN_MS): CappedDuration {
        val raw = (endTime - startTime).coerceAtLeast(0)
        val duration = raw.coerceAtMost(maxMs)
        return CappedDuration(
            durationMillis = duration,
            endTime = startTime + duration,
            wasCapped = raw > maxMs,
        )
    }
}
