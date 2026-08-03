package com.ncert7.aitutorandlab.domain.garden

/** Row shown in the avatar Scene segment plant list. */
data class GardenPlantedListRow(
    val conceptLabel: String,
    val kindLabel: String,
    val slotLabel: String,
    val zoneIndex: Int = 0,
    val zoneLabel: String = "",
)
