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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.avatar.SavedTutorAvatar
import com.anurag.eduai.uikit.avatar.core.AvatarState
import com.anurag.eduai.uikit.theme.EduAiDimens
import com.anurag.eduai.uikit.theme.EduAiTheme

/**
 * Single Home spotlight: tutor avatar + today's focus + Start CTA in one row.
 * Replaces the stacked [HeroFocusCard] / [HomeTutorBubble] pair.
 */
@Composable
fun HomeFocusStrip(
    eyebrow: String,
    title: String,
    subtitle: String,
    buttonLabel: String,
    onStartClick: () -> Unit,
    onTutorClick: () -> Unit,
    modifier: Modifier = Modifier,
    todayDone: Boolean = false,
    xpEarned: Int = 0,
) {
    val colors = EduAiTheme.colors
    val wash =
        if (todayDone) {
            Brush.linearGradient(listOf(colors.successBg, colors.successBg))
        } else {
            Brush.linearGradient(listOf(colors.accentBg, colors.proBg))
        }
    val eyebrowColor = if (todayDone) colors.success else colors.accent
    val avatarMood = if (todayDone) AvatarState.Happy else AvatarState.Idle

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(EduAiDimens.cardRadius))
                .background(wash)
                .then(
                    if (todayDone) {
                        Modifier.shimmer(highlight = colors.success.copy(alpha = 0.18f))
                    } else {
                        Modifier.shimmer()
                    },
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (todayDone) colors.success.copy(alpha = 0.15f) else colors.surface1)
                    .pressScaleClickable(onClick = onTutorClick, pressedScale = 0.94f),
        ) {
            SavedTutorAvatar(
                state = avatarMood,
                modifier = Modifier.matchParentSize(),
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .pressScaleClickable(onClick = onStartClick, pressedScale = 0.98f),
            verticalArrangement = Arrangement.Center,
        ) {
            if (todayDone) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 2.dp),
                ) {
                    Text(
                        text = "All done · +",
                        color = eyebrowColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    AnimatedCounterText(
                        value = xpEarned,
                        color = eyebrowColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = " XP",
                        color = eyebrowColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                Text(
                    text = eyebrow,
                    color = eyebrowColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            Text(
                text = title,
                color = colors.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (todayDone) {
            EduSecondaryButton(text = buttonLabel, onClick = onStartClick, fillMaxWidth = false)
        } else {
            EduPrimaryButton(text = buttonLabel, onClick = onStartClick, fillMaxWidth = false)
        }
    }
}
