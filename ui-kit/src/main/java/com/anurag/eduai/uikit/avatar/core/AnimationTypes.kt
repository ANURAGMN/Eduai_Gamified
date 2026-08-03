package com.anurag.eduai.uikit.avatar.core

enum class AnimationLayer(val priority: Int) {
    LipSync(6),
    Gesture(5),
    Emotion(4),
    HeadMotion(3),
    Eyes(2),
    Body(1),
    Idle(0)
}

enum class MouthShape {
    Closed, A, E, I, O, U, Smile
}

enum class HeadMotion {
    None, SmallNod, Nod, Tilt, Shake, Emphasis
}

enum class EyeMotion {
    Neutral, Blink, Wink, LookLeft, LookRight, LookUp, LookDown, Surprised
}

enum class HandGesture {
    None, Relaxed, PointDown, PointForward, PointLeft, Wave, Clap, Explain, ThumbsUp,
    /** One hand rises toward the chin — "think about it". */
    Think,
    /** Both palms open outward — "let's calculate / here's the idea". */
    OpenPalm
}

enum class EmotionType {
    Neutral, Teaching, Happy, Confused, Surprised, Celebrating, Explaining
}
