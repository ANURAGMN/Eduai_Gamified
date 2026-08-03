package com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncert7.aitutorandlab.ui.theme.AccentBlue
import com.ncert7.aitutorandlab.ui.theme.BrandPrimary
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.ui.theme.TextSecondary
import com.ncert7.aitutorandlab.ui.theme.White

/**
 * Inline voice dock shown at the bottom of the chat screen when voice is the active
 * input mode. Tutor avatar and conversation stay visible above; this is just the input
 * surface — a status line, a live waveform, and the mic orb (docked low), plus a "type"
 * chip that hands control back to the keyboard. The caller owns the STT/TTS controllers
 * and the hands-free loop and passes the current state in.
 */
@Composable
fun VoiceInputBar(
    isKannada: Boolean,
    isListening: Boolean,
    isSpeaking: Boolean,
    isThinking: Boolean,
    transcript: String,
    statusMessage: String,
    amplitude: Float,
    onMicTap: () -> Unit,
    onStopListening: () -> Unit,
    onSwitchToType: () -> Unit,
    suggestions: List<String> = emptyList(),
    onSuggestionClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val statusLine = when {
        isListening -> statusMessage.ifBlank { if (isKannada) "ಕೇಳುತ್ತಿದೆ…" else "Listening…" }
        isSpeaking -> if (isKannada) "ಶಿಕ್ಷಕರು ಮಾತನಾಡುತ್ತಿದ್ದಾರೆ" else "Teacher is speaking"
        isThinking -> if (isKannada) "ಶಿಕ್ಷಕರು ಯೋಚಿಸುತ್ತಿದ್ದಾರೆ…" else "Teacher is thinking…"
        else -> if (isKannada) "ಮಾತನಾಡಲು ಟ್ಯಾಪ್ ಮಾಡಿ" else "Tap to speak"
    }

    // Pulse the orb while listening or thinking so it never looks frozen.
    val orbAnimating = isListening || (isThinking && !isSpeaking)
    val pulse = rememberInfiniteTransition(label = "orbPulse")
    val orbScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (orbAnimating) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "orbScale",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(White)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Suggested questions — tappable even in voice mode, so they stay discoverable.
        if (!isListening && suggestions.isNotEmpty()) {
            AutoSuggestionChips(
                suggestions = suggestions,
                visible = true,
                onSuggestionClick = onSuggestionClick,
            )
            Spacer(Modifier.height(8.dp))
        }

        // Live waveform while listening; reserve the space otherwise so the dock height
        // doesn't jump.
        if (isListening) {
            VoiceWaveAnimation(
                amplitude = amplitude,
                isListening = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp),
            )
        } else {
            Spacer(Modifier.height(22.dp))
        }

        Spacer(Modifier.height(8.dp))

        // Live transcript of what the learner is saying, so they can see it captured.
        if (transcript.isNotBlank()) {
            val scroll = rememberScrollState()
            LaunchedEffect(transcript) { scroll.animateScrollTo(scroll.maxValue) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 96.dp)
                    .verticalScroll(scroll),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = transcript,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Text(
            text = statusLine,
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(16.dp))

        // Orb centered and docked low; "Type" chip sits on the left.
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, TextSecondary.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                    .clickable { onSwitchToType() }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = if (isKannada) "ಟೈಪ್ ಮಾಡಿ" else "Type",
                    color = TextSecondary,
                    fontSize = 13.sp,
                )
            }

            val orbColor = when {
                isListening -> BrandPrimary
                isSpeaking || isThinking -> AccentBlue.copy(alpha = 0.35f)
                else -> BrandPrimary.copy(alpha = 0.08f)
            }
            val orbTappable = !isSpeaking && !isThinking
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .scale(if (orbAnimating) orbScale else 1f)
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(orbColor)
                    .border(
                        width = if (isListening) 0.dp else 1.5.dp,
                        color = if (isListening) Color.Transparent else BrandPrimary,
                        shape = CircleShape,
                    )
                    .then(
                        if (orbTappable) Modifier.clickable {
                            if (isListening) onStopListening() else onMicTap()
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when {
                        isSpeaking -> Icons.AutoMirrored.Filled.VolumeUp
                        isThinking -> Icons.Default.MoreHoriz
                        else -> Icons.Default.Mic
                    },
                    contentDescription = when {
                        isListening -> "Stop"
                        isThinking -> "Thinking"
                        else -> "Speak"
                    },
                    tint = if (isListening) White else BrandPrimary,
                    modifier = Modifier.size(34.dp),
                )
            }
        }
    }
}
