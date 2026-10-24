package com.anurag.eduai.uikit.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.theme.EduAiTheme

/** Stable filter ids — never scrape localized chip labels back to values. */
object HomeSearchFilterIds {
    const val SUBJECT_ALL = "all"
    const val SUBJECT_SCIENCE = "science"
    const val SUBJECT_MATH = "math"

    const val LEARN_ALL = "all"
    const val LEARN_NOTES = "notes"
    const val LEARN_WHITEBOARD = "whiteboard"
    const val LEARN_ASK_DOUBT = "ask_doubt"
}

data class SearchResultItem(
    val conceptId: String,
    val topicName: String,
    val subjectName: String,
    val chapterName: String,
    val subjectId: String,
    val isWhiteboardAvailable: Boolean,
    val isMath: Boolean,
    val problemId: String,
    val chapterId: String,
    val wbConceptId: String? = null,
)

data class SearchFilterOption(
    val id: String,
    val label: String,
)

@Composable
private fun FilterChipMenu(
    categoryLabel: String,
    selectedId: String,
    options: List<SearchFilterOption>,
    onSelectionChange: (String) -> Unit,
    icon: ImageVector,
    enabled: Boolean = true,
) {
    val colors = EduAiTheme.colors
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.id == selectedId }?.label ?: categoryLabel
    val displayText =
        if (selectedId == HomeSearchFilterIds.SUBJECT_ALL || selectedId == HomeSearchFilterIds.LEARN_ALL) {
            categoryLabel
        } else {
            selectedLabel
        }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            border = BorderStroke(1.dp, if (enabled) colors.border else colors.border.copy(alpha = 0.3f)),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (enabled) colors.textSecondary else colors.textMuted,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = displayText,
                color = if (enabled) colors.text else colors.textMuted,
                fontSize = 12.sp,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (enabled) colors.textSecondary else colors.textMuted,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.label, fontSize = 14.sp) },
                    onClick = {
                        onSelectionChange(opt.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun HomeSearchRail(
    query: String,
    onQueryChange: (String) -> Unit,
    subjectFilterId: String,
    onSubjectFilterChange: (String) -> Unit,
    learningOptionId: String,
    onLearningOptionChange: (String) -> Unit,
    subjectOptions: List<SearchFilterOption>,
    learningOptions: List<SearchFilterOption>,
    placeholder: String,
    subjectFilterLabel: String,
    learningFilterLabel: String,
    surpriseMeLabel: String,
    onSearchClick: () -> Unit,
    onSurpriseMeClick: () -> Unit,
    isSearching: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors

    Column(modifier = modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                enabled = !isSearching,
                placeholder = {
                    Text(placeholder, color = colors.textSecondary, fontSize = 14.sp)
                },
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.surface2,
                        unfocusedContainerColor = colors.surface2,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                    ),
                shape = RoundedCornerShape(12.dp),
            )
            Button(
                onClick = onSearchClick,
                enabled = !isSearching,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = placeholder,
                    modifier = Modifier.size(20.dp),
                    tint = colors.onAccent,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChipMenu(
                categoryLabel = subjectFilterLabel,
                selectedId = subjectFilterId,
                options = subjectOptions,
                onSelectionChange = onSubjectFilterChange,
                icon = Icons.Outlined.MenuBook,
                enabled = !isSearching,
            )
            FilterChipMenu(
                categoryLabel = learningFilterLabel,
                selectedId = learningOptionId,
                options = learningOptions,
                onSelectionChange = onLearningOptionChange,
                icon = Icons.Outlined.School,
                enabled = !isSearching,
            )
            OutlinedButton(
                onClick = onSurpriseMeClick,
                enabled = !isSearching,
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                border =
                    BorderStroke(
                        1.dp,
                        if (!isSearching) colors.border else colors.border.copy(alpha = 0.3f),
                    ),
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (!isSearching) colors.textSecondary else colors.textMuted,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    surpriseMeLabel,
                    color = if (!isSearching) colors.text else colors.textMuted,
                    fontSize = 12.sp,
                )
            }
            if (isSearching) {
                Spacer(modifier = Modifier.width(8.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = colors.accent,
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchResultCard(
    result: SearchResultItem,
    learningOptionId: String,
    notesLabel: String,
    whiteboardLabel: String,
    askDoubtLabel: String,
    onNotes: (subjectId: String, chapterId: String) -> Unit,
    onWhiteboard: (wbConceptId: String) -> Unit,
    onAskDoubt: (topicName: String, subjectName: String, chapterName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    val showNotes =
        learningOptionId == HomeSearchFilterIds.LEARN_ALL ||
            learningOptionId == HomeSearchFilterIds.LEARN_NOTES
    val showWhiteboard =
        (learningOptionId == HomeSearchFilterIds.LEARN_ALL ||
            learningOptionId == HomeSearchFilterIds.LEARN_WHITEBOARD) &&
            result.isWhiteboardAvailable
    val showAskDoubt =
        learningOptionId == HomeSearchFilterIds.LEARN_ALL ||
            learningOptionId == HomeSearchFilterIds.LEARN_ASK_DOUBT

    val subtitleParts =
        listOf(result.subjectName, result.chapterName)
            .map { it.trim() }
            .filter { it.isNotEmpty() && !looksLikeRawId(it) }
    val subtitle = subtitleParts.joinToString(" · ")

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface2)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(16.dp),
    ) {
        Text(
            text = result.topicName,
            color = colors.text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                color = colors.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (showNotes && result.subjectId.isNotBlank()) {
                OutlinedButton(
                    onClick = { onNotes(result.subjectId, result.chapterId) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(notesLabel, color = colors.text, fontSize = 12.sp)
                }
            }
            if (showWhiteboard && result.wbConceptId != null) {
                OutlinedButton(
                    onClick = { onWhiteboard(result.wbConceptId) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(whiteboardLabel, color = colors.text, fontSize = 12.sp)
                }
            }
            if (showAskDoubt) {
                OutlinedButton(
                    onClick = {
                        onAskDoubt(result.topicName, result.subjectName, result.chapterName)
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(askDoubtLabel, color = colors.text, fontSize = 12.sp)
                }
            }
        }
    }
}

/** Hide UUID-like / opaque ids from search subtitles. */
private fun looksLikeRawId(value: String): Boolean {
    if (value.length >= 20 && value.count { it == '-' } >= 3) return true
    if (value.matches(Regex("^[0-9a-fA-F-]{16,}$"))) return true
    return false
}
