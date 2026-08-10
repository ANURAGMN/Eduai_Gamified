package com.ncert7.aitutorandlab.service.ads

import android.content.Context
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.debug.DebugLogger

object ClickAdGate {

    private const val TAG = "ClickAdGate"

    private lateinit var sharedPrefs: SharedPreferenceUtils

    fun initialize(context: Context) {
        sharedPrefs = SharedPreferenceUtils(context)
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
}
