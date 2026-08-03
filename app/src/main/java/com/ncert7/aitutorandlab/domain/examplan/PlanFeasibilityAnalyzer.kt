package com.ncert7.aitutorandlab.domain.examplan

import com.ncert7.aitutorandlab.data.local.dao.ConceptDao
import com.ncert7.aitutorandlab.data.local.entities.ConceptEntity
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanDayEntity
import com.ncert7.aitutorandlab.utils.ExamPlanCopy
import com.ncert7.aitutorandlab.utils.getLocalizedName
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanFeasibilityAnalyzer @Inject constructor(
    private val conceptDao: ConceptDao,
    private val planTrialMaterializer: PlanTrialMaterializer,
) {
    private val zone = ZoneId.of("Asia/Kolkata")

    suspend fun analyze(
        chapterIds: List<String>,
        languageCode: String,
        dailyMinutes: Int,
        examDate: LocalDate,
        startDate: LocalDate = LocalDate.now(zone),
        examType: String = "Unit Test",
    ): PlanFeasibilityResult {
        if (chapterIds.isEmpty()) {
            return emptyResult(examDate, startDate)
        }

        val concepts = loadStudyConcepts(chapterIds)
        val generated =
            ExamPlanGenerator.generate(
                chapterIds = chapterIds.distinct(),
                concepts = concepts,
                conceptLabel = { it.getLocalizedName(languageCode) },
                dailyMinutes = dailyMinutes.coerceIn(15, 90),
                startDate = startDate,
                examType = examType,
            )

        val requiredPlanDays = generated.days.size
        val lessonDays = generated.days.count { it.dayType == "LESSON" }
        val reviseDays = generated.days.count { it.dayType == "REVISE" }
        val mockExamDays = generated.days.count { it.dayType == "MOCK" || it.dayType == "EXAM" }
        val availableCalendarDays =
            (ChronoUnit.DAYS.between(startDate, examDate) + 1)
                .coerceAtLeast(0)
                .toInt()

        val totalTrialItems =
            generated.days.sumOf { day ->
                countTrialItemsForGeneratedDay(day, languageCode).toInt()
            }

        val issues = mutableListOf<PlanFeasibilityIssue>()
        if (examDate.isBefore(startDate)) {
            issues +=
                PlanFeasibilityIssue(
                    severity = PlanFeasibilitySeverity.ERROR,
                    message = ExamPlanCopy.examDatePast(languageCode),
                )
        }

        val earliestStart = examDate.minusDays((requiredPlanDays - 1).toLong())
        if (requiredPlanDays > availableCalendarDays) {
            issues +=
                PlanFeasibilityIssue(
                    severity = PlanFeasibilitySeverity.ERROR,
                    message =
                        ExamPlanCopy.planNeedsMoreDays(
                            languageCode = languageCode,
                            requiredPlanDays = requiredPlanDays,
                            lessonDays = lessonDays,
                            reviseDays = reviseDays,
                            availableCalendarDays = availableCalendarDays,
                        ),
                )
        } else if (earliestStart.isBefore(startDate)) {
            issues +=
                PlanFeasibilityIssue(
                    severity = PlanFeasibilitySeverity.ERROR,
                    message =
                        ExamPlanCopy.startByDate(languageCode, earliestStart),
                )
        }

        if (issues.none { it.severity == PlanFeasibilitySeverity.ERROR }) {
            if (availableCalendarDays - requiredPlanDays <= 1) {
                issues +=
                    PlanFeasibilityIssue(
                        severity = PlanFeasibilitySeverity.WARNING,
                        message =
                            ExamPlanCopy.tightScheduleWarning(
                                languageCode,
                                availableCalendarDays - requiredPlanDays,
                            ),
                    )
            }
            if (totalTrialItems >= 40) {
                issues +=
                    PlanFeasibilityIssue(
                        severity = PlanFeasibilitySeverity.WARNING,
                        message =
                            ExamPlanCopy.largeWorkloadWarning(
                                languageCode,
                                totalTrialItems,
                                requiredPlanDays,
                            ),
                    )
            }
        }

        return PlanFeasibilityResult(
            requiredPlanDays = requiredPlanDays,
            lessonDays = lessonDays,
            reviseDays = reviseDays,
            mockExamDays = mockExamDays,
            availableCalendarDays = availableCalendarDays,
            totalTrialItems = totalTrialItems,
            issues = issues,
        )
    }

    fun computeStartDate(
        examDate: LocalDate,
        requiredPlanDays: Int,
        earliestAllowedStart: LocalDate = LocalDate.now(zone),
    ): LocalDate? {
        if (requiredPlanDays <= 0) return null
        val start = examDate.minusDays((requiredPlanDays - 1).toLong())
        return if (start.isBefore(earliestAllowedStart)) null else start
    }

    private suspend fun loadStudyConcepts(chapterIds: List<String>): List<ConceptEntity> {
        val concepts = mutableListOf<ConceptEntity>()
        chapterIds.distinct().forEach { chapterId ->
            concepts.addAll(conceptDao.getConceptsForChapterSync(chapterId, "STUDY"))
            concepts.addAll(conceptDao.getConceptsForChapterSync(chapterId, "MATH PROBLEM"))
        }
        return concepts.distinctBy { it.conceptId }
    }

    private suspend fun countTrialItemsForGeneratedDay(
        day: ExamPlanGenerator.GeneratedDay,
        languageCode: String,
    ): Long {
        if (day.dayType != "LESSON" && day.dayType != "REVISE") return 0
        val placeholderDay =
            ExamPlanDayEntity(
                studentId = "feasibility",
                dayIndex = day.dayIndex,
                calendarEpochDay = day.calendarEpochDay,
                dayType = day.dayType,
                status = "UPCOMING",
                label = day.label,
                conceptIds = day.conceptIds.joinToString(","),
                estimatedMinutes = day.estimatedMinutes,
            )
        return planTrialMaterializer.materializeDay(placeholderDay, languageCode).size.toLong()
    }

    private fun emptyResult(examDate: LocalDate, startDate: LocalDate): PlanFeasibilityResult {
        val available =
            (ChronoUnit.DAYS.between(startDate, examDate) + 1)
                .coerceAtLeast(0)
                .toInt()
        return PlanFeasibilityResult(
            requiredPlanDays = 0,
            lessonDays = 0,
            reviseDays = 0,
            mockExamDays = 0,
            availableCalendarDays = available,
            totalTrialItems = 0,
            issues = emptyList(),
        )
    }

    private fun formatDate(date: LocalDate): String =
        date.dayOfMonth.toString() + " " + date.month.name.lowercase().replaceFirstChar { it.titlecase() }
}
