package com.perqa.byebox.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object AppLogger {
    private const val TAG = "AppLogger"
    private const val FLUSH_INTERVAL_SECONDS = 1L
    private const val FLUSH_THRESHOLD = 50
    private const val MAX_BUFFER_SIZE = 600
    private const val MAX_VISIBLE_LINES = 500

    private val logBuffer = CopyOnWriteArrayList<String>()
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val logExecutor = Executors.newSingleThreadScheduledExecutor()

    private var appContext: Context? = null
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    @Volatile private var initialized = false

    private val pendingWrites = ConcurrentLinkedQueue<String>()
    private val pendingWriteCount = AtomicInteger(0)
    private val fileLock = Any()

    init {
        logExecutor.scheduleWithFixedDelay(
            { flushPendingWrites() },
            FLUSH_INTERVAL_SECONDS,
            FLUSH_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        )
    }

    fun init(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        initialized = true

        runCatching {
            appContext?.let { ctx ->
                val internalFile = File(ctx.filesDir, "box_log.txt")
                if (!internalFile.exists()) internalFile.createNewFile()

                ctx.getExternalFilesDir(null)?.let { externalDir ->
                    val externalFile = File(externalDir, "box_log.txt")
                    if (!externalFile.exists()) externalFile.createNewFile()
                }
            }
        }
        info("SYSTEM", "ByeBox инициализирован. Готов к подключению.")
    }

    fun info(tag: String, message: String) {
        log("[INFO] [$tag] $message")
        Log.i(tag, message)
    }

    fun warn(tag: String, message: String) {
        log("[WARNING] [$tag] $message")
        Log.w(tag, message)
    }

    fun error(tag: String, message: String, tr: Throwable? = null) {
        val fullMsg = if (tr != null) "$message: ${tr.localizedMessage}" else message
        log("[ERROR] [$tag] $fullMsg")
        if (tr != null) Log.e(tag, message, tr) else Log.e(tag, message)
    }

    fun core(message: String) {
        log("[CORE] $message")
        Log.i("xray-core", message)
    }

    fun xray(message: String) {
        Log.d("xray", message)
        log("[xray] $message")
    }

    fun clearLogs() {
        logBuffer.clear()
        flushPendingWrites()
        mainHandler.post { _logs.value = listOf("[SYSTEM] Логи очищены.") }
        runCatching {
            appContext?.let { ctx ->
                val internalFile = File(ctx.filesDir, "box_log.txt")
                if (internalFile.exists()) internalFile.delete()
                internalFile.createNewFile()

                ctx.getExternalFilesDir(null)?.let { externalDir ->
                    val externalFile = File(externalDir, "box_log.txt")
                    if (externalFile.exists()) externalFile.delete()
                    externalFile.createNewFile()
                }
            }
        }
    }

    private fun log(formattedMessage: String) {
        val timestamp = try { timeFormat.format(Date()) } catch (e: Exception) { "??:??:??.???" }
        val logLine = "[$timestamp] $formattedMessage"

        logBuffer.add(logLine)
        if (logBuffer.size > MAX_BUFFER_SIZE) {
            val snapshot = logBuffer.toList()
            if (snapshot.size > MAX_VISIBLE_LINES) {
                logBuffer.removeAll(snapshot.take(snapshot.size - MAX_VISIBLE_LINES).toSet())
            }
        }

        mainHandler.post {
            _logs.value = logBuffer.toList().takeLast(MAX_VISIBLE_LINES)
        }

        pendingWrites.add(logLine + "\n")
        if (pendingWriteCount.incrementAndGet() >= FLUSH_THRESHOLD) {
            logExecutor.execute { flushPendingWrites() }
        }
    }

    private fun flushPendingWrites() {
        val lines = mutableListOf<String>()
        while (true) {
            val line = pendingWrites.poll() ?: break
            lines.add(line)
        }
        if (lines.isEmpty()) return
        pendingWriteCount.addAndGet(-lines.size)

        synchronized(fileLock) {
            runCatching {
                appContext?.let { ctx ->
                    val content = lines.joinToString("")
                    val internalFile = File(ctx.filesDir, "box_log.txt")
                    FileWriter(internalFile, true).use { it.write(content) }

                    ctx.getExternalFilesDir(null)?.let { externalDir ->
                        val externalFile = File(externalDir, "box_log.txt")
                        FileWriter(externalFile, true).use { it.write(content) }
                    }
                }
            }
        }
    }
}
