package com.ncert7.aitutorandlab.ui.screens.conceptscreen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.domain.examplan.PlanTrialProgressTracker
import com.ncert7.aitutorandlab.domain.examplan.SimulationTrialThresholds
import com.ncert7.aitutorandlab.domain.examplan.TrialSessionStore
import com.ncert7.aitutorandlab.domain.garden.GardenMomentCoordinator
import com.ncert7.aitutorandlab.domain.progress.ProgressEventTracker
import com.ncert7.aitutorandlab.repository.ChapterRepository
import com.ncert7.aitutorandlab.repository.ConceptRepository
import com.ncert7.aitutorandlab.service.analytics.SimulationAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.SimulationInteraction
import com.ncert7.aitutorandlab.service.sync.DataSyncService
import com.ncert7.aitutorandlab.ui.screens.conceptscreen.dataclass.ConceptScreenState
import com.ncert7.aitutorandlab.ui.screens.conceptscreen.components.SimulationTrialPromptKind
import com.ncert7.aitutorandlab.ui.screens.conceptscreen.SimulationViewerTiming
import com.ncert7.aitutorandlab.utils.StreakManager
import com.ncert7.aitutorandlab.utils.TrialCopy
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import com.ncert7.aitutorandlab.utils.isKannada
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLEncoder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lightweight ViewModel used by [com.ncert7.aitutorandlab.ui.screens.conceptscreen.components.ConceptSimulationViewer]
 * to track simulation URL progress.
 * Delegates to [ProgressEventTracker] which handles the full chain:
 *   1. Write to progress table
 *   2. Recalculate chapter progress
 *   3. Record streak activity
 */
@HiltViewModel
class ConceptSimulationViewModel @Inject constructor(
    private val conceptRepository: ConceptRepository,
    private val chapterRepository: ChapterRepository,
    private val progressEventTracker: ProgressEventTracker,
    private val planTrialProgressTracker: PlanTrialProgressTracker,
    private val streakManager: StreakManager,
    private val sharedPrefs: SharedPreferenceUtils,
    private val gardenMomentCoordinator: GardenMomentCoordinator,
) : ViewModel() {

    private val _state = MutableStateFlow(ConceptScreenState())
    val state: StateFlow<ConceptScreenState> = _state.asStateFlow()

    // A plant earned mid-sim must not pop over the running simulation. For FREE-browse sims we mute
    // the app-wide celebration host while the viewer is on screen; it surfaces the plant once, on
    // the next safe screen. Plan-launched sims (activeTrialItemId set) are already handled by the
    // Plan screen's own suppression + the growGarden defer, so we don't touch the flag for them
    // (avoids a shared-flag tug-of-war during the sim → Plan handoff).
    fun onViewerVisible() {
        if (TrialSessionStore.activeTrialItemId == null) {
            gardenMomentCoordinator.setGlobalHostSuppressed(true)
        }
    }

    fun onViewerHidden() {
        if (TrialSessionStore.activeTrialItemId == null) {
            gardenMomentCoordinator.setGlobalHostSuppressed(false)
        }
    }

    private val _isAdCheckPending = MutableStateFlow(true)
    val isAdCheckPending: StateFlow<Boolean> = _isAdCheckPending.asStateFlow()

    private val _simulationTitle = MutableStateFlow("")
    val simulationTitle: StateFlow<String> = _simulationTitle.asStateFlow()

    private val _simulationUrl = MutableStateFlow("")
    val simulationUrl: StateFlow<String> = _simulationUrl.asStateFlow()

    private val _progressUpdateTrigger = MutableStateFlow(0)
    val progressUpdateTrigger: StateFlow<Int> = _progressUpdateTrigger.asStateFlow()

    private val _trialThresholds = MutableStateFlow<SimulationTrialThresholds?>(null)
    val trialThresholds: StateFlow<SimulationTrialThresholds?> = _trialThresholds.asStateFlow()

    private val _trialPrompt = MutableStateFlow<SimulationTrialPromptKind?>(null)
    val trialPrompt: StateFlow<SimulationTrialPromptKind?> = _trialPrompt.asStateFlow()

    companion object {
        private const val TAG = "ConceptSimulationVM"
        const val TRIAL_EXPLORE_PROMPT_MS = SimulationViewerTiming.TRIAL_OVERLAY_MS
        /** In-sim taps required before chapter progress counts a URL sim as completed. */
        const val MIN_INTERACTIONS_FOR_CHAPTER_PROGRESS = 7

        /**
         * TEMP kill-switch. When false, completing a sim by *free browsing* (not from the Plan)
         * does nothing — no chapter-progress bump, no XP/gems, no garden plant/celebration. Only
         * the streak-on-open (recorded elsewhere) remains. Plan-trial sims are unaffected.
         * Flip back to true to restore free-browse chapter progress + rewards.
         */
        const val CHAPTER_PROGRESS_FROM_FREE_BROWSE_ENABLED = false
    }

    private var timeExplorePromptShown = false
    /** Captured when the page loads so proceed still works if the store is cleared. */
    private var capturedTrialItemId: Long? = null

    fun captureTrialItemId() {
        capturedTrialItemId = TrialSessionStore.activeTrialItemId ?: capturedTrialItemId
    }

    fun markSimulationUrlCompleted(conceptId: String) {
        if (conceptId.isBlank()) return
        viewModelScope.launch {
            val studentId = sharedPrefs.getUserId() ?: run {
                DebugLogger.errorLog(TAG, "No studentId — cannot mark simulation URL completed")
                return@launch
            }
            // Trial sims skip chapter progress / XP, but still count for the daily streak.
            if (TrialSessionStore.activeTrialItemId != null) {
                DebugLogger.debugLog(TAG, "Trial mode — streak only (URL completion via click count)")
                streakManager.recordLearningActivityForUser(studentId)
                return@launch
            }
            // TEMP: free-browse chapter progress + rewards are disabled. No chapter %, XP, or plant.
            if (!CHAPTER_PROGRESS_FROM_FREE_BROWSE_ENABLED) {
                DebugLogger.debugLog(TAG, "Free-browse chapter progress DISABLED — no completion/XP/plant for $conceptId")
                return@launch
            }
            val language = getCurrentLanguageCode()
            progressEventTracker.markSimulationUrlCompleted(studentId, conceptId, language)
            DebugLogger.debugLog(TAG, " Simulation URL completed tracked: conceptId=$conceptId [$language]")
        }
    }

    /**
     * Resolve where the header "Next" button should go, as a nav route string (or null if there is
     * nothing after this — the caller falls back to leaving the sim). Order:
     *   1. The next simulation in this chapter (by orderIndex).
     *   2. If this is the last sim, the first still-unfinished simulation in this chapter.
     *   3. If every sim here is done, the NEXT chapter — opened at its trial/plan screen, which
     *      surfaces that chapter's first unfinished aspect (study / simulation / revision).
     * Runs off the main thread (DB reads); returns on the calling coroutine.
     */
    suspend fun resolveNextRoute(
        conceptId: String,
        subjectName: String,
        chapterName: String,
    ): String? {
        if (conceptId.isBlank()) return null
        val language = getCurrentLanguageCode()
        val studentId = sharedPrefs.getUserId()
        val chapter = conceptRepository.getChapterForConcept(conceptId) ?: return null
        val sims = conceptRepository.getSimulationConceptsForChapter(chapter.chapterId, language)
        val idx = sims.indexOfFirst { it.conceptId == conceptId }

        // 1. Next simulation in this chapter.
        if (idx in 0 until sims.lastIndex) {
            simRoute(sims[idx + 1], subjectName, chapterName, language)?.let { return it }
        }

        // 2. At the end → first unfinished simulation in this chapter (skip the current one).
        if (idx >= 0 && !studentId.isNullOrBlank()) {
            for (s in sims) {
                if (s.conceptId == conceptId) continue
                val done = conceptRepository
                    .getProgress(studentId, "SIMULATION", s.conceptId, language)?.status == "COMPLETED"
                if (!done) simRoute(s, subjectName, chapterName, language)?.let { return it }
            }
        }

        // 3. Everything here is done → next chapter's plan (its first unfinished aspect).
        val chapters = chapterRepository.getChaptersForSubject(chapter.subjectId)
        val cIdx = chapters.indexOfFirst { it.chapterId == chapter.chapterId }
        if (cIdx in 0 until chapters.lastIndex) {
            return "chapter_trial/${chapters[cIdx + 1].chapterId}"
        }
        return null
    }

    /** Build the concept_sim_view route for a target simulation concept (mirrors LearningNavigator). */
    private fun simRoute(
        concept: com.ncert7.aitutorandlab.data.local.entities.ConceptEntity,
        subjectName: String,
        chapterName: String,
        language: String,
    ): String? {
        val kn = language.equals("kn", ignoreCase = true)
        val url = (if (kn) concept.simulationUrlKannada else concept.simulationUrl)
            ?.takeIf { it.isNotBlank() }
            ?: concept.simulationUrl?.takeIf { it.isNotBlank() }
            ?: return null
        val title = (if (kn) concept.conceptNameKannada else concept.conceptName)
            .ifBlank { concept.conceptName }
        fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
        val cid = concept.conceptId.ifBlank { "empty" }
        return "concept_sim_view/${enc(url)}/${enc(title.replace("/", "-"))}/${enc(cid)}/${enc(subjectName)}/${enc(chapterName)}"
    }

    fun syncTrialSimClickCount(clickCount: Int) {
        val trialItemId = TrialSessionStore.activeTrialItemId ?: return
        viewModelScope.launch {
            planTrialProgressTracker.syncToCount(trialItemId, clickCount)
        }
    }

    suspend fun flushTrialSimClickCount(clickCount: Int) {
        val trialItemId = TrialSessionStore.activeTrialItemId ?: return
        planTrialProgressTracker.syncToCount(trialItemId, clickCount)
        planTrialProgressTracker.reconcileCompletion(trialItemId)
    }

    fun onHtmlInteractionBudget(htmlBudget: Int) {
        val trialItemId = TrialSessionStore.activeTrialItemId ?: return
        viewModelScope.launch {
            val thresholds = planTrialProgressTracker.applyHtmlInteractionBudget(trialItemId, htmlBudget)
            _trialThresholds.value = thresholds
            DebugLogger.debugLog(
                TAG,
                "Trial sim thresholds — budget=$htmlBudget, complete@${thresholds.completionAt}",
            )
        }
    }

    /** Called only after [SimulationViewerTiming.TRIAL_OVERLAY_MS] on the page — never on click/coach end. */
    fun showTimeBasedExplorePromptIfNeeded() {
        if (timeExplorePromptShown || _trialPrompt.value != null) {
            DebugLogger.debugLog(TAG, "Trial explore prompt skipped — already shown")
            return
        }
        if (remainingOverlayDelayMs() > 0L) {
            DebugLogger.debugLog(
                TAG,
                "Trial explore prompt skipped — ${remainingOverlayDelayMs() / 1000}s left on time gate",
            )
            return
        }
        timeExplorePromptShown = true
        _trialPrompt.value = SimulationTrialPromptKind.TIME_EXPLORATION
        DebugLogger.debugLog(TAG, "Trial explore prompt shown after 3 minutes")
    }

    /** Milliseconds left before the second (footer / description) narration plays (survives rotation). */
    fun remainingFooterDelayMs(): Long {
        val started = _viewerSession.value.startedAtMs
        if (started <= 0L) return SimulationViewerTiming.FOOTER_TTS_MS
        val elapsed = System.currentTimeMillis() - started
        return (SimulationViewerTiming.FOOTER_TTS_MS - elapsed).coerceAtLeast(0L)
    }

    /** Milliseconds left before the proceed overlay should appear (survives rotation). */
    fun remainingOverlayDelayMs(): Long {
        val started = _viewerSession.value.startedAtMs
        if (started <= 0L) return SimulationViewerTiming.TRIAL_OVERLAY_MS
        val elapsed = System.currentTimeMillis() - started
        return (SimulationViewerTiming.TRIAL_OVERLAY_MS - elapsed).coerceAtLeast(0L)
    }

    data class SimViewerSession(
        val url: String = "",
        val startedAtMs: Long = 0L,
        val pageReadyHandled: Boolean = false,
        val introHandled: Boolean = false,
        val footerHandled: Boolean = false,
        val guideUnlocked: Boolean = false,
        val harvestJson: String? = null,
        val guideStepIdx: Int = 0,
        val guideDismissed: Boolean = false,
        /** Highest guide step already read aloud — survives rotation so TTS isn't repeated. */
        val narratedStepIdx: Int = -1,
        /** True once the brief intro walkthrough is done and the coach is in free-play adaptive mode. */
        val coachAdaptive: Boolean = false,
        /** True once the learner has done enough (interactions/time) and the coach has eased off. */
        val coachEasedOff: Boolean = false,
        /** Wall-clock + interaction baseline captured when adaptive mode began, for the ease-off rule. */
        val adaptiveStartMs: Long = 0L,
        val adaptiveStartInteractions: Int = 0,
    )

    private val _viewerSession = MutableStateFlow(SimViewerSession())
    val viewerSession: StateFlow<SimViewerSession> = _viewerSession.asStateFlow()

    /** Starts or resumes a viewer session; resets state only when the URL changes. */
    fun beginViewerSession(url: String): Boolean {
        if (url.isBlank()) return false
        if (_viewerSession.value.url == url) return false
        _viewerSession.value =
            SimViewerSession(
                url = url,
                startedAtMs = System.currentTimeMillis(),
            )
        resetTrialPromptState()
        DebugLogger.debugLog(TAG, "New simulation viewer session: $url")
        return true
    }

    fun shouldHandlePageReady(url: String): Boolean =
        _viewerSession.value.url == url && !_viewerSession.value.pageReadyHandled

    fun markPageReadyHandled(url: String) {
        if (_viewerSession.value.url != url) return
        _viewerSession.value = _viewerSession.value.copy(pageReadyHandled = true)
    }

    fun shouldHandleIntro(url: String): Boolean =
        _viewerSession.value.url == url && !_viewerSession.value.introHandled

    fun markIntroHandled(url: String) {
        if (_viewerSession.value.url != url) return
        _viewerSession.value = _viewerSession.value.copy(introHandled = true)
    }

    fun shouldHandleFooter(url: String): Boolean =
        _viewerSession.value.url == url && !_viewerSession.value.footerHandled

    fun markFooterHandled(url: String) {
        if (_viewerSession.value.url != url) return
        _viewerSession.value = _viewerSession.value.copy(footerHandled = true)
    }

    fun storeHarvestJson(url: String, json: String) {
        if (_viewerSession.value.url != url || _viewerSession.value.harvestJson != null) return
        _viewerSession.value = _viewerSession.value.copy(harvestJson = json)
    }

    fun unlockGuide(url: String) {
        if (_viewerSession.value.url != url || _viewerSession.value.guideUnlocked) return
        _viewerSession.value = _viewerSession.value.copy(guideUnlocked = true)
        DebugLogger.debugLog(TAG, "Guided coach unlocked after intro TTS")
    }

    fun setGuideStepIdx(url: String, idx: Int) {
        if (_viewerSession.value.url != url) return
        _viewerSession.value = _viewerSession.value.copy(guideStepIdx = idx)
    }

    /** Records that [idx] has been narrated so it isn't re-spoken on recomposition/rotation. */
    fun markStepNarrated(url: String, idx: Int) {
        if (_viewerSession.value.url != url || _viewerSession.value.narratedStepIdx == idx) return
        _viewerSession.value = _viewerSession.value.copy(narratedStepIdx = idx)
    }

    fun dismissGuide(url: String) {
        if (_viewerSession.value.url != url) return
        _viewerSession.value = _viewerSession.value.copy(guideDismissed = true)
    }

    /**
     * Ends the brief intro walkthrough and hands over to free-play adaptive coaching. The
     * baseline time + interaction count are stamped here so the ease-off rule measures only
     * what the learner does *after* the intro. No-op once already adaptive (survives rotation).
     */
    fun enterAdaptiveCoach(url: String, nowMs: Long, interactions: Int) {
        val s = _viewerSession.value
        if (s.url != url || s.coachAdaptive) return
        _viewerSession.value = s.copy(
            coachAdaptive = true,
            adaptiveStartMs = nowMs,
            adaptiveStartInteractions = interactions,
        )
        DebugLogger.debugLog(TAG, "Adaptive coach entered @${nowMs} interactions=$interactions")
    }

    /** Marks the coach as having eased off (learner has done ~enough); keeps it out of the way. */
    fun easeOffCoach(url: String) {
        val s = _viewerSession.value
        if (s.url != url || s.coachEasedOff) return
        _viewerSession.value = s.copy(coachEasedOff = true)
        DebugLogger.debugLog(TAG, "Adaptive coach eased off")
    }

    /**
     * Restarts the coach from the top — used when the learner switches coaching style (v1/v2/v3)
     * so the new style can be observed from the beginning. Keeps the guide unlocked + the harvested
     * structure so it re-arms immediately.
     */
    fun restartCoach(url: String) {
        val s = _viewerSession.value
        if (s.url != url) return
        _viewerSession.value = s.copy(
            guideStepIdx = 0,
            guideDismissed = false,
            narratedStepIdx = -1,
            coachAdaptive = false,
            coachEasedOff = false,
            adaptiveStartMs = 0L,
            adaptiveStartInteractions = 0,
        )
        DebugLogger.debugLog(TAG, "Coach restarted for style switch")
    }

    fun clearViewerSession() {
        _viewerSession.value = SimViewerSession()
    }

    fun resetTrialPromptState() {
        timeExplorePromptShown = false
        _trialPrompt.value = null
        capturedTrialItemId = null
    }

    /** Full reset when leaving the viewer entirely. */
    fun resetViewerSession() {
        resetTrialPromptState()
        clearViewerSession()
    }

    fun dismissTrialPromptContinueExploring() {
        _trialPrompt.value = null
    }

    fun clearTrialPrompt() {
        _trialPrompt.value = null
    }

    /**
     * Soft exit from the time-based "Next item" overlay.
     * Persists real interaction credit only — does **not** force the item DONE.
     * Forcing GE here caused "Level cleared!" / XP celebrations when the learner
     * had barely touched the sim (or not at all).
     */
    /**
     * @return true if, after syncing real interaction credit and reconciling, the trial item is
     * actually DONE. Callers use this to decide whether to soft-proceed (skip celebration) or let the
     * normal resume run the celebration — a genuine DONE must NOT be soft-proceeded past.
     */
    suspend fun completeTrialSimProceed(clickCount: Int): Boolean {
        val trialItemId = TrialSessionStore.activeTrialItemId ?: capturedTrialItemId ?: return false
        planTrialProgressTracker.syncToCount(trialItemId, clickCount)
        planTrialProgressTracker.reconcileCompletion(trialItemId)
        return planTrialProgressTracker.isDone(trialItemId)
    }

    suspend fun keyConceptFallback(conceptId: String): String? {
        if (conceptId.isBlank()) return null
        return conceptRepository.getConcept(conceptId)?.description?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun trialPromptCopy(kind: SimulationTrialPromptKind, languageCode: String): Pair<String, String> =
        when (kind) {
            SimulationTrialPromptKind.TIME_EXPLORATION ->
                TrialCopy.simTimeExplorePrompt(languageCode)
        }

    fun initializeSimulationWithAdCheck(
        conceptId: String,
        simulationUrl: String? = null,
        simulationTitle: String? = null
    ) {
        if (simulationUrl != null && simulationTitle != null) {
            _simulationTitle.value = simulationTitle
            _simulationUrl.value = simulationUrl
            _isAdCheckPending.value = false
            DebugLogger.debugLog(
                TAG,
                "initializeSimulationWithAdCheck (external) for $conceptId: title=$simulationTitle",
            )
            return
        }

        _isAdCheckPending.value = true
        viewModelScope.launch {
            try {
                val concept = _state.value.concepts.find { it.id == conceptId }
                _simulationTitle.value = concept?.name ?: "Simulation"

                if (concept == null) {
                    DebugLogger.errorLog(TAG, "Concept not found in state for ID: $conceptId")
                    _simulationUrl.value = ""
                    return@launch
                }

                val selectedUrl = getSelectedSimulationUrl(concept.simulationUrl, concept.simulationUrlKannada)
                _simulationUrl.value = selectedUrl ?: ""

                DebugLogger.debugLog(
                    TAG,
                    "initializeSimulationWithAdCheck (state search) for $conceptId: url=${_simulationUrl.value}",
                )
            } catch (e: Exception) {
                DebugLogger.errorLog(TAG, "Error initializing simulation: ${e.message} | ${e.stackTraceToString()}")
            } finally {
                _isAdCheckPending.value = false
            }
        }
    }

    private fun getSelectedSimulationUrl(
        englishUrl: String?,
        kannadaUrl: String?
    ): String? {
        return if (isKannada()) {
            kannadaUrl?.takeIf { it.isNotBlank() && it != "Not found" }
        } else {
            englishUrl?.takeIf { it.isNotBlank() && it != "Not found" }
        }
    }

    /**
     * Page finished loading — streak only. Chapter % must not advance on open alone.
     */
    fun onSimulationOpened(conceptId: String) {
        viewModelScope.launch {
            val studentId = sharedPrefs.getUserId() ?: return@launch
            if (conceptId.isEmpty()) return@launch
            DebugLogger.debugLog(TAG, "Simulation opened (streak only): $conceptId")
            streakManager.recordLearningActivityForUser(studentId) { newStreak ->
                DebugLogger.debugLog(TAG, "Open streak touch → $newStreak")
            }
        }
    }

    /**
     * Record chapter progress when the learner actually engaged
     * (coach concluded, or at least [MIN_INTERACTIONS_FOR_CHAPTER_PROGRESS] taps).
     */
    fun maybeMarkCompletedAfterEngagement(conceptId: String, interactionCount: Int) {
        if (interactionCount < MIN_INTERACTIONS_FOR_CHAPTER_PROGRESS) {
            DebugLogger.debugLog(
                TAG,
                "Skip chapter progress — only $interactionCount taps (need $MIN_INTERACTIONS_FOR_CHAPTER_PROGRESS)",
            )
            return
        }
        val trialItemId = TrialSessionStore.activeTrialItemId ?: capturedTrialItemId
        if (trialItemId == null) {
            // Non-trial session: the engagement threshold is enough to credit the concept.
            markSimulationCompleted(conceptId)
            return
        }
        // Trial session: only credit the concept (XP + gamification overlay) once the trial task
        // itself is genuinely DONE — i.e. completed with the engagement threshold met — so a
        // barely-touched trial sim doesn't award concept XP or stack an overlay on the trial reward.
        viewModelScope.launch {
            if (planTrialProgressTracker.isDone(trialItemId)) {
                markSimulationCompleted(conceptId)
            } else {
                DebugLogger.debugLog(TAG, "Trial concept credit deferred — item $trialItemId not DONE yet")
            }
        }
    }

    fun markSimulationCompleted(conceptId: String) {
        viewModelScope.launch {
            // Free-browse (no trial item) chapter progress + rewards are temporarily disabled:
            // no chapter %, XP, or garden plant. Plan-trial crediting (trialItemId set) still runs.
            val trialItemId = TrialSessionStore.activeTrialItemId ?: capturedTrialItemId
            if (trialItemId == null && !CHAPTER_PROGRESS_FROM_FREE_BROWSE_ENABLED) {
                DebugLogger.debugLog(TAG, "Free-browse chapter progress DISABLED — skipping $conceptId")
                return@launch
            }
            try {
                val studentId = sharedPrefs.getUserId() ?: ""
                val language = getCurrentLanguageCode()

                DebugLogger.debugLog(
                    TAG,
                    "🔄 markSimulationCompleted called for conceptId: $conceptId, studentId: $studentId [$language]"
                )

                if (studentId.isEmpty() || conceptId.isEmpty()) {
                    DebugLogger.errorLog(
                        TAG,
                        " Failed to mark simulation completed - studentId: $studentId, conceptId: $conceptId"
                    )
                    return@launch
                }

                DebugLogger.debugLog(TAG, "📍 About to call progressEventTracker.markSimulationUrlCompleted with language=$language")
                // ProgressEventTracker already calls streakRepository.recordActivity — no second streak path.
                progressEventTracker.markSimulationUrlCompleted(studentId, conceptId, language)

                DebugLogger.debugLog(
                    TAG,
                    "✅ Simulation URL marked as COMPLETED for concept: $conceptId [$language] - Progress bars should update!"
                )

                val progress = conceptRepository.getProgress(studentId, "SIMULATION", conceptId, language)
                DebugLogger.debugLog(
                    TAG,
                    "🔍 Verification: Progress for SIMULATION/$conceptId/$language = ${progress?.status ?: "NOT FOUND"} (progress was ${if (progress != null) "FOUND" else "NOT FOUND"})"
                )

                SimulationAnalyticsTracker.trackSimulationComplete(
                    conceptId = conceptId,
                    interaction = SimulationInteraction.URL
                )

                if (progress != null) {
                    DebugLogger.debugLog(TAG, "📤 Syncing progress to Firestore for progressId=${progress.progressId}")
                    DataSyncService.syncProgressUpdate(progress.progressId, studentId)
                } else {
                    DebugLogger.errorLog(TAG, "⚠️ Progress was null after marking - sync skipped")
                }

                _progressUpdateTrigger.value = _progressUpdateTrigger.value + 1
                DebugLogger.debugLog(TAG, "🔄 UI recomposition triggered: ${_progressUpdateTrigger.value}")
            } catch (e: Exception) {
                DebugLogger.errorLog(
                    TAG,
                    " Error marking simulation completed: ${e.message} | ${e.stackTraceToString()}"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
