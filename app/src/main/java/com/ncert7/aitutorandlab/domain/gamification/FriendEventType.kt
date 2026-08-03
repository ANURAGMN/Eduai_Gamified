package com.ncert7.aitutorandlab.domain.gamification

enum class FriendEventType(val storageKey: String) {
    STREAK_MILESTONE("STREAK_MILESTONE"),
    LEAGUE_PROMOTED("LEAGUE_PROMOTED"),
    CONCEPT_MILESTONE("CONCEPT_MILESTONE"),
    FRIEND_LINKED("FRIEND_LINKED"),
    INVITE_REWARD("INVITE_REWARD"),
    PLAN_EXPIRED_INCOMPLETE("PLAN_EXPIRED_INCOMPLETE"),
    ;

    companion object {
        fun fromStorage(value: String): FriendEventType? =
            entries.firstOrNull { it.storageKey == value }
    }
}

enum class FriendAddResult {
    SUCCESS,
    INVALID_CODE,
    SELF_ADD,
    ALREADY_FRIENDS,
    NOT_FOUND,
    FAILED,
}
