package com.anurag.eduai.uikit.avatar.core

data class GesturePlan(
    val gesture: HandGesture,
    val emotion: EmotionType,
    val headMotion: HeadMotion,
    val eyebrowRaise: Boolean = false,
    /** Hold the final pose briefly for key teaching moments. */
    val holdEmphasis: Boolean = false,
) {
    companion object {
        /** Neutral teaching pose used when wiring Android TTS word boundaries. */
        val NeutralSpeaking =
            GesturePlan(
                gesture = HandGesture.None,
                emotion = EmotionType.Teaching,
                headMotion = HeadMotion.SmallNod,
            )
    }
}

data class SentencePlan(
    val text: String,
    val gesturePlan: GesturePlan,
    val estimatedDurationMs: Long
)

data class AvatarFrame(
    val state: AvatarState,
    val mouthShape: MouthShape,
    val viseme: Viseme,
    val headRotationX: Float,
    val headRotationY: Float,
    val headRotationZ: Float,
    val eyeMotion: EyeMotion,
    val eyeBlinkProgress: Float = 0f,
    val pupilOffsetX: Float,
    val pupilOffsetY: Float,
    val bodySway: Float,
    val bodyBreathing: Float,
    val bodyLean: Float,
    val bodyBounce: Float = 0f,
    val leftArmAngle: Float,
    val rightArmAngle: Float,
    val leftHandGesture: HandGesture,
    val rightHandGesture: HandGesture,
    val eyebrowOffset: Float,
    val smileAmount: Float,
    val eyeSurprise: Float,
    val eyeSquint: Float = 0f,
    // True while a sentence is actively being spoken — the mouth should lip-sync in ANY
    // emotional state (happy/celebrating/confused), not only Speaking/Explaining.
    val isSpeaking: Boolean = false
)
