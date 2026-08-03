package com.ncert7.aitutorandlab.repository

import com.ncert7.aitutorandlab.data.local.dao.FriendDao
import com.ncert7.aitutorandlab.data.local.dao.GamificationDao
import com.ncert7.aitutorandlab.data.local.dao.StudentDao
import com.ncert7.aitutorandlab.data.local.entities.FriendConnectionEntity
import com.ncert7.aitutorandlab.data.local.entities.FriendFeedItemEntity
import com.ncert7.aitutorandlab.data.local.entities.FeedVisibility
import com.ncert7.aitutorandlab.domain.gamification.FriendAddResult
import com.ncert7.aitutorandlab.domain.gamification.FriendEventType
import com.ncert7.aitutorandlab.domain.gamification.LeagueTier
import com.ncert7.aitutorandlab.service.analytics.GamificationAnalyticsTracker
import kotlinx.coroutines.flow.Flow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class FriendCodeLookup(
    val studentId: String,
    val displayName: String,
)

@Singleton
class FriendRepository @Inject constructor(
    private val friendDao: FriendDao,
    private val gamificationDao: GamificationDao,
    private val studentDao: StudentDao,
    private val firebaseRepository: FirebaseRepository,
) {
    fun observeConnections(studentId: String): Flow<List<FriendConnectionEntity>> =
        friendDao.observeConnections(studentId)

    fun observeFriendCount(studentId: String): Flow<Int> =
        friendDao.observeFriendCount(studentId)

    fun observeHomeFeed(studentId: String, limit: Int = 8): Flow<List<FriendFeedItemEntity>> =
        friendDao.observeFeed(studentId, limit)

    fun observeUnseenFeedCount(studentId: String): Flow<Int> =
        friendDao.observeUnseenFeedCount(studentId)

    suspend fun getMyFriendCode(studentId: String): String {
        val profile = gamificationDao.getProfile(studentId) ?: return ""
        return profile.friendCode
    }

    suspend fun syncFriendCodeToRemote(studentId: String) {
        val profile = gamificationDao.getProfile(studentId) ?: return
        if (profile.friendCode.isBlank()) return
        firebaseRepository.registerFriendCode(
            code = profile.friendCode,
            studentId = studentId,
            displayName = resolveDisplayName(studentId),
        )
    }

    suspend fun syncFriendSocialData(studentId: String) {
        if (studentId.isBlank()) return
        firebaseRepository.fetchFriendConnections(studentId).forEach { remote ->
            friendDao.upsertConnection(
                FriendConnectionEntity(
                    studentId = remote.studentId,
                    friendStudentId = remote.friendStudentId,
                    displayName = resolveDisplayName(remote.friendStudentId),
                    status = remote.status,
                    createdAt = remote.updatedAt,
                    isSynced = true,
                ),
            )
        }
        firebaseRepository.fetchFriendFeed(studentId).forEach { remote ->
            friendDao.insertFeedItem(
                FriendFeedItemEntity(
                    ownerStudentId = remote.ownerStudentId,
                    fromStudentId = remote.fromStudentId,
                    fromDisplayName = remote.fromDisplayName.ifBlank { "Friend" },
                    eventType = remote.eventType,
                    message = remote.message,
                    eventKey = remote.eventKey,
                    cheers = remote.cheers,
                    createdAt = remote.createdAt,
                    isSynced = true,
                ),
            )
        }
        backfillLinkedFeedIfMissing(studentId)
        refreshFriendDisplayNames(studentId)
    }

    suspend fun addFriendByCode(studentId: String, rawCode: String): FriendAddResult {
        if (studentId.isBlank()) return FriendAddResult.FAILED
        val code = rawCode.trim().uppercase(Locale.US)
        if (code.length < 6) return FriendAddResult.INVALID_CODE

        val myProfile = gamificationDao.getProfile(studentId) ?: return FriendAddResult.FAILED
        if (myProfile.friendCode.equals(code, ignoreCase = true)) return FriendAddResult.SELF_ADD

        val lookup = resolveFriendCode(code) ?: return FriendAddResult.NOT_FOUND
        if (lookup.studentId == studentId) return FriendAddResult.SELF_ADD

        if (friendDao.getConnection(studentId, lookup.studentId) != null) {
            return FriendAddResult.ALREADY_FRIENDS
        }

        val myName = resolveDisplayName(studentId)
        val friendName = resolveDisplayName(lookup.studentId)
        val now = System.currentTimeMillis()
        friendDao.upsertConnections(
            listOf(
                FriendConnectionEntity(
                    studentId = studentId,
                    friendStudentId = lookup.studentId,
                    displayName = friendName,
                    createdAt = now,
                ),
                FriendConnectionEntity(
                    studentId = lookup.studentId,
                    friendStudentId = studentId,
                    displayName = myName,
                    createdAt = now,
                ),
            ),
        )

        firebaseRepository.upsertFriendConnection(
            ownerStudentId = studentId,
            friendStudentId = lookup.studentId,
            displayName = friendName,
        )
        firebaseRepository.upsertFriendConnection(
            ownerStudentId = lookup.studentId,
            friendStudentId = studentId,
            displayName = myName,
        )
        if (myProfile.invitedByCode.isNullOrBlank() && !myProfile.inviteRewardGranted) {
            gamificationDao.upsertProfile(
                myProfile.copy(
                    invitedByCode = code,
                    updatedAt = now,
                    isSynced = false,
                ),
            )
        }
        publishFriendLinkedFeed(
            adderStudentId = studentId,
            friendStudentId = lookup.studentId,
            friendDisplayName = friendName,
            adderDisplayName = myName,
        )
        GamificationAnalyticsTracker.friendAdded(method = "code")
        return FriendAddResult.SUCCESS
    }

    suspend fun publishFeedEvent(
        actorStudentId: String,
        eventType: FriendEventType,
        message: String,
        eventKeySuffix: String,
    ) {
        val actorName = resolveDisplayName(actorStudentId)
        val friendIds = friendDao.getFriendIds(actorStudentId)
        if (friendIds.isEmpty()) return

        val eventKey = "${actorStudentId}_${eventType.storageKey}_$eventKeySuffix"
        friendIds.forEach { ownerId ->
            deliverFeedItem(
                ownerStudentId = ownerId,
                fromStudentId = actorStudentId,
                fromDisplayName = actorName,
                eventType = eventType,
                message = message,
                eventKey = eventKey,
                visibility = FeedVisibility.FRIENDS,
            )
        }
    }

    /** Owner-only feed entry — local only in v1 (not pushed to friends). */
    suspend fun publishSelfFeedEvent(
        ownerStudentId: String,
        eventType: FriendEventType,
        message: String,
        eventKey: String,
    ) {
        if (ownerStudentId.isBlank()) return
        if (friendDao.hasFeedItem(ownerStudentId, eventKey) > 0) return
        val displayName = resolveDisplayName(ownerStudentId)
        deliverFeedItem(
            ownerStudentId = ownerStudentId,
            fromStudentId = ownerStudentId,
            fromDisplayName = displayName,
            eventType = eventType,
            message = message,
            eventKey = eventKey,
            visibility = FeedVisibility.SELF,
            syncRemote = false,
        )
    }

    suspend fun cheerFeedItem(studentId: String, feedItemId: Long): Boolean =
        friendDao.cheerFeedItem(studentId, feedItemId) > 0

    suspend fun markHomeFeedSeen(studentId: String) {
        friendDao.markAllFeedSeen(studentId)
    }

    private suspend fun backfillLinkedFeedIfMissing(studentId: String) {
        friendDao.getConnections(studentId).forEach { connection ->
            val friendName = resolveDisplayName(connection.friendStudentId)
            val eventKey = "${studentId}_FRIEND_LINKED_${connection.friendStudentId}"
            if (friendDao.hasFeedItem(studentId, eventKey) > 0) return@forEach
            deliverFeedItem(
                ownerStudentId = studentId,
                fromStudentId = connection.friendStudentId,
                fromDisplayName = friendName,
                eventType = FriendEventType.FRIEND_LINKED,
                message = "You're now friends with $friendName",
                eventKey = eventKey,
            )
        }
    }

    private suspend fun refreshFriendDisplayNames(studentId: String) {
        friendDao.getConnections(studentId).forEach { connection ->
            val resolved = resolveDisplayName(connection.friendStudentId)
            if (resolved == connection.displayName) return@forEach

            friendDao.upsertConnection(connection.copy(displayName = resolved))
            val eventKey = "${studentId}_FRIEND_LINKED_${connection.friendStudentId}"
            val message = "You're now friends with $resolved"
            friendDao.updateFeedItemDisplay(
                ownerStudentId = studentId,
                eventKey = eventKey,
                fromDisplayName = resolved,
                message = message,
            )
            firebaseRepository.upsertFriendConnection(
                ownerStudentId = studentId,
                friendStudentId = connection.friendStudentId,
                displayName = resolved,
            )
            firebaseRepository.publishFriendFeedItem(
                ownerStudentId = studentId,
                fromStudentId = connection.friendStudentId,
                fromDisplayName = resolved,
                eventType = FriendEventType.FRIEND_LINKED.storageKey,
                message = message,
                eventKey = eventKey,
            )
        }
    }

    private suspend fun publishFriendLinkedFeed(
        adderStudentId: String,
        friendStudentId: String,
        friendDisplayName: String,
        adderDisplayName: String,
    ) {
        deliverFeedItem(
            ownerStudentId = adderStudentId,
            fromStudentId = friendStudentId,
            fromDisplayName = friendDisplayName,
            eventType = FriendEventType.FRIEND_LINKED,
            message = "You're now friends with $friendDisplayName",
            eventKey = "${adderStudentId}_FRIEND_LINKED_${friendStudentId}",
        )
        deliverFeedItem(
            ownerStudentId = friendStudentId,
            fromStudentId = adderStudentId,
            fromDisplayName = adderDisplayName,
            eventType = FriendEventType.FRIEND_LINKED,
            message = "$adderDisplayName added you as a friend",
            eventKey = "${friendStudentId}_FRIEND_LINKED_${adderStudentId}",
        )
    }

    private suspend fun deliverFeedItem(
        ownerStudentId: String,
        fromStudentId: String,
        fromDisplayName: String,
        eventType: FriendEventType,
        message: String,
        eventKey: String,
        visibility: String = FeedVisibility.FRIENDS,
        syncRemote: Boolean = true,
    ) {
        val now = System.currentTimeMillis()
        friendDao.insertFeedItem(
            FriendFeedItemEntity(
                ownerStudentId = ownerStudentId,
                fromStudentId = fromStudentId,
                fromDisplayName = fromDisplayName,
                eventType = eventType.storageKey,
                message = message,
                eventKey = eventKey,
                visibility = visibility,
                createdAt = now,
            ),
        )
        if (syncRemote) {
            firebaseRepository.publishFriendFeedItem(
                ownerStudentId = ownerStudentId,
                fromStudentId = fromStudentId,
                fromDisplayName = fromDisplayName,
                eventType = eventType.storageKey,
                message = message,
                eventKey = eventKey,
                createdAt = now,
            )
        }
    }

    private suspend fun resolveFriendCode(code: String): FriendCodeLookup? {
        gamificationDao.getProfileByFriendCode(code)?.let { local ->
            return FriendCodeLookup(local.studentId, resolveDisplayName(local.studentId))
        }
        return firebaseRepository.lookupFriendCode(code)?.let { remote ->
            FriendCodeLookup(remote.studentId, resolveDisplayName(remote.studentId))
        }
    }

    private suspend fun resolveDisplayName(studentId: String): String {
        val localName = studentDao.getStudentSync(studentId)?.studentName?.trim().orEmpty()
        if (localName.isNotBlank() && !localName.equals(GENERIC_NAME, ignoreCase = true)) {
            return localName
        }

        firebaseRepository.fetchUserDisplayName(studentId)?.let { return it }
        firebaseRepository.fetchFriendCodeDisplayName(studentId)?.let { return it }

        if (localName.isNotBlank()) return localName

        if (studentId.contains("@")) {
            val fromEmail =
                studentId.substringBefore("@")
                    .replace(".", " ")
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { part ->
                        part.replaceFirstChar { char -> char.titlecase(Locale.getDefault()) }
                    }
            if (fromEmail.isNotBlank()) return fromEmail
        }

        return "Friend"
    }

    suspend fun publishLeaguePromotionIfNeeded(
        studentId: String,
        previousTier: LeagueTier,
        newTier: LeagueTier,
    ) {
        if (newTier.ordinal <= previousTier.ordinal) return
        publishFeedEvent(
            actorStudentId = studentId,
            eventType = FriendEventType.LEAGUE_PROMOTED,
            message = "Promoted to the ${newTier.leagueTitle()}",
            eventKeySuffix = newTier.storageKey,
        )
    }

    companion object {
        private const val GENERIC_NAME = "Student"
    }
}
