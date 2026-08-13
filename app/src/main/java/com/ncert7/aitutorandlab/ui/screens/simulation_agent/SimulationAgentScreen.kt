package com.ncert7.aitutorandlab.ui.screens.simulation_agent

import android.Manifest.permission.RECORD_AUDIO
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import com.ncert7.aitutorandlab.R
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.ncert7.aitutorandlab.domain.examplan.TrialSessionStore
import com.ncert7.aitutorandlab.ui.components.AgentSessionTimeGate
import com.ncert7.aitutorandlab.ui.components.LoadStallProceedGate
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.ncert7.aitutorandlab.config.GamificationFeatureFlags
import com.ncert7.aitutorandlab.service.analytics.ScreenName
import com.ncert7.aitutorandlab.service.analytics.TrackScreenEvent
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.AutoListenAfterAgentTurn
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.ChatBotSettings
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.ChatHeaderIcons
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.ChatMessageFontSize
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.VoiceInputBar
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.InputSection
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.AppDialog
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatBotSettingsState
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatUiState
import com.ncert7.aitutorandlab.ui.screens.simulation_agent.components.SimulationConversationView
import androidx.compose.ui.unit.sp
import com.ncert7.aitutorandlab.ui.screens.simulation_agent.components.rememberSimulationKeyConceptTts
import com.ncert7.aitutorandlab.ui.screens.simulation_agent.components.speakFromApiInsight
import com.ncert7.aitutorandlab.ui.screens.simulation_agent.components.speakSimulationIntro
import com.ncert7.aitutorandlab.ui.screens.simulation_agent.components.speakTitleFallback
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.screens.simulation_agent.viewmodel.SimAgentUiState
import com.ncert7.aitutorandlab.ui.screens.simulation_agent.viewmodel.SimulationAgentViewModel
import com.ncert7.aitutorandlab.domain.simulation.usecase.SimulationIntent
import com.ncert7.aitutorandlab.ui.viewModel.SpeechToText
import com.ncert7.aitutorandlab.ui.viewModel.TextToSpeech
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.ncert7.aitutorandlab.utils.normalizeLanguageCode

/**
 * Simulation Agent Screen - PURELY PRESENTATIONAL
 * All business logic is in SimulationAgentViewModel
 * This composable only:
 * 1. Observes state from ViewModel
 * 2. Renders UI based on state
 * 3. Forwards user actions to ViewModel
 */
@Composable
fun SimulationAgentScreen(
    simulationId: String,
    conceptId: String = "",
    onNavigateBack: () -> Unit,
    ttsController: TextToSpeech = viewModel(),
    sttController: SpeechToText = viewModel()
) {
    val dimens = LocalDimensions.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val viewModel: SimulationAgentViewModel = hiltViewModel()
    TrackScreenEvent(ScreenName.SIMULATIONAGENT)

    var errorCardHeightDp by remember { mutableStateOf(0.dp) }

    // Observe ALL state from ViewModel - no local state management
    val uiState by viewModel.uiState.collectAsState()
    val currentTeacherMessage by viewModel.currentTeacherMessage.collectAsState()
    val isSessionStarted by viewModel.isSessionStarted.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val userInput by viewModel.userInput.collectAsState()
    val isInputEnabled by viewModel.isInputEnabled.collectAsState()
    val shouldTriggerTts by viewModel.shouldTriggerTts.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val showSessionResumeDialog by viewModel.showSessionResumeDialog.collectAsState()
    val simulationUrls by viewModel.simulationUrls.collectAsState()
    val sessionData by viewModel.sessionData.collectAsState()

    // Iframe / session stall → 15s "continue to next" dialog.
    val simHtmlUrl = sessionData?.simulation?.htmlUrl?.takeIf { it.isNotBlank() }
    var simPageReady by remember(simHtmlUrl) { mutableStateOf(false) }
    var simPageFailed by remember(simHtmlUrl) { mutableStateOf(false) }
    val loadStalled =
        (uiState is SimAgentUiState.Loading && simHtmlUrl == null) ||
            errorMessage != null ||
            uiState is SimAgentUiState.Error ||
            (uiState is SimAgentUiState.Success && simHtmlUrl == null) ||
            (simHtmlUrl != null && !simPageReady) ||
            simPageFailed

    // TTS/STT states
    val ttsState by ttsController.state.collectAsState()
    val sttState by sttController.state.collectAsState()
    val wordBoundaryIndex by ttsController.currentWordIndex.collectAsState()
    val useNativeAvatar = GamificationFeatureFlags.isNativeTutorAvatarEnabled(context)

    LaunchedEffect(useNativeAvatar) {
        ttsController.setNativeLipSyncEnabled(useNativeAvatar)
    }

    // Hands-free voice: persisted toggle (default on). Mic auto-opens after the tutor speaks.
    val sharedPrefs = remember { SharedPreferenceUtils(context) }

    // Settings state — message font loads from prefs (shared XS default with Study/Math/Revision).
    var showSettingsMenu by remember { mutableStateOf(false) }
    var settingsState by remember {
        mutableStateOf(ChatBotSettingsState(messageFontSp = sharedPrefs.getChatMessageFontSp()))
    }
    var permissionGranted by remember { mutableStateOf(false) }
    var lastProcessedSpeechText by remember { mutableStateOf("") }

    val normalizedLang = normalizeLanguageCode(currentLanguage.ifEmpty { getCurrentLanguageCode() })
    val (keyConceptTts, _) = rememberSimulationKeyConceptTts(
        languageCode = normalizedLang,
        avatarCode = settingsState.selectedAvatar,
        ttsController = ttsController,
    )

    var handsFreeMode by remember { mutableStateOf(sharedPrefs.getHandsFreeMode()) }
    val handsFreeLabel = if (currentLanguage.startsWith("kn", ignoreCase = true)) "ಧ್ವನಿ ಸಂಭಾಷಣೆ" else "Hands-free voice"

    // Input mode: voice dock vs. text keyboard (persisted default).
    var inputMode by remember { mutableStateOf(if (sharedPrefs.getVoiceFirst()) "voice" else "text") }
    var focusTextField by remember { mutableStateOf(false) }

    // Initialize avatar display name only once when string resources are available
    val boyDisplayName = stringResource(R.string.boy)
    val girlDisplayName = stringResource(R.string.girl)
    val disableDisplayName = stringResource(R.string.disable)

    // Initialize settings state with proper display name on first composition
    LaunchedEffect(Unit) {
        settingsState = viewModel.initializeAvatarDisplayName(
            avatarCode = settingsState.selectedAvatar,
            boyDisplayName = boyDisplayName,
            girlDisplayName = girlDisplayName,
            disableDisplayName = disableDisplayName,
            currentState = settingsState
        )
    }


    /**
     * Animation values (UI-only)
     */
    val avatarSize by animateDpAsState(
        targetValue = if (isSessionStarted) dimens.avatarSizeLarge * 1.5f else dimens.avatarSizeLarge * 2.5f,
        label = "avatarSize"
    )

    /**
     * Permission launcher
     */
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

    /**
     * INITIALIZATION - One-time setup
     * Uses Unit as key so it only runs once per composition lifecycle
     */
    LaunchedEffect(Unit) {
        sttController.initialize(context)
        ttsController.initialize(context)
        viewModel.loadAvailableSimulations()
    }

    /**
     * SESSION INITIALIZATION
     * Uses simulationId as key so it only runs when simulationId changes
     * ViewModel internally checks if session is already started for this ID
     */
    LaunchedEffect(simulationId) {
        keyConceptTts.resetDedupe()
        viewModel.setConceptId(conceptId)
        viewModel.startNewSession(simulationId)
    }

    LaunchedEffect(uiState) {
        if (uiState is SimAgentUiState.Loading) {
            ttsController.stop()
            viewModel.onTtsStopped()
        }
    }

    /**
     * TTS STATE SYNCHRONIZATION
     * Notify ViewModel when TTS state changes
     */
    LaunchedEffect(ttsState.isSpeaking) {
        if (ttsState.isSpeaking) {
            viewModel.handleIntent(SimulationIntent.TtsStarted)
        } else {
            viewModel.handleIntent(SimulationIntent.TtsStopped)
        }
    }

    /**
     * LANGUAGE SYNC
     * Lock the TTS voice + engine language to the selected app language so a Kannada
     * session is read aloud in Kannada (not the en-IN default). Re-runs once device
     * voices load and whenever the language or avatar changes.
     */
    LaunchedEffect(currentLanguage, ttsState.voicesFullyLoaded, settingsState.selectedAvatar) {
        if (currentLanguage.isNotEmpty()) {
            ttsController.setAppLanguage(settingsState.selectedAvatar, currentLanguage)
        }
    }

    /**
     * TTS PLAYBACK CONTROL
     * CRITICAL: Uses shouldTriggerTts flag from ViewModel to prevent re-triggering on config changes
     * Only triggers when ViewModel explicitly sets shouldTriggerTts to true (on new message)
     */
    LaunchedEffect(shouldTriggerTts) {
        if (shouldTriggerTts && currentTeacherMessage.isNotEmpty() && !ttsState.isSpeaking) {
            ttsController.speak(currentTeacherMessage)
            viewModel.handleIntent(SimulationIntent.TtsTriggered) // Acknowledge that TTS was triggered
        }
    }

    /**
     * STT Result Handling
     * Processes speech-to-text results and updates input
     */
    LaunchedEffect(sttState.resultText, sttState.isListening) {
        val spoken = sttState.resultText.trim()
        if (spoken.isNotEmpty() &&
            !sttState.isListening &&
            spoken != lastProcessedSpeechText
        ) {
            lastProcessedSpeechText = spoken
            // Auto-send captured voice when ready; ignore 1-char junk (pocket/noise STT).
            // Otherwise stage it in the input box.
            if (isInputEnabled && spoken.length >= 2) {
                ttsController.stop()
                viewModel.handleIntent(SimulationIntent.SendUserResponse(spoken))
                viewModel.onUserInputChanged("")
            } else {
                viewModel.onUserInputChanged(spoken)
            }
        }
    }

    /**
     * Clean up STT tracking when input is cleared
     */
    LaunchedEffect(userInput) {
        if (userInput.isEmpty() && lastProcessedSpeechText.isNotEmpty()) {
            lastProcessedSpeechText = ""
        }
    }

    /**
     * Back press handling
     */
    BackHandler {
        val consumed = viewModel.onBackPressed()
        if (!consumed) {
            onNavigateBack()
        }
    }

    /**
     * Voice options (derived state)
     */
    val voiceOptions = remember(ttsState.availableVoices, currentLanguage, settingsState.selectedAvatar) {
        ttsController.getFilteredVoiceOptions(currentLanguage.ifEmpty { "en" }, settingsState.selectedAvatar)
    }

    val displayedVoiceName = remember(ttsState.selectedVoice, currentLanguage, settingsState.selectedAvatar) {
        ttsState.selectedVoice?.let { ttsController.formatVoiceName(it) }
            ?: ttsController.getDefaultVoiceName(currentLanguage.ifEmpty { "en" }, settingsState.selectedAvatar)
    }

    // Hands-free loop: auto-open the mic once the tutor's turn completes (voice mode only).
    AutoListenAfterAgentTurn(
        enabled = inputMode == "voice" && handsFreeMode && !showSettingsMenu,
        turnComplete = !ttsState.isSpeaking &&
                isInputEnabled &&
                currentTeacherMessage.isNotEmpty(),
        isListening = sttState.isListening,
        canListen = permissionGranted && sttState.isInitialized,
        onStartListening = {
            sttController.startListening(currentLanguage)
        }
    )

    Box(modifier = Modifier.fillMaxSize().background(White)) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            /**
             * Header icons
             */
            ChatHeaderIcons(
                isSpeaking = ttsState.isSpeaking,
                showSettingsMenu = showSettingsMenu,
                onVolumeClick = {
                    if (ttsState.isSpeaking) {
                        ttsController.stop()
                    } else if (currentTeacherMessage.isNotEmpty()) {
                        ttsController.speak(currentTeacherMessage)
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
                            availableConcepts = viewModel.availableSimulations.collectAsState().value.map { it.title },
                            selectedConcept = simulationId,
                            isLoadingConcepts = viewModel.simulationsLoading.collectAsState().value
                        ),
                        onAvatarChange = { displayName ->
                            // Handle avatar change through ViewModel
                            settingsState = viewModel.handleAvatarChange(
                                displayName = displayName,
                                boyDisplayName = boyDisplayName,
                                girlDisplayName = girlDisplayName,
                                ttsController = ttsController,
                                currentState = settingsState
                            )
                            viewModel.onAvatarChanged()
                        },
                        onVoiceChange = { selectedDisplayName ->
                            ttsState.availableVoices.find {
                                ttsController.formatVoiceName(it) == selectedDisplayName
                            }?.let { voice ->
                                ttsController.setVoice(voice)
                                if (ttsState.isSpeaking) {
                                    ttsController.stop()
                                    ttsController.speak(currentTeacherMessage)
                                }
                                viewModel.onVoiceChanged()
                            }
                        },
                        onConceptChange = { selectedTitle ->
                            // Find the simulation ID from the title
                            val selectedSimulation = viewModel.availableSimulations.value.find { it.title == selectedTitle }
                            selectedSimulation?.let { simulation ->
                                // Close settings menu
                                showSettingsMenu = false
                                // Start the selected simulation
                                viewModel.startNewSession(simulation.id)
                            }
                        },
                        onLevelChange = { levelCode ->
                            settingsState = settingsState.copy(selectedStudentLevel = levelCode)
                        },
                        onSpeedChange = { label ->
                            settingsState = settingsState.copy(selectedSpeed = label)
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
                                ttsController.speak(currentTeacherMessage)
                            }
                            viewModel.onSpeedChanged()
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
                        defaultInputLabel = if (currentLanguage.startsWith("kn", ignoreCase = true)) "ಡೀಫಾಲ್ಟ್ ಇನ್‌ಪುಟ್" else "Default input",
                        voiceFirstLabel = if (currentLanguage.startsWith("kn", ignoreCase = true)) "ಧ್ವನಿ ಮೊದಲು" else "Voice first",
                        textFirstLabel = if (currentLanguage.startsWith("kn", ignoreCase = true)) "ಪಠ್ಯ ಮೊದಲು" else "Text first",
                        onFontSizeChange = { sp ->
                            sharedPrefs.setChatMessageFontSp(sp)
                            settingsState = settingsState.copy(messageFontSp = sp)
                        },
                    )
                }
            )

            /**
             * Error handling
             */
            if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimens.spaceMedium)
                        .onGloballyPositioned { coords ->
                            errorCardHeightDp = with(density) { coords.size.height.toDp() }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(dimens.spaceMedium),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(dimens.spaceSmall))
                        Text(
                            text = errorMessage ?: "Unknown error occurred",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(dimens.spaceMedium))
                        Button(
                            onClick = { viewModel.handleIntent(SimulationIntent.RetrySession(simulationId)) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            /**
             * Main content area
             */
            SimulationConversationView(
                avatarSize = avatarSize,
                currentMessage = currentTeacherMessage,
                isLoading = uiState is SimAgentUiState.Loading,
                // Keep the live WebView while the teacher thinks — tearing it down on every
                // /respond made the agent look broken (quotes panel for 15–20s).
                isSimulationLoading = uiState is SimAgentUiState.Loading &&
                    sessionData?.simulation?.htmlUrl.isNullOrBlank(),
                isHtmlLoading = simHtmlUrl != null && !simPageReady && !simPageFailed,
                ttsController = ttsController,
                messageFontSize = ChatMessageFontSize
                    .resolveFontSp(settingsState.messageFontSp, mathAgent = false).sp,
                messageLineHeight = ChatMessageFontSize
                    .lineHeightSp(
                        ChatMessageFontSize.resolveFontSp(
                            settingsState.messageFontSp,
                            mathAgent = false,
                        ),
                    ).sp,
                onParamsChanged = { viewModel.handleIntent(SimulationIntent.ParametersChanged(it)) },
                simulationUrl = sessionData?.simulation?.htmlUrl?.takeIf { it.isNotBlank() },
                onPageFinished = {
                    simPageReady = true
                    simPageFailed = false
                    viewModel.onSimulationUrlLoaded(simulationId)
                    scope.launch {
                        delay(3_000)
                        if (
                            keyConceptTts.hasSpokenForSimulation(simulationId) ||
                            keyConceptTts.hasPendingForSimulation(simulationId)
                        ) return@launch
                        sessionData?.concepts?.currentConcept?.keyInsight
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { insight ->
                                keyConceptTts.speakFromApiInsight(
                                    keyInsight = insight,
                                    simulationKey = simulationId,
                                    conceptIndex = sessionData?.concepts?.currentIndex ?: 0,
                                )
                            }
                            ?: sessionData?.simulation?.title?.trim()?.takeIf { it.isNotEmpty() }?.let { title ->
                                keyConceptTts.speakTitleFallback(
                                    title = title,
                                    simulationKey = simulationId,
                                )
                            }
                    }
                },
                onLoadFailed = { simPageFailed = true },
                onSimulationIntroReported = { htmlText ->
                    htmlText.trim().takeIf { it.isNotEmpty() }?.let { text ->
                        keyConceptTts.speakSimulationIntro(
                            text = text,
                            simulationKey = simulationId,
                        )
                    }
                },
                onSimulationFooterReported = { _ ->
                    // Auto second-narration of the footer/description text is DISABLED — it
                    // interrupted the learner mid-task. The coach narrates each round on demand.
                },
                languageCode = normalizeLanguageCode(currentLanguage),
                useNativeAvatar = useNativeAvatar,
                ttsState = ttsState,
                wordBoundaryIndex = wordBoundaryIndex,
                isListening = sttState.isListening,
                modifier = Modifier.weight(1f).background(White),
            )

            /**
             * User Input section — voice dock or text keyboard.
             */
            if (inputMode == "voice") {
                VoiceInputBar(
                    isKannada = currentLanguage.startsWith("kn", ignoreCase = true),
                    isListening = sttState.isListening,
                    isSpeaking = ttsState.isSpeaking,
                    isThinking = uiState is SimAgentUiState.Loading,
                    transcript = sttState.resultText,
                    statusMessage = sttState.statusMessage,
                    amplitude = sttState.audioAmplitude,
                    onMicTap = {
                        if (ttsState.isSpeaking) ttsController.stop()
                        if (permissionGranted && sttState.isInitialized) {
                            sttController.startListening(currentLanguage)
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
                )
            } else {
                InputSection(
                    chatState = ChatUiState(
                        inputText = userInput,
                        isLoading = !isInputEnabled
                    ),
                    sttState = sttState,
                    onTextChange = { viewModel.handleIntent(SimulationIntent.UpdateInput(it)) },
                    onSendClick = {
                        ttsController.stop()
                        viewModel.handleIntent(SimulationIntent.SendUserResponse(userInput))
                    },
                    onSpeakClick = {
                        // Text-mode mic → switch to the voice dock and start listening.
                        focusTextField = false
                        inputMode = "voice"
                        if (ttsState.isSpeaking) ttsController.stop()
                        if (permissionGranted && sttState.isInitialized) {
                            sttController.startListening(currentLanguage)
                        } else if (!permissionGranted) {
                            permissionLauncher.launch(RECORD_AUDIO)
                        }
                    },
                    onStopListening = { sttController.stopListening() },
                    onSuggestionClick = { /* Not used */ },
                    shouldDisableSend = !isInputEnabled,
                    showImageIcon = false,
                    kannadaKeyboard = currentLanguage.startsWith("kn", ignoreCase = true),
                    autoFocus = focusTextField
                )
            }
        }

        AgentSessionTimeGate(
            languageCode = currentLanguage,
            inTrialMode = TrialSessionStore.activeTrialItemId != null,
            onProceed = {
                viewModel.recordTrialProceed()
                TrialSessionStore.markSoftProceedToNext()
                onNavigateBack()
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        LoadStallProceedGate(
            waiting = loadStalled,
            resetKey = simHtmlUrl ?: simulationId,
            errorMessage = errorMessage?.takeIf { it.isNotBlank() }
                ?: (uiState as? SimAgentUiState.Error)?.message?.takeIf { it.isNotBlank() },
            onContinue = {
                viewModel.recordTrialProceed()
                TrialSessionStore.markSoftProceedToNext()
                onNavigateBack()
            },
        )
    }

    // Session Resume Dialog - Ask to continue or start fresh
    AppDialog(
        show = showSessionResumeDialog,
        title = stringResource(R.string.existing_session_found),
        message = stringResource(R.string.resume_or_start_fresh),
        confirmText = stringResource(R.string.continue_session),
        dismissText = stringResource(R.string.start_new),
        onConfirm = {
            viewModel.handleIntent(SimulationIntent.ContinueExistingSession(simulationId))
        },
        onDismiss = {
            viewModel.handleIntent(SimulationIntent.StartFreshSession(simulationId))
        }
    )}
