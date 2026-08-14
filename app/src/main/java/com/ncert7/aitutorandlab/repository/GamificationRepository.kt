package com.ncert7.aitutorandlab.repository

import com.ncert7.aitutorandlab.data.local.dao.GamificationDao
import com.ncert7.aitutorandlab.data.local.entities.GamificationProfileEntity
import com.ncert7.aitutorandlab.data.local.entities.GemEventEntity
import com.ncert7.aitutorandlab.data.local.entities.XpEventEntity
import com.ncert7.aitutorandlab.domain.gamification.EconomyConfig
import com.ncert7.aitutorandlab.domain.gamification.GamificationWeekKey
import com.ncert7.aitutorandlab.domain.gamification.QuestDayKey
import com.ncert7.aitutorandlab.service.analytics.GamificationAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.EconomySource
import kotlinx.coroutines.flow.Flow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class GamificationRepository @Inject constructor(
    private val gamificationDao: GamificationDao,
    private val leagueRepository: LeagueRepository,
) {
    fun observeProfile(studentId: String): Flow<GamificationProfileEntity?> =
        gamificationDao.observeProfile(studentId)

    suspend fun getProfile(studentId: String): GamificationProfileEntity? =
        gamificationDao.getProfile(studentId)

    suspend fun getOrCreateProfile(studentId: String): GamificationProfileEntity {
        val existing = gamificationDao.getProfile(studentId)
        if (existing != null) {
            return ensureWeekRollover(existing)
        }
        val profile =
            GamificationProfileEntity(
                studentId = studentId,
                currentWeekKey = GamificationWeekKey.current(),
                friendCode = generateFriendCode(),
            )
        gamificationDao.upsertProfile(profile)
        return profile
    }

    suspend fun hasXpEvent(
        studentId: String,
        itemType: String,
        itemId: String,
        language: String,
    ): Boolean = gamificationDao.countXpEvent(studentId, itemType, itemId, language) > 0

    /** Whether gems for this grant key were already recorded (grants are idempotent per grantKey). */
    suspend fun hasGemGrant(studentId: String, grantKey: String): Boolean =
        gamificationDao.countGemEvent(studentId, grantKey) > 0

    suspend fun recordXpAward(
        studentId: String,
        itemType: String,
        itemId: String,
        language: String,
        xpAmount: Int,
        countsForLeague: Boolean = true,
    ): GamificationProfileEntity? {
        if (xpAmount <= 0) return gamificationDao.getProfile(studentId)

        val event =
            XpEventEntity(
                studentId = studentId,
                itemType = itemType,
                itemId = itemId,
                language = language,
                xpAmount = xpAmount,
                weekKey = GamificationWeekKey.current(),
                countsForLeague = countsForLeague,
            )
        val rowId = gamificationDao.insertXpEvent(event)
        if (rowId == -1L) return null

        val profile = getOrCreateProfile(studentId)
        val rolled = ensureWeekRollover(profile)
        val weeklyDelta = if (countsForLeague) xpAmount else 0
        val updated =
            rolled.copy(
                lifetimeXp = rolled.lifetimeXp + xpAmount,
                weeklyXp = rolled.weeklyXp + weeklyDelta,
                updatedAt = System.currentTimeMillis(),
                isSynced = false,
            )
        gamificationDao.upsertProfile(updated)
        GamificationAnalyticsTracker.xpEarned(
            amount = xpAmount,
            source = xpSourceFor(itemType),
            kind = itemType,
        )
        return updated
    }

    private fun xpSourceFor(itemType: String): EconomySource =
        when {
            itemType.contains("TRIAL", ignoreCase = true) -> EconomySource.ITEM_COMPLETE
            itemType.contains("SESSION", ignoreCase = true) -> EconomySource.SESSION_BONUS
            itemType.contains("STREAK", ignoreCase = true) -> EconomySource.STREAK
            itemType.contains("QUEST", ignoreCase = true) -> EconomySource.QUEST
            else -> EconomySource.ITEM_COMPLETE
        }

    suspend fun grantGemsIfEligible(
        studentId: String,
        grantKey: String,
        gemsAmount: Int,
        source: String,
    ): Int? {
        if (gemsAmount <= 0 || grantKey.isBlank()) return null
        if (gamificationDao.countGemEvent(studentId, grantKey) > 0) return null

        val event =
            GemEventEntity(
                studentId = studentId,
                grantKey = grantKey,
                gemsAmount = gemsAmount,
                source = source,
            )
        val rowId = gamificationDao.insertGemEvent(event)
        if (rowId == -1L) return null

        val profile = getOrCreateProfile(studentId)
        val updated =
            profile.copy(
                gems = profile.gems + gemsAmount,
                updatedAt = System.currentTimeMillis(),
                isSynced = false,
            )
        gamificationDao.upsertProfile(updated)
        GamificationAnalyticsTracker.gemsEarned(
            amount = gemsAmount,
            source = gemSourceFor(source),
        )
        return gemsAmount
    }

    private fun gemSourceFor(source: String): EconomySource =
        when {
            source.contains("invite", ignoreCase = true) -> EconomySource.INVITE
            source.contains("quest", ignoreCase = true) -> EconomySource.QUEST
            source.contains("streak", ignoreCase = true) -> EconomySource.STREAK_MILESTONE
            source.contains("trial", ignoreCase = true) || source.contains("ad", ignoreCase = true) ->
                EconomySource.AD_CLAIM
            else -> EconomySource.AD_CLAIM
        }

    suspend fun canGrantQuestGemToday(studentId: String, questDate: String): Boolean {
        val start = QuestDayKey.startOfDayMillis(questDate)
        val end = QuestDayKey.endOfDayMillis(questDate)
        val count = gamificationDao.countQuestGemGrantsToday(studentId, start, end)
        return count < EconomyConfig.MAX_QUEST_REWARDED_ADS_PER_DAY
    }

    private suspend fun ensureWeekRollover(profile: GamificationProfileEntity): GamificationProfileEntity {
        val weekKey = GamificationWeekKey.current()
        if (profile.currentWeekKey == weekKey) return profile
        val rolled = leagueRepository.applyWeekRollover(profile, weekKey)
        gamificationDao.upsertProfile(rolled)
        return rolled
    }

    private fun generateFriendCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString {
            append(alphabet[Random.nextInt(alphabet.length)])
            repeat(7) {
                append(alphabet[Random.nextInt(alphabet.length)])
            }
        }.uppercase(Locale.US)
    }
}
