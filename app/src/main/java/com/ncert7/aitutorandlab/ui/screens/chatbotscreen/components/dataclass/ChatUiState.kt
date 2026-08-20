package com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass

import com.ncert7.aitutorandlab.data.remote.SessionMetadata
/**
 * Consolidated UI state for the chat screen
 * Reduces number of StateFlows from 20+ to just 2-3
 */
data class ChatUiState(
    // Messages
    val messages: List<ChatMessageModel> = emptyList(),
    val inputText: String = "",

    // Loading & Typing
    val isLoading: Boolean = false,
    val isTyping: Boolean = false,
    val typingText: String = "",

    // Session
    val isSessionStarted: Boolean = false,
    val selectedConcept: String? = null,
    val availableConcepts: List<String> = emptyList(),
    val displayConcepts: List<String> = emptyList(),

    // Auto-suggestions
    val autosuggestions: List<String> = emptyList(),
    val showAutosuggestions: Boolean = false,
    val isUserActive: Boolean = false,

    // Resources
    val resourceCardState: ResourceCardUiState = ResourceCardUiState.Hidden,

    // TTS
    val shouldStartTTS: Boolean = false,
    val fullTextForTTS: String = "",
    val ttsPausedForResource: Boolean = false,

    // Typing Animation State
    val isTypingComplete: Boolean = false,
    val waitingForTTSToComplete: Boolean = false,

    // Settings
    val studentLevel: String = "medium",
    val isKannada: Boolean = false,
    val currentLanguage: String = "en",

    // Metadata
    val agentMetadata: SessionMetadata? = null,
    val currentState: String? = null,
    // Pending Message Queue (for messages received while resource card is showing)
    val pendingAgentResponse: String? = null,
    val waitingForResourceCardDismiss: Boolean = false,

    // Concept Map Generation Status
    val conceptMapStatus: String? = null,

    // Resource Loading Message (shown instead of thinking text when resource is being loaded)
    val loadingResourceMessage: String? = null,

    // Session Resume Dialog (for ConceptScreen) - stores conceptName when dialog should be shown
    val pendingConceptForDialog: String? = null,
    val currentProgressPercentage: Int = 0,

    // Session Resume Dialog for Revision Screen
    val showSessionResumeDialog: Boolean = false

)

/**
 * Represents the last AI message for quick access
 */
val ChatUiState.lastAiMessage: ChatMessageModel?
    get() = messages.findLast { it.sender.lowercase() == "ai" }

/**
 * Check if conversation has started.
 * Error-only messages (e.g. server failure before any agent reply) do not count, so the idle
 * full-body avatar stays visible — matching Maths session-start failures.
 */
val ChatUiState.isConversationStarted: Boolean
    get() = messages.any { !it.isError }

