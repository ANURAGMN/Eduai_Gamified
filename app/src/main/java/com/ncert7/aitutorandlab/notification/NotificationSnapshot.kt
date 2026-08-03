package com.ncert7.aitutorandlab.notification

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class NotificationEvalTrigger {
    DAILY_ALARM,
    PERIODIC_SWEEP,
}

data class ExpiringAvatar(
    val id: String,
    val name: String,
)

data class InProgressChapter(
    val chapterId: String,
    val chapterName: String,
    val overallPercent: Int,
)

data class NotificationSnapshot(
    val studentId: String,
    val studentName: String,
    val languageCode: String,
    val streakCount: Int,
    val studiedToday: Boolean,
    val todayPlanLabel: String,
    val todayPlanMinutes: Int,
    val todayDayIndex: Int?,
    val pendingTrialTasksCount: Int = 0,
    val daysToExam: Int? = null,
    val daysSinceLastActivity: Int? = null,
    val inactivity14PreviouslySent: Boolean = false,
    val weeklyXp: Int = 0,
    val weeklyXpTarget: Int = 500,
    val expiringAvatar: ExpiringAvatar? = null,
    val inProgressChapter: InProgressChapter? = null,
    val streakSavedPending: Boolean = false,
)

object NotificationTimeRules {
    private val zone = ZoneId.of("Asia/Kolkata")
    const val STREAK_AT_RISK_START_HOUR = 18
    const val EVENING_START_HOUR = 18
    const val EXAM_COUNTDOWN_MAX_DAYS = 14

    fun nowTime(): LocalTime = LocalTime.now(zone)

    fun todayEpochDay(): Long = LocalDate.now(zone).toEpochDay()

    fun todayDateString(): String = LocalDate.now(zone).toString()

    fun isQuietHours(hour: Int, startHour: Int, endHour: Int): Boolean =
        if (startHour == endHour) {
            false
        } else if (startHour > endHour) {
            hour >= startHour || hour < endHour
        } else {
            hour in startHour until endHour
        }

    fun isReminderWindow(now: LocalTime, reminderHour: Int, reminderMinute: Int): Boolean {
        val target = LocalTime.of(reminderHour, reminderMinute)
        val diffMinutes = kotlin.math.abs(now.toSecondOfDay() - target.toSecondOfDay()) / 60
        return diffMinutes <= 30
    }

    fun isEvening(hour: Int): Boolean = hour >= EVENING_START_HOUR
}
