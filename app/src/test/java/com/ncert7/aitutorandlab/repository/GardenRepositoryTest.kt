package com.ncert7.aitutorandlab.repository

import com.ncert7.aitutorandlab.data.local.dao.GardenDao
import com.ncert7.aitutorandlab.data.local.entities.GardenStateEntity
import com.ncert7.aitutorandlab.data.local.entities.GrownItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GardenRepositoryTest {

    private lateinit var dao: FakeGardenDao
    private lateinit var repository: GardenRepository

    @Before
    fun setUp() {
        dao = FakeGardenDao()
        repository = GardenRepository(dao)
    }

    @Test
    fun recordStep_incrementsUntilSevenThenPlants() = runBlocking {
        repeat(6) { step ->
            val planted =
                repository.recordStep(
                    studentId = STUDENT,
                    trialItemId = 100L + step,
                    conceptId = "c-$step",
                    chapterId = "ch-1",
                    kind = "STUDY",
                )
            assertNull(planted)
            assertEquals(step + 1, dao.state.steps)
        }

        val planted =
            repository.recordStep(
                studentId = STUDENT,
                trialItemId = 106L,
                conceptId = "c-final",
                chapterId = "ch-1",
                kind = "STUDY",
            )
        assertNotNull(planted)
        assertEquals(0, dao.state.steps)
        assertEquals(1, dao.items.size)
        assertEquals(106L.toString(), planted!!.id)
        assertEquals(0, planted.plot)
        assertTrue(planted.slot in 0..5)
    }

    @Test
    fun recordStep_isIdempotentForSameTrialItem() = runBlocking {
        repeat(7) {
            repository.recordStep(STUDENT, 42L, "c", "ch", "STUDY")
        }
        val again = repository.recordStep(STUDENT, 42L, "c", "ch", "STUDY")
        assertNull(again)
        assertEquals(1, dao.items.size)
        assertEquals(1, dao.state.steps)
    }

    @Test
    fun recordStep_afterPlantCreditsNextStepForSameTrialItem() = runBlocking {
        repeat(6) {
            repository.recordStep(STUDENT, 100L + it, "c", "ch", "STUDY")
        }
        repository.recordStep(STUDENT, 106L, "c", "ch", "STUDY")
        assertEquals(0, dao.state.steps)
        repository.recordStep(STUDENT, 106L, "c", "ch", "STUDY")
        assertEquals(1, dao.state.steps)
    }

    @Test
    fun recordCompletion_plantsImmediatelyOncePerConceptKind() = runBlocking {
        val first =
            repository.recordCompletion(
                studentId = STUDENT,
                conceptId = "c-sim",
                chapterId = "ch-1",
                kind = "SIMULATION",
            )
        assertNotNull(first)
        assertEquals("task:c-sim:SIMULATION", first!!.id)
        assertEquals(1, dao.items.size)
        assertEquals(0, dao.state.steps)

        val replay =
            repository.recordCompletion(
                studentId = STUDENT,
                conceptId = "c-sim",
                chapterId = "ch-1",
                kind = "simulation", // case-insensitive key
            )
        assertNull(replay)
        assertEquals(1, dao.items.size)

        val otherKind =
            repository.recordCompletion(
                studentId = STUDENT,
                conceptId = "c-sim",
                chapterId = "ch-1",
                kind = "REVISION",
            )
        assertNotNull(otherKind)
        assertEquals(2, dao.items.size)
        assertEquals("task:c-sim:REVISION", otherKind!!.id)
    }

    @Test
    fun nextFreePlot_skipsGaps() = runBlocking {
        dao.items.add(
            GrownItemEntity(
                id = "1",
                studentId = STUDENT,
                zone = 0,
                plot = 0,
                slot = 2,
                conceptId = "a",
                chapterId = "ch",
                kind = "STUDY",
                completedAt = 0L,
            ),
        )
        dao.items.add(
            GrownItemEntity(
                id = "2",
                studentId = STUDENT,
                zone = 0,
                plot = 11,
                slot = 3,
                conceptId = "b",
                chapterId = "ch",
                kind = "STUDY",
                completedAt = 0L,
            ),
        )
        dao.state = GardenStateEntity(studentId = STUDENT, steps = 6)

        val planted = repository.recordStep(STUDENT, 99L, "c", "ch", "STUDY")
        assertNotNull(planted)
        assertEquals(1, planted!!.plot)
    }

    @Test
    fun getProgress_returnsSnapshot() = runBlocking {
        repeat(3) {
            repository.recordStep(STUDENT, 200L + it, "c", "ch", "SIM_AGENT")
        }
        val progress = repository.getProgress(STUDENT)
        assertNotNull(progress)
        assertEquals(3, progress!!.steps)
        assertEquals(0, progress.totalPlanted)
        assertEquals(com.anurag.eduai.uikit.garden.quest.STARTER_GARDEN_ZONE, progress.currentZone)
    }

    @Test
    fun recordStep_advancesToNextZoneWhenCurrentPlaceFull() = runBlocking {
        var trialId = 300L
        repeat(12) {
            repeat(6) {
                repository.recordStep(STUDENT, trialId++, "c", "ch", "STUDY")
            }
            val planted = repository.recordStep(STUDENT, trialId++, "c", "ch", "STUDY")
            assertNotNull(planted)
            assertEquals(0, planted!!.zone)
        }
        assertEquals(12, dao.items.size)
        assertEquals("0", dao.state.route)

        repeat(6) {
            repository.recordStep(STUDENT, trialId++, "c", "ch", "STUDY")
        }
        val planted =
            repository.recordStep(
                studentId = STUDENT,
                trialItemId = trialId,
                conceptId = "c-next-zone",
                chapterId = "ch-2",
                kind = "STUDY",
            )
        assertNotNull(planted)
        assertEquals(13, dao.items.size)
        assertEquals("0,1", dao.state.route)
        assertEquals(1, planted!!.zone)
        assertEquals(0, planted.plot)
    }

    private class FakeGardenDao : GardenDao {
        var state: GardenStateEntity = GardenStateEntity(studentId = STUDENT)
            set(value) {
                field = value
                stateFlow.value = value
            }
        val items = mutableListOf<GrownItemEntity>()
        private val stateFlow = MutableStateFlow(state)

        override suspend fun getState(studentId: String): GardenStateEntity? =
            if (state.studentId == studentId) state else null

        override fun observeState(studentId: String): Flow<GardenStateEntity?> =
            stateFlow.map { current ->
                if (current.studentId == studentId) current else null
            }

        override suspend fun upsertState(state: GardenStateEntity) {
            this.state = state
        }

        override suspend fun updateSteps(studentId: String, steps: Int) {
            if (state.studentId == studentId) state = state.copy(steps = steps)
        }

        override suspend fun updatePreferredSlot(studentId: String, slot: Int) {
            if (state.studentId == studentId) state = state.copy(preferredSlot = slot)
        }

        override suspend fun updateRoute(studentId: String, route: String) {
            if (state.studentId == studentId) state = state.copy(route = route)
        }

        override suspend fun updateTheme(studentId: String, theme: String) {
            if (state.studentId == studentId) state = state.copy(theme = theme)
        }

        override suspend fun getAllItems(studentId: String): List<GrownItemEntity> =
            items.filter { it.studentId == studentId }.sortedBy { it.completedAt }

        override suspend fun getItem(id: String): GrownItemEntity? = items.find { it.id == id }

        override suspend fun getItemsInZone(studentId: String, zone: Int): List<GrownItemEntity> =
            items.filter { it.studentId == studentId && it.zone == zone }

        override suspend fun countItems(studentId: String): Int =
            items.count { it.studentId == studentId }

        override suspend fun getLatestItem(studentId: String): GrownItemEntity? =
            items.filter { it.studentId == studentId }.maxByOrNull { it.completedAt }

        override suspend fun insertItem(item: GrownItemEntity) {
            items.add(item)
        }
    }

    companion object {
        private const val STUDENT = "student-test"
    }
}
