package com.ncert7.aitutorandlab.notification

/** Weekly avatar drop timing — mirrors [com.anurag.eduai.uikit.avatar.weeklyAvatarPresets]. */
object NotificationAvatarRules {
    private const val WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000
    private const val TWENTY_FOUR_HOURS_MS = 24L * 60 * 60 * 1000

    fun millisUntilNextWeeklyDrop(now: Long = System.currentTimeMillis()): Long {
        val nextBoundary = ((now / WEEK_MILLIS) + 1) * WEEK_MILLIS
        return (nextBoundary - now).coerceAtLeast(0)
    }

    fun isWithin24HoursOfWeeklyDrop(now: Long = System.currentTimeMillis()): Boolean =
        millisUntilNextWeeklyDrop(now) <= TWENTY_FOUR_HOURS_MS
}
