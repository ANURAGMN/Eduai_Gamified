package com.ncert7.aitutorandlab.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.anurag.eduai.uikit.theme.EduThemeMode
import com.anurag.eduai.uikit.theme.ThemeModeStore

private val DarkColorScheme = darkColorScheme()

private val LightColorScheme = lightColorScheme()

/** Resolves the user's saved Light/Dark choice, falling back to the device setting. */
@Composable
private fun resolveDarkTheme(): Boolean =
    when (ThemeModeStore.mode.value) {
        EduThemeMode.Dark -> true
        EduThemeMode.Light -> false
        EduThemeMode.System -> isSystemInDarkTheme()
    }

@Composable
fun AppTheme(
    darkTheme: Boolean = resolveDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
