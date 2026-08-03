package com.ncert7.aitutorandlab.ui.screens.plan

import com.anurag.eduai.uikit.components.PlanDayNode
import com.anurag.eduai.uikit.components.PlanDayStatus
import com.anurag.eduai.uikit.components.PlanDayType
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanDayEntity

object ExamPlanUiMapper {

    fun toPlanDayNode(day: ExamPlanDayEntity, displayLabel: String = day.label): PlanDayNode =
        day.mapToPlanDayNode(displayLabel)

    fun toPlanDayNodes(
        days: List<ExamPlanDayEntity>,
        displayLabel: (ExamPlanDayEntity) -> String = { it.label },
    ): List<PlanDayNode> =
        days.map { it.mapToPlanDayNode(displayLabel(it)) }

    private fun ExamPlanDayEntity.mapToPlanDayNode(displayLabel: String): PlanDayNode =
        PlanDayNode(
            day = dayIndex,
            status =
                when (status) {
                    "DONE" -> PlanDayStatus.Done
                    "PARTIAL" -> PlanDayStatus.Partial
                    "TODAY" -> PlanDayStatus.Today
                    else -> PlanDayStatus.Upcoming
                },
            type =
                when (dayType) {
                    "REVISE" -> PlanDayType.Revise
                    "MOCK" -> PlanDayType.Mock
                    "EXAM" -> PlanDayType.Exam
                    else -> PlanDayType.Lesson
                },
            label = displayLabel,
            conceptIds = conceptIds,
        )

    fun firstConceptId(day: ExamPlanDayEntity?): String? = firstConceptId(day?.conceptIds)

    fun firstConceptId(day: PlanDayNode): String? = firstConceptId(day.conceptIds)

    private fun firstConceptId(conceptIds: String?): String? =
        conceptIds
            ?.split(",")
            ?.firstOrNull { it.isNotBlank() }
}
