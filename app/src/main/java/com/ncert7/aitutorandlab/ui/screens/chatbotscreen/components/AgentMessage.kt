package com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.text.TextProcessor
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.text.TextWithHighlights
import com.ncert7.aitutorandlab.ui.theme.White
import com.ncert7.aitutorandlab.ui.viewModel.TextToSpeech
import kotlin.math.roundToInt

/**
 * Composable for displaying agent message with auto scrolling
 * Shows a single message space that updates with new responses
 * Auto-scrolls to CENTER the TTS highlighted word in the viewport
 */
@Composable
fun AgentMessage(
    modifier: Modifier = Modifier,
    text: String,
    isTyping: Boolean = false,
    typingText: String = "",
    fullText: String = text,
    ttsController: TextToSpeech = viewModel(),
    reduceTextSize: Boolean = false,
    messageFontSize: TextUnit? = null,
    messageLineHeight: TextUnit? = null,
) {
    val scrollState = rememberScrollState()
    val ttsState by ttsController.state.collectAsState()
    val currentWordIndex by ttsController.currentWordIndex.collectAsState()

    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var containerHeight by remember { mutableStateOf(0) }

    val processor = remember { TextProcessor() }
    val processed = remember(fullText) {
        processor.process(fullText)
    }

    // Auto-scroll to CENTER current word
    LaunchedEffect(currentWordIndex, ttsState.isSpeaking, textLayout, containerHeight) {
        if (!ttsState.isSpeaking || currentWordIndex < 0 || textLayout == null || containerHeight == 0) {
            return@LaunchedEffect
        }

        val layout = textLayout!!
        val words = processed.wordPositions

        if (currentWordIndex !in words.indices) return@LaunchedEffect

        val word = words[currentWordIndex]
        val lineIndex = layout.getLineForOffset(word.start)

        val lineTop = layout.getLineTop(lineIndex)
        val lineBottom = layout.getLineBottom(lineIndex)
        val lineCenter = (lineTop + lineBottom) / 2

        val viewportCenter = containerHeight / 2f
        val targetScroll = (lineCenter - viewportCenter).coerceAtLeast(0f)

        scrollState.animateScrollTo(
            targetScroll.roundToInt(),
            animationSpec = tween(300)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .background(
                color = White,
                shape = RoundedCornerShape(12.dp)
            )
            .onGloballyPositioned { coordinates ->
                // Track container height for centering calculation
                containerHeight = coordinates.size.height
            }
            .verticalScroll(scrollState)
    ) {
        Column {
            Spacer(modifier = Modifier.height(if (reduceTextSize) 12.dp else 8.dp))
            // TextWithHighlights final display
            TextWithHighlights(
                text = if (isTyping) typingText else fullText,
                isTyping = isTyping,
                fullText = fullText,
                ttsController = ttsController,
                onTextLayout = { textLayout = it },
                reduceTextSize = reduceTextSize,
                messageFontSize = messageFontSize,
                messageLineHeight = messageLineHeight,
            )
            Spacer(modifier = Modifier.height(if (reduceTextSize) 12.dp else 8.dp))
        }
    }
}