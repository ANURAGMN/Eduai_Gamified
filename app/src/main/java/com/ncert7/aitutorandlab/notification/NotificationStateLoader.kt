package com.ncert7.aitutorandlab.notification

import android.content.Context
import com.anurag.eduai.uikit.avatar.AvatarUnlockStore
import com.anurag.eduai.uikit.avatar.weeklyAvatarPresets
import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.data.local.dao.ChapterAgentProgressDao
import com.ncert7.aitutorandlab.data.local.dao.ChapterDao
import com.ncert7.aitutorandlab.data.local.dao.ExamPlanDao
import com.ncert7.aitutorandlab.data.local.dao.NotificationLogDao
import com.ncert7.aitutorandlab.data.local.dao.PlanTrialItemDao
import com.ncert7.aitutorandlab.data.local.dao.ProgressDao
import com.ncert7.aitutorandlab.data.local.dao.SessionDao
import com.ncert7.aitutorandlab.data.local.dao.StreakDao
import com.ncert7.aitutorandlab.data.local.dao.StudentDao
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanDayEntity
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemStatus
import com.ncert7.aitutorandlab.domain.gamification.EconomyConfig
import com.ncert7.aitutorandlab.domain.gamification.StreakFreezeService
import com.ncert7.aitutorandlab.repository.GamificationRepository
import com.ncert7.aitutorandlab.utils.normalizeLanguageCode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationStateLoader @Inject constructor(
    private val streakDao: StreakDao,
    private val progressDao: ProgressDao,
    private val studentDao: StudentDao,
    private val examPlanDao: ExamPlanDao,
    private val planTrialItemDao: PlanTrialItemDao,
    private val sessionDao: SessionDao,
    private val notificationLogDao: NotificationLogDao,
    private val gamificationRepository: GamificationRepository,
    private val chapterAgentProgressDao: ChapterAgentProgressDao,
    private val chapterDao: ChapterDao,
    private val streakFreezeService: StreakFreezeService,
    @ApplicationContext private val appContext: Context,
) {
    private val zone = ZoneId.of("Asia/Kolkata")

    suspend fun load(studentId: String, languageCode: String): NotificationSnapshot? {
        if (studentId.isBlank()) return null

        streakFreezeService.applyAutoFreezeIfEligible(studentId)

        val student = studentDao.getStudentSync(studentId)
        val streak = streakDao.getStreakByUserId(studentId)
        val (startOfDay, endOfDay) = todayBounds()
        val completedToday =
            progressDao.getTodayFullyCompletedActivityCount(
                studentId = studentId,
                startOfDay = startOfDay,
                endOfDay = endOfDay,
                appName = AppConfig.APP_NAME,
            )
        val studiedToday =
            completedToday > 0 ||
                streak?.let { studiedOnStreakDay(it.lastStreakDate) } == true

        val planDay = resolveTodayPlanDay(studentId)
        val pendingTrialTasksCount = resolvePendingTrialTasks(studentId, planDay)
        val daysToExam = resolveDaysToExam(studentId)
        val daysSinceLastActivity = resolveDaysSinceLastActivity()
        val inactivity14PreviouslySent =
            notificationLogDao.wasShownEver(
                studentId = studentId,
                type = NotificationType.INACTIVITY_14.id,
                dedupKey = "inactivity",
            )
        val weeklyXp = gamificationRepository.getProfile(studentId)?.weeklyXp ?: 0
        val expiringAvatar = resolveExpiringAvatar()
        val inProgressChapter = resolveInProgressChapter(studentId, languageCode)
        val streakSavedPending = streakFreezeService.hasPendingStreakSavedNotification(studentId)

        return NotificationSnapshot(
            studentId = studentId,
            studentName = student?.studentName?.substringBefore(" ").orEmpty(),
            languageCode = normalizeLanguageCode(languageCode),
            streakCount = streak?.streakCount ?: 0,
            studiedToday = studiedToday,
            todayPlanLabel = planDay?.label?.lineSequence()?.firstOrNull().orEmpty(),
            todayPlanMinutes = planDay?.estimatedMinutes ?: 30,
            todayDayIndex = planDay?.dayIndex,
            pendingTrialTasksCount = pendingTrialTasksCount,
            daysToExam = daysToExam,
            daysSinceLastActivity = daysSinceLastActivity,
            inactivity14PreviouslySent = inactivity14PreviouslySent,
            weeklyXp = weeklyXp,
            weeklyXpTarget = EconomyConfig.WEEKLY_XP_BAR_TARGET,
            expiringAvatar = expiringAvatar,
            inProgressChapter = inProgressChapter,
            streakSavedPending = streakSavedPending,
        )
    }

    private suspend fun resolveInProgressChapter(
        studentId: String,
        languageCode: String,
    ): InProgressChapter? {
        val lang = normalizeLanguageCode(languageCode)
        val row =
            chapterAgentProgressDao
                .getAllChapterProgress(studentId, lang, AppConfig.APP_NAME)
                .filter { progress ->
                    progress.overallPercentage in 1..99 ||
                        progress.status.equals("IN_PROGRESS", ignoreCase = true)
                }
                .sortedByDescending { it.updatedAt }
                .firstOrNull()
                ?: return null

        val chapter = chapterDao.getChapterById(row.chapterId) ?: return null
        val name =
            if (lang.equals("kn", ignoreCase = true)) {
                chapter.chapterNameKannada.ifBlank { chapter.chapterName }
            } else {
                chapter.chapterName
            }
        return InProgressChapter(
            chapterId = row.chapterId,
            chapterName = name,
            overallPercent = row.overallPercentage.coerceIn(1, 99),
        )
    }

    private fun resolveExpiringAvatar(): ExpiringAvatar? {
        if (!NotificationAvatarRules.isWithin24HoursOfWeeklyDrop()) return null
        AvatarUnlockStore.load(appContext)
        val now = System.currentTimeMillis()
        val unused =
            weeklyAvatarPresets(now).firstOrNull { preset ->
                !AvatarUnlockStore.isUnlocked(preset.id)
            } ?: return null
        return ExpiringAvatar(id = unused.id, name = unused.name)
    }

    private suspend fun resolvePendingTrialTasks(
        studentId: String,
        planDay: ExamPlanDayEntity?,
    ): Int {
        val dayIndex = planDay?.dayIndex ?: return 0
        return planTrialItemDao
            .getItemsForDay(studentId, dayIndex)
            .count { it.status != PlanTrialItemStatus.DONE }
    }

    private suspend fun resolveDaysToExam(studentId: String): Int? {
        val plan = examPlanDao.getActivePlan(studentId) ?: return null
        val examDay = plan.examEpochDay
        if (examDay <= 0L) return null
        val days = (examDay - LocalDate.now(zone).toEpochDay()).toInt()
        return days.takeIf { it > 0 }
    }

    private suspend fun resolveDaysSinceLastActivity(): Int? {
        val latest = sessionDao.getLatestSession() ?: return null
        val lastActiveMs = latest.sessionEndTime ?: latest.sessionStartTime
        if (lastActiveMs <= 0L) return null
        val lastDay = LocalDate.ofInstant(Instant.ofEpochMilli(lastActiveMs), zone)
        val days = (LocalDate.now(zone).toEpochDay() - lastDay.toEpochDay()).toInt()
        return days.takeIf { it > 0 }
    }

    private fun todayBounds(): Pair<Long, Long> {
        val today = LocalDate.now(zone)
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    private suspend fun resolveTodayPlanDay(studentId: String): ExamPlanDayEntity? {
        val todayEpoch = LocalDate.now(zone).toEpochDay()
        return examPlanDao.getPlanDays(studentId).firstOrNull { day ->
            day.calendarEpochDay == todayEpoch
        }
    }

    private fun studiedOnStreakDay(lastStreakDateMs: Long): Boolean {
        if (lastStreakDateMs <= 0L) return false
        val streakDay =
            LocalDate.ofInstant(Instant.ofEpochMilli(lastStreakDateMs), zone)
        return streakDay == LocalDate.now(zone)
    }
}
