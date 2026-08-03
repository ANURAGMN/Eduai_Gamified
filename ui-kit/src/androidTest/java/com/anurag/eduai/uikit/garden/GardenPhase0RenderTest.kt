package com.anurag.eduai.uikit.garden

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anurag.eduai.uikit.components.GardenRailState
import com.anurag.eduai.uikit.components.GrowRail
import com.anurag.eduai.uikit.garden.quest.ConfettiBurst
import com.anurag.eduai.uikit.garden.quest.Theme
import com.anurag.eduai.uikit.garden.world.GardenScene
import com.anurag.eduai.uikit.garden.world.GardenState
import com.anurag.eduai.uikit.garden.world.IslandScene
import com.anurag.eduai.uikit.garden.world.IslandState
import com.anurag.eduai.uikit.garden.world.OutpostScene
import com.anurag.eduai.uikit.garden.world.OutpostState
import com.anurag.eduai.uikit.garden.world.TiersScene
import com.anurag.eduai.uikit.garden.world.TiersState
import com.anurag.eduai.uikit.theme.EduAiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders every ported scene composable at the box sizes Eduapp will use.
 * Pass = no crash, recomposition settles (Canvas draw path exercised).
 *
 * Run on a physical device:
 * ```
 * ./gradlew :ui-kit:connectedDebugAndroidTest
 * ```
 */
@RunWith(AndroidJUnit4::class)
class GardenPhase0RenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun gardenScene_rendersAtWidth98() {
        renderGardenScene(98)
    }

    @Test
    fun gardenScene_rendersAtWidth180() {
        renderGardenScene(180)
    }

    @Test
    fun gardenScene_rendersAtWidth328() {
        renderGardenScene(328)
    }

    private fun renderGardenScene(widthDp: Int) {
        composeRule.setContent {
            GardenScene(
                state = GardenState(),
                time = 2.5f,
                modifier = Modifier.size(widthDp.dp),
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun outpostScene_rendersAtSceneSegmentWidth() {
        composeRule.setContent {
            OutpostScene(
                state = OutpostState(),
                time = 1.0f,
                modifier = Modifier.width(328.dp).height(280.dp),
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun islandScene_rendersCompactFocus() {
        val state =
            IslandState().apply {
                view = 5
                partial = 0.4f
            }
        composeRule.setContent {
            IslandScene(
                state = state,
                time = 3.0f,
                modifier = Modifier.size(98.dp),
                cover = false,
                focusSpan = 118f,
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun islandScene_rendersCoverBand() {
        val state =
            IslandState().apply {
                view = 5
                partial = 0.4f
            }
        composeRule.setContent {
            IslandScene(
                state = state,
                time = 3.0f,
                modifier = Modifier.width(328.dp).height(138.dp),
                cover = true,
                focusSpan = 0f,
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun colonyScene_rendersCompactFocus() {
        val state =
            TiersState().apply {
                applyTaskCount(3)
                partial = 0.5f
            }
        composeRule.setContent {
            TiersScene(
                state = state,
                time = 2.0f,
                modifier = Modifier.size(98.dp),
                cover = false,
                focusSpan = 118f,
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun colonyScene_rendersCoverBand() {
        val state =
            TiersState().apply {
                applyTaskCount(3)
                partial = 0.5f
            }
        composeRule.setContent {
            TiersScene(
                state = state,
                time = 2.0f,
                modifier = Modifier.width(328.dp).height(138.dp),
                cover = true,
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun confettiBurst_firesWithoutCrash() {
        composeRule.setContent {
            Box(Modifier.fillMaxSize()) {
                ConfettiBurst(trigger = 1, theme = Theme.GARDEN)
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1600)
        composeRule.waitForIdle()
    }

    @Test
    fun bigGrowingItem_rendersAtHomeRailArtSize() {
        composeRule.setContent {
            BigGrowingItem(
                theme = Theme.GARDEN,
                currentZone = 0,
                slot = 2,
                steps = 4,
                time = 1.5f,
                modifier = Modifier.size(104.dp),
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun growRail_rendersCompactOnPhoneWidth() {
        composeRule.setContent {
            EduAiTheme {
                GrowRail(
                    state =
                        GardenRailState(
                            sectionTitle = "Your garden",
                            openLabel = "Open",
                            conceptTitle = "Multiplication of integers",
                            subtitle = "Study · 4 of 7",
                            steps = 4,
                            stepsPerPlant = 7,
                            statusLine = "2/12 · meadow",
                            hintLine = "Keep learning to grow",
                            currentZone = 0,
                            slot = 2,
                        ),
                    modifier = Modifier.width(328.dp),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(2000)
        composeRule.waitForIdle()
    }
}
