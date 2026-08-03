package com.ncert7.aitutorandlab.domain.examplan

import com.ncert7.aitutorandlab.data.local.dao.ExamPlanDao
import com.ncert7.aitutorandlab.data.local.dao.PlanTrialItemDao
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanDayEntity
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanDayStatus
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemEntity
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemStatus
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.repository.FriendRepository
import com.ncert7.aitutorandlab.repository.PlanTrialRepository
import com.ncert7.aitutorandlab.service.analytics.GamificationAnalyticsTracker
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanTrialRolloverService @Inject constructor(
    private val examPlanDao: ExamPlanDao,
    private val planTrialItemDao: PlanTrialItemDao,
    private val planTrialRepository: PlanTrialRepository,
    private val friendRepository: FriendRepository,
    private val planMutationLock: ExamPlanMutationLock,
) {
    private val zone = ZoneId.of("Asia/Kolkata")

    suspend fun process(studentId: String, languageCode: String) {
        if (studentId.isBlank()) return
        val plan = examPlanDao.getActivePlan(studentId) ?: return
        if (!plan.isActive) return

        if (handleDeadlineIfNeeded(studentId, languageCode)) return

        val days = examPlanDao.getPlanDays(studentId)
        if (days.isEmpty()) return

        val todayEpoch = LocalDate.now(zone).toEpochDay()
        rolloverPastDays(studentId, days.sortedBy { it.dayIndex }, todayEpoch)
        finalizeCalendarStatuses(studentId, examPlanDao.getPlanDays(studentId), todayEpoch)
    }

    private suspend fun handleDeadlineIfNeeded(
        studentId: String,
        @Suppress("UNUSED_PARAMETER") languageCode: String,
    ): Boolean {
        val days = examPlanDao.getPlanDays(studentId)
        val examDay =
            days.lastOrNull { it.dayType == "EXAM" }
                ?: days.maxByOrNull { it.calendarEpochDay }
                ?: return false

        val todayEpoch = LocalDate.now(zone).toEpochDay()
        if (todayEpoch < examDay.calendarEpochDay) return false

        val incompleteCount = planTrialItemDao.countIncompleteForStudent(studentId)
        if (incompleteCount == 0) return false

        val studyIncomplete = planTrialItemDao.countIncompleteByKind(studentId, "STUDY")
        val simIncomplete = incompleteCount - studyIncomplete
        friendRepository.publishSelfFeedEvent(
            ownerStudentId = studentId,
            eventType = com.ncert7.aitutorandlab.domain.gamification.FriendEventType.PLAN_EXPIRED_INCOMPLETE,
            message = buildExpiredPlanMessage(studyIncomplete, simIncomplete),
            eventKey = "${studentId}_PLAN_EXPIRED_${examDay.calendarEpochDay}",
        )

        planTrialRepository.clearTrialItems(studentId)
        examPlanDao.deletePlanDays(studentId)
        examPlanDao.deletePlan(studentId)

        GamificationAnalyticsTracker.planDeletedDeadline(incompleteCount)

        DebugLogger.debugLog(
            TAG,
            "Expired plan for $studentId — $incompleteCount incomplete trial items at deadline",
        )
        return true
    }

    private suspend fun rolloverPastDays(
        studentId: String,
        sortedDays: List<ExamPlanDayEntity>,
        todayEpoch: Long,
    ) {
        for (index in sortedDays.indices) {
            val day = sortedDays[index]
            if (day.calendarEpochDay >= todayEpoch) continue

            val items = planTrialItemDao.getItemsForDay(studentId, day.dayIndex)
            val incomplete =
                items.filter { it.status != PlanTrialItemStatus.DONE }
            if (incomplete.isEmpty()) {
                if (day.status != ExamPlanDayStatus.PARTIAL) {
                    examPlanDao.updateDayStatus(studentId, day.dayIndex, ExamPlanDayStatus.DONE)
                }
                continue
            }

            examPlanDao.updateDayStatus(studentId, day.dayIndex, ExamPlanDayStatus.PARTIAL)

            val nextDay = sortedDays.getOrNull(index + 1) ?: continue
            carryItemsToNextDay(
                studentId = studentId,
                incomplete = incomplete,
                fromDayIndex = day.dayIndex,
                targetDay = nextDay,
            )
        }
    }

    private suspend fun carryItemsToNextDay(
        studentId: String,
        incomplete: List<PlanTrialItemEntity>,
        fromDayIndex: Int,
        targetDay: ExamPlanDayEntity,
    ) {
        planMutationLock.withPlanMutation {
            val freshTarget =
                examPlanDao.getPlanDays(studentId).firstOrNull { it.dayIndex == targetDay.dayIndex }
                    ?: return@withPlanMutation
            if (freshTarget.id == 0L) return@withPlanMutation

            val existing = planTrialItemDao.getItemsForDay(studentId, freshTarget.dayIndex)
            val existingKeys = existing.map { it.itemKey() }.toSet()

            val toMove =
                incomplete.filter { item ->
                    item.itemKey() !in existingKeys
                }
            if (toMove.isEmpty()) {
                planTrialItemDao.deleteItemsByIds(incomplete.map { it.id })
                return@withPlanMutation
            }

            val moved =
                toMove.map { item ->
                    item.copy(
                        id = 0,
                        planDayId = freshTarget.id,
                        dayIndex = freshTarget.dayIndex,
                        carriedFromDayIndex = fromDayIndex,
                        updatedAt = System.currentTimeMillis(),
                    )
                }

            val keptOnTarget =
                existing.filter { existingItem ->
                    moved.none { it.itemKey() == existingItem.itemKey() }
                }
            val merged = moved + keptOnTarget
            val resequenced =
                merged.mapIndexed { sequenceIndex, item ->
                    item.copy(sequenceIndex = sequenceIndex)
                }

            planTrialItemDao.deleteItemsByIds(incomplete.map { it.id })
            planTrialItemDao.upsertItems(resequenced)
            GamificationAnalyticsTracker.trialRollover(
                movedCount = toMove.size,
                fromDay = fromDayIndex,
                toDay = freshTarget.dayIndex,
            )
        }
    }

    private suspend fun finalizeCalendarStatuses(
        studentId: String,
        days: List<ExamPlanDayEntity>,
        todayEpoch: Long,
    ) {
        days.forEach { day ->
            val status =
                when {
                    day.status == ExamPlanDayStatus.PARTIAL -> ExamPlanDayStatus.PARTIAL
                    day.calendarEpochDay > todayEpoch -> ExamPlanDayStatus.UPCOMING
                    day.calendarEpochDay == todayEpoch -> ExamPlanDayStatus.TODAY
                    else -> ExamPlanDayStatus.DONE
                }
            if (day.status != status) {
                examPlanDao.updateDayStatus(studentId, day.dayIndex, status)
            }
        }
    }

    private fun PlanTrialItemEntity.itemKey(): String =
        "$kind|$conceptId|$sourceId"

    private fun buildExpiredPlanMessage(studyIncomplete: Int, simIncomplete: Int): String =
        when {
            studyIncomplete > 0 && simIncomplete > 0 ->
                "Your exam plan ended with $studyIncomplete study and $simIncomplete simulation tasks unfinished. Start a new plan when you're ready."
            studyIncomplete > 0 ->
                "Your exam plan ended with $studyIncomplete study tasks unfinished. Start a new plan when you're ready."
            simIncomplete > 0 ->
                "Your exam plan ended with $simIncomplete simulation tasks unfinished. Start a new plan when you're ready."
            else ->
                "Your exam plan ended before all tasks were finished. Start a new plan when you're ready."
        }

    companion object {
        private const val TAG = "PlanTrialRollover"
    }
}
