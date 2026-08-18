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
import com.ncert7.aitutorandlab.utils.isKannadaLanguage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the vertical exam-trial queue for a plan day from syllabus data.
 *
 * Lesson days: 2 sim URLs before each study (sim, sim, study, …). Sim agents are omitted
 * (they appear on revise days and at the end of chapter trials).
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
                        requiredCount = SimulationTrialThresholds.DEFAULT_GOAL,
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
        // Prefer the language-specific URL, but fall back to English so a KN day still
        // materializes when Firestore has no simulationUrlKannada (otherwise rematerialize
        // on language switch strips sims and the exam-trial day looks empty/broken).
        val preferred = concept.simulationUrlFor(languageCode)?.takeIf { isValidSimUrl(it) }
        if (preferred != null) return preferred
        if (isKannadaLanguage(languageCode)) {
            return concept.simulationUrl?.takeIf { isValidSimUrl(it) }
        }
        return null
    }

    private fun resolvedSimulationId(concept: ConceptEntity, languageCode: String): String? {
        val preferred = concept.simulationIdFor(languageCode)?.takeIf { isValidSimId(it) }
        if (preferred != null) return preferred
        if (isKannadaLanguage(languageCode)) {
            return concept.simulationId?.takeIf { isValidSimId(it) }
        }
        return null
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
     * Builds a standalone trial for a single chapter (opened from the chapter picker).
     *
     * Priority (Science-style, also used for Math with math agents in the study slots):
     * 1. Interleave — up to 2 sim URLs, then a Study (or Math) agent, repeat
     * 2. Any leftover sims / study-only agents
     * 3. Revision agent
     * 4. Simulation agents last (sim + chat agent)
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

        val simsWithUrl = simsWithUrlForChapter(chapterId, languageCode)
        val studies =
            conceptDao.getStudyConceptsForChapter(chapterId).sortedBy { it.orderIndex }
        val mathProblems =
            conceptDao.getConceptsForChapterSync(chapterId, "MATH PROBLEM")
                .sortedBy { it.orderIndex }
                .filter { it.problemId.isNotBlank() }

        // Study chat fills the interleaved agent slots for Science; Math problems do so when
        // there are no STUDY concepts (Math chapters).
        val interleavedAgents = if (studies.isNotEmpty()) studies else mathProblems
        val interleavedKind =
            if (studies.isNotEmpty()) PlanTrialItemKind.STUDY else PlanTrialItemKind.MATH

        val (agentsWithSims, agentsOnly) =
            TrialItemOrdering.partitionStudiesBySimAvailability(
                studiesOnDay = interleavedAgents,
                allStudiesInChapter = interleavedAgents,
                simsWithUrl = simsWithUrl,
            )

        // 1. sim, sim, study/math, … for each agent that still has a sim block.
        agentsWithSims.forEach { agent ->
            val block =
                TrialItemOrdering.simUrlsForStudy(
                    study = agent,
                    allStudiesInChapter = interleavedAgents,
                    simsWithUrl = simsWithUrl,
                )
            block.forEach { sim ->
                val url = resolvedSimulationUrl(sim, languageCode) ?: return@forEach
                emit(sim, PlanTrialItemKind.SIM_URL, url, requiredCount = SimulationTrialThresholds.DEFAULT_GOAL)
            }
            val agentSourceId =
                if (interleavedKind == PlanTrialItemKind.MATH) {
                    agent.problemId
                } else {
                    agent.conceptId
                }
            emit(agent, interleavedKind, agentSourceId, requiredCount = 7)
        }

        // Leftover sims that did not fit a 2-sim study block (more sims than 2×agents).
        val consumedSimIds =
            agentsWithSims
                .flatMap { agent ->
                    TrialItemOrdering.simUrlsForStudy(agent, interleavedAgents, simsWithUrl)
                }
                .map { it.conceptId }
                .toSet()
        simsWithUrl
            .filter { it.conceptId !in consumedSimIds }
            .forEach { sim ->
                val url = resolvedSimulationUrl(sim, languageCode) ?: return@forEach
                emit(sim, PlanTrialItemKind.SIM_URL, url, requiredCount = SimulationTrialThresholds.DEFAULT_GOAL)
            }

        // Agents with no remaining sim block — after the interleaved section.
        agentsOnly.forEach { agent ->
            val agentSourceId =
                if (interleavedKind == PlanTrialItemKind.MATH) {
                    agent.problemId
                } else {
                    agent.conceptId
                }
            emit(agent, interleavedKind, agentSourceId, requiredCount = 7)
        }

        // If studies filled the interleaved slots, any math problems still go before revision
        // so the very end stays reserved for simulation agents.
        if (studies.isNotEmpty()) {
            mathProblems.forEach { problem ->
                emit(problem, PlanTrialItemKind.MATH, problem.problemId, requiredCount = 7)
            }
        }

        // 2. Revision (one per chapter when there is study content to revise).
        studies.firstOrNull()?.let { rep ->
            emit(rep, PlanTrialItemKind.REVISION, chapterId, requiredCount = 1)
        }

        // 3. Simulation agents last.
        conceptDao.getConceptsForChapterSync(chapterId, "SIMULATION")
            .sortedBy { it.orderIndex }
            .filter { isValidSimId(resolvedSimulationId(it, languageCode)) }
            .forEach { sim ->
                val simId = resolvedSimulationId(sim, languageCode) ?: return@forEach
                emit(sim, PlanTrialItemKind.SIM_AGENT, simId, requiredCount = 7)
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
