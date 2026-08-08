package com.ncert7.aitutorandlab.ui.screens.simulation_agent.components

/**
 * Cleans simulation intro/footer text before TTS — drops emojis/icons and expands
 * measurements, rates, and math notation so the engine reads them naturally.
 */
object SimulationIntroTtsSanitizer {
    private val EMOJI_AND_PICTographs =
        Regex("[\\p{Extended_Pictographic}\\p{Emoji_Presentation}\\p{Emoji}\\uFE0F\\u200D]")
    private val MISC_ICON_SYMBOLS = Regex("[\\u2600-\\u26FF\\u2700-\\u27BF]")
    // Non-breaking / typographic spaces. Java's \s does NOT match these, so units like "km / h"
    // written with an nbsp would slip past the expansion patterns and be read as "km h".
    private val UNICODE_SPACES =
        Regex("[\\u00A0\\u1680\\u2000-\\u200A\\u2007\\u202F\\u205F\\u3000\\uFEFF]")
    private val INTERACTIVE_UI_HINT =
        Regex("\\bInteractive\\s*[-–—:]\\s*", RegexOption.IGNORE_CASE)
    private val KEY_CONCEPT_LABEL =
        Regex("^Key Concept\\s*[-–—:]\\s*", RegexOption.IGNORE_CASE)

    private val SUPERSCRIPT_DIGITS =
        mapOf(
            '⁰' to '0', '¹' to '1', '²' to '2', '³' to '3', '⁴' to '4',
            '⁵' to '5', '⁶' to '6', '⁷' to '7', '⁸' to '8', '⁹' to '9',
        )
    private val SUBSCRIPT_DIGITS =
        mapOf(
            '₀' to '0', '₁' to '1', '₂' to '2', '₃' to '3', '₄' to '4',
            '₅' to '5', '₆' to '6', '₇' to '7', '₈' to '8', '₉' to '9',
        )
    private val VULGAR_FRACTIONS =
        mapOf(
            '½' to "one half",
            '⅓' to "one third",
            '⅔' to "two thirds",
            '¼' to "one quarter",
            '¾' to "three quarters",
            '⅕' to "one fifth",
            '⅖' to "two fifths",
            '⅗' to "three fifths",
            '⅘' to "four fifths",
            '⅙' to "one sixth",
            '⅚' to "five sixths",
            '⅛' to "one eighth",
            '⅜' to "three eighths",
            '⅝' to "five eighths",
            '⅞' to "seven eighths",
        )

    private val NUMBER = "(\\d+(?:[.,]\\d+)?)"

    fun forSpeech(raw: String): String {
        if (raw.isBlank()) return ""

        val withoutEmojis =
            buildString {
                raw.codePoints().forEach { cp ->
                    // Never strip ASCII. In Unicode, digits 0-9 (and '#','*') carry the \p{Emoji}
                    // property, so the emoji regex below would otherwise DELETE every number before
                    // it's spoken. Real emoji/pictographs are all above ASCII (>= 0x80).
                    if (cp < 0x80) {
                        appendCodePoint(cp)
                        return@forEach
                    }
                    val charStr = String(Character.toChars(cp))
                    if (EMOJI_AND_PICTographs.containsMatchIn(charStr)) return@forEach
                    if (MISC_ICON_SYMBOLS.matches(charStr)) return@forEach
                    appendCodePoint(cp)
                }
            }

        return withoutEmojis
            .replace(UNICODE_SPACES, " ")
            .replace(INTERACTIVE_UI_HINT, "")
            .replace(KEY_CONCEPT_LABEL, "")
            .let(::expandForSpeech)
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim(' ', '.', '-', '–', '—')
    }

    private fun expandForSpeech(text: String): String {
        var s = normalizeMathCharacters(text)
        s = expandTemperature(s)
        s = expandVulgarFractions(s)
        s = expandSuperscriptsAndSubscripts(s)
        s = expandCompoundUnits(s)
        s = expandSquaredAndCubedUnits(s)
        s = expandSimpleUnits(s)
        s = expandMathSymbols(s)
        s = expandNumericFractions(s)
        return s
    }

    private fun normalizeMathCharacters(text: String): String {
        val normalizedDigits = buildString {
            text.forEach { ch ->
                append(
                    when (ch.code) {
                        in 0xFF10..0xFF19 -> (ch.code - 0xFF10 + '0'.code).toChar()
                        in 0x0660..0x0669 -> (ch.code - 0x0660 + '0'.code).toChar()
                        else -> ch
                    },
                )
            }
        }
        return normalizedDigits
            .replace('\u2212', '-')
            .replace('\u00D7', 'x')
            .replace('\u00F7', '/')
            .replace('\u2248', '~')
            .replace("\u2264", "<=")
            .replace("\u2265", ">=")
            .replace("\u00B1", "+/-")
            .replace("\u221A", " square root of ")
            .replace("\u03C0", " pi ")
            .replace(Regex("(?<![a-zA-Z])-(\\d+(?:[.,]\\d+)?)"), "minus $1")
            // Strip ANY digit-grouping comma — both Western 3-digit (30,000) AND Indian 2-2-3
            // (3,00,000 / 3,87,69,957). The old pattern only matched a comma followed by exactly 3
            // digits, so Indian-grouped numbers kept a stray comma and the TTS read them digit-by-
            // digit ("3 zero zero zero zero zero"). A comma between two digits is always a grouping
            // separator here (decimals use "."), so removing it lets the engine read a real number.
            .replace(Regex("(?<=\\d),(?=\\d)"), "")
    }

    private fun expandVulgarFractions(text: String): String {
        val builder = StringBuilder()
        text.forEach { ch ->
            builder.append(VULGAR_FRACTIONS[ch] ?: ch)
            if (VULGAR_FRACTIONS.containsKey(ch)) builder.append(' ')
        }
        return builder.toString()
    }

    private fun expandSuperscriptsAndSubscripts(text: String): String {
        val builder = StringBuilder()
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                ch in SUPERSCRIPT_DIGITS -> {
                    val digits = buildString {
                        var j = i
                        while (j < text.length && text[j] in SUPERSCRIPT_DIGITS) {
                            append(SUPERSCRIPT_DIGITS.getValue(text[j]))
                            j++
                        }
                        i = j - 1
                    }
                    when (digits) {
                        "2" -> builder.append(" squared")
                        "3" -> builder.append(" cubed")
                        else -> builder.append(" to the power of ").append(digits)
                    }
                }
                ch in SUBSCRIPT_DIGITS -> {
                    builder.append(' ')
                    while (i < text.length && text[i] in SUBSCRIPT_DIGITS) {
                        builder.append(SUBSCRIPT_DIGITS.getValue(text[i]))
                        i++
                    }
                    i--
                }
                else -> builder.append(ch)
            }
            i++
        }
        return builder.toString()
    }

    private fun expandCompoundUnits(text: String): String =
        text
            .replace(Regex("(?i)${NUMBER}\\s*km\\s*/\\s*h(?:r)?\\b"), "$1 kilometers per hour")
            .replace(Regex("(?i)${NUMBER}\\s*k(?:m)?ph\\b"), "$1 kilometers per hour")
            .replace(Regex("(?i)\\bkm\\s*/\\s*h(?:r)?\\b"), "kilometers per hour")
            .replace(Regex("(?i)\\bk(?:m)?ph\\b"), "kilometers per hour")
            .replace(Regex("(?i)${NUMBER}\\s*mph\\b"), "$1 miles per hour")
            .replace(Regex("(?i)\\bmph\\b"), "miles per hour")
            .replace(Regex("(?i)${NUMBER}\\s*m\\s*/\\s*s\\b"), "$1 meters per second")
            .replace(Regex("(?i)\\bm\\s*/\\s*s\\b"), "meters per second")
            .replace(Regex("(?i)${NUMBER}\\s*cm\\s*/\\s*s\\b"), "$1 centimeters per second")
            .replace(Regex("(?i)\\bcm\\s*/\\s*s\\b"), "centimeters per second")

    private fun expandSquaredAndCubedUnits(text: String): String =
        text
            .replace(Regex("(?i)${NUMBER}\\s*km\\s*(?:\\^?2|²)\\b"), "$1 square kilometers")
            .replace(Regex("(?i)${NUMBER}\\s*m\\s*(?:\\^?2|²)\\b"), "$1 square meters")
            .replace(Regex("(?i)${NUMBER}\\s*cm\\s*(?:\\^?2|²)\\b"), "$1 square centimeters")
            .replace(Regex("(?i)${NUMBER}\\s*mm\\s*(?:\\^?2|²)\\b"), "$1 square millimeters")
            .replace(Regex("(?i)${NUMBER}\\s*cm\\s*(?:\\^?3|³)\\b"), "$1 cubic centimeters")
            .replace(Regex("(?i)${NUMBER}\\s*m\\s*(?:\\^?3|³)\\b"), "$1 cubic meters")
            .replace(Regex("(?i)\\bkm\\s*(?:\\^?2|²)\\b"), "square kilometers")
            .replace(Regex("(?i)\\bm\\s*(?:\\^?2|²)\\b"), "square meters")
            .replace(Regex("(?i)\\bcm\\s*(?:\\^?2|²)\\b"), "square centimeters")

    private fun expandSimpleUnits(text: String): String =
        text
            .replace(Regex("(?i)${NUMBER}\\s*km\\b"), "$1 kilometers")
            .replace(Regex("(?i)${NUMBER}\\s*cm\\b"), "$1 centimeters")
            .replace(Regex("(?i)${NUMBER}\\s*mm\\b"), "$1 millimeters")
            .replace(Regex("(?i)${NUMBER}\\s*mg\\b"), "$1 milligrams")
            .replace(Regex("(?i)${NUMBER}\\s*kg\\b"), "$1 kilograms")
            .replace(Regex("(?i)${NUMBER}\\s*ml\\b"), "$1 milliliters")
            .replace(Regex("(?i)${NUMBER}\\s*g\\b"), "$1 grams")
            .replace(Regex("(?i)${NUMBER}\\s*l\\b"), "$1 liters")
            .replace(Regex("(?i)${NUMBER}\\s*m\\b(?!\\w)"), "$1 meters")
            .replace(Regex("(?i)${NUMBER}\\s*hz\\b"), "$1 hertz")
            .replace(Regex("(?i)${NUMBER}\\s*sec(?:s|onds?)?\\b"), "$1 seconds")
            .replace(Regex("(?i)${NUMBER}\\s*min(?:s|utes?)?\\b"), "$1 minutes")
            .replace(Regex("(?i)${NUMBER}\\s*h(?:r|rs|ours?)\\b"), "$1 hours")
            // Plurals like "kms"/"cms" (trailing s) — handle before the singular fallbacks.
            .replace(Regex("(?i)\\bkms\\b"), "kilometers")
            .replace(Regex("(?i)\\bcms\\b"), "centimeters")
            .replace(Regex("(?i)\\bkm\\b"), "kilometers")
            .replace(Regex("(?i)\\bcm\\b"), "centimeters")
            .replace(Regex("(?i)\\bmm\\b"), "millimeters")
            .replace(Regex("(?i)\\bkg\\b"), "kilograms")
            .replace(Regex("(?i)\\bmg\\b"), "milligrams")
            .replace(Regex("(?i)\\bml\\b"), "milliliters")
            .replace(Regex("(?i)\\bhz\\b"), "hertz")

    private fun expandTemperature(text: String): String =
        text
            // Combined unicode degree symbols (U+2103, U+2109)
            .replace("\u2103", " degrees Celsius")
            .replace("\u2109", " degrees Fahrenheit")
            .replace("℃", " degrees Celsius")
            .replace("℉", " degrees Fahrenheit")
            // Glued forms: 100°C, 100ºC, 100 ° C, etc.
            .replace(Regex("(?i)($NUMBER)\\s*[°º˚\\u00B0]\\s*C\\b"), "$1 degrees Celsius")
            .replace(Regex("(?i)($NUMBER)\\s*[°º˚\\u00B0]\\s*F\\b"), "$1 degrees Fahrenheit")
            // Literal glued pairs (most reliable — TTS otherwise reads ° as "degree")
            .replace("°C", " degrees Celsius")
            .replace("°F", " degrees Fahrenheit")
            .replace("ºC", " degrees Celsius")
            .replace("ºF", " degrees Fahrenheit")
            .replace("˚C", " degrees Celsius")
            .replace("˚F", " degrees Fahrenheit")
            // Spaced forms: ° C, degree C
            .replace(Regex("(?i)[°º˚\\u00B0]\\s*C\\b"), "degrees Celsius")
            .replace(Regex("(?i)[°º˚\\u00B0]\\s*F\\b"), "degrees Fahrenheit")
            .replace(Regex("(?i)\\bdegrees?\\s+C\\b"), "degrees Celsius")
            .replace(Regex("(?i)\\bdegrees?\\s+F\\b"), "degrees Fahrenheit")

    private fun expandMathSymbols(text: String): String =
        text
            .replace("=>", " leads to ")
            .replace("->", " leads to ")
            .replace("\u2192", " leads to ")
            .replace("\u21D2", " leads to ")
            .replace(Regex("(?i)(?<=\\d)\\s*[xX]\\s*(?=\\d)"), " times ")
            .replace(Regex("(?<=\\d)\\s*/\\s*(?=\\d)"), " divided by ")
            .replace(Regex("\\s*=\\s*"), " is equal to ")
            .replace("+/-", " plus or minus ")
            .replace(">=", " greater than or equal to ")
            .replace("<=", " less than or equal to ")
            .replace('~', ' ')
            .replace(Regex("\\s+"), " ")

    private fun expandNumericFractions(text: String): String =
        text.replace(Regex("\\b(\\d+)\\s*/\\s*(\\d+)\\b"), "$1 over $2")
}
