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

/**
 * Which coaching style the guided-sim coach uses. Selectable at runtime (per the header selector)
 * so the three can be compared side by side on the same simulation:
 *  - [SCRIPTED] (v1): the original linear walkthrough — every step, start to finish, then done.
 *  - [ADAPTIVE] (v2): a brief intro walkthrough, then hands-off; coaches only on wrong/stuck and
 *    eases off after enough interactions/time.
 *  - [GUIDED] (v3): fully guided to the end — walks every step, then keeps suggesting and
 *    highlighting the next action round after round, reacting to wrong answers AND to off-path
 *    detours, until the mission's rounds are done or every control has been explored.
 */
enum class SimCoachMode(val short: String, val label: String, val blurb: String) {
    SCRIPTED("V1", "Scripted walkthrough", "Original step-by-step guide, start to finish."),
    ADAPTIVE("V2", "Brief intro, then adaptive", "Short intro, then hands-off — helps on wrong/stuck, then eases off."),
    GUIDED("V3", "Fully guided", "Guides every round to the end, reacting to wrong answers and detours."),
    ONE_CLOCK("V4", "One-clock coach", "Page-side loop owns glow + text + voice on one clock — never out of sync."),
    ;

    companion object {
        // v4 is the one-clock coach: a single page-side loop reads each round and renders glow + text
        // + voice together (Kotlin is a passive mirror), so nothing can drift out of sync. Default to
        // it; V1/V2/V3 remain selectable for comparison.
        val DEFAULT = ONE_CLOCK
        fun fromKey(key: String?): SimCoachMode =
            values().firstOrNull { it.name == key } ?: DEFAULT
    }
}

/**
 * How the coach delivers help within the one-clock coach. Student-switchable, fixed (no adaptivity).
 * The `id` is what the page-side engine reads via window.__eduHintMode.
 *  - [ASK]      Try-first: state the problem, wait; Hint gives a nudge, then the answer.
 *  - [GUIDED]   Step-by-step: reveal the next move + glow right away.
 *  - [SELF]     Self-explain: pose the reasoning first; reveal on request.
 *  - [ONDEMAND] Answer-on-tap: stay quiet; one Show-answer reveals.
 */
enum class HintMode(val id: String, val label: String) {
    ASK("ask", "Try first"),
    GUIDED("guided", "Step-by-step"),
    SELF("self", "Self-explain"),
    ONDEMAND("ondemand", "Answer on tap"),
    ;

    companion object {
        val DEFAULT = ASK
        fun fromId(id: String?): HintMode = values().firstOrNull { it.id == id } ?: DEFAULT
    }
}

/** A pre-authored, hosted guide for a simulation (one per sim, model-generated + reviewed). */
data class SimGuideDoc(
    val simId: String,
    val lang: String,
    val steps: List<GuideStepDoc>,
    val coach: SimCoachData? = null,
    val practice: SimPracticeDoc? = null,
)

/**
 * Authored, repeatable practice rounds for v3 on scoring/puzzle sims (Math especially). After the
 * lesson is taught, the coach runs [rounds] rounds of this short guided sequence: it walks the
 * [steps] (highlighting each control, gating on the tap, auto-advancing pure guidance), then the
 * learner's Check/Submit produces a verdict. On a correct verdict it says an [onCorrect] line
 * (the observation/inference) and starts the next round; on a wrong one it says [onWrong] (why +
 * how to fix) and lets them retry. After [rounds] wins it plays [done].
 */
data class SimPracticeDoc(
    val rounds: Int,
    val steps: List<GuideStepDoc>,
    val onCorrect: List<String>,
    val onWrong: List<String>,
    val done: String?,
)

/**
 * Authored per-simulation coaching for the "fully guided" adaptive mode. After a brief intro
 * walkthrough (the first few [SimGuideDoc.steps]) the coach goes hands-off: the learner tries
 * things freely, and the coach only speaks up to (a) explain *why* an answer was wrong,
 * (b) nudge when the learner is stuck, or (c) celebrate a correct answer. It eases off once the
 * learner has done enough — [easeOffAfterInteractions] taps or [easeOffAfterSeconds] seconds of
 * active play (a proxy for finishing ~75% of the mission).
 *
 * All banks are cycled through in order and then repeat, so authoring 2–4 lines each is plenty.
 * When a sim ships no `coach` block, [SimCoachData.default] fills in generic-but-usable copy.
 */
data class SimCoachData(
    val mission: String?,
    val whenStuck: List<String>,
    val whenWrong: List<String>,
    val whenCorrect: List<String>,
    val doneMessage: String,
    /** Gentle redirects (v3) when the learner taps something other than the suggested control. */
    val whenDeviate: List<String> = emptyList(),
    /**
     * Per-element inference lines for v3 exploration, keyed by (lowercased) control label. Lets the
     * coach say the actual result for EACH option — "Vinegar is an acid — it turns blue litmus red."
     * — not just the two the guide scripts. Matched by substring so "lemon" hits "Lemon Juice".
     */
    val elements: Map<String, String> = emptyMap(),
    val introSteps: Int = 2,
    val stuckAfterSeconds: Int = 22,
    val easeOffAfterInteractions: Int = 8,
    val easeOffAfterSeconds: Int = 180,
    /** v3 only: how many correct rounds count as "done" when the sim reports verdicts. */
    val roundsToComplete: Int = 5,
) {
    fun stuckLine(i: Int): String? = whenStuck.cycle(i)
    fun wrongLine(i: Int): String? = whenWrong.cycle(i)
    fun correctLine(i: Int): String? = whenCorrect.cycle(i)
    fun deviateLine(i: Int): String? = whenDeviate.cycle(i)

    /** The authored inference for an option's label, matched leniently (either contains the other). */
    fun inferenceFor(label: String): String? {
        if (elements.isEmpty() || label.isBlank()) return null
        val l = label.trim().lowercase()
        elements[l]?.let { return it }
        // Longest keys first so "baking soda" wins over "soda".
        return elements.entries
            .sortedByDescending { it.key.length }
            .firstOrNull { (k, _) -> l.contains(k) || k.contains(l) }
            ?.value
    }

    private fun List<String>.cycle(i: Int): String? =
        if (isEmpty()) null else this[((i % size) + size) % size]

    companion object {
        /** Generic coach used when a sim ships no authored `coach` block. */
        fun default(mission: String?): SimCoachData = SimCoachData(
            mission = mission,
            whenStuck = listOf(
                "Give it a try — tap one of the controls and watch what happens.",
                "Not sure where to start? Try the highlighted control, then check the result.",
                "Take your best guess — you can always try again.",
            ),
            whenWrong = listOf(
                "Not quite — take another look and try again.",
                "Close! Think about what the question is really asking, then give it another go.",
            ),
            whenCorrect = listOf(
                "Nice — that's right! Keep going.",
                "Great work! Try the next one.",
                "You've got it. See if you can do it again.",
            ),
            whenDeviate = listOf(
                "Nice curiosity! For this round, try the glowing control.",
                "Good to explore — when you're ready, tap the highlighted one to continue.",
            ),
            doneMessage = "You've got the hang of this! Keep exploring on your own, " +
                "or move on whenever you're ready.",
        )
    }
}

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
 * A single interactive control the page exposed, classified so the v3 tutor can guide a
 * select → act → observe loop over *every* element (e.g. try each solution, then dip):
 *  - [isAction] controls trigger a result ("Dip", "Add", "Mix", "Check"…).
 *  - the rest are options you pick first (lemon, soap, water…).
 */
data class SimControl(
    val index: Int,
    val label: String,
    val isAction: Boolean,
    val needsNext: Boolean,
)

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
    fun buildFromDoc(doc: SimGuideDoc, structureJson: String): List<SimGuideStep> =
        buildSteps(doc.steps, structureJson)

    /**
     * Maps a list of authored [GuideStepDoc]s to live [SimGuideStep]s (label → data-edu-step index),
     * so highlight + tap-gating work against the real DOM. Shared by the scripted guide and the
     * repeatable practice rounds.
     */
    fun buildSteps(steps: List<GuideStepDoc>, structureJson: String): List<SimGuideStep> {
        val controls = parse(structureJson)
        return steps
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

    /**
     * All interactive controls the page harvested, classified into options vs. action triggers.
     * Powers the v3 "cover every element" exploration after the scripted lesson.
     */
    fun harvestedControls(structureJson: String): List<SimControl> =
        parse(structureJson).map { c ->
            val l = c.label.lowercase()
            SimControl(
                index = c.index,
                label = c.label,
                isAction = ACTION_HINTS.any { l.contains(it) },
                needsNext = c.needsNext,
            )
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
                coach = parseCoach(o.optJSONObject("coach")),
                practice = parsePractice(o.optJSONObject("practice")),
            )
        } catch (e: Exception) {
            DebugLogger.errorLog("SimGuide", "parseDoc failed: ${e.message}")
            null
        }

    private fun parsePractice(o: JSONObject?): SimPracticeDoc? {
        if (o == null) return null
        val arr = o.optJSONArray("steps") ?: return null
        val steps = (0 until arr.length()).mapNotNull { i ->
            val so = arr.optJSONObject(i) ?: return@mapNotNull null
            val text = so.optString("text").trim()
            if (text.isEmpty()) null
            else GuideStepDoc(text = text, target = so.optString("target").trim().ifEmpty { null })
        }
        if (steps.isEmpty()) return null
        fun bank(key: String): List<String> {
            val a = o.optJSONArray(key) ?: return emptyList()
            return (0 until a.length()).mapNotNull { a.optString(it).trim().ifEmpty { null } }
        }
        return SimPracticeDoc(
            rounds = o.optInt("rounds", 3).coerceIn(1, 20),
            steps = steps,
            onCorrect = bank("onCorrect"),
            onWrong = bank("onWrong"),
            done = o.optString("done").trim().ifEmpty { null },
        )
    }

    private fun parseCoach(o: JSONObject?): SimCoachData? {
        if (o == null) return null
        fun bank(key: String): List<String> {
            val arr = o.optJSONArray(key) ?: return emptyList()
            return (0 until arr.length())
                .mapNotNull { arr.optString(it).trim().ifEmpty { null } }
        }
        val elements = o.optJSONObject("elements")?.let { eo ->
            buildMap {
                eo.keys().forEach { k ->
                    val v = eo.optString(k).trim()
                    if (k.isNotBlank() && v.isNotEmpty()) put(k.trim().lowercase(), v)
                }
            }
        } ?: emptyMap()
        val base = SimCoachData.default(o.optString("mission").trim().ifEmpty { null })
        return base.copy(
            mission = o.optString("mission").trim().ifEmpty { base.mission },
            whenStuck = bank("whenStuck").ifEmpty { base.whenStuck },
            whenWrong = bank("whenWrong").ifEmpty { base.whenWrong },
            whenCorrect = bank("whenCorrect").ifEmpty { base.whenCorrect },
            whenDeviate = bank("whenDeviate").ifEmpty { base.whenDeviate },
            elements = elements,
            doneMessage = o.optString("done").trim().ifEmpty { base.doneMessage },
            introSteps = o.optInt("introSteps", base.introSteps).coerceAtLeast(0),
            stuckAfterSeconds = o.optInt("stuckAfterSeconds", base.stuckAfterSeconds).coerceAtLeast(6),
            easeOffAfterInteractions =
                o.optInt("easeOffAfterInteractions", base.easeOffAfterInteractions).coerceAtLeast(1),
            easeOffAfterSeconds =
                o.optInt("easeOffAfterSeconds", base.easeOffAfterSeconds).coerceAtLeast(30),
            roundsToComplete = o.optInt("roundsToComplete", base.roundsToComplete).coerceAtLeast(1),
        )
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
