package com.ncert7.aitutorandlab.domain.gamification

import com.ncert7.aitutorandlab.data.local.entities.ConceptEntity
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.repository.ConceptRepository
import com.ncert7.aitutorandlab.repository.GamificationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GamificationRewardService @Inject constructor(
    private val gamificationRepository: GamificationRepository,
    private val conceptRepository: ConceptRepository,
    private val rewardEventBus: RewardEventBus,
    private val friendFeedService: FriendFeedService,
) {
    companion object {
        private const val TAG = "GamificationRewardService"
    }

    suspend fun awardXpIfEligible(
        studentId: String,
        itemType: String,
        itemId: String,
        language: String,
    ): XpAwardResult? {
        val xpAmount = EconomyConfig.xpForItemType(itemType)
        if (xpAmount <= 0) return null

        val before = gamificationRepository.getOrCreateProfile(studentId)
        val weeklyBefore = before.weeklyXp
        val lifetimeBefore = before.lifetimeXp
        val updated =
            gamificationRepository.recordXpAward(
                studentId = studentId,
                itemType = itemType,
                itemId = itemId,
                language = language,
                xpAmount = xpAmount,
                countsForLeague = true,
            ) ?: return null

        var totalXp = xpAmount
        var weeklyAfter = updated.weeklyXp
        var lifetimeXp = updated.lifetimeXp

        val bonus = tryAwardSessionBonus(studentId, itemId, language)
        if (bonus != null) {
            totalXp += bonus.xpAmount
            weeklyAfter += bonus.xpAmount
            lifetimeXp += bonus.xpAmount
        }

        emitRewardEvent(
            xpEarned = totalXp,
            weeklyXpBefore = weeklyBefore,
            weeklyXpAfter = weeklyAfter,
        )

        runCatching {
            friendFeedService.onXpAwarded(studentId, lifetimeBefore, lifetimeXp)
        }

        DebugLogger.debugLog(TAG, "Awarded $totalXp XP for $itemType/$itemId ($language)")

        return XpAwardResult(
            xpAmount = totalXp,
            lifetimeXp = lifetimeXp,
            weeklyXp = weeklyAfter,
            itemType = itemType,
            itemId = itemId,
        )
    }

    /**
     * Awards XP for finishing a trial list item and returns display data for the
     * Nice work! overlay — does **not** emit [RewardEventBus] (avoids double overlay).
     */
    suspend fun awardTrialItemXp(
        studentId: String,
        trialItemId: Long,
        kind: String,
        language: String,
    ): RewardUiEvent? {
        val xpAmount = EconomyConfig.xpForTrialKind(kind)
        if (xpAmount <= 0) return null

        val itemType = EconomyConfig.trialXpItemType(kind)
        val itemId = trialItemId.toString()
        val target = EconomyConfig.WEEKLY_XP_BAR_TARGET.toFloat()
        val profile = gamificationRepository.getOrCreateProfile(studentId)
        val weeklyBefore = profile.weeklyXp
        val lifetimeBefore = profile.lifetimeXp

        if (gamificationRepository.hasXpEvent(studentId, itemType, itemId, language)) {
            return RewardUiEvent(
                xpEarned = xpAmount,
                gemsEarned = 0,
                xpBarFrom = (weeklyBefore / target).coerceIn(0f, 1f),
                xpBarTo = (weeklyBefore / target).coerceIn(0f, 1f),
                weeklyXpTotal = weeklyBefore,
            )
        }

        val updated =
            gamificationRepository.recordXpAward(
                studentId = studentId,
                itemType = itemType,
                itemId = itemId,
                language = language,
                xpAmount = xpAmount,
                countsForLeague = true,
            ) ?: return null

        runCatching {
            friendFeedService.onXpAwarded(studentId, lifetimeBefore, updated.lifetimeXp)
        }

        DebugLogger.debugLog(TAG, "Trial item $trialItemId ($kind) +$xpAmount XP")

        return RewardUiEvent(
            xpEarned = xpAmount,
            gemsEarned = 0,
            xpBarFrom = (weeklyBefore / target).coerceIn(0f, 1f),
            xpBarTo = (updated.weeklyXp / target).coerceIn(0f, 1f),
            weeklyXpTotal = updated.weeklyXp,
        )
    }

    /** Bonus XP from rewarded ad — lifetime only, does not affect weekly league rank. */
    suspend fun awardTrialDoubleXp(
        studentId: String,
        trialItemId: Long,
        kind: String,
        language: String,
    ): RewardUiEvent? {
        val xpAmount = EconomyConfig.xpForTrialKind(kind)
        if (xpAmount <= 0) return null

        val itemType = EconomyConfig.trialDoubleXpItemType(kind)
        val itemId = trialItemId.toString()
        if (gamificationRepository.hasXpEvent(studentId, itemType, itemId, language)) {
            return RewardUiEvent(
                xpEarned = xpAmount,
                gemsEarned = 0,
                xpBarFrom = 0f,
                xpBarTo = 0f,
                weeklyXpTotal = gamificationRepository.getProfile(studentId)?.weeklyXp ?: 0,
            )
        }

        val profile = gamificationRepository.getOrCreateProfile(studentId)
        val weeklyXp = profile.weeklyXp
        val target = EconomyConfig.WEEKLY_XP_BAR_TARGET.toFloat()

        val updated =
            gamificationRepository.recordXpAward(
                studentId = studentId,
                itemType = itemType,
                itemId = itemId,
                language = language,
                xpAmount = xpAmount,
                countsForLeague = false,
            ) ?: return null

        DebugLogger.debugLog(TAG, "Trial double XP +$xpAmount lifetime for item $trialItemId ($kind)")

        return RewardUiEvent(
            xpEarned = xpAmount,
            gemsEarned = 0,
            xpBarFrom = (weeklyXp / target).coerceIn(0f, 1f),
            xpBarTo = (weeklyXp / target).coerceIn(0f, 1f),
            weeklyXpTotal = updated.weeklyXp,
        )
    }

    suspend fun awardSessionBonusIfEligible(
        studentId: String,
        conceptId: String,
        language: String,
    ): XpAwardResult? = tryAwardSessionBonus(studentId, conceptId, language)

    private suspend fun tryAwardSessionBonus(
        studentId: String,
        conceptId: String,
        language: String,
    ): XpAwardResult? {
        if (gamificationRepository.hasXpEvent(
                studentId,
                EconomyConfig.ITEM_TYPE_SESSION_BONUS,
                conceptId,
                language,
            )
        ) {
            return null
        }

        val concept = conceptRepository.getConcept(conceptId) ?: return null
        if (!isConceptSessionComplete(studentId, concept, language)) return null

        val updated =
            gamificationRepository.recordXpAward(
                studentId = studentId,
                itemType = EconomyConfig.ITEM_TYPE_SESSION_BONUS,
                itemId = conceptId,
                language = language,
                xpAmount = EconomyConfig.XP_SESSION_BONUS,
                countsForLeague = true,
            ) ?: return null

        DebugLogger.debugLog(TAG, "Session bonus +${EconomyConfig.XP_SESSION_BONUS} XP for concept $conceptId")

        return XpAwardResult(
            xpAmount = EconomyConfig.XP_SESSION_BONUS,
            lifetimeXp = updated.lifetimeXp,
            weeklyXp = updated.weeklyXp,
            itemType = EconomyConfig.ITEM_TYPE_SESSION_BONUS,
            itemId = conceptId,
        )
    }

    private suspend fun isConceptSessionComplete(
        studentId: String,
        concept: ConceptEntity,
        language: String,
    ): Boolean {
        suspend fun isDone(type: String): Boolean {
            val progress =
                conceptRepository.getProgress(
                    studentId = studentId,
                    itemType = type,
                    itemId = concept.conceptId,
                    language,
                )
            return progress?.status == "COMPLETED"
        }

        if (!isDone("CONCEPT")) return false

        if (!concept.simulationId.isNullOrBlank()) {
            if (!isDone("SIMULATION")) return false
            if (!isDone("SIMULATION_AGENT")) return false
        }

        if (concept.type.equals("math", ignoreCase = true) ||
            concept.type.equals("MATH PROBLEM", ignoreCase = true)
        ) {
            if (!isDone("MATH_AGENT")) return false
        }

        if (concept.type.equals("science", ignoreCase = true)) {
            val science =
                conceptRepository.getProgress(
                    studentId = studentId,
                    itemType = "SCIENCE_AGENT",
                    itemId = concept.conceptId,
                    language,
                )
            if (science?.status != "COMPLETED" || science.progressPercentage < 100) return false
        }

        return true
    }

    private fun emitRewardEvent(
        xpEarned: Int,
        weeklyXpBefore: Int,
        weeklyXpAfter: Int,
    ) {
        val target = EconomyConfig.WEEKLY_XP_BAR_TARGET.toFloat()
        rewardEventBus.tryEmit(
            RewardUiEvent(
                xpEarned = xpEarned,
                gemsEarned = 0,
                xpBarFrom = (weeklyXpBefore / target).coerceIn(0f, 1f),
                xpBarTo = (weeklyXpAfter / target).coerceIn(0f, 1f),
                weeklyXpTotal = weeklyXpAfter,
            ),
        )
    }
}
