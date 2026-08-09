package com.ncert7.aitutorandlab.repository

import com.ncert7.aitutorandlab.domain.gamification.FriendFeedService
import com.ncert7.aitutorandlab.domain.gamification.StreakFreezeService
import com.ncert7.aitutorandlab.service.analytics.GamificationAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.StreakMilestone
import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.data.firebase.model.Streak
import com.ncert7.aitutorandlab.data.local.dao.StreakDao
import com.ncert7.aitutorandlab.data.local.entities.StreakEntity
import com.ncert7.aitutorandlab.debug.DebugLogger
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

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

    /**
     * Get user's current streak
     * First tries local cache, falls back to Firestore
     */
    suspend fun getUserStreak(userId: String): StreakEntity? {
        return try {
            // Try to get from local database first
            val localStreak = streakDao.getStreakByUserId(userId)

            if (localStreak != null) {
                DebugLogger.debugLog("StreakRepository", "Streak found in local DB: ${localStreak.streakCount}")
                return localStreak
            }

            // If not in local DB, try to fetch from Firestore
            val remoteStreak = firebaseRepository.getStreak(userId)
            if (remoteStreak != null) {
                val streakEntity = StreakEntity(
                    userId = remoteStreak.userId,
                    streakCount = remoteStreak.streakCount,
                    lastStreakDate = remoteStreak.lastStreakDate,
                    createdAt = remoteStreak.createdAt,
                    updatedAt = remoteStreak.updatedAt,
                    appName = remoteStreak.appName,
                    isSynced = true
                )
                // Cache it locally
                streakDao.insertStreak(streakEntity)
                DebugLogger.debugLog("StreakRepository", "Streak fetched from Firestore and cached: ${streakEntity.streakCount}")
                return streakEntity
            }

            // No streak found anywhere
            DebugLogger.debugLog("StreakRepository", "No streak found for user: $userId")
            null
        } catch (e: Exception) {
            DebugLogger.errorLog("StreakRepository", "Error getting user streak: ${e.message}")
            null
        }
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
     * Records ANY learning activity and updates the streak accordingly in the database.
     * Handles same-day checks, consecutive day increments, and resets.
     *
     * @param userId The ID of the student
     * @return The new streak count
     */
    suspend fun recordActivity(userId: String): Int {
        return try {
            val now = System.currentTimeMillis()
            val today = getDayIdentifier(now)
            // Local-only: avoid a Firestore read on the hot learning path.
            // Login sync / getUserStreak hydrate Room; same-day returns without any write.
            val currentStreak = streakDao.getStreakByUserId(userId)

            val newStreakCount = when {
                // First ever streak event for this user
                currentStreak == null -> {
                    DebugLogger.debugLog("StreakRepository", "First streak event for $userId - starting at 1")
                    1
                }

                // Same calendar day → do NOT increment (no Room write, no deferred upload)
                isSameDay(currentStreak.lastStreakDate, now) -> {
                    DebugLogger.debugLog("StreakRepository", "Same day activity for $userId - streak remains ${currentStreak.streakCount}")
                    return currentStreak.streakCount
                }

                // Next consecutive day → continue streak
                isConsecutiveDay(currentStreak.lastStreakDate, now) -> {
                    val newCount = currentStreak.streakCount + 1
                    DebugLogger.debugLog("StreakRepository", "Consecutive day for $userId - streak increased to $newCount")
                    newCount
                }

                // Days were skipped → use a weekly freeze or reset streak
                else -> {
                    val frozenCount =
                        streakFreezeService.resolveStreakAfterMissedDay(
                            userId = userId,
                            currentStreak = currentStreak,
                            now = now,
                        )
                    if (frozenCount != null) {
                        DebugLogger.debugLog(
                            "StreakRepository",
                            "Streak freeze used for $userId — continuing at $frozenCount",
                        )
                        frozenCount
                    } else {
                        DebugLogger.debugLog(
                            "StreakRepository",
                            "Day(s) skipped for $userId - streak reset to 1 (was ${currentStreak.streakCount})",
                        )
                        if (currentStreak.streakCount > 1) {
                            GamificationAnalyticsTracker.streakBreak(currentStreak.streakCount)
                        }
                        1
                    }
                }
            }

            // Update database and sync
            val result = updateStreak(userId, newStreakCount, today)
            if (newStreakCount > 1 || currentStreak == null) {
                GamificationAnalyticsTracker.streakExtended(newStreakCount)
            }
            StreakMilestone.entries.firstOrNull { it.value == newStreakCount }?.let { milestone ->
                GamificationAnalyticsTracker.streakMilestone(milestone)
            }
            result
        } catch (e: Exception) {
            DebugLogger.errorLog("StreakRepository", "Error recording activity for streak: ${e.message}")
            0
        }
    }

    /**
     * Create initial streak for new user (local + deferred Firestore upload).
     */
    suspend fun createStreakForUser(userId: String): Boolean {
        return try {
            val now = System.currentTimeMillis()
            val dayIdentifier = getDayIdentifier(now)

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
    fun effectiveDisplayStreak(streak: StreakEntity?): Int {
        if (streak == null) return 1
        if (streak.lastStreakDate == 0L) return 0
        val now = System.currentTimeMillis()
        return if (isSameDay(streak.lastStreakDate, now) || isConsecutiveDay(streak.lastStreakDate, now)) {
            streak.streakCount
        } else {
            0
        }
    }

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
     * Check if two timestamps fall on the same calendar day
     */
    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Check if time2 is exactly one calendar day after time1
     */
    private fun isConsecutiveDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply {
            timeInMillis = time1
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val cal2 = Calendar.getInstance().apply {
            timeInMillis = time2
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        cal1.add(Calendar.DAY_OF_YEAR, 1)

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Get day identifier (start of day timestamp) for consistent day comparison
     */
    private fun getDayIdentifier(time: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Calculate difference in calendar days between two timestamps
     */
    private fun getDayDifference(time1: Long, time2: Long): Int {
        val cal1 = Calendar.getInstance().apply {
            timeInMillis = time1
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val cal2 = Calendar.getInstance().apply {
            timeInMillis = time2
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffInMillis = cal2.timeInMillis - cal1.timeInMillis
        return (diffInMillis / (24 * 60 * 60 * 1000)).toInt()
    }

    /**
     * Sync streak when user logs in.
     * Offline-first merge: never blindly overwrite a newer / higher local streak with remote.
     * New users get a local streak=1 and a deferred Firestore upload (no eager write).
     */
    suspend fun syncStreakOnLogin(userId: String): StreakEntity {
        return try {
            DebugLogger.debugLog("StreakRepository", "Syncing streak on login for user: $userId")

            val local = streakDao.getStreakByUserId(userId)
            val remoteStreak = firebaseRepository.getStreak(userId)

            when {
                remoteStreak == null && local == null -> {
                    val now = System.currentTimeMillis()
                    val dayIdentifier = getDayIdentifier(now)
                    val newStreak = StreakEntity(
                        userId = userId,
                        streakCount = 1,
                        lastStreakDate = dayIdentifier,
                        createdAt = now,
                        updatedAt = now,
                        appName = AppConfig.APP_NAME,
                        isSynced = false
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
                    val cached = StreakEntity(
                        userId = remoteStreak.userId,
                        streakCount = remoteStreak.streakCount,
                        lastStreakDate = remoteStreak.lastStreakDate,
                        createdAt = remoteStreak.createdAt,
                        updatedAt = remoteStreak.updatedAt,
                        appName = remoteStreak.appName,
                        isSynced = true
                    )
                    streakDao.insertStreak(cached)
                    DebugLogger.debugLog(
                        "StreakRepository",
                        "Remote streak cached locally: ${cached.streakCount}",
                    )
                    cached
                }

                else -> {
                    val merged = mergeStreakForLogin(local!!, remoteStreak!!)
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
        } catch (e: Exception) {
            DebugLogger.errorLog("StreakRepository", "Error syncing streak on login: ${e.message}")

            val existing = streakDao.getStreakByUserId(userId)
            if (existing != null) return existing

            val now = System.currentTimeMillis()
            val dayIdentifier = getDayIdentifier(now)
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
     * Prefer unsynced local when it is at least as fresh as remote.
     * Same activity day → max(count). Otherwise more recent [StreakEntity.lastStreakDate] wins.
     */
    private fun mergeStreakForLogin(
        local: StreakEntity,
        remote: Streak,
    ): StreakEntity {
        val remoteEntity = StreakEntity(
            userId = remote.userId,
            streakCount = remote.streakCount,
            lastStreakDate = remote.lastStreakDate,
            createdAt = minOf(local.createdAt, remote.createdAt),
            updatedAt = remote.updatedAt,
            appName = remote.appName.ifBlank { local.appName },
            isSynced = true
        )

        // Device ahead of cloud — keep local and push later.
        if (!local.isSynced && local.updatedAt >= remote.updatedAt) {
            return local
        }

        if (local.lastStreakDate == remote.lastStreakDate) {
            val bestCount = maxOf(local.streakCount, remote.streakCount)
            val needsUpload = bestCount > remote.streakCount
            return local.copy(
                streakCount = bestCount,
                createdAt = minOf(local.createdAt, remote.createdAt),
                updatedAt = maxOf(local.updatedAt, remote.updatedAt),
                isSynced = !needsUpload
            )
        }

        return if (local.lastStreakDate > remote.lastStreakDate) {
            local.copy(isSynced = false)
        } else {
            remoteEntity
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
            isSameDay(streak.lastStreakDate, now) || isConsecutiveDay(streak.lastStreakDate, now)
        } catch (e: Exception) {
            DebugLogger.errorLog("StreakRepository", "Error checking streak validity: ${e.message}")
            false
        }
    }
}
