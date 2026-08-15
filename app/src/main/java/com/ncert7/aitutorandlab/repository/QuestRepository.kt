package com.ncert7.aitutorandlab.repository

import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.local.dao.ExamPlanDao
import com.ncert7.aitutorandlab.data.local.dao.QuestDailyDao
import com.ncert7.aitutorandlab.data.local.dao.ProgressDao
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanDayEntity
import com.ncert7.aitutorandlab.data.local.entities.QuestDailyEntity
import com.ncert7.aitutorandlab.domain.examplan.TrialQuestProgress
import com.ncert7.aitutorandlab.domain.gamification.DailyQuestEngine
import com.ncert7.aitutorandlab.domain.gamification.QuestClaimType
import com.ncert7.aitutorandlab.domain.gamification.QuestDayKey
import com.ncert7.aitutorandlab.service.analytics.GamificationAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.QuestKind
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestRepository @Inject constructor(
    private val questDailyDao: QuestDailyDao,
    private val progressDao: ProgressDao,
    private val examPlanDao: ExamPlanDao,
    private val planTrialRepository: PlanTrialRepository,
    private val gamificationRepository: GamificationRepository,
    private val sharedPrefs: SharedPreferenceUtils,
) {
    fun observeTodayQuest(studentId: String): Flow<QuestDailyEntity?> {
        val questDate = QuestDayKey.current()
        return questDailyDao.observeQuest(studentId, questDate)
    }

    suspend fun refreshTodayQuest(studentId: String, languageCode: String) {
        if (studentId.isBlank()) return

        val questDate = QuestDayKey.current()
        val startOfDay = QuestDayKey.startOfDayMillis(questDate)
        val endOfDay = QuestDayKey.endOfDayMillis(questDate)

        val todayPlanDay = resolveTodayPlanDay(studentId)
        val plan = examPlanDao.getActivePlan(studentId)
        val planChapterIds =
            plan?.chapterIds
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                .orEmpty()
        if (todayPlanDay != null) {
            planTrialRepository.ensureTrialItemsForDay(todayPlanDay, languageCode)
        }
        val trialItems =
            todayPlanDay?.let { day ->
                planTrialRepository.getTrialItems(studentId, day.dayIndex)
            }.orEmpty()

        val calculated =
            TrialQuestProgress.fromTrialItems(trialItems, todayPlanDay)
                ?: DailyQuestEngine.calculate(
                    todayPlanDay = todayPlanDay,
                    planChapterIds = planChapterIds,
                    studentId = studentId,
                    languageCode = languageCode,
                    startOfDay = startOfDay,
                    endOfDay = endOfDay,
                    progressDao = progressDao,
                )

        val existing = questDailyDao.getQuest(studentId, questDate)
        val overrideActive = sharedPrefs.isQuestAdTestOverrideActive(questDate)
        val simsDone =
            if (overrideActive && calculated.simsTotal > 0) {
                calculated.simsTotal
            } else {
                calculated.simsDone
            }
        val studyDone =
            if (overrideActive && calculated.studyTotal > 0) {
                calculated.studyTotal
            } else {
                calculated.studyDone
            }
        val entity =
            QuestDailyEntity(
                studentId = studentId,
                questDate = questDate,
                simsDone = simsDone,
                simsTotal = calculated.simsTotal,
                studyDone = studyDone,
                studyTotal = calculated.studyTotal,
                simsClaimed = existing?.simsClaimed ?: false,
                studyClaimed = existing?.studyClaimed ?: false,
                bonusClaimed = existing?.bonusClaimed ?: false,
                updatedAt = System.currentTimeMillis(),
                isSynced = false,
            )
        trackQuestTransitions(existing, entity)
        questDailyDao.upsertQuest(entity)
        scheduleQuestUpload()
    }

    private fun trackQuestTransitions(before: QuestDailyEntity?, after: QuestDailyEntity) {
        if (before == null) return
        val simsJustCompleted =
            before.simsDone < before.simsTotal &&
                after.simsDone >= after.simsTotal &&
                after.simsTotal > 0
        val studyJustCompleted =
            before.studyDone < before.studyTotal &&
                after.studyDone >= after.studyTotal &&
                after.studyTotal > 0
        if (simsJustCompleted) GamificationAnalyticsTracker.questComplete(QuestKind.SIMS)
        if (studyJustCompleted) GamificationAnalyticsTracker.questComplete(QuestKind.STUDY)
        val bonusNowUnlocked =
            after.simsTotal > 0 &&
                after.studyTotal > 0 &&
                after.simsDone >= after.simsTotal &&
                after.studyDone >= after.studyTotal &&
                (before.simsDone < before.simsTotal || before.studyDone < before.studyTotal)
        if (bonusNowUnlocked && !after.bonusClaimed) {
            GamificationAnalyticsTracker.bonusUnlocked()
        }
    }

    suspend fun claimSims(studentId: String): Boolean {
        val questDate = QuestDayKey.current()
        healClaimFromGemGrant(studentId, QuestClaimType.SIMS, questDate)
        val quest = questDailyDao.getQuest(studentId, questDate) ?: return false
        if (quest.simsClaimed || quest.simsTotal <= 0 || quest.simsDone < quest.simsTotal) return false
        questDailyDao.markSimsClaimed(studentId, questDate, System.currentTimeMillis())
        scheduleQuestUpload()
        return true
    }

    suspend fun claimStudy(studentId: String): Boolean {
        val questDate = QuestDayKey.current()
        healClaimFromGemGrant(studentId, QuestClaimType.STUDY, questDate)
        val quest = questDailyDao.getQuest(studentId, questDate) ?: return false
        if (quest.studyClaimed || quest.studyTotal <= 0 || quest.studyDone < quest.studyTotal) return false
        questDailyDao.markStudyClaimed(studentId, questDate, System.currentTimeMillis())
        scheduleQuestUpload()
        return true
    }

    suspend fun claimBonus(studentId: String): Boolean {
        val questDate = QuestDayKey.current()
        healClaimFromGemGrant(studentId, QuestClaimType.BONUS, questDate)
        val quest = questDailyDao.getQuest(studentId, questDate) ?: return false
        if (quest.bonusClaimed) return false
        if (quest.simsTotal <= 0 || quest.studyTotal <= 0) return false
        if (quest.simsDone < quest.simsTotal || quest.studyDone < quest.studyTotal) return false
        questDailyDao.markBonusClaimed(studentId, questDate, System.currentTimeMillis())
        scheduleQuestUpload()
        return true
    }

    suspend fun canClaimSims(studentId: String): Boolean {
        val questDate = QuestDayKey.current()
        healClaimFromGemGrant(studentId, QuestClaimType.SIMS, questDate)
        val quest = questDailyDao.getQuest(studentId, questDate) ?: return false
        return !quest.simsClaimed && quest.simsTotal > 0 && quest.simsDone >= quest.simsTotal
    }

    suspend fun canClaimStudy(studentId: String): Boolean {
        val questDate = QuestDayKey.current()
        healClaimFromGemGrant(studentId, QuestClaimType.STUDY, questDate)
        val quest = questDailyDao.getQuest(studentId, questDate) ?: return false
        return !quest.studyClaimed && quest.studyTotal > 0 && quest.studyDone >= quest.studyTotal
    }

    suspend fun canClaimBonus(studentId: String): Boolean {
        val questDate = QuestDayKey.current()
        healClaimFromGemGrant(studentId, QuestClaimType.BONUS, questDate)
        val quest = questDailyDao.getQuest(studentId, questDate) ?: return false
        if (quest.bonusClaimed) return false
        if (quest.simsTotal <= 0 || quest.studyTotal <= 0) return false
        return quest.simsDone >= quest.simsTotal && quest.studyDone >= quest.studyTotal
    }

    /** Debug-only: mark study quest done and reset claim flags so ad flow can be tested quickly. */
    suspend fun debugPrepareAdClaimTest(studentId: String, languageCode: String) {
        if (studentId.isBlank()) return
        val questDate = QuestDayKey.current()
        refreshTodayQuest(studentId, languageCode)
        val quest = questDailyDao.getQuest(studentId, questDate) ?: return
        sharedPrefs.setQuestAdTestOverrideDate(questDate)
        questDailyDao.upsertQuest(
            quest.copy(
                simsDone = quest.simsTotal.coerceAtLeast(quest.simsDone),
                studyDone = quest.studyTotal.coerceAtLeast(1),
                simsClaimed = false,
                studyClaimed = false,
                bonusClaimed = false,
                updatedAt = System.currentTimeMillis(),
                isSynced = false,
            ),
        )
    }

    suspend fun maybeClearAdTestOverride(studentId: String) {
        val questDate = QuestDayKey.current()
        if (!sharedPrefs.isQuestAdTestOverrideActive(questDate)) return
        val quest = questDailyDao.getQuest(studentId, questDate) ?: return
        if (quest.simsClaimed && quest.studyClaimed && quest.bonusClaimed) {
            sharedPrefs.clearQuestAdTestOverride()
        }
    }

    private suspend fun resolveTodayPlanDay(studentId: String): ExamPlanDayEntity? {
        val days = examPlanDao.getPlanDays(studentId)
        return days.firstOrNull { it.status == "TODAY" }
            ?: days.firstOrNull { it.status == "UPCOMING" }
    }

    /** R.4: if gems were already granted for this claim, force the local claimed flag. */
    private suspend fun healClaimFromGemGrant(
        studentId: String,
        claimType: QuestClaimType,
        questDate: String,
    ) {
        val grantKey = claimType.grantKey(questDate)
        if (!gamificationRepository.hasGemGrant(studentId, grantKey)) return
        val quest = questDailyDao.getQuest(studentId, questDate) ?: return
        val needs =
            when (claimType) {
                QuestClaimType.SIMS -> !quest.simsClaimed
                QuestClaimType.STUDY -> !quest.studyClaimed
                QuestClaimType.BONUS -> !quest.bonusClaimed
            }
        if (!needs) return
        when (claimType) {
            QuestClaimType.SIMS ->
                questDailyDao.markSimsClaimed(studentId, questDate, System.currentTimeMillis())
            QuestClaimType.STUDY ->
                questDailyDao.markStudyClaimed(studentId, questDate, System.currentTimeMillis())
            QuestClaimType.BONUS ->
                questDailyDao.markBonusClaimed(studentId, questDate, System.currentTimeMillis())
        }
        scheduleQuestUpload()
    }

    private fun scheduleQuestUpload() {
        try {
            // RV.1: quest claim flags must reach the cloud promptly — the deferred (~5m) window
            // risks a reinstall double-grant. Push immediately; the worker stays durable/retried.
            com.ncert7.aitutorandlab.service.sync.DataSyncService.scheduleImmediateUpload()
        } catch (_: Exception) {
        }
    }
}
