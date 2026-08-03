package com.anurag.eduai.uikit.avatar.animation

import com.anurag.eduai.uikit.avatar.core.MouthShape
import com.anurag.eduai.uikit.avatar.core.Viseme
import kotlin.random.Random

enum class LipSyncMode {
    RandomV1,
    PhonemeV2,
    EduappViseme
}

class LipSyncController(
    private var mode: LipSyncMode = LipSyncMode.EduappViseme
) {

    private val speakingShapes = listOf(
        MouthShape.A, MouthShape.E, MouthShape.I, MouthShape.O, MouthShape.U, MouthShape.Closed
    )

    private var lastChangeMs: Long = 0
    private var currentShape: MouthShape = MouthShape.Closed
    private var currentViseme: Viseme = Viseme.Rest
    private val changeIntervalMs = 80L..140L

    // Minimum time a viseme stays on screen. If a short sentence's phoneme timeline gets
    // scaled into a much shorter estimated duration, the mouth would otherwise flip shapes
    // several times faster than natural — the "blabbering/shaking" at the end of short lines.
    private val minVisemeHoldMs = 90L
    private var lastVisemeChangeMs: Long = 0

    private var visemeTimeline: List<EduappVisemeMapper.VisemeFrame> = emptyList()
    private var timelineStartMs: Long = 0
    private var timelineDurationMs: Long = 0

    // Word-boundary anchoring: the timeline plays at its NATURAL phoneme rate, and each real
    // TTS word boundary (noteWordBoundary) snaps the "native playhead" to that word's start so
    // the mouth stays locked to the actual audio. Written from the TTS thread, read on the
    // frame thread — hence @Volatile.
    private var wordStartMs: List<Long> = emptyList()
    @Volatile private var anchorRealMs: Long = 0
    @Volatile private var anchorNativeMs: Long = 0

    fun setMode(newMode: LipSyncMode) {
        mode = newMode
        reset()
    }

    fun startPhonemeTimeline(text: String, durationMs: Long, startMs: Long) {
        when (mode) {
            LipSyncMode.EduappViseme -> {
                val timeline = EduappVisemeMapper.visemeTimelineWithWords(text)
                visemeTimeline = timeline.frames
                wordStartMs = timeline.wordStartMs
                timelineDurationMs = durationMs.coerceAtLeast(300)
                timelineStartMs = startMs
                anchorRealMs = startMs
                anchorNativeMs = 0
                lastVisemeChangeMs = 0
                currentViseme = visemeTimeline.firstOrNull()?.viseme ?: Viseme.Rest
            }
            else -> {
                val shapes = PhonemeMapper.mouthShapesForText(text)
                phonemeShapes = shapes
                phonemeStartMs = startMs
                phonemeDurationMs = durationMs.coerceAtLeast(300)
                currentShape = shapes.firstOrNull() ?: MouthShape.Closed
            }
        }
    }

    private var phonemeShapes: List<MouthShape> = emptyList()
    private var phonemeStartMs: Long = 0
    private var phonemeDurationMs: Long = 0

    fun update(isSpeaking: Boolean, timestampMs: Long): Pair<MouthShape, Viseme> {
        if (!isSpeaking) {
            currentShape = MouthShape.Closed
            currentViseme = Viseme.Rest
            return currentShape to currentViseme
        }

        return when (mode) {
            LipSyncMode.EduappViseme -> updateEduappViseme(timestampMs)
            LipSyncMode.PhonemeV2 -> updatePhoneme(timestampMs) to EduappVisemeMapper.mouthShapeToViseme(currentShape)
            LipSyncMode.RandomV1 -> updateRandom(timestampMs) to EduappVisemeMapper.mouthShapeToViseme(currentShape)
        }
    }

    /**
     * Re-anchor the viseme playhead to the start of [wordIndex] at real time [timestampMs].
     * Called from the TTS word-boundary callback so the mouth tracks the actual audio.
     */
    fun noteWordBoundary(wordIndex: Int, timestampMs: Long) {
        if (wordIndex in wordStartMs.indices) {
            anchorNativeMs = wordStartMs[wordIndex]
            anchorRealMs = timestampMs
        }
    }

    private fun updateEduappViseme(timestampMs: Long): Pair<MouthShape, Viseme> {
        if (visemeTimeline.isEmpty()) {
            val shape = updateRandom(timestampMs)
            currentViseme = EduappVisemeMapper.mouthShapeToViseme(shape)
            return shape to currentViseme
        }

        // Play at the natural phoneme rate from the last word anchor. With real TTS,
        // noteWordBoundary keeps re-anchoring so this stays locked to the audio; without TTS
        // it just advances from the sentence start at a believable rate (no estimate-scaling,
        // which is what used to over-compress short lines into a blabber).
        val nativeMs = anchorNativeMs + (timestampMs - anchorRealMs)

        var target = visemeTimeline.last().viseme
        var accumulated = 0L
        for (frame in visemeTimeline) {
            accumulated += frame.durationMs
            if (nativeMs < accumulated) {
                target = frame.viseme
                break
            }
        }

        // Debounce: only actually switch if the current shape has been held long enough.
        if (target != currentViseme && timestampMs - lastVisemeChangeMs >= minVisemeHoldMs) {
            currentViseme = target
            currentShape = visemeToMouthShape(target)
            lastVisemeChangeMs = timestampMs
        }
        return currentShape to currentViseme
    }

    private fun visemeToMouthShape(viseme: Viseme): MouthShape = when (viseme) {
        Viseme.Rest -> MouthShape.Smile
        Viseme.Closed -> MouthShape.Closed
        Viseme.Wide -> MouthShape.E
        Viseme.Open -> MouthShape.A
        Viseme.Round -> MouthShape.O
        Viseme.FV, Viseme.Th, Viseme.Smush, Viseme.Kiss -> MouthShape.E
    }

    private fun updatePhoneme(timestampMs: Long): MouthShape {
        if (phonemeShapes.isEmpty()) return updateRandom(timestampMs)
        val elapsed = timestampMs - phonemeStartMs
        val progress = (elapsed.toFloat() / phonemeDurationMs).coerceIn(0f, 0.999f)
        val index = (progress * phonemeShapes.size).toInt().coerceIn(0, phonemeShapes.lastIndex)
        currentShape = phonemeShapes[index]
        return currentShape
    }

    private fun updateRandom(timestampMs: Long): MouthShape {
        if (timestampMs - lastChangeMs >= Random.nextLong(changeIntervalMs.first, changeIntervalMs.last)) {
            currentShape = speakingShapes.random()
            lastChangeMs = timestampMs
        }
        return currentShape
    }

    fun reset() {
        currentShape = MouthShape.Closed
        currentViseme = Viseme.Rest
        lastChangeMs = 0
        lastVisemeChangeMs = 0
        visemeTimeline = emptyList()
        wordStartMs = emptyList()
        anchorRealMs = 0
        anchorNativeMs = 0
        phonemeShapes = emptyList()
        timelineStartMs = 0
        timelineDurationMs = 0
    }
}
