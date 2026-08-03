package com.ncert7.aitutorandlab.domain.examplan

import android.app.Activity
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.domain.gamification.EconomyConfig
import com.ncert7.aitutorandlab.domain.gamification.GamificationRewardService
import com.ncert7.aitutorandlab.domain.gamification.QuestDayKey
import com.ncert7.aitutorandlab.domain.gamification.RewardUiEvent
import com.ncert7.aitutorandlab.repository.GamificationRepository
import com.ncert7.aitutorandlab.service.ads.RewardedAdManager
import com.ncert7.aitutorandlab.service.analytics.AdPlacement
import javax.inject.Inject
import javax.inject.Singleton

enum class TrialAdClaimResult {
    SUCCESS,
    NOT_READY,
    AD_SKIPPED,
    GRANT_FAILED,
}

@Singleton
class TrialRewardService @Inject constructor(
    private val sharedPrefs: SharedPreferenceUtils,
    private val gamificationRepository: GamificationRepository,
    private val gamificationRewardService: GamificationRewardService,
    private val rewardedAdManager: RewardedAdManager,
) {
    companion object {
        private const val TAG = "TrialRewardService"
    }

    fun isRewardedAdReady(): Boolean = rewardedAdManager.isReady()

    fun preloadRewardedAd() {
        rewardedAdManager.preload()
    }

    fun incrementCompletionsSinceMandatoryAd(studentId: String, trialItemId: Long): Int {
        if (sharedPrefs.getLastCountedTrialItemId(studentId) == trialItemId) {
            return sharedPrefs.getTrialCompletionsSinceMandatoryAd(studentId)
        }
        sharedPrefs.setLastCountedTrialItemId(studentId, trialItemId)
        val next = sharedPrefs.getTrialCompletionsSinceMandatoryAd(studentId) + 1
        sharedPrefs.setTrialCompletionsSinceMandatoryAd(studentId, next)
        return next
    }

    fun requiresMandatoryAdClaim(completionsSinceLastAd: Int): Boolean =
        completionsSinceLastAd > 0 &&
            completionsSinceLastAd % EconomyConfig.TRIALS_PER_MANDATORY_AD == 0

    fun resetMandatoryAdCounter(studentId: String) {
        sharedPrefs.setTrialCompletionsSinceMandatoryAd(studentId, 0)
    }

    suspend fun claimMandatoryRewardAd(
        activity: Activity,
        studentId: String,
        batchIndex: Int,
    ): TrialAdClaimResult {
        if (studentId.isBlank()) return TrialAdClaimResult.GRANT_FAILED

        if (!rewardedAdManager.isReady()) {
            rewardedAdManager.preload()
            if (!rewardedAdManager.awaitReady(timeoutMs = 5_000)) {
                return TrialAdClaimResult.NOT_READY
            }
        }

        val earned = rewardedAdManager.showForReward(activity, AdPlacement.TRIAL_CLAIM)
        if (!earned) return TrialAdClaimResult.AD_SKIPPED

        val grantKey = mandatoryGrantKey(studentId, batchIndex)
        val gems =
            gamificationRepository.grantGemsIfEligible(
                studentId = studentId,
                grantKey = grantKey,
                gemsAmount = EconomyConfig.GEM_TRIAL_MANDATORY_CLAIM,
                source = "trial_mandatory_claim",
            )

        resetMandatoryAdCounter(studentId)

        if (gems != null || gamificationRepository.hasGemGrant(studentId, grantKey)) {
            DebugLogger.debugLog(TAG, "Mandatory trial claim ad OK — gems=${gems ?: 0} ($grantKey)")
            return TrialAdClaimResult.SUCCESS
        }

        DebugLogger.errorLog(TAG, "Mandatory trial gem grant failed for $grantKey")
        return TrialAdClaimResult.GRANT_FAILED
    }

    suspend fun claimDoubleXpAd(
        activity: Activity,
        studentId: String,
        trialItemId: Long,
        kind: String,
        language: String,
    ): Pair<TrialAdClaimResult, RewardUiEvent?> {
        if (studentId.isBlank()) return TrialAdClaimResult.GRANT_FAILED to null

        if (!rewardedAdManager.isReady()) {
            rewardedAdManager.preload()
            return TrialAdClaimResult.NOT_READY to null
        }

        val earned = rewardedAdManager.showForReward(activity, AdPlacement.TRIAL_DOUBLE_XP)
        if (!earned) return TrialAdClaimResult.AD_SKIPPED to null

        val bonus =
            gamificationRewardService.awardTrialDoubleXp(
                studentId = studentId,
                trialItemId = trialItemId,
                kind = kind,
                language = language,
            ) ?: return TrialAdClaimResult.GRANT_FAILED to null

        DebugLogger.debugLog(TAG, "Trial double XP ad OK — +${bonus.xpEarned} lifetime for item $trialItemId")
        return TrialAdClaimResult.SUCCESS to bonus
    }

    fun mandatoryGrantKey(studentId: String, batchIndex: Int): String =
        "trial_mandatory_${QuestDayKey.current()}_${studentId.takeLast(6)}_$batchIndex"
}
