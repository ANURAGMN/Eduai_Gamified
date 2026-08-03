package com.anurag.eduai.uikit.avatar.core

enum class AvatarState {
    Idle,
    Listening,
    Thinking,
    Speaking,
    Happy,
    Confused,
    Celebrating,
    Explaining;

    val blinkFrequencyMultiplier: Float
        get() = when (this) {
            Thinking, Confused -> 0.6f
            Speaking, Explaining -> 1.0f
            Celebrating, Happy -> 1.3f
            else -> 0.8f
        }

    val breathingAmplitude: Float
        get() = when (this) {
            Speaking, Explaining -> 1.2f
            Celebrating -> 1.5f
            Thinking -> 0.7f
            else -> 1.0f
        }

    val headPostureOffset: Float
        get() = when (this) {
            Listening -> -3f
            Thinking -> 5f
            Confused -> 4f
            Explaining -> -2f
            else -> 0f
        }
}
