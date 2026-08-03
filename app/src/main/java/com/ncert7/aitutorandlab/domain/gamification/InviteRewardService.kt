package com.ncert7.aitutorandlab.domain.gamification

import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.data.local.dao.GamificationDao
import com.ncert7.aitutorandlab.data.local.dao.ProgressDao
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.repository.FirebaseRepository
import com.ncert7.aitutorandlab.repository.GamificationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Path B (code-entry fallback): when an invitee completes their first concept,
 * both users receive invite gem rewards once.
 */
@Singleton
class InviteRewardService @Inject constructor(
    private val gamificationDao: GamificationDao,
    private val gamificationRepository: GamificationRepository,
    private val firebaseRepository: FirebaseRepository,
    private val friendFeedService: FriendFeedService,
    private val progressDao: ProgressDao,
) {
    suspend fun tryGrantOnFirstConceptCompleted(inviteeStudentId: String, language: String) {
        if (inviteeStudentId.isBlank()) return
        val totalCompleted =
            progressDao.getTotalCompletedConceptsFlow(
                inviteeStudentId,
                language,
                AppConfig.APP_NAME,
            ).first()
        if (totalCompleted != 1) return

        val profile = gamificationDao.getProfile(inviteeStudentId) ?: return
        if (profile.inviteRewardGranted) return
        val inviteCode = profile.invitedByCode?.trim()?.uppercase().orEmpty()
        if (inviteCode.isBlank()) return

        val inviterProfile =
            gamificationDao.getProfileByFriendCode(inviteCode)
                ?: firebaseRepository.lookupFriendCode(inviteCode)?.studentId?.let { inviterId ->
                    gamificationDao.getProfile(inviterId)
                }
                ?: return

        if (inviterProfile.studentId == inviteeStudentId) return

        val inviteeGrantKey = "invite_reward_invitee_${inviteeStudentId}"
        val inviterGrantKey = "invite_reward_inviter_${inviterProfile.studentId}_$inviteeStudentId"

        val inviteeGems =
            gamificationRepository.grantGemsIfEligible(
                studentId = inviteeStudentId,
                grantKey = inviteeGrantKey,
                gemsAmount = EconomyConfig.GEM_INVITE_REWARD,
                source = "invite_reward",
            )
        val inviterGems =
            gamificationRepository.grantGemsIfEligible(
                studentId = inviterProfile.studentId,
                grantKey = inviterGrantKey,
                gemsAmount = EconomyConfig.GEM_INVITE_REWARD,
                source = "invite_reward",
            )

        if (inviteeGems == null && inviterGems == null) return

        gamificationDao.upsertProfile(
            profile.copy(
                inviteRewardGranted = true,
                updatedAt = System.currentTimeMillis(),
                isSynced = false,
            ),
        )

        friendFeedService.onInviteRewardGranted(
            inviteeStudentId = inviteeStudentId,
            inviterStudentId = inviterProfile.studentId,
            gemsEach = EconomyConfig.GEM_INVITE_REWARD,
        )
        DebugLogger.debugLog(TAG, "Invite reward granted for invitee=$inviteeStudentId inviter=${inviterProfile.studentId}")
    }

    companion object {
        private const val TAG = "InviteRewardService"
    }
}
