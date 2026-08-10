package com.ncert7.aitutorandlab.ui.screens.conceptscreen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.local.entities.ProgressEntity
import com.ncert7.aitutorandlab.domain.progress.model.ProgressStatus
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.repository.ChapterRepository
import com.ncert7.aitutorandlab.repository.ConceptRepository
import com.ncert7.aitutorandlab.repository.StudentLocalRepository
import com.ncert7.aitutorandlab.repository.SubjectRepository
import com.ncert7.aitutorandlab.ui.models.ConceptUiModel
import com.ncert7.aitutorandlab.ui.screens.conceptscreen.dataclass.ConceptScreenState
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import com.ncert7.aitutorandlab.utils.getLocalizedName
import com.ncert7.aitutorandlab.utils.isKannadaLanguage
import com.ncert7.aitutorandlab.utils.normalizeLanguageCode
import com.ncert7.aitutorandlab.data.local.dao.ProgressDao
import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.domain.progress.ProgressEventTracker
import com.ncert7.aitutorandlab.domain.progress.model.ProgressStatus as DomainProgressStatus
import com.ncert7.aitutorandlab.service.analytics.SimulationAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.SimulationInteraction
import com.ncert7.aitutorandlab.service.analytics.SimulationSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PendingNavigation(
    val route: String,
    val isDirect: Boolean = false,
    val simulationUrl: String? = null,
    val simulationTitle: String? = null,
    val conceptId: String? = null,
    val simulationId: String? = null
)

@HiltViewModel
class ConceptViewModel @Inject constructor(
    private val conceptRepository: ConceptRepository,
    private val chapterRepository: ChapterRepository,
    private val subjectRepository: SubjectRepository,
    private val progressEventTracker: ProgressEventTracker,
    private val progressDao: ProgressDao,
    private val sharedPrefs: SharedPreferenceUtils
) : ViewModel() {

    private val _state = MutableStateFlow(ConceptScreenState())
    val state: StateFlow<ConceptScreenState> = _state.asStateFlow()

    private val _pendingNavigation = MutableStateFlow<PendingNavigation?>(null)
    val pendingNavigation: StateFlow<PendingNavigation?> = _pendingNavigation.asStateFlow()

    private val _showAdDialog = MutableStateFlow(false)
    val showAdDialog: StateFlow<Boolean> = _showAdDialog.asStateFlow()

    private var pendingAfterAd: PendingNavigation? = null

    fun onSimulationOpened(simId: String, conceptId: String) {
        viewModelScope.launch {
            val pending = PendingNavigation(
                route = "simulation_agent",
                simulationId = simId,
                conceptId = conceptId,
                isDirect = true
            )
            val needsAd = com.ncert7.aitutorandlab.service.ads.ClickAdGate.shouldShowAdBeforeNextClick()
            SimulationAnalyticsTracker.trackSimulationClickAndWait(
                conceptId = conceptId,
                interaction = SimulationInteraction.AGENT,
                source = SimulationSource.CONCEPT_LIST
            )
            DebugLogger.debugLog("ConceptVM", "Simulation Agent Clicked: simId=$simId, conceptId=$conceptId")
            if (needsAd) {
                pendingAfterAd = pending
                _showAdDialog.value = true
            } else {
                _pendingNavigation.value = pending
            }
        }
    }

    fun onSimulationUrlOpened(title: String, url: String, conceptId: String) {
        viewModelScope.launch {
            val pending = PendingNavigation(
                route = "concept_sim_view",
                simulationUrl = url,
                simulationTitle = title,
                conceptId = conceptId,
                isDirect = true
            )
            val needsAd = com.ncert7.aitutorandlab.service.ads.ClickAdGate.shouldShowAdBeforeNextClick()
            SimulationAnalyticsTracker.trackSimulationClickAndWait(
                conceptId = conceptId,
                interaction = SimulationInteraction.URL,
                source = SimulationSource.CONCEPT_LIST
            )
            if (needsAd) {
                pendingAfterAd = pending
                _showAdDialog.value = true
            } else {
                _pendingNavigation.value = pending
            }
        }
    }

    fun dismissAdAndNavigate() {
        _showAdDialog.value = false
        // Ad was shown → reset the in-sim interaction counter that gates the next ad.
        com.ncert7.aitutorandlab.service.ads.ClickAdGate.consumeAd()
        pendingAfterAd?.let { _pendingNavigation.value = it }
        pendingAfterAd = null
    }

    fun markAdShown() {
        _pendingNavigation.value?.let { nav ->
            _pendingNavigation.value = nav.copy(isDirect = true)
        }
    }

    fun clearPendingNavigation() {
        _pendingNavigation.value = null
    }

    fun loadConcepts(chapterId: String, type: String, language: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val studentId = sharedPrefs.getUserId() ?: ""
                val chapter = chapterRepository.getChapter(chapterId)
                val subject = chapter?.let { subjectRepository.getSubject(it.subjectId) }
                val classLevel = 7 // Force class 7 syllabus display
                val lang = normalizeLanguageCode(language)

                DebugLogger.debugLog("ConceptVM", " Loading concepts: chapterId=$chapterId, type=$type, language=$language")

                // Load concepts based on type using specialized repository methods
                val concepts = when {
                    // Trial view: lessons AND simulations for the chapter, merged so both
                    // sit together and any can be picked and started.
                    type.equals("TRIAL", ignoreCase = true) -> {
                        val study = conceptRepository.getStudyConceptsForChapter(chapterId)
                        val sims = conceptRepository.getSimulationConceptsForChapter(chapterId, lang)
                        DebugLogger.debugLog("ConceptVM", " Loaded TRIAL: ${study.size} study + ${sims.size} sim concepts")
                        study + sims
                    }
                    type.equals("SIMULATION", ignoreCase = true) -> {
                        val simConcepts = conceptRepository.getSimulationConceptsForChapter(chapterId, lang)
                        DebugLogger.debugLog("ConceptVM", " Loaded SIMULATION concepts: ${simConcepts.size}")
                        simConcepts
                    }
                    type.equals("MATH PROBLEM", ignoreCase = true) -> {
                        val mathConcepts = conceptRepository.getMathProblemConceptsForChapter(chapterId)
                        DebugLogger.debugLog("ConceptVM", " Loaded MATH PROBLEM concepts: ${mathConcepts.size}")
                        mathConcepts
                    }
                    type.equals("STUDY", ignoreCase = true) -> {
                        val studyConcepts = conceptRepository.getStudyConceptsForChapter(chapterId)
                        DebugLogger.debugLog("ConceptVM", " Loaded STUDY concepts: ${studyConcepts.size}")
                        studyConcepts
                    }
                    else -> {
                        DebugLogger.warnLog("ConceptVM", " Unknown type: $type, returning empty list")
                        emptyList()
                    }
                }

                if (concepts.isEmpty()) {
                    DebugLogger.warnLog("ConceptVM", " No concepts found for chapter=$chapterId, type=$type")
                }

                val subjectId = chapter?.subjectId ?: ""

                // Reactively collect progress changes for this chapter and overall chapter progress
                // NOTE: getAllProgress() returns all progress entries, but we filter by language in determineSimulationStatus()
                val progressFlow = progressDao.getAllProgress(studentId, AppConfig.APP_NAME)
                val chapterProgressFlow = progressDao.getChapterWiseProgressFlow(studentId, subjectId, lang, AppConfig.APP_NAME)

                combine(progressFlow, chapterProgressFlow) { allProgress, progressSummaries ->
                    DebugLogger.debugLog("ConceptVM", " PROGRESS FLOW TRIGGERED - Language=$lang, Progress entries=${allProgress.size}")
                    DebugLogger.debugLog("ConceptVM", "  Filtering progress for language=$lang only")

                    val conceptUiModels = concepts.mapIndexed { index, concept ->
                        val displayName = when {
                            type.equals("MATH PROBLEM", ignoreCase = true) -> {
                                if (isKannadaLanguage(lang)) {
                                    concept.problemTopicNameKn.ifEmpty { concept.conceptNameKannada.ifBlank { concept.conceptName } }
                                } else {
                                    concept.problemTopicName.ifEmpty { concept.conceptName }
                                }
                            }
                            else -> {
                                concept.getLocalizedName(lang)
                            }
                        }

                        val simId = if (isKannadaLanguage(lang)) {
                            concept.simulationIdKannada
                        } else {
                            concept.simulationId
                        }

                        val simUrl = if (isKannadaLanguage(lang)) {
                            concept.simulationUrlKannada
                        } else {
                            concept.simulationUrl
                        }

                        val hasAgent = !simId.isNullOrBlank() && !simId.equals("null", ignoreCase = true) && !simId.trim().equals("not found", ignoreCase = true)
                        val hasUrl = !simUrl.isNullOrBlank() && !simUrl.equals("null", ignoreCase = true) && !simUrl.trim().equals("not found", ignoreCase = true)

                        val status = when {
                            // Trial: status per item's own type, and NOT gated on the previous
                            // item, so a learner can pick and start any lesson or simulation.
                            type.equals("TRIAL", ignoreCase = true) -> {
                                if (concept.type.equals("SIMULATION", ignoreCase = true)) {
                                    determineSimulationStatus(
                                        allProgress, concept.conceptId, hasAgent, hasUrl, index, concepts, lang
                                    )
                                } else {
                                    val progress = allProgress.find { it.itemType == "CONCEPT" && it.itemId == concept.conceptId && it.language == lang }
                                    determineConceptStatus(progress, true, null)
                                }
                            }
                            type.equals("SIMULATION", ignoreCase = true) -> {
                                determineSimulationStatus(
                                    allProgress, concept.conceptId, hasAgent, hasUrl, index, concepts, lang
                                )
                            }
                            else -> {
                                val progress = allProgress.find { it.itemType == "CONCEPT" && it.itemId == concept.conceptId && it.language == lang }
                                val prevStatus = if (index > 0) {
                                    allProgress.find { it.itemType == "CONCEPT" && it.itemId == concepts[index - 1].conceptId && it.language == lang }?.status
                                } else null
                                determineConceptStatus(progress, index == 0, prevStatus)
                            }
                        }

                        ConceptUiModel(
                            id = concept.conceptId,
                            name = displayName,
                            sessionKey = concept.conceptName,
                            order = concept.orderIndex,
                            status = when (status) {
                                ProgressStatus.COMPLETED.value -> {
                                    DebugLogger.debugLog("ConceptVM", "   ✅ $displayName [$lang] → COMPLETED")
                                    DomainProgressStatus.COMPLETED
                                }
                                ProgressStatus.IN_PROGRESS.value, "STARTED" -> {
                                    DebugLogger.debugLog("ConceptVM", "   🔄 $displayName [$lang] → IN_PROGRESS")
                                    DomainProgressStatus.IN_PROGRESS
                                }
                                else -> {
                                    DebugLogger.debugLog("ConceptVM", "   ⭕ $displayName [$lang] → NOT_STARTED")
                                    DomainProgressStatus.NOT_STARTED
                                }
                            },
                            type = concept.type,
                            simulationUrl = simUrl ?: "",
                            simulationId = simId ?: "",
                            problemId = concept.problemId,
                            problemTopicName = if (isKannadaLanguage(lang)) concept.problemTopicNameKn else concept.problemTopicName
                        )
                    }

                    if (conceptUiModels.isNotEmpty() && conceptUiModels[0].status == DomainProgressStatus.NOT_STARTED) {
                        unlockFirstConcept(studentId, conceptUiModels[0].id)
                    }

                    //  CRITICAL FIX: Get chapter progress from the real-time flow
                    // This is the source of truth for progress bar display on this screen
                    val summary = progressSummaries.find { it.chapterId == chapterId }
                    val totalConcepts = summary?.totalConcepts ?: concepts.size
                    val completedConcepts = summary?.completedConcepts ?: 0
                    val overallPct = summary?.completionPercentage ?: 0
                    
                    val progressUiModel = com.ncert7.aitutorandlab.ui.models.ChapterProgressUiModel(
                        completed = completedConcepts,
                        total = totalConcepts,
                        progressFraction = overallPct / 100f,
                        progressPercentage = overallPct,
                        remaining = (totalConcepts - completedConcepts).coerceAtLeast(0)
                    )

                    DebugLogger.debugLog(
                        "ConceptVM",
                        " Chapter Progress Updated [$lang]: $completedConcepts/$totalConcepts concepts completed (${progressUiModel.progressPercentage}%)"
                    )
                    DebugLogger.debugLog(
                        "ConceptVM",
                        "   Concept Card Statuses: ${conceptUiModels.joinToString(", ") { "${it.name}=${it.status.name}" }}"
                    )

                    _state.value = _state.value.copy(
                        concepts = conceptUiModels,
                        chapterName = chapter?.getLocalizedName(lang) ?: "",
                        chapterId = chapterId,
                        type = type,
                        progressUiModel = progressUiModel,
                        subjectName = subject?.getLocalizedName(lang) ?: "",
                        classLevel = classLevel,
                        isLoading = false,
                        error = null
                    )
                }.collect()
            } catch (e: Exception) {
                DebugLogger.errorLog("ConceptVM", "Error loading concepts: ${e.message}")
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    /**
     * Determine the status for SIMULATION type concepts
     * Any present component (Agent or URL) must be completed for SIMULATION to be marked COMPLETED
     */
    private fun determineSimulationStatus(
        allProgress: List<ProgressEntity>,
        conceptId: String,
        hasAgent: Boolean,
        hasUrl: Boolean,
        index: Int,
        concepts: List<com.ncert7.aitutorandlab.data.local.entities.ConceptEntity>,
        language: String
    ): String {
        // Helper: find progress by itemType and conceptId - EXACT language match only (no fallback to empty language)
        // This ensures Kannada progress is never matched with English progress
        fun findProgress(itemType: String, id: String): com.ncert7.aitutorandlab.data.local.entities.ProgressEntity? {
            val found = allProgress.find { it.itemType == itemType && it.itemId == id && it.language == language }
            if (found != null) {
                DebugLogger.debugLog("ConceptVM", "   findProgress($itemType, $id) [$language] = ${found.status}")
            } else {
                DebugLogger.debugLog("ConceptVM", "   findProgress($itemType, $id) [$language] = NOT FOUND")
            }
            return found
        }

        return when {
            hasAgent && hasUrl -> {
                val agentDone = findProgress("SIMULATION_AGENT", conceptId)
                    ?.status == ProgressStatus.COMPLETED.value
                val urlDone = findProgress("SIMULATION", conceptId)
                    ?.status == ProgressStatus.COMPLETED.value

                DebugLogger.debugLog("ConceptVM", "   Concept $conceptId [hasAgent+hasUrl]: agent=$agentDone, url=$urlDone, language=$language")

                if (agentDone || urlDone) {
                    ProgressStatus.COMPLETED.value
                } else {
                    val prevStatus = if (index > 0) {
                        allProgress.find { it.itemId == concepts[index - 1].conceptId && it.language == language }?.status
                    } else null
                    determineConceptStatus(null, index == 0, prevStatus)
                }
            }
            hasAgent -> {
                val agentDone = findProgress("SIMULATION_AGENT", conceptId)
                    ?.status == ProgressStatus.COMPLETED.value
                DebugLogger.debugLog("ConceptVM", "   Concept $conceptId [hasAgent only]: agent=$agentDone, language=$language")
                if (agentDone) ProgressStatus.COMPLETED.value else {
                    val prevStatus = if (index > 0) {
                        allProgress.find { it.itemId == concepts[index - 1].conceptId && it.language == language }?.status
                    } else null
                    determineConceptStatus(null, index == 0, prevStatus)
                }
            }
            hasUrl -> {
                val urlDone = findProgress("SIMULATION", conceptId)
                    ?.status == ProgressStatus.COMPLETED.value
                DebugLogger.debugLog("ConceptVM", "   Concept $conceptId [hasUrl only]: url=$urlDone, language=$language")
                if (urlDone) ProgressStatus.COMPLETED.value else {
                    val prevStatus = if (index > 0) {
                        allProgress.find { it.itemId == concepts[index - 1].conceptId && it.language == language }?.status
                    } else null
                    determineConceptStatus(null, index == 0, prevStatus)
                }
            }
            else -> ProgressStatus.NOT_STARTED.value
        }
    }

    /**
     * Determine the status for STUDY and MATH PROBLEM type concepts
     * First concept is always unlocked
     * Subsequent concepts unlock only when previous is completed
     */
    private fun determineConceptStatus(
        progress: ProgressEntity?,
        isFirstConcept: Boolean,
        previousConceptStatus: String?
    ): String {
        if (progress != null) {
            return progress.status
        }

        if (isFirstConcept) {
            return ProgressStatus.IN_PROGRESS.value
        }

        if (previousConceptStatus == ProgressStatus.COMPLETED.value) {
            return ProgressStatus.IN_PROGRESS.value
        }

        return ProgressStatus.NOT_STARTED.value
    }



    private suspend fun unlockFirstConcept(studentId: String, conceptId: String) {
        try {
            val language = getCurrentLanguageCode()
            progressEventTracker.markStudyInProgress(studentId, conceptId, language)
            DebugLogger.debugLog("ConceptVM", "First concept unlocked: $conceptId")
        } catch (e: Exception) {
            DebugLogger.debugLog("ConceptVM", "Error unlocking first concept: ${e.message}")
        }
    }
}