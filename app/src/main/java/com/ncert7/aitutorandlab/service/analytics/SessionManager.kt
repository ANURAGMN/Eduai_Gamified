package com.ncert7.aitutorandlab.service.analytics

import android.content.Context
import com.ncert7.aitutorandlab.data.local.database.EduAiDatabase
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.local.entities.AppAnalyticsEntity
import com.ncert7.aitutorandlab.data.local.entities.SessionEntity
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.service.sync.DataSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Singleton manager for handling sessions and analytics tracking.
 * Provides a clean, centralized way to track app lifecycle and screen events.
 */
object SessionManager {

    private const val MAX_ORPHAN_SESSION_MS = 2L * 60 * 60 * 1000
    private const val MAX_ORPHAN_SCREEN_MS = 2L * 60 * 60 * 1000

    private lateinit var database: EduAiDatabase
    private lateinit var sharedPrefs: SharedPreferenceUtils
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Thread-safe session ID tracking
    private var currentSessionId: String? = null
    private val sessionMutex = Mutex()

    /**
     * Initialize the SessionManager
     */
    fun initialize(context: Context) {
        database = EduAiDatabase.getInstance(context)
        sharedPrefs = SharedPreferenceUtils(context)
        DebugLogger.debugLog("SessionManager", "Initialized")
    }

    /**
     * Start a new session
     */
    suspend fun startSession() = withContext(Dispatchers.IO) {
        sessionMutex.withLock {
            try {
                // Check if we already have an active session
                if (currentSessionId != null) {
                    DebugLogger.debugLog("SessionManager", "Session already active: $currentSessionId")
                    return@withContext
                }

                // Check and cleanup any old session(s) still open locally or in prefs
                val studentId = sharedPrefs.getUserId() ?: ""
                val oldSessionId = sharedPrefs.getCurrentSession()
                if (oldSessionId != null) {
                    cleanupOldSession(oldSessionId)
                }
                if (studentId.isNotBlank()) {
                    database.sessionDao()
                        .getOpenSessionsForStudent(studentId)
                        .filter { it.sessionId != oldSessionId }
                        .forEach { cleanupOldSession(it.sessionId) }
                }

                // Create new session
                val sessionId = UUID.randomUUID().toString()
                val startTime = System.currentTimeMillis()

                val session = SessionEntity(
                    sessionId = sessionId,
                    studentId = sharedPrefs.getUserId() ?: "",
                    sessionDate = getCurrentDate(),
                    sessionStartTime = startTime,
                    sessionEndTime = null,
                    durationMillis = 0,
                    appName = com.ncert7.aitutorandlab.config.AppConfig.APP_NAME,
                    isSynced = false
                )

                // Save to database first
                database.sessionDao().insertSession(session)

                // Update in-memory and SharedPrefs only after successful DB insert
                currentSessionId = sessionId
                sharedPrefs.setCurrentSession(sessionId)

                DebugLogger.debugLog("SessionManager", "Session started: $sessionId")
            } catch (e: Exception) {
                DebugLogger.debugLog("SessionManager", "Error starting session: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * End the current session - ensures all data is saved
     */
    suspend fun endSession() = withContext(Dispatchers.IO) {
        sessionMutex.withLock {
            try {
                val sessionId = currentSessionId ?: sharedPrefs.getCurrentSession()
                if (sessionId == null) {
                    DebugLogger.debugLog("SessionManager", "No active session to end")
                    return@withContext
                }

                DebugLogger.debugLog("SessionManager", "Ending session: $sessionId")

                // Close all active screens first
                closeAllActiveScreens(sessionId)

                // Update session end time
                val session = database.sessionDao().getSession(sessionId)
                if (session != null) {
                    val endTime = System.currentTimeMillis()
                    val updatedSession = session.copy(
                        sessionEndTime = endTime,
                        durationMillis = endTime - session.sessionStartTime
                    )
                    database.sessionDao().updateSession(updatedSession)

                    DebugLogger.debugLog(
                        "SessionManager",
                        "Session ended: $sessionId, Duration: ${updatedSession.durationMillis / 1000}s"
                    )
                }

                // Clear session references
                currentSessionId = null
                sharedPrefs.clearCurrentSession()

            } catch (e: Exception) {
                DebugLogger.debugLog("SessionManager", "Error ending session: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Track screen entry - async and non-blocking
     */
    suspend fun trackScreenEntry(
        screenName: ScreenName,
        conceptId: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val sessionId = getCurrentSessionId()
            if (sessionId == null) {
                DebugLogger.debugLog("SessionManager", "No session for entry: ${screenName.displayName}")
                return@withContext
            }

            val analyticsId = database.appAnalyticsDao().insertAnalytics(
                AppAnalyticsEntity(
                    sessionId = sessionId,
                    studentId = sharedPrefs.getUserId() ?: "",
                    screenName = screenName.displayName,
                    eventType = EventType.ENTRY.type,
                    entryTime = System.currentTimeMillis(),
                    exitTime = null,
                    durationMillis = 0,
                    conceptId = conceptId?.takeIf { it.isNotBlank() && it != "empty" },
                    appName = com.ncert7.aitutorandlab.config.AppConfig.APP_NAME,
                    // High-frequency: keep local, mirror to Firestore only if the flag is on.
                    isSynced = !AnalyticsFirestoreMirror.ENABLED
                )
            )
            // Screen metric goes to GA4 (not Firestore) — same insight, zero Firestore cost.
            FirebaseAnalyticsHelper.logEvent(
                eventName = "screen_view",
                screen = screenName,
                params = mapOf("concept_id" to conceptId?.takeIf { it.isNotBlank() && it != "empty" }),
            )
            if (AnalyticsFirestoreMirror.ENABLED) {
                DataSyncService.syncAnalyticsUpdate(analyticsId)
            }
            DebugLogger.debugLog("SessionManager", "Entry: ${screenName.displayName}")

        } catch (e: Exception) {
            DebugLogger.debugLog("SessionManager", "Error tracking entry: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Track screen exit event
     */
    suspend fun trackScreenExit(screenName: ScreenName) = withContext(Dispatchers.IO) {
        try {
            val sessionId = getCurrentSessionId()
            if (sessionId == null) {
                DebugLogger.debugLog("SessionManager", "No session for exit: ${screenName.displayName}")
                return@withContext
            }

            val activeAnalytics = database.appAnalyticsDao()
                .getActiveAnalyticsForScreen(sessionId, screenName.displayName)

            if (activeAnalytics != null) {
                val exitTime = System.currentTimeMillis()
                val duration = exitTime - activeAnalytics.entryTime

                database.appAnalyticsDao().updateAnalyticsExit(
                    analyticsId = activeAnalytics.analyticsId,
                    eventType = EventType.EXIT.type,
                    exitTime = exitTime,
                    durationMillis = duration
                )

                // Duration metric → GA4; Firestore only if the mirror is on.
                FirebaseAnalyticsHelper.logEvent(
                    eventName = "screen_time",
                    screen = screenName,
                    params = mapOf("duration_ms" to duration),
                )
                if (AnalyticsFirestoreMirror.ENABLED) {
                    DataSyncService.syncAnalyticsUpdate(activeAnalytics.analyticsId)
                }

                DebugLogger.debugLog(
                    "SessionManager","Exit: ${screenName.displayName}, Duration: ${duration / 1000}s"
                )
            } else {
                DebugLogger.debugLog("SessionManager", "No active entry for: ${screenName.displayName}")
            }

        } catch (e: Exception) {
            DebugLogger.debugLog("SessionManager", "Error tracking exit: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Track screen exit immediately (non-suspend version)
     * Used when called from DisposableEffect's onDispose to ensure it completes
     * even if the calling coroutine scope is cancelled
     */
    fun trackScreenExitImmediate(screenName: ScreenName) {
        scope.launch {
            trackScreenExit(screenName)
        }
    }

    /**
     * Ensures a session row exists before persisting click/ad analytics.
     */
    suspend fun ensureActiveSession() {
        if (getCurrentSessionId() == null) {
            startSession()
        }
    }

    /**
     * Re-track screen entry when the app returns to foreground (new session may have started).
     */
    fun trackScreenEntryImmediate(screenName: ScreenName, conceptId: String? = null) {
        scope.launch {
            if (getCurrentSessionId() == null) {
                startSession()
            }
            trackScreenEntry(screenName, conceptId)
        }
    }

    /**
     * Get current session ID
     */
    fun getCurrentSessionId(): String? {
        return currentSessionId ?: sharedPrefs.getCurrentSession()
    }

    //closes if any old session
    private suspend fun cleanupOldSession(oldSessionId: String) {
        try {
            DebugLogger.debugLog("SessionManager", "Cleaning up old session: $oldSessionId")

            val oldSession = database.sessionDao().getSession(oldSessionId)
            if (oldSession != null && oldSession.sessionEndTime == null) {
                val endTime = System.currentTimeMillis()
                val rawDuration = endTime - oldSession.sessionStartTime
                val duration = rawDuration.coerceAtMost(MAX_ORPHAN_SESSION_MS)
                val cappedEndTime = oldSession.sessionStartTime + duration

                if (rawDuration > MAX_ORPHAN_SESSION_MS) {
                    DebugLogger.debugLog(
                        "SessionManager",
                        "Capping orphan session ${rawDuration / 1000}s -> ${duration / 1000}s"
                    )
                }

                // Close active screens using capped end time
                closeAllActiveScreens(
                    sessionId = oldSessionId,
                    exitTime = cappedEndTime,
                    capOrphanDuration = true
                )

                val updatedSession = oldSession.copy(
                    sessionEndTime = cappedEndTime,
                    durationMillis = duration
                )
                database.sessionDao().updateSession(updatedSession)

                DebugLogger.debugLog("SessionManager", "Old session cleaned up")
            }
        } catch (e: Exception) {
            DebugLogger.debugLog("SessionManager", "Error cleaning old session: ${e.message}")
        }
    }

    private suspend fun closeAllActiveScreens(
        sessionId: String,
        exitTime: Long = System.currentTimeMillis(),
        capOrphanDuration: Boolean = false
    ) {
        try {
            val activeAnalytics = database.appAnalyticsDao()
                .getAnalyticsForSession(sessionId)
                .filter { it.exitTime == null }

            if (activeAnalytics.isNotEmpty()) {
                activeAnalytics.forEach { analytics ->
                    val rawDuration = exitTime - analytics.entryTime
                    val duration = if (capOrphanDuration) {
                        rawDuration.coerceAtMost(MAX_ORPHAN_SCREEN_MS)
                    } else {
                        rawDuration
                    }
                    val actualExitTime = analytics.entryTime + duration
                    database.appAnalyticsDao().updateAnalyticsExit(
                        analyticsId = analytics.analyticsId,
                        eventType = EventType.EXIT.type,
                        exitTime = actualExitTime,
                        durationMillis = duration
                    )
                }
                DebugLogger.debugLog("SessionManager", "Closed ${activeAnalytics.size} active screen(s)")
            }
        } catch (e: Exception) {
            DebugLogger.debugLog("SessionManager", "Error closing screens: ${e.message}")
        }
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}