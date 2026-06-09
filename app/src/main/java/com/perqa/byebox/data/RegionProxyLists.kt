package com.perqa.byebox.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Fetches per-app proxy package lists from GitHub for region-based auto-selection.
 * Lists are maintained by the Hiddify project at:
 * https://github.com/hiddify/Android-GFW-Apps
 *
 * Each list is a newline-separated list of Android package names.
 */
object RegionProxyLists {
    private const val TAG = "RegionProxyLists"
    private const val BASE_URL = "https://raw.githubusercontent.com/hiddify/Android-GFW-Apps/refs/heads/master/"

    enum class Region(val displayName: String) {
        CN("China (GFW)"),
        IR("Iran"),
        RU("Russia"),
        AF("Afghanistan"),
        ID("Indonesia"),
        TR("Turkey"),
        BR("Brazil"),
        OTHER("Other")
    }

    enum class AppProxyMode {
        INCLUDE,  // VPN only for listed apps (proxy)
        EXCLUDE   // VPN for all except listed apps (direct)
    }

    /**
     * Fetch package list for a given region and mode.
     * Returns null on failure.
     */
    suspend fun fetch(
        region: Region,
        mode: AppProxyMode
    ): Set<String>? = withContext(Dispatchers.IO) {
        val filename = when (mode) {
            AppProxyMode.INCLUDE -> "proxy_${region.name.lowercase()}"
            AppProxyMode.EXCLUDE -> "direct_${region.name.lowercase()}"
        }
        val url = "$BASE_URL$filename"
        try {
            val text = URL(url).readText()
            val packages = text.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() && it.contains('.') }
                .toSet()
            Log.i(TAG, "Fetched ${packages.size} packages for $region/$mode")
            packages
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch $url: ${e.message}")
            null
        }
    }

    /**
     * Fetch both include and exclude lists for a region.
     */
    suspend fun fetchBoth(region: Region): Pair<Set<String>?, Set<String>?> {
        val include = fetch(region, AppProxyMode.INCLUDE)
        val exclude = fetch(region, AppProxyMode.EXCLUDE)
        return Pair(include, exclude)
    }
}
