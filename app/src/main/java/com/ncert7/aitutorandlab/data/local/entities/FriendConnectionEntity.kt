package com.ncert7.aitutorandlab.data.local.entities

import androidx.room.Entity

@Entity(
    tableName = "friend_connection",
    primaryKeys = ["studentId", "friendStudentId"],
)
data class FriendConnectionEntity(
    val studentId: String,
    val friendStudentId: String,
    val status: String = "ACCEPTED",
    val displayName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
)
