package com.ncert7.aitutorandlab.domain.simulation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the sim-URL + guide-filename resolution behind the two production bugs:
 *   Bug 1 — Kannada language loaded the English sim (blank `simulation_url_kannada`).
 *   Bug 2 — coach guide resolves by exact sim filename (`science_3_1.html` → `science_3_1.guide.json`).
 */
class SimulationUrlResolverTest {

    private val EN = "https://anuragmn.github.io/EduAI_app/Simulations/science_3_1.html"
    private val KN = "https://anuragmn.github.io/EduAI_app/Simulations/science_3_1_kn.html"

    // ---- isValidUrl -------------------------------------------------------

    @Test
    fun isValidUrl_rejectsBlankAndSentinels() {
        assertFalse(SimulationUrlResolver.isValidUrl(null))
        assertFalse(SimulationUrlResolver.isValidUrl(""))
        assertFalse(SimulationUrlResolver.isValidUrl("   "))
        assertFalse(SimulationUrlResolver.isValidUrl("null"))
        assertFalse(SimulationUrlResolver.isValidUrl("Not found"))
        assertFalse(SimulationUrlResolver.isValidUrl("NOT FOUND"))
    }

    @Test
    fun isValidUrl_acceptsRealUrl() {
        assertTrue(SimulationUrlResolver.isValidUrl(EN))
    }

    // ---- isKannada --------------------------------------------------------

    @Test
    fun isKannada_matchesKnVariantsOnly() {
        assertTrue(SimulationUrlResolver.isKannada("kn"))
        assertTrue(SimulationUrlResolver.isKannada("KN"))
        assertTrue(SimulationUrlResolver.isKannada("kn-IN"))
        assertTrue(SimulationUrlResolver.isKannada("kn_IN"))
        assertFalse(SimulationUrlResolver.isKannada("en"))
        assertFalse(SimulationUrlResolver.isKannada("kannada")) // not a code
        assertFalse(SimulationUrlResolver.isKannada(null))
    }

    // ---- deriveKannadaUrl -------------------------------------------------

    @Test
    fun deriveKannadaUrl_insertsKnBeforeHtml() {
        assertEquals(KN, SimulationUrlResolver.deriveKannadaUrl(EN))
    }

    @Test
    fun deriveKannadaUrl_preservesQueryAndFragment() {
        assertEquals(
            "https://host/x/science_3_1_kn.html?v=2#top",
            SimulationUrlResolver.deriveKannadaUrl("https://host/x/science_3_1.html?v=2#top"),
        )
    }

    @Test
    fun deriveKannadaUrl_idempotentForAlreadyKn() {
        assertEquals(KN, SimulationUrlResolver.deriveKannadaUrl(KN))
    }

    @Test
    fun deriveKannadaUrl_nullForNonHtmlOrInvalid() {
        assertNull(SimulationUrlResolver.deriveKannadaUrl("https://host/x/science_3_1.pdf"))
        assertNull(SimulationUrlResolver.deriveKannadaUrl("Not found"))
        assertNull(SimulationUrlResolver.deriveKannadaUrl(null))
    }

    // ---- selectSimUrl (Bug 1) --------------------------------------------

    @Test
    fun selectSimUrl_englishLanguageAlwaysEnglish() {
        assertEquals(EN, SimulationUrlResolver.selectSimUrl(EN, KN, "en"))
    }

    @Test
    fun selectSimUrl_kannadaPrefersExplicitKannadaUrl() {
        assertEquals(KN, SimulationUrlResolver.selectSimUrl(EN, KN, "kn"))
    }

    @Test
    fun selectSimUrl_kannadaBlankKn_defaultFallsBackToEnglish() {
        // Default (allowDerivedKannada = false) reproduces today's safe fallback:
        // better English content than a possible 404 for sims with no _kn file.
        assertEquals(EN, SimulationUrlResolver.selectSimUrl(EN, "", "kn"))
        assertEquals(EN, SimulationUrlResolver.selectSimUrl(EN, "Not found", "kn"))
        assertEquals(EN, SimulationUrlResolver.selectSimUrl(EN, null, "kn"))
    }

    @Test
    fun selectSimUrl_kannadaBlankKn_derivesWhenAllowed() {
        assertEquals(
            KN,
            SimulationUrlResolver.selectSimUrl(EN, "", "kn", allowDerivedKannada = true),
        )
    }

    @Test
    fun selectSimUrl_kannadaBlankKnAndBlankEn_isNull() {
        assertNull(SimulationUrlResolver.selectSimUrl("", "", "kn"))
        assertNull(SimulationUrlResolver.selectSimUrl("Not found", null, "kn", allowDerivedKannada = true))
    }

    // ---- guideFileNameForSim (Bug 2) -------------------------------------

    @Test
    fun guideFileName_science3_1_matchesAuthoredGuide() {
        assertEquals("science_3_1.guide.json", SimulationUrlResolver.guideFileNameForSim(EN))
    }

    @Test
    fun guideFileName_kannadaVariant() {
        assertEquals("science_3_1_kn.guide.json", SimulationUrlResolver.guideFileNameForSim(KN))
    }

    @Test
    fun guideFileName_stripsQueryAndFragment() {
        assertEquals(
            "math_1_1_new.guide.json",
            SimulationUrlResolver.guideFileNameForSim("https://host/s/math_1_1_new.html?ts=9#a"),
        )
    }

    @Test
    fun guideFileName_nullForNonHtml() {
        assertNull(SimulationUrlResolver.guideFileNameForSim("https://host/s/science_3_1.json"))
        assertNull(SimulationUrlResolver.guideFileNameForSim(""))
    }
}
