package com.anurag.eduai.uikit.garden.world

import androidx.compose.ui.graphics.Color

enum class Arch { DISC, CUP, SPIKE, BUSH, TREE, ROSETTE, SHROOM, GRASS, BROAD, CUSHION, CACTUS }

enum class TreeForm { ROUND, TRI, FAN }

/**
 * A species is data, not a drawing. Adding one costs a line here —
 * this is the whole reason the renderer is procedural rather than sprite-based.
 */
data class Species(
    val key: String,
    val name: String,
    val arch: Arch,
    val c0: Color,
    val c1: Color,
    val c2: Color = c1,
    val height: Float = 70f,
    val petals: Int = 10,
    val radius: Float = 10f,
    val stems: Int = 3,
    val stalks: Int = 4,
    val cupWidth: Float = 7f,
    val cupHeight: Float = 16f,
    val bell: Boolean = false,
    val cylinder: Boolean = false,
    val frond: Boolean = false,
    val bloom: Boolean = false,
    val fronds: Int = 7,
    val frondLen: Float = 26f,
    val treeForm: TreeForm = TreeForm.ROUND,
    val trunk: Float = 36f,
    val canopy: Float = 20f,
    val dots: Boolean = false,
    val snowCap: Boolean = false,
    val notch: Boolean = false,
    val fruit: Boolean = false,
    val pear: Boolean = false,
    val bark: Color = Color(0xFF8A6E4F),
    val leafColor: Color? = null,
    val stemColor: Color? = null,
    val foliage: List<Color> = listOf(
        Color(0xFF2A7F58), Color(0xFF3FA878), Color(0xFF57BC8D)
    )
)

object SpeciesTable {

    private val all = listOf(
        // ---- meadow ----
        Species("sunflower", "Sunflower", Arch.DISC, Color(0xFFEF9F27), Color(0xFFFAC775), Color(0xFF854F0B), height = 74f, radius = 11f),
        Species("daisy", "Daisy", Arch.DISC, Color(0xFFFFFFFF), Color(0xFFF1EFE8), Color(0xFFFAC775), height = 50f, radius = 8f, petals = 12),
        Species("poppy", "Poppy", Arch.CUP, Color(0xFFE24B4A), Color(0xFFF09595), height = 56f, cupWidth = 8f, cupHeight = 13f),
        Species("tulip", "Tulips", Arch.CUP, Color(0xFFD4537E), Color(0xFFF2A8C4), height = 58f),
        Species("rose", "Rose bush", Arch.BUSH, Color(0xFFD4537E), Color(0xFFED93B1), radius = 19f),
        Species("lavender", "Lavender", Arch.SPIKE, Color(0xFF7F77DD), Color(0xFFAFA9EC), height = 56f),

        // ---- woodland ----
        Species("oak", "Little oak", Arch.TREE, Color(0xFF2E8C63), Color(0xFF3FA878), Color(0xFF4FB483), trunk = 38f, canopy = 21f),
        Species("birch", "Birch", Arch.TREE, Color(0xFF5DCAA5), Color(0xFF69B98A), Color(0xFF9FE1CB), trunk = 42f, canopy = 17f, bark = Color(0xFFDCD6C8)),
        Species("blossom", "Cherry blossom", Arch.TREE, Color(0xFFED93B1), Color(0xFFF2A8C4), Color(0xFFF7C0D5), trunk = 34f, canopy = 20f, dots = true),
        Species("fern", "Fern", Arch.ROSETTE, Color(0xFF2E8C63), Color(0xFF4FB483), frond = true, fronds = 7, frondLen = 28f),
        Species("bluebell", "Bluebells", Arch.CUP, Color(0xFF378ADD), Color(0xFF85B7EB), height = 44f, bell = true, cupWidth = 5f, cupHeight = 11f),
        Species("toadstool", "Toadstools", Arch.SHROOM, Color(0xFFE24B4A), Color(0xFFF09595)),

        // ---- wetland ----
        Species("lotus", "Lotus", Arch.ROSETTE, Color(0xFF3FA878), Color(0xFF4FB483), Color(0xFFF2A8C4), fronds = 9, frondLen = 20f, bloom = true),
        Species("waterlily", "Water lily", Arch.ROSETTE, Color(0xFF2E8C63), Color(0xFF3FA878), Color(0xFFFFFFFF), fronds = 9, frondLen = 18f, bloom = true),
        Species("cattail", "Cattails", Arch.SPIKE, Color(0xFF8A6E4F), Color(0xFF8A6E4F), height = 64f, cylinder = true, stemColor = Color(0xFF5DCAA5)),
        Species("iris", "Iris", Arch.CUP, Color(0xFF534AB7), Color(0xFFAFA9EC), height = 56f, stems = 2, cupWidth = 8f, cupHeight = 15f),
        Species("papyrus", "Papyrus", Arch.ROSETTE, Color(0xFF5DCAA5), Color(0xFF9FE1CB), Color(0xFFFAC775), frond = true, fronds = 9, frondLen = 24f, bloom = true),
        Species("marshmarigold", "Marsh marigold", Arch.DISC, Color(0xFFEF9F27), Color(0xFFFAC775), Color(0xFF854F0B), height = 42f, radius = 8f, petals = 11, leafColor = Color(0xFF3FA878)),

        // ---- beach ----
        Species("marram", "Marram grass", Arch.GRASS, Color(0xFF9BC48F), Color(0xFFB9D3A0), Color(0xFFE8DFC9), height = 54f, stalks = 9),
        Species("seaholly", "Sea holly", Arch.DISC, Color(0xFF85B7EB), Color(0xFFB5D4F4), Color(0xFF378ADD), height = 44f, radius = 8f, petals = 9, leafColor = Color(0xFF9FC3B4), stemColor = Color(0xFF8FB3A6)),
        Species("beachrose", "Beach rose", Arch.BUSH, Color(0xFFED93B1), Color(0xFFF4C0D1), radius = 16f, foliage = listOf(Color(0xFF3E8C67), Color(0xFF4FA57C), Color(0xFF6FBB93))),
        Species("sealavender", "Sea lavender", Arch.SPIKE, Color(0xFFAFA9EC), Color(0xFFCECBF6), height = 40f, stalks = 5, leafColor = Color(0xFF9FC3B4)),
        Species("morningglory", "Morning glory", Arch.CUP, Color(0xFF7F77DD), Color(0xFFCECBF6), height = 60f, cupWidth = 8f, cupHeight = 11f),
        Species("dunedaisy", "Dune daisy", Arch.DISC, Color(0xFFFAC775), Color(0xFFFFFFFF), Color(0xFFEF9F27), height = 40f, radius = 8f, petals = 11, leafColor = Color(0xFF9BC48F)),

        // ---- island ----
        Species("palm", "Coconut palm", Arch.TREE, Color(0xFF2E8C63), Color(0xFF3FA878), Color(0xFF8A6E4F), treeForm = TreeForm.FAN, trunk = 52f, canopy = 20f, bark = Color(0xFFA5825E), fruit = true),
        Species("hibiscus", "Hibiscus", Arch.DISC, Color(0xFFE24B4A), Color(0xFFF09595), Color(0xFFFAC775), height = 46f, radius = 13f, petals = 5),
        Species("banana", "Banana", Arch.BROAD, Color(0xFF3FA878), Color(0xFF4FB483), Color(0xFFFAC775), fronds = 5, frondLen = 30f, fruit = true),
        Species("monstera", "Monstera", Arch.BROAD, Color(0xFF2E8C63), Color(0xFF3FA878), fronds = 5, frondLen = 26f, notch = true),
        Species("orchid", "Orchid", Arch.SPIKE, Color(0xFFD4537E), Color(0xFFF2A8C4), height = 44f, stalks = 3, leafColor = Color(0xFF3FA878)),
        Species("seagrape", "Sea grape", Arch.BUSH, Color(0xFF993556), Color(0xFFD4537E), radius = 17f, foliage = listOf(Color(0xFF357C5A), Color(0xFF479A70), Color(0xFF5FB489))),

        // ---- desert ----
        Species("saguaro", "Saguaro", Arch.CACTUS, Color(0xFF4FA57C), Color(0xFF3E8C67), Color(0xFFF2A8C4)),
        Species("pricklypear", "Prickly pear", Arch.CACTUS, Color(0xFF5DCAA5), Color(0xFF4FA57C), Color(0xFFEF9F27), pear = true),
        Species("aloe", "Aloe", Arch.ROSETTE, Color(0xFF69B98A), Color(0xFF9FE1CB), Color(0xFFE24B4A), fronds = 7, frondLen = 24f, bloom = true),
        Species("datepalm", "Date palm", Arch.TREE, Color(0xFF69B98A), Color(0xFF9BC48F), Color(0xFF854F0B), treeForm = TreeForm.FAN, trunk = 48f, canopy = 18f, bark = Color(0xFFA5825E), fruit = true),
        Species("desertmarigold", "Desert marigold", Arch.DISC, Color(0xFFEF9F27), Color(0xFFFAC775), Color(0xFF854F0B), height = 40f, radius = 8f, petals = 12, leafColor = Color(0xFF9BC48F)),
        Species("agave", "Agave", Arch.ROSETTE, Color(0xFF6FA88C), Color(0xFF8FC4A8), Color(0xFFFAC775), fronds = 9, frondLen = 26f, bloom = true),

        // ---- highland ----
        Species("pine", "Pine", Arch.TREE, Color(0xFF0F6E56), Color(0xFF1D9E75), treeForm = TreeForm.TRI, trunk = 30f, canopy = 17f),
        Species("edelweiss", "Edelweiss", Arch.DISC, Color(0xFFFFFFFF), Color(0xFFF1EFE8), Color(0xFFFAC775), height = 32f, radius = 9f, petals = 8, leafColor = Color(0xFF9FC3B4)),
        Species("heather", "Heather", Arch.BUSH, Color(0xFF993556), Color(0xFFD4537E), radius = 14f, foliage = listOf(Color(0xFF3E8C67), Color(0xFF5FA37F), Color(0xFF8FBFA3))),
        Species("gentian", "Gentian", Arch.CUP, Color(0xFF185FA5), Color(0xFF378ADD), height = 34f, cupWidth = 6f, cupHeight = 12f),
        Species("snowdrop", "Snowdrops", Arch.CUP, Color(0xFFFFFFFF), Color(0xFFE1F5EE), height = 36f, bell = true, cupWidth = 5f, cupHeight = 10f),
        Species("juniper", "Juniper", Arch.BUSH, Color(0xFF6B8FA8), Color(0xFF9FBDD2), radius = 15f, foliage = listOf(Color(0xFF33705A), Color(0xFF4A8A70), Color(0xFF6BA88C))),

        // ---- ice peak ----
        Species("frostfir", "Frost fir", Arch.TREE, Color(0xFF0F6E56), Color(0xFF1D9E75), treeForm = TreeForm.TRI, trunk = 28f, canopy = 16f, snowCap = true, bark = Color(0xFF6E6258)),
        Species("snowcrocus", "Snow crocus", Arch.CUP, Color(0xFFAFA9EC), Color(0xFFEEEDFE), height = 26f, cupWidth = 6f, cupHeight = 11f, stemColor = Color(0xFF8FB3A6), leafColor = Color(0xFF8FB3A6)),
        Species("icemoss", "Ice moss", Arch.CUSHION, Color(0xFF9FC3B4), Color(0xFFC3D9CF), Color(0xFFFFFFFF), radius = 13f),
        Species("dwarfwillow", "Dwarf willow", Arch.BUSH, Color(0xFFE1F5EE), Color(0xFFFFFFFF), radius = 13f, foliage = listOf(Color(0xFF7FA394), Color(0xFF93B5A5), Color(0xFFB0CBC0))),
        Species("lichen", "Glacier lichen", Arch.CUSHION, Color(0xFFB4B2A9), Color(0xFFD3D1C7), Color(0xFFEF9F27), radius = 12f),
        Species("snowbell", "Snow bell", Arch.CUP, Color(0xFFCFE4F2), Color(0xFFFFFFFF), height = 30f, bell = true, cupWidth = 5f, cupHeight = 10f, stemColor = Color(0xFF8FB3A6), leafColor = Color(0xFF8FB3A6))
    )

    private val byKey = all.associateBy { it.key }

    val count: Int get() = all.size

    operator fun get(key: String): Species = byKey.getValue(key)
}
