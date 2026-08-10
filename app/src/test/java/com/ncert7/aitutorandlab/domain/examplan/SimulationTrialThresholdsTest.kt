package com.ncert7.aitutorandlab.domain.examplan

import org.junit.Assert.assertEquals
import org.junit.Test

class SimulationTrialThresholdsTest {

    @Test
    fun richHtml_usesSeven() {
        val t = SimulationTrialThresholds.compute(htmlClickBudget = 15)
        assertEquals(7, t.completionAt)
        assertEquals(15, t.htmlClickBudget)
    }

    @Test
    fun sparseHtml_lowersGoal() {
        val t = SimulationTrialThresholds.compute(htmlClickBudget = 5)
        assertEquals(5, t.completionAt)
    }

    @Test
    fun mediumHtml_capsAtPlanGoal() {
        val t = SimulationTrialThresholds.compute(htmlClickBudget = 8)
        assertEquals(7, t.completionAt)
    }
}
