package com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ncert7.aitutorandlab.domain.chatbot.usecase.TextHighlightUseCase
import com.ncert7.aitutorandlab.domain.chatbot.usecase.TextProcessingUseCase
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.ChatMessageFontSize
import com.ncert7.aitutorandlab.ui.theme.AccentBlue
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.ui.viewModel.TextToSpeech

@Composable
fun TextWithHighlights(
    modifier: Modifier = Modifier,
    text: String,
    isTyping: Boolean = false,
    fullText: String = text,
    ttsController: TextToSpeech = viewModel(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    reduceTextSize: Boolean = false,
    /** When set, overrides the default/compact sizes (used for learner font preference). */
    messageFontSize: TextUnit? = null,
    messageLineHeight: TextUnit? = null,
) {
    val currentWordIndex by ttsController.currentWordIndex.collectAsState()
    val ttsState by ttsController.state.collectAsState()

    val textProcessor = remember { TextProcessingUseCase() }
    val highlightUseCase = remember { TextHighlightUseCase(textProcessor) }

    val highlightResult = remember(fullText, text, isTyping, currentWordIndex, ttsState.isSpeaking) {
        val shouldHighlight = ttsState.isSpeaking && ttsController.currentSpeakingText == fullText
        highlightUseCase.build(text, fullText, isTyping, currentWordIndex, shouldHighlight)
    }

    val styledText = buildAnnotatedString {
        append(highlightResult.displayText)

        highlightResult.boldRanges.forEach { range ->
            val start = range.first
            val end = minOf(range.last + 1, highlightResult.displayText.length)
            if (start < highlightResult.displayText.length) {
                addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary), start, end)
            }
        }

        highlightResult.highlightRange?.let { range ->
            val start = range.first
            val end = minOf(range.last + 1, highlightResult.displayText.length)
            if (start < highlightResult.displayText.length) {
                addStyle(
                    SpanStyle(background = AccentBlue, color = Color.White, fontWeight = FontWeight.Bold),
                    start,
                    end
                )
            }
        }
    }

    val defaultFont =
        when {
            messageFontSize != null -> messageFontSize
            reduceTextSize -> 16.sp
            else -> ChatMessageFontSize.DEFAULT_SP.sp
        }
    val defaultLineHeight =
        when {
            messageLineHeight != null -> messageLineHeight
            reduceTextSize -> 16.sp
            else -> ChatMessageFontSize.lineHeightSp(ChatMessageFontSize.DEFAULT_SP).sp
        }

    Text(
        text = styledText,
        fontSize = defaultFont,
        lineHeight = defaultLineHeight,
        color = TextPrimary,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier,
        onTextLayout = onTextLayout
    )
}