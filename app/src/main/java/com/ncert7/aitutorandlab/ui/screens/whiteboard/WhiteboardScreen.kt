package com.ncert7.aitutorandlab.ui.screens.whiteboard

import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.data.remote.WbRevealTimelineUnit
import com.ncert7.aitutorandlab.ui.components.DropDownMenu
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.ChatHeaderIcons
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatBotSettingsState
import com.ncert7.aitutorandlab.ui.theme.BrandPrimary
import com.ncert7.aitutorandlab.ui.theme.IconPrimary
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.ui.theme.White
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

            if (isLoading && concepts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp)) {
                    items(concepts) { concept ->
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
                                Text("Chapter: ${concept.chapter} | Visuals: ${if(concept.hasScene) "Yes" else "No"}", color = Color.DarkGray, fontSize = 11.sp)
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
    ttsController: TextToSpeech = hiltViewModel()
) {
    val context = LocalContext.current
    val chatHistory by viewModel.chatHistory.collectAsState()
    val whiteboardData by viewModel.whiteboardData.collectAsState()
    val latestTurn by viewModel.latestTurn.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val title by viewModel.conceptTitle.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var inputText by remember { mutableStateOf("") }

    var showSettingsMenu by remember { mutableStateOf(false) }
    var settingsState by remember { mutableStateOf(ChatBotSettingsState()) }
    val ttsState by ttsController.state.collectAsState()

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

    LaunchedEffect(Unit) {
        ttsController.initialize(context)
    }

    LaunchedEffect(latestTurn) {
        latestTurn?.let { turn ->
            allowAutoScroll = true
            ttsController.speak(turn.message)
        }
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
                        voiceOptions = voiceOptions,
                        selectedVoiceName = displayedVoiceName,
                        onVoiceChange = { selectedDisplayName ->
                            ttsState.availableVoices.find { ttsController.formatVoiceName(it) == selectedDisplayName }?.let { voice ->
                                ttsController.setVoice(voice)
                                if (ttsState.isSpeaking) {
                                    ttsController.stop()
                                    ttsController.speak(latestMessage)
                                }
                            }
                        },
                        selectedSpeed = settingsState.selectedSpeed,
                        onSpeedChange = { label ->
                            settingsState = settingsState.copy(selectedSpeed = label)
                            val speed = when (label) { "0.75x" -> 0.75f; "1.25x" -> 1.25f; "1.5x" -> 1.5f; else -> 1.0f }
                            ttsController.setSpeechRate(speed)
                        }
                    )
                }
            )

            // ==========================================
            // VISUAL WHITEBOARD 1: THE SVG RENDERER
            // ==========================================
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

            // ==========================================
            // VISUAL WHITEBOARD 2: FLOWCHART & TEXT
            // ==========================================
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
                                HandwritingExplanationSection(card = card)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        whiteboardData?.flowchartSteps?.let { steps ->
                            if (steps.isNotEmpty()) {
                                FlowchartSection(steps = steps, timeline = timeline, currentCharIndex = currentCharIndex)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // USER INPUT SECTION
            // ==========================================
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                if (errorMessage != null) {
                    ErrorBanner(message = errorMessage!!)
                }

                val lastUserMsg = chatHistory.lastOrNull { it.role == "user" }?.content
                if (!lastUserMsg.isNullOrEmpty()) {
                    Text(
                        text = "You: $lastUserMsg",
                        color = Color.DarkGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.White).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
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
                    IconButton(
                        onClick = {
                            allowAutoScroll = true
                            ttsController.stop()
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        },
                        enabled = !isLoading && inputText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = if(!isLoading && inputText.isNotBlank()) Color(0xFF4F46E5) else Color.LightGray)
                    }
                }
            }
        }
    }
}

// ---------------- UI COMPONENTS ----------------

@Composable
fun WhiteboardSettings(
    expanded: Boolean,
    onDismiss: () -> Unit,
    voiceOptions: List<String>,
    selectedVoiceName: String,
    onVoiceChange: (String) -> Unit,
    selectedSpeed: String,
    onSpeedChange: (String) -> Unit
) {
    val dimens = LocalDimensions.current

    // FIX: Using the standard Compose DropdownMenu
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .background(White)
            .border(dimens.inputBorderWidth, BrandPrimary)
    ) {
        Column(
            modifier = Modifier
                .padding(dimens.cardPadding)
                .widthIn(max = dimens.dropdownMaxWidth)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimens.spaceSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.settings),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                )

                IconButton(onClick = onDismiss, modifier = Modifier.size(dimens.iconLarge)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close_settings),
                        tint = IconPrimary
                    )
                }
            }

            Spacer(Modifier.height(dimens.spaceMedium))

            // Voice
            Text(
                text = stringResource(R.string.select_voice),
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(dimens.spaceSmall))
            DropDownMenu(
                label = stringResource(R.string.voice),
                options = voiceOptions,
                selectedValue = selectedVoiceName,
                onValueSelected = onVoiceChange
            )

            Spacer(Modifier.height(dimens.spaceMedium))

            // Speed
            Text(
                text = stringResource(R.string.select_speed),
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(dimens.spaceSmall))
            DropDownMenu(
                label = stringResource(R.string.speed),
                options = listOf("0.75x", "1.0x", "1.25x", "1.5x"),
                selectedValue = selectedSpeed,
                onValueSelected = onSpeedChange
            )
        }
    }
}

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
            if (webView.url == null) {
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
fun FlowchartSection(steps: List<FlowchartStep>, timeline: List<WbRevealTimelineUnit>, currentCharIndex: Int) {
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
                            Text("↓", fontSize = 20.sp, color = Color(0xFF4F46E5), fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth(0.9f).clip(RoundedCornerShape(12.dp)).background(Color(0xFFEEF2FF))
                                .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(12.dp)).padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(step.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E1B4B), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HandwritingExplanationSection(card: ExplanationCard) {
    Card(
        shape = RoundedCornerShape(12.dp), border = BorderStroke(1.5.dp, Color(0xFFFCD34D)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF7)), modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(card.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, color = Color(0xFF4F46E5), letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(card.text, fontSize = 20.sp, fontFamily = FontFamily.Cursive, color = Color(0xFF1F2937), lineHeight = 28.sp)
        }
    }
}