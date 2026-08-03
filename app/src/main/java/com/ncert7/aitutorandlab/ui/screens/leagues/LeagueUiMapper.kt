package com.ncert7.aitutorandlab.ui.screens.leagues

import com.anurag.eduai.uikit.components.LeagueParticipant
import com.anurag.eduai.uikit.screens.LeagueUiState
import com.ncert7.aitutorandlab.domain.gamification.LeagueConfig
import com.ncert7.aitutorandlab.domain.gamification.LeagueTier
import com.ncert7.aitutorandlab.repository.LeagueBoardState

object LeagueUiMapper {

    fun toUiState(board: LeagueBoardState): LeagueUiState {
        val promotionTarget =
            board.tier.promotionTarget()?.displayName() ?: board.tier.displayName()
        return LeagueUiState(
            leagueName = board.tier.leagueTitle(),
            daysRemaining = board.daysRemaining,
            promotionCount = LeagueConfig.PROMOTION_COUNT,
            demotionCount = LeagueConfig.DEMOTION_COUNT,
            promotionTargetName = promotionTarget,
            participants =
                board.rankedCohort.members.map { ranked ->
                    LeagueParticipant(
                        rank = ranked.rank,
                        name = ranked.member.displayName,
                        xp = ranked.member.weeklyXp,
                        streak = ranked.member.streak,
                        isCurrentUser = ranked.isCurrentUser,
                    )
                },
        )
    }
}
