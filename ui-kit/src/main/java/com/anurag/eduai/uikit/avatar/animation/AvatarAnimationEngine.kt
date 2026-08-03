package com.anurag.eduai.uikit.avatar.animation

import com.anurag.eduai.uikit.avatar.core.AvatarFrame
import com.anurag.eduai.uikit.avatar.core.AvatarState
import com.anurag.eduai.uikit.avatar.core.GesturePlan
import com.anurag.eduai.uikit.avatar.core.HandGesture
import com.anurag.eduai.uikit.avatar.core.MouthShape

class AvatarAnimationEngine {

    private val lipSync = LipSyncController()
    private val idleMotion = IdleMotionController()
    private val headMotion = HeadMotionController()
    private val emotion = EmotionController()
    private val hands = HandGestureController()

    private var state: AvatarState = AvatarState.Idle
    private var isSpeaking = false
    private var eyebrowRaiseActive = false
    private var lastGestureMs: Long = -10_000L
    private var emphasisLean = 0f

    fun setState(newState: AvatarState) {
        state = newState
    }

    fun beginSpeaking(gesturePlan: GesturePlan, timestampMs: Long, spokenText: String = "", durationMs: Long = 0) {
        isSpeaking = true
        eyebrowRaiseActive = gesturePlan.eyebrowRaise
        state = when (gesturePlan.emotion) {
            com.anurag.eduai.uikit.avatar.core.EmotionType.Celebrating -> AvatarState.Celebrating
            com.anurag.eduai.uikit.avatar.core.EmotionType.Happy -> AvatarState.Happy
            com.anurag.eduai.uikit.avatar.core.EmotionType.Confused -> AvatarState.Confused
            com.anurag.eduai.uikit.avatar.core.EmotionType.Explaining, com.anurag.eduai.uikit.avatar.core.EmotionType.Teaching -> AvatarState.Explaining
            else -> AvatarState.Speaking
        }
        emotion.setEmotion(gesturePlan.emotion)
        headMotion.trigger(gesturePlan.headMotion, timestampMs)

        // Gestures every ~5–8s (or always for high-signal praise/emphasis).
        val highSignal = gesturePlan.gesture in setOf(
            HandGesture.Clap, HandGesture.Wave, HandGesture.OpenPalm, HandGesture.Think
        ) || gesturePlan.holdEmphasis
        val enoughGap = timestampMs - lastGestureMs >= 5500
        if (gesturePlan.gesture != HandGesture.None && (highSignal || enoughGap)) {
            hands.trigger(gesturePlan.gesture, timestampMs, holdEmphasis = gesturePlan.holdEmphasis)
            lastGestureMs = timestampMs
        }

        emphasisLean = if (gesturePlan.holdEmphasis || gesturePlan.eyebrowRaise) 4.5f else 0f

        if (spokenText.isNotBlank() && durationMs > 0) {
            lipSync.startPhonemeTimeline(spokenText, durationMs, timestampMs)
        }
    }

    /** Forward a real TTS word boundary so the lip-sync timeline re-anchors to the audio. */
    fun noteWordBoundary(wordIndex: Int, timestampMs: Long) {
        lipSync.noteWordBoundary(wordIndex, timestampMs)
    }

    fun endSpeaking() {
        isSpeaking = false
        eyebrowRaiseActive = false
        emphasisLean = 0f
        lipSync.reset()
        state = AvatarState.Idle
        emotion.setEmotion(com.anurag.eduai.uikit.avatar.core.EmotionType.Neutral)
    }

    fun setListening() {
        state = AvatarState.Listening
        isSpeaking = false
        emotion.setEmotion(com.anurag.eduai.uikit.avatar.core.EmotionType.Neutral)
    }

    fun setThinking(timestampMs: Long = System.currentTimeMillis()) {
        state = AvatarState.Thinking
        isSpeaking = false
        lipSync.reset()
        emotion.setEmotion(com.anurag.eduai.uikit.avatar.core.EmotionType.Neutral)
        headMotion.trigger(com.anurag.eduai.uikit.avatar.core.HeadMotion.Tilt, timestampMs)
        hands.trigger(HandGesture.Think, timestampMs)
    }

    fun update(timestampMs: Long, deltaMs: Long): AvatarFrame {
        // Emotion first so idle eyes can squint from the current smile.
        val emotionResult = emotion.update(deltaMs.toFloat())
        val idle = idleMotion.update(
            state = state,
            timestampMs = timestampMs,
            deltaMs = deltaMs,
            isSpeaking = isSpeaking,
            smileAmount = emotionResult.smileAmount
        )
        val head = headMotion.update(timestampMs, idle.headMicroMotion, isSpeaking, deltaMs)
        val (mouth, viseme) = lipSync.update(isSpeaking, timestampMs)
        val handResult = hands.update(timestampMs)

        val finalMouth = if (emotionResult.smileAmount > 0.5f && !isSpeaking) {
            MouthShape.Smile
        } else {
            mouth
        }

        val leanTarget = state.headPostureOffset * 0.3f + emphasisLean
        // Soft lean blend so emphasis eases in/out with the rest of the body.
        val bodyLean = leanTarget + idle.shoulderOffset * 0.4f

        return AvatarFrame(
            state = state,
            mouthShape = finalMouth,
            viseme = viseme,
            headRotationX = head.x,
            headRotationY = head.y,
            headRotationZ = head.z,
            eyeMotion = idle.eyeMotion,
            eyeBlinkProgress = idle.eyeBlinkProgress,
            pupilOffsetX = idle.pupilOffsetX,
            pupilOffsetY = idle.pupilOffsetY,
            bodySway = idle.bodySway,
            bodyBreathing = idle.bodyBreathing,
            bodyLean = bodyLean,
            bodyBounce = idle.bodyBounce,
            leftArmAngle = handResult.leftArmAngle,
            rightArmAngle = handResult.rightArmAngle,
            leftHandGesture = handResult.leftGesture,
            rightHandGesture = handResult.rightGesture,
            eyebrowOffset = emotionResult.eyebrowOffset + if (eyebrowRaiseActive) 0.45f else 0f,
            smileAmount = emotionResult.smileAmount,
            eyeSurprise = emotionResult.eyeSurprise,
            eyeSquint = idle.eyeSquint,
            isSpeaking = isSpeaking
        )
    }
}
