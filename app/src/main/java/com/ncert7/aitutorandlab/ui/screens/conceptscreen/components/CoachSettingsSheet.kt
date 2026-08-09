package com.ncert7.aitutorandlab.ui.screens.conceptscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ncert7.aitutorandlab.ui.theme.BrandPrimary
import com.ncert7.aitutorandlab.ui.theme.CardBackground
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.ui.theme.TextSecondary
import com.ncert7.aitutorandlab.ui.theme.White

/**
 * Self-contained coach settings sheet (opened from the header gear). Plain params only, so it has no
 * dependency on the chat/math settings state — the sim viewer supplies the values and callbacks.
 *  1. Teaching methodology  2. Voice on/off  3. Voice selection  4. Speed  5. Avatar
 */
@Composable
fun CoachSettingsSheet(
    hintMode: String,
    onHintMode: (String) -> Unit,
    voiceEnabled: Boolean,
    onVoiceEnabled: (Boolean) -> Unit,
    speed: Float,
    onSpeed: (Float) -> Unit,
    voiceOptions: List<String> = emptyList(),
    selectedVoice: String = "",
    onVoiceSelect: (String) -> Unit = {},
    avatarOptions: List<String> = listOf("Boy", "Girl"),
    selectedAvatar: String = "Boy",
    onAvatarSelect: (String) -> Unit = {},
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(CardBackground)
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Coach settings", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp).clickable { onDismiss() },
                )
            }

            SettingSection(title = "Teaching methodology")
            listOf(
                "ask" to "Try first",
                "guided" to "Step-by-step",
                "self" to "Self-explain",
                "ondemand" to "Answer on tap",
            ).forEach { (id, label) ->
                SelectRow(label = label, selected = id == hintMode, onClick = { onHintMode(id) })
            }

            SettingSection(title = "Voice")
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Voice", color = TextPrimary, fontSize = 14.sp)
                Switch(checked = voiceEnabled, onCheckedChange = onVoiceEnabled)
            }

            if (voiceOptions.isNotEmpty()) {
                SettingSection(title = "Voice selection")
                voiceOptions.forEach { name ->
                    SelectRow(label = name, selected = name == selectedVoice, onClick = { onVoiceSelect(name) })
                }
            }

            SettingSection(title = "Speed")
            Text(
                text = "${(kotlin.math.round(speed * 10f) / 10f)}x",
                color = TextSecondary,
                fontSize = 12.sp,
            )
            Slider(
                value = speed,
                onValueChange = onSpeed,
                valueRange = 0.5f..1.5f,
                steps = 9,
            )

            SettingSection(title = "Avatar")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                avatarOptions.forEach { name ->
                    Text(
                        text = name,
                        color = if (name == selectedAvatar) White else BrandPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (name == selectedAvatar) BrandPrimary else BrandPrimary.copy(alpha = 0.12f))
                            .clickable { onAvatarSelect(name) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSection(title: String) {
    Spacer(Modifier.height(14.dp))
    Text(text = title, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun SelectRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) BrandPrimary else TextPrimary,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (selected) 2.dp else 0.5.dp,
                color = if (selected) BrandPrimary else BrandPrimary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}
