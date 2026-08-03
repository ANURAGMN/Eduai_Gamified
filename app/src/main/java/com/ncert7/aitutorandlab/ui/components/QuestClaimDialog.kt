package com.ncert7.aitutorandlab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions

@Composable
fun QuestClaimDialog(
    title: String,
    message: String,
    gemsReward: Int,
    adReady: Boolean,
    onWatchAd: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dimens = LocalDimensions.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .background(
                        MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(dimens.cornerRadiusSmall),
                    )
                    .padding(dimens.spaceMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(dimens.spaceSmall))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(dimens.spaceMedium))
            Button(
                onClick = onWatchAd,
                enabled = adReady,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Watch short video · +$gemsReward gems")
            }
            if (!adReady) {
                Spacer(modifier = Modifier.height(dimens.spaceSmall))
                Text(
                    text = "Loading ad… try again in a moment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(dimens.spaceSmall))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Not now")
            }
        }
    }
}
