package com.ncert7.aitutorandlab.utils

import com.ncert7.aitutorandlab.domain.gamification.FriendEventType

/**
 * Rebuilds friend-feed messages at display time so EN↔KN toggles don't leave frozen English
 * prose on the rail. Stored DB/Firebase text stays English for backwards compatibility.
 */
object FriendFeedCopy {
    private val streakRe = Regex("""Reached a (\d+)-day streak""", RegexOption.IGNORE_CASE)
    private val completedTopicRe = Regex("""Completed (.+)""", RegexOption.IGNORE_CASE)
    private val conceptsRe = Regex("""Completed (\d+) concepts""", RegexOption.IGNORE_CASE)
    private val xpRe = Regex("""Hit (\d+) XP""", RegexOption.IGNORE_CASE)
    private val rankRe = Regex("""Climbed to #(\d+) in the league \(\+(\d+)\)""", RegexOption.IGNORE_CASE)
    private val friendsWithRe = Regex("""You're now friends with (.+)""", RegexOption.IGNORE_CASE)
    private val addedYouRe = Regex("""(.+) added you as a friend""", RegexOption.IGNORE_CASE)
    private val inviteSelfRe =
        Regex(
            """Earned (\d+) gems for completing your first lesson with a friend""",
            RegexOption.IGNORE_CASE,
        )
    private val inviteOtherRe =
        Regex(
            """Earned (\d+) gems — your friend finished their first lesson""",
            RegexOption.IGNORE_CASE,
        )
    private val promotedRe = Regex("""Promoted to the (.+)""", RegexOption.IGNORE_CASE)

    fun wantsToBeFriends(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಸ್ನೇಹಿತರಾಗಲು ಬಯಸುತ್ತಾರೆ" else "wants to be friends"

    fun connectedPlaceholder(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) {
            "ಸಂಪರ್ಕಿತ — ಇಲ್ಲಿ ಸರಣಿ ಮತ್ತು ಮೈಲಿಗಲ್ಲುಗಳು ಕಾಣಿಸುತ್ತವೆ."
        } else {
            "Connected — you'll see streaks and milestones here."
        }

    fun localize(
        message: String,
        eventType: String,
        languageCode: String,
    ): String {
        if (!isKannadaLanguage(languageCode)) return message
        val type = FriendEventType.fromStorage(eventType)
        streakRe.matchEntire(message)?.let { m ->
            return "ಗಳಿಸಿದ ${m.groupValues[1]}-ದಿನದ ಸರಣಿ"
        }
        conceptsRe.matchEntire(message)?.let { m ->
            return "${m.groupValues[1]} ಪರಿಕಲ್ಪನೆಗಳನ್ನು ಪೂರ್ಣಗೊಳಿಸಿದರು"
        }
        xpRe.matchEntire(message)?.let { m ->
            return "${m.groupValues[1]} XP ತಲುಪಿದರು"
        }
        rankRe.matchEntire(message)?.let { m ->
            return "ಲೀಗ್‌ನಲ್ಲಿ #${m.groupValues[1]} ಕ್ಕೆ ಏರಿದರು (+${m.groupValues[2]})"
        }
        friendsWithRe.matchEntire(message)?.let { m ->
            return "ನೀವು ಈಗ ${m.groupValues[1]} ಜೊತೆ ಸ್ನೇಹಿತರು"
        }
        addedYouRe.matchEntire(message)?.let { m ->
            return "${m.groupValues[1]} ನಿಮ್ಮನ್ನು ಸ್ನೇಹಿತರನ್ನಾಗಿ ಸೇರಿಸಿದರು"
        }
        inviteSelfRe.matchEntire(message)?.let { m ->
            return "ಸ್ನೇಹಿತರೊಂದಿಗೆ ಮೊದಲ ಪಾಠ ಮುಗಿಸಿ ${m.groupValues[1]} ರತ್ನಗಳನ್ನು ಗಳಿಸಿದ್ದೀರಿ"
        }
        inviteOtherRe.matchEntire(message)?.let { m ->
            return "${m.groupValues[1]} ರತ್ನಗಳು — ನಿಮ್ಮ ಸ್ನೇಹಿತರು ಮೊದಲ ಪಾಠ ಮುಗಿಸಿದರು"
        }
        promotedRe.matchEntire(message)?.let { m ->
            return "${m.groupValues[1]} ಗೆ ಬಡ್ತಿ"
        }
        // TOPIC_COMPLETED and generic "Completed X" after more-specific concept count.
        if (type == FriendEventType.TOPIC_COMPLETED || type == null) {
            completedTopicRe.matchEntire(message)?.let { m ->
                return "${m.groupValues[1]} ಪೂರ್ಣಗೊಳಿಸಿದರು"
            }
        }
        return message
    }
}
