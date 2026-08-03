package com.anurag.eduai.uikit.garden

/**
 * Home-rail and avatar-tab strings for the garden feature.
 * Theme-specific verbs (planted / installed) come from [com.anurag.eduai.uikit.garden.quest.ThemeCopy].
 */
data class GardenHomeCopy(
    val yourCollection: (String) -> String,
    val openLabel: String,
    val justPlanted: String,
    val readyToGrow: String,
    val stepsOf: (Int, Int) -> String,
    val milestoneMore: (Int, String) -> String,
    val celebrationLine: (Int, String, Int, Int, String, Int) -> String,
    val nudgePlaceComplete: (String, String) -> String,
    val nudgeReady: String,
    val nudgeStepsLeft: (Int) -> String,
    val nudgeDefault: String,
    val hintKeepLearning: String,
    val hintPlantedKeepGoing: (Int, String) -> String,
    val segmentLook: String,
    val segmentScene: String,
    val segmentJourney: String,
    val avatarTabTitle: String,
    val whatYouGrew: String,
    val nothingPlantedYet: String,
    val journeyTitle: String,
    val journeySubtitle: String,
    val themeChosen: (String) -> String,
    val chosen: String,
    val loadingGarden: String,
    val surpriseLabel: String,
    val surprisePreview: (String) -> String,
    val yourPlaces: String,
    val growInPlace: (String) -> String,
    val placeFull: String,
    val nextPlaceTitle: String,
    val nextPlaceSubtitle: String,
    val viewInScene: String,
    val continueLabel: String,
    val lockedLabel: String,
    /** Shown on locked places — {place} is the previous zone name (Meadow, Luna, …). */
    val placeUnlockHint: (String) -> String,
    val placesUnlockedOf: (Int, Int) -> String,
    val starterPlantBadge: String,
    val starterPlantHomeNudge: (String) -> String,
    val seeAllCollected: (Int) -> String,
    /** Home collection shelf — first locked empty slot. */
    val collectionUnlockHint: String,
)

fun defaultGardenHomeCopy(): GardenHomeCopy =
    GardenHomeCopy(
        yourCollection = { place -> "Your ${place.lowercase()}" },
        openLabel = "Open",
        justPlanted = "Just planted",
        readyToGrow = "ready",
        stepsOf = { steps, total -> "$steps of $total" },
        milestoneMore = { n, what -> "  ·  $n more and $what" },
        celebrationLine = { total, done, filled, capacity, zone, remaining ->
            "$total $done · $filled/$capacity in $zone · $remaining places left"
        },
        nudgePlaceComplete = { zone, next -> "$zone done! Grow in $next" },
        nudgeReady = "Finish 1 task → a new plant",
        nudgeStepsLeft = { left -> "$left steps → a new plant" },
        nudgeDefault = "Finish 1 task to grow",
        hintKeepLearning = "Keep learning to grow",
        hintPlantedKeepGoing = { count, done -> "$count $done · keep learning for the next" },
        segmentLook = "Look",
        segmentScene = "Scene",
        segmentJourney = "Journey",
        avatarTabTitle = "Avatar & garden",
        whatYouGrew = "What you grew",
        nothingPlantedYet = "Nothing planted yet — complete a trial task",
        journeyTitle = "What you are growing",
        journeySubtitle =
            "Four ways to watch the same work pile up. Switch any time — nothing is lost.",
        themeChosen = { name -> "$name chosen" },
        chosen = "chosen",
        loadingGarden = "Loading your garden…",
        surpriseLabel = "Surprise",
        surprisePreview = { name -> "You'll get a ${name.lowercase()}" },
        yourPlaces = "Your places",
        growInPlace = { place -> "Grow in the $place" },
        placeFull = "Full",
        nextPlaceTitle = "Where next?",
        nextPlaceSubtitle = "Your place is full — pick where to grow next.",
        viewInScene = "View in Scene",
        continueLabel = "Continue",
        lockedLabel = "Locked",
        placeUnlockHint = { place -> "Fill $place to unlock" },
        placesUnlockedOf = { unlocked, total -> "$unlocked of $total unlocked" },
        starterPlantBadge = "Free",
        starterPlantHomeNudge = { plant ->
            "Surprise is on — or pick your free $plant to grow it every time."
        },
        seeAllCollected = { count -> "See all $count" },
        collectionUnlockHint = "Complete tasks to unlock more…",
    )
