package com.anurag.eduai.uikit.events

sealed interface GamificationEvent {
    val userId: String
    val timestamp: Long

    data class XpEarned(
        override val userId: String,
        val amount: Int,
        val source: String,
        val scope: XpScope,
        override val timestamp: Long = System.currentTimeMillis(),
    ) : GamificationEvent

    data class StreakDayCompleted(
        override val userId: String,
        val streakCount: Int,
        val frozen: Boolean,
        override val timestamp: Long = System.currentTimeMillis(),
    ) : GamificationEvent

    data class LearningStepCompleted(
        override val userId: String,
        val topicId: String,
        val step: LearningStep,
        val score: Int? = null,
        override val timestamp: Long = System.currentTimeMillis(),
    ) : GamificationEvent

    data class QuestCompleted(
        override val userId: String,
        val questId: String,
        val rewardType: String,
        val rewardValue: Int,
        override val timestamp: Long = System.currentTimeMillis(),
    ) : GamificationEvent

    data class BadgeEarned(
        override val userId: String,
        val badgeId: String,
        override val timestamp: Long = System.currentTimeMillis(),
    ) : GamificationEvent

    data class BookmarkToggled(
        override val userId: String,
        val topicId: String,
        val type: BookmarkType,
        val bookmarked: Boolean,
        override val timestamp: Long = System.currentTimeMillis(),
    ) : GamificationEvent
}
