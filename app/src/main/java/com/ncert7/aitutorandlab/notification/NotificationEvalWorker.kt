package com.ncert7.aitutorandlab.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.di.NotificationEntryPoint
import dagger.hilt.android.EntryPointAccessors

class NotificationEvalWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val trigger =
                inputData.getString(KEY_TRIGGER)?.let { raw ->
                    runCatching { NotificationEvalTrigger.valueOf(raw) }.getOrNull()
                } ?: NotificationEvalTrigger.PERIODIC_SWEEP

            val orchestrator =
                EntryPointAccessors
                    .fromApplication(applicationContext, NotificationEntryPoint::class.java)
                    .notificationOrchestrator()
            orchestrator.runEvalPass(trigger)
            Result.success()
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "Eval worker failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        const val TAG = "NotificationEvalWorker"
        const val KEY_TRIGGER = "trigger"
    }
}
