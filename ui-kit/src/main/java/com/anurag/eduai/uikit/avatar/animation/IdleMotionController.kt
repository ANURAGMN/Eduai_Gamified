package com.anurag.eduai.uikit.avatar.animation

import com.anurag.eduai.uikit.avatar.core.AvatarState
import com.anurag.eduai.uikit.avatar.core.EyeMotion
import kotlin.math.sin
import kotlin.random.Random

/**
 * Idle / ambient layers: randomized blinks, saccades, breathing, listening gaze,
 * and a speaking bounce that keeps the torso from looking frozen.
 */
class IdleMotionController {

    private var nextBlinkMs: Long = 0
    private var blinkStartMs: Long = 0
    private var isBlinking = false
    private var blinkDurationMs = 140L
    private var nextEyeMoveMs: Long = 0
    private var pupilTargetX = 0f
    private var pupilTargetY = 0f
    private var pupilCurrentX = 0f
    private var pupilCurrentY = 0f
    private var nextShoulderMs: Long = 0
    private var shoulderTarget = 0f
    private var shoulderCurrent = 0f

    fun update(
        state: AvatarState,
        timestampMs: Long,
        deltaMs: Long,
        isSpeaking: Boolean = false,
        smileAmount: Float = 0f
    ): IdleMotionResult {
        // Randomized blink every ~2–5s (scaled by state). Avoid fixed intervals.
        if (nextBlinkMs == 0L) {
            nextBlinkMs = timestampMs + Random.nextLong(2000, 4500)
        }
        val blinkBase = (3500 / state.blinkFrequencyMultiplier).toLong().coerceIn(1800, 5000)
        if (timestampMs >= nextBlinkMs && !isBlinking) {
            isBlinking = true
            blinkStartMs = timestampMs
            blinkDurationMs = Random.nextLong(110, 180)
            nextBlinkMs = timestampMs + blinkBase + Random.nextLong(-400, 1200)
        }

        var eyeBlinkProgress = 0f
        val eyeMotion: EyeMotion
        if (isBlinking) {
            val elapsed = timestampMs - blinkStartMs
            if (elapsed >= blinkDurationMs) {
                isBlinking = false
                eyeMotion = EyeMotion.Neutral
            } else {
                val t = (elapsed.toFloat() / blinkDurationMs).coerceIn(0f, 1f)
                eyeBlinkProgress = sin(t * Math.PI.toFloat()).coerceIn(0f, 1f)
                eyeMotion = EyeMotion.Blink
            }
        } else {
            eyeMotion = EyeMotion.Neutral
        }

        // State-aware saccades: look toward student when listening; glance up when thinking.
        if (timestampMs >= nextEyeMoveMs) {
            when (state) {
                AvatarState.Listening -> {
                    pupilTargetX = Random.nextFloat() * 2f - 1f
                    pupilTargetY = Random.nextFloat() * 1.5f - 0.5f
                    nextEyeMoveMs = timestampMs + Random.nextLong(2200, 4200)
                }
                AvatarState.Thinking, AvatarState.Confused -> {
                    pupilTargetX = Random.nextFloat() * 4f - 2f
                    pupilTargetY = -(2.5f + Random.nextFloat() * 2f) // brief upward glance
                    nextEyeMoveMs = timestampMs + Random.nextLong(900, 1800)
                }
                AvatarState.Speaking, AvatarState.Explaining -> {
                    pupilTargetX = Random.nextFloat() * 3f - 1.5f
                    pupilTargetY = Random.nextFloat() * 1.8f - 0.9f
                    nextEyeMoveMs = timestampMs + Random.nextLong(1600, 3200)
                }
                else -> {
                    pupilTargetX = Random.nextFloat() * 3.6f - 1.8f
                    pupilTargetY = Random.nextFloat() * 2.4f - 1.2f
                    nextEyeMoveMs = timestampMs + Random.nextLong(1800, 4200)
                }
            }
        }

        val eyeLerp = (deltaMs / 280f).coerceIn(0f, 1f)
        pupilCurrentX += (pupilTargetX - pupilCurrentX) * eyeLerp
        pupilCurrentY += (pupilTargetY - pupilCurrentY) * eyeLerp

        // Soft shoulder micro-shifts every few seconds.
        if (timestampMs >= nextShoulderMs) {
            shoulderTarget = Random.nextFloat() * 1.4f - 0.7f
            nextShoulderMs = timestampMs + Random.nextLong(3000, 6000)
        }
        val shoulderLerp = (deltaMs / 400f).coerceIn(0f, 1f)
        shoulderCurrent += (shoulderTarget - shoulderCurrent) * shoulderLerp

        // Amplitudes deliberately kept small — the goal is "alive but calm", not fidgety.
        val breathingPhase = timestampMs / 1000f * 2f * Math.PI.toFloat()
        val breathing = sin(breathingPhase) * 2f * state.breathingAmplitude
        val sway = sin(timestampMs / 2000f * 2f * Math.PI.toFloat()) * 1.1f + shoulderCurrent * 0.22f
        val headMicro = sin(timestampMs / 3000f * 2f * Math.PI.toFloat()) * 0.9f

        // Tiny vertical bounce while talking + gentle idle lift otherwise.
        val speakingBounce = if (isSpeaking) sin(timestampMs / 260f) * 1.1f else 0f
        val idleLift = sin(timestampMs / 1600f) * 0.4f
        val bodyBounce = speakingBounce + idleLift

        // Eye squint during smiles — softens the "staring" look.
        val eyeSquint = (smileAmount * 0.45f).coerceIn(0f, 0.55f)

        // Listening lean / teaching lean baked into posture offset elsewhere;
        // amplify breathing slightly when idle-listening so it never freezes.
        val listeningBreathBoost = if (state == AvatarState.Listening) 1.15f else 1f

        return IdleMotionResult(
            eyeMotion = eyeMotion,
            eyeBlinkProgress = eyeBlinkProgress,
            pupilOffsetX = pupilCurrentX,
            pupilOffsetY = pupilCurrentY,
            bodyBreathing = breathing * listeningBreathBoost,
            bodySway = sway,
            bodyBounce = bodyBounce,
            headMicroMotion = headMicro + state.headPostureOffset,
            eyeSquint = eyeSquint,
            shoulderOffset = shoulderCurrent
        )
    }
}

data class IdleMotionResult(
    val eyeMotion: EyeMotion,
    val eyeBlinkProgress: Float = 0f,
    val pupilOffsetX: Float,
    val pupilOffsetY: Float,
    val bodyBreathing: Float,
    val bodySway: Float,
    val bodyBounce: Float = 0f,
    val headMicroMotion: Float,
    val eyeSquint: Float = 0f,
    val shoulderOffset: Float = 0f
)
