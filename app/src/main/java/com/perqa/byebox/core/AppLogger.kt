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
import java.util.concurrent.CopyOnWriteArrayList

object AppLogger {
    private const val TAG = "AppLogger"

    // Thread-safe in-memory list — CopyOnWriteArrayList is safe from any thread including Go JNI threads
    private val logBuffer = CopyOnWriteArrayList<String>()

    // StateFlow is only updated on the main thread via Handler to avoid JNI crashes
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    private var appContext: Context? = null
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    @Volatile private var initialized = false

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
        Log.i("sing-box-core", message)
    }

    /**
     * Called from Go JNI goroutine threads — MUST be safe to call from any thread.
     * Only uses Android Log (thread-safe), delegates actual logging to log() which is thread-safe.
     */
    fun singbox(message: String) {
        Log.d("sing-box", message)
        log("[sing-box] $message")
    }

    fun clearLogs() {
        logBuffer.clear()
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

    /**
     * Thread-safe log function — safe to call from Go JNI threads, Kotlin coroutines, or any thread.
     * - File writes: synchronized on a dedicated lock object (no JVM dependencies)
     * - StateFlow updates: posted to main thread via Handler (only main thread can safely update StateFlow)
     */
    private val fileLock = Any()

    private fun log(formattedMessage: String) {
        val timestamp = try { timeFormat.format(Date()) } catch (e: Exception) { "??:??:??.???" }
        val logLine = "[$timestamp] $formattedMessage"

        // Add to in-memory buffer — CopyOnWriteArrayList is thread-safe
        logBuffer.add(logLine)
        if (logBuffer.size > 600) {
            // Trim from front — create a new snapshot to avoid concurrent modification
            val snapshot = logBuffer.toList()
            if (snapshot.size > 500) {
                logBuffer.removeAll(snapshot.take(snapshot.size - 500).toSet())
            }
        }

        // Post StateFlow update to main thread — StateFlow.value must NOT be set from Go JNI threads
        mainHandler.post {
            _logs.value = logBuffer.toList().takeLast(500)
        }

        // Write to files — synchronized on a plain lock (no JVM coroutine dependencies)
        synchronized(fileLock) {
            runCatching {
                appContext?.let { ctx ->
                    val internalFile = File(ctx.filesDir, "box_log.txt")
                    FileWriter(internalFile, true).use { it.write(logLine + "\n") }

                    ctx.getExternalFilesDir(null)?.let { externalDir ->
                        val externalFile = File(externalDir, "box_log.txt")
                        FileWriter(externalFile, true).use { it.write(logLine + "\n") }
                    }
                }
            }
        }
    }
}
