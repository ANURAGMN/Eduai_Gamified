package com.ncert7.aitutorandlab.ui.screens.home

import com.anurag.eduai.uikit.garden.GardenHomeCopy
import com.anurag.eduai.uikit.components.BookmarkItem
import com.anurag.eduai.uikit.components.FriendUpdate
import com.anurag.eduai.uikit.components.PlanDayNode
import com.anurag.eduai.uikit.components.PlanDayStatus
import com.anurag.eduai.uikit.components.GardenRailState
import com.anurag.eduai.uikit.components.QuestTrailState
import com.anurag.eduai.uikit.components.RevisionItem
import com.anurag.eduai.uikit.components.SubjectTile
import com.anurag.eduai.uikit.screens.HomeUiState
import com.anurag.eduai.uikit.theme.EduChipRole
import com.ncert7.aitutorandlab.data.local.entities.ConceptEntity
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanDayEntity
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemEntity
import com.ncert7.aitutorandlab.data.local.entities.ProgressEntity
import com.ncert7.aitutorandlab.data.local.entities.QuestDailyEntity
import com.ncert7.aitutorandlab.data.local.entities.SubjectEntity
import com.anurag.eduai.uikit.garden.quest.GardenPlantedRow
import com.anurag.eduai.uikit.garden.quest.SLOTS_PER_ZONE
import com.anurag.eduai.uikit.garden.quest.starterSlot
import com.anurag.eduai.uikit.garden.quest.Theme
import com.anurag.eduai.uikit.garden.quest.ThemeCopy
import com.anurag.eduai.uikit.garden.quest.ZONES
import com.anurag.eduai.uikit.garden.quest.placeBased
import com.ncert7.aitutorandlab.domain.garden.GardenSlotResolver
import com.ncert7.aitutorandlab.data.local.entities.GardenTheme
import com.anurag.eduai.uikit.garden.CollectionShelfState
import com.anurag.eduai.uikit.garden.CollectionShelfItem
import com.ncert7.aitutorandlab.domain.garden.GardenProgress
import com.ncert7.aitutorandlab.data.local.entities.GrownItemEntity
import com.ncert7.aitutorandlab.domain.examplan.TrialQuestProgress
import com.ncert7.aitutorandlab.ui.screens.plan.ExamPlanUiMapper
import com.ncert7.aitutorandlab.ui.screens.plan.TrialItemTitleParser
import com.ncert7.aitutorandlab.ui.screens.plan.TrialQuestNavigation
import com.anurag.eduai.uikit.screens.YoutubeVideoItem
import com.ncert7.aitutorandlab.domain.youtube.YoutubeVideo
import com.ncert7.aitutorandlab.repository.YoutubeVideoRepository
import com.ncert7.aitutorandlab.utils.SubjectIds
import com.ncert7.aitutorandlab.utils.SubjectIconUrls
import com.ncert7.aitutorandlab.utils.resolveSubjectIconUrl
import com.ncert7.aitutorandlab.utils.getLocalizedName
import com.ncert7.aitutorandlab.utils.GardenCopyFactory
import com.ncert7.aitutorandlab.utils.HomeCopy
import com.ncert7.aitutorandlab.utils.GardenWorldLabels
import com.ncert7.aitutorandlab.utils.isKannadaLanguage
import com.ncert7.aitutorandlab.domain.gamification.EconomyConfig

/** Navigation targets derived from the mapped home state. */
data class GamifiedHomeFocus(
    val conceptId: String? = null,
    val simulationId: String? = null,
    val simulationConceptId: String? = null,
    val pendingSimTrialItemId: Long? = null,
    val pendingSimTrialRoute: String? = null,
    val pendingStudyTrialItemId: Long? = null,
    val pendingStudyTrialRoute: String? = null,
    /** Next item in today's trial queue — hero Start uses this. */
    val pendingNextTrialItemId: Long? = null,
    val pendingNextTrialRoute: String? = null,
)

object GamifiedHomeMocks {
    const val GEMS = 240
    const val LEAGUE_NAME = "Silver"
    const val LEAGUE_RANK = 4
}

object GamifiedHomeMapper {

    fun map(
        greeting: String,
        userName: String,
        streak: Int,
        todayConceptCount: Int,
        todaySimulationCount: Int,
        selectedSubjectName: String,
        selectedSubjectId: String = "",
        progressConcepts: List<Pair<ProgressEntity?, ConceptEntity?>>,
        progressSimulations: List<Pair<ProgressEntity?, ConceptEntity?>>,
        languageCode: String,
        gems: Int = GamifiedHomeMocks.GEMS,
        leagueName: String = GamifiedHomeMocks.LEAGUE_NAME,
        leagueRank: Int = 0,
        weeklyXp: Int = 0,
        planDays: List<PlanDayNode> = emptyList(),
        todayPlanDay: ExamPlanDayEntity? = null,
        todayQuest: QuestDailyEntity? = null,
        todayTrialItems: List<PlanTrialItemEntity> = emptyList(),
        friends: List<com.anurag.eduai.uikit.components.FriendUpdate> = emptyList(),
        friendCount: Int = 0,
        availableSubjects: List<SubjectEntity> = emptyList(),
        gardenEnabled: Boolean = false,
        gardenProgress: GardenProgress? = null,
        gardenHighlightNewPlant: Boolean = false,
        gardenHighlightStarterPlant: Boolean = false,
        gardenPlantedItems: List<GrownItemEntity> = emptyList(),
    ): Pair<HomeUiState, GamifiedHomeFocus> {
        val planConceptId = ExamPlanUiMapper.firstConceptId(todayPlanDay)
        val focusConcept = pickFocusConcept(progressConcepts, planConceptId)
        val focusSimulation = pickFocusSimulation(progressSimulations, planConceptId)
        val focusConceptName =
            focusConcept?.getLocalizedName(languageCode)
                ?: focusSimulation?.second?.getLocalizedName(languageCode)
                .orEmpty()

        val planTodayDone = todayPlanDay?.status == "DONE"
        val questProgressDone =
            todayQuest?.let { quest ->
                (quest.simsTotal <= 0 || quest.simsDone >= quest.simsTotal) &&
                    (quest.studyTotal <= 0 || quest.studyDone >= quest.studyTotal)
            } ?: false
        val todayDone =
            planTodayDone ||
                questProgressDone ||
                (todayPlanDay == null && todayConceptCount >= 1 && todaySimulationCount >= 1)

        val xpEstimate = (todayConceptCount + todaySimulationCount) * EconomyConfig.XP_CONCEPT
        val todayPlanLabel =
            planDays.firstOrNull { it.status == PlanDayStatus.Today }?.label
                ?: todayPlanDay?.label.orEmpty()
        val nextPending = TrialQuestNavigation.firstPendingInQueue(todayTrialItems)
        val nextPendingItem = nextPending?.let { id -> todayTrialItems.firstOrNull { it.id == id.itemId } }
        val heroContent =
            buildHeroContent(
                todayPlanDay = todayPlanDay,
                todayPlanLabel = todayPlanLabel,
                nextPendingItem = nextPendingItem,
                fallbackConceptName = focusConceptName,
                selectedSubjectName = selectedSubjectName,
                todayDone = todayDone,
                languageCode = languageCode,
            )

        val gardenRail =
            mapGarden(
                enabled = gardenEnabled,
                progress = gardenProgress,
                highlightNewPlant = gardenHighlightNewPlant,
                highlightStarterPlant = gardenHighlightStarterPlant,
                conceptTitle =
                    TrialItemTitleParser.heroTitle(
                        item = nextPendingItem,
                        todayPlanLabel = todayPlanLabel,
                        fallbackConceptName = focusConceptName,
                        selectedSubjectName = selectedSubjectName,
                    ),
                kindLabel = trialKindLabel(todayTrialItems, languageCode),
                languageCode = languageCode,
                plantedItems = gardenPlantedItems,
            )
        val standaloneCollection =
            if (gardenEnabled && gardenProgress != null && gardenRail == null) {
                mapCollectionShelfFromEntities(gardenProgress, gardenPlantedItems, languageCode)
            } else {
                null
            }

        val state =
            HomeUiState(
                greeting = greeting,
                userName = userName.ifBlank { "Student" },
                streak = streak,
                gems = gems,
                leagueName = leagueName,
                leagueRank = leagueRank,
                weeklyXp = weeklyXp,
                todayDone = todayDone,
                heroEyebrow = heroContent.eyebrow,
                heroTitle = heroContent.title,
                heroSubtitle = heroContent.subtitle,
                heroButtonLabel = heroContent.buttonLabel,
                heroDoneTitle = heroContent.doneTitle,
                heroDoneSubtitle = heroContent.doneSubtitle,
                heroDoneButtonLabel = heroContent.doneButtonLabel,
                heroXpEarned = xpEstimate,
                planDays = planDays.ifEmpty { defaultPlanDays() },
                quests = mapQuestTrail(todayQuest, todayPlanDay, todayTrialItems),
                friends = friends,
                friendCount = friendCount,
                bookmarks = mapBookmarks(progressConcepts, progressSimulations, languageCode),
                revision = mapRevision(progressConcepts, languageCode),
                subjectsSectionTitle = if (isKannadaLanguage(languageCode)) "ವಿಷಯಗಳು" else "Subjects",
                subjects = mapSubjectTiles(availableSubjects, languageCode, selectedSubjectName, selectedSubjectId),
                tutorTitle = HomeCopy.tutorTitle(languageCode),
                tutorMessage = HomeCopy.tutorMessage(languageCode),
                garden = gardenRail,
                collectionShelf = standaloneCollection,
            )

        val focusConceptId = planConceptId ?: focusConcept?.conceptId
        val pendingSim = TrialQuestNavigation.firstPendingSim(todayTrialItems)
        val pendingStudy = TrialQuestNavigation.firstPendingStudy(todayTrialItems)
        val focus =
            GamifiedHomeFocus(
                conceptId = focusConceptId,
                simulationId = focusSimulation?.second?.simulationId?.takeIf { it.isNotBlank() },
                simulationConceptId = focusSimulation?.second?.conceptId ?: focusConceptId,
                pendingSimTrialItemId = pendingSim?.itemId,
                pendingSimTrialRoute = pendingSim?.route,
                pendingStudyTrialItemId = pendingStudy?.itemId,
                pendingStudyTrialRoute = pendingStudy?.route,
                pendingNextTrialItemId = nextPending?.itemId,
                pendingNextTrialRoute = nextPending?.route,
            )

        return state to focus
    }

    /** Garden home rail — Phase 2: Garden theme only, read-only display. */
    fun mapGarden(
        enabled: Boolean,
        progress: GardenProgress?,
        conceptTitle: String,
        kindLabel: String,
        languageCode: String,
        highlightNewPlant: Boolean = false,
        highlightStarterPlant: Boolean = false,
        plantedItems: List<GrownItemEntity> = emptyList(),
    ): GardenRailState? {
        if (!enabled || progress == null) return null

        val theme =
            when (progress.theme.uppercase()) {
                GardenTheme.OUTPOST -> Theme.OUTPOST
                GardenTheme.ISLAND -> Theme.ISLAND
                GardenTheme.COLONY -> Theme.COLONY
                else -> Theme.GARDEN
            }
        // Island / colony home rail deferred until Phase 2 perf gate passes.
        if (!theme.placeBased) return null
        val copy = GardenCopyFactory.themeCopy(languageCode, theme)
        val homeCopy = GardenCopyFactory.homeCopy(languageCode)
        val zoneIndex = progress.currentZone.coerceIn(0, ZONES.lastIndex)
        val zone = ZONES[zoneIndex]
        val slot = GardenSlotResolver.displaySlot(progress)
        val slotLabels =
            (0 until SLOTS_PER_ZONE).map { index ->
                GardenWorldLabels.slotName(zone, theme, index, languageCode)
            }
        val steps = progress.steps.coerceIn(0, progress.stepsPerPlant)
        val ready = steps >= progress.stepsPerPlant
        val artSteps =
            if (highlightNewPlant && steps == 0) {
                progress.stepsPerPlant
            } else {
                steps
            }
        val progressText =
            when {
                highlightNewPlant && steps == 0 -> homeCopy.justPlanted
                ready -> homeCopy.readyToGrow
                else -> homeCopy.stepsOf(steps, progress.stepsPerPlant)
            }
        val zoneName = GardenWorldLabels.zoneName(zone, theme, languageCode).lowercase()
        val statusCore = "${progress.filledInZone}/${progress.zoneCapacity} · $zoneName"
        val milestone = nextGardenMilestone(progress.totalPlanted, copy)
        val remainingScenes = GardenSlotResolver.remainingScenes(progress.currentZone)
        // Indicate that filling this scene unlocks the next one — only while there's a next to unlock
        // and the current place isn't already full.
        val unlockHint =
            if (remainingScenes > 0 && progress.filledInZone < progress.zoneCapacity) {
                "  ·  " + homeCopy.placeUnlockHint(zoneName)
            } else {
                ""
            }
        val statusLine =
            statusCore +
                (milestone?.let { (n, what) ->
                    homeCopy.milestoneMore(n, what)
                } ?: "") +
                unlockHint
        val celebrationLine =
            if (highlightNewPlant) {
                homeCopy.celebrationLine(
                    progress.totalPlanted,
                    copy.done,
                    progress.filledInZone,
                    progress.zoneCapacity,
                    zoneName,
                    remainingScenes,
                )
            } else {
                null
            }

        val growNudgeLine =
            when {
                highlightNewPlant -> null
                highlightStarterPlant -> {
                    val starterName = GardenWorldLabels.slotName(zone, theme, theme.starterSlot(), languageCode).lowercase()
                    homeCopy.starterPlantHomeNudge(starterName)
                }
                else ->
                    mapGardenGrowNudge(
                        progress = progress,
                        zoneName = zoneName,
                        theme = theme,
                        steps = steps,
                        ready = ready,
                        homeCopy = homeCopy,
                    )
            }

        return GardenRailState(
            sectionTitle = homeCopy.yourCollection(copy.placeCollection),
            openLabel =
                if (progress.totalPlanted > 0) {
                    homeCopy.seeAllCollected(progress.totalPlanted)
                } else {
                    homeCopy.openLabel
                },
            conceptTitle = conceptTitle,
            subtitle = "$kindLabel · $progressText",
            steps = steps,
            artSteps = artSteps,
            stepsPerPlant = progress.stepsPerPlant,
            statusLine = statusLine,
            hintLine =
                when {
                    progress.filledInZone > 0 && steps == 0 && !ready ->
                        homeCopy.hintPlantedKeepGoing(progress.filledInZone, copy.done)
                    else -> homeCopy.hintKeepLearning
                },
            currentZone = zoneIndex,
            slot = slot,
            preferredSlot = progress.preferredSlot,
            slotLabels = slotLabels,
            theme = theme,
            ready = ready,
            highlightNewPlant = highlightNewPlant,
            highlightStarterPlant = highlightStarterPlant,
            celebrationLine = celebrationLine,
            growNudgeLine = growNudgeLine,
            slotPickerTitle = copy.pickerTitle,
            surpriseLabel = homeCopy.surpriseLabel,
            surprisePreview = homeCopy.surprisePreview,
            collection = mapCollectionShelfFromEntities(progress, plantedItems, languageCode),
        )
    }

    fun mapCollectionShelfFromEntities(
        progress: GardenProgress,
        plantedItems: List<GrownItemEntity>,
        languageCode: String,
    ): CollectionShelfState =
        mapCollectionShelf(
            progress = progress,
            planted = plantedItems.map { GardenPlantedRow(zone = it.zone, plot = it.plot, slot = it.slot) },
            languageCode = languageCode,
        )

    fun mapCollectionShelf(
        progress: GardenProgress,
        planted: List<GardenPlantedRow>,
        languageCode: String,
    ): CollectionShelfState {
        val theme = themeFromProgress(progress)
        val copy = GardenCopyFactory.themeCopy(languageCode, theme)
        val homeCopy = GardenCopyFactory.homeCopy(languageCode)
        val items =
            planted.map { item ->
                CollectionShelfItem(
                    zone = item.zone,
                    slot = item.slot,
                    label =
                        if (theme.placeBased) {
                            ""
                        } else {
                            (item.plot + 1).toString()
                        },
                )
            }
        return CollectionShelfState(
            theme = theme,
            items = items,
            totalCount = planted.size,
            emptyMessage = copy.sceneEmpty,
            sectionTitle = homeCopy.yourCollection(copy.placeCollection),
            seeAllLabel =
                if (planted.isNotEmpty()) {
                    homeCopy.seeAllCollected(planted.size)
                } else {
                    homeCopy.openLabel
                },
            lockedSlotCount = 3,
            lockedSlotHint = homeCopy.collectionUnlockHint,
        )
    }

    private fun themeFromProgress(progress: GardenProgress): Theme =
        when (progress.theme.uppercase()) {
            GardenTheme.OUTPOST -> Theme.OUTPOST
            GardenTheme.ISLAND -> Theme.ISLAND
            GardenTheme.COLONY -> Theme.COLONY
            else -> Theme.GARDEN
        }

    private fun mapGardenGrowNudge(
        progress: GardenProgress,
        zoneName: String,
        theme: Theme,
        steps: Int,
        ready: Boolean,
        homeCopy: GardenHomeCopy,
    ): String {
        val stepsLeft = (progress.stepsPerPlant - steps).coerceAtLeast(0)
        val placeFull = progress.filledInZone >= progress.zoneCapacity
        val nextZoneIndex = (progress.currentZone + 1).coerceAtMost(ZONES.lastIndex)
        val nextZoneName = ZONES[nextZoneIndex].name(theme).lowercase()

        return when {
            placeFull && progress.currentZone < ZONES.lastIndex ->
                homeCopy.nudgePlaceComplete(zoneName, nextZoneName)
            ready -> homeCopy.nudgeReady
            stepsLeft in 1..2 -> homeCopy.nudgeStepsLeft(stepsLeft)
            else -> homeCopy.nudgeDefault
        }
    }

    private fun nextGardenMilestone(totalPlanted: Int, copy: ThemeCopy): Pair<Int, String>? {
        val next = copy.milestones.firstOrNull { totalPlanted < it.first } ?: return null
        return (next.first - totalPlanted) to next.second
    }

    private fun trialKindLabel(
        todayTrialItems: List<PlanTrialItemEntity>,
        languageCode: String,
    ): String {
        val kannada = isKannadaLanguage(languageCode)
        val pendingStudy = TrialQuestNavigation.firstPendingStudy(todayTrialItems)
        val pendingSim = TrialQuestNavigation.firstPendingSim(todayTrialItems)
        val entity =
            todayTrialItems.firstOrNull {
                pendingStudy?.itemId == it.id || pendingSim?.itemId == it.id
            } ?: todayTrialItems.firstOrNull { it.status != com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemStatus.DONE }
        return when (entity?.kind) {
            com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemKind.REVISION ->
                if (kannada) "ಪುನರಾವಲೋಕನ" else "Revision"
            com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemKind.SIM_AGENT,
            com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemKind.SIM_URL,
            -> if (kannada) "ಸಿಮ್ಯುಲೇಶನ್" else "Simulation"
            com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemKind.STUDY ->
                if (kannada) "ಅಧ್ಯಯನ" else "Study"
            else -> if (kannada) "ಪ್ರಯೋಗ" else "Trial"
        }
    }

    fun mapYoutubeVideos(
        videos: List<YoutubeVideo>,
        languageCode: String,
        youtubeVideoRepository: YoutubeVideoRepository,
    ): List<YoutubeVideoItem> =
        videos.map { video ->
            YoutubeVideoItem(
                videoId = video.videoId,
                title = youtubeVideoRepository.localizedTitle(video, languageCode),
                thumbnailUrl = youtubeVideoRepository.thumbnailUrl(video.videoId),
            )
        }

    private data class HeroContent(
        val eyebrow: String,
        val title: String,
        val subtitle: String,
        val buttonLabel: String,
        val doneTitle: String,
        val doneSubtitle: String,
        val doneButtonLabel: String,
    )

    private fun buildHeroContent(
        todayPlanDay: ExamPlanDayEntity?,
        todayPlanLabel: String,
        nextPendingItem: PlanTrialItemEntity?,
        fallbackConceptName: String,
        selectedSubjectName: String,
        todayDone: Boolean,
        languageCode: String,
    ): HeroContent {
        val kannada = isKannadaLanguage(languageCode)
        val heroTitle =
            TrialItemTitleParser.heroTitle(
                item = nextPendingItem,
                todayPlanLabel = todayPlanLabel,
                fallbackConceptName = fallbackConceptName,
                selectedSubjectName = selectedSubjectName,
            )
        val nextLine =
            nextPendingItem?.title?.let { TrialItemTitleParser.heroNextLine(it, kannada) }

        if (todayPlanDay == null) {
            return HeroContent(
                eyebrow = if (todayDone) {
                    if (kannada) "ಇಂದಿಗೆ ಎಲ್ಲಾ ಮುಗಿದಿದೆ" else "All done for today"
                } else {
                    if (kannada) "ಇಂದಿನ ಗಮನ" else "Today's focus"
                },
                title = heroTitle,
                subtitle =
                    if (todayDone) {
                        if (kannada) {
                            "ನೀವು ಇಂದು ಪಾಠ ಮತ್ತು ಸಿಮ್ಯುಲೇಶನ್ ಪೂರ್ಣಗೊಳಿಸಿದ್ದೀರಿ."
                        } else {
                            "You completed a lesson and a simulation today."
                        }
                    } else if (nextLine != null) {
                        nextLine
                    } else {
                        if (kannada) "ಪಾಠ + ಸಿಮ್ಯುಲೇಶನ್ ಅಭ್ಯಾಸ" else "Concept lesson + simulation practice"
                    },
                buttonLabel =
                    if (todayDone) {
                        if (kannada) "ಇನ್ನಷ್ಟು ಅನ್ವೇಷಿಸಿ" else "Explore more"
                    } else {
                        if (kannada) "ಪ್ರಾರಂಭಿಸಿ" else "Start now"
                    },
                doneTitle =
                    if (kannada) {
                        "ಚೆನ್ನಾಗಿ ಮಾಡಿದ್ದೀರಿ${if (heroTitle.isNotBlank()) " — $heroTitle" else ""}"
                    } else {
                        "Nice work${if (heroTitle.isNotBlank()) " on $heroTitle" else "!"}"
                    },
                doneSubtitle =
                    if (kannada) {
                        "ನಾಳೆ ಹಿಂತಿರುಗಿ, ಅಥವಾ ಮತ್ತೊಂದು ವಿಷಯದಲ್ಲಿ ಮುಂದುವರಿಯಿರಿ."
                    } else {
                        "Come back tomorrow, or keep going with another topic."
                    },
                doneButtonLabel = if (kannada) "ಅಧ್ಯಾಯಗಳನ್ನು ಅನ್ವೇಷಿಸಿ" else "Explore chapters",
            )
        }

        val minutes = todayPlanDay.estimatedMinutes.coerceAtLeast(1)
        val (eyebrow, subtitle, button) =
            when (todayPlanDay.dayType) {
                "REVISE" ->
                    Triple(
                        if (kannada) "ಪುನರಾವಲೋಕನ ದಿನ" else "Revision day",
                        nextLine ?: if (kannada) {
                            "ಇಂದಿನ ವಿಷಯಗಳಿಗಾಗಿ ಪುನರಾವಲೋಕನ ತೆರೆಯಿರಿ."
                        } else {
                            "Open a revision session for today's topics."
                        },
                        if (kannada) "ಪುನರಾವಲೋಕನ ಪ್ರಾರಂಭಿಸಿ" else "Start revision",
                    )
                "MOCK" ->
                    Triple(
                        if (kannada) "ಮಾಕ್ ದಿನ · $minutes min" else "Mock day · $minutes min",
                        if (kannada) "ಯೋಜನೆಯ ಅಧ್ಯಾಯಗಳಲ್ಲಿ ಬಾಕಿ ಏಜೆಂಟ್‌ಗಳನ್ನು ಪೂರ್ಣಗೊಳಿಸಿ." else "Complete pending agents across your plan chapters.",
                        if (kannada) "ಮಾಕ್ ಪ್ರಾರಂಭಿಸಿ" else "Start mock",
                    )
                "EXAM" ->
                    Triple(
                        if (kannada) "ಪರೀಕ್ಷಾ ದಿನ" else "Exam day",
                        if (kannada) "ನೀವು ಸಿದ್ಧರಿದ್ದೀರಿ — ಶಾಂತರಾಗಿ ಉತ್ತಮ ಪ್ರದರ್ಶನ ನೀಡಿ." else "You're prepared — stay calm and do your best.",
                        if (kannada) "ಯೋಜನೆ ನೋಡಿ" else "View plan",
                    )
                else ->
                    Triple(
                        if (kannada) "ಇಂದಿನ ಗಮನ · $minutes min" else "Today's focus · $minutes min",
                        nextLine ?: if (kannada) "ಇಂದಿನ ಪ್ರಯೋಗದ ಸರದಿ" else "Today's trial queue",
                        when {
                            todayDone -> if (kannada) "ಇನ್ನಷ್ಟು ಅನ್ವೇಷಿಸಿ" else "Explore more"
                            nextLine != null -> if (kannada) "ಮುಂದಿನ ಪ್ರಯೋಗ" else "Continue trial"
                            kannada -> "ಪ್ರಾರಂಭಿಸಿ"
                            else -> "Start now"
                        },
                    )
            }

        return HeroContent(
            eyebrow = if (todayDone) {
                if (kannada) "ಇಂದಿಗೆ ಎಲ್ಲಾ ಮುಗಿದಿದೆ" else "All done for today"
            } else {
                eyebrow
            },
            title = heroTitle,
            subtitle = subtitle,
            buttonLabel = button,
            doneTitle = if (kannada) "ಇಂದಿನ ಯೋಜನೆ ಪೂರ್ಣ!" else "Nice work on today's plan!",
            doneSubtitle =
                if (kannada) "ನಾಳೆ ಮುಂದಿನ ದಿನಕ್ಕಾಗಿ ಹಿಂತಿರುಗಿ, ಅಥವಾ ಈಗಲೇ ಮುಂದೆ ಹೋಗಿ." else "Come back tomorrow for the next day, or get ahead now.",
            doneButtonLabel = if (kannada) "ಪೂರ್ಣ ಯೋಜನೆ ನೋಡಿ" else "View full plan",
        )
    }

    private fun defaultPlanDays(): List<PlanDayNode> = emptyList()

    private fun mapQuestTrail(
        quest: QuestDailyEntity?,
        todayPlanDay: ExamPlanDayEntity?,
        todayTrialItems: List<PlanTrialItemEntity>,
    ): QuestTrailState {
        val trialProgress = TrialQuestProgress.fromTrialItems(todayTrialItems, todayPlanDay)
        val studyLabelPrefix = trialProgress?.studyLabelPrefix ?: studyLabelPrefix(todayPlanDay)
        if (quest == null) {
            return QuestTrailState(studyLabelPrefix = studyLabelPrefix)
        }
        return QuestTrailState(
            simsDone = quest.simsDone,
            simsTotal = quest.simsTotal,
            simsClaimed = quest.simsClaimed,
            simsLabelPrefix = "Sims · ",
            studyDone = quest.studyDone,
            studyTotal = quest.studyTotal,
            studyClaimed = quest.studyClaimed,
            studyLabelPrefix = studyLabelPrefix,
            bonusClaimed = quest.bonusClaimed,
        )
    }

    private fun studyLabelPrefix(todayPlanDay: ExamPlanDayEntity?): String =
        when (todayPlanDay?.dayType) {
            "REVISE" -> "Revision · "
            "MOCK" -> "Mock task · "
            "EXAM" -> "Rest day"
            else -> "Trial · "
        }

    private fun pickFocusConcept(
        progressConcepts: List<Pair<ProgressEntity?, ConceptEntity?>>,
        preferredConceptId: String?,
    ): ConceptEntity? {
        preferredConceptId?.let { id ->
            progressConcepts.firstOrNull { it.second?.conceptId == id }?.second?.let { return it }
        }
        val inProgress =
            progressConcepts.firstOrNull { (progress, concept) ->
                progress?.status == "IN_PROGRESS" && concept != null
            }?.second
        if (inProgress != null) return inProgress
        return progressConcepts.firstOrNull { it.second != null }?.second
    }

    private fun pickFocusSimulation(
        progressSimulations: List<Pair<ProgressEntity?, ConceptEntity?>>,
        preferredConceptId: String?,
    ): Pair<ProgressEntity?, ConceptEntity?>? {
        preferredConceptId?.let { id ->
            progressSimulations.firstOrNull { it.second?.conceptId == id }?.let { return it }
        }
        val inProgress =
            progressSimulations.firstOrNull { (progress, concept) ->
                progress?.status == "IN_PROGRESS" && concept != null
            }
        if (inProgress != null) return inProgress
        return progressSimulations.firstOrNull { it.second != null }
    }

    private fun mapBookmarks(
        progressConcepts: List<Pair<ProgressEntity?, ConceptEntity?>>,
        progressSimulations: List<Pair<ProgressEntity?, ConceptEntity?>>,
        languageCode: String,
    ): List<BookmarkItem> {
        val conceptBookmarks =
            progressConcepts
                .filter { (progress, concept) -> progress?.status == "COMPLETED" && concept != null }
                .mapNotNull { (_, concept) ->
                    concept?.let {
                        BookmarkItem(
                            key = it.getLocalizedName(languageCode),
                            typeLabel = "Concept",
                            role = EduChipRole.Accent,
                            conceptId = it.conceptId,
                        )
                    }
                }
        val simulationBookmarks =
            progressSimulations
                .filter { (progress, concept) -> progress?.status == "COMPLETED" && concept != null }
                .mapNotNull { (_, concept) ->
                    concept?.let {
                        BookmarkItem(
                            key = it.getLocalizedName(languageCode),
                            typeLabel = "Simulation",
                            role = EduChipRole.Pro,
                            conceptId = it.conceptId,
                            simulationId = it.simulationId,
                        )
                    }
                }
        return (conceptBookmarks + simulationBookmarks).take(3).ifEmpty {
            listOf(
                BookmarkItem("Your saved topics", "Bookmark", EduChipRole.Warning, isPlaceholder = true),
            )
        }
    }

    private fun mapSubjectTiles(
        availableSubjects: List<SubjectEntity>,
        languageCode: String,
        selectedSubjectName: String,
        selectedSubjectId: String,
    ): List<SubjectTile> {
        if (availableSubjects.isNotEmpty()) {
            return availableSubjects.map { subject ->
                val name = subject.getLocalizedName(languageCode)
                SubjectTile(
                    name = name,
                    role = chipRoleForSubject(subject, languageCode),
                    subjectId = subject.subjectId,
                    iconUrl = resolveSubjectIconUrl(subject.subjectId, name, subject.iconUrl),
                )
            }
        }
        return defaultSubjectTiles(languageCode)
    }

    private fun defaultSubjectTiles(languageCode: String): List<SubjectTile> {
        val kannada = isKannadaLanguage(languageCode)
        return listOf(
            SubjectTile(
                name = if (kannada) "ಗಣಿತ" else "Math",
                role = EduChipRole.Pro,
                subjectId = SubjectIds.MATH,
                iconUrl = SubjectIconUrls.MATH,
            ),
            SubjectTile(
                name = if (kannada) "ವಿಜ್ಞಾನ" else "Science",
                role = EduChipRole.Accent,
                subjectId = SubjectIds.SCIENCE,
                iconUrl = SubjectIconUrls.SCIENCE,
            ),
        )
    }

    private fun chipRoleForSubject(subject: SubjectEntity, languageCode: String): EduChipRole {
        val label = subject.getLocalizedName(languageCode)
        return when {
            label.contains("math", ignoreCase = true) ||
                label.contains("ಗಣಿತ", ignoreCase = false) -> EduChipRole.Pro
            label.contains("science", ignoreCase = true) ||
                label.contains("ವಿಜ್ಞಾನ", ignoreCase = false) -> EduChipRole.Accent
            else -> EduChipRole.Pro
        }
    }

    private fun mapRevision(
        progressConcepts: List<Pair<ProgressEntity?, ConceptEntity?>>,
        languageCode: String,
    ): List<RevisionItem> {
        return progressConcepts
            .filter { (progress, concept) -> progress?.status == "COMPLETED" && concept != null }
            .mapNotNull { (_, concept) ->
                concept?.let {
                    RevisionItem(
                        topic = it.getLocalizedName(languageCode),
                        score = 0,
                        conceptId = it.conceptId,
                        chapterId = it.chapterId,
                    )
                }
            }
            .take(2)
    }
}
