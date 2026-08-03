package com.anurag.eduai.uikit.garden.quest

import androidx.compose.runtime.Immutable
import com.anurag.eduai.uikit.garden.world.hash

/** One planted row — slot index only, never a species key. */
@Immutable
data class GardenPlantedRow(
    val zone: Int,
    val plot: Int,
    val slot: Int,
)

/**
 * Read-only garden scene model for [ZoneScene] / [ThemeScene]. Built in `:app` from Room rows;
 * `:ui-kit` draws from this snapshot only.
 */
@Immutable
data class GardenSceneSnapshot(
    val currentZone: Int,
    val steps: Int,
    val preferredSlot: Int = -1,
    val planted: List<GardenPlantedRow> = emptyList(),
    /** Seeds the surprise slot when [preferredSlot] is -1. */
    val previewSeed: Int = 0,
) {
    fun plantedIn(zoneIndex: Int): List<GardenPlantedRow> = planted.filter { it.zone == zoneIndex }

    val filledHere: Int get() = plantedIn(currentZone).size

    val totalPlanted: Int get() = planted.size

    /** Stable preview slot for the sprout currently growing. */
    val slot: Int
        get() =
            if (preferredSlot in 0 until SLOTS_PER_ZONE) {
                preferredSlot
            } else {
                (hash(previewSeed, 11) * SLOTS_PER_ZONE).toInt().coerceIn(0, SLOTS_PER_ZONE - 1)
            }
}
