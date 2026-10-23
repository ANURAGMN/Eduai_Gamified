package com.ncert7.aitutorandlab.ui.screens.home.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.local.dao.ChapterAgentProgressDao
import com.ncert7.aitutorandlab.data.local.dao.ChapterDao
import com.ncert7.aitutorandlab.data.local.dao.ConceptDao
import com.ncert7.aitutorandlab.data.local.dao.ProgressDao
import com.ncert7.aitutorandlab.data.local.dao.StudentDao
import com.ncert7.aitutorandlab.data.local.entities.ConceptEntity
import com.ncert7.aitutorandlab.data.local.entities.ProgressEntity
import com.ncert7.aitutorandlab.data.local.entities.GamificationProfileEntity
import com.ncert7.aitutorandlab.data.local.entities.StudentEntity
import com.ncert7.aitutorandlab.data.local.entities.SubjectEntity
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.domain.gamification.FriendFeedService
import com.ncert7.aitutorandlab.domain.gamification.QuestClaimResult
import com.ncert7.aitutorandlab.domain.gamification.QuestClaimType
import com.ncert7.aitutorandlab.domain.gamification.QuestGemRewardService
import com.ncert7.aitutorandlab.repository.ExamPlanRepository
import com.ncert7.aitutorandlab.repository.FriendRepository
import com.ncert7.aitutorandlab.repository.GamificationRepository
import com.ncert7.aitutorandlab.repository.LeagueRepository
import com.ncert7.aitutorandlab.domain.examplan.PlanTrialProgressTracker
import com.ncert7.aitutorandlab.repository.PlanTrialRepository
import com.ncert7.aitutorandlab.repository.QuestRepository
import com.ncert7.aitutorandlab.repository.StreakRepository
import com.ncert7.aitutorandlab.repository.SubjectRepository
import com.ncert7.aitutorandlab.service.sync.DataSyncService
import com.anurag.eduai.uikit.screens.YoutubeVideoItem
import com.ncert7.aitutorandlab.ui.screens.home.GamifiedHomeMapper
import com.ncert7.aitutorandlab.ui.screens.plan.ExamPlanUiMapper
import com.anurag.eduai.uikit.components.FriendUpdate
import com.anurag.eduai.uikit.components.PlanDayNode
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanDayEntity
import com.ncert7.aitutorandlab.data.local.entities.FriendFeedItemEntity
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemEntity
import com.ncert7.aitutorandlab.data.local.entities.QuestDailyEntity
import com.ncert7.aitutorandlab.service.analytics.GamificationAnalyticsTracker
import com.ncert7.aitutorandlab.domain.youtube.YoutubeVideo
import com.ncert7.aitutorandlab.data.local.entities.GardenTheme
import com.ncert7.aitutorandlab.data.local.entities.GrownItemEntity
import com.ncert7.aitutorandlab.domain.garden.GardenProgress
import com.ncert7.aitutorandlab.domain.garden.GardenStarterHighlight
import com.ncert7.aitutorandlab.domain.onboarding.OnboardingChapterCatalog
import com.ncert7.aitutorandlab.repository.GardenRepository
import com.ncert7.aitutorandlab.repository.TutorConfigRepository
import com.ncert7.aitutorandlab.repository.YoutubeVideoRepository
import com.ncert7.aitutorandlab.ui.screens.friends.FriendUiMapper
import com.ncert7.aitutorandlab.utils.getLocalizedName
import com.ncert7.aitutorandlab.utils.isKannada
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import com.ncert7.aitutorandlab.utils.normalizeLanguageCode
import com.ncert7.aitutorandlab.utils.TrialTitleResolver
import com.anurag.eduai.uikit.avatar.AvatarUnlockStore
import com.anurag.eduai.uikit.components.SearchResultItem
import com.ncert7.aitutorandlab.data.remote.AgenticAIClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val conceptDao: ConceptDao,
    private val chapterDao: ChapterDao,
    private val chapterAgentProgressDao: ChapterAgentProgressDao,
    private val progressDao: ProgressDao,
    private val studentDao: StudentDao,
    private val streakRepository: StreakRepository,
    private val gamificationRepository: GamificationRepository,
    private val leagueRepository: LeagueRepository,
    private val friendRepository: FriendRepository,
    private val friendFeedService: FriendFeedService,
    private val examPlanRepository: ExamPlanRepository,
    private val planTrialRepository: PlanTrialRepository,
    private val planTrialProgressTracker: PlanTrialProgressTracker,
    private val questRepository: QuestRepository,
    private val questGemRewardService: QuestGemRewardService,
    private val sharedPrefs: SharedPreferenceUtils,
    private val subjectRepository: SubjectRepository,
    private val youtubeVideoRepository: YoutubeVideoRepository,
    private val gardenRepository: GardenRepository,
    private val tutorConfigRepository: TutorConfigRepository,
    private val apiClient: AgenticAIClient,
    @ApplicationContext private val appContext: Context,
) : ViewModel(){

    private val userId: String
        get() = sharedPrefs.getUserId() ?: ""

    var progressConcepts =
        MutableStateFlow<List<Pair<ProgressEntity?, ConceptEntity?>>>(emptyList())
    var progressSimulations =
        MutableStateFlow<List<Pair<ProgressEntity?, ConceptEntity?>>>(emptyList())

    private val _streakCount = MutableStateFlow(0)
    val streakCount: StateFlow<Int> = _streakCount

    private val _dailyStreakGreeting = MutableStateFlow<Int?>(null)
    val dailyStreakGreeting: StateFlow<Int?> = _dailyStreakGreeting

    private val _streakExtended = MutableStateFlow<Int?>(null)
    val streakExtended: StateFlow<Int?> = _streakExtended

    private val _todayConceptCount = MutableStateFlow(0)
    val todayConceptCount: StateFlow<Int> = _todayConceptCount

    private val _todaySimulationCount = MutableStateFlow(0)
    val todaySimulationCount: StateFlow<Int> = _todaySimulationCount

    private val _totalCompletedConcept = MutableStateFlow(0)
    val totalCompletedConcept: StateFlow<Int> = _totalCompletedConcept

    private val _totalCompletedSimulation = MutableStateFlow(0)
    val totalCompletedSimulation: StateFlow<Int> = _totalCompletedSimulation

    private val _student = MutableStateFlow<StudentEntity?>(null)
    val student: StateFlow<StudentEntity?> = _student

    private val _studentLoaded = MutableStateFlow(false)
    val studentLoaded: StateFlow<Boolean> = _studentLoaded

    private val _greeting = MutableStateFlow("")
    val greeting: StateFlow<String> = _greeting

    private val _languageChangeTrigger = MutableStateFlow(0)
    val languageChangeTrigger: StateFlow<Int> = _languageChangeTrigger

    private val _currentLanguage = MutableStateFlow(normalizeLanguageCode(getCurrentLanguageCode()))
    val currentLanguage: StateFlow<String> = _currentLanguage

    private val _selectedSubjectName = MutableStateFlow("")
    val selectedSubjectName: StateFlow<String> = _selectedSubjectName

    private val _availableSubjects = MutableStateFlow<List<SubjectEntity>>(emptyList())
    val availableSubjects: StateFlow<List<SubjectEntity>> = _availableSubjects

    private val _chapterCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val chapterCounts: StateFlow<Map<String, Int>> = _chapterCounts

    private val _chapterProgressSums = MutableStateFlow<Map<String, Int>>(emptyMap())
    val chapterProgressSums: StateFlow<Map<String, Int>> = _chapterProgressSums

    private val _gamificationProfile = MutableStateFlow<GamificationProfileEntity?>(null)
    val gamificationProfile: StateFlow<GamificationProfileEntity?> = _gamificationProfile

    private val _leagueRank = MutableStateFlow(0)
    val leagueRank: StateFlow<Int> = _leagueRank

    private val _planDays = MutableStateFlow<List<PlanDayNode>>(emptyList())
    val planDays: StateFlow<List<PlanDayNode>> = _planDays

    private var cachedPlanDayEntities: List<ExamPlanDayEntity> = emptyList()

    private val _todayPlanDay = MutableStateFlow<ExamPlanDayEntity?>(null)
    val todayPlanDay: StateFlow<ExamPlanDayEntity?> = _todayPlanDay

    private val _todayQuest = MutableStateFlow<QuestDailyEntity?>(null)
    val todayQuest: StateFlow<QuestDailyEntity?> = _todayQuest

    private val _todayTrialItems = MutableStateFlow<List<PlanTrialItemEntity>>(emptyList())
    val todayTrialItems: StateFlow<List<PlanTrialItemEntity>> = _todayTrialItems

    private val _localizedTrialTitles = MutableStateFlow<Map<Long, String>>(emptyMap())
    val localizedTrialTitles: StateFlow<Map<Long, String>> = _localizedTrialTitles

    private val _rewardedAdReady = MutableStateFlow(false)
    val rewardedAdReady: StateFlow<Boolean> = _rewardedAdReady

    private val _friendFeedItems = MutableStateFlow<List<FriendFeedItemEntity>>(emptyList())

    private val _friendUpdates = MutableStateFlow<List<FriendUpdate>>(emptyList())
    val friendFeed: StateFlow<List<FriendUpdate>> = _friendUpdates

    private val _unseenFriendFeed = MutableStateFlow(0)
    val unseenFriendFeed: StateFlow<Int> = _unseenFriendFeed

    private val _friendCount = MutableStateFlow(0)
    val friendCount: StateFlow<Int> = _friendCount

    private val _youtubeVideos = MutableStateFlow<List<YoutubeVideo>>(emptyList())
    val youtubeVideos: StateFlow<List<YoutubeVideo>> = _youtubeVideos

    private val _gardenProgress = MutableStateFlow<GardenProgress?>(null)
    val gardenProgress: StateFlow<GardenProgress?> = _gardenProgress

    private val _gardenHighlightNewPlant = MutableStateFlow(false)
    val gardenHighlightNewPlant: StateFlow<Boolean> = _gardenHighlightNewPlant

    private val _gardenHighlightStarterPlant = MutableStateFlow(false)
    val gardenHighlightStarterPlant: StateFlow<Boolean> = _gardenHighlightStarterPlant

    private val _gardenPlantedItems = MutableStateFlow<List<GrownItemEntity>>(emptyList())
    val gardenPlantedItems: StateFlow<List<GrownItemEntity>> = _gardenPlantedItems

    // --- Search Feature State ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchSubject = MutableStateFlow("All")
    val searchSubject: StateFlow<String> = _searchSubject

    private val _searchLearningOption = MutableStateFlow("All / infer from search")
    val searchLearningOption: StateFlow<String> = _searchLearningOption

    private val _searchResults = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<SearchResultItem>> = _searchResults

    private val _hasSearched = MutableStateFlow(false)
    val hasSearched: StateFlow<Boolean> = _hasSearched

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private var whiteboardTitleToIdMap: Map<String, String> = emptyMap()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _hasSearched.value = false
    }
    fun updateSearchSubject(subject: String) { _searchSubject.value = subject }
    fun updateSearchLearningOption(option: String) { _searchLearningOption.value = option }

    fun performSearch() {
        viewModelScope.launch {
            val query = _searchQuery.value.trim().lowercase()
            val subjectFilter = _searchSubject.value
            val optionFilter = _searchLearningOption.value
            val isKn = _currentLanguage.value == "kn"

            _hasSearched.value = true

            if (query.isBlank()) {
                _searchResults.value = emptyList()
                return@launch
            }

            _isSearching.value = true
            delay(1000)

            try {
                val needsWbData = optionFilter.contains("All", ignoreCase = true) || optionFilter == "Whiteboard"
                if (needsWbData && whiteboardTitleToIdMap.isEmpty()) {
                    val wbResult = apiClient.getWhiteboardConcepts()
                    if (wbResult.isSuccess) {
                        whiteboardTitleToIdMap = wbResult.getOrNull()?.concepts
                            ?.filter { it.hasScene }
                            ?.associateBy({ it.title.trim().lowercase() }, { it.conceptId })
                            ?: emptyMap()
                    }
                }

                val allSubjects = subjectRepository.getSubjectsForClass(_student.value?.classLevel ?: 7)
                val chapterMap = mutableMapOf<String, com.ncert7.aitutorandlab.data.local.entities.ChapterEntity>()
                for (sub in allSubjects) {
                    chapterDao.getChaptersForSubjectSync(sub.subjectId).forEach { ch ->
                        chapterMap[ch.chapterId] = ch
                    }
                }

                val allConcepts = conceptDao.getConceptsByIds(conceptDao.getAllConceptsSync().map { it.conceptId }).first().ifEmpty { conceptDao.getAllConceptsSync() }

                val mappedResults = mutableListOf<SearchResultItem>()

                for (concept in allConcepts) {
                    val title = if (isKn && concept.conceptNameKannada.isNotBlank()) concept.conceptNameKannada else concept.conceptName
                    if (!title.lowercase().contains(query)) continue

                    val chapter = chapterMap[concept.chapterId]
                    val subjectId = chapter?.subjectId ?: ""
                    val subjectName = allSubjects.find { it.subjectId == subjectId }?.getLocalizedName(_currentLanguage.value) ?: subjectId

                    val isMath = subjectId.contains("MATH", ignoreCase = true) || subjectName.contains("Math", ignoreCase = true)

                    val subjectMatch = when (subjectFilter) {
                        "Science" -> !isMath
                        "Math" -> isMath
                        else -> true
                    }

                    val localTitleLower = concept.conceptName.trim().lowercase()
                    val wbConceptId = whiteboardTitleToIdMap[localTitleLower]
                    val isWb = wbConceptId != null

                    val optionMatch = when (optionFilter) {
                        "Whiteboard" -> isWb
                        else -> true
                    }

                    if (subjectMatch && optionMatch) {
                        val chapterName = if (isKn && chapter?.chapterNameKannada?.isNotBlank() == true) chapter.chapterNameKannada else chapter?.chapterName ?: "Unknown Chapter"
                        mappedResults.add(
                            SearchResultItem(
                                conceptId = concept.conceptId,
                                topicName = title,
                                subjectName = subjectName,
                                isWhiteboardAvailable = isWb,
                                isMath = isMath,
                                problemId = concept.problemId ?: "",
                                chapterId = concept.chapterId,
                                wbConceptId = wbConceptId
                            )
                        )
                    }
                }
                _searchResults.value = mappedResults.take(20)
            } catch (e: Exception) {
                DebugLogger.errorLog("HomeViewModel", "Search failure: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun surpriseMe() {
        viewModelScope.launch {
            _hasSearched.value = true
            _isSearching.value = true
            delay(1000)

            try {
                val isKn = _currentLanguage.value == "kn"
                val subjectFilter = _searchSubject.value
                val optionFilter = _searchLearningOption.value

                val needsWbData = optionFilter.contains("All", ignoreCase = true) || optionFilter == "Whiteboard"
                if (needsWbData && whiteboardTitleToIdMap.isEmpty()) {
                    val wbResult = apiClient.getWhiteboardConcepts()
                    if (wbResult.isSuccess) {
                        whiteboardTitleToIdMap = wbResult.getOrNull()?.concepts
                            ?.filter { it.hasScene }
                            ?.associateBy({ it.title.trim().lowercase() }, { it.conceptId })
                            ?: emptyMap()
                    }
                }

                val allSubjects = subjectRepository.getSubjectsForClass(_student.value?.classLevel ?: 7)
                val chapterMap = mutableMapOf<String, com.ncert7.aitutorandlab.data.local.entities.ChapterEntity>()
                for (sub in allSubjects) {
                    chapterDao.getChaptersForSubjectSync(sub.subjectId).forEach { ch ->
                        chapterMap[ch.chapterId] = ch
                    }
                }

                val allConcepts = conceptDao.getConceptsByIds(conceptDao.getAllConceptsSync().map { it.conceptId }).first().ifEmpty { conceptDao.getAllConceptsSync() }

                val validConcepts = mutableListOf<SearchResultItem>()

                for (concept in allConcepts) {
                    val title = if (isKn && concept.conceptNameKannada.isNotBlank()) concept.conceptNameKannada else concept.conceptName
                    val chapter = chapterMap[concept.chapterId]
                    val subjectId = chapter?.subjectId ?: ""
                    val subjectName = allSubjects.find { it.subjectId == subjectId }?.getLocalizedName(_currentLanguage.value) ?: subjectId
                    val isMath = subjectId.contains("MATH", ignoreCase = true) || subjectName.contains("Math", ignoreCase = true)

                    val subjectMatch = when (subjectFilter) {
                        "Science" -> !isMath
                        "Math" -> isMath
                        else -> true
                    }

                    val localTitleLower = concept.conceptName.trim().lowercase()
                    val wbConceptId = whiteboardTitleToIdMap[localTitleLower]
                    val isWb = wbConceptId != null

                    val optionMatch = when (optionFilter) {
                        "Whiteboard" -> isWb
                        else -> true
                    }

                    if (subjectMatch && optionMatch) {
                        val chapterName = if (isKn && chapter?.chapterNameKannada?.isNotBlank() == true) chapter.chapterNameKannada else chapter?.chapterName ?: "Unknown Chapter"
                        validConcepts.add(
                            SearchResultItem(
                                conceptId = concept.conceptId,
                                topicName = title,
                                subjectName = subjectName,
                                isWhiteboardAvailable = isWb,
                                isMath = isMath,
                                problemId = concept.problemId ?: "",
                                chapterId = concept.chapterId,
                                wbConceptId = wbConceptId
                            )
                        )
                    }
                }
                _searchResults.value = validConcepts.shuffled().take(5)
            } catch (e: Exception) {
                DebugLogger.errorLog("HomeViewModel", "Surprise me failure: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }
    // --- End Search Block ---

    fun setLanguage(lang: String) {
        syncLanguage(lang)
    }

    fun syncLanguage(lang: String) {
        val normalized = normalizeLanguageCode(lang)
        if (_currentLanguage.value == normalized) return
        _currentLanguage.value = normalized
        DebugLogger.debugLog("HomeViewModel", "Language dynamically changed to: $normalized")
        if (cachedPlanDayEntities.isNotEmpty()) {
            viewModelScope.launch {
                remapLocalizedPlanDays(cachedPlanDayEntities, normalized)
            }
        }
        viewModelScope.launch {
            remapLocalizedTrialTitles(_todayTrialItems.value, normalized)
        }
        refreshSelectedSubjectName()
    }

    private suspend fun remapLocalizedTrialTitles(
        items: List<PlanTrialItemEntity>,
        language: String,
    ) {
        if (items.isEmpty()) {
            _localizedTrialTitles.value = emptyMap()
            return
        }
        _localizedTrialTitles.value =
            items.associate { item ->
                item.id to
                        TrialTitleResolver.localizedItemTitle(
                            entity = item,
                            languageCode = language,
                            conceptDao = conceptDao,
                            chapterDao = chapterDao,
                        )
            }
    }

    private suspend fun remapLocalizedPlanDays(
        entities: List<ExamPlanDayEntity>,
        language: String,
    ) {
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

    fun refreshSelectedSubjectName() {
        viewModelScope.launch {
            val language = _currentLanguage.value
            val subjectId = sharedPrefs.getSubjectSelectionId()
            val subject = subjectRepository.getSubject(subjectId)
            _selectedSubjectName.value = subject?.getLocalizedName(language) ?: ""
        }
    }

    fun refreshAvailableSubjects() {
        viewModelScope.launch {
            val classLevel = _student.value?.classLevel ?: 7
            _availableSubjects.value =
                subjectRepository.getSubjectsForClass(classLevel).sortedBy { it.orderIndex }
        }
    }

    private fun observeAvailableSubjects() {
        viewModelScope.launch {
            combine(_student, _currentLanguage) { student, _ -> student?.classLevel ?: 7 }
                .collectLatest { classLevel ->
                    _availableSubjects.value =
                        subjectRepository.getSubjectsForClass(classLevel).sortedBy { it.orderIndex }
                }
        }
    }

    private fun observeChapterCounts() {
        viewModelScope.launch {
            chapterDao.getChapterCountsBySubjectFlow().collectLatest { rows ->
                _chapterCounts.value = rows.associate { it.subjectId to it.chapterCount }
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeChapterProgressSums() {
        viewModelScope.launch {
            combine(_student, _currentLanguage) { student, lang ->
                val id = student?.studentId?.takeIf { it.isNotBlank() } ?: userId
                id to lang
            }.distinctUntilChanged()
                .flatMapLatest { (id, lang) ->
                    if (id.isBlank()) {
                        flowOf(emptyList())
                    } else {
                        chapterAgentProgressDao.getChapterProgressSumsBySubjectFlow(
                            studentId = id,
                            language = lang,
                            appName = AppConfig.APP_NAME,
                        )
                    }
                }
                .collectLatest { rows ->
                    _chapterProgressSums.value = rows.associate { it.subjectId to it.progressSum }
                }
        }
    }

    val startOfDay = LocalDate.now()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    val endOfDay = LocalDate.now()
        .plusDays(1)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli() - 1

    init {
        getStudent()
        observeStreak()
        recordAppOpenStreak()
        observeTodayProgress()
        observeTotalCounts()
        observeProgressConceptsAndSimulations()
        observeSelectedSubjectName()
        observeAvailableSubjects()
        observeChapterCounts()
        observeChapterProgressSums()
        observeGamificationProfile()
        observeLeagueRank()
        observeExamPlan()
        observeTodayTrialItems()
        backfillTrialChapterProgress()
        observeDailyQuests()
        observeFriendFeed()
        loadYoutubeVideos()
        observeGardenProgress()
        applyOnboardingPicksOnce()
    }

    private fun recordAppOpenStreak() {
        viewModelScope.launch {
            val id = userId.takeIf { it.isNotBlank() } ?: return@launch
            try {
                val count = streakRepository.recordActivity(id)
                DebugLogger.debugLog("HomeViewModel", "App-open streak → $count")
            } catch (e: Exception) {
                DebugLogger.errorLog("HomeViewModel", "App-open streak failed: ${e.message}")
            }
        }
    }

    private fun applyOnboardingPicksOnce() {
        viewModelScope.launch {
            if (sharedPrefs.hasAppliedOnboardingPicks()) return@launch
            val id = userId.takeIf { it.isNotBlank() } ?: return@launch
            DataSyncService.awaitGardenRestore()
            val language = _currentLanguage.value
            val subjectKey = sharedPrefs.getOnboardingSubject().orEmpty()
            val chapterLabel = sharedPrefs.getOnboardingChapter().orEmpty()
            val world = sharedPrefs.getOnboardingWorld().orEmpty()

            val subjectId =
                OnboardingChapterCatalog.subjectIdForKey(subjectKey)
                    ?: subjectRepository.getSubjectsForClass(_student.value?.classLevel ?: 7)
                        .firstOrNull {
                            it.subjectName.trim().startsWith(subjectKey.trim(), ignoreCase = true)
                        }?.subjectId

            if (subjectId != null) {
                sharedPrefs.setSubjectSelectionId(subjectId)
                refreshSelectedSubjectName()
            }

            if (subjectId != null && chapterLabel.isNotBlank()) {
                val existingPlan = examPlanRepository.getActivePlan(id)
                if (existingPlan == null) {
                    val chapters = chapterDao.getChaptersForSubjectSync(subjectId)
                    val chapterEntity =
                        OnboardingChapterCatalog.resolveChapter(chapters, chapterLabel, language)
                            ?: chapters.sortedBy { it.orderIndex }.firstOrNull()
                    if (chapterEntity != null) {
                        try {
                            examPlanRepository.createOnboardingPlan(
                                studentId = id,
                                subjectId = subjectId,
                                chapterId = chapterEntity.chapterId,
                                languageCode = language,
                            )
                            refreshExamPlanStatuses()
                        } catch (e: Exception) {
                            DebugLogger.errorLog(
                                "HomeViewModel",
                                "Onboarding plan creation failed: ${e.message}",
                            )
                        }
                    }
                }
            }

            val gardenTheme =
                if (world.equals("Space", ignoreCase = true)) GardenTheme.OUTPOST else GardenTheme.GARDEN
            val remoteGardenApplied = DataSyncService.wasGardenRestoredFromRemote()
            val hasRestoredPlants = (gardenRepository.getProgress(id)?.totalPlanted ?: 0) > 0
            if (!remoteGardenApplied && !hasRestoredPlants) {
                gardenRepository.setTheme(id, gardenTheme)
                gardenRepository.applyOnboardingStartingScene(id, gardenTheme)
            }
            _gardenProgress.value = gardenRepository.getProgress(id)
            refreshGardenPlantedItems(id)

            sharedPrefs.getOnboardingAvatar()?.takeIf { it.isNotBlank() }?.let { presetId ->
                AvatarUnlockStore.unlock(appContext, presetId)
                runCatching {
                    tutorConfigRepository.applyPreset(appContext, id, presetId)
                }.onFailure { e ->
                    DebugLogger.errorLog(
                        "HomeViewModel",
                        "Onboarding avatar apply failed: ${e.message}",
                    )
                }
            }

            sharedPrefs.setOnboardingPicksApplied()
            try {
                com.ncert7.aitutorandlab.repository.FirebaseRepository()
                    .markOnboardingPicksApplied(
                        userId = id,
                        subjectId = subjectId,
                        chapterId = null,
                    )
            } catch (e: Exception) {
                DebugLogger.errorLog(
                    "HomeViewModel",
                    "Onboarding picks cloud mark failed: ${e.message}",
                )
            }
        }
    }

    fun setGardenPreferredSlot(slot: Int) {
        viewModelScope.launch {
            val id = _student.value?.studentId?.takeIf { it.isNotBlank() } ?: userId
            if (id.isBlank()) return@launch
            gardenRepository.setPreferredSlot(id, slot)
            _gardenProgress.value = gardenRepository.getProgress(id)
        }
    }

    fun refreshGardenOnHomeResume() {
        viewModelScope.launch {
            val id = _student.value?.studentId?.takeIf { it.isNotBlank() } ?: userId
            val progress =
                if (id.isBlank()) null else gardenRepository.getProgress(id)
            _gardenProgress.value = progress
            refreshGardenPlantedItems(id)
            if (progress != null && id.isNotBlank()) {
                val theme = gardenRepository.toComposeTheme(progress.theme)
                val starterSeen = sharedPrefs.hasSeenGardenStarterPlantHighlight(id)
                _gardenHighlightStarterPlant.value =
                    GardenStarterHighlight.shouldShow(progress, theme, starterSeen)
                val lastSeen = sharedPrefs.getLastHomeGardenPlantTotal(id)
                if (progress.totalPlanted > lastSeen) {
                    _gardenHighlightNewPlant.value = true
                }
            } else {
                _gardenHighlightStarterPlant.value = false
            }
        }
    }

    fun acknowledgeGardenStarterHighlight() {
        _gardenHighlightStarterPlant.value = false
        viewModelScope.launch {
            val id = _student.value?.studentId?.takeIf { it.isNotBlank() } ?: userId
            if (id.isNotBlank()) {
                sharedPrefs.setGardenStarterPlantHighlightSeen(id)
            }
        }
    }

    fun acknowledgeGardenHighlight() {
        _gardenHighlightNewPlant.value = false
        viewModelScope.launch {
            val id = _student.value?.studentId?.takeIf { it.isNotBlank() } ?: userId
            val totalPlanted = _gardenProgress.value?.totalPlanted ?: return@launch
            if (id.isNotBlank()) {
                sharedPrefs.setLastHomeGardenPlantTotal(id, totalPlanted)
            }
        }
    }

    fun refreshGardenProgress() {
        viewModelScope.launch {
            val id = _student.value?.studentId?.takeIf { it.isNotBlank() } ?: userId
            _gardenProgress.value =
                if (id.isBlank()) null else gardenRepository.getProgress(id)
            refreshGardenPlantedItems(id)
        }
    }

    private suspend fun refreshGardenPlantedItems(studentId: String) {
        _gardenPlantedItems.value =
            if (studentId.isBlank()) {
                emptyList()
            } else {
                gardenRepository.getPlantedItems(studentId)
            }
    }

    private fun observeGardenProgress() {
        viewModelScope.launch {
            DataSyncService.awaitGardenRestore()
            combine(_student, _todayTrialItems) { student, items ->
                val studentId = student?.studentId?.takeIf { it.isNotBlank() } ?: userId
                studentId to trialProgressSignature(items)
            }
                .distinctUntilChanged()
                .collectLatest { (studentId, _) ->
                    _gardenProgress.value =
                        if (studentId.isBlank()) null else gardenRepository.getProgress(studentId)
                    refreshGardenPlantedItems(studentId)
                }
        }
        viewModelScope.launch {
            DataSyncService.awaitGardenRestore()
            _student
                .flatMapLatest { student ->
                    val studentId = student?.studentId?.takeIf { it.isNotBlank() } ?: userId
                    if (studentId.isBlank()) {
                        flowOf(null)
                    } else {
                        gardenRepository.observeProgress(studentId)
                    }
                }
                .collectLatest { progress ->
                    if (progress != null) {
                        _gardenProgress.value = progress
                        val studentId =
                            _student.value?.studentId?.takeIf { it.isNotBlank() } ?: userId
                        refreshGardenPlantedItems(studentId)
                        if (studentId.isNotBlank()) {
                            val lastSeen = sharedPrefs.getLastHomeGardenPlantTotal(studentId)
                            if (progress.totalPlanted > lastSeen) {
                                _gardenHighlightNewPlant.value = true
                            }
                        }
                    }
                }
        }
    }

    private fun trialProgressSignature(items: List<PlanTrialItemEntity>): String =
        items.joinToString("|") { "${it.id}:${it.status}:${it.completedCount}" }

    fun mapYoutubeItems(languageCode: String): List<YoutubeVideoItem> =
        GamifiedHomeMapper.mapYoutubeVideos(
            videos = _youtubeVideos.value,
            languageCode = normalizeLanguageCode(languageCode),
            youtubeVideoRepository = youtubeVideoRepository,
        )

    fun refreshYoutubeVideos() {
        viewModelScope.launch {
            _youtubeVideos.value = youtubeVideoRepository.fetchVideos()
        }
    }

    private fun loadYoutubeVideos() {
        viewModelScope.launch {
            _youtubeVideos.value = youtubeVideoRepository.fetchVideos()
        }
    }

    private fun observeFriendFeed() {
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            friendRepository.debugPurgeSelfFeed(userId)
            friendRepository.seedDemoFriendRequestsIfNeeded(userId)
            friendFeedService.simulateBotFriendFeedIfNeeded(userId)
            friendRepository.syncFriendSocialData(userId)
        }
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            combine(
                friendRepository.observeHomeFeed(userId),
                friendRepository.observeConnections(userId),
                friendRepository.observePendingRequests(userId),
                _currentLanguage,
            ) { items, connections, pending, language ->
                _friendFeedItems.value = items
                _friendCount.value = connections.size
                val requests = FriendUiMapper.toFriendRequestUpdates(pending, language)
                val feedOrLinked =
                    when {
                        items.isNotEmpty() -> FriendUiMapper.toFriendUpdates(items, userId, language)
                        connections.isNotEmpty() -> FriendUiMapper.toLinkedFriendUpdates(connections, language)
                        else -> emptyList()
                    }
                _friendUpdates.value = requests + feedOrLinked
            }.collectLatest { }
        }
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            friendRepository.observeUnseenFeedCount(userId).collectLatest { count ->
                _unseenFriendFeed.value = count
            }
        }
    }

    fun refreshFriendSocialData() {
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            friendRepository.syncFriendSocialData(userId)
        }
    }

    fun cheerFriendAtIndex(index: Int) {
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            val update = _friendUpdates.value.getOrNull(index) ?: return@launch
            if (update.isRequest) {
                val accepted = friendRepository.acceptFriendRequest(userId, update.requestFriendId)
                if (accepted) {
                    val bot = friendRepository.getAcceptedDemoBots(userId)
                        .firstOrNull { it.friendStudentId == update.requestFriendId }
                    if (bot != null) {
                        friendFeedService.seedFeedFromAcceptedBot(
                            ownerStudentId = userId,
                            botId = bot.friendStudentId,
                            botName = bot.displayName,
                        )
                    }
                }
                return@launch
            }
            if (update.feedItemId < 0L) return@launch
            friendRepository.cheerFeedItem(userId, update.feedItemId)
            GamificationAnalyticsTracker.cheerSent()
        }
    }

    fun markFriendFeedSeen() {
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            friendRepository.markHomeFeedSeen(userId)
        }
    }

    suspend fun getMyFriendCode(): String =
        if (userId.isEmpty()) "" else friendRepository.getMyFriendCode(userId)

    private fun observeExamPlan() {
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            examPlanRepository.ensureActivePlan(
                studentId = userId,
                subjectId = sharedPrefs.getSubjectSelectionId(),
                languageCode = _currentLanguage.value,
            )
        }
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            combine(
                examPlanRepository.observePlanDays(userId),
                _currentLanguage,
            ) { entities, language -> entities to language }
                .collectLatest { (entities, language) ->
                    cachedPlanDayEntities = entities
                    remapLocalizedPlanDays(entities, language)
                    val scheduleDays = entities.filter { it.isExamScheduleDay() }
                    _todayPlanDay.value =
                        scheduleDays.firstOrNull { it.status == "TODAY" }
                            ?: scheduleDays.firstOrNull { it.status == "UPCOMING" }
                }
        }
    }

    fun ensureExamPlanForCurrentSubject() {
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            examPlanRepository.ensureActivePlan(
                studentId = userId,
                subjectId = sharedPrefs.getSubjectSelectionId(),
                languageCode = _currentLanguage.value,
            )
        }
    }

    fun refreshExamPlanStatuses() {
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            examPlanRepository.refreshDayStatuses(userId, _currentLanguage.value)
            questRepository.refreshTodayQuest(userId, _currentLanguage.value)
        }
    }

    suspend fun chapterIdForConcept(conceptId: String): String? =
        conceptDao.getConcept(conceptId)?.chapterId

    fun refreshDailyQuests() {
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            questRepository.refreshTodayQuest(userId, _currentLanguage.value)
        }
    }

    fun refreshRewardedAdState() {
        _rewardedAdReady.value = questGemRewardService.isRewardedAdReady()
    }

    fun preloadRewardedAd() {
        questGemRewardService.preloadRewardedAd()
        refreshRewardedAdState()
    }

    fun claimQuestWithAd(
        activity: Activity,
        claimType: QuestClaimType,
        onResult: (QuestClaimResult) -> Unit,
    ) {
        viewModelScope.launch {
            val result = questGemRewardService.claimWithRewardedAd(activity, userId, claimType)
            refreshRewardedAdState()
            onResult(result)
        }
    }

    private fun backfillTrialChapterProgress() {
        viewModelScope.launch {
            val id = userId.takeIf { it.isNotBlank() } ?: return@launch
            planTrialProgressTracker.creditAlreadyDoneItems(id)
        }
    }

    private fun observeTodayTrialItems() {
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            _todayPlanDay.collectLatest { day ->
                if (day == null) return@collectLatest
                if (sharedPrefs.hasCompletedFirstRun() && !sharedPrefs.hasAppliedOnboardingPicks()) {
                    return@collectLatest
                }
                planTrialRepository.ensureTrialItemsForDay(day, _currentLanguage.value)
            }
        }
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            _todayPlanDay
                .flatMapLatest { day ->
                    if (day == null) {
                        flowOf(emptyList())
                    } else {
                        planTrialRepository.observeTrialItems(userId, day.dayIndex)
                    }
                }.collect { items ->
                    _todayTrialItems.value = items
                    remapLocalizedTrialTitles(items, _currentLanguage.value)
                }
        }
    }

    private fun observeDailyQuests() {
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            _currentLanguage.collectLatest { language ->
                questRepository.refreshTodayQuest(userId, language)
            }
        }
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            questRepository.observeTodayQuest(userId).collectLatest { quest ->
                _todayQuest.value = quest
            }
        }
    }

    private fun observeGamificationProfile() {
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            gamificationRepository.getOrCreateProfile(userId)
            friendRepository.syncFriendCodeToRemote(userId)
            gamificationRepository.observeProfile(userId).collectLatest { profile ->
                _gamificationProfile.value = profile
                if (profile != null) {
                    syncLeagueMember(profile)
                }
            }
        }
    }

    private fun observeLeagueRank() {
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            leagueRepository.observeCachedRank(userId).collectLatest { rank ->
                _leagueRank.value = rank
            }
        }
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            refreshHomeLeagueRank()
        }
    }

    fun refreshHomeLeagueRank() {
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            val student = studentDao.getStudentSync(userId)
            val streak = _streakCount.value
            leagueRepository.refreshHomeLeagueCache(
                studentId = userId,
                displayName = student?.studentName.orEmpty(),
                streak = streak,
            )
        }
    }

    private suspend fun syncLeagueMember(GamificationProfileEntity: GamificationProfileEntity) {
        val student = studentDao.getStudentSync(userId)
        leagueRepository.syncUserWeeklyXp(
            studentId = userId,
            weeklyXp = GamificationProfileEntity.weeklyXp,
            displayName = student?.studentName.orEmpty(),
            streak = _streakCount.value,
        )
    }

    private fun observeSelectedSubjectName() {
        viewModelScope.launch {
            _currentLanguage.collectLatest {
                refreshSelectedSubjectName()
            }
        }
    }

    private fun observeProgressConceptsAndSimulations() {
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            _currentLanguage.collectLatest { language ->
                kotlinx.coroutines.coroutineScope {
                    launch {
                        progressDao.getAllProgress(userId, AppConfig.APP_NAME)
                            .collectLatest { allProgressList ->
                                val allProgress = allProgressList.filter {
                                    it.itemType == "CONCEPT" && it.language == language
                                }

                                val completedList = allProgress
                                    .filter { it.status == "COMPLETED" }
                                    .sortedByDescending { it.completedAt ?: 0L }

                                val inProgressList = allProgress
                                    .filter { it.status == "IN_PROGRESS" }
                                    .sortedByDescending { it.lastAccessedAt }

                                val curatedProgress = mutableListOf<ProgressEntity>()
                                curatedProgress.addAll(inProgressList)

                                val remainingSlots = (4 - curatedProgress.size).coerceAtLeast(0)
                                if (remainingSlots > 0) {
                                    curatedProgress.addAll(
                                        completedList.take(remainingSlots)
                                    )
                                }

                                if (curatedProgress.isEmpty()) {
                                    val firstUnitConcepts = conceptDao.getFirstConceptsOfChapter("1", "STUDY", 4)
                                    val combined = firstUnitConcepts.map { concept ->
                                        null to concept
                                    }
                                    progressConcepts.value = combined
                                } else {
                                    val conceptIds = curatedProgress.map { it.itemId }
                                    val concepts = conceptDao.getConceptsByIds(conceptIds).first()
                                    val combined = curatedProgress.map { progress ->
                                        val concept = concepts.find { it.conceptId == progress.itemId }
                                        progress to concept
                                    }
                                    progressConcepts.value = combined
                                }
                            }
                    }

                    launch {
                        progressDao.getAllProgress(userId, AppConfig.APP_NAME)
                            .collectLatest { allProgressList ->
                                val allProgress = allProgressList.filter {
                                    (it.itemType == "SIMULATION" || it.itemType == "SIMULATION_AGENT") && it.language == language
                                }

                                val completedList = allProgress
                                    .filter { it.status == "COMPLETED" }
                                    .sortedByDescending { it.completedAt ?: 0L }

                                val inProgressList = allProgress
                                    .filter { it.status == "IN_PROGRESS" }
                                    .sortedByDescending { it.lastAccessedAt }

                                val curatedProgress = mutableListOf<ProgressEntity>()
                                curatedProgress.addAll(inProgressList)

                                val remainingSlots = (4 - curatedProgress.size).coerceAtLeast(0)
                                if (remainingSlots > 0) {
                                    curatedProgress.addAll(completedList.take(remainingSlots))
                                }

                                if (curatedProgress.isEmpty()) {
                                    val firstUnitSimulations = conceptDao.getFirstConceptsOfChapter("1", "SIMULATION", 4)
                                    val combined = firstUnitSimulations.map { concept ->
                                        null to concept
                                    }
                                    progressSimulations.value = combined
                                } else {
                                    val conceptIds = curatedProgress.map { it.itemId }
                                    val concepts = conceptDao.getConceptsByIds(conceptIds).first()
                                    val combined = curatedProgress.map { progress ->
                                        val concept = concepts.find { it.conceptId == progress.itemId }
                                        progress to concept
                                    }
                                    progressSimulations.value = combined
                                }
                            }
                    }
                }
            }
        }
    }

    private fun observeStreak() {
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            var previous: Int? = null
            streakRepository.getStreakFlow(userId).collectLatest { streak ->
                val count = streakRepository.effectiveDisplayStreak(streak)
                _streakCount.value = count
                val prev = previous
                if (prev != null && count > prev) {
                    _streakExtended.value = count
                    sharedPrefs.setStreakGreetingShownToday()
                }
                previous = count
                if (count >= 1 &&
                    _streakExtended.value == null &&
                    !sharedPrefs.wasStreakGreetingShownToday()
                ) {
                    _dailyStreakGreeting.value = count
                }
            }
        }
    }

    fun acknowledgeDailyStreakGreeting() {
        sharedPrefs.setStreakGreetingShownToday()
        _dailyStreakGreeting.value = null
    }

    fun acknowledgeStreakExtended() {
        _streakExtended.value = null
    }


    private fun observeTodayProgress() {
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            _currentLanguage.collectLatest { language ->
                kotlinx.coroutines.coroutineScope {
                    launch {
                        progressDao.getTodayCompletedConceptCountFlow(userId, language, startOfDay, endOfDay, AppConfig.APP_NAME)
                            .collectLatest { count ->
                                _todayConceptCount.value = count
                            }
                    }
                    launch {
                        progressDao.getTodayCompletedSimulationCountFlow(userId, language, startOfDay, endOfDay, AppConfig.APP_NAME)
                            .collectLatest { count ->
                                _todaySimulationCount.value = count
                            }
                    }
                }
            }
        }
    }

    private fun observeTotalCounts() {
        viewModelScope.launch {
            if (userId.isEmpty()) return@launch
            _currentLanguage.collectLatest { language ->
                kotlinx.coroutines.coroutineScope {
                    launch {
                        progressDao.getTotalCompletedConceptsFlow(userId, language, AppConfig.APP_NAME)
                            .collectLatest { count ->
                                _totalCompletedConcept.value = count
                            }
                    }
                    launch {
                        progressDao.getTotalCompletedSimulationsFlow(userId, language, AppConfig.APP_NAME)
                            .collectLatest { count ->
                                _totalCompletedSimulation.value = count
                            }
                    }
                }
            }
        }
    }

    fun getGreeting() {
        val hour = LocalTime.now().hour

        _greeting.value = when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Good Night"
        }
    }

    fun getStudent() {
        viewModelScope.launch {
            val result = studentDao.getStudentSync(userId)
            _student.value = result
            _studentLoaded.value = true
            refreshAvailableSubjects()
        }
    }

    fun onLanguageChanged() {
        _languageChangeTrigger.value += 1
    }

    fun requestOpenPlanSetup() {
        sharedPrefs.setOpenExamPlanSetupPending(true)
    }
}