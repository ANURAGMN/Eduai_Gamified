package com.ncert7.aitutorandlab.debug

import android.content.Context
import com.ncert7.aitutorandlab.BuildConfig
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils

/** Debug helpers for replaying the first-run onboarding flow after sign-in. */
object OnboardingDebugHelper {

    fun isForceOnboardingEnabled(context: Context): Boolean {
        if (!BuildConfig.DEBUG) return false
        return SharedPreferenceUtils(context).isForceOnboardingDebugEnabled()
    }

    /** Resets onboarding state when the dev toggle is on — call right before navigating to `"main"`. */
    fun prepareOnboardingReplayAfterSignIn(context: Context) {
        if (!BuildConfig.DEBUG) return
        val prefs = SharedPreferenceUtils(context)
        if (prefs.isForceOnboardingDebugEnabled()) {
            prefs.resetOnboardingForDebugReplay()
        }
    }
}
