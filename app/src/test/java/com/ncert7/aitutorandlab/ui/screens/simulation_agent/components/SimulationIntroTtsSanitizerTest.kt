package com.ncert7.aitutorandlab.ui.screens.simulation_agent.components

import org.junit.Assert.assertTrue
import org.junit.Test

class SimulationIntroTtsSanitizerTest {

    @Test
    fun expandsDistanceAndSpeedUnits() {
        val result =
            SimulationIntroTtsSanitizer.forSpeech(
                "A car travels 60 km/h for 5 km with a 10 cm gap.",
            )
        assertTrue(result.contains("kilometers per hour"))
        assertTrue(result.contains("kilometers"))
        assertTrue(result.contains("centimeters"))
    }

    @Test
    fun expandsStandaloneUnits() {
        val result = SimulationIntroTtsSanitizer.forSpeech("Measure in cm and km/h limits.")
        assertTrue(result.contains("centimeters"))
        assertTrue(result.contains("kilometers per hour"))
    }

    @Test
    fun expandsArrowsAndEquals() {
        val result = SimulationIntroTtsSanitizer.forSpeech("Reactants -> Products and 2 + 2 = 4")
        assertTrue(result.contains("leads to"))
        assertTrue(result.contains("is equal to"))
    }

    @Test
    fun expandsMathMultiplicationAndFractions() {
        val result = SimulationIntroTtsSanitizer.forSpeech("Area = 3 x 4 and speed is 1/2 km/h.")
        assertTrue(result.contains("times"))
        assertTrue(result.contains("over"))
        assertTrue(result.contains("kilometers per hour"))
    }

    @Test
    fun expandsSuperscriptsAndTemperature() {
        val result = SimulationIntroTtsSanitizer.forSpeech("Water boils at 100°C and area is 5 m².")
        assertTrue(result.contains("degrees Celsius"))
        assertTrue(result.contains("square meters"))
    }

    @Test
    fun nonBreakingSpaceDoesNotBreakUnitExpansion() {
        // Java's \s does NOT match U+00A0, so "60<nbsp>km/h" used to be read as "km h".
        // After normalization it must expand fully.
        val nbsp = "\u00A0" // non-breaking space
        val result = SimulationIntroTtsSanitizer.forSpeech("Speed 60${nbsp}km/h over 5${nbsp}km")
        assertTrue(result.contains("60 kilometers per hour"))
        assertTrue(result.contains("5 kilometers"))
    }

    @Test
    fun preservesPlainNumbers() {
        // Regression: ASCII digits 0-9 carry the Unicode \p{Emoji} property, so the emoji filter
        // must NOT delete them. Numbers were being skipped in spoken output for sim narration.
        val result = SimulationIntroTtsSanitizer.forSpeech("There are 42 marbles and 7 boxes, total 49.")
        assertTrue(result.contains("42"))
        assertTrue(result.contains("7"))
        assertTrue(result.contains("49"))
    }
}
