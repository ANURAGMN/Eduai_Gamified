package com.ncert7.aitutorandlab.repository

import com.ncert7.aitutorandlab.data.local.dao.ChapterDao
import com.ncert7.aitutorandlab.data.local.dao.ExamPlanDao
import com.ncert7.aitutorandlab.data.local.dao.PlanTrialItemDao
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanDayEntity
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanEntity
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemEntity
import com.ncert7.aitutorandlab.utils.getLocalizedName
import java.time.LocalDate
import java.time.ZoneId
import com.ncert7.aitutorandlab.domain.examplan.ExamPlanMutationLock
import com.ncert7.aitutorandlab.domain.examplan.PlanTrialMaterializer
import com.ncert7.aitutorandlab.domain.migration.AppDataMigrationRunner
import com.ncert7.aitutorandlab.domain.migration.TrialScheduleMigrator
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanTrialRepository @Inject constructor(
    private val planTrialItemDao: PlanTrialItemDao,
    private val examPlanDao: ExamPlanDao,
    private val chapterDao: ChapterDao,
    private val materializer: PlanTrialMaterializer,
    private val appDataMigrationRunner: Lazy<AppDataMigrationRunner>,
    private val planMutationLock: ExamPlanMutationLock,
) : TrialScheduleMigrator {
    fun observeTrialItems(studentId: String, dayIndex: Int): Flow<List<PlanTrialItemEntity>> =
        planTrialItemDao.observeItemsForDay(studentId, dayIndex)

    suspend fun getTrialItems(studentId: String, dayIndex: Int): List<PlanTrialItemEntity> =
        planTrialItemDao.getItemsForDay(studentId, dayIndex)

    suspend fun ensureTrialItemsForDay(
        day: ExamPlanDayEntity,
        languageCode: String,
    ) {
        val resolved = resolvePlanDay(day) ?: return
        appDataMigrationRunner.get().runPendingMigrations(resolved.studentId, languageCode)
        planMutationLock.withPlanMutation {
            val freshDay = resolvePlanDay(day) ?: return@withPlanMutation
            syncTrialItemsForResolvedDay(freshDay, languageCode)
        }
    }

    /**
     * Ensures a standalone chapter trial exists (opened from the chapter picker). Stored
     * under a chapter-scoped [dayIndex]. Rematerializes when the syllabus queue changes
     * (e.g. older builds only stored SIM_URL and omitted STUDY / MATH / SIM_AGENT), merging
     * progress by kind+concept+source so done sims stay done.
     */
    suspend fun ensureChapterTrial(
        studentId: String,
        chapterId: String,
        dayIndex: Int,
        languageCode: String,
    ) {
        if (studentId.isBlank()) return
        val planDayId = ensureChapterTrialPlanDay(studentId, dayIndex, chapterId, languageCode) ?: return
        val day =
            examPlanDao.getPlanDays(studentId).firstOrNull { it.dayIndex == dayIndex } ?: return
        val expected =
            materializer.materializeChapter(
                studentId = studentId,
                chapterId = chapterId,
                dayIndex = dayIndex,
                planDayId = planDayId,
                languageCode = languageCode,
            )
        if (expected.isEmpty()) return

        val existing = planTrialItemDao.getItemsForDay(studentId, dayIndex)
        if (existing.isEmpty()) {
            planTrialItemDao.upsertItems(expected)
            return
        }
        if (trialOrderSignature(existing) == trialOrderSignature(expected)) return
        applyRematerializedItems(day, expected, existing)
    }

    /**
     * Chapter trials use a negative [dayIndex] and need a real [ExamPlanDayEntity] row so trial
     * items satisfy the planDayId foreign key (dayIndex alone is not a valid exam_plan_day.id).
     */
    private suspend fun ensureChapterTrialPlanDay(
        studentId: String,
        dayIndex: Int,
        chapterId: String,
        languageCode: String,
    ): Long? {
        examPlanDao
            .getPlanDays(studentId)
            .firstOrNull { it.dayIndex == dayIndex }
            ?.id
            ?.let { return it }

        ensureActivePlanForChapterTrial(studentId, chapterId)

        val chapterName =
            chapterDao.getChapter(chapterId)?.getLocalizedName(languageCode) ?: chapterId
        val day =
            ExamPlanDayEntity(
                studentId = studentId,
                dayIndex = dayIndex,
                calendarEpochDay = LocalDate.now(ZoneId.of("Asia/Kolkata")).toEpochDay(),
                dayType = "CHAPTER_TRIAL",
                status = "TODAY",
                label = chapterName,
                conceptIds = chapterId,
            )
        examPlanDao.upsertDays(listOf(day))
        return examPlanDao.getPlanDays(studentId).firstOrNull { it.dayIndex == dayIndex }?.id
    }

    private suspend fun ensureActivePlanForChapterTrial(studentId: String, chapterId: String) {
        if (examPlanDao.getActivePlan(studentId) != null) return
        val chapter = chapterDao.getChapter(chapterId) ?: return
        val today = LocalDate.now(ZoneId.of("Asia/Kolkata")).toEpochDay()
        examPlanDao.upsertPlan(
            ExamPlanEntity(
                studentId = studentId,
                subjectId = chapter.subjectId,
                chapterIds = chapterId,
                startEpochDay = today,
                examEpochDay = today + 30,
            ),
        )
    }

    /** Runs pending app-data migrations (including trial schedule rebuilds). */
    suspend fun ensureTrialScheduleCurrent(studentId: String, languageCode: String) {
        if (studentId.isBlank()) return
        appDataMigrationRunner.get().runPendingMigrations(studentId, languageCode)
    }

    override suspend fun materializeAllPlanDays(studentId: String, languageCode: String) {
        planMutationLock.withPlanMutation {
            materializeAllPlanDaysLocked(studentId, languageCode)
        }
    }

    /** Called while [ExamPlanMutationLock] is already held during plan-day replacement. */
    suspend fun materializeAllPlanDaysLocked(studentId: String, languageCode: String) {
        val days = examPlanDao.getPlanDays(studentId)
        days.forEach { day ->
            forceSyncTrialItemsForResolvedDay(day, languageCode)
        }
    }

    suspend fun clearTrialItems(studentId: String) {
        planTrialItemDao.deleteAllForStudent(studentId)
    }

    suspend fun getLatestUncelebratedDone(studentId: String, dayIndex: Int): PlanTrialItemEntity? =
        planTrialItemDao.getLatestUncelebratedDone(studentId, dayIndex)

    suspend fun getNextIncomplete(studentId: String, dayIndex: Int): PlanTrialItemEntity? =
        planTrialItemDao.getNextIncomplete(studentId, dayIndex)

    /**
     * After completing [completedItem], launch the next incomplete item forward in the queue.
     * Only when nothing remains ahead, wrap to the earliest incomplete from the start.
     */
    suspend fun getNextIncompleteAfterCompleted(
        studentId: String,
        dayIndex: Int,
        completedItem: PlanTrialItemEntity,
    ): PlanTrialItemEntity? =
        planTrialItemDao.getNextIncompleteAfterSequence(
            studentId = studentId,
            dayIndex = dayIndex,
            afterSequenceIndex = completedItem.sequenceIndex,
        ) ?: planTrialItemDao.getNextIncomplete(studentId, dayIndex)

    suspend fun getTrialItemById(itemId: Long): PlanTrialItemEntity? =
        planTrialItemDao.getItemById(itemId)

    private suspend fun resolvePlanDay(day: ExamPlanDayEntity): ExamPlanDayEntity? =
        examPlanDao
            .getPlanDays(day.studentId)
            .firstOrNull { it.dayIndex == day.dayIndex }
            ?.takeIf { it.id != 0L }

    private suspend fun syncTrialItemsForResolvedDay(
        day: ExamPlanDayEntity,
        languageCode: String,
    ) {
        if (day.id == 0L || !planDayExists(day)) return
        val expected = expectedItemsForDay(day, languageCode)
        if (expected.isEmpty()) return

        val existing = planTrialItemDao.getItemsForDay(day.studentId, day.dayIndex)
        if (existing.isEmpty()) {
            planTrialItemDao.upsertItems(expected)
            return
        }

        if (trialOrderSignature(existing) == trialOrderSignature(expected)) return

        applyRematerializedItems(day, expected, existing)
    }

    private suspend fun forceSyncTrialItemsForResolvedDay(
        day: ExamPlanDayEntity,
        languageCode: String,
    ) {
        if (day.id == 0L || !planDayExists(day)) return
        val expected = expectedItemsForDay(day, languageCode)
        if (expected.isEmpty()) return

        val existing = planTrialItemDao.getItemsForDay(day.studentId, day.dayIndex)
        if (existing.isEmpty()) {
            planTrialItemDao.upsertItems(expected)
            return
        }

        if (trialOrderSignature(existing) == trialOrderSignature(expected)) return

        applyRematerializedItems(day, expected, existing)
    }

    /** Lesson/revise days use [PlanTrialMaterializer.materializeDay]; chapter trials use materializeChapter. */
    private suspend fun expectedItemsForDay(
        day: ExamPlanDayEntity,
        languageCode: String,
    ): List<PlanTrialItemEntity> {
        if (day.dayType.equals("CHAPTER_TRIAL", ignoreCase = true)) {
            val chapterId = day.conceptIds.trim().takeIf { it.isNotBlank() } ?: return emptyList()
            return materializer.materializeChapter(
                studentId = day.studentId,
                chapterId = chapterId,
                dayIndex = day.dayIndex,
                planDayId = day.id,
                languageCode = languageCode,
            )
        }
        return materializer.materializeDay(day, languageCode)
    }

    private suspend fun planDayExists(day: ExamPlanDayEntity): Boolean =
        examPlanDao.getPlanDays(day.studentId).any { it.id == day.id }

    private suspend fun applyRematerializedItems(
        day: ExamPlanDayEntity,
        expected: List<PlanTrialItemEntity>,
        existing: List<PlanTrialItemEntity>,
    ) {
        val merged = mergeTrialProgress(expected, existing)
        planTrialItemDao.deleteForPlanDay(day.studentId, day.id)
        planTrialItemDao.upsertItems(merged)
    }

    private fun mergeTrialProgress(
        expected: List<PlanTrialItemEntity>,
        existing: List<PlanTrialItemEntity>,
    ): List<PlanTrialItemEntity> {
        val progressByKey = existing.associateBy { it.progressKey() }
        return expected.map { item ->
            progressByKey[item.progressKey()]?.let { old ->
                item.copy(
                    completedCount = old.completedCount,
                    status = old.status,
                    celebrated = old.celebrated,
                    carriedFromDayIndex = old.carriedFromDayIndex,
                )
            } ?: item
        }
    }

    // Key by kind + conceptId (NOT sourceId): sourceId is the language-specific URL/ID, so a
    // language switch would otherwise look like a brand-new set of items.
    private fun PlanTrialItemEntity.progressKey(): String = "$kind|$conceptId"

    // Same stability as progressKey — rematerialize when the concept/kind lineup changes, not when
    // only the sim URL language flips (URLs still update via forceSync / first upsert).
    private fun trialOrderSignature(items: List<PlanTrialItemEntity>): List<Pair<String, String>> =
        items.sortedBy { it.sequenceIndex }.map { it.kind to it.conceptId }
}
