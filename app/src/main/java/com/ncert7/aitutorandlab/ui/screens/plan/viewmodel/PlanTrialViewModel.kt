package com.ncert7.aitutorandlab.ui.screens.plan.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncert7.aitutorandlab.notification.NotificationPermissionGate
import com.ncert7.aitutorandlab.notification.NotificationPrimerVariant
import dagger.hilt.android.qualifiers.ApplicationContext
import com.ncert7.aitutorandlab.data.local.dao.ChapterDao
import com.ncert7.aitutorandlab.data.local.dao.ConceptDao
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanDayEntity
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemEntity
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemKind
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemStatus
import com.ncert7.aitutorandlab.domain.examplan.PlanTrialProgressTracker
import com.ncert7.aitutorandlab.domain.examplan.TrialAdClaimResult
import com.ncert7.aitutorandlab.domain.examplan.TrialRewardService
import com.ncert7.aitutorandlab.domain.examplan.TrialSessionStore
import com.ncert7.aitutorandlab.domain.gamification.EconomyConfig
import com.ncert7.aitutorandlab.domain.gamification.GamificationRewardService
import com.ncert7.aitutorandlab.domain.garden.GardenCelebration
import com.ncert7.aitutorandlab.domain.garden.GardenMomentCoordinator
import com.anurag.eduai.uikit.garden.quest.Theme
import com.anurag.eduai.uikit.garden.quest.ZONES
import com.anurag.eduai.uikit.garden.quest.placeBased
import com.ncert7.aitutorandlab.ui.screens.garden.GardenNextPlacePickerUi
import com.ncert7.aitutorandlab.domain.moment.MomentTokens
import com.ncert7.aitutorandlab.domain.moment.MomentType
import com.ncert7.aitutorandlab.domain.moment.MomentUiModel
import com.ncert7.aitutorandlab.domain.moment.MomentVariantPicker
import com.ncert7.aitutorandlab.service.analytics.EngagementAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.GamificationAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.TrialViewMode
import com.ncert7.aitutorandlab.repository.ExamPlanRepository
import com.ncert7.aitutorandlab.repository.LeagueRankProjection
import com.ncert7.aitutorandlab.repository.LeagueRepository
import com.ncert7.aitutorandlab.repository.PlanTrialRepository
import com.ncert7.aitutorandlab.repository.GardenRepository
import com.ncert7.aitutorandlab.utils.TrialCopy
import com.ncert7.aitutorandlab.utils.TrialTitleResolver
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import com.ncert7.aitutorandlab.utils.getLocalizedName
import com.ncert7.aitutorandlab.utils.normalizeLanguageCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class PlanTrialItemUi(
    val id: Long,
    val title: String,
    val kind: String,
    val kindLabel: String,
    val status: String,
    val statusLabel: String,
    val conceptId: String,
    val chapterId: String,
    val sourceId: String,
    val progressLabel: String?,
    val completedCount: Int = 0,
    val requiredCount: Int = 1,
    val carriedFromDayIndex: Int? = null,
)

data class TrialAdvanceUiState(
    val completedItemId: Long,
    val completedItemKind: String,
    val title: String,
    val subtitle: String,
    val launchNext: PlanTrialItemUi?,
    val xpEarned: Int = 0,
    val gemsEarned: Int = 0,
    val xpBarFrom: Float = 0f,
    val xpBarTo: Float = 0f,
    val weeklyXpTotal: Int = 0,
    val requiresMandatoryClaim: Boolean = false,
    val mandatoryGemsReward: Int = 0,
    val mandatoryClaimCompleted: Boolean = true,
    val mandatoryBatchIndex: Int = 0,
    val doubleXpAmount: Int = 0,
    val doubleXpBonusEarned: Int = 0,
    val doubleXpClaimed: Boolean = false,
    val adReady: Boolean = false,
    val mandatoryAdSkipped: Boolean = false,
)

data class TrialPartialReturnUiState(
    val item: PlanTrialItemUi,
    val title: String,
    val message: String,
    val remaining: Int,
)

data class TrialExitHookUiState(
    val pendingTrialCount: Int,
    val title: String,
    val message: String,
)

@HiltViewModel
class PlanTrialViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    private val planTrialRepository: PlanTrialRepository,
    private val examPlanRepository: ExamPlanRepository,
    private val planTrialProgressTracker: PlanTrialProgressTracker,
    private val gamificationRewardService: GamificationRewardService,
    private val trialRewardService: TrialRewardService,
    private val leagueRepository: LeagueRepository,
    private val sharedPrefs: SharedPreferenceUtils,
    private val momentVariantPicker: MomentVariantPicker,
    private val gardenMomentCoordinator: GardenMomentCoordinator,
    private val gardenRepository: GardenRepository,
    private val conceptDao: ConceptDao,
    private val chapterDao: ChapterDao,
) : ViewModel() {

    // When opened from the chapter picker, the trial is scoped to a chapter (no plan day).
    private val chapterId: String? = savedStateHandle.get<String>("chapterId")

    // Chapter trials live in a negative day-index space so they never collide with the
    // exam-plan days (which are >= 0).
    private val dayIndex: Int =
        savedStateHandle.get<Int>("dayIndex") ?: run {
            val h = checkNotNull(chapterId).hashCode()
            -(if (h == Int.MIN_VALUE) Int.MAX_VALUE else kotlin.math.abs(h))
        }

    private val userId: String
        get() = sharedPrefs.getUserId().orEmpty()

    private val activeLanguage: String
        get() = _languageCode.value

    private val _languageCode =
        MutableStateFlow(normalizeLanguageCode(getCurrentLanguageCode()))

    val displayLanguage: StateFlow<String> = _languageCode

    private val _planDay = MutableStateFlow<ExamPlanDayEntity?>(null)
    val planDay: StateFlow<ExamPlanDayEntity?> = _planDay

    private val _planDayTitle = MutableStateFlow<String?>(null)
    val planDayTitle: StateFlow<String?> = _planDayTitle

    private val _dayLoadComplete = MutableStateFlow(false)
    val dayLoadComplete: StateFlow<Boolean> = _dayLoadComplete

    private val _items = MutableStateFlow<List<PlanTrialItemUi>>(emptyList())
    val items: StateFlow<List<PlanTrialItemUi>> = _items

    private val _advanceOverlay = MutableStateFlow<TrialAdvanceUiState?>(null)
    val advanceOverlay: StateFlow<TrialAdvanceUiState?> = _advanceOverlay

    private val _launchTarget = MutableStateFlow<PlanTrialItemUi?>(null)
    val launchTarget: StateFlow<PlanTrialItemUi?> = _launchTarget

    private val _partialReturnPrompt = MutableStateFlow<TrialPartialReturnUiState?>(null)
    val partialReturnPrompt: StateFlow<TrialPartialReturnUiState?> = _partialReturnPrompt

    private val _exitHook = MutableStateFlow<TrialExitHookUiState?>(null)
    val exitHook: StateFlow<TrialExitHookUiState?> = _exitHook

    private val _moment = MutableStateFlow<MomentUiModel?>(null)
    val moment: StateFlow<MomentUiModel?> = _moment

    private val _gardenPlacePicker = MutableStateFlow<GardenNextPlacePickerUi?>(null)
    val gardenPlacePicker: StateFlow<GardenNextPlacePickerUi?> = _gardenPlacePicker

    private var pendingGemMoment: MomentUiModel? = null
    private var pendingAdvanceOverlay: TrialAdvanceUiState? = null
    private var pendingPlaceCelebration: GardenCelebration? = null

    private val _screenActive = MutableStateFlow(false)

    private var pendingExitAction: (() -> Unit)? = null
    private var mandatoryAdJob: Job? = null
    private var trialDayStartMs: Long = 0L
    private var trialDayStartTracked = false
    private var currentTrialView: TrialViewMode = TrialViewMode.PATH

    companion object {
        private const val MANDATORY_AD_INITIAL_DELAY_MS = 3_000L
        private const val MANDATORY_AD_RETRY_COUNT = 30
        private const val MANDATORY_AD_RETRY_DELAY_MS = 300L
        private const val MANDATORY_AD_WATCHDOG_MS = 25_000L
        // After a denied/closed reward ad, hold the "points not awarded" message briefly, then move on.
        private const val POINTS_DENIED_AUTO_ADVANCE_MS = 1_800L
    }

    private fun showMoment(model: MomentUiModel) {
        cancelMandatoryAdSchedule()
        GamificationAnalyticsTracker.momentShown(
            moment = model.moment,
            variantId = model.variantId,
            celebratory = model.celebratory,
        )
        _moment.value = model
    }

    init {
        viewModelScope.launch {
            combine(
                planTrialRepository.observeTrialItems(userId, dayIndex),
                _languageCode,
                _planDay,
            ) { rows, lang, day -> Triple(rows, lang, day) }
                .collectLatest { (rows, lang, day) ->
                    val uiItems = rows.map { entity -> entity.toUi(lang) }
                    if (_items.value != uiItems) {
                        _items.value = uiItems
                    }
                    if (day != null) {
                        val title =
                            TrialTitleResolver.localizedPlanDayLabel(day, lang, conceptDao, chapterDao)
                        if (_planDayTitle.value != title) {
                            _planDayTitle.value = title
                        }
                    }
                }
        }
        viewModelScope.launch {
            combine(
                planTrialRepository.observeTrialItems(userId, dayIndex),
                _screenActive,
            ) { rows, active ->
                if (active) rows else emptyList()
            }
                .distinctUntilChanged()
                .collect {
                    if (
                        _screenActive.value &&
                        _partialReturnPrompt.value == null &&
                        _advanceOverlay.value == null &&
                        _moment.value == null
                    ) {
                        processTrialScreenResume()
                    }
                }
        }
    }

    fun syncLanguage(lang: String) {
        val normalized = normalizeLanguageCode(lang)
        if (_languageCode.value != normalized) {
            _languageCode.value = normalized
        }
    }

    fun refreshLanguage() {
        syncLanguage(getCurrentLanguageCode())
    }

    fun onTrialScreenVisible() {
        viewModelScope.launch {
            syncLanguage(getCurrentLanguageCode())
            _screenActive.value = true
            trialDayStartMs = System.currentTimeMillis()
            trialRewardService.preloadRewardedAd()
            if (!_dayLoadComplete.value) {
                loadDay()
                _dayLoadComplete.value = true
            }
            if (!trialDayStartTracked) {
                trialDayStartTracked = true
                val day = _planDay.value
                GamificationAnalyticsTracker.trialDayStart(
                    dayIndex = dayIndex,
                    dayType = day?.dayType.orEmpty(),
                    itemCount = items.value.size,
                    trialView = currentTrialView,
                )
            }
            processTrialScreenResume()
        }
    }

    fun onTrialViewSelected(pathView: Boolean) {
        currentTrialView = if (pathView) TrialViewMode.PATH else TrialViewMode.STACKED
        GamificationAnalyticsTracker.trialViewSelected(currentTrialView)
    }

    /** Single entry point after returning to the trial list — partial prompt OR celebration, never both. */
    private suspend fun processTrialScreenResume() {
        if (_advanceOverlay.value != null) return

        flushPendingSessionProgress()
        reconcileActiveTrialSession()

        if (_partialReturnPrompt.value != null) return

        if (_moment.value != null) return

        if (pendingAdvanceOverlay != null) {
            revealAdvanceOverlayIfReady()
            return
        }

        val partial = buildPartialReturnIfNeeded()
        if (partial != null) {
            cancelMandatoryAdSchedule()
            _advanceOverlay.value = null
            pendingAdvanceOverlay = null
            pendingGemMoment = null
            _partialReturnPrompt.value = partial
            showMoment(resolveComebackMoment(partial))
            return
        }

        checkDeferredCelebration()
    }

    private suspend fun ensureGardenCelebrationQueued() {
        if (userId.isBlank()) return
        if (gardenMomentCoordinator.pending.value != null) return
        gardenRepository.queueCelebrationForUnshownPlant(
            studentId = userId,
            lastCelebratedPlantTotal = sharedPrefs.getLastGardenCelebrationPlantTotal(userId),
            coordinator = gardenMomentCoordinator,
        )
    }

    private fun revealAdvanceOverlayIfReady() {
        if (_moment.value != null) return
        if (pendingGemMoment != null) return
        if (gardenMomentCoordinator.pending.value != null) return
        val pending = pendingAdvanceOverlay ?: return
        pendingAdvanceOverlay = null
        _advanceOverlay.value = pending
        if (!trialRewardService.isRewardedAdReady()) {
            trialRewardService.preloadRewardedAd()
        }
    }

    private suspend fun beginPostCompletionCelebrations() {
        val showingGardenMoment =
            when (_moment.value?.moment) {
                MomentType.PLANT_COMPLETED,
                MomentType.PLACE_COMPLETED,
                -> true
                else -> false
            }
        if (userId.isNotBlank() && !showingGardenMoment &&
            gardenMomentCoordinator.pending.value == null
        ) {
            gardenRepository.queueCelebrationForUnshownPlant(
                studentId = userId,
                lastCelebratedPlantTotal = sharedPrefs.getLastGardenCelebrationPlantTotal(userId),
                coordinator = gardenMomentCoordinator,
            )
        }
        when {
            gardenMomentCoordinator.pending.value != null -> maybeShowGardenMoment()
            pendingGemMoment != null -> showPendingGemMoment()
        }
    }

    private fun celebrationsBlockMandatoryAd(): Boolean {
        if (_moment.value != null) return true
        if (pendingGemMoment != null) return true
        if (gardenMomentCoordinator.pending.value != null) return true
        if (_gardenPlacePicker.value != null) return true
        return false
    }

    private fun showPendingGemMoment() {
        if (_moment.value != null) return
        val pending = pendingGemMoment
        if (pending == null) {
            // No gem moment to show — don't dead-end; continue the chain to the XP/gems advance
            // overlay (auto-next / mandatory ad) if one is staged.
            revealAdvanceOverlayIfReady()
            return
        }
        pendingGemMoment = null
        showMoment(pending)
    }

    private fun maybeShowGardenMoment() {
        if (_moment.value != null) return
        if (_partialReturnPrompt.value != null) return
        if (_exitHook.value != null) return
        val celebration = gardenMomentCoordinator.pending.value ?: return
        val model = gardenMomentCoordinator.buildMoment(activeLanguage) ?: return
        if (celebration.placeCompleted && celebration.remainingScenes > 0) {
            pendingPlaceCelebration = celebration
        }
        gardenMomentCoordinator.clear()
        sharedPrefs.setLastGardenCelebrationPlantTotal(userId, celebration.totalPlanted)
        showMoment(model)
    }

    private fun completeMandatoryClaimWithoutReward(
        adSkipped: Boolean = false,
        autoAdvance: Boolean = false,
    ) {
        cancelMandatoryAdSchedule()
        _advanceOverlay.update { current ->
            if (current == null || current.mandatoryClaimCompleted) return@update current
            current.copy(
                mandatoryClaimCompleted = true,
                mandatoryAdSkipped = adSkipped,
                subtitle =
                    when {
                        adSkipped ->
                            TrialCopy.advanceMandatoryAdSkipped(
                                activeLanguage,
                                current.mandatoryGemsReward,
                            )
                        else -> advanceSubtitle(current.launchNext)
                    },
            )
        }
        // Reward denied/closed during ad play: show the "points not awarded" message, then move on to
        // the next task automatically. Not used for the exit/back path, which must not auto-launch.
        if (autoAdvance) {
            mandatoryAdJob?.cancel()
            mandatoryAdJob =
                viewModelScope.launch {
                    delay(POINTS_DENIED_AUTO_ADVANCE_MS)
                    val overlay = _advanceOverlay.value ?: return@launch
                    if (overlay.requiresMandatoryClaim && !overlay.mandatoryClaimCompleted) return@launch
                    onAdvanceFinished()
                }
        }
    }

    private fun advanceSubtitle(next: PlanTrialItemUi?): String =
        if (next != null) {
            TrialCopy.advanceUpNext(activeLanguage, next.kindLabel, next.title)
        } else {
            TrialCopy.advanceAllDone(activeLanguage)
        }

    private suspend fun flushPendingSessionProgress() {
        TrialSessionStore.consumePendingSessionProgress()?.let { (itemId, count) ->
            planTrialProgressTracker.syncToCount(itemId, count)
            planTrialProgressTracker.reconcileCompletion(itemId)
        }
    }

    private suspend fun reconcileActiveTrialSession() {
        TrialSessionStore.activeTrialItemId?.let { trialItemId ->
            planTrialProgressTracker.reconcileCompletion(trialItemId)
        }
    }

    fun onTrialScreenHidden() {
        _screenActive.value = false
    }

    fun prepareLaunch(item: PlanTrialItemUi) {
        TrialSessionStore.activeTrialItemId = item.id
    }

    fun refreshAdReady() {
        _advanceOverlay.update { overlay ->
            overlay?.copy(adReady = trialRewardService.isRewardedAdReady())
        }
        if (_advanceOverlay.value?.adReady != true) {
            trialRewardService.preloadRewardedAd()
        }
    }

    fun claimMandatoryAd(activity: Activity) {
        val overlay = _advanceOverlay.value ?: return
        if (!overlay.requiresMandatoryClaim || overlay.mandatoryClaimCompleted) return
        if (_partialReturnPrompt.value != null) return
        viewModelScope.launch {
            when (
                trialRewardService.claimMandatoryRewardAd(
                    activity = activity,
                    studentId = userId,
                    batchIndex = overlay.mandatoryBatchIndex,
                )
            ) {
                TrialAdClaimResult.SUCCESS -> {
                    _advanceOverlay.update { current ->
                        current?.copy(
                            mandatoryClaimCompleted = true,
                            gemsEarned = current.gemsEarned + EconomyConfig.GEM_TRIAL_MANDATORY_CLAIM,
                            subtitle = advanceSubtitle(current.launchNext),
                        )
                    }
                }
                TrialAdClaimResult.NOT_READY -> refreshAdReady()
                TrialAdClaimResult.AD_SKIPPED ->
                    completeMandatoryClaimWithoutReward(adSkipped = true, autoAdvance = true)
                TrialAdClaimResult.GRANT_FAILED ->
                    completeMandatoryClaimWithoutReward(adSkipped = false, autoAdvance = true)
            }
        }
    }

    fun skipMandatoryAd() {
        val overlay = _advanceOverlay.value ?: return
        if (!overlay.requiresMandatoryClaim || overlay.mandatoryClaimCompleted) return
        completeMandatoryClaimWithoutReward(adSkipped = true)
    }

    fun autoClaimMandatoryAd(activity: Activity) {
        if (_partialReturnPrompt.value != null || celebrationsBlockMandatoryAd()) return
        val overlay = _advanceOverlay.value ?: return
        if (!overlay.requiresMandatoryClaim || overlay.mandatoryClaimCompleted) return
        mandatoryAdJob?.cancel()
        mandatoryAdJob =
            viewModelScope.launch {
            val completed =
                withTimeoutOrNull(MANDATORY_AD_WATCHDOG_MS) {
                    delay(MANDATORY_AD_INITIAL_DELAY_MS)
                    if (_partialReturnPrompt.value != null || celebrationsBlockMandatoryAd()) return@withTimeoutOrNull
                    val current = _advanceOverlay.value ?: return@withTimeoutOrNull
                    if (current.mandatoryClaimCompleted) return@withTimeoutOrNull
                    repeat(MANDATORY_AD_RETRY_COUNT) {
                        if (_partialReturnPrompt.value != null || celebrationsBlockMandatoryAd()) return@withTimeoutOrNull
                        val live = _advanceOverlay.value ?: return@withTimeoutOrNull
                        if (live.mandatoryClaimCompleted) return@withTimeoutOrNull
                        refreshAdReady()
                        if (trialRewardService.isRewardedAdReady()) {
                            when (
                                trialRewardService.claimMandatoryRewardAd(
                                    activity = activity,
                                    studentId = userId,
                                    batchIndex = live.mandatoryBatchIndex,
                                )
                            ) {
                                TrialAdClaimResult.SUCCESS -> {
                                    _advanceOverlay.update { state ->
                                        state?.copy(
                                            mandatoryClaimCompleted = true,
                                            gemsEarned =
                                                state.gemsEarned + EconomyConfig.GEM_TRIAL_MANDATORY_CLAIM,
                                            subtitle = advanceSubtitle(state.launchNext),
                                        )
                                    }
                                    return@withTimeoutOrNull
                                }
                                TrialAdClaimResult.AD_SKIPPED -> {
                                    completeMandatoryClaimWithoutReward(adSkipped = true, autoAdvance = true)
                                    return@withTimeoutOrNull
                                }
                                TrialAdClaimResult.GRANT_FAILED -> {
                                    completeMandatoryClaimWithoutReward(adSkipped = false, autoAdvance = true)
                                    return@withTimeoutOrNull
                                }
                                TrialAdClaimResult.NOT_READY -> Unit
                            }
                        } else {
                            trialRewardService.preloadRewardedAd()
                        }
                        delay(MANDATORY_AD_RETRY_DELAY_MS)
                    }
                }
            if (completed == null &&
                _advanceOverlay.value?.requiresMandatoryClaim == true &&
                _advanceOverlay.value?.mandatoryClaimCompleted == false
            ) {
                completeMandatoryClaimWithoutReward(adSkipped = true, autoAdvance = true)
            }
        }
    }

    private fun cancelMandatoryAdSchedule() {
        mandatoryAdJob?.cancel()
        mandatoryAdJob = null
    }

    fun claimDoubleXpAd(activity: Activity) {
        val overlay = _advanceOverlay.value ?: return
        if (overlay.doubleXpClaimed || overlay.doubleXpAmount <= 0) return
        viewModelScope.launch {
            val (result, bonus) =
                trialRewardService.claimDoubleXpAd(
                    activity = activity,
                    studentId = userId,
                    trialItemId = overlay.completedItemId,
                    kind = overlay.completedItemKind,
                    language = activeLanguage,
                )
            when (result) {
                TrialAdClaimResult.SUCCESS ->
                    bonus?.let { reward ->
                        _advanceOverlay.update { current ->
                            current?.copy(
                                doubleXpClaimed = true,
                                doubleXpBonusEarned = reward.xpEarned,
                            )
                        }
                    }
                TrialAdClaimResult.NOT_READY -> refreshAdReady()
                else -> Unit
            }
        }
    }

    fun onAdvanceFinished() {
        val overlay = _advanceOverlay.value ?: return
        if (overlay.requiresMandatoryClaim && !overlay.mandatoryClaimCompleted) return
        viewModelScope.launch {
            planTrialProgressTracker.markCelebrated(overlay.completedItemId)
            _advanceOverlay.value = null
            pendingAdvanceOverlay = null
            _moment.value = null
            // A task's reward was just consumed — remember it so the first return to home can request
            // an in-app review (fully compliant; any card shown is Google's own).
            sharedPrefs.setHasCompletedAnyTask()
            overlay.launchNext?.let { next ->
                // More to do → a good moment to ask about reminders (the gate enforces its own
                // once-a-day / max-3 cadence).
                NotificationPermissionGate.onMeaningfulWin(
                    context = appContext,
                    prefs = sharedPrefs,
                    variant = NotificationPrimerVariant.DEFAULT,
                )
                TrialSessionStore.activeTrialItemId = next.id
                _launchTarget.value = next
            } ?: run {
                TrialSessionStore.clear()
            }
        }
    }

    fun clearLaunchTarget() {
        _launchTarget.value = null
    }

    fun dismissMoment() {
        val current = _moment.value
        _moment.value = null
        when (current?.moment) {
            MomentType.PLANT_COMPLETED -> showPendingGemMoment()
            MomentType.PLACE_COMPLETED -> maybeShowGardenPlacePicker()
            else -> revealAdvanceOverlayIfReady()
        }
    }

    private fun maybeShowGardenPlacePicker() {
        val pending = pendingPlaceCelebration
        pendingPlaceCelebration = null
        if (pending == null || pending.remainingScenes <= 0) {
            showPendingGemMoment()
            return
        }
        viewModelScope.launch {
            val progress = gardenRepository.getProgress(userId)
            val theme = gardenRepository.toComposeTheme(progress?.theme.orEmpty())
            if (!theme.placeBased) {
                showPendingGemMoment()
                return@launch
            }
            // Offer the next few unlocked-but-unvisited scenes to choose from (like the prototype's
            // offers()). Falls back to the single next zone if that's all that's left.
            val unlocked = progress?.unlockedZones ?: listOf(pending.zone)
            val candidates =
                ZONES.indices.filter { it !in unlocked }.take(3)
                    .ifEmpty { listOf((pending.zone + 1).coerceAtMost(ZONES.lastIndex)) }
            EngagementAnalyticsTracker.placeCompleted(pending.zone)
            EngagementAnalyticsTracker.nextPlaceOffered(candidates.size)
            _gardenPlacePicker.value =
                GardenNextPlacePickerUi(
                    completedZoneIndex = pending.zone,
                    candidateZoneIndexes = candidates,
                    recommendedZoneIndex = candidates.first(),
                    theme = theme,
                )
        }
    }

    /** The student explicitly picked a scene to grow next. */
    fun confirmGardenPlacePicker(zoneIndex: Int) {
        viewModelScope.launch {
            _gardenPlacePicker.value ?: return@launch
            EngagementAnalyticsTracker.nextPlacePicked(zoneIndex)
            if (userId.isNotBlank()) {
                gardenRepository.unlockZoneIfNeeded(userId, zoneIndex)
            }
            _gardenPlacePicker.value = null
            showPendingGemMoment()
        }
    }

    /** "Surprise me" / continue — takes the recommended scene so the picker never blocks progress. */
    fun dismissGardenPlacePicker() {
        val picker = _gardenPlacePicker.value
        _gardenPlacePicker.value = null
        viewModelScope.launch {
            if (picker != null && userId.isNotBlank()) {
                EngagementAnalyticsTracker.nextPlaceSurprise(picker.recommendedZoneIndex)
                gardenRepository.unlockZoneIfNeeded(userId, picker.recommendedZoneIndex)
            }
            showPendingGemMoment()
        }
    }

    fun dismissPartialReturnPrompt() {
        _partialReturnPrompt.value = null
        dismissMoment()
        TrialSessionStore.activeTrialItemId = null
        viewModelScope.launch {
            processTrialScreenResume()
        }
    }

    fun continuePartialItem() {
        val item = _partialReturnPrompt.value?.item ?: return
        _partialReturnPrompt.value = null
        dismissMoment()
        prepareLaunch(item)
        _launchTarget.value = item
    }

    fun requestExit(onConfirmExit: () -> Unit) {
        _advanceOverlay.value?.let { overlay ->
            if (overlay.requiresMandatoryClaim && !overlay.mandatoryClaimCompleted) {
                skipMandatoryAd()
            } else {
                onAdvanceFinished()
            }
            return
        }
        if (_partialReturnPrompt.value != null) {
            dismissPartialReturnPrompt()
            return
        }
        val incompleteCount = items.value.count { it.status != PlanTrialItemStatus.DONE }
        if (incompleteCount == 0) {
            onConfirmExit()
            return
        }
        pendingExitAction = onConfirmExit
        viewModelScope.launch {
            buildExitHook(incompleteCount)
        }
    }

    fun confirmExit() {
        _exitHook.value = null
        dismissMoment()
        pendingExitAction?.invoke()
        pendingExitAction = null
    }

    fun dismissExitHook() {
        _exitHook.value = null
        dismissMoment()
        pendingExitAction = null
    }

    private suspend fun buildExitHook(incompleteCount: Int) {
        val incompleteEntities =
            planTrialRepository
                .getTrialItems(userId, dayIndex)
                .filter { it.status != PlanTrialItemStatus.DONE }
        val pendingXp = incompleteEntities.sumOf { EconomyConfig.xpForTrialKind(it.kind) }
        val projection = leagueRepository.projectRankAfterAdditionalXp(userId, pendingXp)
        val mandatoryAdSlots = (incompleteCount + 1) / EconomyConfig.TRIALS_PER_MANDATORY_AD
        val potentialGems = mandatoryAdSlots * EconomyConfig.GEM_TRIAL_MANDATORY_CLAIM

        val leagueLine =
            projection?.let { p ->
                buildString {
                    append(p.rankHint())
                    p.promotionHint()?.let { hint -> append(" · $hint") }
                }
            } ?: TrialCopy.exitLeagueFallback(activeLanguage)

        _exitHook.value =
            TrialExitHookUiState(
                pendingTrialCount = incompleteCount,
                title = TrialCopy.exitHookTitle(activeLanguage, incompleteCount),
                message =
                    TrialCopy.exitHookMessage(
                        languageCode = activeLanguage,
                        potentialGems = potentialGems,
                        trialsPerAd = EconomyConfig.TRIALS_PER_MANDATORY_AD,
                        leagueLine = leagueLine,
                    ),
            )
        showMoment(resolveExitMoment(_exitHook.value!!, projection))
    }

    fun trackMomentPrimary() {
        _moment.value?.let { moment ->
            GamificationAnalyticsTracker.momentPrimary(moment.moment, moment.variantId)
        }
    }

    fun trackMomentSecondary() {
        _moment.value?.let { moment ->
            GamificationAnalyticsTracker.momentSecondary(moment.moment)
        }
    }

    private fun resolveExitMoment(
        hook: TrialExitHookUiState,
        projection: LeagueRankProjection?,
    ): MomentUiModel =
        momentVariantPicker
            .pick(
                moment = MomentType.EXIT_INCOMPLETE,
                tokens =
                    MomentTokens(
                        pending = hook.pendingTrialCount,
                        league = projection?.tier?.leagueTitle() ?: "your league",
                        rank = projection?.projectedRank ?: 0,
                    ),
                languageCode = activeLanguage,
            ).copy(body = hook.message)

    private suspend fun buildPartialReturnIfNeeded(): TrialPartialReturnUiState? {
        val trialItemId = TrialSessionStore.activeTrialItemId ?: return null
        planTrialProgressTracker.reconcileCompletion(trialItemId)
        val entity = planTrialRepository.getTrialItemById(trialItemId) ?: return null
        if (entity.status == PlanTrialItemStatus.DONE) return null
        if (entity.completedCount >= entity.requiredCount) return null

        val remaining = entity.requiredCount - entity.completedCount
        val (title, message) =
            when (entity.kind) {
                PlanTrialItemKind.SIM_AGENT -> TrialCopy.partialReturnSim(activeLanguage)
                PlanTrialItemKind.STUDY,
                PlanTrialItemKind.SIM_URL,
                PlanTrialItemKind.MATH,
                ->
                    TrialCopy.partialReturnStudy(
                        languageCode = activeLanguage,
                        remainingLabel = TrialCopy.knowledgeBitesLabel(activeLanguage, remaining),
                        completed = entity.completedCount,
                        required = entity.requiredCount,
                    )
                else -> TrialCopy.partialReturnDefault(activeLanguage)
            }
        return TrialPartialReturnUiState(
            item = entity.toUi(_languageCode.value),
            title = title,
            message = message,
            remaining = remaining,
        )
    }

    private fun resolveComebackMoment(partial: TrialPartialReturnUiState): MomentUiModel =
        momentVariantPicker
            .pick(
                moment = MomentType.COMEBACK_INCOMPLETE,
                tokens =
                    MomentTokens(
                        bite = partial.item.title,
                        remaining = partial.remaining,
                    ),
                languageCode = activeLanguage,
            ).copy(body = partial.message)

    private suspend fun loadDay() {
        if (userId.isBlank()) return
        // Chapter trial: materialize the chapter's items and title with the chapter name.
        chapterId?.let { chapter ->
            planTrialRepository.ensureChapterTrial(userId, chapter, dayIndex, _languageCode.value)
            examPlanRepository.getPlanDay(userId, dayIndex)?.let { day ->
                _planDay.value = day
            }
            val name = chapterDao.getChapter(chapter)?.getLocalizedName(_languageCode.value)
            if (!name.isNullOrBlank() && _planDayTitle.value != name) {
                _planDayTitle.value = name
            }
            return
        }
        val day =
            examPlanRepository.getPlanDay(userId, dayIndex)
                ?: return
        _planDay.value = day
        if (userId.isNotBlank()) {
            planTrialRepository.ensureTrialItemsForDay(day, _languageCode.value)
        }
    }

    private suspend fun PlanTrialItemEntity.toUi(lang: String): PlanTrialItemUi {
        val kindLabel = TrialCopy.kindLabel(lang, kind)
        val statusLabel = TrialCopy.statusLabel(lang, status)
        val localizedTitle =
            TrialTitleResolver.localizedItemTitle(this, lang, conceptDao, chapterDao)
        val progressLabel =
            if (requiredCount > 1 && status != PlanTrialItemStatus.DONE) {
                TrialCopy.progressLabel(lang, completedCount, requiredCount)
            } else {
                null
            }
        return PlanTrialItemUi(
            id = id,
            title = localizedTitle,
            kind = kind,
            kindLabel = kindLabel,
            status = status,
            statusLabel = statusLabel,
            conceptId = conceptId,
            chapterId = chapterId,
            sourceId = sourceId,
            progressLabel = progressLabel,
            completedCount = completedCount,
            requiredCount = requiredCount,
            carriedFromDayIndex = carriedFromDayIndex,
        )
    }

    private suspend fun checkDeferredCelebration() {
        if (userId.isBlank()) return
        if (_partialReturnPrompt.value != null) return
        if (_moment.value != null) return
        if (_advanceOverlay.value != null) return
        if (pendingAdvanceOverlay != null) return

        ensureGardenCelebrationQueued()

        // Build the XP/gems reward for the latest just-completed item (if any). Doing this even when
        // a garden plant celebration is queued is the key fix: the plant, the gem moment, and the
        // XP/gems advance overlay now play as one chain in the SAME session — instead of the plant
        // showing now and the reward being split off to the next app launch.
        buildDeferredRewardIfNeeded()

        if (gardenMomentCoordinator.pending.value != null || pendingGemMoment != null) {
            beginPostCompletionCelebrations()
        }
        revealAdvanceOverlayIfReady()
    }

    /**
     * Awards XP/gems for the most recent uncelebrated completed item and stages the gem moment and
     * the advance overlay. No-op while an item is still in progress, or when there is nothing new to
     * reward. Kept separate from [checkDeferredCelebration] so the plant celebration and this reward
     * are staged together and then played as one continuous sequence.
     */
    private suspend fun buildDeferredRewardIfNeeded() {
        TrialSessionStore.activeTrialItemId?.let { activeId ->
            val active = planTrialRepository.getTrialItemById(activeId) ?: return@let
            if (active.status != PlanTrialItemStatus.DONE ||
                active.completedCount < active.requiredCount
            ) {
                return
            }
        }

        val completed = planTrialRepository.getLatestUncelebratedDone(userId, dayIndex) ?: return
        if (pendingAdvanceOverlay?.completedItemId == completed.id) return
        if (_advanceOverlay.value?.completedItemId == completed.id) return
        planTrialProgressTracker.reconcileCompletion(completed.id)
        val fresh = planTrialRepository.getTrialItemById(completed.id) ?: return
        if (fresh.status != PlanTrialItemStatus.DONE) return
        if (fresh.completedCount < fresh.requiredCount) return
        val reward =
            gamificationRewardService.awardTrialItemXp(
                studentId = userId,
                trialItemId = fresh.id,
                kind = fresh.kind,
                language = activeLanguage,
            )
        val completions = trialRewardService.incrementCompletionsSinceMandatoryAd(userId, fresh.id)
        val requiresMandatory = trialRewardService.requiresMandatoryAdClaim(completions)
        val doubleXpAmount = EconomyConfig.xpForTrialKind(fresh.kind)
        val nextEntity =
            planTrialRepository.getNextIncompleteAfterCompleted(
                studentId = userId,
                dayIndex = dayIndex,
                completedItem = fresh,
            )
        val nextUi = nextEntity?.toUi(_languageCode.value)
        val kindMoment =
            when {
                nextUi == null -> MomentType.DAY_COMPLETED
                fresh.kind == PlanTrialItemKind.STUDY -> MomentType.STUDY_COMPLETED
                else -> MomentType.SIM_COMPLETED
            }
        pendingGemMoment =
            momentVariantPicker.pick(
                kindMoment,
                MomentTokens(gems = reward?.gemsEarned ?: 0, xp = reward?.xpEarned ?: 0),
                languageCode = activeLanguage,
            )
        GamificationAnalyticsTracker.trialItemComplete(
            kind = fresh.kind,
            chapterId = fresh.chapterId,
            dayIndex = dayIndex,
            attempts = fresh.completedCount.coerceAtLeast(1),
        )
        if (fresh.kind == PlanTrialItemKind.STUDY) {
            GamificationAnalyticsTracker.studyComplete(fresh.conceptId, fresh.chapterId)
        }
        if (nextUi == null) {
            val durationMs =
                if (trialDayStartMs > 0L) System.currentTimeMillis() - trialDayStartMs else 0L
            GamificationAnalyticsTracker.trialDayComplete(
                dayIndex = dayIndex,
                dayType = _planDay.value?.dayType.orEmpty(),
                items = items.value.size,
                durationMs = durationMs,
            )
        }
        pendingAdvanceOverlay =
            TrialAdvanceUiState(
                completedItemId = fresh.id,
                completedItemKind = fresh.kind,
                title =
                    if (nextUi != null) {
                        TrialCopy.advanceNiceWork(activeLanguage)
                    } else {
                        TrialCopy.advanceDayComplete(activeLanguage)
                    },
                subtitle =
                    if (nextUi != null) {
                        TrialCopy.advanceUpNext(activeLanguage, nextUi.kindLabel, nextUi.title)
                    } else {
                        TrialCopy.advanceAllDone(activeLanguage)
                    },
                launchNext = nextUi,
                xpEarned = reward?.xpEarned ?: 0,
                gemsEarned = reward?.gemsEarned ?: 0,
                xpBarFrom = reward?.xpBarFrom ?: 0f,
                xpBarTo = reward?.xpBarTo ?: 0f,
                weeklyXpTotal = reward?.weeklyXpTotal ?: 0,
                requiresMandatoryClaim = requiresMandatory,
                mandatoryGemsReward = EconomyConfig.GEM_TRIAL_MANDATORY_CLAIM,
                mandatoryClaimCompleted = !requiresMandatory,
                mandatoryBatchIndex = completions / EconomyConfig.TRIALS_PER_MANDATORY_AD,
                doubleXpAmount = doubleXpAmount,
                adReady = trialRewardService.isRewardedAdReady(),
            )
    }
}
