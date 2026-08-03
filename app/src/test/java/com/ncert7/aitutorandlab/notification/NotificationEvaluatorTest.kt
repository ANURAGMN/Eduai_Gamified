package com.ncert7.aitutorandlab.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

class NotificationEvaluatorTest {
    private val snapshot =
        NotificationSnapshot(
            studentId = "u1",
            studentName = "Sam",
            languageCode = "en",
            streakCount = 5,
            studiedToday = false,
            todayPlanLabel = "Math",
            todayPlanMinutes = 20,
            todayDayIndex = 1,
        )

    private fun settings(
        mode: NotificationReminderMode = NotificationReminderMode.STANDARD,
        reminderHour: Int = 17,
        reminderMinute: Int = 0,
        quietStart: Int = 20,
        quietEnd: Int = 8,
        categories: Set<NotificationCategory> =
            setOf(
                NotificationCategory.REMINDERS,
                NotificationCategory.STREAKS,
                NotificationCategory.QUESTS,
            ),
    ) = NotificationEvalSettings(
        reminderMode = mode,
        reminderHour = reminderHour,
        reminderMinute = reminderMinute,
        quietHoursStart = quietStart,
        quietHoursEnd = quietEnd,
        enabledCategories = categories,
    )

    @Test
    fun dailyReminder_eligibleOnExactAlarm() {
        val result =
            NotificationEvaluator.eligibleCandidates(
                snapshot = snapshot,
                settings = settings(),
                trigger = NotificationEvalTrigger.DAILY_ALARM,
                now = LocalTime.of(17, 0),
            )
        assertEquals(listOf(NotificationType.DAILY_REMINDER), result.map { it.type })
    }

    @Test
    fun streakAtRisk_eligibleAfterSixPmOnSweep() {
        val result =
            NotificationEvaluator.eligibleCandidates(
                snapshot = snapshot,
                settings = settings(),
                trigger = NotificationEvalTrigger.PERIODIC_SWEEP,
                now = LocalTime.of(18, 30),
            )
        assertEquals(
            listOf(NotificationType.STREAK_AT_RISK),
            result.map { it.type },
        )
    }

    @Test
    fun streakAtRisk_notEligibleBeforeSixPm() {
        val result =
            NotificationEvaluator.eligibleCandidates(
                snapshot = snapshot,
                settings = settings(),
                trigger = NotificationEvalTrigger.PERIODIC_SWEEP,
                now = LocalTime.of(17, 30),
            )
        assertEquals(listOf(NotificationType.DAILY_REMINDER), result.map { it.type })
    }

    @Test
    fun studiedToday_blocksDailyAndStreakButNotExamCountdown() {
        val result =
            NotificationEvaluator.eligibleCandidates(
                snapshot =
                    snapshot.copy(
                        studiedToday = true,
                        daysToExam = 5,
                    ),
                settings = settings(),
                trigger = NotificationEvalTrigger.PERIODIC_SWEEP,
                now = LocalTime.of(17, 0),
            )
        assertEquals(listOf(NotificationType.EXAM_COUNTDOWN), result.map { it.type })
    }

    @Test
    fun examCountdown_hasHighestPriorityAmongEveningCandidates() {
        val result =
            NotificationEvaluator.eligibleCandidates(
                snapshot =
                    snapshot.copy(
                        daysToExam = 3,
                        pendingTrialTasksCount = 2,
                    ),
                settings = settings(),
                trigger = NotificationEvalTrigger.PERIODIC_SWEEP,
                now = LocalTime.of(18, 30),
            )
        assertEquals(NotificationType.EXAM_COUNTDOWN, result.first().type)
    }

    @Test
    fun tasksPending_eligibleInEveningWithPendingItems() {
        val result =
            NotificationEvaluator.eligibleCandidates(
                snapshot = snapshot.copy(pendingTrialTasksCount = 2, daysToExam = null),
                settings = settings(),
                trigger = NotificationEvalTrigger.PERIODIC_SWEEP,
                now = LocalTime.of(19, 0),
            )
        assertTrue(result.any { it.type == NotificationType.TASKS_PENDING })
    }

    @Test
    fun inactivity3_eligibleAfterThreeDaysAway() {
        val result =
            NotificationEvaluator.eligibleCandidates(
                snapshot = snapshot.copy(daysSinceLastActivity = 3, daysToExam = null),
                settings = settings(),
                trigger = NotificationEvalTrigger.PERIODIC_SWEEP,
                now = LocalTime.of(11, 0),
            )
        assertEquals(listOf(NotificationType.INACTIVITY_3), result.map { it.type })
    }

    @Test
    fun inactivity14_suppressedAfterPreviouslySent() {
        val result =
            NotificationEvaluator.eligibleCandidates(
                snapshot =
                    snapshot.copy(
                        daysSinceLastActivity = 20,
                        inactivity14PreviouslySent = true,
                        daysToExam = null,
                    ),
                settings = settings(),
                trigger = NotificationEvalTrigger.PERIODIC_SWEEP,
                now = LocalTime.of(11, 0),
            )
        assertFalse(result.any { it.type.name.startsWith("INACTIVITY") })
    }

    @Test
    fun quietHours_blocksAll() {
        val result =
            NotificationEvaluator.eligibleCandidates(
                snapshot = snapshot,
                settings = settings(),
                trigger = NotificationEvalTrigger.PERIODIC_SWEEP,
                now = LocalTime.of(21, 0),
            )
        assertTrue(result.isEmpty())
    }

    @Test
    fun reminderModeOff_blocksAll() {
        val result =
            NotificationEvaluator.eligibleCandidates(
                snapshot = snapshot,
                settings = settings(mode = NotificationReminderMode.OFF),
                trigger = NotificationEvalTrigger.DAILY_ALARM,
                now = LocalTime.of(17, 0),
            )
        assertTrue(result.isEmpty())
    }

    @Test
    fun streakComeback_eligibleOneDayAfterBreak() {
        val result =
            NotificationEvaluator.eligibleCandidates(
                snapshot =
                    snapshot.copy(
                        daysSinceLastActivity = 1,
                        studiedToday = false,
                        streakCount = 4,
                        daysToExam = null,
                    ),
                settings = settings(),
                trigger = NotificationEvalTrigger.PERIODIC_SWEEP,
                now = LocalTime.of(11, 0),
            )
        assertEquals(listOf(NotificationType.STREAK_COMEBACK), result.map { it.type })
    }

    @Test
    fun weeklyXpClose_eligibleOnThursdayNearTarget() {
        val thursday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.THURSDAY))
        val result =
            NotificationEvaluator.eligibleCandidates(
                snapshot =
                    snapshot.copy(
                        weeklyXp = 430,
                        weeklyXpTarget = 500,
                        daysToExam = null,
                    ),
                settings = settings(),
                trigger = NotificationEvalTrigger.PERIODIC_SWEEP,
                now = LocalTime.of(11, 0),
                today = thursday,
            )
        assertTrue(result.any { it.type == NotificationType.WEEKLY_XP_CLOSE })
    }

    @Test
    fun weeklyXpClose_notEligibleOnMonday() {
        val monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        val result =
            NotificationEvaluator.eligibleCandidates(
                snapshot =
                    snapshot.copy(
                        weeklyXp = 430,
                        weeklyXpTarget = 500,
                        daysToExam = null,
                    ),
                settings = settings(),
                trigger = NotificationEvalTrigger.PERIODIC_SWEEP,
                now = LocalTime.of(11, 0),
                today = monday,
            )
        assertFalse(result.any { it.type == NotificationType.WEEKLY_XP_CLOSE })
    }

    @Test
    fun avatarUnlockExpiring_eligibleWhenUnusedAvatarNearDrop() {
        val result =
            NotificationEvaluator.eligibleCandidates(
                snapshot =
                    snapshot.copy(
                        expiringAvatar = ExpiringAvatar(id = "nova", name = "Nova"),
                        daysToExam = null,
                    ),
                settings =
                    settings(
                        categories =
                            setOf(
                                NotificationCategory.REMINDERS,
                                NotificationCategory.STREAKS,
                                NotificationCategory.QUESTS,
                                NotificationCategory.AVATAR,
                            ),
                    ),
                trigger = NotificationEvalTrigger.PERIODIC_SWEEP,
                now = LocalTime.of(11, 0),
            )
        assertEquals(listOf(NotificationType.AVATAR_UNLOCK_EXPIRING), result.map { it.type })
    }

    @Test
    fun disabledCategory_excludesMatchingTypes() {
        val result =
            NotificationEvaluator.eligibleCandidates(
                snapshot =
                    snapshot.copy(
                        daysToExam = 3,
                        pendingTrialTasksCount = 2,
                    ),
                settings =
                    settings(
                        categories = setOf(NotificationCategory.REMINDERS),
                    ),
                trigger = NotificationEvalTrigger.PERIODIC_SWEEP,
                now = LocalTime.of(18, 30),
            )
        assertFalse(result.any { it.type == NotificationType.STREAK_AT_RISK })
        assertFalse(result.any { it.type == NotificationType.TASKS_PENDING })
        assertTrue(result.any { it.type == NotificationType.EXAM_COUNTDOWN })
    }

    @Test
    fun priority_examCountdownBeatsStreakAtRiskInEvening() {
        val result =
            NotificationEvaluator.eligibleCandidates(
                snapshot =
                    snapshot.copy(
                        daysToExam = 2,
                        pendingTrialTasksCount = 1,
                    ),
                settings = settings(),
                trigger = NotificationEvalTrigger.PERIODIC_SWEEP,
                now = LocalTime.of(19, 0),
            )
        assertEquals(NotificationType.EXAM_COUNTDOWN, result.first().type)
    }

    @Test
    fun inactivity3_notEligibleWhenStudiedToday() {
        val result =
            NotificationEvaluator.eligibleCandidates(
                snapshot =
                    snapshot.copy(
                        studiedToday = true,
                        daysSinceLastActivity = 4,
                        daysToExam = null,
                    ),
                settings = settings(),
                trigger = NotificationEvalTrigger.PERIODIC_SWEEP,
                now = LocalTime.of(11, 0),
            )
        assertFalse(result.any { it.type.name.startsWith("INACTIVITY") })
    }

    @Test
    fun chapterProgress_eligibleWithInProgressChapter() {
        val result =
            NotificationEvaluator.eligibleCandidates(
                snapshot =
                    snapshot.copy(
                        inProgressChapter =
                            InProgressChapter(
                                chapterId = "ch1",
                                chapterName = "Fractions",
                                overallPercent = 42,
                            ),
                        daysToExam = null,
                    ),
                settings = settings(),
                trigger = NotificationEvalTrigger.PERIODIC_SWEEP,
                now = LocalTime.of(11, 0),
            )
        assertTrue(result.any { it.type == NotificationType.CHAPTER_PROGRESS })
    }

    @Test
    fun streakSaved_eligibleWhenFreezePending() {
        val result =
            NotificationEvaluator.eligibleCandidates(
                snapshot =
                    snapshot.copy(
                        streakSavedPending = true,
                        daysToExam = null,
                    ),
                settings = settings(),
                trigger = NotificationEvalTrigger.PERIODIC_SWEEP,
                now = LocalTime.of(11, 0),
            )
        assertEquals(listOf(NotificationType.STREAK_SAVED), result.map { it.type })
    }

    @Test
    fun streakSaved_notEligibleWithoutPendingFlag() {
        val result =
            NotificationEvaluator.eligibleCandidates(
                snapshot = snapshot.copy(streakSavedPending = false, daysToExam = null),
                settings = settings(),
                trigger = NotificationEvalTrigger.PERIODIC_SWEEP,
                now = LocalTime.of(11, 0),
            )
        assertFalse(result.any { it.type == NotificationType.STREAK_SAVED })
    }
}
