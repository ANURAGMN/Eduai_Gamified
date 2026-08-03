package com.anurag.eduai.uikit.avatar.animation

import com.anurag.eduai.uikit.avatar.core.MouthShape

/**
 * Maps English text to mouth shapes for lip sync v2 (Goal.md phoneme example).
 * Uses lightweight letter-to-viseme rules until TTS phoneme output is wired.
 */
object PhonemeMapper {

    private val vowelOpen = setOf('a', 'o')
    private val vowelWide = setOf('e')
    private val vowelSmile = setOf('i')
    private val vowelRound = setOf('u', 'w')
    private val labials = setOf('m', 'b', 'p')

    fun mouthShapesForText(text: String): List<MouthShape> {
        if (text.isBlank()) return listOf(MouthShape.Closed)

        val shapes = mutableListOf<MouthShape>()
        var i = 0
        val lower = text.lowercase()

        while (i < lower.length) {
            val c = lower[i]
            when {
                c.isWhitespace() || c in ".,!?;:'\"-" -> {
                    if (shapes.lastOrNull() != MouthShape.Closed) {
                        shapes.add(MouthShape.Closed)
                    }
                }
                c in labials -> shapes.add(MouthShape.Closed)
                c == 'h' -> shapes.add(MouthShape.A)
                c in vowelOpen -> shapes.add(if (c == 'o') MouthShape.O else MouthShape.A)
                c in vowelWide -> shapes.add(MouthShape.E)
                c in vowelSmile -> shapes.add(MouthShape.I)
                c in vowelRound -> shapes.add(MouthShape.U)
                c == 'l' -> shapes.add(MouthShape.A)
                c == 'r' -> shapes.add(MouthShape.O)
                c == 'f' || c == 'v' -> shapes.add(MouthShape.E)
                c == 's' || c == 'z' || c == 'c' -> shapes.add(MouthShape.I)
                c == 'y' -> shapes.add(MouthShape.I)
                c == 'g' || c == 'k' -> shapes.add(MouthShape.A)
                c == 't' || c == 'd' -> shapes.add(MouthShape.E)
                c == 'n' -> shapes.add(MouthShape.Closed)
                else -> shapes.add(MouthShape.Closed)
            }
            i++
        }

        return dedupeAdjacent(shapes).ifEmpty { listOf(MouthShape.Closed) }
    }

    private fun dedupeAdjacent(shapes: List<MouthShape>): List<MouthShape> {
        if (shapes.isEmpty()) return shapes
        return buildList {
            var prev: MouthShape? = null
            for (shape in shapes) {
                if (shape != prev) {
                    add(shape)
                    prev = shape
                }
            }
        }
    }
}
