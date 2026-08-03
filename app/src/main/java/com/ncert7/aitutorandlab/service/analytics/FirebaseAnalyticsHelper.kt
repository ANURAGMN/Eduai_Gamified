package com.ncert7.aitutorandlab.service.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.ncert7.aitutorandlab.config.GamificationFeatureFlags
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import com.ncert7.aitutorandlab.BuildConfig

/**
 * GA4 sink for all product analytics. High-frequency events stay here only (not Firestore).
 */
object FirebaseAnalyticsHelper {

    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var gamifiedFlag: Boolean = false

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        firebaseAnalytics = FirebaseAnalytics.getInstance(appContext)
        gamifiedFlag = GamificationFeatureFlags.isGamifiedHomeEnabled(appContext)
        applyKidsComplianceSettings()
        refreshUserProperties(appContext)
    }

    fun refreshUserProperties(context: Context) {
        gamifiedFlag = GamificationFeatureFlags.isGamifiedHomeEnabled(context.applicationContext)
        firebaseAnalytics?.setUserProperty("flag_gamified", gamifiedFlag.toString())
        firebaseAnalytics?.setUserProperty("app_version", BuildConfig.VERSION_NAME)
    }

    private fun applyKidsComplianceSettings() {
        firebaseAnalytics?.setAnalyticsCollectionEnabled(true)
        firebaseAnalytics?.setUserProperty("allow_ad_personalization_signals", "false")
    }

    fun logEvent(
        eventName: String,
        screen: ScreenName? = null,
        params: Map<String, Any?> = emptyMap(),
    ) {
        val bundle = Bundle()
        screen?.let { bundle.putString("screen", it.displayName) }
        bundle.putString("flag_gamified", gamifiedFlag.toString())
        bundle.putString("app_version", BuildConfig.VERSION_NAME)
        bundle.putString("language", getCurrentLanguageCode())
        params.forEach { (key, value) ->
            when (value) {
                null -> Unit
                is String -> bundle.putString(key, value)
                is Int -> bundle.putLong(key, value.toLong())
                is Long -> bundle.putLong(key, value)
                is Boolean -> bundle.putString(key, value.toString())
                else -> bundle.putString(key, value.toString())
            }
        }
        firebaseAnalytics?.logEvent(eventName, bundle)
    }

    fun logSimulationClick(
        conceptId: String,
        source: ClickSource,
        interaction: SimulationInteraction,
    ) {
        logEvent(
            eventName = "simulation_click",
            params =
                mapOf(
                    "item_id" to conceptId,
                    "concept_id" to conceptId,
                    "content_type" to interaction.value,
                    "interaction_type" to interaction.value,
                    "source" to source.value,
                ),
        )
    }

    fun logSimulationComplete(
        conceptId: String,
        interaction: SimulationInteraction,
    ) {
        logEvent(
            eventName = "simulation_complete",
            params =
                mapOf(
                    "item_id" to conceptId,
                    "concept_id" to conceptId,
                    "interaction_type" to interaction.value,
                ),
        )
    }

    fun logContentClick(
        itemId: String,
        contentType: ContentClickType,
        source: ClickSource,
    ) {
        logEvent(
            eventName = "content_click",
            params =
                mapOf(
                    "item_id" to itemId,
                    "content_type" to contentType.value,
                    "source" to source.value,
                ),
        )
    }

    fun logFunnelStep(step: FunnelStep) {
        logEvent(
            eventName = "funnel_step",
            params = mapOf("funnel_step" to step.value),
        )
    }

    fun logAdEvent(
        adType: AdType,
        interaction: AdInteraction,
        placement: AdPlacement,
        detail: String? = null,
    ) {
        logEvent(
            eventName = "ad_event",
            params =
                buildMap {
                    put("ad_type", adType.value)
                    put("ad_interaction", interaction.value)
                    put("ad_placement", placement.value)
                    detail?.let { put("ad_detail", it) }
                },
        )
    }

    fun logNavTab(tabRoute: String) {
        logEvent(
            eventName = "nav_tab",
            screen = ScreenName.HOME,
            params =
                mapOf(
                    "item_id" to tabRoute,
                    "interaction_type" to ContentClickType.NAV_TAB.value,
                ),
        )
    }
}
