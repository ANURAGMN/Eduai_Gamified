package com.ncert7.aitutorandlab.data.local.entities

import androidx.room.Entity

@Entity(
    tableName = "quest_daily",
    primaryKeys = ["studentId", "questDate"],
)
data class QuestDailyEntity(
    val studentId: String,
    /** Local calendar date in Asia/Kolkata — `yyyy-MM-dd`. */
    val questDate: String,
    val simsDone: Int = 0,
    val simsTotal: Int = 3,
    val studyDone: Int = 0,
    val studyTotal: Int = 1,
    val simsClaimed: Boolean = false,
    val studyClaimed: Boolean = false,
    val bonusClaimed: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
)
