package com.ncert7.aitutorandlab.ui.screens.doubts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoubtsBottomSheet(
    subjectName: String,
    chapterName: String,
    onDismiss: () -> Unit,
    viewModel: DoubtsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var questionText by remember { mutableStateOf("") }

    // Tools for scrolling and hiding the keyboard
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    // Automatically scroll to the bottom when the AI gives a response or error
    LaunchedEffect(state.answer, state.error) {
        if (state.answer != null || state.error != null) {
            delay(100) // Brief delay to let the UI render the new text box before calculating scroll bounds
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
            // Reduced title size for better visual hierarchy
            Text("Ask AI a Doubt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("From: $subjectName - $chapterName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = questionText,
                onValueChange = { questionText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "E.g., How do plants eat without mouths?",
                        // Faded placeholder text
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                },
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    keyboardController?.hide() // Force keyboard down on click
                    viewModel.askDoubt(questionText, subjectName, chapterName)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = questionText.isNotBlank() && !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Ask Question", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Display Answer securely inside a stylized UI box
            if (state.answer != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "AI Response",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.answer!!,
                            style = MaterialTheme.typography.bodyLarge, // Increased text size
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Display Error inside a stylized red/error box
            if (state.error != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Extra spacer so it scrolls nicely past the keyboard/navigation bar
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}