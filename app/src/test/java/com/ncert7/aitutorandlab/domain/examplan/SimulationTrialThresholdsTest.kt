package com.ncert7.aitutorandlab.domain.examplan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SimulationTrialThresholdsTest {

    @Test
    fun richHtml_usesSevenAndTen() {
        val t = SimulationTrialThresholds.compute(htmlClickBudget = 15)
        assertEquals(7, t.completionAt)
        assertEquals(7, t.firstPromptAt)
        assertEquals(10, t.secondPromptAt)
    }

    @Test
    fun sparseHtml_lowersGoalAndSkipsSecondPrompt() {
        val t = SimulationTrialThresholds.compute(htmlClickBudget = 5)
        assertEquals(5, t.completionAt)
        assertNull(t.secondPromptAt)
    }

    @Test
    fun mediumHtml_secondPromptAtBudgetCap() {
        val t = SimulationTrialThresholds.compute(htmlClickBudget = 8)
        assertEquals(7, t.completionAt)
        assertEquals(8, t.secondPromptAt)
    }
}
