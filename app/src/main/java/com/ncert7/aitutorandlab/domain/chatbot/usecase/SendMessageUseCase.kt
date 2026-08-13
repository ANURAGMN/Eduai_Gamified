package com.ncert7.aitutorandlab.domain.chatbot.usecase

import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatMessageModel
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
) {

    fun createUserMessage(content: String): ChatMessageModel {
        return ChatMessageModel(
            sender = "user",
            content = content,
        )
    }

    fun createAIMessage(content: String, isError: Boolean = false, canRetry: Boolean = false): ChatMessageModel {
        return ChatMessageModel(
            sender = "ai",
            content = content,
            isError = isError,
            canRetry = canRetry,
        )
    }
}
