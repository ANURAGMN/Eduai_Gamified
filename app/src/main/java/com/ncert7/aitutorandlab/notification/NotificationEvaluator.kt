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
        sentTodayKeys: List<String> = emptyList(), // Added to help filter duplicates
        now: LocalTime = NotificationTimeRules.nowTime(),
        today: LocalDate = LocalDate.now(zone),
    ): List<Candidate> {
        if (settings.reminderMode == NotificationReminderMode.OFF) return emptyList()

        // 1. ISOLATE DAILY ALARM
        // If the custom time alarm fires, immediately return the DAILY_REMINDER.
        // This completely prevents other types from stealing its spot.
        if (trigger == NotificationEvalTrigger.DAILY_ALARM) {
            return if (settings.isCategoryEnabled(NotificationCategory.REMINDERS)) {
                listOf(Candidate(NotificationType.DAILY_REMINDER, "daily"))
            } else {
                emptyList()
            }
        }

        val hour = now.hour
        val inQuietHours = NotificationTimeRules.isQuietHours(hour, settings.quietHoursStart, settings.quietHoursEnd)

        if (inQuietHours) {
            return emptyList()
        }

        val candidates = mutableListOf<Candidate>()

        // 2. CUSTOM REMINDER FIX
        // Removed `!snapshot.studiedToday` so the reminder still queues even if they opened the app earlier.
        if (settings.isCategoryEnabled(NotificationCategory.REMINDERS) &&
            isDailyReminderEligible(trigger, settings, now)
        ) {
            candidates += Candidate(NotificationType.DAILY_REMINDER, "daily")
        }

        // 3. STREAK FIX
        // Removed `!snapshot.studiedToday` here as well so users can get encouraged anytime.
        if (settings.isCategoryEnabled(NotificationCategory.STREAKS) &&
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

        // 4. TASKS FIX
        // Evaluates during the day, not just the evening.
        if (settings.isCategoryEnabled(NotificationCategory.QUESTS) &&
            isTasksPendingEligible(snapshot, trigger, now)
        ) {
            candidates += Candidate(
                NotificationType.TASKS_PENDING,
                "tasks_${snapshot.pendingTrialTasksCount}",
            )
        }

        // 5. CHAPTER SPAM PREVENTION
        // Added a check against `sentTodayKeys` to drop the candidate if it was already sent.
        if (settings.isCategoryEnabled(NotificationCategory.REMINDERS) &&
            isChapterProgressEligible(snapshot, trigger)
        ) {
            snapshot.inProgressChapter?.let { chapter ->
                val dedupKey = "chapter_${chapter.chapterId}"
                if (dedupKey !in sentTodayKeys) {
                    candidates += Candidate(NotificationType.CHAPTER_PROGRESS, dedupKey)
                }
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

        // 6. WEEKLY XP FIX
        // Removed the Thursday-Saturday restriction and 85% requirement.
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
        // Removed: if (snapshot.streakCount <= 0) return false
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

        // Removed weekend-only check

        val target = snapshot.weeklyXpTarget
        if (target <= 0) return false

        // Removed 85% requirement limit; simply returns true if they haven't met the target yet
        return snapshot.weeklyXp < target
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
        // Removed evening-only check
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
            // Lowered minimum inactive days from 3..6 down to 1..6
            inactiveDays in 1..6 ->
                Candidate(NotificationType.INACTIVITY_3, "inactivity")
            inactiveDays in 7..13 ->
                Candidate(NotificationType.INACTIVITY_7, "inactivity")
            inactiveDays >= 14 ->
                Candidate(NotificationType.INACTIVITY_14, "inactivity")
            else -> null
        }
    }
}