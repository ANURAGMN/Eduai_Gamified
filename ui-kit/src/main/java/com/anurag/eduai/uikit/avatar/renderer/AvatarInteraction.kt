package com.anurag.eduai.uikit.avatar.renderer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

/**
 * Shared touch interaction for avatar renderers, so both the Compose-drawn ("Free")
 * avatar and the PNG portrait avatars (Boy/Girl) get the same feel:
 *
 * - Press and hold: the avatar eases its gaze/head toward the touch point.
 * - Tap: a short "greet" pulse (0 -> 1 -> 0) other code can use to boost smile amount,
 *   swap in a wave gesture, and pop the whole figure very slightly for feedback.
 *
 * [gazeX]/[gazeY] are normalized to roughly -1..1 and already spring-eased, so callers
 * can multiply them straight into pupil offsets / head rotation without extra smoothing.
 */
class AvatarInteraction internal constructor(
    val gazeX: Float,
    val gazeY: Float,
    val pulse: Float,
    val tapPosition: Offset?,
    val modifier: Modifier
)

@Composable
fun rememberAvatarInteraction(): AvatarInteraction {
    val scope = rememberCoroutineScope()
    val greetPulse = remember { Animatable(0f) }
    var pointerActive by remember { mutableStateOf(false) }
    var pointerNormX by remember { mutableStateOf(0f) }
    var pointerNormY by remember { mutableStateOf(0f) }
    var tapMarker by remember { mutableStateOf<Offset?>(null) }

    val gazeX by animateFloatAsState(
        targetValue = if (pointerActive) pointerNormX else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "avatarGazeX"
    )
    val gazeY by animateFloatAsState(
        targetValue = if (pointerActive) pointerNormY else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "avatarGazeY"
    )

    val interactionModifier = Modifier.pointerInput(Unit) {
        detectTapGestures(
            onPress = { offset ->
                pointerActive = true
                val w = size.width.toFloat().coerceAtLeast(1f)
                val h = size.height.toFloat().coerceAtLeast(1f)
                pointerNormX = ((offset.x / w) * 2f - 1f).coerceIn(-1f, 1f)
                pointerNormY = ((offset.y / h) * 2f - 1f).coerceIn(-1f, 1f)
                tryAwaitRelease()
                pointerActive = false
            },
            onTap = { offset ->
                tapMarker = offset
                scope.launch {
                    greetPulse.snapTo(1f)
                    greetPulse.animateTo(0f, tween(durationMillis = 950, easing = FastOutSlowInEasing))
                }
            }
        )
    }

    return AvatarInteraction(
        gazeX = gazeX,
        gazeY = gazeY,
        pulse = greetPulse.value,
        tapPosition = tapMarker,
        modifier = interactionModifier
    )
}

/**
 * Returns a scale that snaps down to [restScale] whenever [key] changes and eases back up
 * to 1 over [durationMs]. Used to soften discrete state switches (most notably viseme/mouth
 * shape changes during lip sync) that would otherwise pop into place with no transition at
 * all, without needing a full continuous-parameter blend between shapes.
 */
@Composable
fun <T> rememberPopOnChange(key: T, restScale: Float = 0.72f, durationMs: Int = 90): Float {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(key) {
        scale.snapTo(restScale)
        scale.animateTo(1f, tween(durationMillis = durationMs, easing = FastOutSlowInEasing))
    }
    return scale.value
}
