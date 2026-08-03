package com.anurag.eduai.uikit.screens

/** Localized copy for the avatar studio screen. */
data class AvatarStudioCopy(
    val screenTitle: String,
    val sectionTitle: String,
    val mood: String,
    val customize: String,
    val expression: String,
    val saved: String,
    val saveWithAds: String,
    val shareWithFriends: String,
    val saveHint: String,
    val saveAdFailed: String,
    val weeklyAvatars: String,
    val newInDays: (Int) -> String,
    val unlockHint: String,
    val use: String,
    val share: String,
    val unlockWithAds: String,
    val locked: String,
    val savingTutor: String,
    val unlockingPreset: (String) -> String,
    val outfit: (Int) -> String,
    val neck: (Int) -> String,
    val hair: (Int) -> String,
    val hairColor: (Int) -> String,
    val glasses: (Int) -> String,
    val frame: (Int) -> String,
    val eyeLine: (Boolean) -> String,
    val cheeks: (Boolean) -> String,
    val moodLabel: (Int) -> String,
    val gestureLabel: (Int) -> String,
    val spin360: String,
    val swipeMoods: String,
    val swipeOptions: String,
    val swipeExpressions: String,
    val swipeAvatars: String,
)

fun defaultAvatarStudioCopy(): AvatarStudioCopy =
    AvatarStudioCopy(
        screenTitle = "Avatar studio",
        sectionTitle = "Avatar studio",
        mood = "Mood",
        customize = "Customize",
        expression = "Expression",
        saved = "Saved ✓",
        saveWithAds = "Save · 2 ads",
        shareWithFriends = "Share with friends",
        saveHint = "Watch 2 ads to save. Your tutor appears on Home and in celebrations.",
        saveAdFailed = "Could not finish both ads. Your tutor look was not saved — try again.",
        weeklyAvatars = "This week's avatars",
        newInDays = { days -> if (days == 1) "New in 1 day" else "New in $days days" },
        unlockHint = "Watch 2 ads to unlock. Yours to keep and share.",
        use = "Use",
        share = "Share",
        unlockWithAds = "Unlock · 2 ads",
        locked = "Locked",
        savingTutor = "Saving your tutor",
        unlockingPreset = { name -> "Unlocking “$name”" },
        outfit = { v ->
            when (v) {
                1 -> "Outfit: Shirt"
                2 -> "Outfit: Hoodie"
                3 -> "Outfit: V-neck"
                else -> "Outfit: Tee"
            }
        },
        neck = { v ->
            when (v) {
                1 -> "Neck: Slim"
                2 -> "Neck: Broad"
                else -> "Neck: Regular"
            }
        },
        hair = { v ->
            when (v) {
                1 -> "Hair: Side-part"
                2 -> "Hair: Curly"
                else -> "Hair: Tousled"
            }
        },
        hairColor = { v ->
            when (v) {
                1 -> "Color: Black"
                2 -> "Color: Auburn"
                else -> "Color: Brown"
            }
        },
        glasses = { v ->
            when (v) {
                1 -> "Specs: Round"
                2 -> "Specs: None"
                else -> "Specs: Classic"
            }
        },
        frame = { v ->
            when (v) {
                1 -> "Frame: Brown"
                2 -> "Frame: Navy"
                else -> "Frame: Black"
            }
        },
        eyeLine = { on -> if (on) "Eye line: On" else "Eye line: Off" },
        cheeks = { on -> if (on) "Cheeks: On" else "Cheeks: Off" },
        moodLabel = { v ->
            when (v) {
                1 -> "Mood: Happy"
                2 -> "Mood: Angry"
                3 -> "Mood: Thinking"
                4 -> "Mood: Surprised"
                5 -> "Mood: Sad"
                else -> "Mood: Auto"
            }
        },
        gestureLabel = { v ->
            when (v) {
                1 -> "Gesture: Wave"
                2 -> "Gesture: Clap"
                3 -> "Gesture: Point"
                4 -> "Gesture: Palms"
                5 -> "Gesture: Think"
                else -> "Gesture: Auto"
            }
        },
        spin360 = "Spin 360°",
        swipeMoods = "Swipe moods",
        swipeOptions = "Swipe options",
        swipeExpressions = "Swipe expressions",
        swipeAvatars = "Swipe avatars",
    )
