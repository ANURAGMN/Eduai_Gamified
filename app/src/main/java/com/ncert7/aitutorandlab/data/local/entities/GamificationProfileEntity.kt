package com.ncert7.aitutorandlab.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gamification_profile")
data class GamificationProfileEntity(
    @PrimaryKey val studentId: String,
    val lifetimeXp: Int = 0,
    val weeklyXp: Int = 0,
    val gems: Int = 0,
    val leagueTier: String = "BRONZE",
    val currentWeekKey: String = "",
    val cohortId: String? = null,
    val friendCode: String = "",
    val invitedByCode: String? = null,
    val inviteRewardGranted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
)
