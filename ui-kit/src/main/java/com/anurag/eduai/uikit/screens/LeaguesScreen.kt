package com.anurag.eduai.uikit.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anurag.eduai.uikit.components.LeagueHeroBanner
import com.anurag.eduai.uikit.components.LeagueParticipant
import com.anurag.eduai.uikit.components.LeagueZone
import com.anurag.eduai.uikit.components.LeaderboardRow
import com.anurag.eduai.uikit.components.SectionHeader
import com.anurag.eduai.uikit.components.ZoneDivider
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.anurag.eduai.uikit.theme.EduChipRole

data class LeagueUiState(
    val leagueName: String = "Silver League",
    val daysRemaining: Int = 3,
    val promotionCount: Int = 5,
    val demotionCount: Int = 3,
    val promotionTargetName: String = "Gold",
    val participants: List<LeagueParticipant> =
        listOf(
            LeagueParticipant(1, "Kabir", 812, streak = 21),
            LeagueParticipant(2, "Meher", 764, streak = 14),
            LeagueParticipant(3, "Rehan", 705, streak = 30),
            LeagueParticipant(4, "Ishaan", 640, streak = 5),
            LeagueParticipant(5, "Diya", 588, streak = 9),
            LeagueParticipant(6, "Aarav", 520, streak = 3),
            LeagueParticipant(7, "Aanya", 486, streak = 6, isCurrentUser = true),
            LeagueParticipant(8, "Priya", 455, streak = 2),
            LeagueParticipant(9, "Vihaan", 410, streak = 1),
            LeagueParticipant(10, "Sara", 372),
            LeagueParticipant(11, "Kian", 340),
            LeagueParticipant(12, "Nisha", 298),
            LeagueParticipant(13, "Arjun", 260),
            LeagueParticipant(14, "Zoya", 210),
            LeagueParticipant(15, "Farhan", 150),
        ),
)

/**
 * Full Leagues tab — hero banner (league + countdown + standing) followed by
 * a ranked leaderboard split into promotion / safe / demotion bands, mirroring
 * the prototype's league mechanic (spec §3.2 Leagues tab).
 */
@Composable
fun EduLeaguesScreen(
    state: LeagueUiState = LeagueUiState(),
    onParticipantClick: (LeagueParticipant) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    val currentRank = state.participants.firstOrNull { it.isCurrentUser }?.rank ?: state.participants.size
    val total = state.participants.size

    fun zoneFor(rank: Int): LeagueZone =
        when {
            rank <= state.promotionCount -> LeagueZone.Promotion
            rank > total - state.demotionCount -> LeagueZone.Demotion
            else -> LeagueZone.Safe
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.surface1)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .padding(bottom = 48.dp),
    ) {
        SectionHeader(title = "Leagues")
        LeagueHeroBanner(
            leagueName = state.leagueName,
            daysRemaining = state.daysRemaining,
            currentRank = currentRank,
            totalParticipants = total,
            promotionCount = state.promotionCount,
            promotionTargetName = state.promotionTargetName,
        )
        Spacer(modifier = Modifier.height(16.dp))

        var lastZone: LeagueZone? = null
        state.participants.forEach { participant ->
            val zone = zoneFor(participant.rank)
            if (zone != lastZone) {
                val label =
                    when (zone) {
                        LeagueZone.Promotion ->
                            "Promotion zone · advances to ${state.promotionTargetName}"
                        LeagueZone.Safe -> "Safe zone"
                        LeagueZone.Demotion -> "Demotion zone"
                    }
                val role =
                    when (zone) {
                        LeagueZone.Promotion -> EduChipRole.Success
                        LeagueZone.Safe -> EduChipRole.Neutral
                        LeagueZone.Demotion -> EduChipRole.Danger
                    }
                ZoneDivider(label = label, role = role)
                lastZone = zone
            }
            LeaderboardRow(
                participant = participant,
                zone = zone,
                onClick = onParticipantClick,
                modifier = Modifier.padding(vertical = 3.dp),
            )
        }
    }
}
