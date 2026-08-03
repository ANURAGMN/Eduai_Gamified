package com.anurag.eduai.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.theme.EduAiTheme

/** A chapter option for [EduChapterPicker]. */
data class EduChapterPickerItem(val id: String, val label: String)

/**
 * Onboarding-style chapter picker: a back link, a bold title, a soft subtitle, a
 * scrollable list of selectable rows (with a "recommended" hint on the first), and a
 * Continue button. Mirrors the onboarding step-2 look so browsing by subject feels the
 * same. Purely presentational — the caller owns selection + navigation.
 */
@Composable
fun EduChapterPicker(
    title: String,
    subtitle: String,
    chapters: List<EduChapterPickerItem>,
    selectedId: String,
    recommendedLabel: String,
    backLabel: String,
    continueLabel: String,
    loadingLabel: String,
    emptyLabel: String = "No chapters available yet.",
    isLoading: Boolean = false,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface1)
            .padding(horizontal = 22.dp)
            .padding(top = 16.dp, bottom = 22.dp),
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onBack() }
                .padding(vertical = 6.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp),
            )
            Text(backLabel, color = colors.accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(10.dp))
        Text(title, color = colors.text, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = colors.textSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (isLoading) {
                Text(loadingLabel, color = colors.textMuted, fontSize = 13.sp)
            } else if (chapters.isEmpty()) {
                Text(emptyLabel, color = colors.textMuted, fontSize = 13.sp)
            }
            chapters.forEachIndexed { i, c ->
                ChapterPickRow(
                    title = c.label,
                    recommended = i == 0,
                    recommendedLabel = recommendedLabel,
                    selected = c.id == selectedId,
                    onClick = { onSelect(c.id) },
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        EduPrimaryButton(
            text = continueLabel,
            onClick = onContinue,
            fillMaxWidth = true,
            enabled = selectedId.isNotEmpty(),
        )
    }
}

@Composable
private fun ChapterPickRow(
    title: String,
    recommended: Boolean,
    recommendedLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = EduAiTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) colors.accentBg else colors.surface2)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.accent else colors.border,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (recommended) Text(recommendedLabel, color = colors.accent, fontSize = 12.sp)
        }
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) colors.accent else colors.surface1)
                .border(if (selected) 0.dp else 1.5.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
