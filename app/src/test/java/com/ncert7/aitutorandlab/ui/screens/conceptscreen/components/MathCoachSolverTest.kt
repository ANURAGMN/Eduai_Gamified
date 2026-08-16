package com.ncert7.aitutorandlab.ui.screens.conceptscreen.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regression tests for [MathCoachSolver], pinned to the exact prompts/options harvested from the
 * live chapter-1 math sims (see coach-check logcats). The pattern cases guard the 2026-08-07 rewrite
 * that fixed the two on-device failures: a lead-in number corrupting the ratio, and the squares rule.
 */
class MathCoachSolverTest {

    // ---- PATTERN (math_1_5_new) — the three round types seen in the log ------------------------

    @Test
    fun `pattern repunit 7 77 777 gives 7777`() {
        val sol = MathCoachSolver.solve(
            "Find the next product Pattern: 7 multiplied by 1, 11, 111, ... 7 77 777 ?",
            listOf("7770", "7777", "7077", "77777"),
        )
        assertNotNull(sol)
        assertEquals("7777", sol!!.correctOptionLabel)
    }

    @Test
    fun `pattern squares 81 9801 998001 gives 99980001`() {
        // Roots 9, 99, 999 follow x10+9 → next root 9999 → 9999^2 = 99980001.
        val sol = MathCoachSolver.solve(
            "Find the next product Pattern from 9x9, 99x99, 999x999, ... 81 9801 998001 ?",
            listOf("99980001", "99908001", "99800001", "999980001"),
        )
        assertNotNull(sol)
        assertEquals("99980001", sol!!.correctOptionLabel)
    }

    @Test
    fun `pattern geometric with lead-in 12 24 48 gives 96`() {
        // The "2" in "multiplied by 2" must NOT corrupt the ratio; sequence is 12,24,48 → x2 → 96.
        val sol = MathCoachSolver.solve(
            "Find the next product Each term is multiplied by 2. 12 24 48 ?",
            listOf("92", "96", "108", "120"),
        )
        assertNotNull(sol)
        assertEquals("96", sol!!.correctOptionLabel)
    }

    // ---- BUILD (math_1_4_new) — solveBuild picks the biggest button that still fits -------------

    @Test
    fun `build picks biggest fitting button then null at target`() {
        val btns = listOf(100000L, 10000L, 1000L, 100L, 10L, 1L)
        // Start of 40,629: +10,000 fits, +1,00,000 does not.
        assertEquals("10000", MathCoachSolver.solveBuild(40629, 0, btns).controlValue)
        // At 40,000: +10,000 and +1,000 overshoot, +100 fits.
        assertEquals("100", MathCoachSolver.solveBuild(40629, 40000, btns).controlValue)
        // Exactly on target: no further move (coach then glows Lock Build).
        assertNull(MathCoachSolver.solveBuild(40629, 40629, btns).controlValue)
        // Overshot: no move (coach then glows Reset).
        assertNull(MathCoachSolver.solveBuild(5072, 105020, btns).controlValue)
    }

    // ---- Core sanity: compare mixed notations, rounding ---------------------------------------

    @Test
    fun `compare 30 thousand is less than 3 lakh`() {
        val sol = MathCoachSolver.solve(
            "Select correct relation after mentally converting both notations. 30 thousand 3 lakh",
            listOf("<", "=", ">"),
        )
        assertNotNull(sol)
        assertEquals("<", sol!!.correctOptionLabel)
    }
}
