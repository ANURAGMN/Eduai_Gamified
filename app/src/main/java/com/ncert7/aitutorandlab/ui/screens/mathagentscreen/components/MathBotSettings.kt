package com.ncert7.aitutorandlab.ui.screens.mathagentscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import com.ncert7.aitutorandlab.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ncert7.aitutorandlab.ui.components.DropDownMenu
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.InputModeChip
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatBotSettingsState
import com.ncert7.aitutorandlab.ui.theme.BrandPrimary
import com.ncert7.aitutorandlab.ui.theme.IconPrimary
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.ui.theme.White

@Composable
fun MathBotSettings(
    expanded: Boolean,
    onDismiss: () -> Unit,
    state: ChatBotSettingsState,
    onAvatarChange: (String) -> Unit,
    onVoiceChange: (String) -> Unit,
    onProblemChange: (String) -> Unit,
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
) {
    val dimens = LocalDimensions.current
    val levelLow = stringResource(R.string.level_low)
    val levelMedium = stringResource(R.string.level_medium)
    val levelAdvanced = stringResource(R.string.level_advanced)

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

            // Problem Selection
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
                            text = "Loading problems...",
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                Text(
                    text = "Select Problem",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(dimens.spaceSmall))

                // Display problems with IDs from API response
                val problemDisplayOptions = state.availableConcepts.mapIndexed { index, problemId ->
                    val displayName = if (index < state.displayConcepts.size) {
                        state.displayConcepts[index]
                    } else {
                        problemId
                    }
                    "$displayName (ID: $problemId)"
                }

                // Map selected problem to display with ID
                val selectedDisplayProblem = if (state.selectedConcept != null) {
                    val index = state.availableConcepts.indexOf(state.selectedConcept)
                    if (index >= 0 && index < state.displayConcepts.size) {
                        "${state.displayConcepts[index]} (ID: ${state.selectedConcept})"
                    } else {
                        "${state.selectedConcept} (ID: ${state.selectedConcept})"
                    }
                } else {
                    "Tap to choose problem"
                }

                DropDownMenu(
                    label = "Problem",
                    options = problemDisplayOptions.ifEmpty { state.availableConcepts },
                    selectedValue = selectedDisplayProblem,
                    onValueSelected = { selectedDisplay ->
                        // Extract problem ID from display string (format: "Title (ID: problemId)")
                        val problemId = if (selectedDisplay.contains("(ID:")) {
                            selectedDisplay.substringAfterLast("(ID: ").substringBefore(")")
                        } else {
                            selectedDisplay
                        }
                        onProblemChange(problemId)
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
                        levelLow -> "low"
                        levelMedium -> "medium"
                        levelAdvanced -> "advanced"
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
        }
    }
}
