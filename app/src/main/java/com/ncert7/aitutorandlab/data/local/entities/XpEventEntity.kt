package com.ncert7.aitutorandlab.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "xp_event",
    indices = [
        Index(
            value = ["studentId", "itemType", "itemId", "language"],
            unique = true,
        ),
    ],
)
data class XpEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: String,
    val itemType: String,
    val itemId: String,
    val language: String,
    val xpAmount: Int,
    val weekKey: String,
    val countsForLeague: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
)
