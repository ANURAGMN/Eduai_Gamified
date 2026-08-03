package com.anurag.eduai.uikit.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.anurag.eduai.uikit.theme.EduAiDimens
import com.anurag.eduai.uikit.theme.EduAiTheme

@Composable
fun EduPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillMaxWidth: Boolean = false,
) {
    val colors = EduAiTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by
        animateFloatAsState(
            targetValue = if (pressed) 0.93f else 1f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            label = "primaryBtnPress",
        )
    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier =
            (if (fillMaxWidth) modifier.fillMaxWidth() else modifier.widthIn(min = 96.dp)).scale(scale),
        enabled = enabled,
        shape = RoundedCornerShape(EduAiDimens.buttonRadius),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.onAccent,
                disabledContainerColor = colors.border,
                disabledContentColor = colors.textMuted,
            ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun EduSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillMaxWidth: Boolean = false,
) {
    val colors = EduAiTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by
        animateFloatAsState(
            targetValue = if (pressed) 0.93f else 1f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            label = "secondaryBtnPress",
        )
    OutlinedButton(
        onClick = onClick,
        interactionSource = interactionSource,
        enabled = enabled,
        modifier =
            (if (fillMaxWidth) modifier.fillMaxWidth() else modifier.widthIn(min = 96.dp)).scale(scale),
        shape = RoundedCornerShape(EduAiDimens.buttonRadius),
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = colors.surface2,
                contentColor = colors.textSecondary,
                disabledContentColor = colors.textMuted,
            ),
        border = BorderStroke(1.dp, colors.borderStrong),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun EduGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text = text, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}
