package com.ncert7.aitutorandlab.domain.chatbot.model

import com.ncert7.aitutorandlab.data.remote.SessionMetadata
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatMessageModel

data class SessionResult(
    val success: Boolean,
    val autosuggestions: List<String> = emptyList(),
    val agentResponse: String? = null,
    val metadata: SessionMetadata? = null,
    val messages: List<ChatMessageModel> = emptyList(),
    val currentState: String? = null,
    /** HTTP status from the agent API when [success] is false; null for local/non-HTTP failures. */
    val httpStatusCode: Int? = null,
)

data class SessionData(
    val threadId: String,
    val sessionId: String?,
    val messages: List<ChatMessageModel>
)
