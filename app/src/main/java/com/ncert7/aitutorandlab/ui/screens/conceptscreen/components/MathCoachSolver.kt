package com.ncert7.aitutorandlab.ui.screens.conceptscreen.components

/**
 * The "brain" for the v3 Maths coach. Given the problem a math simulation shows on screen (prompt
 * text + answer options harvested as control labels), it works out the answer and produces
 * **number-specific worked feedback** — e.g. "the digit after the thousands place is 9, so
 * 3,87,69,957 rounds up to 3,87,70,000". This is what lifts Maths coaching from generic method
 * hints to a real tutor.
 *
 * Covers the chapter-1 math sims that are solvable from the visible problem (verified live against
 * the anuragmn/EduAI_app sims):
 *   ROUND   1_6, 1_9, 1_12   — place inferred from the gap between options (robust to wording)
 *   COMPARE 1_8              — unit-aware ("30 thousand" vs "3 lakh")
 *   RATIO   1_10             — a÷b, closest "N x" option
 *   COMMA   1_2              — correct Indian + International grouping card
 *   PATTERN 1_5              — next term of a "×10 + d" / geometric sequence
 *   SPEED   1_13             — distance ÷ days, better daily speed
 * plus stateful helpers for BUILD (1_4) and CALC (1_11), which also need the running total.
 *
 * The combinatorial sims (1_1 arrange A+B, 1_3 permutation vault, 1_7 card expression) stay
 * method-only. Pure Kotlin, no Android deps — unit-tested in MathCoachSolverTest.
 *
 * DOM extraction contract (verified live): problem in `.mission` (`p` + `.num`/`.pair .num`),
 * options in `.opts .opt` / `.choice` (raw value in `data-v`), confirm `#checkBtn`. Build/calc
 * expose "Target: X | Current: Y" in the mission text. Re-read after each confirm tap.
 */
object MathCoachSolver {

    enum class Kind { ROUND, COMPARE, RATIO, COMMA, PATTERN, SPEED, UNKNOWN }

    data class Solution(
        val kind: Kind,
        val answer: String?,               // canonical answer (option label / sign / number)
        val correctOptionLabel: String?,   // option to treat as correct / highlight as reteach
        val whyCorrect: String,
        val whyWrong: String,
    )

    /** A guided move for the stateful build/calc sims. */
    data class Move(val controlValue: String?, val why: String)

    // ---- number parsing -------------------------------------------------------------------------

    fun numbersIn(text: String): List<Long> =
        Regex("""\d[\d,]*""").findAll(text)
            .mapNotNull { it.value.replace(",", "").toLongOrNull() }
            .toList()

    private val UNITS = linkedMapOf(
        "thousand" to 1_000L, "lakh" to 1_00_000L, "lakhs" to 1_00_000L,
        "crore" to 1_00_00_000L, "crores" to 1_00_00_000L,
        "million" to 1_000_000L, "billion" to 1_000_000_000L,
    )

    /** Unit-aware values: "30 thousand" → 30000, "3 lakh" → 300000, "3,87,69,957" → 38769957. */
    fun valuesIn(text: String): List<Long> {
        val re = Regex("""(\d[\d,]*(?:\.\d+)?)\s*(thousand|lakhs?|crores?|million|billion)?""", RegexOption.IGNORE_CASE)
        return re.findAll(text).mapNotNull { m ->
            val base = m.groupValues[1].replace(",", "").toDoubleOrNull() ?: return@mapNotNull null
            val unit = m.groupValues[2].lowercase()
            val mult = if (unit.isEmpty()) 1L else UNITS[unit] ?: UNITS[unit.trimEnd('s')] ?: 1L
            Math.round(base * mult)
        }.toList()
    }

    /** Indian grouping: 12,34,567. */
    fun formatIndian(n: Long): String {
        val neg = n < 0; val s = kotlin.math.abs(n).toString()
        if (s.length <= 3) return (if (neg) "-" else "") + s
        val head = s.substring(0, s.length - 3); val tail = s.substring(s.length - 3)
        val g = StringBuilder(); var c = 0
        for (i in head.length - 1 downTo 0) { g.append(head[i]); c++; if (c % 2 == 0 && i != 0) g.append(',') }
        return (if (neg) "-" else "") + g.reverse().toString() + "," + tail
    }

    /** International grouping: 1,234,567. */
    fun formatIntl(n: Long): String {
        val neg = n < 0; val s = kotlin.math.abs(n).toString()
        val g = StringBuilder(); var c = 0
        for (i in s.length - 1 downTo 0) { g.append(s[i]); c++; if (c % 3 == 0 && i != 0) g.append(',') }
        return (if (neg) "-" else "") + g.reverse().toString()
    }

    private fun placeName(p: Long) = when {
        p == 1000L -> "thousand"; p == 100L -> "hundred"; p == 10L -> "ten"
        p == 100000L -> "lakh"; p == 1000000L -> "ten-lakh"; p == 10000000L -> "crore"
        else -> formatIndian(p)
    }

    // ---- MCQ solve ------------------------------------------------------------------------------

    /** Parse + solve a multiple-choice math problem. Null when it isn't a solvable archetype. */
    fun solve(prompt: String, options: List<String> = emptyList()): Solution? {
        val kind = detect(prompt, options)
        return when (kind) {
            Kind.ROUND -> solveRound(prompt, options)
            Kind.COMPARE -> solveCompare(prompt, options)
            Kind.RATIO -> solveRatio(prompt, options)
            Kind.COMMA -> solveComma(prompt, options)
            Kind.PATTERN -> solvePattern(prompt, options)
            Kind.SPEED -> solveSpeed(prompt, options)
            Kind.UNKNOWN -> null
        }
    }

    private fun detect(prompt: String, options: List<String>): Kind {
        val t = prompt.lowercase()
        val opt = options.map { it.lowercase() }
        return when {
            options.isNotEmpty() && options.all { it.trim() in setOf("<", "=", ">") } -> Kind.COMPARE
            opt.isNotEmpty() && opt.all { Regex("""^\d+(\.\d+)?\s*x$""").containsMatchIn(it.trim()) } -> Kind.RATIO
            opt.any { "km/day" in it } || "daily speed" in t -> Kind.SPEED
            opt.any { "indian" in it || "international" in it } || "comma" in t -> Kind.COMMA
            "round" in t || "nearest" in t -> Kind.ROUND
            "pattern" in t || "next product" in t || "next term" in t -> Kind.PATTERN
            "compare" in t || "bigger" in t || "greater" in t -> Kind.COMPARE
            else -> Kind.UNKNOWN
        }
    }

    private fun solveRound(prompt: String, options: List<String>): Solution? {
        val optVals = options.mapNotNull { numbersIn(it).firstOrNull() }.sorted()
        // Infer the place scale from the gap between options — robust to "nearest 1,000" / "ten lakh".
        val place = optVals.zipWithNext { a, b -> b - a }.filter { it > 0 }.minOrNull()
            ?: Regex("""nearest\s+([\d,]+)""", RegexOption.IGNORE_CASE)
                .find(prompt)?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull()
            ?: return null
        val n = numbersIn(prompt).filter { it != place && it !in optVals }.maxOrNull() ?: return null
        val ans = Math.round(n.toDouble() / place) * place
        val after = (n / (place / 10)) % 10
        val dir = if (after >= 5) "up" else "down"
        val correct = options.firstOrNull { numbersIn(it).firstOrNull() == ans } ?: formatIndian(ans)
        return Solution(
            Kind.ROUND, formatIndian(ans), correct,
            "Right — the digit after the ${placeName(place)}s place is $after, so ${formatIndian(n)} rounds $dir to ${formatIndian(ans)}.",
            "Check the digit after the ${placeName(place)}s place: it's $after. " +
                (if (after >= 5) "5 or more rounds up" else "Less than 5 rounds down") +
                " — so ${formatIndian(n)} rounds to ${formatIndian(ans)}.",
        )
    }

    private fun solveCompare(prompt: String, options: List<String>): Solution? {
        val v = valuesIn(prompt)
        val a = v.getOrNull(0) ?: return null
        val b = v.getOrNull(1) ?: return null
        val sign = if (a < b) "<" else if (a > b) ">" else "="
        val da = a.toString().length; val db = b.toString().length
        val reason = if (da != db) "${formatIndian(if (a > b) a else b)} has more digits, so it's bigger"
        else "they have the same number of digits, so compare from the leftmost digit"
        return Solution(
            Kind.COMPARE, sign, sign,
            "Correct — ${formatIndian(a)} $sign ${formatIndian(b)}. ${reason.replaceFirstChar { it.uppercase() }}.",
            "Look again: ${formatIndian(a)} has $da digits, ${formatIndian(b)} has $db. " +
                "${reason.replaceFirstChar { it.uppercase() }}, so it's ${formatIndian(a)} $sign ${formatIndian(b)}.",
        )
    }

    private fun solveRatio(prompt: String, options: List<String>): Solution? {
        val v = valuesIn(prompt)
        val a = v.getOrNull(0) ?: return null
        val b = v.getOrNull(1)?.takeIf { it != 0L } ?: return null
        val r = a.toDouble() / b
        val best = options.minByOrNull { opt ->
            val ov = opt.trim().removeSuffix("x").removeSuffix("X").toDoubleOrNull() ?: Double.MAX_VALUE
            kotlin.math.abs(ov - r)
        } ?: return null
        val r1 = String.format("%.1f", r)
        return Solution(
            Kind.RATIO, best, best,
            "Right — ${formatIndian(a)} ÷ ${formatIndian(b)} ≈ $r1, so it's about $best.",
            "Divide the bigger by the smaller: ${formatIndian(a)} ÷ ${formatIndian(b)} ≈ $r1 — the closest is $best.",
        )
    }

    private fun solveComma(prompt: String, options: List<String>): Solution? {
        val n = numbersIn(prompt).maxByOrNull { it.toString().length } ?: return null
        val ind = formatIndian(n); val intl = formatIntl(n)
        // Must match Indian grouping in the "Indian …" slot AND International in the "International …"
        // slot — a card with them swapped contains both strings but is WRONG (real-sim bug).
        val re = Regex("""indian\s+([\d,]+)\s+international\s+([\d,]+)""", RegexOption.IGNORE_CASE)
        val correct = options.firstOrNull { opt ->
            val m = re.find(opt) ?: return@firstOrNull false
            m.groupValues[1].trim() == ind && m.groupValues[2].trim() == intl
        }
        return Solution(
            Kind.COMMA, "$ind / $intl", correct,
            "Correct — Indian $ind (first 3 digits, then every 2) and International $intl (every 3).",
            "For $n: Indian is $ind (first 3 from the right, then every 2); International is $intl (every 3). Pick the card with both.",
        )
    }

    private fun solvePattern(prompt: String, options: List<String>): Solution? {
        val all = numbersIn(prompt).filter { it > 0 }
        if (all.size < 2) return null
        val optVals = options.mapNotNull { numbersIn(it).firstOrNull() }
        // A lead-in like "7 multiplied by 1, 11, 111" or "Each term is multiplied by 2." prepends
        // stray numbers that corrupt the rule. Try progressively shorter TRAILING runs and take the
        // first whose rule lands on one of the options — that discards the lead-in automatically.
        for (start in 0..(all.size - 2)) {
            val seq = all.subList(start, all.size)
            if (seq.size < 2) break
            val hit = patternCandidates(seq).firstOrNull { it.first in optVals } ?: continue
            val next = hit.first
            val why = "${hit.second}, so the next is $next"
            val correct = options.firstOrNull { numbersIn(it).firstOrNull() == next }
            return Solution(
                Kind.PATTERN, next.toString(), correct,
                "Right — $why.",
                "Spot the rule: $why.",
            )
        }
        return null
    }

    /** Arithmetic / geometric / repunit(×10+d) rules that are consistent across the whole sequence. */
    private fun basicPatternRules(seq: List<Long>): List<Pair<Long, String>> {
        if (seq.size < 2) return emptyList()
        val d0 = seq[0]
        val ratio = seq[1].toDouble() / seq[0]
        val diff = seq[1] - seq[0]
        return buildList {
            if ((1 until seq.size).all { seq[it] == seq[it - 1] * 10 + d0 }) {
                add((seq.last() * 10 + d0) to "each term is the previous ×10 + $d0")
            }
            if (seq[0] != 0L && ratio == Math.floor(ratio) &&
                (1 until seq.size).all { seq[it].toDouble() == seq[it - 1] * ratio }
            ) {
                add(Math.round(seq.last() * ratio) to "each term multiplies by ${ratio.toLong()}")
            }
            if ((1 until seq.size).all { seq[it] - seq[it - 1] == diff }) {
                add((seq.last() + diff) to "each term adds $diff")
            }
        }
    }

    /** Basic rules plus a "squares" rule (81, 9801, 998001 → roots 9, 99, 999 follow a sub-rule). */
    private fun patternCandidates(seq: List<Long>): List<Pair<Long, String>> {
        val out = basicPatternRules(seq).toMutableList()
        val roots = seq.map { Math.round(Math.sqrt(it.toDouble())) }
        if (roots.size >= 2 && roots.zip(seq).all { (r, n) -> r * r == n }) {
            basicPatternRules(roots).firstOrNull()?.let { rc ->
                out.add((rc.first * rc.first) to "each term is a square (roots ${rc.second})")
            }
        }
        return out
    }

    private fun solveSpeed(prompt: String, options: List<String>): Solution? {
        val nums = numbersIn(prompt)
        val dist = nums.getOrNull(0) ?: return null
        val days = nums.getOrNull(1)?.takeIf { it != 0L } ?: return null
        val need = dist.toDouble() / days
        val best = options.minByOrNull { opt ->
            val v = numbersIn(opt).firstOrNull()?.toDouble() ?: Double.MAX_VALUE
            kotlin.math.abs(v - need)
        } ?: return null
        return Solution(
            Kind.SPEED, best, best,
            "Right — ${formatIndian(dist)} ÷ $days ≈ ${Math.round(need)} km/day, so $best is the better choice.",
            "Work out the daily need: ${formatIndian(dist)} ÷ $days ≈ ${Math.round(need)} km/day — so pick $best.",
        )
    }

    // ---- stateful helpers (need the live running total from the sim) -----------------------------

    /** BUILD (1_4): which place-value button to tap next toward [target] from [current]. */
    fun solveBuild(target: Long, current: Long, buttonValues: List<Long>): Move {
        if (current == target) return Move(null, "You've matched the target — tap Lock Build.")
        if (current > target) return Move(null, "That's over the target — tap Reset and use smaller buttons.")
        val fit = buttonValues.sortedDescending().firstOrNull { current + it <= target }
        return Move(
            fit?.toString(),
            if (fit != null) "Add the biggest button that still fits: +${formatIndian(fit)} (you're at ${formatIndian(current)} of ${formatIndian(target)})."
            else "Tap Reset and pick the biggest button that fits.",
        )
    }

    /** CALC (1_11): remaining taps of the single [step] button to reach [target] from [current]. */
    fun solveCalc(target: Long, current: Long, step: Long): Move {
        if (step <= 0) return Move(null, "")
        val remaining = ((target - current) / step)
        return Move(
            if (current < target) step.toString() else null,
            "Target ÷ $step = ${target / step} taps in all — you're at ${formatIndian(current)}, ${remaining.coerceAtLeast(0)} taps to go.",
        )
    }
}
