package com.ncert7.aitutorandlab.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Per-student garden loop state (single row per student). */
@Entity(tableName = "garden_state")
data class GardenStateEntity(
    @PrimaryKey val studentId: String,
    /** GARDEN | OUTPOST | ISLAND | COLONY */
    val theme: String = GardenTheme.GARDEN,
    /** Places visited in order, e.g. `"0,4,2"`. */
    val route: String = "0",
    /** Steps toward the current growing item (0–6; 7th triggers plant). */
    val steps: Int = 0,
    /** -1 = surprise slot; 0–5 = locked pick. */
    val preferredSlot: Int = -1,
)

object GardenTheme {
    const val GARDEN = "GARDEN"
    const val OUTPOST = "OUTPOST"
    const val ISLAND = "ISLAND"
    const val COLONY = "COLONY"
}
