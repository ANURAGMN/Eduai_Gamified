package com.ncert7.aitutorandlab.domain.gamification

/**
 * Local defaults for economy values. Remote Config `economy_config` can replace these later.
 */
object EconomyConfig {
    const val XP_CONCEPT = 10
    const val XP_SIMULATION = 10
    const val XP_SIMULATION_AGENT = 15
    const val XP_MATH_AGENT = 15
    const val XP_SCIENCE_AGENT = 20
    const val XP_REVISION_AGENT = 12
    const val XP_SESSION_BONUS = 10
    const val XP_STREAK_DAY = 5

    const val GEM_QUEST_SIMS = 15
    const val GEM_QUEST_STUDY = 15
    const val GEM_QUEST_BONUS = 30
    const val GEM_TRIAL_MANDATORY_CLAIM = 15
    const val GEM_INVITE_REWARD = 50
    const val MAX_QUEST_REWARDED_ADS_PER_DAY = 3
    const val TRIALS_PER_MANDATORY_AD = 2

    const val WEEKLY_XP_BAR_TARGET = 500

    const val ITEM_TYPE_SESSION_BONUS = "SESSION_BONUS"
    const val ITEM_TYPE_STREAK_DAY = "STREAK_DAY"

    fun xpForItemType(itemType: String): Int =
        when (itemType) {
            "CONCEPT" -> XP_CONCEPT
            "SIMULATION" -> XP_SIMULATION
            "SIMULATION_AGENT" -> XP_SIMULATION_AGENT
            "MATH_AGENT" -> XP_MATH_AGENT
            "SCIENCE_AGENT" -> XP_SCIENCE_AGENT
            "REVISION_AGENT" -> XP_REVISION_AGENT
            ITEM_TYPE_SESSION_BONUS -> XP_SESSION_BONUS
            ITEM_TYPE_STREAK_DAY -> XP_STREAK_DAY
            else -> 0
        }

    /** XP for completing one exam-trial list item (shown on Nice work! overlay). */
    fun xpForTrialKind(kind: String): Int =
        when (kind) {
            "STUDY" -> XP_CONCEPT
            "SIM_URL" -> XP_SIMULATION
            "SIM_AGENT" -> XP_SIMULATION_AGENT
            "REVISION" -> XP_REVISION_AGENT
            "MATH" -> XP_SIMULATION_AGENT
            else -> 0
        }

    fun trialXpItemType(kind: String): String = "TRIAL_$kind"

    fun trialDoubleXpItemType(kind: String): String = "TRIAL_DOUBLE_$kind"
}
