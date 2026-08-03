package com.ncert7.aitutorandlab.domain.gamification

enum class QuestClaimType {
    SIMS,
    STUDY,
    BONUS,
    ;

    fun grantKey(questDate: String): String =
        when (this) {
            SIMS -> "quest_sims_$questDate"
            STUDY -> "quest_study_$questDate"
            BONUS -> "quest_bonus_$questDate"
        }

    fun gemAmount(): Int =
        when (this) {
            SIMS -> EconomyConfig.GEM_QUEST_SIMS
            STUDY -> EconomyConfig.GEM_QUEST_STUDY
            BONUS -> EconomyConfig.GEM_QUEST_BONUS
        }

    fun sourceLabel(): String =
        when (this) {
            SIMS -> "QUEST_SIMS"
            STUDY -> "QUEST_STUDY"
            BONUS -> "QUEST_BONUS"
        }
}
