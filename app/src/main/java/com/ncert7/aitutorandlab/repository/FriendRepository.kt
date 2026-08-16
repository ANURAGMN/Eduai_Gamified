package com.ncert7.aitutorandlab.repository

import com.ncert7.aitutorandlab.data.local.dao.FriendDao
import com.ncert7.aitutorandlab.data.local.dao.GamificationDao
import com.ncert7.aitutorandlab.data.local.dao.StudentDao
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.local.entities.FriendConnectionEntity
import com.ncert7.aitutorandlab.data.local.entities.FriendFeedItemEntity
import com.ncert7.aitutorandlab.data.local.entities.FeedVisibility
import com.ncert7.aitutorandlab.domain.gamification.FriendAddResult
import com.ncert7.aitutorandlab.domain.gamification.FriendEventType
import com.ncert7.aitutorandlab.domain.gamification.LeagueConfig
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
    private val sharedPrefs: SharedPreferenceUtils,
) {
    fun observeConnections(studentId: String): Flow<List<FriendConnectionEntity>> =
        friendDao.observeConnections(studentId)

    fun observePendingRequests(studentId: String): Flow<List<FriendConnectionEntity>> =
        friendDao.observePendingRequests(studentId)

    fun observeFriendCount(studentId: String): Flow<Int> =
        friendDao.observeFriendCount(studentId)

    fun observeHomeFeed(studentId: String, limit: Int = 8): Flow<List<FriendFeedItemEntity>> =
        friendDao.observeFriendsFeed(studentId, limit)

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
            val isSelf =
                remote.fromStudentId == studentId ||
                    remote.fromStudentId == remote.ownerStudentId
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
                    visibility = if (isSelf) FeedVisibility.SELF else FeedVisibility.FRIENDS,
                    isSynced = true,
                ),
            )
        }
        friendDao.deleteSelfAuthoredFeed(studentId)
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

    /**
     * One-shot for the updated gamified app: seed two local PENDING bot requests
     * (same name pool as league bots). Never synced to Firestore.
     */
    suspend fun seedDemoFriendRequestsIfNeeded(studentId: String) {
        if (studentId.isBlank() || sharedPrefs.hasSeededDemoFriendRequests()) return
        sharedPrefs.setSeededDemoFriendRequests()
        insertDemoPendingRequests(studentId, count = 2)
        ensureActiveDemoFriends(studentId)
    }

    /** Debug-only: always insert [count] fresh PENDING bot requests (local only). */
    suspend fun debugSimulateFriendRequests(studentId: String, count: Int = 2): Int {
        if (studentId.isBlank()) return 0
        sharedPrefs.setSeededDemoFriendRequests()
        val n = insertDemoPendingRequests(studentId, count = count.coerceIn(1, 5), forceUnique = true)
        ensureActiveDemoFriends(studentId)
        return n
    }

    /** Debug-only: wipe self-authored feed noise that was flooding "Friends' updates". */
    suspend fun debugPurgeSelfFeed(studentId: String): Int {
        if (studentId.isBlank()) return 0
        return friendDao.deleteSelfAuthoredFeed(studentId)
    }

    /**
     * Ensure at least 2 accepted demo bots exist so Friends' updates can show
     * streak / topic / XP / rank activity without real friends.
     */
    suspend fun ensureActiveDemoFriends(studentId: String) {
        if (studentId.isBlank()) return
        val existing = getAcceptedDemoBots(studentId)
        if (existing.size >= 2) return
        val needed = 2 - existing.size
        val usedNames = existing.map { it.displayName.lowercase(Locale.US) }.toSet()
        val names =
            LeagueConfig.BOT_NAMES
                .filter { it.lowercase(Locale.US) !in usedNames }
                .shuffled()
                .take(needed)
        val now = System.currentTimeMillis()
        names.forEachIndexed { index, name ->
            val botId = "demo_bot_active_${name.lowercase(Locale.US)}"
            if (friendDao.getConnection(studentId, botId) != null) return@forEachIndexed
            friendDao.upsertConnection(
                FriendConnectionEntity(
                    studentId = studentId,
                    friendStudentId = botId,
                    status = STATUS_ACCEPTED,
                    displayName = name,
                    createdAt = now - index,
                    isSynced = false,
                ),
            )
        }
    }

    suspend fun getAcceptedDemoBots(studentId: String): List<FriendConnectionEntity> =
        friendDao.getConnections(studentId).filter { isDemoBot(it.friendStudentId) }

    /**
     * Insert a friends-visible feed item for [ownerStudentId] authored by someone else.
     * Local only (bots / simulations never sync to Firestore).
     * @return true if a new row was inserted
     */
    suspend fun deliverIncomingLocalFeed(
        ownerStudentId: String,
        fromStudentId: String,
        fromDisplayName: String,
        eventType: FriendEventType,
        message: String,
        eventKey: String,
    ): Boolean {
        if (ownerStudentId.isBlank() || eventKey.isBlank()) return false
        if (friendDao.hasFeedItem(ownerStudentId, eventKey) > 0) return false
        deliverFeedItem(
            ownerStudentId = ownerStudentId,
            fromStudentId = fromStudentId,
            fromDisplayName = fromDisplayName,
            eventType = eventType,
            message = message,
            eventKey = eventKey,
            visibility = FeedVisibility.FRIENDS,
            syncRemote = false,
        )
        return true
    }

    private suspend fun insertDemoPendingRequests(
        studentId: String,
        count: Int,
        forceUnique: Boolean = false,
    ): Int {
        val names = LeagueConfig.BOT_NAMES.shuffled().take(count)
        val now = System.currentTimeMillis()
        var inserted = 0
        names.forEachIndexed { index, name ->
            val botId =
                if (forceUnique) {
                    "demo_bot_${name.lowercase(Locale.US)}_${now}_$index"
                } else {
                    "demo_bot_${name.lowercase(Locale.US)}_$index"
                }
            if (!forceUnique && friendDao.getConnection(studentId, botId) != null) return@forEachIndexed
            friendDao.upsertConnection(
                FriendConnectionEntity(
                    studentId = studentId,
                    friendStudentId = botId,
                    status = STATUS_PENDING,
                    displayName = name,
                    createdAt = now - index,
                    isSynced = false,
                ),
            )
            inserted++
        }
        return inserted
    }

    suspend fun acceptFriendRequest(studentId: String, friendStudentId: String): Boolean {
        if (studentId.isBlank() || friendStudentId.isBlank()) return false
        val pending = friendDao.getConnection(studentId, friendStudentId) ?: return false
        if (pending.status != STATUS_PENDING) return false

        val now = System.currentTimeMillis()
        val myName = resolveDisplayName(studentId)
        val friendName = pending.displayName.ifBlank { resolveDisplayName(friendStudentId) }
        val isBot = isDemoBot(friendStudentId)

        friendDao.upsertConnection(
            pending.copy(
                status = STATUS_ACCEPTED,
                displayName = friendName,
                createdAt = now,
                isSynced = false,
            ),
        )

        if (!isBot) {
            friendDao.upsertConnection(
                FriendConnectionEntity(
                    studentId = friendStudentId,
                    friendStudentId = studentId,
                    status = STATUS_ACCEPTED,
                    displayName = myName,
                    createdAt = now,
                    isSynced = false,
                ),
            )
            firebaseRepository.upsertFriendConnection(
                ownerStudentId = studentId,
                friendStudentId = friendStudentId,
                displayName = friendName,
            )
            firebaseRepository.upsertFriendConnection(
                ownerStudentId = friendStudentId,
                friendStudentId = studentId,
                displayName = myName,
            )
            publishFriendLinkedFeed(
                adderStudentId = studentId,
                friendStudentId = friendStudentId,
                friendDisplayName = friendName,
                adderDisplayName = myName,
            )
        } else {
            deliverFeedItem(
                ownerStudentId = studentId,
                fromStudentId = friendStudentId,
                fromDisplayName = friendName,
                eventType = FriendEventType.FRIEND_LINKED,
                message = "You're now friends with $friendName",
                eventKey = "${studentId}_FRIEND_LINKED_$friendStudentId",
                syncRemote = false,
            )
        }
        GamificationAnalyticsTracker.friendAdded(method = if (isBot) "demo_request" else "request")
        return true
    }

    private suspend fun backfillLinkedFeedIfMissing(studentId: String) {
        friendDao.getConnections(studentId).forEach { connection ->
            if (isDemoBot(connection.friendStudentId)) return@forEach
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
            if (isDemoBot(connection.friendStudentId)) return@forEach
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
        private const val STATUS_PENDING = "PENDING"
        private const val STATUS_ACCEPTED = "ACCEPTED"

        fun isDemoBot(friendStudentId: String): Boolean =
            friendStudentId.startsWith("demo_bot_")
    }
}
