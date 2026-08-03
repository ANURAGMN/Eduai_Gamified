package com.ncert7.aitutorandlab.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tutor_config")
data class TutorConfigEntity(
    @PrimaryKey val studentId: String,
    val character: String = "Free",
    val outfit: Int = 0,
    val neck: Int = 0,
    val hair: Int = 0,
    val hairColor: Int = 0,
    val glasses: Int = 0,
    val frameColor: Int = 0,
    val eyeLine: Boolean = false,
    val cheeks: Boolean = true,
    val presetId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
)
