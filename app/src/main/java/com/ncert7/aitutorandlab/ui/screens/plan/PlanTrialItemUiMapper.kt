package com.ncert7.aitutorandlab.ui.screens.plan

import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemEntity
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemKind
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemStatus
import com.ncert7.aitutorandlab.ui.screens.plan.viewmodel.PlanTrialItemUi

object PlanTrialItemUiMapper {
    fun toUi(entity: PlanTrialItemEntity): PlanTrialItemUi {
        val kindLabel =
            when (entity.kind) {
                PlanTrialItemKind.SIM_AGENT -> "Sim agent"
                PlanTrialItemKind.SIM_URL -> "Simulation"
                PlanTrialItemKind.STUDY -> "Study"
                PlanTrialItemKind.REVISION -> "Revision"
                PlanTrialItemKind.MATH -> "Math problem"
                else -> entity.kind
            }
        val statusLabel =
            when (entity.status) {
                PlanTrialItemStatus.DONE -> "Done"
                PlanTrialItemStatus.IN_PROGRESS -> "In progress"
                else -> "Pending"
            }
        val progressLabel =
            if (entity.requiredCount > 1 && entity.status != PlanTrialItemStatus.DONE) {
                "${entity.completedCount} / ${entity.requiredCount} bites"
            } else {
                null
            }
        return PlanTrialItemUi(
            id = entity.id,
            title = entity.title,
            kind = entity.kind,
            kindLabel = kindLabel,
            status = entity.status,
            statusLabel = statusLabel,
            conceptId = entity.conceptId,
            chapterId = entity.chapterId,
            sourceId = entity.sourceId,
            progressLabel = progressLabel,
            completedCount = entity.completedCount,
            requiredCount = entity.requiredCount,
            carriedFromDayIndex = entity.carriedFromDayIndex,
        )
    }
}
