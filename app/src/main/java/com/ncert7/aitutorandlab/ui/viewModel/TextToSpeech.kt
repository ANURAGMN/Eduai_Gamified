package com.ncert7.aitutorandlab.ui.viewModel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech as AndroidTextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.ViewModel
import com.ncert7.aitutorandlab.config.isNativeTutorAvatarEnabledForContext
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.text.ProcessedText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TextToSpeech @Inject constructor() : ViewModel(), AndroidTextToSpeech.OnInitListener {

    data class TTSState(
        val isInitialized: Boolean = false,
        val isSpeaking: Boolean = false,
        val selectedLanguage: String = "en-IN",
        // The app-wide selected language (source of truth from the UI). Used as the
        // fallback when a spoken chunk has no detectable Indic script, so Kannada
        // users don't get an English voice by default.
        val appLanguage: String = "en-IN",
        val detectedLanguage: String = "",
        val selectedCharacter: String = "boy",
        val speechRate: Float = 1.0f,
        val pitch: Float = 1.0f,
        val statusMessage: String = "",
        val debugMode: Boolean = false,
        val availableVoices: List<Voice> = emptyList(),
        val selectedVoice: Voice? = null,
        val selectedVoiceDisplayName: String = "Default Voice",
        val currentViseme: String = "rest",
        val voicesFullyLoaded: Boolean = false,
        val speakingDurationMs: Long = 0,
        val speakingText: String = "",
    )

    private val _state = MutableStateFlow(TTSState())
    val state: StateFlow<TTSState> = _state.asStateFlow()

    private var textToSpeech: AndroidTextToSpeech? = null
    private var webView: WebView? = null
    private var nativeLipSyncEnabled = false

    // Never speak while the app isn't in the foreground (home button, an ad / reward video, a
    // system dialog, or being sent to the Play Store). We stop the moment the visible activity
    // pauses and block any speech that's requested while we're away.
    @Volatile
    private var appInForeground = true
    private var application: Application? = null
    private var lifecycleCallbacksRegistered = false
    private val activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            appInForeground = true
        }

        override fun onActivityPaused(activity: Activity) {
            appInForeground = false
            stop()
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    private val languagePatterns = mapOf(
        "hi-IN" to Regex("[\u0900-\u097F]+"),
        "kn-IN" to Regex("[\u0C80-\u0CFF]+"),
        "ta-IN" to Regex("[\u0B80-\u0BFF]+"),
        "te-IN" to Regex("[\u0C00-\u0C7F]+")
    )

    private val languageNames = mapOf(
        "en-IN" to "English",
        "hi-IN" to "हिंदी (Hindi)",
        "kn-IN" to "ಕನ್ನಡ (Kannada)",
        "ta-IN" to "தமிழ் (Tamil)",
        "te-IN" to "తెలుగు (Telugu)"
    )

    private var speechStartTime: Long = 0
    private var totalEstimatedDuration: Long = 0

    private val _currentWordIndex = MutableStateFlow(-1)
    val currentWordIndex: StateFlow<Int> = _currentWordIndex.asStateFlow()
    private var currentSpeakingWords: List<String> = emptyList()
    private var currentWordRanges: List<IntRange> = emptyList()
    var currentSpeakingText: String = ""
        private set
    var currentProcessedData: ProcessedText? = null
        private set

    // Setup utterance progress listener
    private val utteranceListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            if (isIgnorableUtterance(utteranceId)) return
            _state.value = _state.value.copy(isSpeaking = true)
            speechStartTime = System.currentTimeMillis()
            updateStatus("Playing")
            DebugLogger.debugLog("TTS", "Utterance started: $utteranceId")
        }

        override fun onDone(utteranceId: String?) {
            if (isIgnorableUtterance(utteranceId)) return
            _state.value = _state.value.copy(
                isSpeaking = false,
                speakingDurationMs = 0,
                speakingText = "",
                statusMessage = "Playback finished",
            )
            _currentWordIndex.value = -1
            speechStartTime = 0
            totalEstimatedDuration = 0
            stopLipSync()
            DebugLogger.debugLog("TTS", "Utterance done: $utteranceId")
        }

        override fun onError(utteranceId: String?) {
            if (isIgnorableUtterance(utteranceId)) return
            _state.value = _state.value.copy(
                isSpeaking = false,
                speakingDurationMs = 0,
                speakingText = "",
                statusMessage = "Playback error",
            )
            _currentWordIndex.value = -1
            DebugLogger.errorLog("TTS", "Utterance error: $utteranceId")
        }

        // onRangeStart is available API 26+
        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            if (isIgnorableUtterance(utteranceId)) return
            // Use processedData word positions if available
            val idx = if (currentProcessedData != null) {
                currentProcessedData?.wordPositions?.indexOfFirst { word ->
                    start >= word.start && start <= word.end
                } ?: -1
            } else {
                // map character offset to word index
                currentWordRanges.indexOfFirst { range -> start in range }
            }

            if (idx >= 0) {
                _currentWordIndex.value = idx
            }
        }
    }

    fun initialize(context: Context) {
        nativeLipSyncEnabled = isNativeTutorAvatarEnabledForContext(context)

        // Register once so TTS stops app-wide whenever the foreground activity pauses.
        if (!lifecycleCallbacksRegistered) {
            (context.applicationContext as? Application)?.let { app ->
                application = app
                app.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
                lifecycleCallbacksRegistered = true
            }
        }
        register(this)

        //Only skip if the engine instance already exists
        if (textToSpeech != null) return

        updateStatus("Initializing Text-to-Speech...")
        DebugLogger.debugLog("TextToSpeech", "Initializing Text-to-Speech...")
        textToSpeech = AndroidTextToSpeech(context, this)
    }

    /** When true, lip sync is driven by [EduTutorAvatarWithLipSync] instead of WebView JS. */
    fun setNativeLipSyncEnabled(enabled: Boolean) {
        nativeLipSyncEnabled = enabled
        if (enabled && _state.value.isInitialized) {
            applyDefaultsForAvatarLanguage("tutor", _state.value.selectedLanguage)
        }
    }

    /** Maps legacy boy/girl to a single warm tutor voice when native avatar is active. */
    private fun resolveVoiceProfile(avatar: String): String {
        if (!nativeLipSyncEnabled) return avatar
        return when (avatar.lowercase()) {
            "disable" -> "disable"
            else -> "tutor"
        }
    }

    private fun Voice.engineVariantCode(): String? =
        name.split('-').getOrNull(3)?.lowercase()

    private fun preferredVoiceNames(shortLang: String, profile: String): List<String> =
        when (Pair(shortLang, profile.lowercase())) {
            Pair("en", "boy") -> listOf("en-in-x-ene-local", "ene-local", "en-in-x-ene-network", "ene-network")
            Pair("en", "girl") -> listOf("en-in-x-ena-local", "ena-local", "en-in-x-ena-network", "ena-network")
            // Default English tutor voice: ENE, offline (local) preferred, network as fallback.
            Pair("en", "tutor") -> listOf("en-in-x-ene-local", "ene-local", "en-in-x-ene-network", "ene-network")
            Pair("kn", "boy") -> listOf("kn-in-x-knd-network", "knd-network", "knd-in-x-knd-local", "knd-local")
            Pair("kn", "girl") -> listOf("kn-in-x-knc-network", "knc-network", "kn-in-x-knc-local", "knc-local")
            // Default Kannada tutor voice: KND, offline (local) preferred, network as fallback.
            Pair("kn", "tutor") -> listOf("kn-in-x-knd-local", "knd-local", "kn-in-x-knd-network", "knd-network")
            else -> emptyList()
        }

    private fun isIgnorableUtterance(utteranceId: String?): Boolean =
        utteranceId == PRE_SPEAK_FLUSH_ID || utteranceId?.startsWith(FORCE_VOICE_PREFIX) == true

    companion object {
        private const val PRE_SPEAK_FLUSH_ID = "pre_speak_flush"
        private const val FORCE_VOICE_PREFIX = "force_voice_"

        // Fullscreen ads (rewarded/interstitial) may NOT pause the host activity, and the WebView
        // speech engine isn't governed by the activity lifecycle — so a playing utterance can leak
        // over an ad. The ad manager flips this flag around show/dismiss; every live engine mutes
        // and no new speech starts while it's set.
        @Volatile
        var adShowing: Boolean = false
            private set

        private val liveInstances =
            java.util.Collections.newSetFromMap(java.util.WeakHashMap<TextToSpeech, Boolean>())

        private fun register(t: TextToSpeech) {
            synchronized(liveInstances) { liveInstances.add(t) }
        }

        private fun unregister(t: TextToSpeech) {
            synchronized(liveInstances) { liveInstances.remove(t) }
        }

        /** Call when a fullscreen ad becomes visible: mute and hard-stop every live TTS engine. */
        fun onAdShown() {
            adShowing = true
            val snapshot = synchronized(liveInstances) { liveInstances.toList() }
            snapshot.forEach { runCatching { it.stop() } }
        }

        /** Call when the ad is dismissed/failed so speech is allowed again. */
        fun onAdDismissed() {
            adShowing = false
        }
    }

    fun setupWebView(webView: WebView) {
        this.webView = webView
        webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Initialize with current character
                    switchCharacter(_state.value.selectedCharacter)
                    injectCharacterImages(view?.context!!)
                    updateStatus("Lip sync ready")
                }
            }

            loadUrl("file:///android_asset/LipSync.html")
        }
    }

    override fun onInit(status: Int) {
        if (status == AndroidTextToSpeech.SUCCESS) {
            textToSpeech?.let { tts ->
                // register listener so we can update current word / speaking state
                tts.setOnUtteranceProgressListener(utteranceListener)

                val voices = tts.voices?.toList() ?: emptyList()

                // Filter to only Indian accent voices (both local and network)
                val indianVoices = voices.filter { voice ->
                    val locale = voice.locale.toString().lowercase()
                    val country = voice.locale.country.lowercase()
                    // Only keep voices with Indian locale (IN)
                    country == "in" || locale.contains("_in") || locale.contains("-in")
                }

                // Sort voices: prioritize by quality, then by name
                val sortedVoices = indianVoices.sortedWith(
                    compareByDescending<Voice> { it.quality }
                        .thenBy { it.locale.displayLanguage }
                        .thenBy { it.name }
                )



                _state.value = _state.value.copy(
                    isInitialized = true,
                    availableVoices = sortedVoices,
                    voicesFullyLoaded = true,
                    statusMessage = "TTS Ready • ${sortedVoices.size} voices (${indianVoices.count { it.isNetworkConnectionRequired }} network, ${indianVoices.count { !it.isNetworkConnectionRequired }} local)"
                )

                DebugLogger.debugLog(
                    "TTS",
                    "Initialized with ${sortedVoices.size} Indian voices. Network voices: ${sortedVoices.count { it.isNetworkConnectionRequired }}, Local voices: ${sortedVoices.count { !it.isNetworkConnectionRequired }}"
                )

                val defaultAvatar = if (nativeLipSyncEnabled) "tutor" else _state.value.selectedCharacter
                applyDefaultsForAvatarLanguage(defaultAvatar, _state.value.appLanguage)
            }
        }
    }
    fun formatVoiceName(voice: Voice): String {
        // Build a human-friendly label instead of exposing the raw engine id
        // (e.g. "en-in-x-ene-local"). Leads with gender + language so a parent can
        // tell the options apart at a glance. A short variant tag keeps entries
        // unique when a language has several voices.
        val gender = when {
            voice.features?.contains("male") == true -> "Male"
            voice.features?.contains("female") == true -> "Female"
            else -> ""
        }
        val language = voice.locale.displayLanguage
        // The 4th dash-segment of the engine id distinguishes sibling voices.
        val variant = voice.name.split('-').getOrNull(3)
            ?.takeIf { it.isNotBlank() }
            ?.uppercase()
            ?.let { " (Voice $it)" } ?: ""
        val quality = when (voice.quality) {
            Voice.QUALITY_VERY_HIGH -> " • Best"
            Voice.QUALITY_HIGH -> " • Clear"
            else -> ""
        }
        val connectivity = if (voice.isNetworkConnectionRequired) " • Online" else " • Offline"

        val base = listOf(gender, language).filter { it.isNotBlank() }.joinToString(" ")
        return "$base$variant$quality$connectivity".trim()
    }
    fun getFilteredVoiceOptions(languageShort: String, avatar: String): List<String> {
        val shortLang = languageShort.split('-', limit = 2)[0].lowercase()

        // Filter voices by language
        val filteredVoices = _state.value.availableVoices.filter { voice ->
            voice.locale.language.equals(shortLang, ignoreCase = true) &&
                    voice.locale.country.equals("IN", ignoreCase = true)
        }

        return filteredVoices.map { formatVoiceName(it) }
    }
    fun getDefaultVoiceName(languageShort: String, avatar: String): String {
        val shortLang = languageShort.split('-', limit = 2)[0].lowercase()
        val profile = resolveVoiceProfile(avatar)
        // Use the same resolver as the actual pick so the label always agrees.
        return pickDefaultVoice(shortLang, profile)?.let { formatVoiceName(it) } ?: "Default Voice"
    }
    fun setVoice(voice: Voice) {
        textToSpeech?.let { tts ->
            // Stop any ongoing speech
            tts.stop()

            //  Set the new voice
            tts.voice = voice

            //  FORCE the engine to accept the new voice with a silent flush
            val utteranceId = "force_voice_${System.currentTimeMillis()}"
            val bundle = Bundle()
            bundle.putString(AndroidTextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            tts.speak("", AndroidTextToSpeech.QUEUE_FLUSH, bundle, utteranceId)
            _state.value = _state.value.copy(
                selectedVoice = voice,
                selectedVoiceDisplayName = formatVoiceName(voice)
            )

            DebugLogger.debugLog("TTS", "Voice FORCED to: ${formatVoiceName(voice)}")
        } ?: DebugLogger.errorLog("TTS", "Cannot set voice — engine is null")
    }

    /**
     * Clean text for TTS by removing asterisks and markdown formatting
     */
    private fun cleanTextForTTS(text: String): String {
        val withoutMarkers = text
            .replace("**", "")  // Remove bold markers
            .replace("*", "")   // Remove any remaining asterisks
            .trim()
        // Cleanup is language-aware, and applies to EVERY app TTS caller (study/chat agent, the
        // simulation-agent teacher message, intros, …).
        // - Indic (Kannada, Hindi, …): run the full sanitizer, which strips emoji/icons and any stray
        //   symbols/markup (bullets, arrows, brackets, ×, …) and drops grouping commas, so the native
        //   voice reads only the actual words and digits — never non-text glyphs or injected English.
        // - English: spell numbers as words so the engine never reads digit-by-digit ("+1000" →
        //   "plus one thousand", not "plus one oh oh oh").
        val sanitizer = com.ncert7.aitutorandlab.ui.screens.simulation_agent.components
            .SimulationIntroTtsSanitizer
        val isIndic = languagePatterns.values.any { it.containsMatchIn(withoutMarkers) }
        return if (isIndic) {
            sanitizer.forSpeech(withoutMarkers)
        } else {
            sanitizer.spokenNumbers(withoutMarkers)
        }
    }

    /**
     * Speak the given text with lip sync animation
     */
    fun speak(text: String, processedData: ProcessedText? = null) {
        // Don't start talking if the app isn't in the foreground, or while a fullscreen ad is up
        // (e.g. an async result arrives while an ad is showing or the app is backgrounded).
        if (!appInForeground || adShowing) return
        if (!_state.value.isInitialized || text.isBlank()) {
            updateStatus("Error: Cannot speak - TTS not ready or empty text")
            return
        }

        ensureDefaultVoiceApplied()

        currentSpeakingText = text
        currentProcessedData = processedData

        // Clean text for TTS (remove asterisks)
        val cleanedText = cleanTextForTTS(text)

        // If processedData provided,use its word positions
        if (processedData != null) {
            currentSpeakingWords = processedData.wordPositions.map {
                text.substring(it.start, it.end + 1)
            }
            currentWordRanges = processedData.wordPositions.map {
                it.start..it.end
            }
        } else {
            val matches = Regex("\\S+").findAll(cleanedText).toList()
            currentSpeakingWords = matches.map { it.value }
            currentWordRanges = matches.map { it.range.first..it.range.last }
        }
        textToSpeech?.let { tts ->
            _state.value.selectedVoice?.let { preferredVoice ->
                if (tts.voice != preferredVoice) {
                    tts.stop()
                    tts.voice = preferredVoice
                    // Force the engine to accept the new voice with a silent flush
                    val bundle = Bundle()
                    bundle.putString(AndroidTextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, PRE_SPEAK_FLUSH_ID)
                    tts.speak("", AndroidTextToSpeech.QUEUE_FLUSH, bundle, PRE_SPEAK_FLUSH_ID)
                }
            }

            val detectedLang = detectLanguage(cleanedText)
            if (detectedLang != _state.value.selectedLanguage) {
                setLanguageInternal(detectedLang)
            }

            tts.setSpeechRate(_state.value.speechRate)
            tts.setPitch(_state.value.pitch)

            speechStartTime = System.currentTimeMillis()
            totalEstimatedDuration = estimateDuration(cleanedText)
            _state.value = _state.value.copy(
                speakingDurationMs = totalEstimatedDuration,
                speakingText = cleanedText,
            )
            startLipSync(cleanedText)

            val utteranceId = "tts_${System.currentTimeMillis()}"
            val bundle = Bundle()
            bundle.putString(AndroidTextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            tts.speak(cleanedText, AndroidTextToSpeech.QUEUE_FLUSH, bundle, utteranceId)
        }
    }

    /**
     * Stop current speech and lip sync
     */
    fun stop() {
        textToSpeech?.stop()
        stopLipSync()

        // Reset timing
        speechStartTime = 0
        totalEstimatedDuration = 0
        _currentWordIndex.value = -1

        _state.value = _state.value.copy(
            isSpeaking = false,
            speakingDurationMs = 0,
            speakingText = "",
            statusMessage = "Speech stopped",
        )
    }

    /**
     * Update text and detect language
     */
    fun updateText(text: String) {
        val detectedLangCode = detectLanguage(text)
        val langName = languageNames[detectedLangCode] ?: detectedLangCode

        _state.value = _state.value.copy(
            detectedLanguage = langName,
            statusMessage = "Detected language: $langName"
        )
    }

    /**
     * Set speech language
     */
    fun setLanguage(languageCode: String) {
        setLanguageInternal(languageCode)
        val langName = languageNames[languageCode] ?: languageCode
        _state.value = _state.value.copy(
            selectedLanguage = languageCode,
            statusMessage = "Language set to $langName"
        )
    }

    /**
     * Set the app-wide selected language (e.g. from the language picker / chat screen).
     * This becomes the default voice + language and the fallback used when a spoken
     * chunk has no detectable script, so a Kannada session speaks Kannada by default
     * instead of English. Safe to call repeatedly; picks the best default voice once
     * device voices have loaded.
     */
    fun setAppLanguage(avatar: String, languageCode: String) {
        val normalized = when (languageCode.split('-', limit = 2)[0].lowercase()) {
            "kn" -> "kn-IN"
            "hi" -> "hi-IN"
            "ta" -> "ta-IN"
            "te" -> "te-IN"
            else -> "en-IN"
        }
        _state.value = _state.value.copy(appLanguage = normalized)
        // Apply the matching default voice + engine language only once voices exist.
        if (_state.value.availableVoices.isNotEmpty()) {
            applyDefaultsForAvatarLanguage(avatar, normalized)
        } else {
            setLanguageInternal(normalized)
        }
    }

    private fun setLanguageInternal(languageCode: String) {
        textToSpeech?.let { tts ->
            val locale = when (languageCode) {
                "en-IN" -> createLocale("en", "IN")
                "hi-IN" -> createLocale("hi", "IN")
                "kn-IN" -> createLocale("kn", "IN")
                "ta-IN" -> createLocale("ta", "IN")
                "te-IN" -> createLocale("te", "IN")
                else -> createLocale("en", "IN")
            }

            val result = tts.setLanguage(locale)
            if (result == AndroidTextToSpeech.LANG_MISSING_DATA || result == AndroidTextToSpeech.LANG_NOT_SUPPORTED) {
                // Requested voice data isn't installed on this device. Fall back to
                // English audio, but keep selectedLanguage honest (was wrongly set to
                // hi-IN before) and surface a clear, actionable message.
                updateStatus("Voice data for $languageCode isn't installed on this device — install it in Settings > Text-to-speech to hear this language.")
                tts.language = createLocale("en", "IN")
                _state.value = _state.value.copy(selectedLanguage = "en-IN")
            } else {
                _state.value = _state.value.copy(selectedLanguage = languageCode)
            }
        }
    }

    /**
     * Get current playback position in milliseconds
     * Estimates position based on elapsed time since speech started
     */
    fun getCurrentPosition(): Float {
        return if (_state.value.isSpeaking && speechStartTime > 0) {
            val elapsed = System.currentTimeMillis() - speechStartTime
            elapsed.toFloat()
        } else {
            0f
        }
    }

    /**
     * Estimate total duration of text in milliseconds
     * Average speaking rate: ~150 words per minute = 2.5 words/second = 400ms/word
     */
    private fun estimateDuration(text: String): Long {
        val wordCount = text.split("\\s+".toRegex()).size
        val baseRate = 400L// milliseconds per word
        // Adjust for speech rate
        val adjustedRate = (baseRate / _state.value.speechRate).toLong()
        return wordCount * adjustedRate
    }

    fun getEstimatedDurationMs(text: String): Long {
        return estimateDuration(text)
    }
    /**
     * Set speech rate
     */
    fun setSpeechRate(rate: Float) {
        _state.value = _state.value.copy(speechRate = rate)
    }
    /**
     * Set speech pitch
     */
    fun setPitch(pitch: Float) {
        _state.value = _state.value.copy(pitch = pitch)
    }

    fun injectCharacterImages(context: Context) {
        val boyBase64 = context.assets.open("images/boy.png").use {
            android.util.Base64.encodeToString(it.readBytes(), android.util.Base64.NO_WRAP)
        }
        val girlBase64 = context.assets.open("images/girl.png").use {
            android.util.Base64.encodeToString(it.readBytes(), android.util.Base64.NO_WRAP)
        }

        webView?.evaluateJavascript(
            """
        window.lipSync.setImageData({
            boy: "data:image/png;base64,$boyBase64",
            girl: "data:image/png;base64,$girlBase64"
        });
        """.trimIndent(),
            null
        )
    }
    /**
     * Switch character in lip sync animation
     */
    fun switchCharacter(character: String) {
        val profile = resolveVoiceProfile(character)
        _state.value = _state.value.copy(selectedCharacter = profile)
        if (nativeLipSyncEnabled) return

        webView?.post {
            webView?.evaluateJavascript(
                "window.AndroidLipSyncAPI.switchCharacter('$character')"
            ) { result ->
                DebugLogger.debugLog("LipSync", "Character switch result: $result")
            }
        }
    }
    /**
     * Toggle debug mode in lip sync
     */
    fun toggleDebug() {
        val newDebugMode = !_state.value.debugMode
        _state.value = _state.value.copy(debugMode = newDebugMode)

        webView?.post {
            webView?.evaluateJavascript(
                "window.AndroidLipSyncAPI.toggleDebug($newDebugMode)"
            ) { result ->
                DebugLogger.debugLog("LipSync", "Debug toggle result: $result")
            }
        }
    }
    /**
     * Test lip sync animation
     */
    fun testAnimation() {
        webView?.post {
            webView?.evaluateJavascript(
                "window.AndroidLipSyncAPI.testAnimation()"
            ) { result ->
                DebugLogger.debugLog("LipSync", "Test animation result: $result")
            }
        }
    }
    /**
     * Start lip sync animation
     */
    private fun startLipSync(text: String) {
        if (nativeLipSyncEnabled) return
        val escapedText = text.replace("'", "\\'").replace("\n", "\\n")
        val speechRate = _state.value.speechRate
        webView?.post {
            webView?.evaluateJavascript(
                "window.AndroidLipSyncAPI.startLipSync('$escapedText', $speechRate)"
            ) { result ->
                DebugLogger.debugLog("LipSync", "Start lip sync result: $result")
            }
        }
    }
    /**
     * Stop lip sync animation
     */
    private fun stopLipSync() {
        if (nativeLipSyncEnabled) return
        webView?.post {
            webView?.evaluateJavascript(
                "window.AndroidLipSyncAPI.stopLipSync()"
            ) { result ->
                DebugLogger.debugLog("LipSync", "Stop lip sync result: $result")
            }
        }
    }
    /**
     * Detect language from text
     */
    private fun detectLanguage(text: String): String {
        if (text.isBlank()) return _state.value.appLanguage
        // Check for Indic scripts
        for ((langCode, pattern) in languagePatterns) {
            if (pattern.find(text) != null) {
                return langCode
            }
        }
        // No Indic script found — fall back to the app-selected language rather than
        // hard-defaulting to English, so a Kannada session keeps a Kannada voice.
        return _state.value.appLanguage
    }

    /**
     * Update status message
     */
    private fun updateStatus(message: String) {
        _state.value = _state.value.copy(statusMessage = message)
        DebugLogger.debugLog("TTS", message)
    }

    /**
     * Resolve the default voice for a language + avatar profile. Robust to device
     * voice-name variations: matches on the variant code (ENE for English, KND for
     * Kannada) and prefers the OFFLINE/local voice, rather than requiring an exact
     * engine-name string like "en-in-x-ene-local". Falls back to the mapped engine
     * names, then gender/language/any. Used by both the actual voice pick and the
     * settings label so they always agree.
     */
    private fun pickDefaultVoice(shortLang: String, profile: String): Voice? {
        val indianVoices = _state.value.availableVoices.filter { voice ->
            val localeStr = voice.locale.toString().lowercase()
            localeStr.contains("_in") || localeStr.contains("-in")
        }
        val langVoices = indianVoices.filter { it.locale.language.equals(shortLang, ignoreCase = true) }

        // 1) Preferred default by variant code + offline preference.
        val desiredCode =
            when (profile.lowercase()) {
                "tutor", "disable", "boy" ->
                    when (shortLang) {
                        "en" -> "ene"
                        "kn" -> "knd"
                        else -> null
                    }
                "girl" ->
                    when (shortLang) {
                        "en" -> "ena"
                        else -> null
                    }
                else -> null
            }
        if (desiredCode != null) {
            (langVoices.firstOrNull {
                !it.isNetworkConnectionRequired && it.engineVariantCode() == desiredCode
            } ?: langVoices.firstOrNull { it.engineVariantCode() == desiredCode })
                ?.let { return it }
        }

        // 2) Mapped engine names (falls back to the tutor list for unmapped profiles).
        val preferredNames = preferredVoiceNames(shortLang, profile)
            .ifEmpty { preferredVoiceNames(shortLang, "tutor") }
        preferredNames.firstNotNullOfOrNull { prefName ->
            indianVoices.find { it.name.contains(prefName, ignoreCase = true) }
        }?.let { return it }

        // 3) Gender match (for legacy boy/girl), then offline, then any.
        val genderMatches: (Voice) -> Boolean = { voice ->
            val features = voice.features?.map { it.lowercase() } ?: emptyList()
            when (profile.lowercase()) {
                "boy" -> features.any { it.contains("male") } && !features.any { it.contains("female") }
                "girl" -> features.any { it.contains("female") }
                else -> true
            }
        }
        langVoices.firstOrNull { genderMatches(it) }?.let { return it }
        langVoices.firstOrNull { !it.isNetworkConnectionRequired }?.let { return it }
        return langVoices.firstOrNull() ?: indianVoices.firstOrNull()
    }

    private fun ensureDefaultVoiceApplied() {
        if (!_state.value.voicesFullyLoaded || _state.value.selectedVoice != null) return
        val defaultAvatar = if (nativeLipSyncEnabled) "tutor" else _state.value.selectedCharacter
        applyDefaultsForAvatarLanguage(defaultAvatar, _state.value.appLanguage)
    }

    fun applyDefaultsForAvatarLanguage(avatar: String, languageCode: String) {
        val shortLang = languageCode.split('-', limit = 2)[0].lowercase()
        val profile = resolveVoiceProfile(avatar)

        // Resolve the default voice (variant code + offline preference, robust to device
        // voice-name variations — see pickDefaultVoice).
        val chosen: Voice? = pickDefaultVoice(shortLang, profile)

        // Apply the selected voice
        chosen?.let { voice ->
            setVoice(voice) // already contains the flush now

            // Extra safety — if something is currently speaking, restart it with the new voice
            if (_state.value.isSpeaking && currentSpeakingText.isNotBlank()) {
                // small delay so the flush finishes
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    speak(currentSpeakingText)
                }, 300)
            }
        }

        // Set language
        val langCode = when (shortLang) {
            "en" -> "en-IN"
            "kn" -> "kn-IN"
            "hi" -> "hi-IN"
            "ta" -> "ta-IN"
            "te" -> "te-IN"
            else -> "en-IN"
        }
        setLanguageInternal(langCode)

        setSpeechRate(1.0f)
        setPitch(1.0f)
        _state.value = _state.value.copy(
            speechRate = 1.0f,
            pitch = 1.0f,
            statusMessage = "Default voice applied: ${chosen?.let { formatVoiceName(it) } ?: "none"}"
        )

        updateStatus("Default voice applied: ${chosen?.let { formatVoiceName(it) } ?: "none"}")
        DebugLogger.debugLog("TTS", "Applying defaults with ${_state.value.availableVoices.size} filtered voices")
        DebugLogger.debugLog(
            "TTS",
            "Applied defaults: lang=$shortLang, avatar=$profile, voice=${chosen?.name ?: "none"}, speed=1.0x, pitch=1.0x"
        )
    }
    /**
     * Cleanup resources
     */
    fun cleanup() {
        unregister(this)
        if (lifecycleCallbacksRegistered) {
            application?.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
            lifecycleCallbacksRegistered = false
            application = null
        }
        textToSpeech?.let { tts ->
            tts.stop()
            tts.shutdown()
        }
        textToSpeech = null
        webView = null
        _state.value = _state.value.copy(
            isInitialized = false,
            isSpeaking = false,
            statusMessage = "Text-to-Speech resources released"
        )
    }

    /**
     * Create a Locale using the modern API (handles deprecation)
     * Uses Locale.Builder for API 21+, falls back to constructor for older APIs
     */
    private fun createLocale(language: String, country: String): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Modern way (API 21+)
            Locale.Builder()
                .setLanguage(language)
                .setRegion(country)
                .build()
        } else {
            // Fallback for older APIs (below API 21)
            @Suppress("DEPRECATION")
            Locale(language, country)
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}
