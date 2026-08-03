package com.anurag.eduai.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.theme.EduAiDimens
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.anurag.eduai.uikit.theme.EduChipRole
import com.anurag.eduai.uikit.theme.forRole

/** Which band of the leaderboard a participant falls into this cycle. */
enum class LeagueZone { Promotion, Safe, Demotion }

data class LeagueParticipant(
    val rank: Int,
    val name: String,
    val xp: Int,
    val streak: Int = 0,
    val isCurrentUser: Boolean = false,
)

/**
 * Top banner for the Leagues screen — league name, countdown, and the user's
 * standing. Uses the `pro` accent (amber) to read as a "premium" ranked mode,
 * distinct from the `accent` (blue) used for everyday learning surfaces.
 */
@Composable
fun LeagueHeroBanner(
    leagueName: String,
    daysRemaining: Int,
    currentRank: Int,
    totalParticipants: Int,
    promotionCount: Int = 5,
    promotionTargetName: String = "Gold",
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(EduAiDimens.heroRadius))
                .background(colors.pro)
                .padding(EduAiDimens.cardPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = leagueName,
                    color = colors.onAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = if (daysRemaining == 1) "1 day left" else "$daysRemaining days left",
                color = colors.onAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text =
                "You're rank $currentRank of $totalParticipants — top $promotionCount promote to $promotionTargetName",
            color = colors.onAccent.copy(alpha = 0.9f),
            fontSize = 12.sp,
        )
    }
}

/** Thin labeled rule that separates the promotion / safe / demotion bands. */
@Composable
fun ZoneDivider(
    label: String,
    role: EduChipRole,
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    val (fg, _) = colors.forRole(role)
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.border))
        Text(text = label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.border))
    }
}

/** Single leaderboard row: rank/medal, avatar initial, name + streak, XP total. */
@Composable
fun LeaderboardRow(
    participant: LeagueParticipant,
    zone: LeagueZone,
    onClick: (LeagueParticipant) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    val rowBg =
        when {
            participant.isCurrentUser -> colors.accentBg
            zone == LeagueZone.Promotion -> colors.successBg.copy(alpha = 0.5f)
            zone == LeagueZone.Demotion -> colors.dangerBg.copy(alpha = 0.5f)
            else -> Color.Transparent
        }
    val medalColor =
        when (participant.rank) {
            1 -> colors.pro
            2 -> colors.textSecondary
            3 -> colors.warning
            else -> null
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(EduAiDimens.buttonRadius))
                .background(rowBg)
                .then(
                    if (participant.isCurrentUser) {
                        Modifier.border(1.dp, colors.accent, RoundedCornerShape(EduAiDimens.buttonRadius))
                    } else {
                        Modifier
                    },
                )
                .pressScaleClickable(onClick = { onClick(participant) }, pressedScale = 0.97f)
                .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            if (medalColor != null) {
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = medalColor,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text(
                    text = "${participant.rank}",
                    color = colors.textMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (participant.isCurrentUser) colors.accent else colors.surface1),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = participant.name.take(1).uppercase(),
                color = if (participant.isCurrentUser) colors.onAccent else colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (participant.isCurrentUser) "${participant.name} (You)" else participant.name,
                color = colors.text,
                fontSize = 13.sp,
                fontWeight = if (participant.isCurrentUser) FontWeight.SemiBold else FontWeight.Normal,
            )
            if (participant.streak > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.LocalFireDepartment,
                        contentDescription = null,
                        tint = colors.warning,
                        modifier = Modifier.size(11.dp),
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = "${participant.streak} day streak", color = colors.textMuted, fontSize = 10.sp)
                }
            }
        }
        Text(
            text = "${participant.xp} XP",
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
