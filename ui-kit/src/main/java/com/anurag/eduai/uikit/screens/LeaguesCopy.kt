package com.anurag.eduai.uikit.screens

/**
 * Localized chrome for the leagues tab. Call sites pass language-specific copy;
 * ui-kit defaults stay English.
 */
data class LeaguesCopy(
    val sectionTitle: String = "Leagues",
    val daysLeft: (Int) -> String = { days ->
        if (days == 1) "1 day left" else "$days days left"
    },
    val standingLine: (Int, Int, Int, String) -> String = { rank, total, promoCount, target ->
        "You're rank $rank of $total — top $promoCount promote to $target"
    },
    val promotionZone: (String) -> String = { target ->
        "Promotion zone · advances to $target"
    },
    val safeZone: String = "Safe zone",
    val demotionZone: String = "Demotion zone",
)

fun defaultLeaguesCopy(): LeaguesCopy = LeaguesCopy()
