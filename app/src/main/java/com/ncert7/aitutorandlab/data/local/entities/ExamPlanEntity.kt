package com.ncert7.aitutorandlab.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exam_plan")
data class ExamPlanEntity(
    @PrimaryKey val studentId: String,
    val subjectId: String,
    val examType: String = "Unit Test",
    val dailyMinutes: Int = 30,
    val startEpochDay: Long,
    /** Calendar day the exam is scheduled for (EXAM plan day). */
    val examEpochDay: Long = startEpochDay,
    /** Comma-separated chapter IDs included in this plan. */
    val chapterIds: String,
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
)
