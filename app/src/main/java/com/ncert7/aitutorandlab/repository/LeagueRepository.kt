package com.ncert7.aitutorandlab.repository

import com.ncert7.aitutorandlab.data.local.dao.GamificationDao
import com.ncert7.aitutorandlab.data.local.dao.LeagueDao
import com.ncert7.aitutorandlab.data.local.entities.GamificationProfileEntity
import com.ncert7.aitutorandlab.data.local.entities.LeagueCacheEntity
import com.ncert7.aitutorandlab.data.local.entities.LeagueMemberEntity
import com.ncert7.aitutorandlab.domain.gamification.FriendFeedService
import com.ncert7.aitutorandlab.domain.gamification.GamificationWeekKey
import com.ncert7.aitutorandlab.domain.gamification.LeagueCohortEngine
import com.ncert7.aitutorandlab.domain.gamification.LeagueConfig
import com.ncert7.aitutorandlab.domain.gamification.LeagueTier
import com.ncert7.aitutorandlab.domain.gamification.RankedCohort
import com.ncert7.aitutorandlab.service.analytics.GamificationAnalyticsTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class LeagueBoardState(
    val tier: LeagueTier,
    val weekKey: String,
    val cohortId: String,
    val daysRemaining: Int,
    val rankedCohort: RankedCohort,
)

data class LeagueRankProjection(
    val currentRank: Int,
    val projectedRank: Int,
    val tier: LeagueTier,
    val pendingXp: Int,
) {
    fun promotionHint(): String? {
        val target = tier.promotionTarget() ?: return null
        return if (projectedRank <= LeagueConfig.PROMOTION_COUNT) {
            "Finish to push for ${target.displayName()} League!"
        } else {
            null
        }
    }

    fun rankHint(): String =
        if (projectedRank < currentRank) {
            "You could move up to #$projectedRank in ${tier.leagueTitle()}"
        } else {
            "Stay in the top ${LeagueConfig.PROMOTION_COUNT} to reach ${tier.promotionTarget()?.displayName() ?: "the next"} tier"
        }
}

@Singleton
class LeagueRepository @Inject constructor(
    private val leagueDao: LeagueDao,
    private val gamificationDao: GamificationDao,
    private val friendRepository: FriendRepository,
    private val friendFeedService: FriendFeedService,
) {
    fun observeBoardState(
        studentId: String,
        displayName: String,
        streak: Int,
    ): Flow<LeagueBoardState?> {
        if (studentId.isBlank()) return kotlinx.coroutines.flow.flowOf(null)
        return combine(
            gamificationDao.observeProfile(studentId),
            observeMembersForProfile(studentId),
        ) { profile, members ->
            val activeProfile = profile ?: return@combine null
            val weekKey = GamificationWeekKey.current()
            val tier = LeagueTier.fromStorage(activeProfile.leagueTier)
            val cohortId = activeProfile.cohortId ?: return@combine null
            if (members.isEmpty()) return@combine null
            val ranked = LeagueCohortEngine.rankMembers(members, studentId)
            LeagueBoardState(
                tier = tier,
                weekKey = weekKey,
                cohortId = cohortId,
                daysRemaining = GamificationWeekKey.daysRemainingInWeek(),
                rankedCohort = ranked,
            )
        }.distinctUntilChanged()
    }

    fun observeCachedRank(studentId: String): Flow<Int> =
        leagueDao.observeCache(studentId).map { cache ->
            val weekKey = GamificationWeekKey.current()
            if (cache != null && cache.weekKey == weekKey && cache.rank > 0) {
                cache.rank
            } else {
                0
            }
        }

    suspend fun refreshHomeLeagueCache(
        studentId: String,
        displayName: String,
        streak: Int,
    ): Int {
        if (studentId.isBlank()) return 0
        val weekKey = GamificationWeekKey.current()
        val cache = leagueDao.getCache(studentId)
        val now = System.currentTimeMillis()
        if (
            cache != null &&
                cache.weekKey == weekKey &&
                now - cache.fetchedAt < LeagueConfig.CACHE_STALE_MS &&
                cache.rank > 0
        ) {
            return cache.rank
        }
        val profile = gamificationDao.getProfile(studentId) ?: return 0
        val cohortId = ensureCohort(studentId, displayName, streak, profile)
        return updateRankCache(studentId, cohortId)
    }

    suspend fun refreshLeagueBoard(
        studentId: String,
        displayName: String,
        streak: Int,
    ): LeagueBoardState? {
        if (studentId.isBlank()) return null
        val profile = gamificationDao.getProfile(studentId) ?: return null
        val cohortId = ensureCohort(studentId, displayName, streak, profile)
        val weekKey = GamificationWeekKey.current()
        val members = leagueDao.getMembers(weekKey, cohortId)
        val ranked = LeagueCohortEngine.rankMembers(members, studentId)
        updateRankCache(studentId, cohortId, ranked)
        return LeagueBoardState(
            tier = LeagueTier.fromStorage(profile.leagueTier),
            weekKey = weekKey,
            cohortId = cohortId,
            daysRemaining = GamificationWeekKey.daysRemainingInWeek(),
            rankedCohort = ranked,
        )
    }

    suspend fun applyWeekRollover(
        profile: GamificationProfileEntity,
        newWeekKey: String,
    ): GamificationProfileEntity {
        if (profile.currentWeekKey == newWeekKey) return profile

        val previousTier = LeagueTier.fromStorage(profile.leagueTier)
        var tier = previousTier
        val oldWeekKey = profile.currentWeekKey
        val oldCohortId = profile.cohortId
        if (oldWeekKey.isNotBlank() && !oldCohortId.isNullOrBlank()) {
            val members = leagueDao.getMembers(oldWeekKey, oldCohortId)
            if (members.isNotEmpty()) {
                val ranked = LeagueCohortEngine.rankMembers(members, profile.studentId)
                tier =
                    LeagueTier.adjustAfterWeek(
                        rank = ranked.userRank,
                        participantCount = members.size,
                        current = tier,
                    )
            }
        }

        if (tier.ordinal > previousTier.ordinal) {
            GamificationAnalyticsTracker.leaguePromoted(previousTier, tier)
            friendRepository.publishLeaguePromotionIfNeeded(
                studentId = profile.studentId,
                previousTier = previousTier,
                newTier = tier,
            )
        } else if (tier.ordinal < previousTier.ordinal) {
            GamificationAnalyticsTracker.leagueDemoted(previousTier, tier)
        }

        return profile.copy(
            currentWeekKey = newWeekKey,
            weeklyXp = 0,
            cohortId = null,
            leagueTier = tier.storageKey,
            updatedAt = System.currentTimeMillis(),
            isSynced = false,
        )
    }

    suspend fun syncUserWeeklyXp(studentId: String, weeklyXp: Int, displayName: String, streak: Int) {
        val profile = gamificationDao.getProfile(studentId) ?: return
        val cohortId = profile.cohortId ?: return
        val weekKey = GamificationWeekKey.current()
        if (profile.currentWeekKey != weekKey) return
        leagueDao.updateMemberXp(
            weekKey = weekKey,
            cohortId = cohortId,
            memberId = studentId,
            weeklyXp = weeklyXp,
            streak = streak,
            displayName = displayName.ifBlank { "Student" },
            updatedAt = System.currentTimeMillis(),
        )
        updateRankCache(studentId, cohortId)
    }

    private fun observeMembersForProfile(studentId: String): Flow<List<LeagueMemberEntity>> {
        val weekKey = GamificationWeekKey.current()
        return gamificationDao.observeProfile(studentId).flatMapLatest { profile ->
            val cohortId = profile?.cohortId
            if (cohortId.isNullOrBlank() || profile.currentWeekKey != weekKey) {
                flowOf(emptyList())
            } else {
                leagueDao.observeMembers(weekKey, cohortId)
            }
        }
    }

    private suspend fun ensureCohort(
        studentId: String,
        displayName: String,
        streak: Int,
        profile: GamificationProfileEntity,
    ): String {
        val weekKey = GamificationWeekKey.current()
        val tier = LeagueTier.fromStorage(profile.leagueTier)
        // Keep remote cohortId when still this week — but only if local members exist.
        // After reinstall/restore, profile has cohortId while league_member (bots) is empty;
        // updateMemberXp alone cannot create rows → board stays "0 of 0".
        val existingCohortId = profile.cohortId?.takeIf { profile.currentWeekKey == weekKey }
        if (!existingCohortId.isNullOrBlank()) {
            val existingMembers = leagueDao.getMembers(weekKey, existingCohortId)
            if (existingMembers.isNotEmpty()) {
                leagueDao.updateMemberXp(
                    weekKey = weekKey,
                    cohortId = existingCohortId,
                    memberId = studentId,
                    weeklyXp = profile.weeklyXp,
                    streak = streak,
                    displayName = displayName.ifBlank { "Student" },
                    updatedAt = System.currentTimeMillis(),
                )
                return existingCohortId
            }
        }

        val cohortId =
            existingCohortId
                ?: LeagueCohortEngine.buildCohortId(weekKey, tier, studentId)
        val botCount = (LeagueConfig.COHORT_SIZE - 1).coerceAtLeast(1)
        val bots =
            LeagueCohortEngine.generateBots(
                cohortId = cohortId,
                weekKey = weekKey,
                tier = tier,
                userWeeklyXp = profile.weeklyXp,
                botCount = botCount,
            )
        val userMember =
            LeagueMemberEntity(
                weekKey = weekKey,
                cohortId = cohortId,
                tier = tier.storageKey,
                memberId = studentId,
                displayName = displayName.ifBlank { "Student" },
                weeklyXp = profile.weeklyXp,
                streak = streak,
                isBot = false,
            )
        leagueDao.deleteCohort(weekKey, cohortId)
        leagueDao.upsertMembers(bots + userMember)
        gamificationDao.upsertProfile(
            profile.copy(
                cohortId = cohortId,
                currentWeekKey = weekKey,
                updatedAt = System.currentTimeMillis(),
                isSynced = false,
            ),
        )
        return cohortId
    }

    private suspend fun updateRankCache(
        studentId: String,
        cohortId: String,
        ranked: RankedCohort? = null,
    ): Int {
        val weekKey = GamificationWeekKey.current()
        val previousRank = leagueDao.getCache(studentId)?.rank ?: 0
        val members = leagueDao.getMembers(weekKey, cohortId)
        val resolvedRank =
            ranked?.userRank
                ?: LeagueCohortEngine.rankMembers(members, studentId).userRank
        leagueDao.upsertCache(
            LeagueCacheEntity(
                studentId = studentId,
                weekKey = weekKey,
                cohortId = cohortId,
                rank = resolvedRank,
                totalParticipants = members.size,
                fetchedAt = System.currentTimeMillis(),
            ),
        )
        runCatching {
            friendFeedService.onLeagueRankImproved(studentId, previousRank, resolvedRank)
        }
        return resolvedRank
    }

    suspend fun projectRankAfterAdditionalXp(
        studentId: String,
        additionalXp: Int,
    ): LeagueRankProjection? {
        if (studentId.isBlank() || additionalXp <= 0) return null
        val profile = gamificationDao.getProfile(studentId) ?: return null
        val cohortId = profile.cohortId ?: return null
        val weekKey = GamificationWeekKey.current()
        if (profile.currentWeekKey != weekKey) return null
        val members = leagueDao.getMembers(weekKey, cohortId)
        if (members.isEmpty()) return null
        val tier = LeagueTier.fromStorage(profile.leagueTier)
        val currentRank = LeagueCohortEngine.rankMembers(members, studentId).userRank
        val projectedMembers =
            members.map { member ->
                if (member.memberId == studentId) {
                    member.copy(weeklyXp = member.weeklyXp + additionalXp)
                } else {
                    member
                }
            }
        val projectedRank = LeagueCohortEngine.rankMembers(projectedMembers, studentId).userRank
        return LeagueRankProjection(
            currentRank = currentRank,
            projectedRank = projectedRank,
            tier = tier,
            pendingXp = additionalXp,
        )
    }
}
