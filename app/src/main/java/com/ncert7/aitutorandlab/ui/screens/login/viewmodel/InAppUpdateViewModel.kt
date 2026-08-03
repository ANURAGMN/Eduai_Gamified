package com.ncert7.aitutorandlab.ui.screens.login.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncert7.aitutorandlab.service.update.GooglePlayUpdateManager
import com.ncert7.aitutorandlab.service.update.UpdateCallback
import com.ncert7.aitutorandlab.service.update.UpdateType
import com.google.android.play.core.appupdate.AppUpdateInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State for in-app update UI
 * Tracks whether an update is available, in progress, or failed
 */
data class UpdateState(
    val updateAvailable: Boolean = false,
    val appUpdateInfo: AppUpdateInfo? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val error: String? = null,
    val updateInstalled: Boolean = false
)

/**
 * ViewModel for managing in-app updates using Google Play API
 *
 * Responsibilities:
 * - Checking for available updates from Play Store
 * - Triggering the native Google in-app update UI
 * - Tracking download progress and state
 * - Following MVVM architecture best practices
 *
 * Uses:
 * - GooglePlayUpdateManager: Handles actual Play Store API calls
 * - UpdateCallback: Receives state changes from the update manager
 * - StateFlow: Exposes UI state for Compose to observe
 */
@HiltViewModel
class InAppUpdateViewModel @Inject constructor() : ViewModel(), UpdateCallback {

    companion object {
        private const val TAG = "InAppUpdateViewModel"
    }

    // Real Google Play update manager (no fake implementation)
    private val updateManager: GooglePlayUpdateManager = GooglePlayUpdateManager()

    private var updateFlowStarted = false

    // UI State - exposed as read-only StateFlow
    private val _updateState = MutableStateFlow(UpdateState())
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    init {
        Log.d(TAG, "InAppUpdateViewModel initialized with GooglePlayUpdateManager")
        updateManager.setUpdateCallback(this)
    }

    /**
     * Check for available updates from Play Store
     * This is called when the login screen is displayed
     *
     * If an update is available, Google's native in-app update UI will be triggered automatically
     */
    fun checkForUpdate(activity: Activity) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting update check from Play Store")
                updateManager.checkForUpdate(activity)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for update: ${e.message}", e)
                _updateState.value = _updateState.value.copy(
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun checkResumeState(activity: Activity) {
        viewModelScope.launch {
            try {
                updateManager.checkResumeState(activity)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking resume update state: ${e.message}", e)
            }
        }
    }

    /**
     * Start the in-app update flow
     * This shows Google's native UI for the user to confirm the update
     */
    fun startUpdate(
        activity: Activity,
        updateType: UpdateType = UpdateType.FLEXIBLE
    ) {
        if (updateFlowStarted) return
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting in-app update with type: $updateType")
                updateFlowStarted = true
                _updateState.value = _updateState.value.copy(isDownloading = true)
                updateManager.startUpdate(activity, updateType)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting update: ${e.message}", e)
                updateFlowStarted = false
                _updateState.value = _updateState.value.copy(
                    error = e.message ?: "Unknown error occurred",
                    isDownloading = false
                )
            }
        }
    }

    /**
     * Complete a flexible update after download completes
     * Call this after user confirms installation
     */
    fun completeUpdate(activity: Activity) {
        try {
            Log.d(TAG, "Completing update")
            updateManager.completeUpdate(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Error completing update: ${e.message}", e)
        }
    }

    /**
     * Clear the update state (e.g., when user dismisses the update)
     */
    fun dismissUpdate() {
        Log.d(TAG, "Update dismissed by user")
        updateFlowStarted = false
        _updateState.value = UpdateState()
    }

    // ============ UpdateCallback Implementation ============
    // These methods are called by GooglePlayUpdateManager when state changes

    override fun onUpdateAvailable(appUpdateInfo: AppUpdateInfo) {
        Log.d(TAG, "Update available - version: ${appUpdateInfo.availableVersionCode()}")
        _updateState.value = _updateState.value.copy(
            updateAvailable = true,
            appUpdateInfo = appUpdateInfo
        )
    }

    override fun onUpdateInProgress(bytesDownloaded: Long, totalBytes: Long) {
        val progress = if (totalBytes > 0) {
            (bytesDownloaded.toFloat() / totalBytes.toFloat())
        } else {
            0f
        }
        Log.d(TAG, "Update progress: ${(progress * 100).toInt()}%")
        _updateState.value = _updateState.value.copy(
            downloadProgress = progress
        )
    }

    override fun onUpdateInstalled() {
        Log.d(TAG, "Update installed successfully")
        _updateState.value = _updateState.value.copy(
            updateInstalled = true,
            isDownloading = false
        )
    }

    override fun onUpdateFailed(exception: Exception) {
        Log.e(TAG, "Update failed: ${exception.message}", exception)
        _updateState.value = _updateState.value.copy(
            error = exception.message ?: "Update failed",
            isDownloading = false
        )
    }

    // Cleanup when ViewModel is destroyed
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared, unregistering listeners")
        updateManager.unregisterListener()
    }
}
