package com.ncert7.aitutorandlab.domain.gamification

import android.content.Context
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Per-user streak freeze allowance and pending notification flags (SharedPreferences).
 * One auto-freeze per ISO week; onboarding copy promises this protection.
 */
object StreakFreezeStore {
    private const val PREFS = "eduai_streak_freeze"
    private const val FREEZES_PER_WEEK = 1
    private val zone = ZoneId.of("Asia/Kolkata")

    fun canUseFreeze(context: Context, userId: String): Boolean =
        remainingFreezesThisWeek(context, userId) > 0

    fun remainingFreezesThisWeek(context: Context, userId: String): Int {
        val prefs = prefs(context)
        val weekKey = currentWeekKey()
        val storedWeek = prefs.getString(weekKey(userId), null)
        if (storedWeek != weekKey) return FREEZES_PER_WEEK
        val used = prefs.getInt(usedKey(userId), 0)
        return (FREEZES_PER_WEEK - used).coerceAtLeast(0)
    }

    fun consumeFreeze(context: Context, userId: String) {
        val prefs = prefs(context)
        val weekKey = currentWeekKey()
        val storedWeek = prefs.getString(weekKey(userId), null)
        val used =
            if (storedWeek == weekKey) {
                prefs.getInt(usedKey(userId), 0) + 1
            } else {
                1
            }
        prefs.edit()
            .putString(weekKey(userId), weekKey)
            .putInt(usedKey(userId), used)
            .apply()
    }

    fun hasPendingStreakSavedNotification(context: Context, userId: String): Boolean =
        prefs(context).getBoolean(pendingKey(userId), false)

    fun setPendingStreakSavedNotification(context: Context, userId: String, pending: Boolean) {
        prefs(context).edit().putBoolean(pendingKey(userId), pending).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun weekKey(userId: String) = "week_$userId"

    private fun usedKey(userId: String) = "used_$userId"

    private fun pendingKey(userId: String) = "pending_saved_$userId"

    private fun currentWeekKey(): String {
        val today = LocalDate.now(zone)
        val week = today.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())
        val year = today.get(WeekFields.of(Locale.getDefault()).weekBasedYear())
        return "${year}W$week"
    }
}
