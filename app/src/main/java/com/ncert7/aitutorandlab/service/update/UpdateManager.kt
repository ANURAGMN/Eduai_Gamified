package com.ncert7.aitutorandlab.service.update

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateInfo

/**
 * Interface for managing in-app updates using Google Play's real in-app update API
 */
interface UpdateManager {
    /**
     * Check for available updates from Play Store
     * If an update is available, the callback will be triggered
     */
    suspend fun checkForUpdate(activity: Activity)

    /**
     * Start the update flow
     * This will show Google's native in-app update UI
     */
    suspend fun startUpdate(activity: Activity, updateType: UpdateType = UpdateType.FLEXIBLE)

    /**
     * Complete the flexible update flow after user confirms
     * This installs the downloaded update
     */
    fun completeUpdate(activity: Activity)

    /**
     * On resume, detect a downloaded flexible update or continue an immediate update.
     */
    suspend fun checkResumeState(activity: Activity)

    /**
     * Set a callback for update state changes
     */
    fun setUpdateCallback(callback: UpdateCallback?)
}

/**
 * Update type determines the UI behavior
 * - FLEXIBLE: User can dismiss and continue using app, update installs in background
 * - IMMEDIATE: Mandatory update, user must update to continue using app
 */
enum class UpdateType {
    IMMEDIATE,
    FLEXIBLE
}

/**
 * Callback interface for tracking update state changes
 */
interface UpdateCallback {
    fun onUpdateAvailable(appUpdateInfo: AppUpdateInfo)
    fun onUpdateInProgress(bytesDownloaded: Long, totalBytes: Long)
    fun onUpdateInstalled()
    fun onUpdateFailed(exception: Exception)
}
