package com.ncert7.aitutorandlab.domain.examplan

import com.ncert7.aitutorandlab.data.local.entities.ConceptEntity
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil

object ExamPlanGenerator {

    private const val MINUTES_PER_CONCEPT = 18
    private const val MAX_CHAPTERS = 6

    data class GeneratedDay(
        val dayIndex: Int,
        val calendarEpochDay: Long,
        val dayType: String,
        val label: String,
        val conceptIds: List<String>,
        val estimatedMinutes: Int,
    )

    data class GeneratedPlan(
        val examType: String,
        val dailyMinutes: Int,
        val startEpochDay: Long,
        val chapterIds: List<String>,
        val days: List<GeneratedDay>,
    )

    fun generate(
        chapterIds: List<String>,
        concepts: List<ConceptEntity>,
        conceptLabel: (ConceptEntity) -> String,
        dailyMinutes: Int = 30,
        startDate: LocalDate = LocalDate.now(ZoneId.of("Asia/Kolkata")),
        examType: String = "Unit Test",
    ): GeneratedPlan {
        val selectedChapters = chapterIds.take(MAX_CHAPTERS)
        val orderedConcepts =
            concepts
                .filter { it.chapterId in selectedChapters }
                .sortedWith(compareBy({ it.chapterId }, { it.orderIndex }))

        val conceptsPerDay = (dailyMinutes / MINUTES_PER_CONCEPT).coerceAtLeast(1)
        val lessonChunks = orderedConcepts.chunked(conceptsPerDay)

        val days = mutableListOf<GeneratedDay>()
        var dayIndex = 1
        var cursorDate = startDate

        fun addDay(
            type: String,
            label: String,
            conceptIds: List<String> = emptyList(),
            minutes: Int = dailyMinutes,
        ) {
            days +=
                GeneratedDay(
                    dayIndex = dayIndex,
                    calendarEpochDay = cursorDate.toEpochDay(),
                    dayType = type,
                    label = label,
                    conceptIds = conceptIds,
                    estimatedMinutes = minutes,
                )
            dayIndex++
            cursorDate = cursorDate.plusDays(1)
        }

        if (lessonChunks.isEmpty()) {
            addDay("LESSON", "Explore your first chapter", emptyList(), dailyMinutes)
        } else {
            lessonChunks.forEach { chunk ->
                val label =
                    if (chunk.size == 1) {
                        conceptLabel(chunk.first())
                    } else {
                        "${conceptLabel(chunk.first())} +${chunk.size - 1} more"
                    }
                val minutes = (chunk.size * MINUTES_PER_CONCEPT).coerceAtMost(dailyMinutes + 10)
                addDay("LESSON", label, chunk.map { it.conceptId }, minutes)
            }
        }

        val reviseDays =
            when {
                lessonChunks.size >= 4 -> 2
                lessonChunks.isNotEmpty() -> 1
                else -> 0
            }
        repeat(reviseDays) { index ->
            val reviseConcepts =
                orderedConcepts
                    .takeLast((orderedConcepts.size / reviseDays).coerceAtLeast(1))
                    .map { it.conceptId }
            addDay(
                type = "REVISE",
                label = if (reviseDays == 1) "Revision day" else "Revision block ${index + 1}",
                conceptIds = reviseConcepts.distinct(),
                minutes = dailyMinutes,
            )
        }

        addDay("MOCK", "Mock practice · ${examType.lowercase()}", orderedConcepts.map { it.conceptId }, dailyMinutes)
        addDay("EXAM", "Exam day — you've got this!", emptyList(), 0)

        return GeneratedPlan(
            examType = examType,
            dailyMinutes = dailyMinutes,
            startEpochDay = startDate.toEpochDay(),
            chapterIds = selectedChapters,
            days = days,
        )
    }

    fun resolveStatuses(
        days: List<GeneratedDay>,
        today: LocalDate = LocalDate.now(ZoneId.of("Asia/Kolkata")),
        completedDayIndices: Set<Int> = emptySet(),
    ): List<Pair<GeneratedDay, String>> {
        val todayEpoch = today.toEpochDay()
        return days.map { day ->
            val status =
                when {
                    day.dayIndex in completedDayIndices -> "DONE"
                    day.calendarEpochDay < todayEpoch -> "DONE"
                    day.calendarEpochDay == todayEpoch -> "TODAY"
                    else -> "UPCOMING"
                }
            day to status
        }
    }

    fun estimatedTotalDays(conceptCount: Int, dailyMinutes: Int): Int {
        val conceptsPerDay = (dailyMinutes / MINUTES_PER_CONCEPT).coerceAtLeast(1)
        val lessonDays = ceil(conceptCount.toDouble() / conceptsPerDay).toInt().coerceAtLeast(1)
        val reviseDays = if (lessonDays >= 4) 2 else if (lessonDays > 0) 1 else 0
        return lessonDays + reviseDays + 2
    }
}
