package com.ncert7.aitutorandlab.ui.screens.leagues

import com.anurag.eduai.uikit.components.LeagueParticipant
import com.anurag.eduai.uikit.screens.LeagueUiState
import com.ncert7.aitutorandlab.domain.gamification.LeagueConfig
import com.ncert7.aitutorandlab.repository.LeagueBoardState
import com.ncert7.aitutorandlab.utils.LeaguesCopyFactory

object LeagueUiMapper {

    fun toUiState(
        board: LeagueBoardState,
        languageCode: String = "en",
    ): LeagueUiState {
        val promotionTargetTier = board.tier.promotionTarget() ?: board.tier
        return LeagueUiState(
            leagueName = LeaguesCopyFactory.leagueTitle(board.tier, languageCode),
            daysRemaining = board.daysRemaining,
            promotionCount = LeagueConfig.PROMOTION_COUNT,
            demotionCount = LeagueConfig.DEMOTION_COUNT,
            promotionTargetName = LeaguesCopyFactory.tierDisplayName(promotionTargetTier, languageCode),
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
