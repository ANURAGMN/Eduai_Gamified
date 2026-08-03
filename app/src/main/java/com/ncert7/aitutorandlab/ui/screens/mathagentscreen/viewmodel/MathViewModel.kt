package com.ncert7.aitutorandlab.ui.screens.mathagentscreen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.domain.chatbot.usecase.AvatarChangeUseCase
import com.ncert7.aitutorandlab.domain.examplan.PlanTrialProgressTracker
import com.ncert7.aitutorandlab.domain.examplan.TrialSessionStore
import com.ncert7.aitutorandlab.domain.mathagent.usecase.MathIntent
import com.ncert7.aitutorandlab.domain.mathagent.usecase.MathImageHandlingUseCase
import com.ncert7.aitutorandlab.domain.mathagent.usecase.MathProblemsUseCase
import com.ncert7.aitutorandlab.domain.mathagent.usecase.MathSendMessageUseCase
import com.ncert7.aitutorandlab.domain.mathagent.usecase.MathSessionUseCase
import com.ncert7.aitutorandlab.domain.progress.ProgressEventTracker
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatBotSettingsState
import com.ncert7.aitutorandlab.repository.ConceptRepository
import com.ncert7.aitutorandlab.ui.screens.mathagentscreen.dataclass.MathUiState
import com.ncert7.aitutorandlab.ui.viewModel.TextToSpeech
import com.ncert7.aitutorandlab.utils.isKannada
import com.ncert7.aitutorandlab.utils.resolveProgressLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MathViewModel @Inject constructor(
    private val mathSessionUseCase: MathSessionUseCase,
    private val mathProblemsUseCase: MathProblemsUseCase,
    private val mathSendMessageUseCase: MathSendMessageUseCase,
    private val mathImageHandlingUseCase: MathImageHandlingUseCase,
    private val sharedPreferenceUtils: SharedPreferenceUtils,
    private val progressEventTracker: ProgressEventTracker,
    private val planTrialProgressTracker: PlanTrialProgressTracker,

    private val avatarChangeUseCase: AvatarChangeUseCase,
    private val conceptRepository: ConceptRepository

) : ViewModel() {

    private val _uiState = MutableStateFlow(MathUiState())
    val uiState: StateFlow<MathUiState> = _uiState.asStateFlow()

    private var userId = ""
    private var initialized = false

    init {
        // Auto-initialize with userId from SharedPreferences
        val storedUserId = sharedPreferenceUtils.getUserId()
        if (!storedUserId.isNullOrEmpty() && storedUserId != "null") {
            DebugLogger.debugLog("MathViewModel", "Init: Initializing with userId: $storedUserId")
            initialize(storedUserId)
        }
    }

    /**
     * Routes all intents from the UI to appropriate handlers
     */
    fun onIntent(intent: MathIntent) = when (intent) {
        is MathIntent.Initialize -> initialize(intent.userId)
        is MathIntent.AutoStartWithProblem -> autoStartWithProblem(intent.problemId)
        is MathIntent.UpdateInputText -> updateInput(intent.text)
        is MathIntent.SetKannada -> {
            _uiState.update {
                it.copy(
                    isKannada = intent.enabled,
                    currentLanguage = if (intent.enabled) "kn" else "en"
                )
            }
        }
        is MathIntent.SelectProblem -> selectProblem(intent.problemId)
        is MathIntent.SelectImage -> selectImage(intent.imageUri)
        is MathIntent.SendMessage -> sendMessage(intent.message, null)
        is MathIntent.SendMessageWithImage -> sendMessage(intent.message, intent.imageBase64)
        is MathIntent.ContinueExistingSession -> continuePreviousSession(intent.problemId)
        is MathIntent.StartFreshSession -> startFreshSession(intent.problemId)
        is MathIntent.DismissSessionDialog -> dismissSessionDialog()
        is MathIntent.ConsumeTTSTrigger -> consumeTTSTrigger()
        is MathIntent.ClearSelectedImage -> clearSelectedImage()
        is MathIntent.HideAutosuggestions -> hideAutosuggestions()
        is MathIntent.MarkUserActive -> markUserActive()
        is MathIntent.MarkUserInactive -> markUserInactive()
        is MathIntent.RefreshProblems -> refreshProblems()
        is MathIntent.StartIdleTimer -> startIdleTimer()
    }

    /**
     * Initialize the ViewModel and load available problems
     */
    private fun initialize(userId: String) {
        if (initialized) return
        initialized = true
        this.userId = userId

        DebugLogger.debugLog("MathViewModel", "Initialize called with userId: $userId")
        loadProblems()
    }

    /**
     * Load all available math problems
     */
    private fun loadProblems() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val result = mathProblemsUseCase.getAvailableProblems()
                result.onSuccess { problems ->
                    _uiState.update {
                        it.copy(
                            problems = problems,
                            isLoading = false
                        )
                    }
                    DebugLogger.debugLog("MathViewModel", "Problems loaded: ${problems.size}")
              
                }.onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.message
                        )
                    }
                    DebugLogger.errorLog(
                        "MathViewModel",
                        "Failed to load problems: ${exception.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
                DebugLogger.errorLog("MathViewModel", "Exception loading problems: ${e.message}")
            }
        }
    }

    /**
     * Auto-start with a specific problem
     * Checks if there's an existing session and shows dialog if found
     */
    private fun autoStartWithProblem(problemId: String) {
        if (_uiState.value.sessionStarted) {
            DebugLogger.debugLog("MathViewModel", "autoStartWithProblem skipped — session already started")
            return
        }

        val problem = mathProblemsUseCase.findProblemById(_uiState.value.problems, problemId)
        _uiState.update {
            it.copy(
                selectedProblem = problem ?: it.selectedProblem,
                problemId = problemId
            )
        }

        if (problem == null) {
            DebugLogger.warnLog(
                "MathViewModel",
                "Problem '$problemId' not in catalog (${_uiState.value.problems.size} loaded) — starting session with nav problemId anyway"
            )
        }

        viewModelScope.launch {
            try {
                val hasSession = mathSessionUseCase.hasExistingSession(problemId)
                if (hasSession) {
                    _uiState.update {
                        it.copy(
                            pendingProblemForDialog = problemId,
                            showSessionDialog = true
                        )
                    }
                    DebugLogger.debugLog("MathViewModel", "Existing session found for problem: $problemId, showing dialog")
                } else {
                    DebugLogger.debugLog("MathViewModel", "No existing session for problem: $problemId, starting new")
                    startMathSession(problemId)
                }
            } catch (e: Exception) {
                DebugLogger.errorLog(
                    "MathViewModel",
                    "Error checking for existing session: ${e.message}"
                )
                startMathSession(problemId)
            }
        }
    }

    /**
     * Select a problem from the list
     * Checks if there's an existing session and shows dialog if found
     */
    private fun selectProblem(problemId: String) {
        val problem = mathProblemsUseCase.findProblemById(_uiState.value.problems, problemId)
        if (problem != null) {
            _uiState.update { it.copy(selectedProblem = problem, problemId = problemId) }

            // Check for existing session
            viewModelScope.launch {
                try {
                    val hasSession = mathSessionUseCase.hasExistingSession(problemId)
                    if (hasSession) {
                        // Show dialog to ask user to continue or start fresh
                        _uiState.update {
                            it.copy(
                                pendingProblemForDialog = problemId,
                                showSessionDialog = true
                            )
                        }
                        DebugLogger.debugLog("MathViewModel", "Existing session found for problem: $problemId, showing dialog")
                    } else {
                        // No existing session, start new one
                        DebugLogger.debugLog("MathViewModel", "No existing session for problem: $problemId, starting new")
                        startMathSession(problemId)
                    }
                } catch (e: Exception) {
                    DebugLogger.errorLog(
                        "MathViewModel",
                        "Error checking for existing session: ${e.message}"
                    )
                    startMathSession(problemId)
                }
            }
        }
    }

    /**
     * Start a new math tutoring session
     */
    private fun startMathSession(problemId: String) {
        DebugLogger.debugLog("MathViewModel", "startMathSession called with problemId: '$problemId'")

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val isKannada = isKannada()
                DebugLogger.debugLog("MathViewModel", "Starting session with isKannada: $isKannada (from app language)")

                val sessionResult = mathSessionUseCase.startSession(
                    problemId = problemId,
                    studentId = userId,
                    isKannada = isKannada
                )

                if (sessionResult.success) {
                    DebugLogger.debugLog("MathViewModel", "✓ Session started successfully. Setting state with problemId='$problemId', threadId='${sessionResult.threadId}'")

                    _uiState.update {
                        it.copy(
                            currentState = sessionResult.currentState ?: it.currentState,
                            sessionStarted = true,
                            isLoading = false,
                            metadata = sessionResult.metadata ?: it.metadata,
                            messages = sessionResult.messages,
                            threadId = sessionResult.threadId,
                            problemId = problemId,
                            // Auto-speak the agent's first message + drive highlight,
                            // mirroring ChatViewModel's shouldStartTTS/fullTextForTTS pattern
                            shouldStartTTS = sessionResult.messages.lastOrNull { msg -> msg.role.lowercase() == "assistant" }?.content?.isNotEmpty() == true,
                            fullTextForTTS = sessionResult.messages.lastOrNull { msg -> msg.role.lowercase() == "assistant" }?.content ?: it.fullTextForTTS
                        )
                    }

                    // Track Math Agent progress using conceptId instead of problemId
                    // markMathAgentCompleted also marks CONCEPT/COMPLETED for the study component
                    viewModelScope.launch {
                        try {
                            val concept = conceptRepository.getConceptByProblemId(problemId)
                            val actualId = concept?.conceptId ?: problemId
                            // Use language from uiState (set by SetKannada intent), fallback to SharedPrefs
                            val lang = resolveProgressLanguage(_uiState.value.currentLanguage.takeIf { it.isNotBlank() })
                            progressEventTracker.markMathAgentCompleted(userId, actualId, lang)
                            DebugLogger.debugLog("MathViewModel", "Math agent progress tracked for concept: $actualId (problem: $problemId) [$lang]")
                        } catch (e: Exception) {
                            DebugLogger.errorLog("MathViewModel", "Error tracking math progress: ${e.message}")
                        }
                    }

                    // Verify state was updated
                    val updatedState = _uiState.value
                    DebugLogger.debugLog("MathViewModel", "State verified after session start - problemId: '${updatedState.problemId}', threadId: '${updatedState.threadId}', sessionStarted: ${updatedState.sessionStarted}")
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = sessionResult.agentResponse
                        )
                    }
                    DebugLogger.errorLog(
                        "MathViewModel",
                        "✗ Failed to start session: ${sessionResult.agentResponse}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
                DebugLogger.errorLog(
                    "MathViewModel",
                    "✗ Exception starting session: ${e.message}\n${e.stackTraceToString()}"
                )
            }
        }
    }

    /**
     * Send a message with optional image to continue the session
     */
    private fun sendMessage(message: String, imageUri: String?) {
        val currentState = _uiState.value

        // Check if session is started
        if (!currentState.sessionStarted) {
            DebugLogger.errorLog("MathViewModel", "Cannot send message: Session not started yet", )
            _uiState.update {
                it.copy(errorMessage = "Session not initialized. Please wait for the session to start.")
            }
            return
        }

        val problemId = currentState.problemId
        val threadId = currentState.threadId

        DebugLogger.debugLog("MathViewModel", "sendMessage - problemId: '$problemId', threadId: '$threadId', sessionStarted: ${currentState.sessionStarted}")

        if (problemId.isNullOrEmpty()) {
            DebugLogger.errorLog(
                "MathViewModel",
                " Cannot send message: problemId is null or empty. Current state: problemId='$problemId'"
            )
            _uiState.update {
                it.copy(errorMessage = "Problem ID not set. Please try again.")
            }
            return
        }

        if (threadId.isNullOrEmpty()) {
            DebugLogger.errorLog(
                "MathViewModel",
                "✗ Cannot send message: threadId is null or empty. Current state: threadId='$threadId'"
            )
            _uiState.update {
                it.copy(errorMessage = "Session thread ID not set. Please start a new session.")
            }
            return
        }

        // Trial progress: each math turn counts toward completing the Math trial item.
        TrialSessionStore.activeTrialItemId?.let { trialItemId ->
            viewModelScope.launch { planTrialProgressTracker.recordIncrement(trialItemId) }
        }

        // Backend requires a non-empty user message for every turn.
        // If the user only attached an image without typing anything,
        // append a default text so the request is valid.
        val effectiveMessage = if (message.isBlank() && imageUri != null) {
            "Here is the image of my answer."
        } else {
            message
        }

        // Create user message using usecase (attach image so it can be shown in chat history)
        val userMessage = mathSendMessageUseCase.createUserMessage(effectiveMessage, imageUri)

        // Add user message to chat
        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                inputText = "",
                selectedImageUri = null
            )
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, isTyping = true) }

                val finalThreadId = _uiState.value.threadId

                if (finalThreadId.isNullOrEmpty()) {
                    DebugLogger.errorLog(
                        "MathViewModel",
                        "✗ ThreadId is null or empty when attempting to continue session. Cannot proceed."
                    )
                    val errorMessage = mathSendMessageUseCase.createErrorMessage("Session thread ID is missing. Please restart the session.")
                    _uiState.update {
                        it.copy(
                            messages = it.messages + errorMessage,
                            isLoading = false,
                            isTyping = false,
                            errorMessage = "Invalid session thread ID"
                        )
                    }
                    return@launch
                }

                DebugLogger.debugLog("MathViewModel", "Calling continueSession with problemId='$problemId', threadId='$finalThreadId', imageUri: ${imageUri != null}")

                // Use actual app language setting instead of UI state
                val isKannada = isKannada()
                DebugLogger.debugLog("MathViewModel", "Continuing session with isKannada: $isKannada (from app language)")

                // Continue session with optional image URI
                val sessionResult = mathSessionUseCase.continueSession(
                    problemId = problemId,
                    userMessage = effectiveMessage,
                    isKannada = isKannada,
                    imageUri = imageUri
                )

                if (sessionResult.success) {
                    _uiState.update { state ->
                        val lastAssistantMsg = sessionResult.messages.lastOrNull { it.role.lowercase() == "assistant" }
                        state.copy(
                            messages = state.messages + sessionResult.messages,
                            currentState = sessionResult.currentState ?: state.currentState,
                            metadata = sessionResult.metadata ?: state.metadata,
                            isLoading = false,
                            isTyping = false,
                            shouldStartTTS = lastAssistantMsg?.content?.isNotEmpty() == true,
                            fullTextForTTS = lastAssistantMsg?.content ?: state.fullTextForTTS
                        )
                    }
                    DebugLogger.debugLog("MathViewModel", "✓ Message sent successfully with threadId: $threadId")
                } else {
                    // Add error message
                    val errorMessage = mathSendMessageUseCase.createErrorMessage(
                        sessionResult.agentResponse ?: "Failed to send message"
                    )
                    _uiState.update {
                        it.copy(
                            messages = it.messages + errorMessage,
                            isLoading = false,
                            isTyping = false,
                            errorMessage = sessionResult.agentResponse
                        )
                    }
                    DebugLogger.errorLog(
                        "MathViewModel",
                        " Failed to send message: ${sessionResult.agentResponse}"
                    )
                }
            } catch (e: Exception) {
                val errorMessage = mathSendMessageUseCase.createErrorMessage("Error: ${e.message}")
                _uiState.update {
                    it.copy(
                        messages = it.messages + errorMessage,
                        isLoading = false,
                        isTyping = false,
                        errorMessage = e.message
                    )
                }
                DebugLogger.errorLog(
                    "MathViewModel",
                    " Exception sending message: ${e.message}"
                )
            }
        }
    }


    /**
     * Continue with an existing session
     * Loads the stored thread ID and resumes the session with history
     */
    private fun continuePreviousSession(problemId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // Dismiss dialog first
                _uiState.update {
                    it.copy(
                        pendingProblemForDialog = null,
                        showSessionDialog = false
                    )
                }

                // Load existing thread mapping
                val mapping = mathSessionUseCase.loadThreadMapping(problemId)
                if (mapping != null) {
                    val (threadId, sessionId) = mapping
                    DebugLogger.debugLog("MathViewModel", "Resuming session - threadId=$threadId, sessionId=$sessionId")

                    // Resume the session with history
                    val sessionResult = mathSessionUseCase.resumeSession(threadId, sessionId)

                    if (sessionResult.success) {
                        _uiState.update {
                            it.copy(
                                sessionStarted = true,
                                isLoading = false,
                                threadId = threadId,
                                problemId = problemId,
                                messages = sessionResult.messages,
                                currentState = sessionResult.currentState ?: it.currentState,
                                metadata = sessionResult.metadata ?: it.metadata,
                                // Auto-speak the resumed agent's last message + drive highlight
                                shouldStartTTS = sessionResult.messages.lastOrNull { msg -> msg.role.lowercase() == "assistant" }?.content?.isNotEmpty() == true,
                                fullTextForTTS = sessionResult.messages.lastOrNull { msg -> msg.role.lowercase() == "assistant" }?.content ?: it.fullTextForTTS
                            )
                        }
                        DebugLogger.debugLog("MathViewModel", "Session resumed successfully with ${sessionResult.messages.size} messages")
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Failed to resume session"
                            )
                        }
                        DebugLogger.errorLog(
                            "MathViewModel",
                            "Failed to resume session: ${sessionResult.agentResponse}"
                        )
                    }
                } else {
                    // No mapping found, start fresh
                    DebugLogger.debugLog("MathViewModel", "No session mapping found for problem $problemId")
                    startMathSession(problemId)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error resuming session: ${e.message}"
                    )
                }
                DebugLogger.errorLog(
                    "MathViewModel",
                    "Exception continuing previous session: ${e.message}"
                )
            }
        }
    }

    /**
     * Start a fresh session, deleting any existing session data
     * Used when user chooses to start new instead of resuming
     */
    private fun startFreshSession(problemId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // Dismiss dialog first
                _uiState.update {
                    it.copy(
                        pendingProblemForDialog = null,
                        showSessionDialog = false
                    )
                }

                // Delete existing session mapping
                mathSessionUseCase.deleteSessionMapping(problemId)
                DebugLogger.debugLog("MathViewModel", "Deleted existing session for problem: $problemId")

                // Reset UI for new session
                _uiState.update {
                    it.copy(
                        problemId = problemId,
                        messages = emptyList(),
                        threadId = null,
                        currentState = "START",
                        metadata = it.metadata.copy()
                    )
                }

                // Start new session
                startMathSession(problemId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error starting fresh session: ${e.message}"
                    )
                }
                DebugLogger.errorLog(
                    "MathViewModel",
                    "Exception starting fresh session: ${e.message}"
                )
            }
        }
    }

    /**
     * Dismiss the session resume dialog without taking action
     */
    private fun dismissSessionDialog() {
        _uiState.update {
            it.copy(
                pendingProblemForDialog = null,
                showSessionDialog = false
            )
        }
        DebugLogger.debugLog("MathViewModel", "Session dialog dismissed")
    }

    /**
     * Reset the shouldStartTTS flag after the screen has consumed it and
     * triggered ttsController.speak(...). This mirrors ChatViewModel's
     * shouldStartTTS pattern so the auto-speak doesn't re-trigger on recomposition.
     */
    private fun consumeTTSTrigger() {
        _uiState.update { it.copy(shouldStartTTS = false) }
    }

    /**
     * Update input text
     */
    private fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    /**
     * Hide auto-suggestions
     */
    private fun hideAutosuggestions() {
        _uiState.update { it.copy(showAutosuggestions = false) }
    }

    /**
     * Mark user as active
     */
    private fun markUserActive() {
        DebugLogger.debugLog("MathViewModel", "User marked as active")
    }

    /**
     * Mark user as inactive
     */
    private fun markUserInactive() {
        DebugLogger.debugLog("MathViewModel", "User marked as inactive")
    }

    /**
     * Refresh problems list
     */
    private fun refreshProblems() {
        loadProblems()
    }

    /**
     * Start idle timer
     */
    private fun startIdleTimer() {
        DebugLogger.debugLog("MathViewModel", "Idle timer started")
    }

    /**
     * Show session dialog for a specific problem if a session exists
     */
    fun selectProblemWithDialog(problemId: String) {
        _uiState.update {
            it.copy(
                pendingProblemForDialog = problemId,
                showSessionDialog = true
            )
        }
    }


    /**
     * Check if there's an existing session for a problem
     */
    fun hasExistingSession(problemId: String): Boolean {
        // This would typically check with a session manager or use case
        // For now, returning false - implement based on your session logic
        return false
    }

    /**
     * Update input text
     */
    fun onImageSelected(imageUri: String) {
        _uiState.update { it.copy(selectedImageUri = imageUri) }
    }

    /**
     * Clear selected image
     */
    fun clearSelectedImage() {
        _uiState.update { it.copy(selectedImageUri = null) }
    }

    /**
     * Handle image selection from picker
     * Converts URI to File and then to Base64
     */
    private fun selectImage(imageUri: String) {
        viewModelScope.launch {
            try {
                DebugLogger.debugLog("MathViewModel", "Image selected: $imageUri")
                _uiState.update { it.copy(selectedImageUri = imageUri) }
            } catch (e: Exception) {
                DebugLogger.errorLog("MathViewModel", "Error selecting image: ${e.message}")
            }
        }
    }

    /**
     * Handles avatar change with proper validation and delegation to use case.
     * This mirrors ChatViewModel.handleAvatarChange so the Math Agent screen's
     * avatar selection (boy/girl/disable) actually switches the character and voice.
     * @param displayName The localized display name from UI
     * @param boyDisplayName The localized "boy" string
     * @param girlDisplayName The localized "girl" string
     * @param ttsController The TTS controller to apply voice/character changes
     * @param currentState The current settings state
     * @return Updated ChatBotSettingsState with normalized code and display name
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

        // Apply avatar change through use case (switches character + voice in the WebView)
        val normalizedCode = avatarChangeUseCase.changeAvatar(
            avatarCode = avatarCode,
            ttsController = ttsController,
            currentLanguage = _uiState.value.currentLanguage
        )

        DebugLogger.debugLog("MathViewModel", "Avatar changed to: $normalizedCode")

        // Return updated state with both code and display name
        return currentState.copy(
            selectedAvatar = normalizedCode,
            selectedAvatarDisplayName = displayName
        )
    }

    /**
     * Convert image file to Base64
     */
    fun imageFileToBase64(file: File): String? {
        return mathImageHandlingUseCase.fileToBase64(file)
    }
}