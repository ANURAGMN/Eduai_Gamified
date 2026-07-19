package com.ncert7.aitutorandlab.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.service.analytics.InteractionTracker
import com.ncert7.aitutorandlab.service.analytics.SessionManager
import com.ncert7.aitutorandlab.service.sync.DataSyncService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppLifecycleObserver : DefaultLifecycleObserver {

    fun register() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        DebugLogger.debugLog("AppLifecycleObserver", "Registered")
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        DebugLogger.debugLog("AppLifecycleObserver", "App → Foreground")

        // For subsequent app returns from background, start new session
        owner.lifecycleScope.launch {
            SessionManager.startSession()
            DebugLogger.debugLog("AppLifecycleObserver", "Session started on app return")
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        DebugLogger.debugLog("AppLifecycleObserver", "App → Background")
        owner.lifecycleScope.launch {
            InteractionTracker.endSession()
            delay(300)
            SessionManager.endSession()
            DataSyncService.syncSimulationInteractions()
            DebugLogger.debugLog("AppLifecycleObserver", "Session ended")
        }
    }
}