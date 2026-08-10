package com.ncert7.aitutorandlab.domain.examplan

import com.ncert7.aitutorandlab.data.local.entities.ConceptEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class TrialItemOrderingTest {

    @Test
    fun simUrlsForStudy_assignsTwoSimsPerStudyIndex() {
        val studies =
            listOf(
                study("s1", order = 1),
                study("s2", order = 2),
                study("s3", order = 3),
                study("s4", order = 4),
            )
        val sims = (1..8).map { sim("sim$it", it) }

        assertEquals(
            listOf("sim1", "sim2"),
            TrialItemOrdering.simUrlsForStudy(studies[0], studies, sims).map { it.conceptId },
        )
        assertEquals(
            listOf("sim3", "sim4"),
            TrialItemOrdering.simUrlsForStudy(studies[1], studies, sims).map { it.conceptId },
        )
        assertEquals(
            listOf("sim7", "sim8"),
            TrialItemOrdering.simUrlsForStudy(studies[3], studies, sims).map { it.conceptId },
        )
    }

    @Test
    fun simUrlsForStudy_day1Study1GetsFirstTwoSimsOnly() {
        val studies =
            listOf(
                study("s1", order = 1),
                study("s2", order = 2),
            )
        val sims = (1..8).map { sim("sim$it", it) }

        assertEquals(
            listOf("sim1", "sim2"),
            TrialItemOrdering.simUrlsForStudy(studies[0], studies, sims).map { it.conceptId },
        )
    }

    @Test
    fun simUrlsForStudy_oneSimBeforeFirstStudyWhenOnlyOneExists() {
        val studies = listOf(study("s1", order = 1))
        val sims = listOf(sim("sim1", 1))

        assertEquals(
            listOf("sim1"),
            TrialItemOrdering.simUrlsForStudy(studies[0], studies, sims).map { it.conceptId },
        )
    }

    @Test
    fun partitionStudiesBySimAvailability_stacksStudyOnlyAtEnd() {
        val studiesOnDay =
            listOf(
                study("s1", order = 1),
                study("s2", order = 2),
                study("s3", order = 3),
            )
        val allStudies = studiesOnDay
        // 2 sims/study → s1 and s2 get blocks; s3 has no sims left.
        val sims = (1..4).map { sim("sim$it", it) }

        val (withSims, studyOnly) =
            TrialItemOrdering.partitionStudiesBySimAvailability(
                studiesOnDay = studiesOnDay,
                allStudiesInChapter = allStudies,
                simsWithUrl = sims,
            )

        assertEquals(listOf("s1", "s2"), withSims.map { it.conceptId })
        assertEquals(listOf("s3"), studyOnly.map { it.conceptId })
    }

    @Test
    fun capSimUrlsByPacing_keepsFullBlockWhenExamNotImminent() {
        val sims = listOf(sim("a", 1), sim("b", 2))
        val capped =
            TrialItemOrdering.capSimUrlsByPacing(
                sims = sims,
                dailyMinutes = 18,
                daysUntilExam = 5,
                studiesOnDay = 1,
            )
        assertEquals(listOf("a", "b"), capped.map { it.conceptId })
    }

    @Test
    fun capSimUrlsByPacing_trimsWhenExamIsTomorrow() {
        val sims = listOf(sim("a", 1), sim("b", 2))
        val capped =
            TrialItemOrdering.capSimUrlsByPacing(
                sims = sims,
                dailyMinutes = 18,
                daysUntilExam = 1,
                studiesOnDay = 1,
            )
        // 18 − 12 study reserve = 6 min → 1 sim at 4 min each.
        assertEquals(listOf("a"), capped.map { it.conceptId })
    }

    private fun study(id: String, order: Int) =
        ConceptEntity(
            conceptId = id,
            chapterId = "ch1",
            conceptName = id,
            conceptNameKannada = "",
            orderIndex = order,
            type = "STUDY",
            problemId = "",
            problemTopicName = "",
            problemTopicNameKn = "",
            simulationId = "",
            simulationIdKannada = "",
        )

    private fun sim(id: String, order: Int) =
        ConceptEntity(
            conceptId = id,
            chapterId = "ch1",
            conceptName = id,
            conceptNameKannada = "",
            orderIndex = order,
            type = "SIMULATION",
            problemId = "",
            problemTopicName = "",
            problemTopicNameKn = "",
            simulationId = "",
            simulationIdKannada = "",
            simulationUrl = "https://example.com/$id",
        )
}
