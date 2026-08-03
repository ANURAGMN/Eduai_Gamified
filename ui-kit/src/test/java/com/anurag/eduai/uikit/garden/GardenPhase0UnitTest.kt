package com.anurag.eduai.uikit.garden

import com.anurag.eduai.uikit.garden.quest.SLOTS_PER_ZONE
import com.anurag.eduai.uikit.garden.quest.STEPS_PER_TASK
import com.anurag.eduai.uikit.garden.quest.Theme
import com.anurag.eduai.uikit.garden.quest.ThemeCopy
import com.anurag.eduai.uikit.garden.quest.ZONE_CAPACITY
import com.anurag.eduai.uikit.garden.quest.ZONES
import com.anurag.eduai.uikit.garden.quest.placeBased
import com.anurag.eduai.uikit.garden.world.Habitats
import com.anurag.eduai.uikit.garden.world.ISLAND_CELLS
import com.anurag.eduai.uikit.garden.world.PlaceState
import com.anurag.eduai.uikit.garden.world.Plant
import com.anurag.eduai.uikit.garden.world.PLOTS
import com.anurag.eduai.uikit.garden.world.SCENE_W
import com.anurag.eduai.uikit.garden.world.SpeciesTable
import com.anurag.eduai.uikit.garden.world.TOTAL_TIER_TASKS
import com.anurag.eduai.uikit.garden.world.TaskKind
import com.anurag.eduai.uikit.garden.world.WORLDS
import com.anurag.eduai.uikit.garden.world.hash
import com.anurag.eduai.uikit.garden.world.shade
import com.anurag.eduai.uikit.garden.world.swayDegrees
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.graphics.Color
import java.io.File
import kotlin.math.abs

/**
 * Phase 0 verification: drawing library ported into :ui-kit with bug fixes intact.
 * No persistence, no Eduapp hooks — compile-time and pure-logic checks only.
 */
class GardenPhase0UnitTest {

    // --- Port inventory ---

    @Test
    fun portInventory_expectedKotlinFilesPresent() {
        val root =
            File("src/main/java/com/anurag/eduai/uikit/garden").let { relative ->
                val fromModule = File("ui-kit", relative.path)
                if (fromModule.exists()) fromModule else relative
            }
        assertTrue("garden source root missing: $root", root.exists())
        val ktFiles = root.walkTopDown().filter { it.extension == "kt" }.map { it.name }.toSet()
        val expectedWorld =
            setOf(
                "Species.kt",
                "Plants.kt",
                "Habitats.kt",
                "Terrain.kt",
                "Ambient.kt",
                "OutpostModel.kt",
                "Modules.kt",
                "OutpostScene.kt",
                "IslandModel.kt",
                "IslandScene.kt",
                "TiersModel.kt",
                "TiersScene.kt",
                "Util.kt",
                "GardenState.kt",
                "GardenScene.kt",
            )
        val expectedQuest = setOf("ThemeCopy.kt", "Confetti.kt", "QuestZones.kt")
        expectedWorld.forEach { assertTrue("Missing world/$it", ktFiles.contains(it)) }
        expectedQuest.forEach { assertTrue("Missing quest/$it", ktFiles.contains(it)) }
        assertFalse("Wrapper screens must not be ported", ktFiles.any { it.endsWith("Screen.kt") })
    }

    // --- Util.kt (BUGS #3 clock fix lives in rememberSceneTime source) ---

    @Test
    fun hash_isDeterministicAndInUnitRange() {
        repeat(20) { i ->
            val a = hash(i, 7)
            val b = hash(i, 7)
            assertEquals(a, b, 0f)
            assertTrue(a in 0f..1f)
        }
        assertNotEquals(hash(0, 0), hash(1, 0))
    }

    @Test
    fun shade_lightensAndDarkens() {
        val base = Color(0xFF808080)
        val lighter = shade(base, 0.5f)
        val darker = shade(base, -0.5f)
        assertTrue(lighter.red > base.red)
        assertTrue(darker.red < base.red)
    }

    @Test
    fun swayDegrees_staysWithinReasonableAmplitude() {
        val samples =
            (0..120).map { t ->
                swayDegrees(t / 10f, phase = 2.5f, amount = 1f)
            }
        assertTrue(samples.max() < 12f)
        assertTrue(samples.min() > -12f)
        assertNotEquals(0f, samples.max())
    }

    @Test
    fun rememberSceneTime_anchorsOriginInSource() {
        val utilSource =
            readGardenSource("world/Util.kt")
        assertTrue(
            "BUGS #3: scene clock must anchor on first frame, not raw uptime",
            utilSource.contains("var origin = 0L") &&
                utilSource.contains("(nanos - origin)"),
        )
    }

    // --- Catalogue sizes ---

    @Test
    fun speciesTable_has48Entries() {
        assertEquals(48, SpeciesTable.count)
    }

    @Test
    fun habitats_andWorlds_eightPlacesEach() {
        assertEquals(8, Habitats.all.size)
        assertEquals(8, WORLDS.size)
        assertEquals(8, ZONES.size)
    }

    @Test
    fun plotGrid_andIsland_andColony_constants() {
        assertEquals(12, PLOTS.size)
        assertEquals(12, ZONE_CAPACITY)
        assertEquals(6, SLOTS_PER_ZONE)
        assertEquals(7, STEPS_PER_TASK)
        assertEquals(61, ISLAND_CELLS.size)
        assertEquals(96, TOTAL_TIER_TASKS)
        assertEquals(332f, SCENE_W)
    }

    // --- Theme / copy ---

    @Test
    fun themeCopy_allFourThemesHaveRequiredStrings() {
        Theme.entries.forEach { theme ->
            val copy = ThemeCopy.of(theme)
            assertTrue(copy.placeCollection.isNotBlank())
            assertTrue(copy.item.isNotBlank())
            assertTrue(copy.startShort.isNotBlank())
            assertTrue(copy.finishShort.isNotBlank())
            if (theme.placeBased) {
                assertTrue(copy.pickerTitle.isNotBlank())
            }
        }
    }

    @Test
    fun theme_placeBased_onlyGardenAndOutpost() {
        assertTrue(Theme.GARDEN.placeBased)
        assertTrue(Theme.OUTPOST.placeBased)
        assertFalse(Theme.ISLAND.placeBased)
        assertFalse(Theme.COLONY.placeBased)
    }

    @Test
    fun zone_slotNames_differByTheme_sameSlotIndex() {
        val zone = ZONES[0]
        val gardenName = zone.slotName(Theme.GARDEN, 0)
        val spaceName = zone.slotName(Theme.OUTPOST, 0)
        assertNotEquals(gardenName, spaceName)
    }

    // --- BUGS #12: firstFreePlot not count-as-index ---

    @Test
    fun firstFreePlot_returnsLowestHole_notPlantCount() {
        val place = PlaceState(Habitats.all[0])
        place.plants.add(
            Plant(plot = 0, species = "sunflower", kind = TaskKind.STUDY, concept = "Test"),
        )
        place.plants.add(
            Plant(plot = 11, species = "daisy", kind = TaskKind.SIM, concept = "Test"),
        )
        assertEquals(1, place.firstFreePlot())
        assertEquals(2, place.plants.size)
    }

    // --- BUGS terrain fixes present in source ---

    @Test
    fun beachTerrain_seaStopsBeforeBackPlots() {
        val terrain = readGardenSource("world/Terrain.kt")
        // Sea rect height 40 from y=96 → ends at 136; back plots at y≈152
        assertTrue(terrain.contains("Offset(-6f, 96f), size = Size(SCENE_W + 12f, 40f)"))
    }

    @Test
    fun islandTerrain_landmassBezierFixPresent() {
        val terrain = readGardenSource("world/Terrain.kt")
        assertTrue(terrain.contains("quadraticBezierTo(166f, 104f, 272f, 136f)"))
    }

    // --- BUGS #14–17: cover/focus in draw, not aspectRatio hacks ---

    @Test
    fun islandAndColonyScenes_haveCoverAndFocusSpan() {
        val island = readGardenSource("world/IslandScene.kt")
        val tiers = readGardenSource("world/TiersScene.kt")
        listOf(island, tiers).forEach { src ->
            assertTrue(src.contains("focusSpan: Float"))
            assertTrue(src.contains("cover: Boolean"))
            assertTrue(src.contains("maxOf(size.width"))
        }
    }

    @Test
    fun islandScene_coverModeSkipsMisalignedHitTest() {
        val island = readGardenSource("world/IslandScene.kt")
        assertTrue(
            "BUGS #20: cover thumbnails must not use fit-scale hit test",
            island.contains("if (cover)") && island.contains("pointerInput"),
        )
    }

    @Test
    fun drawRoundRectCompat_existsForMinSdkCompat() {
        val plants = readGardenSource("world/Plants.kt")
        assertTrue(plants.contains("fun DrawScope.drawRoundRectCompat"))
    }

    private fun readGardenSource(relativePath: String): String {
        val candidates =
            listOf(
                File("ui-kit/src/main/java/com/anurag/eduai/uikit/garden/$relativePath"),
                File("src/main/java/com/anurag/eduai/uikit/garden/$relativePath"),
            )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("Could not find garden source: $relativePath")
        return file.readText()
    }
}
