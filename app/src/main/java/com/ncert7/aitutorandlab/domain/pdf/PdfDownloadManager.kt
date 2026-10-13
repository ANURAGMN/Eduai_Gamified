package com.ncert7.aitutorandlab.domain.pdf

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class PdfDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Isolated client without API interceptors to prevent auth header errors on Google Drive URLs
    private val client = OkHttpClient()

    suspend fun downloadPdf(
        url: String,
        fileName: String,
        onProgress: suspend (Int) -> Unit
    ): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return@withContext null

            val body = response.body ?: return@withContext null
            val totalBytes = body.contentLength()
            val inputStream = body.byteStream()

            val file = File(context.filesDir, "$fileName.pdf")
            val outputStream = FileOutputStream(file)

            var bytesCopied = 0L
            val buffer = ByteArray(8 * 1024)
            var bytes = inputStream.read(buffer)
            var lastProgress = 0

            while (bytes >= 0) {
                outputStream.write(buffer, 0, bytes)
                bytesCopied += bytes

                if (totalBytes > 0) {
                    val progress = ((bytesCopied * 100) / totalBytes).toInt()
                    if (progress > lastProgress) {
                        lastProgress = progress
                        onProgress(progress)
                    }
                }
                bytes = inputStream.read(buffer)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            return@withContext file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}