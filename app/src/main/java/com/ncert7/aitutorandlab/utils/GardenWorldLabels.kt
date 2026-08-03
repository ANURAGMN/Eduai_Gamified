package com.ncert7.aitutorandlab.utils

import com.anurag.eduai.uikit.garden.quest.Theme
import com.anurag.eduai.uikit.garden.quest.Zone
import com.anurag.eduai.uikit.garden.world.WORLDS

/** Localized habitat, world, plant and module names for the garden / space scene. */
object GardenWorldLabels {
    fun zoneName(zone: Zone, theme: Theme, languageCode: String): String {
        if (!isKannadaLanguage(languageCode)) return zone.name(theme)
        return if (theme == Theme.GARDEN) {
            habitatKn[zone.habitat.key] ?: zone.habitat.name
        } else {
            worldKn[WORLDS[zone.index].key] ?: zone.world.name
        }
    }

    fun slotName(zone: Zone, theme: Theme, slot: Int, languageCode: String): String {
        if (!isKannadaLanguage(languageCode)) return zone.slotName(theme, slot)
        return if (theme == Theme.GARDEN) {
            val speciesKey = zone.habitat.species.getOrNull(slot) ?: return zone.slotName(theme, slot)
            speciesKn[speciesKey] ?: zone.slotName(theme, slot)
        } else {
            val worldKey = WORLDS[zone.index].key
            modulesKn[worldKey]?.getOrNull(slot) ?: zone.slotName(theme, slot)
        }
    }

    private val habitatKn: Map<String, String> =
        mapOf(
            "meadow" to "ಹುಲ್ಲಿನ ಗವೇದಿ",
            "woodland" to "ಅರಣ್ಯ",
            "wetland" to "ತೇವ ನೆಲ",
            "beach" to "ಕಡಲ ತೀರ",
            "island" to "ದ್ವೀಪ",
            "desert" to "ಮರುಭೂಮಿ",
            "highland" to "ಬೆಟ್ಟ ಪ್ರದೇಶ",
            "icepeak" to "ಹಿಮ ಶಿಖರ",
        )

    private val worldKn: Map<String, String> =
        mapOf(
            "luna" to "ಚಂದ್ರ",
            "mars" to "ಮಂಗಳ",
            "europa" to "ಯೂರೋಪಾ",
            "titan" to "ಟೈಟನ್",
            "venus" to "ಶುಕ್ರ",
            "io" to "ಐ.o",
            "ceres" to "ಸಿರೀಸ್",
            "enceladus" to "ಎನ್ಸೆಲಡಸ್",
        )

    private val speciesKn: Map<String, String> =
        mapOf(
            "sunflower" to "ಸೂರ್ಯಕಾಂತಿ",
            "daisy" to "ಡೈಸಿ",
            "poppy" to "ಗಸಗಸೆ",
            "tulip" to "ಟ್ಯುಲಿಪ್",
            "rose" to "ಗುಲಾಬಿ ಕುಂಜ",
            "lavender" to "ಲ್ಯಾವೆಂಡರ್",
            "oak" to "ಚಿಕ್ಕ ಇಲಾವು",
            "birch" to "ಬರ್ಚ್ ಮರ",
            "blossom" to "ಚೆರಿ ಮಲ್ಲಿಗೆ",
            "fern" to "ಪುಟ್ಟರವ",
            "bluebell" to "ನೀಲಿ ಗಂಟಲು",
            "toadstool" to "ಆಮೆ ಕುಡಿ",
            "lotus" to "ತಾವರೆ",
            "waterlily" to "ನೀಲೋತ್ಪಲ",
            "cattail" to "ಕ್ಯಾಟ್‌ಟೈಲ್",
            "iris" to "ಐರಿಸ್",
            "papyrus" to "ಪapyrus",
            "marshmarigold" to "marsh marigold",
            "marram" to "ಮಾರಾಮ್ ಹುಲ್ಲು",
            "seaholly" to "ಸಮುದ್ರ holly",
            "beachrose" to "ಕಡಲ ಗುಲಾಬಿ",
            "sealavender" to "ಸಮುದ್ರ lavender",
            "morningglory" to "ಮorning glory",
            "dunedaisy" to " dunes daisy",
            "palm" to "ತೆಂಗಿನ ಮರ",
            "hibiscus" to "ದಾಸವಾಳ",
            "banana" to "ಬಾಳೆ",
            "monstera" to "monstera",
            "orchid" to "ಆರ್ಕಿಡ್",
            "seagrape" to "ಸಮುದ್ರ ದ್ರಾಕ್ಷಿ",
            "saguaro" to "saguaro",
            "pricklypear" to "prickly pear",
            "aloe" to "ಲೋ",
            "datepalm" to "ಖರ್ಜೂರ ಮರ",
            "desertmarigold" to "ಮರುಭೂಮಿ marigold",
            "agave" to "agave",
            "pine" to "ದೇವದಾರು",
            "edelweiss" to "edelweiss",
            "heather" to "heather",
            "gentian" to "gentian",
            "snowdrop" to "snowdrop",
            "juniper" to "juniper",
            "frostfir" to "ಹಿಮ ದೇವದಾರು",
            "snowcrocus" to "ಹಿಮ crocus",
            "icemoss" to "ಹಿಮ moss",
            "dwarfwillow" to "ಗಿಡ willow",
            "lichen" to "lichen",
            "snowbell" to "ಹಿಮ bell",
        )

    private val modulesKn: Map<String, List<String>> =
        mapOf(
            "luna" to
                listOf(
                    "ವಾಸ ಗುಂಬಜ",
                    "ಸೌರ ಶಕ್ತಿ ವ್ಯವಸ್ಥೆ",
                    "ಸಂಪರ್ಕ ಕಮ்பಿ",
                    "ಮಣ್ಣು ಪ್ರಯೋಗಾಲಯ",
                    "ದಂಡೆ",
                    "ಸಂಗ್ರಹ ಸಿಲೋ",
                ),
            "mars" to
                listOf(
                    "ಜೈವ ಗುಂಬಜ",
                    "ಸೌರ ಶಕ್ತಿ ವ್ಯವಸ್ಥೆ",
                    "ಹವಾಮಾನ ಕಮ்பಿ",
                    "ಮಾದರಿ ಪ್ರಯೋಗಾಲಯ",
                    "ರೋವರ್ ಗарааж",
                    "ನೀರಿನ ತಂಕ",
                ),
            "europa" to
                listOf(
                    "ಐಸ್ ಗುಂಬಜ",
                    "ತಾಪ ಶಕ್ತಿ ವ್ಯವಸ್ಥೆ",
                    "relay ಕಮ്പಿ",
                    "ಡ್ರಿಲ್ ಸಾಧನ",
                    "ಜಲಾವರಣ ಬೇ",
                    "cryo ತಂಕ",
                ),
            "titan" to
                listOf(
                    "ಮಂಧ ಗುಂಬಜ",
                    "methane ಸಾಧನ",
                    "balloon ಕಮ്പಿ",
                    "ರಸಾಯನ ಪ್ರಯೋಗಾಲಯ",
                    "lake lander",
                    "fuel ತಂಕ",
                ),
            "venus" to
                listOf(
                    "ಮೇಘ ಗುಂಬಜ",
                    "reflector ವ್ಯವಸ್ಥೆ",
                    "sky ಕಮ்பಿ",
                    "acid ಪ್ರಯೋಗಾಲಯ",
                    "airship dock",
                    "ballast ತಂಕ",
                ),
            "io" to
                listOf(
                    "shield ಗುಂಬಜ",
                    "heat ವ್ಯವಸ್ಥೆ",
                    "seismic ಕಮ்பಿ",
                    "lava ಪ್ರಯೋಗಾಲಯ",
                    "ash pad",
                    "sulphur ತಂಕ",
                ),
            "ceres" to
                listOf(
                    "rock ಗುಂಬಜ",
                    "mirror ವ್ಯವಸ್ಥೆ",
                    "beacon ಕಮ್ಬಿ",
                    "ore ಪ್ರಯೋಗಾಲಯ",
                    "tug pad",
                    "ice hopper",
                ),
            "enceladus" to
                listOf(
                    "frost ಗುಂಬಜ",
                    "plume ವ್ಯವಸ್ಥೆ",
                    "signal ಕಮ್ಬಿ",
                    "geyser ಪ್ರಯೋಗಾಲಯ",
                    "skiff pad",
                    "vapour ತಂಕ",
                ),
        )
}
