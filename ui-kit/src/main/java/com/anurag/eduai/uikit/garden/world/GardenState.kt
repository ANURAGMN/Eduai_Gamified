package com.anurag.eduai.uikit.garden.world

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Mode { PLANT, MOVE, DECORATE }

@Stable
class TaskState(val kind: TaskKind, val concept: String) {
    var steps by mutableIntStateOf(0)
    val done: Boolean get() = steps >= STEPS_PER_TASK

    companion object {
        const val STEPS_PER_TASK = 7
    }
}

data class Plant(val plot: Int, val species: String, val kind: TaskKind, val concept: String)
data class Decor(val slot: Int, val kind: DecorKind)

@Stable
class PlaceState(val habitat: Habitat) {
    val tasks: List<TaskState> = buildList {
        val concepts = CONCEPTS.getValue(habitat.key)
        concepts.forEach {
            add(TaskState(TaskKind.SIM, it))
            add(TaskState(TaskKind.STUDY, it))
        }
        add(TaskState(TaskKind.REVISION, "Halfway recap"))
        add(TaskState(TaskKind.REVISION, "Chapter recap"))
    }

    val plants = mutableStateListOf<Plant>()
    val decor = mutableStateListOf<Decor>()

    val capacity: Int get() = PLOTS.size
    val complete: Boolean get() = plants.size >= capacity
    val tasksLeft: Int get() = tasks.count { !it.done }

    fun firstFreePlot(): Int {
        val used = plants.map { it.plot }.toSet()
        return (0 until capacity).firstOrNull { it !in used } ?: -1
    }

    /** Prefers a task already in progress, so the sprout in the scene is the one you touched last. */
    fun activeTask(): Int {
        val started = tasks.indexOfFirst { it.steps in 1 until TaskState.STEPS_PER_TASK }
        if (started >= 0) return started
        return tasks.indexOfFirst { it.steps == 0 }
    }
}

@Stable
class GardenState {
    val places: List<PlaceState> = Habitats.all.map { PlaceState(it) }

    var placeIndex by mutableIntStateOf(0)
    var mode by mutableStateOf(Mode.PLANT)
    var species by mutableStateOf(Habitats.all[0].species[0])
    var decorKind by mutableStateOf(DecorKind.FENCE)
    var selectedPlot by mutableIntStateOf(-1)
    var movingFrom by mutableIntStateOf(-1)
    var pendingTask by mutableIntStateOf(-1)
    var justPlanted by mutableIntStateOf(-1)
    var gardenName by mutableStateOf("My meadow")
    var sproutLine by mutableIntStateOf(0)

    /** Tab 1's model: auto by default, choosing a spot is one tap away rather than required. */
    var autoPlace by mutableStateOf(true)

    /** Set for a moment after an auto-place, so we can offer "put it somewhere else". */
    var lastAutoPlot by mutableIntStateOf(-1)

    val place: PlaceState get() = places[placeIndex]

    val totalPlants: Int get() = places.sumOf { it.plants.size }
    val chaptersDone: Int get() = places.count { it.complete }
    val heritageStage: Float get() = 0.2f + chaptersDone / places.size.toFloat() * 0.8f

    fun unlocked(index: Int): Boolean = index == 0 || places[index - 1].complete

    fun step(taskIndex: Int) {
        val task = place.tasks[taskIndex]
        if (task.done) return
        if (task.steps < TaskState.STEPS_PER_TASK - 1) {
            task.steps++
            return
        }
        task.steps = TaskState.STEPS_PER_TASK - 1
        pendingTask = taskIndex
        mode = Mode.PLANT
        selectedPlot = -1
        lastAutoPlot = -1
        if (autoPlace) {
            val plot = place.firstFreePlot()
            placeAt(plot)
            lastAutoPlot = plot
        }
    }

    fun placeAt(plot: Int) {
        val taskIndex = pendingTask
        if (taskIndex < 0 || plot < 0) return
        val task = place.tasks[taskIndex]
        task.steps = TaskState.STEPS_PER_TASK
        place.plants.add(Plant(plot, species, task.kind, task.concept))
        pendingTask = -1
        justPlanted = plot
        selectedPlot = -1
    }

    /** "Actually, put it somewhere else" — hands the just-planted item straight to move mode. */
    fun relocateLast() {
        if (lastAutoPlot < 0) return
        mode = Mode.MOVE
        movingFrom = lastAutoPlot
        selectedPlot = -1
        lastAutoPlot = -1
    }

    fun movePlant(toPlot: Int) {
        val from = movingFrom
        if (from < 0) return
        val index = place.plants.indexOfFirst { it.plot == from }
        if (index >= 0) place.plants[index] = place.plants[index].copy(plot = toPlot)
        movingFrom = -1
    }

    fun addDecor(slot: Int) {
        place.decor.add(Decor(slot, decorKind))
    }

    fun removeDecor(slot: Int) {
        val index = place.decor.indexOfFirst { it.slot == slot }
        if (index >= 0) place.decor.removeAt(index)
    }

    fun goTo(index: Int) {
        if (!unlocked(index)) return
        placeIndex = index
        selectedPlot = -1
        movingFrom = -1
        pendingTask = -1
        mode = Mode.PLANT
        species = places[index].habitat.species[0]
        gardenName = "My " + places[index].habitat.name.lowercase()
    }

    fun completeChapter() {
        while (place.activeTask() >= 0 && place.firstFreePlot() >= 0) {
            val t = place.activeTask()
            place.tasks[t].steps = TaskState.STEPS_PER_TASK
            val speciesList = place.habitat.species
            place.plants.add(
                Plant(
                    place.firstFreePlot(),
                    speciesList[place.plants.size % speciesList.size],
                    place.tasks[t].kind,
                    place.tasks[t].concept
                )
            )
        }
    }

    fun reset() {
        places.forEach { p ->
            p.plants.clear()
            p.decor.clear()
            p.tasks.forEach { it.steps = 0 }
        }
        placeIndex = 0
        pendingTask = -1
        selectedPlot = -1
        movingFrom = -1
        species = Habitats.all[0].species[0]
        gardenName = "My meadow"
    }

    /** Seed a partly-grown first chapter so the app does not open on an empty field. */
    fun seed() {
        val p = places[0]
        val slots = listOf(0, 1, 2, 4, 5, 6, 8, 9)
        slots.forEachIndexed { i, slot ->
            p.tasks[i].steps = TaskState.STEPS_PER_TASK
            p.plants.add(
                Plant(slot, p.habitat.species[i % p.habitat.species.size], p.tasks[i].kind, p.tasks[i].concept)
            )
        }
        p.tasks[8].steps = 3
        p.decor.add(Decor(0, DecorKind.FENCE))
        p.decor.add(Decor(4, DecorKind.BENCH))
    }
}

val SPROUT_LINES = listOf(
    "I like what you did with this corner.",
    "That one is my favourite so far.",
    "Take your time. It keeps growing.",
    "You planted this. Nobody else did.",
    "I moved the bench. Hope that is alright."
)
