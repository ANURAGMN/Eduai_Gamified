package com.ncert7.aitutorandlab.ui.screens.conceptscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncert7.aitutorandlab.ui.theme.TextOnPrimary
import com.ncert7.aitutorandlab.ui.theme.White

@Composable
fun SimulationVoiceToggle(
    voiceEnabled: Boolean,
    onVoiceEnabledChange: (Boolean) -> Unit,
    languageCode: String,
    modifier: Modifier = Modifier,
) {
    val kn = languageCode.startsWith("kn", ignoreCase = true)
    val onLabel = if (kn) "ಧ್ವನಿ" else "Voice on"
    val offLabel = if (kn) "ಆಫ್" else "Off"

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(White.copy(alpha = 0.18f))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VoiceOption(
            selected = voiceEnabled,
            label = onLabel,
            onClick = { onVoiceEnabledChange(true) },
        )
        VoiceOption(
            selected = !voiceEnabled,
            label = offLabel,
            onClick = { onVoiceEnabledChange(false) },
        )
    }
}

@Composable
private fun VoiceOption(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(start = 2.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            modifier = Modifier.padding(0.dp),
            colors = RadioButtonDefaults.colors(
                selectedColor = White,
                unselectedColor = TextOnPrimary.copy(alpha = 0.7f),
            ),
        )
        Text(
            text = label,
            color = TextOnPrimary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
