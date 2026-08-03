package com.ncert7.aitutorandlab.service.logging

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.debug.DebugLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firestore Error Logger - Logs all errors to Firebase Firestore
 *
 * Features:
 * - Works in BOTH debug and release modes
 * - Logs errors silently without showing to users
 * - Automatically includes app name and user ID
 * - Stores timestamp, error message, stack trace
 * - Uses Coroutine for non-blocking operations
 * - Safe even if Firestore is unavailable (catches exceptions)
 *
 * Collection Structure:
 * /errors/{appName}/{document} = {
 *   userId: String,
 *   appName: String,
 *   errorMessage: String,
 *   errorType: String,
 *   stackTrace: String,
 *   timestamp: String (ISO 8601),
 *   severity: String (ERROR, WARNING, INFO),
 *   source: String (tag/source of error)
 * }
 */
@Singleton
class FirestoreErrorLogger @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedPrefs: SharedPreferenceUtils,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "FirestoreErrorLogger"
        private const val ERRORS_COLLECTION = "errors"
        private const val ERROR_BATCH_SIZE = 100  // Max 100 before archiving
    }

    /**
     * Log an error to Firestore
     * This works in BOTH debug and release modes
     *
     * @param tag Source tag (e.g., "ConceptViewModel")
     * @param message Error message
     * @param exception Optional exception with stack trace
     * @param severity Error severity (ERROR, WARNING, INFO)
     */
    fun logError(
        tag: String,
        message: String,
        exception: Exception? = null,
        severity: String = "ERROR"
    ) {
        // Log to Firestore in background (non-blocking)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = sharedPrefs.getUserId() ?: "anonymous"
                val appName = getAppName()
                val timestamp = getCurrentTimestamp()

                val errorData = mapOf(
                    "userId" to userId,
                    "appName" to appName,
                    "source" to tag,
                    "errorMessage" to message,
                    "errorType" to (exception?.javaClass?.simpleName ?: "Unknown"),
                    "stackTrace" to (exception?.stackTraceToString() ?: "No stack trace"),
                    "severity" to severity,
                    "timestamp" to timestamp,
                    "timestampMillis" to System.currentTimeMillis(),
                    "deviceInfo" to getDeviceInfo()
                )

                // Add to Firestore under /errors/{appName}/
                firestore
                    .collection(ERRORS_COLLECTION)
                    .document(appName)
                    .collection("logs")
                    .add(errorData)
                    .addOnSuccessListener {
                        DebugLogger.debugLog(TAG, "Error logged to Firestore: $tag - $message")
                    }
                    .addOnFailureListener { e ->
                        DebugLogger.debugLog(TAG, "Failed to log to Firestore: ${e.message}")
                    }

            } catch (e: Exception) {
                // Silent fail - don't crash the app or show errors about logging errors
                DebugLogger.debugLog(TAG, "Error logging error: ${e.message}")
            }
        }
    }

    /**
     * Log a warning to Firestore
     */
    fun logWarning(
        tag: String,
        message: String,
        exception: Exception? = null
    ) {
        logError(tag, message, exception, "WARNING")
    }

    /**
     * Log info to Firestore
     */
    fun logInfo(
        tag: String,
        message: String
    ) {
        logError(tag, message, null, "INFO")
    }

    /**
     * Get current timestamp in ISO 8601 format
     */
    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    /**
     * Get app name from context
     */
    private fun getAppName(): String = AppConfig.APP_NAME

    /**
     * Get device information
     */
    private fun getDeviceInfo(): Map<String, Any> {
        return mapOf(
            "manufacturer" to (android.os.Build.MANUFACTURER ?: "Unknown"),
            "model" to (android.os.Build.MODEL ?: "Unknown"),
            "osVersion" to android.os.Build.VERSION.SDK_INT,
            "brand" to (android.os.Build.BRAND ?: "Unknown")
        )
    }

    /**
     * Query errors for a specific user
     * Usage: Get all errors for debugging purposes (admin panel)
     */
    suspend fun getUserErrors(
        userId: String,
        appName: String,
        limit: Long = 50
    ): List<Map<String, Any>> {
        return try {
            val snapshot = firestore
                .collection(ERRORS_COLLECTION)
                .document(appName)
                .collection("logs")
                .whereEqualTo("userId", userId)
                .orderBy("timestampMillis", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            snapshot.documents.map { it.data ?: emptyMap() }
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "Error querying user errors: ${e.message}")
            emptyList()
        }
    }

    /**
     * Query errors by app name
     */
    suspend fun getAppErrors(
        appName: String,
        limit: Long = 100,
        severityFilter: String? = null
    ): List<Map<String, Any>> {
        return try {
            var query = firestore
                .collection(ERRORS_COLLECTION)
                .document(appName)
                .collection("logs")
                .orderBy("timestampMillis", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)

            if (severityFilter != null) {
                query = query.whereEqualTo("severity", severityFilter)
            }

            val snapshot = query.get().await()
            snapshot.documents.map { it.data ?: emptyMap() }
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "Error querying app errors: ${e.message}")
            emptyList()
        }
    }

    /**
     * Delete old errors (for cleanup)
     * Call this periodically to keep Firestore clean
     */
    suspend fun cleanupOldErrors(
        appName: String,
        daysOld: Int = 30
    ) {
        try {
            val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)

            val snapshot = firestore
                .collection(ERRORS_COLLECTION)
                .document(appName)
                .collection("logs")
                .whereLessThan("timestampMillis", cutoffTime)
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }
            DebugLogger.debugLog(TAG, "Cleaned up ${snapshot.size()} old errors")
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "Error cleaning up old errors: ${e.message}")
        }
    }
}

// Extension function for Tasks to support .await()
suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }
        addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }
        addOnCanceledListener {
            continuation.cancel()
        }
    }
}
