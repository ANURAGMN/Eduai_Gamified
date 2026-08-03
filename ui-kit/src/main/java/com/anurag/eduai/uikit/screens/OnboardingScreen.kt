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
import com.anurag.eduai.uikit.components.Entrance
import com.anurag.eduai.uikit.components.EduPrimaryButton
import com.anurag.eduai.uikit.components.pressScaleClickable
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.anurag.eduai.uikit.theme.EduChipRole
import com.anurag.eduai.uikit.theme.forRole

/** What the student chose during first-run. Handed to the app when onboarding finishes. */
data class OnboardingResult(
    val subject: String,
    val chapter: String,
    /** "Garden" or "Space" — the reward world. */
    val world: String,
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

/**
 * First-run onboarding — three intro slides, then three picks: subject → chapter → reward world.
 * Mirrors the approved prototype flow. Calls [onFinish] with the chosen subject, chapter and world
 * when the student taps "Build my plan".
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
                else ->
                    WorldStep(
                        copy = copy,
                        selected = world,
                        onSelect = { world = it },
                        onBack = { pickStage = STAGE_CHAPTER },
                        onBuild = {
                            if (world.isNotEmpty()) {
                                onWorldSelected(world)
                                onFinish(OnboardingResult(subject, chapter, world))
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

@Composable
private fun ColumnScope.WorldStep(
    copy: OnboardingStrings,
    selected: String,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
    onBuild: () -> Unit,
) {
    val colors = EduAiTheme.colors
    BackLink(copy.backChapter, onBack)
    StepLabel(copy.step3)
    Text(copy.pickWorldTitle, color = colors.text, fontSize = 25.sp, fontWeight = FontWeight.Black)
    Spacer(modifier = Modifier.height(6.dp))
    Text(copy.pickWorldSub, color = colors.textSecondary, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(18.dp))
    Column(
        modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        copy.worlds.forEachIndexed { index, world ->
            Entrance(delayMillis = index * 60) {
                WorldCard(
                    name = world.label,
                    headline = world.headline,
                    sub = world.sub,
                    icon = if (index == 0) Icons.Filled.Eco else Icons.Filled.RocketLaunch,
                    role = if (index == 0) EduChipRole.Success else EduChipRole.Pro,
                    selected = selected == world.key,
                ) { onSelect(world.key) }
            }
        }
    }
    Spacer(modifier = Modifier.height(14.dp))
    EduPrimaryButton(text = copy.buildPlan, onClick = onBuild, fillMaxWidth = true, enabled = selected.isNotEmpty())
}

@Composable
private fun WorldCard(
    name: String,
    headline: String,
    sub: String,
    icon: ImageVector,
    role: EduChipRole,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = EduAiTheme.colors
    val (fg, bg) = colors.forRole(role)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) bg else colors.surface2)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) fg else colors.border,
                    shape = RoundedCornerShape(16.dp),
                )
                .pressScaleClickable(onClick = onClick, pressedScale = 0.98f)
                .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(15.dp)).background(bg),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(27.dp)) }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name.uppercase(), color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(headline, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(sub, color = colors.textSecondary, fontSize = 12.5.sp, lineHeight = 17.sp)
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
