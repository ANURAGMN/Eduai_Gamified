package com.anurag.eduai.uikit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.theme.EduAiTheme

/**
 * Two-in-one rating prompt. The student picks 1–5 stars; a high rating routes to the Play Store, a
 * low one opens an inline feedback box that goes to the team instead. The threshold is [HIGH_RATING].
 *
 * Note on Play policy: this dialog is a *custom* sentiment step, and the "high → store" path should
 * deep-link to the store listing, NOT invoke the Play In-App Review API (whose guidelines forbid
 * gating on sentiment). The host wires that.
 *
 * The live Eduapp build currently uses Play In-App Review only ([AppRatingHost] note); this
 * component remains for previews / future reuse and accepts [RatingDialogCopy] for EN/KN.
 */
private const val HIGH_RATING = 4

@Composable
fun EduRatingDialog(
    appName: String = "the app",
    onRateOnPlay: () -> Unit,
    onSubmitFeedback: (stars: Int, message: String) -> Unit,
    onDismiss: () -> Unit,
    copy: RatingDialogCopy = defaultRatingDialogCopy(),
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    var stars by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("") }
    val isLow = stars in 1 until HIGH_RATING

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isLow) copy.titleHelpUs else copy.titleEnjoying(appName),
                color = colors.text,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isLow) copy.bodyFeedback else copy.bodyRate,
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { i ->
                        val filled = i <= stars
                        Icon(
                            imageVector = if (filled) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = copy.starContentDescription(i),
                            tint = if (filled) colors.warning else colors.borderStrong,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .pressScaleClickable(onClick = { stars = i }, pressedScale = 0.85f),
                        )
                    }
                }
                if (isLow) {
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = feedback,
                        onValueChange = { feedback = it },
                        placeholder = { Text(copy.feedbackPlaceholder) },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = stars > 0,
                onClick = {
                    if (stars >= HIGH_RATING) onRateOnPlay() else onSubmitFeedback(stars, feedback)
                },
            ) {
                Text(
                    text =
                        when {
                            stars == 0 -> copy.rateLabel
                            stars >= HIGH_RATING -> copy.rateOnPlayLabel
                            else -> copy.sendFeedbackLabel
                        },
                    color = if (stars > 0) colors.accent else colors.textMuted,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(copy.notNowLabel, color = colors.textMuted)
            }
        },
    )
}
