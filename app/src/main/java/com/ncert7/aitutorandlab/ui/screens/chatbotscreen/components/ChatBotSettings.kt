package com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import com.ncert7.aitutorandlab.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ncert7.aitutorandlab.ui.components.DropDownMenu
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatBotSettingsState
import com.ncert7.aitutorandlab.ui.theme.BrandPrimary
import com.ncert7.aitutorandlab.ui.theme.IconPrimary
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.ui.theme.White

@Composable
fun ChatBotSettings(
    expanded: Boolean,
    onDismiss: () -> Unit,
    state: ChatBotSettingsState,
    onAvatarChange: (String) -> Unit,
    onVoiceChange: (String) -> Unit,
    onConceptChange: (String) -> Unit,
    onLevelChange: (String) -> Unit,
    onSpeedChange: (String) -> Unit,
    isRevisionMode: Boolean = false,
    useNativeTutorAvatar: Boolean = false,
    handsFreeMode: Boolean = true,
    onHandsFreeChange: (Boolean) -> Unit = {},
    handsFreeLabel: String = "Hands-free voice",
    showInputModeSetting: Boolean = false,
    voiceFirst: Boolean = true,
    onInputModeChange: (Boolean) -> Unit = {},
    defaultInputLabel: String = "Default input",
    voiceFirstLabel: String = "Voice first",
    textFirstLabel: String = "Text first",
    onFontSizeChange: (Float) -> Unit = {},
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

            // Hands-free voice toggle — when on, the mic auto-opens after the tutor speaks.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.spaceSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = handsFreeLabel,
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

            // Default input mode — which surface the chat opens in.
            if (showInputModeSetting) {
                Text(
                    text = defaultInputLabel,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(dimens.spaceSmall))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
                ) {
                    InputModeChip(
                        label = voiceFirstLabel,
                        selected = voiceFirst,
                        onClick = { onInputModeChange(true) },
                        modifier = Modifier.weight(1f)
                    )
                    InputModeChip(
                        label = textFirstLabel,
                        selected = !voiceFirst,
                        onClick = { onInputModeChange(false) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(dimens.spaceMedium))
            }

            if (!useNativeTutorAvatar) {
            // Avatar (legacy WebView boy/girl — hidden when native tutor is enabled)
            Text(
                text = stringResource(R.string.select_avatar),
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(dimens.spaceSmall))

            DropDownMenu(
                label = stringResource(R.string.avatar),
                options = listOf(
                    stringResource(R.string.disable),
                    stringResource(R.string.boy),
                    stringResource(R.string.girl)
                ),
                selectedValue = state.selectedAvatarDisplayName,
                onValueSelected = onAvatarChange
            )

            Spacer(Modifier.height(dimens.spaceMedium))
            }

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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(dimens.iconMedium),
                            color = BrandPrimary,
                            strokeWidth = dimens.inputBorderWidth
                        )
                        Spacer(Modifier.height(dimens.spaceSmall))
                        Text(
                            text = stringResource(R.string.loading_topics),
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                Text(
                    text = if (isRevisionMode) stringResource(R.string.select_chapter) else stringResource(R.string.select_concepts),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(dimens.spaceSmall))

                // Map selected concept to display name
                val selectedDisplayConcept = if (state.selectedConcept != null) {
                    val index = state.availableConcepts.indexOf(state.selectedConcept)
                    if (index >= 0 && index < state.displayConcepts.size) {
                        state.displayConcepts[index]
                    } else {
                        state.selectedConcept
                    }
                } else {
                    null
                }

                DropDownMenu(
                    label = if (isRevisionMode) stringResource(R.string.chapter) else stringResource(R.string.select_concepts),
                    options = state.displayConcepts.ifEmpty { state.availableConcepts },
                    selectedValue = selectedDisplayConcept ?: stringResource(R.string.tap_to_choose_topic),
                    onValueSelected = { displayedConcept ->
                        // Map displayed concept back to original concept
                        val originalConcept = if (state.displayConcepts.isNotEmpty()) {
                            val index = state.displayConcepts.indexOf(displayedConcept)
                            if (index >= 0 && index < state.availableConcepts.size) {
                                state.availableConcepts[index]
                            } else {
                                displayedConcept
                            }
                        } else {
                            displayedConcept
                        }
                        onConceptChange(originalConcept)
                    }
                )
            }

            Spacer(Modifier.height(dimens.spaceMedium))

            // Student Level
            Text(
                text = stringResource(R.string.select_student_level),
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(dimens.spaceSmall))
            DropDownMenu(
                label = stringResource(R.string.student_level),
                options = listOf(
                    stringResource(R.string.level_low),
                    stringResource(R.string.level_medium),
                    stringResource(R.string.level_advanced)
                ),
                selectedValue = when (state.selectedStudentLevel) {
                    "low" -> stringResource(R.string.level_low)
                    "medium" -> stringResource(R.string.level_medium)
                    "advanced" -> stringResource(R.string.level_advanced)
                    else -> stringResource(R.string.level_medium)
                },
                onValueSelected = { displayName ->
                    val code = when (displayName) {
                        "low" -> "low"
                        "medium" -> "medium"
                        "advanced" -> "advanced"
                        else -> "medium"
                    }
                    onLevelChange(code)
                }
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
                selectedValue = state.selectedSpeed,
                onValueSelected = onSpeedChange
            )

            Spacer(modifier = Modifier.height(dimens.spaceMedium))

            ChatFontSizeSettingSection(
                selectedSp = state.messageFontSp,
                onFontSizeChange = onFontSizeChange,
            )
        }
    }
}

@Composable
fun ChatFontSizeSettingSection(
    selectedSp: Float,
    onFontSizeChange: (Float) -> Unit,
) {
    val dimens = LocalDimensions.current
    val nearest = ChatMessageFontSize.nearestPreset(selectedSp)
    Text(
        text = stringResource(R.string.select_chat_font_size),
        color = TextPrimary,
        style = MaterialTheme.typography.titleSmall
    )
    Spacer(modifier = Modifier.height(dimens.spaceSmall))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ChatMessageFontSize.PRESETS.forEach { (label, sp) ->
            InputModeChip(
                label = label,
                selected = nearest == sp,
                onClick = { onFontSizeChange(sp) },
                modifier = Modifier.weight(1f),
                compact = true,
            )
        }
    }
}

@Composable
internal fun InputModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) BrandPrimary.copy(alpha = 0.12f) else White)
            .border(
                width = if (selected) 1.5.dp else 0.5.dp,
                color = if (selected) BrandPrimary else BrandPrimary.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp),
            )
            .clickable { onClick() }
            .padding(vertical = if (compact) 6.dp else 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) BrandPrimary else TextPrimary,
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall,
        )
    }
}