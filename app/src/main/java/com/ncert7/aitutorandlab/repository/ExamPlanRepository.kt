package com.ncert7.aitutorandlab.repository

import com.ncert7.aitutorandlab.data.local.dao.ChapterDao
import com.ncert7.aitutorandlab.data.local.dao.ConceptDao
import com.ncert7.aitutorandlab.data.local.dao.ExamPlanDao
import com.ncert7.aitutorandlab.data.local.dao.ProgressDao
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanDayEntity
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanEntity
import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.domain.examplan.DefaultExamPlan
import com.ncert7.aitutorandlab.domain.examplan.ExamPlanMutationLock
import com.ncert7.aitutorandlab.domain.examplan.ExamPlanGenerator
import com.ncert7.aitutorandlab.domain.examplan.PlanFeasibilityAnalyzer
import com.ncert7.aitutorandlab.domain.examplan.PlanFeasibilityIssue
import com.ncert7.aitutorandlab.domain.examplan.PlanFeasibilityResult
import com.ncert7.aitutorandlab.domain.examplan.PlanFeasibilitySeverity
import com.ncert7.aitutorandlab.domain.examplan.PlanTrialRolloverService
import com.ncert7.aitutorandlab.service.analytics.GamificationAnalyticsTracker
import com.ncert7.aitutorandlab.utils.SubjectIds
import com.ncert7.aitutorandlab.utils.getLocalizedName
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExamPlanRepository @Inject constructor(
    private val examPlanDao: ExamPlanDao,
    private val chapterDao: ChapterDao,
    private val conceptDao: ConceptDao,
    private val progressDao: ProgressDao,
    private val planTrialRepository: PlanTrialRepository,
    private val planTrialRolloverService: PlanTrialRolloverService,
    private val planFeasibilityAnalyzer: PlanFeasibilityAnalyzer,
    private val sharedPrefs: SharedPreferenceUtils,
    private val planMutationLock: ExamPlanMutationLock,
) {
    private val zone = ZoneId.of("Asia/Kolkata")

    fun observePlanDays(studentId: String): Flow<List<ExamPlanDayEntity>> =
        examPlanDao.observePlanDays(studentId)

    fun observeActivePlan(studentId: String): Flow<ExamPlanEntity?> =
        examPlanDao.observeActivePlan(studentId)

    suspend fun getActivePlan(studentId: String): ExamPlanEntity? =
        examPlanDao.getActivePlan(studentId)

    suspend fun ensureActivePlan(
        studentId: String,
        subjectId: String,
        languageCode: String,
    ) {
        if (studentId.isBlank()) return

        if (!sharedPrefs.isExamPlanUserConfigured()) {
            // First-run picks include the exam plan — don't seed the generic default while home applies them.
            if (sharedPrefs.hasCompletedFirstRun() && !sharedPrefs.hasAppliedOnboardingPicks()) {
                return
            }
            ensureDefaultPlan(studentId, languageCode)
            return
        }

        if (subjectId.isBlank()) return

        val existing = examPlanDao.getActivePlan(studentId)
        if (existing != null && existing.subjectId == subjectId) {
            refreshDayStatuses(studentId, languageCode)
            return
        }

        val chapters =
            chapterDao.getChaptersForSubjectSync(subjectId)
                .sortedBy { it.orderIndex }
        if (chapters.isEmpty()) return

        val chapterIds = chapters.map { it.chapterId }
        val concepts = mutableListOf<com.ncert7.aitutorandlab.data.local.entities.ConceptEntity>()
        chapters.forEach { chapter ->
            concepts.addAll(conceptDao.getConceptsForChapterSync(chapter.chapterId, "STUDY"))
        }

        val generated =
            ExamPlanGenerator.generate(
                chapterIds = chapterIds,
                concepts = concepts,
                conceptLabel = { it.getLocalizedName(languageCode) },
            )

        val completedIndices = findCompletedLessonDays(studentId, generated.days, languageCode)
        val withStatus =
            ExamPlanGenerator.resolveStatuses(
                days = generated.days,
                completedDayIndices = completedIndices,
            )

        val plan =
            ExamPlanEntity(
                studentId = studentId,
                subjectId = subjectId,
                examType = generated.examType,
                dailyMinutes = generated.dailyMinutes,
                startEpochDay = generated.startEpochDay,
                examEpochDay = generated.days.lastOrNull()?.calendarEpochDay ?: generated.startEpochDay,
                chapterIds = generated.chapterIds.joinToString(","),
                updatedAt = System.currentTimeMillis(),
                isSynced = false,
            )

        planMutationLock.withPlanMutation {
            examPlanDao.upsertPlan(plan)
            examPlanDao.deletePlanDays(studentId)
            examPlanDao.upsertDays(
                withStatus.map { (day, status) ->
                    ExamPlanDayEntity(
                        studentId = studentId,
                        dayIndex = day.dayIndex,
                        calendarEpochDay = day.calendarEpochDay,
                        dayType = day.dayType,
                        status = status,
                        label = day.label,
                        conceptIds = day.conceptIds.joinToString(","),
                        estimatedMinutes = day.estimatedMinutes,
                    )
                },
            )
            planTrialRepository.materializeAllPlanDaysLocked(studentId, languageCode)
        }
        schedulePlanUpload()
    }

    private suspend fun ensureDefaultPlan(studentId: String, languageCode: String) {
        sharedPrefs.setSubjectSelectionId(SubjectIds.SCIENCE)

        val chapters =
            chapterDao.getChaptersForSubjectSync(SubjectIds.SCIENCE)
                .sortedBy { it.orderIndex }
        val chapterId = DefaultExamPlan.resolveChapterId(chapters) ?: return

        val existing = examPlanDao.getActivePlan(studentId)
        if (existing != null && DefaultExamPlan.matchesPlan(existing, chapterId)) {
            refreshDayStatuses(studentId, languageCode)
            return
        }

        val examDate = DefaultExamPlan.defaultExamDate()
        var dailyMinutes = DefaultExamPlan.DAILY_MINUTES
        while (dailyMinutes <= 90) {
            val result =
                createCustomPlan(
                    studentId = studentId,
                    subjectId = SubjectIds.SCIENCE,
                    chapterIds = listOf(chapterId),
                    languageCode = languageCode,
                    examType = DefaultExamPlan.EXAM_TYPE,
                    dailyMinutes = dailyMinutes,
                    examDate = examDate,
                )
            if (result.canSave) return
            dailyMinutes += 15
        }
    }

    suspend fun refreshDayStatuses(studentId: String, languageCode: String) {
        planTrialRepository.ensureTrialScheduleCurrent(studentId, languageCode)
        planTrialRolloverService.process(studentId, languageCode)
    }

    private suspend fun findCompletedLessonDays(
        studentId: String,
        days: List<ExamPlanGenerator.GeneratedDay>,
        languageCode: String,
    ): Set<Int> {
        val completed = mutableSetOf<Int>()
        for (day in days) {
            if (day.dayType != "LESSON" || day.conceptIds.isEmpty()) continue
            val allDone =
                day.conceptIds.all { conceptId ->
                    val progress =
                        progressDao.getProgress(
                            studentId = studentId,
                            itemType = "CONCEPT",
                            itemId = conceptId,
                            language = languageCode,
                            appName = AppConfig.APP_NAME,
                        )
                    progress?.status == "COMPLETED"
                }
            if (allDone) completed.add(day.dayIndex)
        }
        return completed
    }

    suspend fun getTodayPlanDay(studentId: String): ExamPlanDayEntity? {
        val days = examPlanDao.getPlanDays(studentId).filter { it.isExamScheduleDay() }
        return days.firstOrNull { it.status == "TODAY" }
            ?: days.firstOrNull { it.status == "UPCOMING" }
    }

    suspend fun getPlanDay(studentId: String, dayIndex: Int): ExamPlanDayEntity? =
        examPlanDao.getPlanDays(studentId).firstOrNull { it.dayIndex == dayIndex }

    suspend fun analyzePlanFeasibility(
        chapterIds: List<String>,
        languageCode: String,
        dailyMinutes: Int,
        examDate: LocalDate,
        examType: String,
        startDate: LocalDate = LocalDate.now(zone),
    ): PlanFeasibilityResult =
        planFeasibilityAnalyzer.analyze(
            chapterIds = chapterIds.distinct(),
            languageCode = languageCode,
            dailyMinutes = dailyMinutes,
            examDate = examDate,
            startDate = startDate,
            examType = examType,
        )

    suspend fun createCustomPlan(
        studentId: String,
        subjectId: String,
        chapterIds: List<String>,
        languageCode: String,
        examType: String,
        dailyMinutes: Int,
        examDate: LocalDate,
        startDate: LocalDate = LocalDate.now(zone),
    ): PlanFeasibilityResult {
        if (studentId.isBlank() || subjectId.isBlank() || chapterIds.isEmpty()) {
            return PlanFeasibilityResult(
                requiredPlanDays = 0,
                lessonDays = 0,
                reviseDays = 0,
                mockExamDays = 0,
                availableCalendarDays = 0,
                totalTrialItems = 0,
                issues =
                    listOf(
                        PlanFeasibilityIssue(
                            severity = PlanFeasibilitySeverity.ERROR,
                            message = "Select at least one chapter.",
                        ),
                    ),
            )
        }

        val feasibility =
            planFeasibilityAnalyzer.analyze(
                chapterIds = chapterIds.distinct(),
                languageCode = languageCode,
                dailyMinutes = dailyMinutes,
                examDate = examDate,
                startDate = startDate,
                examType = examType,
            )
        if (!feasibility.canSave) return feasibility

        val selectedChapterIds = chapterIds.distinct()
        val concepts = mutableListOf<com.ncert7.aitutorandlab.data.local.entities.ConceptEntity>()
        selectedChapterIds.forEach { chapterId ->
            // Study items = plain STUDY concepts plus math-subject "MATH PROBLEM" concepts,
            // deduped by conceptId and kept in chapter/orderIndex order.
            concepts.addAll(conceptDao.getConceptsForChapterSync(chapterId, "STUDY"))
            concepts.addAll(conceptDao.getConceptsForChapterSync(chapterId, "MATH PROBLEM"))
        }
        val dedupedConcepts =
            concepts.distinctBy { it.conceptId }.sortedWith(compareBy({ it.chapterId }, { it.orderIndex }))
        concepts.clear()
        concepts.addAll(dedupedConcepts)

        val scheduleStart =
            planFeasibilityAnalyzer.computeStartDate(
                examDate = examDate,
                requiredPlanDays = feasibility.requiredPlanDays,
                earliestAllowedStart = startDate,
            ) ?: return feasibility.copy(
                issues =
                    feasibility.issues +
                        PlanFeasibilityIssue(
                            severity = PlanFeasibilitySeverity.ERROR,
                            message = "Could not schedule plan before the exam date.",
                        ),
            )

        val generated =
            ExamPlanGenerator.generate(
                chapterIds = selectedChapterIds,
                concepts = concepts,
                conceptLabel = { it.getLocalizedName(languageCode) },
                dailyMinutes = dailyMinutes.coerceIn(15, 90),
                startDate = scheduleStart,
                examType = examType,
            )

        val completedIndices = findCompletedLessonDays(studentId, generated.days, languageCode)
        val withStatus =
            ExamPlanGenerator.resolveStatuses(
                days = generated.days,
                completedDayIndices = completedIndices,
            )

        val plan =
            ExamPlanEntity(
                studentId = studentId,
                subjectId = subjectId,
                examType = generated.examType,
                dailyMinutes = generated.dailyMinutes,
                startEpochDay = generated.startEpochDay,
                examEpochDay = examDate.toEpochDay(),
                chapterIds = generated.chapterIds.joinToString(","),
                updatedAt = System.currentTimeMillis(),
                isSynced = false,
            )

        planMutationLock.withPlanMutation {
            examPlanDao.upsertPlan(plan)
            examPlanDao.deletePlanDays(studentId)
            examPlanDao.upsertDays(
                withStatus.map { (day, status) ->
                    ExamPlanDayEntity(
                        studentId = studentId,
                        dayIndex = day.dayIndex,
                        calendarEpochDay = day.calendarEpochDay,
                        dayType = day.dayType,
                        status = status,
                        label = day.label,
                        conceptIds = day.conceptIds.joinToString(","),
                        estimatedMinutes = day.estimatedMinutes,
                    )
                },
            )
            planTrialRepository.materializeAllPlanDaysLocked(studentId, languageCode)
        }
        schedulePlanUpload()
        GamificationAnalyticsTracker.planCreated(
            chapterCount = selectedChapterIds.size,
            dailyMinutes = dailyMinutes,
            daysToExam = feasibility.requiredPlanDays,
            totalItems = feasibility.totalTrialItems,
        )
        return feasibility
    }

    /**
     * Creates a one-chapter exam plan from first-run onboarding — exam ~1 week out, same defaults
     * as [DefaultExamPlan]. Marks the plan as user-configured so [ensureActivePlan] won't overwrite.
     */
    suspend fun createOnboardingPlan(
        studentId: String,
        subjectId: String,
        chapterId: String,
        languageCode: String,
    ): Boolean {
        if (studentId.isBlank() || subjectId.isBlank() || chapterId.isBlank()) return false
        var dailyMinutes = DefaultExamPlan.DAILY_MINUTES
        val examDate = DefaultExamPlan.defaultExamDate()
        while (dailyMinutes <= 90) {
            val result =
                createCustomPlan(
                    studentId = studentId,
                    subjectId = subjectId,
                    chapterIds = listOf(chapterId),
                    languageCode = languageCode,
                    examType = DefaultExamPlan.EXAM_TYPE,
                    dailyMinutes = dailyMinutes,
                    examDate = examDate,
                )
            if (result.canSave) {
                sharedPrefs.setExamPlanUserConfigured(true)
                return true
            }
            dailyMinutes += 15
        }
        return false
    }

    private fun schedulePlanUpload() {
        try {
            com.ncert7.aitutorandlab.service.sync.DataSyncService.scheduleDeferredUpload()
        } catch (_: Exception) {
        }
    }
}
