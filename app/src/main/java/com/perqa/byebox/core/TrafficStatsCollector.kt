package com.perqa.byebox.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

data class TrafficStats(
    val downBytes: Long = 0L,
    val upBytes: Long = 0L,
    val downSpeed: Long = 0L, // bytes/s
    val upSpeed: Long = 0L    // bytes/s
)

class TrafficStatsCollector(private val port: Int = 9090) {
    private val _stats = MutableStateFlow(TrafficStats())
    val stats: StateFlow<TrafficStats> = _stats.asStateFlow()

    private var collectJob: Job? = null
    private var lastDownBytes: Long = 0L
    private var lastUpBytes: Long = 0L
    private var lastSampleTime: Long = 0L

    suspend fun start(): Boolean = withContext(Dispatchers.IO) {
        try {
            val testUrl = URL("http://127.0.0.1:$port")
            val testConn = testUrl.openConnection() as HttpURLConnection
            testConn.connectTimeout = 2000
            testConn.readTimeout = 2000
            testConn.connect()
            val testCode = testConn.responseCode
            testConn.disconnect()
            testCode == 200
        } catch (e: Exception) {
            false
        }
    }

    fun update(bytesDown: Long, bytesUp: Long) {
        val now = System.currentTimeMillis()
        val dt = (now - lastSampleTime).coerceAtLeast(1)
        val downSpeed = ((bytesDown - lastDownBytes) * 1000L) / dt
        val upSpeed = ((bytesUp - lastUpBytes) * 1000L) / dt
        lastDownBytes = bytesDown
        lastUpBytes = bytesUp
        lastSampleTime = now
        _stats.value = TrafficStats(
            downBytes = bytesDown,
            upBytes = bytesUp,
            downSpeed = downSpeed.coerceAtLeast(0),
            upSpeed = upSpeed.coerceAtLeast(0)
        )
    }

    fun reset() {
        _stats.value = TrafficStats()
        lastDownBytes = 0L
        lastUpBytes = 0L
        lastSampleTime = 0L
    }
}
