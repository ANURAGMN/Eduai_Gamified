package com.ncert7.aitutorandlab.ui.screens.plan

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.anurag.eduai.uikit.components.EduScreenTopBar
import com.anurag.eduai.uikit.components.PlanTrialAdvanceOverlay
import com.anurag.eduai.uikit.components.PlanTrialPath
import com.anurag.eduai.uikit.components.PlanTrialStacked
import com.anurag.eduai.uikit.components.TrialNodeState
import com.anurag.eduai.uikit.components.TrialNodeType
import com.anurag.eduai.uikit.components.TrialPathChapter
import com.anurag.eduai.uikit.components.TrialPathNode
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemKind
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemStatus
import com.ncert7.aitutorandlab.utils.ExamPlanCopy
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import com.ncert7.aitutorandlab.utils.TrialCopy
import com.ncert7.aitutorandlab.ui.screens.garden.GardenNextPlacePickerOverlay
import com.ncert7.aitutorandlab.ui.screens.plan.components.TrialMomentHost
import com.ncert7.aitutorandlab.service.analytics.ScreenName
import com.ncert7.aitutorandlab.service.analytics.TrackScreenEvent
import com.ncert7.aitutorandlab.ui.screens.plan.viewmodel.PlanTrialItemUi
import com.ncert7.aitutorandlab.ui.screens.plan.viewmodel.PlanTrialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanTrialScreen(
    onNavigateBack: () -> Unit,
    onItemClick: (PlanTrialItemUi) -> Unit,
    viewModel: PlanTrialViewModel = hiltViewModel(),
) {
    TrackScreenEvent(screenName = ScreenName.PLAN_TRIAL)

    val colors = EduAiTheme.colors
    val context = LocalContext.current
    val languageCode by viewModel.displayLanguage.collectAsState()
    SideEffect {
        viewModel.syncLanguage(getCurrentLanguageCode())
    }
    val overlayLabels = TrialCopy.trialOverlayLabels(languageCode)
    val activity = context as? Activity
    val items by viewModel.items.collectAsState()
    val planDay by viewModel.planDay.collectAsState()
    val planDayTitle by viewModel.planDayTitle.collectAsState()
    val dayLoadComplete by viewModel.dayLoadComplete.collectAsState()
    val advanceOverlay by viewModel.advanceOverlay.collectAsState()
    val launchTarget by viewModel.launchTarget.collectAsState()
    val partialReturnPrompt by viewModel.partialReturnPrompt.collectAsState()
    val exitHook by viewModel.exitHook.collectAsState()
    val moment by viewModel.moment.collectAsState()
    val gardenPlacePicker by viewModel.gardenPlacePicker.collectAsState()
    var pathView by rememberSaveable { mutableStateOf(true) }

    LifecycleResumeEffect(Unit) {
        viewModel.onTrialScreenVisible()
        onPauseOrDispose { viewModel.onTrialScreenHidden() }
    }

    LaunchedEffect(launchTarget) {
        launchTarget?.let { item ->
            viewModel.prepareLaunch(item)
            onItemClick(item)
            viewModel.clearLaunchTarget()
        }
    }

    LaunchedEffect(advanceOverlay) {
        if (advanceOverlay != null) {
            viewModel.refreshAdReady()
        }
    }

    LaunchedEffect(
        advanceOverlay?.completedItemId,
        advanceOverlay?.requiresMandatoryClaim,
        advanceOverlay?.mandatoryClaimCompleted,
        partialReturnPrompt,
        moment,
    ) {
        if (partialReturnPrompt != null || moment != null) return@LaunchedEffect
        val overlay = advanceOverlay ?: return@LaunchedEffect
        if (!overlay.requiresMandatoryClaim || overlay.mandatoryClaimCompleted) return@LaunchedEffect
        activity?.let { viewModel.autoClaimMandatoryAd(it) }
    }

    BackHandler {
        viewModel.requestExit(onNavigateBack)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                EduScreenTopBar(
                    title =
                        planDay?.let { day ->
                            TrialCopy.trialDayTitle(languageCode, day.dayIndex, planDayTitle)
                        } ?: planDayTitle?.takeIf { it.isNotBlank() }
                            ?: TrialCopy.trialScreenTitle(languageCode),
                    showBack = true,
                    onBack = { viewModel.requestExit(onNavigateBack) },
                )
            },
        ) { padding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(colors.surface1)
                        .padding(padding),
            ) {
                when {
                    !dayLoadComplete -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    items.isEmpty() -> {
                        Text(
                            text = ExamPlanCopy.emptyDayMessage(languageCode),
                            color = colors.textMuted,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 24.dp),
                        )
                    }
                    else -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            TrialViewToggle(
                                pathView = pathView,
                                languageCode = languageCode,
                                onSelect = { selected ->
                                    pathView = selected
                                    viewModel.onTrialViewSelected(selected)
                                },
                            )
                            if (pathView) {
                                PlanTrialPath(
                                    chapters = buildTrialChapters(items),
                                    onNodeClick = { node ->
                                        items.firstOrNull { it.id == node.id }?.let { item ->
                                            viewModel.prepareLaunch(item)
                                            onItemClick(item)
                                        }
                                    },
                                    dayIndex = modPositive(planDay?.dayIndex ?: 0, 4),
                                    onChestClick = {
                                        Toast.makeText(
                                            context,
                                            TrialCopy.chapterClearedToast(languageCode),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    },
                                    onDayCompleteClick = {
                                        Toast.makeText(
                                            context,
                                            TrialCopy.dayCompleteToast(languageCode),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                PlanTrialStacked(
                                    chapters = buildTrialChapters(items),
                                    onNodeClick = { node ->
                                        items.firstOrNull { it.id == node.id }?.let { item ->
                                            viewModel.prepareLaunch(item)
                                            onItemClick(item)
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (partialReturnPrompt == null && moment == null) {
            advanceOverlay?.let { overlay ->
                PlanTrialAdvanceOverlay(
                    visible = true,
                    title = overlay.title,
                    subtitle = overlay.subtitle,
                    xpEarned = overlay.xpEarned,
                    gemsEarned = overlay.gemsEarned,
                    bonusXpEarned = overlay.doubleXpBonusEarned,
                    xpBarFrom = overlay.xpBarFrom,
                    xpBarTo = overlay.xpBarTo,
                    weeklyXpTotal = overlay.weeklyXpTotal,
                    requiresMandatoryClaim = overlay.requiresMandatoryClaim,
                    mandatoryGemsReward = overlay.mandatoryGemsReward,
                    mandatoryClaimCompleted = overlay.mandatoryClaimCompleted,
                    mandatoryAdSkipped = overlay.mandatoryAdSkipped,
                    doubleXpAmount = overlay.doubleXpAmount,
                    doubleXpClaimed = overlay.doubleXpClaimed,
                    adReady = overlay.adReady,
                    labels = overlayLabels,
                    onWatchMandatoryAd = {
                        activity?.let { viewModel.claimMandatoryAd(it) }
                    },
                    onSkipMandatoryAd = { viewModel.skipMandatoryAd() },
                    onWatchDoubleXpAd = {
                        activity?.let { viewModel.claimDoubleXpAd(it) }
                    },
                    onFinished = { viewModel.onAdvanceFinished() },
                )
            }
        }

        TrialMomentHost(
            moment = moment,
            onPrimary = {
                viewModel.trackMomentPrimary()
                when {
                    partialReturnPrompt != null -> viewModel.continuePartialItem()
                    exitHook != null -> viewModel.dismissExitHook()
                    moment != null -> viewModel.dismissMoment()
                    advanceOverlay != null -> viewModel.onAdvanceFinished()
                }
            },
            onSecondary = {
                viewModel.trackMomentSecondary()
                when {
                    partialReturnPrompt != null -> viewModel.dismissPartialReturnPrompt()
                    exitHook != null -> viewModel.confirmExit()
                }
            },
        )

        GardenNextPlacePickerOverlay(
            picker = gardenPlacePicker,
            languageCode = languageCode,
            onConfirm = { zoneIndex -> viewModel.confirmGardenPlacePicker(zoneIndex) },
            onDismiss = { viewModel.dismissGardenPlacePicker() },
        )
    }
}

/**
 * Groups the flat trial-item list into chapter "clubs" for the path: one [TrialPathChapter] per
 * chapter (name parsed from the item title prefix), items tagged by type, and only the first
 * not-yet-done item marked Active — the rest are Upcoming (still tappable, just a lighter tint).
 */
private fun buildTrialChapters(items: List<PlanTrialItemUi>): List<TrialPathChapter> {
    val activeId = items.firstOrNull { it.status != PlanTrialItemStatus.DONE }?.id
    val chapterOrder = items.map { it.chapterId }.distinct()
    return chapterOrder.map { chapterId ->
        val chapterItems = items.filter { it.chapterId == chapterId }
            val chapterTitle =
                chapterItems.first().title.substringBefore(" · ").trim().ifBlank { "Chapter" }
            TrialPathChapter(
                title = chapterTitle,
                nodes =
                    chapterItems.map { item ->
                        val type =
                            when (item.kind) {
                                PlanTrialItemKind.SIM_URL -> TrialNodeType.Simulation
                                PlanTrialItemKind.SIM_AGENT -> TrialNodeType.SimAgent
                                PlanTrialItemKind.STUDY -> TrialNodeType.Study
                                PlanTrialItemKind.MATH -> TrialNodeType.Math
                                PlanTrialItemKind.REVISION -> TrialNodeType.Revision
                                else -> TrialNodeType.Study
                            }
                        val state =
                            when {
                                item.status == PlanTrialItemStatus.DONE -> TrialNodeState.Done
                                item.id == activeId -> TrialNodeState.Active
                                else -> TrialNodeState.Upcoming
                            }
                        val concept = item.title.substringAfterLast(" · ").trim()
                        val progress =
                            if (item.requiredCount > 0) {
                                item.completedCount.toFloat() / item.requiredCount
                            } else {
                                0f
                            }
                        TrialPathNode(
                            id = item.id,
                            type = type,
                            state = state,
                            title = concept.ifBlank { item.title },
                            progress = progress,
                        )
                    },
            )
    }
}

@Composable
private fun TrialViewToggle(pathView: Boolean, languageCode: String, onSelect: (Boolean) -> Unit) {
    val colors = EduAiTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.surface2)
                    .padding(4.dp),
        ) {
            ToggleChip(label = TrialCopy.trialPathToggle(languageCode), selected = pathView) { onSelect(true) }
            ToggleChip(label = TrialCopy.trialStackedToggle(languageCode), selected = !pathView) { onSelect(false) }
        }
    }
}

@Composable
private fun ToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = EduAiTheme.colors
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(if (selected) colors.accent else Color.Transparent)
                .clickable { onClick() }
                .padding(horizontal = 18.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            color = if (selected) colors.onAccent else colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

private fun modPositive(value: Int, size: Int): Int {
    if (size <= 0) return 0
    val mod = value % size
    return if (mod < 0) mod + size else mod
}
