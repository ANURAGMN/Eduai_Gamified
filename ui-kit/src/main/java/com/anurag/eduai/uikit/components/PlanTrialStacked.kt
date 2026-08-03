package com.anurag.eduai.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.theme.EduAiTheme

/**
 * The "stacked" trial view: the same chapter/type grouping as the path, but as a clean list.
 * The chapter name appears once as a header, each type (Simulations / Study / Revision) once as a
 * sub-group, and rows show only the concept — no repeated chapter or type text.
 */
@Composable
fun PlanTrialStacked(
    chapters: List<TrialPathChapter>,
    onNodeClick: (TrialPathNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        chapters.forEach { chapter ->
            ChapterBand(title = chapter.title)
            Spacer(modifier = Modifier.height(10.dp))

            var lastType: TrialNodeType? = null
            chapter.nodes.forEachIndexed { index, node ->
                if (node.type != lastType) {
                    val runLength =
                        chapter.nodes.drop(index).takeWhile { it.type == node.type }.size
                    TypeSubHeader(type = node.type, count = runLength)
                    Spacer(modifier = Modifier.height(6.dp))
                    lastType = node.type
                }
                StackedRow(node = node, onClick = { onNodeClick(node) })
                Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun ChapterBand(title: String) {
    val colors = EduAiTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.accent)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column {
            Text(
                text = "CHAPTER",
                color = colors.onAccent.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = title,
                color = colors.onAccent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TypeSubHeader(type: TrialNodeType, count: Int) {
    val colors = EduAiTheme.colors
    val color = typeColor(type)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(typeIcon(type), contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = typeGroupLabel(type).uppercase(),
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "· $count", color = colors.textMuted, fontSize = 12.sp)
    }
}

@Composable
private fun StackedRow(node: TrialPathNode, onClick: () -> Unit) {
    val colors = EduAiTheme.colors
    val color = typeColor(node.type)
    val done = node.state == TrialNodeState.Done
    val active = node.state == TrialNodeState.Active

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (active) color.copy(alpha = 0.10f) else colors.surface2)
                .pressScaleClickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (done) color else color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = typeIcon(node.type),
                contentDescription = null,
                tint = if (done) colors.onAccent else color,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = node.title,
                color = colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            val subtitle =
                when {
                    done -> "Completed"
                    active -> "Start now"
                    else -> "Not started"
                }
            Text(text = subtitle, color = if (active) color else colors.textMuted, fontSize = 12.sp)
            if (!done && node.progress > 0f) {
                Spacer(modifier = Modifier.height(6.dp))
                EduProgressBar(progress = node.progress, modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (done) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = colors.success, modifier = Modifier.size(20.dp))
        } else {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun typeColor(type: TrialNodeType): Color {
    val colors = EduAiTheme.colors
    return when (type) {
        TrialNodeType.Simulation -> colors.pro
        TrialNodeType.Study -> colors.accent
        TrialNodeType.Revision -> colors.warning
    }
}

private fun typeIcon(type: TrialNodeType): ImageVector =
    when (type) {
        TrialNodeType.Simulation -> Icons.Outlined.Science
        TrialNodeType.Study -> Icons.Outlined.MenuBook
        TrialNodeType.Revision -> Icons.Outlined.Autorenew
    }

private fun typeGroupLabel(type: TrialNodeType): String =
    when (type) {
        TrialNodeType.Simulation -> "Simulations"
        TrialNodeType.Study -> "Study"
        TrialNodeType.Revision -> "Revision"
    }
