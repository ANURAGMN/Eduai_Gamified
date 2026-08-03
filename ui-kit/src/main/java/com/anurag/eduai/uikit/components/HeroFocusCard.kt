package com.anurag.eduai.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.theme.EduAiDimens
import com.anurag.eduai.uikit.theme.EduAiTheme

/** Matches prototype: soft accent→pro diagonal wash, dark text, compact primary CTA. */
@Composable
fun HeroFocusCard(
    eyebrow: String,
    title: String,
    subtitle: String,
    buttonLabel: String,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(EduAiDimens.cardRadius))
                .background(Brush.linearGradient(listOf(colors.accentBg, colors.proBg)))
                .shimmer()
                .pressScaleClickable(onClick = onStartClick, pressedScale = 0.97f)
                .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = eyebrow,
            color = colors.accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Text(
            text = title,
            color = colors.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 22.sp,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = subtitle,
            color = colors.textSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        EduPrimaryButton(text = buttonLabel, onClick = onStartClick, fillMaxWidth = false)
    }
}

@Composable
fun HeroDoneCard(
    eyebrow: String,
    title: String,
    subtitle: String,
    buttonLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    xpEarned: Int = 35,
) {
    val colors = EduAiTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(EduAiDimens.cardRadius))
                .background(colors.successBg)
                .shimmer(highlight = colors.success.copy(alpha = 0.18f))
                .pressScaleClickable(onClick = onActionClick, pressedScale = 0.97f)
                .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(bottom = 6.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                text = "All done for today · +",
                color = colors.success,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            AnimatedCounterText(
                value = xpEarned,
                color = colors.success,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = " XP earned",
                color = colors.success,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = title,
            color = colors.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = subtitle,
            color = colors.textSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        EduSecondaryButton(text = buttonLabel, onClick = onActionClick, fillMaxWidth = false)
    }
}
