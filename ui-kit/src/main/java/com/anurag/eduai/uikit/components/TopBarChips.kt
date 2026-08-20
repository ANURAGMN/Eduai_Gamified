package com.anurag.eduai.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.avatar.SavedTutorAvatar
import com.anurag.eduai.uikit.avatar.core.AvatarState
import com.anurag.eduai.uikit.theme.EduAiDimens
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.anurag.eduai.uikit.theme.EduChipRole

@Composable
fun TopBarChips(
    greeting: String,
    userName: String,
    streak: Int,
    gems: Int,
    leagueName: String,
    leagueRank: Int,
    streakCaption: String = "Streak",
    gemsCaption: String = "Gems",
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
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .pressScaleClickable(onClick = onProfileClick, pressedScale = 0.97f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    Box(
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
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = greeting,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = userName,
                        color = colors.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                LabeledStatChip(
                    caption = streakCaption,
                    onClick = onStreakClick,
                ) {
                    EduChip(
                        label = "$streak",
                        role = EduChipRole.Warning,
                        leading = {
                            Box(Modifier.size(16.dp)) {
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
                }
                LabeledStatChip(
                    caption = gemsCaption,
                    onClick = onGemsClick,
                ) {
                    EduChip(
                        label = "$gems",
                        role = EduChipRole.Pro,
                        showNotificationDot = showGemsDot,
                        leading = {
                            Icon(
                                Icons.Outlined.Diamond,
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
                }
                LabeledStatChip(
                    caption = leagueName,
                    captionColor = leagueTierAccent(leagueName),
                    onClick = onLeagueClick,
                ) {
                    LeagueRankChip(
                        leagueName = leagueName,
                        leagueRank = leagueRank,
                        showNotificationDot = showLeagueDot,
                        onClick = onLeagueClick,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun LabeledStatChip(
    caption: String,
    onClick: () -> Unit,
    captionColor: Color? = null,
    chip: @Composable () -> Unit,
) {
    val colors = EduAiTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.pressScaleClickable(onClick = onClick, pressedScale = 0.97f),
    ) {
        chip()
        Text(
            text = caption,
            color = captionColor ?: colors.textMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .padding(top = 3.dp)
                    .widthIn(max = 56.dp),
        )
    }
}

/**
 * Compact league chip: tier-colored shield + rank only (e.g. #4).
 * Tier name sits as a small caption under the chip.
 */
@Composable
private fun LeagueRankChip(
    leagueName: String,
    leagueRank: Int,
    showNotificationDot: Boolean,
    onClick: () -> Unit,
) {
    val colors = EduAiTheme.colors
    val accent = leagueTierAccent(leagueName)
    val label = if (leagueRank > 0) "#$leagueRank" else "—"
    val contentDescription =
        if (leagueRank > 0) {
            "$leagueName, rank $leagueRank"
        } else {
            leagueName
        }

    Box {
        Row(
            Modifier
                .widthIn(min = 44.dp)
                .clip(RoundedCornerShape(EduAiDimens.chipRadius))
                .background(accent.copy(alpha = 0.14f))
                .pressScaleClickable(onClick = onClick, pressedScale = 0.93f)
                .padding(horizontal = 9.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Shield,
                contentDescription = contentDescription,
                tint = accent,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = label,
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
        if (showNotificationDot) {
            NotificationDot(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 2.dp),
                borderColor = colors.surface1,
            )
        }
    }
}

private fun leagueTierAccent(leagueName: String): Color {
    val key = leagueName.trim().lowercase()
    return when {
        key.startsWith("gold") || key.startsWith("ಚಿನ್ನ") -> Color(0xFFC9A227)
        key.startsWith("silver") || key.startsWith("ಬೆಳ್ಳಿ") -> Color(0xFF7A8494)
        else -> Color(0xFFB87333)
    }
}
