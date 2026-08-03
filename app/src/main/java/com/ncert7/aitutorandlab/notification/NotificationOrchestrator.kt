package com.ncert7.aitutorandlab.notification

import com.ncert7.aitutorandlab.BuildConfig
import com.ncert7.aitutorandlab.config.GamificationFeatureFlags
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.domain.gamification.StreakFreezeService
import com.ncert7.aitutorandlab.service.analytics.GamificationAnalyticsTracker
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationOrchestrator @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val sharedPreferenceUtils: SharedPreferenceUtils,
    private val settingsStore: NotificationSettingsStore,
    private val stateLoader: NotificationStateLoader,
    private val ledger: NotificationLedger,
    private val notificationHelper: NotificationHelper,
    private val streakFreezeService: StreakFreezeService,
) {
    sealed class DebugFireResult {
        data class Fired(val typeId: String) : DebugFireResult()

        data object NotDebugBuild : DebugFireResult()

        data object NoPermission : DebugFireResult()

        data object NotLoggedIn : DebugFireResult()
    }

    suspend fun runEvalPass(trigger: NotificationEvalTrigger) {
        if (!GamificationFeatureFlags.isGamifiedHomeEnabled(appContext)) return
        if (!settingsStore.isMasterEnabled()) return
        if (settingsStore.reminderMode() == NotificationReminderMode.OFF) return
        if (!NotificationPermissionHelper.hasPostNotificationsPermission(appContext)) return

        val studentId = sharedPreferenceUtils.getUserId()?.takeIf { it.isNotBlank() } ?: return
        val languageCode = sharedPreferenceUtils.getLanguagePreference().orEmpty()
        val snapshot = stateLoader.load(studentId, languageCode) ?: return

        val candidates =
            NotificationEvaluator.eligibleCandidates(
                snapshot = snapshot,
                settings = settingsStore.toEvalSettings(),
                trigger = trigger,
            )
        if (candidates.isEmpty()) return

        val dailyCap = settingsStore.effectiveDailyCap()
        if (ledger.blockReason(studentId, dailyCap) != null) return

        val todayEpochDay = NotificationTimeRules.todayEpochDay()
        val winner =
            candidates.firstOrNull { candidate ->
                !ledger.wasShownToday(
                    studentId = studentId,
                    type = candidate.type,
                    dedupKey = candidate.dedupKey,
                    todayEpochDay = todayEpochDay,
                )
            } ?: return

        val tokens = buildTokens(snapshot, winner.type)
        val content =
            NotificationContentCatalog.resolve(
                type = winner.type,
                tokens = tokens,
                languageCode = snapshot.languageCode,
            )
        val resolved =
            content.copy(
                deepLinkParams = deepLinkParamsFor(winner.type, snapshot),
            )

        notificationHelper.fire(resolved)
        ledger.recordSend(
            studentId = studentId,
            type = winner.type,
            dedupKey = winner.dedupKey,
            todayEpochDay = todayEpochDay,
        )
        if (winner.type == NotificationType.STREAK_SAVED) {
            streakFreezeService.clearPendingStreakSavedNotification(studentId)
        }
        GamificationAnalyticsTracker.notificationShown(winner.type.id)
        DebugLogger.debugLog(TAG, "Fired ${winner.type.id} via $trigger")
    }

    /** Debug-only: bypasses quiet hours, studied-today, budget, and dedup checks. */
    suspend fun fireDebugTest(type: NotificationType): DebugFireResult {
        if (!BuildConfig.DEBUG) return DebugFireResult.NotDebugBuild
        if (!NotificationPermissionHelper.hasPostNotificationsPermission(appContext)) {
            return DebugFireResult.NoPermission
        }

        val studentId = sharedPreferenceUtils.getUserId()?.takeIf { it.isNotBlank() }
            ?: return DebugFireResult.NotLoggedIn
        val languageCode = sharedPreferenceUtils.getLanguagePreference().orEmpty()
        val snapshot = stateLoader.load(studentId, languageCode) ?: return DebugFireResult.NotLoggedIn

        val tokens = buildTokens(snapshot, type, debug = true)
        val content =
            NotificationContentCatalog.resolve(
                type = type,
                tokens = tokens,
                languageCode = snapshot.languageCode,
            )

        notificationHelper.fire(
            content.copy(deepLinkParams = deepLinkParamsFor(type, snapshot)),
        )
        DebugLogger.debugLog(TAG, "Debug fired ${type.id}")
        return DebugFireResult.Fired(type.id)
    }

    companion object {
        private const val TAG = "NotificationOrchestrator"

        private fun buildTokens(
            snapshot: NotificationSnapshot,
            type: NotificationType,
            debug: Boolean = false,
        ): NotificationTokens {
            val bite =
                when (type) {
                    NotificationType.CHAPTER_PROGRESS ->
                        snapshot.inProgressChapter?.chapterName?.takeIf { it.isNotBlank() }
                            ?: if (debug) "Fractions" else "your chapter"
                    else -> snapshot.todayPlanLabel.ifBlank { "today's plan" }
                }
            val days =
                when (type) {
                    NotificationType.EXAM_COUNTDOWN ->
                        snapshot.daysToExam ?: if (debug) 5 else 0
                    NotificationType.TASKS_PENDING ->
                        snapshot.pendingTrialTasksCount.takeIf { it > 0 } ?: if (debug) 2 else 0
                    NotificationType.CHAPTER_PROGRESS ->
                        snapshot.inProgressChapter?.overallPercent
                            ?: if (debug) 42 else 0
                    NotificationType.INACTIVITY_3 ->
                        snapshot.daysSinceLastActivity?.coerceAtLeast(3) ?: if (debug) 3 else 3
                    NotificationType.INACTIVITY_7 ->
                        snapshot.daysSinceLastActivity?.coerceAtLeast(7) ?: if (debug) 7 else 7
                    NotificationType.INACTIVITY_14 ->
                        snapshot.daysSinceLastActivity?.coerceAtLeast(14) ?: if (debug) 14 else 14
                    else -> snapshot.todayPlanMinutes.coerceAtLeast(1)
                }
            return NotificationTokens(
                name = snapshot.studentName.ifBlank { "there" },
                bite = bite,
                days = days.coerceAtLeast(0),
                streak = snapshot.streakCount.coerceAtLeast(if (type == NotificationType.STREAK_AT_RISK) 1 else 0),
                avatar =
                    snapshot.expiringAvatar?.name
                        ?: if (debug && type == NotificationType.AVATAR_UNLOCK_EXPIRING) "Nova" else "",
            )
        }

        private fun deepLinkParamsFor(
            type: NotificationType,
            snapshot: NotificationSnapshot,
        ): Map<String, String> =
            when (type) {
                NotificationType.DAILY_REMINDER,
                NotificationType.STREAK_AT_RISK,
                NotificationType.TASKS_PENDING,
                NotificationType.STREAK_COMEBACK,
                -> mapOf("dayIndex" to (snapshot.todayDayIndex ?: 1).toString())

                NotificationType.CHAPTER_PROGRESS ->
                    snapshot.inProgressChapter?.chapterId?.let { mapOf("chapterId" to it) } ?: emptyMap()

                else -> emptyMap()
            }
    }
}
