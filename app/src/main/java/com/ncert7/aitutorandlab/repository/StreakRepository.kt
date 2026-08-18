package com.ncert7.aitutorandlab.repository

import com.ncert7.aitutorandlab.domain.gamification.FriendFeedService
import com.ncert7.aitutorandlab.domain.gamification.StreakActivityRules
import com.ncert7.aitutorandlab.domain.gamification.StreakDayLogic
import com.ncert7.aitutorandlab.domain.gamification.StreakFreezeService
import com.ncert7.aitutorandlab.domain.gamification.StreakSyncPolicy
import com.ncert7.aitutorandlab.service.analytics.GamificationAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.StreakMilestone
import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.data.firebase.model.Streak
import com.ncert7.aitutorandlab.data.local.dao.StreakDao
import com.ncert7.aitutorandlab.data.local.entities.StreakEntity
import com.ncert7.aitutorandlab.debug.DebugLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repository for managing streak data
 * Handles both local (Room) and remote (Firestore) streak operations
 * Serves as a single source of truth for streak data
 */
class StreakRepository(
    private val streakDao: StreakDao,
    private val firebaseRepository: FirebaseRepository,
    private val friendFeedService: FriendFeedService,
    private val streakFreezeService: StreakFreezeService,
) {
    private val streakMutex = Mutex()

    /**
     * Get user's current streak
     * First tries local cache, falls back to Firestore
     */
    suspend fun getUserStreak(userId: String): StreakEntity? {
        return try {
            streakMutex.withLock { loadLocalOrRemoteLocked(userId) }
        } catch (e: Exception) {
            DebugLogger.errorLog("StreakRepository", "Error getting user streak: ${e.message}")
            null
        }
    }

    /** Room first; Firestore only when the table is empty (reinstall / new device). */
    private suspend fun loadLocalOrRemoteLocked(userId: String): StreakEntity? {
        val localStreak = streakDao.getStreakByUserId(userId)
        if (localStreak != null) {
            DebugLogger.debugLog("StreakRepository", "Streak found in local DB: ${localStreak.streakCount}")
            return localStreak
        }

        val remoteStreak = firebaseRepository.getStreak(userId)
        if (remoteStreak != null) {
            val streakEntity = cacheRemoteLocked(remoteStreak)
            DebugLogger.debugLog(
                "StreakRepository",
                "Streak fetched from Firestore and cached: ${streakEntity.streakCount}",
            )
            return streakEntity
        }

        DebugLogger.debugLog("StreakRepository", "No streak found for user: $userId")
        return null
    }

    private suspend fun cacheRemoteLocked(remoteStreak: Streak): StreakEntity {
        val streakEntity = StreakEntity(
            userId = remoteStreak.userId,
            streakCount = remoteStreak.streakCount,
            lastStreakDate = remoteStreak.lastStreakDate,
            createdAt = remoteStreak.createdAt,
            updatedAt = remoteStreak.updatedAt,
            appName = remoteStreak.appName,
            isSynced = true,
        )
        streakDao.insertStreak(streakEntity)
        return streakEntity
    }

    /**
     * Get user's streak as a reactive Flow
     * Automatically emits whenever the streak data changes in the database
     * Perfect for UI reactive updates
     */
    fun getStreakFlow(userId: String): Flow<StreakEntity?> {
        return streakDao.getStreakByUserIdFlow(userId)
    }

    /**
     * Update streak locally and sync with Firestore
     * Returns the updated streak count
     */
    suspend fun updateStreak(userId: String, newStreakCount: Int, lastStreakDate: Long): Int {
        return try {
            val now = System.currentTimeMillis()

            // Update local database
            val streakEntity = StreakEntity(
                userId = userId,
                streakCount = newStreakCount,
                lastStreakDate = lastStreakDate,
                updatedAt = now,
                appName = AppConfig.APP_NAME,
                isSynced = false
            )
            streakDao.insertStreak(streakEntity)
            DebugLogger.debugLog("StreakRepository", "Streak updated locally: $newStreakCount")

            // Defer the Firestore push. The row is already marked isSynced=false; the coalesced
            // outbox flush (debounced / on background) syncs streak via syncUnsyncedStreak().
            // Replaces the old eager trio: triggerFullSync + direct updateStreak + markSynced.
            com.ncert7.aitutorandlab.service.sync.DataSyncService.scheduleDeferredUpload()

            friendFeedService.onStreakUpdated(userId, newStreakCount)
            newStreakCount
        } catch (e: Exception) {
            DebugLogger.errorLog("StreakRepository", "Error updating streak: ${e.message}")
            0
        }
    }

    /**
     * Records ANY learning / app-open activity and updates the streak accordingly in the database.
     * Handles same-day checks, consecutive day increments, and resets.
     * Same calendar day → no Room write and no deferred Firestore upload.
     *
     * @param userId The ID of the student
     * @return The new streak count
     */
    suspend fun recordActivity(userId: String): Int {
        return try {
            streakMutex.withLock { recordActivityLocked(userId) }
        } catch (e: Exception) {
            DebugLogger.errorLog("StreakRepository", "Error recording activity for streak: ${e.message}")
            0
        }
    }

    private suspend fun recordActivityLocked(userId: String): Int {
            val now = System.currentTimeMillis()
            val today = StreakDayLogic.startOfDay(now)
            // Hot path stays local. Firestore only if Room is empty (reinstall / first device).
            val currentStreak = loadLocalOrRemoteLocked(userId)

            val freezeContinuation =
                if (
                    currentStreak != null &&
                    !StreakDayLogic.isSameDay(currentStreak.lastStreakDate, now) &&
                    !StreakDayLogic.isConsecutiveDay(currentStreak.lastStreakDate, now)
                ) {
                    streakFreezeService.resolveStreakAfterMissedDay(
                        userId = userId,
                        currentStreak = currentStreak,
                        now = now,
                    )
                } else {
                    null
                }

            return when (val outcome = StreakActivityRules.next(currentStreak, now, freezeContinuation)) {
                is StreakActivityRules.Result.NoWrite -> {
                    DebugLogger.debugLog(
                        "StreakRepository",
                        "Same day activity for $userId - streak remains ${outcome.count}",
                    )
                    outcome.count
                }
                is StreakActivityRules.Result.Persist -> {
                    val newStreakCount = outcome.count
                    when {
                        currentStreak == null ->
                            DebugLogger.debugLog("StreakRepository", "First streak event for $userId - starting at 1")
                        freezeContinuation != null ->
                            DebugLogger.debugLog(
                                "StreakRepository",
                                "Streak freeze used for $userId — continuing at $newStreakCount",
                            )
                        StreakDayLogic.isConsecutiveDay(currentStreak.lastStreakDate, now) ->
                            DebugLogger.debugLog(
                                "StreakRepository",
                                "Consecutive day for $userId - streak increased to $newStreakCount",
                            )
                        else -> {
                            DebugLogger.debugLog(
                                "StreakRepository",
                                "Day(s) skipped for $userId - streak reset to 1 (was ${currentStreak.streakCount})",
                            )
                            if (currentStreak.streakCount > 1) {
                                GamificationAnalyticsTracker.streakBreak(currentStreak.streakCount)
                            }
                        }
                    }

                    val result = updateStreak(userId, newStreakCount, today)
                    if (newStreakCount > 1 || currentStreak == null) {
                        GamificationAnalyticsTracker.streakExtended(newStreakCount)
                    }
                    StreakMilestone.entries.firstOrNull { it.value == newStreakCount }?.let { milestone ->
                        GamificationAnalyticsTracker.streakMilestone(milestone)
                    }
                    result
                }
            }
    }

    /**
     * Create initial streak for new user (local + deferred Firestore upload).
     */
    suspend fun createStreakForUser(userId: String): Boolean {
        return try {
            val now = System.currentTimeMillis()
            val dayIdentifier = StreakDayLogic.startOfDay(now)

            val streakEntity = StreakEntity(
                userId = userId,
                streakCount = 1,
                lastStreakDate = dayIdentifier,
                createdAt = now,
                updatedAt = now,
                appName = AppConfig.APP_NAME,
                isSynced = false
            )

            streakDao.insertStreak(streakEntity)
            DebugLogger.debugLog("StreakRepository", "Initial streak created for user: $userId")
            com.ncert7.aitutorandlab.service.sync.DataSyncService.scheduleDeferredUpload()
            true
        } catch (e: Exception) {
            DebugLogger.errorLog("StreakRepository", "Error creating streak: ${e.message}")
            false
        }
    }

    /**
     * Display streak for UI: keep new-user default of 1 when no row exists;
     * show 0 when the stored streak has expired (missed day(s) without freeze yet).
     */
    fun effectiveDisplayStreak(streak: StreakEntity?): Int =
        StreakDayLogic.effectiveDisplayCount(streak)

    /**
     * Clear all unsynced local streak data on logout
     * NOTE: Firestore streak is NOT deleted - it persists for the user
     */
    suspend fun clearLocalStreakOnLogout(): Boolean {
        return try {
            streakDao.clearAll()
            DebugLogger.debugLog("StreakRepository", "Local streak data cleared on logout (Firestore data preserved)")
            true
        } catch (e: Exception) {
            DebugLogger.errorLog("StreakRepository", "Error clearing local streak: ${e.message}")
            false
        }
    }

    /**
     * Sync all unsynced streak data with Firestore
     */
    suspend fun syncUnsyncedStreaks(): Boolean {
        return try {
            val streak = streakDao.getUnsyncedStreak()
            if (streak == null) {
                DebugLogger.debugLog("StreakRepository", "No unsynced streak to sync")
                return true
            }

            var allSynced = true
            val success = firebaseRepository.updateStreak(
                streak.userId,
                streak.streakCount,
                streak.lastStreakDate
            )
            if (success) {
                streakDao.markStreakAsSynced(streak.userId)
            } else {
                allSynced = false
            }

            DebugLogger.debugLog("StreakRepository", "Sync complete. All synced: $allSynced")
            allSynced
        } catch (e: Exception) {
            DebugLogger.errorLog("StreakRepository", "Error syncing streaks: ${e.message}")
            false
        }
    }

    /**
     * Clear all streak data (on user logout)
     */
    suspend fun clearAllStreaks(): Boolean {
        return try {
            streakDao.clearAll()
            DebugLogger.debugLog("StreakRepository", "All local streaks cleared")
            true
        } catch (e: Exception) {
            DebugLogger.errorLog("StreakRepository", "Error clearing streaks: ${e.message}")
            false
        }
    }

    /**
     * Sync streak when user logs in.
     * Offline-first merge: never blindly overwrite a newer / higher local streak with remote.
     * New users get a local streak=1 and a deferred Firestore upload (no eager write).
     */
    suspend fun syncStreakOnLogin(userId: String): StreakEntity {
        return try {
            DebugLogger.debugLog("StreakRepository", "Syncing streak on login for user: $userId")

            streakMutex.withLock {
                val local = streakDao.getStreakByUserId(userId)
                val remoteStreak = firebaseRepository.getStreak(userId)

                when {
                    remoteStreak == null && local == null -> {
                        val now = System.currentTimeMillis()
                        val dayIdentifier = StreakDayLogic.startOfDay(now)
                        val newStreak = StreakEntity(
                            userId = userId,
                            streakCount = 1,
                            lastStreakDate = dayIdentifier,
                            createdAt = now,
                            updatedAt = now,
                            appName = AppConfig.APP_NAME,
                            isSynced = false,
                        )
                        streakDao.insertStreak(newStreak)
                        com.ncert7.aitutorandlab.service.sync.DataSyncService.scheduleDeferredUpload()
                        DebugLogger.debugLog("StreakRepository", "New user streak created locally (deferred upload)")
                        newStreak
                    }

                    remoteStreak == null && local != null -> {
                        if (!local.isSynced) {
                            com.ncert7.aitutorandlab.service.sync.DataSyncService.scheduleDeferredUpload()
                        }
                        DebugLogger.debugLog(
                            "StreakRepository",
                            "No remote streak — keeping local count=${local.streakCount}",
                        )
                        local
                    }

                    remoteStreak != null && local == null -> {
                        val cached = cacheRemoteLocked(remoteStreak)
                        DebugLogger.debugLog(
                            "StreakRepository",
                            "Remote streak cached locally: ${cached.streakCount}",
                        )
                        cached
                    }

                    else -> {
                        val merged = StreakSyncPolicy.mergeForLogin(local!!, remoteStreak!!)
                        streakDao.insertStreak(merged)
                        if (!merged.isSynced) {
                            com.ncert7.aitutorandlab.service.sync.DataSyncService.scheduleDeferredUpload()
                        }
                        DebugLogger.debugLog(
                            "StreakRepository",
                            "Login merge → count=${merged.streakCount}, synced=${merged.isSynced}",
                        )
                        merged
                    }
                }
            }
        } catch (e: Exception) {
            DebugLogger.errorLog("StreakRepository", "Error syncing streak on login: ${e.message}")

            val existing = streakDao.getStreakByUserId(userId)
            if (existing != null) return existing

            val now = System.currentTimeMillis()
            val dayIdentifier = StreakDayLogic.startOfDay(now)
            val fallbackStreak = StreakEntity(
                userId = userId,
                streakCount = 1,
                lastStreakDate = dayIdentifier,
                createdAt = now,
                updatedAt = now,
                appName = AppConfig.APP_NAME,
                isSynced = false
            )
            streakDao.insertStreak(fallbackStreak)
            com.ncert7.aitutorandlab.service.sync.DataSyncService.scheduleDeferredUpload()
            fallbackStreak
        }
    }

    /**
     * Public method to check if streak is valid (not expired)
     */
    suspend fun isStreakValid(userId: String): Boolean {
        return try {
            val streak = getUserStreak(userId) ?: return false
            if (streak.lastStreakDate == 0L) return false

            val now = System.currentTimeMillis()
            StreakDayLogic.isSameDay(streak.lastStreakDate, now) ||
                StreakDayLogic.isConsecutiveDay(streak.lastStreakDate, now)
        } catch (e: Exception) {
            DebugLogger.errorLog("StreakRepository", "Error checking streak validity: ${e.message}")
            false
        }
    }
}
