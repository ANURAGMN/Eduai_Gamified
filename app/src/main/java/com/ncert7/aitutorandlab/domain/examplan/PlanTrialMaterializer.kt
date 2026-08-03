package com.ncert7.aitutorandlab.domain.examplan

import com.ncert7.aitutorandlab.data.local.dao.ChapterDao
import com.ncert7.aitutorandlab.data.local.dao.ConceptDao
import com.ncert7.aitutorandlab.data.local.dao.ExamPlanDao
import com.ncert7.aitutorandlab.data.local.entities.ConceptEntity
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanDayEntity
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemEntity
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemKind
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemStatus
import com.ncert7.aitutorandlab.utils.TrialCopy
import com.ncert7.aitutorandlab.utils.getLocalizedName
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the vertical exam-trial queue for a plan day from syllabus data.
 *
 * Lesson days: sim URLs batched before each study (sim1…simN, study1, …). Sim agents are omitted.
 * Revise days: revision items first, then simulation agents for the covered chapters.
 */
@Singleton
class PlanTrialMaterializer @Inject constructor(
    private val conceptDao: ConceptDao,
    private val chapterDao: ChapterDao,
    private val examPlanDao: ExamPlanDao,
) {
    suspend fun materializeDay(
        day: ExamPlanDayEntity,
        languageCode: String,
    ): List<PlanTrialItemEntity> {
        val conceptIds =
            day.conceptIds
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

        return when (day.dayType) {
            "LESSON" -> materializeLessonDay(day, conceptIds, languageCode)
            "REVISE" -> materializeReviseDay(day, conceptIds, languageCode)
            else -> emptyList()
        }
    }

    private suspend fun materializeLessonDay(
        day: ExamPlanDayEntity,
        studyConceptIds: List<String>,
        languageCode: String,
    ): List<PlanTrialItemEntity> {
        if (studyConceptIds.isEmpty()) return emptyList()

        val studyConcepts =
            studyConceptIds.mapNotNull { conceptDao.getConcept(it) }
        if (studyConcepts.isEmpty()) return emptyList()

        val daysUntilExam = daysUntilExam(day)
        val chapterIds = studyConcepts.map { it.chapterId }.distinct()
        val chapterOrder =
            chapterIds.associateWith { chapterId ->
                chapterDao.getChapter(chapterId)?.orderIndex ?: Int.MAX_VALUE
            }
        val orderedChapterIds = chapterIds.sortedBy { chapterOrder[it] ?: Int.MAX_VALUE }

        val studyByChapter = studyConcepts.groupBy { it.chapterId }
        val items = mutableListOf<PlanTrialItemEntity>()
        var sequence = 0

        for (chapterId in orderedChapterIds) {
            val chapterName =
                chapterDao.getChapter(chapterId)?.getLocalizedName(languageCode) ?: chapterId

            val studiesOnDay =
                studyByChapter[chapterId].orEmpty().sortedBy { it.orderIndex }
            val allStudiesInChapter =
                conceptDao.getStudyConceptsForChapter(chapterId).sortedBy { it.orderIndex }
            val simsWithUrl = simsWithUrlForChapter(chapterId, languageCode)

            val (studiesWithSims, studiesStudyOnly) =
                TrialItemOrdering.partitionStudiesBySimAvailability(
                    studiesOnDay = studiesOnDay,
                    allStudiesInChapter = allStudiesInChapter,
                    simsWithUrl = simsWithUrl,
                )

            fun emitStudy(study: ConceptEntity) {
                items +=
                    buildItem(
                        day = day,
                        chapterId = chapterId,
                        concept = study,
                        kind = PlanTrialItemKind.STUDY,
                        sourceId = study.conceptId,
                        title =
                            TrialCopy.itemTitle(
                                languageCode = languageCode,
                                chapterName = chapterName,
                                kind = PlanTrialItemKind.STUDY,
                                conceptName = study.getLocalizedName(languageCode),
                            ),
                        sequenceIndex = sequence++,
                        requiredCount = 7,
                    )
            }

            fun emitSimUrl(concept: ConceptEntity, simUrl: String) {
                items +=
                    buildItem(
                        day = day,
                        chapterId = chapterId,
                        concept = concept,
                        kind = PlanTrialItemKind.SIM_URL,
                        sourceId = simUrl,
                        title =
                            TrialCopy.itemTitle(
                                languageCode = languageCode,
                                chapterName = chapterName,
                                kind = PlanTrialItemKind.SIM_URL,
                                conceptName = concept.getLocalizedName(languageCode),
                            ),
                        sequenceIndex = sequence++,
                        requiredCount = 7,
                    )
            }

            studiesWithSims.forEachIndexed { index, study ->
                val simBlock =
                    TrialItemOrdering.simUrlsForStudy(
                        study = study,
                        allStudiesInChapter = allStudiesInChapter,
                        simsWithUrl = simsWithUrl,
                    )
                val cappedBlock =
                    TrialItemOrdering.capSimUrlsByPacing(
                        sims = simBlock,
                        dailyMinutes = day.estimatedMinutes,
                        daysUntilExam = daysUntilExam,
                        studiesOnDay = studiesWithSims.size - index,
                    )

                cappedBlock.forEach { sim ->
                    emitSimUrl(sim, resolvedSimulationUrl(sim, languageCode)!!)
                }

                if (cappedBlock.isEmpty()) {
                    val studyUrl = resolvedSimulationUrl(study, languageCode)
                    if (isValidSimUrl(studyUrl)) {
                        emitSimUrl(study, studyUrl!!)
                    }
                }

                emitStudy(study)
            }

            studiesStudyOnly.forEach { study ->
                val studyUrl = resolvedSimulationUrl(study, languageCode)
                if (isValidSimUrl(studyUrl)) {
                    emitSimUrl(study, studyUrl!!)
                }
                emitStudy(study)
            }
        }

        return items
    }

    private suspend fun materializeReviseDay(
        day: ExamPlanDayEntity,
        conceptIds: List<String>,
        languageCode: String,
    ): List<PlanTrialItemEntity> {
        if (conceptIds.isEmpty()) return emptyList()

        val items = mutableListOf<PlanTrialItemEntity>()
        var sequence = 0
        val chapterIds = linkedSetOf<String>()

        conceptIds.forEach { conceptId ->
            val concept = conceptDao.getConcept(conceptId) ?: return@forEach
            val chapter =
                chapterDao.getChapter(concept.chapterId)
                    ?: return@forEach
            chapterIds += concept.chapterId
            items +=
                buildItem(
                    day = day,
                    chapterId = concept.chapterId,
                    concept = concept,
                    kind = PlanTrialItemKind.REVISION,
                    sourceId = concept.chapterId,
                    title =
                        TrialCopy.itemTitle(
                            languageCode = languageCode,
                            chapterName = chapter.getLocalizedName(languageCode),
                            kind = PlanTrialItemKind.REVISION,
                            conceptName = concept.getLocalizedName(languageCode),
                        ),
                    sequenceIndex = sequence++,
                    requiredCount = 1,
                )
        }

        chapterIds.forEach { chapterId ->
            val chapterName =
                chapterDao.getChapter(chapterId)?.getLocalizedName(languageCode) ?: chapterId
            val simAgents =
                conceptDao.getConceptsForChapterSync(chapterId, "SIMULATION")
                    .sortedBy { it.orderIndex }
                    .filter { isValidSimId(resolvedSimulationId(it, languageCode)) }

            simAgents.forEach { sim ->
                val simId = resolvedSimulationId(sim, languageCode)!!
                items +=
                    buildItem(
                        day = day,
                        chapterId = chapterId,
                        concept = sim,
                        kind = PlanTrialItemKind.SIM_AGENT,
                        sourceId = simId,
                        title =
                            TrialCopy.itemTitle(
                                languageCode = languageCode,
                                chapterName = chapterName,
                                kind = PlanTrialItemKind.SIM_AGENT,
                                conceptName = sim.getLocalizedName(languageCode),
                            ),
                        sequenceIndex = sequence++,
                        requiredCount = 7,
                    )
            }
        }

        return items
    }

    private suspend fun simsWithUrlForChapter(
        chapterId: String,
        languageCode: String,
    ): List<ConceptEntity> {
        val sims =
            conceptDao.getConceptsForChapterSync(chapterId, "SIMULATION")
                .sortedBy { it.orderIndex }
        return sims.filter { isValidSimUrl(resolvedSimulationUrl(it, languageCode)) }
    }

    private suspend fun daysUntilExam(day: ExamPlanDayEntity): Long {
        val plan = examPlanDao.getActivePlan(day.studentId) ?: return 7
        return (plan.examEpochDay - day.calendarEpochDay).coerceAtLeast(0)
    }

    private fun resolvedSimulationUrl(concept: ConceptEntity, languageCode: String): String? {
        val localized = concept.simulationUrlFor(languageCode)
        return localized?.takeIf { isValidSimUrl(it) }
    }

    private fun resolvedSimulationId(concept: ConceptEntity, languageCode: String): String? {
        val localized = concept.simulationIdFor(languageCode)
        return localized?.takeIf { isValidSimId(it) }
    }

    private fun buildItem(
        day: ExamPlanDayEntity,
        chapterId: String,
        concept: ConceptEntity,
        kind: String,
        sourceId: String,
        title: String,
        sequenceIndex: Int,
        requiredCount: Int,
    ): PlanTrialItemEntity =
        buildItemRaw(
            studentId = day.studentId,
            planDayId = day.id,
            dayIndex = day.dayIndex,
            chapterId = chapterId,
            concept = concept,
            kind = kind,
            sourceId = sourceId,
            title = title,
            sequenceIndex = sequenceIndex,
            requiredCount = requiredCount,
        )

    private fun buildItemRaw(
        studentId: String,
        planDayId: Long,
        dayIndex: Int,
        chapterId: String,
        concept: ConceptEntity,
        kind: String,
        sourceId: String,
        title: String,
        sequenceIndex: Int,
        requiredCount: Int,
    ): PlanTrialItemEntity =
        PlanTrialItemEntity(
            studentId = studentId,
            planDayId = planDayId,
            dayIndex = dayIndex,
            chapterId = chapterId,
            conceptId = concept.conceptId,
            kind = kind,
            sourceId = sourceId,
            title = title,
            sequenceIndex = sequenceIndex,
            requiredCount = requiredCount,
            completedCount = 0,
            status = PlanTrialItemStatus.PENDING,
            celebrated = false,
        )

    /**
     * Builds a standalone trial for a single chapter (opened from the chapter picker),
     * independent of the exam-plan schedule. Order matches the plan trial: simulations
     * (SIM_URL) → lessons (STUDY) → revision → simulation agents. Stored under a
     * chapter-scoped [dayIndex].
     */
    suspend fun materializeChapter(
        studentId: String,
        chapterId: String,
        dayIndex: Int,
        planDayId: Long,
        languageCode: String,
    ): List<PlanTrialItemEntity> {
        val chapterName =
            chapterDao.getChapter(chapterId)?.getLocalizedName(languageCode) ?: chapterId
        val items = mutableListOf<PlanTrialItemEntity>()
        var sequence = 0

        fun emit(concept: ConceptEntity, kind: String, sourceId: String, requiredCount: Int) {
            items +=
                buildItemRaw(
                    studentId = studentId,
                    planDayId = planDayId,
                    dayIndex = dayIndex,
                    chapterId = chapterId,
                    concept = concept,
                    kind = kind,
                    sourceId = sourceId,
                    title =
                        TrialCopy.itemTitle(
                            languageCode = languageCode,
                            chapterName = chapterName,
                            kind = kind,
                            conceptName = concept.getLocalizedName(languageCode),
                        ),
                    sequenceIndex = sequence++,
                    requiredCount = requiredCount,
                )
        }

        // Order: simulations → study → revision → sim agent → math (agents last).

        // 1. Simulations (URL) for the chapter.
        simsWithUrlForChapter(chapterId, languageCode).forEach { sim ->
            val url = resolvedSimulationUrl(sim, languageCode) ?: return@forEach
            emit(sim, PlanTrialItemKind.SIM_URL, url, requiredCount = 7)
        }

        // 2. Lessons.
        val studies =
            conceptDao.getStudyConceptsForChapter(chapterId).sortedBy { it.orderIndex }
        studies.forEach { study ->
            emit(study, PlanTrialItemKind.STUDY, study.conceptId, requiredCount = 7)
        }

        // 3. Revision for the chapter (one item, when there is content to revise).
        studies.firstOrNull()?.let { rep ->
            emit(rep, PlanTrialItemKind.REVISION, chapterId, requiredCount = 1)
        }

        // 4. Simulation agents.
        conceptDao.getConceptsForChapterSync(chapterId, "SIMULATION")
            .sortedBy { it.orderIndex }
            .filter { isValidSimId(resolvedSimulationId(it, languageCode)) }
            .forEach { sim ->
                val simId = resolvedSimulationId(sim, languageCode) ?: return@forEach
                emit(sim, PlanTrialItemKind.SIM_AGENT, simId, requiredCount = 7)
            }

        // 5. Math practice problems — agent-based, so they come last (Math chapters).
        conceptDao.getConceptsForChapterSync(chapterId, "MATH PROBLEM")
            .sortedBy { it.orderIndex }
            .forEach { problem ->
                val problemId = problem.problemId.takeIf { it.isNotBlank() } ?: return@forEach
                emit(problem, PlanTrialItemKind.MATH, problemId, requiredCount = 7)
            }

        return items
    }

    private fun ConceptEntity.simulationIdFor(languageCode: String): String? =
        when (languageCode) {
            "kn" -> simulationIdKannada
            else -> simulationId
        }

    private fun ConceptEntity.simulationUrlFor(languageCode: String): String? =
        when (languageCode) {
            "kn" -> simulationUrlKannada
            else -> simulationUrl
        }

    private fun isValidSimId(value: String?): Boolean =
        !value.isNullOrBlank() &&
            value != "null" &&
            !value.equals("not found", ignoreCase = true)

    private fun isValidSimUrl(value: String?): Boolean = isValidSimId(value)
}
