package com.anurag.eduai.uikit.components

data class PlanTrialOverlayLabels(
    val weeklyXp: String = "Weekly XP",
    val xpEarned: String = "XP earned",
    val bonusXp: String = "Bonus XP",
    val gems: String = "Gems",
    val playingRewardAd: (Int) -> String = { gems -> "Playing reward ad to claim +$gems gems…" },
    val loadingRewardAd: String = "Loading reward ad…",
    val mandatoryAdWatch: (Int) -> String = { gems -> "Watch ad for +$gems gems" },
    val skipMandatoryAd: (Int) -> String = { gems -> "Continue without +$gems gems" },
    val mandatoryAdSkipped: (Int) -> String = { gems ->
        "You didn't watch the full ad — +$gems gems weren't added."
    },
    val doubleXpWatch: (Int) -> String = { amount -> "Double XP · watch ad (+$amount bonus)" },
    val doubleXpLoading: String = "Double XP · loading ad…",
    val bonusXpAdded: (Int) -> String = { amount -> "+$amount bonus XP added to your profile" },
    val startingIn: (Int) -> String = { seconds -> "Starting in $seconds…" },
)
