package com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.ui.theme.AccentBlue
import com.ncert7.aitutorandlab.ui.theme.HeaderGradientStart
import com.ncert7.aitutorandlab.ui.theme.IconPrimary
import com.ncert7.aitutorandlab.ui.theme.IconSecondary
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.ui.theme.White
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatUiState
import com.ncert7.aitutorandlab.ui.viewModel.SpeechToText

/**
 * Comprehensive input section that handles:
 * - Auto-suggestions display
 * - Text input field with image, mic, and send buttons
 * - Listening overlay for speech-to-text
 */
@Composable
fun InputSection(
    chatState: ChatUiState,
    sttState: SpeechToText.STTState,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onSpeakClick: () -> Unit,
    onStopListening: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    shouldDisableSend: Boolean = false,
    showImageIcon: Boolean = true,
    onImagePickerClick: (() -> Unit)? = null,
    selectedImageUri: String? = null,
    onRemoveImage: (() -> Unit)? = null,
    kannadaKeyboard: Boolean = false,
    autoFocus: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(White)
    ) {
        // Auto-suggestions
        val shouldShowAutosuggestions = !sttState.isListening &&
                chatState.showAutosuggestions &&
                chatState.inputText.isEmpty() &&
                !chatState.isLoading

        if (shouldShowAutosuggestions) {
            AutoSuggestionChips(
                suggestions = chatState.autosuggestions,
                visible = true,
                onSuggestionClick = onSuggestionClick
            )
        }
        //input field
        if (!sttState.isListening) {
            // Selected image preview - lets the user see and remove the
            // image they attached before sending it
            if (selectedImageUri != null) {
                SelectedImagePreview(
                    imageUri = selectedImageUri,
                    onRemoveClick = { onRemoveImage?.invoke() }
                )
            }
            InputField(
                textValue = chatState.inputText,
                onTextChange = onTextChange,
                onSpeakClick = onSpeakClick,
                onSendClick = onSendClick,
                shouldDisableSend = shouldDisableSend,
                showImageIcon = showImageIcon,
                onImagePickerClick = onImagePickerClick ?: {},
                kannadaKeyboard = kannadaKeyboard,
                autoFocus = autoFocus
            )
        } else {
            ListeningOverlay(
                text = sttState.resultText,
                amplitude = sttState.audioAmplitude,
                statusMessage = sttState.statusMessage,
                onStopClick = onStopListening
            )
        }
    }
}

/**
 * Shows a thumbnail of the image the user has attached, with a remove (X)
 * button so they can clear it before sending the message.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun SelectedImagePreview(
    imageUri: String,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = LocalDimensions.current
    Box(
        modifier = modifier
            .padding(start = dimens.inputPadding, top = dimens.spaceSmall)
            .size(72.dp)
    ) {
        GlideImage(
            model = imageUri,
            contentDescription = stringResource(R.string.attach_image),
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(dimens.cornerRadiusSmall))
                .border(dimens.inputBorderWidth, AccentBlue, RoundedCornerShape(dimens.cornerRadiusSmall))
        )

        // Remove button
        IconButton(
            onClick = onRemoveClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(20.dp)
                .background(White, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = stringResource(R.string.remove_image),
                tint = IconPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Input field component with image, text input, mic, and send buttons
 */
@Composable
private fun InputField(
    textValue: String,
    onTextChange: (String) -> Unit,
    onSpeakClick: () -> Unit,
    onSendClick: () -> Unit,
    shouldDisableSend: Boolean = false,
    showImageIcon: Boolean = true,
    onImagePickerClick: () -> Unit = {},
    kannadaKeyboard: Boolean = false,
    autoFocus: Boolean = false,
    modifier: Modifier = Modifier
) {

    val dimens = LocalDimensions.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // When the screen switches into text mode (e.g. tapping "Type" in the voice dock),
    // grab focus and raise the keyboard immediately.
    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Buffer input locally so recompositions from other StateFlows
    // (chatViewModel TTS ticks etc.) don't interrupt the IME mid-keystroke.
    // lastExternalValue tracks the last textValue we pushed INTO localText,
    // so we only overwrite localText when the *external* source changes it
    // (e.g. send-clear, STT inject) — never when ViewModel just echoes back
    // what we already typed. This check is synchronous (no LaunchedEffect delay).
    var localText by remember { mutableStateOf(TextFieldValue(textValue)) }
    var lastExternalValue by remember { mutableStateOf(textValue) }

    if (textValue != lastExternalValue) {
        // External change (send cleared it, STT filled it) — sync localText now,
        // in the same composition pass, with no frame delay
        lastExternalValue = textValue
        localText = TextFieldValue(textValue, selection = TextRange(textValue.length))
    }

    val hasText = localText.text.isNotBlank()

    // Determine if send should be enabled
    val canSend = hasText && !shouldDisableSend

    // Row layout with image icon, text field, and action buttons
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(dimens.inputPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Text Input Field
        TextField(
            value = localText,
            shape = RoundedCornerShape(dimens.inputRadius),
            onValueChange = { newValue ->
                localText = newValue          // update local instantly, no recomposition lag
                onTextChange(newValue.text)   // propagate to ViewModel
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .border(
                    shape = RoundedCornerShape(dimens.inputRadius),
                    width = dimens.inputBorderWidth,
                    color = AccentBlue
                ),
            placeholder = {
                Text(
                    text = stringResource(R.string.type_or_speak),
                    color = TextPrimary
                )
            },
            leadingIcon = {
                // Leading Icon - Attach Image
                if (showImageIcon) {
                    IconButton(
                        onClick = onImagePickerClick,
                        modifier = Modifier.size(dimens.iconMedium),
                        enabled = !shouldDisableSend
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = stringResource(R.string.attach_image),
                            tint = if (shouldDisableSend) IconPrimary.copy(alpha = 0.5f) else IconPrimary,
                            modifier = Modifier.size(dimens.iconMedium)
                        )
                    }
                }
            },
            trailingIcon = {
                if (hasText) {
                    // Send Icon
                    IconButton(
                        onClick = {
                            if (canSend) {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                onSendClick()
                            }
                        },
                        enabled = canSend,
                        modifier = Modifier.size(dimens.iconMedium)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.send_message),
                            tint = if (canSend) HeaderGradientStart else IconSecondary.copy(alpha = 0.5f),
                        )
                    }
                } else {
                    // Mic Icon - disable mic during AI response
                    IconButton(
                        onClick = onSpeakClick,
                        enabled = !shouldDisableSend,
                        modifier = Modifier.size(dimens.iconMedium)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = stringResource(R.string.start_listening),
                            tint = if (shouldDisableSend) IconPrimary.copy(alpha = 0.5f) else IconPrimary,
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                // Hint the IME (e.g. Gboard) to switch to Kannada script when the app
                // language is Kannada. Best-effort: honored if the user has a Kannada
                // keyboard/layout available; otherwise falls back to the default.
                hintLocales = if (kannadaKeyboard) LocaleList("kn-IN") else LocaleList.Empty,
                imeAction = if (canSend) {
                    ImeAction.Send
                } else {
                    ImeAction.Default
                }
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (canSend) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onSendClick()
                    }
                }
            ),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = White,
                unfocusedContainerColor = White,
                disabledContainerColor = White.copy(alpha = 0.9f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                disabledTextColor = TextPrimary.copy(alpha = 0.5f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )
    }
}


@Preview
@Composable
fun InputSectionPreview() {
    InputField(
        textValue = "",
        onTextChange = {},
        onSpeakClick = {},
        onSendClick = {},
        shouldDisableSend = false,
        showImageIcon = true,
        onImagePickerClick = {}
    )
}