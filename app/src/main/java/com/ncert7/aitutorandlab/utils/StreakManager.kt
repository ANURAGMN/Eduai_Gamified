package com.ncert7.aitutorandlab.utils

import android.content.Context
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.repository.StreakRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * StreakManager is responsible for tracking the user's learning streak
 * based on ANY learning activity: concept screen opens, agent sessions, simulations, etc.
 *
 * Streak Logic:
 * - A streak continues if ANY learning activity happens on consecutive calendar days
 * - Multiple activities on the same day don't increase the streak
 * - Missing a day breaks the streak (resets to 1 on next activity)
 * - The streak is calendar-day based, not 24-hour based
 *
 * Activities that trigger streak:
 * - Concept screen opened
 * - Study agent session completed
 * - Simulation agent session started
 * - Simulation URL loaded
 * - Revision session started
 */
class StreakManager(
    context: Context,
    private val streakRepository: StreakRepository,
    private val userId: String,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    /**
     * Private SharedPreferences file used only for streak-related state.
     * This isolates behavioral tracking from the rest of the app data.
     */
    private val prefs = context.getSharedPreferences("streak_prefs", Context.MODE_PRIVATE)

    private companion object {
        const val KEY_LAST_STREAK_DAY = "last_streak_day"
        const val KEY_STREAK_COUNT = "streak_count"
        private const val TAG = "StreakManager"
    }

    /**
     * Records ANY learning activity and updates the streak accordingly.
     * This is the primary method for recording all types of learning activities:
     * - Concept screen opens
     * - Study agent completed
     * - Simulation agent started
     * - Simulation URL loaded
     * - Revision session started
     *
     * Streak behavior:
     * - First time: streak = 1
     * - Same day: streak unchanged (multiple activities same day don't increase streak)
     * - Next consecutive day: streak + 1
     * - Day(s) skipped: streak resets to 1
     *
     * @param activityName Optional name of the activity for logging
     * @return The current streak count after update
     */
    fun recordActivity(activityName: String = "UNKNOWN"): Int {
        val now = System.currentTimeMillis()
        val today = getDayIdentifier(now)

        val lastStreakDay = prefs.getLong(KEY_LAST_STREAK_DAY, 0L)
        val oldStreak = prefs.getInt(KEY_STREAK_COUNT, 0)

        val newStreak = when {
            // First ever streak event
            lastStreakDay == 0L -> {
                DebugLogger.debugLog(TAG, "[$activityName] First streak event - starting at 1")
                1
            }

            // Same calendar day → do NOT increment
            isSameDay(lastStreakDay, now) -> {
                DebugLogger.debugLog(TAG, "[$activityName] Same day - streak remains $oldStreak")
                oldStreak
            }

            // Next consecutive day → continue streak
            isConsecutiveDay(lastStreakDay, now) -> {
                val newCount = oldStreak + 1
                DebugLogger.debugLog(TAG, "[$activityName] Consecutive day - streak increased to $newCount")
                newCount
            }

            // Days were skipped → reset streak
            else -> {
                DebugLogger.debugLog(TAG, "[$activityName] Day(s) skipped - streak reset to 1 (was $oldStreak)")
                1
            }
        }

        prefs.edit()
            .putLong(KEY_LAST_STREAK_DAY, today)
            .putInt(KEY_STREAK_COUNT, newStreak)
            .apply()

        return newStreak
    }

    /**
     * Records a Concept screen open and updates the streak accordingly.
     *
     * Streak behavior:
     * - First time: streak = 1
     * - Same day: streak unchanged
     * - Next consecutive day: streak + 1
     * - Day(s) skipped: streak resets to 1
     *
     * @return The current streak count after update
     */
    /**
     * Records a Concept / simulation open via [StreakRepository.recordActivity]
     * (same-day no-op, freeze-aware miss handling, deferred Firestore upload).
     */
    fun onConceptOpened(onStreakUpdated: (Int) -> Unit = {}) {
        scope.launch {
            try {
                if (userId.isBlank()) {
                    DebugLogger.errorLog(TAG, "onConceptOpened skipped — empty userId")
                    return@launch
                }
                val newStreak = streakRepository.recordActivity(userId)
                onStreakUpdated(newStreak)
            } catch (e: Exception) {
                DebugLogger.errorLog(TAG, "Error updating streak: ${e.message}")
            }
        }
    }

    /**
     * Async streak touch for paths that already have a concrete student id
     * (avoids relying on the DI-captured [userId], which can be empty pre-login).
     */
    fun recordLearningActivityForUser(studentId: String, onStreakUpdated: (Int) -> Unit = {}) {
        scope.launch {
            try {
                if (studentId.isBlank()) return@launch
                onStreakUpdated(streakRepository.recordActivity(studentId))
            } catch (e: Exception) {
                DebugLogger.errorLog(TAG, "Error recording learning activity: ${e.message}")
            }
        }
    }

    /**
     * Provides the current streak value with validation.
     * If days were skipped since last activity, returns 0 instead of the old streak.
     *
     * @return Current valid streak count, or 0 if streak has expired
     */
    fun getCurrentStreak(): Int {
        val now = System.currentTimeMillis()
        val lastStreakDay = prefs.getLong(KEY_LAST_STREAK_DAY, 0L)
        val storedStreak = prefs.getInt(KEY_STREAK_COUNT, 0)

        // No streak recorded yet
        if (lastStreakDay == 0L) {
            DebugLogger.debugLog("StreakManager", "No streak data found")
            return 0
        }

        // Check if streak is still valid
        val isValid = isSameDay(lastStreakDay, now) || isConsecutiveDay(lastStreakDay, now)

        return if (isValid) {
            DebugLogger.debugLog("StreakManager", "Valid streak: $storedStreak days")
            storedStreak
        } else {
            // Streak expired but not reset yet (will reset on next concept open)
            DebugLogger.debugLog("StreakManager", "Streak expired - was $storedStreak days")
            0
        }
    }

    /**
     * Resets the streak completely.
     * Call this on user logout or when manually resetting progress.
     */
    fun resetStreak() {
        prefs.edit()
            .remove(KEY_LAST_STREAK_DAY)
            .remove(KEY_STREAK_COUNT)
            .apply()
        DebugLogger.debugLog("StreakManager", "Streak reset")
    }

    /**
     * Checks if two timestamps fall on the same calendar day.
     */
    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Checks if time2 is exactly one calendar day after time1.
     */
    private fun isConsecutiveDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply {
            timeInMillis = time1
            // Reset to start of day for accurate comparison
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val cal2 = Calendar.getInstance().apply {
            timeInMillis = time2
            // Reset to start of day for accurate comparison
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Add one day to cal1 and check if it equals cal2
        cal1.add(Calendar.DAY_OF_YEAR, 1)

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Gets a day identifier (start of day timestamp) for consistent day comparison.
     */
    private fun getDayIdentifier(time: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Get the number of days since the streak started.
     * Useful for analytics or displaying "streak age".
     */
    fun getDaysSinceStreakStart(): Int {
        val lastStreakDay = prefs.getLong(KEY_LAST_STREAK_DAY, 0L)
        if (lastStreakDay == 0L) return 0

        val now = System.currentTimeMillis()
        val daysDiff = getDayDifference(lastStreakDay, now)

        return daysDiff
    }

    /**
     * Calculates the difference in calendar days between two timestamps.
     */
    private fun getDayDifference(time1: Long, time2: Long): Int {
        val cal1 = Calendar.getInstance().apply {
            timeInMillis = time1
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val cal2 = Calendar.getInstance().apply {
            timeInMillis = time2
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffInMillis = cal2.timeInMillis - cal1.timeInMillis
        return (diffInMillis / (24 * 60 * 60 * 1000)).toInt()
    }
}