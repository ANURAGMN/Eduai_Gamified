package com.anurag.eduai.uikit.avatar

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Opens the system share sheet with a rendered avatar image plus invite text.
 * The avatar is captured off-screen from [config] so shares always match what
 * the user sees in the studio.
 */
fun shareAvatar(
    context: Context,
    avatarName: String,
    config: TutorConfig,
) {
    captureTutorShareBitmap(
        context = context,
        config = config,
        onReady = { bitmap ->
            val uri =
                runCatching { saveShareBitmap(context, bitmap) }
                    .getOrNull()
            launchShareIntent(context, avatarName, uri)
        },
        onError = {
            launchShareIntent(context, avatarName, imageUri = null)
        },
    )
}

private fun saveShareBitmap(context: Context, bitmap: Bitmap) =
    FileProvider.getUriForFile(
        context,
        "${context.packageName}.eduai.share",
        writeShareFile(context, bitmap),
    )

private fun writeShareFile(context: Context, bitmap: Bitmap): File {
    val dir = File(context.cacheDir, "share").apply { mkdirs() }
    val file = File(dir, "eduai_tutor_share.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return file
}

private fun launchShareIntent(
    context: Context,
    avatarName: String,
    imageUri: android.net.Uri?,
) {
    val text =
        "I just unlocked the “$avatarName” tutor on EduAI 🎓 " +
            "Come learn with me — first concept's on me!"
    val send =
        Intent(Intent.ACTION_SEND).apply {
            if (imageUri != null) {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            putExtra(Intent.EXTRA_SUBJECT, "My EduAI tutor: $avatarName")
        }
    val chooser =
        Intent.createChooser(send, "Share your tutor").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    context.startActivity(chooser)
}
