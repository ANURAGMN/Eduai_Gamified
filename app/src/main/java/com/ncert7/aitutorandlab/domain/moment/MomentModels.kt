package com.ncert7.aitutorandlab.domain.moment

import com.anurag.eduai.uikit.avatar.TutorConfig
import com.anurag.eduai.uikit.avatar.core.EmotionType
import com.ncert7.aitutorandlab.domain.garden.GardenMomentArt

/**
 * The distinct trial moments that surface a full-screen avatar screen.
 * Celebrations fire on completion; the two nudges pull the student back to pending work.
 */
enum class MomentType {
    SIM_COMPLETED,
    STUDY_COMPLETED,
    DAY_COMPLETED,
    COMEBACK_INCOMPLETE,
    EXIT_INCOMPLETE,
    PLANT_COMPLETED,
    PLACE_COMPLETED,
}

/**
 * Runtime values injected into variant copy via {tokens}. Anything unknown is left blank.
 */
data class MomentTokens(
    val name: String = "",
    val bite: String = "",
    val remaining: Int = 0,
    val pending: Int = 0,
    val league: String = "",
    val rank: Int = 0,
    val gems: Int = 0,
    val xp: Int = 0,
    val item: String = "",
    val place: String = "",
    val planted: Int = 0,
    val remainingInPlace: Int = 0,
    val remainingScenes: Int = 0,
) {
    fun fill(template: String): String =
        template
            .replace("{name}", name)
            .replace("{bite}", bite)
            .replace("{remaining}", remaining.toString())
            .replace("{pending}", pending.toString())
            .replace("{league}", league)
            .replace("{rank}", rank.toString())
            .replace("{gems}", gems.toString())
            .replace("{xp}", xp.toString())
            .replace("{item}", item)
            .replace("{place}", place)
            .replace("{planted}", planted.toString())
            .replace("{remainingInPlace}", remainingInPlace.toString())
            .replace("{remainingScenes}", remainingScenes.toString())
}

/**
 * A single screen template. Five of these exist per [MomentType]; one is drawn at random each time.
 * [emotion] varies the avatar's face so repeats feel different.
 */
data class MomentVariant(
    val id: String,
    val celebratory: Boolean,
    val emotion: EmotionType,
    val headline: String,
    val body: String,
    val primaryCta: String,
    val secondaryCta: String? = null,
)

/**
 * A fully-resolved moment ready to render: chosen variant + a randomly picked avatar look,
 * with all tokens already filled in.
 */
data class MomentUiModel(
    val moment: MomentType,
    val variantId: String,
    val celebratory: Boolean,
    val emotion: EmotionType,
    val avatarConfig: TutorConfig,
    val headline: String,
    val body: String,
    val primaryCta: String,
    val secondaryCta: String?,
    val gems: Int = 0,
    val xp: Int = 0,
    val gardenArt: GardenMomentArt? = null,
)
