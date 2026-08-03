package com.ncert7.aitutorandlab.repository

import com.ncert7.aitutorandlab.domain.gamification.FriendFeedService
import com.ncert7.aitutorandlab.domain.gamification.StreakFreezeService
import com.ncert7.aitutorandlab.service.analytics.GamificationAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.StreakMilestone
import com.ncert7.aitutorandlab.config.AppConfig
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
            val currentStreak = getUserStreak(userId)

            val newStreakCount = when {
                // First ever streak event for this user
                currentStreak == null -> {
                    DebugLogger.debugLog("StreakRepository", "First streak event for $userId - starting at 1")
                    1
                }

                // Same calendar day → do NOT increment
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
     * Create initial streak for new user
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

            // Try to sync with Firestore
            firebaseRepository.updateStreak(userId, 1, dayIdentifier)
            true
        } catch (e: Exception) {
            DebugLogger.errorLog("StreakRepository", "Error creating streak: ${e.message}")
            false
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
     * Sync streak when user logs in
     * - For new users: Initializes streak = 1 if doesn't exist
     * - For existing users: Fetches from Firestore and validates
     * - Handles consecutive day logic automatically
     */
    suspend fun syncStreakOnLogin(userId: String): StreakEntity {
        return try {
            DebugLogger.debugLog("StreakRepository", "Syncing streak on login for user: $userId")

            // Try to get user's existing streak from Firestore
            val remoteStreak = firebaseRepository.getStreak(userId)

            if (remoteStreak != null) {
                // EXISTING USER - Fetch their streak
                DebugLogger.debugLog("StreakRepository", "Existing user found with streak: ${remoteStreak.streakCount}")

                val streakEntity = StreakEntity(
                    userId = remoteStreak.userId,
                    streakCount = remoteStreak.streakCount,
                    lastStreakDate = remoteStreak.lastStreakDate,
                    createdAt = remoteStreak.createdAt,
                    updatedAt = remoteStreak.updatedAt,
                    appName = remoteStreak.appName,
                    isSynced = true
                )

                // Cache locally
                streakDao.insertStreak(streakEntity)
                DebugLogger.debugLog("StreakRepository", "Existing user streak cached locally")

                return streakEntity
            } else {
                // NEW USER - Create initial streak
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

                // Save to local DB
                streakDao.insertStreak(newStreak)
                DebugLogger.debugLog("StreakRepository", "New user streak created and saved locally (count=1)")

                // Try to sync to Firestore (if online)
                try {
                    firebaseRepository.updateStreak(userId, 1, dayIdentifier)
                    streakDao.markStreakAsSynced(userId)
                    DebugLogger.debugLog("StreakRepository", "New user streak synced to Firestore")
                } catch (e: Exception) {
                    DebugLogger.errorLog("StreakRepository", "New user streak sync error: ${e.message}")
                }

                return newStreak
            }
        } catch (e: Exception) {
            DebugLogger.errorLog("StreakRepository", "Error syncing streak on login: ${e.message}")

            // Fallback: Create initial streak locally for safety
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
            return fallbackStreak
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
