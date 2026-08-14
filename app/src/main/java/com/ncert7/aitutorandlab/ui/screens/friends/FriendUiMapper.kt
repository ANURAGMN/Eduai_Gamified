package com.ncert7.aitutorandlab.ui.screens.friends

import com.anurag.eduai.uikit.components.FriendUpdate
import com.anurag.eduai.uikit.theme.EduChipRole
import com.ncert7.aitutorandlab.data.local.entities.FeedVisibility
import com.ncert7.aitutorandlab.data.local.entities.FriendConnectionEntity
import com.ncert7.aitutorandlab.data.local.entities.FriendFeedItemEntity
import com.ncert7.aitutorandlab.domain.gamification.FriendEventType
import com.ncert7.aitutorandlab.utils.FriendFeedCopy
import java.util.Locale

object FriendUiMapper {

    fun toFriendUpdates(
        items: List<FriendFeedItemEntity>,
        ownerStudentId: String = "",
        languageCode: String = "en",
    ): List<FriendUpdate> =
        items
            .asSequence()
            .filter { item ->
                item.visibility != FeedVisibility.SELF &&
                    (ownerStudentId.isBlank() || item.fromStudentId != ownerStudentId)
            }
            // One card per person (newest first), then collapse same display name
            // (e.g. two of your own emails both resolve to "anurag").
            .distinctBy { it.fromStudentId }
            .distinctBy { it.fromDisplayName.trim().lowercase(Locale.US) }
            .map { item ->
                FriendUpdate(
                    name = item.fromDisplayName,
                    event = FriendFeedCopy.localize(item.message, item.eventType, languageCode),
                    cheers = item.cheers,
                    role = roleForEvent(item.eventType),
                    seen = item.seen,
                    feedItemId = item.id,
                )
            }
            .toList()

    fun toFriendRequestUpdates(
        pending: List<FriendConnectionEntity>,
        languageCode: String = "en",
    ): List<FriendUpdate> =
        pending.map { connection ->
            FriendUpdate(
                name = connection.displayName.ifBlank { "Student" },
                event = FriendFeedCopy.wantsToBeFriends(languageCode),
                cheers = 0,
                role = EduChipRole.Accent,
                seen = false,
                isRequest = true,
                requestFriendId = connection.friendStudentId,
            )
        }

    fun toLinkedFriendUpdates(
        connections: List<FriendConnectionEntity>,
        languageCode: String = "en",
    ): List<FriendUpdate> =
        connections
            .distinctBy { it.displayName.trim().lowercase(Locale.US) }
            .map { connection ->
                FriendUpdate(
                    name = connection.displayName.ifBlank { "Friend" },
                    event = FriendFeedCopy.connectedPlaceholder(languageCode),
                    cheers = 0,
                    role = EduChipRole.Accent,
                    seen = true,
                )
            }

    private fun roleForEvent(eventType: String): EduChipRole =
        when (FriendEventType.fromStorage(eventType)) {
            FriendEventType.LEAGUE_PROMOTED -> EduChipRole.Pro
            FriendEventType.LEAGUE_RANK_JUMP -> EduChipRole.Pro
            FriendEventType.STREAK_MILESTONE -> EduChipRole.Warning
            FriendEventType.CONCEPT_MILESTONE -> EduChipRole.Success
            FriendEventType.TOPIC_COMPLETED -> EduChipRole.Success
            FriendEventType.XP_MILESTONE -> EduChipRole.Accent
            FriendEventType.FRIEND_LINKED -> EduChipRole.Accent
            FriendEventType.INVITE_REWARD -> EduChipRole.Success
            FriendEventType.PLAN_EXPIRED_INCOMPLETE -> EduChipRole.Warning
            null -> EduChipRole.Accent
        }
}
