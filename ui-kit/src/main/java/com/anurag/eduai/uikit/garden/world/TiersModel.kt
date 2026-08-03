package com.anurag.eduai.uikit.garden.world

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

data class Tier(val name: String, val detail: String)

val TIERS = listOf(
    Tier("Lunar landing", "Bare surface, a lander, your first habitat pod."),
    Tier("Solar outpost", "Arrays go up. The lights come on."),
    Tier("Bio dome", "Plants start growing under glass."),
    Tier("Rover station", "A garage, and rovers begin exploring."),
    Tier("Research lab", "Labs and the first crew working outside."),
    Tier("Launch pad", "A pad, and rockets arriving with supplies."),
    Tier("Communication hub", "Dish array up, satellites overhead."),
    Tier("Terraform zone", "Oxygen towers. Grass and water appear."),
    Tier("Colony city", "Residential towers, streets, lit windows."),
    Tier("Fusion reactor", "Advanced power. The whole colony brightens."),
    Tier("Orbital station", "A ring station and a space elevator."),
    Tier("Interstellar civilisation", "A second world, ships leaving the system.")
)

const val TASKS_PER_TIER = 8
const val TOTAL_TIER_TASKS = 96

/**
 * The ladder is the subject-level arc, not the chapter one. Twelve rungs inside a single
 * chapter would reach interstellar civilisation in week one with nothing left to unlock.
 */
@Stable
class TiersState {
    var tasks by mutableIntStateOf(26)
        private set

    var stage by mutableIntStateOf(4)

    /**
     * How far along the build currently in progress is, 0..1. A tier takes eight tasks, so without
     * this the scene would sit still for seven of them — the build site is what makes a single step
     * visible.
     */
    var partial by mutableFloatStateOf(0f)

    val tier: Tier get() = TIERS[stage - 1]
    val next: Tier? get() = if (stage < TIERS.size) TIERS[stage] else null
    val intoTier: Int get() = tasks % TASKS_PER_TIER

    fun jumpToStage(value: Int) {
        stage = value.coerceIn(1, TIERS.size)
        tasks = (stage - 1) * TASKS_PER_TIER + 2
    }

    fun addTasks(n: Int) = applyTaskCount(tasks + n)

    /** Absolute set — the quest loop owns the count, so it pushes rather than increments. */
    fun applyTaskCount(n: Int) {
        tasks = n.coerceIn(0, TOTAL_TIER_TASKS)
        stage = (tasks / TASKS_PER_TIER + 1).coerceIn(1, TIERS.size)
    }
}

/** The colony reuses the module archetypes with a single fixed palette. */
val COLONY_PALETTE = World(
    key = "colony",
    name = "Colony",
    chapter = "",
    skyTop = Color(0xFF2A1410),
    skyBottom = Color(0xFF8A4A2C),
    ground = listOf(Color(0xFFC4643A), Color(0xFFAE5730), Color(0xFF984A28)),
    parent = ParentBody.PHOBOS,
    surface = Surface.DUNE,
    body = Color(0xFFEFE3D2),
    bodyDark = Color(0xFFC7B49C),
    trim = Color(0xFFE0763F),
    glass = Color(0xFFF2C6A0),
    light = Color(0xFFFFD764),
    moduleNames = listOf("Dome", "Array", "Mast", "Lab", "Pad", "Tank")
)
