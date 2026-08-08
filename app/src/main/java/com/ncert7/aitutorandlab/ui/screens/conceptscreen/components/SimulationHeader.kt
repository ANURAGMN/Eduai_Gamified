package com.ncert7.aitutorandlab.ui.screens.conceptscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.ui.theme.HeaderGradientStart
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.theme.TextOnPrimary

/**
simulation header is a simple row with a back button and title text, 
styled with a gradient background.
 
 @params title: The title to display in the header
 @params onBackClick: Callback function to be invoked when the back button is clicked
 */
@Composable
fun SimulationHeader(
    title: String,
    onBackClick: () -> Unit,
    voiceEnabled: Boolean = true,
    onVoiceEnabledChange: (Boolean) -> Unit = {},
    languageCode: String = "en",
    showVoiceToggle: Boolean = true,
    /** When non-null, shows the v1/v2/v3 coach-style selector for side-by-side comparison. */
    coachMode: SimCoachMode? = null,
    onCoachModeChange: (SimCoachMode) -> Unit = {},
) {
    val dimens = LocalDimensions.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGradientStart)
            .padding(dimens.spaceSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = TextOnPrimary,
                modifier = Modifier.size(dimens.iconMedium)
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextOnPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            modifier = Modifier.weight(1f)
        )

        if (coachMode != null) {
            CoachModeSelector(
                selected = coachMode,
                onSelect = onCoachModeChange,
                modifier = Modifier.padding(end = 6.dp),
            )
        }

        if (showVoiceToggle) {
            SimulationVoiceToggle(
                voiceEnabled = voiceEnabled,
                onVoiceEnabledChange = onVoiceEnabledChange,
                languageCode = languageCode,
            )
        }
    }
}

/** Compact V1 · V2 · V3 segmented control to switch coaching styles for comparison. */
@Composable
private fun CoachModeSelector(
    selected: SimCoachMode,
    onSelect: (SimCoachMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SimCoachMode.values().forEach { mode ->
            val active = mode == selected
            Text(
                text = mode.short,
                color = if (active) HeaderGradientStart else TextOnPrimary,
                fontSize = 11.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (active) Color.White else Color.Transparent)
                    .clickable { onSelect(mode) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}
