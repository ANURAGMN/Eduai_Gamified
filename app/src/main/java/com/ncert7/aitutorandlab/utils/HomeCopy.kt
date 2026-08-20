package com.ncert7.aitutorandlab.utils

/** Home screen bilingual chrome and tutor copy (resolved at display time). */
object HomeCopy {
    fun tutorTitle(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ನಿಮ್ಮ ಶಿಕ್ಷಕ" else "Your tutor"

    fun tutorMessage(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) {
            "ಇಂದಿನ ಕ್ವೆಸ್ಟ್‌ಗಳಿಗೆ ಸಿದ್ಧವೇ? ಹೋಗೋಣ!"
        } else {
            "Ready for today's quests? Let's go!"
        }

    fun youtubeSectionTitle(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ವೀಡಿಯೋ ಪಾಠಗಳು" else "Video lessons"

    fun youtubePlaybackFailedMessage(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) {
            "ಈ ವೀಡಿಯೋವನ್ನು ಆ್ಯಪ್‌ನಲ್ಲಿ ಪ್ಲೇ ಮಾಡಲು ಸಾಧ್ಯವಾಗಲಿಲ್ಲ."
        } else {
            "This video couldn't be played in the app."
        }

    fun openInYoutubeLabel(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "YouTube ನಲ್ಲಿ ತೆರೆಯಿರಿ" else "Open in YouTube"

    fun streakCaption(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಸರಣಿ" else "Streak"

    fun gemsCaption(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ರತ್ನಗಳು" else "Gems"

    fun gemsHowToEarnTitle(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ರತ್ನಗಳನ್ನು ಹೇಗೆ ಗಳಿಸುವುದು" else "How to earn gems"

    /**
     * Explains gem sources shown when the home Gems chip is tapped.
     * [gemsPerMandatoryAd] / [trialsPerAd] should match [EconomyConfig].
     */
    fun gemsHowToEarnBody(
        languageCode: String,
        gemsPerMandatoryAd: Int,
        trialsPerAd: Int,
    ): String =
        if (isKannadaLanguage(languageCode)) {
            "ರತ್ನಗಳು ಪ್ರತಿ ಕಾರ್ಯದಿಂದ ಬರುವುದಿಲ್ಲ — ಅವುಗಳನ್ನು ಹೀಗೆ ಗಳಿಸಿ:\n\n" +
                "• ದೈನಂದಿನ ಕ್ವೆಸ್ಟ್‌ಗಳನ್ನು ಮುಗಿಸಿ, ನಂತರ ಬಹುಮಾನಕ್ಕೆ ಚಿಕ್ಕ ವೀಡಿಯೋ ನೋಡಿ\n" +
                "• ಪ್ರತಿ $trialsPerAd ಯೋಜನೆ ಕಾರ್ಯಗಳ ನಂತರ ಚಿಕ್ಕ ವೀಡಿಯೋ ನೋಡಿ (+$gemsPerMandatoryAd ರತ್ನ)\n" +
                "• ಸ್ನೇಹಿತರನ್ನು ಆಹ್ವಾನಿಸಿ"
        } else {
            "Gems aren't awarded for every task — here's how to earn them:\n\n" +
                "• Finish daily quests, then watch a short video to claim\n" +
                "• After every $trialsPerAd plan tasks, watch a short video (+$gemsPerMandatoryAd gems)\n" +
                "• Invite friends"
        }

    fun gemsHowToEarnGotIt(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಸರಿ" else "Got it"

    /** Short tier label for the home top-bar caption. */
    fun leagueCaption(tierStorageKey: String?, languageCode: String): String {
        val key = tierStorageKey?.trim()?.uppercase().orEmpty()
        val kannada = isKannadaLanguage(languageCode)
        return when (key) {
            "GOLD" -> if (kannada) "ಚಿನ್ನ" else "Gold"
            "SILVER" -> if (kannada) "ಬೆಳ್ಳಿ" else "Silver"
            else -> if (kannada) "ಕಂಚು" else "Bronze"
        }
    }

    fun subjectsSectionTitle(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ವಿಷಯಗಳು" else "Subjects"

    /** Second line on a subject row, e.g. "14 chapters" / "1 chapter". */
    fun chapterCount(languageCode: String, count: Int): String =
        if (isKannadaLanguage(languageCode)) {
            "$count ಅಧ್ಯಾಯ${if (count == 1) "" else "ಗಳು"}"
        } else {
            "$count chapter${if (count == 1) "" else "s"}"
        }

    // —— Plan trail ——
    fun planSectionTitle(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ನಿಮ್ಮ ಪರೀಕ್ಷಾ ತಯಾರಿ ಯೋಜನೆ" else "Your exam prep plan"

    fun seeAllLabel(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಎಲ್ಲಾ ನೋಡಿ" else "See all"

    fun planAddLabel(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಯೋಜನೆ ಸೇರಿಸಿ" else "Add plan"

    fun planEmptyTitle(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಇನ್ನೂ ಪರೀಕ್ಷಾ ಯೋಜನೆ ಇಲ್ಲ" else "No exam plan yet"

    fun planEmptyBody(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) {
            "ಪರೀಕ್ಷಾ ಪ್ರಕಾರ, ಅಧ್ಯಾಯಗಳು ಮತ್ತು ದೈನಂದಿನ ಅಧ್ಯಯನ ಸಮಯವನ್ನು ಹೊಂದಿಸಿ."
        } else {
            "Set exam type, chapters, and daily study time to build your prep trail."
        }

    fun planTodayLabel(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಇಂದು" else "Today"

    fun planDayLabel(languageCode: String, day: Int): String =
        if (isKannadaLanguage(languageCode)) "ದಿನ $day" else "Day $day"

    fun planGrowHint(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) {
            "ಒಂದು ದಿನ ಮುಗಿಸಿ ನಿಮ್ಮ ಜಗತ್ತನ್ನು ಬೆಳೆಸಿ — ಪ್ರತಿ ಕಾರ್ಯವೂ ಹೊಸದನ್ನು ನೆಡುತ್ತದೆ."
        } else {
            "Finish a day to grow your world — every task plants something new."
        }

    // —— Quests / revision / bookmarks / friends ——
    fun questsSectionTitle(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಇಂದಿನ ಕ್ವೆಸ್ಟ್‌ಗಳು" else "Today's quests"

    fun questsScreenSubtitle(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) {
            "ರತ್ನಗಳನ್ನು ಗಳಿಸಲು ದೈನಂದಿನ ಕ್ವೆಸ್ಟ್‌ಗಳನ್ನು ಪೂರ್ಣಗೊಳಿಸಿ. ಬಹುಮಾನ ಪಡೆಯಲು ಚಿಕ್ಕ ವೀಡಿಯೋ ನೋಡಿ."
        } else {
            "Complete daily quests to earn gems. Watch a short video to claim rewards."
        }

    fun questsEmptyHint(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) {
            "ಹೋಮ್ ತೆರೆದಾಗ ಅಥವಾ ಇಂದು ಕಲಿಯಲು ಪ್ರಾರಂಭಿಸಿದಾಗ ಕ್ವೆಸ್ಟ್‌ಗಳು ರಿಫ್ರೆಶ್ ಆಗುತ್ತವೆ."
        } else {
            "Quests refresh when you open Home or start learning today."
        }

    fun revisionSectionTitle(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಪುನರಾವಲೋಕನ ಬೇಕು" else "Needs revision"

    fun lastQuizPercent(languageCode: String, score: Int): String =
        revisionLastQuizTemplate(languageCode).replace("%d", score.toString())

    fun revisionLastQuizTemplate(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಕೊನೆಯ ರಸಪ್ರಶ್ನೆ %d%%" else "%d%% last quiz"

    fun planDayPrefix(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ದಿನ" else "Day"

    fun bookmarksSectionTitle(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಇತ್ತೀಚೆಗೆ ಪೂರ್ಣಗೊಂಡಿದ್ದು" else "Recently completed"

    fun seeAllCountLabel(languageCode: String, count: Int): String =
        if (isKannadaLanguage(languageCode)) "ಎಲ್ಲಾ ನೋಡಿ ($count)" else "See all ($count)"

    fun friendsSectionTitle(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಸ್ನೇಹಿತರ ಅಪ್‌ಡೇಟ್‌ಗಳು" else "Friends' updates"

    fun friendsAddLabel(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಸೇರಿಸಿ" else "Add"

    fun friendsAddCardTitle(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಸ್ನೇಹಿತರನ್ನು ಸೇರಿಸಿ" else "Add a friend"

    fun friendsAddCardBody(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) {
            "ಖಾತೆಗಳನ್ನು ಲಿಂಕ್ ಮಾಡಲು ಅವರ ಸ್ನೇಹಿತ ಕೋಡ್ ನಮೂದಿಸಿ."
        } else {
            "Enter their friend code to link accounts."
        }

    fun inviteFriendsTitle(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಸ್ನೇಹಿತರನ್ನು ಆಹ್ವಾನಿಸಿ" else "Invite friends"

    fun inviteFriendsSubtitle(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ನಿಮ್ಮ ಕೋಡ್ ಹಂಚಿಕೊಳ್ಳಿ" else "Share your code"

    fun shareLabel(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಹಂಚಿ" else "Share"

    fun acceptLabel(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಸ್ವೀಕರಿಸಿ" else "Accept"

    fun bookmarkTypeConcept(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಪರಿಕಲ್ಪನೆ" else "Concept"

    fun bookmarkTypeSimulation(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಸಿಮ್ಯುಲೇಶನ್" else "Simulation"

    fun bookmarkTypeGeneric(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಬುಕ್‌ಮಾರ್ಕ್" else "Bookmark"

    fun bookmarkPlaceholderKey(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಇನ್ನೂ ಯಾವುದೇ ಪೂರ್ಣಗೊಂಡ ವಿಷಯಗಳಿಲ್ಲ" else "No completed topics yet"

    // —— NCERT textbooks ——
    fun textbooksSectionTitle(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಎನ್‌ಸಿಇಆರ್‌ಟಿ ಪಠ್ಯಪುಸ್ತಕಗಳು" else "NCERT textbooks"

    fun textbooksSectionSubtitle(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) {
            "ಅಧಿಕೃತ ವಿಜ್ಞಾನ ಮತ್ತು ಗಣಿತ ಪುಸ್ತಕಗಳನ್ನು ಓದಿ"
        } else {
            "Read the official Science & Maths books"
        }

    fun textbookScienceTitle(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ವಿಜ್ಞಾನ" else "Science"

    fun textbookMathPart1Title(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಗಣಿತ – ಭಾಗ 1" else "Mathematics – Part 1"

    fun textbookMathPart2Title(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಗಣಿತ – ಭಾಗ 2" else "Mathematics – Part 2"

    fun textbookClassSubtitle(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "7ನೇ ತರಗತಿ · ಕನ್ನಡ" else "Class 7 · English"
}
