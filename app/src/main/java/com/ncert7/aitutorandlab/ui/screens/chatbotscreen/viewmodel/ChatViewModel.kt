package com.ncert7.aitutorandlab.ui.screens.chatbotscreen.viewmodel

import androidx.core.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.ncert7.aitutorandlab.data.remote.SessionMetadata
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.domain.chatbot.controller.IdleTimerController
import com.ncert7.aitutorandlab.domain.chatbot.controller.ResourceController
import com.ncert7.aitutorandlab.domain.chatbot.controller.TypingAnimationController
import com.ncert7.aitutorandlab.domain.chatbot.model.ResourceDecision
import com.ncert7.aitutorandlab.domain.chatbot.model.SessionResult
import com.ncert7.aitutorandlab.domain.chatbot.usecase.AutoSuggestionUseCase
import com.ncert7.aitutorandlab.domain.chatbot.usecase.AvatarChangeUseCase
import com.ncert7.aitutorandlab.domain.chatbot.usecase.ChatIntent
import com.ncert7.aitutorandlab.domain.chatbot.usecase.ConceptMapUseCase
import com.ncert7.aitutorandlab.domain.chatbot.usecase.ConceptProgressUseCase
import com.ncert7.aitutorandlab.domain.chatbot.usecase.HandleAgentResponseUseCase
import com.ncert7.aitutorandlab.domain.chatbot.usecase.ResourceDecisionUseCase
import com.ncert7.aitutorandlab.domain.chatbot.usecase.SendMessageUseCase
import com.ncert7.aitutorandlab.domain.chatbot.usecase.SessionUseCase
import com.ncert7.aitutorandlab.domain.chatbot.usecase.TranslationUseCase
import com.ncert7.aitutorandlab.domain.examplan.PlanTrialProgressTracker
import com.ncert7.aitutorandlab.domain.examplan.TrialSessionStore
import com.ncert7.aitutorandlab.domain.progress.ProgressEventTracker
import com.ncert7.aitutorandlab.service.analytics.GamificationAnalyticsTracker
import com.ncert7.aitutorandlab.repository.ConceptRepository
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatBotSettingsState
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatUiState
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ResourceCardUiState
import com.ncert7.aitutorandlab.ui.viewModel.TextToSpeech
import com.ncert7.aitutorandlab.utils.ErrorHandler
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import com.ncert7.aitutorandlab.utils.isKannada
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sessionUseCase: SessionUseCase,
    private val autoSuggestionUseCase: AutoSuggestionUseCase,
    private val resourceDecisionUseCase: ResourceDecisionUseCase,
    private val conceptMapUseCase: ConceptMapUseCase,
    private val idleTimerController: IdleTimerController,
    private val typingAnimationController: TypingAnimationController,
    private val resourceController: ResourceController,
    private val sendMessageUseCase: SendMessageUseCase,
    private val handleAgentResponseUseCase: HandleAgentResponseUseCase,
    private val translationUseCase: TranslationUseCase,
    private val conceptRepository: ConceptRepository,
    private val conceptProgressUseCase: ConceptProgressUseCase,
    private val avatarChangeUseCase: AvatarChangeUseCase,
    private val progressEventTracker: ProgressEventTracker,
    private val planTrialProgressTracker: PlanTrialProgressTracker,
    @ApplicationContext private val context: android.content.Context,

    ) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var userId = ""
    private var initialized = false
    /** Tracks in-flight auto-start / concept selection so sends wait for the active thread. */
    private var sessionSetupJob: Job? = null

    /**
     * Syncs the ViewModel's isKannada flag with the current app locale.
     * This should be called at critical points to ensure the UI state matches the actual system language.
     */
    private fun syncLanguageState() {
        val currentAppLanguage = getCurrentLanguageCode()
        val currentIsKannada = isKannada()

        _uiState.update {
            if (it.currentLanguage != currentAppLanguage || it.isKannada != currentIsKannada) {
                DebugLogger.debugLog(
                    "ChatViewModel",
                    "Language state sync: currentLanguage: ${it.currentLanguage} -> $currentAppLanguage, isKannada: ${it.isKannada} -> $currentIsKannada"
                )
                it.copy(
                    currentLanguage = currentAppLanguage,
                    isKannada = currentIsKannada
                )
            } else {
                it
            }
        }
    }

    /**
     * handles all intents from the UI and routes them to appropriate functions
     */
    fun onIntent(intent: ChatIntent) = when (intent) {
        is ChatIntent.Initialize -> initialize(intent.userId)
        is ChatIntent.AutoStartWithConcept -> autoStartWithConcept(intent.conceptId)
        is ChatIntent.UpdateInputText -> updateInput(intent.text)
        is ChatIntent.SetStudentLevel -> _uiState.update { it.copy(studentLevel = intent.level) }
        is ChatIntent.SetKannada -> {
            _uiState.update { it.copy(isKannada = intent.enabled, currentLanguage = if (intent.enabled) "kn" else "en") }
            refreshConcepts()
        }
        is ChatIntent.SelectConcept -> selectConcept(intent.concept)
        is ChatIntent.SendMessage -> sendMessage(intent.message, false)
        is ChatIntent.TapAutosuggestion -> sendMessage(intent.suggestion, true)
        is ChatIntent.StartFreshSession -> startFreshSession(intent.concept)
        is ChatIntent.HasExistingSession -> Unit
        is ChatIntent.StartIdleTimer -> startIdleTimer()
        is ChatIntent.HideAutosuggestions -> hideAutosuggestions()
        is ChatIntent.MarkUserActive -> markUserActive()
        is ChatIntent.MarkUserInactive -> markUserInactive()
        is ChatIntent.RefreshConcepts -> refreshConcepts()
        is ChatIntent.DismissResource -> dismissResource()
        is ChatIntent.ResumeTTS -> resumeTTS()
    }

    /**
     * Checks if there's an existing session for the given concept (English session key).
     */
    fun hasExistingSession(concept: String) = sessionUseCase.hasExistingSession(concept)

    /** Time-based proceed: mark the current trial study item complete before leaving. */
    fun recordTrialProceed() {
        val trialItemId = TrialSessionStore.activeTrialItemId ?: return
        viewModelScope.launch {
            planTrialProgressTracker.recordGeReached(trialItemId)
        }
    }

    /** Session API keys are always the English concept title stored at start time. */
    private suspend fun resolveSessionConceptKey(concept: String): String {
        conceptRepository.getAllConcepts().find {
            it.conceptName == concept ||
                it.conceptNameKannada == concept ||
                it.conceptId == concept
        }?.conceptName?.let { return it }
        return concept
    }

    private fun launchSessionSetup(block: suspend () -> Unit): Job {
        sessionSetupJob?.cancel()
        return viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DebugLogger.errorLog("ChatViewModel", "Session setup error: ${e.message}")
                appendSessionError(ErrorHandler.httpStatusFrom(e))
            }
        }.also { sessionSetupJob = it }
    }

    /**
     * Initializes the ViewModel with the user ID and loads the list of available concepts.
     * This should be called once when the chat screen is opened.
     */
    private fun initialize(id: String) {
        if (initialized) return
        initialized = true
        userId = id

        // Use LocalizationUtils for language detection
        val appLanguage = getCurrentLanguageCode()
        val isKannadaMode = isKannada()

        _uiState.update {
            it.copy(
                isKannada = isKannadaMode,
                currentLanguage = appLanguage
            )
        }
        DebugLogger.debugLog("ChatViewModel", "Initialized with app language: $appLanguage, isKannada: $isKannadaMode")

        refreshConcepts()
    }

    /**
     * Auto-starts a session with a concept from navigation (when user clicks a concept).
     * Fetches the concept name from the database using conceptId and delegates to selectConcept.
     * Uses only English concept name for starting the session, not Kannada.
     */
    private fun autoStartWithConcept(conceptId: String) = launchSessionSetup {
        try {
            DebugLogger.debugLog("ChatViewModel", "autoStartWithConcept called with conceptId: $conceptId, userId: $userId")

            val conceptEntity = conceptRepository.getConcept(conceptId)
            if (conceptEntity == null) {
                DebugLogger.errorLog("ChatViewModel", "Concept not found for ID: $conceptId")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        messages = it.messages + sendMessageUseCase.createAIMessage(
                            ErrorHandler.getStudyAgentErrorMessage(context, null)
                        ),
                    )
                }
                return@launchSessionSetup
            }

            DebugLogger.debugLog("ChatViewModel", "Auto-starting with concept: ${conceptEntity.conceptName}")
            selectConceptInternal(conceptEntity.conceptName)
        } catch (e: Exception) {
            DebugLogger.errorLog("ChatViewModel", "Error auto-starting concept: ${e.message}")
            appendSessionError(ErrorHandler.httpStatusFrom(e))
        }
    }

    /**
     * Fetches the list of available concepts from the local database and updates the UI state.
     * Shows a loading indicator while fetching.
     * Uses Kannada concept names if Kannada mode is enabled.
     */
    private fun refreshConcepts() = viewModelScope.launch {
        try {
            // Fetch all concepts from the database
            val conceptEntities = conceptRepository.getAllConcepts()

            // Extract English concept names for internal use (session management)
            val concepts = conceptEntities.map { it.conceptName }

            // Use Kannada names for display if Kannada mode is enabled
            val displayConcepts = if (_uiState.value.isKannada) {
                conceptEntities.map {
                    it.conceptNameKannada.ifBlank { it.conceptName }
                }
            } else {
                concepts
            }

            _uiState.update {
                it.copy(
                    availableConcepts = concepts,
                    displayConcepts = displayConcepts
                )
            }
            DebugLogger.debugLog("ChatViewModel", "Concepts loaded from DB: ${concepts.size}, Display concepts: ${displayConcepts.size}")
        } catch (e: Exception) {
            DebugLogger.errorLog("ChatViewModel", "Error loading concepts from DB: ${e.message}")
        }
    }

    /**
     * Updates the input text in the UI state and marks the user as active if the text is not blank.
     * Also hides autosuggestions when the user starts typing.
     */
    private fun updateInput(text: String) {
        _uiState.update {
            it.copy(inputText = text, isUserActive = text.isNotBlank(), showAutosuggestions = false)
        }
        if (text.isNotBlank()) {
            idleTimerController.markUserActive(
                scope = viewModelScope,
                onActive = { /* no-op: already handled above */ },
                onInactive = { _uiState.update { it.copy(isUserActive = false) } }
            )
        }
    }

    /**
     * Marks the user as active and starts monitoring for inactivity.
     * When the user is active, autosuggestions are hidden.
     */
    private fun markUserActive() = idleTimerController.markUserActive(
        scope = viewModelScope,
        onActive = { _uiState.update {
                        it.copy(isUserActive = true,
                            showAutosuggestions = false)
                        }
                   },
        onInactive = { _uiState.update { it.copy(isUserActive = false) } }
    )

    /**
     * Marks the user as inactive after a short delay.
     * This is called when the user stops interacting with the chat.
     */
    private fun markUserInactive() = idleTimerController.markUserInactive(
        scope = viewModelScope,
        onInactive = { _uiState.update { it.copy(isUserActive = false) } }
    )

    /**
     * Starts the idle timer which will show autosuggestions after a delay if the user remains inactive.
     */
    private fun startIdleTimer() = idleTimerController.startIdleTimer(viewModelScope) {
        if (autoSuggestionUseCase.shouldShowAutosuggestions(_uiState.value))
            _uiState.update { it.copy(showAutosuggestions = true) }
    }

    /**
     * Hides autosuggestions and cancels the idle timer.
     * This is called when the user starts typing or interacts with the chat in a way that indicates they are active.
     */
    private fun hideAutosuggestions() {
        idleTimerController.cancelIdleTimer()
        _uiState.update { it.copy(showAutosuggestions = false) }
    }

    /**
     * Handles the selection of a concept by the user.
     * It checks if there's an existing session for the selected concept.
     * If from ConceptScreen and session exists, shows dialog. Otherwise proceeds directly.
     */
    private fun selectConcept(concept: String, showDialogIfExists: Boolean = false) =
        launchSessionSetup {
            val sessionKey = resolveSessionConceptKey(concept)
            selectConceptInternal(sessionKey, showDialogIfExists)
        }

    private suspend fun selectConceptInternal(concept: String, showDialogIfExists: Boolean = false) {
        syncLanguageState()

        DebugLogger.debugLog("ChatViewModel", "selectConcept called with concept: $concept, showDialog: $showDialogIfExists")

        val mapping = sessionUseCase.loadThreadMapping(concept)

        if (showDialogIfExists && mapping != null) {
            DebugLogger.debugLog("ChatViewModel", "Session exists, showing dialog for concept: $concept")
            _uiState.update { it.copy(pendingConceptForDialog = concept) }
        } else {
            resetUiForConcept(concept)
            if (mapping != null) {
                DebugLogger.debugLog("ChatViewModel", "Found existing session, resuming: threadId=${mapping.first}, sessionId=${mapping.second}")
                resumeSession(mapping.first, mapping.second)
            } else {
                DebugLogger.debugLog("ChatViewModel", "No existing session found, starting new session")
                startSession(concept)
            }
        }
    }

    /**
     * Overloaded method for ConceptScreen to check and show dialog if session exists
     */
    fun selectConceptWithDialog(concept: String) {
        if (sessionUseCase.hasExistingSession(concept)) {
            // Show dialog
            _uiState.update { it.copy(pendingConceptForDialog = concept) }
        } else {
            // No session exists, start directly
            selectConcept(concept, showDialogIfExists = false)
        }
    }

    /**
     * Dismiss the session dialog without taking action
     */
    fun dismissSessionDialog() {
        _uiState.update { it.copy(pendingConceptForDialog = null) }
    }

    /**
     * Resets the UI state for a new concept selection.
     * This includes clearing messages, hiding autosuggestions, resetting typing state, and showing a loading indicator.
     */
    private fun resetUiForConcept(concept: String) {
        typingAnimationController.cancel()
        resourceController.cancel()
        _uiState.update {
            it.copy(
                selectedConcept = concept,
                messages = emptyList(),
                autosuggestions = emptyList(),
                typingText = "",
                isTyping = false,
                resourceCardState = ResourceCardUiState.Hidden,
                pendingAgentResponse = null,
                loadingResourceMessage = null,
                isLoading = true,
                currentProgressPercentage = 0
            )
        }
    }

    /**
     * Helper method to get current progress
     */
    fun getCurrentProgress(): Int {
        return _uiState.value.currentProgressPercentage
    }

    /**
     * Helper method to get visited states from metadata
     */
    fun getVisitedStates(): Set<String> {
        return conceptProgressUseCase.getVisitedStates(_uiState.value.agentMetadata)
    }

    /**
     * Starts a new session for the given concept by calling the SessionUseCase.
     * Translates autosuggestions if Kannada mode is enabled.
     */
    private suspend fun startSession(concept: String): SessionResult {
        // Sync language state with current locale before starting session
        syncLanguageState()

        DebugLogger.debugLog("ChatViewModel", "startSession called for concept: $concept, userId: $userId, studentLevel: ${_uiState.value.studentLevel}")
        val result = sessionUseCase.startSession(concept, userId, _uiState.value.isKannada, _uiState.value.studentLevel)

        if (!result.success) {
            DebugLogger.errorLog(
                "ChatViewModel",
                "Failed to start session for concept: $concept (HTTP ${result.httpStatusCode})"
            )
            appendSessionError(result.httpStatusCode)
            return result
        }

        DebugLogger.debugLog("ChatViewModel", "Session started successfully for concept: $concept")

        // Mark concept as IN_PROGRESS when session starts successfully
        // Match by English concept name since concept parameter is always in English
        val conceptEntity = conceptRepository.getAllConcepts().find { it.conceptName == concept }
        if (conceptEntity != null && userId.isNotEmpty()) {
            conceptProgressUseCase.markConceptInProgress(userId, conceptEntity.conceptId)

            //  Track study start with ProgressEventTracker
            // This marks study as in progress and triggers streak update
            progressEventTracker.markStudyInProgress(
                studentId = userId,
                conceptId = conceptEntity.conceptId
            )
        }

        // Translate autosuggestions if Kannada mode is enabled
        val translatedSuggestions = if (_uiState.value.isKannada && result.autosuggestions.isNotEmpty()) {
            translationUseCase.translateListToKannada(result.autosuggestions)
        } else {
            result.autosuggestions
        }

        _uiState.update { it.copy(
            isSessionStarted = true,
            autosuggestions = translatedSuggestions,
            agentMetadata = result.metadata,
            currentState = result.currentState,
            showAutosuggestions = false,
            isLoading = false) }

        // Update progress based on explicit currentState from API
        val progress = conceptProgressUseCase.calculateProgressPercentage(result.currentState, result.metadata)
        _uiState.update { it.copy(currentProgressPercentage = progress) }

        val openingMessage = result.agentResponse?.trim().orEmpty()
        if (openingMessage.isNotEmpty()) {
            handleAgentMessage(openingMessage, result.metadata)
        } else {
            // Session is alive — don't paint a fake HTTP 500 "Server error." for a blank opener.
            DebugLogger.errorLog("ChatViewModel", "Session started but opening agent message was empty")
            appendSessionError(null)
        }
        return result
    }

    /**
     * Resumes an existing session using the thread ID and session ID.
     * It fetches the session history and updates the UI state with the previous messages and session information.
     * Translates only the last AI message to Kannada if Kannada mode is enabled (since only last message is displayed).
     */
    private suspend fun resumeSession(threadId: String, sessionId: String?) {
        // Sync language state with current locale before resuming
        syncLanguageState()

        val result = sessionUseCase.resumeSession(threadId, sessionId)

        if (!result.success) {
            DebugLogger.errorLog(
                "ChatViewModel",
                "resumeSession failed for thread=$threadId (HTTP ${result.httpStatusCode}), starting fresh"
            )
            val concept = _uiState.value.selectedConcept
            if (!concept.isNullOrBlank()) {
                sessionUseCase.deleteSessionMapping(concept)
                startSession(concept)
            } else {
                appendSessionError(result.httpStatusCode)
                _uiState.update { it.copy(isSessionStarted = false) }
            }
            return
        }

        if (result.messages.isEmpty()) {
            DebugLogger.warnLog("ChatViewModel", "resumeSession returned empty history for thread=$threadId, starting fresh")
            val concept = _uiState.value.selectedConcept
            if (!concept.isNullOrBlank()) {
                sessionUseCase.deleteSessionMapping(concept)
                startSession(concept)
            } else {
                _uiState.update { it.copy(isLoading = false, isSessionStarted = false) }
            }
            return
        }

        val currentIsKannada = _uiState.value.isKannada
        DebugLogger.debugLog("ChatViewModel", "resumeSession - isKannada=$currentIsKannada, messages count=${result.messages.size}")

        // Smart translate only the last AI message based on current app language
        val translatedMessages = if (result.messages.isNotEmpty()) {
            val lastAiMessageIndex = result.messages.indexOfLast { it.sender.lowercase() == "ai" }

            DebugLogger.debugLog("ChatViewModel", "resumeSession - Last AI message index: $lastAiMessageIndex")

            if (lastAiMessageIndex >= 0) {
                result.messages.mapIndexed { index, message ->
                    if (index == lastAiMessageIndex) {
                        val content = message.content
                        DebugLogger.debugLog("ChatViewModel", "resumeSession - Translating last AI message: ${content.take(50)}...")

                        val translated = if (currentIsKannada) {
                            // App in Kannada - translate if message is in English
                            if (isTextInKannada(content)) {
                                content // Already Kannada
                            } else {
                                translationUseCase.translateToKannada(content)
                            }
                        } else {
                            // App in English - translate if message is in Kannada
                            if (isTextInKannada(content)) {
                                translationUseCase.translateToEnglish(content)
                            } else {
                                content // Already English
                            }
                        }

                        DebugLogger.debugLog("ChatViewModel", "resumeSession - Translation result: ${translated.take(50)}...")
                        message.copy(content = translated)
                    } else {
                        message
                    }
                }
            } else {
                DebugLogger.debugLog("ChatViewModel", "resumeSession - No AI messages found")
                result.messages
            }
        } else {
            DebugLogger.debugLog("ChatViewModel", "resumeSession - Skipping translation (isKannada=${_uiState.value.isKannada}, messages empty=${result.messages.isEmpty()})")
            result.messages
        }

        _uiState.update { it.copy(
            isSessionStarted = result.success,
            messages = translatedMessages,
            isLoading = false,
            agentMetadata = result.metadata,
            currentState = result.currentState
        ) }

        // Update progress using explicit currentState from resume response
        val resumedProgress = conceptProgressUseCase.calculateProgressPercentage(result.currentState, result.metadata)
        _uiState.update { it.copy(currentProgressPercentage = resumedProgress) }
    }

    /**
     * Starts a fresh session for the given concept by deleting any existing session mapping and then starting a new session.
     */
    private fun startFreshSession(concept: String) = launchSessionSetup {
        val sessionKey = resolveSessionConceptKey(concept)
        sessionUseCase.deleteSessionMapping(sessionKey)
        _uiState.update {
            ChatUiState(
                selectedConcept = sessionKey,
                availableConcepts = it.availableConcepts,
                currentLanguage = it.currentLanguage,
                studentLevel = it.studentLevel,
                isKannada = it.isKannada
            )
        }
        selectConceptInternal(sessionKey)
    }

    /**
     * Sends a message to the agent and handles the response.
     * It updates the UI state with the user's message, shows a loading indicator,
     * and then processes the agent's response to update the chat messages, autosuggestions,
     * and any resources that need to be displayed.
     * Translates autosuggestions based on current app language (bidirectional).
     */
    private fun sendMessage(message: String, fromSuggestion: Boolean) {
        if (message.isBlank()) return
        viewModelScope.launch {
            // Sync language state before processing message
            syncLanguageState()

            hideAutosuggestions(); markUserActive()
            _uiState.update { it.copy(
                messages = it.messages + sendMessageUseCase.createUserMessage(message),
                isLoading = true)
            }

            if (!fromSuggestion) {
                TrialSessionStore.activeTrialItemId?.let { trialItemId ->
                    planTrialProgressTracker.recordIncrement(trialItemId)
                    _uiState.value.selectedConcept?.let { conceptId ->
                        val turnIndex =
                            _uiState.value.messages.count { it.sender == "user" } + 1
                        GamificationAnalyticsTracker.studyTurn(conceptId, turnIndex)
                    }
                }
            }

            // Wait for navigation auto-start / concept switch before continuing the thread.
            sessionSetupJob?.join()

            if (!_uiState.value.isSessionStarted) {
                val concept = _uiState.value.selectedConcept
                if (concept.isNullOrBlank()) {
                    return@launch appendSessionError(null)
                }
                if (!startSession(concept).success) {
                    return@launch
                }
            }

            val currentIsKannada = _uiState.value.isKannada
            val response = sessionUseCase.continueSession(
                message,
                fromSuggestion,
                _uiState.value.studentLevel,
                currentIsKannada
            )

            if (!response.success) return@launch appendSessionError(response.httpStatusCode)

            // Smart translate autosuggestions based on current app language
            val translatedSuggestions = if (response.autosuggestions.isNotEmpty()) {
                if (currentIsKannada) {
                    // App in Kannada - translate suggestions if they're in English
                    translationUseCase.translateListToKannada(response.autosuggestions)
                } else {
                    // App in English - translate suggestions if they're in Kannada
                    response.autosuggestions.map { suggestion ->
                        if (isTextInKannada(suggestion)) {
                            translationUseCase.translateToEnglish(suggestion)
                        } else {
                            suggestion
                        }
                    }
                }
            } else {
                response.autosuggestions
            }

            _uiState.update { it.copy(
                autosuggestions = translatedSuggestions,
                agentMetadata = response.metadata,
                currentState = response.currentState,
                showAutosuggestions = false)
            }

            // Calculate and update progress in database
            val continuedProgress = conceptProgressUseCase.calculateProgressPercentage(
                response.currentState,
                response.metadata
            )
            _uiState.update { it.copy(currentProgressPercentage = continuedProgress) }

            // Look up concept entity to get conceptId for progress tracking
            // Match against both English and Kannada names to ensure progress is tracked in all languages
            val conceptEntity = conceptRepository.getAllConcepts().find {
                it.conceptName == _uiState.value.selectedConcept || 
                it.conceptNameKannada == _uiState.value.selectedConcept
            }

            // Route ALL progress updates through ProgressEventTracker so that:
            //   - chapter_agent_progress table is always recalculated
            //   - streak is updated
            //   - single source of truth for DB writes
            if (conceptEntity != null && userId.isNotEmpty()) {
                if (response.currentState?.uppercase() == "END") {
                    // Mark as COMPLETED when END node is reached
                    val lang = _uiState.value.currentLanguage.ifBlank { "en" }
                    progressEventTracker.markStudyCompleted(
                        studentId = userId,
                        conceptId = conceptEntity.conceptId,
                        language = lang
                    )
                    conceptProgressUseCase.markConceptCompleted(userId, conceptEntity.conceptId)
                    DebugLogger.debugLog("ChatViewModel", "Concept ${conceptEntity.conceptId} marked COMPLETED - END node reached [$lang]")
                } else {
                    // Mark as IN_PROGRESS for all intermediate exchanges
                    val lang = _uiState.value.currentLanguage.ifBlank { "en" }
                    progressEventTracker.markStudyInProgress(
                        studentId = userId,
                        conceptId = conceptEntity.conceptId,
                        language = lang
                    )
                }
            }

            val resourceShown = response.metadata?.let { metadata ->
                response.agentResponse?.let { agentText ->
                    handleResource(metadata, agentText)
                } ?: false
            } ?: false
            response.agentResponse?.let { agentText ->
                if (resourceShown) _uiState.update { it.copy(pendingAgentResponse = agentText) }
                else handleAgentMessage(agentText, response.metadata)
            }
            // Only set isLoading to false if no resource is being generated
            if (!resourceShown) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Appends an error message to the chat when something goes wrong with sending a message or processing the agent's response.
     */
    private fun appendSessionError(httpStatusCode: Int? = null) = _uiState.update {
        it.copy(
            messages = it.messages + sendMessageUseCase.createAIMessage(
                ErrorHandler.getStudyAgentErrorMessage(context, httpStatusCode)
            ),
            isLoading = false,
        )
    }
    /**
     * Processes the agent's response text,
     * updates the chat messages with the new response, and starts the typing animation.
     * Smart translation: translates based on current app language (bidirectional).
     */
    private fun handleAgentMessage(text: String, metadata: SessionMetadata?) {
        viewModelScope.launch {
            val cleaned = handleAgentResponseUseCase.processAgentResponse(text)

            // Smart translation based on current app language
            val currentIsKannada = _uiState.value.isKannada
            val displayText = if (currentIsKannada) {
                // App in Kannada - translate if response is in English
                if (isTextInKannada(cleaned)) {
                    cleaned // Already in Kannada
                } else {
                    DebugLogger.debugLog("ChatViewModel", "Translating agent response to Kannada...")
                    translationUseCase.translateToKannada(cleaned)
                }
            } else {
                // App in English - translate if response is in Kannada
                if (isTextInKannada(cleaned)) {
                    DebugLogger.debugLog("ChatViewModel", "Translating agent response to English...")
                    translationUseCase.translateToEnglish(cleaned)
                } else {
                    cleaned // Already in English
                }
            }

            _uiState.update { it.copy(messages = it.messages + sendMessageUseCase.createAIMessage(displayText), isTyping = true, typingText = "", fullTextForTTS = displayText, shouldStartTTS = true, isTypingComplete = false, showAutosuggestions = false) }
            typingAnimationController.startTypingAnimation(displayText, viewModelScope) { typingText, complete ->
                _uiState.update { it.copy(
                    typingText = typingText,
                    isTyping = !complete,
                    isTypingComplete = complete,
                    shouldStartTTS = if (complete) false
                                    else it.shouldStartTTS)
                }
            }
        }
    }

    /**
     * Determines if any resource (like an image or concept map)
     * should be shown based on the agent's response and session metadata.
     */
    private fun handleResource(metadata: SessionMetadata, agentResponse: String): Boolean {
        return when (
            val decision = resourceDecisionUseCase.decide(metadata)) {
            is ResourceDecision.ShowImage -> {
                DebugLogger.debugLog("ChatViewModel", "Showing image: ${decision.url}")
                startImageResource(decision.url, decision.description)
                true
            }
            is ResourceDecision.ShowConceptMap -> {
                DebugLogger.debugLog("ChatViewModel", "Generating concept map")
                generateAndShowConceptMap(agentResponse)
                true
            }
            ResourceDecision.None -> {
                DebugLogger.debugLog("ChatViewModel", "No resource to show")
                false
            }
        }
    }

    /**
     * Starts a timer to show an image resource for a certain duration.
     */
    private fun startImageResource(url: String, description: String?, duration: Int=10) = startResource(duration) { remaining ->
        ResourceCardUiState.Image(url, description, remaining, duration)
    }

    private fun generateAndShowConceptMap(agentResponse: String) {
        viewModelScope.launch {
            // Show loading message "Generating concept map..." in ChatContentArea
            _uiState.update { it.copy(
                loadingResourceMessage = "Generating concept map...",
                isLoading = true
            ) }

            // Generate concept map from agent response
            val result = conceptMapUseCase.generateConceptMap(
                aiResponse = agentResponse,
                language = _uiState.value.currentLanguage
            )

            // Clear loading state
            _uiState.update { it.copy(
                loadingResourceMessage = null,
                isLoading = false
            ) }

            if (result.success && result.json != null && !result.isDefault) {
                // Successfully generated concept map - show ResourceCard
                DebugLogger.debugLog("ChatViewModel", "Concept map generated successfully")
                startConceptMap(result.json)
            } else {
                // Failed to generate or got default map - log error and show agent message normally
                val errorMsg = if (result.isDefault)
                    "Default concept map detected - skipping"
                else
                    "Error generating concept map"

                DebugLogger.debugLog("ChatViewModel", errorMsg)
                _uiState.update { it.copy(conceptMapStatus = errorMsg) }

                // Show the pending agent message with typing animation
                val pending = _uiState.value.pendingAgentResponse
                pending?.let {
                    _uiState.update { it.copy(pendingAgentResponse = null) }
                    handleAgentMessage(it, _uiState.value.agentMetadata)
                }
            }
        }
    }

    /**
     * Starts a timer to show a concept map resource for a certain duration, using the provided JSON data.
     */
    private fun startConceptMap(json: String, duration: Int = 10) = startResource(duration) { remaining ->
        ResourceCardUiState.ConceptMap(json, 0f, false, remaining, duration)
    }


    /**
     * Starts a timer to display a resource (image or concept map) for a specified duration.
     */
    private fun startResource(duration: Int, builder: (Int) -> ResourceCardUiState) {
        // Immediately show the resource card
        _uiState.update {
            it.copy(
                resourceCardState = builder(duration),
                loadingResourceMessage = null,
                isLoading = false,
                ttsPausedForResource = true
            )
        }

        DebugLogger.debugLog("ChatViewModel", "ResourceCard shown: ${_uiState.value.resourceCardState}")

        // Start the countdown timer
        resourceController.startTimer(viewModelScope, duration,
            onTick = { remaining -> _uiState.update {
                it.copy(resourceCardState = builder(remaining),
                    loadingResourceMessage = null,
                    ttsPausedForResource = true) } },
            onFinish = { dismissResource() }
        )
    }

    /**
     * Dismisses the currently displayed resource and resets the related UI state.
     */
    private fun dismissResource() {
        resourceController.cancel()
        val pending = _uiState.value.pendingAgentResponse
        _uiState.update { it.copy(resourceCardState = ResourceCardUiState.Hidden, ttsPausedForResource = false, pendingAgentResponse = null, loadingResourceMessage = null, isLoading = false) }
        pending?.let { handleAgentMessage(it, _uiState.value.agentMetadata) }
    }

    /**
     * Resumes TTS playback if it was paused for a resource.
     * This is called when a resource is dismissed to allow the agent's response to be read aloud again.
     */
    private fun resumeTTS() = _uiState.update { it.copy(ttsPausedForResource = false) }

    /**
     * Handles avatar change with proper validation and delegation to use case
     * Returns updated ChatBotSettingsState with both code and display name
     * @param displayName The localized display name from UI
     * @param boyDisplayName The localized "boy" string
     * @param girlDisplayName The localized "girl" string
     * @param ttsController The TTS controller to apply voice changes
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

        // Apply avatar change through use case
        val normalizedCode = avatarChangeUseCase.changeAvatar(
            avatarCode = avatarCode,
            ttsController = ttsController,
            currentLanguage = _uiState.value.currentLanguage
        )

        // Return updated state with both code and display name
        return currentState.copy(
            selectedAvatar = normalizedCode,
            selectedAvatarDisplayName = displayName
        )
    }

    /**
     * Initialize settings state with proper display name for current avatar
     * @param avatarCode Current avatar code
     * @param boyDisplayName Localized "boy" string
     * @param girlDisplayName Localized "girl" string
     * @param disableDisplayName Localized "disable" string
     * @param currentState Current settings state
     * @return Updated state with display name
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

    /**
     * Helper function to detect if text contains Kannada characters
     * Kannada Unicode range: \u0C80-\u0CFF
     */
    private fun isTextInKannada(text: String): Boolean {
        return text.any { it in '\u0C80'..'\u0CFF' }
    }
}