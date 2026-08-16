package com.ncert7.aitutorandlab.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exam_plan_day",
    foreignKeys = [
        ForeignKey(
            entity = ExamPlanEntity::class,
            parentColumns = ["studentId"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["studentId", "dayIndex"], unique = true),
    ],
)
data class ExamPlanDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: String,
    val dayIndex: Int,
    val calendarEpochDay: Long,
    /** LESSON, REVISE, MOCK, EXAM — or CHAPTER_TRIAL for standalone chapter picker runs. */
    val dayType: String,
    /** DONE, TODAY, UPCOMING */
    val status: String,
    val label: String,
    /** Comma-separated concept IDs for lesson/revise days. */
    val conceptIds: String = "",
    val estimatedMinutes: Int = 18,
) {
    /**
     * Real exam-prep schedule day (shown on plan trail / overview).
     * Chapter-picker trials use a negative [dayIndex] + CHAPTER_TRIAL and stay status TODAY —
     * they must not appear as extra "Today" nodes on the exam plan.
     */
    fun isExamScheduleDay(): Boolean =
        dayIndex >= 0 && !dayType.equals("CHAPTER_TRIAL", ignoreCase = true)
}
