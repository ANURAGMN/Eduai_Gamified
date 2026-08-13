package com.ncert7.aitutorandlab.utils

import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemKind
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemStatus

/** Bilingual copy for exam-trial flows (dialogs, overlays, labels). */
object TrialCopy {
    private fun kn(languageCode: String): Boolean = isKannadaLanguage(languageCode)

    fun itemTitle(
        languageCode: String,
        chapterName: String,
        kind: String,
        conceptName: String,
    ): String {
        val kindLabel = kindLabel(languageCode, kind)
        return "$chapterName · $kindLabel · $conceptName"
    }

    fun kindLabel(languageCode: String, kind: String): String =
        when (kind) {
            PlanTrialItemKind.SIM_AGENT ->
                if (kn(languageCode)) "ಸಿಮ್ ಏಜೆಂಟ್" else "Sim agent"
            PlanTrialItemKind.SIM_URL ->
                if (kn(languageCode)) "ಸಿಮ್ಯುಲೇಶನ್" else "Simulation"
            PlanTrialItemKind.STUDY ->
                if (kn(languageCode)) "ಅಧ್ಯಯನ" else "Study"
            PlanTrialItemKind.REVISION ->
                if (kn(languageCode)) "ಪರಿಷ್ಕರಣೆ" else "Revision"
            PlanTrialItemKind.MATH ->
                if (kn(languageCode)) "ಗಣಿತ ಸಮಸ್ಯೆ" else "Math problem"
            else -> kind
        }

    fun statusLabel(languageCode: String, status: String): String =
        when (status) {
            PlanTrialItemStatus.DONE ->
                if (kn(languageCode)) "ಪೂರ್ಣ" else "Done"
            PlanTrialItemStatus.IN_PROGRESS ->
                if (kn(languageCode)) "ಪ್ರಗತಿಯಲ್ಲಿ" else "In progress"
            else ->
                if (kn(languageCode)) "ಬಾಕಿ" else "Pending"
        }

    fun progressLabel(languageCode: String, completed: Int, required: Int): String =
        if (kn(languageCode)) {
            "$completed / $required ಜ್ಞಾನ ತುಣುಕುಗಳು"
        } else {
            "$completed / $required bites"
        }

    fun knowledgeBitesLabel(languageCode: String, count: Int): String =
        if (kn(languageCode)) {
            if (count == 1) "1 ಜ್ಞಾನ ತುಣುಕು" else "$count ಜ್ಞಾನ ತುಣುಕುಗಳು"
        } else {
            if (count == 1) "1 more knowledge bite" else "$count more knowledge bites"
        }

    fun partialReturnSim(languageCode: String): Pair<String, String> =
        if (kn(languageCode)) {
            "ಬಹುತೇಕ ಆಗಿತು!" to
                "ಮುಂದುವರಿಸಿ — ಗುರಿಯನ್ನು ತಲುಪಲು ಇನ್ನೂ ಕೆಲವು ಜ್ಞಾನ ತುಣುಕುಗಳು ಬಾಕಿ. " +
                "ಮುಗಿಸಿದ ನಂತರ ನಾವು ಆಚರಿಸಿ ಮುಂದಿನ ಹಂತಕ್ಕೆ ಹೋಗುತ್ತೇವೆ."
        } else {
            "Almost there!" to
                "Keep exploring — a few more knowledge bites until you reach the goal. " +
                "We'll celebrate and move on once you finish."
        }

    /** Shown ~5 minutes after the learner opens a trial simulation. */
    fun simTimeExplorePrompt(languageCode: String): Pair<String, String> =
        if (kn(languageCode)) {
            "ಚೆನ್ನಾಗಿ ಅನ್ವೇಷಿಸಿದ್ದೀರಿ! 🎉" to
                "ನೀವು ಈ ಪ್ರಯೋಗವನ್ನು ನಿಜವಾಗಿಯೂ ಆಳವಾಗಿ ನೋಡಿದ್ದೀರಿ — ನಿಯಂತ್ರಣಗಳನ್ನು ಪ್ರಯತ್ನಿಸಿ, " +
                "ಪ್ರತಿ ಬಾರಿ ಏನಾಗುತ್ತದೆ ಎಂಬುದನ್ನು ಗಮನಿಸಿದ್ದೀರಿ. ಇದೇ ವಿಜ್ಞಾನಿಗಳು ಕಲಿಯುವ ರೀತಿ. " +
                "ಈಗ ನಿಮ್ಮ ಯೋಜನೆಯ ಮುಂದಿನ ಹಂತಕ್ಕೆ ಹೋಗಲು ಸಿದ್ಧವೇ, ಅಥವಾ ಇಷ್ಟವಿದ್ದರೆ ಇನ್ನೂ ಸ್ವಲ್ಪ ಪ್ರಯೋಗ ಮಾಡಿ."
        } else {
            "Nicely explored! 🎉" to
                "You've really dug into this experiment — trying the controls and watching what changes each time. " +
                "That's exactly how scientists learn. Ready to move on to the next step in your plan, " +
                "or would you like to keep experimenting here a little longer?"
        }

    fun simProceedLabel(languageCode: String): String =
        if (kn(languageCode)) "ಮುಂದಿನ ಅಂಶ" else "Next item"

    fun simKeepExploringLabel(languageCode: String): String =
        if (kn(languageCode)) "ಅನ್ವೇಷಿಸಿ" else "Keep exploring"

    /** Recurring time-check prompt for agent interactions (study / revision / sim agent). */
    fun agentTimeCheckPrompt(languageCode: String, inTrial: Boolean): Pair<String, String> =
        when {
            inTrial -> simTimeExplorePrompt(languageCode)
            kn(languageCode) ->
                "ಸ್ವಲ್ಪ ಸಮಯವಾಯಿತು" to
                    "ನೀವು ಇಲ್ಲಿ ಸ್ವಲ್ಪ ಸಮಯದಿಂದ ಇದ್ದೀರಿ. ಸಿದ್ಧವಾದಾಗ ಮುಗಿಸಿ, ಅಥವಾ ಮುಂದುವರಿಸಿ."
            else ->
                "You've been here a while" to
                    "Take a moment to wrap up when you're ready — or keep going."
        }

    fun agentProceedLabel(languageCode: String, inTrial: Boolean): String =
        if (inTrial) simProceedLabel(languageCode)
        else if (kn(languageCode)) "ಮುಗಿದಿದೆ" else "Done"

    fun agentKeepGoingLabel(languageCode: String): String =
        if (kn(languageCode)) "ಮುಂದುವರಿಸಿ" else "Keep going"

    fun partialReturnStudy(
        languageCode: String,
        remainingLabel: String,
        completed: Int,
        required: Int,
    ): Pair<String, String> =
        if (kn(languageCode)) {
            "ಮುಂದುವರಿಸಿ!" to
                "ಈ ಅಧ್ಯಯನವನ್ನು ಮುಗಿಸಲು $remainingLabel ಆಯ್ಕೆ ಮಾಡಿ " +
                "(${completed} / ${required} ತುಣುಕುಗಳು). " +
                "ಮುಂದಿನ ಅಂಶಕ್ಕೆ ಹೋಗುವ ಮೊದಲು ಇದನ್ನು ಪೂರ್ಣಗೊಳಿಸಿ."
        } else {
            "Keep going!" to
                "Pick up $remainingLabel to finish this session " +
                "($completed of $required bites so far). " +
                "Finish it before moving to the next item."
        }

    fun partialReturnDefault(languageCode: String): Pair<String, String> =
        if (kn(languageCode)) {
            "ಮುಂದುವರಿಸಿ!" to "ಮುಂದಿನ ಅಂಶಕ್ಕೆ ಹೋಗುವ ಮೊದಲು ಇದನ್ನು ಪೂರ್ಣಗೊಳಿಸಿ."
        } else {
            "Keep going!" to "Complete this item before moving to the next one."
        }

    fun advanceNiceWork(languageCode: String): String =
        if (kn(languageCode)) "ಅದ್ಭುತ!" else "Nice work!"

    fun advanceDayComplete(languageCode: String): String =
        if (kn(languageCode)) "ಇಂದಿನ ಪ್ರಯೋಗ ಪೂರ್ಣ!" else "Today's trial complete!"

    fun advanceRewardAdStarting(languageCode: String): String =
        if (kn(languageCode)) "ನಿಮ್ಮ ಬಹುಮಾನ ಜಾಹೀರಾತು 2 ಸೆಕೆಂಡುಗಳಲ್ಲಿ…" else "Your reward ad starts in 2 seconds…"

    fun advanceMandatoryAdSkipped(languageCode: String, gems: Int): String =
        if (kn(languageCode)) {
            "ನೀವು ಜಾಹೀರಾತನ್ನು ಪೂರ್ಣವಾಗಿ ನೋಡಲಿಲ್ಲ — +$gems ರತ್ನಗಳು ಸೇರಿಸಲಾಗಿಲ್ಲ."
        } else {
            "You didn't watch the full ad — +$gems gems weren't added."
        }

    fun advanceUpNext(languageCode: String, kindLabel: String, title: String): String =
        if (kn(languageCode)) {
            "ಮುಂದೆ: $kindLabel · $title"
        } else {
            "Up next: $kindLabel · $title"
        }

    fun advanceAllDone(languageCode: String): String =
        if (kn(languageCode)) "ಈ ದಿನದ ಎಲ್ಲಾ ಅಂಶಗಳನ್ನು ಪೂರ್ಣಗೊಳಿಸಿದ್ದೀರಿ." else "You finished every item for this day."

    fun exitHookTitle(languageCode: String, count: Int): String =
        if (kn(languageCode)) {
            if (count == 1) "1 ಪ್ರಯೋಗ ಅಂಶ ಬಾಕಿ" else "$count ಪ್ರಯೋಗ ಅಂಶಗಳು ಬಾಕಿ"
        } else {
            "$count trial${if (count == 1) "" else "s"} still waiting"
        }

    fun exitHookMessage(
        languageCode: String,
        potentialGems: Int,
        trialsPerAd: Int,
        leagueLine: String,
    ): String =
        if (kn(languageCode)) {
            "ಅವುಗಳನ್ನು ಪೂರ್ಣಗೊಳಿಸಿ +$potentialGems ರತ್ನಗಳನ್ನು ಪಡೆಯಿರಿ " +
                "(ಪ್ರತಿ $trialsPerAd ಪ್ರಯೋಗಗಳಿಗೆ ಒಂದು ಜಾಹೀರಾತು) ಮತ್ತು $leagueLine."
        } else {
            "Complete them to claim up to +$potentialGems gems " +
                "(watch an ad every $trialsPerAd trials) and $leagueLine."
        }

    fun exitLeagueFallback(languageCode: String): String =
        if (kn(languageCode)) "ಈ ವಾರ ಹೆಚ್ಚು XP ಗಳಿಸಲು ಮುಂದುವರಿಸಿ." else "Keep going to earn more XP this week."

    fun chapterClearedToast(languageCode: String): String =
        if (kn(languageCode)) "ಅಧ್ಯಾಯ ಪೂರ್ಣ! ಬಹುಮಾನ ಅನ್ಲಾಕ್ 🎉" else "Chapter cleared! Reward unlocked 🎉"

    fun dayCompleteToast(languageCode: String): String =
        if (kn(languageCode)) "ದಿನ ಪೂರ್ಣ — ಅದ್ಭುತ ಕೆಲಸ!" else "Day complete — brilliant work!"

    fun weeklyXpLabel(languageCode: String): String =
        if (kn(languageCode)) "ವಾರದ XP" else "Weekly XP"

    fun xpEarnedLabel(languageCode: String): String =
        if (kn(languageCode)) "XP ಗಳಿಸಿದ್ದು" else "XP earned"

    fun bonusXpLabel(languageCode: String): String =
        if (kn(languageCode)) "ಬೋನಸ್ XP" else "Bonus XP"

    fun gemsLabel(languageCode: String): String =
        if (kn(languageCode)) "ರತ್ನಗಳು" else "Gems"

    fun playingRewardAd(languageCode: String, gems: Int): String =
        if (kn(languageCode)) {
            "+$gems ರತ್ನಗಳನ್ನು ಪಡೆಯಲು ಜಾಹೀರಾತು ನಡೆಯುತ್ತಿದೆ…"
        } else {
            "Playing reward ad to claim +$gems gems…"
        }

    fun loadingRewardAd(languageCode: String): String =
        if (kn(languageCode)) "ಜಾಹೀರಾತು ಲೋಡ್ ಆಗುತ್ತಿದೆ…" else "Loading reward ad…"

    fun mandatoryAdWatch(languageCode: String, gems: Int): String =
        if (kn(languageCode)) {
            "+$gems ರತ್ನಗಳಿಗೆ ಜಾಹೀರಾತು ನೋಡಿ"
        } else {
            "Watch ad for +$gems gems"
        }

    fun skipMandatoryAd(languageCode: String, gems: Int): String =
        if (kn(languageCode)) {
            "+$gems ರತ್ನಗಳಿಲ್ಲದೆ ಮುಂದುವರಿಸಿ"
        } else {
            "Continue without +$gems gems"
        }

    fun doubleXpWatch(languageCode: String, amount: Int): String =
        if (kn(languageCode)) {
            "ಎರಡು ಪಟ್ಟು XP · ಜಾಹೀರಾತು ನೋಡಿ (+$amount ಬೋನಸ್)"
        } else {
            "Double XP · watch ad (+$amount bonus)"
        }

    fun doubleXpLoading(languageCode: String): String =
        if (kn(languageCode)) "ಎರಡು ಪಟ್ಟು XP · ಜಾಹೀರಾತು ಲೋಡ್…" else "Double XP · loading ad…"

    fun bonusXpAdded(languageCode: String, amount: Int): String =
        if (kn(languageCode)) {
            "+$amount ಬೋನಸ್ XP ನಿಮ್ಮ ಪ್ರೊಫೈಲ್‌ಗೆ ಸೇರಿಸಲಾಗಿದೆ"
        } else {
            "+$amount bonus XP added to your profile"
        }

    fun startingIn(languageCode: String, seconds: Int): String =
        if (kn(languageCode)) "ಪ್ರಾರಂಭ $seconds…" else "Starting in $seconds…"

    fun trialOverlayLabels(languageCode: String): com.anurag.eduai.uikit.components.PlanTrialOverlayLabels =
        com.anurag.eduai.uikit.components.PlanTrialOverlayLabels(
            weeklyXp = weeklyXpLabel(languageCode),
            xpEarned = xpEarnedLabel(languageCode),
            bonusXp = bonusXpLabel(languageCode),
            gems = gemsLabel(languageCode),
            playingRewardAd = { gems -> playingRewardAd(languageCode, gems) },
            loadingRewardAd = loadingRewardAd(languageCode),
            mandatoryAdWatch = { gems -> mandatoryAdWatch(languageCode, gems) },
            skipMandatoryAd = { gems -> skipMandatoryAd(languageCode, gems) },
            mandatoryAdSkipped = { gems -> advanceMandatoryAdSkipped(languageCode, gems) },
            doubleXpWatch = { amount -> doubleXpWatch(languageCode, amount) },
            doubleXpLoading = doubleXpLoading(languageCode),
            bonusXpAdded = { amount -> bonusXpAdded(languageCode, amount) },
            startingIn = { seconds -> startingIn(languageCode, seconds) },
        )

    fun trialScreenTitle(languageCode: String): String =
        if (kn(languageCode)) "ಪರೀಕ್ಷಾ ಪ್ರಯೋಗ" else "Exam trial"

    fun trialDayTitle(languageCode: String, dayIndex: Int, planTitle: String?): String {
        // Chapter trials use a synthetic negative dayIndex — show only the chapter name, no "Day".
        if (dayIndex < 0) return planTitle?.takeIf { it.isNotBlank() }.orEmpty()
        val dayLabel = if (kn(languageCode)) "ದಿನ $dayIndex" else "Day $dayIndex"
        return planTitle?.let { "$dayLabel · $it" } ?: dayLabel
    }

    fun trialPathToggle(languageCode: String): String =
        if (kn(languageCode)) "ಪ್ರಯೋಗ ಮಾರ್ಗ" else "Trial path"

    fun trialStackedToggle(languageCode: String): String =
        if (kn(languageCode)) "ಅಂತಸ್ತು" else "Stacked"
}
