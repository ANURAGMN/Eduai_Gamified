package com.ncert7.aitutorandlab.utils

import com.anurag.eduai.uikit.navigation.EduBottomNavItem
import com.ncert7.aitutorandlab.ui.screens.garden.AvatarGardenSegment

data class NavWalkStep(
    val route: String,
    val title: String,
    val body: String,
    /** When set, the Avatar tab is switched to this segment while the card is shown. */
    val segment: AvatarGardenSegment? = null,
)

/** First-run bottom-nav walkthrough titles/bodies + chrome (EN / KN). */
object NavTourCopy {
    fun steps(languageCode: String): List<NavWalkStep> =
        if (isKannadaLanguage(languageCode)) kannadaSteps() else englishSteps()

    fun skipLabel(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಬಿಟ್ಟುಬಿಡಿ" else "Skip"

    fun backLabel(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಹಿಂದೆ" else "Back"

    fun nextLabel(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಮುಂದೆ" else "Next"

    fun doneLabel(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಸರಿ" else "Got it"

    fun stepOfTotal(languageCode: String): (Int, Int) -> String =
        if (isKannadaLanguage(languageCode)) {
            { step, total -> "${step + 1} / $total" }
        } else {
            { step, total -> "${step + 1} of $total" }
        }

    private fun englishSteps(): List<NavWalkStep> =
        listOf(
            NavWalkStep(
                EduBottomNavItem.Plan.route,
                "Your exam planner",
                "Your whole plan, day by day. Follow it to study a little every day and stay on track for your exam.",
            ),
            NavWalkStep(
                EduBottomNavItem.Avatar.route,
                "Your world",
                "Grow a garden or a space colony as you learn — every task you finish adds to the scene.",
                segment = AvatarGardenSegment.Scene,
            ),
            NavWalkStep(
                EduBottomNavItem.Avatar.route,
                "Choose your journey",
                "Switch between Garden and Space, and travel to new places as you unlock them.",
                segment = AvatarGardenSegment.Journey,
            ),
            NavWalkStep(
                EduBottomNavItem.Avatar.route,
                "Customise your tutor",
                "Give your AI tutor a look you like, and unlock more styles as you keep learning.",
                segment = AvatarGardenSegment.Look,
            ),
            NavWalkStep(
                EduBottomNavItem.Leagues.route,
                "Leaderboard & leagues",
                "Earn XP and climb weekly leagues with friends — ranked on effort, never on grades.",
            ),
            NavWalkStep(
                EduBottomNavItem.Home.route,
                "You're all set!",
                "This is home — your daily focus starts here. Jump into your first task whenever you're ready.",
            ),
        )

    private fun kannadaSteps(): List<NavWalkStep> =
        listOf(
            NavWalkStep(
                EduBottomNavItem.Plan.route,
                "ನಿಮ್ಮ ಪರೀಕ್ಷಾ ಯೋಜನೆ",
                "ದಿನದಿಂದ ದಿನಕ್ಕೆ ನಿಮ್ಮ ಸಂಪೂರ್ಣ ಯೋಜನೆ. ಪ್ರತಿದಿನ ಸ್ವಲ್ಪ ಅಧ್ಯಯನ ಮಾಡಿ ಪರೀಕ್ಷೆಗೆ ಸಿದ್ಧರಾಗಿ.",
            ),
            NavWalkStep(
                EduBottomNavItem.Avatar.route,
                "ನಿಮ್ಮ ಜಗತ್ತು",
                "ಕಲಿಯುತ್ತಾ ನಿಮ್ಮ ತೋಟ ಅಥವಾ ಬಾಹ್ಯಾಕಾಶ ವಸಾಹತನ್ನು ಬೆಳೆಸಿ — ಪೂರ್ಣಗೊಳಿಸಿದ ಪ್ರತಿ ಕಾರ್ಯವೂ ದೃಶ್ಯಕ್ಕೆ ಸೇರುತ್ತದೆ.",
                segment = AvatarGardenSegment.Scene,
            ),
            NavWalkStep(
                EduBottomNavItem.Avatar.route,
                "ನಿಮ್ಮ ಪಯಣ ಆರಿಸಿ",
                "ತೋಟ ಮತ್ತು ಬಾಹ್ಯಾಕಾಶದ ನಡುವೆ ಬದಲಿಸಿ, ಅನ್‌ಲಾಕ್ ಆದಂತೆ ಹೊಸ ಸ್ಥಳಗಳಿಗೆ ಪಯಣಿಸಿ.",
                segment = AvatarGardenSegment.Journey,
            ),
            NavWalkStep(
                EduBottomNavItem.Avatar.route,
                "ನಿಮ್ಮ ಶಿಕ್ಷಕನನ್ನು ಹೊಂದಿಸಿ",
                "ನಿಮ್ಮ AI ಶಿಕ್ಷಕನಿಗೆ ಇಷ್ಟದ ರೂಪ ನೀಡಿ, ಕಲಿಯುತ್ತಾ ಹೆಚ್ಚಿನ ಶೈಲಿಗಳನ್ನು ಅನ್‌ಲಾಕ್ ಮಾಡಿ.",
                segment = AvatarGardenSegment.Look,
            ),
            NavWalkStep(
                EduBottomNavItem.Leagues.route,
                "ಲೀಡರ್‌ಬೋರ್ಡ್ ಮತ್ತು ಲೀಗ್‌ಗಳು",
                "XP ಗಳಿಸಿ ವಾರದ ಲೀಗ್‌ಗಳಲ್ಲಿ ಏರಿ — ಅಂಕಗಳಲ್ಲ, ಪ್ರಯತ್ನದಲ್ಲಿ ಸ್ಪರ್ಧೆ.",
            ),
            NavWalkStep(
                EduBottomNavItem.Home.route,
                "ಸಿದ್ಧರಾಗಿದ್ದೀರಿ!",
                "ಇದು ಹೋಮ್ — ನಿಮ್ಮ ದೈನಂದಿನ ಕೇಂದ್ರ ಇಲ್ಲಿಂದ. ಸಿದ್ಧವಾದಾಗ ಮೊದಲ ಕಾರ್ಯಕ್ಕೆ ಹೋಗಿ.",
            ),
        )
}
