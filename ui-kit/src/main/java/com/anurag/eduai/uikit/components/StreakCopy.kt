package com.anurag.eduai.uikit.components

/**
 * Localized chrome for streak celebration overlays. Call sites in the app module pass language-
 * specific copy; ui-kit defaults stay English.
 */
data class StreakCopy(
    val dayStreakLabel: String = "day streak",
    val greetingLine: (String) -> String = { name -> "Good to see you, $name — keep it alive today." },
    val continueLabel: String = "Let's go",
    val extendedTitle: String = "Streak extended!",
    val extendedLine: (String) -> String = { name -> "+1 day — great work today, $name." },
    val awesomeLabel: String = "Awesome",
    val fallbackName: String = "there",
    /** Mon…Sun short labels for the week row. */
    val weekdayLetters: List<String> = listOf("M", "T", "W", "T", "F", "S", "S"),
)

fun defaultStreakCopy(): StreakCopy = StreakCopy()
