package com.ncert7.aitutorandlab.domain.onboarding

import com.ncert7.aitutorandlab.BuildConfig
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils

/**
 * Gates the first-run onboarding UI (slides + subject/chapter/world/avatar).
 *
 * - **Upgrade:** users who completed onboarding before [MANDATORY_UI_VERSION] must replay once.
 * - **Sign-out → sign-in:** local prefs are cleared on logout; cloud hydrate no longer skips the UI.
 */
object OnboardingGate {

    /**
     * Bump when onboarding content changes and existing installs must replay.
     * Match the [BuildConfig.VERSION_CODE] of the release that shipped the new flow (1.0.11 → 13).
     */
    const val MANDATORY_UI_VERSION = 13

    fun shouldShowOnboarding(prefs: SharedPreferenceUtils): Boolean =
        !prefs.hasCompletedFirstRun()

    /**
     * Call on cold start before the UI reads [SharedPreferenceUtils.hasCompletedFirstRun].
     */
    fun ensureUpgradeReplay(prefs: SharedPreferenceUtils) {
        if (prefs.getOnboardingUiVersion() >= MANDATORY_UI_VERSION) return
        // Fresh install — user has never finished onboarding on this device.
        if (!prefs.hasCompletedFirstRun()) return
        prefs.resetOnboardingForReplay()
    }

    fun markOnboardingCompleted(prefs: SharedPreferenceUtils) {
        prefs.setOnboardingUiVersion(BuildConfig.VERSION_CODE)
    }
}
