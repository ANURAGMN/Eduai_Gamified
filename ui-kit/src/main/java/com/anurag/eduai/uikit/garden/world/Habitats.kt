package com.anurag.eduai.uikit.garden.world

import androidx.compose.ui.graphics.Color

/** Scene is authored in a fixed 332 x 352 space and scaled to the view. */
const val SCENE_W = 332f
const val SCENE_H = 352f

data class Plot(val x: Float, val y: Float, val scale: Float)

val PLOTS: List<Plot> = buildList {
    listOf(
        Triple(152f, 0.50f, listOf(46f, 116f, 194f, 268f)),
        Triple(218f, 0.68f, listOf(34f, 110f, 194f, 280f)),
        Triple(302f, 0.92f, listOf(46f, 126f, 208f, 288f))
    ).forEach { (y, s, xs) -> xs.forEach { add(Plot(it, y, s)) } }
}

/** Six edge slots for decorations, so ornament never competes with a plot. */
val DECOR_SLOTS: List<Plot> = listOf(
    Plot(26f, 334f, 0.95f),
    Plot(302f, 326f, 0.95f),
    Plot(16f, 250f, 0.70f),
    Plot(314f, 242f, 0.70f),
    Plot(120f, 346f, 1.00f),
    Plot(224f, 342f, 1.00f)
)

enum class Ambience { MEADOW, WOODS, WETLAND, BEACH, ISLAND, DESERT, HIGHLAND, ICE }

data class Habitat(
    val key: String,
    val name: String,
    val chapter: String,
    val skyTop: Color,
    val skyBottom: Color,
    val far: List<Color>,
    val ground: List<Color>,
    val sunCore: Color,
    val sunGlow: Color,
    val speckle: List<Color>,
    val species: List<String>,
    val ambience: Ambience,
    val pond: Boolean = false
)

object Habitats {
    val all = listOf(
        Habitat(
            key = "meadow",
            name = "Meadow",
            chapter = "Nutrition in plants",
            skyTop = Color(0xFFB8E2F7), skyBottom = Color(0xFFEAF7E6),
            far = listOf(Color(0xFFA6DCB6), Color(0xFF88CF9C), Color(0xFF6FC28C)),
            ground = listOf(Color(0xFF5FC788), Color(0xFF74D298), Color(0xFF8ADBAA)),
            sunCore = Color(0xFFFFD764), sunGlow = Color(0xFFFFE9A8),
            speckle = listOf(Color.White, Color(0xFFFFE07A), Color(0xFFFF9EC1), Color(0xFFC9C2FF)),
            species = listOf("sunflower", "daisy", "poppy", "tulip", "rose", "lavender"),
            ambience = Ambience.MEADOW
        ),
        Habitat(
            key = "woodland",
            name = "Woodland",
            chapter = "Nutrition in animals",
            skyTop = Color(0xFFCDE7E0), skyBottom = Color(0xFFE6F2E0),
            far = listOf(Color(0xFF7FB394), Color(0xFF5C9B77), Color(0xFF3F7F5C)),
            ground = listOf(Color(0xFF4E9C6B), Color(0xFF5DAA79), Color(0xFF6DB587)),
            sunCore = Color(0xFFFFE9A8), sunGlow = Color(0xFFFFF3CE),
            speckle = listOf(Color.White, Color(0xFFFFD98A), Color(0xFFC9A3E8), Color(0xFF9FE0B8)),
            species = listOf("oak", "birch", "blossom", "fern", "bluebell", "toadstool"),
            ambience = Ambience.WOODS
        ),
        Habitat(
            key = "wetland",
            name = "Wetland",
            chapter = "Respiration in organisms",
            skyTop = Color(0xFFBEE6EE), skyBottom = Color(0xFFE6F5F0),
            far = listOf(Color(0xFF9FD6D0), Color(0xFF82C6BF), Color(0xFF6FBBA5)),
            ground = listOf(Color(0xFF63BE8C), Color(0xFF74C99B), Color(0xFF86D3AB)),
            sunCore = Color(0xFFFFE79C), sunGlow = Color(0xFFFFF4D2),
            speckle = listOf(Color.White, Color(0xFFFFE07A), Color(0xFFFFB0D2), Color(0xFFB7E8F2)),
            species = listOf("lotus", "waterlily", "cattail", "iris", "papyrus", "marshmarigold"),
            ambience = Ambience.WETLAND,
            pond = true
        ),
        Habitat(
            key = "beach",
            name = "Beach",
            chapter = "Winds, storms and cyclones",
            skyTop = Color(0xFFA8DDF5), skyBottom = Color(0xFFEAF7FB),
            far = listOf(Color(0xFF7FCBE0), Color(0xFF5FB9D4), Color(0xFF3FA6C6)),
            ground = listOf(Color(0xFFF4E4BE), Color(0xFFEDD9A9), Color(0xFFE4CC96)),
            sunCore = Color(0xFFFFD764), sunGlow = Color(0xFFFFECB8),
            speckle = listOf(Color.White, Color(0xFFFFD98A), Color(0xFFFFAEC8), Color(0xFFBFE9F5)),
            species = listOf("marram", "seaholly", "beachrose", "sealavender", "morningglory", "dunedaisy"),
            ambience = Ambience.BEACH
        ),
        Habitat(
            key = "island",
            name = "Island",
            chapter = "Light",
            skyTop = Color(0xFF9FDAF2), skyBottom = Color(0xFFE4F6FB),
            far = listOf(Color(0xFF5FBFD8), Color(0xFF43ACC8), Color(0xFF2E9FC4)),
            ground = listOf(Color(0xFFF2E1BB), Color(0xFF5FC788), Color(0xFF74D298)),
            sunCore = Color(0xFFFFD05A), sunGlow = Color(0xFFFFE7A4),
            speckle = listOf(Color.White, Color(0xFFFFD05A), Color(0xFFFF8FB8), Color(0xFFA9E7F2)),
            species = listOf("palm", "hibiscus", "banana", "monstera", "orchid", "seagrape"),
            ambience = Ambience.ISLAND
        ),
        Habitat(
            key = "desert",
            name = "Desert",
            chapter = "Heat",
            skyTop = Color(0xFFFFD79B), skyBottom = Color(0xFFFFF3DE),
            far = listOf(Color(0xFFE0A870), Color(0xFFCE9159), Color(0xFFB87C48)),
            ground = listOf(Color(0xFFF2DCA8), Color(0xFFE9CE92), Color(0xFFDFC07D)),
            sunCore = Color(0xFFFF9F3D), sunGlow = Color(0xFFFFD08A),
            speckle = listOf(Color.White, Color(0xFFFFC864), Color(0xFFFF9E7A), Color(0xFFE8D3A0)),
            species = listOf("saguaro", "pricklypear", "aloe", "datepalm", "desertmarigold", "agave"),
            ambience = Ambience.DESERT
        ),
        Habitat(
            key = "highland",
            name = "Highland",
            chapter = "Physical and chemical changes",
            skyTop = Color(0xFFB9D6EC), skyBottom = Color(0xFFE9F2F6),
            far = listOf(Color(0xFFA9BFD2), Color(0xFF8FA9C0), Color(0xFF7392AC)),
            ground = listOf(Color(0xFF8FB79B), Color(0xFFA0C4A8), Color(0xFFB0D0B5)),
            sunCore = Color(0xFFFFEEC0), sunGlow = Color(0xFFFFF7E2),
            speckle = listOf(Color.White, Color(0xFFFFE7A8), Color(0xFFD8C4F0), Color(0xFFBEDCC8)),
            species = listOf("pine", "edelweiss", "heather", "gentian", "snowdrop", "juniper"),
            ambience = Ambience.HIGHLAND
        ),
        Habitat(
            key = "icepeak",
            name = "Ice peak",
            chapter = "Acids, bases and salts",
            skyTop = Color(0xFFA9CDEC), skyBottom = Color(0xFFE8F3FB),
            far = listOf(Color(0xFFBDD6EC), Color(0xFFA6C6E2), Color(0xFF8FB4D8)),
            ground = listOf(Color(0xFFDAE9F5), Color(0xFFE8F2FA), Color(0xFFF4F9FD)),
            sunCore = Color(0xFFFFF0D0), sunGlow = Color(0xFFFFFAF0),
            speckle = listOf(Color.White, Color(0xFFDCEBFA), Color(0xFFC8E0F5), Color.White),
            species = listOf("frostfir", "snowcrocus", "icemoss", "dwarfwillow", "lichen", "snowbell"),
            ambience = Ambience.ICE
        )
    )
}

enum class DecorKind(val label: String) {
    FENCE("Fence"), BENCH("Bench"), LANTERN("Lantern"), STONES("Stepping stones")
}

enum class TaskKind(val label: String, val tint: Color) {
    SIM("Simulation", Color(0xFF7C5CFF)),
    STUDY("Study", Color(0xFF12B5A6)),
    REVISION("Revision", Color(0xFFEF9F27))
}

/** Concepts per chapter — placeholder until the real NCERT list is wired in. */
val CONCEPTS: Map<String, List<String>> = mapOf(
    "meadow" to listOf("Photosynthesis", "Modes of nutrition", "Parasites", "Saprotrophs", "Replenishing nutrients"),
    "woodland" to listOf("Digestion in humans", "Teeth and tongue", "Nutrition in amoeba", "Ruminants", "Absorption"),
    "wetland" to listOf("Breathing", "Aerobic and anaerobic", "Breathing in animals", "Respiration in plants", "Cellular respiration"),
    "beach" to listOf("Air pressure", "High speed winds", "Thunderstorms", "Cyclones", "Staying safe"),
    "island" to listOf("Reflection", "Plane mirrors", "Spherical mirrors", "Lenses", "Sunlight and colours"),
    "desert" to listOf("Hot and cold", "Thermometers", "Conduction", "Convection", "Radiation"),
    "highland" to listOf("Physical changes", "Chemical changes", "Rusting", "Crystallisation", "Galvanisation"),
    "icepeak" to listOf("Natural indicators", "Neutralisation", "Litmus", "Acids around us", "Bases around us")
)

/** Cumulative visitors — the compounding engine. Thresholds count every plant, everywhere. */
data class Visitor(val threshold: Int, val label: String)

val VISITORS = listOf(
    Visitor(6, "Bees"),
    Visitor(14, "Butterflies"),
    Visitor(24, "Songbirds"),
    Visitor(30, "A rabbit")
)
