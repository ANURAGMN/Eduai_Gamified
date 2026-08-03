package com.ncert7.aitutorandlab.domain.gamification

import android.content.Context
import com.ncert7.aitutorandlab.data.local.dao.StreakDao
import com.ncert7.aitutorandlab.data.local.entities.StreakEntity
import com.ncert7.aitutorandlab.debug.DebugLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreakFreezeService @Inject constructor(
    private val streakDao: StreakDao,
    @ApplicationContext private val context: Context,
) {
    private val zone = ZoneId.of("Asia/Kolkata")

    fun hasPendingStreakSavedNotification(userId: String): Boolean =
        StreakFreezeStore.hasPendingStreakSavedNotification(context, userId)

    fun clearPendingStreakSavedNotification(userId: String) {
        StreakFreezeStore.setPendingStreakSavedNotification(context, userId, false)
    }

    /** Auto-applies a weekly streak freeze when exactly one calendar day was missed. */
    suspend fun applyAutoFreezeIfEligible(userId: String): Boolean {
        val streak = streakDao.getStreakByUserId(userId) ?: return false
        if (streak.streakCount <= 0 || streak.lastStreakDate <= 0L) return false

        val todayStart = startOfDay(System.currentTimeMillis())
        val gapDays = dayGap(streak.lastStreakDate, todayStart)
        if (gapDays != 2) return false
        if (!StreakFreezeStore.canUseFreeze(context, userId)) return false

        val yesterdayStart = startOfDay(System.currentTimeMillis() - DAY_MS)
        StreakFreezeStore.consumeFreeze(context, userId)
        StreakFreezeStore.setPendingStreakSavedNotification(context, userId, true)
        streakDao.insertStreak(
            streak.copy(
                lastStreakDate = yesterdayStart,
                updatedAt = System.currentTimeMillis(),
                isSynced = false,
            ),
        )
        DebugLogger.debugLog(TAG, "Auto streak freeze applied for $userId (streak=${streak.streakCount})")
        return true
    }

    /** Uses a freeze when the user studies after missing exactly one day. */
    fun resolveStreakAfterMissedDay(
        userId: String,
        currentStreak: StreakEntity,
        now: Long,
    ): Int? {
        val gapDays = dayGap(currentStreak.lastStreakDate, startOfDay(now))
        if (gapDays != 2) return null
        if (!StreakFreezeStore.canUseFreeze(context, userId)) return null

        StreakFreezeStore.consumeFreeze(context, userId)
        StreakFreezeStore.setPendingStreakSavedNotification(context, userId, true)
        DebugLogger.debugLog(TAG, "Streak freeze used on study for $userId")
        return currentStreak.streakCount + 1
    }

    private fun dayGap(fromDayStartMs: Long, toDayStartMs: Long): Int {
        val from = LocalDate.ofInstant(Instant.ofEpochMilli(fromDayStartMs), zone)
        val to = LocalDate.ofInstant(Instant.ofEpochMilli(toDayStartMs), zone)
        return (to.toEpochDay() - from.toEpochDay()).toInt()
    }

    private fun startOfDay(timeMs: Long): Long {
        val cal =
            Calendar.getInstance().apply {
                timeInMillis = timeMs
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        return cal.timeInMillis
    }

    companion object {
        private const val TAG = "StreakFreezeService"
        private const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
