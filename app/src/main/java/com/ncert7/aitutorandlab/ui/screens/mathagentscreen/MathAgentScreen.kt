package com.ncert7.aitutorandlab.ui.screens.mathagentscreen

import android.Manifest.permission.RECORD_AUDIO
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ncert7.aitutorandlab.domain.examplan.TrialSessionStore
import com.ncert7.aitutorandlab.ui.components.AgentSessionTimeGate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.config.GamificationFeatureFlags
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.service.analytics.ScreenName
import kotlinx.coroutines.delay
import com.ncert7.aitutorandlab.service.analytics.TrackScreenEvent
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.AppDialog
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.AutoListenAfterAgentTurn
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.ChatEffects
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.VoiceInputBar
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.ChatHeaderIcons
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.ChatMessageFontSize
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.ConversationView
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.InitialAvatarView
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.InputSection
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.ResourcesCard
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatMessageModel
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ResourceCardUiState
import com.ncert7.aitutorandlab.ui.theme.White
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatBotSettingsState
import com.ncert7.aitutorandlab.ui.viewModel.SpeechToText
import com.ncert7.aitutorandlab.ui.viewModel.TextToSpeech
import com.ncert7.aitutorandlab.ui.screens.mathagentscreen.dataclass.isConversationStarted
import com.ncert7.aitutorandlab.ui.screens.mathagentscreen.dataclass.lastAiMessage
import com.ncert7.aitutorandlab.domain.chatbot.usecase.ChatIntent
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.viewmodel.ChatViewModel
import com.ncert7.aitutorandlab.ui.screens.mathagentscreen.components.MathBotSettings
import com.ncert7.aitutorandlab.ui.screens.mathagentscreen.viewmodel.MathViewModel
import androidx.compose.ui.unit.sp
import com.ncert7.aitutorandlab.domain.mathagent.usecase.MathIntent
import com.ncert7.aitutorandlab.ui.screens.mathagentscreen.dataclass.MathMessageModel

@Composable
fun MathAgentScreen(
    problemId: String? = null,
    chatViewModel: ChatViewModel = hiltViewModel(),
    ttsController: TextToSpeech = hiltViewModel(),
    sttController: SpeechToText = hiltViewModel(),
    mathViewModel: MathViewModel = hiltViewModel()
) {
    // Debug logging
    DebugLogger.debugLog("MathAgentScreen", "MathAgentScreen composable - problemId: $problemId")

    // Track screen analytics
    TrackScreenEvent(ScreenName.MATH_AGENT)

    // State collectors - using consolidated UI state
    val chatState by chatViewModel.uiState.collectAsState()
    val ttsState by ttsController.state.collectAsState()
    val avatarBoyDisplayName = stringResource(R.string.boy)
    val avatarGirlDisplayName = stringResource(R.string.girl)
    val avatarDisableDisplayName = stringResource(R.string.disable)
    val sttState by sttController.state.collectAsState()
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val wordBoundaryIndex by ttsController.currentWordIndex.collectAsState()
    val mathState by mathViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val useNativeAvatar = GamificationFeatureFlags.isNativeTutorAvatarEnabled(context)

    LaunchedEffect(useNativeAvatar) {
        ttsController.setNativeLipSyncEnabled(useNativeAvatar)
    }
    // Local UI state
    var permissionGranted by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }

    // Hands-free voice: persisted toggle (default on). Mic auto-opens after the tutor speaks.
    val sharedPrefs = remember { SharedPreferenceUtils(context) }

    // Settings state — message font loads from prefs; Math renders 0.5sp smaller than base.
    var settingsState by remember {
        mutableStateOf(ChatBotSettingsState(messageFontSp = sharedPrefs.getChatMessageFontSp()))
    }
    var handsFreeMode by remember { mutableStateOf(sharedPrefs.getHandsFreeMode()) }
    val handsFreeLabel = if (mathState.isKannada) "ಧ್ವನಿ ಸಂಭಾಷಣೆ" else "Hands-free voice"

    // Input mode: voice dock vs. text keyboard (persisted default).
    var inputMode by remember { mutableStateOf(if (sharedPrefs.getVoiceFirst()) "voice" else "text") }
    var focusTextField by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val agentThinking = (mathState.isLoading || mathState.isTyping) && !ttsState.isSpeaking

    LaunchedEffect(mathState.errorMessage) {
        mathState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        sttController.handlePermissionResult(
            SpeechToText.RECORD_AUDIO_PERMISSION_REQUEST,
            if (isGranted) intArrayOf(PackageManager.PERMISSION_GRANTED)
            else intArrayOf(PackageManager.PERMISSION_DENIED)
        )
    }

    LaunchedEffect(Unit) {
        permissionGranted =
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
    }

    val beginListening: () -> Unit = {
        mathViewModel.onIntent(MathIntent.HideAutosuggestions)
        mathViewModel.onIntent(MathIntent.MarkUserActive)
        if (permissionGranted && sttState.isInitialized) {
            sttController.startListening(if (mathState.isKannada) "kn-IN" else "en-IN")
        } else if (!permissionGranted) {
            permissionLauncher.launch(RECORD_AUDIO)
        }
    }

    val imageTooLargeMessage = stringResource(R.string.image_too_large)
    val imageProcessingMessage = stringResource(R.string.image_processing_may_take_time)


    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fileSizeBytes = try {
                context.contentResolver.openFileDescriptor(it, "r")?.use { pfd ->
                    pfd.statSize
                } ?: 0L
            } catch (e: Exception) {
                DebugLogger.errorLog("MathAgentScreen", "Failed to read image size: ${e.message}")
                0L
            }

            val fiveMbInBytes = 5 * 1024 * 1024L

            if (fileSizeBytes > fiveMbInBytes) {
                android.widget.Toast.makeText(
                    context,
                    imageTooLargeMessage,
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@let
            }

            DebugLogger.debugLog("MathAgentScreen", "Image selected: $it")
            mathViewModel.onIntent(MathIntent.SelectImage(it.toString()))
        }
    }
    // Convert math messages to chat messages for display
    val mathMessagesAsChatMessages = remember(mathState.messages) {
        mathState.messages.map { mathMsg ->
            ChatMessageModel(
                sender = if (mathMsg.role.lowercase() == "assistant") "ai" else "user",
                content = mathMsg.content,
                timestamp = mathMsg.timestamp,
                isError = mathMsg.isError
            )
        }
    }

    // Create a temporary chat state with math messages for display
    // remove remember wrapper, keep as plain copy
    val displayChatState = chatState.copy(
        messages = mathMessagesAsChatMessages,
        inputText = mathState.inputText,
        isLoading = mathState.isLoading,
        isTyping = mathState.isTyping,
        typingText = mathState.typingText
    )

    // Auto-speak agent's first message on session start/resume + drive highlight,
    // mirroring ConceptScreen/ChatEffects' shouldStartTTS -> ttsController.speak() flow
    LaunchedEffect(mathState.shouldStartTTS, ttsState.isInitialized) {
        if (mathState.shouldStartTTS && ttsState.isInitialized) {
            val textToSpeak = mathState.fullTextForTTS
            if (textToSpeak.isNotEmpty()) {
                if (ttsState.isSpeaking) {
                    ttsController.stop()
                    delay(50)
                }
                DebugLogger.debugLog("MathAgentScreen", "Auto-starting TTS on session start: ${textToSpeak.take(50)}...")
                ttsController.speak(textToSpeak)
            }
            // Reset the flag so this doesn't re-fire on recomposition
            mathViewModel.onIntent(MathIntent.ConsumeTTSTrigger)
        }
    }

    // TTS trigger - start speaking when typing animation begins
    LaunchedEffect(mathState.isTyping, ttsState.isInitialized) {
        if (mathState.isTyping && ttsState.isInitialized && !ttsState.isSpeaking) {
            val textToSpeak = mathState.typingText
            if (textToSpeak.isNotEmpty()) {
                if (ttsState.isSpeaking) {
                    ttsController.stop()
                    delay(50)
                }
                DebugLogger.debugLog("MathAgentScreen", "Starting TTS for typing text: ${textToSpeak.take(50)}...")
                ttsController.speak(textToSpeak)
            }
        }
    }

    // Stop TTS when user starts listening
    LaunchedEffect(sttState.isListening) {
        if (sttState.isListening && ttsState.isSpeaking) {
            ttsController.stop()
        }
    }

    // Use extension properties directly (no alias needed)
    val lastAIMessage: MathMessageModel? = mathState.lastAiMessage
    val isConversationStarted: Boolean = mathState.isConversationStarted

    val voiceOptions = remember(ttsState.availableVoices, mathState.currentLanguage, settingsState.selectedAvatar) {
        ttsController.getFilteredVoiceOptions(mathState.currentLanguage, settingsState.selectedAvatar)
    }

    val displayedVoiceName = remember(ttsState.selectedVoice, mathState.currentLanguage, settingsState.selectedAvatar) {
        ttsState.selectedVoice?.let { ttsController.formatVoiceName(it) }
            ?: ttsController.getDefaultVoiceName(mathState.currentLanguage, settingsState.selectedAvatar)
    }


    // Animation values
    val avatarSize by animateDpAsState(
        targetValue = if (isConversationStarted) 100.dp else 180.dp,
        label = "avatarSize"
    )

    // Auto-start once the catalog fetch finishes (success or failure).
    // Do not gate on problems.isNotEmpty() — a failed/empty catalog used to leave the screen stuck on the avatar.
    var didAutoStart by remember(problemId) { mutableStateOf(false) }
    LaunchedEffect(problemId, mathState.isLoading, mathState.sessionStarted) {
        DebugLogger.debugLog(
            "MathAgentScreen",
            "LaunchedEffect triggered - problemId: $problemId, problemsCount: ${mathState.problems.size}, isLoading: ${mathState.isLoading}, sessionStarted: ${mathState.sessionStarted}"
        )

        if (didAutoStart || mathState.sessionStarted) return@LaunchedEffect
        if (problemId.isNullOrEmpty() || problemId == "null") {
            DebugLogger.debugLog("MathAgentScreen", "No problemId provided — waiting for concept click")
            return@LaunchedEffect
        }
        if (mathState.isLoading) return@LaunchedEffect

        didAutoStart = true
        DebugLogger.debugLog("MathAgentScreen", "Auto-starting with provided problemId: $problemId")
        mathViewModel.onIntent(MathIntent.AutoStartWithProblem(problemId))
    }

    // Effects
    ChatEffects(
        chatViewModel = chatViewModel,
        ttsController = ttsController,
        sttController = sttController,
        chatState = chatState,
        ttsState = ttsState,
        sttState = sttState,
        permissionLauncher = permissionLauncher,
        onPermissionGranted = { permissionGranted = it },
        onSpeechTextProcessed = { },
        lastProcessedSpeechText = "",
        // Math VM owns session start — do not auto-start study chat with problemId as conceptId.
        conceptId = null,
        settingsState = settingsState,
        onSettingsStateUpdate = { settingsState = it },
        avatarBoyDisplayName = avatarBoyDisplayName,
        avatarGirlDisplayName = avatarGirlDisplayName,
        avatarDisableDisplayName = avatarDisableDisplayName,
        // Route captured voice to the Math VM (not chatViewModel) and auto-send so
        // voice is a one-tap flow. Stage instead if busy or an image is attached.
        onSpeechCaptured = { spoken ->
            val busy = mathState.isLoading || mathState.isTyping ||
                    ttsState.isSpeaking || !mathState.sessionStarted
            if (busy || mathState.selectedImageUri != null) {
                mathViewModel.onIntent(MathIntent.UpdateInputText(spoken))
            } else {
                mathViewModel.onIntent(MathIntent.HideAutosuggestions)
                mathViewModel.onIntent(MathIntent.SendMessage(spoken))
                mathViewModel.onIntent(MathIntent.UpdateInputText(""))
            }
        }
    )

    // Hands-free loop: auto-open the mic once the tutor's turn completes (voice mode only).
    AutoListenAfterAgentTurn(
        enabled = inputMode == "voice" && handsFreeMode && !showSettingsMenu,
        turnComplete = !ttsState.isSpeaking &&
                !mathState.isLoading &&
                !mathState.isTyping &&
                mathState.sessionStarted &&
                lastAIMessage != null &&
                chatState.resourceCardState is ResourceCardUiState.Hidden,
        isListening = sttState.isListening,
        canListen = permissionGranted && sttState.isInitialized,
        onStartListening = {
            sttController.startListening(if (mathState.isKannada) "kn-IN" else "en-IN")
        }
    )

    // Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = White,
            contentColor = White,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (inputMode == "voice") {
                    VoiceInputBar(
                        isKannada = mathState.isKannada,
                        isListening = sttState.isListening,
                        isSpeaking = ttsState.isSpeaking,
                        isThinking = agentThinking,
                        transcript = sttState.resultText,
                        statusMessage = sttState.statusMessage,
                        amplitude = sttState.audioAmplitude,
                        onMicTap = { beginListening() },
                        onStopListening = { sttController.stopListening() },
                        onSwitchToType = {
                            sttController.stopListening()
                            inputMode = "text"
                            focusTextField = true
                        },
                        modifier = Modifier.imePadding()
                    )
                } else {
                InputSection(
                    chatState = displayChatState,
                    sttState = sttState,
                    onTextChange = { mathViewModel.onIntent(MathIntent.UpdateInputText(it)) },
                    onSendClick = {
                        if (mathState.inputText.isNotBlank() || mathState.selectedImageUri != null) {
                            // Send message with or without image
                            if (mathState.selectedImageUri != null) {
                                android.widget.Toast.makeText(
                                    context,
                                    imageProcessingMessage,
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                mathViewModel.onIntent(
                                    MathIntent.SendMessageWithImage(mathState.inputText, mathState.selectedImageUri!!)
                                )
                            } else {
                                mathViewModel.onIntent(MathIntent.SendMessage(mathState.inputText))
                            }
                            mathViewModel.onIntent(MathIntent.UpdateInputText(""))
                        }
                    },
                    onSpeakClick = {
                        // Text-mode mic → switch to the voice dock and start listening.
                        focusTextField = false
                        inputMode = "voice"
                        beginListening()
                    },
                    onStopListening = { sttController.stopListening() },
                    onSuggestionClick = { },
                    shouldDisableSend = mathState.isLoading || mathState.isTyping || !mathState.sessionStarted,
                    showImageIcon = true,
                    onImagePickerClick = {
                        imagePickerLauncher.launch("image/*")
                    },
                    selectedImageUri = mathState.selectedImageUri,
                    onRemoveImage = { mathViewModel.onIntent(MathIntent.ClearSelectedImage) },
                    kannadaKeyboard = mathState.isKannada,
                    autoFocus = focusTextField,
                    modifier = Modifier.imePadding()
                )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Header icons (settings, tts icon, kannada toggle)
                ChatHeaderIcons(
                    isSpeaking = ttsState.isSpeaking,
                    showSettingsMenu = showSettingsMenu,
                    onVolumeClick = {
                        handleVolumeClick(
                            ttsState = ttsState,
                            lastAIMessage = lastAIMessage,
                            ttsController = ttsController
                        )
                    },
                    onSettingsClick = { showSettingsMenu = true },
                    settingsContent = { }
                )

                Box(modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Avatar or Conversation View
                        if (!isConversationStarted) {
                            InitialAvatarView(
                                avatarSize = avatarSize,
                                ttsController = ttsController,
                                isLoading = mathState.isLoading,
                                languageCode = mathState.currentLanguage,
                                useNativeAvatar = useNativeAvatar,
                                ttsState = ttsState,
                                wordBoundaryIndex = wordBoundaryIndex,
                                isListening = sttState.isListening,
                            )
                        } else {
                            ConversationView(
                                avatarSize = avatarSize,
                                chatState = displayChatState,
                                lastAIMessage = lastAIMessage?.let { mathMsg ->
                                    ChatMessageModel(
                                        sender = "ai",
                                        content = mathMsg.content,
                                        timestamp = mathMsg.timestamp,
                                        isError = mathMsg.isError
                                    )
                                },
                                ttsController = ttsController,
                                useNativeAvatar = useNativeAvatar,
                                ttsState = ttsState,
                                wordBoundaryIndex = wordBoundaryIndex,
                                isListening = sttState.isListening,
                                messageFontSize = ChatMessageFontSize
                                    .resolveFontSp(settingsState.messageFontSp, mathAgent = true).sp,
                                messageLineHeight = ChatMessageFontSize
                                    .lineHeightSp(
                                        ChatMessageFontSize.resolveFontSp(
                                            settingsState.messageFontSp,
                                            mathAgent = true,
                                        ),
                                    ).sp,
                            )
                        }
                    }

                    // Resource card overlay (if needed for math)
                    if (chatState.resourceCardState !is ResourceCardUiState.Hidden) {
                        ResourcesCard(
                            state = chatState.resourceCardState,
                            onDismiss = { chatViewModel.onIntent(ChatIntent.DismissResource) }
                        )
                    }

                }
            }
        }

        // Settings Dialog — using correct MathBotSettings signature
        if (showSettingsMenu) {
            MathBotSettings(
                expanded = true,
                onDismiss = { showSettingsMenu = false },
                state = settingsState.copy(
                    voiceOptions = voiceOptions,
                    displayedVoiceName = displayedVoiceName,
                    availableConcepts = mathState.problems.map { it.id },
                    displayConcepts = mathState.problems.map { it.id },
                    selectedConcept = mathState.problemId.ifEmpty { null},
                    isLoadingConcepts = mathState.problems.isEmpty() && mathState.isLoading
                ),
                onAvatarChange = { displayName ->
                    settingsState = mathViewModel.handleAvatarChange(
                        displayName = displayName,
                        boyDisplayName = avatarBoyDisplayName,
                        girlDisplayName = avatarGirlDisplayName,
                        ttsController = ttsController,
                        currentState = settingsState
                    )
                },
                onVoiceChange = { selectedDisplayName ->
                    ttsState.availableVoices
                        .find { ttsController.formatVoiceName(it) == selectedDisplayName }
                        ?.let { ttsController.setVoice(it) }
                },
                onProblemChange = { selectedProblemId ->
                    mathViewModel.onIntent(MathIntent.SelectProblem(selectedProblemId))
                    showSettingsMenu = false
                },
                onLevelChange = { /* Student level not used in MathViewModel yet */ },
                onSpeedChange = { label ->
                    val speed = when (label) {
                        "0.75x" -> 0.75f
                        "1.0x" -> 1.0f
                        "1.25x" -> 1.25f
                        "1.5x" -> 1.5f
                        else -> 1.0f
                    }
                    ttsController.setSpeechRate(speed)
                    settingsState = settingsState.copy(selectedSpeed = label)
                },
                useNativeTutorAvatar = useNativeAvatar,
                handsFreeMode = handsFreeMode,
                onHandsFreeChange = {
                    handsFreeMode = it
                    sharedPrefs.setHandsFreeMode(it)
                },
                handsFreeLabel = handsFreeLabel,
                showInputModeSetting = true,
                voiceFirst = inputMode == "voice",
                onInputModeChange = { voiceFirst ->
                    sharedPrefs.setVoiceFirst(voiceFirst)
                    if (!voiceFirst) sttController.stopListening()
                    focusTextField = false
                    inputMode = if (voiceFirst) "voice" else "text"
                },
                defaultInputLabel = if (mathState.isKannada) "ಡೀಫಾಲ್ಟ್ ಇನ್‌ಪುಟ್" else "Default input",
                voiceFirstLabel = if (mathState.isKannada) "ಧ್ವನಿ ಮೊದಲು" else "Voice first",
                textFirstLabel = if (mathState.isKannada) "ಪಠ್ಯ ಮೊದಲು" else "Text first",
                onFontSizeChange = { sp ->
                    sharedPrefs.setChatMessageFontSp(sp)
                    settingsState = settingsState.copy(messageFontSp = sp)
                },
            )
        }

        // Existing session dialog — using correct intent names
        if (mathState.showSessionDialog) {
            AppDialog(
                show = mathState.showSessionDialog,
                title = stringResource(R.string.existing_session_found),
                message = stringResource(R.string.resume_or_start_fresh),
                confirmText = stringResource(R.string.continue_session),
                dismissText = stringResource(R.string.start_new),
                onConfirm = {
                    mathState.pendingProblemForDialog?.let { pendingId ->
                        mathViewModel.onIntent(MathIntent.ContinueExistingSession(pendingId))
                    }
                },
                onDismiss = {
                    mathState.pendingProblemForDialog?.let { pendingId ->
                        mathViewModel.onIntent(MathIntent.StartFreshSession(pendingId))
                    }
                }
            )
        }

        AgentSessionTimeGate(
            languageCode = chatState.currentLanguage,
            inTrialMode = TrialSessionStore.activeTrialItemId != null,
            onProceed = {
                chatViewModel.recordTrialProceed()
                backDispatcher?.onBackPressed()
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private fun handleVolumeClick(
    ttsState: TextToSpeech.TTSState,
    lastAIMessage: MathMessageModel?,
    ttsController: TextToSpeech
) {
    if (ttsState.isSpeaking) {
        ttsController.stop()
    } else {
        lastAIMessage?.let {
            ttsController.speak(it.content)
        }
    }
}