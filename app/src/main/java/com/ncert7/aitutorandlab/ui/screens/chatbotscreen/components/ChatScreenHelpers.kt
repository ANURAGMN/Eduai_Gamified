package com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components

import android.Manifest
import android.content.pm.PackageManager
import android.webkit.WebView
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.anurag.eduai.uikit.avatar.rememberSavedTutorConfig
import com.anurag.eduai.uikit.avatar.EduTutorAvatarWithLipSync
import com.anurag.eduai.uikit.avatar.core.AvatarState
import com.ncert7.aitutorandlab.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.ncert7.aitutorandlab.config.LocalNativeTutorAvatarEnabled
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.ui.components.LoadingInsightPanel
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.domain.chatbot.usecase.ChatIntent
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatBotSettingsState
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatMessageModel
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.theme.TextSecondary
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatUiState
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ResourceCardUiState
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.viewmodel.ChatViewModel
import com.ncert7.aitutorandlab.ui.viewModel.SpeechToText
import com.ncert7.aitutorandlab.ui.viewModel.TextToSpeech
import kotlinx.coroutines.delay

private fun resolveAgentAvatarMood(
    isSpeaking: Boolean,
    isListening: Boolean,
    isThinking: Boolean,
): AvatarState =
    when {
        isSpeaking -> AvatarState.Speaking
        isListening -> AvatarState.Listening
        isThinking -> AvatarState.Thinking
        else -> AvatarState.Idle
    }

/** Full tutor on load; cropped face once the session is live. */
private enum class AgentAvatarFraming {
    FullBody,
    FaceCloseUp,
}

private data class AgentFaceCrop(val zoom: Float, val offsetYFraction: Float)

private fun faceCropFor(avatarSize: Dp): AgentFaceCrop =
    if (avatarSize <= 120.dp) {
        AgentFaceCrop(zoom = 2.12f, offsetYFraction = 0.20f)
    } else {
        AgentFaceCrop(zoom = 1.72f, offsetYFraction = 0.14f)
    }

@Composable
fun AgentTutorAvatarBubble(
    avatarSize: Dp,
    ttsState: TextToSpeech.TTSState,
    wordBoundaryIndex: Int,
    isListening: Boolean,
    isThinking: Boolean,
    fullBody: Boolean,
    modifier: Modifier = Modifier,
    elevation: Dp = LocalDimensions.current.cardElevation,
) {
    NativeTutorAvatarCard(
        avatarSize = avatarSize,
        ttsState = ttsState,
        wordBoundaryIndex = wordBoundaryIndex,
        isListening = isListening,
        isThinking = isThinking,
        framing = if (fullBody) AgentAvatarFraming.FullBody else AgentAvatarFraming.FaceCloseUp,
        modifier = modifier,
        elevation = elevation,
    )
}

@Composable
private fun NativeTutorAvatarCard(
    avatarSize: Dp,
    ttsState: TextToSpeech.TTSState,
    wordBoundaryIndex: Int,
    isListening: Boolean,
    isThinking: Boolean,
    framing: AgentAvatarFraming,
    modifier: Modifier = Modifier,
    elevation: Dp = LocalDimensions.current.cardElevation,
) {
    val mood =
        resolveAgentAvatarMood(
            isSpeaking = ttsState.isSpeaking,
            isListening = isListening,
            isThinking = isThinking,
        )
    val crop =
        when (framing) {
            AgentAvatarFraming.FullBody -> AgentFaceCrop(zoom = 1f, offsetYFraction = 0f)
            AgentAvatarFraming.FaceCloseUp -> faceCropFor(avatarSize)
        }
    val tutorLook = rememberSavedTutorConfig()
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        shape = CircleShape,
        modifier = modifier
            .size(avatarSize)
            .clip(CircleShape),
    ) {
        EduTutorAvatarWithLipSync(
            isSpeaking = ttsState.isSpeaking,
            spokenText = ttsState.speakingText,
            estimatedDurationMs = ttsState.speakingDurationMs,
            wordBoundaryIndex = wordBoundaryIndex,
            mood = mood,
            character = tutorLook.character,
            outfitVariant = tutorLook.outfit,
            hairStyle = tutorLook.hair,
            hairColor = tutorLook.hairColor,
            glassesStyle = tutorLook.glasses,
            glassesColor = tutorLook.frameColor,
            neckStyle = tutorLook.neck,
            underEyeLine = tutorLook.eyeLine,
            cheekShading = tutorLook.cheeks,
            faceZoom = crop.zoom,
            faceOffsetYFraction = crop.offsetYFraction,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * Initial avatar view shown before conversation starts
 */
@Composable
fun InitialAvatarView(
    avatarSize: Dp,
    ttsController: TextToSpeech,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    languageCode: String = "en",
    useNativeAvatar: Boolean = LocalNativeTutorAvatarEnabled.current,
    ttsState: TextToSpeech.TTSState = TextToSpeech.TTSState(),
    wordBoundaryIndex: Int = -1,
    isListening: Boolean = false,
) {
    val dimens = LocalDimensions.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (useNativeAvatar) {
                AgentTutorAvatarBubble(
                    avatarSize = avatarSize,
                    ttsState = ttsState,
                    wordBoundaryIndex = wordBoundaryIndex,
                    isListening = isListening,
                    isThinking = isLoading,
                    fullBody = true,
                    elevation = 12.dp,
                )
            } else {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape),
                ) {
                    AndroidView(
                        factory = {
                            WebView(it).apply {
                                setBackgroundColor(0)
                                ttsController.setupWebView(this)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Show "Teacher is thinking..." when loading
            if (isLoading) {
                Spacer(Modifier.height(dimens.spaceMedium))
                LoadingInsightPanel(
                    statusText = stringResource(R.string.teacher_thinking),
                    languageCode = languageCode,
                    centered = true,
                    rotateThinking = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimens.cardPadding),
                )
            }
        }
    }
}

/**
 * Conversation view with avatar and content area
 */
@Composable
fun ConversationView(
    avatarSize: Dp,
    chatState: ChatUiState,
    lastAIMessage: ChatMessageModel?,
    ttsController: TextToSpeech,
    modifier: Modifier = Modifier,
    useNativeAvatar: Boolean = LocalNativeTutorAvatarEnabled.current,
    ttsState: TextToSpeech.TTSState = TextToSpeech.TTSState(),
    wordBoundaryIndex: Int = -1,
    isListening: Boolean = false,
    messageFontSize: TextUnit? = null,
    messageLineHeight: TextUnit? = null,
) {
    val dimens = LocalDimensions.current
    val isWaitingForAgent = chatState.isLoading || chatState.isTyping || chatState.waitingForTTSToComplete
    val isThinking = isWaitingForAgent && !ttsState.isSpeaking
    Column(modifier = modifier.fillMaxSize()) {
        // Avatar at top -
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (useNativeAvatar) {
                AgentTutorAvatarBubble(
                    avatarSize = avatarSize,
                    ttsState = ttsState,
                    wordBoundaryIndex = wordBoundaryIndex,
                    isListening = isListening,
                    isThinking = isThinking,
                    fullBody = false,
                )
            } else {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = dimens.cardElevation),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape),
                ) {
                    AndroidView(
                        factory = {
                            WebView(it).apply {
                                setBackgroundColor(0)
                                ttsController.setupWebView(this)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        Spacer(Modifier.height(dimens.spaceMedium))

        // Content area - This scrolls
        ChatContentArea(
            isLoading = chatState.isLoading,
            loadingResourceMessage = chatState.loadingResourceMessage,
            lastAIMessage = lastAIMessage,
            isTyping = chatState.isTyping,
            typingText = chatState.typingText,
            ttsController = ttsController,
            isResourceCardShowing = chatState.resourceCardState !is ResourceCardUiState.Hidden,
            languageCode = chatState.currentLanguage,
            messageFontSize = messageFontSize,
            messageLineHeight = messageLineHeight,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
    }
}

/**
 * All LaunchedEffects consolidated in one place
 */
@Composable
fun ChatEffects(
    chatViewModel: ChatViewModel,
    ttsController: TextToSpeech,
    sttController: SpeechToText,
    chatState: ChatUiState,
    ttsState: TextToSpeech.TTSState,
    sttState: SpeechToText.STTState,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    onPermissionGranted: (Boolean) -> Unit,
    onSpeechTextProcessed: (String) -> Unit,
    lastProcessedSpeechText: String,
    conceptId: String? = null,
    settingsState: ChatBotSettingsState,
    onSettingsStateUpdate: (ChatBotSettingsState) -> Unit,
    avatarBoyDisplayName: String,
    avatarGirlDisplayName: String,
    avatarDisableDisplayName: String,
    // When a screen (e.g. Math) drives its own ViewModel, it supplies this to receive
    // the finished voice transcript and decide how to send/stage it. When null, the
    // default chat behaviour (auto-send via chatViewModel) is used.
    onSpeechCaptured: ((String) -> Unit)? = null
){
    val context = LocalContext.current

    // Initialize controllers and check permissions
    LaunchedEffect(Unit) {
        val sharedPrefs = SharedPreferenceUtils(context)
        val userId = sharedPrefs.getUserId().orEmpty()
        chatViewModel.onIntent(ChatIntent.Initialize(userId))
        sttController.initialize(context)
        ttsController.initialize(context)

        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        onPermissionGranted(hasPermission)

        // Do not auto-request mic permission on chat open — trial/study works via typing.
        // Permission is requested when the user taps the mic button.

        // Auto-start session after initialization completes
        conceptId?.let {
            DebugLogger.debugLog("ChatEffects", "Auto-starting with conceptId: $it")
            chatViewModel.onIntent(ChatIntent.AutoStartWithConcept(it))
        }
    }

    // Initialize avatar display name once using pre-loaded string resources
    LaunchedEffect(Unit) {
        val updatedState = chatViewModel.initializeAvatarDisplayName(
            avatarCode = settingsState.selectedAvatar,
            boyDisplayName = avatarBoyDisplayName,
            girlDisplayName = avatarGirlDisplayName,
            disableDisplayName = avatarDisableDisplayName,
            currentState = settingsState
        )
        onSettingsStateUpdate(updatedState)
    }

    // Keep the TTS voice + language locked to the app-selected language. Without this
    // the engine stays on its en-IN default and a Kannada session is read aloud in
    // English. Re-runs once device voices finish loading and whenever the language or
    // avatar changes.
    LaunchedEffect(chatState.isKannada, ttsState.voicesFullyLoaded, settingsState.selectedAvatar) {
        val langCode = if (chatState.isKannada) "kn-IN" else "en-IN"
        ttsController.setAppLanguage(settingsState.selectedAvatar, langCode)
    }

    // TTS trigger - monitor chatState.shouldStartTTS changes
    LaunchedEffect(chatState.shouldStartTTS, ttsState.isInitialized) {
        if (chatState.shouldStartTTS && ttsState.isInitialized) {
            val textToSpeak = chatState.fullTextForTTS
            if (textToSpeak.isNotEmpty()) {
                if (ttsState.isSpeaking) {
                    ttsController.stop()
                    delay(50)
                }
                ttsController.speak(textToSpeak)
            }
        }
    }

    // Stop TTS on concept change
    LaunchedEffect(chatState.selectedConcept) {
        if (ttsState.isSpeaking) {
            ttsController.stop()
        }
    }

    // Stop TTS when resource card is shown
    LaunchedEffect(chatState.resourceCardState) {
        if (chatState.resourceCardState !is ResourceCardUiState.Hidden && ttsState.isSpeaking) {
            ttsController.stop()
        }
    }


    // Handle speech recognition
    LaunchedEffect(sttState.isListening) {
        if (sttState.isListening) {
            chatViewModel.onIntent(ChatIntent.MarkUserActive)
            chatViewModel.onIntent(ChatIntent.HideAutosuggestions)
            // Stop TTS when user starts listening
            if (ttsState.isSpeaking) {
                ttsController.stop()
            }
        } else {
            val spoken = sttState.resultText.trim()
            if (spoken.isNotEmpty() && spoken != lastProcessedSpeechText) {
                onSpeechTextProcessed(spoken)
                if (onSpeechCaptured != null) {
                    // Screen owns the send/stage decision (e.g. Math via its own VM).
                    onSpeechCaptured(spoken)
                } else {
                    // Default: auto-send the captured speech so voice becomes a one-tap
                    // flow (speak → it sends) instead of speak → stop → send. Only send
                    // when the agent is free; otherwise just stage the text in the input.
                    val agentBusy = chatState.isLoading || chatState.isTyping ||
                            ttsState.isSpeaking ||
                            chatState.resourceCardState !is ResourceCardUiState.Hidden
                    if (agentBusy) {
                        chatViewModel.onIntent(ChatIntent.UpdateInputText(spoken))
                    } else {
                        chatViewModel.onIntent(ChatIntent.HideAutosuggestions)
                        chatViewModel.onIntent(ChatIntent.SendMessage(spoken))
                        chatViewModel.onIntent(ChatIntent.UpdateInputText(""))
                    }
                }
            }
            chatViewModel.onIntent(ChatIntent.MarkUserInactive)
        }
    }

    // Start idle timer AFTER everything completes (typing, TTS, and resource card if exists)
    LaunchedEffect(ttsState.isSpeaking, chatState.isLoading, chatState.isTyping, chatState.waitingForTTSToComplete, chatState.isUserActive, chatState.resourceCardState) {
        val isResourceCardShowing = chatState.resourceCardState !is ResourceCardUiState.Hidden

        // Only trigger if all agent message components are complete AND user is idle
        if (!ttsState.isSpeaking &&
            !chatState.isLoading &&
            !chatState.isTyping &&
            !chatState.waitingForTTSToComplete &&
            !chatState.isUserActive &&
            !isResourceCardShowing &&  // Wait for resource card to be dismissed
            chatState.messages.isNotEmpty()) {

            // All agent message components complete, check if we should start idle timer
            DebugLogger.debugLog("ChatScreenHelpers", """
                ═══════════════════════════════════════════════════════
                IDLE TIMER TRIGGER CONDITIONS MET
                ═══════════════════════════════════════════════════════
                !ttsState.isSpeaking: ${!ttsState.isSpeaking}
                !isLoading: ${!chatState.isLoading}
                !isTyping: ${!chatState.isTyping}
                !waitingForTTSToComplete: ${!chatState.waitingForTTSToComplete}
                !isUserActive: ${!chatState.isUserActive}
                !isResourceCardShowing: ${!isResourceCardShowing}
                messages.isNotEmpty(): ${chatState.messages.isNotEmpty()}
                inputText.isEmpty(): ${chatState.inputText.isEmpty()}
                autosuggestions.size: ${chatState.autosuggestions.size}
                ═══════════════════════════════════════════════════════
            """.trimIndent())

            // Start the idle timer which will show autosuggestions after 5s delay
            if (chatState.inputText.isEmpty() && chatState.autosuggestions.isNotEmpty()) {
                DebugLogger.debugLog("ChatScreenHelpers", " Starting idle timer (5s countdown)")
                chatViewModel.onIntent(ChatIntent.StartIdleTimer)
            } else {
                DebugLogger.debugLog("ChatScreenHelpers", " NOT starting timer - inputText: '${chatState.inputText}', suggestions: ${chatState.autosuggestions.size}")
            }
        }
    }

    // Cleanup STT
    DisposableEffect(Unit) {
        sttController.initialize(context)
        onDispose {
            sttController.destroy()
        }
    }
}

/**
 * Hands-free voice loop: when an agent turn finishes (TTS done, not loading/typing,
 * and there is an agent message on screen), automatically open the mic so the user can
 * reply without tapping. Edge-triggered on the false→true transition of [turnComplete],
 * so it fires exactly once per agent response and does NOT re-open the mic while the
 * user sits idle (if they stay silent, STT times out and the loop naturally pauses).
 *
 * Combined with the silence-auto-stop + auto-send, this produces a full back-and-forth
 * conversation. Gated by [enabled] (the hands-free toggle) and [canListen]
 * (permission granted + recognizer initialized).
 */
@Composable
fun AutoListenAfterAgentTurn(
    enabled: Boolean,
    turnComplete: Boolean,
    isListening: Boolean,
    canListen: Boolean,
    onStartListening: () -> Unit,
) {
    var prevTurnComplete by remember { mutableStateOf(false) }
    LaunchedEffect(turnComplete, enabled) {
        val justCompleted = turnComplete && !prevTurnComplete
        prevTurnComplete = turnComplete
        if (justCompleted && enabled && !isListening && canListen) {
            // Small settle delay so the TTS tail / state fully clears before the mic opens.
            delay(450)
            onStartListening()
        }
    }
}
