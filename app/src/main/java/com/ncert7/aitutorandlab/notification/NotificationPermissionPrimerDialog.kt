package com.ncert7.aitutorandlab.notification

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ncert7.aitutorandlab.utils.isKannadaLanguage

@Composable
fun NotificationPermissionPrimerDialog(
    variant: NotificationPrimerVariant,
    languageCode: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val copy = primerCopy(variant, languageCode)
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDecline,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text(text = copy.title) },
        text = { Text(text = copy.body) },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text(text = copy.acceptLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text(text = copy.declineLabel)
            }
        },
    )
}

private data class PrimerCopy(
    val title: String,
    val body: String,
    val acceptLabel: String,
    val declineLabel: String,
)

private fun primerCopy(variant: NotificationPrimerVariant, languageCode: String): PrimerCopy {
    if (isKannadaLanguage(languageCode)) {
        return when (variant) {
            NotificationPrimerVariant.STUDY ->
                PrimerCopy(
                    title = "ಪ್ರತಿದಿನ ಹೆಚ್ಚು ಓದಿ",
                    body =
                        "ದೈನಂದಿನ ಜ್ಞಾಪನೆ ಪಡೆಯುವ ವಿದ್ಯಾರ್ಥಿಗಳು ಹೆಚ್ಚು ಓದುತ್ತಾರೆ ಮತ್ತು ತಮ್ಮ ಯೋಜನೆ ಮುಗಿಸುತ್ತಾರೆ. " +
                            "ಸಮಯವನ್ನು ನೀವೇ ಆಯ್ಕೆ ಮಾಡಬಹುದು — ಯಾವಾಗ ಬೇಕಾದರೂ ಆಫ್ ಮಾಡಬಹುದು.",
                    acceptLabel = "ಅಧಿಸೂಚನೆಗಳನ್ನು ಆನ್ ಮಾಡಿ",
                    declineLabel = "ಈಗ ಅಲ್ಲ",
                )
            NotificationPrimerVariant.FRIENDS ->
                PrimerCopy(
                    title = "ಗೆಳೆಯರ ಅಪ್‌ಡೇಟ್ ತಪ್ಪಿಸಬೇಡಿ",
                    body =
                        "ಗೆಳೆಯರು ನಿಮ್ಮನ್ನು ಪ್ರೋತ್ಸಾಹಿಸಿದಾಗ ಅಥವಾ ಲೀಡರ್‌ಬೋರ್ಡ್‌ನಲ್ಲಿ ಮುಂದೆ ಹೋದಾಗ ತಿಳಿಯಿರಿ. " +
                            "ಯಾವಾಗ ಬೇಕಾದರೂ ಆಫ್ ಮಾಡಬಹುದು.",
                    acceptLabel = "ಅಧಿಸೂಚನೆಗಳನ್ನು ಆನ್ ಮಾಡಿ",
                    declineLabel = "ಈಗ ಅಲ್ಲ",
                )
            NotificationPrimerVariant.STREAK ->
                PrimerCopy(
                    title = "ನಿಮ್ಮ ಸ್ಟ್ರೀಕ್ ಕಳೆದುಕೊಳ್ಳಬೇಡಿ",
                    body =
                        "ದೈನಂದಿನ ಯೋಜನೆ ಮತ್ತು ಸ್ಟ್ರೀಕ್‌ಗಾಗಿ ಸೌಮ್ಯ ಜ್ಞಾಪನೆ ಪಡೆಯಿರಿ. " +
                            "ಸಮಯವನ್ನು ನೀವೇ ಆಯ್ಕೆ ಮಾಡಬಹುದು — ಯಾವಾಗ ಬೇಕಾದರೂ ಆಫ್ ಮಾಡಬಹುದು.",
                    acceptLabel = "ಅಧಿಸೂಚನೆಗಳನ್ನು ಆನ್ ಮಾಡಿ",
                    declineLabel = "ಈಗ ಅಲ್ಲ",
                )
            NotificationPrimerVariant.QUEST ->
                PrimerCopy(
                    title = "ನಿಮ್ಮ ಬಹುಮಾನಗಳನ್ನು ತಪ್ಪಿಸಬೇಡಿ",
                    body =
                        "ಕвест್‌ಗಳು ಮತ್ತು ಸ್ಟ್ರೀಕ್‌ಗೆ ಸಂಬಂಧಿಸಿದ ಸೌಮ್ಯ ಜ್ಞಾಪನೆಗಳನ್ನು ಪಡೆಯಿರಿ. " +
                            "ನೀವು ಯಾವಾಗ ಬೇಕಾದರೂ ಆಫ್ ಮಾಡಬಹುದು.",
                    acceptLabel = "ಅಧಿಸೂಚನೆಗಳನ್ನು ಆನ್ ಮಾಡಿ",
                    declineLabel = "ಈಗ ಅಲ್ಲ",
                )
            NotificationPrimerVariant.DEFAULT ->
                PrimerCopy(
                    title = "ಪಾಠದ ಪದ್ಧತಿಯಲ್ಲೇ ಇರಿ",
                    body =
                        "ದೈನಂದಿನ ಯೋಜನೆ ಮತ್ತು ಸ್ಟ್ರೀಕ್‌ಗಾಗಿ ಸೌಮ್ಯ ಜ್ಞಾಪನೆ ಪಡೆಯಿರಿ. " +
                            "ಸಮಯವನ್ನು ನೀವೇ ಆಯ್ಕೆ ಮಾಡಬಹುದು.",
                    acceptLabel = "ಅಧಿಸೂಚನೆಗಳನ್ನು ಆನ್ ಮಾಡಿ",
                    declineLabel = "ಈಗ ಅಲ್ಲ",
                )
        }
    }

    val bodyEn =
        "Get a gentle reminder for your daily plan and streak. " +
            "You choose the time — turn it off anytime in Settings."
    return when (variant) {
        NotificationPrimerVariant.STUDY ->
            PrimerCopy(
                title = "Study a little more each day",
                body =
                    "Students who get a daily nudge study more and finish their plan. " +
                        "You choose the time — turn it off anytime in Settings.",
                acceptLabel = "Turn on notifications",
                declineLabel = "Not now",
            )
        NotificationPrimerVariant.FRIENDS ->
            PrimerCopy(
                title = "Don't miss your friends' updates",
                body =
                    "Get a heads-up when friends cheer you on or pass you on the leaderboard. " +
                        "Turn it off anytime in Settings.",
                acceptLabel = "Turn on notifications",
                declineLabel = "Not now",
            )
        NotificationPrimerVariant.STREAK ->
            PrimerCopy(
                title = "Never lose your streak",
                body = bodyEn,
                acceptLabel = "Turn on notifications",
                declineLabel = "Not now",
            )
        NotificationPrimerVariant.QUEST ->
            PrimerCopy(
                title = "Don't miss your rewards",
                body = bodyEn,
                acceptLabel = "Turn on notifications",
                declineLabel = "Not now",
            )
        NotificationPrimerVariant.DEFAULT ->
            PrimerCopy(
                title = "Stay on track",
                body = bodyEn,
                acceptLabel = "Turn on notifications",
                declineLabel = "Not now",
            )
    }
}
