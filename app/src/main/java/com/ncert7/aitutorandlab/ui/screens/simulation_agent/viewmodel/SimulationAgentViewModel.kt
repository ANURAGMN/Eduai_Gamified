package com.ncert7.aitutorandlab.ui.screens.simulation_agent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.remote.SimSessionResponse
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.domain.chatbot.usecase.AvatarChangeUseCase
import com.ncert7.aitutorandlab.domain.examplan.PlanTrialProgressTracker
import com.ncert7.aitutorandlab.domain.examplan.TrialSessionStore
import com.ncert7.aitutorandlab.domain.progress.ProgressEventTracker
import com.ncert7.aitutorandlab.domain.simulation.model.SimulationScreenState
import com.ncert7.aitutorandlab.domain.simulation.usecase.SimulationIntent
import com.ncert7.aitutorandlab.domain.simulation.usecase.LoadSimulationsUseCase
import com.ncert7.aitutorandlab.domain.simulation.usecase.SendSimulationResponseUseCase
import com.ncert7.aitutorandlab.domain.simulation.usecase.SimulationSessionUseCase
import com.ncert7.aitutorandlab.domain.simulation.usecase.SimulationInfo
import com.ncert7.aitutorandlab.repository.ConceptRepository
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatBotSettingsState
import com.ncert7.aitutorandlab.ui.viewModel.TextToSpeech
import com.ncert7.aitutorandlab.utils.ErrorHandler
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import com.ncert7.aitutorandlab.utils.isKannada
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Simulation Agent screen
 * Contains ALL business logic - UI is purely presentational
 */
@HiltViewModel
class SimulationAgentViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedPrefs: SharedPreferenceUtils,
    private val avatarChangeUseCase: AvatarChangeUseCase,
    private val simulationSessionUseCase: SimulationSessionUseCase,
    private val sendSimulationResponseUseCase: SendSimulationResponseUseCase,
    private val loadSimulationsUseCase: LoadSimulationsUseCase,
    private val progressEventTracker: ProgressEventTracker,
    private val planTrialProgressTracker: PlanTrialProgressTracker,
    private val conceptRepository: ConceptRepository
) : ViewModel() {

    // API/Session State
    private val _uiState = MutableStateFlow<SimAgentUiState>(SimAgentUiState.Initial)
    val uiState: StateFlow<SimAgentUiState> = _uiState.asStateFlow()

    private val _sessionData = MutableStateFlow<SimSessionResponse?>(null)
    val sessionData: StateFlow<SimSessionResponse?> = _sessionData.asStateFlow()

    // Simulations List
    private val _availableSimulations = MutableStateFlow<List<SimulationInfo>>(emptyList())
    val availableSimulations: StateFlow<List<SimulationInfo>> = _availableSimulations.asStateFlow()

    private val _simulationsLoading = MutableStateFlow(false)
    val simulationsLoading: StateFlow<Boolean> = _simulationsLoading.asStateFlow()

    // UI Control State - ALL UI logic managed here
    private val _currentTeacherMessage = MutableStateFlow("")
    val currentTeacherMessage: StateFlow<String> = _currentTeacherMessage.asStateFlow()

    private val _showWebView = MutableStateFlow(false)
    val showWebView: StateFlow<Boolean> = _showWebView.asStateFlow()

    private val _simulationUrls = MutableStateFlow<List<String>>(emptyList())
    val simulationUrls: StateFlow<List<String>> = _simulationUrls.asStateFlow()

    private val _isSessionStarted = MutableStateFlow(false)
    val isSessionStarted: StateFlow<Boolean> = _isSessionStarted.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _userInput = MutableStateFlow("")
    val userInput: StateFlow<String> = _userInput.asStateFlow()

    // TTS/Speech State Management
    private val _isTtsSpeaking = MutableStateFlow(false)
    val isTtsSpeaking: StateFlow<Boolean> = _isTtsSpeaking.asStateFlow()

    private val _hasSpokeCurrentMessage = MutableStateFlow(false)
    val hasSpokeCurrentMessage: StateFlow<Boolean> = _hasSpokeCurrentMessage.asStateFlow()

    // NEW: Track if TTS should be triggered for current message
    private val _shouldTriggerTts = MutableStateFlow(false)
    val shouldTriggerTts: StateFlow<Boolean> = _shouldTriggerTts.asStateFlow()

    // Input Enabling Logic
    private val _isInputEnabled = MutableStateFlow(true)
    val isInputEnabled: StateFlow<Boolean> = _isInputEnabled.asStateFlow()

    // WebView delay job
    private var webViewDelayJob: Job? = null

    // NEW: Track current simulation ID to prevent re-starting on config change
    private var currentSimulationId: String? = null

    // Language State for STT
    private val _currentLanguage = MutableStateFlow("")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    // Track changed parameters from webview
    private val _changedSimulationParams = MutableStateFlow<Map<String, Any>?>(null)
    val changedSimulationParams: StateFlow<Map<String, Any>?> = _changedSimulationParams.asStateFlow()

    // Session Dialog State - for continuing existing session or starting new
    private val _showSessionResumeDialog = MutableStateFlow(false)
    val showSessionResumeDialog: StateFlow<Boolean> = _showSessionResumeDialog.asStateFlow()

    private val _pendingSimulationForDialog = MutableStateFlow<String?>(null)
    val pendingSimulationForDialog: StateFlow<String?> = _pendingSimulationForDialog.asStateFlow()

    // Track the conceptId associated with this simulation
    private var currentConceptId: String? = null

    //Consolidated screen state for unified rendering
    private val _screenState = MutableStateFlow(SimulationScreenState())
    val screenState: StateFlow<SimulationScreenState> = _screenState.asStateFlow()

    companion object {
        private const val TAG = "SimulationAgentVM"
        private const val WEBVIEW_DELAY_MS = 300L
    }

    init {
        // Initialize language state
        _currentLanguage.value = if (isKannada()) "kn-IN" else "en-IN"

        // Update input enabled state whenever TTS or loading state changes
        viewModelScope.launch {
            uiState.collect { state ->
                updateInputEnabledState(state)
                updateScreenState()
            }
        }

        viewModelScope.launch {
            isTtsSpeaking.collect {
                updateInputEnabledState(uiState.value)
                updateScreenState()
            }
        }

        // Sync all state flows to screenState whenever they change
        viewModelScope.launch {
            currentTeacherMessage.collect {
                updateScreenState()
            }
        }

        viewModelScope.launch {
            simulationUrls.collect {
                updateScreenState()
            }
        }

        viewModelScope.launch {
            isSessionStarted.collect {
                updateScreenState()
            }
        }

        viewModelScope.launch {
            errorMessage.collect {
                updateScreenState()
            }
        }

        viewModelScope.launch {
            userInput.collect {
                updateScreenState()
            }
        }

        viewModelScope.launch {
            isInputEnabled.collect {
                updateScreenState()
            }
        }

        viewModelScope.launch {
            shouldTriggerTts.collect {
                updateScreenState()
            }
        }

        viewModelScope.launch {
            hasSpokeCurrentMessage.collect {
                updateScreenState()
            }
        }

        viewModelScope.launch {
            showWebView.collect {
                updateScreenState()
            }
        }

        viewModelScope.launch {
            currentLanguage.collect {
                updateScreenState()
            }
        }

        viewModelScope.launch {
            sessionData.collect {
                updateScreenState()
            }
        }
    }

    /**
     * Sync all individual state flows into the consolidated screenState
     * This ensures the UI has a single source of truth
     */
    private fun updateScreenState() {
        _screenState.value = SimulationScreenState(
            isSessionStarted = _isSessionStarted.value,
            isLoading = _uiState.value is SimAgentUiState.Loading,
            errorMessage = _errorMessage.value,
            currentTeacherMessage = _currentTeacherMessage.value,
            simulationUrls = _simulationUrls.value,
            showWebView = _showWebView.value,
            hasSimulationHTMLUrl = _sessionData.value?.simulation?.htmlUrl?.isNotBlank() == true,
            userInput = _userInput.value,
            isInputEnabled = _isInputEnabled.value,
            shouldTriggerTts = _shouldTriggerTts.value,
            hasSpokeCurrentMessage = _hasSpokeCurrentMessage.value,
            sessionData = _sessionData.value,
            currentLanguage = _currentLanguage.value
        )
    }

    /**
     * Public API UI Logic
     */

    /**
     * Handle user intents dispatched from UI
     * This is the main entry point for all user actions
     */
    fun handleIntent(intent: SimulationIntent) {
        when (intent) {
            is SimulationIntent.SendUserResponse -> {
                val spoken = intent.text.trim()
                if (spoken.isNotEmpty()) {
                    _userInput.value = spoken
                }
                onSendClick()
            }
            is SimulationIntent.UpdateInput -> onUserInputChanged(intent.text)
            is SimulationIntent.ParametersChanged -> onSimulationParamsChanged(intent.params)
            is SimulationIntent.OnBackPressed -> onBackPressed()
            is SimulationIntent.TtsStarted -> onTtsStarted()
            is SimulationIntent.TtsStopped -> onTtsStopped()
            is SimulationIntent.TtsTriggered -> onTtsTriggered()
            is SimulationIntent.LoadSimulations -> loadAvailableSimulations()
            is SimulationIntent.StartNewSession -> startNewSession(intent.simulationId)
            is SimulationIntent.RetrySession -> onRetryClick(intent.simulationId)
            is SimulationIntent.AvatarChanged -> onAvatarChanged()
            is SimulationIntent.VoiceChanged -> onVoiceChanged()
            is SimulationIntent.SpeedChanged -> onSpeedChanged()
            is SimulationIntent.SubmitQuizAnswer -> submitQuizAnswer(intent.answer)
            is SimulationIntent.DismissSessionDialog -> dismissSessionDialog()
            is SimulationIntent.ContinueExistingSession -> continueExistingSession(intent.simulationId)
            is SimulationIntent.StartFreshSession -> startFreshSession(intent.simulationId)
        }
    }

    /**
     * Public API UI Logic
     */
    /**
     * Called when user types in input field
     */
    fun onUserInputChanged(text: String) {
        _userInput.value = text
    }

    /**
     * Called when user clicks send button
     */
    fun onSendClick() {
        val input = _userInput.value.trim()
        if (input.isBlank() || !_isInputEnabled.value) {
            return
        }

        // Clear input IMMEDIATELY — keep the simulation WebView mounted while awaiting reply
        _userInput.value = ""

        // Send response
        sendStudentResponse(input)
    }

    /**
     * Called when TTS starts speaking
     */
    fun onTtsStarted() {
        _isTtsSpeaking.value = true
        _hasSpokeCurrentMessage.value = false
        // Reset the trigger flag once TTS has started
        _shouldTriggerTts.value = false
    }

    /**
     * Called when TTS stops speaking (either finished or manually stopped)
     */
    fun onTtsStopped() {
        _isTtsSpeaking.value = false

        // If message hasn't been spoken yet, trigger webview display
        if (!_hasSpokeCurrentMessage.value &&
            _currentTeacherMessage.value.isNotEmpty() &&
            _simulationUrls.value.isNotEmpty()
        ) {
            scheduleWebViewDisplay()
        }
    }

    /**
     * Acknowledge that TTS was triggered - prevents re-triggering on config change
     */
    fun onTtsTriggered() {
        _shouldTriggerTts.value = false
    }

    /**
     * Called when WebView close button is clicked
     */
    fun onWebViewClose() {
        _showWebView.value = false
    }

    /**
     * Called when simulation parameters are changed in the webview
     * Example: user changes pendulum length from 5 to 6
     */
    fun onSimulationParamsChanged(changedParams: Map<String, Any>) {
        _changedSimulationParams.value = changedParams
        DebugLogger.debugLog(TAG, "Simulation parameters changed: $changedParams")
    }

    /**
     * Called when back button is pressed
     */
    fun onBackPressed(): Boolean {
        return if (_showWebView.value) {
            _showWebView.value = false
            true // consumed
        } else {
            resetSession()
            false // not consumed - navigate back
        }
    }

    /**
     * Called when retry button is clicked in error state
     */
    fun onRetryClick(simulationId: String) {
        _errorMessage.value = null
        currentSimulationId = null // Reset to allow retry
        startNewSession(simulationId)
    }

    /**
     * Called when settings change avatar
     */
    fun onAvatarChanged() {
        // If TTS is speaking, it will be stopped and restarted by TTS controller
        // We just need to prevent showing webview again
        _hasSpokeCurrentMessage.value = true
    }

    /**
     * Called when settings change voice
     */
    fun onVoiceChanged() {
        // If TTS is speaking, it will be stopped and restarted by TTS controller
        // We just need to prevent showing webview again
        _hasSpokeCurrentMessage.value = true
    }

    /**
     * Called when settings change speed
     */
    fun onSpeedChanged() {
        // If TTS is speaking, it will be stopped and restarted by TTS controller
        // We just need to prevent showing webview again
        _hasSpokeCurrentMessage.value = true
    }

    /**
     * Business Logic
     */

    /**
     * Update input enabled state based on loading and TTS state
     */
    private fun updateInputEnabledState(currentUiState: SimAgentUiState) {
        _isInputEnabled.value = currentUiState !is SimAgentUiState.Loading && !_isTtsSpeaking.value
    }

    /**
     * Schedule webview display after delay
     */
    private fun scheduleWebViewDisplay() {
        webViewDelayJob?.cancel()
        webViewDelayJob = viewModelScope.launch {
            delay(WEBVIEW_DELAY_MS)
            _hasSpokeCurrentMessage.value = true
            _showWebView.value = true
        }
    }

    /**
     * Process new session response
     */
    private fun processSessionResponse(response: SimSessionResponse) {
        // Check if this is a new message
        val isNewMessage = _currentTeacherMessage.value != response.teacherMessage.text

        if (isNewMessage) {
            // New message - reset state
            _showWebView.value = false
            _hasSpokeCurrentMessage.value = false
            _currentTeacherMessage.value = response.teacherMessage.text

            // IMPORTANT: Set flag to trigger TTS in UI
            _shouldTriggerTts.value = true

            // Process simulation URLs
            val paramChange = response.simulation.paramChange
            if (paramChange != null) {
                // Use the URL properties if available, otherwise use the main URL
                _simulationUrls.value = if (paramChange.beforeUrl != null && paramChange.afterUrl != null) {
                    listOf(
                        paramChange.beforeUrl,
                        paramChange.afterUrl
                    )
                } else {
                    // If URLs are not provided, just use the main simulation URL
                    listOf(response.simulation.htmlUrl)
                }
            } else {
                _simulationUrls.value = listOf(response.simulation.htmlUrl)
            }

            _isSessionStarted.value = true

            DebugLogger.debugLog(TAG, " New teacher message processed:")
            DebugLogger.debugLog(TAG, "  Message: ${response.teacherMessage.text}")
            DebugLogger.debugLog(TAG, "  Has param change: ${paramChange != null}")
            DebugLogger.debugLog(TAG, "  URL count: ${_simulationUrls.value.size}")
        } else {
            DebugLogger.debugLog(TAG, "Same message - no state change needed")
        }
        checkTrialGeProgress(response)
    }

    /**
     * Soft exit from the time-based proceed overlay.
     * Leaves the trial item as-is — completion is only via real GE / bite progress,
     * not wall-clock time (which was awarding "Level cleared!" with no learning).
     * @return true if the item is already DONE (caller must not soft-proceed past celebration).
     */
    suspend fun recordTrialProceed(): Boolean {
        val trialItemId = TrialSessionStore.activeTrialItemId ?: return false
        planTrialProgressTracker.reconcileCompletion(trialItemId)
        val done = planTrialProgressTracker.isDone(trialItemId)
        DebugLogger.debugLog(TAG, "Trial sim agent soft proceed for item $trialItemId done=$done")
        return done
    }

    private fun checkTrialGeProgress(response: SimSessionResponse) {
        val trajectory = response.learningState.trajectoryStatus?.uppercase()?.trim()
        if (trajectory != "GE") return

        val trialItemId = TrialSessionStore.activeTrialItemId
        viewModelScope.launch {
            if (trialItemId != null) {
                planTrialProgressTracker.recordGeReached(trialItemId)
                DebugLogger.debugLog(TAG, "Trial sim agent reached GE for item $trialItemId")
            }
            // Chapter progress: only when the goal is achieved, not when the session opens.
            val conceptId = currentConceptId
            val studentId = sharedPrefs.getUserId()
            if (conceptId != null && studentId != null) {
                val lang = getCurrentLanguageCode()
                progressEventTracker.markSimulationAgentCompleted(studentId, conceptId, lang)
                DebugLogger.debugLog(TAG, "Marked Simulation Agent completed on GE for concept: $conceptId [$lang]")
            }
        }
    }

    fun setConceptId(conceptId: String) {
        if (conceptId.isNotBlank()) {
            currentConceptId = conceptId
            DebugLogger.debugLog(TAG, "Concept ID set to: $conceptId")
        }
    }

    /**
     * Reset session data (for back navigation)
     */
    private fun resetSessionForNavigation() {
        currentSimulationId = null
        currentConceptId = null
        _sessionData.value = null
        _uiState.value = SimAgentUiState.Initial
        _currentTeacherMessage.value = ""
        _showWebView.value = false
        _simulationUrls.value = emptyList()
        _isSessionStarted.value = false
        _errorMessage.value = null
        _userInput.value = ""
        _hasSpokeCurrentMessage.value = false
        _shouldTriggerTts.value = false
        webViewDelayJob?.cancel()
        DebugLogger.debugLog(TAG, "Session reset for navigation")
    }

    /**
     * ERROR HANDLING
     */

    /**
     * Handle exceptions
     */
    private fun handleError(e: Exception, operation: String): String {
        return ErrorHandler.handleException(context, e, operation, TAG)
    }

    /**
     * API OPERATIONS
     */

    /**
     * Simulation iframe finished loading — does not advance chapter progress.
     * Progress is recorded when the learning trajectory reaches GE (goal achieved).
     */
    fun onSimulationUrlLoaded(simulationId: String) {
        DebugLogger.debugLog(TAG, "Simulation URL loaded for $simulationId (chapter progress deferred until GE)")
    }

    /**
     * Helper function to fetch conceptId from simulationId
     * Needed for progress tracking
     * Queries concepts table by matching simulationId field
     */
    private suspend fun fetchConceptIdForSimulation(simulationId: String): String? {
        return try {
            withContext(Dispatchers.IO) {
                // Get all concepts and find the one with matching simulationId
                val allConcepts = conceptRepository.getAllConcepts()
                val concept = allConcepts.find {
                    it.simulationId == simulationId || it.simulationIdKannada == simulationId
                }
                concept?.conceptId
            }
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "Error fetching conceptId for simulation $simulationId: ${e.message}")
            null
        }
    }

    /**
     * When the agent session returns a blank html_url (common for some Science chapters),
     * fall back to the concept catalog simulation URL so the WebView still loads.
     */
    private suspend fun applyConceptHtmlFallback(
        simulationId: String,
        response: SimSessionResponse,
    ): SimSessionResponse {
        if (response.simulation.htmlUrl.isNotBlank()) return response
        return try {
            val conceptId =
                currentConceptId
                    ?: fetchConceptIdForSimulation(simulationId)
                    ?: return response
            val concept =
                withContext(Dispatchers.IO) { conceptRepository.getConcept(conceptId) }
                    ?: return response
            val lang = getCurrentLanguageCode()
            val fallback =
                if (lang == "kn") {
                    concept.simulationUrlKannada?.takeIf { it.isNotBlank() }
                        ?: concept.simulationUrl
                } else {
                    concept.simulationUrl?.takeIf { it.isNotBlank() }
                        ?: concept.simulationUrlKannada
                }?.takeIf { it.isNotBlank() }
                    ?: return response
            DebugLogger.debugLog(
                TAG,
                "Agent html_url blank — using concept simulationUrl for $simulationId",
            )
            response.copy(simulation = response.simulation.copy(htmlUrl = fallback))
        } catch (e: Exception) {
            DebugLogger.warnLog(TAG, "Concept HTML fallback failed: ${e.message}")
            response
        }
    }

    /**
     * Load all available simulations from the API
     */
    fun loadAvailableSimulations() {
        viewModelScope.launch {
            try {
                _simulationsLoading.value = true
                DebugLogger.debugLog(TAG, "Loading available simulations...")

                val result = loadSimulationsUseCase.loadSimulations()
                if (result.isSuccess) {
                    _availableSimulations.value = result.getOrNull() ?: emptyList()
                    DebugLogger.debugLog(TAG, "Loaded ${_availableSimulations.value.size} simulations")
                } else {
                    throw result.exceptionOrNull() ?: Exception("Failed to load simulations")
                }

            } catch (e: Exception) {
                DebugLogger.errorLog(TAG, " Failed to load simulations: ${e.message}")
                // Fallback to default simulations
                _availableSimulations.value = listOf(
                    SimulationInfo("simple_pendulum", "Simple Pendulum", ""),
                    SimulationInfo("earth_rotation_revolution", "Earth Rotation & Revolution", ""),
                    SimulationInfo("light_shadows", "Light & Shadows", "")
                )
            } finally {
                _simulationsLoading.value = false
            }
        }
    }

    /**
     * Start a new teaching session
     * IMPORTANT: Only starts if simulation ID has changed (prevents re-start on config change)
     * Shows dialog if session already exists for this simulation
     */
    fun startNewSession(simulationId: String) {
        // Check if we're already in this simulation session
        if (currentSimulationId == simulationId && _sessionData.value != null) {
            DebugLogger.debugLog(TAG, "Session already active for $simulationId")
            // Show dialog to ask continue or start fresh
            _pendingSimulationForDialog.value = simulationId
            _showSessionResumeDialog.value = true
            DebugLogger.debugLog(TAG, "Showing session resume dialog")
            return
        }

        // Check if there's a saved session for this simulation ID
        if (simulationSessionUseCase.hasExistingSession(simulationId)) {
            DebugLogger.debugLog(TAG, "Found existing session for $simulationId")
            _pendingSimulationForDialog.value = simulationId
            _showSessionResumeDialog.value = true
            DebugLogger.debugLog(TAG, "Showing session resume dialog for saved session")
            return
        }

        currentSimulationId = simulationId
        performStartNewSession(simulationId)
    }

    /**
     * Continue with existing session
     * Loads the stored session ID and resumes with history
     */
    fun continueExistingSession(simulationId: String) {
        DebugLogger.debugLog(TAG, "User chose to continue existing session for $simulationId")
        dismissSessionDialog()

        val existingSessionId = simulationSessionUseCase.getSessionId(simulationId)
        if (existingSessionId != null) {
            DebugLogger.debugLog(TAG, "Resuming session: $existingSessionId")
            currentSimulationId = simulationId
            performResumeSession(simulationId, existingSessionId)
        } else {
            DebugLogger.errorLog(TAG, "No saved session found for $simulationId")
            // Fallback to starting new session
            currentSimulationId = simulationId
            performStartNewSession(simulationId)
        }
    }

    /**
     * Start fresh session
     * Clears old session mapping and starts new one
     */
    fun startFreshSession(simulationId: String) {
        DebugLogger.debugLog(TAG, "User chose to start fresh session for $simulationId")
        dismissSessionDialog()

        // Delete existing session mapping
        simulationSessionUseCase.clearSession(simulationId)

        currentSimulationId = simulationId
        performStartNewSession(simulationId)
    }

    /**
     * Clear session mapping without starting a new session
     */
    fun clearSessionMapping(simulationId: String) {
        simulationSessionUseCase.clearSession(simulationId)
    }

    /**
     * Dismiss session resume dialog
     */
    fun dismissSessionDialog() {
        _showSessionResumeDialog.value = false
        _pendingSimulationForDialog.value = null
        DebugLogger.debugLog(TAG, "Session dialog dismissed")
    }

    /**
     * Internal method to actually start the session and save mapping
     */
    private fun performStartNewSession(simulationId: String) {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                _sessionData.value = null
                _currentTeacherMessage.value = ""
                _uiState.value = SimAgentUiState.Loading
                DebugLogger.debugLog(TAG, "Starting new session for simulation: $simulationId")

                val result = simulationSessionUseCase.startNewSession(simulationId)

                if (result.isSuccess) {
                    val response = applyConceptHtmlFallback(
                        simulationId,
                        result.getOrNull()!!,
                    )
                    DebugLogger.debugLog(TAG, "Session started successfully")
                    DebugLogger.debugLog(TAG, "Session ID: ${response.sessionId}")
                    DebugLogger.debugLog(TAG, "Teacher Message: ${response.teacherMessage.text}")
                    DebugLogger.debugLog(TAG, "Simulation URL: ${response.simulation.htmlUrl}")


                    // Track concept id for later GE completion — do NOT mark chapter progress on session start.
                    val conceptId = fetchConceptIdForSimulation(simulationId)
                    if (conceptId != null) {
                        currentConceptId = conceptId
                        DebugLogger.debugLog(TAG, "Session started for concept $conceptId (progress deferred until GE)")
                    } else {
                        DebugLogger.errorLog(TAG, "Could not find conceptId for simulationId: $simulationId")
                    }

                    _sessionData.value = response
                    processSessionResponse(response)
                    _errorMessage.value = null
                    _uiState.value = SimAgentUiState.Success(response)
                } else {
                    throw result.exceptionOrNull() ?: Exception("Failed to start simulation session")
                }

            } catch (e: Exception) {
                val errorMsg = handleError(e, "start_session")
                _errorMessage.value = errorMsg
                _uiState.value = SimAgentUiState.Error(errorMsg)
            }
        }
    }

    /**
     * Internal method to resume an existing session with history
     */
    private fun performResumeSession(simulationId: String, sessionId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = SimAgentUiState.Loading
                DebugLogger.debugLog(TAG, "Resuming session for simulation: $simulationId with session ID: $sessionId")

                val result = simulationSessionUseCase.resumeExistingSession(simulationId)

                if (result.isSuccess) {
                    val response = result.getOrNull()!!
                    DebugLogger.debugLog(TAG, "Session resumed successfully")
                    DebugLogger.debugLog(TAG, "Session ID: ${response.sessionId}")
                    DebugLogger.debugLog(TAG, "Teacher Message: ${response.teacherMessage.text}")
                    DebugLogger.debugLog(TAG, "Exchange count: ${response.learningState.exchangeCount}")

                    _sessionData.value = response
                    processSessionResponse(response)
                    _uiState.value = SimAgentUiState.Success(response)
                } else {
                    throw result.exceptionOrNull() ?: Exception("Failed to resume simulation session")
                }

            } catch (e: Exception) {
                val errorMsg = handleError(e, "resume_session")
                DebugLogger.errorLog(TAG, "Failed to resume session: $errorMsg")
                _errorMessage.value = errorMsg
                _uiState.value = SimAgentUiState.Error(errorMsg)
                // Fallback to starting new session
                performStartNewSession(simulationId)
            }
        }
    }

    /**
     * Send a student response to the current session
     */
    private fun sendStudentResponse(response: String) {
        val currentSessionId = _sessionData.value?.sessionId
        if (currentSessionId == null) {
            val errorMsg = "No active session. Please restart the simulation."
            DebugLogger.errorLog(TAG, "No active session")
            _errorMessage.value = errorMsg
            _uiState.value = SimAgentUiState.Error(errorMsg)
            return
        }

        viewModelScope.launch {
            try {
                _errorMessage.value = null
                _uiState.value = SimAgentUiState.Loading
                DebugLogger.debugLog(TAG, "Sending student response: $response")

                // Get changed parameters if any
                val changedParams = _changedSimulationParams.value
                if (changedParams != null) {
                    DebugLogger.debugLog(TAG, "Student changed parameters: $changedParams")
                }

                val result = sendSimulationResponseUseCase.sendResponse(
                    sessionId = currentSessionId,
                    studentResponse = response,
                    changedParams = changedParams
                )

                if (result.isSuccess) {
                    // Clear changed parameters after sending
                    _changedSimulationParams.value = null

                    val apiResponse = result.getOrNull()!!
                    DebugLogger.debugLog(TAG, " Response received successfully")
                    DebugLogger.debugLog(TAG, "Teacher Message: ${apiResponse.teacherMessage.text}")
                    DebugLogger.debugLog(TAG, "Understanding Level: ${apiResponse.learningState.understandingLevel}")

                    apiResponse.simulation.paramChange?.let { change ->
                        DebugLogger.debugLog(TAG, " Parameter Changed!")
                        DebugLogger.debugLog(TAG, "  Parameter: ${change.parameter}")
                        // Safely stringify JsonElement values
                        val beforeVal = change.before?.toString() ?: "null"
                        val afterVal = change.after?.toString() ?: "null"
                        DebugLogger.debugLog(TAG, "  Before Value: $beforeVal")
                        DebugLogger.debugLog(TAG, "  After Value: $afterVal")
                        DebugLogger.debugLog(TAG, "  Before URL: ${change.beforeUrl}")
                        DebugLogger.debugLog(TAG, "  After URL: ${change.afterUrl}")
                    }

                    _sessionData.value = apiResponse
                    processSessionResponse(apiResponse)
                    _errorMessage.value = null
                    _uiState.value = SimAgentUiState.Success(apiResponse)
                } else {
                    throw result.exceptionOrNull() ?: Exception("Failed to send response")
                }

            } catch (e: Exception) {
                val errorMsg = handleError(e, "send_response")
                _errorMessage.value = errorMsg
                _uiState.value = SimAgentUiState.Error(errorMsg)
            }
        }
    }

    /**
     * Submit quiz answer for the current session
     */
    fun submitQuizAnswer(answer: String) {
        val currentSessionId = _sessionData.value?.sessionId
        if (currentSessionId == null) {
            val errorMsg = "No active session. Please restart the simulation."
            DebugLogger.errorLog(TAG, " No active session")
            _errorMessage.value = errorMsg
            _uiState.value = SimAgentUiState.Error(errorMsg)
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = SimAgentUiState.Loading
                DebugLogger.debugLog(TAG, "Submitting quiz answer: $answer")

                val result = sendSimulationResponseUseCase.submitQuizAnswer(
                    sessionId = currentSessionId,
                    answer = answer
                )

                if (result.isSuccess) {
                    val apiResponse = result.getOrNull()!!
                    DebugLogger.debugLog(TAG, " Quiz answer submitted successfully")
                    DebugLogger.debugLog(TAG, "Teacher Message: ${apiResponse.teacherMessage.text}")

                    _sessionData.value = apiResponse
                    processSessionResponse(apiResponse)
                    _uiState.value = SimAgentUiState.Success(apiResponse)
                } else {
                    throw result.exceptionOrNull() ?: Exception("Failed to submit quiz answer")
                }

            } catch (e: Exception) {
                val errorMsg = handleError(e, "submit_quiz")
                _errorMessage.value = errorMsg
                _uiState.value = SimAgentUiState.Error(errorMsg)
            }
        }
    }

    /**
     * Reset session data
     */
    fun resetSession(): Boolean {
        resetSessionForNavigation()
        return false // not consumed - navigate back
    }

    /**
     * Handles avatar change with proper validation and delegation to use case
     * Returns updated ChatBotSettingsState with both code and display name
     */
    fun handleAvatarChange(
        displayName: String,
        boyDisplayName: String,
        girlDisplayName: String,
        ttsController: TextToSpeech,
        currentState: ChatBotSettingsState
    ): ChatBotSettingsState {
        // Convert display name to code
        val avatarCode = avatarChangeUseCase.getAvatarCodeFromDisplayName(
            displayName = displayName,
            boyDisplayName = boyDisplayName,
            girlDisplayName = girlDisplayName
        )

        // Apply avatar change through use case (simulation always uses "en" language)
        val normalizedCode = avatarChangeUseCase.changeAvatar(
            avatarCode = avatarCode,
            ttsController = ttsController,
            currentLanguage = "en"
        )

        // Return updated state with both code and display name
        return currentState.copy(
            selectedAvatar = normalizedCode,
            selectedAvatarDisplayName = displayName
        )
    }

    /**
     * Initialize settings state with proper display name for current avatar
     */
    fun initializeAvatarDisplayName(
        avatarCode: String,
        boyDisplayName: String,
        girlDisplayName: String,
        disableDisplayName: String,
        currentState: ChatBotSettingsState
    ): ChatBotSettingsState {
        val displayName = avatarChangeUseCase.getDisplayNameFromCode(
            avatarCode = avatarCode,
            boyDisplayName = boyDisplayName,
            girlDisplayName = girlDisplayName,
            disableDisplayName = disableDisplayName
        )
        return currentState.copy(
            selectedAvatar = avatarCode,
            selectedAvatarDisplayName = displayName
        )
    }
}

/**
 * UI State for the teaching screen
 */
sealed class SimAgentUiState {
    object Initial : SimAgentUiState()
    object Loading : SimAgentUiState()
    data class Success(val data: SimSessionResponse) : SimAgentUiState()
    data class Error(val message: String) : SimAgentUiState()
}

