package com.ncert7.aitutorandlab.service.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.ncert7.aitutorandlab.BuildConfig
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.service.analytics.AdAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.AdInteraction
import com.ncert7.aitutorandlab.service.analytics.AdPlacement
import com.ncert7.aitutorandlab.service.analytics.AdType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class RewardedAdManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "RewardedAdManager"
        private const val PRELOAD_TIMEOUT_MS = 20_000L
        private const val PRELOAD_POLL_MS = 250L
        private const val SHOW_TIMEOUT_MS = 45_000L
    }

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    fun isReady(): Boolean = rewardedAd != null

    fun preload() {
        if (isLoading || rewardedAd != null) return
        val unitId = BuildConfig.REWARDED_AD_UNIT_ID
        if (unitId.isBlank()) {
            DebugLogger.errorLog(TAG, "REWARDED_AD_UNIT_ID is blank")
            return
        }

        isLoading = true
        RewardedAd.load(
            context,
            unitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isLoading = false
                    rewardedAd = ad
                    DebugLogger.debugLog(TAG, "Rewarded ad loaded")
                    AdAnalyticsTracker.track(AdType.REWARDED, AdInteraction.LOADED, AdPlacement.QUEST_CLAIM)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    rewardedAd = null
                    DebugLogger.errorLog(TAG, "Rewarded ad load failed: ${error.message}")
                    AdAnalyticsTracker.track(
                        AdType.REWARDED,
                        AdInteraction.FAILED,
                        AdPlacement.QUEST_CLAIM,
                        detail = error.message,
                    )
                }
            },
        )
    }

    /** Blocks until an ad is ready or [timeoutMs] elapses. */
    suspend fun awaitReady(timeoutMs: Long = PRELOAD_TIMEOUT_MS): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!isReady()) {
            if (!isLoading) preload()
            if (System.currentTimeMillis() >= deadline) {
                DebugLogger.errorLog(TAG, "Timed out waiting for rewarded ad")
                return false
            }
            delay(PRELOAD_POLL_MS)
        }
        return true
    }

    suspend fun showForReward(
        activity: Activity,
        placement: AdPlacement,
    ): Boolean =
        withTimeoutOrNull(SHOW_TIMEOUT_MS) {
            showForRewardNow(activity, placement)
        } ?: false.also {
            DebugLogger.errorLog(TAG, "Rewarded ad show timed out")
            AdAnalyticsTracker.track(AdType.REWARDED, AdInteraction.FAILED, placement, detail = "timeout")
        }

    private suspend fun showForRewardNow(
        activity: Activity,
        placement: AdPlacement,
    ): Boolean =
        suspendCancellableCoroutine { continuation ->
            val ad = rewardedAd
            if (ad == null) {
                AdAnalyticsTracker.track(AdType.REWARDED, AdInteraction.NOT_READY, placement)
                preload()
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            var userEarned = false
            ad.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        rewardedAd = null
                        preload()
                        AdAnalyticsTracker.track(AdType.REWARDED, AdInteraction.CLOSED, placement)
                        if (continuation.isActive) {
                            continuation.resume(userEarned)
                        }
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        rewardedAd = null
                        preload()
                        AdAnalyticsTracker.track(
                            AdType.REWARDED,
                            AdInteraction.FAILED,
                            placement,
                            detail = error.message,
                        )
                        if (continuation.isActive) {
                            continuation.resume(false)
                        }
                    }

                    override fun onAdShowedFullScreenContent() {
                        AdAnalyticsTracker.track(AdType.REWARDED, AdInteraction.SHOWN, placement)
                    }
                }

            ad.show(activity) { _ ->
                userEarned = true
                AdAnalyticsTracker.track(AdType.REWARDED, AdInteraction.REWARD_EARNED, placement)
            }
        }

    suspend fun showRewardedSequence(
        activity: Activity,
        totalAds: Int,
        placement: AdPlacement,
    ): Boolean {
        if (totalAds <= 0) return true
        if (!awaitReady()) return false

        repeat(totalAds) { index ->
            if (index > 0 && !awaitReady()) {
                DebugLogger.errorLog(TAG, "Second rewarded ad not ready after first ad")
                return false
            }
            if (!showForReward(activity, placement)) return false
        }
        return true
    }
}
