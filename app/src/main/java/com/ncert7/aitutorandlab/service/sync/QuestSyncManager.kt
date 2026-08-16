package com.ncert7.aitutorandlab.service.sync

import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.data.local.dao.QuestDailyDao
import com.ncert7.aitutorandlab.data.local.entities.QuestDailyEntity
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.domain.gamification.QuestDayKey
import com.ncert7.aitutorandlab.repository.FirebaseRepository
import com.ncert7.aitutorandlab.service.analytics.EngagementAnalyticsTracker

/**
 * Syncs today's quest claim flags so reinstall cannot double-claim (R.4).
 * Progress counters (simsDone/studyDone) are recomputed locally; claimed flags are OR-merged.
 */
class QuestSyncManager(
    private val questDailyDao: QuestDailyDao,
    private val firebaseRepository: FirebaseRepository,
) {

    suspend fun pushTodayQuest(studentId: String) {
        if (studentId.isBlank()) return
        try {
            val questDate = QuestDayKey.current()
            val quest = questDailyDao.getQuest(studentId, questDate) ?: return
            if (quest.isSynced) return
            val ok = firebaseRepository.saveQuestDaily(studentId, questDate, quest.toPayload())
            if (ok) {
                questDailyDao.upsertQuest(quest.copy(isSynced = true))
            }
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "pushTodayQuest failed: ${e.message}")
        }
    }

    suspend fun restoreTodayQuest(studentId: String): Boolean {
        if (studentId.isBlank()) return false
        return try {
            val questDate = QuestDayKey.current()
            val remote = firebaseRepository.getQuestDaily(studentId, questDate)
            if (remote == null) {
                EngagementAnalyticsTracker.restoreSkipped(DOMAIN, "remote_empty")
                return false
            }
            val local = questDailyDao.getQuest(studentId, questDate)
            val merged =
                QuestDailyEntity(
                    studentId = studentId,
                    questDate = questDate,
                    simsDone = maxOf(local?.simsDone ?: 0, (remote["simsDone"] as? Number)?.toInt() ?: 0),
                    simsTotal = maxOf(local?.simsTotal ?: 0, (remote["simsTotal"] as? Number)?.toInt() ?: 0)
                        .coerceAtLeast(3),
                    studyDone = maxOf(local?.studyDone ?: 0, (remote["studyDone"] as? Number)?.toInt() ?: 0),
                    studyTotal = maxOf(local?.studyTotal ?: 0, (remote["studyTotal"] as? Number)?.toInt() ?: 0)
                        .coerceAtLeast(1),
                    // OR-merge claims — once claimed anywhere, stays claimed (R.4).
                    simsClaimed = (local?.simsClaimed == true) || (remote["simsClaimed"] as? Boolean == true),
                    studyClaimed = (local?.studyClaimed == true) || (remote["studyClaimed"] as? Boolean == true),
                    bonusClaimed = (local?.bonusClaimed == true) || (remote["bonusClaimed"] as? Boolean == true),
                    updatedAt = maxOf(
                        local?.updatedAt ?: 0L,
                        (remote["updatedAt"] as? Number)?.toLong() ?: 0L,
                    ),
                    isSynced = true,
                )
            questDailyDao.upsertQuest(merged)
            EngagementAnalyticsTracker.restoreApplied(DOMAIN, itemCount = 1)
            DebugLogger.debugLog(TAG, "Quest restored for $studentId date=$questDate")
            true
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "restoreTodayQuest failed: ${e.message}")
            EngagementAnalyticsTracker.restoreSkipped(DOMAIN, "error")
            false
        }
    }

    private fun QuestDailyEntity.toPayload(): Map<String, Any?> =
        mapOf(
            "studentId" to studentId,
            "questDate" to questDate,
            "simsDone" to simsDone,
            "simsTotal" to simsTotal,
            "studyDone" to studyDone,
            "studyTotal" to studyTotal,
            "simsClaimed" to simsClaimed,
            "studyClaimed" to studyClaimed,
            "bonusClaimed" to bonusClaimed,
            "updatedAt" to updatedAt,
            "appName" to AppConfig.APP_NAME,
        )

    companion object {
        private const val TAG = "QuestSyncManager"
        private const val DOMAIN = "quest"
    }
}
