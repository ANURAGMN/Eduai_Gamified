package com.anurag.eduai.uikit.garden.quest

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.anurag.eduai.uikit.garden.world.hash

private val GARDEN_COLOURS = listOf(
    Color(0xFF7C5CFF), Color(0xFF12B5A6), Color(0xFFEF9F27),
    Color(0xFF2FBF71), Color(0xFFF2A8C4), Color(0xFFFFD764)
)

private val OUTPOST_COLOURS = listOf(
    Color(0xFF5FC7E8), Color(0xFF7FE3C8), Color(0xFFFFD764),
    Color(0xFFFF6B4A), Color(0xFFAEE4F5), Color.White
)

/**
 * A one-shot burst. Bump [trigger] to fire it. Runs its own short animation rather than
 * reading the scene clock, because it is a moment rather than an ambient loop.
 */
@Composable
fun ConfettiBurst(
    trigger: Int,
    theme: Theme,
    modifier: Modifier = Modifier,
    count: Int = 28,
    durationMillis: Int = 1500
) {
    if (trigger <= 0) return
    val progress = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis, easing = LinearEasing))
    }

    // Split by palette, not by shape: the island is a green, sunlit place like the garden, and the
    // colony is a dark one like the outpost. Space confetti over a beach reads as a glitch.
    val colours =
        when (theme) {
            Theme.GARDEN, Theme.ISLAND -> GARDEN_COLOURS
            Theme.OUTPOST, Theme.COLONY -> OUTPOST_COLOURS
        }

    Canvas(modifier) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        val w = size.width
        val h = size.height

        for (i in 0 until count) {
            val delay = hash(i, 5) * 0.25f
            val local = ((p - delay) / (1f - delay)).coerceIn(0f, 1f)
            if (local <= 0f) continue

            val x = w * (0.06f + hash(i, 1) * 0.88f)
            // gravity-ish: starts quick, keeps accelerating
            val fall = local * local * (h + 60f) - 30f
            val drift = (hash(i, 2) - 0.5f) * 40f * local
            val spin = hash(i, 3) * 720f * local
            val alpha = if (local > 0.75f) (1f - local) / 0.25f else 1f
            val size1 = 5f + hash(i, 4) * 4f

            rotate(spin, Offset(x + drift, fall)) {
                drawRect(
                    color = colours[i % colours.size].copy(alpha = alpha),
                    topLeft = Offset(x + drift - size1 / 2f, fall - size1),
                    size = Size(size1, size1 * 1.6f)
                )
            }
        }
    }
}
