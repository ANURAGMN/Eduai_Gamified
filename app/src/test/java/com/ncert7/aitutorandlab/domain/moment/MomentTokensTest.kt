package com.ncert7.aitutorandlab.domain.moment

import org.junit.Assert.assertEquals
import org.junit.Test

class MomentTokensTest {
    @Test
    fun fill_replacesItemAndPlace() {
        val filled =
            MomentTokens(item = "sunflower", place = "meadow")
                .fill("A {item} for the {place}.")

        assertEquals("A sunflower for the meadow.", filled)
    }

    @Test
    fun fill_replacesGardenStats() {
        val filled =
            MomentTokens(
                planted = 5,
                remainingInPlace = 7,
                remainingScenes = 6,
            ).fill("{planted} · {remainingInPlace} here · {remainingScenes} scenes")

        assertEquals("5 · 7 here · 6 scenes", filled)
    }
}
