package com.anurag.eduai.uikit.avatar.animation

import com.anurag.eduai.uikit.avatar.core.HeadMotion
import kotlin.math.sin

/**
 * Discrete head gestures (nod/tilt/shake) plus continuous speaking micro-motion.
 * Motions ease back to a soft neutral instead of snapping off.
 */
class HeadMotionController {

    private var activeMotion: HeadMotion = HeadMotion.None
    private var motionStartMs: Long = 0
    private var motionDurationMs: Long = 600

    private var currentX = 0f
    private var currentY = 0f
    private var currentZ = 0f

    fun trigger(motion: HeadMotion, timestampMs: Long) {
        if (motion == HeadMotion.None) return
        activeMotion = motion
        motionStartMs = timestampMs
        motionDurationMs = when (motion) {
            HeadMotion.SmallNod -> 420
            HeadMotion.Nod -> 720
            HeadMotion.Tilt -> 900
            HeadMotion.Shake -> 800
            HeadMotion.Emphasis -> 550
            HeadMotion.None -> 0
        }
    }

    fun update(timestampMs: Long, baseOffset: Float, isSpeaking: Boolean, deltaMs: Long): HeadRotation {
        val target = targetRotation(timestampMs, baseOffset, isSpeaking)
        // ~250ms ease toward the target so returns to neutral feel natural.
        val lerp = (deltaMs / 250f).coerceIn(0f, 1f)
        currentX += (target.x - currentX) * lerp
        currentY += (target.y - currentY) * lerp
        currentZ += (target.z - currentZ) * lerp
        return HeadRotation(currentX, currentY, currentZ)
    }

    private fun targetRotation(timestampMs: Long, baseOffset: Float, isSpeaking: Boolean): HeadRotation {
        // Subtle continuous bob/tilt while speaking — the single biggest "alive" cue.
        // Kept small so the head reads as calmly present, not nodding constantly.
        val speakingBobX = if (isSpeaking) sin(timestampMs / 320f) * 1.5f else 0f
        val speakingTiltZ = if (isSpeaking) sin(timestampMs / 920f) * 1.2f else 0f
        val speakingYawY = if (isSpeaking) sin(timestampMs / 1400f) * 0.9f else 0f

        if (activeMotion == HeadMotion.None) {
            return HeadRotation(
                baseOffset + speakingBobX,
                speakingYawY,
                speakingTiltZ
            )
        }

        val elapsed = timestampMs - motionStartMs
        if (elapsed > motionDurationMs) {
            activeMotion = HeadMotion.None
            return HeadRotation(baseOffset + speakingBobX, speakingYawY, speakingTiltZ)
        }

        val progress = elapsed.toFloat() / motionDurationMs
        val wave = sin(progress * Math.PI.toFloat())

        return when (activeMotion) {
            HeadMotion.SmallNod ->
                HeadRotation(baseOffset + speakingBobX + wave * 3f, speakingYawY, speakingTiltZ)
            HeadMotion.Nod ->
                HeadRotation(baseOffset + speakingBobX + wave * 5f, speakingYawY, speakingTiltZ)
            HeadMotion.Tilt ->
                HeadRotation(baseOffset + speakingBobX, speakingYawY, speakingTiltZ + wave * 6f)
            HeadMotion.Shake ->
                HeadRotation(
                    baseOffset + speakingBobX,
                    speakingYawY + sin(progress * 8 * Math.PI.toFloat()) * 5.5f,
                    speakingTiltZ
                )
            HeadMotion.Emphasis ->
                HeadRotation(baseOffset + speakingBobX - wave * 3f, speakingYawY, speakingTiltZ)
            HeadMotion.None ->
                HeadRotation(baseOffset + speakingBobX, speakingYawY, speakingTiltZ)
        }
    }
}

data class HeadRotation(val x: Float, val y: Float, val z: Float)
