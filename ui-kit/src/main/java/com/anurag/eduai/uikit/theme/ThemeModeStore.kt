package com.anurag.eduai.uikit.theme

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Process-wide, prefs-backed theme choice. [EduAiTheme] reads [mode] as its default, so every
 * screen honors the user's pick without per-screen wiring. Defaults to [EduThemeMode.System] so a
 * fresh install keeps following the device until the user explicitly chooses Light or Dark.
 */
object ThemeModeStore {
    private const val PREFS = "eduai_theme"
    private const val KEY = "theme_mode"

    val mode: MutableState<EduThemeMode> = mutableStateOf(EduThemeMode.System)
    private var loaded = false

    /** Load the saved choice once (call early, e.g. MainActivity.onCreate before setContent). */
    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        mode.value = parse(saved)
    }

    /** Persist and apply a new choice; all EduAiTheme surfaces recompose. */
    fun set(context: Context, value: EduThemeMode) {
        mode.value = value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, value.name)
            .apply()
    }

    private fun parse(raw: String?): EduThemeMode =
        when (raw) {
            EduThemeMode.Light.name -> EduThemeMode.Light
            EduThemeMode.Dark.name -> EduThemeMode.Dark
            else -> EduThemeMode.System
        }
}
