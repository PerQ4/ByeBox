package com.perqa.byebox.core

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class SingBoxRuntime(private val context: Context) {
    data class PrepareResult(
        val executable: File?,
        val reason: String?
    ) {
        val isAvailable: Boolean get() = executable != null
    }

    data class StartResult(
        val started: Boolean,
        val reason: String? = null
    )

    private var process: Process? = null
    private var outputThread: Thread? = null

    fun prepareExecutable(forceExtract: Boolean = false): PrepareResult {
        val abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty()
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        Log.i("SingBoxRuntime", "ABI: $abi, nativeLibDir: $nativeLibDir")

        // Strategy 1: binary bundled as libbox.so in nativeLibraryDir (installed by Android)
        val nativeLibFile = File(nativeLibDir, "libbox.so")
        if (nativeLibFile.exists() && nativeLibFile.length() > 0) {
            Log.i("SingBoxRuntime", "Found libbox.so in nativeLibDir: ${nativeLibFile.absolutePath}, size=${nativeLibFile.length()}")
            return PrepareResult(nativeLibFile, null)
        }

        // Strategy 2: extract from assets
        val candidates = listOf(
            "sing-box/$abi/sing-box",
            "sing-box/sing-box-$abi",
            "sing-box"
        )
        val assetPath = candidates.firstOrNull { existsInAssets(it) }
        if (assetPath != null) {
            val outDir = context.getDir("bin", Context.MODE_PRIVATE)
            val outFile = File(outDir, "sing-box")
            if (forceExtract || !outFile.exists() || outFile.length() == 0L) {
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            try { outFile.setExecutable(true, false) } catch (_: Exception) {}
            try { Runtime.getRuntime().exec(arrayOf("chmod", "755", outFile.absolutePath)).waitFor() } catch (_: Exception) {}
            try { Runtime.getRuntime().exec(arrayOf("/system/bin/chmod", "755", outFile.absolutePath)).waitFor() } catch (_: Exception) {}

            Log.i("SingBoxRuntime", "Binary: ${outFile.absolutePath}, canExec=${outFile.canExecute()}, size=${outFile.length()}")

            if (outFile.canExecute()) {
                return PrepareResult(outFile, null)
            }

            return PrepareResult(outFile, "Binary extracted to ${outFile.absolutePath} but not executable (noexec on /data)")
        }

        return PrepareResult(null, "sing-box binary not found in assets for ABI: $abi")
    }

    fun writeConfig(configJson: String): File {
        val outDir = context.getDir("bin", Context.MODE_PRIVATE)
        return File(outDir, "config.json").apply {
            writeText(configJson)
        }
    }

    fun start(
        executable: File,
        configFile: File,
        onOutput: (String) -> Unit = {}
    ): StartResult {
        stop()
        return try {
            Log.i("SingBoxRuntime", "Starting: ${executable.absolutePath} run -c ${configFile.absolutePath}")

            val pb = ProcessBuilder(
                executable.absolutePath,
                "run",
                "-c",
                configFile.absolutePath
            )
            pb.redirectErrorStream(true)
            pb.environment()["TMPDIR"] = context.cacheDir.absolutePath

            process = pb.start()

            val startedProcess = process ?: return StartResult(false, "process was not created")
            outputThread = Thread({
                startedProcess.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        onOutput(line)
                        Log.d("SingBoxRuntime", line)
                    }
                }
            }, "SingBoxOutput").apply {
                isDaemon = true
                start()
            }

            Thread.sleep(800)
            if (!startedProcess.isAlive) {
                StartResult(false, "process exited early with code ${startedProcess.exitValue()}")
            } else {
                StartResult(true)
            }
        } catch (e: Exception) {
            Log.e("SingBoxRuntime", "Failed to start sing-box: ${e.message}", e)
            StartResult(false, e.message)
        }
    }

    fun stop() {
        val currentProcess = process
        if (currentProcess != null) {
            try {
                currentProcess.destroy()
                if (!currentProcess.waitFor(1200, TimeUnit.MILLISECONDS)) {
                    currentProcess.destroyForcibly()
                }
            } catch (e: Exception) {
                currentProcess.destroyForcibly()
            }
        }
        outputThread?.interrupt()
        outputThread = null
        process = null
    }

    private fun existsInAssets(path: String): Boolean {
        return try {
            context.assets.open(path).close()
            true
        } catch (e: Exception) {
            false
        }
    }
}
