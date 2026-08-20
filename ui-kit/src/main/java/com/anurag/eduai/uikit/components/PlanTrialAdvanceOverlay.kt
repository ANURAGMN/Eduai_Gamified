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
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.anurag.eduai.uikit.R
import com.anurag.eduai.uikit.avatar.SavedTutorAvatar
import com.anurag.eduai.uikit.avatar.core.AvatarState
import com.anurag.eduai.uikit.theme.EduAiTheme

/**
 * Celebratory welcome before auto-launching the next trial item.
 * Countdown pauses while the app is in the background or a mandatory ad claim is pending.
 */
@Composable
fun PlanTrialAdvanceOverlay(
    visible: Boolean,
    title: String,
    subtitle: String,
    xpEarned: Int = 0,
    gemsEarned: Int = 0,
    bonusXpEarned: Int = 0,
    xpBarFrom: Float = 0f,
    xpBarTo: Float = 0f,
    weeklyXpTotal: Int = 0,
    weeklyXpTarget: Int = 500,
    requiresMandatoryClaim: Boolean = false,
    mandatoryGemsReward: Int = 0,
    mandatoryClaimCompleted: Boolean = true,
    mandatoryAdSkipped: Boolean = false,
    doubleXpAmount: Int = 0,
    doubleXpClaimed: Boolean = false,
    adReady: Boolean = false,
    totalSeconds: Int = 3,
    labels: PlanTrialOverlayLabels = PlanTrialOverlayLabels(),
    onWatchMandatoryAd: () -> Unit = {},
    onSkipMandatoryAd: () -> Unit = {},
    onWatchDoubleXpAd: () -> Unit = {},
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    val feedback = rememberEduFeedback()
    val confettiColors =
        listOf(colors.accent, colors.pro, colors.success, colors.warning, colors.danger)
    val processLifecycleOwner = ProcessLifecycleOwner.get()
    var paused by remember { mutableStateOf(false) }
    var secondsLeft by remember(visible, title, mandatoryClaimCompleted, mandatoryAdSkipped) {
        mutableIntStateOf(if (mandatoryAdSkipped) 5 else totalSeconds)
    }
    var confettiTrigger by remember { mutableIntStateOf(0) }
    var celebrationStarted by remember(visible, title) { mutableStateOf(false) }
    val showRewards = xpEarned > 0 || gemsEarned > 0 || bonusXpEarned > 0
    val canAutoAdvance = !requiresMandatoryClaim || mandatoryClaimCompleted

    DisposableEffect(processLifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                paused = event == Lifecycle.Event.ON_STOP
            }
        processLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { processLifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(visible, title, canAutoAdvance, mandatoryAdSkipped) {
        if (!visible || !canAutoAdvance) return@LaunchedEffect
        secondsLeft = if (mandatoryAdSkipped) 5 else totalSeconds
        while (secondsLeft > 0) {
            if (!paused) {
                kotlinx.coroutines.delay(1_000)
                secondsLeft -= 1
            } else {
                kotlinx.coroutines.delay(100)
            }
        }
        onFinished()
    }

    LaunchedEffect(visible, title) {
        if (!visible) return@LaunchedEffect
        celebrationStarted = false
        confettiTrigger += 1
        feedback.reward()
        celebrationStarted = true
    }

    val barFill by
        animateFloatAsState(
            targetValue = if (celebrationStarted) xpBarTo.coerceIn(0f, 1f) else xpBarFrom.coerceIn(0f, 1f),
            animationSpec = tween(900, delayMillis = 250),
            label = "trialXpBar",
        )

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            ConfettiRain(active = visible, colors = confettiColors)
            ConfettiBurst(
                trigger = confettiTrigger,
                colors = confettiColors,
                particleCount = 52,
                originFraction = Offset(0.5f, 0.38f),
            )

            AnimatedVisibility(
                visible = celebrationStarted,
                enter =
                    fadeIn(tween(200)) +
                        scaleIn(
                            initialScale = 0.72f,
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
                    SavedTutorAvatar(
                        state = AvatarState.Celebrating,
                        modifier = Modifier.size(88.dp),
                    )
                    val checkPulse = rememberPulseScale(min = 0.94f, max = 1.06f, durationMillis = 900)
                    EduLottie(
                        resId = R.raw.eduai_success,
                        modifier = Modifier.size(72.dp),
                        iterations = 1,
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = colors.success,
                            modifier = Modifier.size(64.dp).scale(checkPulse),
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title,
                        color = colors.text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = subtitle,
                        color = if (mandatoryAdSkipped) colors.danger else colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (mandatoryAdSkipped) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp),
                    )

                    if (showRewards) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = labels.weeklyXp,
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
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(colors.border),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth(barFill)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(colors.accent),
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (xpEarned > 0) {
                                TrialRewardStat(
                                    label = labels.xpEarned,
                                    value = xpEarned,
                                    valueColor = colors.accent,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (bonusXpEarned > 0) {
                                TrialRewardStat(
                                    label = labels.bonusXp,
                                    value = bonusXpEarned,
                                    valueColor = colors.success,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (gemsEarned > 0) {
                                TrialRewardStat(
                                    label = labels.gems,
                                    value = gemsEarned,
                                    valueColor = colors.pro,
                                    leadingIcon = true,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    if (requiresMandatoryClaim && !mandatoryClaimCompleted) {
                        Spacer(modifier = Modifier.height(16.dp))
                        EduSecondaryButton(
                            text =
                                if (adReady) {
                                    labels.mandatoryAdWatch(mandatoryGemsReward)
                                } else {
                                    labels.loadingRewardAd
                                },
                            onClick = onWatchMandatoryAd,
                            enabled = adReady,
                            fillMaxWidth = true,
                        )
                        Text(
                            text = labels.skipMandatoryAd(mandatoryGemsReward),
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier =
                                Modifier
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onSkipMandatoryAd)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }

                    if (doubleXpAmount > 0 && !doubleXpClaimed && (!requiresMandatoryClaim || mandatoryClaimCompleted)) {
                        Spacer(modifier = Modifier.height(10.dp))
                        EduSecondaryButton(
                            text =
                                if (adReady) {
                                    labels.doubleXpWatch(doubleXpAmount)
                                } else {
                                    labels.doubleXpLoading
                                },
                            onClick = onWatchDoubleXpAd,
                            enabled = adReady,
                            fillMaxWidth = true,
                        )
                    } else if (doubleXpClaimed && bonusXpEarned > 0) {
                        Text(
                            text = labels.bonusXpAdded(bonusXpEarned),
                            color = colors.success,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }

                    if (canAutoAdvance && secondsLeft > 0) {
                        Text(
                            text = labels.startingIn(secondsLeft),
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrialRewardStat(
    label: String,
    value: Int,
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
                .padding(vertical = 10.dp),
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
                prefix = "+",
                durationMillis = 900,
            )
        }
        Text(text = label, color = colors.textMuted, fontSize = 11.sp)
    }
}
