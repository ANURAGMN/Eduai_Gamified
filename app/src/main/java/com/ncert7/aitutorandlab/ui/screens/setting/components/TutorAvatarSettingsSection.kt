package com.ncert7.aitutorandlab.ui.screens.setting.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anurag.eduai.uikit.avatar.AllAvatarPresets
import com.anurag.eduai.uikit.avatar.SavedTutorAvatar
import com.anurag.eduai.uikit.avatar.TutorConfig
import com.anurag.eduai.uikit.avatar.rememberSavedTutorConfig
import com.ncert7.aitutorandlab.ui.screens.setting.viewmodel.TutorConfigViewModel
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.ui.theme.TextSecondary

private val settingsPresets =
    AllAvatarPresets.filter { it.id in setOf("scholar", "sage", "spark", "nova", "ace") }

@Composable
fun TutorAvatarSettingsSection(
    onOpenStudio: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TutorConfigViewModel = hiltViewModel(),
) {
    val dimens = LocalDimensions.current
    val saved = rememberSavedTutorConfig()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
        ) {
            SavedTutorAvatar(modifier = Modifier.size(72.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your tutor",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                )
                Text(
                    text = "Pick a look for Study, Math, Revision, and Simulation agents.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }

        Spacer(Modifier.height(dimens.spaceMedium))

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
        ) {
            settingsPresets.forEach { preset ->
                val selected = saved.matchesPreset(preset.config)
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.applyPreset(preset) },
                    label = { Text(preset.name) },
                )
            }
        }

        Spacer(Modifier.height(dimens.spaceSmall))

        OutlinedButton(
            onClick = onOpenStudio,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Customize in Avatar Studio")
        }
    }
}

private fun TutorConfig.matchesPreset(other: TutorConfig): Boolean =
    character == other.character &&
        outfit == other.outfit &&
        neck == other.neck &&
        hair == other.hair &&
        hairColor == other.hairColor &&
        glasses == other.glasses &&
        frameColor == other.frameColor &&
        eyeLine == other.eyeLine &&
        cheeks == other.cheeks
