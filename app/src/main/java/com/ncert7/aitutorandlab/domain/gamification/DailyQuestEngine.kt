package com.ncert7.aitutorandlab.domain.gamification

import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.data.local.dao.ProgressDao
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanDayEntity
import com.ncert7.aitutorandlab.domain.progress.model.ProgressStatus

data class DailyQuestProgress(
    val simsDone: Int,
    val simsTotal: Int,
    val studyDone: Int,
    val studyTotal: Int,
    val studyLabelPrefix: String,
)

object DailyQuestEngine {
    private const val DEFAULT_SIMS_TOTAL = 3
    private const val DEFAULT_STUDY_TOTAL = 1

    suspend fun calculate(
        todayPlanDay: ExamPlanDayEntity?,
        planChapterIds: List<String>,
        studentId: String,
        languageCode: String,
        startOfDay: Long,
        endOfDay: Long,
        progressDao: ProgressDao,
    ): DailyQuestProgress {
        if (todayPlanDay?.dayType == "EXAM") {
            return DailyQuestProgress(
                simsDone = 0,
                simsTotal = 0,
                studyDone = 0,
                studyTotal = 0,
                studyLabelPrefix = "Rest day",
            )
        }

        val simsDone =
            progressDao.getTodayCompletedSimulationCount(
                studentId = studentId,
                language = languageCode,
                startOfDay = startOfDay,
                endOfDay = endOfDay,
                appName = AppConfig.APP_NAME,
                completedStatus = ProgressStatus.COMPLETED.value,
            ).coerceAtMost(DEFAULT_SIMS_TOTAL)

        val (studyDone, studyTotal, studyLabelPrefix) =
            when (todayPlanDay?.dayType) {
                "REVISE" -> {
                    val conceptIds = parseConceptIds(todayPlanDay.conceptIds)
                    val done =
                        if (conceptIds.isEmpty()) {
                            progressDao.countTodayCompletedRevisions(
                                studentId = studentId,
                                language = languageCode,
                                startOfDay = startOfDay,
                                endOfDay = endOfDay,
                                appName = AppConfig.APP_NAME,
                                completedStatus = ProgressStatus.COMPLETED.value,
                            )
                        } else {
                            progressDao.countTodayCompletedRevisionsForConcepts(
                                studentId = studentId,
                                conceptIds = conceptIds,
                                language = languageCode,
                                startOfDay = startOfDay,
                                endOfDay = endOfDay,
                                appName = AppConfig.APP_NAME,
                                completedStatus = ProgressStatus.COMPLETED.value,
                            )
                        }
                    Triple(done.coerceAtMost(1), 1, "Revision · ")
                }
                "MOCK" -> {
                    val done =
                        if (planChapterIds.isEmpty()) {
                            0
                        } else {
                            progressDao.countTodayCompletedInChapters(
                                studentId = studentId,
                                chapterIds = planChapterIds,
                                language = languageCode,
                                startOfDay = startOfDay,
                                endOfDay = endOfDay,
                                appName = AppConfig.APP_NAME,
                                completedStatus = ProgressStatus.COMPLETED.value,
                            )
                        }
                    Triple(done.coerceAtLeast(0).coerceAtMost(1), 1, "Mock task · ")
                }
                "LESSON", null -> {
                    val conceptIds = todayPlanDay?.let { parseConceptIds(it.conceptIds) }.orEmpty()
                    if (conceptIds.isEmpty()) {
                        val done =
                            progressDao.getTodayCompletedConceptCount(
                                studentId = studentId,
                                language = languageCode,
                                startOfDay = startOfDay,
                                endOfDay = endOfDay,
                                appName = AppConfig.APP_NAME,
                                completedStatus = ProgressStatus.COMPLETED.value,
                            )
                        Triple(done.coerceAtMost(1), DEFAULT_STUDY_TOTAL, "Study · ")
                    } else {
                        val done =
                            progressDao.countTodayCompletedConceptsForIds(
                                studentId = studentId,
                                conceptIds = conceptIds,
                                language = languageCode,
                                startOfDay = startOfDay,
                                endOfDay = endOfDay,
                                appName = AppConfig.APP_NAME,
                                completedStatus = ProgressStatus.COMPLETED.value,
                            )
                        Triple(
                            done.coerceAtMost(conceptIds.size),
                            conceptIds.size.coerceAtLeast(1),
                            "Plan task · ",
                        )
                    }
                }
                else -> Triple(0, DEFAULT_STUDY_TOTAL, "Study · ")
            }

        return DailyQuestProgress(
            simsDone = simsDone,
            simsTotal = DEFAULT_SIMS_TOTAL,
            studyDone = studyDone,
            studyTotal = studyTotal,
            studyLabelPrefix = studyLabelPrefix,
        )
    }

    private fun parseConceptIds(raw: String): List<String> =
        raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
}
