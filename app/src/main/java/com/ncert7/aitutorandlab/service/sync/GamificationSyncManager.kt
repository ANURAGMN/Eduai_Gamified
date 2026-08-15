package com.ncert7.aitutorandlab.service.sync

import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.data.local.dao.GamificationDao
import com.ncert7.aitutorandlab.data.local.entities.GamificationProfileEntity
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.repository.FirebaseRepository
import com.ncert7.aitutorandlab.service.analytics.EngagementAnalyticsTracker

/**
 * Syncs gamification **profile balances** (XP, gems, league tier, friend code) to Firestore.
 *
 * Correctness rules (P1 §3 / R.2):
 * - Profile is LWW on `updatedAt` — never recompute balances by replaying XP/gem ledgers.
 * - Ledgers stay local / future audit upload keyed by business unique keys, not Room auto-ids.
 */
class GamificationSyncManager(
    private val gamificationDao: GamificationDao,
    private val firebaseRepository: FirebaseRepository,
) {

    suspend fun pushProfiles() {
        try {
            val dirty = gamificationDao.getUnsyncedProfiles()
            for (profile in dirty) {
                val ok =
                    firebaseRepository.saveGamificationProfile(
                        profile.studentId,
                        profile.toPayload(),
                    )
                if (ok) {
                    gamificationDao.markProfileSynced(profile.studentId)
                }
            }
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "pushProfiles failed: ${e.message}")
        }
    }

    suspend fun pushProfile(studentId: String) {
        if (studentId.isBlank()) return
        try {
            val profile = gamificationDao.getProfile(studentId) ?: return
            val ok = firebaseRepository.saveGamificationProfile(studentId, profile.toPayload())
            if (ok) gamificationDao.markProfileSynced(studentId)
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "pushProfile failed: ${e.message}")
        }
    }

    /**
     * Restore remote profile when local is missing or remote is newer (LWW).
     * Never awards XP/gems from events — balances come only from the profile doc.
     */
    suspend fun restoreProfile(studentId: String): GardenRestorePolicy.Outcome {
        if (studentId.isBlank()) return GardenRestorePolicy.Outcome.REMOTE_EMPTY
        return try {
            val remote = firebaseRepository.getGamificationProfile(studentId)
            if (remote == null) {
                EngagementAnalyticsTracker.restoreSkipped(DOMAIN, GardenRestorePolicy.Outcome.REMOTE_EMPTY.reason)
                return GardenRestorePolicy.Outcome.REMOTE_EMPTY
            }
            val remoteUpdated = (remote["updatedAt"] as? Number)?.toLong() ?: 0L
            val local = gamificationDao.getProfile(studentId)
            val localNewer =
                localProfileWins(
                    localMissing = local == null,
                    localXp = local?.lifetimeXp ?: 0,
                    localWeekly = local?.weeklyXp ?: 0,
                    localGems = local?.gems ?: 0,
                    localUpdatedAt = local?.updatedAt ?: 0L,
                    remoteUpdatedAt = remoteUpdated,
                )
            if (localNewer) {
                EngagementAnalyticsTracker.restoreSkipped(DOMAIN, "local_newer")
                return GardenRestorePolicy.Outcome.SKIPPED_LOCAL_PROGRESS
            }
            // Prefer remote friendCode when local is blank; keep local code if already set and remote empty.
            val remoteCode = remote["friendCode"] as? String ?: ""
            val friendCode =
                when {
                    remoteCode.isNotBlank() -> remoteCode
                    local?.friendCode?.isNotBlank() == true -> local.friendCode
                    else -> ""
                }
            val restored =
                GamificationProfileEntity(
                    studentId = studentId,
                    lifetimeXp = (remote["lifetimeXp"] as? Number)?.toInt() ?: 0,
                    weeklyXp = (remote["weeklyXp"] as? Number)?.toInt() ?: 0,
                    gems = (remote["gems"] as? Number)?.toInt() ?: 0,
                    leagueTier = remote["leagueTier"] as? String ?: "BRONZE",
                    currentWeekKey = remote["currentWeekKey"] as? String ?: "",
                    cohortId = remote["cohortId"] as? String,
                    friendCode = friendCode,
                    invitedByCode = remote["invitedByCode"] as? String ?: local?.invitedByCode,
                    inviteRewardGranted =
                        remote["inviteRewardGranted"] as? Boolean
                            ?: local?.inviteRewardGranted
                            ?: false,
                    updatedAt = remoteUpdated.takeIf { it > 0L } ?: System.currentTimeMillis(),
                    isSynced = true,
                )
            gamificationDao.upsertProfile(restored)
            EngagementAnalyticsTracker.restoreApplied(DOMAIN, itemCount = 1)
            DebugLogger.debugLog(
                TAG,
                "Gamification profile restored for $studentId xp=${restored.lifetimeXp} gems=${restored.gems}",
            )
            GardenRestorePolicy.Outcome.APPLIED
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "restoreProfile failed: ${e.message}")
            EngagementAnalyticsTracker.restoreSkipped(DOMAIN, GardenRestorePolicy.Outcome.ERROR.reason)
            GardenRestorePolicy.Outcome.ERROR
        }
    }

    private fun GamificationProfileEntity.toPayload(): Map<String, Any?> =
        mapOf(
            "studentId" to studentId,
            "lifetimeXp" to lifetimeXp,
            "weeklyXp" to weeklyXp,
            "gems" to gems,
            "leagueTier" to leagueTier,
            "currentWeekKey" to currentWeekKey,
            "cohortId" to cohortId,
            "friendCode" to friendCode,
            "invitedByCode" to invitedByCode,
            "inviteRewardGranted" to inviteRewardGranted,
            "updatedAt" to updatedAt,
            "appName" to AppConfig.APP_NAME,
        )

    companion object {
        private const val TAG = "GamificationSyncManager"
        private const val DOMAIN = "gamification"

        /**
         * Pure LWW decision (RV.3, unit-testable): does the LOCAL profile win over remote,
         * meaning restore should be skipped? Local wins only when it exists, is not a
         * zero-balance placeholder, and is at least as new as remote (`>=`, so a same-timestamp
         * tie keeps local and avoids re-applying identical remote data). Never derived from ledgers.
         */
        fun localProfileWins(
            localMissing: Boolean,
            localXp: Int,
            localWeekly: Int,
            localGems: Int,
            localUpdatedAt: Long,
            remoteUpdatedAt: Long,
        ): Boolean {
            if (localMissing) return false
            val placeholder = localXp == 0 && localWeekly == 0 && localGems == 0
            if (placeholder) return false
            return localUpdatedAt >= remoteUpdatedAt
        }
    }
}
