package com.anurag.eduai.uikit.avatar.animation

import com.anurag.eduai.uikit.avatar.core.EmotionType
import com.anurag.eduai.uikit.avatar.core.HandGesture

/**
 * Emotion blends over ~300ms so Neutral → Thinking → Happy never snaps.
 */
class EmotionController {

    private var targetEmotion: EmotionType = EmotionType.Neutral
    private var currentSmile = 0f
    private var currentEyebrow = 0f
    private var currentSurprise = 0f

    fun setEmotion(emotion: EmotionType) {
        targetEmotion = emotion
    }

    fun update(deltaMs: Float): EmotionResult {
        val (targetSmile, targetEyebrow, targetSurprise) = when (targetEmotion) {
            EmotionType.Neutral -> Triple(0.28f, 0f, 0f) // tiny idle smile — "listening" warmth
            EmotionType.Teaching -> Triple(0.4f, 0.05f, 0f)
            EmotionType.Happy -> Triple(0.72f, 0f, 0.05f)
            EmotionType.Confused -> Triple(0.05f, 0.55f, 0.2f)
            EmotionType.Surprised -> Triple(0.1f, 0.75f, 0.9f)
            EmotionType.Celebrating -> Triple(0.9f, 0.1f, 0.35f)
            EmotionType.Explaining -> Triple(0.35f, 0.15f, 0.05f)
        }

        // 200–400ms window: 320ms feels natural for tutoring expressions.
        val lerp = (deltaMs / 320f).coerceIn(0f, 1f)
        currentSmile += (targetSmile - currentSmile) * lerp
        currentEyebrow += (targetEyebrow - currentEyebrow) * lerp
        currentSurprise += (targetSurprise - currentSurprise) * lerp

        return EmotionResult(currentSmile, currentEyebrow, currentSurprise)
    }
}

data class EmotionResult(
    val smileAmount: Float,
    val eyebrowOffset: Float,
    val eyeSurprise: Float
)

/**
 * Meaning-driven hand poses. Active gestures ease in/out; resting arms stay relaxed
 * at the sides so they don't thrash every sentence.
 */
class HandGestureController {

    private var activeGesture: HandGesture = HandGesture.None
    private var gestureStartMs: Long = 0
    private var gestureDurationMs: Long = 1400
    private var holdExtraMs: Long = 0

    fun trigger(gesture: HandGesture, timestampMs: Long, holdEmphasis: Boolean = false) {
        if (gesture == HandGesture.None) return
        activeGesture = gesture
        gestureStartMs = timestampMs
        gestureDurationMs = when (gesture) {
            HandGesture.Clap, HandGesture.Wave -> 1100
            HandGesture.Think -> 1600
            HandGesture.OpenPalm, HandGesture.Explain -> 1500
            HandGesture.PointDown, HandGesture.PointForward, HandGesture.PointLeft -> 1300
            else -> 1200
        }
        // Key teaching moments hold the pose a beat before returning to neutral.
        holdExtraMs = if (holdEmphasis) 450 else 0
    }

    fun update(timestampMs: Long): HandGestureResult {
        if (activeGesture == HandGesture.None) {
            return HandGestureResult(HandGesture.Relaxed, HandGesture.Relaxed, 6f, 6f)
        }

        val total = gestureDurationMs + holdExtraMs
        val elapsed = timestampMs - gestureStartMs
        if (elapsed > total) {
            activeGesture = HandGesture.None
            holdExtraMs = 0
            return HandGestureResult(HandGesture.Relaxed, HandGesture.Relaxed, 6f, 6f)
        }

        val progress = (elapsed.toFloat() / gestureDurationMs).coerceIn(0f, 1f)
        val lift = when {
            progress < 0.18f -> progress / 0.18f
            progress > 0.82f && elapsed <= gestureDurationMs -> (1f - progress) / 0.18f
            else -> 1f // plateau (includes emphasis hold)
        }

        val angle = -18f + lift * 48f

        return when (activeGesture) {
            HandGesture.PointDown -> HandGestureResult(HandGesture.PointDown, HandGesture.None, angle + 28f, -18f)
            HandGesture.PointForward -> HandGestureResult(HandGesture.PointForward, HandGesture.None, angle, -18f)
            HandGesture.PointLeft -> HandGestureResult(HandGesture.PointLeft, HandGesture.None, angle, -18f)
            HandGesture.ThumbsUp -> HandGestureResult(HandGesture.ThumbsUp, HandGesture.None, angle, -18f)
            HandGesture.Wave -> HandGestureResult(HandGesture.Wave, HandGesture.Wave, angle, angle)
            HandGesture.Clap -> HandGestureResult(HandGesture.Clap, HandGesture.Clap, angle, angle)
            HandGesture.Explain -> HandGestureResult(HandGesture.Explain, HandGesture.Explain, angle, angle - 8f)
            HandGesture.OpenPalm -> HandGestureResult(HandGesture.OpenPalm, HandGesture.OpenPalm, angle - 4f, angle)
            // Think: one hand rises toward the chin/cheek side.
            HandGesture.Think -> HandGestureResult(HandGesture.Think, HandGesture.None, angle + 18f, -12f)
            HandGesture.Relaxed, HandGesture.None -> HandGestureResult(HandGesture.Relaxed, HandGesture.Relaxed, 6f, 6f)
        }
    }
}

data class HandGestureResult(
    val leftGesture: HandGesture,
    val rightGesture: HandGesture,
    val leftArmAngle: Float,
    val rightArmAngle: Float
)
