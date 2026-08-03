package com.ncert7.aitutorandlab.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "league_cache")
data class LeagueCacheEntity(
    @PrimaryKey val studentId: String,
    val weekKey: String,
    val cohortId: String,
    val rank: Int,
    val totalParticipants: Int,
    val fetchedAt: Long = System.currentTimeMillis(),
)
