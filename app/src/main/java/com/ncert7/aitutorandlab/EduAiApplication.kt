package com.ncert7.aitutorandlab

import android.app.Application
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.local.database.EduAiDatabase
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.service.analytics.ContentClickAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.AdAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.FunnelAnalyticsTracker
import com.ncert7.aitutorandlab.repository.SimulationInteractionRepository
import com.ncert7.aitutorandlab.service.analytics.InteractionTracker
import com.ncert7.aitutorandlab.service.analytics.GamificationAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.SessionManager
import com.ncert7.aitutorandlab.service.ads.ClickAdGate
import com.ncert7.aitutorandlab.service.analytics.SimulationAnalyticsTracker
import com.ncert7.aitutorandlab.service.logging.CrashlyticsLogger
import com.ncert7.aitutorandlab.service.sync.DataSyncService
import com.ncert7.aitutorandlab.service.sync.WeeklySyncWorker
import com.ncert7.aitutorandlab.notification.NotificationChannels
import com.ncert7.aitutorandlab.notification.NotificationScheduler
import com.ncert7.aitutorandlab.utils.AppLifecycleObserver
import com.ncert7.aitutorandlab.utils.LanguageHelper
import com.ncert7.aitutorandlab.utils.bindStoredLanguagePreference
import com.ncert7.aitutorandlab.utils.normalizeLanguageCode
import dagger.hilt.android.HiltAndroidApp
import com.ncert7.aitutorandlab.di.AppDataMigrationEntryPoint
import com.ncert7.aitutorandlab.di.TutorConfigEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class EduAiApplication : Application(), Configuration.Provider {

    private lateinit var appLifecycleObserver: AppLifecycleObserver
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

    override fun onCreate() {
        super.onCreate()
        CrashlyticsLogger.initialize()
        DebugLogger.debugLog("EduAiApplication", "Application onCreate")

        // Initialize language preference from SharedPreferences
        initializeLanguage()

        // Initialize DataSyncService for real-time and offline sync
        DataSyncService.initialize(this)

        runAppDataMigrations()
        loadTutorConfig()

        // Initialize SessionManager (handles both sessions and analytics)
        SessionManager.initialize(this)
        GamificationAnalyticsTracker.initialize(this)
        SimulationAnalyticsTracker.initialize(this)
        ContentClickAnalyticsTracker.initialize(this)
        AdAnalyticsTracker.initialize(this)
        FunnelAnalyticsTracker.initialize(this)
        ClickAdGate.initialize(this)

        NotificationChannels.ensureCreated(this)

        val database = EduAiDatabase.getInstance(this)
        val sharedPref = SharedPreferenceUtils(this)
        NotificationScheduler.scheduleAll(this)
        InteractionTracker.initialize(
            SimulationInteractionRepository(
                interactionDao = database.simulationInteractionDao(),
                sharedPreferenceUtils = sharedPref
            )
        )

        // Register app lifecycle observer (handles session start/end on foreground/background)
        appLifecycleObserver = AppLifecycleObserver()
        appLifecycleObserver.register()

        // Cold start: ProcessLifecycleOwner may already be STARTed before the observer registers
        applicationScope.launch {
            SessionManager.startSession()
            DebugLogger.debugLog("EduAiApplication", "Initial session started on app launch")
        }

        scheduleDailySync()
    }

    private fun loadTutorConfig() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                val userId = SharedPreferenceUtils(this@EduAiApplication).getUserId() ?: return@launch
                val repo =
                    EntryPointAccessors
                        .fromApplication(this@EduAiApplication, TutorConfigEntryPoint::class.java)
                        .tutorConfigRepository()
                repo.ensureLoaded(this@EduAiApplication, userId)
            } catch (e: Exception) {
                DebugLogger.errorLog("EduAiApplication", "Tutor config load failed: ${e.message}")
            }
        }
    }

    private fun runAppDataMigrations() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                val prefs = SharedPreferenceUtils(this@EduAiApplication)
                val userId = prefs.getUserId() ?: return@launch
                val language = normalizeLanguageCode(prefs.getLanguagePreference())
                val runner =
                    EntryPointAccessors
                        .fromApplication(
                            this@EduAiApplication,
                            AppDataMigrationEntryPoint::class.java,
                        ).appDataMigrationRunner()
                runner.runPendingMigrations(userId, language)
            } catch (e: Exception) {
                DebugLogger.errorLog(
                    "EduAiApplication",
                    "App data migration failed: ${e.message}",
                )
            }
        }
    }

    private fun initializeLanguage() {
        try {
            val sharedPref = SharedPreferenceUtils(this)
            bindStoredLanguagePreference { sharedPref.getLanguagePreference() }
            val savedLanguage = sharedPref.getLanguagePreference() ?: "en"
            LanguageHelper.setLanguage(savedLanguage)
            DebugLogger.debugLog("EduAiApplication", "Language initialized to: $savedLanguage")
        } catch (e: Exception) {
            DebugLogger.debugLog("EduAiApplication", "Error initializing language: ${e.message}")
        }
    }

    private fun scheduleDailySync() {
        val request =
            PeriodicWorkRequestBuilder<WeeklySyncWorker>(
                1, TimeUnit.DAYS
            )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1, // Initial delay
                    TimeUnit.MINUTES
                )
                .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "DAILY_SYNC_WORK",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )

        DebugLogger.debugLog("EduAiApplication", "Daily sync worker scheduled with exponential backoff retry ")
    }

    /**
     * Cleanup resources when app is terminated
     */
    override fun onTerminate() {
        super.onTerminate()
        try {
            // Shutdown DataSyncService
            // DataSyncService.shutdown() // Not strictly needed for object, but following structure
            // Unregister app lifecycle observer
            // appLifecycleObserver.unregister() // AppLifecycleObserver.kt currently doesn't have unregister, only register
            DebugLogger.debugLog("EduAiApplication", " Application terminated and cleaned up")
        } catch (e: Exception) {
            DebugLogger.errorLog("EduAiApplication", " Error during cleanup: ${e.message}")
        }
    }
}