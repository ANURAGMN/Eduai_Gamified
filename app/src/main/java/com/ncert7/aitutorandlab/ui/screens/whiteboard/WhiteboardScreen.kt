package com.ncert7.aitutorandlab.ui.screens.whiteboard

import android.Manifest
import android.content.pm.PackageManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.remote.WbRevealTimelineUnit
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.AutoListenAfterAgentTurn
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.ChatHeaderIcons
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatBotSettingsState
import com.ncert7.aitutorandlab.ui.screens.whiteboard.components.WhiteboardSettings
import com.ncert7.aitutorandlab.ui.screens.whiteboard.components.WhiteboardVoiceInputBar
import com.ncert7.aitutorandlab.ui.viewModel.SpeechToText
import com.ncert7.aitutorandlab.ui.viewModel.TextToSpeech

@Composable
fun CustomTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhiteboardConceptsScreen(
    onBackClick: () -> Unit,
    onConceptClick: (String) -> Unit,
    viewModel: WhiteboardViewModel = hiltViewModel()
) {
    val concepts by viewModel.concepts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var selectedFilter by remember { mutableStateOf("All") }

    val filteredConcepts = remember(concepts, selectedFilter) {
        if (selectedFilter == "All") {
            concepts
        } else {
            concepts.filter { it.subject?.equals(selectedFilter, ignoreCase = true) == true }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchConcepts()
    }

    Scaffold(
        topBar = { CustomTopBar(title = "Whiteboard Concepts", onBack = onBackClick) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF4F6F9))) {
            if (errorMessage != null) {
                ErrorBanner(errorMessage!!)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Science", "Math").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 12.sp) },
                        modifier = Modifier.height(32.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = Color.Black,
                            selectedContainerColor = Color(0xFF4F46E5),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (isLoading && concepts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp)) {
                    items(filteredConcepts) { concept ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .clickable { onConceptClick(concept.conceptId) },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Text(concept.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                                Spacer(modifier = Modifier.height(2.dp))

                                val displaySubject = concept.subject?.replaceFirstChar { it.uppercase() } ?: "N/A"

                                Text(
                                    "Subject: $displaySubject | Chapter: ${concept.chapter} | Visuals: ${if(concept.hasScene) "Yes" else "No"}",
                                    color = Color.DarkGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhiteboardChatbotScreen(
    conceptId: String,
    onBackClick: () -> Unit,
    viewModel: WhiteboardViewModel = hiltViewModel(),
    ttsController: TextToSpeech = hiltViewModel(),
    sttController: SpeechToText = hiltViewModel()
) {
    val context = LocalContext.current
    val sharedPrefs = remember { SharedPreferenceUtils(context) }

    val chatHistory by viewModel.chatHistory.collectAsState()
    val whiteboardData by viewModel.whiteboardData.collectAsState()
    val latestTurn by viewModel.latestTurn.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val title by viewModel.conceptTitle.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val conceptsList by viewModel.concepts.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showSettingsMenu by remember { mutableStateOf(false) }

    var handsFreeMode by remember { mutableStateOf(sharedPrefs.getHandsFreeMode()) }
    var inputMode by remember { mutableStateOf(if (sharedPrefs.getVoiceFirst()) "voice" else "text") }
    var focusTextField by remember { mutableStateOf(false) }

    var settingsState by remember {
        // Always force the default size to Medium (28f) when opening a concept
        mutableStateOf(ChatBotSettingsState(messageFontSp = 28f))
    }

    val ttsState by ttsController.state.collectAsState()
    val sttState by sttController.state.collectAsState()

    val voiceOptions = remember(ttsState.availableVoices, settingsState.selectedAvatar) {
        ttsController.getFilteredVoiceOptions("en", settingsState.selectedAvatar)
    }
    val displayedVoiceName = remember(ttsState.selectedVoice, settingsState.selectedAvatar) {
        ttsState.selectedVoice?.let { ttsController.formatVoiceName(it) }
            ?: ttsController.getDefaultVoiceName("en", settingsState.selectedAvatar)
    }

    val scrollState = rememberScrollState()
    var allowAutoScroll by remember { mutableStateOf(true) }
    val isDragged by scrollState.interactionSource.collectIsDraggedAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var permissionGranted by remember { mutableStateOf(false) }
    var pendingMicPermissionRequest by remember { mutableStateOf(false) }

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
    }

    val beginListening: () -> Unit = {
        if (permissionGranted && sttState.isInitialized) {
            sttController.startListening("en-IN")
        } else if (!permissionGranted) {
            if (!pendingMicPermissionRequest) {
                pendingMicPermissionRequest = true
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    LaunchedEffect(Unit) {
        permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        ttsController.initialize(context)
        sttController.initialize(context)
    }

    LaunchedEffect(showSettingsMenu) {
        if (showSettingsMenu && conceptsList.isEmpty()) {
            settingsState = settingsState.copy(isLoadingConcepts = true)
            viewModel.fetchConcepts()
        }
    }

    LaunchedEffect(conceptsList, conceptId) {
        if (conceptsList.isNotEmpty()) {
            settingsState = settingsState.copy(
                availableConcepts = conceptsList.map { it.conceptId },
                displayConcepts = conceptsList.map { it.title },
                selectedConcept = conceptId,
                isLoadingConcepts = false
            )
        } else {
            settingsState = settingsState.copy(selectedConcept = conceptId)
        }
    }

    LaunchedEffect(latestTurn) {
        latestTurn?.let { turn ->
            allowAutoScroll = true
            ttsController.speak(turn.message)
        }
    }

    LaunchedEffect(focusTextField) {
        if (focusTextField) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    var wasListening by remember { mutableStateOf(false) }
    LaunchedEffect(sttState.isListening) {
        if (wasListening && !sttState.isListening) {
            val spokenText = sttState.resultText.trim()
            if (spokenText.isNotBlank()) {
                viewModel.sendMessage(spokenText)
            }
        }
        wasListening = sttState.isListening
    }

    val currentWordIndex by ttsController.currentWordIndex.collectAsState()
    val latestMessage = latestTurn?.message ?: ""
    val currentCharIndex = remember(currentWordIndex, ttsState.isSpeaking, latestMessage) {
        when {
            !ttsState.isSpeaking -> Int.MAX_VALUE
            currentWordIndex == -1 -> 0
            else -> {
                val matches = Regex("\\S+").findAll(latestMessage).toList()
                if (currentWordIndex in matches.indices) {
                    matches[currentWordIndex].range.first
                } else {
                    Int.MAX_VALUE
                }
            }
        }
    }

    LaunchedEffect(isDragged) {
        if (isDragged) allowAutoScroll = false
    }

    LaunchedEffect(currentCharIndex) {
        if (allowAutoScroll && currentCharIndex < Int.MAX_VALUE && scrollState.maxValue > 0) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    LaunchedEffect(conceptId) {
        viewModel.startSession(conceptId)
    }

    Scaffold(
        topBar = {
            CustomTopBar(title = title) {
                ttsController.stop()
                onBackClick()
            }
        },
        modifier = Modifier.imePadding()
    ) { padding ->

        AutoListenAfterAgentTurn(
            enabled = inputMode == "voice" && handsFreeMode && !showSettingsMenu,
            turnComplete = !ttsState.isSpeaking && !isLoading && latestTurn != null,
            isListening = sttState.isListening,
            canListen = permissionGranted && sttState.isInitialized,
            onStartListening = {
                sttController.startListening("en-IN")
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding(), bottom = 0.dp)
                .background(Color(0xFFF4F6F9))
        ) {
            ChatHeaderIcons(
                isSpeaking = ttsState.isSpeaking,
                showSettingsMenu = showSettingsMenu,
                onVolumeClick = {
                    if (ttsState.isSpeaking) ttsController.stop()
                    else ttsController.speak(latestMessage)
                },
                onSettingsClick = { showSettingsMenu = !showSettingsMenu },
                settingsContent = {
                    WhiteboardSettings(
                        expanded = true,
                        onDismiss = { showSettingsMenu = false },
                        state = settingsState.copy(
                            voiceOptions = voiceOptions,
                            displayedVoiceName = displayedVoiceName
                        ),
                        onVoiceChange = { selectedDisplayName ->
                            ttsState.availableVoices.find { ttsController.formatVoiceName(it) == selectedDisplayName }?.let { voice ->
                                ttsController.setVoice(voice)
                                if (ttsState.isSpeaking) {
                                    ttsController.stop()
                                    ttsController.speak(latestMessage)
                                }
                            }
                        },
                        onConceptChange = { newConceptId ->
                            if (newConceptId != conceptId) {
                                viewModel.startSession(newConceptId)
                                settingsState = settingsState.copy(selectedConcept = newConceptId)
                                showSettingsMenu = false
                            }
                        },
                        onSpeedChange = { label ->
                            settingsState = settingsState.copy(selectedSpeed = label)
                            val speed = when (label) { "0.75x" -> 0.75f; "1.25x" -> 1.25f; "1.5x" -> 1.5f; else -> 1.0f }
                            ttsController.setSpeechRate(speed)
                        },
                        handsFreeMode = handsFreeMode,
                        onHandsFreeChange = {
                            handsFreeMode = it
                            sharedPrefs.setHandsFreeMode(it)
                        },
                        voiceFirst = inputMode == "voice",
                        onInputModeChange = { voiceFirst ->
                            sharedPrefs.setVoiceFirst(voiceFirst)
                            if (!voiceFirst) sttController.stopListening()
                            focusTextField = false
                            inputMode = if (voiceFirst) "voice" else "text"
                        },
                        onFontSizeChange = { sp ->
                            sharedPrefs.setChatMessageFontSp(sp)
                            settingsState = settingsState.copy(messageFontSp = sp)
                        }
                    )
                }
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.Black),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 0.dp, bottom = 4.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    Text("Visual Board", fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    if (whiteboardData == null || whiteboardData?.svgString.isNullOrEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Visuals loading...", color = Color.LightGray)
                        }
                    } else {
                        val timeline = latestTurn?.revealTimeline ?: emptyList()
                        SvgRenderer(svgString = whiteboardData!!.svgString!!, timeline = timeline, currentCharIndex = currentCharIndex)
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.Black),
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(8.dp)) {
                    Text("Process Flow", fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                allowAutoScroll = true
                                ttsController.stop()
                                ttsController.speak(latestMessage)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4F46E5))
                        ) { Text("Replay", fontSize = 12.sp) }

                        OutlinedButton(
                            onClick = { ttsController.stop() },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4F46E5))
                        ) { Text("Stop", fontSize = 12.sp) }
                    }

                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

                    if (whiteboardData != null) {
                        val timeline = latestTurn?.revealTimeline ?: emptyList()

                        whiteboardData?.explanationCard?.let { card ->
                            val cardTimeline = timeline.find { it.unitType == "text_card" }
                            val isRevealed = cardTimeline == null || cardTimeline.triggerCharIndex <= currentCharIndex
                            AnimatedVisibility(visible = isRevealed, enter = fadeIn()) {
                                HandwritingExplanationSection(card = card, messageFontSp = settingsState.messageFontSp)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        whiteboardData?.flowchartSteps?.let { steps ->
                            if (steps.isNotEmpty()) {
                                FlowchartSection(steps = steps, timeline = timeline, currentCharIndex = currentCharIndex, messageFontSp = settingsState.messageFontSp)
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                if (errorMessage != null) {
                    ErrorBanner(message = errorMessage!!)
                }

                val lastUserMsg = chatHistory.lastOrNull { it.role == "user" }?.content
                if (!lastUserMsg.isNullOrEmpty()) {
                    val uiScale = when (settingsState.messageFontSp) {
                        24f -> 0.8f
                        32f -> 1.2f
                        else -> 1.0f
                    }
                    Text(
                        text = "You: $lastUserMsg",
                        color = Color.DarkGray,
                        fontSize = (14 * uiScale).sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                }

                if (inputMode == "voice") {
                    Column(modifier = Modifier.imePadding()) {
                        WhiteboardVoiceInputBar(
                            isKannada = false,
                            isListening = sttState.isListening,
                            isSpeaking = ttsState.isSpeaking,
                            isThinking = isLoading,
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
                            messageFontSp = settingsState.messageFontSp
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color.White).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            placeholder = { Text("Ask a question...", color = Color.DarkGray) },
                            shape = RoundedCornerShape(24.dp),
                            textStyle = LocalTextStyle.current.copy(color = Color.Black),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                cursorColor = Color(0xFF4F46E5)
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        if (inputText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    allowAutoScroll = true
                                    ttsController.stop()
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                },
                                enabled = !isLoading
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = if(!isLoading) Color(0xFF4F46E5) else Color.LightGray
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    focusTextField = false
                                    inputMode = "voice"
                                    beginListening()
                                },
                                enabled = !isLoading
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Speak",
                                    tint = if(!isLoading) Color(0xFF4F46E5) else Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- UI COMPONENTS ----------------

@Composable
fun ErrorBanner(message: String) {
    val displayMessage = if (message.contains("{") || message.contains("500")) {
        "The tutor is taking a break. Please try again."
    } else {
        message
    }

    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFFFFE4E6)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = Color(0xFFE11D48), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = displayMessage, color = Color(0xFFE11D48), fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SvgRenderer(svgString: String, timeline: List<WbRevealTimelineUnit>, currentCharIndex: Int) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isWebViewLoaded by remember { mutableStateOf(false) }
    var lastLoadedSvg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentCharIndex, timeline, isWebViewLoaded, webViewInstance) {
        if (isWebViewLoaded && webViewInstance != null) {
            val jsBuilder = StringBuilder()
            timeline.filter { it.unitType == "scene_part" && it.partId != null }.forEach { unit ->
                val opacity = if (currentCharIndex >= unit.triggerCharIndex) "1" else "0"
                jsBuilder.append("var el = document.getElementById('${unit.partId}'); if(el) el.style.opacity = '$opacity';\n")
            }
            webViewInstance?.evaluateJavascript(jsBuilder.toString(), null)
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        isWebViewLoaded = true
                    }
                }
                webViewInstance = this
            }
        },
        update = { webView ->
            if (lastLoadedSvg != svgString) {
                lastLoadedSvg = svgString
                isWebViewLoaded = false

                val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                        <style>
                            body { margin: 0; padding: 0; display: flex; justify-content: center; align-items: center; }
                            svg { width: 100%; height: auto; max-height: 100%; display: block; }
                        </style>
                    </head>
                    <body>
                        $svgString
                    </body>
                    </html>
                """.trimIndent()
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun FlowchartSection(steps: List<FlowchartStep>, timeline: List<WbRevealTimelineUnit>, currentCharIndex: Int, messageFontSp: Float = 28f) {
    val scale = when (messageFontSp) {
        24f -> 0.8f
        32f -> 1.2f
        else -> 1.0f
    }

    Card(
        shape = RoundedCornerShape(12.dp), border = BorderStroke(1.5.dp, Color(0xFFFCD34D)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF5)), modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            steps.forEachIndexed { index, step ->
                val stepTimeline = timeline.find { it.unitType == "process_step" && it.unitIndex == index }
                val isRevealed = stepTimeline == null || stepTimeline.triggerCharIndex <= currentCharIndex

                AnimatedVisibility(visible = isRevealed, enter = fadeIn()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (index > 0) {
                            Text("↓", fontSize = (20 * scale).sp, color = Color(0xFF4F46E5), fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth(0.9f).clip(RoundedCornerShape(12.dp)).background(Color(0xFFEEF2FF))
                                .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(12.dp)).padding((12 * scale).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(step.text, fontSize = (14 * scale).sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E1B4B), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HandwritingExplanationSection(card: ExplanationCard, messageFontSp: Float = 28f) {
    val scale = when (messageFontSp) {
        24f -> 0.8f
        32f -> 1.2f
        else -> 1.0f
    }

    Card(
        shape = RoundedCornerShape(12.dp), border = BorderStroke(1.5.dp, Color(0xFFFCD34D)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF7)), modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(card.title, fontSize = (12 * scale).sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, color = Color(0xFF4F46E5), letterSpacing = (1 * scale).sp)
            Spacer(modifier = Modifier.height((6 * scale).dp))
            Text(card.text, fontSize = (20 * scale).sp, fontFamily = FontFamily.Cursive, color = Color(0xFF1F2937), lineHeight = (28 * scale).sp)
        }
    }
}