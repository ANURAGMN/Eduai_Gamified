package com.ncert7.aitutorandlab.ui.viewModel

import android.content.Context
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.ncert7.aitutorandlab.debug.DebugLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SpeechToText @Inject constructor() : ViewModel() {
    private val TAG = "SpeechToText"
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        const val RECORD_AUDIO_PERMISSION_REQUEST = 100
        private const val CONTINUOUS_RESTART_DELAY_MS = 100L
        // After the user pauses this long (with something already captured), stop
        // listening automatically so voice input finishes hands-free.
        private const val SILENCE_TIMEOUT_MS = 1600L
    }

    // Fires when the user has gone quiet after speaking — auto-stops the session so
    // the captured text can be sent without tapping the close button.
    private val silenceRunnable = Runnable {
        if (_state.value.isListening && fullTranscript.isNotBlank()) {
            stopListening()
        }
    }

    private fun scheduleSilenceAutoStop() {
        handler.removeCallbacks(silenceRunnable)
        if (fullTranscript.isNotBlank()) {
            handler.postDelayed(silenceRunnable, SILENCE_TIMEOUT_MS)
        }
    }

    private fun cancelSilenceAutoStop() {
        handler.removeCallbacks(silenceRunnable)
    }

    // ---- Localized status strings ----------------------------------------------------
    // The recognizer status is shown to the learner, so it must match the app language.
    // (Resource strings aren't reachable from this ViewModel here, so we branch inline.)
    private fun isKannadaSelected(): Boolean =
        _state.value.selectedLanguage.startsWith("kn", ignoreCase = true)

    private val msgListening get() = if (isKannadaSelected()) "ಕೇಳುತ್ತಿದೆ…" else "Listening…"
    private val msgReady get() = if (isKannadaSelected()) "ಮಾತನಾಡಿ…" else "Ready for speech…"
    private val msgProcessing get() = if (isKannadaSelected()) "ಪ್ರಕ್ರಿಯೆಗೊಳಿಸುತ್ತಿದೆ…" else "Processing speech…"
    private val msgDidntCatch get() = if (isKannadaSelected()) "ಸರಿಯಾಗಿ ಕೇಳಿಸಲಿಲ್ಲ" else "Didn't catch that"
    private val msgKeepSpeaking get() = if (isKannadaSelected()) "ಸಿಕ್ಕಿತು! ಮುಂದುವರಿಸಿ…" else "Got it! Keep speaking…"
    private val msgGenericError get() = if (isKannadaSelected()) "ದೋಷ ಸಂಭವಿಸಿದೆ, ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ" else "Something went wrong, try again"

    data class STTState(
        val isInitialized: Boolean = false,
        val isListening: Boolean = false,
        val selectedLanguage: String = "en-IN",
        val statusMessage: String = "",
        val resultText: String = "",
        val hasPermission: Boolean = false,
        val audioAmplitude: Float = 0f  // Voice amplitude for animation (0f to 1f)
    )

    private val _state = MutableStateFlow(STTState())
    val state: StateFlow<STTState> = _state.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private val fullTranscript = StringBuilder()
    private var appContext: Context? = null

    // Initialize the SpeechToText system (UI should call this once e.g. on screen start)
    fun initialize(context: Context) {
        if (_state.value.isInitialized) return
        appContext = context.applicationContext
        DebugLogger.debugLog(TAG, "initialize() - Starting setup")

        val hasPermission = checkAudioPermission()
        _state.value = _state.value.copy(hasPermission = hasPermission)

        if (hasPermission) {
            initializeSpeechRecognizer()
        }
    }

    private fun checkAudioPermission(): Boolean {
        return appContext?.let { ctx ->
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } ?: false
    }

    private fun initializeSpeechRecognizer() {
        appContext?.let { ctx ->
            if (SpeechRecognizer.isRecognitionAvailable(ctx)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(ctx).apply {
                    setRecognitionListener(speechRecognitionListener)
                }
                _state.value = _state.value.copy(
                    isInitialized = true,
                    statusMessage = "Speech recognizer initialized"
                )
                DebugLogger.debugLog(TAG, "Speech recognizer initialized successfully")
            } else {
                _state.value = _state.value.copy(
                    statusMessage = "Speech recognition not available on this device"
                )
                DebugLogger.errorLog(TAG, "Recognition not available")
            }
        }
    }

    /**
     * Public start API.
     * Pass `requestedLanguage` like "kn" or "kn-IN" to request Kannada recognition.
     * If `requestedLanguage` is null, current selected language is used.
     */
    fun startListening(requestedLanguage: String? = null) {
        requestedLanguage?.let { lang ->
            val normalized = normalizeLanguageTag(lang)
            setLanguage(normalized)
        }
        if (!_state.value.hasPermission) {
            _state.value = _state.value.copy(statusMessage = "Audio permission required")
            DebugLogger.errorLog(TAG, "startListening: missing audio permission")
            return
        }

        if (!_state.value.isInitialized) {
            _state.value = _state.value.copy(statusMessage = "Speech recognizer not initialized")
            DebugLogger.errorLog(TAG, "startListening: recognizer not initialized")
            return
        }

        if (_state.value.isListening) {
            DebugLogger.debugLog(TAG, "startListening: already listening")
            return
        }

        // Normalize and set language if provided
        requestedLanguage?.let { lang ->
            val normalized = normalizeLanguageTag(lang)
            setLanguage(normalized)
        }

        // Reset transcript and update state
        fullTranscript.clear()
        _state.value = _state.value.copy(
            resultText = "",
            statusMessage = msgListening,
            isListening = true
        )

        DebugLogger.debugLog(TAG, "=== USER STARTED NEW SPEECH SESSION === language=${_state.value.selectedLanguage}")
        startRecognition()
    }

    // Stop the current speech recognition session
    fun stopListening() {
        if (!_state.value.isListening) return
        cancelSilenceAutoStop()
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "stopListening error: $e")
        }
        _state.value = _state.value.copy(
            isListening = false,
            statusMessage = "Stopped",
            audioAmplitude = 0f  // Reset amplitude
        )
        DebugLogger.debugLog(TAG, "Stopped Listening...")
    }

    // Internal method to start recognition using current state's language
    private fun startRecognition() {
        if (!_state.value.isListening) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, _state.value.selectedLanguage)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, _state.value.selectedLanguage)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        DebugLogger.debugLog(TAG, "startRecognition() - listening with language=${_state.value.selectedLanguage}")
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "Error starting speech recognizer: $e")
            _state.value = _state.value.copy(statusMessage = "Error starting recognizer")
            // stop listening to avoid inconsistent state
            stopListening()
        }
    }

    fun setLanguage(language: String) {
        val normalized = normalizeLanguageTag(language)
        _state.value = _state.value.copy(selectedLanguage = normalized)
        DebugLogger.debugLog(TAG, "Language set to: $normalized")
    }

    // Accepts short or full tags: "kn" -> "kn-IN", "en" -> "en-IN", returns full tag
    private fun normalizeLanguageTag(tag: String): String {
        val code = tag.trim().lowercase()
        return when {
            code.startsWith("kn") -> "kn-IN"
            code.startsWith("hi") -> "hi-IN"
            code.startsWith("ta") -> "ta-IN"
            code.startsWith("te") -> "te-IN"
            code.startsWith("en") -> "en-IN"
            code.contains("-") -> tag // assume already full tag like en-IN
            else -> "en-IN"
        }
    }

    private val speechRecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            DebugLogger.debugLog(TAG, "onReadyForSpeech - Mic is live")
            _state.value = _state.value.copy(statusMessage = msgReady)
        }

        override fun onBeginningOfSpeech() {
            // User resumed talking — cancel any pending auto-stop.
            cancelSilenceAutoStop()
            _state.value = _state.value.copy(statusMessage = msgListening)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _state.value = _state.value.copy(statusMessage = msgProcessing)
            // If we've already captured something, start the silence countdown so the
            // session ends (and sends) hands-free once the user stops talking.
            scheduleSilenceAutoStop()
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> msgDidntCatch
                else -> msgGenericError
            }

            DebugLogger.errorLog(TAG, "onError: $errorMessage ($error)")

            // For no speech detected errors, show message and wait before closing
            if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                // Keep listening state true but show the message
                _state.value = _state.value.copy(statusMessage = errorMessage)

                // Wait 2 seconds before closing
                handler.postDelayed({
                    _state.value = _state.value.copy(
                        isListening = false,
                        statusMessage = errorMessage,
                        audioAmplitude = 0f
                    )
                }, 2000L)
            } else {
                // For other errors, close immediately
                _state.value = _state.value.copy(
                    isListening = false,
                    statusMessage = errorMessage,
                    audioAmplitude = 0f
                )
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (matches.isNullOrEmpty() || matches[0].trim().isEmpty()) {
                DebugLogger.debugLog(TAG, "onResults() - Empty or blank result → stopping")

                // Show "Didn't catch that" and wait before closing
                _state.value = _state.value.copy(statusMessage = msgDidntCatch)

                handler.postDelayed({
                    _state.value = _state.value.copy(
                        isListening = false,
                        audioAmplitude = 0f
                    )
                }, 2000L)
                return
            }

            val spokenText = matches[0].trim()
            DebugLogger.debugLog(TAG, "onResults() - Recognized: \"$spokenText\"")

            if (fullTranscript.isEmpty()) fullTranscript.append(spokenText) else fullTranscript.append(" ").append(spokenText)

            _state.value = _state.value.copy(
                resultText = fullTranscript.toString(),
                statusMessage = msgKeepSpeaking
            )

            // If still in listening mode restart recognition to continue streaming input,
            // and (re)arm the silence auto-stop so a pause ends the session automatically.
            if (_state.value.isListening) {
                scheduleSilenceAutoStop()
                handler.postDelayed({ startRecognition() }, CONTINUOUS_RESTART_DELAY_MS)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!partial.isNullOrEmpty() && partial[0].trim().isNotEmpty()) {
                // Speech is actively coming in — hold off the auto-stop.
                cancelSilenceAutoStop()
                val current = if (fullTranscript.isEmpty()) partial[0] else fullTranscript.toString() + " " + partial[0]
                _state.value = _state.value.copy(resultText = current)
            }
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Convert RMS dB to amplitude (0f to 1f range)
            // RMS values typically range from 0 to ~10 dB
            val normalizedAmplitude = (rmsdB / 10f).coerceIn(0f, 1f)
            _state.value = _state.value.copy(audioAmplitude = normalizedAmplitude)
        }
    }

    fun handlePermissionResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted()
            } else {
                _state.value = _state.value.copy(
                    hasPermission = false,
                    statusMessage = "Audio permission denied"
                )
                DebugLogger.errorLog(TAG, "Permission denied")
            }
        }
    }

    private fun onPermissionGranted() {
        _state.value = _state.value.copy(hasPermission = true)
        if (!_state.value.isInitialized) {
            initializeSpeechRecognizer()
        }
    }

    override fun onCleared() {
        super.onCleared()
        destroy()
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "destroy error: $e")
        } finally {
            speechRecognizer = null
            appContext = null
            _state.value = _state.value.copy(
                isInitialized = false,
                isListening = false
            )
        }
    }
}
