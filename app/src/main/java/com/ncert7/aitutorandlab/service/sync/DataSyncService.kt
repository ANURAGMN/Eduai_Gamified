package com.ncert7.aitutorandlab.service.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.local.database.EduAiDatabase
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.utils.NetworkConnectivityObserver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

/**
 * Service to manage data synchronization between local Room database and Firestore.
 * Handles real-time triggers and monitors network for offline-to-online sync.
 */
object DataSyncService {
    private const val TAG = "DataSyncService"

    // Stable WorkManager unique names (so KEEP dedupes) + debounce window for coalesced uploads.
    private const val BACKGROUND_SYNC_WORK = "DATA_SYNC_WORK"
    private const val DEFERRED_UPLOAD_WORK = "firestore_deferred_upload"
    private const val IMMEDIATE_UPLOAD_WORK = "firestore_immediate_upload"
    private const val DEFERRED_UPLOAD_DELAY_MIN = 5L

    // RV.2: cap the login restore sequence so a hung network call can't block the Home gate forever.
    private const val RESTORE_TIMEOUT_MS = 20_000L
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(supervisorJob + Dispatchers.IO)

    private var isInitialized = false
    private lateinit var applicationContext: Context  // Use app context, not activity context
    private lateinit var database: EduAiDatabase
    private lateinit var sharedPref: SharedPreferenceUtils
    private var syncManager: ProgressAnalyticsSessionSyncManager? = null
    private var simulationSyncManager: SimulationSyncManager? = null
    private var connectivityObserver: NetworkConnectivityObserver? = null
    private var networkListenerJob: Job? = null  // Track the network listener job

    // Garden/space reward mirror — lazy so it's created after [database] is set in initialize().
    private val gardenSyncManager: GardenSyncManager by lazy {
        GardenSyncManager(database.gardenDao(), com.ncert7.aitutorandlab.repository.FirebaseRepository())
    }

    private val gamificationSyncManager: GamificationSyncManager by lazy {
        GamificationSyncManager(
            database.gamificationDao(),
            com.ncert7.aitutorandlab.repository.FirebaseRepository(),
        )
    }

    private val examPlanSyncManager: ExamPlanSyncManager by lazy {
        ExamPlanSyncManager(
            database.examPlanDao(),
            com.ncert7.aitutorandlab.repository.FirebaseRepository(),
        )
    }

    private val questSyncManager: QuestSyncManager by lazy {
        QuestSyncManager(
            database.questDailyDao(),
            com.ncert7.aitutorandlab.repository.FirebaseRepository(),
        )
    }

    /** Completed when the in-flight login restore finishes (or immediately if none is running). */
    @Volatile
    private var gardenRestoreGate: CompletableDeferred<Unit> =
        CompletableDeferred<Unit>().also { it.complete(Unit) }

    /**
     * True when the latest [onUserAuthenticated] garden restore applied remote state/items.
     * Used so onboarding does not overwrite a restored remote theme when plant count is still 0 (R.1).
     */
    @Volatile
    private var gardenRestoredFromRemote: Boolean = false

    /** Blocks until [onUserAuthenticated]'s garden restore completes — avoids clobbering remote data. */
    suspend fun awaitGardenRestore() {
        if (!isInitialized) return
        gardenRestoreGate.await()
    }

    /** Whether login restore wrote remote garden into Room (theme/items). Resets each auth. */
    fun wasGardenRestoredFromRemote(): Boolean = gardenRestoredFromRemote

    /**
     * Initializes the DataSyncService
     * Must be called once from Application.onCreate() with application context
     */
    fun initialize(context: Context) {
        if (isInitialized) return

        synchronized(this) {
            if (isInitialized) return

            // Store application context, not activity context
            this.applicationContext = context.applicationContext
            this.database = EduAiDatabase.getInstance(applicationContext)
            this.sharedPref = SharedPreferenceUtils(applicationContext)

            // Initialize network observer
            connectivityObserver = NetworkConnectivityObserver.getInstance(applicationContext)
            connectivityObserver?.register()

            // Get current student ID and initialize sync manager
            val currentStudentId = sharedPref.getUserId()
            if (!currentStudentId.isNullOrBlank()) {
                updateStudentId(currentStudentId)
            }

            // Listen for network changes
            listenToNetworkChanges()

            isInitialized = true
            DebugLogger.debugLog(TAG, " DataSyncService initialized with studentId: $currentStudentId")
        }
    }

    /**
     * Syncs a single progress update immediately (real-time)
     * Falls back to offline queue if network is unavailable
     */
    fun syncProgressUpdate(progressId: Long, studentId: String) {
        scope.launch {
            try {
                if (syncManager == null) {
                    updateStudentId(studentId)
                }

                if (NetworkConnectivityObserver.isOnline(applicationContext)) {
                    DebugLogger.debugLog(TAG, " Real-time progress sync starting: $progressId")
                    try {
                        syncManager?.syncProgressUpdate(progressId, studentId)
                        DebugLogger.debugLog(TAG, " Real-time progress sync completed: $progressId")
                    } catch (e: Exception) {
                        DebugLogger.errorLog(TAG, " Real-time progress sync failed: ${e.message}")
                        scheduleBackgroundSync()
                    }
                } else {
                    DebugLogger.debugLog(TAG, " Device offline, progress update queued: $progressId")
                    scheduleBackgroundSync()
                }
            } catch (e: Exception) {
                DebugLogger.errorLog(TAG, " Progress sync error: ${e.message}")
                scheduleBackgroundSync()
            }
        }
    }

    /**
     * Syncs a single analytics update immediately (real-time)
     * Falls back to offline queue if network is unavailable
     */
    fun syncAnalyticsUpdate(analyticsId: Long) {
        scope.launch {
            try {
                if (syncManager == null) {
                    val studentId = sharedPref.getUserId() ?: return@launch
                    updateStudentId(studentId)
                }

                if (NetworkConnectivityObserver.isOnline(applicationContext)) {
                    DebugLogger.debugLog(TAG, " Real-time analytics sync starting: $analyticsId")
                    try {
                        syncManager?.syncAnalyticsUpdate(analyticsId)
                        DebugLogger.debugLog(TAG, " Real-time analytics sync completed: $analyticsId")
                    } catch (e: Exception) {
                        DebugLogger.errorLog(TAG, " Real-time analytics sync failed: ${e.message}")
                        scheduleBackgroundSync()
                    }
                } else {
                    DebugLogger.debugLog(TAG, " Device offline, analytics update queued: $analyticsId")
                    scheduleBackgroundSync()
                }
            } catch (e: Exception) {
                DebugLogger.errorLog(TAG, " Analytics sync error: ${e.message}")
                scheduleBackgroundSync()
            }
        }
    }

    /**
     * Syncs a single session update immediately (real-time)
     * Falls back to offline queue if network is unavailable
     */
    fun syncSessionUpdate(sessionId: String) {
        scope.launch {
            try {
                if (syncManager == null) {
                    val studentId = sharedPref.getUserId() ?: return@launch
                    updateStudentId(studentId)
                }

                // Immediately attempt sync (online or offline, let WorkManager handle it)
                DebugLogger.debugLog(TAG, "Syncing session: $sessionId")
                try {
                    syncManager?.syncSessionUpdate(sessionId)
                    DebugLogger.debugLog(TAG, "Session sync completed: $sessionId")
                } catch (e: Exception) {
                    DebugLogger.errorLog(TAG, " Session sync failed, queuing for retry: ${e.message}")
                    scheduleBackgroundSync()
                }
            } catch (e: Exception) {
                DebugLogger.errorLog(TAG, "Session sync error: ${e.message}")
                scheduleBackgroundSync()
            }
        }
    }

    /**
     * Triggers a manual full sync of all unsynced data
     */
    fun triggerFullSync() {
        scope.launch {
            try {
                if (NetworkConnectivityObserver.isOnline(applicationContext)) {
                    DebugLogger.debugLog(TAG, " Triggering full sync...")
                    val result = syncManager?.syncAllUnsyncedData()
                    if (result != null) {
                        if (result.success) {
                            DebugLogger.debugLog(TAG, " Full sync completed:\n${result.message}")
                        } else {
                            DebugLogger.errorLog(TAG, " Full sync failed:\n${result.message}")
                        }
                    }
                    syncSimulationInteractionsInternal()
                    sharedPref.getUserId()?.takeIf { it.isNotBlank() }?.let { uid ->
                        gardenSyncManager.pushGarden(uid)
                        gamificationSyncManager.pushProfile(uid)
                        examPlanSyncManager.pushPlan(uid)
                        questSyncManager.pushTodayQuest(uid)
                    }
                    gamificationSyncManager.pushProfiles()
                    examPlanSyncManager.pushPlans()
                } else {
                    DebugLogger.debugLog(TAG, " Device offline, scheduling background sync")
                    scheduleBackgroundSync()
                }
            } catch (e: Exception) {
                DebugLogger.errorLog(TAG, " Full sync error: ${e.message}")
                scheduleBackgroundSync()
            }
        }
    }

    fun syncSimulationInteractions() {
        scope.launch {
            syncSimulationInteractionsInternal()
        }
    }

    private suspend fun syncSimulationInteractionsInternal() {
        try {
            if (simulationSyncManager == null) {
                simulationSyncManager = SimulationSyncManager(
                    interactionDao = database.simulationInteractionDao(),
                    sharedPreferenceUtils = sharedPref
                )
            }

            val result = simulationSyncManager?.syncTodayIfNeeded()
            if (result?.success == true) {
                DebugLogger.debugLog(TAG, "Simulation interaction sync: ${result.message}")
            } else if (result != null) {
                DebugLogger.errorLog(TAG, "Simulation interaction sync failed: ${result.message}")
                scheduleBackgroundSync()
            }
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "Simulation interaction sync error: ${e.message}")
            scheduleBackgroundSync()
        }
    }

    // ==================== PRIVATE METHODS ====================

    /**
     * Listens to network connectivity changes and triggers sync when online
     * Job is tracked for proper cleanup
     */
    private fun listenToNetworkChanges() {
        // Cancel previous job if exists
        networkListenerJob?.cancel()

        networkListenerJob = scope.launch {
            try {
                connectivityObserver?.isOnline?.collectLatest { isOnline ->
                    if (isOnline) {
                        DebugLogger.debugLog(TAG, " Device came online, checking for unsynced data...")
                        triggerFullSync()
                    } else {
                        DebugLogger.debugLog(TAG, " Device went offline")
                    }
                }
            } catch (e: Exception) {
                DebugLogger.errorLog(TAG, " Network listener error: ${e.message}")
            }
        }
    }

    /**
     * Schedules a background sync task using WorkManager
     */
    private fun scheduleBackgroundSync() {
        try {
            val syncRequest = OneTimeWorkRequestBuilder<DataSyncWorker>()
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1,
                    TimeUnit.MINUTES
                )
                .build()

            // Stable unique name so ExistingWorkPolicy.KEEP actually dedupes (a timestamped
            // name would defeat KEEP and let workers pile up).
            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                BACKGROUND_SYNC_WORK,
                ExistingWorkPolicy.KEEP,
                syncRequest
            )

            DebugLogger.debugLog(TAG, " Background sync scheduled with WorkManager")
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, " Failed to schedule background sync: ${e.message}")
        }
    }

    /**
     * Coalesced, deferred upload of the outbox. Schedules a SINGLE delayed worker for the
     * debounce window; rapid dirty events collapse into one batched flush via
     * [ExistingWorkPolicy.KEEP]. Use this from hot paths instead of [triggerFullSync].
     */
    fun scheduleDeferredUpload() {
        if (!isInitialized) return
        try {
            val request = OneTimeWorkRequestBuilder<DataSyncWorker>()
                .setInitialDelay(DEFERRED_UPLOAD_DELAY_MIN, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                DEFERRED_UPLOAD_WORK,
                ExistingWorkPolicy.KEEP,
                request
            )
            DebugLogger.debugLog(TAG, "Deferred upload scheduled (coalesced, ${DEFERRED_UPLOAD_DELAY_MIN}m window)")
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "Failed to schedule deferred upload: ${e.message}")
        }
    }

    /**
     * RV.1: immediate (no-delay) upload for critical events — e.g. a quest claim — where the
     * [scheduleDeferredUpload] debounce window would risk a reinstall double-grant (claim flag not
     * yet uploaded, local gem-grant key wiped, so heal can't fire on the new device). Runs under its
     * own unique name with REPLACE so it fires now and is not dedup-blocked by a pending deferred
     * upload; the worker's network constraint + retry keep it durable across process death.
     */
    fun scheduleImmediateUpload() {
        if (!isInitialized) return
        try {
            val request = OneTimeWorkRequestBuilder<DataSyncWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                IMMEDIATE_UPLOAD_WORK,
                ExistingWorkPolicy.REPLACE,
                request
            )
            DebugLogger.debugLog(TAG, "Immediate upload scheduled (no delay)")
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "Failed to schedule immediate upload: ${e.message}")
        }
    }

    /**
     * Updates the sync manager with new student ID
     * Call this when user logs in
     */
    fun updateStudentId(studentId: String) {
        syncManager = ProgressAnalyticsSessionSyncManager(
            progressDao = database.progressDao(),
            analyticsDao = database.appAnalyticsDao(),
            sessionDao = database.sessionDao(),
            studentId = studentId,
            firestore =FirebaseFirestore.getInstance() ,
            streakDao = database.streakDao(),
            chapterProgressDao = database.chapterAgentProgressDao()
        )

        DebugLogger.debugLog(TAG, "👤 Student ID updated: $studentId")
    }

    /**
     * Call after sign-in: backfill pre-login analytics/sessions, then sync everything.
     */
    fun onUserAuthenticated(studentId: String) {
        val restoreGate = CompletableDeferred<Unit>()
        gardenRestoreGate = restoreGate
        gardenRestoredFromRemote = false
        scope.launch {
            try {
                // RV.2: bound the whole restore sequence. A network *hang* (not an exception) would
                // otherwise never reach `finally`, leaving the Home gate closed forever. On timeout
                // we fall through, log, and still release the gate below so Home always renders.
                val completed = withTimeoutOrNull(RESTORE_TIMEOUT_MS) {
                    database.appAnalyticsDao().backfillEmptyStudentId(studentId)
                    database.sessionDao().backfillEmptyStudentId(studentId)
                    updateStudentId(studentId)
                    val outcome = gardenSyncManager.restoreGarden(studentId)
                    gardenRestoredFromRemote = outcome == GardenRestorePolicy.Outcome.APPLIED
                    gamificationSyncManager.restoreProfile(studentId)
                    examPlanSyncManager.restorePlan(studentId)
                    questSyncManager.restoreTodayQuest(studentId)
                    triggerFullSync()
                    outcome
                }
                if (completed != null) {
                    DebugLogger.debugLog(
                        TAG,
                        "User authenticated — garden restore=$completed funnel synced for $studentId",
                    )
                } else {
                    // Ensure identity is set even if restore timed out mid-flight.
                    updateStudentId(studentId)
                    DebugLogger.errorLog(
                        TAG,
                        "onUserAuthenticated restore timed out after ${RESTORE_TIMEOUT_MS}ms; releasing Home gate",
                    )
                }
            } catch (e: Exception) {
                DebugLogger.errorLog(TAG, "onUserAuthenticated failed: ${e.message}")
                updateStudentId(studentId)
            } finally {
                restoreGate.complete(Unit)
            }
        }
    }

    /**
     * Cleanup - call this from Application.onTerminate()
     * DO NOT call from Activity.onDestroy() - this is a singleton
     */
    fun shutdown() {
        try {
            // Cancel the network listener job
            networkListenerJob?.cancel()
            networkListenerJob = null

            // Properly destroy observer to allow GC and remove context reference
            connectivityObserver?.destroy()
            connectivityObserver = null

            // Cancel all coroutines
            supervisorJob.cancel()

            // Clear references
            syncManager = null

            isInitialized = false
            DebugLogger.debugLog(TAG, " DataSyncService shutdown complete")
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, " Error during shutdown: ${e.message}")
        }
    }
}
