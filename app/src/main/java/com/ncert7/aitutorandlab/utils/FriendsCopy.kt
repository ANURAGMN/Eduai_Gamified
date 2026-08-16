package com.ncert7.aitutorandlab.utils

import com.ncert7.aitutorandlab.domain.gamification.FriendAddResult

/** Friends screen chrome + snackbar status (EN / KN). */
object FriendsCopy {
    private fun kn(languageCode: String): Boolean = isKannadaLanguage(languageCode)

    fun screenTitle(languageCode: String, friendCount: Int): String =
        if (kn(languageCode)) "ಸ್ನೇಹಿತರು · $friendCount" else "Friends · $friendCount"

    fun backContentDescription(languageCode: String): String =
        if (kn(languageCode)) "ಹಿಂದೆ" else "Back"

    fun friendRequestsTitle(languageCode: String): String =
        if (kn(languageCode)) "ಸ್ನೇಹಿತ ವಿನಂತಿಗಳು" else "Friend requests"

    fun wantsToBeFriends(languageCode: String): String =
        FriendFeedCopy.wantsToBeFriends(languageCode)

    fun acceptLabel(languageCode: String): String = HomeCopy.acceptLabel(languageCode)

    fun yourFriendCodeTitle(languageCode: String): String =
        if (kn(languageCode)) "ನಿಮ್ಮ ಸ್ನೇಹಿತ ಕೋಡ್" else "Your friend code"

    fun loadingCode(languageCode: String): String =
        if (kn(languageCode)) "ಲೋಡ್ ಆಗುತ್ತಿದೆ…" else "Loading…"

    fun copyCodeLabel(languageCode: String): String =
        if (kn(languageCode)) "ಕೋಡ್ ನಕಲಿಸಿ" else "Copy code"

    fun shareLabel(languageCode: String): String = HomeCopy.shareLabel(languageCode)

    fun addFriendTitle(languageCode: String): String = HomeCopy.friendsAddCardTitle(languageCode)

    fun addFriendBody(languageCode: String): String =
        if (kn(languageCode)) {
            "ಅವರ ೮-ಅಕ್ಷರದ ಕೋಡ್ ನಮೂದಿಸಿ. ತಕ್ಷಣ ಲಿಂಕ್ — ಅವರು ಮೊದಲ ಪಾಠ ಮುಗಿಸಿದಾಗ ಪ್ರತಿಯೊಬ್ಬರಿಗೂ ೫೦ ರತ್ನ."
        } else {
            "Enter their 8-character code. Link instantly — earn 50 gems each when they finish their first lesson."
        }

    fun friendCodeFieldLabel(languageCode: String): String =
        if (kn(languageCode)) "ಸ್ನೇಹಿತ ಕೋಡ್" else "Friend code"

    fun addFriendButton(languageCode: String): String =
        if (kn(languageCode)) "ಸ್ನೇಹಿತರನ್ನು ಸೇರಿಸಿ" else "Add friend"

    fun shareMessage(languageCode: String, code: String): String =
        if (kn(languageCode)) {
            "EduAI ನಲ್ಲಿ ನನ್ನನ್ನು ಸೇರಿಸಿ! ನನ್ನ ಸ್ನೇಹಿತ ಕೋಡ್ $code"
        } else {
            "Add me on EduAI! My friend code is $code"
        }

    fun shareChooserTitle(languageCode: String): String =
        if (kn(languageCode)) "ಸ್ನೇಹಿತ ಕೋಡ್ ಹಂಚಿ" else "Share friend code"

    fun clipboardLabel(languageCode: String): String = friendCodeFieldLabel(languageCode)

    fun addResultMessage(languageCode: String, result: FriendAddResult): String =
        when (result) {
            FriendAddResult.SUCCESS ->
                if (kn(languageCode)) "ಸ್ನೇಹಿತರನ್ನು ಸೇರಿಸಲಾಗಿದೆ!" else "Friend added!"
            FriendAddResult.INVALID_CODE ->
                if (kn(languageCode)) "ಮಾನ್ಯ ಸ್ನೇಹಿತ ಕೋಡ್ ನಮೂದಿಸಿ." else "Enter a valid friend code."
            FriendAddResult.SELF_ADD ->
                if (kn(languageCode)) "ನಿಮ್ಮದೇ ಕೋಡ್ ಸೇರಿಸಲಾಗುವುದಿಲ್ಲ." else "You can't add your own code."
            FriendAddResult.ALREADY_FRIENDS ->
                if (kn(languageCode)) "ನೀವು ಈಗಾಗಲೇ ಸ್ನೇಹಿತರು." else "You're already friends."
            FriendAddResult.NOT_FOUND ->
                if (kn(languageCode)) "ಆ ಕೋಡ್‌ನ ವಿದ್ಯಾರ್ಥಿ ಸಿಗಲಿಲ್ಲ." else "No student found with that code."
            FriendAddResult.FAILED ->
                if (kn(languageCode)) {
                    "ಸ್ನೇಹಿತರನ್ನು ಸೇರಿಸಲಾಗಲಿಲ್ಲ. ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ."
                } else {
                    "Could not add friend. Try again."
                }
        }

    fun acceptResultMessage(languageCode: String, ok: Boolean): String =
        if (ok) {
            if (kn(languageCode)) "ಸ್ನೇಹಿತ ವಿನಂತಿ ಸ್ವೀಕರಿಸಲಾಗಿದೆ!" else "Friend request accepted!"
        } else {
            if (kn(languageCode)) "ವಿನಂತಿ ಸ್ವೀಕರಿಸಲಾಗಲಿಲ್ಲ." else "Could not accept request."
        }
}
