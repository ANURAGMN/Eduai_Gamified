package com.anurag.eduai.uikit.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.R
import com.anurag.eduai.uikit.avatar.EduTutorAvatar
import com.anurag.eduai.uikit.avatar.TutorConfig
import com.anurag.eduai.uikit.avatar.core.AvatarState
import com.anurag.eduai.uikit.avatar.core.EmotionType
import com.anurag.eduai.uikit.theme.EduAiTheme

/**
 * A single full-screen "moment" surface used for both celebrations and nudges.
 * The [avatar] look and [emotion] are chosen at random by the caller so each appearance feels fresh.
 *
 * Nudges (exit/comeback) are a plain dialog: scrim + white card + avatar + copy + buttons.
 * Celebrations add confetti rain + burst, avatar halo, Lottie pop, and +XP/+gems count-up
 * on the same clean white card.
 */
@Composable
fun MomentOverlay(
    visible: Boolean,
    celebratory: Boolean,
    avatar: TutorConfig,
    emotion: EmotionType,
    headline: String,
    body: String,
    primaryCta: String,
    onPrimary: () -> Unit,
    secondaryCta: String? = null,
    onSecondary: () -> Unit = {},
    gems: Int = 0,
    xp: Int = 0,
    xpLabel: String = "XP",
    gemsLabel: String = "gems",
    illustration: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    val feedback = rememberEduFeedback()

    val celebrationAccent = colors.pro
    val confettiColors =
        listOf(colors.accent, colors.pro, colors.success, colors.warning, colors.danger)

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
    ) {
        var started by remember { mutableStateOf(false) }
        var burst by remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) {
            if (celebratory) feedback.reward() else feedback.tap()
            started = true
            burst = 1
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.62f)),
            contentAlignment = Alignment.Center,
        ) {
            val haloPulse = rememberPulseScale(min = 0.92f, max = 1.12f, durationMillis = 1400)

            if (celebratory) {
                ConfettiRain(active = visible, colors = confettiColors)
            }

            AnimatedVisibility(
                visible = started,
                enter =
                    fadeIn(tween(220)) +
                        scaleIn(
                            initialScale = 0.6f,
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
                            .padding(horizontal = 28.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(colors.surface1)
                            .padding(horizontal = 26.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Avatar with a pulsing glow halo (+ a Lottie burst for celebrations),
                    // or a custom illustration with the avatar as speaker below it.
                    if (illustration != null) {
                        illustration()
                        Spacer(modifier = Modifier.height(12.dp))
                        EduTutorAvatar(
                            character = avatar.character,
                            state = emotion.toAvatarState(),
                            modifier = Modifier.size(88.dp),
                            outfitVariant = avatar.outfit,
                            hairStyle = avatar.hair,
                            hairColor = avatar.hairColor,
                            glassesStyle = avatar.glasses,
                            glassesColor = avatar.frameColor,
                            neckStyle = avatar.neck,
                            underEyeLine = avatar.eyeLine,
                            cheekShading = avatar.cheeks,
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            if (celebratory) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(150.dp)
                                            .scale(haloPulse)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    colors =
                                                        listOf(
                                                            celebrationAccent.copy(alpha = 0.45f),
                                                            Color.Transparent,
                                                        ),
                                                ),
                                            ),
                                )
                                EduLottie(
                                    resId = R.raw.eduai_quest_bonus,
                                    modifier = Modifier.size(196.dp),
                                    iterations = 1,
                                )
                            }
                            EduTutorAvatar(
                                character = avatar.character,
                                state = emotion.toAvatarState(),
                                modifier = Modifier.size(116.dp),
                                outfitVariant = avatar.outfit,
                                hairStyle = avatar.hair,
                                hairColor = avatar.hairColor,
                                glassesStyle = avatar.glasses,
                                glassesColor = avatar.frameColor,
                                neckStyle = avatar.neck,
                                underEyeLine = avatar.eyeLine,
                                cheekShading = avatar.cheeks,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = headline,
                        color = if (celebratory) celebrationAccent else colors.text,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = body,
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )

                    if (celebratory && (gems > 0 || xp > 0)) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (xp > 0) {
                                RewardChip(value = xp, label = xpLabel, valueColor = colors.accent)
                            }
                            if (gems > 0) {
                                RewardChip(value = gems, label = gemsLabel, valueColor = colors.pro)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    EduPrimaryButton(
                        text = primaryCta,
                        onClick = onPrimary,
                        fillMaxWidth = true,
                    )
                    if (secondaryCta != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        EduGhostButton(
                            text = secondaryCta,
                            onClick = onSecondary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // A one-shot confetti burst from the avatar area on appear (celebrations only).
            if (celebratory) {
                ConfettiBurst(
                    trigger = burst,
                    colors = confettiColors,
                    particleCount = 42,
                    originFraction = Offset(0.5f, 0.4f),
                )
            }
        }
    }
}

/** A small pill that counts a reward up from zero. */
@Composable
private fun RewardChip(
    value: Int,
    label: String,
    valueColor: Color,
) {
    val colors = EduAiTheme.colors
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(valueColor.copy(alpha = 0.14f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedCounterText(
            value = value,
            color = valueColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            prefix = "+",
            durationMillis = 1100,
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Maps the richer [EmotionType] palette onto the animation engine's [AvatarState]. */
private fun EmotionType.toAvatarState(): AvatarState =
    when (this) {
        EmotionType.Neutral -> AvatarState.Idle
        EmotionType.Teaching -> AvatarState.Explaining
        EmotionType.Happy -> AvatarState.Happy
        EmotionType.Confused -> AvatarState.Confused
        EmotionType.Surprised -> AvatarState.Celebrating
        EmotionType.Celebrating -> AvatarState.Celebrating
        EmotionType.Explaining -> AvatarState.Explaining
    }
