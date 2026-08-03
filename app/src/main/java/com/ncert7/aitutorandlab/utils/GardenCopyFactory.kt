package com.ncert7.aitutorandlab.utils

import com.anurag.eduai.uikit.garden.GardenHomeCopy
import com.anurag.eduai.uikit.garden.defaultGardenHomeCopy
import com.anurag.eduai.uikit.garden.quest.Theme
import com.anurag.eduai.uikit.garden.quest.ThemeCopy

object GardenCopyFactory {
    fun homeCopy(languageCode: String): GardenHomeCopy =
        if (isKannadaLanguage(languageCode)) kannadaHome() else defaultGardenHomeCopy()

    fun themeCopy(languageCode: String, theme: Theme): ThemeCopy =
        if (isKannadaLanguage(languageCode)) kannadaTheme(theme) else ThemeCopy.of(theme)

    private fun kannadaHome(): GardenHomeCopy =
        GardenHomeCopy(
            yourCollection = { place -> "ನಿಮ್ಮ ${place.lowercase()}" },
            openLabel = "ತೆರೆಯಿರಿ",
            justPlanted = "ಈಗ ಬೆಳೆದಿದೆ",
            readyToGrow = "ಬೆಳೆಯಲು ಸಿದ್ಧ",
            stepsOf = { steps, total -> "$steps / $total" },
            milestoneMore = { n, what -> "  ·  ಇನ್ನೂ $n ಮತ್ತು $what" },
            celebrationLine = { total, done, filled, capacity, zone, remaining ->
                "$total $done · $filled/$capacity $zone · $remaining ಸ್ಥಳಗಳು ಬಾಕಿ"
            },
            nudgePlaceComplete = { zone, next -> "$zone ಪೂರ್ಣ! $next ನಲ್ಲಿ ಬೆಳೆಯಿರಿ" },
            nudgeReady = "1 ಕಾರ್ಯ ಮಾಡಿ → ಹೊಸ ಬೆಳೆ",
            nudgeStepsLeft = { left -> "$left ಹೆಜ್ಜೆ → ಹೊಸ ಬೆಳೆ" },
            nudgeDefault = "1 ಕಾರ್ಯ ಮಾಡಿ → ಬೆಳೆಯಿರಿ",
            hintKeepLearning = "ಬೆಳೆಯಲು ಕಲಿಕೆಯನ್ನು ಮುಂದುವರಿಸಿ",
            hintPlantedKeepGoing = { count, done ->
                "$count $done · ಮುಂದಿನದನ್ನು ಬೆಳೆಯಲು ಕಲಿಯಿರಿ"
            },
            segmentLook = "ನೋಟ",
            segmentScene = "ದೃಶ್ಯ",
            segmentJourney = "ಪ್ರಯಾಣ",
            avatarTabTitle = "ಅವತಾರ ಮತ್ತು garden",
            whatYouGrew = "ನಿಮ್ಮ ಬೆಳೆಗಳು",
            nothingPlantedYet = "ಇನ್ನೂ ಏನೂ ಬೆಳೆದಿಲ್ಲ — ಪ್ರಯೋಗ ಪೂರ್ಣಗೊಳಿಸಿ",
            journeyTitle = "ನೀವು ಏನು ಬೆಳೆಯುತ್ತಿದ್ದೀರಿ",
            journeySubtitle =
                "ನಾಲ್ಕು ರೀತಿಯಲ್ಲಿ ನೋಡಿ — ಯಾವಾಗ ಬೇಕಾದರೂ ಬದಲಿಸಿ, ಏನೂ ಕಳೆದು ಹೋಗುವುದಿಲ್ಲ.",
            themeChosen = { name -> "$name ಆಯ್ಕೆ ಮಾಡಲಾಗಿದೆ" },
            chosen = "ಆಯ್ಕೆ",
            loadingGarden = "Garden ಲೋಡ್ ಆಗುತ್ತಿದೆ…",
            surpriseLabel = "ಆಶ್ಚರ್ಯ",
            surprisePreview = { name -> "ನೀವು ${name.lowercase()} ಪಡೆಯುತ್ತೀರಿ" },
            yourPlaces = "ನಿಮ್ಮ ಸ್ಥಳಗಳು",
            growInPlace = { place -> "$place ನಲ್ಲಿ ಬೆಳೆಯಿರಿ" },
            placeFull = "ಪೂರ್ಣ",
            nextPlaceTitle = "ಮುಂದೆ ಎಲ್ಲಿ?",
            nextPlaceSubtitle = "ಈ ಸ್ಥಳ ತುಂಬಿದೆ — ಮುಂದೆ ಎಲ್ಲಿ ಬೆಳೆಯುವಿರಿ ಆರಿಸಿ.",
            viewInScene = "ದೃಶ್ಯದಲ್ಲಿ ನೋಡಿ",
            continueLabel = "ಮುಂದುವರಿಸಿ",
            lockedLabel = "ಲಾಕ್",
            placeUnlockHint = { place -> "$place ತುಂಬಿಸಿ — ಅನ್ಲಾಕ್" },
            placesUnlockedOf = { unlocked, total -> "$unlocked / $total ಅನ್ಲಾಕ್" },
            starterPlantBadge = "ಉಚಿತ",
            starterPlantHomeNudge = { plant ->
                "ಆಶ್ಚರ್ಯ ಆನ್ — ಅಥವಾ ನಿಮ್ಮ ಉಚಿತ $plant ಆರಿಸಿ, ಪ್ರತಿ ಬಾರಿ ಅದನ್ನೇ ಬೆಳೆಯಿರಿ."
            },
            seeAllCollected = { count -> "ಎಲ್ಲಾ $count ನೋಡಿ" },
            collectionUnlockHint = "ಹೆಚ್ಚು ಅನ್ಲಾಕ್ ಮಾಡಲು ಕಾರ್ಯಗಳನ್ನು ಪೂರ್ಣಗೊಳಿಸಿ…",
        )

    private fun kannadaTheme(theme: Theme): ThemeCopy =
        when (theme) {
            Theme.GARDEN -> gardenKn()
            Theme.OUTPOST -> outpostKn()
            Theme.ISLAND -> islandKn()
            Theme.COLONY -> colonyKn()
        }

    private fun gardenKn() =
        ThemeCopy(
            placeCollection = "ತೋಟ",
            pitch = "ಎಂಟು ಸ್ಥಳಗಳು, ಪ್ರತಿಯೊಂದರಲ್ಲಿ ಹನ್ನೆರಡು ಬೆಳೆಗಳು. ನೀವು ಏನು ಬೆಳೆಯಬೇಕೆಂದು ಆರಿಸಿ.",
            item = "ಬೆಳೆ",
            growing = "ಬೆಳೆಯುತ್ತಿದೆ",
            done = "ಬೆಳೆದಿದೆ",
            startButton = "ಪ್ರಾರಂಭ · ಇದನ್ನು ಬೆಳೆಯಿರಿ",
            continueButton = "ಮುಂದುವರಿಸಿ",
            finishButton = "ಬೆಳೆಸಿ",
            startShort = "ಬೆಳೆಯಲು ಪ್ರಾರಂಭ",
            finishShort = "ಬೆಳೆಸಿ",
            momentTitle = "ಬೆಳೆದಿದೆ",
            pickerTitle = "ನೀವು ಏನು ಬೆಳೆಯುತ್ತಿದ್ದೀರಿ?",
            pickerBody = "ಈಗ ಆರಿಸಿ — ಕಾರ್ಯ ಪ್ರಾರಂಭಿಸಿದ ನಂತರ ಬೆಳೆದ ತನಕ ಲಾಕ್ ಆಗಿರುತ್ತದೆ.",
            sceneEmpty = "ಇನ್ನೂ ಏನೂ ಇಲ್ಲ. ಕಾರ್ಯ ಮುಗಿಸಿ — ಇಲ್ಲಿ ಬರುತ್ತದೆ.",
            sceneListTitle = "ಇಲ್ಲಿ ನೀವು ಬೆಳೆಸಿದವು",
            startHint = "ಪ್ರಾರಂಭಿಸಿದರೆ ಮುಗಿಸಬೇಕು",
            everythingDone = "ಎಲ್ಲಾ ಸ್ಥಳಗಳು ತುಂಬಿವೆ. ಬೆಳೆಯಲು ಏನೂ ಬಾಕಿ ಇಲ್ಲ — ಆದರೆ ಪ್ರಯಾಣ ಇಲ್ಲಿದೆ.",
            milestones =
                listOf(
                    6 to "ಜೇನುನೊಣಗಳು ಬರುತ್ತವೆ",
                    14 to "ಚಿಟ್ಟೆಗಳು ಕಾಣಿಸಿಕೊಳ್ಳುತ್ತವೆ",
                    24 to "ಹಾಡುಗಾರ ಪಕ್ಷಿಗಳು ಬರುತ್ತವೆ",
                    36 to "ಮುದ್ದೆ ಮುದ್ದೆ ಮುದ್ದೆ ಕಾಣಿಸಿಕೊಳ್ಳುತ್ತದೆ",
                ),
            unlockAfterPlace = { place -> "$place ತುಂಬಿಸಿ — ಅನ್ಲಾಕ್" },
            avatarGrowHint =
                "ಕಲಿಯಿರಿ — ಪ್ರಯೋಗ ಕಾರ್ಯಗಳನ್ನು ಮುಗಿಸಿ, ಹೊಸ ಬೆಳೆಗಳನ್ನು ಬೆಳೆಸಿ ಮತ್ತು ಹೊಸ ಸ್ಥಳಗಳನ್ನು ಅನ್ಲಾಕ್ ಮಾಡಿ.",
        )

    private fun outpostKn() =
        ThemeCopy(
            placeCollection = "ಬಾಹ್ಯಾಕಾಶ",
            pitch = "ಎಂಟು ಜಗತ್ತುಗಳು, ಪ್ರತಿಯೊಂದರಲ್ಲಿ ಹನ್ನೆರಡು ಮಾಡ್ಯೂಲ್‌ಗಳು. ನೀವು ಏನು ನಿರ್ಮಿಸಬೇಕೆಂದು ಆರಿಸಿ.",
            item = "ಮಾಡ್ಯೂಲ್",
            growing = "ನಿರ್ಮಿಸಲಾಗುತ್ತಿದೆ",
            done = "ಸ್ಥಾಪಿಸಲಾಗಿದೆ",
            startButton = "ಪ್ರಾರಂಭ · ಇದನ್ನು ನಿರ್ಮಿಸಿ",
            continueButton = "ಮುಂದುವರಿಸಿ",
            finishButton = "ಸ್ಥಾಪಿಸಿ",
            startShort = "ನಿರ್ಮಾಣ ಪ್ರಾರಂಭ",
            finishShort = "ಸ್ಥಾಪಿಸಿ",
            momentTitle = "ಸ್ಥಾಪಿಸಲಾಗಿದೆ",
            pickerTitle = "ನೀವು ಏನು ನಿರ್ಮಿಸುತ್ತಿದ್ದೀರಿ?",
            pickerBody = "ಈಗ ಆರಿಸಿ — ಕಾರ್ಯ ಪ್ರಾರಂಭಿಸಿದ ನಂತರ ಸ್ಥಾಪಿಸುವವರೆಗೆ ಲಾಕ್ ಆಗಿರುತ್ತದೆ.",
            sceneEmpty = "ಇನ್ನೂ ಏನೂ ಇಲ್ಲ. ಕಾರ್ಯ ಮುಗಿಸಿ — ಇಲ್ಲಿ ಬರುತ್ತದೆ.",
            sceneListTitle = "ಇಲ್ಲಿ ನೀವು ನಿರ್ಮಿಸಿದ್ದು",
            startHint = "ಪ್ರಾರಂಭಿಸಿದರೆ ಮುಗಿಸಬೇಕು",
            everythingDone = "ಎಲ್ಲಾ ಜಗತ್ತುಗಳು ಪೂರ್ಣ. ಸ್ಥಾಪಿಸಲು ಏನೂ ಬಾಕಿ ಇಲ್ಲ — ಆದರೆ ಮಾರ್ಗ ಇಲ್ಲಿದೆ.",
            milestones =
                listOf(
                    6 to "ವಿದ್ಯುತ್ ಆನ್ ಆಗುತ್ತದೆ",
                    14 to "ಮೊದಲ ತಂಡ ಬರುತ್ತದೆ",
                    24 to "ಸರಬರಾಜು ಬರುತ್ತದೆ",
                    36 to "ಬೀಕನ್ ಬೆಳಗುತ್ತದೆ",
                ),
            unlockAfterPlace = { place -> "$place ಪೂರ್ಣಗೊಳಿಸಿ — ಅನ್ಲಾಕ್" },
            avatarGrowHint =
                "ಕಲಿಯಿರಿ — ಪ್ರಯೋಗ ಕಾರ್ಯಗಳನ್ನು ಮುಗಿಸಿ, ಮಾಡ್ಯೂಲ್‌ಗಳನ್ನು ಸ್ಥಾಪಿಸಿ ಮತ್ತು ಹೊಸ ಜಗತ್ತುಗಳನ್ನು ಅನ್ಲಾಕ್ ಮಾಡಿ.",
        )

    private fun islandKn() =
        ThemeCopy(
            placeCollection = "ದ್ವೀಪ",
            pitch = "ಒಂದು ದ್ವೀಪ, 61 ಟೈಲ್‌ಗಳು. ಶಿಖರದಿಂದ ಹೊರಗೆ ಬೆಳೆಯುತ್ತದೆ — ಆಯ್ಕೆ ಇಲ್ಲ.",
            item = "ಟೈಲ್",
            growing = "ಸ್ವಚ್ಛಗೊಳಿಸಲಾಗುತ್ತಿದೆ",
            done = "ಸ್ವಚ್ಛಗೊಂಡಿದೆ",
            startButton = "ಪ್ರಾರಂಭ · ಮುಂದಿನ ಟೈಲ್",
            continueButton = "ಮುಂದುವರಿಸಿ",
            finishButton = "ದ್ವೀಪಕ್ಕೆ ಸೇರಿಸಿ",
            startShort = "ಸ್ವಚ್ಛಗೊಳಿಸಲು ಪ್ರಾರಂಭ",
            finishShort = "ಟೈಲ್ ಸೇರಿಸಿ",
            momentTitle = "ಭೂಮಿ ಸಿಕ್ಕಿತು",
            pickerTitle = "",
            pickerBody = "",
            sceneEmpty = "ಇನ್ನೂ ಖಾಲಿ ಬಂಡೆ. ಕಾರ್ಯ ಮುಗಿಸಿ — ಮೊದಲ ಟೈಲ್ ಕಾಣಿಸುತ್ತದೆ.",
            sceneListTitle = "ದ್ವೀಪದಲ್ಲಿರುವವು",
            startHint = "ಪ್ರಾರಂಭಿಸಿದರೆ ಮುಗಿಸಬೇಕು",
            everythingDone = "ಸಂಪೂರ್ಣ ದ್ವೀಪ ನಿಮ್ಮದು. ಐವತ್ತೊಂದು ಟೈಲ್‌ಗಳು — ಪ್ರತಿಯೊಂದೂ ಗಳಿಕೆ.",
            milestones =
                listOf(
                    3 to "ಶಿಖರದ ಕಲ್ಲು ಹಾಕಲಾಗುತ್ತದೆ",
                    9 to "ಮರ ಕತ್ತಿ ಕುಟೀರು ಕಾಣಿಸಿಕೊಳ್ಳುತ್ತದೆ",
                    20 to "ಗಾಳಿ ಗಿರಣಿ ತಿರುಗಲು ಪ್ರಾರಂಭಿಸುತ್ತದೆ",
                    34 to "ಗ್ರಾಮದ ಬಾವಿ ತೋಡಲಾಗುತ್ತದೆ",
                    50 to "ದೀಪಸ್ತಂಭ ಬೆಳಗುತ್ತದೆ",
                    57 to "ಜೆಟ್ಟಿ ನೀರಿಗೆ ತಲುಪುತ್ತದೆ",
                ),
            unlockAfterPlace = { _ -> "" },
            avatarGrowHint =
                "ಕಲಿಯಿರಿ — ಪ್ರಯೋಗ ಕಾರ್ಯಗಳನ್ನು ಮುಗಿಸಿ, ಟೈಲ್‌ಗಳನ್ನು ಸ್ವಚ್ಛಗೊಳಿಸಿ ಮತ್ತು ದ್ವೀಪವನ್ನು ಬೆಳೆಸಿ.",
        )

    private fun colonyKn() =
        ThemeCopy(
            placeCollection = "ಬಾಹ್ಯಾಕಾಶ colony",
            pitch = "ಒಂದು colony, ಹನ್ನೆರಡು ಹಂತಗಳು. ಚಂದ್ರದಿಂದ ನಕ್ಷತ್ರಗಳವರೆಗೆ — ಅತಿ ದೀರ್ಘ ಪ್ರಯಾಣ.",
            item = "ನಿರ್ಮಾಣ",
            growing = "ನಿರ್ಮಿಸಲಾಗುತ್ತಿದೆ",
            done = "ನಿರ್ಮಿಸಲಾಗಿದೆ",
            startButton = "ಪ್ರಾರಂಭ · ಇದನ್ನು ನಿರ್ಮಿಸಿ",
            continueButton = "ಮುಂದುವರಿಸಿ",
            finishButton = "ಕಳುಹಿಸಿ",
            startShort = "ನಿರ್ಮಾಣ ಪ್ರಾರಂಭ",
            finishShort = "ಕಳುಹಿಸಿ",
            momentTitle = "ನಿರ್ಮಿಸಲಾಗಿದೆ",
            pickerTitle = "",
            pickerBody = "",
            sceneEmpty = "ಇನ್ನೂ ಖಾಲಿ ಮೇಲ್ಮೈ. ಕಾರ್ಯ ಮುಗಿಸಿ — lander ಇಳಿಯುತ್ತದೆ.",
            sceneListTitle = "colony ನಿರ್ಮಿಸಿದ್ದು",
            startHint = "ಪ್ರಾರಂಭಿಸಿದರೆ ಮುಗಿಸಬೇಕು",
            everythingDone = "ನಕ್ಷತ್ರಗಳವರೆಗೆ. 96 ಕಾರ್ಯಗಳು, ಹನ್ನೆರಡು ಹಂತಗಳು.",
            milestones =
                listOf(
                    8 to "ಸೌರ ಶಕ್ತಿ ಸರಬರಾಜು",
                    16 to "bio dome ತೆರೆಯುತ್ತದೆ",
                    24 to "rovers ಅನ್ವೇಷಣೆ ಪ್ರಾರಂಭ",
                    40 to "launch pad ಮೊದಲ火箭",
                    56 to "ಆಮ್ಲಜನಕ ಟವರ್‌ಗಳು terraforming",
                    80 to "fusion reactor colony ಬೆಳಗಿಸುತ್ತದೆ",
                ),
            unlockAfterPlace = { _ -> "" },
            avatarGrowHint =
                "ಕಲಿಯಿರಿ — ಪ್ರಯೋಗ ಕಾರ್ಯಗಳನ್ನು ಮುಗಿಸಿ, colony ಅನ್ನು ಹಂತ ಹಂತವಾಗಿ ನಿರ್ಮಿಸಿ.",
        )
}
