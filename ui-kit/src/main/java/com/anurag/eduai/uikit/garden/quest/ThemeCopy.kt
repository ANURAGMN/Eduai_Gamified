package com.anurag.eduai.uikit.garden.quest

/**
 * Every user-facing string that changes when the theme changes.
 *
 * The point of the theme is that it is a wardrobe, not a second product — the loop, the slots and
 * the stored data are identical. But the *words* have to move with it, or a student who picks the
 * outpost gets a button that says "Plant it" over a solar array.
 *
 * Keeping them in one object also makes the Kannada port a single file to translate rather than a
 * hunt through the composables. In Eduapp this becomes `GardenLabels`, taking a language code,
 * because `:ui-kit` has no `res/` directory of its own.
 */
data class ThemeCopy(
    /** What the collection of places is called. Used as the tab label. */
    val placeCollection: String,
    /** One line for the picker in the journey — what this theme feels like. */
    val pitch: String,
    /** One thing you grow, singular, lowercase. */
    val item: String,
    /** Present continuous, shown on the locked chip. */
    val growing: String,
    /** Past participle — "planted" / "installed". */
    val done: String,
    val startButton: String,
    val continueButton: String,
    val finishButton: String,
    /**
     * Short forms for the Eduapp rail, where the button shares a row with 104 dp of art.
     * "Start · clear the next tile" needs 170 dp at the default font scale and 340 dp at the
     * accessibility maximum — it does not fit on a 320 dp phone at any scale. The long forms stay
     * for the standalone home screen, which has the whole width.
     */
    val startShort: String,
    val finishShort: String,
    val momentTitle: String,
    val pickerTitle: String,
    val pickerBody: String,
    val sceneEmpty: String,
    val sceneListTitle: String,
    val startHint: String,
    val everythingDone: String,
    /** Ambient rewards, as (total items needed, what happens). */
    val milestones: List<Pair<Int, String>>,
    /** Previous place name — hint on locked journey chips. */
    val unlockAfterPlace: (String) -> String,
    /** One-line nudge on the Avatar tab — how tasks grow the scene. */
    val avatarGrowHint: String,
) {
    fun lockedNote(what: String): String =
        "$growing a ${what.lowercase()}. You can pick a new one once this is $done."

    /** Shown the moment a theme is picked, so the switch is never silent. */
    val selectedNote: String get() = "$placeCollection selected. $pitch"

    companion object {
        private val GARDEN =
            ThemeCopy(
                placeCollection = "Garden",
                pitch = "Eight places, twelve plants each. You pick what grows.",
                item = "plant",
                growing = "Growing",
                done = "planted",
                startButton = "Start · grow this one",
                continueButton = "Keep going",
                finishButton = "Plant it",
                startShort = "Start growing",
                finishShort = "Plant it",
                momentTitle = "Planted",
                pickerTitle = "What are you growing?",
                pickerBody = "Pick now — once you start the task it is locked in until it is planted.",
                sceneEmpty = "Nothing yet. Finish a task and it lands here.",
                sceneListTitle = "Everything you have grown here",
                startHint = "Once you start, you see it through",
                everythingDone = "Every place is full. Nothing left to plant — but the journey is all there.",
                milestones =
                    listOf(
                        6 to "the bees arrive",
                        14 to "butterflies turn up",
                        24 to "songbirds move in",
                        36 to "a rabbit appears",
                    ),
                unlockAfterPlace = { place -> "Fill $place to unlock" },
                avatarGrowHint =
                    "Learn more — complete trial tasks to grow plants and unlock new places.",
            )

        private val OUTPOST =
            ThemeCopy(
                placeCollection = "Space",
                pitch = "Eight worlds, twelve modules each. You pick what gets built.",
                item = "module",
                growing = "Building",
                done = "installed",
                startButton = "Start · build this one",
                continueButton = "Keep going",
                finishButton = "Install it",
                startShort = "Start building",
                finishShort = "Install it",
                momentTitle = "Installed",
                pickerTitle = "What are you building?",
                pickerBody = "Pick now — once you start the task it is locked in until it is installed.",
                sceneEmpty = "Nothing yet. Finish a task and it lands here.",
                sceneListTitle = "Everything you have built here",
                startHint = "Once you start, you see it through",
                everythingDone = "Every world is built out. Nothing left to install — but the route is all there.",
                milestones =
                    listOf(
                        6 to "power comes online",
                        14 to "the first crew arrives",
                        24 to "a supply drop lands",
                        36 to "the beacon lights up",
                    ),
                unlockAfterPlace = { place -> "Complete $place to unlock" },
                avatarGrowHint =
                    "Learn more — complete trial tasks to install modules and unlock new worlds.",
            )

        /**
         * The island is one scene, not eight — sixty-one tiles revealed from the peak outward, one
         * per finished task. Nothing to choose, so the picker strings are never read. The milestones
         * are the six real landmarks, at the cell indices they are actually placed at.
         */
        private val ISLAND =
            ThemeCopy(
                placeCollection = "Island",
                pitch = "One island, 61 tiles. It grows outward from the peak — nothing to pick.",
                item = "tile",
                growing = "Clearing",
                done = "cleared",
                startButton = "Start · clear the next tile",
                continueButton = "Keep going",
                finishButton = "Add it to the island",
                startShort = "Start clearing",
                finishShort = "Add the tile",
                momentTitle = "Land gained",
                pickerTitle = "",
                pickerBody = "",
                sceneEmpty = "Bare rock so far. Finish a task and the first tile appears.",
                sceneListTitle = "Everything on the island",
                startHint = "Once you start, you see it through",
                everythingDone = "The whole island is yours. Sixty-one tiles, every one earned.",
                milestones =
                    listOf(
                        3 to "the summit cairn goes up",
                        9 to "the woodcutter's cabin appears",
                        20 to "the windmill starts turning",
                        34 to "the village well is dug",
                        50 to "the lighthouse lights",
                        57 to "the jetty reaches the water",
                    ),
                unlockAfterPlace = { _ -> "" },
                avatarGrowHint =
                    "Learn more — complete trial tasks to clear tiles and grow your island.",
            )

        /**
         * The colony is a ladder, not a map: twelve tiers of eight tasks. It is the longest arc of
         * the four and the only one where the whole scene changes rather than filling up.
         */
        private val COLONY =
            ThemeCopy(
                placeCollection = "Space colony",
                pitch = "One colony, twelve tiers. Lunar landing to interstellar — the slowest burn.",
                item = "build",
                growing = "Building",
                done = "built",
                startButton = "Start · build this one",
                continueButton = "Keep going",
                finishButton = "Ship it",
                startShort = "Start building",
                finishShort = "Ship it",
                momentTitle = "Built",
                pickerTitle = "",
                pickerBody = "",
                sceneEmpty = "Bare surface so far. Finish a task and the lander sets down.",
                sceneListTitle = "Everything the colony has built",
                startHint = "Once you start, you see it through",
                everythingDone = "Interstellar. Ninety-six tasks, twelve tiers, a civilisation.",
                milestones =
                    listOf(
                        8 to "the solar arrays go up",
                        16 to "the bio dome opens",
                        24 to "rovers start exploring",
                        40 to "the launch pad takes its first rocket",
                        56 to "oxygen towers begin terraforming",
                        80 to "the fusion reactor lights the whole colony",
                    ),
                unlockAfterPlace = { _ -> "" },
                avatarGrowHint =
                    "Learn more — complete trial tasks to build your colony, tier by tier.",
            )

        fun of(theme: Theme): ThemeCopy =
            when (theme) {
                Theme.GARDEN -> GARDEN
                Theme.OUTPOST -> OUTPOST
                Theme.ISLAND -> ISLAND
                Theme.COLONY -> COLONY
            }
    }
}
