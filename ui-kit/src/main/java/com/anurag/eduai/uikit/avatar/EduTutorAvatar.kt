package com.anurag.eduai.uikit.avatar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.anurag.eduai.uikit.avatar.animation.AvatarAnimationEngine
import com.anurag.eduai.uikit.avatar.core.AvatarFrame
import com.anurag.eduai.uikit.avatar.core.AvatarState
import com.anurag.eduai.uikit.avatar.core.GesturePlan
import com.anurag.eduai.uikit.avatar.core.TutorCharacter
import com.anurag.eduai.uikit.avatar.renderer.AvatarRenderer
import com.anurag.eduai.uikit.avatar.renderer.OrbAvatarRenderer

/**
 * Drives an [AvatarAnimationEngine] on the Compose frame clock and emits a fresh
 * [AvatarFrame] each frame — so the avatar breathes, blinks, sways, and reacts
 * even when fully idle. [state] can be changed to shift the tutor's mood.
 */
@Composable
fun rememberTutorFrame(state: AvatarState = AvatarState.Idle): AvatarFrame {
    val engine = remember { AvatarAnimationEngine() }
    LaunchedEffect(state) { engine.setState(state) }
    var frame by remember { mutableStateOf(engine.update(0L, 16L)) }
    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val deltaMs = ((now - last) / 1_000_000L).coerceIn(1L, 64L)
            last = now
            frame = engine.update(now / 1_000_000L, deltaMs)
        }
    }
    return frame
}

/**
 * Single entry point for the ported tutor avatars. [TutorCharacter.Orb] renders
 * the reactive glowing orb; anything else renders the code-drawn "Free" face.
 * Both are pure Compose — no Rive, no assets.
 */
@Composable
fun EduTutorAvatar(
    character: TutorCharacter = TutorCharacter.Orb,
    state: AvatarState = AvatarState.Idle,
    modifier: Modifier = Modifier,
    outfitVariant: Int = 0,
    hairStyle: Int = 0,
    hairColor: Int = 0,
    glassesStyle: Int = 0,
    glassesColor: Int = 0,
    neckStyle: Int = 0,
    underEyeLine: Boolean = false,
    cheekShading: Boolean = true,
    moodOverride: Int = 0,
    gestureOverride: Int = 0,
    spinTrigger: Int = 0,
) {
    val frame = rememberTutorFrame(state)
    when (character) {
        TutorCharacter.Orb -> OrbAvatarRenderer(frame = frame, modifier = modifier)
        else ->
            AvatarRenderer(
                frame = frame,
                modifier = modifier,
                outfitVariant = outfitVariant,
                hairStyle = hairStyle,
                hairColor = hairColor,
                glassesStyle = glassesStyle,
                glassesColor = glassesColor,
                neckStyle = neckStyle,
                underEyeLine = underEyeLine,
                cheekShading = cheekShading,
                moodOverride = moodOverride,
                gestureOverride = gestureOverride,
                spinTrigger = spinTrigger,
            )
    }
}

/**
 * Tutor avatar driven by real TTS state: phoneme timeline + word-boundary re-anchoring.
 * Use this on agent screens instead of the WebView LipSync.html path.
 */
@Composable
fun EduTutorAvatarWithLipSync(
    isSpeaking: Boolean,
    spokenText: String,
    estimatedDurationMs: Long,
    wordBoundaryIndex: Int,
    mood: AvatarState = AvatarState.Idle,
    character: TutorCharacter = TutorCharacter.Free,
    modifier: Modifier = Modifier,
    outfitVariant: Int = 0,
    hairStyle: Int = 0,
    hairColor: Int = 0,
    glassesStyle: Int = 0,
    glassesColor: Int = 0,
    neckStyle: Int = 0,
    underEyeLine: Boolean = false,
    cheekShading: Boolean = true,
    /** Zoom for small agent circles — 1f = full body, ~1.7f = face-forward crop. */
    faceZoom: Float = 1f,
    /** Nudge zoomed content down as a fraction of composable height (centers the face). */
    faceOffsetYFraction: Float = 0f,
) {
    val engine = remember { AvatarAnimationEngine() }

    LaunchedEffect(mood, isSpeaking) {
        val now = System.currentTimeMillis()
        when {
            isSpeaking -> Unit
            mood == AvatarState.Listening -> engine.setListening()
            mood == AvatarState.Thinking -> engine.setThinking(now)
            else -> engine.setState(mood)
        }
    }

    LaunchedEffect(isSpeaking, spokenText, estimatedDurationMs) {
        if (isSpeaking && spokenText.isNotBlank()) {
            val now = System.currentTimeMillis()
            engine.beginSpeaking(
                gesturePlan = GesturePlan.NeutralSpeaking,
                timestampMs = now,
                spokenText = spokenText,
                durationMs = estimatedDurationMs.coerceAtLeast(300L),
            )
        } else if (!isSpeaking) {
            engine.endSpeaking()
        }
    }

    LaunchedEffect(wordBoundaryIndex, isSpeaking) {
        if (isSpeaking && wordBoundaryIndex >= 0) {
            engine.noteWordBoundary(wordBoundaryIndex, System.currentTimeMillis())
        }
    }

    var frame by remember { mutableStateOf(engine.update(0L, 16L)) }
    LaunchedEffect(Unit) {
        var lastFrameNanos = withFrameNanos { it }
        while (true) {
            val frameNanos = withFrameNanos { it }
            val deltaMs = ((frameNanos - lastFrameNanos) / 1_000_000L).coerceIn(1L, 64L)
            lastFrameNanos = frameNanos
            // Lip-sync anchors use System.currentTimeMillis(); frame clock must match.
            frame = engine.update(System.currentTimeMillis(), deltaMs)
        }
    }

    val avatarModifier = modifier.agentFaceZoom(faceZoom, faceOffsetYFraction)

    when (character) {
        TutorCharacter.Orb -> OrbAvatarRenderer(frame = frame, modifier = avatarModifier)
        else ->
            AvatarRenderer(
                frame = frame,
                modifier = avatarModifier,
                outfitVariant = outfitVariant,
                hairStyle = hairStyle,
                hairColor = hairColor,
                glassesStyle = glassesStyle,
                glassesColor = glassesColor,
                neckStyle = neckStyle,
                underEyeLine = underEyeLine,
                cheekShading = cheekShading,
            )
    }
}

private fun Modifier.agentFaceZoom(faceZoom: Float, faceOffsetYFraction: Float): Modifier =
    avatarFaceZoom(faceZoom, faceOffsetYFraction)

internal fun Modifier.avatarFaceZoom(faceZoom: Float, faceOffsetYFraction: Float): Modifier {
    if (faceZoom == 1f && faceOffsetYFraction == 0f) return this
    return this.graphicsLayer {
        scaleX = faceZoom
        scaleY = faceZoom
        translationY = size.height * faceOffsetYFraction
    }
}
