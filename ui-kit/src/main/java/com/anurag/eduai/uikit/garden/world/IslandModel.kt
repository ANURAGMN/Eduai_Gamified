package com.anurag.eduai.uikit.garden.world

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.atan2

const val ISLAND_W = 332f
const val ISLAND_H = 286f

const val TILE_W = 30f
const val TILE_H = 15f
const val TILE_LIFT = 11f
const val ISO_CX = 166f
const val ISO_CY = 138f

data class Region(val name: String, val top: Color, val wall: Color, val wallDark: Color)

val REGIONS = listOf(
    Region("The peak", Color(0xFFF4FAFF), Color(0xFFC4D7E6), Color(0xFFA9C0D2)),
    Region("Highland", Color(0xFFA9C6A9), Color(0xFF7E9A82), Color(0xFF6A8570)),
    Region("The woods", Color(0xFF2F8A5F), Color(0xFF206A4A), Color(0xFF18563C)),
    Region("The fields", Color(0xFF63C489), Color(0xFF45A16B), Color(0xFF398A5A)),
    Region("Shore meadow", Color(0xFF8ED9AC), Color(0xFF68B78A), Color(0xFF579F77)),
    Region("The beach", Color(0xFFF2E1BB), Color(0xFFD6BE93), Color(0xFFBFA67B))
)

data class Cell(
    val index: Int,
    val r: Int,
    val c: Int,
    val ring: Int,
    val elevation: Float,
    val hasFeature: Boolean,
    val variant: Float
) {
    val x: Float get() = ISO_CX + (c - r) * TILE_W / 2f
    val y: Float get() = ISO_CY + (c + r) * TILE_H / 2f - elevation * TILE_LIFT
    val region: Region get() = REGIONS[ring]
    /** Painter's order: everything on a diagonal shares a base line. */
    val depth: Int get() = r + c
}

enum class LandmarkKind { CAIRN, CABIN, WINDMILL, WELL, LIGHTHOUSE, JETTY }

data class Landmark(val cellIndex: Int, val name: String, val kind: LandmarkKind)

val LANDMARKS = listOf(
    Landmark(3, "Summit cairn", LandmarkKind.CAIRN),
    Landmark(9, "Woodcutter's cabin", LandmarkKind.CABIN),
    Landmark(20, "Windmill", LandmarkKind.WINDMILL),
    Landmark(34, "Village well", LandmarkKind.WELL),
    Landmark(50, "Lighthouse", LandmarkKind.LIGHTHOUSE),
    Landmark(57, "Jetty", LandmarkKind.JETTY)
)

/** 61 cells in an isometric diamond, revealed from the peak outward. */
val ISLAND_CELLS: List<Cell> = buildList {
    val raw = mutableListOf<Triple<Int, Int, Int>>()
    for (r in -5..5) for (c in -5..5) {
        val ring = abs(r) + abs(c)
        if (ring <= 5) raw.add(Triple(r, c, ring))
    }
    raw.sortWith(compareBy({ it.third }, { atan2(it.first.toFloat(), it.second.toFloat()) }))
    raw.forEachIndexed { i, (r, c, ring) ->
        add(
            Cell(
                index = i,
                r = r,
                c = c,
                ring = ring,
                elevation = (5 - ring) * 0.62f,
                hasFeature = hash(i, 3) > 0.30f,
                variant = hash(i, 7)
            )
        )
    }
}

val LANDMARK_BY_CELL: Map<Int, Landmark> = LANDMARKS.associateBy { it.cellIndex }

@Stable
class IslandState {
    /** Days finished. One tile per day. */
    var day by mutableIntStateOf(34)

    /** What the timeline is currently showing. */
    var view by mutableIntStateOf(34)

    var selected by mutableIntStateOf(-1)

    /**
     * How far along the tile currently being earned is, 0..1. The quest loop pushes one seventh per
     * step, so the next tile visibly lowers into place across a task instead of popping in at the end.
     */
    var partial by mutableFloatStateOf(0f)

    val total: Int get() = ISLAND_CELLS.size
    val viewingPast: Boolean get() = view < day

    fun addDay() {
        if (day < total) {
            day++
            view = day
            selected = -1
        }
    }

    fun regionProgress(ringIndex: Int): Pair<Int, Int> {
        val cells = ISLAND_CELLS.filter { it.ring == ringIndex }
        return cells.count { it.index < view } to cells.size
    }

    fun landmarksBuilt(): Int = LANDMARKS.count { it.cellIndex < view }
}
