package com.anurag.eduai.uikit.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Bouncy, tactile press feedback. Presses squash the target to [pressedScale];
 * on release a bouncy spring lets it overshoot back past 1.0, so taps feel
 * springy rather than flat.
 */
fun Modifier.pressScaleClickable(
    onClick: () -> Unit,
    pressedScale: Float = 0.90f,
    enabled: Boolean = true,
    hapticOnPress: Boolean = true,
): Modifier =
    composed {
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        // soundEnabled = false: taps are haptic-only, and this avoids allocating a
        // ToneGenerator for every clickable element on screen.
        val feedback = rememberEduFeedback(soundEnabled = false)
        LaunchedEffect(pressed) {
            if (pressed && hapticOnPress) feedback.tap()
        }
        val scale by
            animateFloatAsState(
                targetValue = if (pressed) pressedScale else 1f,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                label = "pressScale",
            )
        this
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick,
            )
    }

/**
 * Looping scale. `RepeatMode.Reverse` reads as a "breathing" idle cue (flame,
 * claim badges); `RepeatMode.Restart` reads as an expanding "ping" (notification
 * rings). Defaults are punchy on purpose.
 */
@Composable
fun rememberPulseScale(
    min: Float = 1f,
    max: Float = 1.15f,
    durationMillis: Int = 900,
    repeatMode: RepeatMode = RepeatMode.Reverse,
): Float {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by
        transition.animateFloat(
            initialValue = min,
            targetValue = max,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis, easing = LinearEasing),
                    repeatMode = repeatMode,
                ),
            label = "pulseScale",
        )
    return scale
}

/**
 * Pops content into place after [delayMillis] with a bouncy scale + upward slide.
 * Uses [graphicsLayer] instead of [AnimatedVisibility] so enter motion still runs
 * inside vertically scrolling parents (AnimatedVisibility often skips there).
 */
@Composable
fun Entrance(
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.76f) }
    val offsetY = remember { Animatable(36f) }

    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        coroutineScope {
            launch {
                alpha.animateTo(1f, animationSpec = tween(320, easing = FastOutSlowInEasing))
            }
            launch {
                scale.animateTo(
                    1f,
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                )
            }
            launch {
                offsetY.animateTo(
                    0f,
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                )
            }
        }
    }

    Box(
        modifier =
            modifier.graphicsLayer {
                this.alpha = alpha.value
                scaleX = scale.value
                scaleY = scale.value
                translationY = offsetY.value
            },
    ) {
        content()
    }
}

/**
 * Diagonal light sweep that travels across the content on a loop — gives a
 * "premium"/alive sheen to hero surfaces. Draw it after the content so the
 * highlight sits on top.
 */
fun Modifier.shimmer(
    highlight: Color = Color.White.copy(alpha = 0.35f),
    durationMillis: Int = 2200,
): Modifier =
    composed {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val progress by
            transition.animateFloat(
                initialValue = -0.4f,
                targetValue = 1.4f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                label = "shimmerProgress",
            )
        drawWithContent {
            drawContent()
            val bandWidth = size.width * 0.35f
            val x = progress * size.width
            drawRect(
                brush =
                    Brush.linearGradient(
                        colors = listOf(Color.Transparent, highlight, Color.Transparent),
                        start = Offset(x - bandWidth, 0f),
                        end = Offset(x + bandWidth, size.height),
                    ),
            )
        }
    }

private data class ConfettiParticle(
    val angleRad: Float,
    val distance: Float,
    val color: Color,
    val radius: Float,
    val spinDir: Float,
)

/**
 * One-shot confetti burst. Every time [trigger] changes to a new non-zero value,
 * a fresh batch of particles fires outward from [originFraction] (fraction of
 * width/height) and falls under gravity while fading. Overlay it on top of the
 * surface you want to celebrate.
 */
@Composable
fun ConfettiBurst(
    trigger: Int,
    modifier: Modifier = Modifier,
    colors: List<Color>,
    particleCount: Int = 34,
    originFraction: Offset = Offset(0.5f, 0.35f),
) {
    val progress = remember { Animatable(0f) }
    val particles =
        remember(trigger) {
            if (trigger == 0) {
                emptyList()
            } else {
                val rnd = Random(trigger * 9301 + 49297)
                List(particleCount) {
                    ConfettiParticle(
                        angleRad = (rnd.nextFloat() * 2f * Math.PI.toFloat()),
                        distance = 120f + rnd.nextFloat() * 260f,
                        color = colors[rnd.nextInt(colors.size)],
                        radius = 4f + rnd.nextFloat() * 6f,
                        spinDir = if (rnd.nextBoolean()) 1f else -1f,
                    )
                }
            }
        }

    LaunchedEffect(trigger) {
        if (trigger != 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, animationSpec = tween(1100, easing = FastOutSlowInEasing))
        }
    }

    if (particles.isEmpty()) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val p = progress.value
        val origin = Offset(size.width * originFraction.x, size.height * originFraction.y)
        val gravity = 520f
        particles.forEach { part ->
            val dx = cos(part.angleRad) * part.distance * p
            val dy = sin(part.angleRad) * part.distance * p + gravity * p * p
            val alpha = (1f - p).coerceIn(0f, 1f)
            // slight pulsing radius so pieces flutter
            val r = part.radius * (0.7f + 0.3f * sin((p * 12f + part.spinDir) * Math.PI.toFloat()))
            drawCircle(
                color = part.color.copy(alpha = alpha),
                radius = r.coerceAtLeast(1f),
                center = Offset(origin.x + dx, origin.y + dy),
            )
        }
    }
}

/**
 * Continuous full-screen confetti rain for the reward moment. Pieces fall from
 * above the top edge, sway side to side, and recycle forever while [active].
 */
@Composable
fun ConfettiRain(
    active: Boolean,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    pieceCount: Int = 70,
) {
    if (!active) return
    val transition = rememberInfiniteTransition(label = "rain")
    val time by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1800, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "rainTime",
        )
    val pieces =
        remember(pieceCount) {
            val rnd = Random(1234)
            List(pieceCount) {
                RainPiece(
                    xFraction = rnd.nextFloat(),
                    phase = rnd.nextFloat(),
                    speed = 0.6f + rnd.nextFloat() * 0.9f,
                    swayAmp = 8f + rnd.nextFloat() * 22f,
                    radius = 4f + rnd.nextFloat() * 6f,
                    color = colors[rnd.nextInt(colors.size)],
                )
            }
        }
    Canvas(modifier = modifier.fillMaxSize()) {
        val h = size.height + 40f
        pieces.forEach { p ->
            val t = (time * p.speed + p.phase) % 1f
            val y = t * h - 20f
            val x = p.xFraction * size.width + sin((t * 6.28f) + p.phase * 6.28f) * p.swayAmp
            drawCircle(
                color = p.color,
                radius = p.radius,
                center = Offset(x, y),
            )
        }
    }
}

private data class RainPiece(
    val xFraction: Float,
    val phase: Float,
    val speed: Float,
    val swayAmp: Float,
    val radius: Float,
    val color: Color,
)

/**
 * Text that counts up from 0 to [value] the first time it appears (and animates
 * to any later value change). Great for XP / gems / streak so the numbers feel
 * earned rather than static.
 */
@Composable
fun AnimatedCounterText(
    value: Int,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    prefix: String = "",
    suffix: String = "",
    durationMillis: Int = 900,
) {
    var target by remember { mutableStateOf(0) }
    LaunchedEffect(value) { target = value }
    val animated by
        animateIntAsState(
            targetValue = target,
            animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
            label = "counter",
        )
    Text(
        text = "$prefix$animated$suffix",
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        modifier = modifier,
    )
}
