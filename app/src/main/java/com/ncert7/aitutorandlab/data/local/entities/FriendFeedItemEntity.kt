package com.ncert7.aitutorandlab.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "friend_feed_item",
    indices = [Index(value = ["ownerStudentId", "eventKey"], unique = true)],
)
data class FriendFeedItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerStudentId: String,
    val fromStudentId: String,
    val fromDisplayName: String,
    val eventType: String,
    val message: String,
    val cheers: Int = 0,
    val cheeredByMe: Boolean = false,
    val seen: Boolean = false,
    val eventKey: String,
    val createdAt: Long = System.currentTimeMillis(),
    /** FRIENDS = visible in friends' feeds; SELF = owner-only reminder. */
    val visibility: String = FeedVisibility.FRIENDS,
    val isSynced: Boolean = false,
)
