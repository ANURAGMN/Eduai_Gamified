package com.ncert7.aitutorandlab.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One planted row in the student's garden (slot index only — never a species key). */
@Entity(
    tableName = "garden_item",
    indices = [
        Index(value = ["studentId", "zone", "plot"]),
        Index(value = ["studentId"]),
    ],
)
data class GrownItemEntity(
    /** Trial item id at plant time — idempotent replays cannot plant twice. */
    @PrimaryKey val id: String,
    val studentId: String,
    val zone: Int,
    val plot: Int,
    /** Visual slot 0–5; preserved when switching Garden ↔ Space themes. */
    val slot: Int,
    val conceptId: String,
    val chapterId: String,
    val kind: String,
    val completedAt: Long,
)
