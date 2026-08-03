package com.ncert7.aitutorandlab.notification

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object NotificationEvaluator {
    data class Candidate(
        val type: NotificationType,
        val dedupKey: String,
    )

    fun eligibleCandidates(
        snapshot: NotificationSnapshot,
        settings: NotificationEvalSettings,
        trigger: NotificationEvalTrigger,
        now: LocalTime = NotificationTimeRules.nowTime(),
        today: LocalDate = LocalDate.now(zone),
    ): List<Candidate> {
        if (settings.reminderMode == NotificationReminderMode.OFF) return emptyList()

        val hour = now.hour
        if (NotificationTimeRules.isQuietHours(hour, settings.quietHoursStart, settings.quietHoursEnd)) {
            return emptyList()
        }

        val candidates = mutableListOf<Candidate>()

        if (settings.isCategoryEnabled(NotificationCategory.REMINDERS) &&
            !snapshot.studiedToday &&
            isDailyReminderEligible(trigger, settings, now)
        ) {
            candidates += Candidate(NotificationType.DAILY_REMINDER, "daily")
        }

        if (settings.isCategoryEnabled(NotificationCategory.STREAKS) &&
            !snapshot.studiedToday &&
            isStreakAtRiskEligible(snapshot, trigger, now)
        ) {
            candidates += Candidate(NotificationType.STREAK_AT_RISK, "streak")
        }

        if (settings.isCategoryEnabled(NotificationCategory.STREAKS) &&
            isStreakSavedEligible(snapshot, trigger)
        ) {
            candidates += Candidate(NotificationType.STREAK_SAVED, "freeze")
        }

        if (settings.isCategoryEnabled(NotificationCategory.STREAKS) &&
            isStreakComebackEligible(snapshot, trigger)
        ) {
            candidates += Candidate(NotificationType.STREAK_COMEBACK, "comeback")
        }

        if (settings.isCategoryEnabled(NotificationCategory.QUESTS) &&
            isTasksPendingEligible(snapshot, trigger, now)
        ) {
            candidates += Candidate(
                NotificationType.TASKS_PENDING,
                "tasks_${snapshot.pendingTrialTasksCount}",
            )
        }

        if (settings.isCategoryEnabled(NotificationCategory.REMINDERS) &&
            isChapterProgressEligible(snapshot, trigger)
        ) {
            snapshot.inProgressChapter?.let { chapter ->
                candidates +=
                    Candidate(
                        NotificationType.CHAPTER_PROGRESS,
                        "chapter_${chapter.chapterId}",
                    )
            }
        }

        if (settings.isCategoryEnabled(NotificationCategory.REMINDERS) &&
            isExamCountdownEligible(snapshot, trigger)
        ) {
            snapshot.daysToExam?.let { days ->
                candidates += Candidate(NotificationType.EXAM_COUNTDOWN, "exam_$days")
            }
        }

        resolveInactivityCandidate(snapshot)?.let { candidate ->
            if (settings.isCategoryEnabled(NotificationCategory.REMINDERS)) {
                candidates += candidate
            }
        }

        if (settings.isCategoryEnabled(NotificationCategory.QUESTS) &&
            isWeeklyXpCloseEligible(snapshot, trigger, today)
        ) {
            candidates += Candidate(NotificationType.WEEKLY_XP_CLOSE, "weekly_xp")
        }

        snapshot.expiringAvatar?.let { avatar ->
            if (settings.isCategoryEnabled(NotificationCategory.AVATAR)) {
                candidates +=
                    Candidate(
                        NotificationType.AVATAR_UNLOCK_EXPIRING,
                        "avatar_${avatar.id}",
                    )
            }
        }

        return candidates.sortedBy { it.type.evalPriority }
    }

    private val zone = ZoneId.of("Asia/Kolkata")

    private fun isDailyReminderEligible(
        trigger: NotificationEvalTrigger,
        settings: NotificationEvalSettings,
        now: LocalTime,
    ): Boolean =
        when (trigger) {
            NotificationEvalTrigger.DAILY_ALARM -> true
            NotificationEvalTrigger.PERIODIC_SWEEP ->
                NotificationTimeRules.isReminderWindow(
                    now,
                    settings.reminderHour,
                    settings.reminderMinute,
                )
        }

    private fun isStreakSavedEligible(
        snapshot: NotificationSnapshot,
        trigger: NotificationEvalTrigger,
    ): Boolean {
        if (!snapshot.streakSavedPending) return false
        if (snapshot.streakCount <= 0) return false
        return trigger == NotificationEvalTrigger.PERIODIC_SWEEP ||
            trigger == NotificationEvalTrigger.DAILY_ALARM
    }

    private fun isStreakAtRiskEligible(
        snapshot: NotificationSnapshot,
        trigger: NotificationEvalTrigger,
        now: LocalTime,
    ): Boolean {
        if (snapshot.streakCount <= 0) return false
        if (!NotificationTimeRules.isEvening(now.hour)) return false
        return trigger == NotificationEvalTrigger.PERIODIC_SWEEP ||
            trigger == NotificationEvalTrigger.DAILY_ALARM
    }

    private fun isStreakComebackEligible(
        snapshot: NotificationSnapshot,
        trigger: NotificationEvalTrigger,
    ): Boolean {
        if (snapshot.studiedToday) return false
        if (snapshot.daysSinceLastActivity != 1) return false
        if (snapshot.streakCount <= 0) return false
        return trigger == NotificationEvalTrigger.PERIODIC_SWEEP ||
            trigger == NotificationEvalTrigger.DAILY_ALARM
    }

    private fun isWeeklyXpCloseEligible(
        snapshot: NotificationSnapshot,
        trigger: NotificationEvalTrigger,
        today: LocalDate,
    ): Boolean {
        if (trigger != NotificationEvalTrigger.PERIODIC_SWEEP) return false
        if (today.dayOfWeek !in DayOfWeek.THURSDAY..DayOfWeek.SATURDAY) return false
        val target = snapshot.weeklyXpTarget
        if (target <= 0) return false
        val lowerBound = (target * 0.85).toInt()
        return snapshot.weeklyXp in lowerBound until target
    }

    private fun isChapterProgressEligible(
        snapshot: NotificationSnapshot,
        trigger: NotificationEvalTrigger,
    ): Boolean {
        if (snapshot.inProgressChapter == null) return false
        return trigger == NotificationEvalTrigger.PERIODIC_SWEEP ||
            trigger == NotificationEvalTrigger.DAILY_ALARM
    }

    private fun isTasksPendingEligible(
        snapshot: NotificationSnapshot,
        trigger: NotificationEvalTrigger,
        now: LocalTime,
    ): Boolean {
        if (snapshot.pendingTrialTasksCount <= 0) return false
        if (!NotificationTimeRules.isEvening(now.hour)) return false
        return trigger == NotificationEvalTrigger.PERIODIC_SWEEP
    }

    private fun isExamCountdownEligible(
        snapshot: NotificationSnapshot,
        trigger: NotificationEvalTrigger,
    ): Boolean {
        val days = snapshot.daysToExam ?: return false
        if (days > NotificationTimeRules.EXAM_COUNTDOWN_MAX_DAYS) return false
        return trigger == NotificationEvalTrigger.PERIODIC_SWEEP ||
            trigger == NotificationEvalTrigger.DAILY_ALARM
    }

    private fun resolveInactivityCandidate(snapshot: NotificationSnapshot): Candidate? {
        if (snapshot.studiedToday) return null
        val inactiveDays = snapshot.daysSinceLastActivity ?: return null
        if (snapshot.inactivity14PreviouslySent && inactiveDays >= 14) return null

        return when {
            inactiveDays in 3..6 ->
                Candidate(NotificationType.INACTIVITY_3, "inactivity")
            inactiveDays in 7..13 ->
                Candidate(NotificationType.INACTIVITY_7, "inactivity")
            inactiveDays >= 14 ->
                Candidate(NotificationType.INACTIVITY_14, "inactivity")
            else -> null
        }
    }
}
