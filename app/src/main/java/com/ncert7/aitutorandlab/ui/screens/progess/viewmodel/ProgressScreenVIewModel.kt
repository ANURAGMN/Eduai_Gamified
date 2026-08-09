package com.ncert7.aitutorandlab.ui.screens.progess.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncert7.aitutorandlab.data.local.dao.ChapterProgressSummary
import com.ncert7.aitutorandlab.data.local.dao.DailyConceptCount
import com.ncert7.aitutorandlab.data.local.entities.StudentEntity
import com.ncert7.aitutorandlab.data.local.entities.SubjectEntity
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.repository.ProgressRepository
import com.ncert7.aitutorandlab.repository.StreakRepository
import com.ncert7.aitutorandlab.repository.StudentLocalRepository
import com.ncert7.aitutorandlab.repository.SubjectRepository
import com.ncert7.aitutorandlab.utils.isKannada
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 *  FIXED: ProgressScreenViewModel
 *
 * Changes:
 * 1. Removed ChapterRepository dependency (not needed)
 * 2. Changed getChapterProgressSummary() to use ProgressRepository.getChapterWiseProgress()
 * 3. Added proper Flow collection with collectLatest
 * 4. All progress data now flows from a single source of truth
 * 5. Real-time updates whenever progress table changes
 */
@HiltViewModel
class ProgressScreenViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val subjectRepository: SubjectRepository,
    private val studentRepository: StudentLocalRepository,
    private val streakRepository: StreakRepository,
    private val sharedPrefs: com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
) : ViewModel() {

    private val userId: String
        get() = sharedPrefs.getUserId() ?: ""


    // --- State holders ---
    private val _totalCompletedConcept = MutableStateFlow(0)
    val totalCompletedConcept: StateFlow<Int> = _totalCompletedConcept.asStateFlow()

    private val _totalCompletedSimulation = MutableStateFlow(0)
    val totalCompletedSimulation: StateFlow<Int> = _totalCompletedSimulation.asStateFlow()

    private val _streakCount = MutableStateFlow(0)
    val streakCount: StateFlow<Int> = _streakCount.asStateFlow()

    private val _sevenDayProgress = MutableStateFlow<List<DailyConceptCount>>(emptyList())
    val sevenDayProgress: StateFlow<List<DailyConceptCount>> = _sevenDayProgress.asStateFlow()

    /**
     * ✅ FIXED: Chapter-wise progress using real-time Flow from ProgressRepository
     * This now uses actual progress data from progress table, not aggregated data
     */
    private val _chapterProgressSummary = MutableStateFlow<List<ChapterProgressSummary>>(emptyList())
    val chapterProgressSummary: StateFlow<List<ChapterProgressSummary>> = _chapterProgressSummary.asStateFlow()

    private val _subjects = MutableStateFlow<List<SubjectEntity>>(emptyList())
    val subjects: StateFlow<List<SubjectEntity>> = _subjects.asStateFlow()

    private val _selectedSubject = MutableStateFlow<SubjectEntity?>(null)
    val selectedSubject: StateFlow<SubjectEntity?> = _selectedSubject.asStateFlow()

    private val _student = MutableStateFlow<StudentEntity?>(null)
    val student: StateFlow<StudentEntity?> = _student.asStateFlow()

    // --- Processed Weekly Data (UI-ready) ---
    private val _weeklyProgressData = MutableStateFlow<List<DayProgress>>(emptyList())
    val weeklyProgressData: StateFlow<List<DayProgress>> = _weeklyProgressData.asStateFlow()

    private val _maxWeeklyValue = MutableStateFlow(1)
    val maxWeeklyValue: StateFlow<Int> = _maxWeeklyValue.asStateFlow()

    // totalScore: derived from completed concepts (each concept = 10 pts, each sim = 20 pts)
    private val _totalScore = MutableStateFlow(0)
    val totalScore: StateFlow<Int> = _totalScore.asStateFlow()

    // --- Chapter Progress Categorization (UI-ready) ---
    private val _inProgressChapters = MutableStateFlow<List<ChapterProgressSummary>>(emptyList())
    val inProgressChapters: StateFlow<List<ChapterProgressSummary>> = _inProgressChapters.asStateFlow()

    private val _completedChapters = MutableStateFlow<List<ChapterProgressSummary>>(emptyList())
    val completedChapters: StateFlow<List<ChapterProgressSummary>> = _completedChapters.asStateFlow()

    private val _notStartedChapters = MutableStateFlow<List<ChapterProgressSummary>>(emptyList())
    val notStartedChapters: StateFlow<List<ChapterProgressSummary>> = _notStartedChapters.asStateFlow()

    private val _chaptersToShow = MutableStateFlow<List<ChapterProgressSummary>>(emptyList())
    val chaptersToShow: StateFlow<List<ChapterProgressSummary>> = _chaptersToShow.asStateFlow()

    private val _showAllChapters = MutableStateFlow(false)
    val showAllChapters: StateFlow<Boolean> = _showAllChapters.asStateFlow()

    private val _hasMoreChapters = MutableStateFlow(false)
    val hasMoreChapters: StateFlow<Boolean> = _hasMoreChapters.asStateFlow()

    // Holds the active collection Job so we can cancel it when subject changes
    private var chapterProgressJob: Job? = null

    private val _currentLanguage = MutableStateFlow(if (isKannada()) "kn" else "en")
    val currentLanguage: StateFlow<String> = _currentLanguage

    fun setLanguage(lang: String) {
        if (_currentLanguage.value != lang) {
            _currentLanguage.value = lang
            DebugLogger.debugLog("ProgressVM", "Language dynamically changed to: $lang")
        }
    }

    init {
        getStudent()
        observeStreak()
        observeConceptCount()
        observeSimulationCount()
        observeTotalScore()
    }

    // --- Reactive Data Observation ---

    private fun observeConceptCount() {
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            _currentLanguage.collectLatest { language ->
                kotlinx.coroutines.coroutineScope {
                    progressRepository.getTotalCompletedConceptsFlow(userId, language)
                        .collectLatest { count ->
                            _totalCompletedConcept.value = count
                            DebugLogger.debugLog("ProgressVM", "Concepts updated: $count ($language)")
                        }
                }
            }
        }
    }

    private fun observeSimulationCount() {
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            _currentLanguage.collectLatest { language ->
                kotlinx.coroutines.coroutineScope {
                    progressRepository.getTotalCompletedSimulationsFlow(userId, language)
                        .collectLatest { count ->
                            _totalCompletedSimulation.value = count
                            DebugLogger.debugLog("ProgressVM", "Simulations updated: $count ($language)")
                        }
                }
            }
        }
    }

    private fun observeTotalScore() {
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            _currentLanguage.collectLatest { language ->
                kotlinx.coroutines.coroutineScope {
                    kotlinx.coroutines.flow.combine(
                        progressRepository.getTotalCompletedConceptsFlow(userId, language),
                        progressRepository.getTotalCompletedSimulationsFlow(userId, language)
                    ) { concepts, sims ->
                        (concepts * 10) + (sims * 20)
                    }.collectLatest { score ->
                        _totalScore.value = score
                        DebugLogger.debugLog("ProgressVM", "Total score updated: $score ($language)")
                    }
                }
            }
        }
    }

    private fun observeStreak() {
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            // Use Flow for seamless sync - UI updates immediately when DB updates
            streakRepository.getStreakFlow(userId).collectLatest { streak ->
                _streakCount.value = streakRepository.effectiveDisplayStreak(streak)
                DebugLogger.debugLog("ProgressVM", "Streak updated: ${_streakCount.value}")
            }
        }
    }

    fun getSevenDayProgress(sevenDaysAgoTimeStamp: Long) {
        viewModelScope.launch {
            try {
                // Count ALL activities (concepts + simulations + revision) for the weekly chart
                val result = progressRepository.getDailyCompletedActivityLast7Days(userId, sevenDaysAgoTimeStamp)
                DebugLogger.debugLog("ProgressVM", "Weekly Activity Data: $result")
                _sevenDayProgress.value = result
                processWeeklyData(result)
            } catch (e: Exception) {
                DebugLogger.errorLog("ProgressVM", "Error loading weekly activity: ${e.message}")
            }
        }
    }

    /**
     * ✅ FIXED: Load chapter-wise progress with REAL-TIME updates
     *
     * Key changes:
     * 1. Now uses ProgressRepository.getChapterWiseProgress() (NEW method)
     * 2. This method returns Flow<List<ChapterProgressSummary>> from ProgressDao
     * 3. Data is calculated directly from progress table (real counts, not aggregated)
     * 4. Flow emits whenever progress table changes
     * 5. collectLatest ensures UI always has latest data
     *
     * @param classLevel Class level (currently unused, kept for compatibility)
     * @param subjectId Subject ID to filter chapters
     */
    fun getChapterProgressSummary(classLevel: Int, subjectId: String, language: String) {
        // Cancel the previous flow collection before starting a new one
        chapterProgressJob?.cancel()
        chapterProgressJob = viewModelScope.launch {
            try {
                DebugLogger.debugLog(
                    "ProgressVM",
                    "Starting chapter progress observation for subject=$subjectId, language=$language"
                )

                // ✅ KEY FIX: Use ProgressRepository method instead of non-existent ChapterRepository
                progressRepository.getChapterWiseProgress(userId, subjectId, language)
                    .collectLatest { chapters ->
                        _chapterProgressSummary.value = chapters
                        categorizeChapters(chapters)

                        val completedCount = chapters.count { it.completionPercentage >= 100 }
                        DebugLogger.debugLog(
                            "ProgressVM",
                            "Chapter progress updated: ${chapters.size} chapters, " +
                                    "$completedCount completed"
                        )
                    }
            } catch (e: Exception) {
                DebugLogger.errorLog(
                    "ProgressVM",
                    "Error loading chapter progress: ${e.message}"
                )
            }
        }
    }

    fun loadSubjects(classLevel: Int) {
        viewModelScope.launch {
            // Hardcode to class 7 to ensure syllabus is independent of user's profile class level
            val subjectList = subjectRepository.getSubjectsForClass(7)
            _subjects.value = subjectList
            if (subjectList.isNotEmpty() && _selectedSubject.value == null) {
                _selectedSubject.value = subjectList.first()
            }
            DebugLogger.debugLog("ProgressVM", "Loaded ${subjectList.size} subjects for class $classLevel")
        }
    }

    fun selectSubject(subject: SubjectEntity) {
        _selectedSubject.value = subject
        _showAllChapters.value = false
    }

    fun getStudent() {
        viewModelScope.launch {
            _student.value = studentRepository.getStudentSync(userId)
        }
    }

    // --- Business Logic ---

    private fun processWeeklyData(rawData: List<DailyConceptCount>) {
        val today = LocalDate.now()
        val last7Days = (6 downTo 0).map { today.minusDays(it.toLong()).toString() }
        val progressMap = rawData.associateBy { it.date }
        val weeklyData = last7Days.map { date ->
            DayProgress(dayLabel = getDayOfWeek(date), count = progressMap[date]?.count ?: 0)
        }
        _weeklyProgressData.value = weeklyData
        _maxWeeklyValue.value = (weeklyData.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
    }

    private fun getDayOfWeek(dateString: String): String {
        return try {
            val date = LocalDate.parse(dateString)
            date.dayOfWeek.name.take(3).lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } catch (e: Exception) { "???" }
    }

    private fun categorizeChapters(chapters: List<ChapterProgressSummary>) {
        val inProgress  = chapters.filter { it.completionPercentage > 0 && it.completionPercentage < 100 }
        val completed   = chapters.filter { it.completionPercentage >= 100 }
        val notStarted  = chapters.filter { it.completionPercentage <= 0 }

        _inProgressChapters.value  = inProgress
        _completedChapters.value   = completed
        _notStartedChapters.value  = notStarted

        updateChaptersToShow(chapters, inProgress, notStarted)
    }

    private fun updateChaptersToShow(
        allChapters: List<ChapterProgressSummary>,
        inProgress: List<ChapterProgressSummary>,
        notStarted: List<ChapterProgressSummary>
    ) {
        val chaptersToDisplay = if (_showAllChapters.value) {
            allChapters
        } else {
            val selected = inProgress.take(4).toMutableList()
            if (selected.size < 4) selected.addAll(notStarted.take(4 - selected.size))
            selected
        }
        _chaptersToShow.value  = chaptersToDisplay
        _hasMoreChapters.value = allChapters.size > chaptersToDisplay.size
    }

    fun toggleShowAllChapters() {
        _showAllChapters.value = !_showAllChapters.value
        categorizeChapters(_chapterProgressSummary.value)
    }

    fun calculateBarHeight(count: Int): Float {
        val maxValue = _maxWeeklyValue.value
        return (count.toFloat() / maxValue * 100).coerceAtLeast(4f)
    }

    fun getProgressColor(percentage: Int): ProgressColorType = when {
        percentage >= 100 -> ProgressColorType.COMPLETED
        percentage >= 80  -> ProgressColorType.HIGH_PROGRESS
        percentage >= 50  -> ProgressColorType.MEDIUM_PROGRESS
        percentage > 0    -> ProgressColorType.STARTED
        else              -> ProgressColorType.NOT_STARTED
    }

    fun capitalizeFirstLetter(text: String): String =
        text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    fun getShowMoreButtonText(): String =
        if (_showAllChapters.value) "show_less" else "show_more_count"

    fun getHiddenChaptersCount(): Int =
        _chapterProgressSummary.value.size - _chaptersToShow.value.size

    fun getSevenDaysAgoInMillis(): Long {
        val sevenDaysAgo = LocalDate.now().minusDays(7)
        return sevenDaysAgo.toEpochDay() * 24 * 60 * 60 * 1000
    }
}

data class DayProgress(val dayLabel: String, val count: Int)

enum class ProgressColorType {
    COMPLETED, HIGH_PROGRESS, MEDIUM_PROGRESS, STARTED, NOT_STARTED
}