package com.anurag.eduai.uikit.avatar.core

enum class TutorCharacter {
    Boy, Girl, Free, Orb;

    val assetName: String?
        get() = when (this) {
            Boy -> "avatar_boy"
            Girl -> "avatar_girl"
            Free -> null
            Orb -> null
        }

    // Only Boy/Girl use the PNG-portrait pipeline. Free (code-drawn character) and Orb
    // (reactive glowing visual) are both Compose-drawn.
    val usesPortrait: Boolean
        get() = this == Boy || this == Girl

    val label: String
        get() = when (this) {
            Boy -> "Boy"
            Girl -> "Girl"
            Free -> "Free"
            Orb -> "Orb"
        }
}

enum class Viseme {
    Rest, Closed, Wide, Open, Round, FV, Th, Smush, Kiss
}
