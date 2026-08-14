package com.ncert7.aitutorandlab.domain.gamification

import android.app.Activity
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.repository.GamificationRepository
import com.ncert7.aitutorandlab.repository.QuestRepository
import com.ncert7.aitutorandlab.service.ads.RewardedAdManager
import com.ncert7.aitutorandlab.service.analytics.AdPlacement
import javax.inject.Inject
import javax.inject.Singleton

enum class QuestClaimResult {
    SUCCESS,
    NOT_ELIGIBLE,
    NOT_READY,
    AD_SKIPPED,
    DAILY_CAP_REACHED,
    GRANT_FAILED,
}

@Singleton
class QuestGemRewardService @Inject constructor(
    private val questRepository: QuestRepository,
    private val gamificationRepository: GamificationRepository,
    private val rewardedAdManager: RewardedAdManager,
    private val rewardEventBus: RewardEventBus,
) {
    companion object {
        private const val TAG = "QuestGemRewardService"
    }

    fun isRewardedAdReady(): Boolean = rewardedAdManager.isReady()

    fun preloadRewardedAd() {
        rewardedAdManager.preload()
    }

    suspend fun claimWithRewardedAd(
        activity: Activity,
        studentId: String,
        claimType: QuestClaimType,
    ): QuestClaimResult {
        if (studentId.isBlank()) return QuestClaimResult.NOT_ELIGIBLE
        if (!isQuestClaimEligible(studentId, claimType)) return QuestClaimResult.NOT_ELIGIBLE

        val questDate = QuestDayKey.current()
        if (!gamificationRepository.canGrantQuestGemToday(studentId, questDate)) {
            return QuestClaimResult.DAILY_CAP_REACHED
        }

        val placement =
            when (claimType) {
                QuestClaimType.BONUS -> AdPlacement.QUEST_BONUS
                else -> AdPlacement.QUEST_CLAIM
            }

        if (!rewardedAdManager.isReady()) {
            rewardedAdManager.preload()
            // Give a loading ad a brief window (matches the mandatory-claim path) instead of failing
            // instantly — reduces spurious "not ready" when the ad is a moment from ready.
            if (!rewardedAdManager.awaitReady(timeoutMs = 5_000)) {
                return QuestClaimResult.NOT_READY
            }
        }

        val earned = rewardedAdManager.showForReward(activity, placement)
        if (!earned) return QuestClaimResult.AD_SKIPPED

        val grantKey = claimType.grantKey(questDate)
        val gems =
            gamificationRepository.grantGemsIfEligible(
                studentId = studentId,
                grantKey = grantKey,
                gemsAmount = claimType.gemAmount(),
                source = claimType.sourceLabel(),
            )
        if (gems == null) {
            // grantGemsIfEligible is idempotent and only returns null once the gems for this grantKey
            // are already recorded. That state is reached when a prior attempt granted gems but its
            // claim-flag write failed — recover here by ensuring the quest is marked claimed, so the UI
            // doesn't stay stuck showing it as claimable forever (and no gems are re-granted).
            if (gamificationRepository.hasGemGrant(studentId, grantKey)) {
                val remarked = markQuestClaimed(claimType, studentId)
                DebugLogger.debugLog(
                    TAG,
                    "Quest claim $claimType recovered — gems already granted, remark=$remarked ($grantKey)",
                )
                return if (remarked) QuestClaimResult.SUCCESS else QuestClaimResult.GRANT_FAILED
            }
            return QuestClaimResult.GRANT_FAILED
        }

        val marked = markQuestClaimed(claimType, studentId)
        if (!marked) {
            DebugLogger.errorLog(
                TAG,
                "Quest not marked claimed after gem grant for $grantKey — recovers on next claim attempt",
            )
        }

        val profile = gamificationRepository.getProfile(studentId)
        rewardEventBus.tryEmit(
            RewardUiEvent(
                xpEarned = 0,
                gemsEarned = gems,
                xpBarFrom = 0f,
                xpBarTo = 0f,
                weeklyXpTotal = profile?.weeklyXp ?: 0,
            ),
        )

        DebugLogger.debugLog(TAG, "Quest claim $claimType granted +$gems gems ($grantKey)")
        questRepository.maybeClearAdTestOverride(studentId)
        return QuestClaimResult.SUCCESS
    }

    private suspend fun markQuestClaimed(claimType: QuestClaimType, studentId: String): Boolean =
        when (claimType) {
            QuestClaimType.SIMS -> questRepository.claimSims(studentId)
            QuestClaimType.STUDY -> questRepository.claimStudy(studentId)
            QuestClaimType.BONUS -> questRepository.claimBonus(studentId)
        }

    private suspend fun isQuestClaimEligible(
        studentId: String,
        claimType: QuestClaimType,
    ): Boolean =
        when (claimType) {
            QuestClaimType.SIMS -> questRepository.canClaimSims(studentId)
            QuestClaimType.STUDY -> questRepository.canClaimStudy(studentId)
            QuestClaimType.BONUS -> questRepository.canClaimBonus(studentId)
        }
}
