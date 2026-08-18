package com.anurag.eduai.uikit.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.garden.quest.ConfettiBurst
import com.anurag.eduai.uikit.garden.quest.Theme

// A full-screen takeover moment, deliberately dark (Duolingo-style) regardless of app light/dark
// theme — near-black with a slight cool tint, not pure #000.
private val Backdrop = Color(0xFF0E1116)
private val Ink = Color(0xFF14181E)
private val SoftText = Color(0xFFC2C7CF)
private val FlameOuter = Color(0xFFF0641E)
private val FlameInner = Color(0xFFFFC24D)

/**
 * First-open-of-the-day streak screen — the gentle "good to see you, keep it alive" beat, shown once
 * per calendar day before home. [doneDays] lights the last N of the seven-day row; [todayIndex] is
 * the ringed day. Not the triumphant extension moment — that is [StreakExtendedOverlay].
 */
@Composable
fun StreakCelebrationOverlay(
    visible: Boolean,
    streak: Int,
    name: String = "there",
    doneDays: Int = 0,
    todayIndex: Int = 0,
    copy: StreakCopy = defaultStreakCopy(),
    onContinue: () -> Unit,
) {
    if (!visible) return
    val displayName = name.ifBlank { copy.fallbackName }
    Box(
        Modifier
            .fillMaxSize()
            .background(Backdrop)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Flame(Modifier.size(118.dp))
            Spacer(Modifier.height(8.dp))
            Text("$streak", color = Color.White, fontSize = 62.sp, fontWeight = FontWeight.Medium)
            Text(copy.dayStreakLabel, color = SoftText, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                copy.greetingLine(displayName),
                color = SoftText,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            WeekRow(
                doneDays = doneDays,
                todayIndex = todayIndex,
                weekdayLetters = copy.weekdayLetters,
            )
            Spacer(Modifier.height(32.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable { onContinue() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(copy.continueLabel, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/**
 * The earned, triumphant beat — fired when the streak actually extends (off
 * StreakRepository.recordActivity, i.e. the day's first finished activity). Bigger flame, confetti,
 * the week row lit through today. This celebrates loudly, unlike the gentle first-open screen above.
 */
@Composable
fun StreakExtendedOverlay(
    visible: Boolean,
    streak: Int,
    name: String = "there",
    doneDays: Int = 0,
    todayIndex: Int = 0,
    copy: StreakCopy = defaultStreakCopy(),
    onDone: () -> Unit,
) {
    if (!visible) return
    val displayName = name.ifBlank { copy.fallbackName }
    var trigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { trigger = 1 }
    Box(
        Modifier
            .fillMaxSize()
            .background(Backdrop)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Flame(Modifier.size(140.dp))
            Spacer(Modifier.height(8.dp))
            Text(copy.extendedTitle, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text("$streak", color = Color.White, fontSize = 66.sp, fontWeight = FontWeight.Medium)
            Text(copy.dayStreakLabel, color = SoftText, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                copy.extendedLine(displayName),
                color = SoftText,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            WeekRow(
                doneDays = doneDays,
                todayIndex = todayIndex,
                weekdayLetters = copy.weekdayLetters,
            )
            Spacer(Modifier.height(32.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable { onDone() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(copy.awesomeLabel, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
        ConfettiBurst(trigger = trigger, theme = Theme.GARDEN, count = 44, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun Flame(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        val outer =
            Path().apply {
                moveTo(cx, h * 0.04f)
                cubicTo(cx + w * 0.46f, h * 0.44f, cx + w * 0.30f, h * 0.96f, cx, h * 0.96f)
                cubicTo(cx - w * 0.30f, h * 0.96f, cx - w * 0.46f, h * 0.44f, cx, h * 0.04f)
                close()
            }
        drawPath(outer, FlameOuter)

        val inner =
            Path().apply {
                moveTo(cx, h * 0.40f)
                cubicTo(cx + w * 0.26f, h * 0.62f, cx + w * 0.17f, h * 0.90f, cx, h * 0.90f)
                cubicTo(cx - w * 0.17f, h * 0.90f, cx - w * 0.26f, h * 0.62f, cx, h * 0.40f)
                close()
            }
        drawPath(inner, FlameInner)
    }
}

@Composable
private fun WeekRow(
    doneDays: Int,
    todayIndex: Int,
    weekdayLetters: List<String> = listOf("M", "T", "W", "T", "F", "S", "S"),
) {
    val labels =
        if (weekdayLetters.size >= 7) weekdayLetters.take(7) else listOf("M", "T", "W", "T", "F", "S", "S")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { i, d ->
            val done = i < doneDays
            val today = i == todayIndex
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (done) FlameInner else Color(0x33FFFFFF))
                        .border(
                            if (today) 2.dp else 0.dp,
                            if (today) Color.White else Color.Transparent,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) Text("✓", color = Color(0xFF7A3E00), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(3.dp))
                Text(d, color = SoftText, fontSize = 10.sp)
            }
        }
    }
}
