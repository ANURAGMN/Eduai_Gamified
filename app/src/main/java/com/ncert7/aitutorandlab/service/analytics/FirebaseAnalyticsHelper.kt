package com.ncert7.aitutorandlab.service.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode

/**
 * Logs GA4 / Firebase Analytics events for MVP simulation engagement metrics.
 */
object FirebaseAnalyticsHelper {

    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext)
    }

    fun logSimulationClick(
        conceptId: String,
        source: ClickSource,
        interaction: SimulationInteraction
    ) {
        logEvent(
            eventName = "simulation_click",
            itemId = conceptId,
            contentType = interaction.value,
            source = source
        )
    }

    fun logSimulationComplete(
        conceptId: String,
        interaction: SimulationInteraction
    ) {
        logEvent(
            eventName = "simulation_complete",
            itemId = conceptId,
            contentType = interaction.value,
            source = null
        )
    }

    fun logContentClick(
        itemId: String,
        contentType: ContentClickType,
        source: ClickSource
    ) {
        logEvent(
            eventName = "content_click",
            itemId = itemId,
            contentType = contentType.value,
            source = source
        )
    }

    fun logFunnelStep(step: FunnelStep) {
        val bundle = Bundle().apply {
            putString("funnel_step", step.value)
            putString("language", getCurrentLanguageCode())
        }
        firebaseAnalytics?.logEvent("funnel_step", bundle)
    }

    fun logAdEvent(
        adType: AdType,
        interaction: AdInteraction,
        placement: AdPlacement,
        detail: String? = null
    ) {
        val bundle = Bundle().apply {
            putString("ad_type", adType.value)
            putString("ad_interaction", interaction.value)
            putString("ad_placement", placement.value)
            putString("language", getCurrentLanguageCode())
            detail?.let { putString("ad_detail", it) }
        }
        firebaseAnalytics?.logEvent("ad_event", bundle)
    }

    private fun logEvent(
        eventName: String,
        itemId: String,
        contentType: String,
        source: ClickSource?
    ) {
        val bundle = Bundle().apply {
            putString("item_id", itemId)
            putString("content_type", contentType)
            putString("language", getCurrentLanguageCode())
            source?.let { putString("source", it.value) }
            if (eventName.startsWith("simulation")) {
                putString("concept_id", itemId)
                putString("interaction_type", contentType)
            }
        }
        firebaseAnalytics?.logEvent(eventName, bundle)
    }
}

