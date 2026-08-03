package com.ncert7.aitutorandlab.domain.moment

import com.anurag.eduai.uikit.avatar.core.EmotionType
import com.ncert7.aitutorandlab.utils.isKannadaLanguage

/**
 * The copy catalog: exactly five variants per moment. The picker draws one at random
 * (never the same one twice in a row). Emotions are varied within a moment so repeats
 * feel fresh. Celebrations use confetti; the two nudges do not.
 *
 * Later this can be sourced from Remote Config; these are the built-in defaults.
 */
object MomentVariants {

    fun forMoment(moment: MomentType, languageCode: String = "en"): List<MomentVariant> {
        val english =
            when (moment) {
                MomentType.SIM_COMPLETED -> SIM_COMPLETED
                MomentType.STUDY_COMPLETED -> STUDY_COMPLETED
                MomentType.DAY_COMPLETED -> DAY_COMPLETED
                MomentType.COMEBACK_INCOMPLETE -> COMEBACK_INCOMPLETE
                MomentType.EXIT_INCOMPLETE -> EXIT_INCOMPLETE
                MomentType.PLANT_COMPLETED -> PLANT_COMPLETED
                MomentType.PLACE_COMPLETED -> PLACE_COMPLETED
            }
        if (!isKannadaLanguage(languageCode)) return english
        return english.map { variant ->
            val kn = KN_TEXT[variant.id]
            if (kn == null) {
                variant
            } else {
                variant.copy(
                    headline = kn.headline,
                    body = kn.body,
                    primaryCta = kn.primaryCta,
                    secondaryCta = kn.secondaryCta,
                )
            }
        }
    }

    private val SIM_COMPLETED =
        listOf(
            MomentVariant("sim_done_1", true, EmotionType.Celebrating, "Nailed it!", "{bite} done — +{gems} gems.", "Keep going"),
            MomentVariant("sim_done_2", true, EmotionType.Happy, "Boom — done!", "That's another one cleared.", "Next up"),
            MomentVariant("sim_done_3", true, EmotionType.Surprised, "You're on fire!", "{bite} complete. Nice focus.", "Continue"),
            MomentVariant("sim_done_4", true, EmotionType.Celebrating, "Clean run!", "Simulation cleared — the next one's waiting.", "Let's go"),
            MomentVariant("sim_done_5", true, EmotionType.Happy, "Level cleared!", "+{gems} gems. Momentum is yours.", "Keep it up"),
        )

    private val STUDY_COMPLETED =
        listOf(
            MomentVariant("study_done_1", true, EmotionType.Celebrating, "Brilliant!", "You finished {bite}. +{gems} gems.", "Keep going"),
            MomentVariant("study_done_2", true, EmotionType.Happy, "That's a wrap!", "Study session complete — well done.", "Next up"),
            MomentVariant("study_done_3", true, EmotionType.Surprised, "Sharp work!", "{bite} understood. Onwards!", "Continue"),
            MomentVariant("study_done_4", true, EmotionType.Happy, "You've got this!", "Another concept in the bank.", "Let's go"),
            MomentVariant("study_done_5", true, EmotionType.Celebrating, "Knowledge unlocked!", "+{gems} gems for finishing {bite}.", "Keep it up"),
        )

    private val DAY_COMPLETED =
        listOf(
            MomentVariant("day_done_1", true, EmotionType.Celebrating, "Day complete!", "Every task done today. Huge. +{gems} gems.", "Done"),
            MomentVariant("day_done_2", true, EmotionType.Happy, "Perfect day!", "You cleared the whole trial for today.", "Done"),
            MomentVariant("day_done_3", true, EmotionType.Celebrating, "Full marks!", "All of today's tasks — finished. +{gems} gems.", "Done"),
            MomentVariant("day_done_4", true, EmotionType.Surprised, "Unstoppable!", "Today is 100% complete. See you tomorrow.", "Done"),
            MomentVariant("day_done_5", true, EmotionType.Happy, "Streak fuel!", "Day cleared — your streak thanks you.", "Done"),
        )

    private val COMEBACK_INCOMPLETE =
        listOf(
            MomentVariant("comeback_1", false, EmotionType.Teaching, "Almost there", "Just {remaining} more to finish {bite}. Wrap it up?", "Finish now", "Later"),
            MomentVariant("comeback_2", false, EmotionType.Explaining, "Let's close it out", "You left {bite} half-done — two minutes to finish.", "Finish now", "Later"),
            MomentVariant("comeback_3", false, EmotionType.Neutral, "Pick up where you left off", "{bite} is {remaining} steps from done.", "Continue", "Later"),
            MomentVariant("comeback_4", false, EmotionType.Teaching, "Don't lose the progress", "You're {remaining} away on {bite}. Finish it?", "Finish now", "Later"),
            MomentVariant("comeback_5", false, EmotionType.Explaining, "So close!", "Complete {bite} to bank the win.", "Continue", "Later"),
        )

    private val EXIT_INCOMPLETE =
        listOf(
            MomentVariant("exit_1", false, EmotionType.Explaining, "Leaving already?", "You've got {pending} tasks left today. Finishing them climbs you in {league}.", "Keep going", "Leave anyway"),
            MomentVariant("exit_2", false, EmotionType.Confused, "One more?", "{pending} tasks between you and a better rank in {league}.", "Keep going", "Leave anyway"),
            MomentVariant("exit_3", false, EmotionType.Neutral, "Rank #{rank} is close", "Finish {pending} tasks to move up in {league}.", "Keep going", "Leave anyway"),
            MomentVariant("exit_4", false, EmotionType.Explaining, "Don't stop now", "{pending} left today — and the league resets soon.", "Keep going", "Leave anyway"),
            MomentVariant("exit_5", false, EmotionType.Confused, "Hold on!", "You're {pending} tasks from a stronger {league} finish.", "Keep going", "Leave anyway"),
        )

    private val PLANT_COMPLETED =
        listOf(
            MomentVariant(
                "plant_done_1",
                true,
                EmotionType.Celebrating,
                "Planted!",
                "A {item} for the {place}.\n{planted} planted · {remainingInPlace} more here · {remainingScenes} places left to explore",
                "Keep growing",
            ),
            MomentVariant(
                "plant_done_2",
                true,
                EmotionType.Happy,
                "Look at that!",
                "Your {place} has a new {item}.\n{planted} so far · {remainingInPlace} to fill this place · {remainingScenes} scenes ahead",
                "Nice",
            ),
            MomentVariant(
                "plant_done_3",
                true,
                EmotionType.Surprised,
                "It landed!",
                "A {item} just took root in the {place}.\n{planted} planted · {remainingInPlace} more to go here",
                "Continue",
            ),
            MomentVariant(
                "plant_done_4",
                true,
                EmotionType.Celebrating,
                "Growing!",
                "One more {item} in the {place}.\n{planted} total · {remainingScenes} places left to explore",
                "Keep going",
            ),
            MomentVariant(
                "plant_done_5",
                true,
                EmotionType.Happy,
                "Fresh growth!",
                "The {place} looks better with that {item}.\n{planted} planted · {remainingInPlace} more here",
                "Onwards",
            ),
        )

    private val PLACE_COMPLETED =
        listOf(
            MomentVariant(
                "place_done_1",
                true,
                EmotionType.Celebrating,
                "Place complete!",
                "The {place} is full — all {planted} plants so far.\n{remainingScenes} places left to explore.",
                "Explore",
            ),
            MomentVariant(
                "place_done_2",
                true,
                EmotionType.Happy,
                "What a view!",
                "You filled the whole {place} ({planted} planted).\n{remainingScenes} scenes ahead.",
                "Continue",
            ),
            MomentVariant(
                "place_done_3",
                true,
                EmotionType.Surprised,
                "Incredible!",
                "Twelve in the {place}. {planted} total · {remainingScenes} places left.",
                "Keep going",
            ),
            MomentVariant(
                "place_done_4",
                true,
                EmotionType.Celebrating,
                "Full bloom!",
                "The {place} is complete ({planted} planted).\n{remainingScenes} new places to discover.",
                "Next",
            ),
            MomentVariant(
                "place_done_5",
                true,
                EmotionType.Happy,
                "Mission done!",
                "Every spot in the {place} is yours. {planted} planted · {remainingScenes} scenes left.",
                "Onwards",
            ),
        )

    private data class KnText(
        val headline: String,
        val body: String,
        val primaryCta: String,
        val secondaryCta: String? = null,
    )

    private val KN_TEXT: Map<String, KnText> =
        mapOf(
            "sim_done_1" to KnText("ಅದ್ಭುತ!", "{bite} ಪೂರ್ಣ — +{gems} ರತ್ನಗಳು.", "ಮುಂದುವರಿಸಿ"),
            "sim_done_2" to KnText("ಸಂಪೂರ್ಣ!", "ಇನ್ನೊಂದು ಅಂಶ ಪೂರ್ಣಗೊಂಡಿದೆ.", "ಮುಂದೆ"),
            "sim_done_3" to KnText("ನೀವು ಅದ್ಭುತ!", "{bite} ಪೂರ್ಣ. ಉತ್ತಮ ಕೇಂದ್ರೀಕರಣ.", "ಮುಂದುವರಿಸಿ"),
            "sim_done_4" to KnText("ಚೆನ್ನಾಗಿ!", "ಸಿಮ್ಯುಲೇಶನ್ ಪೂರ್ಣ — ಮುಂದಿನದು ಕಾಯುತ್ತಿದೆ.", "ಹೋಗೋಣ"),
            "sim_done_5" to KnText("ಹಂತ ಪೂರ್ಣ!", "+{gems} ರತ್ನಗಳು. ಜೋಶ ನಿಮ್ಮದು.", "ಮುಂದುವರಿಸಿ"),
            "study_done_1" to KnText("ಅದ್ಭುತ!", "{bite} ಮುಗಿಸಿದ್ದೀರಿ. +{gems} ರತ್ನಗಳು.", "ಮುಂದುವರಿಸಿ"),
            "study_done_2" to KnText("ಪೂರ್ಣ!", "ಅಧ್ಯಯನ ಅವಧಿ ಪೂರ್ಣ — ಅದ್ಭುತ.", "ಮುಂದೆ"),
            "study_done_3" to KnText("ಚುರುಕಾದ ಕೆಲಸ!", "{bite} ಅರ್ಥವಾಯಿತು. ಮುಂದೆ!", "ಮುಂದುವರಿಸಿ"),
            "study_done_4" to KnText("ನಿಮಗೆ ಸಾಧ್ಯ!", "ಇನ್ನೊಂದು ಸಂಕಲ್ಪ ಸೇರಿತು.", "ಹೋಗೋಣ"),
            "study_done_5" to KnText("ಜ್ಞಾನ ಅನ್ಲಾಕ್!", "{bite} ಮುಗಿಸಿದಕ್ಕೆ +{gems} ರತ್ನಗಳು.", "ಮುಂದುವರಿಸಿ"),
            "day_done_1" to KnText("ದಿನ ಪೂರ್ಣ!", "ಇಂದಿನ ಎಲ್ಲಾ ಕಾರ್ಯಗಳು ಮುಗಿದವು. +{gems} ರತ್ನಗಳು.", "ಮುಗಿದಿದೆ"),
            "day_done_2" to KnText("ಪರಿಪೂರ್ಣ ದಿನ!", "ಇಂದಿನ ಪ್ರಯೋಗವನ್ನು ಸಂಪೂರ್ಣವಾಗಿ ಮುಗಿಸಿದ್ದೀರಿ.", "ಮುಗಿದಿದೆ"),
            "day_done_3" to KnText("ಪೂರ್ಣ ಅಂಕ!", "ಇಂದಿನ ಎಲ್ಲಾ ಕಾರ್ಯಗಳು ಮುಗಿದಿವೆ. +{gems} ರತ್ನಗಳು.", "ಮುಗಿದಿದೆ"),
            "day_done_4" to KnText("ನಿಲ್ಲಲಾಗದ!", "ಇಂದು 100% ಪೂರ್ಣ. ನಾಳೆ ಮತ್ತೆ.", "ಮುಗಿದಿದೆ"),
            "day_done_5" to KnText("ಸ್ಟ್ರೀಕ್ ಬೂಸ್ಟ್!", "ದಿನ ಪೂರ್ಣ — ನಿಮ್ಮ ಸ್ಟ್ರೀಕ್ ಧನ್ಯವಾದ.", "ಮುಗಿದಿದೆ"),
            "comeback_1" to KnText("ಬಹುತೇಕ ಆಗಿತು", "{bite} ಮುಗಿಸಲು {remaining} ಬಾಕಿ. ಮುಗಿಸುವಿರಾ?", "ಈಗ ಮುಗಿಸಿ", "ನಂತರ"),
            "comeback_2" to KnText("ಮುಗಿಸೋಣ", "{bite} ಅರ್ಧದಲ್ಲಿಯೇ ಬಿಟ್ಟಿದ್ದೀರಿ — ಎರಡು ನಿಮಿಷ.", "ಈಗ ಮುಗಿಸಿ", "ನಂತರ"),
            "comeback_3" to KnText("ಮುಂದುವರಿಸಿ", "{bite} ಮುಗಿಸಲು {remaining} ಹಂತಗಳು.", "ಮುಂದುವರಿಸಿ", "ನಂತರ"),
            "comeback_4" to KnText("ಪ್ರಗತಿ ಕಳೆದುಕೊಳ್ಳಬೇಡಿ", "{bite} ನಲ್ಲಿ {remaining} ಬಾಕಿ. ಮುಗಿಸುವಿರಾ?", "ಈಗ ಮುಗಿಸಿ", "ನಂತರ"),
            "comeback_5" to KnText("ಬಹುತೇಕ!", "{bite} ಮುಗಿಸಿ ವಿಜಯ ಸಂಪಾದಿಸಿ.", "ಮುಂದುವರಿಸಿ", "ನಂತರ"),
            "exit_1" to KnText("ಈಗಲೇ ಹೊರಗೆ?", "ಇಂದು {pending} ಕಾರ್ಯಗಳು ಬಾಕಿ. ಮುಗಿಸಿದರೆ {league} ನಲ್ಲಿ ಮೇಲೇರುವಿರಿ.", "ಮುಂದುವರಿಸಿ", "ಹೊರಗೆ ಹೋಗಿ"),
            "exit_2" to KnText("ಇನ್ನೊಂದು?", "{league} ನಲ್ಲಿ ಉತ್ತಮ ಶ್ರೇಣಿಗೆ {pending} ಕಾರ್ಯಗಳು.", "ಮುಂದುವರಿಸಿ", "ಹೊರಗೆ ಹೋಗಿ"),
            "exit_3" to KnText("#{rank} ಶ್ರೇಣಿ ಹತ್ತಿರ", "{league} ನಲ್ಲಿ ಮೇಲಕ್ಕೆ {pending} ಕಾರ್ಯಗಳು.", "ಮುಂದುವರಿಸಿ", "ಹೊರಗೆ ಹೋಗಿ"),
            "exit_4" to KnText("ನಿಲ್ಲಬೇಡಿ", "ಇಂದು {pending} ಬಾಕಿ — ಲೀಗ್ ಶೀಘ್ರ ರೀಸೆಟ್.", "ಮುಂದುವರಿಸಿ", "ಹೊರಗೆ ಹೋಗಿ"),
            "exit_5" to KnText("ನಿಲ್ಲಿ!", "{league} ನಲ್ಲಿ ಬಲವಾದ ಮುಕ್ತಾಯಕ್ಕೆ {pending} ಕಾರ್ಯಗಳು.", "ಮುಂದುವರಿಸಿ", "ಹೊರಗೆ ಹೋಗಿ"),
            "plant_done_1" to
                KnText(
                    "ಬೆಳೆದಿದೆ!",
                    "{place} ನಲ್ಲಿ {item}.\n{planted} ಬೆಳೆದಿದೆ · ಇಲ್ಲಿ ಇನ್ನೂ {remainingInPlace} · {remainingScenes} ಸ್ಥಳಗಳು ಬಾಕಿ",
                    "ಬೆಳೆಯಲು ಮುಂದುವರಿಸಿ",
                ),
            "plant_done_2" to
                KnText(
                    "ನೋಡಿ!",
                    "ನಿಮ್ಮ {place} ನಲ್ಲಿ ಹೊಸ {item}.\n{planted} ಇಲ್ಲಿಯವರೆಗೆ · ಇಲ್ಲಿ {remainingInPlace} · {remainingScenes} ದೃಶ್ಯಗಳು ಮುಂದೆ",
                    "ಚೆನ್ನಾಗಿದೆ",
                ),
            "plant_done_3" to
                KnText(
                    "ಬಂದು ಬಿತ್ತು!",
                    "{place} ನಲ್ಲಿ {item} ಬೇರೂರಿತು.\n{planted} ಬೆಳೆದಿದೆ · ಇಲ್ಲಿ ಇನ್ನೂ {remainingInPlace}",
                    "ಮುಂದುವರಿಸಿ",
                ),
            "plant_done_4" to
                KnText(
                    "ಬೆಳೆಯುತ್ತಿದೆ!",
                    "{place} ನಲ್ಲಿ ಇನ್ನೊಂದು {item}.\n{planted} ಒಟ್ಟು · {remainingScenes} ಸ್ಥಳಗಳು ಬಾಕಿ",
                    "ಮುಂದುವರಿಸಿ",
                ),
            "plant_done_5" to
                KnText(
                    "ಹೊಸ ಬೆಳೆ!",
                    "ಆ {item} ನಿಂದ {place} ಇನ್ನೂ ಚೆನ್ನಾಗಿ ಕಾಣುತ್ತದೆ.\n{planted} ಬೆಳೆದಿದೆ · ಇಲ್ಲಿ ಇನ್ನೂ {remainingInPlace}",
                    "ಮುಂದೆ",
                ),
            "place_done_1" to
                KnText(
                    "ಸ್ಥಳ ಪೂರ್ಣ!",
                    "{place} ತುಂಬಿದೆ — {planted} ಬೆಳೆಗಳು.\n{remainingScenes} ಸ್ಥಳಗಳು ಬಾಕಿ.",
                    "ಅನ್ವೇಷಿಸಿ",
                ),
            "place_done_2" to
                KnText(
                    "ಅದ್ಭುತ ದೃಶ್ಯ!",
                    "ನೀವು {place} ({planted} ಬೆಳೆದಿದೆ) ಸಂಪೂರ್ಣ ತುಂಬಿದ್ದೀರಿ.\n{remainingScenes} ದೃಶ್ಯಗಳು ಮುಂದೆ.",
                    "ಮುಂದುವರಿಸಿ",
                ),
            "place_done_3" to
                KnText(
                    "ಅದ್ಭುತ!",
                    "{place} ನಲ್ಲಿ ಹನ್ನೆರಡು. {planted} ಒಟ್ಟು · {remainingScenes} ಸ್ಥಳಗಳು ಬಾಕಿ.",
                    "ಮುಂದುವರಿಸಿ",
                ),
            "place_done_4" to
                KnText(
                    "ಪೂರ್ಣ ಅರಳಿಕೆ!",
                    "{place} ಪೂರ್ಣ ({planted} ಬೆಳೆದಿದೆ).\n{remainingScenes} ಹೊಸ ಸ್ಥಳಗಳು.",
                    "ಮುಂದೆ",
                ),
            "place_done_5" to
                KnText(
                    "ಕಾರ್ಯ ಪೂರ್ಣ!",
                    "{place} ನಲ್ಲಿ ಪ್ರತಿ ಜಾಗ ನಿಮ್ಮದು. {planted} ಬೆಳೆದಿದೆ · {remainingScenes} ದೃಶ್ಯಗಳು ಬಾಕಿ.",
                    "ಮುಂದೆ",
                ),
        )
}
