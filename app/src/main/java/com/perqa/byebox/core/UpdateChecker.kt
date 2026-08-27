package com.perqa.byebox.core

import com.perqa.byebox.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val latestVersion: String,
    val downloadUrl: String,
    val releaseNotes: String,
)

object UpdateChecker {
    private const val GITHUB_API = "https://api.github.com/repos/PerQ4/byebox/releases/latest"

    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(GITHUB_API).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val json = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val release = org.json.JSONObject(json)
            val tagName = release.optString("tag_name", "").trim()
            val body = release.optString("body", "")

            if (tagName.isBlank()) return@withContext null

            val current = BuildConfig.VERSION_NAME
            val latest = tagName.removePrefix("v").removePrefix("V")

            if (compareVersions(latest, current) <= 0) return@withContext null

            UpdateInfo(
                latestVersion = tagName,
                downloadUrl = "https://github.com/PerQ4/byebox/releases/tag/$tagName",
                releaseNotes = body,
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun compareVersions(a: String, b: String): Int {
        val partsA = a.split(".").map { it.toIntOrNull() ?: 0 }
        val partsB = b.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(partsA.size, partsB.size)
        for (i in 0 until maxLen) {
            val diff = (partsA.getOrElse(i) { 0 }) - (partsB.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }
}
