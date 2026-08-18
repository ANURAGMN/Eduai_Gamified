package com.anurag.eduai.uikit.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.aspectRatio
import com.anurag.eduai.uikit.components.Entrance
import com.anurag.eduai.uikit.components.EduPrimaryButton
import com.anurag.eduai.uikit.components.pressScaleClickable
import com.anurag.eduai.uikit.garden.quest.GardenPlantedRow
import com.anurag.eduai.uikit.garden.quest.GardenSceneSnapshot
import com.anurag.eduai.uikit.garden.quest.SLOTS_PER_ZONE
import com.anurag.eduai.uikit.garden.quest.Theme
import com.anurag.eduai.uikit.garden.quest.ThemeScene
import com.anurag.eduai.uikit.garden.quest.sceneAspect
import com.anurag.eduai.uikit.garden.quest.starterZone
import com.anurag.eduai.uikit.garden.world.rememberSceneTime
import com.anurag.eduai.uikit.avatar.EduTutorAvatar
import com.anurag.eduai.uikit.avatar.OnboardingTutorPresets
import com.anurag.eduai.uikit.avatar.avatarFaceZoom
import com.anurag.eduai.uikit.avatar.core.TutorCharacter
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.anurag.eduai.uikit.theme.EduChipRole
import com.anurag.eduai.uikit.theme.forRole

/** What the student chose during first-run. Handed to the app when onboarding finishes. */
data class OnboardingResult(
    val subject: String,
    val chapter: String,
    /** "Garden" or "Space" — the reward world. */
    val world: String,
    /** Chosen tutor avatar preset id (see AllAvatarPresets); empty if skipped. */
    val avatarPresetId: String = "",
)

private data class OnboardingSlide(
    val icon: ImageVector,
    val role: EduChipRole,
    val title: String,
    val body: String,
)

/** Class-7 chapters per subject — loaded from the app; falls back to NCERT lists if DB is empty. */
private val defaultOnboardingChapters =
    mapOf(
        "Math" to emptyList<String>(),
        "Science" to emptyList<String>(),
    )

private const val STAGE_SUBJECT = 0
private const val STAGE_CHAPTER = 1
private const val STAGE_WORLD = 2
private const val STAGE_TUTOR = 3

/** Tutor grid tiles — 30% shorter than a square. */
private const val TUTOR_CHOICE_SIZE_SCALE = 0.70f

/** Reward-world scene previews — a further 20% shorter than tutor scale. */
private const val WORLD_CHOICE_SIZE_SCALE = TUTOR_CHOICE_SIZE_SCALE * 0.80f

/** Face avatars in onboarding tutor cards — nudge down so hair sits inside the clip. */
private const val ONBOARDING_TUTOR_FACE_OFFSET_Y = 0.17f

/**
 * First-run onboarding — intro slides, then Subject → Chapter → World → Meet your tutor,
 * finishing with "Build my plan". Calls [onFinish] with the chosen picks.
 */
@Composable
fun EduOnboardingScreen(
    languageCode: String = "en",
    chaptersBySubject: Map<String, List<String>> = defaultOnboardingChapters,
    onSlideView: (Int) -> Unit = {},
    onSlideSkip: (Int) -> Unit = {},
    onSubjectSelected: (String) -> Unit = {},
    onChapterSelected: (String) -> Unit = {},
    onWorldSelected: (String) -> Unit = {},
    onAvatarSelected: (String) -> Unit = {},
    onFinish: (OnboardingResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    val copy = remember(languageCode) { onboardingStrings(languageCode) }
    val onboardingSlides =
        remember(copy) {
            listOf(
                OnboardingSlide(Icons.Filled.AutoAwesome, EduChipRole.Accent, copy.slides[0].title, copy.slides[0].body),
                OnboardingSlide(Icons.Filled.LocalFireDepartment, EduChipRole.Warning, copy.slides[1].title, copy.slides[1].body),
                OnboardingSlide(Icons.Filled.EmojiEvents, EduChipRole.Pro, copy.slides[2].title, copy.slides[2].body),
            )
        }
    var slide by rememberSaveable { mutableIntStateOf(0) }
    var pickStage by rememberSaveable { mutableIntStateOf(-1) } // -1 while still on intro slides
    var subject by rememberSaveable { mutableStateOf("") }
    var chapter by rememberSaveable { mutableStateOf("") }
    var world by rememberSaveable { mutableStateOf("") }
    var tutor by rememberSaveable { mutableStateOf("") }
    val colors = EduAiTheme.colors

    LaunchedEffect(slide, pickStage) {
        if (pickStage < STAGE_SUBJECT) onSlideView(slide)
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.surface1)
                .padding(horizontal = 22.dp)
                .padding(top = 20.dp, bottom = 26.dp),
    ) {
        if (pickStage < STAGE_SUBJECT) {
            IntroSlide(
                slide = onboardingSlides[slide],
                index = slide,
                total = onboardingSlides.size,
                copy = copy,
                onSkip = {
                    onSlideSkip(slide)
                    pickStage = STAGE_SUBJECT
                },
                onNext = { if (slide < onboardingSlides.lastIndex) slide++ else pickStage = STAGE_SUBJECT },
            )
        } else {
            when (pickStage) {
                STAGE_SUBJECT ->
                    SubjectStep(
                        copy = copy,
                        onPick = {
                            subject = it
                            chapter = ""
                            onSubjectSelected(it)
                            pickStage = STAGE_CHAPTER
                        },
                    )
                STAGE_CHAPTER ->
                    ChapterStep(
                        copy = copy,
                        subject = subject,
                        subjectLabel = copy.subjects.firstOrNull { it.key == subject }?.label ?: subject,
                        chapters = chaptersBySubject[subject].orEmpty(),
                        selected = chapter,
                        onSelect = { chapter = it },
                        onBack = { pickStage = STAGE_SUBJECT },
                        onNext = {
                            if (chapter.isNotEmpty()) {
                                onChapterSelected(chapter)
                                pickStage = STAGE_WORLD
                            }
                        },
                    )
                STAGE_WORLD ->
                    WorldStep(
                        copy = copy,
                        selected = world,
                        onSelect = { world = it },
                        onBack = { pickStage = STAGE_CHAPTER },
                        onBuild = {
                            if (world.isNotEmpty()) {
                                onWorldSelected(world)
                                pickStage = STAGE_TUTOR
                            }
                        },
                    )
                else ->
                    TutorStep(
                        copy = copy,
                        selected = tutor,
                        onSelect = { tutor = it },
                        onBack = { pickStage = STAGE_WORLD },
                        onBuild = {
                            if (tutor.isNotEmpty()) {
                                onAvatarSelected(tutor)
                                onFinish(OnboardingResult(subject, chapter, world, tutor))
                            }
                        },
                    )
            }
        }
    }
}

@Composable
private fun ColumnScope.IntroSlide(
    slide: OnboardingSlide,
    index: Int,
    total: Int,
    copy: OnboardingStrings,
    onSkip: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = EduAiTheme.colors
    val (fg, bg) = colors.forRole(slide.role)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Text(
            text = copy.skip,
            color = colors.textMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier =
                Modifier
                    .pressScaleClickable(onClick = onSkip, pressedScale = 0.9f)
                    .padding(6.dp),
        )
    }

    // Art + copy — re-keyed on index so it pops in fresh on every slide change.
    key(index) {
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Entrance(delayMillis = 0) {
                Box(
                    modifier =
                        Modifier
                            .size(132.dp)
                            .clip(RoundedCornerShape(40.dp))
                            .background(bg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(slide.icon, contentDescription = null, tint = fg, modifier = Modifier.size(58.dp))
                }
            }
            Spacer(modifier = Modifier.height(26.dp))
            Entrance(delayMillis = 80) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = slide.title,
                        color = colors.text,
                        fontSize = 27.sp,
                        lineHeight = 31.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = slide.body,
                        color = colors.textSecondary,
                        fontSize = 14.5.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(300.dp),
                    )
                }
            }
        }
    }

    // Progress dots — the active one stretches into a pill.
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(total) { i ->
            val active = i == index
            val dotWidth by animateDpAsState(
                targetValue = if (active) 22.dp else 8.dp,
                animationSpec = tween(250),
                label = "dot$i",
            )
            Box(
                modifier =
                    Modifier
                        .padding(horizontal = 3.5.dp)
                        .height(8.dp)
                        .width(dotWidth)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (active) colors.accent else colors.borderStrong),
            )
        }
    }

    EduPrimaryButton(
        text = if (index < total - 1) copy.next else copy.getStarted,
        onClick = onNext,
        fillMaxWidth = true,
    )
}

/* ---------------- step 1 · subject ---------------- */

@Composable
private fun ColumnScope.SubjectStep(
    copy: OnboardingStrings,
    onPick: (String) -> Unit,
) {
    val colors = EduAiTheme.colors
    Spacer(modifier = Modifier.height(8.dp))
    StepLabel(copy.step1)
    Text(copy.pickSubjectTitle, color = colors.text, fontSize = 25.sp, fontWeight = FontWeight.Black)
    Spacer(modifier = Modifier.height(6.dp))
    Text(copy.pickSubjectSub, color = colors.textSecondary, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(22.dp))
    Column(
        modifier = Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        copy.subjects.forEachIndexed { index, subject ->
            Entrance(delayMillis = index * 60) {
                SubjectCard(subject.label, subject.subtitle, if (index == 0) Icons.Filled.Calculate else Icons.Filled.Science, if (index == 0) EduChipRole.Accent else EduChipRole.Pro) {
                    onPick(subject.key)
                }
            }
        }
    }
}

@Composable
private fun SubjectCard(
    name: String,
    sub: String,
    icon: ImageVector,
    role: EduChipRole,
    onClick: () -> Unit,
) {
    val colors = EduAiTheme.colors
    val (fg, bg) = colors.forRole(role)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface2)
                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                .pressScaleClickable(onClick = onClick, pressedScale = 0.97f)
                .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(bg),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(24.dp)) }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = colors.textSecondary, fontSize = 12.5.sp)
        }
        Text("›", color = colors.textMuted, fontSize = 22.sp)
    }
}

/* ---------------- step 2 · chapter ---------------- */

@Composable
private fun ColumnScope.ChapterStep(
    copy: OnboardingStrings,
    subject: String,
    subjectLabel: String,
    chapters: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = EduAiTheme.colors
    BackLink(copy.backSubject, onBack)
    StepLabel(copy.step2)
    Text(copy.chapterTitle(subjectLabel), color = colors.text, fontSize = 25.sp, fontWeight = FontWeight.Black)
    Spacer(modifier = Modifier.height(6.dp))
    Text(copy.chapterSub, color = colors.textSecondary, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(18.dp))
    Column(
        modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        chapters.forEachIndexed { i, c ->
            Entrance(delayMillis = i * 50) {
                ChapterRow(
                    title = c,
                    recommended = i == 0,
                    recommendedLabel = copy.recommended,
                    selected = c == selected,
                    onClick = { onSelect(c) },
                )
            }
        }
        if (chapters.isEmpty()) {
            Text(
                text = copy.loadingChapters,
                color = colors.textMuted,
                fontSize = 13.sp,
            )
        }
    }
    Spacer(modifier = Modifier.height(14.dp))
    EduPrimaryButton(text = copy.continueLabel, onClick = onNext, fillMaxWidth = true, enabled = selected.isNotEmpty())
}

@Composable
private fun ChapterRow(
    title: String,
    recommended: Boolean,
    recommendedLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = EduAiTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) colors.accentBg else colors.surface2)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) colors.accent else colors.border,
                    shape = RoundedCornerShape(14.dp),
                )
                .pressScaleClickable(onClick = onClick, pressedScale = 0.97f)
                .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (recommended) Text(recommendedLabel, color = colors.accent, fontSize = 11.5.sp)
        }
        Box(
            modifier =
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (selected) colors.accent else colors.surface1)
                    .border(if (selected) 0.dp else 1.5.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(14.dp))
        }
    }
}

/* ---------------- step 3 · reward world ---------------- */

/** Theme behind each onboarding world key — must match HomeViewModel.applyOnboardingPicksOnce. */
private fun worldTheme(key: String): Theme =
    if (key.equals("Space", ignoreCase = true)) Theme.OUTPOST else Theme.GARDEN

/** A lively starter scene so the picker shows the real garden / space art, not an empty plot. */
private fun previewScene(theme: Theme): GardenSceneSnapshot {
    val zone = theme.starterZone()
    return GardenSceneSnapshot(
        currentZone = zone,
        steps = 2,
        previewSeed = theme.ordinal + 1,
        planted = (0 until 5).map { GardenPlantedRow(zone = zone, plot = it, slot = it % SLOTS_PER_ZONE) },
    )
}

@Composable
private fun ColumnScope.WorldStep(
    copy: OnboardingStrings,
    selected: String,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
    onBuild: () -> Unit,
) {
    val colors = EduAiTheme.colors
    val time by rememberSceneTime(enabled = true)
    BackLink(copy.backChapter, onBack)
    StepLabel(copy.step3)
    Text(copy.pickWorldTitle, color = colors.text, fontSize = 22.sp, fontWeight = FontWeight.Black)
    Spacer(modifier = Modifier.height(4.dp))
    Text(copy.pickWorldSub, color = colors.textSecondary, fontSize = 13.sp)
    Spacer(modifier = Modifier.height(12.dp))
    Column(
        modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        copy.worlds.forEachIndexed { index, world ->
            val theme = worldTheme(world.key)
            val scene = remember(theme) { previewScene(theme) }
            Entrance(delayMillis = index * 60) {
                WorldCard(
                    name = world.label,
                    headline = world.headline,
                    sub = world.sub,
                    theme = theme,
                    scene = scene,
                    time = time,
                    role = if (index == 0) EduChipRole.Success else EduChipRole.Pro,
                    selected = selected == world.key,
                ) { onSelect(world.key) }
            }
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
    EduPrimaryButton(text = copy.continueLabel, onClick = onBuild, fillMaxWidth = true, enabled = selected.isNotEmpty())
}

/* ---------------- step 4 · tutor avatar ---------------- */

@Composable
private fun ColumnScope.TutorStep(
    copy: OnboardingStrings,
    selected: String,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
    onBuild: () -> Unit,
) {
    val colors = EduAiTheme.colors
    BackLink(copy.backWorld, onBack)
    StepLabel(copy.step4)
    Text(copy.pickTutorTitle, color = colors.text, fontSize = 22.sp, fontWeight = FontWeight.Black)
    Spacer(modifier = Modifier.height(4.dp))
    Text(copy.pickTutorSub, color = colors.textSecondary, fontSize = 13.sp)
    Spacer(modifier = Modifier.height(12.dp))
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth().weight(1f),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        items(OnboardingTutorPresets, key = { it.id }) { preset ->
            val c = preset.config
            val isOrb = c.character == TutorCharacter.Orb
            TutorCard(
                name = preset.name,
                tagline = preset.tagline,
                selected = selected == preset.id,
                // Orbs glow — show them on a dark frame so they're clearly visible; faces on light.
                avatarBg = if (isOrb) Color(0xFF17263A) else colors.surface1,
                avatar = {
                    EduTutorAvatar(
                        character = c.character,
                        modifier =
                            if (isOrb) {
                                Modifier.fillMaxSize()
                            } else {
                                Modifier.fillMaxSize().avatarFaceZoom(1.9f, ONBOARDING_TUTOR_FACE_OFFSET_Y)
                            },
                        outfitVariant = c.outfit,
                        hairStyle = c.hair,
                        hairColor = c.hairColor,
                        glassesStyle = c.glasses,
                        glassesColor = c.frameColor,
                        neckStyle = c.neck,
                        underEyeLine = c.eyeLine,
                        cheekShading = c.cheeks,
                    )
                },
                onClick = { onSelect(preset.id) },
            )
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = copy.surpriseMe,
        color = colors.accent,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .pressScaleClickable(
                onClick = { OnboardingTutorPresets.randomOrNull()?.let { onSelect(it.id) } },
                pressedScale = 0.9f,
            )
            .padding(vertical = 4.dp),
    )
    Spacer(modifier = Modifier.height(6.dp))
    EduPrimaryButton(text = copy.buildPlan, onClick = onBuild, fillMaxWidth = true, enabled = selected.isNotEmpty())
}

@Composable
private fun TutorCard(
    name: String,
    tagline: String,
    selected: Boolean,
    avatarBg: Color,
    avatar: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    Box(modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface2)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) colors.accent else colors.border,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .pressScaleClickable(onClick = onClick, pressedScale = 0.96f)
                    .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 30% shorter than a square so eight tutors fit without heavy scrolling.
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f / TUTOR_CHOICE_SIZE_SCALE)
                        .clip(RoundedCornerShape(8.dp))
                        .background(avatarBg),
                contentAlignment = Alignment.Center,
            ) {
                avatar()
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(name, color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(
                tagline,
                color = colors.textMuted,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        if (selected) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

@Composable
private fun WorldCard(
    name: String,
    headline: String,
    sub: String,
    theme: Theme,
    scene: GardenSceneSnapshot,
    time: Float,
    role: EduChipRole,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = EduAiTheme.colors
    val (fg, bg) = colors.forRole(role)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) bg else colors.surface2)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) fg else colors.border,
                    shape = RoundedCornerShape(12.dp),
                )
                .pressScaleClickable(onClick = onClick, pressedScale = 0.98f)
                .padding(7.dp),
    ) {
        // Shorter scene band so Garden + Space both fit above Continue.
        ThemeScene(
            state = scene,
            theme = theme,
            time = time,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(sceneAspect(theme) / WORLD_CHOICE_SIZE_SCALE)
                    .clip(RoundedCornerShape(8.dp)),
            showPreview = true,
            cover = true,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name.uppercase(), color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(headline, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(sub, color = colors.textSecondary, fontSize = 11.sp, lineHeight = 14.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier =
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (selected) fg else colors.surface1)
                        .border(if (selected) 0.dp else 1.5.dp, colors.border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/* ---------------- shared bits ---------------- */

@Composable
private fun StepLabel(text: String) {
    val colors = EduAiTheme.colors
    Text(text, color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun BackLink(label: String, onClick: () -> Unit) {
    val colors = EduAiTheme.colors
    Text(
        text = "‹ $label",
        color = colors.accent,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.pressScaleClickable(onClick = onClick, pressedScale = 0.9f).padding(vertical = 4.dp),
    )
    Spacer(modifier = Modifier.height(6.dp))
}
