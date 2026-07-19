package com.ncert7.aitutorandlab.service.analytics

import android.content.Context
import com.ncert7.aitutorandlab.debug.DebugLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Tracks AdMob banner impressions, clicks, and load outcomes to Firestore + GA4.
 */
object AdAnalyticsTracker {

    private const val TAG = "AdAnalytics"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initialize(context: Context) {
        AnalyticsEventRecorder.initialize(context)
        FirebaseAnalyticsHelper.initialize(context)
    }

    fun track(
        adType: AdType,
        interaction: AdInteraction,
        placement: AdPlacement,
        detail: String? = null
    ) {
        scope.launch {
            trackAndWait(adType, interaction, placement, detail)
        }
    }

    suspend fun trackAndWait(
        adType: AdType,
        interaction: AdInteraction,
        placement: AdPlacement,
        detail: String? = null
    ) {
        AnalyticsEventRecorder.recordAdEvent(
            adType = adType,
            interaction = interaction,
            placement = placement,
            detail = detail
        )
        FirebaseAnalyticsHelper.logAdEvent(adType, interaction, placement, detail)
        DebugLogger.debugLog(
            TAG,
            "${interaction.value}: type=${adType.value}, placement=${placement.value}" +
                (detail?.let { ", detail=$it" } ?: "")
        )
    }
}
