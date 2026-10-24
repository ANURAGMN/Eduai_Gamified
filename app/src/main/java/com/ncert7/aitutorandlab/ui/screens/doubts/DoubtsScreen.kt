package com.ncert7.aitutorandlab.ui.screens.doubts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ncert7.aitutorandlab.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoubtsBottomSheet(
    subjectName: String,
    chapterName: String,
    /** Prefills the question field (e.g. topic heading); user types after it. */
    initialQuestion: String = "",
    onDismiss: () -> Unit,
    viewModel: DoubtsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var questionText by remember(initialQuestion) {
        mutableStateOf(
            if (initialQuestion.isBlank()) {
                ""
            } else {
                initialQuestion.trimEnd() + " "
            },
        )
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    LaunchedEffect(state.answer, state.error) {
        if (state.answer != null || state.error != null) {
            delay(100)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    ModalBottomSheet(onDismissRequest = {
        viewModel.resetState()
        onDismiss()
    }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp, top = 8.dp)
        ) {
            Text(
                stringResource(R.string.ask_ai_doubt),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.ask_ai_doubt_from, subjectName, chapterName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = questionText,
                onValueChange = { questionText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        stringResource(R.string.ask_ai_doubt_hint),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                },
                maxLines = 4,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.askDoubt(questionText, subjectName, chapterName)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = questionText.isNotBlank() && !state.isLoading,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        stringResource(R.string.ask_ai_doubt_submit),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (state.answer != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.ask_ai_doubt_response),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = cleanAiText(state.answer!!),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (state.error != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = cleanAiText(state.error!!),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Strip raw Markdown and LaTeX symbols so the AI response renders cleanly
 * in a standard Compose Text field.
 */
private fun cleanAiText(input: String): String {
    return input
        .replace("**", "")
        .replace("`", "")
        .replace("$", "")
        .replace(Regex("(?m)^###\\s*"), "")
        .replace(Regex("(?m)^##\\s*"), "")
        .replace(Regex("(?m)^#\\s*"), "")
        .replace(Regex("(?m)^\\*\\s"), "• ")
        .replace(Regex("(?m)^-\\s"), "• ")
        .trim()
}
