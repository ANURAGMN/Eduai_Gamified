package com.ncert7.aitutorandlab.ui.screens.conceptscreen.components

import android.content.Context
import com.ncert7.aitutorandlab.debug.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Loads a pre-authored guide for a simulation.
 *
 * Resolution order:
 *   1. Bundled asset  — `assets/sim_guides/{simFile}.guide.json` (used for local testing now).
 *   2. Hosted JSON    — next to the sim: `.../{simFile}.html` → `.../{simFile}.guide.json`.
 *   3. null           — no guide; the coach falls back to the harvested heuristic.
 *
 * Cached in memory per sim URL.
 */
object SimGuideRepository {

    private val cache = mutableMapOf<String, SimGuideDoc?>()

    suspend fun fetchGuide(context: Context, simUrl: String): SimGuideDoc? {
        synchronized(cache) { if (cache.containsKey(simUrl)) return cache[simUrl] }
        val appContext = context.applicationContext
        val doc = withContext(Dispatchers.IO) {
            loadFromAssets(appContext, simUrl) ?: loadFromNetwork(simUrl)
        }
        synchronized(cache) { cache[simUrl] = doc }
        return doc
    }

    private fun loadFromAssets(context: Context, simUrl: String): SimGuideDoc? {
        val name = fileNameFor(simUrl) ?: return null
        return try {
            val text = context.assets.open("sim_guides/$name").bufferedReader().use { it.readText() }
            DebugLogger.debugLog("SimGuideRepository", "loaded local guide asset: $name")
            SimGuideBuilder.parseDoc(text)
        } catch (e: Exception) {
            null // not bundled — fall through to network
        }
    }

    private fun loadFromNetwork(simUrl: String): SimGuideDoc? {
        val guideUrl = guideUrlFor(simUrl) ?: return null
        return try {
            SimGuideBuilder.parseDoc(URL(guideUrl).readText())
        } catch (e: Exception) {
            DebugLogger.debugLog("SimGuideRepository", "no hosted guide at $guideUrl (${e.message})")
            null
        }
    }

    /** `.../science_2_6.html` → `science_2_6.guide.json` */
    private fun fileNameFor(simUrl: String): String? {
        val file = simUrl.substringBefore('#').substringBefore('?').substringAfterLast('/').trim()
        if (!file.endsWith(".html", ignoreCase = true)) return null
        return file.dropLast(".html".length) + ".guide.json"
    }

    private fun guideUrlFor(simUrl: String): String? {
        val base = simUrl.substringBefore('#').substringBefore('?').trim()
        if (!base.endsWith(".html", ignoreCase = true)) return null
        return base.dropLast(".html".length) + ".guide.json"
    }
}
