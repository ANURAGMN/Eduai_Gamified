package com.ncert7.aitutorandlab.debug

import android.util.Log
import com.ncert7.aitutorandlab.BuildConfig
import com.ncert7.aitutorandlab.service.logging.FirestoreErrorLogger


object DebugLogger {
    // Lazy initialize FirestoreErrorLogger to avoid circular dependencies
    private var firestoreLogger: FirestoreErrorLogger? = null

    /**
     * Set the Firestore logger instance
     * Call this once in your App.onCreate() or MainActivity
     */
    fun setFirestoreLogger(logger: FirestoreErrorLogger) {
        firestoreLogger = logger
    }

    fun debugLog(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    fun errorLog(tag: String, message: String, exception: Exception? = null) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message, exception)
        }

        // IMPORTANT: Log to Firestore in BOTH debug and release modes
        // Users won't see these errors, only firebase console will have them
        firestoreLogger?.logError(tag, message, exception, "ERROR")
        com.ncert7.aitutorandlab.service.logging.CrashlyticsLogger.logError(tag, message, exception)
    }

    fun warnLog(tag: String, message: String, exception: Exception? = null) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, message, exception)
        }

        // Log warnings to Firestore in BOTH modes
        firestoreLogger?.logWarning(tag, message, exception)
        com.ncert7.aitutorandlab.service.logging.CrashlyticsLogger.logWarning(tag, message, exception)
    }

    /**
     * Log info messages to Firestore only (no logcat)
     * Useful for tracking app behavior in production
     */
    fun infoLog(tag: String, message: String) {
        firestoreLogger?.logInfo(tag, message)
    }
}