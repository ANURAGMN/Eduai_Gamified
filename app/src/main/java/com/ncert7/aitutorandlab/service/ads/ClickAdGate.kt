package com.ncert7.aitutorandlab.service.ads

import android.content.Context
import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.local.database.EduAiDatabase
import com.ncert7.aitutorandlab.debug.DebugLogger
import java.time.LocalDate
import java.time.ZoneId

object ClickAdGate {

    private const val TAG = "ClickAdGate"

    private lateinit var database: EduAiDatabase
    private lateinit var sharedPrefs: SharedPreferenceUtils

    fun initialize(context: Context) {
        database = EduAiDatabase.getInstance(context)
        sharedPrefs = SharedPreferenceUtils(context)
    }

    suspend fun getTodayClickCount(): Int {
        val studentId = sharedPrefs.getUserId().orEmpty()
        if (studentId.isEmpty()) return 0
        val (startOfDay, endOfDay) = todayBounds()
        return database.appAnalyticsDao().getTodayClickCount(
            studentId = studentId,
            startOfDay = startOfDay,
            endOfDay = endOfDay,
            appName = AppConfig.APP_NAME
        )
    }

    /** Called for each in-simulation interaction (tap/slider/input). Accumulates toward the next ad. */
    fun recordSimInteraction() {
        if (!::sharedPrefs.isInitialized) return
        sharedPrefs.addSimInteractionsSinceAd(1)
    }

    /** Ad cadence is engagement-based: fire once the learner has done enough in-sim interactions. */
    fun shouldShowAdBeforeNextClick(): Boolean {
        if (!::sharedPrefs.isInitialized) return false
        val count = sharedPrefs.getSimInteractionsSinceAd()
        val show = ClickAdPolicy.shouldShowAd(count)
        DebugLogger.debugLog(TAG, "Sim interactions since last ad: $count, showAd=$show")
        return show
    }

    /** Reset the interaction counter once an ad has actually been shown. */
    fun consumeAd() {
        if (!::sharedPrefs.isInitialized) return
        sharedPrefs.resetSimInteractionsSinceAd()
    }

    private fun todayBounds(): Pair<Long, Long> {
        val startOfDay = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val endOfDay = LocalDate.now()
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli() - 1
        return startOfDay to endOfDay
    }
}
