package com.ncert7.aitutorandlab.data.local.entities

import androidx.room.Entity

@Entity(
    tableName = "league_member",
    primaryKeys = ["weekKey", "cohortId", "memberId"],
)
data class LeagueMemberEntity(
    val weekKey: String,
    val cohortId: String,
    val tier: String,
    val memberId: String,
    val displayName: String,
    val weeklyXp: Int = 0,
    val streak: Int = 0,
    val isBot: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)
