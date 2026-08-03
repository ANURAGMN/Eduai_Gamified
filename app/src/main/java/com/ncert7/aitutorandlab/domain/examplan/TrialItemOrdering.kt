package com.ncert7.aitutorandlab.domain.examplan

import com.ncert7.aitutorandlab.data.local.entities.ConceptEntity

/**
 * Pure ordering helpers for exam-trial queues.
 *
 * Lesson days: up to [SIMS_PER_STUDY_BLOCK] sim URLs before each study (sim1–3, study1, sim4–6,
 * study2, …). Studies with no sims left in the chapter are stacked after interleaved blocks.
 * Revise days: revision items first, then simulation agents.
 */
object TrialItemOrdering {
    /** Default sim URLs placed before each study agent when the chapter has enough sims. */
    const val SIMS_PER_STUDY_BLOCK = 3

    private const val MINUTES_PER_SIM_URL = 4
    private const val MINUTES_PER_STUDY = 12

    /**
     * Returns the simulation URL slice for [study] based on its index in the full chapter syllabus.
     * Study 1 → sims 0..2, study 2 → sims 3..5, etc. (continues across plan days).
     */
    fun simUrlsForStudy(
        study: ConceptEntity,
        allStudiesInChapter: List<ConceptEntity>,
        simsWithUrl: List<ConceptEntity>,
        simsPerBlock: Int = SIMS_PER_STUDY_BLOCK,
    ): List<ConceptEntity> {
        if (simsWithUrl.isEmpty() || simsPerBlock <= 0) return emptyList()

        val studies = allStudiesInChapter.sortedBy { it.orderIndex }
        val studyIndex = studies.indexOfFirst { it.conceptId == study.conceptId }
        if (studyIndex < 0) return emptyList()

        val start = studyIndex * simsPerBlock
        if (start >= simsWithUrl.size) return emptyList()

        val end = minOf(start + simsPerBlock, simsWithUrl.size)
        return simsWithUrl.subList(start, end)
    }

    /**
     * Splits [studiesOnDay] into interleaved (has sims) vs stacked-at-end (study only).
     */
    fun partitionStudiesBySimAvailability(
        studiesOnDay: List<ConceptEntity>,
        allStudiesInChapter: List<ConceptEntity>,
        simsWithUrl: List<ConceptEntity>,
    ): Pair<List<ConceptEntity>, List<ConceptEntity>> {
        val withSims = mutableListOf<ConceptEntity>()
        val studyOnly = mutableListOf<ConceptEntity>()

        studiesOnDay.sortedBy { it.orderIndex }.forEach { study ->
            if (simUrlsForStudy(study, allStudiesInChapter, simsWithUrl).isNotEmpty()) {
                withSims += study
            } else {
                studyOnly += study
            }
        }

        return withSims to studyOnly
    }

    /**
     * Limits sim URLs only when the exam is imminent (≤1 day away).
     * Interleaved study blocks are already sized to [SIMS_PER_STUDY_BLOCK]; do not trim them
     * on normal lesson days — the plan's per-day minute estimate is for studies, not sim count.
     */
    fun capSimUrlsByPacing(
        sims: List<ConceptEntity>,
        dailyMinutes: Int,
        daysUntilExam: Long,
        studiesOnDay: Int,
    ): List<ConceptEntity> {
        if (sims.isEmpty()) return emptyList()
        if (daysUntilExam > 1) return sims

        val studyReserve = studiesOnDay.coerceAtLeast(1) * MINUTES_PER_STUDY
        val simBudget = (dailyMinutes - studyReserve).coerceAtLeast(MINUTES_PER_SIM_URL)
        val budgetCap = (simBudget / MINUTES_PER_SIM_URL).coerceAtLeast(1)

        return sims.take(minOf(sims.size, budgetCap))
    }
}
