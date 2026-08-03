package com.ncert7.aitutorandlab.config

import android.content.Context
import com.ncert7.aitutorandlab.BuildConfig
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils

object GamificationFeatureFlags {

    fun isGamifiedHomeEnabled(context: Context): Boolean {
        if (BuildConfig.GAMIFIED_HOME_ENABLED) return true
        val prefs = SharedPreferenceUtils(context)
        return BuildConfig.DEBUG &&
            prefs.hasGamifiedHomeDebugOverride() &&
            prefs.isGamifiedHomeDebugEnabled()
    }

    fun isNativeTutorAvatarEnabled(context: Context): Boolean {
        if (BuildConfig.NATIVE_TUTOR_AVATAR_ENABLED || BuildConfig.GAMIFIED_HOME_ENABLED) return true
        val prefs = SharedPreferenceUtils(context)
        return BuildConfig.DEBUG &&
            prefs.hasNativeTutorAvatarDebugOverride() &&
            prefs.isNativeTutorAvatarDebugEnabled()
    }

    fun isGardenEnabled(context: Context): Boolean {
        if (BuildConfig.GARDEN_ENABLED) return true
        val prefs = SharedPreferenceUtils(context)
        return BuildConfig.DEBUG &&
            prefs.hasGardenDebugOverride() &&
            prefs.isGardenDebugEnabled()
    }
}
