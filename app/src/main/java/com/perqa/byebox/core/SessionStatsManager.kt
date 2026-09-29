package com.perqa.byebox.core

import com.v2ray.ang.handler.MmkvManager
import java.util.Locale

/**
 * Tracks the current VPN session: when it started and the cumulative
 * traffic transferred since connect.
 *
 * The values live in MMKV so they survive process restarts — the VPN
 * service runs in a separate process and the UI reads the same store.
 *
 * - [start] is called from the core service manager when the core loop
 *   starts, [stop] when it stops.
 * - The byte counters are incremented by [TrafficStatsManager] (the single
 *   owner of the native outbound counters) every poll tick.
 */
object SessionStatsManager {
    const val PREF_SESSION_STARTED_AT = "pref_session_started_at"
    const val PREF_SESSION_UPLOAD_BYTES = "pref_session_upload_bytes"
    const val PREF_SESSION_DOWNLOAD_BYTES = "pref_session_download_bytes"

    /** Marks a session start and zeroes the per-session traffic counters. */
    fun start() {
        MmkvManager.encodeSettings(PREF_SESSION_STARTED_AT, System.currentTimeMillis())
        MmkvManager.encodeSettings(PREF_SESSION_UPLOAD_BYTES, 0L)
        MmkvManager.encodeSettings(PREF_SESSION_DOWNLOAD_BYTES, 0L)
    }

    /** Clears the running session (on disconnect). */
    fun stop() {
        MmkvManager.encodeSettings(PREF_SESSION_STARTED_AT, 0L)
        MmkvManager.encodeSettings(PREF_SESSION_UPLOAD_BYTES, 0L)
        MmkvManager.encodeSettings(PREF_SESSION_DOWNLOAD_BYTES, 0L)
    }

    /** Milliseconds since the session start, or 0 when no session is active. */
    fun elapsedMillis(): Long {
        val started = MmkvManager.decodeSettingsLong(PREF_SESSION_STARTED_AT, 0L)
        if (started <= 0L) return 0L
        val elapsed = System.currentTimeMillis() - started
        return if (elapsed > 0) elapsed else 0L
    }

    fun uploadBytes(): Long = MmkvManager.decodeSettingsLong(PREF_SESSION_UPLOAD_BYTES, 0L)

    fun downloadBytes(): Long = MmkvManager.decodeSettingsLong(PREF_SESSION_DOWNLOAD_BYTES, 0L)

    /** Formats elapsed milliseconds as `HH:MM:SS` (always two-digit hours). */
    fun formatElapsedFull(ms: Long): String {
        if (ms <= 0) return "00:00:00"
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }

    /** Formats elapsed milliseconds as `H:MM:SS` (or `MM:SS` under an hour). */
    fun formatElapsed(ms: Long): String {
        if (ms <= 0) return "00:00"
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%02d:%02d", m, s)
        }
    }

    /** Formats a byte count using the app's language for the unit labels. */
    fun formatBytes(bytes: Long, language: String): String {
        val value = bytes.coerceAtLeast(0L)
        val gb = com.perqa.byebox.ui.main.Loc.get("unit_gb", language)
        val mb = com.perqa.byebox.ui.main.Loc.get("unit_mb", language)
        val kb = com.perqa.byebox.ui.main.Loc.get("unit_kb", language)
        val b = com.perqa.byebox.ui.main.Loc.get("unit_b", language)
        return when {
            value >= 1_073_741_824L -> String.format(Locale.US, "%.2f $gb", value / 1_073_741_824.0)
            value >= 1_048_576L -> String.format(Locale.US, "%.1f $mb", value / 1_048_576.0)
            value >= 1024L -> String.format(Locale.US, "%.0f $kb", value / 1024.0)
            else -> "$value $b"
        }
    }
}