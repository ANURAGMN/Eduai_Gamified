package com.ncert7.aitutorandlab.domain.examplan

import org.junit.Assert.assertEquals
import org.junit.Test

class SimulationTrialThresholdsTest {

    @Test
    fun compute_alwaysUsesDefaultGoal_evenWhenHtmlBudgetIsSmaller() {
        val t = SimulationTrialThresholds.compute(htmlClickBudget = 2)
        assertEquals(2, t.htmlClickBudget)
        assertEquals(SimulationTrialThresholds.DEFAULT_GOAL, t.completionAt)
    }

    @Test
    fun compute_keepsGoalWhenBudgetIsLarger() {
        val t = SimulationTrialThresholds.compute(htmlClickBudget = 40)
        assertEquals(40, t.htmlClickBudget)
        assertEquals(SimulationTrialThresholds.DEFAULT_GOAL, t.completionAt)
    }

    @Test
    fun compute_honorsHigherPlanRequiredCount() {
        val t = SimulationTrialThresholds.compute(htmlClickBudget = 3, planRequiredCount = 20)
        assertEquals(20, t.completionAt)
    }

    @Test
    fun compute_raisesPlanRequiredCountBelowDefaultToDefault() {
        val t = SimulationTrialThresholds.compute(htmlClickBudget = 5, planRequiredCount = 7)
        assertEquals(SimulationTrialThresholds.DEFAULT_GOAL, t.completionAt)
    }
}
