package com.ncert7.aitutorandlab.ui.screens.whiteboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.ui.components.DropDownMenu
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.InputModeChip
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatBotSettingsState
import com.ncert7.aitutorandlab.ui.theme.BrandPrimary
import com.ncert7.aitutorandlab.ui.theme.IconPrimary
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.ui.theme.White

@Composable
fun WhiteboardSettings(
    expanded: Boolean,
    onDismiss: () -> Unit,
    state: ChatBotSettingsState,
    onVoiceChange: (String) -> Unit,
    onConceptChange: (String) -> Unit,
    onSpeedChange: (String) -> Unit,
    handsFreeMode: Boolean,
    onHandsFreeChange: (Boolean) -> Unit,
    voiceFirst: Boolean,
    onInputModeChange: (Boolean) -> Unit,
    onFontSizeChange: (Float) -> Unit
) {
    val dimens = LocalDimensions.current

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

            // Hands-free voice toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.spaceSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Hands-free voice",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = handsFreeMode,
                    onCheckedChange = onHandsFreeChange
                )
            }

            Spacer(Modifier.height(dimens.spaceMedium))

            // Default input mode
            Text(
                text = "Default input",
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(dimens.spaceSmall))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
            ) {
                InputModeChip(
                    label = "Voice first",
                    selected = voiceFirst,
                    onClick = { onInputModeChange(true) },
                    modifier = Modifier.weight(1f)
                )
                InputModeChip(
                    label = "Text first",
                    selected = !voiceFirst,
                    onClick = { onInputModeChange(false) },
                    modifier = Modifier.weight(1f)
                )
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
                options = state.voiceOptions,
                selectedValue = state.displayedVoiceName,
                onValueSelected = onVoiceChange
            )

            Spacer(Modifier.height(dimens.spaceMedium))

            // Concept
            if (state.isLoadingConcepts) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimens.spaceSmall),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(dimens.iconMedium),
                        color = BrandPrimary,
                        strokeWidth = dimens.inputBorderWidth
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.select_concepts),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(dimens.spaceSmall))

                val selectedDisplayConcept = if (state.selectedConcept != null) {
                    val index = state.availableConcepts.indexOf(state.selectedConcept)
                    if (index >= 0 && index < state.displayConcepts.size) state.displayConcepts[index] else state.selectedConcept
                } else null

                DropDownMenu(
                    label = stringResource(R.string.select_concepts),
                    options = state.displayConcepts.ifEmpty { state.availableConcepts },
                    selectedValue = selectedDisplayConcept ?: stringResource(R.string.tap_to_choose_topic),
                    onValueSelected = { displayedConcept ->
                        val originalConcept = if (state.displayConcepts.isNotEmpty()) {
                            val index = state.displayConcepts.indexOf(displayedConcept)
                            if (index >= 0 && index < state.availableConcepts.size) state.availableConcepts[index] else displayedConcept
                        } else displayedConcept
                        onConceptChange(originalConcept)
                    }
                )
            }

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
                selectedValue = state.selectedSpeed,
                onValueSelected = onSpeedChange
            )

            Spacer(modifier = Modifier.height(dimens.spaceMedium))

            // Custom 3-Tier Text Size
            Text(
                text = "Text size",
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(dimens.spaceSmall))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InputModeChip(
                    label = "S",
                    selected = state.messageFontSp == 24f,
                    onClick = { onFontSizeChange(24f) },
                    modifier = Modifier.weight(1f)
                )
                InputModeChip(
                    label = "M",
                    selected = state.messageFontSp == 28f,
                    onClick = { onFontSizeChange(28f) },
                    modifier = Modifier.weight(1f)
                )
                InputModeChip(
                    label = "L",
                    selected = state.messageFontSp == 32f,
                    onClick = { onFontSizeChange(32f) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}