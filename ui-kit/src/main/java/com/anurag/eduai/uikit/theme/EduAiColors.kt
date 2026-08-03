package com.anurag.eduai.uikit.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class EduAiColors(
    val accent: Color,
    val accentBg: Color,
    val success: Color,
    val successBg: Color,
    val warning: Color,
    val warningBg: Color,
    val danger: Color,
    val dangerBg: Color,
    val pro: Color,
    val proBg: Color,
    val surface1: Color,
    val surface2: Color,
    val border: Color,
    val borderStrong: Color,
    val text: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val heroGradientStart: Color,
    val heroGradientEnd: Color,
    val toastBackground: Color,
    val onAccent: Color = Color.White,
    val onSuccess: Color = Color.White,
)

val LightEduAiColors =
    EduAiColors(
        accent = EduAiPalette.Accent,
        accentBg = EduAiPalette.AccentBg,
        success = EduAiPalette.Success,
        successBg = EduAiPalette.SuccessBg,
        warning = EduAiPalette.Warning,
        warningBg = EduAiPalette.WarningBg,
        danger = EduAiPalette.Danger,
        dangerBg = EduAiPalette.DangerBg,
        pro = EduAiPalette.Pro,
        proBg = EduAiPalette.ProBg,
        surface1 = EduAiPalette.Surface1,
        surface2 = EduAiPalette.Surface2,
        border = EduAiPalette.Border,
        borderStrong = EduAiPalette.BorderStrong,
        text = EduAiPalette.Text,
        textSecondary = EduAiPalette.TextSecondary,
        textMuted = EduAiPalette.TextMuted,
        heroGradientStart = EduAiPalette.HeroGradientStart,
        heroGradientEnd = EduAiPalette.HeroGradientEnd,
        toastBackground = EduAiPalette.ToastBackground,
    )

val DarkEduAiColors =
    EduAiColors(
        accent = EduAiPaletteDark.Accent,
        accentBg = EduAiPaletteDark.AccentBg,
        success = EduAiPaletteDark.Success,
        successBg = EduAiPaletteDark.SuccessBg,
        warning = EduAiPaletteDark.Warning,
        warningBg = EduAiPaletteDark.WarningBg,
        danger = EduAiPaletteDark.Danger,
        dangerBg = EduAiPaletteDark.DangerBg,
        pro = EduAiPaletteDark.Pro,
        proBg = EduAiPaletteDark.ProBg,
        surface1 = EduAiPaletteDark.Surface1,
        surface2 = EduAiPaletteDark.Surface2,
        border = EduAiPaletteDark.Border,
        borderStrong = EduAiPaletteDark.BorderStrong,
        text = EduAiPaletteDark.Text,
        textSecondary = EduAiPaletteDark.TextSecondary,
        textMuted = EduAiPaletteDark.TextMuted,
        heroGradientStart = EduAiPaletteDark.HeroGradientStart,
        heroGradientEnd = EduAiPaletteDark.HeroGradientEnd,
        toastBackground = EduAiPaletteDark.ToastBackground,
    )

enum class EduChipRole {
    Accent,
    Success,
    Warning,
    Danger,
    Pro,
    Neutral,
}

fun EduAiColors.forRole(role: EduChipRole): Pair<Color, Color> =
    when (role) {
        EduChipRole.Accent -> accent to accentBg
        EduChipRole.Success -> success to successBg
        EduChipRole.Warning -> warning to warningBg
        EduChipRole.Danger -> danger to dangerBg
        EduChipRole.Pro -> pro to proBg
        EduChipRole.Neutral -> textSecondary to surface2
    }
