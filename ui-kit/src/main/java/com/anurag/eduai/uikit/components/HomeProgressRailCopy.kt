package com.anurag.eduai.uikit.components

/**
 * Localized chrome for [HomeProgressRail]. Call sites pass language-specific copy;
 * ui-kit defaults stay English.
 */
data class HomeProgressRailCopy(
    val sectionTitle: String = "Your week",
    val seeAllLabel: String = "See all",
    val streakTitle: (Int) -> String = { streak ->
        if (streak == 1) "1-day streak" else "$streak-day streak"
    },
    val streakSubtitle: String = "Keep it alive today",
    val leagueTitle: (String, Int) -> String = { leagueName, rank ->
        if (rank > 0) "$leagueName · Rank $rank" else leagueName
    },
    val leagueSubtitle: (Int) -> String = { promoteCount ->
        "Top $promoteCount promote · tap to open"
    },
    val inviteTitle: String = "Invite friends",
    val inviteSubtitle: String = "Learn together",
    val shareLabel: String = "Share",
)

fun defaultHomeProgressRailCopy(): HomeProgressRailCopy = HomeProgressRailCopy()
