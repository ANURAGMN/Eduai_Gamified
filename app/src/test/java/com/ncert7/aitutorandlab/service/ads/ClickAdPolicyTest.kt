package com.ncert7.aitutorandlab.service.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClickAdPolicyTest {

    @Test
    fun belowThreshold_isAdFree() {
        for (n in 0 until ClickAdPolicy.SIM_INTERACTIONS_PER_AD) {
            assertFalse(ClickAdPolicy.shouldShowAd(n))
        }
    }

    @Test
    fun atOrAboveThreshold_showsAd() {
        assertTrue(ClickAdPolicy.shouldShowAd(ClickAdPolicy.SIM_INTERACTIONS_PER_AD))
        assertTrue(ClickAdPolicy.shouldShowAd(ClickAdPolicy.SIM_INTERACTIONS_PER_AD + 5))
    }
}
