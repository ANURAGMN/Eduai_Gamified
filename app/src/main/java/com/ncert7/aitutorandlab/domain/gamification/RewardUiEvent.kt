package com.ncert7.aitutorandlab.domain.gamification

data class RewardUiEvent(
    val xpEarned: Int,
    val gemsEarned: Int = 0,
    val xpBarFrom: Float,
    val xpBarTo: Float,
    val weeklyXpTotal: Int,
)

data class XpAwardResult(
    val xpAmount: Int,
    val lifetimeXp: Int,
    val weeklyXp: Int,
    val itemType: String,
    val itemId: String,
)
