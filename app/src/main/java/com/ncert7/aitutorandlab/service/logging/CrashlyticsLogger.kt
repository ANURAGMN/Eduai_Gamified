package com.ncert7.aitutorandlab.service.logging

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.ncert7.aitutorandlab.BuildConfig

object CrashlyticsLogger {
    fun initialize() {
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true
        FirebaseCrashlytics.getInstance().setCustomKey("build_type", if (BuildConfig.DEBUG) "debug" else "release")
        FirebaseCrashlytics.getInstance().setCustomKey("version_name", BuildConfig.VERSION_NAME)
    }

    fun setUserId(studentId: String?) {
        FirebaseCrashlytics.getInstance().setUserId(studentId.orEmpty())
    }

    fun logError(tag: String, message: String, exception: Exception? = null) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.log("$tag: $message")
        exception?.let { crashlytics.recordException(it) }
    }

    fun logWarning(tag: String, message: String, exception: Exception? = null) {
        FirebaseCrashlytics.getInstance().log("WARN $tag: $message")
        exception?.let { FirebaseCrashlytics.getInstance().recordException(it) }
    }
}
