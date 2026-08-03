package com.ncert7.aitutorandlab.ui.screens.friends

import com.anurag.eduai.uikit.components.FriendUpdate
import com.anurag.eduai.uikit.theme.EduChipRole
import com.ncert7.aitutorandlab.data.local.entities.FriendConnectionEntity
import com.ncert7.aitutorandlab.data.local.entities.FriendFeedItemEntity
import com.ncert7.aitutorandlab.domain.gamification.FriendEventType

object FriendUiMapper {

    fun toFriendUpdates(items: List<FriendFeedItemEntity>): List<FriendUpdate> =
        items.map { item ->
            FriendUpdate(
                name = item.fromDisplayName,
                event = item.message,
                cheers = item.cheers,
                role = roleForEvent(item.eventType),
                seen = item.seen,
            )
        }

    fun toLinkedFriendUpdates(connections: List<FriendConnectionEntity>): List<FriendUpdate> =
        connections.map { connection ->
            FriendUpdate(
                name = connection.displayName.ifBlank { "Friend" },
                event = "Connected — you'll see streaks and milestones here.",
                cheers = 0,
                role = EduChipRole.Accent,
                seen = true,
            )
        }

    private fun roleForEvent(eventType: String): EduChipRole =
        when (FriendEventType.fromStorage(eventType)) {
            FriendEventType.LEAGUE_PROMOTED -> EduChipRole.Pro
            FriendEventType.STREAK_MILESTONE -> EduChipRole.Warning
            FriendEventType.CONCEPT_MILESTONE -> EduChipRole.Success
            FriendEventType.FRIEND_LINKED -> EduChipRole.Accent
            FriendEventType.INVITE_REWARD -> EduChipRole.Success
            FriendEventType.PLAN_EXPIRED_INCOMPLETE -> EduChipRole.Warning
            null -> EduChipRole.Accent
        }
}
