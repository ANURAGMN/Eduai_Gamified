package com.ncert7.aitutorandlab.domain.examplan

import com.ncert7.aitutorandlab.data.local.entities.ChapterEntity
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanEntity
import com.ncert7.aitutorandlab.utils.SubjectIds
import java.time.LocalDate
import java.time.ZoneId

/** Default exam plan for fresh installs and upgrades before the user customizes. */
object DefaultExamPlan {
    const val EXAM_TYPE = "Unit Test"
    const val DAILY_MINUTES = 30
    /** Matches the "1 week" preset in exam plan setup (exam on day 7 from today). */
    const val EXAM_DAYS_FROM_TODAY = 7L
    const val CHAPTER_ORDER_INDEX = 2

    const val EMPTY_DAY_MESSAGE =
        "Please plan your study — tap Add plan to schedule this day."

    private val zone = ZoneId.of("Asia/Kolkata")

    fun defaultExamDate(from: LocalDate = LocalDate.now(zone)): LocalDate =
        from.plusDays(EXAM_DAYS_FROM_TODAY)

    fun resolveChapterId(chapters: List<ChapterEntity>): String? {
        if (chapters.isEmpty()) return null
        val sorted = chapters.sortedBy { it.orderIndex }
        return sorted.firstOrNull { it.orderIndex == CHAPTER_ORDER_INDEX }?.chapterId
            ?: sorted.getOrNull(1)?.chapterId
    }

    fun matchesPlan(plan: ExamPlanEntity, defaultChapterId: String): Boolean {
        val chapterIds = plan.chapterIds.split(",").map { it.trim() }.filter { it.isNotBlank() }
        return plan.subjectId == SubjectIds.SCIENCE &&
            plan.examType == EXAM_TYPE &&
            chapterIds == listOf(defaultChapterId)
    }
}
