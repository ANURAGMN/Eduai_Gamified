package com.anurag.eduai.uikit.avatar

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.anurag.eduai.uikit.avatar.core.AvatarState
import com.anurag.eduai.uikit.avatar.core.TutorCharacter

/**
 * The persisted "look" of the user's tutor — character choice plus every Free-face
 * customization. Mood/gesture/spin are momentary interactions and are intentionally
 * not saved.
 */
data class TutorConfig(
    val character: TutorCharacter = TutorCharacter.Orb,
    val outfit: Int = 0,
    val neck: Int = 0,
    val hair: Int = 0,
    val hairColor: Int = 0,
    val glasses: Int = 0,
    val frameColor: Int = 0,
    val eyeLine: Boolean = false,
    val cheeks: Boolean = true,
)

/**
 * Process-wide, SharedPreferences-backed store for the saved tutor look. Exposes a
 * Compose [state] so any screen that reads it re-renders when the config is saved.
 */
object TutorConfigStore {
    private const val PREFS = "eduai_tutor_config"
    val state = mutableStateOf(TutorConfig())
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.contains("character")) return
        state.value =
            TutorConfig(
                character =
                    runCatching { TutorCharacter.valueOf(p.getString("character", "Orb")!!) }
                        .getOrDefault(TutorCharacter.Orb),
                outfit = p.getInt("outfit", 0),
                neck = p.getInt("neck", 0),
                hair = p.getInt("hair", 0),
                hairColor = p.getInt("hairColor", 0),
                glasses = p.getInt("glasses", 0),
                frameColor = p.getInt("frameColor", 0),
                eyeLine = p.getBoolean("eyeLine", false),
                cheeks = p.getBoolean("cheeks", true),
            )
    }

    fun save(context: Context, config: TutorConfig) {
        state.value = config
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        editor.putString("character", config.character.name)
        editor.putInt("outfit", config.outfit)
        editor.putInt("neck", config.neck)
        editor.putInt("hair", config.hair)
        editor.putInt("hairColor", config.hairColor)
        editor.putInt("glasses", config.glasses)
        editor.putInt("frameColor", config.frameColor)
        editor.putBoolean("eyeLine", config.eyeLine)
        editor.putBoolean("cheeks", config.cheeks)
        editor.commit()
    }
}

/** Loads (once) and returns the current saved tutor config, re-composing on save. */
@Composable
fun rememberSavedTutorConfig(): TutorConfig {
    val context = LocalContext.current
    TutorConfigStore.load(context)
    val config by TutorConfigStore.state
    return config
}

/**
 * Renders the user's *saved* tutor avatar at the given [state]/mood. Used anywhere
 * the persisted look should appear — Home mascot, top bar, reward moment.
 */
@Composable
fun SavedTutorAvatar(
    state: AvatarState = AvatarState.Idle,
    modifier: Modifier = Modifier,
) {
    val cfg = rememberSavedTutorConfig()
    EduTutorAvatar(
        character = cfg.character,
        state = state,
        modifier = modifier,
        outfitVariant = cfg.outfit,
        hairStyle = cfg.hair,
        hairColor = cfg.hairColor,
        glassesStyle = cfg.glasses,
        glassesColor = cfg.frameColor,
        neckStyle = cfg.neck,
        underEyeLine = cfg.eyeLine,
        cheekShading = cfg.cheeks,
    )
}
