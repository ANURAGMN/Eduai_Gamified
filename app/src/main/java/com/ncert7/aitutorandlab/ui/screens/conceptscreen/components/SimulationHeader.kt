package com.ncert7.aitutorandlab.ui.screens.conceptscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

        if (showVoiceToggle) {
            SimulationVoiceToggle(
                voiceEnabled = voiceEnabled,
                onVoiceEnabledChange = onVoiceEnabledChange,
                languageCode = languageCode,
            )
        }
    }
}
