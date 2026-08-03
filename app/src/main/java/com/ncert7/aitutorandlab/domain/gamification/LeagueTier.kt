package com.ncert7.aitutorandlab.domain.gamification

import java.util.Locale

enum class LeagueTier(val storageKey: String) {
    BRONZE("BRONZE"),
    SILVER("SILVER"),
    GOLD("GOLD"),
    ;

    fun displayName(): String =
        storageKey.lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) }

    fun leagueTitle(): String = "${displayName()} League"

    fun promotionTarget(): LeagueTier? =
        when (this) {
            BRONZE -> SILVER
            SILVER -> GOLD
            GOLD -> null
        }

    fun demotionTarget(): LeagueTier? =
        when (this) {
            GOLD -> SILVER
            SILVER -> BRONZE
            BRONZE -> null
        }

    companion object {
        fun fromStorage(value: String?): LeagueTier {
            val normalized = value?.trim()?.uppercase(Locale.US).orEmpty()
            return entries.firstOrNull { it.storageKey == normalized } ?: BRONZE
        }

        fun adjustAfterWeek(rank: Int, participantCount: Int, current: LeagueTier): LeagueTier {
            if (participantCount <= 0) return current
            val promoteCutoff = LeagueConfig.PROMOTION_COUNT
            val demoteStart = participantCount - LeagueConfig.DEMOTION_COUNT + 1
            return when {
                rank <= promoteCutoff && current.promotionTarget() != null -> current.promotionTarget()!!
                rank >= demoteStart && current.demotionTarget() != null -> current.demotionTarget()!!
                else -> current
            }
        }
    }
}
