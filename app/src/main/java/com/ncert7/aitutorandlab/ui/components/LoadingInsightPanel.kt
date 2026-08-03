package com.ncert7.aitutorandlab.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.ui.components.LoadingInsightPanel
import com.ncert7.aitutorandlab.ui.components.localizedAuthor
import com.ncert7.aitutorandlab.ui.components.localizedText
import com.ncert7.aitutorandlab.ui.theme.BackgroundPrimary
import com.ncert7.aitutorandlab.ui.theme.BrandPrimary
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.ui.theme.TextSecondary
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import kotlinx.coroutines.delay

@Composable
fun LoadingInsightPanel(
    statusText: String,
    modifier: Modifier = Modifier,
    languageCode: String = getCurrentLanguageCode(),
    centered: Boolean = false,
    showQuote: Boolean = true,
    rotateThinking: Boolean = false,
) {
    val dimens = LocalDimensions.current
    // Rotate the quote (and the thinking line, when enabled) every 3 seconds so the wait
    // feels like it's progressing rather than frozen.
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            tick++
        }
    }
    val quote = remember(tick, languageCode) { LoadingQuotes.random(languageCode) }
    val displayStatus = if (rotateThinking) {
        val msgs = thinkingMessages(languageCode)
        msgs[tick % msgs.size]
    } else {
        statusText
    }

    val horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
    val textAlign = if (centered) TextAlign.Center else TextAlign.Start

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlignment,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (centered) Arrangement.Center else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(dimens.iconLarge),
                strokeWidth = dimens.inputBorderWidth,
                color = BrandPrimary,
            )
            Spacer(modifier = Modifier.width(dimens.spaceMedium))
            Crossfade(targetState = displayStatus, label = "thinkingStatus") { msg ->
                Text(
                    text = msg,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = textAlign,
                )
            }
        }

        if (showQuote) {
            Spacer(modifier = Modifier.height(dimens.spaceMedium))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(dimens.cornerRadiusMedium),
                colors = CardDefaults.cardColors(containerColor = BackgroundPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = dimens.cardElevation / 2),
            ) {
                Column(
                    modifier = Modifier.padding(dimens.cardPadding),
                    horizontalAlignment = horizontalAlignment,
                ) {
                    Text(
                        text = stringResource(R.string.loading_insight_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        textAlign = textAlign,
                    )
                    Spacer(modifier = Modifier.height(dimens.spaceExtraSmall))
                    Text(
                        text = "${quote.emoji}  \"${quote.localizedText(languageCode)}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontStyle = FontStyle.Italic,
                        textAlign = textAlign,
                    )
                    Spacer(modifier = Modifier.height(dimens.spaceExtraSmall))
                    Text(
                        text = "— ${quote.localizedAuthor(languageCode)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = BrandPrimary,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = textAlign,
                    )
                }
            }
        }
    }
}

/** Encouraging "thinking" lines that cycle while the tutor prepares a reply. */
private fun thinkingMessages(languageCode: String): List<String> =
    if (languageCode.startsWith("kn", ignoreCase = true)) {
        listOf(
            "ಒಂದು ಕ್ಷಣ…",
            "ಉತ್ತರ ಹುಡುಕುತ್ತಿದ್ದೇನೆ…",
            "ಒಳ್ಳೆಯ ಪ್ರಶ್ನೆ! ಯೋಚಿಸುತ್ತಿದ್ದೇನೆ…",
            "ಚೆನ್ನಾಗಿ ವಿವರಿಸಲು ಸಿದ್ಧಪಡಿಸುತ್ತಿದ್ದೇನೆ…",
        )
    } else {
        listOf(
            "One moment…",
            "Looking that up…",
            "Good question — thinking…",
            "Putting together a clear answer…",
        )
    }
