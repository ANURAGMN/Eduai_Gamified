package com.ncert7.aitutorandlab.utils

/**
 * Picks the simulation HTML URL for the active app language.
 *
 * Kannada sessions must load `*_kn.html` (or the Firestore Kannada URL). Falling back to the
 * English HTML while TTS speaks Kannada is what users report as “English sim + Kannada voice”.
 */
object SimulationLanguageUrl {

    fun isUsable(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val t = url.trim()
        if (t.equals("null", ignoreCase = true)) return false
        if (t.equals("not found", ignoreCase = true)) return false
        return t.startsWith("http://") || t.startsWith("https://") || t.endsWith(".html")
    }

    /** True when the URL already points at a Kannada HTML asset. */
    fun looksKannada(url: String): Boolean =
        url.contains("_kn.html", ignoreCase = true) ||
            url.contains("/kn/", ignoreCase = true) ||
            url.contains("_kannada", ignoreCase = true)

    /**
     * Best-effort rewrite: `science_3_1.html` → `science_3_1_kn.html`
     * (keeps query string). Returns null if already Kannada or not an `.html` URL.
     */
    fun toKannadaTwin(englishUrl: String): String? {
        if (!isUsable(englishUrl) || looksKannada(englishUrl)) return null
        val rewritten = englishUrl.replace(Regex("""\.html([?#].*)?$""", RegexOption.IGNORE_CASE), "_kn.html$1")
        return rewritten.takeIf { it != englishUrl }
    }

    /**
     * Resolve which HTML to load.
     * Order for Kannada: explicit KN URL → `_kn.html` twin of EN URL → (optional) EN fallback.
     */
    fun resolve(
        languageCode: String,
        englishUrl: String?,
        kannadaUrl: String?,
        allowEnglishFallback: Boolean = true,
    ): String? {
        val kn = languageCode.equals("kn", ignoreCase = true) ||
            languageCode.equals("kannada", ignoreCase = true)
        if (!kn) return englishUrl?.takeIf { isUsable(it) }

        kannadaUrl?.takeIf { isUsable(it) }?.let { return it }
        englishUrl?.takeIf { isUsable(it) }?.let { en ->
            toKannadaTwin(en)?.let { return it }
            if (allowEnglishFallback) return en
        }
        return null
    }

    /**
     * When the agent API returns a non-blank `html_url`, still replace it if the app is in
     * Kannada and we have a better KN catalog URL (or can derive a `_kn.html` twin).
     */
    fun preferKannadaOverAgentUrl(
        languageCode: String,
        agentHtmlUrl: String?,
        englishUrl: String?,
        kannadaUrl: String?,
    ): String? {
        val kn = languageCode.equals("kn", ignoreCase = true) ||
            languageCode.equals("kannada", ignoreCase = true)
        if (!kn) return agentHtmlUrl?.takeIf { isUsable(it) } ?: englishUrl?.takeIf { isUsable(it) }

        val preferred = resolve(
            languageCode = "kn",
            englishUrl = englishUrl,
            kannadaUrl = kannadaUrl,
            allowEnglishFallback = false,
        )
        if (preferred != null) return preferred

        val agent = agentHtmlUrl?.takeIf { isUsable(it) }
        if (agent != null) {
            if (looksKannada(agent)) return agent
            toKannadaTwin(agent)?.let { return it }
            return agent
        }
        return englishUrl?.takeIf { isUsable(it) }
    }
}
