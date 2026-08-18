package com.anurag.eduai.uikit.avatar

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import com.anurag.eduai.uikit.avatar.core.TutorCharacter

/** A named premium avatar look that can be unlocked and shared. */
data class AvatarPreset(
    val id: String,
    val name: String,
    val tagline: String,
    val config: TutorConfig,
)

/**
 * The full pool of premium avatars. A rotating slice of these is offered each
 * week (see [weeklyAvatarPresets]) to give people a reason to come back.
 */
val AllAvatarPresets: List<AvatarPreset> =
    listOf(
        AvatarPreset(
            "scholar", "Scholar", "Sharp and studious",
            TutorConfig(TutorCharacter.Free, outfit = 1, neck = 0, hair = 1, hairColor = 0, glasses = 0, frameColor = 0, eyeLine = true, cheeks = true),
        ),
        AvatarPreset(
            "nova", "Nova", "Cosmic energy",
            TutorConfig(TutorCharacter.Orb),
        ),
        AvatarPreset(
            "ace", "Ace", "Ready to win",
            TutorConfig(TutorCharacter.Free, outfit = 2, neck = 2, hair = 2, hairColor = 2, glasses = 1, frameColor = 2, eyeLine = false, cheeks = true),
        ),
        AvatarPreset(
            "sage", "Sage", "Calm and wise",
            TutorConfig(TutorCharacter.Free, outfit = 3, neck = 1, hair = 0, hairColor = 1, glasses = 0, frameColor = 1, eyeLine = false, cheeks = true),
        ),
        AvatarPreset(
            "spark", "Spark", "Bright and bubbly",
            TutorConfig(TutorCharacter.Free, outfit = 0, neck = 0, hair = 2, hairColor = 2, glasses = 2, frameColor = 0, eyeLine = true, cheeks = true),
        ),
        AvatarPreset(
            "pulse", "Pulse", "Pure focus",
            TutorConfig(TutorCharacter.Orb),
        ),
        AvatarPreset(
            "quill", "Quill", "Old-school cool",
            TutorConfig(TutorCharacter.Free, outfit = 1, neck = 2, hair = 1, hairColor = 0, glasses = 0, frameColor = 2, eyeLine = true, cheeks = false),
        ),
        // "Legends" — original scientist archetypes (no real person's likeness), for the
        // discover-a-scientist vibe. Name real scientists in lesson content, not here.
        AvatarPreset(
            "inventor", "The Inventor", "Sparks of genius",
            TutorConfig(TutorCharacter.Free, outfit = 3, neck = 0, hair = 2, hairColor = 2, glasses = 1, frameColor = 0, eyeLine = true, cheeks = true),
        ),
        AvatarPreset(
            "astronomer", "The Astronomer", "Head in the stars",
            TutorConfig(TutorCharacter.Free, outfit = 2, neck = 1, hair = 0, hairColor = 1, glasses = 2, frameColor = 1, eyeLine = false, cheeks = true),
        ),
        AvatarPreset(
            "naturalist", "The Naturalist", "Curious about everything",
            TutorConfig(TutorCharacter.Free, outfit = 1, neck = 2, hair = 1, hairColor = 1, glasses = 0, frameColor = 0, eyeLine = false, cheeks = true),
        ),
        AvatarPreset(
            "mathematician", "The Mathematician", "Numbers everywhere",
            TutorConfig(TutorCharacter.Free, outfit = 0, neck = 1, hair = 2, hairColor = 0, glasses = 1, frameColor = 2, eyeLine = true, cheeks = true),
        ),
    )

/**
 * Distinct-looking subset shown in the onboarding tutor picker. Drops visual duplicates:
 * `quill` / `naturalist` read the same as `scholar` (outfit 1, hair 1, no glasses), and `pulse`
 * is the identical Orb config as `nova`. Order is stable.
 */
val OnboardingTutorPresets: List<AvatarPreset> =
    listOf("scholar", "nova", "ace", "sage", "spark", "inventor", "astronomer", "mathematician")
        .mapNotNull { id -> AllAvatarPresets.firstOrNull { it.id == id } }

private const val WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000

private fun epochWeek(now: Long): Int = (now / WEEK_MILLIS).toInt()

/** The [count] avatars featured this week — rotates deterministically by week. */
fun weeklyAvatarPresets(now: Long = System.currentTimeMillis(), count: Int = 3): List<AvatarPreset> {
    if (AllAvatarPresets.isEmpty()) return emptyList()
    val start = (epochWeek(now) * count).mod(AllAvatarPresets.size)
    return List(count.coerceAtMost(AllAvatarPresets.size)) { i ->
        AllAvatarPresets[(start + i).mod(AllAvatarPresets.size)]
    }
}

/** Whole days until the next weekly drop. */
fun daysUntilNextDrop(now: Long = System.currentTimeMillis()): Int {
    val nextBoundary = (epochWeek(now) + 1) * WEEK_MILLIS
    val remaining = nextBoundary - now
    return ((remaining + WEEK_MILLIS_DAY - 1) / WEEK_MILLIS_DAY).toInt().coerceAtLeast(1)
}

private const val WEEK_MILLIS_DAY = 24L * 60 * 60 * 1000

/** SharedPreferences-backed set of unlocked preset ids, exposed as Compose state. */
object AvatarUnlockStore {
    private const val PREFS = "eduai_avatar_unlocks"
    private const val KEY = "unlocked_ids"
    val unlocked: MutableState<Set<String>> = mutableStateOf(emptySet())
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        unlocked.value = p.getStringSet(KEY, emptySet())?.toSet() ?: emptySet()
    }

    fun unlock(context: Context, id: String) {
        val next = unlocked.value + id
        unlocked.value = next
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY, next)
            .apply()
    }

    fun isUnlocked(id: String): Boolean = unlocked.value.contains(id)
}

@Composable
fun rememberUnlockedAvatars(): Set<String> {
    val context = LocalContext.current
    AvatarUnlockStore.load(context)
    return AvatarUnlockStore.unlocked.value
}
