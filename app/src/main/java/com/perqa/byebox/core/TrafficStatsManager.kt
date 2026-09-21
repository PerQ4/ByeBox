package com.perqa.byebox.core

import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.util.JsonUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Singleton owner of the core outbound traffic counters.
 *
 * The v2ray core is configured with per-outbound proxy counters (tag = proxy+, either
 * uplink or downlink) that are RESET on every query. Polling them from more than one place
 * (previously the dashboard poller in MainScreenViewModel and the speed notification poller
 * in NotificationManager) caused the two pollers to steal accumulated bytes from each other,
 * so both readouts raced between 0 and the real value.
 *
 * This manager is the single owner of the native counters: it is the only component that
 * queries and resets them. Everything else — the dashboard speed cells and the speed
 * notification — consumes the StateFlows below instead of querying the core itself.
 */
object TrafficStatsManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var pollJob: Job? = null
    private var lastQueryTime: Long = 0L

    /**
     * Outbound tags whose counters feed the aggregate speed.
     *
     * Null means "legacy prefix mode": only tags starting with {@link AppConfig.TAG_PROXY}
     * are counted. This is the behavior for generated (non-custom) configurations.
     */
    private var trackedTags: Set<String>? = null

    private val _uploadSpeedBps = MutableStateFlow(0L)
    val uploadSpeedBps: StateFlow<Long> = _uploadSpeedBps.asStateFlow()

    private val _downloadSpeedBps = MutableStateFlow(0L)
    val downloadSpeedBps: StateFlow<Long> = _downloadSpeedBps.asStateFlow()

    /** Starts the single shared polling loop. Idempotent — safe to call from multiple clients. */
    fun start(configJson: String? = null) {
        if (pollJob?.isActive == true) return

        trackedTags = extractProxyTags(configJson)
        lastQueryTime = System.currentTimeMillis()
        pollJob = scope.launch {
            while (isActive) {
                delay(AppConfig.SPEED_QUERY_INTERVAL_MS)

                val queryTime = System.currentTimeMillis()
                val sinceLastQuery = queryTime - lastQueryTime
                if (sinceLastQuery <= 0) continue
                val seconds = sinceLastQuery / 1000.0

                var proxyUplink = 0L
                var proxyDownlink = 0L
                CoreServiceManager.queryAllOutboundTrafficStats().forEach { stat ->
                    if (isTrackedTag(stat.tag)) {
                        when (stat.direction) {
                            AppConfig.UPLINK -> proxyUplink += stat.value
                            AppConfig.DOWNLINK -> proxyDownlink += stat.value
                        }
                    }
                }

                lastQueryTime = queryTime
                val uploadBps = (proxyUplink / seconds).toLong()
                val downloadBps = (proxyDownlink / seconds).toLong()
                _uploadSpeedBps.value = uploadBps
                _downloadSpeedBps.value = downloadBps
                // Shared bridge to the UI process (the dashboard reads MMKV, not the in-memory flow)
                MmkvManager.encodeSettings(AppConfig.PREF_SPEED_UPLOAD_BPS, uploadBps)
                MmkvManager.encodeSettings(AppConfig.PREF_SPEED_DOWNLOAD_BPS, downloadBps)
            }
        }
    }

    /** Stops the polling loop and resets the speed readouts to zero. Idempotent. */
    fun stop() {
        pollJob?.cancel()
        pollJob = null
        trackedTags = null
        _uploadSpeedBps.value = 0L
        _downloadSpeedBps.value = 0L
        MmkvManager.encodeSettings(AppConfig.PREF_SPEED_UPLOAD_BPS, 0L)
        MmkvManager.encodeSettings(AppConfig.PREF_SPEED_DOWNLOAD_BPS, 0L)
    }

    private fun isTrackedTag(tag: String): Boolean {
        val explicit = trackedTags
        return if (explicit != null) tag in explicit else tag.startsWith(AppConfig.TAG_PROXY)
    }

    /**
     * Collect the outbound tags carrying proxied traffic from a runtime config.
     *
     * Generated configs name every proxy outbound after {@link AppConfig.TAG_PROXY}, but
     * custom configs use arbitrary tags (for example "p1-frda-..."). Builtin service
     * outbounds are filtered out so only real proxy traffic is aggregated.
     */
    private fun extractProxyTags(configJson: String?): Set<String>? {
        if (configJson.isNullOrBlank()) return null

        return try {
            val root = JsonUtil.parseString(configJson) ?: return null
            val outbounds = root.getAsJsonArray("outbounds")?.asSequence() ?: return null

            outbounds
                .mapNotNull { element ->
                    element.takeIf { it.isJsonObject }?.asJsonObject
                        ?.get("tag")?.takeIf { it.isJsonPrimitive }?.asString
                }
                .filter { it.isNotBlank() && !isServiceTag(it) }
                .toSet()
                .ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun isServiceTag(tag: String): Boolean {
        val normalized = tag.lowercase(Locale.US)
        return normalized in SERVICE_TAGS ||
            normalized.startsWith("dns") ||
            normalized.startsWith("loop") ||
            normalized == "api" ||
            normalized == "metrics"
    }

    private val SERVICE_TAGS = setOf(
        AppConfig.TAG_DIRECT,
        AppConfig.TAG_BLOCKED,
        AppConfig.TAG_FRAGMENT,
        AppConfig.TAG_DNS,
        AppConfig.TAG_DOMESTIC_DNS,
        AppConfig.TAG_BALANCER,
    )

    /** Formats a bytes-per-second value as a compact human-readable speed string. */
    fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1_048_576L -> String.format(Locale.US, "%.1f MB/s", bytesPerSec / 1_048_576.0)
            bytesPerSec >= 1024L -> String.format(Locale.US, "%.0f KB/s", bytesPerSec / 1024.0)
            else -> "$bytesPerSec B/s"
        }
    }
}
