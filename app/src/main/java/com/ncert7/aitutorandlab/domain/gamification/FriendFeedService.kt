package com.ncert7.aitutorandlab.domain.gamification

import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.data.local.dao.ProgressDao
import com.ncert7.aitutorandlab.repository.FriendRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendFeedService @Inject constructor(
    private val friendRepository: FriendRepository,
    private val progressDao: ProgressDao,
) {
    suspend fun onStreakUpdated(studentId: String, streakCount: Int) {
        if (studentId.isBlank() || streakCount <= 0) return
        if (streakCount !in STREAK_MILESTONES) return
        friendRepository.publishFeedEvent(
            actorStudentId = studentId,
            eventType = FriendEventType.STREAK_MILESTONE,
            message = "Reached a $streakCount day streak",
            eventKeySuffix = streakCount.toString(),
        )
    }

    suspend fun onConceptCompleted(studentId: String, language: String) {
        if (studentId.isBlank()) return
        val total =
            progressDao.getTotalCompletedConceptsFlow(
                studentId,
                language,
                AppConfig.APP_NAME,
            ).first()
        if (total <= 0 || total % CONCEPT_MILESTONE_STEP != 0) return
        friendRepository.publishFeedEvent(
            actorStudentId = studentId,
            eventType = FriendEventType.CONCEPT_MILESTONE,
            message = "Completed $total concepts",
            eventKeySuffix = total.toString(),
        )
    }

    suspend fun onInviteRewardGranted(
        inviteeStudentId: String,
        inviterStudentId: String,
        gemsEach: Int,
    ) {
        if (inviteeStudentId.isBlank() || inviterStudentId.isBlank()) return
        friendRepository.publishFeedEvent(
            actorStudentId = inviteeStudentId,
            eventType = FriendEventType.INVITE_REWARD,
            message = "Earned $gemsEach gems for completing your first lesson with a friend",
            eventKeySuffix = inviterStudentId,
        )
        friendRepository.publishFeedEvent(
            actorStudentId = inviterStudentId,
            eventType = FriendEventType.INVITE_REWARD,
            message = "Earned $gemsEach gems — your friend finished their first lesson",
            eventKeySuffix = inviteeStudentId,
        )
    }

    companion object {
        val STREAK_MILESTONES = setOf(7, 14, 30)
        const val CONCEPT_MILESTONE_STEP = 10
    }
}
