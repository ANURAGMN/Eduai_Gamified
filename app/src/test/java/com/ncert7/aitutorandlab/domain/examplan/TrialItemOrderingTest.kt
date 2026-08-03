package com.ncert7.aitutorandlab.domain.examplan

import com.ncert7.aitutorandlab.data.local.entities.ConceptEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class TrialItemOrderingTest {

    @Test
    fun simUrlsForStudy_assignsThreeSimsPerStudyIndex() {
        val studies =
            listOf(
                study("s1", order = 1),
                study("s2", order = 2),
                study("s3", order = 3),
                study("s4", order = 4),
            )
        val sims = (1..12).map { sim("sim$it", it) }

        assertEquals(
            listOf("sim1", "sim2", "sim3"),
            TrialItemOrdering.simUrlsForStudy(studies[0], studies, sims).map { it.conceptId },
        )
        assertEquals(
            listOf("sim4", "sim5", "sim6"),
            TrialItemOrdering.simUrlsForStudy(studies[1], studies, sims).map { it.conceptId },
        )
        assertEquals(
            listOf("sim10", "sim11", "sim12"),
            TrialItemOrdering.simUrlsForStudy(studies[3], studies, sims).map { it.conceptId },
        )
    }

    @Test
    fun simUrlsForStudy_day1Study1GetsFirstThreeSimsOnly() {
        val studies =
            listOf(
                study("s1", order = 1),
                study("s2", order = 2),
            )
        val sims = (1..12).map { sim("sim$it", it) }

        assertEquals(
            listOf("sim1", "sim2", "sim3"),
            TrialItemOrdering.simUrlsForStudy(studies[0], studies, sims).map { it.conceptId },
        )
    }

    @Test
    fun simUrlsForStudy_twoSimsBeforeFirstStudyWhenOnlyTwoExist() {
        val studies = listOf(study("s1", order = 1))
        val sims = listOf(sim("sim1", 1), sim("sim2", 2))

        assertEquals(
            listOf("sim1", "sim2"),
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
        val sims = (1..6).map { sim("sim$it", it) }

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
        val sims = listOf(sim("a", 1), sim("b", 2), sim("c", 3))
        val capped =
            TrialItemOrdering.capSimUrlsByPacing(
                sims = sims,
                dailyMinutes = 18,
                daysUntilExam = 5,
                studiesOnDay = 1,
            )
        assertEquals(listOf("a", "b", "c"), capped.map { it.conceptId })
    }

    @Test
    fun capSimUrlsByPacing_trimsWhenExamIsTomorrow() {
        val sims = listOf(sim("a", 1), sim("b", 2), sim("c", 3))
        val capped =
            TrialItemOrdering.capSimUrlsByPacing(
                sims = sims,
                dailyMinutes = 18,
                daysUntilExam = 1,
                studiesOnDay = 1,
            )
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
