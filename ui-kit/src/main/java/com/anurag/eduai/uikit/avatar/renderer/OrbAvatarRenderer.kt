package com.anurag.eduai.uikit.avatar.renderer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.anurag.eduai.uikit.avatar.core.AvatarFrame
import com.anurag.eduai.uikit.avatar.core.AvatarState
import com.anurag.eduai.uikit.avatar.core.Viseme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A non-humanoid, "living" visual for the tutor: a glowing orb that breathes, pulses in
 * time with speech, shifts color with emotional state, and reacts to touch. It sidesteps
 * every uncanny-valley problem a code-drawn face/body runs into, while still consuming the
 * exact animation state the engine already produces (state, viseme, breathing, gaze).
 *
 * Everything is driven off a single continuously-advancing [timeSec] clock so the motion
 * never freezes — even fully idle, the orb shimmers, drifts, and breathes.
 */
@Composable
fun OrbAvatarRenderer(
    frame: AvatarFrame,
    modifier: Modifier = Modifier
) {
    val interaction = rememberAvatarInteraction()

    // Continuous elapsed-time clock in seconds. This is what keeps the orb alive at rest.
    var timeSec by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            timeSec = (now - start) / 1_000_000_000f
        }
    }

    val palette = paletteFor(frame.state)
    // Smoothly cross-fade the orb's color when the emotional state changes rather than
    // snapping, so a shift from e.g. "thinking" purple to "happy" gold reads as a mood
    // change rather than a hard cut.
    val coreColor by animateColorAsState(palette.core, tween(600), label = "orbCore")
    val glowColor by animateColorAsState(palette.glow, tween(600), label = "orbGlow")
    val ringColor by animateColorAsState(palette.ring, tween(600), label = "orbRing")

    // Speech makes the waveform ring come alive; the target amplitude is driven by how open
    // the current viseme is, eased so discrete viseme steps don't pop.
    val isSpeaking = frame.state == AvatarState.Speaking || frame.state == AvatarState.Explaining
    val visemeOpen = if (isSpeaking) visemeOpenness(frame.viseme) else 0f
    val speakAmp by animateFloatAsState(visemeOpen, tween(90), label = "orbSpeak")

    val celebrate = frame.state == AvatarState.Celebrating || frame.state == AvatarState.Happy

    Box(
        modifier = modifier.then(interaction.modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pulse = interaction.pulse
            val unit = size.minDimension * 0.5f
            // Sit the orb a touch above vertical center so it feels "framed" like a face.
            val center = Offset(size.width / 2f, size.height * 0.46f)

            // Breathing: a slow, almost-invisible in/out that never stops. State scales it,
            // and a tap or active speech adds a little extra swell.
            val breath = sin(timeSec * 1.4f) * 0.03f * frame.state.breathingAmplitude
            val breathScale = 1f + breath + speakAmp * 0.10f + pulse * 0.06f

            // Gaze: shift the internal highlight toward the touch point so it feels like the
            // orb is "looking" at the user when pressed.
            val gaze = Offset(interaction.gazeX, interaction.gazeY)

            drawGroundGlow(center, unit, glowColor, breathScale)
            drawHalo(center, unit, glowColor, timeSec, breathScale, speakAmp, pulse)
            drawWaveformRing(center, unit, ringColor, timeSec, breathScale, speakAmp, isSpeaking)
            drawCore(center, unit, coreColor, glowColor, breathScale, gaze, pulse)
            drawParticles(center, unit, ringColor, timeSec, celebrate, speakAmp)

            // Touch ripple, exactly where the user pressed.
            interaction.tapPosition?.let { pos ->
                if (pulse > 0.01f) {
                    drawCircle(
                        color = ringColor.copy(alpha = pulse * 0.5f),
                        radius = unit * 0.2f + (1f - pulse) * unit * 0.9f,
                        center = pos,
                        style = Stroke(width = 2.5f + pulse * 2f)
                    )
                }
            }
        }
    }
}

private data class OrbPalette(val core: Color, val glow: Color, val ring: Color)

/** Maps the tutor's emotional/activity state to a color mood. */
private fun paletteFor(state: AvatarState): OrbPalette = when (state) {
    AvatarState.Idle -> OrbPalette(Color(0xFF4FD1C5), Color(0xFF2A9D8F), Color(0xFF7FE9DE))
    AvatarState.Listening -> OrbPalette(Color(0xFF56B8F5), Color(0xFF2E7FD6), Color(0xFF8FD3FF))
    AvatarState.Thinking -> OrbPalette(Color(0xFFA78BFA), Color(0xFF7C5CF0), Color(0xFFC9B8FF))
    AvatarState.Speaking -> OrbPalette(Color(0xFF48D6A8), Color(0xFF2AA37A), Color(0xFF86F0CC))
    AvatarState.Explaining -> OrbPalette(Color(0xFF6AA0F5), Color(0xFF4173D6), Color(0xFF9FC0FF))
    AvatarState.Happy -> OrbPalette(Color(0xFFFBBF24), Color(0xFFE59810), Color(0xFFFFD874))
    AvatarState.Confused -> OrbPalette(Color(0xFFFB923C), Color(0xFFE0701C), Color(0xFFFFB877))
    AvatarState.Celebrating -> OrbPalette(Color(0xFFF472B6), Color(0xFFE0479A), Color(0xFFFFA0D2))
}

/** How "open" a viseme is, used as the speaking waveform amplitude (0 = shut, 1 = wide). */
private fun visemeOpenness(viseme: Viseme): Float = when (viseme) {
    Viseme.Rest -> 0.08f
    Viseme.Closed -> 0.0f
    Viseme.Wide -> 0.7f
    Viseme.Open -> 1.0f
    Viseme.Round -> 0.5f
    Viseme.FV -> 0.3f
    Viseme.Th -> 0.35f
    Viseme.Smush -> 0.4f
    Viseme.Kiss -> 0.3f
}

/** Soft shadow/glow pooled beneath the orb, subtly breathing with it. */
private fun DrawScope.drawGroundGlow(center: Offset, unit: Float, glow: Color, scale: Float) {
    val y = center.y + unit * 0.92f
    val w = unit * 1.5f * scale
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(glow.copy(alpha = 0.22f), Color.Transparent),
            center = Offset(center.x, y),
            radius = w * 0.6f
        ),
        topLeft = Offset(center.x - w / 2f, y - unit * 0.12f),
        size = Size(w, unit * 0.24f)
    )
}

/** Layered outer halo — the orb's aura. Expands a little with speech and touch. */
private fun DrawScope.drawHalo(
    center: Offset,
    unit: Float,
    glow: Color,
    time: Float,
    scale: Float,
    speakAmp: Float,
    pulse: Float
) {
    val haloR = unit * (0.9f + 0.05f * sin(time * 1.1f) + speakAmp * 0.12f + pulse * 0.1f) * scale
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                glow.copy(alpha = 0.0f),
                glow.copy(alpha = 0.10f + speakAmp * 0.12f),
                Color.Transparent
            ),
            center = center,
            radius = haloR
        ),
        radius = haloR,
        center = center
    )
}

/**
 * The ring of "voice" around the orb. At rest it's a calm circle with a faint shimmer; while
 * speaking, layered sine harmonics ripple its radius so it reads as sound coming off the orb.
 */
private fun DrawScope.drawWaveformRing(
    center: Offset,
    unit: Float,
    ring: Color,
    time: Float,
    scale: Float,
    speakAmp: Float,
    isSpeaking: Boolean
) {
    val baseR = unit * 0.62f * scale
    val idleShimmer = 0.012f
    val amp = unit * (idleShimmer + speakAmp * 0.14f)
    val samples = 96
    val path = Path()
    for (i in 0..samples) {
        val t = i.toFloat() / samples
        val ang = t * 2f * PI.toFloat()
        // A couple of harmonics rotating at different speeds gives an organic, non-repeating
        // wobble rather than a mechanical pulse.
        val wobble = sin(ang * 5f + time * 2.6f) * 0.6f +
            sin(ang * 8f - time * 1.7f) * 0.4f +
            sin(ang * 3f + time * 1.1f) * 0.5f
        val r = baseR + amp * wobble
        val p = Offset(center.x + cos(ang) * r, center.y + sin(ang) * r)
        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    path.close()
    drawPath(
        path,
        color = ring.copy(alpha = if (isSpeaking) 0.55f else 0.28f),
        style = Stroke(width = unit * (0.010f + speakAmp * 0.012f), cap = StrokeCap.Round)
    )
}

/** The solid glowing sphere at the heart of the visual, with a directional highlight. */
private fun DrawScope.drawCore(
    center: Offset,
    unit: Float,
    core: Color,
    glow: Color,
    scale: Float,
    gaze: Offset,
    pulse: Float
) {
    val r = unit * 0.44f * scale
    // Base sphere: bright center falling off to the deeper glow color at the rim.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                lerp(core, Color.White, 0.35f + pulse * 0.2f),
                core,
                lerp(glow, Color.Black, 0.1f)
            ),
            center = Offset(center.x - r * 0.25f, center.y - r * 0.3f),
            radius = r * 1.35f
        ),
        radius = r,
        center = center
    )
    // Rim light for a bit of dimensionality.
    drawCircle(
        color = lerp(core, Color.White, 0.5f).copy(alpha = 0.35f),
        radius = r,
        center = center,
        style = Stroke(width = unit * 0.012f)
    )
    // Specular highlight that drifts toward the gaze/touch direction, so the orb feels like
    // it turns to face the user.
    val hi = Offset(
        center.x - r * 0.34f + gaze.x * r * 0.35f,
        center.y - r * 0.38f + gaze.y * r * 0.35f
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.6f), Color.Transparent),
            center = hi,
            radius = r * 0.5f
        ),
        radius = r * 0.5f,
        center = hi
    )
}

/**
 * Sparkles orbiting the core. They always drift gently; when the tutor is happy or
 * celebrating they multiply, brighten, and spin faster for a festive feel.
 */
private fun DrawScope.drawParticles(
    center: Offset,
    unit: Float,
    ring: Color,
    time: Float,
    celebrate: Boolean,
    speakAmp: Float
) {
    val count = if (celebrate) 14 else 7
    val speed = if (celebrate) 1.6f else 0.5f
    val baseAlpha = if (celebrate) 0.9f else 0.5f
    for (i in 0 until count) {
        val seed = i * 2.3999632f // golden-angle spacing so they never bunch up
        val orbit = unit * (0.72f + 0.18f * sin(seed * 1.7f))
        val ang = seed + time * speed * (0.7f + 0.5f * sin(seed))
        val twinkle = 0.5f + 0.5f * sin(time * 3f + seed * 4f)
        val pr = unit * (0.014f + 0.012f * twinkle) * (1f + speakAmp * 0.6f)
        val pos = Offset(center.x + cos(ang) * orbit, center.y + sin(ang) * orbit)
        drawCircle(
            color = ring.copy(alpha = baseAlpha * twinkle),
            radius = pr,
            center = pos
        )
    }
}
