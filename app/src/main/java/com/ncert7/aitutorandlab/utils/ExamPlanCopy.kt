package com.ncert7.aitutorandlab.utils

import com.anurag.eduai.uikit.components.PlanDayStatus
import com.anurag.eduai.uikit.components.PlanDayType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Bilingual copy for exam-plan overview, setup, and day labels. */
object ExamPlanCopy {
    val EXAM_TYPE_KEYS = listOf("Unit Test", "Mid-term", "Final")

    private fun kn(languageCode: String): Boolean = isKannadaLanguage(languageCode)

    private fun locale(languageCode: String): Locale =
        Locale.forLanguageTag(if (kn(languageCode)) "kn" else "en")

    fun examTypeLabel(languageCode: String, examType: String): String =
        when (examType) {
            "Unit Test" -> if (kn(languageCode)) "ಘಟಕ ಪರೀಕ್ಷೆ" else "Unit Test"
            "Mid-term" -> if (kn(languageCode)) "ಮಧ್ಯಾವಧಿ ಪರೀಕ್ಷೆ" else "Mid-term"
            "Final" -> if (kn(languageCode)) "ಅಂತಿಮ ಪರೀಕ್ಷೆ" else "Final"
            else -> examType
        }

    fun planDayTypeLabel(languageCode: String, type: PlanDayType): String =
        when (type) {
            PlanDayType.Lesson -> if (kn(languageCode)) "ಪಾಠ" else "Lesson"
            PlanDayType.Revise -> if (kn(languageCode)) "ಪುನರಾವಲೋಕನ" else "Revise"
            PlanDayType.Mock -> if (kn(languageCode)) "ಮಾಕ್" else "Mock"
            PlanDayType.Exam -> if (kn(languageCode)) "ಪರೀಕ್ಷೆ" else "Exam"
        }

    fun planDayStatusLabel(languageCode: String, status: PlanDayStatus): String =
        when (status) {
            PlanDayStatus.Done -> if (kn(languageCode)) "ಪೂರ್ಣ" else "Done"
            PlanDayStatus.Partial -> if (kn(languageCode)) "ಭಾಗಶಃ" else "Partial"
            PlanDayStatus.Today -> if (kn(languageCode)) "ಇಂದು" else "Today"
            PlanDayStatus.Upcoming -> if (kn(languageCode)) "ಮುಂದಿನ" else "Upcoming"
        }

    fun localizedStoredDayLabel(languageCode: String, dayType: String, storedLabel: String): String =
        when (dayType) {
            "REVISE" -> {
                val block = Regex("Revision block (\\d+)").find(storedLabel)?.groupValues?.get(1)
                if (block != null) {
                    if (kn(languageCode)) "ಪುನರಾವಲೋಕನ ಬ್ಲಾಕ್ $block" else storedLabel
                } else {
                    if (kn(languageCode)) "ಪುನರಾವಲೋಕನ ದಿನ" else storedLabel
                }
            }
            "MOCK" -> {
                val rawExam = storedLabel.substringAfter("·", storedLabel).trim()
                val examLabel = examTypeLabel(languageCode, normalizeExamTypeKey(rawExam))
                if (kn(languageCode)) "ಮಾಕ್ ಅಭ್ಯಾಸ · $examLabel" else "Mock practice · $examLabel"
            }
            "EXAM" ->
                if (kn(languageCode)) "ಪರೀಕ್ಷಾ ದಿನ — ನೀವು ಸಿದ್ಧರಿದ್ದೀರಿ!" else storedLabel
            else -> storedLabel
        }

    private fun normalizeExamTypeKey(raw: String): String =
        when (raw.lowercase()) {
            "unit test" -> "Unit Test"
            "mid-term", "midterm" -> "Mid-term"
            "final" -> "Final"
            else -> raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

    fun emptyDayMessage(languageCode: String): String =
        if (kn(languageCode)) {
            "ದಯವಿಟ್ಟು ನಿಮ್ಮ ಅಧ್ಯಯನವನ್ನು ಯೋಜಿಸಿ — ಈ ದಿನವನ್ನು ನಿಗದಿಪಡಿಸಲು 'ಯೋಜನೆ ಸೇರಿಸಿ' ಟ್ಯಾಪ್ ಮಾಡಿ."
        } else {
            "Please plan your study — tap Add plan to schedule this day."
        }

    fun overviewTitleSetup(languageCode: String): String =
        if (kn(languageCode)) "ಪರೀಕ್ಷೆಗೆ ಯೋಜನೆ ಮಾಡಿ" else "Plan an exam"

    fun overviewTitlePlan(languageCode: String): String =
        if (kn(languageCode)) "ಪರೀಕ್ಷಾ ತಯಾರಿ ಯೋಜನೆ" else "Exam prep plan"

    fun addPlan(languageCode: String): String =
        if (kn(languageCode)) "ಯೋಜನೆ ಸೇರಿಸಿ" else "Add plan"

    fun growAsYouLearn(languageCode: String): String =
        if (kn(languageCode)) "ಕಲಿಯುತ್ತಾ ಬೆಳೆಯಿರಿ" else "Grow as you learn"

    fun growBannerBody(languageCode: String, dayCount: Int): String =
        if (kn(languageCode)) {
            "ನೀವು ಮುಗಿಸುವ ಪ್ರತಿ ಕಾರ್ಯವು ನಿಮ್ಮ ಪ್ರಪಂಚದಲ್ಲಿ ಹೊಸದನ್ನು ನೆಡುತ್ತದೆ. " +
                "ಒಂದು ದೃಶ್ಯವನ್ನು ತುಂಬಿ (12 ಸಸ್ಯಗಳು) ಮುಂದಿನದನ್ನು ಅನ್ಲಾಕ್ ಮಾಡಿ — " +
                "$dayCount ದಿನಗಳ ತಯಾರಿ, ಬೆಳೆಯಲು ಸಂಪೂರ್ಣ ಪ್ರಪಂಚ."
        } else {
            "Every task you finish plants something new in your world. Fill a scene (12 plants) " +
                "to unlock the next one — $dayCount days of prep, a whole world to grow."
        }

    fun dayRowPrefix(languageCode: String, dayIndex: Int, typeLabel: String): String =
        if (kn(languageCode)) "ದಿನ $dayIndex · $typeLabel" else "Day $dayIndex · $typeLabel"

    fun growsYourWorld(languageCode: String): String =
        if (kn(languageCode)) "+ ನಿಮ್ಮ ಪ್ರಪಂಚವನ್ನು ಬೆಳೆಸುತ್ತದೆ" else "+ grows your world"

    fun buildPlanTitle(languageCode: String): String =
        if (kn(languageCode)) "ನಿಮ್ಮ ಪರೀಕ್ಷಾ ತಯಾರಿ ಯೋಜನೆಯನ್ನು ರಚಿಸಿ" else "Build your exam prep plan"

    fun buildPlanSubtitle(languageCode: String): String =
        if (kn(languageCode)) {
            "ಪರೀಕ್ಷಾ ಪ್ರಕಾರ, ವಿಷಯ, ಅಧ್ಯಾಯಗಳು ಮತ್ತು ದೈನಂದಿನ ಅಧ್ಯಯನ ಸಮಯವನ್ನು ಆಯ್ಕೆಮಾಡಿ. " +
                "ನಾವು ಪಾಠ, ಪುನರಾವಲೋಕನ, ಮಾಕ್ ಮತ್ತು ಪರೀಕ್ಷಾ ದಿನವನ್ನು ನಿಗದಿಪಡಿಸುತ್ತೇವೆ."
        } else {
            "Pick exam type, subject, chapters, and daily study time. We'll schedule lessons, revision, mock, and exam day."
        }

    fun sectionExamType(languageCode: String): String =
        if (kn(languageCode)) "ಪರೀಕ್ಷಾ ಪ್ರಕಾರ" else "Exam type"

    fun sectionSubject(languageCode: String): String =
        if (kn(languageCode)) "ವಿಷಯ" else "Subject"

    fun sectionChapters(languageCode: String): String =
        if (kn(languageCode)) "ಅಧ್ಯಾಯಗಳು" else "Chapters"

    fun loadingSubjects(languageCode: String): String =
        if (kn(languageCode)) "ವಿಷಯಗಳನ್ನು ಲೋಡ್ ಮಾಡಲಾಗುತ್ತಿದೆ…" else "Loading subjects…"

    fun dailyBudgetLabel(languageCode: String, minutes: Int): String =
        if (kn(languageCode)) "ದೈನಂದಿನ ಅಧ್ಯಯನ ಸಮಯ: $minutes ನಿಮಿಷ" else "Daily study budget: $minutes min"

    fun estimatedPlanLength(languageCode: String, days: Int, trialItems: Int): String =
        if (kn(languageCode)) {
            "ಅಂದಾಜು ಯೋಜನೆ ಅವಧಿ: $days ದಿನಗಳು · ~$trialItems ಪ್ರಯೋಗ ಕಾರ್ಯಗಳು"
        } else {
            "Estimated plan length: $days days · ~$trialItems trial tasks"
        }

    fun sectionExamDate(languageCode: String): String =
        if (kn(languageCode)) "ಪರೀಕ್ಷಾ ದಿನಾಂಕ" else "Exam date"

    fun pickSpecificDate(languageCode: String): String =
        if (kn(languageCode)) "ನಿರ್ದಿಷ್ಟ ದಿನಾಂಕ ಆಯ್ಕೆಮಾಡಿ" else "Pick a specific date"

    fun cancel(languageCode: String): String =
        if (kn(languageCode)) "ರದ್ದು" else "Cancel"

    fun ok(languageCode: String): String =
        if (kn(languageCode)) "ಸರಿ" else "OK"

    fun generatePlan(languageCode: String): String =
        if (kn(languageCode)) "ಯೋಜನೆ ರಚಿಸಿ" else "Generate plan"

    fun editPlan(languageCode: String): String =
        if (kn(languageCode)) "ಯೋಜನೆ ಸಂಪಾದಿಸಿ" else "Edit plan"

    fun summaryLine(languageCode: String, examType: String, dailyMinutes: Int): String =
        "${examTypeLabel(languageCode, examType)} · $dailyMinutes " +
            if (kn(languageCode)) "ನಿಮಿಷ/ದಿನ" else "min/day"

    fun summaryDaysChapters(languageCode: String, dayCount: Int, chapterCount: Int): String =
        if (kn(languageCode)) "$dayCount ದಿನಗಳು · $chapterCount ಅಧ್ಯಾಯಗಳು" else "$dayCount days · $chapterCount chapters"

    fun swipeExamTypes(languageCode: String): String =
        if (kn(languageCode)) "ಪರೀಕ್ಷಾ ಪ್ರಕಾರಗಳನ್ನು ಸ್ವೈಪ್ ಮಾಡಿ" else "Swipe exam types"

    fun swipeSubjects(languageCode: String): String =
        if (kn(languageCode)) "ವಿಷಯಗಳನ್ನು ಸ್ವೈಪ್ ಮಾಡಿ" else "Swipe subjects"

    fun swipeExamDates(languageCode: String): String =
        if (kn(languageCode)) "ಪರೀಕ್ಷಾ ದಿನಾಂಕಗಳನ್ನು ಸ್ವೈಪ್ ಮಾಡಿ" else "Swipe exam dates"

    fun datePresetOneWeek(languageCode: String): String =
        if (kn(languageCode)) "1 ವಾರ" else "1 week"

    fun datePresetTwoWeeks(languageCode: String): String =
        if (kn(languageCode)) "2 ವಾರಗಳು" else "2 weeks"

    fun datePresetThreeWeeks(languageCode: String): String =
        if (kn(languageCode)) "3 ವಾರಗಳು" else "3 weeks"

    fun datePresetOneMonth(languageCode: String): String =
        if (kn(languageCode)) "1 ತಿಂಗಳು" else "1 month"

    fun chooseSubject(languageCode: String): String =
        if (kn(languageCode)) "ವಿಷಯವನ್ನು ಆಯ್ಕೆಮಾಡಿ." else "Choose a subject."

    fun selectChapter(languageCode: String): String =
        if (kn(languageCode)) "ಕನಿಷ್ಠ ಒಂದು ಅಧ್ಯಾಯವನ್ನು ಆಯ್ಕೆಮಾಡಿ." else "Select at least one chapter."

    fun fixPlanIssues(languageCode: String): String =
        if (kn(languageCode)) "ಮೊದಲು ಯೋಜನೆ ಸಮಸ್ಯೆಗಳನ್ನು ಸರಿಪಡಿಸಿ." else "Fix plan issues first."

    fun formatExamDate(languageCode: String, date: LocalDate): String =
        DateTimeFormatter.ofPattern("d MMM yyyy", locale(languageCode)).format(date)

    fun examDatePast(languageCode: String): String =
        if (kn(languageCode)) "ಪರೀಕ್ಷಾ ದಿನಾಂಕ ಹಿಂದಿನದಾಗಿದೆ. ಭವಿಷ್ಯದ ದಿನಾಂಕವನ್ನು ಆಯ್ಕೆಮಾಡಿ." else "Exam date is in the past. Pick a future date."

    fun planNeedsMoreDays(
        languageCode: String,
        requiredPlanDays: Int,
        lessonDays: Int,
        reviseDays: Int,
        availableCalendarDays: Int,
    ): String =
        if (kn(languageCode)) {
            "ಈ ಯೋಜನೆಗೆ $requiredPlanDays ದಿನಗಳು ಬೇಕು " +
                "($lessonDays ಪಾಠಗಳು, $reviseDays ಪುನರಾವಲೋಕನ, ಮಾಕ್ + ಪರೀಕ್ಷೆ) " +
                "ಆದರೆ ಪರೀಕ್ಷೆಗೆ $availableCalendarDays ದಿನ(ಗಳು) ಮಾತ್ರ ಉಳಿದಿವೆ. " +
                "ದೈನಂದಿನ ನಿಮಿಷಗಳನ್ನು ಹೆಚ್ಚಿಸಿ, ಕಡಿಮೆ ಅಧ್ಯಾಯಗಳನ್ನು ಆಯ್ಕೆಮಾಡಿ, ಅಥವಾ ಪರೀಕ್ಷೆಯನ್ನು ಮುಂದೂಡಿ."
        } else {
            "This plan needs $requiredPlanDays days " +
                "($lessonDays lessons, $reviseDays revision, mock + exam) " +
                "but only $availableCalendarDays day(s) remain until your exam. " +
                "Increase daily minutes, pick fewer chapters, or move the exam later."
        }

    fun startByDate(languageCode: String, earliestStart: LocalDate): String =
        if (kn(languageCode)) {
            "ಪರೀಕ್ಷೆಗೆ ಮುಂಚೆ ಮುಗಿಸಲು ${formatExamDate(languageCode, earliestStart)} ರೊಮ್ಮೆ ಪ್ರಾರಂಭಿಸಬೇಕು. " +
                "ಪರೀಕ್ಷೆಯನ್ನು ಮುಂದೂಡಿ ಅಥವಾ ಅಧ್ಯಾಯಗಳು / ದೈನಂದಿನ ನಿಮಿಷಗಳನ್ನು ಕಡಿಮೆ ಮಾಡಿ."
        } else {
            "You need to start by ${formatExamDate(languageCode, earliestStart)} to finish before the exam. " +
                "Move the exam later or reduce chapters / raise daily minutes."
        }

    fun tightScheduleWarning(languageCode: String, bufferDays: Int): String =
        if (kn(languageCode)) {
            "ವೇಳಾಪಟ್ಟಿ ಬಿಗಿಯಾಗಿದೆ — ಪರೀಕ್ಷೆಗೆ ಮುಂಚೆ ${bufferDays.coerceAtLeast(0)} buffer ದಿನ(ಗಳು) ಮಾತ್ರ."
        } else {
            "Schedule is tight — only ${bufferDays.coerceAtLeast(0)} buffer day(s) before the exam."
        }

    fun largeWorkloadWarning(languageCode: String, totalTrialItems: Int, requiredPlanDays: Int): String =
        if (kn(languageCode)) {
            "ಹೆಚ್ಚಿನ ಕೆಲಸಭಾರ: $requiredPlanDays ದಿನಗಳಲ್ಲಿ ಸುಮಾರು $totalTrialItems ಪ್ರಯೋಗ ಕಾರ್ಯಗಳು."
        } else {
            "Large workload: about $totalTrialItems trial tasks across $requiredPlanDays days."
        }
}
