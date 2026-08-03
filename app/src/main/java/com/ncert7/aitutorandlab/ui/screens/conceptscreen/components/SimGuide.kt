package com.ncert7.aitutorandlab.ui.screens.conceptscreen.components

import com.ncert7.aitutorandlab.debug.DebugLogger
import org.json.JSONObject

/**
 * One coach step: what to say, which control to highlight (null = just observe), and whether the
 * step advances on its own when the learner taps that control. Sliders and text inputs can't be
 * "tapped once" to complete, so they set [autoAdvance] = false and are advanced with a Next button.
 */
data class SimGuideStep(
    val instruction: String,
    val targetIndex: Int?,
    val autoAdvance: Boolean = true,
)

/** A pre-authored, hosted guide for a simulation (one per sim, model-generated + reviewed). */
data class SimGuideDoc(
    val simId: String,
    val lang: String,
    val steps: List<GuideStepDoc>,
)

/** A hosted step: the instruction, and the label of the control it points at (null = observe). */
data class GuideStepDoc(
    val text: String,
    val target: String?,
)

private data class HarvestedControl(
    val index: Int,
    val label: String,
    val tag: String,
    val type: String = "",
) {
    val isSlider: Boolean get() = type.equals("range", ignoreCase = true)
    val isTextInput: Boolean
        get() = type.lowercase() in setOf("text", "number", "tel", "search", "email") ||
            tag.equals("textarea", ignoreCase = true)
    val needsNext: Boolean get() = isSlider || isTextInput
}

/**
 * Turns the harvested control structure (from the injected DOM harvester) into a short,
 * generic guided walkthrough — no per-simulation authoring. Heuristic: recognise the
 * common "pick an option → trigger an action → observe" pattern; otherwise walk the
 * controls in reading order. English pilot.
 */
object SimGuideBuilder {

    /** Always the final thing the coach says — combines the well-done + explore-more nudge. */
    const val EXPLORE_MORE_CLOSING =
        "Great work — you've explored this experiment! While these are just examples, " +
            "I'd recommend exploring more on your own — try the other options and see what happens!"

    /** Builds a control step with the right verb and advance behaviour for its type. */
    private fun stepFor(c: HarvestedControl, lead: String = "Tap"): SimGuideStep =
        when {
            c.isSlider ->
                SimGuideStep("Drag the “${c.label}” slider, then tap Next.", c.index, autoAdvance = false)
            c.isTextInput ->
                SimGuideStep("Type a value into “${c.label}”, then tap Next.", c.index, autoAdvance = false)
            else ->
                SimGuideStep("$lead “${c.label}”.", c.index, autoAdvance = true)
        }

    /** An "observe" step — names a specific readout when the page exposed one. */
    private fun observeStep(readouts: List<String>): SimGuideStep {
        val what = readouts.firstOrNull()
        val text =
            if (what != null) "Now watch the “$what” closely and notice how it changes."
            else "Now watch the screen closely and notice what changes."
        return SimGuideStep(text, null)
    }

    /**
     * Caps the walkthrough to [max] steps and guarantees the last one is the
     * "explore more on your own" nudge, without duplicating it if already present.
     */
    fun finalizeSteps(steps: List<SimGuideStep>, max: Int): List<SimGuideStep> {
        val closing = SimGuideStep(EXPLORE_MORE_CLOSING, null)
        val body = steps.filter { it.instruction.trim() != EXPLORE_MORE_CLOSING }
        val capped = body.take((max - 1).coerceAtLeast(0)).toMutableList()
        capped += closing
        return capped
    }

    // Verbs that mark a control as the *trigger* that produces a result (as opposed to an option
    // you first select). Crucial for "select an option → then act → observe" simulations such as
    // the litmus test, where picking "Vinegar" does nothing until you "Dip" the strip.
    private val ACTION_HINTS = listOf(
        "mix", "test", "dip", "immerse", "soak", "apply", "start", "run", "go", "play",
        "check", "submit", "reveal", "drop", "pour", "add", "combine", "reset", "next",
        "measure", "observe", "stir", "swirl", "shake", "heat", "cool", "boil", "freeze",
        "connect", "launch", "release", "light", "flip", "spin", "press", "scan", "calculate",
        "solve",
    )

    fun build(structureJson: String): List<SimGuideStep> {
        val controls = parse(structureJson)
        if (controls.isEmpty()) return emptyList()
        val readouts = parseReadouts(structureJson)

        val steps = mutableListOf<SimGuideStep>()

        val (actions, options) = controls.partition { c ->
            val l = c.label.lowercase()
            ACTION_HINTS.any { l.contains(it) }
        }

        // Straight into the highlights — no generic "let me walk you through" preamble.
        if (options.size >= 2 && actions.isNotEmpty() && !options.first().needsNext) {
            // Classic select → act → observe loop.
            steps += stepFor(options.first(), lead = "Pick something to test — tap")
            steps += stepFor(actions.first(), lead = "Now tap")
            steps += observeStep(readouts)
            steps += SimGuideStep("Now try another option and compare the result.", null)
        } else {
            // Fallback: walk each control in order (capped so it stays short).
            controls.take(5).forEach { c -> steps += stepFor(c) }
            steps += observeStep(readouts)
        }

        // The combined "great work + explore more" closing is added by finalizeSteps().
        return steps
    }

    /**
     * Builds the coach steps from a hosted guide, mapping each step's target *label* to a
     * live control's data-edu-step index (so highlight + tap-gating work against the real
     * DOM). Steps whose target isn't found become plain "observe" steps rather than gating
     * on a missing element.
     */
    fun buildFromDoc(doc: SimGuideDoc, structureJson: String): List<SimGuideStep> {
        val controls = parse(structureJson)
        return doc.steps
            .filter { it.text.isNotBlank() }
            .map { s ->
                val control = s.target?.takeIf { it.isNotBlank() }?.let { matchControl(controls, it) }
                SimGuideStep(
                    instruction = s.text.trim(),
                    targetIndex = control?.index,
                    autoAdvance = control == null || !control.needsNext,
                )
            }
    }

    /** Parse a hosted guide JSON document. */
    fun parseDoc(json: String): SimGuideDoc? =
        try {
            val o = JSONObject(json)
            val arr = o.optJSONArray("steps") ?: return null
            val steps = (0 until arr.length()).mapNotNull { i ->
                val so = arr.optJSONObject(i) ?: return@mapNotNull null
                val text = so.optString("text").trim()
                if (text.isEmpty()) null
                else GuideStepDoc(
                    text = text,
                    target = so.optString("target").trim().ifEmpty { null },
                )
            }
            if (steps.isEmpty()) null
            else SimGuideDoc(
                simId = o.optString("simId"),
                lang = o.optString("lang", "en"),
                steps = steps,
            )
        } catch (e: Exception) {
            DebugLogger.errorLog("SimGuide", "parseDoc failed: ${e.message}")
            null
        }

    private fun matchControl(controls: List<HarvestedControl>, target: String): HarvestedControl? {
        val t = target.trim().lowercase()
        return controls.firstOrNull { it.label.trim().lowercase() == t }
            ?: controls.firstOrNull {
                val l = it.label.lowercase()
                l.contains(t) || t.contains(l)
            }
    }

    /** Value/result labels the page exposes (e.g. "Speed", "Distance") for observe steps. */
    private fun parseReadouts(structureJson: String): List<String> =
        try {
            val arr = JSONObject(structureJson).optJSONArray("readouts") ?: return emptyList()
            (0 until arr.length())
                .mapNotNull { arr.optString(it).trim().ifEmpty { null } }
                .distinctBy { it.lowercase() }
                .take(3)
        } catch (e: Exception) {
            emptyList()
        }

    private fun parse(structureJson: String): List<HarvestedControl> =
        try {
            val arr = JSONObject(structureJson).optJSONArray("controls") ?: return emptyList()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val label = o.optString("label").trim()
                if (label.isEmpty()) null
                else HarvestedControl(
                    index = o.optInt("index", i),
                    label = label,
                    tag = o.optString("tag"),
                    type = o.optString("type"),
                )
            }.distinctBy { it.label.lowercase() }
        } catch (e: Exception) {
            DebugLogger.errorLog("SimGuide", "parse failed: ${e.message}")
            emptyList()
        }
}
