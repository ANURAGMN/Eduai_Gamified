package com.ncert7.aitutorandlab.domain.simulation

/**
 * Pure, Android-free resolution of:
 *   1. which simulation HTML a learner should load for a given language, and
 *   2. the coach-guide filename that pairs with that sim.
 *
 * This centralises logic that was previously duplicated (and drifted) across:
 *   - [ConceptSimulationViewModel.simRoute]            (had a KN→EN fallback)
 *   - [ConceptSimulationViewModel.getSelectedSimulationUrl] (no fallback)
 *   - [PlanTrialMaterializer.resolvedSimulationUrl]     (KN→EN fallback)
 *
 * Root cause of the "Kannada shows English sim + Kannada voice" bug: when Firestore's
 * `simulation_url_kannada` is empty, the Kannada URL is blank and the app silently loads the
 * English HTML while TTS stays Kannada. The durable fix is the data migration that populates
 * `simulation_url_kannada`; this resolver additionally makes the precedence explicit and testable,
 * and can optionally derive the `_kn` sibling for sims that are known to have one.
 *
 * No Android dependencies — unit-testable on the JVM.
 */
object SimulationUrlResolver {

    private val INVALID = setOf("", "null", "not found")

    /** A URL/id is usable only if it is non-blank and not a sentinel ("null" / "Not found"). */
    fun isValidUrl(value: String?): Boolean =
        !value.isNullOrBlank() && value.trim().lowercase() !in INVALID

    /** True for Kannada language codes: `kn`, `KN`, `kn-IN`, `kn_IN`. */
    fun isKannada(languageCode: String?): Boolean {
        val code = languageCode?.trim()?.lowercase() ?: return false
        return code == "kn" || code.startsWith("kn-") || code.startsWith("kn_")
    }

    /**
     * Turn an English sim URL into its Kannada sibling by inserting `_kn` before `.html`,
     * preserving any query/fragment:
     *   `.../science_3_1.html`        → `.../science_3_1_kn.html`
     *   `.../science_3_1.html?x=1#f`  → `.../science_3_1_kn.html?x=1#f`
     * Returns null if the value is not a usable `.html` URL. Idempotent for URLs already ending
     * in `_kn.html`.
     */
    fun deriveKannadaUrl(enUrl: String?): String? {
        if (!isValidUrl(enUrl)) return null
        val url = enUrl!!.trim()
        val cut = url.indexOfFirst { it == '?' || it == '#' }
        val path = if (cut >= 0) url.substring(0, cut) else url
        val suffix = if (cut >= 0) url.substring(cut) else ""
        if (!path.endsWith(".html", ignoreCase = true)) return null
        if (path.endsWith("_kn.html", ignoreCase = true)) return path + suffix
        return path.dropLast(".html".length) + "_kn.html" + suffix
    }

    /**
     * The URL the app should load for [languageCode].
     *
     * Kannada precedence:
     *   1. explicit [kannadaUrl] when valid (the durable, data-driven answer);
     *   2. the derived `_kn` sibling of [englishUrl] — only when [allowDerivedKannada] is true
     *      AND the caller knows the `_kn` file exists (else it would 404);
     *   3. [englishUrl] as a last resort so a Kannada session still shows *something*.
     *
     * Non-Kannada: always [englishUrl] (or null if invalid).
     *
     * [allowDerivedKannada] defaults to false so wiring this in does not change 404-safety for
     * sims that have no Kannada variant (e.g. Math Ch.5–9). Turn it on per-call for sim sets that
     * are known to have `_kn` files.
     */
    fun selectSimUrl(
        englishUrl: String?,
        kannadaUrl: String?,
        languageCode: String?,
        allowDerivedKannada: Boolean = false,
    ): String? {
        if (isKannada(languageCode)) {
            kannadaUrl?.takeIf { isValidUrl(it) }?.let { return it }
            if (allowDerivedKannada) deriveKannadaUrl(englishUrl)?.let { return it }
            return englishUrl?.takeIf { isValidUrl(it) }
        }
        return englishUrl?.takeIf { isValidUrl(it) }
    }

    /**
     * The coach-guide asset/host filename that pairs with a sim URL, by exact filename:
     *   `.../science_3_1.html`      → `science_3_1.guide.json`
     *   `.../science_3_1_kn.html`   → `science_3_1_kn.guide.json`
     *   `.../math_1_1_new.html?x=1` → `math_1_1_new.guide.json`
     * Returns null when the URL is not a usable `.html`. Mirrors
     * [SimGuideRepository.fileNameFor]/[guideUrlFor] so the guide loader and any pre-check agree.
     */
    fun guideFileNameForSim(simUrl: String?): String? {
        if (!isValidUrl(simUrl)) return null
        val file = simUrl!!.substringBefore('#').substringBefore('?').substringAfterLast('/').trim()
        if (!file.endsWith(".html", ignoreCase = true)) return null
        return file.dropLast(".html".length) + ".guide.json"
    }
}
