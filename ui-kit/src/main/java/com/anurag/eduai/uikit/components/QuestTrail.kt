package com.anurag.eduai.uikit.components

import android.graphics.PathMeasure
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.RawRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.airbnb.lottie.compose.LottieConstants
import com.anurag.eduai.uikit.R
import com.anurag.eduai.uikit.theme.EduAiTheme
import kotlin.math.sin

data class QuestTrailState(
    val simsDone: Int = 0,
    val simsTotal: Int = 3,
    val simsClaimed: Boolean = false,
    val simsLabelPrefix: String = "Sims · ",
    val studyDone: Int = 0,
    val studyTotal: Int = 1,
    val studyClaimed: Boolean = false,
    val studyLabelPrefix: String = "Plan task · ",
    val bonusClaimed: Boolean = false,
) {
    val quizDone: Int get() = studyDone
    val quizTotal: Int get() = studyTotal
    val quizClaimed: Boolean get() = studyClaimed
}

@Composable
fun QuestTrail(
    state: QuestTrailState,
    onSeeAll: () -> Unit = {},
    onSimsClick: () -> Unit = {},
    onStudyClick: () -> Unit = {},
    onQuizClick: () -> Unit = onStudyClick,
    onBonusClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    val feedback = rememberEduFeedback(soundEnabled = true)
    val simsComplete = state.simsTotal > 0 && state.simsDone >= state.simsTotal
    val studyComplete = state.studyTotal > 0 && state.studyDone >= state.studyTotal
    val simsClaimable = simsComplete && !state.simsClaimed
    val studyClaimable = studyComplete && !state.studyClaimed
    val bonusUnlockable =
        state.simsTotal > 0 &&
            state.studyTotal > 0 &&
            simsComplete &&
            studyComplete &&
            !state.bonusClaimed

    var confettiTrigger by remember { mutableIntStateOf(0) }
    var confettiOrigin by remember { mutableStateOf(Offset(0.5f, 0.45f)) }
    var confettiParticles by remember { mutableIntStateOf(34) }
    val confettiColors =
        listOf(colors.accent, colors.pro, colors.success, colors.warning, colors.danger)

    val trailTarget =
        when {
            state.bonusClaimed -> 1f
            studyComplete -> 0.72f
            simsComplete -> 0.38f
            state.simsTotal > 0 && state.simsDone > 0 ->
                0.12f + 0.26f * (state.simsDone.toFloat() / state.simsTotal.coerceAtLeast(1))
            else -> 0f
        }
    val trailProgress by
        animateFloatAsState(
            targetValue = trailTarget,
            animationSpec = tween(800),
            label = "questTrail",
        )

    val pathWave = rememberInfiniteTransition(label = "questPathWave")
    val wavePhase by
        pathWave.animateFloat(
            initialValue = 0f,
            targetValue = (Math.PI * 2).toFloat(),
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 2500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "wavePhase",
        )
    val waveTravel by
        pathWave.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 2500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "waveTravel",
        )

    fun celebrate(
        origin: Offset,
        isClaim: Boolean,
        action: () -> Unit,
    ): () -> Unit =
        {
            confettiOrigin = origin
            confettiParticles = if (isClaim) 52 else 34
            confettiTrigger += 1
            if (isClaim) feedback.claim() else feedback.tap()
            action()
        }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Today's quests", seeAllLabel = "See all", onSeeAllClick = onSeeAll)
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp)
                    .height(112.dp),
        ) {
            val w = maxWidth
            val c1x = w * (50f / 340f)
            val c2x = w * (170f / 340f)
            val c3x = w * (290f / 340f)
            val c1y = 52.dp
            val c2y = 18.dp
            val c3y = 52.dp

            Canvas(modifier = Modifier.fillMaxWidth().height(112.dp)) {
                val path = buildQuestTrailPath(size.width, size.height, wavePhase)
                val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                drawPath(path = path, color = colors.borderStrong, style = stroke)
                if (trailProgress > 0f) {
                    val measure = PathMeasure(path.asAndroidPath(), false)
                    val segment = android.graphics.Path()
                    measure.getSegment(0f, measure.length * trailProgress, segment, true)
                    drawPath(
                        path = segment.asComposePath(),
                        color = colors.accent,
                        style = stroke,
                    )
                }
                drawQuestPathWave(
                    path = path,
                    travel = waveTravel,
                    color = colors.pro,
                    strokeWidth = 5.dp.toPx(),
                )
            }

            TrailNode(
                modifier = Modifier.offset(x = c1x - 26.dp, y = c1y - 26.dp),
                size = 52.dp,
                onClick = celebrate(Offset(0.15f, 0.45f), simsClaimable, onSimsClick),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SimsProgressRing(
                        progress = state.simsDone.toFloat() / state.simsTotal.coerceAtLeast(1),
                        modifier = Modifier.size(52.dp),
                    )
                    Box(
                        modifier =
                            Modifier
                                .size(41.dp)
                                .clip(CircleShape)
                                .background(colors.surface1)
                                .then(
                                    if (simsClaimable) {
                                        Modifier.shimmer(highlight = colors.pro.copy(alpha = 0.28f))
                                    } else {
                                        Modifier
                                    },
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        QuestMotionIcon(
                            lottieRes = R.raw.eduai_quest_sims,
                            fallbackIcon = Icons.Outlined.SportsEsports,
                            tint = colors.accent,
                            iconSize = 22.dp,
                        )
                    }
                    if (simsClaimable) {
                        ClaimBadge(modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp))
                    } else if (state.simsClaimed) {
                        ClaimedCheck(modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp))
                    }
                }
            }
            ProgressTrailLabel(
                modifier = Modifier.offset(x = c1x - 40.dp, y = 83.dp),
                prefix = state.simsLabelPrefix,
                done = state.simsDone,
                total = state.simsTotal,
                color = if (state.simsClaimed) colors.success else colors.textMuted,
            )

            TrailNode(
                modifier = Modifier.offset(x = c2x - 24.dp, y = c2y - 24.dp),
                size = 48.dp,
                onClick = celebrate(Offset(0.5f, 0.2f), studyClaimable, onStudyClick),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier =
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (studyComplete) colors.success else colors.surface2)
                                .border(
                                    2.dp,
                                    when {
                                        studyClaimable -> colors.pro
                                        studyComplete -> colors.success
                                        else -> colors.borderStrong
                                    },
                                    CircleShape,
                                )
                                .then(
                                    if (studyClaimable) {
                                        Modifier.shimmer(highlight = colors.pro.copy(alpha = 0.28f))
                                    } else {
                                        Modifier
                                    },
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            state.studyClaimed ->
                                QuestMotionIcon(
                                    lottieRes = R.raw.eduai_quest_check,
                                    fallbackIcon = Icons.Filled.Check,
                                    tint = colors.onAccent,
                                    iconSize = 20.dp,
                                    iterations = 1,
                                )
                            studyComplete ->
                                QuestMotionIcon(
                                    lottieRes = R.raw.eduai_quest_quiz,
                                    fallbackIcon = Icons.Filled.Star,
                                    tint = colors.onAccent,
                                    iconSize = 22.dp,
                                )
                            else ->
                                QuestMotionIcon(
                                    lottieRes = R.raw.eduai_quest_quiz_idle,
                                    fallbackIcon = Icons.Outlined.CheckBox,
                                    tint = colors.textSecondary,
                                    iconSize = 20.dp,
                                )
                        }
                    }
                    if (studyClaimable) {
                        ClaimBadge(modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp))
                    } else if (state.studyClaimed) {
                        ClaimedCheck(modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp))
                    }
                }
            }
            ProgressTrailLabel(
                modifier = Modifier.offset(x = c2x - 40.dp, y = 47.dp),
                prefix = state.studyLabelPrefix,
                done = state.studyDone,
                total = state.studyTotal,
                color = if (state.studyClaimed) colors.success else colors.textMuted,
            )

            TrailNode(
                modifier = Modifier.offset(x = c3x - 24.dp, y = c3y - 24.dp),
                size = 48.dp,
                onClick = celebrate(Offset(0.85f, 0.45f), bonusUnlockable, onBonusClick),
            ) {
                val bonusPulse =
                    if (bonusUnlockable) {
                        rememberPulseScale(min = 1f, max = 1.08f, durationMillis = 700)
                    } else {
                        1f
                    }
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .scale(bonusPulse)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (state.bonusClaimed) colors.successBg else colors.warningBg)
                            .border(
                                width = if (bonusUnlockable) 2.dp else 0.dp,
                                color = if (bonusUnlockable) colors.pro else colors.warningBg,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .then(
                                if (bonusUnlockable) {
                                    Modifier.shimmer(highlight = colors.warning.copy(alpha = 0.35f))
                                } else {
                                    Modifier
                                },
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        state.bonusClaimed ->
                            QuestMotionIcon(
                                lottieRes = R.raw.eduai_quest_check,
                                fallbackIcon = Icons.Outlined.CheckBox,
                                tint = colors.success,
                                iconSize = 20.dp,
                                iterations = 1,
                            )
                        bonusUnlockable ->
                            QuestMotionIcon(
                                lottieRes = R.raw.eduai_quest_bonus,
                                fallbackIcon = Icons.Outlined.Videocam,
                                tint = colors.warning,
                                iconSize = 22.dp,
                            )
                        else -> {
                            val idlePulse = rememberPulseScale(min = 0.96f, max = 1.04f, durationMillis = 1400)
                            Icon(
                                Icons.Outlined.Videocam,
                                contentDescription = null,
                                tint = colors.warning,
                                modifier = Modifier.size(19.dp).scale(idlePulse),
                            )
                        }
                    }
                }
            }
            TrailLabel(
                modifier = Modifier.offset(x = c3x - 35.dp, y = 79.dp).width(70.dp),
                text =
                    when {
                        state.bonusClaimed -> "Claimed"
                        bonusUnlockable -> "Tap · +30"
                        else -> "Bonus · +30"
                    },
                color =
                    when {
                        state.bonusClaimed -> colors.success
                        bonusUnlockable -> colors.pro
                        else -> colors.warning
                    },
            )

            ConfettiBurst(
                trigger = confettiTrigger,
                colors = confettiColors,
                particleCount = confettiParticles,
                originFraction = confettiOrigin,
            )
        }
    }
}

/** Wavy S-curve between the three quest nodes; control points bob on a ~2.5s loop. */
private fun buildQuestTrailPath(
    width: Float,
    height: Float,
    wavePhase: Float,
): Path {
    val amplitude = height * 0.028f
    fun waveY(
        yFraction: Float,
        segmentPhase: Float,
    ): Float = height * yFraction + amplitude * sin(wavePhase + segmentPhase)
    return Path().apply {
        moveTo(width * (50f / 340f), waveY(52f / 112f, 0f))
        quadraticTo(
            width * (110f / 340f),
            waveY(18f / 112f, 1.1f),
            width * (170f / 340f),
            waveY(52f / 112f, 2.2f),
        )
        quadraticTo(
            width * (230f / 340f),
            waveY(86f / 112f, 3.3f),
            width * (290f / 340f),
            waveY(52f / 112f, 4.4f),
        )
    }
}

/** Amber pulse that travels start→end once every ~2.5s along the trail. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawQuestPathWave(
    path: Path,
    travel: Float,
    color: Color,
    strokeWidth: Float,
) {
    val measure = PathMeasure(path.asAndroidPath(), false)
    val pathLen = measure.length
    if (pathLen <= 0f) return
    val pulseLen = pathLen * 0.2f
    val head = travel * (pathLen + pulseLen)
    val tail = head - pulseLen
    if (head <= 0f) return
    val segment = android.graphics.Path()
    measure.getSegment(tail.coerceAtLeast(0f), head.coerceAtMost(pathLen), segment, true)
    if (segment.isEmpty) return
    drawPath(
        path = segment.asComposePath(),
        color = color.copy(alpha = 0.82f),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )
}

@Composable
private fun SimsProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    val animated by
        animateFloatAsState(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(700),
            label = "simsRing",
        )
    Canvas(modifier = modifier) {
        val stroke = 5.5.dp.toPx()
        val diameter = size.minDimension - stroke
        val topLeft = Offset(stroke / 2f, stroke / 2f)
        val arcSize = Size(diameter, diameter)
        drawArc(
            color = colors.border,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        if (animated > 0f) {
            drawArc(
                color = colors.accent,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun ClaimBadge(modifier: Modifier = Modifier) {
    val colors = EduAiTheme.colors
    val pulse = rememberPulseScale(min = 0.92f, max = 1.28f, durationMillis = 560)
    val glow = rememberPulseScale(min = 1f, max = 2.1f, durationMillis = 900, repeatMode = RepeatMode.Restart)
    val glowAlpha = (1f - ((glow - 1f) / 1.1f).coerceIn(0f, 1f)) * 0.6f
    Box(modifier = modifier.size(18.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier =
                Modifier
                    .size(18.dp)
                    .scale(glow)
                    .clip(CircleShape)
                    .background(colors.pro.copy(alpha = glowAlpha)),
        )
        Box(
            modifier =
                Modifier
                    .size(18.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(colors.pro)
                    .border(2.dp, colors.surface2, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Videocam, null, tint = colors.onAccent, modifier = Modifier.size(10.dp))
        }
    }
}

@Composable
private fun ClaimedCheck(modifier: Modifier = Modifier) {
    val colors = EduAiTheme.colors
    Box(
        modifier =
            modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(colors.success)
                .border(2.dp, colors.surface2, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Check, null, tint = colors.onAccent, modifier = Modifier.size(10.dp))
    }
}

@Composable
private fun ProgressTrailLabel(
    prefix: String,
    done: Int,
    total: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.width(80.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedCounterText(
            value = done,
            prefix = prefix,
            suffix = "/$total",
            color = color,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun TrailLabel(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.width(80.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun QuestMotionIcon(
    @RawRes lottieRes: Int,
    fallbackIcon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 22.dp,
    animate: Boolean = true,
    iterations: Int = LottieConstants.IterateForever,
) {
    val bob = if (animate) rememberPulseScale(min = 0.94f, max = 1.06f, durationMillis = 1200) else 1f
    Box(
        modifier = modifier.size(iconSize).scale(bob),
        contentAlignment = Alignment.Center,
    ) {
        EduLottie(
            resId = lottieRes,
            modifier = Modifier.size(iconSize),
            iterations = if (animate) iterations else 1,
        ) {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(iconSize * 0.78f),
            )
        }
    }
}

@Composable
private fun TrailNode(
    modifier: Modifier,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .size(size)
                .pressScaleClickable(
                    onClick = onClick,
                    pressedScale = 0.88f,
                    hapticOnPress = false,
                ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
