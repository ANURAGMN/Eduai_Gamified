package com.ncert7.aitutorandlab.utils

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.di.StreakEntryPoint
import com.ncert7.aitutorandlab.service.analytics.InteractionTracker
import com.ncert7.aitutorandlab.service.analytics.SessionManager
import com.ncert7.aitutorandlab.service.sync.DataSyncService
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppLifecycleObserver(
    private val app: Application,
) : DefaultLifecycleObserver {

    fun register() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        DebugLogger.debugLog("AppLifecycleObserver", "Registered")
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        DebugLogger.debugLog("AppLifecycleObserver", "App → Foreground")

        // For subsequent app returns from background, start new session + touch the daily streak.
        // Opening the app (foreground) counts as streak activity — same-day is a no-op in Room/Firestore.
        owner.lifecycleScope.launch {
            SessionManager.startSession()
            DebugLogger.debugLog("AppLifecycleObserver", "Session started on app return")
            recordStreakOnAppOpen()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        DebugLogger.debugLog("AppLifecycleObserver", "App → Background")
        owner.lifecycleScope.launch {
            InteractionTracker.endSession()
            delay(300)
            SessionManager.endSession()
            // Background is the natural once-per-session flush point. triggerFullSync() pushes the
            // whole outbox (progress, streak, sessions, chapter, simulation interactions, garden)
            // in one batch — run AFTER endSession() so the final screen-exit row is included.
            DataSyncService.triggerFullSync()
            DebugLogger.debugLog("AppLifecycleObserver", "Session ended")
        }
    }

    private suspend fun recordStreakOnAppOpen() {
        withContext(Dispatchers.IO) {
            try {
                val userId = SharedPreferenceUtils(app).getUserId()?.takeIf { it.isNotBlank() }
                    ?: return@withContext
                val streakRepository = EntryPointAccessors
                    .fromApplication(app, StreakEntryPoint::class.java)
                    .streakRepository()
                val count = streakRepository.recordActivity(userId)
                DebugLogger.debugLog("AppLifecycleObserver", "App-open streak → $count for $userId")
            } catch (e: Exception) {
                DebugLogger.errorLog("AppLifecycleObserver", "App-open streak failed: ${e.message}")
            }
        }
    }
}
