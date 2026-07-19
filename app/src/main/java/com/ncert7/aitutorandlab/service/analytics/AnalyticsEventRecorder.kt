package com.ncert7.aitutorandlab.service.analytics

import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.local.database.EduAiDatabase
import com.ncert7.aitutorandlab.data.local.entities.AppAnalyticsEntity
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.service.sync.DataSyncService

/**
 * Persists click events to local DB and triggers real-time Firestore sync.
 */
object AnalyticsEventRecorder {

    private const val TAG = "AnalyticsEventRecorder"

    private lateinit var database: EduAiDatabase
    private lateinit var sharedPrefs: SharedPreferenceUtils

    fun initialize(context: android.content.Context) {
        database = EduAiDatabase.getInstance(context)
        sharedPrefs = SharedPreferenceUtils(context)
    }

    suspend fun recordClick(
        screenName: String,
        itemId: String,
        source: String?,
        interactionType: String,
        eventType: String = EventType.CLICK.type
    ) {
        if (itemId.isBlank()) return
        try {
            val sessionId = SessionManager.getCurrentSessionId()
            if (sessionId == null) {
                DebugLogger.debugLog(TAG, "No active session — skipping $eventType for $itemId")
                return
            }

            val analyticsId = database.appAnalyticsDao().insertAnalytics(
                AppAnalyticsEntity(
                    sessionId = sessionId,
                    studentId = sharedPrefs.getUserId().orEmpty(),
                    screenName = screenName,
                    eventType = eventType,
                    entryTime = System.currentTimeMillis(),
                    conceptId = itemId,
                    source = source,
                    interactionType = interactionType,
                    appName = AppConfig.APP_NAME,
                    isSynced = false
                )
            )
            DataSyncService.syncAnalyticsUpdate(analyticsId)
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "Failed to record $eventType: ${e.message}")
        }
    }

    suspend fun recordFunnelStep(
        screenName: String,
        step: FunnelStep
    ) {
        recordClick(
            screenName = screenName,
            itemId = step.value,
            source = null,
            interactionType = step.value,
            eventType = EventType.FUNNEL.type
        )
    }

    suspend fun recordAdEvent(
        adType: AdType,
        interaction: AdInteraction,
        placement: AdPlacement,
        detail: String? = null
    ) {
        try {
            val sessionId = SessionManager.getCurrentSessionId()
            if (sessionId == null) {
                DebugLogger.debugLog(TAG, "No active session — skipping ad ${interaction.value}")
                return
            }

            val analyticsId = database.appAnalyticsDao().insertAnalytics(
                AppAnalyticsEntity(
                    sessionId = sessionId,
                    studentId = sharedPrefs.getUserId().orEmpty(),
                    screenName = "AD",
                    eventType = EventType.AD.type,
                    entryTime = System.currentTimeMillis(),
                    conceptId = adType.value,
                    source = placement.value,
                    interactionType = interaction.value +
                        (detail?.let { "|$it" }.orEmpty()),
                    appName = AppConfig.APP_NAME,
                    isSynced = false
                )
            )
            DataSyncService.syncAnalyticsUpdate(analyticsId)
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "Failed to record ad event: ${e.message}")
        }
    }
}
