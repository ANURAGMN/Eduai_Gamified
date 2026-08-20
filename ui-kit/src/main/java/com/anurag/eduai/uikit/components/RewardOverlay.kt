package com.anurag.eduai.uikit.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.R
import com.anurag.eduai.uikit.avatar.SavedTutorAvatar
import com.anurag.eduai.uikit.avatar.core.AvatarState
import com.anurag.eduai.uikit.theme.EduAiTheme

/**
 * Full-screen celebration shown when the learner finishes today's focus:
 * confetti rain, a success animation, an XP bar that fills, and reward numbers
 * that count up. Fires the escalating reward haptic + chime on appear.
 */
@Composable
fun RewardOverlay(
    visible: Boolean,
    xpEarned: Int,
    gemsEarned: Int,
    xpFrom: Float,
    xpTo: Float,
    weeklyXpTotal: Int = 0,
    weeklyXpTarget: Int = 500,
    onCollect: () -> Unit,
    copy: RewardOverlayCopy = defaultRewardOverlayCopy(),
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    val feedback = rememberEduFeedback()
    val confettiColors =
        listOf(colors.accent, colors.pro, colors.success, colors.warning, colors.danger)

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
    ) {
        var started by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            feedback.reward()
            started = true
        }
        val barFill by
            animateFloatAsState(
                targetValue = if (started) xpTo.coerceIn(0f, 1f) else xpFrom.coerceIn(0f, 1f),
                animationSpec = tween(900, delayMillis = 250),
                label = "xpBar",
            )

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            ConfettiRain(active = visible, colors = confettiColors)

            AnimatedVisibility(
                visible = started,
                enter =
                    fadeIn(tween(200)) +
                        scaleIn(
                            initialScale = 0.7f,
                            animationSpec =
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow,
                                ),
                        ),
            ) {
                Column(
                    modifier =
                        Modifier
                            .padding(horizontal = 32.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.surface1)
                            .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Your saved tutor celebrates with you.
                    SavedTutorAvatar(
                        state = AvatarState.Celebrating,
                        modifier = Modifier.size(84.dp),
                    )

                    // Success animation (Lottie) — falls back to a bouncy check icon.
                    val checkPulse = rememberPulseScale(min = 0.94f, max = 1.06f, durationMillis = 900)
                    EduLottie(
                        resId = R.raw.eduai_success,
                        modifier = Modifier.size(96.dp),
                        iterations = 1,
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = colors.success,
                            modifier = Modifier.size(88.dp).scale(checkPulse),
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = copy.title,
                        color = colors.text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = copy.subtitle,
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // XP bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = copy.weeklyXpLabel,
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "$weeklyXpTotal / $weeklyXpTarget",
                            color = colors.accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.border),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth(barFill)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.accent),
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RewardStat(
                            label = copy.xpEarnedLabel,
                            value = xpEarned,
                            prefix = "+",
                            valueColor = colors.accent,
                            modifier = Modifier.weight(1f),
                        )
                        RewardStat(
                            label = copy.gemsLabel,
                            value = gemsEarned,
                            prefix = "+",
                            valueColor = colors.pro,
                            leadingIcon = true,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    EduPrimaryButton(
                        text = copy.collectCta,
                        onClick = onCollect,
                        fillMaxWidth = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun RewardStat(
    label: String,
    value: Int,
    prefix: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    leadingIcon: Boolean = false,
) {
    val colors = EduAiTheme.colors
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface2)
                .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon) {
                Icon(
                    Icons.Outlined.Diamond,
                    contentDescription = null,
                    tint = valueColor,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.size(3.dp))
            }
            AnimatedCounterText(
                value = value,
                color = valueColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                prefix = prefix,
                durationMillis = 1100,
            )
        }
        Text(text = label, color = colors.textMuted, fontSize = 11.sp)
    }
}
