package com.anurag.eduai.uikit.garden.world

import android.provider.Settings
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import kotlin.math.floor
import kotlin.math.sin

/**
 * Deterministic pseudo-random, ported from the prototype's Z(i, k).
 * Must be stable: calling Random() inside a draw makes plants twitch every frame.
 */
fun hash(i: Int, k: Int): Float {
    val x = sin(i * 127.1f + k * 311.7f) * 43758.5453f
    return x - floor(x)
}

/** Positive amount lightens toward white, negative darkens toward black. */
fun shade(c: Color, amount: Float): Color =
    if (amount >= 0f) lerp(c, Color.White, amount) else lerp(c, Color.Black, -amount)

/**
 * Reads the system animator duration scale. Returns false when the user has
 * turned animations off — an animated scene is exactly what fails motion-sensitive users.
 */
@Composable
fun rememberAnimatorScale(): Float {
    val context = LocalContext.current
    return remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        } catch (e: Exception) {
            1f
        }
    }
}

@Composable
fun rememberMotionEnabled(): Boolean = rememberAnimatorScale() > 0f

/**
 * Shared sway, in degrees.
 *
 * Three sine components at unrelated rates so the motion never visibly repeats and reads as a
 * plant bending rather than a rigid object rotating: a main bend, a slower lean, and a small
 * fast flutter at the tip.
 *
 * On top of that a slow gust envelope swells and drops the whole thing over roughly half a minute,
 * so the scene breathes instead of ticking. Peak is about 7 degrees at amount 1.
 */
fun swayDegrees(time: Float, phase: Float, amount: Float = 1f): Float {
    val bend = sin(time * 1.25f + phase) * 3.6f +
        sin(time * 0.43f + phase * 1.7f) * 1.9f +
        sin(time * 2.10f + phase * 0.6f) * 0.6f
    val gust = 0.75f + sin(time * 0.19f + phase * 0.5f) * 0.45f
    return bend * gust * amount
}

/**
 * One clock for the whole scene. Every animated element derives its phase from
 * this value plus a stable per-element offset, instead of owning its own transition.
 */
@Composable
fun rememberSceneTime(enabled: Boolean): State<Float> =
    produceState(0f, enabled) {
        if (!enabled) {
            value = 0f
            return@produceState
        }
        // `nanos` is device uptime. After a few hours it is large enough that a Float
        // cannot resolve one frame (~16 ms), and every animation quietly freezes.
        // Anchor on the first frame so the value stays small and keeps its precision.
        var origin = 0L
        while (true) {
            withInfiniteAnimationFrameNanos { nanos ->
                if (origin == 0L) origin = nanos
                value = (nanos - origin) / 1_000_000_000f
            }
        }
    }
