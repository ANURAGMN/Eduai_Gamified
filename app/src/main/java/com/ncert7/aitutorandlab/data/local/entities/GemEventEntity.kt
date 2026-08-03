package com.ncert7.aitutorandlab.data.local.entities

import androidx.room.Entity

@Entity(
    tableName = "gem_event",
    primaryKeys = ["studentId", "grantKey"],
)
data class GemEventEntity(
    val studentId: String,
    /** Idempotency key, e.g. `quest_sims_2026-07-24`. */
    val grantKey: String,
    val gemsAmount: Int,
    val source: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
)
