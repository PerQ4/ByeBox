package com.perqa.byebox.core

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.tencent.mmkv.MMKV
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Exports and imports the full application state (MMKV stores + SharedPreferences)
 * so user data (server profiles, subscriptions, UI settings) survives a reinstall
 * caused by a signing-key mismatch.
 */
object SettingsBackup {
    private const val PREFS_NAME = "byebox_settings"
    private const val MMKV_ENTRY = "mmkv"
    private const val SP_ENTRY = "shared_prefs"

    /** Максимальное число записей в импортируемом архиве. */
    private const val MAX_ZIP_ENTRIES = 512

    /** Максимальный суммарный размер распакованных данных (защита от zip-bomb). */
    private const val MAX_UNCOMPRESSED_BYTES = 64L * 1024 * 1024

    /**
     * Exports all settings into a zip archive stored in the user's Downloads folder.
     * @return the created file's display name, or null on failure.
     */
    fun exportToDownloads(context: Context): String? {
        val workDir = File(context.cacheDir, "byebox_backup_${System.currentTimeMillis()}")
        val mmkvDest = File(workDir, MMKV_ENTRY)
        val spDest = File(workDir, SP_ENTRY)
        if (!mmkvDest.mkdirs() || !spDest.mkdirs()) return null

        val count = MMKV.backupAllToDirectory(mmkvDest.absolutePath)
        if (count <= 0) {
            workDir.deleteRecursively()
            return null
        }

        val srcSp = File(context.filesDir.parentFile, "shared_prefs")
        val srcFile = File(srcSp, "$PREFS_NAME.xml")
        if (srcFile.exists()) {
            srcFile.copyTo(File(spDest, "$PREFS_NAME.xml"), overwrite = true)
        }

        val displayName = "byebox-settings-${System.currentTimeMillis()}.zip"
        val zipFile = File(context.cacheDir, displayName)
        return try {
            zipDirectory(workDir, zipFile)
            val saved = saveToDownloads(context, zipFile, displayName)
            if (saved) displayName else null
        } catch (e: Exception) {
            null
        } finally {
            workDir.deleteRecursively()
            zipFile.delete()
        }
    }

    /**
     * Restores settings from a previously exported zip archive.
     * @return true if the archive was applied successfully.
     */
    fun importFromUri(context: Context, uri: Uri): Boolean {
        val workDir = File(context.cacheDir, "byebox_restore_${System.currentTimeMillis()}")
        if (!workDir.mkdirs()) return false
        val zipFile = File(workDir, "restore.zip")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(zipFile).use { output -> input.copyTo(output) }
            } ?: return false

            val topLevelEntries = unzip(zipFile, workDir)

            // Архив должен содержать хотя бы один из ожидаемых разделов бэкапа.
            if (MMKV_ENTRY !in topLevelEntries && SP_ENTRY !in topLevelEntries) {
                LogUtil.w(AppConfig.TAG, "Backup: не найдены ожидаемые записи ($MMKV_ENTRY/$SP_ENTRY)")
                return false
            }

            val mmkvSrc = File(workDir, MMKV_ENTRY)
            if (mmkvSrc.exists() && mmkvSrc.isDirectory) {
                MMKV.restoreAllFromDirectory(mmkvSrc.absolutePath)
            }

            val spSrc = File(workDir, SP_ENTRY)
            if (spSrc.exists() && spSrc.isDirectory) {
                val spDst = File(context.filesDir.parentFile, "shared_prefs")
                spDst.mkdirs()
                spSrc.listFiles().orEmpty().forEach { f ->
                    f.copyTo(File(spDst, f.name), overwrite = true)
                }
            }
            true
        } catch (e: Exception) {
            false
        } finally {
            workDir.deleteRecursively()
        }
    }

    private fun saveToDownloads(context: Context, zipFile: File, displayName: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = context.contentResolver.insert(collection, values) ?: return false
            return context.contentResolver.openOutputStream(itemUri)?.use { out ->
                zipFile.inputStream().use { it.copyTo(out) }
                true
            } ?: false
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists() && !dir.mkdirs()) return false
            val outFile = File(dir, displayName)
            zipFile.inputStream().use { input -> FileOutputStream(outFile).use { input.copyTo(it) } }
            return true
        }
    }

    private fun zipDirectory(srcDir: File, zipFile: File) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            srcDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val entryName = file.relativeTo(srcDir).path.replace('\\', '/')
                zos.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    /**
     * Распаковывает архив в [destDir], защищаясь от Zip Slip и zip-bomb.
     *
     * Все записи обязаны попадать строго внутрь [destDir] (проверка по canonicalPath),
     * число записей и суммарный распакованный размер ограничены.
     *
     * @return множество имён записей верхнего уровня.
     * @throws IllegalStateException при небезопасной записи или превышении лимитов.
     */
    private fun unzip(zipFile: File, destDir: File): Set<String> {
        val destRoot = destDir.canonicalFile
        val destPrefix = destRoot.path + File.separator
        val topLevelEntries = mutableSetOf<String>()
        var entryCount = 0
        var totalBytes = 0L

        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entryCount++
                if (entryCount > MAX_ZIP_ENTRIES) {
                    throw IllegalStateException("archive has too many entries")
                }

                val name = entry.name.orEmpty().replace('\\', '/')
                val outFile = File(destDir, name)
                val outPath = outFile.canonicalFile.path

                // Запись должна находиться внутри destDir: отсекаем абсолютные пути,
                // ".." и иные попытки выйти за пределы каталога распаковки.
                if (outPath != destRoot.path && !outPath.startsWith(destPrefix)) {
                    throw IllegalStateException("unsafe zip entry: $name")
                }

                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val n = zis.read(buffer)
                            if (n < 0) break
                            totalBytes += n
                            if (totalBytes > MAX_UNCOMPRESSED_BYTES) {
                                throw IllegalStateException("archive exceeds size limit")
                            }
                            fos.write(buffer, 0, n)
                        }
                    }
                }

                name.trimStart('/').substringBefore('/')
                    .takeIf { it.isNotEmpty() }
                    ?.let { topLevelEntries.add(it) }

                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return topLevelEntries
    }
}
