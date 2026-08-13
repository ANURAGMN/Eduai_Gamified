package com.ncert7.aitutorandlab.ui.screens.chatbotscreen

import android.Manifest.permission.RECORD_AUDIO
import android.content.pm.PackageManager
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.ncert7.aitutorandlab.domain.examplan.TrialSessionStore
import com.ncert7.aitutorandlab.ui.components.AgentSessionTimeGate
import com.ncert7.aitutorandlab.ui.components.LoadStallProceedGate
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.service.analytics.ScreenName
import com.ncert7.aitutorandlab.service.analytics.TrackScreenEvent
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.AppDialog
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.ChatBotSettings
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.AutoListenAfterAgentTurn
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.ChatEffects
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.ChatHeaderIcons
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.AutoSuggestionChips
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.VoiceInputBar
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.ChatMessageFontSize
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.ConversationView
import androidx.compose.ui.unit.sp
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.InitialAvatarView
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.InputSection
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.ResourcesCard
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatMessageModel
import com.ncert7.aitutorandlab.ui.theme.White
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatUiState
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ResourceCardUiState
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.viewmodel.ChatViewModel
import com.ncert7.aitutorandlab.ui.viewModel.SpeechToText
import com.ncert7.aitutorandlab.ui.viewModel.TextToSpeech
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.isConversationStarted
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.lastAiMessage
import com.ncert7.aitutorandlab.domain.chatbot.usecase.ChatIntent
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatBotSettingsState
import com.ncert7.aitutorandlab.config.GamificationFeatureFlags
import androidx.compose.runtime.LaunchedEffect

@Composable
fun ChatbotScreen(
    conceptId: String? = null,
    chatViewModel: ChatViewModel = hiltViewModel(),
    ttsController: TextToSpeech = hiltViewModel(),
    sttController: SpeechToText = hiltViewModel()
) {
    // Debug logging
    com.ncert7.aitutorandlab.debug.DebugLogger.debugLog("ChatbotScreen", "ChatbotScreen composable - conceptId: $conceptId")

    // Track screen analytics
    TrackScreenEvent(ScreenName.CHATBOT)

    // State collectors - using consolidated UI state
    val chatState by chatViewModel.uiState.collectAsState()
    val ttsState by ttsController.state.collectAsState()
    val sttState by sttController.state.collectAsState()
    val wordBoundaryIndex by ttsController.currentWordIndex.collectAsState()
    val context = LocalContext.current
    val useNativeAvatar = GamificationFeatureFlags.isNativeTutorAvatarEnabled(context)
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    LaunchedEffect(useNativeAvatar) {
        ttsController.setNativeLipSyncEnabled(useNativeAvatar)
    }

    // Local UI state
    var permissionGranted by remember { mutableStateOf(false) }
    var showMicPermissionDialog by remember { mutableStateOf(false) }
    var pendingMicPermissionRequest by remember { mutableStateOf(false) }
    var lastProcessedSpeechText by remember { mutableStateOf("") }
    var showSessionResumeDialog by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var pendingConceptSelection by remember { mutableStateOf<String?>(null) }

    // Hands-free voice: persisted toggle (default on). When on, the mic auto-opens
    // after the tutor finishes speaking.
    val sharedPrefs = remember { SharedPreferenceUtils(context) }

    // Settings state — message font loads from prefs.
    var settingsState by remember {
        mutableStateOf(ChatBotSettingsState(messageFontSp = sharedPrefs.getChatMessageFontSp()))
    }
    var handsFreeMode by remember { mutableStateOf(sharedPrefs.getHandsFreeMode()) }
    val handsFreeLabel = if (chatState.isKannada) "ಧ್ವನಿ ಸಂಭಾಷಣೆ" else "Hands-free voice"

    // Input mode: voice dock vs. text keyboard. Opens in whichever the "default input"
    // setting says (persisted). The hands-free loop runs only in voice mode.
    var inputMode by remember { mutableStateOf(if (sharedPrefs.getVoiceFirst()) "voice" else "text") }
    var focusTextField by remember { mutableStateOf(false) }
    val agentThinking = (chatState.isLoading || chatState.isTyping || chatState.waitingForTTSToComplete) &&
            !ttsState.isSpeaking

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        pendingMicPermissionRequest = false
        sttController.handlePermissionResult(
            SpeechToText.RECORD_AUDIO_PERMISSION_REQUEST,
            if (isGranted) intArrayOf(PackageManager.PERMISSION_GRANTED)
            else intArrayOf(PackageManager.PERMISSION_DENIED)
        )
        if (!isGranted) {
            showMicPermissionDialog = true
        }
    }

    LaunchedEffect(Unit) {
        permissionGranted =
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
    }

    // Start voice capture (shared by the voice-dock orb and the text-mode mic button).
    val beginListening: () -> Unit = {
        chatViewModel.onIntent(ChatIntent.HideAutosuggestions)
        chatViewModel.onIntent(ChatIntent.MarkUserActive)
        if (permissionGranted && sttState.isInitialized) {
            val language = if (chatState.isKannada) "kn-IN" else "en-IN"
            sttController.startListening(language)
        } else if (!permissionGranted) {
            if (!pendingMicPermissionRequest) {
                pendingMicPermissionRequest = true
                permissionLauncher.launch(RECORD_AUDIO)
            }
        }
    }

    val lastAIMessage = chatState.lastAiMessage
    val isConversationStarted = chatState.isConversationStarted

    val voiceOptions = remember(ttsState.availableVoices, chatState.currentLanguage, settingsState.selectedAvatar) {
        ttsController.getFilteredVoiceOptions(chatState.currentLanguage, settingsState.selectedAvatar)
    }

    val displayedVoiceName = remember(ttsState.selectedVoice, chatState.currentLanguage, settingsState.selectedAvatar) {
        ttsState.selectedVoice?.let { ttsController.formatVoiceName(it) }
            ?: ttsController.getDefaultVoiceName(chatState.currentLanguage, settingsState.selectedAvatar)
    }

    val aiMessageOutput = remember(chatState.isTyping, chatState.typingText, lastAIMessage) {
        when {
            chatState.isTyping -> chatState.typingText
            else -> lastAIMessage?.content ?: ""
        }
    }

    val shouldDisableSend = remember(
        chatState.isTyping,
        chatState.isLoading,
        ttsState.isSpeaking,
        chatState.resourceCardState
    ) {
        chatState.isTyping ||
                chatState.isLoading ||
                ttsState.isSpeaking ||
                chatState.resourceCardState !is ResourceCardUiState.Hidden
    }

    // Animation values
    val avatarSize by animateDpAsState(
        targetValue = if (isConversationStarted) 100.dp else 180.dp,
        label = "avatarSize"
    )

    AppDialog(
        show = showMicPermissionDialog,
        title = "Microphone access needed",
        message = "Allow microphone access to speak your answers, or keep typing in the text box.",
        confirmText = "Try again",
        dismissText = "Not now",
        onConfirm = {
            showMicPermissionDialog = false
            permissionLauncher.launch(RECORD_AUDIO)
        },
        onDismiss = { showMicPermissionDialog = false },
    )

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
        onSpeechTextProcessed =  {lastProcessedSpeechText = it} ,
        lastProcessedSpeechText = lastProcessedSpeechText,
        conceptId = conceptId,
        settingsState = settingsState,
        onSettingsStateUpdate = { settingsState = it },
        avatarBoyDisplayName = stringResource(R.string.boy),
        avatarGirlDisplayName = stringResource(R.string.girl),
        avatarDisableDisplayName = stringResource(R.string.disable)
    )

    // Hands-free loop: auto-open the mic once the tutor's turn completes. Runs only in
    // voice mode so the mic never opens while the learner is typing.
    AutoListenAfterAgentTurn(
        enabled = inputMode == "voice" && handsFreeMode && !showSettingsMenu,
        turnComplete = !ttsState.isSpeaking &&
                !chatState.isLoading &&
                !chatState.isTyping &&
                !chatState.waitingForTTSToComplete &&
                lastAIMessage != null &&
                chatState.resourceCardState is ResourceCardUiState.Hidden,
        isListening = sttState.isListening,
        canListen = permissionGranted && sttState.isInitialized,
        onStartListening = {
            sttController.startListening(if (chatState.isKannada) "kn-IN" else "en-IN")
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
            bottomBar = {
                if (inputMode == "voice") {
                    Column(modifier = Modifier.imePadding()) {
                        if (!sttState.isListening &&
                            chatState.showAutosuggestions &&
                            chatState.autosuggestions.isNotEmpty() &&
                            !chatState.isLoading
                        ) {
                            AutoSuggestionChips(
                                suggestions = chatState.autosuggestions,
                                visible = true,
                                onSuggestionClick = { suggestion ->
                                    chatViewModel.onIntent(ChatIntent.TapAutosuggestion(suggestion))
                                    chatViewModel.onIntent(ChatIntent.HideAutosuggestions)
                                },
                            )
                        }
                        VoiceInputBar(
                            isKannada = chatState.isKannada,
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
                        )
                    }
                } else {
                    InputSection(
                        chatState = chatState,
                        sttState = sttState,
                        onTextChange = { chatViewModel.onIntent(ChatIntent.UpdateInputText(it)) },
                        onSendClick = {
                            if (chatState.inputText.isNotBlank()) {
                                chatViewModel.onIntent(ChatIntent.HideAutosuggestions)
                                chatViewModel.onIntent(ChatIntent.SendMessage(chatState.inputText))
                                chatViewModel.onIntent(ChatIntent.UpdateInputText(""))
                            }
                        },
                        onSpeakClick = {
                            // Text-mode mic → switch to the voice dock and start listening.
                            focusTextField = false
                            inputMode = "voice"
                            beginListening()
                        },
                        onStopListening = { sttController.stopListening() },
                        onSuggestionClick = { suggestion ->
                            chatViewModel.onIntent(ChatIntent.TapAutosuggestion(suggestion))
                            chatViewModel.onIntent(ChatIntent.HideAutosuggestions)
                        },
                        shouldDisableSend = shouldDisableSend,
                        showImageIcon = false,
                        kannadaKeyboard = chatState.isKannada,
                        autoFocus = focusTextField,
                        modifier = Modifier
                            .imePadding()
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
                            chatState = chatState,
                            ttsState = ttsState,
                            lastAIMessage = lastAIMessage,
                            chatViewModel = chatViewModel,
                            ttsController = ttsController
                        )
                    },
                    onSettingsClick = { showSettingsMenu = !showSettingsMenu },
                    settingsContent = {
                        val boyDisplayName = stringResource(R.string.boy)
                        val girlDisplayName = stringResource(R.string.girl)

                        ChatBotSettings(
                            expanded = true,
                            onDismiss = { showSettingsMenu = false },
                            state = settingsState.copy(
                                voiceOptions = voiceOptions,
                                displayedVoiceName = displayedVoiceName,
                                availableConcepts = chatState.availableConcepts,
                                displayConcepts = chatState.displayConcepts,
                                selectedConcept = chatState.selectedConcept,
                                isLoadingConcepts = chatState.availableConcepts.isEmpty()
                            ),
                            onAvatarChange = { displayName ->
                                // Handle avatar change through ViewModel - receives display name
                                settingsState = chatViewModel.handleAvatarChange(
                                    displayName = displayName,
                                    boyDisplayName = boyDisplayName,
                                    girlDisplayName = girlDisplayName,
                                    ttsController = ttsController,
                                    currentState = settingsState
                                )
                            },
                            onVoiceChange = { selectedDisplayName ->
                                handleVoiceChange(selectedDisplayName, ttsState, ttsController, aiMessageOutput)
                            },
                            onConceptChange = { concept ->
                                pendingConceptSelection = concept
                                if (chatViewModel.hasExistingSession(concept)) {
                                    pendingConceptSelection = concept
                                    showSessionResumeDialog = true
                                } else {
                                    chatViewModel.onIntent(ChatIntent.SelectConcept(concept))
                                }
                            },
                            onLevelChange = { levelCode ->
                                settingsState = settingsState.copy(selectedStudentLevel = levelCode)
                                chatViewModel.onIntent(ChatIntent.SetStudentLevel(levelCode))
                            },
                            onSpeedChange = { label ->
                                settingsState = settingsState.copy(selectedSpeed = label)
                                handleSpeedChange(label, ttsController, ttsState, aiMessageOutput)
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
                            defaultInputLabel = if (chatState.isKannada) "ಡೀಫಾಲ್ಟ್ ಇನ್‌ಪುಟ್" else "Default input",
                            voiceFirstLabel = if (chatState.isKannada) "ಧ್ವನಿ ಮೊದಲು" else "Voice first",
                            textFirstLabel = if (chatState.isKannada) "ಪಠ್ಯ ಮೊದಲು" else "Text first",
                            onFontSizeChange = { sp ->
                                sharedPrefs.setChatMessageFontSp(sp)
                                settingsState = settingsState.copy(messageFontSp = sp)
                            },
                        )
                    }
                )
                if (!isConversationStarted) {
                    // Initial centered avatar with loading indicator
                    InitialAvatarView(
                        avatarSize = avatarSize,
                        ttsController = ttsController,
                        modifier = Modifier.weight(0.1f).background(White),
                        isLoading = chatState.isLoading,
                        languageCode = chatState.currentLanguage,
                        useNativeAvatar = useNativeAvatar,
                        ttsState = ttsState,
                        wordBoundaryIndex = wordBoundaryIndex,
                        isListening = sttState.isListening,
                    )
                } else {
                    // Conversation view with avatar and scrollable content
                    ConversationView(
                        avatarSize = avatarSize,
                        chatState = chatState,
                        lastAIMessage = lastAIMessage,
                        ttsController = ttsController,
                        modifier = Modifier.weight(0.1f).background(White),
                        useNativeAvatar = useNativeAvatar,
                        ttsState = ttsState,
                        wordBoundaryIndex = wordBoundaryIndex,
                        isListening = sttState.isListening,
                        messageFontSize = ChatMessageFontSize
                            .resolveFontSp(settingsState.messageFontSp, mathAgent = false).sp,
                        messageLineHeight = ChatMessageFontSize
                            .lineHeightSp(
                                ChatMessageFontSize.resolveFontSp(
                                    settingsState.messageFontSp,
                                    mathAgent = false,
                                ),
                            ).sp,
                    )
                }
            }
        }

        // Resource Card - centered on screen
        if (chatState.resourceCardState !is ResourceCardUiState.Hidden) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                ResourcesCard(
                    state = chatState.resourceCardState,
                    onDismiss = { chatViewModel.onIntent(ChatIntent.DismissResource) }
                )
            }
        }

        AgentSessionTimeGate(
            languageCode = chatState.currentLanguage,
            inTrialMode = TrialSessionStore.activeTrialItemId != null,
            onProceed = {
                chatViewModel.recordTrialProceed()
                TrialSessionStore.markSoftProceedToNext()
                backDispatcher?.onBackPressed()
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        LoadStallProceedGate(
            waiting = chatState.isLoading && chatState.messages.isEmpty(),
            resetKey = chatState.selectedConcept,
            errorMessage = chatState.messages.lastOrNull { it.isError }?.content
                ?.takeIf { chatState.isLoading || chatState.messages.none { m -> !m.isError } },
            onContinue = {
                chatViewModel.recordTrialProceed()
                TrialSessionStore.markSoftProceedToNext()
                backDispatcher?.onBackPressed()
            },
        )
    }


    // Session resume dialog
    AppDialog(
        show = showSessionResumeDialog,
        title = stringResource(R.string.existing_session_found),
        message = stringResource(R.string.resume_or_start_fresh),
        confirmText = stringResource(R.string.continue_session),
        dismissText = stringResource(R.string.start_new),
        onConfirm = {
            pendingConceptSelection?.let { chatViewModel.onIntent(ChatIntent.SelectConcept(it)) }
            showSessionResumeDialog = false
            pendingConceptSelection = null
            showSettingsMenu = false
        },
        onDismiss = {
            pendingConceptSelection?.let { chatViewModel.onIntent(ChatIntent.StartFreshSession(it)) }
            showSessionResumeDialog = false
            pendingConceptSelection = null
            showSettingsMenu = false
        }
    )
}

// Helper functions

/**
 * volume Click function check
 * 1. If resource card is showing and TTS was paused for resource, resume TTS for resource
 * 2. If TTS is currently speaking, stop it
 * 3. Else, speak the last AI message
 */
private fun handleVolumeClick(
    chatState: ChatUiState,
    ttsState: TextToSpeech.TTSState,
    lastAIMessage: ChatMessageModel?,
    chatViewModel: ChatViewModel,
    ttsController: TextToSpeech
) {
    when {
        chatState.resourceCardState !is ResourceCardUiState.Hidden && chatState.ttsPausedForResource -> {
            chatViewModel.onIntent(ChatIntent.ResumeTTS)
            lastAIMessage?.let { ttsController.speak(it.content) }
        }
        ttsState.isSpeaking -> ttsController.stop()
        else -> lastAIMessage?.let { ttsController.speak(it.content) }
    }
}

/**
 * Handle voice change from settings
 * 1. it do the voice change
 * 2. if TTS is speaking, stop and restart with new voice
 * 3. if no voice found, do nothing
 */
private fun handleVoiceChange(
    selectedDisplayName: String,
    ttsState: TextToSpeech.TTSState,
    ttsController: TextToSpeech,
    aiMessageOutput: String
) {
    ttsState.availableVoices.find { ttsController.formatVoiceName(it) == selectedDisplayName }?.let { voice ->
        ttsController.setVoice(voice)
        if (ttsState.isSpeaking) {
            ttsController.stop()
            ttsController.speak(aiMessageOutput)
        }
    }
}

/**
 * this function handle speed change from settings
 * 1. it do the speed change
 * 2. if TTS is speaking, stop and restart with new speed
 * 3. if no speed found, set to default 0.75x
 */
private fun handleSpeedChange(
    label: String,
    ttsController: TextToSpeech,
    ttsState: TextToSpeech.TTSState,
    aiMessageOutput: String
) {
    val speed = when (label) {
        "0.75x" -> 0.75f
        "1.0x" -> 1.0f
        "1.25x" -> 1.25f
        "1.5x" -> 1.5f
        else -> 1.0f
    }
    ttsController.setSpeechRate(speed)
    if (ttsState.isSpeaking) {
        ttsController.stop()
        ttsController.speak(aiMessageOutput)
    }
}