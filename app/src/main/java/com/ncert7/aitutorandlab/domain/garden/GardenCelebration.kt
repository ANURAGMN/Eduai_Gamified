package com.ncert7.aitutorandlab.domain.garden

import com.anurag.eduai.uikit.garden.quest.Theme

/** Pending garden celebration after [GardenRepository.recordCompletion] plants a row. */
data class GardenCelebration(
    val theme: Theme,
    val zone: Int,
    val slot: Int,
    val placeCompleted: Boolean,
    val itemLabel: String,
    val placeLabel: String,
    val totalPlanted: Int,
    val remainingInPlace: Int,
    val remainingScenes: Int,
)

/** Params for the planted illustration on [MomentOverlay]. */
data class GardenMomentArt(
    val zone: Int,
    val slot: Int,
    val theme: Theme = Theme.GARDEN,
)
