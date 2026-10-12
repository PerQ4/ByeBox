package com.perqa.byebox.core

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import com.tencent.mmkv.MMKV
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvKeyStore
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

    /** Имя файла с сырым MMKV-ключом внутри архива. */
    private const val KEY_ENTRY = "mmkv_key.b64"

    /** Список ID зашифрованных хранилищ внутри архива (по одному на строку через запятую). */
    private const val ENCRYPTED_IDS_ENTRY = "mmkv_encrypted_ids.txt"

    /** Максимальное число записей в импортируемом архиве. */
    private const val MAX_ZIP_ENTRIES = 512

    /** Максимальный суммарный размер распакованных данных (защита от zip-bomb). */
    private const val MAX_UNCOMPRESSED_BYTES = 64L * 1024 * 1024

    /**
     * Exports all settings into a zip archive stored in the user's Downloads folder.
     *
     * На API 29+ используется MediaStore.Downloads (публичные «Загрузки»).
     * На API 24–28 публичный каталог требует WRITE_EXTERNAL_STORAGE, которого у
     * приложения нет, поэтому архив пишется в app-private внешний каталог, а
     * возвращаемая строка содержит путь, чтобы пользователь видел, где файл.
     *
     * @return имя/путь созданного файла, либо null при ошибке.
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

        // После включения шифрования MMKV файлы хранятся зашифрованными. Их
        // keystore-обёртка device-specific, поэтому для восстановления на другом
        // устройстве в архив дополнительно кладём сырой ключ. Безопасность это
        // не ухудшает: прочие секреты (серверы/пароли) и так лежат в архиве.
        exportMmkvKeyIfEncrypted(context, workDir)

        val srcSp = File(context.filesDir.parentFile, "shared_prefs")
        val srcFile = File(srcSp, "$PREFS_NAME.xml")
        if (srcFile.exists()) {
            srcFile.copyTo(File(spDest, "$PREFS_NAME.xml"), overwrite = true)
        }

        val displayName = "byebox-settings-${System.currentTimeMillis()}.zip"
        val zipFile = File(context.cacheDir, displayName)
        return try {
            zipDirectory(workDir, zipFile)
            // saveToDownloads возвращает строку для показа пользователю
            // (имя файла либо имя+путь), null — при ошибке.
            saveToDownloads(context, zipFile, displayName)
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
                // Сначала применяем ключ из архива (если он есть) и переключаем уже
                // открытые MMKV на нужный ключ ДО восстановления файлов. Иначе MMKV
                // перечитает новые данные старым ключом и потеряет их.
                if (!applyImportedMmkvKey(context, workDir)) {
                    LogUtil.e(AppConfig.TAG, "Backup: импорт ключа MMKV не выполнен")
                    return false
                }
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

    //region MMKV encryption key

    /**
     * Складывает сырой MMKV-ключ и список зашифрованных хранилищ в каталог архива.
     * Делается только если хотя бы одно хранилище реально зашифровано.
     */
    private fun exportMmkvKeyIfEncrypted(context: Context, workDir: File) {
        try {
            if (!MmkvKeyStore.hasEncryptedStorage(context)) return
            val rawKey = MmkvKeyStore.currentRawKeyOrNull(context) ?: return
            val keyB64 = Base64.encodeToString(rawKey.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            File(workDir, KEY_ENTRY).writeText(keyB64)
            File(workDir, ENCRYPTED_IDS_ENTRY)
                .writeText(MmkvKeyStore.encryptedStorageIds(context).joinToString(","))
        } catch (t: Throwable) {
            LogUtil.w(AppConfig.TAG, "Backup: не удалось сохранить MMKV-ключ в архив", t)
        }
    }

    /**
     * Применяет ключ MMKV из архива к локальному защищённому хранилищу и
     * переключает уже открытые инстансы MMKV на этот ключ.
     *
     *  - Если [KEY_ENTRY] есть: переоборачиваем ключ под локальный Keystore,
     *    выставляем состояния восстановленных хранилищ и синхронизируем crypter.
     *  - Если ключа нет (старый архив): данные не зашифрованы — сбрасываем
     *    состояния и снимаем шифрование с живых инстансов, чтобы MMKV не потерял
     *    их при перечитывании.
     *
     * @return false, если ключ повреждён/некорректен или Keystore недоступен —
     *         тогда импорт прерывается, чтобы не повредить данные.
     */
    private fun applyImportedMmkvKey(context: Context, workDir: File): Boolean {
        val keyFile = File(workDir, KEY_ENTRY)
        if (!keyFile.exists()) {
            MmkvKeyStore.preparePlaintextImport(context)
            MmkvKeyStore.alignLiveInstances(null, emptySet())
            return true
        }

        val rawKey = try {
            val keyB64 = keyFile.readText().trim()
            String(Base64.decode(keyB64, Base64.NO_WRAP), Charsets.UTF_8)
        } catch (t: Throwable) {
            LogUtil.e(AppConfig.TAG, "Backup: повреждён MMKV-ключ в архиве", t)
            return false
        }
        if (!MmkvKeyStore.isValidRawKey(rawKey)) {
            LogUtil.e(AppConfig.TAG, "Backup: некорректный MMKV-ключ в архиве")
            return false
        }

        val encryptedIds = readEncryptedIds(workDir)
        if (!MmkvKeyStore.importKey(context, rawKey, encryptedIds)) {
            return false
        }
        MmkvKeyStore.alignLiveInstances(rawKey, encryptedIds)
        return true
    }

    /**
     * Читает список зашифрованных хранилищ из архива. Если файла нет, но ключ
     * присутствует (архив более ранней версии этой функции) — считаем
     * зашифрованными все известные хранилища.
     */
    private fun readEncryptedIds(workDir: File): Set<String> {
        val idsFile = File(workDir, ENCRYPTED_IDS_ENTRY)
        if (!idsFile.exists()) return MmkvKeyStore.STORAGE_IDS.toSet()
        return try {
            idsFile.readText()
                .split(',')
                .map { it.trim() }
                .filter { it in MmkvKeyStore.STORAGE_IDS }
                .toSet()
        } catch (t: Throwable) {
            LogUtil.w(AppConfig.TAG, "Backup: не удалось прочитать список зашифрованных MMKV", t)
            MmkvKeyStore.STORAGE_IDS.toSet()
        }
    }

    //endregion

    /**
     * Сохраняет архив: на API 29+ — в публичные «Загрузки» через MediaStore,
     * на API<29 — в app-private внешний каталог.
     *
     * @return строка для показа пользователю (имя файла либо имя+путь), null при ошибке.
     */
    private fun saveToDownloads(context: Context, zipFile: File, displayName: String): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = context.contentResolver.insert(collection, values) ?: return null
            val ok = context.contentResolver.openOutputStream(itemUri)?.use { out ->
                zipFile.inputStream().use { it.copyTo(out) }
                true
            } ?: false
            return if (ok) displayName else null
        } else {
            // API 24–28: без разрешения WRITE_EXTERNAL_STORAGE публичный
            // Environment.getExternalStoragePublicDirectory() кидает
            // SecurityException. Разрешение в манифесте не запрашиваем,
            // поэтому пишем в app-private внешний каталог (разрешений не
            // требует, удаляется вместе с приложением).
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            if (!dir.exists() && !dir.mkdirs()) return null
            val outFile = File(dir, displayName)
            zipFile.inputStream().use { input -> FileOutputStream(outFile).use { input.copyTo(it) } }
            LogUtil.w(
                AppConfig.TAG,
                "Backup: API<29 — сохранено в app-private каталог: ${outFile.absolutePath}"
            )
            // Возвращаем не только имя, но и реальный путь: сообщение об успехе
            // показывает его пользователю, т.к. файл лежит не в «Загрузках».
            return "${outFile.name} (${outFile.parentFile?.absolutePath})"
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
