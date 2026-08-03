package com.anurag.eduai.uikit.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

enum class EduThemeMode {
    System,
    Light,
    Dark,
}

val LocalEduAiColors = staticCompositionLocalOf { LightEduAiColors }

@Composable
fun EduAiTheme(
    themeMode: EduThemeMode = EduThemeMode.System,
    content: @Composable () -> Unit,
) {
    val darkTheme =
        when (themeMode) {
            EduThemeMode.System -> isSystemInDarkTheme()
            EduThemeMode.Light -> false
            EduThemeMode.Dark -> true
        }
    val colors = if (darkTheme) DarkEduAiColors else LightEduAiColors

    val materialScheme =
        if (darkTheme) {
            darkColorScheme(
                primary = colors.accent,
                onPrimary = colors.onAccent,
                background = colors.surface1,
                surface = colors.surface2,
                onBackground = colors.text,
                onSurface = colors.text,
            )
        } else {
            lightColorScheme(
                primary = colors.accent,
                onPrimary = colors.onAccent,
                background = colors.surface1,
                surface = colors.surface2,
                onBackground = colors.text,
                onSurface = colors.text,
            )
        }

    CompositionLocalProvider(LocalEduAiColors provides colors) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = EduAiTypography,
            content = content,
        )
    }
}

object EduAiTheme {
    val colors: EduAiColors
        @Composable get() = LocalEduAiColors.current
}
