package com.ncert7.aitutorandlab.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "simulation_interactions",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["studentId"]),
        Index(value = ["sessionId"]),
        Index(value = ["interactionDate"]),
        Index(value = ["studentId", "interactionDate", "isSynced"])
    ]
)
data class SimulationInteractionEntity(
    @PrimaryKey(autoGenerate = true)
    val interactionId: Long = 0,
    val studentId: String,
    val sessionId: String,
    val simulationTitle: String,
    val subjectName: String,
    val chapterName: String,
    val elementClicked: String,
    val elementType: String,
    val givenAnswer: String,
    val isCorrect: String,
    val timeTaken: String,
    val timestamp: String,
    val occurredAt: Long,
    val interactionDate: String,
    val appName: String = "",
    val isSynced: Boolean = false
)
