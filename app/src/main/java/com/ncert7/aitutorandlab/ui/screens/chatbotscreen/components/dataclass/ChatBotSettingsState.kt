package com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass

import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.ChatMessageFontSize

data class ChatBotSettingsState(
    val selectedAvatar: String = "disable",
    val selectedAvatarDisplayName: String = "", // Localized display name for avatar
    val selectedSpeed: String = "1.0x",
    val selectedStudentLevel: String = "medium",
    val voiceOptions: List<String> = emptyList(),
    val displayedVoiceName: String = "",
    val availableConcepts: List<String> = emptyList(),//available concepts fetched from backend
    val displayConcepts: List<String> = emptyList(), //translated concept names
    val selectedConcept: String? = null,
    val isLoadingConcepts: Boolean = false,
    /** Base chat message font size in sp (Math applies −0.5sp when rendering). Default XS. */
    val messageFontSp: Float = ChatMessageFontSize.DEFAULT_SP,
)
