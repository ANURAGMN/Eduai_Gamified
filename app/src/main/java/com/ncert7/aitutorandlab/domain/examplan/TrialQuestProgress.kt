package com.ncert7.aitutorandlab.domain.examplan

import com.ncert7.aitutorandlab.data.local.entities.ExamPlanDayEntity
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemEntity
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemKind
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemStatus
import com.ncert7.aitutorandlab.domain.gamification.DailyQuestProgress

/** Derives daily quest counts from today's exam-trial queue when materialized. */
object TrialQuestProgress {
    fun fromTrialItems(
        items: List<PlanTrialItemEntity>,
        todayPlanDay: ExamPlanDayEntity?,
    ): DailyQuestProgress? {
        if (items.isEmpty()) return null

        // MATH items count toward BOTH quest buckets so math-heavy days can still complete the
        // sims and study/bonus quests.
        val simItems =
            items.filter {
                it.kind == PlanTrialItemKind.SIM_AGENT ||
                    it.kind == PlanTrialItemKind.SIM_URL ||
                    it.kind == PlanTrialItemKind.MATH
            }
        val studyItems =
            items.filter {
                it.kind == PlanTrialItemKind.STUDY ||
                    it.kind == PlanTrialItemKind.REVISION ||
                    it.kind == PlanTrialItemKind.MATH
            }

        val studyLabelPrefix =
            when (todayPlanDay?.dayType) {
                "REVISE" -> "Revision · "
                "MOCK" -> "Mock task · "
                "EXAM" -> "Rest day"
                else -> "Trial · "
            }

        return DailyQuestProgress(
            simsDone = simItems.count { it.status == PlanTrialItemStatus.DONE },
            simsTotal = simItems.size,
            studyDone = studyItems.count { it.status == PlanTrialItemStatus.DONE },
            studyTotal = studyItems.size.coerceAtLeast(if (studyItems.isEmpty()) 0 else 1),
            studyLabelPrefix = studyLabelPrefix,
        )
    }
}
