package com.ncert7.aitutorandlab.ui.screens.revisionscreen

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.service.analytics.ScreenName
import com.ncert7.aitutorandlab.service.analytics.TrackScreenEvent
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.AutoListenAfterAgentTurn
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.ChatBotSettings
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.ChatHeaderIcons
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.VoiceInputBar
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.ChatMessageFontSize
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.ConversationView
import androidx.compose.ui.unit.sp
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.InitialAvatarView
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.InputSection
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatBotSettingsState
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.isConversationStarted
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.lastAiMessage
import com.ncert7.aitutorandlab.ui.theme.White
import com.ncert7.aitutorandlab.ui.screens.revisionscreen.viewmodel.RevisionViewModel
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.AppDialog
import com.ncert7.aitutorandlab.ui.viewModel.SpeechToText
import com.ncert7.aitutorandlab.ui.viewModel.TextToSpeech

/**
 * Revision screen for chapter revision sessions.
 * Uses full ChatBotSettings with chapter selection instead of concepts.
 */
@Composable
fun RevisionScreen(
    chapterId: String,
    onBackClick: () -> Unit = {},
    revisionViewModel: RevisionViewModel = hiltViewModel(),
    sttController: SpeechToText = hiltViewModel(),
    ttsController: TextToSpeech = hiltViewModel()
) {
    DebugLogger.debugLog("RevisionScreen", "RevisionScreen composable - chapterId: $chapterId")

    TrackScreenEvent(ScreenName.REVISION)

    val context = LocalContext.current
    val sharedPrefs = remember { SharedPreferenceUtils(context) }

    // State collectors
    val chatState by revisionViewModel.uiState.collectAsState()
    val sttState by sttController.state.collectAsState()
    val ttsState by ttsController.state.collectAsState()
    val wordBoundaryIndex by ttsController.currentWordIndex.collectAsState()
    val useNativeAvatar = GamificationFeatureFlags.isNativeTutorAvatarEnabled(context)

    LaunchedEffect(useNativeAvatar) {
        ttsController.setNativeLipSyncEnabled(useNativeAvatar)
    }

    // Local UI state
    var permissionGranted by remember { mutableStateOf(false) }
    var lastProcessedSpeechText by remember { mutableStateOf("") }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var settingsState by remember {
        mutableStateOf(ChatBotSettingsState(messageFontSp = sharedPrefs.getChatMessageFontSp()))
    }
    var showSessionResumeDialog by remember { mutableStateOf(false) }

    // Hands-free voice: persisted toggle (default on). Mic auto-opens after the tutor speaks.
    var handsFreeMode by remember { mutableStateOf(sharedPrefs.getHandsFreeMode()) }
    val handsFreeLabel = if (chatState.isKannada) "ಧ್ವನಿ ಸಂಭಾಷಣೆ" else "Hands-free voice"

    // Input mode: voice dock vs. text keyboard (persisted default).
    var inputMode by remember { mutableStateOf(if (sharedPrefs.getVoiceFirst()) "voice" else "text") }
    var focusTextField by remember { mutableStateOf(false) }
    val agentThinking = (chatState.isLoading || chatState.isTyping || chatState.waitingForTTSToComplete) &&
            !ttsState.isSpeaking

    val lastAIMessage = chatState.lastAiMessage
    val isConversationStarted = chatState.isConversationStarted

    // Voice options for TTS
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

    // Animation values
    val avatarSize by animateDpAsState(
        targetValue = if (isConversationStarted) 100.dp else 180.dp,
        label = "avatarSize"
    )

    // Permission launcher
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

    // String resources
    val boyDisplayName = stringResource(R.string.boy)
    val girlDisplayName = stringResource(R.string.girl)
    val disableDisplayName = stringResource(R.string.disable)

    // Initialize
    LaunchedEffect(Unit) {
        val userId = sharedPrefs.getUserId() ?: "guest"
        revisionViewModel.initialize(userId, chapterId)
        sttController.initialize(context)
        ttsController.initialize(context)

        // Initialize avatar display name
        settingsState = revisionViewModel.initializeAvatarDisplayName(
            avatarCode = settingsState.selectedAvatar,
            boyDisplayName = boyDisplayName,
            girlDisplayName = girlDisplayName,
            disableDisplayName = disableDisplayName,
            currentState = settingsState
        )
    }

    // Observe dialog state from ViewModel
    LaunchedEffect(chatState.showSessionResumeDialog) {
        showSessionResumeDialog = chatState.showSessionResumeDialog
    }

    // Keep the TTS voice + language locked to the app-selected language so a Kannada
    // session isn't read aloud in English. Re-runs once device voices load / language
    // or avatar changes.
    LaunchedEffect(chatState.isKannada, ttsState.voicesFullyLoaded, settingsState.selectedAvatar) {
        val langCode = if (chatState.isKannada) "kn-IN" else "en-IN"
        ttsController.setAppLanguage(settingsState.selectedAvatar, langCode)
    }

    // Handle speech recognition — auto-send when the agent is free (speak → it sends),
    // otherwise stage the transcript in the input box.
    LaunchedEffect(sttState.isListening) {
        val spoken = sttState.resultText.trim()
        if (!sttState.isListening && spoken.isNotEmpty() && spoken != lastProcessedSpeechText) {
            lastProcessedSpeechText = spoken
            val busy = chatState.isTyping || chatState.isLoading || ttsState.isSpeaking
            if (busy) {
                revisionViewModel.updateInputText(spoken)
            } else {
                revisionViewModel.sendMessage(spoken)
                revisionViewModel.updateInputText("")
            }
        }
    }

    // Stop TTS when user starts speaking
    LaunchedEffect(sttState.isListening) {
        if (sttState.isListening && ttsState.isSpeaking) {
            ttsController.stop()
        }
    }

    LaunchedEffect(chatState.shouldStartTTS, ttsState.isInitialized) {
        if (chatState.shouldStartTTS && ttsState.isInitialized) {
            val textToSpeak = chatState.fullTextForTTS
            if (textToSpeak.isNotEmpty()) {
                if (ttsState.isSpeaking) {
                    ttsController.stop()
                    kotlinx.coroutines.delay(50)
                }
                DebugLogger.debugLog("RevisionScreen", "Starting TTS in parallel with typing animation")
                ttsController.speak(textToSpeak)
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            sttController.destroy()
        }
    }

    // Calculate shouldDisableSend - disable send/mic when typing, loading, OR TTS is speaking
    val shouldDisableSend = remember(chatState.isTyping, chatState.isLoading, ttsState.isSpeaking) {
        chatState.isTyping || chatState.isLoading || ttsState.isSpeaking
    }

    // Hands-free loop: auto-open the mic once the tutor's turn completes (voice mode only).
    AutoListenAfterAgentTurn(
        enabled = inputMode == "voice" && handsFreeMode && !showSettingsMenu,
        turnComplete = !ttsState.isSpeaking &&
                !chatState.isLoading &&
                !chatState.isTyping &&
                !chatState.waitingForTTSToComplete &&
                lastAIMessage != null,
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
                    VoiceInputBar(
                        isKannada = chatState.isKannada,
                        isListening = sttState.isListening,
                        isSpeaking = ttsState.isSpeaking,
                        isThinking = agentThinking,
                        transcript = sttState.resultText,
                        statusMessage = sttState.statusMessage,
                        amplitude = sttState.audioAmplitude,
                        onMicTap = {
                            if (permissionGranted && sttState.isInitialized) {
                                sttController.startListening(if (chatState.isKannada) "kn-IN" else "en-IN")
                            } else if (!permissionGranted) {
                                permissionLauncher.launch(RECORD_AUDIO)
                            }
                        },
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
                        chatState = chatState,
                        sttState = sttState,
                        onTextChange = { revisionViewModel.updateInputText(it) },
                        onSendClick = {
                            if (chatState.inputText.isNotBlank()) {
                                revisionViewModel.sendMessage(chatState.inputText)
                            }
                        },
                        onSpeakClick = {
                            // Text-mode mic → switch to the voice dock and start listening.
                            focusTextField = false
                            inputMode = "voice"
                            if (permissionGranted && sttState.isInitialized) {
                                sttController.startListening(if (chatState.isKannada) "kn-IN" else "en-IN")
                            } else if (!permissionGranted) {
                                permissionLauncher.launch(RECORD_AUDIO)
                            }
                        },
                        onStopListening = { sttController.stopListening() },
                        onSuggestionClick = { },
                        shouldDisableSend = shouldDisableSend,
                        showImageIcon = false,
                        kannadaKeyboard = chatState.isKannada,
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
                // Header with full ChatBotSettings
                ChatHeaderIcons(
                    isSpeaking = ttsState.isSpeaking,
                    showSettingsMenu = showSettingsMenu,
                    onVolumeClick = {
                        if (ttsState.isSpeaking) {
                            ttsController.stop()
                        } else if (lastAIMessage != null) {
                            ttsController.speak(lastAIMessage.content)
                        }
                    },
                    onSettingsClick = { showSettingsMenu = !showSettingsMenu },
                    settingsContent = {
                        ChatBotSettings(
                            expanded = true,
                            onDismiss = { showSettingsMenu = false },
                            state = settingsState.copy(
                                voiceOptions = voiceOptions,
                                displayedVoiceName = displayedVoiceName,
                                selectedConcept = chatState.selectedConcept
                            ),
                            onAvatarChange = { displayName ->
                                settingsState = revisionViewModel.handleAvatarChange(
                                    displayName = displayName,
                                    boyDisplayName = boyDisplayName,
                                    girlDisplayName = girlDisplayName,
                                    ttsController = ttsController,
                                    currentState = settingsState
                                )
                            },
                            onVoiceChange = { selectedDisplayName ->
                                val selectedVoice = ttsState.availableVoices.find { voice ->
                                    ttsController.formatVoiceName(voice) == selectedDisplayName
                                }
                                selectedVoice?.let {
                                    ttsController.setVoice(it)
                                    if (aiMessageOutput.isNotEmpty() && !ttsState.isSpeaking) {
                                        ttsController.speak(aiMessageOutput)
                                    }
                                }
                            },
                            onConceptChange = { chapter ->
                                revisionViewModel.changeChapter(chapter)
                                showSettingsMenu = false
                            },
                            onLevelChange = { levelCode ->
                                settingsState = settingsState.copy(selectedStudentLevel = levelCode)
                            },
                            onSpeedChange = { speedLabel ->
                                settingsState = settingsState.copy(selectedSpeed = speedLabel)
                                val speedValue = when (speedLabel) {
                                    "0.5x" -> 0.5f
                                    "0.75x" -> 0.75f
                                    "1.0x" -> 1.0f
                                    "1.25x" -> 1.25f
                                    "1.5x" -> 1.5f
                                    else -> 1.0f
                                }
                                ttsController.setSpeechRate(speedValue)
                                if (aiMessageOutput.isNotEmpty() && !ttsState.isSpeaking) {
                                    ttsController.speak(aiMessageOutput)
                                }
                            },
                            isRevisionMode = true,
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

                if (isConversationStarted) {
                    ConversationView(
                        avatarSize = avatarSize,
                        chatState = chatState,
                        lastAIMessage = lastAIMessage,
                        ttsController = ttsController,
                        modifier = Modifier.weight(1f),
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
                } else {
                    InitialAvatarView(
                        avatarSize = avatarSize,
                        ttsController = ttsController,
                        isLoading = chatState.isLoading,
                        languageCode = chatState.currentLanguage,
                        modifier = Modifier.weight(1f),
                        useNativeAvatar = useNativeAvatar,
                        ttsState = ttsState,
                        wordBoundaryIndex = wordBoundaryIndex,
                        isListening = sttState.isListening,
                    )
                }
            }
        }

        AgentSessionTimeGate(
            languageCode = chatState.currentLanguage,
            inTrialMode = TrialSessionStore.activeTrialItemId != null,
            onProceed = {
                revisionViewModel.recordTrialProceed()
                onBackClick()
            },
            modifier = Modifier.align(Alignment.BottomCenter),
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
            revisionViewModel.resumeExistingSession()
            showSessionResumeDialog = false
        },
        onDismiss = {
            revisionViewModel.startFreshSession()
            showSessionResumeDialog = false
        }
    )
}
