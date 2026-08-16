package com.ncert7.aitutorandlab.service.sync

import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.data.local.dao.ExamPlanDao
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanDayEntity
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanEntity
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.repository.FirebaseRepository
import com.ncert7.aitutorandlab.service.analytics.EngagementAnalyticsTracker

/**
 * Mirrors the active exam plan + day rows to Firestore.
 * Trial items are rematerialized locally after restore (stable concept lineup).
 */
class ExamPlanSyncManager(
    private val examPlanDao: ExamPlanDao,
    private val firebaseRepository: FirebaseRepository,
) {

    suspend fun pushPlans() {
        try {
            examPlanDao.getUnsyncedActivePlans().forEach { plan ->
                pushPlan(plan.studentId)
            }
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "pushPlans failed: ${e.message}")
        }
    }

    suspend fun pushPlan(studentId: String) {
        if (studentId.isBlank()) return
        try {
            val plan = examPlanDao.getActivePlan(studentId) ?: return
            val days = examPlanDao.getPlanDays(studentId)
            val ok =
                firebaseRepository.saveExamPlan(
                    userId = studentId,
                    plan = plan.toPayload(),
                    days = days.map { it.toPayload() },
                )
            if (ok) examPlanDao.markPlanSynced(studentId)
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "pushPlan failed: ${e.message}")
        }
    }

    /**
     * Restore remote plan when local has none, or remote `updatedAt` is newer.
     * Returns true when a remote plan was written into Room.
     */
    suspend fun restorePlan(studentId: String): Boolean {
        if (studentId.isBlank()) return false
        return try {
            val remote = firebaseRepository.getExamPlan(studentId)
            if (remote == null) {
                EngagementAnalyticsTracker.restoreSkipped(DOMAIN, "remote_empty")
                return false
            }
            val remoteUpdated = (remote["updatedAt"] as? Number)?.toLong() ?: 0L
            val local = examPlanDao.getActivePlan(studentId)
            if (local != null && local.updatedAt >= remoteUpdated) {
                EngagementAnalyticsTracker.restoreSkipped(DOMAIN, "local_newer")
                return false
            }
            val restored =
                ExamPlanEntity(
                    studentId = studentId,
                    subjectId = remote["subjectId"] as? String ?: return false,
                    examType = remote["examType"] as? String ?: "Unit Test",
                    dailyMinutes = (remote["dailyMinutes"] as? Number)?.toInt() ?: 30,
                    startEpochDay = (remote["startEpochDay"] as? Number)?.toLong() ?: 0L,
                    examEpochDay = (remote["examEpochDay"] as? Number)?.toLong()
                        ?: (remote["startEpochDay"] as? Number)?.toLong()
                        ?: 0L,
                    chapterIds = remote["chapterIds"] as? String ?: "",
                    isActive = remote["isActive"] as? Boolean ?: true,
                    updatedAt = remoteUpdated.takeIf { it > 0L } ?: System.currentTimeMillis(),
                    isSynced = true,
                )
            val remoteDays = firebaseRepository.getExamPlanDays(studentId)
            val days =
                remoteDays.mapNotNull { map ->
                    val dayIndex = (map["dayIndex"] as? Number)?.toInt() ?: return@mapNotNull null
                    ExamPlanDayEntity(
                        id = 0,
                        studentId = studentId,
                        dayIndex = dayIndex,
                        calendarEpochDay = (map["calendarEpochDay"] as? Number)?.toLong() ?: 0L,
                        dayType = map["dayType"] as? String ?: "LESSON",
                        status = map["status"] as? String ?: "UPCOMING",
                        label = map["label"] as? String ?: "",
                        conceptIds = map["conceptIds"] as? String ?: "",
                        estimatedMinutes = (map["estimatedMinutes"] as? Number)?.toInt() ?: 18,
                    )
                }.sortedBy { it.dayIndex }

            examPlanDao.upsertPlan(restored)
            examPlanDao.deletePlanDays(studentId)
            if (days.isNotEmpty()) examPlanDao.upsertDays(days)

            EngagementAnalyticsTracker.restoreApplied(DOMAIN, itemCount = days.size)
            DebugLogger.debugLog(TAG, "Exam plan restored for $studentId days=${days.size}")
            true
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "restorePlan failed: ${e.message}")
            EngagementAnalyticsTracker.restoreSkipped(DOMAIN, "error")
            false
        }
    }

    private fun ExamPlanEntity.toPayload(): Map<String, Any?> =
        mapOf(
            "studentId" to studentId,
            "subjectId" to subjectId,
            "examType" to examType,
            "dailyMinutes" to dailyMinutes,
            "startEpochDay" to startEpochDay,
            "examEpochDay" to examEpochDay,
            "chapterIds" to chapterIds,
            "isActive" to isActive,
            "updatedAt" to updatedAt,
            "appName" to AppConfig.APP_NAME,
        )

    private fun ExamPlanDayEntity.toPayload(): Map<String, Any?> =
        mapOf(
            "studentId" to studentId,
            "dayIndex" to dayIndex,
            "calendarEpochDay" to calendarEpochDay,
            "dayType" to dayType,
            "status" to status,
            "label" to label,
            "conceptIds" to conceptIds,
            "estimatedMinutes" to estimatedMinutes,
            "appName" to AppConfig.APP_NAME,
        )

    companion object {
        private const val TAG = "ExamPlanSyncManager"
        private const val DOMAIN = "exam_plan"
    }
}
