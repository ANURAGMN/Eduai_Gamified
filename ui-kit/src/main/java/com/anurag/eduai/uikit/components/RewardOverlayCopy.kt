package com.anurag.eduai.uikit.components

/**
 * Localized chrome for [RewardOverlay]. Call sites pass language-specific copy;
 * ui-kit defaults stay English.
 */
data class RewardOverlayCopy(
    val title: String = "Day complete!",
    val subtitle: String = "You finished today's focus",
    val weeklyXpLabel: String = "Weekly XP",
    val xpEarnedLabel: String = "XP earned",
    val gemsLabel: String = "Gems",
    val collectCta: String = "Collect reward",
)

fun defaultRewardOverlayCopy(): RewardOverlayCopy = RewardOverlayCopy()
