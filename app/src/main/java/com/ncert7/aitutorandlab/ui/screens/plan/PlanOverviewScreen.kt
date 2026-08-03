package com.ncert7.aitutorandlab.ui.screens.plan

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anurag.eduai.uikit.components.EduScreenTopBar
import com.anurag.eduai.uikit.components.PlanDayNode
import com.anurag.eduai.uikit.components.PlanDayStatus
import com.anurag.eduai.uikit.components.PlanDayType
import com.anurag.eduai.uikit.components.pressScaleClickable
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.ncert7.aitutorandlab.utils.ExamPlanCopy
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import com.ncert7.aitutorandlab.service.analytics.EngagementAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.ScreenName
import com.ncert7.aitutorandlab.service.analytics.TrackScreenEvent
import com.ncert7.aitutorandlab.ui.screens.plan.viewmodel.PlanOverviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanOverviewScreen(
    onNavigateBack: () -> Unit = {},
    showBackNavigation: Boolean = true,
    onDayClick: (PlanDayNode) -> Unit = {},
) {
    TrackScreenEvent(screenName = ScreenName.PLAN)

    val viewModel: PlanOverviewViewModel = hiltViewModel()
    val days by viewModel.planDays.collectAsState()
    val activePlan by viewModel.activePlan.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showSetup by viewModel.showSetup.collectAsState()
    val languageCode = getCurrentLanguageCode()

    LaunchedEffect(Unit) {
        viewModel.consumeOpenSetupPending()
    }

    LaunchedEffect(languageCode) {
        viewModel.refreshLanguage()
    }

    EduAiTheme {
        Scaffold(
            topBar = {
                EduScreenTopBar(
                    title =
                        if (showSetup) {
                            ExamPlanCopy.overviewTitleSetup(languageCode)
                        } else {
                            ExamPlanCopy.overviewTitlePlan(languageCode)
                        },
                    showBack = showBackNavigation,
                    onBack = onNavigateBack,
                    actionLabel = if (!showSetup) ExamPlanCopy.addPlan(languageCode) else null,
                    onActionClick = { viewModel.openSetup() },
                )
            },
        ) { padding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(EduAiTheme.colors.surface1)
                        .padding(padding),
            ) {
                when {
                    isLoading && days.isEmpty() && activePlan == null && !showSetup -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    showSetup -> {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                        ) {
                            ExamPlanSetupPanel(
                                viewModel = viewModel,
                                languageCode = languageCode,
                            )
                        }
                    }
                    days.isEmpty() && activePlan == null -> {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = ExamPlanCopy.emptyDayMessage(languageCode),
                                color = EduAiTheme.colors.textSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            OutlinedButton(
                                onClick = { viewModel.openSetup() },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(ExamPlanCopy.addPlan(languageCode))
                            }
                        }
                    }
                    else -> {
                        val plan = activePlan
                        val chapterCount =
                            plan?.chapterIds?.split(",")?.count { it.isNotBlank() } ?: 0
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            PlanRewardBanner(
                                dayCount = days.size,
                                languageCode = languageCode,
                                onTap = { EngagementAnalyticsTracker.planRewardBannerTap(days.size) },
                            )
                            if (plan != null) {
                                PlanSummaryCard(
                                    examType = plan.examType,
                                    dailyMinutes = plan.dailyMinutes,
                                    dayCount = days.size,
                                    chapterCount = chapterCount,
                                    languageCode = languageCode,
                                    onCustomize = { viewModel.openSetup() },
                                )
                            }
                            days.forEach { day ->
                                PlanDayRow(
                                    day = day,
                                    languageCode = languageCode,
                                    onClick = { onDayClick(day) },
                                )
                            }
                            OutlinedButton(
                                onClick = { viewModel.openSetup() },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(ExamPlanCopy.addPlan(languageCode))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanRewardBanner(
    dayCount: Int,
    languageCode: String,
    onTap: () -> Unit = {},
) {
    val colors = EduAiTheme.colors
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .pressScaleClickable(onClick = onTap)
                .background(
                    Brush.horizontalGradient(listOf(colors.accent, colors.pro)),
                    RoundedCornerShape(16.dp),
                )
                .padding(16.dp),
    ) {
        Text(
            text = ExamPlanCopy.growAsYouLearn(languageCode),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = ExamPlanCopy.growBannerBody(languageCode, dayCount),
            color = Color.White.copy(alpha = 0.92f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun PlanDayRow(
    day: PlanDayNode,
    languageCode: String,
    onClick: () -> Unit,
) {
    val colors = EduAiTheme.colors
    val statusLabel = ExamPlanCopy.planDayStatusLabel(languageCode, day.status)
    val typeLabel = ExamPlanCopy.planDayTypeLabel(languageCode, day.type)
    val statusColor =
        when (day.status) {
            PlanDayStatus.Done -> colors.success
            PlanDayStatus.Today -> colors.accent
            PlanDayStatus.Partial -> colors.warning
            PlanDayStatus.Upcoming -> colors.textMuted
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .pressScaleClickable(onClick = onClick, pressedScale = 0.98f)
                .background(colors.surface2, MaterialTheme.shapes.medium)
                .clip(MaterialTheme.shapes.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .width(5.dp)
                    .height(52.dp)
                    .background(statusColor),
        )
        Column(
            modifier = Modifier.weight(1f).padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 10.dp),
        ) {
            Text(
                text = ExamPlanCopy.dayRowPrefix(languageCode, day.day, typeLabel),
                color = colors.textMuted,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = day.label.ifBlank { typeLabel },
                color = colors.text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (day.type == PlanDayType.Lesson) {
                Text(
                    text = ExamPlanCopy.growsYourWorld(languageCode),
                    color = colors.accent,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        Text(
            text = statusLabel,
            color = statusColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier =
                Modifier
                    .padding(end = 12.dp)
                    .background(statusColor.copy(alpha = 0.14f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
