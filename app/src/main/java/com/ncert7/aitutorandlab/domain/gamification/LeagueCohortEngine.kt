package com.ncert7.aitutorandlab.domain.gamification

import com.ncert7.aitutorandlab.data.local.entities.LeagueMemberEntity
import kotlin.math.max
import kotlin.random.Random

object LeagueCohortEngine {

    fun buildCohortId(weekKey: String, tier: LeagueTier, studentId: String): String {
        val suffix = studentId.takeLast(6).ifBlank { "000000" }
        return "local_${weekKey}_${tier.storageKey}_$suffix"
    }

    fun generateBots(
        cohortId: String,
        weekKey: String,
        tier: LeagueTier,
        userWeeklyXp: Int,
        botCount: Int,
    ): List<LeagueMemberEntity> {
        if (botCount <= 0) return emptyList()
        val rng = Random(cohortId.hashCode())
        val median = max(userWeeklyXp, 120)
        val names = LeagueConfig.BOT_NAMES.shuffled(rng).take(botCount)
        return names.mapIndexed { index, name ->
            val factor = 0.55 + rng.nextDouble() * 0.45
            val xp = max(50, (median * factor).toInt() + rng.nextInt(-40, 80))
            LeagueMemberEntity(
                weekKey = weekKey,
                cohortId = cohortId,
                tier = tier.storageKey,
                memberId = "bot_${cohortId}_$index",
                displayName = name,
                weeklyXp = xp,
                streak = rng.nextInt(1, 25),
                isBot = true,
            )
        }
    }

    fun rankMembers(members: List<LeagueMemberEntity>, studentId: String): RankedCohort {
        val sorted =
            members.sortedWith(
                compareByDescending<LeagueMemberEntity> { it.weeklyXp }
                    .thenBy { it.displayName.lowercase() },
            )
        val ranked =
            sorted.mapIndexed { index, member ->
                RankedMember(
                    member = member,
                    rank = index + 1,
                    isCurrentUser = member.memberId == studentId,
                )
            }
        val userRank = ranked.firstOrNull { it.isCurrentUser }?.rank ?: ranked.size
        return RankedCohort(ranked, userRank)
    }
}

data class RankedMember(
    val member: LeagueMemberEntity,
    val rank: Int,
    val isCurrentUser: Boolean,
)

data class RankedCohort(
    val members: List<RankedMember>,
    val userRank: Int,
)
