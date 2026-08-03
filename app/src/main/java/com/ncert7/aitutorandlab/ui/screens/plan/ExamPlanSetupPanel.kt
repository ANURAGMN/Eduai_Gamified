package com.ncert7.aitutorandlab.ui.screens.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import com.anurag.eduai.uikit.components.ScrollableChipRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.ncert7.aitutorandlab.data.local.entities.ChapterEntity
import com.ncert7.aitutorandlab.domain.examplan.PlanFeasibilitySeverity
import com.ncert7.aitutorandlab.domain.examplan.DefaultExamPlan
import com.ncert7.aitutorandlab.ui.screens.plan.viewmodel.PlanOverviewViewModel
import com.ncert7.aitutorandlab.utils.ExamPlanCopy
import java.time.LocalDate
import java.time.ZoneId

private val SETUP_ZONE = ZoneId.of("Asia/Kolkata")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamPlanSetupPanel(
    viewModel: PlanOverviewViewModel,
    languageCode: String,
    modifier: Modifier = Modifier,
) {
    val setup by viewModel.setup.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val colors = EduAiTheme.colors
    var showDatePicker by remember { mutableStateOf(false) }
    val selectedExamEpoch =
        setup.examEpochDay.takeIf { it > 0L }
            ?: DefaultExamPlan.defaultExamDate().toEpochDay()
    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis =
                LocalDate.ofEpochDay(selectedExamEpoch)
                    .atStartOfDay(SETUP_ZONE)
                    .toInstant()
                    .toEpochMilli(),
        )
    val chipColors =
        FilterChipDefaults.filterChipColors(
            containerColor = colors.surface2,
            labelColor = colors.textSecondary,
            selectedContainerColor = colors.accent,
            selectedLabelColor = Color.White,
        )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = ExamPlanCopy.buildPlanTitle(languageCode),
            color = colors.text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = ExamPlanCopy.buildPlanSubtitle(languageCode),
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodySmall,
        )

        Text(ExamPlanCopy.sectionExamType(languageCode), color = colors.textMuted, style = MaterialTheme.typography.labelMedium)
        ScrollableChipRow(hintText = ExamPlanCopy.swipeExamTypes(languageCode)) {
            ExamPlanCopy.EXAM_TYPE_KEYS.forEach { type ->
                FilterChip(
                    selected = setup.examType == type,
                    onClick = { viewModel.selectExamType(type) },
                    label = { Text(ExamPlanCopy.examTypeLabel(languageCode, type)) },
                    colors = chipColors,
                )
            }
        }

        Text(ExamPlanCopy.sectionSubject(languageCode), color = colors.textMuted, style = MaterialTheme.typography.labelMedium)
        if (subjects.isEmpty()) {
            Text(
                text = ExamPlanCopy.loadingSubjects(languageCode),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            ScrollableChipRow(hintText = ExamPlanCopy.swipeSubjects(languageCode)) {
                subjects.forEach { subject ->
                    FilterChip(
                        selected = setup.subjectId == subject.subjectId,
                        onClick = { viewModel.selectSubject(subject.subjectId) },
                        label = { Text(viewModel.subjectLabel(subject)) },
                        colors = chipColors,
                    )
                }
            }
        }

        if (chapters.isNotEmpty()) {
            Text(ExamPlanCopy.sectionChapters(languageCode), color = colors.textMuted, style = MaterialTheme.typography.labelMedium)
            chapters.forEach { chapter ->
                ChapterToggleRow(
                    chapter = chapter,
                    label = viewModel.chapterLabel(chapter),
                    checked = chapter.chapterId in setup.selectedChapterIds,
                    onToggle = { viewModel.toggleChapter(chapter.chapterId) },
                    chipColors = chipColors,
                )
            }
        }

        Text(
            text = ExamPlanCopy.dailyBudgetLabel(languageCode, setup.dailyMinutes),
            color = colors.textMuted,
            style = MaterialTheme.typography.labelMedium,
        )
        Slider(
            value = setup.dailyMinutes.toFloat(),
            onValueChange = { viewModel.setDailyMinutes(it.toInt()) },
            valueRange = 15f..90f,
            steps = 14,
            colors =
                SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.borderStrong,
                ),
        )
        if (setup.estimatedDays > 0) {
            Text(
                text = ExamPlanCopy.estimatedPlanLength(languageCode, setup.estimatedDays, setup.totalTrialItems),
                color = colors.accent,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
        }

        Text(ExamPlanCopy.sectionExamDate(languageCode), color = colors.textMuted, style = MaterialTheme.typography.labelMedium)
        Text(
            text = viewModel.examDateLabel(setup),
            color = colors.text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        ScrollableChipRow(hintText = ExamPlanCopy.swipeExamDates(languageCode)) {
            listOf(
                ExamPlanCopy.datePresetOneWeek(languageCode) to 7L,
                ExamPlanCopy.datePresetTwoWeeks(languageCode) to 14L,
                ExamPlanCopy.datePresetThreeWeeks(languageCode) to 21L,
                ExamPlanCopy.datePresetOneMonth(languageCode) to 30L,
            ).forEach { (label, days) ->
                FilterChip(
                    selected =
                        setup.examEpochDay ==
                            LocalDate.now(SETUP_ZONE).plusDays(days).toEpochDay(),
                    onClick = { viewModel.selectExamPreset(days) },
                    label = { Text(label) },
                    colors = chipColors,
                )
            }
        }
        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text(ExamPlanCopy.pickSpecificDate(languageCode))
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val picked =
                                    java.time.Instant.ofEpochMilli(millis)
                                        .atZone(SETUP_ZONE)
                                        .toLocalDate()
                                viewModel.selectExamDate(picked)
                            }
                            showDatePicker = false
                        },
                    ) {
                        Text(ExamPlanCopy.ok(languageCode))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(ExamPlanCopy.cancel(languageCode))
                    }
                },
            ) {
                DatePicker(state = datePickerState)
            }
        }

        setup.feasibilityIssues.filter { it.severity == PlanFeasibilitySeverity.ERROR }
            .forEach { issue ->
                Text(
                    text = issue.message,
                    color = colors.danger,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        setup.feasibilityIssues.filter { it.severity == PlanFeasibilitySeverity.WARNING }
            .forEach { issue ->
                Text(
                    text = issue.message,
                    color = colors.warning,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

        setup.errorMessage?.let { msg ->
            Text(text = msg, color = colors.warning, style = MaterialTheme.typography.bodySmall)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (viewModel.canDismissSetup()) {
                OutlinedButton(onClick = { viewModel.closeSetup() }, modifier = Modifier.weight(1f)) {
                    Text(ExamPlanCopy.cancel(languageCode))
                }
            }
            Button(
                onClick = { viewModel.saveSetup() },
                enabled = !setup.isSaving && setup.canGenerate,
                modifier = Modifier.weight(1f),
            ) {
                if (setup.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp))
                } else {
                    Text(ExamPlanCopy.generatePlan(languageCode))
                }
            }
        }
    }
}

@Composable
private fun ChapterToggleRow(
    chapter: ChapterEntity,
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    chipColors: androidx.compose.material3.SelectableChipColors,
) {
    val colors = EduAiTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(colors.surface2, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = checked,
            onClick = onToggle,
            label = { Text(label, maxLines = 2) },
            colors = chipColors,
        )
    }
}

@Composable
fun PlanSummaryCard(
    examType: String,
    dailyMinutes: Int,
    dayCount: Int,
    chapterCount: Int,
    languageCode: String,
    onCustomize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.surface2, RoundedCornerShape(12.dp))
                .padding(14.dp),
    ) {
        Text(
            text = ExamPlanCopy.summaryLine(languageCode, examType, dailyMinutes),
            color = colors.text,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = ExamPlanCopy.summaryDaysChapters(languageCode, dayCount, chapterCount),
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(onClick = onCustomize, modifier = Modifier.fillMaxWidth()) {
            Text(ExamPlanCopy.editPlan(languageCode))
        }
    }
}
