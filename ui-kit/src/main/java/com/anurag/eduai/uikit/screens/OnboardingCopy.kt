package com.anurag.eduai.uikit.screens

data class OnboardingSlideCopy(
    val title: String,
    val body: String,
)

data class OnboardingSubjectCopy(
    val key: String,
    val label: String,
    val subtitle: String,
)

data class OnboardingWorldCopy(
    val key: String,
    val label: String,
    val headline: String,
    val sub: String,
)

data class OnboardingStrings(
    val skip: String,
    val next: String,
    val getStarted: String,
    val slides: List<OnboardingSlideCopy>,
    val step1: String,
    val step2: String,
    val step3: String,
    val pickSubjectTitle: String,
    val pickSubjectSub: String,
    val subjects: List<OnboardingSubjectCopy>,
    val backSubject: String,
    val backChapter: String,
    val chapterTitle: (String) -> String,
    val chapterSub: String,
    val recommended: String,
    val loadingChapters: String,
    val continueLabel: String,
    val pickWorldTitle: String,
    val pickWorldSub: String,
    val worlds: List<OnboardingWorldCopy>,
    val buildPlan: String,
)

private fun isKn(languageCode: String): Boolean =
    languageCode.trim().lowercase().let { it == "kn" || it.startsWith("kn-") }

fun onboardingStrings(languageCode: String): OnboardingStrings {
    if (isKn(languageCode)) return kannada()
    return english()
}

private fun english(): OnboardingStrings =
    OnboardingStrings(
        skip = "Skip",
        next = "Next",
        getStarted = "Get started",
        slides =
            listOf(
                OnboardingSlideCopy(
                    title = "Learn a little,\nevery single day",
                    body = "Short guided sessions — a concept, a hands-on simulation, and a quick quiz. About 18 minutes a day.",
                ),
                OnboardingSlideCopy(
                    title = "Build a streak\nworth protecting",
                    body = "Show up daily to grow your flame. Miss a day? Streak freezes and repairs have your back.",
                ),
                OnboardingSlideCopy(
                    title = "Climb leagues\nwith your friends",
                    body = "Earn XP, rise through weekly leagues, and cheer each other on. Ranked on effort, never on grades.",
                ),
            ),
        step1 = "STEP 1 OF 3",
        step2 = "STEP 2 OF 3",
        step3 = "STEP 3 OF 3",
        pickSubjectTitle = "Pick a subject",
        pickSubjectSub = "We'll build your plan around it. You can add more later.",
        subjects =
            listOf(
                OnboardingSubjectCopy("Math", "Math", "Integers, fractions, equations…"),
                OnboardingSubjectCopy("Science", "Science", "Nutrition, heat, acids & bases…"),
            ),
        backSubject = "Subject",
        backChapter = "Chapter",
        chapterTitle = { subject -> "$subject · pick a chapter" },
        chapterSub = "Where do you want to start?",
        recommended = "Recommended to start",
        loadingChapters = "Loading chapters…",
        continueLabel = "Continue",
        pickWorldTitle = "Pick your reward world",
        pickWorldSub = "Every task you finish grows it. You can switch anytime.",
        worlds =
            listOf(
                OnboardingWorldCopy(
                    key = "Garden",
                    label = "Garden",
                    headline = "Study more, grow your woodland",
                    sub = "Start in the woodland — 8 scenic places, 12 plants each. Bees, butterflies and songbirds move in as it fills.",
                ),
                OnboardingWorldCopy(
                    key = "Space",
                    label = "Space",
                    headline = "Explore space, build on Mars",
                    sub = "Start on Mars — build outposts across 8 worlds. Your first solar array is free.",
                ),
            ),
        buildPlan = "Build my plan",
    )

private fun kannada(): OnboardingStrings =
    OnboardingStrings(
        skip = "ಬಿಟ್ಟುಬಿಡಿ",
        next = "ಮುಂದೆ",
        getStarted = "ಪ್ರಾರಂಭಿಸಿ",
        slides =
            listOf(
                OnboardingSlideCopy(
                    title = "ಪ್ರತಿದಿನ ಸ್ವಲ್ಪ,\nಕಲಿಯಿರಿ",
                    body = "ಚಿಕ್ಕ ಮಾರ್ಗದರ್ಶಿತ ಅಧ್ಯಯನ — ಪಾಠ, ಹাতಕೆ ಕೆಲಸದ ಸಿಮ್ಯುಲೇಶನ್ ಮತ್ತು ತ್ವರಿತ ಪ್ರಶ್ನೋತ್ತರ. ದಿನಕ್ಕೆ ಸುಮಾರು 18 ನಿಮಿಷ.",
                ),
                OnboardingSlideCopy(
                    title = "ನಿಮ್ಮ ಸ್ಟ್ರೀಕ್\nಕಾಪಾಡಿಕೊಳ್ಳಿ",
                    body = "ಪ್ರತಿದಿನ ಬನ್ನಿ — ನಿಮ್ಮ ಜ್ವಾಲೆ ಬೆಳೆಯುತ್ತದೆ. ಒಂದು ದಿನ ತಪ್ಪಿದರೆ? ಸ್ಟ್ರೀಕ್ ಫ್ರೀಜ್ ಮತ್ತು ರಿಪೇರ್ ಸಹಾಯ ಮಾಡುತ್ತದೆ.",
                ),
                OnboardingSlideCopy(
                    title = "ಸ್ನೇಹಿತರೊಂದಿಗೆ\nಲೀಗ್‌ನಲ್ಲಿ ಮೇಲೇರು",
                    body = "XP ಗಳಿಸಿ, ವಾರದ ಲೀಗ್‌ಗಳಲ್ಲಿ ಮೇಲಕ್ಕೆ ಹೋಗಿ, ಪರಸ್ಪರ ಉತ್ತೇಜಿಸಿ. ಶ್ರಮದ ಮೇಲೆ — ಅಂಕಗಳ ಮೇಲೆ ಅಲ್ಲ.",
                ),
            ),
        step1 = "ಹಂತ 1 / 3",
        step2 = "ಹಂತ 2 / 3",
        step3 = "ಹಂತ 3 / 3",
        pickSubjectTitle = "ವಿಷಯ ಆರಿಸಿ",
        pickSubjectSub = "ನಿಮ್ಮ ಯೋಜನೆಯನ್ನು ಇದರ ಸುತ್ತ ನಿರ್ಮಿಸುತ್ತೇವೆ. ನಂತರ ಇನ್ನಷ್ಟು ಸೇರಿಸಬಹುದು.",
        subjects =
            listOf(
                OnboardingSubjectCopy("Math", "ಗಣಿತ", "ಪೂರ್ಣಾಂಕಗಳು, ಭಿನ್ನರಾಶಿಗಳು, ಸಮೀಕರಣಗಳು…"),
                OnboardingSubjectCopy("Science", "ವಿಜ್ಞಾನ", "ಪೋಷಣೆ, ಉಷ್ಣತೆ, ಅಮ್ಲಗಳು ಮತ್ತು ಕ್ಷಾರಗಳು…"),
            ),
        backSubject = "ವಿಷಯ",
        backChapter = "ಅಧ್ಯಾಯ",
        chapterTitle = { subject -> "$subject · ಅಧ್ಯಾಯ ಆರಿಸಿ" },
        chapterSub = "ಎಲ್ಲಿ ಪ್ರಾರಂಭಿಸಲು ಬಯಸುತ್ತೀರಿ?",
        recommended = "ಪ್ರಾರಂಭಿಸಲು ಶಿಫಾರಸು",
        loadingChapters = "ಅಧ್ಯಾಯಗಳು ಲೋಡ್ ಆಗುತ್ತಿವೆ…",
        continueLabel = "ಮುಂದುವರಿಸಿ",
        pickWorldTitle = "ನಿಮ್ಮ ಬಹುಮಾನ ಜಗತ್ತು ಆರಿಸಿ",
        pickWorldSub = "ಪೂರ್ಣಗೊಳಿಸಿದ ಪ್ರತಿ ಕಾರ್ಯ ಇದನ್ನು ಬೆಳೆಸುತ್ತದೆ. ಯಾವಾಗ ಬೇಕಾದರೂ ಬದಲಿಸಬಹುದು.",
        worlds =
            listOf(
                OnboardingWorldCopy(
                    key = "Garden",
                    label = "ತೋಟ",
                    headline = "ಹೆಚ್ಚು ಕಲಿಯಿರಿ — ನಿಮ್ಮ ಅರಣ್ಯವನ್ನು ಬೆಳೆಸಿ",
                    sub = "ಅರಣ್ಯದಲ್ಲಿ ಪ್ರಾರಂಭ — 8 ಸುಂದರ ಸ್ಥಳಗಳು, ಪ್ರತಿಯೊಂದರಲ್ಲಿ 12 ಬೆಳೆಗಳು. ತುಂಬಿದಂತೆ ಜೇನುನೊಣ, ಚಿಟ್ಟೆ ಮತ್ತು ಹಾಡುಗಾರ ಪಕ್ಷಿಗಳು ಬರುತ್ತವೆ.",
                ),
                OnboardingWorldCopy(
                    key = "Space",
                    label = "ಬಾಹ್ಯಾಕಾಶ",
                    headline = "ಬಾಹ್ಯಾಕಾಶ ಅನ್ವೇಷಿಸಿ — ಮಂಗಳದಲ್ಲಿ ನಿರ್ಮಿಸಿ",
                    sub = "ಮಂಗಳದಲ್ಲಿ ಪ್ರಾರಂಭ — 8 ಜಗತ್ತುಗಳಲ್ಲಿ outpost ನಿರ್ಮಿಸಿ. ನಿಮ್ಮ ಮೊದಲ ಸೌರ ಶಕ್ತಿ ವ್ಯವಸ್ಥೆ ಉಚಿತ.",
                ),
            ),
        buildPlan = "ನನ್ನ ಯೋಜನೆ ನಿರ್ಮಿಸಿ",
    )
