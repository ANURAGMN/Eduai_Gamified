package com.anurag.eduai.uikit.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.components.BookmarkItem
import com.anurag.eduai.uikit.components.BookmarksRail
import com.anurag.eduai.uikit.components.Entrance
import com.anurag.eduai.uikit.components.FriendUpdate
import com.anurag.eduai.uikit.components.FriendsInviteRail
import com.anurag.eduai.uikit.components.FriendsUpdatesRail
import com.anurag.eduai.uikit.components.EduIntroTourOverlay
import com.anurag.eduai.uikit.components.HomeFocusStrip
import com.anurag.eduai.uikit.components.PlanDayNode
import com.anurag.eduai.uikit.components.PlanDayStatus
import com.anurag.eduai.uikit.components.PlanDayType
import com.anurag.eduai.uikit.components.PlanTrail
import com.anurag.eduai.uikit.components.GardenRailState
import com.anurag.eduai.uikit.components.GrowRail
import com.anurag.eduai.uikit.components.QuestTrail
import com.anurag.eduai.uikit.components.QuestTrailState
import com.anurag.eduai.uikit.components.RevisionItem
import com.anurag.eduai.uikit.components.RevisionRail
import com.anurag.eduai.uikit.components.SectionHeader
import androidx.compose.material3.Icon
import com.anurag.eduai.uikit.components.SubjectTile
import com.anurag.eduai.uikit.components.subjectMaterialIcon
import com.anurag.eduai.uikit.components.SubjectsRail
import com.anurag.eduai.uikit.components.TopBarChips
import com.anurag.eduai.uikit.garden.CollectionShelf
import com.anurag.eduai.uikit.garden.CollectionShelfState
import com.anurag.eduai.uikit.theme.EduChipRole
import com.anurag.eduai.uikit.theme.EduAiTheme

data class HomeUiState(
    val greeting: String = "Good morning",
    val userName: String = "Aanya",
    val streak: Int = 6,
    val gems: Int = 240,
    val leagueName: String = "Silver",
    val leagueRank: Int = 4,
    val streakCaption: String = "Streak",
    val gemsCaption: String = "Gems",
    val weeklyXp: Int = 0,
    val todayDone: Boolean = false,
    val heroEyebrow: String = "Today's focus · 18 min",
    val heroTitle: String = "Multiplication & division of integers",
    val heroSubtitle: String = "Concept + simulation + quick quiz",
    val heroButtonLabel: String = "Start now",
    val heroDoneTitle: String = "Nice work on Integers!",
    val heroDoneSubtitle: String = "Come back tomorrow for Day 5, or get ahead now.",
    val heroDoneButtonLabel: String = "Start Day 5 early",
    val heroXpEarned: Int = 35,
    val quests: QuestTrailState = QuestTrailState(),
    /** When non-null, replaces the quest trail (garden feature flag). */
    val garden: GardenRailState? = null,
    /** Island/colony fallback shelf when the grow rail is unavailable. */
    val collectionShelf: CollectionShelfState? = null,
    val friends: List<FriendUpdate> = emptyList(),
    val friendCount: Int = 0,
    val bookmarks: List<BookmarkItem> =
        listOf(
            BookmarkItem("Division of fractions", "Revision", EduChipRole.Warning),
            BookmarkItem("Properties of integers", "Concept", EduChipRole.Accent),
            BookmarkItem("Multiplication of fractions", "Simulation", EduChipRole.Pro),
        ),
    val planDays: List<PlanDayNode> =
        listOf(
            PlanDayNode(1, PlanDayStatus.Done),
            PlanDayNode(2, PlanDayStatus.Done),
            PlanDayNode(3, PlanDayStatus.Done),
            PlanDayNode(4, PlanDayStatus.Today, PlanDayType.Lesson, "Multiplication & division of integers"),
            PlanDayNode(5, PlanDayStatus.Upcoming, PlanDayType.Lesson, "Multiplication of fractions"),
            PlanDayNode(6, PlanDayStatus.Upcoming, PlanDayType.Lesson, "Operations on decimals"),
            PlanDayNode(7, PlanDayStatus.Upcoming, PlanDayType.Revise),
            PlanDayNode(8, PlanDayStatus.Upcoming, PlanDayType.Revise),
            PlanDayNode(9, PlanDayStatus.Upcoming, PlanDayType.Mock),
        ),
    val revision: List<RevisionItem> =
        listOf(
            RevisionItem("Division of fractions", 45),
            RevisionItem("Integer word problems", 58),
        ),
    val subjectsSectionTitle: String = "Subjects",
    val subjects: List<SubjectTile> =
        listOf(
            SubjectTile("Math", EduChipRole.Pro),
            SubjectTile("Science", EduChipRole.Accent),
        ),
    val tutorTitle: String = "Your tutor",
    val tutorMessage: String = "Ready for today's quests? Let's go!",
    // Section chrome — filled by the app from HomeCopy so EN↔KN toggles update rails.
    val planSectionTitle: String = "Your exam prep plan",
    val planAddLabel: String = "Add plan",
    val planEmptyTitle: String = "No exam plan yet",
    val planEmptyBody: String = "Set exam type, chapters, and daily study time to build your prep trail.",
    val planTodayLabel: String = "Today",
    val planDayPrefix: String = "Day",
    val planGrowHint: String = "Finish a day to grow your world — every task plants something new.",
    val questsSectionTitle: String = "Today's quests",
    val revisionSectionTitle: String = "Needs revision",
    /** Use `%d` for the quiz percent, e.g. `%d%% last quiz` / `ಕೊನೆಯ ರಸಪ್ರಶ್ನೆ %d%%`. */
    val revisionLastQuizTemplate: String = "%d%% last quiz",
    val bookmarksSectionTitle: String = "Bookmarks",
    val seeAllLabel: String = "See all",
    val friendsSectionTitle: String = "Friends' updates",
    val friendsAddLabel: String = "Add",
    val friendsAddCardTitle: String = "Add a friend",
    val friendsAddCardBody: String = "Enter their friend code to link accounts.",
    val inviteFriendsTitle: String = "Invite friends",
    val inviteFriendsSubtitle: String = "Share your code",
    val shareLabel: String = "Share",
    val acceptLabel: String = "Accept",
)

/** Lightweight card model for the home YouTube rail (below subjects). */
data class YoutubeVideoItem(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String = "",
)

/**
 * Pixel-faithful Compose port of prototype `screenHome()` rail order:
 * TopBar → Hero → Quests trail → Friends → Bookmarks → Plan trail → Revision → Subjects
 */
@Composable
fun EduHomeScreen(
    state: HomeUiState = HomeUiState(),
    onProfileClick: () -> Unit = {},
    onStreakClick: () -> Unit = {},
    onGemsClick: () -> Unit = {},
    onLeagueClick: () -> Unit = {},
    onFriendsSeeAll: () -> Unit = {},
    onCheerFriend: (Int) -> Unit = {},
    onAddFriend: () -> Unit = {},
    onInviteShare: () -> Unit = {},
    showFriendDot: Boolean = false,
    onStartToday: () -> Unit = {},
    onQuestsSeeAll: () -> Unit = {},
    onOpenGarden: () -> Unit = {},
    onGardenRailClick: () -> Unit = onOpenGarden,
    onSimsQuestClick: () -> Unit = {},
    onStudyQuestClick: () -> Unit = {},
    onQuizQuestClick: () -> Unit = onStudyQuestClick,
    onBonusQuestClick: () -> Unit = {},
    onPlanSeeAll: () -> Unit = {},
    onPlanAdd: () -> Unit = onPlanSeeAll,
    onPlanDayClick: (PlanDayNode) -> Unit = {},
    onTutorClick: () -> Unit = {},
    onBookmarksSeeAll: () -> Unit = {},
    onBookmarkOpen: (BookmarkItem) -> Unit = {},
    onRevisionOpen: (RevisionItem) -> Unit = {},
    onSubjectOpen: (SubjectTile) -> Unit = {},
    subjectIconContent: @Composable (SubjectTile, Color) -> Unit = { subject, tint ->
        Icon(
            imageVector = subjectMaterialIcon(subject.name),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(26.dp),
        )
    },
    belowSubjectsContent: @Composable () -> Unit = {},
    /** When true, runs the four-step first-run spotlight tour over the real rails. */
    showIntroTour: Boolean = false,
    /** [skipped] true when the student tapped Skip; [step] is the coach step index (0..2). */
    onIntroTourFinished: (skipped: Boolean, step: Int) -> Unit = { _, _ -> },
    /** Called with the current tour step index each time it changes (for analytics). */
    onTourStep: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    val scrollState = rememberScrollState()
    // Bounds of the four rails the tour spotlights (0 chips, 1 hero, 2 grow/quests, 3 plan) plus the
    // scroll viewport, so each target can be scrolled into view before it is highlighted.
    var railBounds by remember { mutableStateOf<Map<Int, Rect>>(emptyMap()) }
    var viewportBounds by remember { mutableStateOf<Rect?>(null) }
    var coachStep by remember { mutableIntStateOf(0) }

    // Report each tour step as it becomes visible (analytics).
    LaunchedEffect(coachStep, showIntroTour) {
        if (showIntroTour && coachStep < homeTourSteps.size) onTourStep(coachStep)
    }

    // Bring each target under a comfortable line below the status bar before it is spotlighted.
    LaunchedEffect(coachStep, showIntroTour) {
        if (!showIntroTour) return@LaunchedEffect
        var target = railBounds[coachStep]
        var guard = 0
        while (target == null && guard < 25) {
            kotlinx.coroutines.delay(16); target = railBounds[coachStep]; guard++
        }
        val t = target ?: return@LaunchedEffect
        val vp = viewportBounds ?: return@LaunchedEffect
        val desiredTop = vp.top + vp.height * 0.20f
        val delta = t.top - desiredTop
        if (kotlin.math.abs(delta) > 6f) scrollState.animateScrollBy(delta)
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.surface1)
                .onGloballyPositioned { viewportBounds = it.boundsInRoot() },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .padding(bottom = 48.dp),
        ) {
        Box(Modifier.fillMaxWidth().onGloballyPositioned { railBounds = railBounds + (0 to it.boundsInRoot()) }) {
        Entrance(delayMillis = 0) {
            TopBarChips(
                greeting = state.greeting,
                userName = state.userName,
                streak = state.streak,
                gems = state.gems,
                leagueName = state.leagueName,
                leagueRank = state.leagueRank,
                streakCaption = state.streakCaption,
                gemsCaption = state.gemsCaption,
                showFriendDot = showFriendDot,
                showGemsDot = false,
                showLeagueDot = false,
                onProfileClick = onProfileClick,
                onStreakClick = onStreakClick,
                onGemsClick = onGemsClick,
                onLeagueClick = onLeagueClick,
            )
        }
        } // end rail-0 bounds

        // Tutor + today's focus in one compact strip (avatar · task · Start).
        Box(Modifier.fillMaxWidth().onGloballyPositioned { railBounds = railBounds + (1 to it.boundsInRoot()) }) {
            Entrance(delayMillis = 80) {
                if (state.todayDone) {
                    HomeFocusStrip(
                        eyebrow = "",
                        title = state.heroDoneTitle,
                        subtitle = state.heroDoneSubtitle,
                        buttonLabel = state.heroDoneButtonLabel,
                        onStartClick = onStartToday,
                        onTutorClick = onTutorClick,
                        todayDone = true,
                        xpEarned = state.heroXpEarned,
                    )
                } else {
                    HomeFocusStrip(
                        eyebrow = state.heroEyebrow,
                        title = state.heroTitle,
                        subtitle = state.heroSubtitle.ifBlank { state.tutorMessage },
                        buttonLabel = state.heroButtonLabel,
                        onStartClick = onStartToday,
                        onTutorClick = onTutorClick,
                    )
                }
            }
        } // end rail-1 bounds (focus + tutor merged)

        // Subjects sits above the garden so browsing by subject is the first thing after
        // the focus strip.
        Entrance(delayMillis = 150) {
            SubjectsRail(
                title = state.subjectsSectionTitle,
                subjects = state.subjects,
                onOpen = onSubjectOpen,
                iconContent = subjectIconContent,
            )
        }

        // Space / garden colony — above video lessons.
        Box(Modifier.fillMaxWidth().onGloballyPositioned { railBounds = railBounds + (2 to it.boundsInRoot()) }) {
        Entrance(delayMillis = 158) {
            when {
                state.garden != null ->
                    GrowRail(
                        state = state.garden,
                        onOpenWorld = onOpenGarden,
                        onRailClick = onGardenRailClick,
                    )
                state.collectionShelf != null -> {
                    val shelf = state.collectionShelf
                    SectionHeader(
                        title = shelf.sectionTitle,
                        seeAllLabel = shelf.seeAllLabel,
                        onSeeAllClick = onOpenGarden,
                    )
                    CollectionShelf(
                        state = shelf,
                        onOpenCollection = onOpenGarden,
                    )
                }
                state.quests.simsTotal > 0 || state.quests.studyTotal > 0 ->
                    QuestTrail(
                        state = state.quests,
                        onSeeAll = onQuestsSeeAll,
                        onSimsClick = onSimsQuestClick,
                        onStudyClick = onStudyQuestClick,
                        onBonusClick = onBonusQuestClick,
                        sectionTitle = state.questsSectionTitle,
                        seeAllLabel = state.seeAllLabel,
                    )
            }
        }
        } // end rail-2 bounds

        // Video lessons (+ textbooks from the app slot) — below the colony.
        Entrance(delayMillis = 165) {
            belowSubjectsContent()
        }

        Entrance(delayMillis = 220) {
            if (state.friendCount == 0 && state.friends.isEmpty()) {
                FriendsInviteRail(
                    onAddFriend = onAddFriend,
                    onInviteShare = onInviteShare,
                    sectionTitle = state.friendsSectionTitle,
                    addLabel = state.friendsAddLabel,
                    addCardTitle = state.friendsAddCardTitle,
                    addCardBody = state.friendsAddCardBody,
                    inviteTitle = state.inviteFriendsTitle,
                    inviteSubtitle = state.inviteFriendsSubtitle,
                    shareLabel = state.shareLabel,
                )
            } else {
                FriendsUpdatesRail(
                    friends = state.friends,
                    onSeeAll = onFriendsSeeAll,
                    onCheer = onCheerFriend,
                    sectionTitle = state.friendsSectionTitle,
                    seeAllLabel = state.seeAllLabel,
                    acceptLabel = state.acceptLabel,
                )
            }
        }

        Box(Modifier.fillMaxWidth().onGloballyPositioned { railBounds = railBounds + (3 to it.boundsInRoot()) }) {
        Entrance(delayMillis = 320) {
            PlanTrail(
                days = state.planDays,
                onSeeAll = onPlanSeeAll,
                onAddPlan = onPlanAdd,
                onDayClick = onPlanDayClick,
                sectionTitle = state.planSectionTitle,
                seeAllLabel = state.seeAllLabel,
                addPlanLabel = state.planAddLabel,
                emptyTitle = state.planEmptyTitle,
                emptyBody = state.planEmptyBody,
                todayLabel = state.planTodayLabel,
                dayLabelFor = { day -> "${state.planDayPrefix} $day" },
                growHint = state.planGrowHint,
            )
        }
        } // end rail-3 bounds

        Spacer(modifier = Modifier.height(12.dp))

        Entrance(delayMillis = 480) {
            RevisionRail(
                items = state.revision,
                onOpen = onRevisionOpen,
                sectionTitle = state.revisionSectionTitle,
                scoreLabelFor = { score ->
                    state.revisionLastQuizTemplate.replace("%d", score.toString())
                },
            )
        }

        // Bookmarks.
        Entrance(delayMillis = 640) {
            BookmarksRail(
                bookmarks = state.bookmarks,
                onSeeAll = onBookmarksSeeAll,
                onOpen = onBookmarkOpen,
                sectionTitle = state.bookmarksSectionTitle,
                seeAllLabel = "${state.seeAllLabel} (${state.bookmarks.size})",
            )
        }
        } // end scrolling Column

        if (showIntroTour && coachStep < homeTourSteps.size) {
            EduIntroTourOverlay(
                step = coachStep,
                total = homeTourSteps.size,
                target = railBounds[coachStep],
                viewport = viewportBounds,
                title = homeTourSteps[coachStep].title,
                body = homeTourSteps[coachStep].body,
                onBack = { if (coachStep > 0) coachStep-- },
                onNext = {
                    if (coachStep >= homeTourSteps.lastIndex) {
                        onIntroTourFinished(false, coachStep)
                    } else {
                        coachStep++
                    }
                },
                onSkip = { onIntroTourFinished(true, coachStep) },
            )
        }
    }
}

private data class HomeTourStep(val title: String, val body: String)

private val homeTourSteps =
    listOf(
        HomeTourStep(
            "Your progress lives here",
            "Streak, XP and league rank. They tick up every day you show up — this is your fuel.",
        ),
        HomeTourStep(
            "Start your day here",
            "Today's focus is one short session — about 18 minutes. Tap Start now and you're going.",
        ),
        HomeTourStep(
            "Learning grows your world",
            "Every task you finish plants something new here. Come back daily and watch it fill in.",
        ),
    )

