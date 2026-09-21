package com.perqa.byebox.core

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Progress of the in-app APK download. */
sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState
    data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : UpdateDownloadState {
        val percent: Int
            get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
            else 0
    }

    data object Verifying : UpdateDownloadState
    data class Error(val message: String) : UpdateDownloadState
}

object UpdateDownloader {

    /**
     * Streams the APK to app-private external storage, reporting progress, then
     * verifies size and SHA-256 (when the release provides a digest).
     *
     * @throws IllegalStateException on HTTP, size or checksum failure.
     */
    suspend fun download(
        context: Context,
        info: UpdateInfo,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/octet-stream")
            setRequestProperty("User-Agent", "ByeBox-Android")
        }
        conn.connect()
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code")

            val total = conn.contentLengthLong.takeIf { it > 0 } ?: info.apkSizeBytes
            val dir = File(context.getExternalFilesDir(null), "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, "byebox-${info.latestVersion}.apk")

            val digest = MessageDigest.getInstance("SHA-256")
            var read = 0L
            conn.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                        read += n
                        onProgress(read, total)
                    }
                }
            }

            if (total > 0 && read != total) {
                file.delete()
                throw IllegalStateException("incomplete download ($read/$total)")
            }

            if (info.apkSha256.isNotBlank()) {
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                if (!actual.equals(info.apkSha256, ignoreCase = true)) {
                    file.delete()
                    throw IllegalStateException("checksum mismatch")
                }
            }
            file
        } finally {
            conn.disconnect()
        }
    }

    /** Launches the system package installer for a downloaded APK. */
    fun install(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
