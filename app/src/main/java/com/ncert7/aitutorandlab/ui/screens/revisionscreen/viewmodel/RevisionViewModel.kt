package com.ncert7.aitutorandlab.ui.screens.revisionscreen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncert7.aitutorandlab.data.local.dao.ChapterDao
import com.ncert7.aitutorandlab.data.local.dao.ConceptDao
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.domain.chatbot.controller.TypingAnimationController
import com.ncert7.aitutorandlab.domain.chatbot.usecase.AvatarChangeUseCase
import com.ncert7.aitutorandlab.domain.revisionagent.usecase.RevisionUseCase
import com.ncert7.aitutorandlab.domain.chatbot.usecase.TranslationUseCase
import com.ncert7.aitutorandlab.domain.examplan.PlanTrialProgressTracker
import com.ncert7.aitutorandlab.domain.examplan.TrialSessionStore
import com.ncert7.aitutorandlab.domain.progress.ProgressEventTracker
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatBotSettingsState
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatMessageModel
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatUiState
import com.ncert7.aitutorandlab.ui.viewModel.TextToSpeech
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import com.ncert7.aitutorandlab.utils.isKannada
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for handling revision chat sessions.
 * Similar to ChatViewModel but focused on revision-specific functionality.
 */
@HiltViewModel
class RevisionViewModel @Inject constructor(
    private val revisionUseCase: RevisionUseCase,
    private val typingAnimationController: TypingAnimationController,
    private val translationUseCase: TranslationUseCase,
    private val avatarChangeUseCase: AvatarChangeUseCase,
    private val progressEventTracker: ProgressEventTracker,
    private val planTrialProgressTracker: PlanTrialProgressTracker,
    private val chapterDao: ChapterDao,
    private val conceptDao: ConceptDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var userId = ""
    private var initialized = false
    private var currentChapterId = ""
    private var currentRevisionId = ""

    /**
     * Initialize the ViewModel with userId and chapterId, then check for existing session
     */
    fun initialize(userId: String, chapterId: String) {
        if (initialized) return
        initialized = true
        this.userId = userId
        this.currentChapterId = chapterId

        // Use LocalizationUtils for language detection
        val appLanguage = getCurrentLanguageCode()
        val isKannadaMode = isKannada()

        _uiState.update {
            it.copy(
                isKannada = isKannadaMode,
                currentLanguage = appLanguage,
                selectedConcept = chapterId
            )
        }

        DebugLogger.debugLog("RevisionViewModel", "Initialized with chapterId: $chapterId, userId: $userId, language: $appLanguage, isKannada: $isKannadaMode")

        // Fetch revisionId from database and check for existing session
        viewModelScope.launch {
            try {
                val chapterEntity = chapterDao.getChapter(chapterId)
                if (chapterEntity != null) {
                    currentRevisionId = chapterEntity.revisionId
                    DebugLogger.debugLog("RevisionViewModel", "Fetched revisionId: $currentRevisionId for chapterId: $chapterId")

                    // Check if there's an existing session
                    val existingThreadId = revisionUseCase.getRevisionThreadId(currentRevisionId)
                    if (existingThreadId != null) {
                        DebugLogger.debugLog("RevisionViewModel", "Found existing session for revisionId: $currentRevisionId, showing dialog")
                        // Show dialog to ask continue or start fresh
                        _uiState.update { it.copy(showSessionResumeDialog = true) }
                    } else {
                        DebugLogger.debugLog("RevisionViewModel", "No existing session found, starting new revision session")
                        // No existing session, start directly
                        autoStartRevision(currentRevisionId)
                    }
                } else {
                    DebugLogger.errorLog("RevisionViewModel", "Could not find chapter in database: $chapterId")
                }
            } catch (e: Exception) {
                DebugLogger.errorLog("RevisionViewModel", "Error fetching revisionId: ${e.message}")
            }
        }
    }


    /**
     * Change to a different chapter
     */
    fun changeChapter(newChapterId: String) = viewModelScope.launch {
        if (newChapterId == currentChapterId) return@launch

        DebugLogger.debugLog("RevisionViewModel", "Changing chapter from '$currentChapterId' to '$newChapterId'")

        // Delete old session
        revisionUseCase.deleteRevisionSessionMapping(currentRevisionId)

        // Reset state and fetch new revisionId
        currentChapterId = newChapterId
        _uiState.update {
            ChatUiState(
                selectedConcept = newChapterId,
                isKannada = it.isKannada,
                currentLanguage = it.currentLanguage
            )
        }

        // Fetch revisionId for new chapter from database
        try {
            val chapterEntity = chapterDao.getChapter(newChapterId)
            if (chapterEntity != null) {
                currentRevisionId = chapterEntity.revisionId
                autoStartRevision(currentRevisionId)
            } else {
                DebugLogger.errorLog("RevisionViewModel", "Could not find chapter in database: $newChapterId")
            }
        } catch (e: Exception) {
            DebugLogger.errorLog("RevisionViewModel", "Error fetching revisionId for new chapter: ${e.message}")
        }
    }

    /**
     * Auto-start revision session for the given revisionId
     */
    private fun autoStartRevision(revisionId: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, selectedConcept = currentChapterId) }

        // Check if there's an existing session using revisionId
        val existingThreadId = revisionUseCase.getRevisionThreadId(revisionId)
        if (existingThreadId != null) {
            DebugLogger.debugLog("RevisionViewModel", "Found existing revision session for revisionId: $revisionId, resuming...")
            resumeRevisionSession(existingThreadId)
        } else {
            DebugLogger.debugLog("RevisionViewModel", "No existing session found for revisionId: $revisionId, starting new revision session")
            startRevisionSession(revisionId)
        }
    }

    /**
     * Start a new revision session using revisionId
     */
    private suspend fun startRevisionSession(revisionId: String) {
        DebugLogger.debugLog("RevisionViewModel", "startRevisionSession called for revisionId: $revisionId")
        val currentIsKannada = _uiState.value.isKannada
        val result = revisionUseCase.startRevisionSession(revisionId, userId, currentIsKannada)

        if (!result.success) {
            DebugLogger.errorLog("RevisionViewModel", "Failed to start revision session for revisionId: $revisionId")
            return appendRevisionError()
        }

        DebugLogger.debugLog("RevisionViewModel", "Revision session started successfully")

        val agentResponse = result.agentResponse ?: ""

        // Smart translation: Only translate if needed based on current app language
        val translatedResponse = if (currentIsKannada) {
            // App is in Kannada - translate if response is in English
            if (isTextInKannada(agentResponse)) {
                agentResponse // Already in Kannada
            } else {
                translationUseCase.translateToKannada(agentResponse)
            }
        } else {
            // App is in English - translate if response is in Kannada
            if (isTextInKannada(agentResponse)) {
                translationUseCase.translateToEnglish(agentResponse)
            } else {
                agentResponse // Already in English
            }
        }

        // Mark revision as COMPLETED when session starts (only STUDY + MATH concepts, NOT simulation)
        viewModelScope.launch {
            try {
                val lang = if (_uiState.value.isKannada) "kn" else "en"
                val studyConcepts = conceptDao.getConceptsForChapterSync(currentChapterId, "STUDY")
                val mathConcepts = conceptDao.getConceptsForChapterSync(currentChapterId, "MATH PROBLEM")
                val conceptsToMark = studyConcepts + mathConcepts
                conceptsToMark.forEach { concept ->
                    progressEventTracker.markRevisionCompleted(userId, concept.conceptId, lang)
                }
                DebugLogger.debugLog("RevisionViewModel", "Marked ${conceptsToMark.size} concepts as revision-completed (STUDY=${studyConcepts.size}, MATH=${mathConcepts.size}) for revisionId: $revisionId")
            } catch (e: Exception) {
                DebugLogger.errorLog("RevisionViewModel", "Error tracking revision completion on start: ${e.message}")
            }
        }

        val aiMessage = ChatMessageModel(
            sender = "ai",
            content = translatedResponse,
        )

        _uiState.update {
            it.copy(
                isSessionStarted = true,
                messages = listOf(aiMessage),
                isLoading = false,
                currentState = result.currentState,
                isTyping = true,
                typingText = "",
                fullTextForTTS = translatedResponse,
                shouldStartTTS = true,
                isTypingComplete = false
            )
        }

        // Start typing animation
        typingAnimationController.startTypingAnimation(
            fullText = translatedResponse,
            scope = viewModelScope
        ) { currentText, isComplete ->
            _uiState.update { state ->
                if (isComplete) {
                    state.copy(
                        isTyping = false,
                        typingText = "",
                        isTypingComplete = true,
                        shouldStartTTS = false
                    )
                } else {
                    state.copy(typingText = currentText)
                }
            }
        }
    }

    /**
     * Simple heuristic to detect if text contains Kannada characters
     */
    private fun isTextInKannada(text: String): Boolean {
        // Kannada Unicode range: \u0C80-\u0CFF
        return text.any { it in '\u0C80'..'\u0CFF' }
    }

    /**
     * Resume an existing revision session
     * Translation is already handled by RevisionUseCase based on current app language
     */
    private suspend fun resumeRevisionSession(threadId: String) {
        val result = revisionUseCase.resumeRevisionSession(threadId, null)

        if (!result.success) {
            DebugLogger.errorLog("RevisionViewModel", "Failed to resume revision session")
            return appendRevisionError()
        }

        // Messages are already translated by RevisionUseCase based on current app language
        _uiState.update {
            it.copy(
                isSessionStarted = result.success,
                messages = result.messages,
                isLoading = false,
                currentState = result.currentState
            )
        }
    }

    /**
     * Send a user message in the revision session
     */
    fun sendMessage(message: String) = viewModelScope.launch {
        if (message.isBlank()) return@launch

        val userMessage = ChatMessageModel(
            sender = "user",
            content = message,
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isLoading = true
            )
        }

        // Send message to revision agent with current language setting using revisionId
        val isKannada = _uiState.value.isKannada
        val result = revisionUseCase.continueRevisionSession(currentRevisionId, message, isKannada)

        if (!result.success) {
            DebugLogger.errorLog("RevisionViewModel", "Failed to send message in revision session")
            return@launch appendRevisionError()
        }

        val agentResponse = result.agentResponse ?: ""

        // Smart translation: Only translate if needed based on current app language
        val translatedResponse = if (isKannada) {
            // App is in Kannada - translate if response is in English
            if (isTextInKannada(agentResponse)) {
                agentResponse // Already in Kannada
            } else {
                translationUseCase.translateToKannada(agentResponse)
            }
        } else {
            // App is in English - translate if response is in Kannada
            if (isTextInKannada(agentResponse)) {
                translationUseCase.translateToEnglish(agentResponse)
            } else {
                agentResponse // Already in English
            }
        }

        val aiMessage = ChatMessageModel(
            sender = "ai",
            content = translatedResponse,
        )

        _uiState.update {
            it.copy(
                messages = it.messages + aiMessage,
                isLoading = false,
                currentState = result.currentState,
                isTyping = true,
                typingText = "",
                fullTextForTTS = translatedResponse,
                shouldStartTTS = true,
                isTypingComplete = false
            )
        }

        // Mark revision COMPLETED when session reaches END state (only STUDY + MATH, NOT simulation)
        if (result.currentState?.uppercase() == "END") {
            viewModelScope.launch {
                try {
                    val lang = if (isKannada) "kn" else "en"
                    val studyConcepts = conceptDao.getConceptsForChapterSync(currentChapterId, "STUDY")
                    val mathConcepts = conceptDao.getConceptsForChapterSync(currentChapterId, "MATH PROBLEM")
                    val conceptsToMark = studyConcepts + mathConcepts
                    conceptsToMark.forEach { concept ->
                        progressEventTracker.markRevisionCompleted(userId, concept.conceptId, lang)
                    }
                    DebugLogger.debugLog("RevisionViewModel", "Revision END reached: marked ${conceptsToMark.size} concepts as revision-completed (STUDY=${studyConcepts.size}, MATH=${mathConcepts.size})")
                    TrialSessionStore.activeTrialItemId?.let { trialItemId ->
                        planTrialProgressTracker.recordGeReached(trialItemId)
                    }
                } catch (e: Exception) {
                    DebugLogger.errorLog("RevisionViewModel", "Error marking revision complete on END: ${e.message}")
                }
            }
        }

        // Start typing animation
        typingAnimationController.startTypingAnimation(
            fullText = translatedResponse,
            scope = viewModelScope
        ) { currentText, isComplete ->
            _uiState.update { state ->
                if (isComplete) {
                    state.copy(
                        isTyping = false,
                        typingText = "",
                        isTypingComplete = true,
                        shouldStartTTS = false
                    )
                } else {
                    state.copy(typingText = currentText)
                }
            }
        }
    }

    /**
     * Update input text
     */
    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    /**
     * Soft exit from the time-based proceed overlay.
     * Does not force the trial item DONE — revision completion still requires real END/GE.
     * @return true if the item is already DONE (caller must not soft-proceed past celebration).
     */
    suspend fun recordTrialProceed(): Boolean {
        val trialItemId = TrialSessionStore.activeTrialItemId ?: return false
        planTrialProgressTracker.reconcileCompletion(trialItemId)
        val done = planTrialProgressTracker.isDone(trialItemId)
        DebugLogger.debugLog(
            "RevisionViewModel",
            "Trial soft proceed for item $trialItemId done=$done",
        )
        return done
    }

    /**
     * Resume an existing revision session
     */
    fun resumeExistingSession() = viewModelScope.launch {
        _uiState.update { it.copy(showSessionResumeDialog = false) }

        val existingThreadId = revisionUseCase.getRevisionThreadId(currentRevisionId)
        if (existingThreadId != null) {
            DebugLogger.debugLog("RevisionViewModel", "Resuming existing revision session for revisionId: $currentRevisionId")
            resumeRevisionSession(existingThreadId)
        } else {
            DebugLogger.errorLog("RevisionViewModel", "Could not find existing thread for revisionId: $currentRevisionId")
        }
    }

    /**
     * Start a fresh revision session (delete old and start new)
     */
    fun startFreshSession() = viewModelScope.launch {
        _uiState.update { it.copy(showSessionResumeDialog = false) }

        DebugLogger.debugLog("RevisionViewModel", "Starting fresh revision session, deleting old session for revisionId: $currentRevisionId")
        revisionUseCase.deleteRevisionSessionMapping(currentRevisionId)
        autoStartRevision(currentRevisionId)
    }


    /**
     * Handle avatar change
     */
    fun handleAvatarChange(
        displayName: String,
        boyDisplayName: String,
        girlDisplayName: String,
        ttsController: TextToSpeech,
        currentState: ChatBotSettingsState
    ): ChatBotSettingsState {
        val avatarCode = avatarChangeUseCase.getAvatarCodeFromDisplayName(
            displayName = displayName,
            boyDisplayName = boyDisplayName,
            girlDisplayName = girlDisplayName
        )

        val normalizedCode = avatarChangeUseCase.changeAvatar(
            avatarCode = avatarCode,
            ttsController = ttsController,
            currentLanguage = _uiState.value.currentLanguage
        )

        return currentState.copy(
            selectedAvatar = normalizedCode,
            selectedAvatarDisplayName = displayName
        )
    }

    /**
     * Initialize avatar display name
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
     * Check if there's an existing revision session for a revisionId
     */
    fun hasExistingSession(revisionId: String): Boolean {
        return revisionUseCase.getRevisionThreadId(revisionId) != null
    }

    private fun appendRevisionError() {
        val kn = _uiState.value.isKannada
        val text = if (kn) {
            "ಸರ್ವರ್ ದೋಷ. ದಯವಿಟ್ಟು ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ."
        } else {
            "Server error. Please try again."
        }
        _uiState.update {
            it.copy(
                messages = it.messages + ChatMessageModel(
                    sender = "ai",
                    content = text,
                    isError = true,
                    canRetry = true,
                ),
                isLoading = false,
            )
        }
    }
}