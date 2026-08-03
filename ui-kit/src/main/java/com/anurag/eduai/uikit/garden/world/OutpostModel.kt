package com.anurag.eduai.uikit.garden.world

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class ModuleKind { DOME, ARRAY, MAST, LAB, PAD, TANK }

enum class ParentBody { EARTH, JUPITER, SATURN, PHOBOS, SUN, NONE }

enum class Surface { CRATER, DUNE, ICE, LAKE, CLOUD, LAVA, ROCK, GEYSER }

data class World(
    val key: String,
    val name: String,
    val chapter: String,
    val skyTop: Color,
    val skyBottom: Color,
    val ground: List<Color>,
    val parent: ParentBody,
    val surface: Surface,
    val body: Color,
    val bodyDark: Color,
    val trim: Color,
    val glass: Color,
    val light: Color,
    val moduleNames: List<String>
)

val WORLDS = listOf(
    World(
        "luna", "Luna", "Nutrition in plants",
        Color(0xFF060C1C), Color(0xFF16233C),
        listOf(Color(0xFF8C929E), Color(0xFF787E8A), Color(0xFF666C78)),
        ParentBody.EARTH, Surface.CRATER,
        Color(0xFFDCE3ED), Color(0xFFA7B0C0), Color(0xFF5FC7E8), Color(0xFF8FDCF2), Color(0xFFFFD764),
        listOf("Habitat dome", "Solar array", "Comms mast", "Regolith lab", "Landing pad", "Storage silo")
    ),
    World(
        "mars", "Mars", "Nutrition in animals",
        Color(0xFF2A1410), Color(0xFF6B3A26),
        listOf(Color(0xFFC4643A), Color(0xFFAE5730), Color(0xFF984A28)),
        ParentBody.PHOBOS, Surface.DUNE,
        Color(0xFFEFE3D2), Color(0xFFC7B49C), Color(0xFFE0763F), Color(0xFFF2C6A0), Color(0xFFFFD764),
        listOf("Biodome", "Solar array", "Weather mast", "Sample lab", "Rover garage", "Water tank")
    ),
    World(
        "europa", "Europa", "Respiration in organisms",
        Color(0xFF08182E), Color(0xFF1B3A5E),
        listOf(Color(0xFFCFE4F2), Color(0xFFB6D3E8), Color(0xFF9CC0DC)),
        ParentBody.JUPITER, Surface.ICE,
        Color(0xFFEAF4FC), Color(0xFFB4CBDE), Color(0xFF4FB9E0), Color(0xFF9EE0F5), Color(0xFF7FE3C8),
        listOf("Ice dome", "Thermal array", "Relay mast", "Drill rig", "Sub bay", "Cryo tank")
    ),
    World(
        "titan", "Titan", "Winds, storms and cyclones",
        Color(0xFF3A2408), Color(0xFF8A5A18),
        listOf(Color(0xFF8A6A32), Color(0xFF75592A), Color(0xFF614821)),
        ParentBody.SATURN, Surface.LAKE,
        Color(0xFFF2DFB4), Color(0xFFC9AF80), Color(0xFFE8A33C), Color(0xFFF7D48C), Color(0xFFFFE9A8),
        listOf("Haze dome", "Methane rig", "Balloon mast", "Chem lab", "Lake lander", "Fuel tank")
    ),
    World(
        "venus", "Venus", "Light",
        Color(0xFF4A2E06), Color(0xFFC98A1E),
        listOf(Color(0xFFE8B44A), Color(0xFFD9A238), Color(0xFFC68F2C)),
        ParentBody.NONE, Surface.CLOUD,
        Color(0xFFF7ECD2), Color(0xFFD2BC92), Color(0xFFC97B1E), Color(0xFFF7D48C), Color(0xFFFF9E7A),
        listOf("Cloud dome", "Reflector array", "Sky mast", "Acid lab", "Airship dock", "Ballast tank")
    ),
    World(
        "io", "Io", "Heat",
        Color(0xFF210A16), Color(0xFF5E1F2C),
        listOf(Color(0xFFD8C24A), Color(0xFFC2AC38), Color(0xFFA8932C)),
        ParentBody.JUPITER, Surface.LAVA,
        Color(0xFFF2EAC6), Color(0xFFC6B78A), Color(0xFFE2622F), Color(0xFFF7D48C), Color(0xFFFF6B4A),
        listOf("Shield dome", "Heat array", "Seismic mast", "Lava lab", "Ash pad", "Sulphur tank")
    ),
    World(
        "ceres", "Ceres", "Physical and chemical changes",
        Color(0xFF08101C), Color(0xFF1A2740),
        listOf(Color(0xFF6E6E76), Color(0xFF5E5E66), Color(0xFF4E4E56)),
        ParentBody.SUN, Surface.ROCK,
        Color(0xFFD6DAE2), Color(0xFF9CA2AE), Color(0xFF7FA8D8), Color(0xFFAECBE8), Color(0xFFFFD764),
        listOf("Rock dome", "Mirror array", "Beacon mast", "Ore lab", "Tug pad", "Ice hopper")
    ),
    World(
        "enceladus", "Enceladus", "Acids, bases and salts",
        Color(0xFF061426), Color(0xFF1C3A5C),
        listOf(Color(0xFFE8F2FA), Color(0xFFD2E3F0), Color(0xFFBCD2E4)),
        ParentBody.SATURN, Surface.GEYSER,
        Color(0xFFFFFFFF), Color(0xFFB8CCDE), Color(0xFF5FC7E8), Color(0xFFAEE4F5), Color(0xFF7FE3C8),
        listOf("Frost dome", "Plume array", "Signal mast", "Geyser lab", "Skiff pad", "Vapour tank")
    )
)

val OUTPOST_CONCEPTS: Map<String, List<String>> = mapOf(
    "luna" to listOf("Photosynthesis", "Modes of nutrition", "Parasites", "Saprotrophs", "Replenishing nutrients"),
    "mars" to listOf("Digestion in humans", "Teeth and tongue", "Nutrition in amoeba", "Ruminants", "Absorption"),
    "europa" to listOf("Breathing", "Aerobic and anaerobic", "Breathing in animals", "Respiration in plants", "Cellular respiration"),
    "titan" to listOf("Air pressure", "High speed winds", "Thunderstorms", "Cyclones", "Staying safe"),
    "venus" to listOf("Reflection", "Plane mirrors", "Spherical mirrors", "Lenses", "Sunlight and colours"),
    "io" to listOf("Hot and cold", "Thermometers", "Conduction", "Convection", "Radiation"),
    "ceres" to listOf("Physical changes", "Chemical changes", "Rusting", "Crystallisation", "Galvanisation"),
    "enceladus" to listOf("Natural indicators", "Neutralisation", "Litmus", "Acids around us", "Bases around us")
)

enum class SiteDecor(val label: String) {
    FLAG("Flag"), FLOODLIGHT("Floodlight"), CRATES("Supply crates"), BEACON("Beacon")
}

data class Module(val plot: Int, val kind: ModuleKind, val taskKind: TaskKind, val concept: String)
data class SiteItem(val slot: Int, val decor: SiteDecor)

data class SolEvent(val name: String, val detail: String)

val SOL_EVENTS = listOf(
    SolEvent("Meteor shower", "Bright streaks over the ridge all night."),
    SolEvent("Supply ship", "A resupply lander touched down before dawn."),
    SolEvent("Aurora", "Charged particles lit the sky up green."),
    SolEvent("Rover discovery", "Pip flagged an odd formation out east."),
    SolEvent("Crystal found", "The drill brought up something clear and heavy."),
    SolEvent("Ice cave", "A hollow opened under the north slope."),
    SolEvent("Satellite photo", "First full map of the base from orbit."),
    SolEvent("Greenhouse harvest", "The first crop came in today.")
)

val OUTPOST_VISITORS = listOf(
    Visitor(6, "A supply drone"),
    Visitor(14, "A satellite in orbit"),
    Visitor(24, "A second rover"),
    Visitor(30, "A passing comet")
)

val PIP_LINES = listOf(
    "Signal is clean from here.",
    "That dome catches the light well.",
    "I mapped the ridge while you were out.",
    "Nobody has stood here before you.",
    "I parked by the crates. Hope that is fine."
)

@Stable
class SiteState(val world: World) {
    val tasks: List<TaskState> = buildList {
        OUTPOST_CONCEPTS.getValue(world.key).forEach {
            add(TaskState(TaskKind.SIM, it))
            add(TaskState(TaskKind.STUDY, it))
        }
        add(TaskState(TaskKind.REVISION, "Halfway recap"))
        add(TaskState(TaskKind.REVISION, "Chapter recap"))
    }

    val modules = mutableStateListOf<Module>()
    val decor = mutableStateListOf<SiteItem>()

    val capacity: Int get() = PLOTS.size
    val complete: Boolean get() = modules.size >= capacity
    val tasksLeft: Int get() = tasks.count { !it.done }

    fun firstFreePlot(): Int {
        val used = modules.map { it.plot }.toSet()
        return (0 until capacity).firstOrNull { it !in used } ?: -1
    }

    fun activeTask(): Int {
        val started = tasks.indexOfFirst { it.steps in 1 until TaskState.STEPS_PER_TASK }
        if (started >= 0) return started
        return tasks.indexOfFirst { it.steps == 0 }
    }
}

@Stable
class OutpostState {
    val sites: List<SiteState> = WORLDS.map { SiteState(it) }

    var worldIndex by mutableIntStateOf(0)
    var mode by mutableStateOf(Mode.PLANT)
    var moduleKind by mutableStateOf(ModuleKind.DOME)
    var decorKind by mutableStateOf(SiteDecor.FLAG)
    var selectedPlot by mutableIntStateOf(-1)
    var movingFrom by mutableIntStateOf(-1)
    var pendingTask by mutableIntStateOf(-1)
    var autoPlace by mutableStateOf(true)
    var lastAutoPlot by mutableIntStateOf(-1)
    var baseName by mutableStateOf("Base Anurag")
    var pipLine by mutableIntStateOf(0)
    var sol by mutableIntStateOf(3)
    var darkChrome by mutableStateOf(true)

    val site: SiteState get() = sites[worldIndex]
    val world: World get() = sites[worldIndex].world

    val totalModules: Int get() = sites.sumOf { it.modules.size }
    val chaptersDone: Int get() = sites.count { it.complete }
    val event: SolEvent get() = SOL_EVENTS[sol % SOL_EVENTS.size]

    fun unlocked(index: Int): Boolean = index == 0 || sites[index - 1].complete

    fun step(taskIndex: Int) {
        val task = site.tasks[taskIndex]
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
            val plot = site.firstFreePlot()
            build(plot)
            lastAutoPlot = plot
        }
    }

    fun build(plot: Int) {
        val taskIndex = pendingTask
        if (taskIndex < 0 || plot < 0) return
        val task = site.tasks[taskIndex]
        task.steps = TaskState.STEPS_PER_TASK
        site.modules.add(Module(plot, moduleKind, task.kind, task.concept))
        pendingTask = -1
        selectedPlot = -1
    }

    fun relocateLast() {
        if (lastAutoPlot < 0) return
        mode = Mode.MOVE
        movingFrom = lastAutoPlot
        selectedPlot = -1
        lastAutoPlot = -1
    }

    fun moveModule(toPlot: Int) {
        val from = movingFrom
        if (from < 0) return
        val index = site.modules.indexOfFirst { it.plot == from }
        if (index >= 0) site.modules[index] = site.modules[index].copy(plot = toPlot)
        movingFrom = -1
    }

    fun addDecor(slot: Int) = site.decor.add(SiteItem(slot, decorKind))

    fun removeDecor(slot: Int) {
        val index = site.decor.indexOfFirst { it.slot == slot }
        if (index >= 0) site.decor.removeAt(index)
    }

    fun goTo(index: Int) {
        if (!unlocked(index)) return
        worldIndex = index
        selectedPlot = -1
        movingFrom = -1
        pendingTask = -1
        lastAutoPlot = -1
        mode = Mode.PLANT
        moduleKind = ModuleKind.DOME
        baseName = "Base on ${sites[index].world.name}"
    }

    fun completeChapter() {
        while (site.activeTask() >= 0 && site.firstFreePlot() >= 0) {
            val t = site.activeTask()
            site.tasks[t].steps = TaskState.STEPS_PER_TASK
            site.modules.add(
                Module(
                    site.firstFreePlot(),
                    ModuleKind.entries[site.modules.size % ModuleKind.entries.size],
                    site.tasks[t].kind,
                    site.tasks[t].concept
                )
            )
        }
    }

    fun nextSol() {
        sol++
        pipLine = (pipLine + 1) % PIP_LINES.size
    }

    fun seed() {
        val s = sites[0]
        val slots = listOf(0, 1, 2, 4, 5, 6, 8, 9)
        slots.forEachIndexed { i, slot ->
            s.tasks[i].steps = TaskState.STEPS_PER_TASK
            s.modules.add(
                Module(slot, ModuleKind.entries[i % ModuleKind.entries.size], s.tasks[i].kind, s.tasks[i].concept)
            )
        }
        s.tasks[8].steps = 3
        s.decor.add(SiteItem(0, SiteDecor.FLAG))
        s.decor.add(SiteItem(4, SiteDecor.CRATES))
        baseName = "Base Anurag"
    }
}
