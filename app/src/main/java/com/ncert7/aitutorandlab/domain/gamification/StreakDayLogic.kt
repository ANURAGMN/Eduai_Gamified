package com.ncert7.aitutorandlab.domain.gamification

import com.ncert7.aitutorandlab.data.local.entities.StreakEntity
import java.util.Calendar

/**
 * Pure calendar helpers for streak. Shared by [com.ncert7.aitutorandlab.repository.StreakRepository]
 * so day math and display rules can be unit-tested without Room/Firestore.
 */
object StreakDayLogic {

    fun startOfDay(timeMs: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /** True when [time2] is exactly one calendar day after [time1]. */
    fun isConsecutiveDay(time1: Long, time2: Long): Boolean {
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
        cal1.add(Calendar.DAY_OF_YEAR, 1)
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * UI count: null row → 1 (new-user default); expired → 0; today or yesterday → stored count.
     */
    fun effectiveDisplayCount(streak: StreakEntity?, nowMs: Long = System.currentTimeMillis()): Int {
        if (streak == null) return 1
        if (streak.lastStreakDate == 0L) return 0
        return if (
            isSameDay(streak.lastStreakDate, nowMs) ||
            isConsecutiveDay(streak.lastStreakDate, nowMs)
        ) {
            streak.streakCount
        } else {
            0
        }
    }
}

/**
 * Decides what [recordActivity] / app-open should do. No I/O.
 *
 * [freezeContinuation] = count to keep when freeze covers a one-day miss; null = no freeze.
 */
object StreakActivityRules {

    sealed class Result {
        /** Same calendar day — no Room write, no Firestore schedule. */
        data class NoWrite(val count: Int) : Result()

        /** Persist this count with today's day-start as lastStreakDate. */
        data class Persist(val count: Int) : Result()
    }

    fun next(
        current: StreakEntity?,
        nowMs: Long,
        freezeContinuation: Int? = null,
    ): Result {
        return when {
            current == null -> Result.Persist(1)
            StreakDayLogic.isSameDay(current.lastStreakDate, nowMs) ->
                Result.NoWrite(current.streakCount)
            StreakDayLogic.isConsecutiveDay(current.lastStreakDate, nowMs) ->
                Result.Persist(current.streakCount + 1)
            freezeContinuation != null -> Result.Persist(freezeContinuation)
            else -> Result.Persist(1)
        }
    }
}
