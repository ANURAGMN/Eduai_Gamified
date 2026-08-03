package com.ncert7.aitutorandlab.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_log",
    indices = [
        Index(value = ["studentId", "shownEpochDay"]),
        Index(value = ["studentId", "type", "shownEpochDay", "dedupKey"], unique = true),
    ],
)
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: String,
    /** [com.ncert7.aitutorandlab.notification.NotificationType.id] */
    val type: String,
    /** Local calendar epoch day when the notification was shown. */
    val shownEpochDay: Long,
    /** Extra dedup scope (e.g. exam day index, inactivity tier). */
    val dedupKey: String = "",
    val shownAtMs: Long = System.currentTimeMillis(),
)
