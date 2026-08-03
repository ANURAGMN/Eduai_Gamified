package com.ncert7.aitutorandlab.data.local.entities

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "notification_budget",
    primaryKeys = ["studentId", "budgetEpochDay"],
    indices = [Index(value = ["studentId"])],
)
data class NotificationBudgetEntity(
    val studentId: String,
    /** Local calendar epoch day for the daily send counter. */
    val budgetEpochDay: Long,
    val sentCount: Int = 0,
    val lastSentAtMs: Long = 0L,
)
