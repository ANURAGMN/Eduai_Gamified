package com.ncert7.aitutorandlab.domain.gamification

import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.local.dao.ProgressDao
import com.ncert7.aitutorandlab.repository.FriendRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class FriendFeedService @Inject constructor(
    private val friendRepository: FriendRepository,
    private val progressDao: ProgressDao,
    private val sharedPrefs: SharedPreferenceUtils,
) {
    suspend fun onStreakUpdated(studentId: String, streakCount: Int) {
        if (studentId.isBlank() || streakCount <= 0) return
        if (streakCount !in STREAK_MILESTONES) return
        friendRepository.publishFeedEvent(
            actorStudentId = studentId,
            eventType = FriendEventType.STREAK_MILESTONE,
            message = "Reached a $streakCount-day streak",
            eventKeySuffix = streakCount.toString(),
        )
    }

    suspend fun onConceptCompleted(
        studentId: String,
        language: String,
        topicTitle: String? = null,
    ) {
        if (studentId.isBlank()) return
        val title = topicTitle?.trim().orEmpty()
        if (title.isNotBlank()) {
            friendRepository.publishFeedEvent(
                actorStudentId = studentId,
                eventType = FriendEventType.TOPIC_COMPLETED,
                message = "Completed $title",
                eventKeySuffix = "${language}_${title.hashCode()}_${LocalDate.now()}",
            )
        }
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

    suspend fun onXpAwarded(studentId: String, lifetimeXpBefore: Int, lifetimeXpAfter: Int) {
        if (studentId.isBlank() || lifetimeXpAfter <= lifetimeXpBefore) return
        val crossed =
            XP_MILESTONES.filter { milestone ->
                lifetimeXpBefore < milestone && lifetimeXpAfter >= milestone
            }
        crossed.forEach { milestone ->
            friendRepository.publishFeedEvent(
                actorStudentId = studentId,
                eventType = FriendEventType.XP_MILESTONE,
                message = "Hit ${milestone} XP",
                eventKeySuffix = milestone.toString(),
            )
        }
    }

    suspend fun onLeagueRankImproved(
        studentId: String,
        previousRank: Int,
        newRank: Int,
    ) {
        if (studentId.isBlank()) return
        if (previousRank <= 0 || newRank <= 0) return
        if (newRank >= previousRank) return
        val jumped = previousRank - newRank
        if (jumped < MIN_RANK_JUMP) return
        friendRepository.publishFeedEvent(
            actorStudentId = studentId,
            eventType = FriendEventType.LEAGUE_RANK_JUMP,
            message = "Climbed to #$newRank in the league (+$jumped)",
            eventKeySuffix = "${LocalDate.now()}_$newRank",
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

    /**
     * Local-only: accepted demo bots post a few streak / topic / XP / rank updates
     * so Friends' updates isn't empty while waiting for real friends.
     */
    suspend fun simulateBotFriendFeedIfNeeded(studentId: String, force: Boolean = false): Int {
        if (studentId.isBlank()) return 0
        friendRepository.ensureActiveDemoFriends(studentId)
        val bots = friendRepository.getAcceptedDemoBots(studentId)
        if (bots.isEmpty()) return 0

        val dayKey = LocalDate.now().toString()
        if (!force && sharedPrefs.hasBotFriendFeedSimulatedForDay(dayKey)) return 0
        sharedPrefs.setBotFriendFeedSimulatedForDay(dayKey)

        val rng = Random(dayKey.hashCode() xor studentId.hashCode())
        var published = 0
        bots.take(3).forEachIndexed { index, bot ->
            val events = botEventBundle(dayKey, index, rng)
            events.forEach { (type, message, suffix) ->
                val ok =
                    friendRepository.deliverIncomingLocalFeed(
                        ownerStudentId = studentId,
                        fromStudentId = bot.friendStudentId,
                        fromDisplayName = bot.displayName,
                        eventType = type,
                        message = message,
                        eventKey = "${bot.friendStudentId}_${type.storageKey}_${dayKey}_$suffix",
                    )
                if (ok) published++
            }
        }
        return published
    }

    /** After accepting a bot request, drop a couple of welcome updates into the rail. */
    suspend fun seedFeedFromAcceptedBot(
        ownerStudentId: String,
        botId: String,
        botName: String,
    ) {
        if (ownerStudentId.isBlank() || !FriendRepository.isDemoBot(botId)) return
        val dayKey = LocalDate.now().toString()
        val bundle =
            listOf(
                Triple(
                    FriendEventType.STREAK_MILESTONE,
                    "Reached a 5-day streak",
                    "welcome_streak",
                ),
                Triple(
                    FriendEventType.TOPIC_COMPLETED,
                    "Completed ${TOPIC_TITLES.random()}",
                    "welcome_topic",
                ),
            )
        bundle.forEach { (type, message, suffix) ->
            friendRepository.deliverIncomingLocalFeed(
                ownerStudentId = ownerStudentId,
                fromStudentId = botId,
                fromDisplayName = botName,
                eventType = type,
                message = message,
                eventKey = "${botId}_${type.storageKey}_${dayKey}_$suffix",
            )
        }
    }

    private fun botEventBundle(
        dayKey: String,
        index: Int,
        rng: Random,
    ): List<Triple<FriendEventType, String, String>> {
        val streak = listOf(3, 5, 7, 14).random(rng)
        val topic = TOPIC_TITLES.random(rng)
        val xp = listOf(100, 250, 500, 750).random(rng)
        val rank = (1..8).random(rng)
        val jump = (2..5).random(rng)
        // Rotate so each bot leans on a different headline event first.
        val primary =
            when (index % 4) {
                0 ->
                    Triple(
                        FriendEventType.STREAK_MILESTONE,
                        "Reached a $streak-day streak",
                        "s$streak",
                    )
                1 ->
                    Triple(
                        FriendEventType.TOPIC_COMPLETED,
                        "Completed $topic",
                        "t${topic.hashCode()}",
                    )
                2 ->
                    Triple(
                        FriendEventType.XP_MILESTONE,
                        "Hit $xp XP",
                        "x$xp",
                    )
                else ->
                    Triple(
                        FriendEventType.LEAGUE_RANK_JUMP,
                        "Climbed to #$rank in the league (+$jump)",
                        "r$rank",
                    )
            }
        val secondary =
            Triple(
                FriendEventType.CONCEPT_MILESTONE,
                "Completed ${(3..12).random(rng) * CONCEPT_MILESTONE_STEP} concepts",
                "c$index",
            )
        return listOf(primary, secondary)
    }

    companion object {
        val STREAK_MILESTONES = setOf(3, 5, 7, 14, 21, 30)
        const val CONCEPT_MILESTONE_STEP = 3
        val XP_MILESTONES = listOf(100, 250, 500, 750, 1000, 1500, 2000)
        const val MIN_RANK_JUMP = 2
        private val TOPIC_TITLES =
            listOf(
                "Fractions",
                "Photosynthesis",
                "Integers",
                "Motion",
                "Algebra basics",
                "The solar system",
                "Ratio & proportion",
                "Acids & bases",
            )
    }
}
