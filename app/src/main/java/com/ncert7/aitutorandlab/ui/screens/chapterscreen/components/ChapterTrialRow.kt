package com.ncert7.aitutorandlab.ui.screens.chapterscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.domain.progress.model.ProgressStatus
import com.ncert7.aitutorandlab.ui.models.ChapterUiModel
import com.ncert7.aitutorandlab.ui.theme.BrandPrimary
import com.ncert7.aitutorandlab.ui.theme.CardBackground
import com.ncert7.aitutorandlab.ui.theme.ColorHint
import com.ncert7.aitutorandlab.ui.theme.CompleteTextColor
import com.ncert7.aitutorandlab.ui.theme.InProgressTextColor
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.ui.theme.TextSecondary

/**
 * Onboarding-style chapter row: a clean, tappable card that opens the chapter's
 * concepts + simulations as a trial. Revision (when present) is a small secondary link
 * so it stays reachable without cluttering the primary tap target.
 */
@Composable
fun ChapterTrialRow(
    chapter: ChapterUiModel,
    onOpen: () -> Unit,
    onRevisionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimensions.current
    val progress = chapter.progressUiModel
    val accent = progressColor(chapter.status)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .border(1.dp, ColorHint, RoundedCornerShape(14.dp))
            .clickable { onOpen() }
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Chapter number badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BrandPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = chapter.orderIndex.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary,
                )
            }

            Spacer(Modifier.size(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = pluralStringResource(
                        R.plurals.concept_count,
                        chapter.totalConcepts,
                        chapter.totalConcepts,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                if (progress != null && chapter.status != ProgressStatus.NOT_STARTED) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimens.progressIndicatorStrokeHeight)
                            .clip(RoundedCornerShape(dimens.cornerRadiusSmall)),
                        trackColor = ColorHint,
                        color = accent,
                    )
                }
            }

            Spacer(Modifier.size(8.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp),
            )
        }

        if (chapter.hasRevision) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onRevisionClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.revision),
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private fun progressColor(status: ProgressStatus) = when (status) {
    ProgressStatus.COMPLETED -> CompleteTextColor
    ProgressStatus.IN_PROGRESS -> InProgressTextColor
    else -> InProgressTextColor
}
