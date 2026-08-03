package com.anurag.eduai.uikit.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.theme.EduAiDimens
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.anurag.eduai.uikit.theme.EduChipRole
import com.anurag.eduai.uikit.theme.forRole

/** A personal milestone shown in the Phase-1 "Your week" rail. */
data class ProgressMoment(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val role: EduChipRole,
)

/**
 * Phase-1 replacement for the "Friends' updates" rail — no social graph required.
 * Shows the learner's own streak/milestones, a peek at their (anonymous) league
 * standing, and an "Invite friends" card that fires the OS share. When the friend
 * graph + referral infra land in Phase 2, swap this for the real friends' feed.
 */
@Composable
fun HomeProgressRail(
    streak: Int,
    leagueName: String,
    leagueRank: Int,
    promoteCount: Int = 5,
    extraMoments: List<ProgressMoment> = emptyList(),
    onSeeAll: () -> Unit = {},
    onLeagueClick: () -> Unit = {},
    onInvite: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        SectionHeader(title = "Your week", seeAllLabel = "See all", onSeeAllClick = onSeeAll)
        HorizontalRail {
            MomentCard(
                title = if (streak == 1) "1-day streak" else "$streak-day streak",
                subtitle = "Keep it alive today",
                icon = Icons.Outlined.LocalFireDepartment,
                role = EduChipRole.Warning,
            )

            extraMoments.forEach { m ->
                MomentCard(title = m.title, subtitle = m.subtitle, icon = m.icon, role = m.role)
            }

            MomentCard(
                title =
                    if (leagueRank > 0) {
                        "$leagueName · Rank $leagueRank"
                    } else {
                        leagueName
                    },
                subtitle = "Top $promoteCount promote · tap to open",
                icon = Icons.Outlined.EmojiEvents,
                role = EduChipRole.Pro,
                onClick = onLeagueClick,
            )

            InviteCard(onInvite = onInvite)
        }
    }
}

@Composable
private fun MomentCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    role: EduChipRole,
    onClick: (() -> Unit)? = null,
) {
    val colors = EduAiTheme.colors
    val (fg, bg) = colors.forRole(role)
    RailCard(onClick = onClick ?: {}, modifier = Modifier.width(150.dp)) {
        IconBubble(icon = icon, fg = fg, bg = bg)
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = title, color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = subtitle,
            color = colors.textSecondary,
            fontSize = 11.sp,
            lineHeight = 14.sp,
        )
    }
}

@Composable
private fun InviteCard(onInvite: () -> Unit) {
    val colors = EduAiTheme.colors
    RailCard(onClick = onInvite, modifier = Modifier.width(150.dp)) {
        IconBubble(icon = Icons.Outlined.PersonAddAlt, fg = colors.accent, bg = colors.accentBg)
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "Invite friends", color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(text = "Learn together", color = colors.textSecondary, fontSize = 11.sp, lineHeight = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(EduAiDimens.chipRadius))
                    .background(colors.accent)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Share", color = colors.onAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun IconBubble(
    icon: ImageVector,
    fg: androidx.compose.ui.graphics.Color,
    bg: androidx.compose.ui.graphics.Color,
) {
    Box(
        modifier = Modifier.size(30.dp).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(15.dp))
    }
}
