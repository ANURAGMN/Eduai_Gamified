package com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ncert7.aitutorandlab.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ncert7.aitutorandlab.ui.components.LoadingInsightPanel
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatMessageModel
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.viewModel.TextToSpeech

/**
 * Main content area showing either loading or agent message
 */
@Composable
fun ChatContentArea(
    isLoading: Boolean,
    loadingResourceMessage: String?,
    lastAIMessage: ChatMessageModel?,
    isTyping: Boolean,
    typingText: String,
    ttsController: TextToSpeech,
    isResourceCardShowing: Boolean = false,
    languageCode: String = "en",
    modifier: Modifier = Modifier
) {
    val dimens= LocalDimensions.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        when {
            // Loading State
            isLoading -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopStart
                ) {
                    LoadingInsightPanel(
                        statusText = loadingResourceMessage ?: stringResource(R.string.thinking),
                        languageCode = languageCode,
                        rotateThinking = loadingResourceMessage == null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimens.spaceMedium),
                    )
                }
            }

            // Agent Message (hidden when ResourceCard is showing)
            !isResourceCardShowing -> {
                // Fade as a fraction of container height — fully dynamic, no hardcoded sizes
                val fadeFraction = 0.12f

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (lastAIMessage != null) {
                        AgentMessage(
                            text = if (isTyping) typingText else lastAIMessage.content,
                            isTyping = isTyping,
                            typingText = typingText,
                            fullText = lastAIMessage.content,
                            ttsController = ttsController,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Top fade — height = fadeFraction of parent
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fadeFraction)
                            .align(Alignment.TopCenter)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.95f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Bottom fade — height = fadeFraction of parent
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fadeFraction)
                            .align(Alignment.BottomCenter)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.95f)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}
