package com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components

/**
 * Chat / agent message typography.
 *
 * Base sizes are what the learner picks in settings. Math agent applies a small −0.5sp offset
 * so its chat reads slightly tighter than Study/Revision at the same preference.
 */
object ChatMessageFontSize {
    /** Default for all Study / Math / Revision / Sim agents (XS). */
    const val DEFAULT_SP = 28f
    const val MATH_OFFSET_SP = -0.5f
    const val MIN_SP = 24f
    const val MAX_SP = 52f

    /**
     * Presets in the in-session settings menu (label → base sp).
     * XS is the default (“ultra small”) for dense agent text.
     */
    val PRESETS: List<Pair<String, Float>> =
        listOf(
            "XS" to 28f,
            "S" to 32f,
            "M" to 36f,
            "L" to 40f,
            "XL" to 44f,
        )

    fun coerce(sp: Float): Float = sp.coerceIn(MIN_SP, MAX_SP)

    fun lineHeightSp(fontSp: Float): Float = fontSp * 1.45f

    /** Effective size for rendering. [mathAgent] shaves 0.5sp off the learner's base choice. */
    fun resolveFontSp(baseSp: Float, mathAgent: Boolean): Float {
        val adjusted = baseSp + if (mathAgent) MATH_OFFSET_SP else 0f
        return coerce(adjusted)
    }

    fun nearestPreset(baseSp: Float): Float =
        PRESETS.minByOrNull { kotlin.math.abs(it.second - baseSp) }?.second ?: DEFAULT_SP
}
