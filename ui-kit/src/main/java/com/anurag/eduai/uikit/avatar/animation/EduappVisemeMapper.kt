package com.anurag.eduai.uikit.avatar.animation

import com.anurag.eduai.uikit.avatar.core.MouthShape
import com.anurag.eduai.uikit.avatar.core.Viseme

/**
 * Ported from Eduapp LipSync.html — English G2P + Kannada syllable viseme mapping.
 */
object EduappVisemeMapper {

    private enum class Script { Latin, Kannada, Other }

    private val kannadaRegex = Regex("[\u0C80-\u0CFF]")
    private val devanagariRegex = Regex("[\u0900-\u097F]")
    private val tamilRegex = Regex("[\u0B80-\u0BFF]")
    private val teluguRegex = Regex("[\u0C00-\u0C7F]")

    data class VisemeFrame(val viseme: Viseme, val durationMs: Long)

    /**
     * A viseme timeline plus the native-time offset (ms from the start) at which each
     * whitespace-delimited word begins. Word offsets let the lip-sync controller re-anchor
     * the timeline to real TTS word-boundary callbacks.
     */
    data class VisemeTimeline(
        val frames: List<VisemeFrame>,
        val wordStartMs: List<Long>
    )

    fun visemeTimelineForText(text: String, speechRate: Float = 0.75f): List<VisemeFrame> =
        visemeTimelineWithWords(text, speechRate).frames

    fun visemeTimelineWithWords(text: String, speechRate: Float = 0.75f): VisemeTimeline {
        val speedMultiplier = 1f / speechRate.coerceIn(0.5f, 1.5f)
        val script = detectScript(text)
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        val frames = mutableListOf<VisemeFrame>()
        val wordStartMs = mutableListOf<Long>()
        var cumMs = 0L
        words.forEachIndexed { index, word ->
            // Offset (native ms, pre-compression cumulative) where this word begins. Merging
            // same-viseme frames in compressConsecutive never shifts these cumulative offsets.
            wordStartMs.add(cumMs)
            val wordFrames =
                when (script) {
                    Script.Latin -> framesForEnglishWord(word, speedMultiplier)
                    Script.Kannada -> framesForKannadaWord(word, speedMultiplier)
                    Script.Other -> framesForIndicFallbackWord(word, speedMultiplier)
                }
            frames.addAll(wordFrames)
            cumMs += wordFrames.sumOf { it.durationMs }
            // Brief closed mouth between words — avoids a permanently open jaw.
            if (index < words.lastIndex) {
                val d = (70 * speedMultiplier).toLong()
                frames.add(VisemeFrame(Viseme.Closed, d))
                cumMs += d
            }
        }
        return VisemeTimeline(compressConsecutive(frames), wordStartMs)
    }

    private fun detectScript(text: String): Script =
        when {
            kannadaRegex.containsMatchIn(text) -> Script.Kannada
            devanagariRegex.containsMatchIn(text) ||
                tamilRegex.containsMatchIn(text) ||
                teluguRegex.containsMatchIn(text) -> Script.Other
            else -> Script.Latin
        }

    private fun framesForEnglishWord(word: String, speedMultiplier: Float): List<VisemeFrame> =
        englishG2P(word).map { token ->
            val (viseme, duration) = englishPhonemeToViseme(token)
            VisemeFrame(viseme, (duration * speedMultiplier).toLong())
        }

    private fun framesForKannadaWord(word: String, speedMultiplier: Float): List<VisemeFrame> =
        parseKannadaSyllables(word).map { syllable ->
            val (viseme, duration) = kannadaSyllableToViseme(syllable)
            VisemeFrame(viseme, (duration * speedMultiplier).toLong())
        }

    /** Per-char fallback for other Indic scripts (same approach as LipSync.html). */
    private fun framesForIndicFallbackWord(word: String, speedMultiplier: Float): List<VisemeFrame> =
        word.filterNot { it.isWhitespace() }.map { ch ->
            val chStr = ch.toString()
            val (viseme, duration) =
                when {
                    chStr.contains(Regex("[\u0CBE\u0CCC]")) -> Viseme.Open to 500
                    chStr.contains(Regex("[ಪಬಮ]")) -> Viseme.Closed to 280
                    else -> Viseme.Rest to 200
                }
            VisemeFrame(viseme, (duration * speedMultiplier).toLong())
        }

    /** Groups Kannada consonants + vowel signs into syllables (ported from LipSync.html). */
    private fun parseKannadaSyllables(text: String): List<String> {
        val syllables = mutableListOf<String>()
        var current = StringBuilder()
        for (ch in text) {
            val code = ch.code
            val isConsonant = code in 0x0C95..0x0CB9
            val isKannadaStart = code in 0x0C80..0x0CFF && !ch.isWhitespace() && current.isEmpty()
            if (isConsonant || isKannadaStart) {
                if (current.isNotEmpty()) {
                    syllables.add(current.toString())
                    current = StringBuilder()
                }
                current.append(ch)
            } else if (code in 0x0CBC..0x0CCC) {
                current.append(ch)
            } else if (ch.isWhitespace()) {
                if (current.isNotEmpty()) {
                    syllables.add(current.toString())
                    current = StringBuilder()
                }
            } else {
                current.append(ch)
            }
        }
        if (current.isNotEmpty()) syllables.add(current.toString())
        return syllables
    }

    /** Maps a Kannada syllable to viseme + duration (durations tuned for kn-IN TTS rate). */
    private fun kannadaSyllableToViseme(syllable: String): Pair<Viseme, Int> {
        if (syllable.isEmpty()) return Viseme.Rest to 200

        val independentVowels = "ಅಆಇಈಉಊಋಎಏಐಒಓಔ"
        val rounded = "ಉಊಒಓಔ"
        val front = "ಇಈಎಏಐ"
        val open = "ಅಆ"

        if (syllable[0] in independentVowels) {
            return when {
                syllable[0] in rounded -> Viseme.Round to 440
                syllable[0] in front -> Viseme.Wide to 400
                syllable[0] in open -> Viseme.Open to 480
                else -> Viseme.Open to 440
            }
        }

        val hasOpenVowelSign = syllable.contains(Regex("[\u0CBE\u0CCC]"))
        val hasRoundVowelSign = syllable.contains(Regex("[\u0CC1\u0CC2\u0CC6\u0CC7\u0CC8\u0CCB\u0CCC]"))
        val hasWideVowelSign = syllable.contains(Regex("[\u0CBF\u0CC0\u0CC3\u0CC4]"))

        return when {
            hasOpenVowelSign -> Viseme.Open to 500
            hasRoundVowelSign -> Viseme.Round to 400
            hasWideVowelSign -> Viseme.Wide to 380
            syllable.contains(Regex("[ಪಬಮ]")) -> Viseme.Closed to 280
            syllable.contains(Regex("[ಸಶಷ]")) -> Viseme.Smush to 260
            syllable.contains(Regex("[ತದ]")) -> Viseme.Th to 260
            else -> Viseme.Rest to 200
        }
    }

    fun mouthShapeToViseme(shape: MouthShape): Viseme = when (shape) {
        MouthShape.Closed -> Viseme.Closed
        MouthShape.A -> Viseme.Open
        MouthShape.E -> Viseme.Wide
        MouthShape.I -> Viseme.Wide
        MouthShape.O -> Viseme.Round
        MouthShape.U -> Viseme.Round
        MouthShape.Smile -> Viseme.Rest
    }

    private fun englishG2P(text: String): List<String> {
        val s = text.lowercase().replace(Regex("[^a-z\\s]"), " ")
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < s.length) {
            if (s[i] == ' ') {
                i++
                continue
            }
            val two = s.substring(i, minOf(i + 2, s.length))
            if (two in listOf("ch", "sh", "th", "ng", "ph", "qu", "wh")) {
                tokens.add(two)
                i += 2
            } else {
                tokens.add(s[i].toString())
                i++
            }
        }
        return tokens
    }

    private fun englishPhonemeToViseme(p: String): Pair<Viseme, Int> = when {
        p in listOf("p", "b", "m") -> Viseme.Closed to 140
        p in listOf("f", "v") -> Viseme.FV to 140
        p == "th" -> Viseme.Th to 140
        p in "aeiou" -> when (p) {
            "o", "u" -> Viseme.Round to 220
            "e", "i" -> Viseme.Wide to 200
            else -> Viseme.Open to 240
        }
        else -> Viseme.Rest to 100
    }

    private fun compressConsecutive(frames: List<VisemeFrame>): List<VisemeFrame> {
        if (frames.isEmpty()) return frames
        val result = mutableListOf<VisemeFrame>()
        for (frame in frames) {
            val last = result.lastOrNull()
            if (last != null && last.viseme == frame.viseme) {
                result[result.lastIndex] = last.copy(durationMs = last.durationMs + frame.durationMs)
            } else {
                result.add(frame)
            }
        }
        return result
    }
}
