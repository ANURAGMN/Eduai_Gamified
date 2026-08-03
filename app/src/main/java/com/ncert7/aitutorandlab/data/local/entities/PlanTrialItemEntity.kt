package com.ncert7.aitutorandlab.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One row in a plan day's exam trial queue. See docs/exam-plan/EXAM_TRIAL_SPEC.md */
@Entity(
    tableName = "plan_trial_item",
    foreignKeys = [
        ForeignKey(
            entity = ExamPlanDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["planDayId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["studentId", "planDayId", "sequenceIndex"], unique = true),
        Index(value = ["studentId", "dayIndex"]),
    ],
)
data class PlanTrialItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: String,
    /** FK to exam_plan_day.id */
    val planDayId: Long,
    val dayIndex: Int,
    val chapterId: String,
    val conceptId: String,
    /** SIM_URL, SIM_AGENT, STUDY, REVISION */
    val kind: String,
    /** conceptId for study/revision; simulationId or url hash for sim items */
    val sourceId: String,
    val title: String,
    val sequenceIndex: Int,
    val requiredCount: Int = 1,
    val completedCount: Int = 0,
    /** PENDING, IN_PROGRESS, DONE */
    val status: String = PlanTrialItemStatus.PENDING,
    val celebrated: Boolean = false,
    val carriedFromDayIndex: Int? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

object PlanTrialItemKind {
    const val SIM_URL = "SIM_URL"
    const val SIM_AGENT = "SIM_AGENT"
    const val STUDY = "STUDY"
    const val REVISION = "REVISION"

    /** Math practice problem — opens the math agent. Used for Math-subject chapter trials. */
    const val MATH = "MATH"
}

object PlanTrialItemStatus {
    const val PENDING = "PENDING"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val DONE = "DONE"
}
