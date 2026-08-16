package com.ncert7.aitutorandlab.ui.screens.plan.viewmodel



import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope

import com.anurag.eduai.uikit.components.PlanDayNode

import com.ncert7.aitutorandlab.data.local.dao.ConceptDao
import com.ncert7.aitutorandlab.data.local.dao.ChapterDao
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils

import com.ncert7.aitutorandlab.data.local.entities.ChapterEntity

import com.ncert7.aitutorandlab.data.local.entities.ExamPlanEntity

import com.ncert7.aitutorandlab.data.local.entities.SubjectEntity

import com.ncert7.aitutorandlab.domain.examplan.DefaultExamPlan
import com.ncert7.aitutorandlab.domain.examplan.PlanFeasibilityIssue

import com.ncert7.aitutorandlab.domain.examplan.PlanFeasibilitySeverity

import com.ncert7.aitutorandlab.repository.ChapterRepository

import com.ncert7.aitutorandlab.repository.ExamPlanRepository

import com.ncert7.aitutorandlab.service.analytics.GamificationAnalyticsTracker

import com.ncert7.aitutorandlab.service.analytics.PlanFeasibilityIssueType

import com.ncert7.aitutorandlab.repository.SubjectRepository

import com.ncert7.aitutorandlab.ui.screens.plan.ExamPlanUiMapper

import com.ncert7.aitutorandlab.utils.SubjectIds
import com.ncert7.aitutorandlab.utils.ExamPlanCopy
import com.ncert7.aitutorandlab.utils.TrialTitleResolver
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import com.ncert7.aitutorandlab.utils.getLocalizedName

import com.ncert7.aitutorandlab.utils.normalizeLanguageCode

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.Job

import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine

import kotlinx.coroutines.flow.update

import kotlinx.coroutines.launch

import java.time.LocalDate

import java.time.ZoneId

import java.time.format.DateTimeFormatter

import javax.inject.Inject



data class PlanSetupUiState(

    val examType: String = "Unit Test",

    val subjectId: String = "",

    val selectedChapterIds: Set<String> = emptySet(),

    val dailyMinutes: Int = 30,

    val examEpochDay: Long = 0L,

    val estimatedDays: Int = 0,

    val totalTrialItems: Int = 0,

    val feasibilityIssues: List<PlanFeasibilityIssue> = emptyList(),

    val isSaving: Boolean = false,

    val errorMessage: String? = null,

) {

    val canGenerate: Boolean =

        selectedChapterIds.isNotEmpty() &&

            subjectId.isNotBlank() &&

            feasibilityIssues.none { it.severity == PlanFeasibilitySeverity.ERROR }

}



@HiltViewModel

class PlanOverviewViewModel @Inject constructor(

    private val examPlanRepository: ExamPlanRepository,

    private val subjectRepository: SubjectRepository,

    private val chapterRepository: ChapterRepository,

    private val conceptDao: ConceptDao,

    private val chapterDao: ChapterDao,

    private val sharedPrefs: SharedPreferenceUtils,

) : ViewModel() {



    private val zone = ZoneId.of("Asia/Kolkata")

    private val examDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

    private val userId: String

        get() = sharedPrefs.getUserId().orEmpty()



    private val _planDays = MutableStateFlow<List<PlanDayNode>>(emptyList())

    val planDays: StateFlow<List<PlanDayNode>> = _planDays



    private val _activePlan = MutableStateFlow<ExamPlanEntity?>(null)

    val activePlan: StateFlow<ExamPlanEntity?> = _activePlan



    private val _isLoading = MutableStateFlow(true)

    val isLoading: StateFlow<Boolean> = _isLoading



    private val _showSetup = MutableStateFlow(false)

    val showSetup: StateFlow<Boolean> = _showSetup



    private val _subjects = MutableStateFlow<List<SubjectEntity>>(emptyList())

    val subjects: StateFlow<List<SubjectEntity>> = _subjects



    private val _chapters = MutableStateFlow<List<ChapterEntity>>(emptyList())

    val chapters: StateFlow<List<ChapterEntity>> = _chapters



    private val _setup = MutableStateFlow(PlanSetupUiState())

    val setup: StateFlow<PlanSetupUiState> = _setup



    private val _currentLanguage =
        MutableStateFlow(normalizeLanguageCode(getCurrentLanguageCode()))

    private var languageCode: String
        get() = _currentLanguage.value
        set(value) {
            _currentLanguage.value = value
        }

    private var feasibilityJob: Job? = null



    init {

        viewModelScope.launch {

            if (userId.isEmpty()) {

                _isLoading.value = false

                return@launch

            }

            languageCode = normalizeLanguageCode(getCurrentLanguageCode())

            _subjects.value = subjectRepository.getSubjectsForClass(7)

            examPlanRepository.ensureActivePlan(
                studentId = userId,
                subjectId = sharedPrefs.getSubjectSelectionId(),
                languageCode = languageCode,
            )

            if (sharedPrefs.consumeOpenExamPlanSetupPending()) {

                _showSetup.value = true

                preloadSetupSelection()

            }

            _isLoading.value = false

        }

        viewModelScope.launch {

            if (userId.isEmpty()) return@launch

            combine(
                examPlanRepository.observePlanDays(userId),
                _currentLanguage,
            ) { entities, language -> entities to language }
                .collectLatest { (entities, language) ->
                    _planDays.value =
                        entities
                            .filter { it.isExamScheduleDay() }
                            .map { day ->
                                ExamPlanUiMapper.toPlanDayNode(
                                    day,
                                    TrialTitleResolver.localizedPlanDayLabel(day, language, conceptDao, chapterDao),
                                )
                            }
                }

        }

        viewModelScope.launch {

            if (userId.isEmpty()) return@launch

            examPlanRepository.observeActivePlan(userId).collectLatest { plan ->

                _activePlan.value = plan

                if (plan != null) {

                    seedSetupFromPlan(plan)

                }

            }

        }

    }



    fun consumeOpenSetupPending(): Boolean {

        val pending = sharedPrefs.consumeOpenExamPlanSetupPending()

        if (pending) {

            openSetup()

        }

        return pending

    }



    fun openSetup() {

        _showSetup.value = true

        viewModelScope.launch { preloadSetupSelection() }

    }



    fun closeSetup() {

        if (!sharedPrefs.isExamPlanUserConfigured()) return

        _showSetup.value = false

    }



    private suspend fun preloadSetupSelection() {

        val plan = _activePlan.value

        val subjectId =

            plan?.subjectId?.takeIf { it.isNotBlank() }

                ?: SubjectIds.SCIENCE

        if (subjectId.isNotBlank()) {

            selectSubject(subjectId)

        }

        _setup.update {
            it.copy(
                examType = plan?.examType ?: DefaultExamPlan.EXAM_TYPE,
                examEpochDay =
                    plan?.examEpochDay?.takeIf { epoch -> epoch > 0L }
                        ?: DefaultExamPlan.defaultExamDate().toEpochDay(),
            )
        }

        refreshFeasibility()

    }



    fun selectExamType(type: String) {

        _setup.update { it.copy(examType = type) }

        refreshFeasibility()

    }



    fun selectSubject(subjectId: String) {

        viewModelScope.launch {

            _setup.update {

                it.copy(subjectId = subjectId, selectedChapterIds = emptySet(), errorMessage = null)

            }

            val chapterList =

                chapterRepository.getChaptersForSubject(subjectId).sortedBy { it.orderIndex }

            _chapters.value = chapterList

            val existing = _activePlan.value

            val preselected =

                if (existing?.subjectId == subjectId) {

                    existing.chapterIds.split(",").filter { it.isNotBlank() }.toSet()

                } else if (subjectId == SubjectIds.SCIENCE) {

                    DefaultExamPlan.resolveChapterId(chapterList)?.let { setOf(it) } ?: emptySet()

                } else {

                    emptySet()

                }

            toggleChapters(preselected)

        }

    }



    fun toggleChapter(chapterId: String) {

        _setup.update { state ->

            val next =

                if (chapterId in state.selectedChapterIds) {

                    state.selectedChapterIds - chapterId

                } else {

                    state.selectedChapterIds + chapterId

                }

            state.copy(selectedChapterIds = next, errorMessage = null)

        }

        refreshFeasibility()

    }



    private fun toggleChapters(ids: Set<String>) {

        _setup.update { it.copy(selectedChapterIds = ids, errorMessage = null) }

        refreshFeasibility()

    }



    fun setDailyMinutes(minutes: Int) {

        _setup.update { it.copy(dailyMinutes = minutes.coerceIn(15, 90), errorMessage = null) }

        refreshFeasibility()

    }



    fun selectExamPreset(daysFromToday: Long) {

        val examDate = LocalDate.now(zone).plusDays(daysFromToday)

        _setup.update { it.copy(examEpochDay = examDate.toEpochDay(), errorMessage = null) }

        refreshFeasibility()

    }

    fun selectExamDate(date: LocalDate) {

        _setup.update { it.copy(examEpochDay = date.toEpochDay(), errorMessage = null) }

        refreshFeasibility()

    }



    fun saveSetup(onSaved: () -> Unit = {}) {

        viewModelScope.launch {

            val state = _setup.value

            if (state.subjectId.isBlank()) {

                _setup.update { it.copy(errorMessage = ExamPlanCopy.chooseSubject(languageCode)) }

                return@launch

            }

            if (state.selectedChapterIds.isEmpty()) {

                _setup.update { it.copy(errorMessage = ExamPlanCopy.selectChapter(languageCode)) }

                return@launch

            }

            if (!state.canGenerate) {

                _setup.update {

                    it.copy(errorMessage = it.feasibilityIssues.firstOrNull()?.message ?: ExamPlanCopy.fixPlanIssues(languageCode))

                }

                return@launch

            }

            _setup.update { it.copy(isSaving = true, errorMessage = null) }

            val result =

                examPlanRepository.createCustomPlan(

                    studentId = userId,

                    subjectId = state.subjectId,

                    chapterIds = state.selectedChapterIds.toList(),

                    languageCode = languageCode,

                    examType = state.examType,

                    dailyMinutes = state.dailyMinutes,

                    examDate = examDateFromState(state),

                )

            if (!result.canSave) {

                _setup.update {

                    it.copy(

                        isSaving = false,

                        feasibilityIssues = result.issues,

                        errorMessage = result.blockingErrors.firstOrNull()?.message,

                    )

                }

                return@launch

            }

            sharedPrefs.setSubjectSelectionId(state.subjectId)

            sharedPrefs.setExamPlanUserConfigured(true)

            _setup.update { it.copy(isSaving = false) }

            _showSetup.value = false

            onSaved()

        }

    }



    fun examDateLabel(state: PlanSetupUiState = _setup.value): String {

        val epoch = state.examEpochDay.takeIf { it > 0L } ?: defaultExamEpochDay(state.estimatedDays)

        return ExamPlanCopy.formatExamDate(languageCode, LocalDate.ofEpochDay(epoch))

    }



    private fun refreshFeasibility() {

        feasibilityJob?.cancel()

        feasibilityJob =

            viewModelScope.launch {

                val state = _setup.value

                if (state.selectedChapterIds.isEmpty()) {

                    _setup.update {

                        it.copy(

                            estimatedDays = 0,

                            totalTrialItems = 0,

                            feasibilityIssues = emptyList(),

                        )

                    }

                    return@launch

                }



                val examEpoch =

                    state.examEpochDay.takeIf { it > 0L }

                        ?: defaultExamEpochDay(state.estimatedDays.coerceAtLeast(7))

                val examDate = LocalDate.ofEpochDay(examEpoch)



                val result =

                    examPlanRepository.analyzePlanFeasibility(

                        chapterIds = state.selectedChapterIds.toList(),

                        languageCode = languageCode,

                        dailyMinutes = state.dailyMinutes,

                        examDate = examDate,

                        examType = state.examType,

                    )



                _setup.update {

                    it.copy(

                        examEpochDay = examEpoch,

                        estimatedDays = result.requiredPlanDays,

                        totalTrialItems = result.totalTrialItems,

                        feasibilityIssues = result.issues,

                        errorMessage = null,

                    )

                }

                result.issues
                    .filter { it.severity == PlanFeasibilitySeverity.WARNING }
                    .forEach { issue ->
                        GamificationAnalyticsTracker.planFeasibilityWarning(
                            issue = mapFeasibilityIssue(issue),
                            requiredDays = result.requiredPlanDays,
                            availableDays = result.availableCalendarDays,
                        )
                    }

            }

    }



    private fun defaultExamEpochDay(estimatedDays: Int): Long {

        val bufferDays = estimatedDays.coerceAtLeast(7)

        return LocalDate.now(zone).plusDays((bufferDays - 1).toLong()).toEpochDay()

    }



    private fun examDateFromState(state: PlanSetupUiState): LocalDate {

        val epoch = state.examEpochDay.takeIf { it > 0L } ?: defaultExamEpochDay(state.estimatedDays)

        return LocalDate.ofEpochDay(epoch)

    }



    private fun seedSetupFromPlan(plan: ExamPlanEntity) {

        val examEpoch =

            plan.examEpochDay.takeIf { it > 0L }

                ?: plan.startEpochDay

        _setup.update {

            it.copy(

                examType = plan.examType,

                subjectId = plan.subjectId,

                selectedChapterIds = plan.chapterIds.split(",").filter { id -> id.isNotBlank() }.toSet(),

                dailyMinutes = plan.dailyMinutes,

                examEpochDay = examEpoch,

            )

        }

        refreshFeasibility()

    }



    fun chapterLabel(chapter: ChapterEntity): String = chapter.getLocalizedName(languageCode)

    fun subjectLabel(subject: SubjectEntity): String = subject.getLocalizedName(languageCode)

    fun refreshLanguage() {
        val lang = normalizeLanguageCode(getCurrentLanguageCode())
        if (_currentLanguage.value != lang) {
            _currentLanguage.value = lang
        }
    }

    fun canDismissSetup(): Boolean = sharedPrefs.isExamPlanUserConfigured()

    private fun mapFeasibilityIssue(issue: PlanFeasibilityIssue): PlanFeasibilityIssueType {
        val message = issue.message.lowercase()
        return when {
            message.contains("capacity") || message.contains("too many") -> PlanFeasibilityIssueType.OVER_CAPACITY
            message.contains("past") || message.contains("exam date") -> PlanFeasibilityIssueType.PAST_DATE
            message.contains("chapter") && message.contains("empty") -> PlanFeasibilityIssueType.EMPTY_CHAPTER
            message.contains("chapter") -> PlanFeasibilityIssueType.TOO_MANY_CHAPTERS
            else -> PlanFeasibilityIssueType.OTHER
        }
    }
}


