package com.ncert7.aitutorandlab.service.sync

import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.local.database.EduAiDatabase
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.di.TutorConfigEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlin.math.pow

/**
 * Weekly background worker responsible for syncing new Firebase data
 * into the local Room database and uploading unsynced user data.
 */
class WeeklySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "WeeklySyncWorker"
        private const val MAX_RETRY_ATTEMPTS = 3
    }
    override suspend fun doWork(): Result {
        return try {
            val database = EduAiDatabase.getInstance(applicationContext)
            val sharedPrefs = SharedPreferenceUtils(applicationContext)
            val studentId = sharedPrefs.getUserId()
            val retryAttempt = inputData.getInt("retry_attempt", 0)
            DebugLogger.debugLog(TAG, "Starting sync work (Attempt ${retryAttempt + 1}/${MAX_RETRY_ATTEMPTS})")

            // 1. Initialize Content Sync Manager
            val SyncManager = FirebaseSyncManager(
                subjectDao = database.subjectDao(),
                chapterDao = database.chapterDao(),
                conceptDao = database.conceptDao(),
                progressDao = database.progressDao(),
                streakDao = database.streakDao(),
                chapterProgressDao = database.chapterAgentProgressDao(),
                context = applicationContext
            )

            // 2. Sync all content (Subjects, Chapters, Concepts)
            val result = SyncManager.syncAllContent()
            if (result.success) {
                DebugLogger.debugLog("WeeklySync", "Content sync successful: ${result.message}")
            }else {
                DebugLogger.errorLog(TAG, "Sync failed: ${result.message}")
                // Retry if sync was unsuccessful
                return handleRetry(retryAttempt)
            }

            // 3. If user is logged in, sync/upload their progress data
            if (!studentId.isNullOrBlank()) {
                // A. Upload unsynced local data to Cloud
                val uploadManager = ProgressAnalyticsSessionSyncManager(
                    progressDao = database.progressDao(),
                    analyticsDao = database.appAnalyticsDao(),
                    sessionDao = database.sessionDao(),
                    streakDao = database.streakDao(),
                    chapterProgressDao = database.chapterAgentProgressDao(),
                    studentId = studentId
                )
                val uploadResult = uploadManager.syncAllUnsyncedData()
                DebugLogger.debugLog("WeeklySync", "Data upload result: ${uploadResult.message}")

                // B. Restore any progress from Cloud (for cross-device sync)
                SyncManager.syncUserProgress(studentId)
                SyncManager.syncUserStreak(studentId)
                SyncManager.syncChapterAgentProgress(studentId)

                val tutorRepo =
                    EntryPointAccessors
                        .fromApplication(applicationContext, TutorConfigEntryPoint::class.java)
                        .tutorConfigRepository()
                tutorRepo.syncPendingToRemote()
                tutorRepo.ensureLoaded(applicationContext, studentId)

                GardenSyncManager(
                    database.gardenDao(),
                    com.ncert7.aitutorandlab.repository.FirebaseRepository(),
                ).pushGarden(studentId)
                GamificationSyncManager(
                    database.gamificationDao(),
                    com.ncert7.aitutorandlab.repository.FirebaseRepository(),
                ).pushProfile(studentId)
                ExamPlanSyncManager(
                    database.examPlanDao(),
                    com.ncert7.aitutorandlab.repository.FirebaseRepository(),
                ).pushPlan(studentId)
                QuestSyncManager(
                    database.questDailyDao(),
                    com.ncert7.aitutorandlab.repository.FirebaseRepository(),
                ).pushTodayQuest(studentId)
            }

            DebugLogger.debugLog("WeeklySync", "Worker executed successfully")
            return Result.success()
        } catch (e: Exception) {
            DebugLogger.errorLog("WeeklySyncWorker", "Sync Error: ${e.message}")
            Result.retry()
        }
    }
    /**
     * Handles retry logic with exponential backoff.
     * @param currentAttempt The current retry attempt number (0-indexed)
     * @return Result.retry() if attempts remain, Result.failure() otherwise
     */
    private fun handleRetry(currentAttempt: Int): Result {
        return if (currentAttempt < MAX_RETRY_ATTEMPTS) {
            val nextAttempt = currentAttempt + 1
            val delayMillis = calculateBackoffDelay(nextAttempt)

            DebugLogger.debugLog(TAG, "Scheduling retry attempt $nextAttempt after ${delayMillis}ms")

            // Use exponential backoff: 2^n * base_delay
            Result.retry()
        } else {
            DebugLogger.errorLog(TAG, "Max retry attempts (${MAX_RETRY_ATTEMPTS}) reached. Giving up.")
            Result.failure()
        }
    }

    /**
     * Calculates exponential backoff delay.
     * Attempt 1: 1 minute, Attempt 2: 2 minutes, Attempt 3: 4 minutes
     */
    private fun calculateBackoffDelay(attemptNumber: Int): Long {
        val baseDelayMinutes = 1L
        val delayMultiplier = 2.0.pow((attemptNumber - 1).toDouble()).toLong()
        return baseDelayMinutes * delayMultiplier * 60 * 1000 // Convert to milliseconds
    }
}
