package com.anurag.eduai.uikit.components

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Central tactile + audio feedback. Haptics are the primary channel (they carry
 * most of the "premium feel"); sound is a light optional accent on the bigger
 * moments only, so taps never get noisy.
 *
 * Drop nicer `.mp3`/`.wav` cues into `res/raw` and load them via SoundPool if you
 * want richer audio later — the ToneGenerator fallback here is intentionally
 * minimal and dependency-free.
 */
class EduFeedback(
    private val vibrator: Vibrator?,
    private val tone: ToneGenerator?,
) {
    /** Light click for ordinary taps. */
    fun tap() = oneShot(12L, 60)

    /** Two quick beats for claiming a quest reward. */
    fun claim() {
        waveform(longArrayOf(0, 22, 45, 32), intArrayOf(0, 130, 0, 210))
        playTone(ToneGenerator.TONE_PROP_BEEP, 90)
    }

    /** Escalating celebratory pattern for the full-screen reward moment. */
    fun reward() {
        waveform(
            longArrayOf(0, 30, 50, 40, 60, 70),
            intArrayOf(0, 150, 0, 210, 0, 255),
        )
        playTone(ToneGenerator.TONE_PROP_BEEP2, 140)
    }

    private fun oneShot(ms: Long, amplitude: Int) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        runCatching { v.vibrate(VibrationEffect.createOneShot(ms, amplitude)) }
    }

    private fun waveform(timings: LongArray, amplitudes: IntArray) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        runCatching { v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1)) }
    }

    private fun playTone(type: Int, durationMs: Int) {
        val t = tone ?: return
        runCatching { t.startTone(type, durationMs) }
    }
}

@Composable
fun rememberEduFeedback(soundEnabled: Boolean = true): EduFeedback {
    val context = LocalContext.current
    return remember(soundEnabled) {
        val vib =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                    ?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        val tone =
            if (soundEnabled) {
                runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 60) }.getOrNull()
            } else {
                null
            }
        EduFeedback(vib, tone)
    }
}
