package com.perqa.byebox.core

import com.perqa.byebox.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Info about an available update, built from a GitHub release.
 */
data class UpdateInfo(
    /** Clean version name, e.g. `1.2.0` or `1.3.0-beta.1`. */
    val latestVersion: String,
    /** Android versionCode of the release (from the release metadata). */
    val latestVersionCode: Int,
    /** Direct browser download URL of the APK asset. */
    val apkUrl: String,
    /** Lower-case hex SHA-256 of the APK, or empty when unknown. */
    val apkSha256: String,
    /** APK size in bytes as reported by GitHub (0 when unknown). */
    val apkSizeBytes: Long,
    /** Release page URL. */
    val downloadUrl: String,
    /** Human readable release notes (marker lines stripped). */
    val releaseNotes: String,
    /** ISO-8601 publish date. */
    val publishedAt: String,
    /** GitHub `prerelease` flag. */
    val prerelease: Boolean,
    /** Lifecycle stage: "", "alpha", "beta", "rc". */
    val stage: String,
    /** Build number inside the stage (0 for stable). */
    val stageNumber: Int,
) {
    val isPreRelease: Boolean get() = prerelease || stage.isNotEmpty()

    /** Version with a human readable, always-English stage label. */
    val displayVersion: String
        get() = if (stage.isEmpty()) latestVersion
        else "$latestVersion (${stage.replaceFirstChar { it.uppercase() }} $stageNumber)"
}

sealed interface UpdateCheckResult {
    /** Installed build is the newest (or the newer release was skipped). */
    data object UpToDate : UpdateCheckResult

    /** A newer build is available. */
    data class Available(val info: UpdateInfo) : UpdateCheckResult

    /** Network / parsing failure. */
    data class Error(val message: String) : UpdateCheckResult
}

/**
 * Checks GitHub Releases for a newer ByeBox build.
 *
 * Uses the *list* endpoint (not `/releases/latest`) so pre-releases are visible too,
 * and ranks releases by the numeric `versionCode` published in the release metadata
 * (`<!-- byebox:versionCode=N -->` in the body or `ByeBox X.Y.Z (N)` in the name).
 */
object UpdateChecker {
    private const val RELEASES_API =
        "https://api.github.com/repos/PerQ4/ByeBox/releases?per_page=30"

    private val VERSION_CODE_MARKER = Regex("""byebox:versionCode\s*=\s*(\d{1,9})""")
    private val NAME_CODE_MARKER = Regex("""\((\d{1,9})\)""")

    suspend fun check(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val releases = JSONArray(httpGet(RELEASES_API))

            var best: JSONObject? = null
            var bestVersion: SemVer? = null
            var bestCode: Int? = null

            for (i in 0 until releases.length()) {
                val release = releases.optJSONObject(i) ?: continue
                if (release.optBoolean("draft", false)) continue

                val version = SemVer.parse(release.optString("tag_name", "")) ?: continue
                val code = extractVersionCode(release)

                val better = when {
                    best == null -> true
                    // Prefer explicit versionCodes; they are the authoritative ordering.
                    code != null && bestCode != null -> code > bestCode
                    code != null && bestCode == null -> true
                    code == null && bestCode != null -> false
                    else -> version > bestVersion!!
                }
                if (better) {
                    best = release
                    bestVersion = version
                    bestCode = code
                }
            }

            val release = best ?: return@withContext UpdateCheckResult.Error("no releases")
            val version = bestVersion!!

            val asset = pickApkAsset(release)
                ?: return@withContext UpdateCheckResult.Error("no apk asset in release")
            val apkUrl = asset.optString("browser_download_url", "")
            if (apkUrl.isBlank()) {
                return@withContext UpdateCheckResult.Error("no apk asset in release")
            }

            val currentCode = BuildConfig.VERSION_CODE
            val currentVersion = SemVer.parse(BuildConfig.VERSION_NAME)
            val isNewer = when {
                bestCode != null -> bestCode > currentCode
                currentVersion != null -> version > currentVersion
                else -> false
            }
            if (!isNewer) return@withContext UpdateCheckResult.UpToDate

            val body = release.optString("body", "")
            val notes = body
                .lineSequence()
                .filterNot { it.contains("byebox:versionCode") }
                .joinToString("\n")
                .trim()

            val digest = asset.optString("digest", "")
            val sha256 = digest.substringAfter("sha256:", "").trim().lowercase()

            val stage = version.stage
            UpdateCheckResult.Available(
                UpdateInfo(
                    latestVersion = version.toString(),
                    latestVersionCode = bestCode ?: currentCode + 1,
                    apkUrl = apkUrl,
                    apkSha256 = sha256,
                    apkSizeBytes = asset.optLong("size", 0L),
                    downloadUrl = release.optString("html_url", ""),
                    releaseNotes = notes,
                    publishedAt = release.optString("published_at", ""),
                    prerelease = release.optBoolean("prerelease", false),
                    stage = stage,
                    stageNumber = version.stageNumber,
                )
            )
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "update check failed")
        }
    }

    private fun extractVersionCode(release: JSONObject): Int? {
        VERSION_CODE_MARKER.find(release.optString("body", ""))
            ?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        NAME_CODE_MARKER.find(release.optString("name", ""))
            ?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        return null
    }

    private fun pickApkAsset(release: JSONObject): JSONObject? {
        val assets = release.optJSONArray("assets") ?: return null
        val apks = (0 until assets.length())
            .mapNotNull { assets.optJSONObject(it) }
            .filter { it.optString("name", "").endsWith(".apk", ignoreCase = true) }
        return apks.firstOrNull { it.optString("name").contains("universal", ignoreCase = true) }
            ?: apks.firstOrNull { it.optString("name").contains("arm64", ignoreCase = true) }
            ?: apks.firstOrNull()
    }

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "ByeBox-Android/${BuildConfig.VERSION_NAME}")
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        return try {
            val code = conn.responseCode
            if (code !in 200..299) throw RuntimeException("HTTP $code")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}

/**
 * Minimal SemVer 2.0.0 implementation (enough for our tags): `MAJOR.MINOR.PATCH[-pre][+build]`.
 * Missing `MINOR`/`PATCH` default to `0`, so the legacy `9.0` tag still parses.
 */
data class SemVer(
    val major: Int,
    val minor: Int = 0,
    val patch: Int = 0,
    val preRelease: List<String> = emptyList(),
) : Comparable<SemVer> {

    val stage: String
        get() = preRelease.firstOrNull()?.lowercase()
            ?.takeIf { it == "alpha" || it == "beta" || it == "rc" }
            .orEmpty()

    val stageNumber: Int
        get() = preRelease.getOrNull(1)?.toIntOrNull() ?: 0

    override fun compareTo(other: SemVer): Int {
        if (major != other.major) return major - other.major
        if (minor != other.minor) return minor - other.minor
        if (patch != other.patch) return patch - other.patch
        // A release has higher precedence than a pre-release.
        if (preRelease.isEmpty() && other.preRelease.isEmpty()) return 0
        if (preRelease.isEmpty()) return 1
        if (other.preRelease.isEmpty()) return -1
        // Compare identifier by identifier (SemVer §11).
        val size = minOf(preRelease.size, other.preRelease.size)
        for (i in 0 until size) {
            val a = preRelease[i]
            val b = other.preRelease[i]
            val an = a.toIntOrNull()
            val bn = b.toIntOrNull()
            val cmp = when {
                an != null && bn != null -> an - bn
                an != null -> -1 // numeric identifiers have lower precedence
                bn != null -> 1
                else -> a.compareTo(b)
            }
            if (cmp != 0) return cmp
        }
        return preRelease.size - other.preRelease.size
    }

    override fun toString(): String = buildString {
        append(major).append('.').append(minor).append('.').append(patch)
        if (preRelease.isNotEmpty()) append('-').append(preRelease.joinToString("."))
    }

    companion object {
        fun parse(raw: String): SemVer? {
            var s = raw.trim()
            if (s.isEmpty()) return null
            if (s.startsWith("v", ignoreCase = true)) s = s.substring(1)
            s = s.substringBefore('+') // drop build metadata
            val core = s.substringBefore('-')
            val pre = s.substringAfter('-', "")
            val parts = core.split('.')
            val major = parts.getOrNull(0)?.toIntOrNull() ?: return null
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            val preList = pre.split('.').filter { it.isNotBlank() }
            return SemVer(major, minor, patch, preList)
        }
    }
}
