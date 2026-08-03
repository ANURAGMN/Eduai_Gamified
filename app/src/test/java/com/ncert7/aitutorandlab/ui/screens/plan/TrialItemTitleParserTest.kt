package com.ncert7.aitutorandlab.ui.screens.plan

import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemKind
import org.junit.Assert.assertEquals
import org.junit.Test

class TrialItemTitleParserTest {
    @Test
    fun parse_simUrlTitle_splitsChapterKindDetail() {
        val parts =
            TrialItemTitleParser.parse(
                "Exploring Substances Acids, Bases and Neutral · Simulation · Setting the Context",
            )
        assertEquals("Exploring Substances Acids, Bases and Neutral", parts.chapter)
        assertEquals("Simulation", parts.kind)
        assertEquals("Setting the Context", parts.detail)
    }

    @Test
    fun heroTitle_simUsesChapter_notSimulationActivityName() {
        val title =
            TrialItemTitleParser.heroTitle(
                item =
                    com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemEntity(
                        id = 1,
                        studentId = "s",
                        planDayId = 1,
                        dayIndex = 1,
                        chapterId = "c",
                        conceptId = "x",
                        kind = PlanTrialItemKind.SIM_URL,
                        sourceId = "url",
                        title = "Exploring Substances · Simulation · Setting the Context",
                        sequenceIndex = 0,
                    ),
                todayPlanLabel = "Acids plan",
                fallbackConceptName = "Wrong",
                selectedSubjectName = "Science",
            )
        assertEquals("Exploring Substances", title)
    }

    @Test
    fun heroNextLine_omitsChapterPrefix() {
        val line =
            TrialItemTitleParser.heroNextLine(
                "Exploring Substances · Simulation · Setting the Context",
                kannada = false,
            )
        assertEquals("Next: Simulation · Setting the Context", line)
    }
}
