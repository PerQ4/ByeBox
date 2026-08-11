package com.perqa.byebox.core

import com.perqa.byebox.data.ProxyConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import kotlin.system.measureTimeMillis

data class ProbeSummary(val total: Int, val ok: Int, val failed: Int)

object PingProbe {
    suspend fun probeConfigs(
        configs: List<ProxyConfig>,
        healthUrl: String,
        strictHealthCheck: Boolean = false,
        onResult: (config: ProxyConfig, ping: Int?) -> Unit = { _, _ -> }
    ): ProbeSummary = coroutineScope {
        var ok = 0
        var failed = 0
        configs.chunked(8).forEach { batch ->
            batch.map { config ->
                async { config to probeConfigLatency(config, healthUrl, strictHealthCheck) }
            }.awaitAll().forEach { (config, ping) ->
                if (ping != null) {
                    ok += 1
                } else {
                    failed += 1
                }
                onResult(config, ping)
            }
        }
        ProbeSummary(configs.size, ok, failed)
    }

    suspend fun probeConfigLatency(config: ProxyConfig, healthUrl: String, strictHealthCheck: Boolean): Int? {
        val ping = probeTcpLatency(config) ?: return null
        if (!strictHealthCheck) return ping
        return if (probeResource(healthUrl)) ping else null
    }

    suspend fun probeTcpLatency(config: ProxyConfig): Int? = withContext(Dispatchers.IO) {
        if (config.address.isBlank() || config.port <= 0) return@withContext null
        var socket: Socket? = null
        try {
            val elapsed = measureTimeMillis {
                socket = Socket()
                socket.soTimeout = 2500
                socket.connect(InetSocketAddress(config.address, config.port), 2500)
            }
            elapsed.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        } catch (_: Exception) {
            null
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun probeResource(rawUrl: String): Boolean = withContext(Dispatchers.IO) {
        if (rawUrl.isBlank()) return@withContext true
        val normalizedUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) rawUrl else "https://$rawUrl"
        val connection = try {
            (URL(normalizedUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 3500
                readTimeout = 3500
                instanceFollowRedirects = false
            }
        } catch (_: Exception) {
            return@withContext false
        }
        try {
            val code = connection.responseCode
            code in 200..399 || code == 204 || code == 405
        } catch (_: Exception) {
            false
        } finally {
            connection.disconnect()
        }
    }
}
