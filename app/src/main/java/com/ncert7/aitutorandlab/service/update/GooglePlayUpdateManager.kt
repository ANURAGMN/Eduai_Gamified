package com.ncert7.aitutorandlab.service.update

import android.app.Activity
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Google Play implementation of UpdateManager
 * Uses Google Play In-App Update API with native popup UI
 *
 * Features:
 * - Automatic detection of available updates from Play Store
 * - Native Google UI for update prompts (no custom dialogs)
 * - Support for both IMMEDIATE and FLEXIBLE update modes
 * - Automatic background download for flexible updates
 * - Progress tracking for downloads
 */
class GooglePlayUpdateManager : UpdateManager {
    companion object {
        private const val TAG = "GooglePlayUpdateManager"
        private const val REQUEST_CODE_UPDATE = 100
    }

    private var appUpdateManager: AppUpdateManager? = null
    private var updateCallback: UpdateCallback? = null
    private var appUpdateInfo: AppUpdateInfo? = null

    /**
     * Listen for install state changes from Google Play
     * This handles background download progress and completion
     */
    private val installListener = InstallStateUpdatedListener { installState ->
        Log.d(TAG, "Install state: ${installState.installStatus()}")
        when (installState.installStatus()) {
            InstallStatus.DOWNLOADED -> {
                // Update downloaded and ready to install
                // For flexible updates, user can choose to install later
                Log.d(TAG, "Update downloaded successfully, ready to install")
                updateCallback?.onUpdateInstalled()
            }
            InstallStatus.DOWNLOADING -> {
                // Report download progress
                val downloaded = installState.bytesDownloaded()
                val total = installState.totalBytesToDownload()
                Log.d(TAG, "Download progress: $downloaded / $total bytes")
                updateCallback?.onUpdateInProgress(downloaded, total)
            }
            InstallStatus.INSTALLED -> {
                Log.d(TAG, "Update installed - app will be updated on next restart")
            }
            else -> {
                Log.d(TAG, "Other install state: ${installState.installStatus()}")
            }
        }
    }

    /**
     * Check for updates from Play Store
     * If an update is available, Google's native popup will be triggered
     */
    override suspend fun checkForUpdate(activity: Activity) = suspendCancellableCoroutine { continuation ->
        try {
            if (appUpdateManager == null) {
                appUpdateManager = AppUpdateManagerFactory.create(activity)
            }

            appUpdateManager?.appUpdateInfo?.addOnSuccessListener { appUpdateInfo ->
                this.appUpdateInfo = appUpdateInfo
                Log.d(TAG, "Update check successful. Available version code: ${appUpdateInfo.availableVersionCode()}")

                // Trigger callback if update is available
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    Log.d(TAG, "Update is available - triggering callback")
                    updateCallback?.onUpdateAvailable(appUpdateInfo)
                } else {
                    Log.d(TAG, "No update available")
                }
                continuation.resume(Unit)
            }?.addOnFailureListener { exception ->
                Log.e(TAG, "Failed to check for update: ${exception.message}", exception)
                updateCallback?.onUpdateFailed(exception)
                continuation.resume(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while checking for update: ${e.message}", e)
            updateCallback?.onUpdateFailed(e)
            continuation.resume(Unit)
        }
    }

    /**
     * Start the update flow using Google's native in-app update popup
     * The popup will be shown by Google Play - no custom UI needed
     */
    override suspend fun startUpdate(
        activity: Activity,
        updateType: UpdateType
    ) = suspendCancellableCoroutine { continuation ->
        try {
            appUpdateInfo?.let { appUpdateInfo ->
                if (appUpdateManager == null) {
                    appUpdateManager = AppUpdateManagerFactory.create(activity)
                }

                // Register listener to track download progress
                appUpdateManager?.registerListener(installListener)

                val updateTypeInt = when (updateType) {
                    UpdateType.IMMEDIATE -> AppUpdateType.IMMEDIATE
                    UpdateType.FLEXIBLE -> AppUpdateType.FLEXIBLE
                }

                Log.d(TAG, "Starting update with type: $updateType")

                // This will show Google's native in-app update UI
                appUpdateManager?.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateTypeInt,
                    activity,
                    REQUEST_CODE_UPDATE
                )
                continuation.resume(Unit)
            } ?: run {
                Log.w(TAG, "No update info available - checkForUpdate may not have been called")
                continuation.resume(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while starting update: ${e.message}", e)
            updateCallback?.onUpdateFailed(e)
            continuation.resume(Unit)
        }
    }

    /**
     * Complete the flexible update
     * This should be called when user confirms installation after download completes
     */
    override fun completeUpdate(activity: Activity) {
        try {
            Log.d(TAG, "Completing update")
            appUpdateManager?.completeUpdate()
        } catch (e: Exception) {
            Log.e(TAG, "Exception while completing update: ${e.message}", e)
        }
    }

    override suspend fun checkResumeState(activity: Activity) = suspendCancellableCoroutine { continuation ->
        try {
            if (appUpdateManager == null) {
                appUpdateManager = AppUpdateManagerFactory.create(activity)
            }
            appUpdateManager?.appUpdateInfo?.addOnSuccessListener { info ->
                when (info.installStatus()) {
                    InstallStatus.DOWNLOADED -> updateCallback?.onUpdateInstalled()
                    InstallStatus.FAILED, InstallStatus.CANCELED ->
                        updateCallback?.onUpdateFailed(
                            IllegalStateException("Update install status=${info.installStatus()}"),
                        )
                }
                if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    appUpdateManager?.registerListener(installListener)
                    appUpdateManager?.startUpdateFlowForResult(
                        info,
                        AppUpdateType.IMMEDIATE,
                        activity,
                        REQUEST_CODE_UPDATE,
                    )
                }
                continuation.resume(Unit)
            }?.addOnFailureListener { exception ->
                updateCallback?.onUpdateFailed(exception)
                continuation.resume(Unit)
            }
        } catch (e: Exception) {
            updateCallback?.onUpdateFailed(e)
            continuation.resume(Unit)
        }
    }

    override fun setUpdateCallback(callback: UpdateCallback?) {
        this.updateCallback = callback
    }

    /**
     * Cleanup: unregister the install listener
     * Call this when ViewModel is cleared
     */
    fun unregisterListener() {
        try {
            appUpdateManager?.unregisterListener(installListener)
            Log.d(TAG, "Install listener unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering listener: ${e.message}")
        }
    }
}
