package com.anurag.eduai.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.avatar.SavedTutorAvatar
import com.anurag.eduai.uikit.avatar.core.AvatarState
import com.anurag.eduai.uikit.theme.EduChipRole
import com.anurag.eduai.uikit.theme.EduAiTheme

@Composable
fun TopBarChips(
    greeting: String,
    userName: String,
    streak: Int,
    gems: Int,
    leagueName: String,
    leagueRank: Int,
    showFriendDot: Boolean = false,
    showGemsDot: Boolean = false,
    showLeagueDot: Boolean = false,
    onProfileClick: () -> Unit = {},
    onStreakClick: () -> Unit = {},
    onGemsClick: () -> Unit = {},
    onLeagueClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    val flamePulse = rememberPulseScale(min = 1f, max = 1.16f, durationMillis = 1000)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.pressScaleClickable(onClick = onProfileClick, pressedScale = 0.97f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    Box(
                        modifier =
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.accent),
                    ) {
                        SavedTutorAvatar(
                            state = AvatarState.Happy,
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                    if (showFriendDot) {
                        NotificationDot(
                            modifier = Modifier.align(Alignment.TopEnd),
                            size = 10.dp,
                            borderColor = colors.surface1,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = greeting, color = colors.textSecondary, fontSize = 12.sp)
                    Text(
                        text = userName,
                        color = colors.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Row(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EduChip(
                    label = "$streak",
                    role = EduChipRole.Warning,
                    leading = {
                        Box(modifier = Modifier.size(16.dp)) {
                            EduLottie(
                                resId = com.anurag.eduai.uikit.R.raw.eduai_flame,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Icon(
                                    Icons.Outlined.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = colors.warning,
                                    modifier = Modifier.size(13.dp).scale(flamePulse),
                                )
                            }
                        }
                    },
                    onClick = onStreakClick,
                    labelContent = { fg ->
                        AnimatedCounterText(
                            value = streak,
                            color = fg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                )
                EduChip(
                    label = "$gems",
                    role = EduChipRole.Pro,
                    showNotificationDot = showGemsDot,
                    leading = {
                        Icon(
                            Icons.Outlined.WorkspacePremium,
                            contentDescription = null,
                            tint = colors.pro,
                            modifier = Modifier.size(13.dp),
                        )
                    },
                    onClick = onGemsClick,
                    labelContent = { fg ->
                        AnimatedCounterText(
                            value = gems,
                            color = fg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                )
                EduChip(
                    modifier =
                        Modifier
                            .zIndex(2f)
                            .defaultMinSize(minHeight = 36.dp),
                    label = leagueRankLabel(leagueName, leagueRank),
                    role = EduChipRole.Neutral,
                    showNotificationDot = showLeagueDot,
                    leading = {
                        Icon(
                            Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(12.dp),
                        )
                    },
                    onClick = onLeagueClick,
                    labelContent = { fg ->
                        Text(
                            text = leagueRankLabel(leagueName, leagueRank),
                            color = fg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
    }
}

/** Compact rank chip — weekly XP lives in the reward overlay, not the top bar. */
private fun leagueRankLabel(leagueName: String, leagueRank: Int): String =
    if (leagueRank > 0) "$leagueName · #$leagueRank" else leagueName
