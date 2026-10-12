package com.v2ray.ang.handler

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.perqa.byebox.BuildConfig
import com.tencent.mmkv.MMKV
import java.io.File
import java.io.RandomAccessFile
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Безопасное хранение сырого MMKV-ключа и состояния шифрования хранилищ.
 *
 * Ключ шифрования MMKV (16 печатных ASCII-байт) НЕ хранится ни в MMKV, ни в
 * открытом виде. Он оборачивается AES/GCM-ключом из Android Keystore
 * (alias [KEYSTORE_ALIAS]) и кладётся в base64(iv + ciphertext) в отдельный
 * SharedPreferences-файл "byebox_secure".
 *
 * В том же файле лежит состояние каждого хранилища:
 *  - [STATE_ENCRYPTED] — файл уже перешифрован этим ключом;
 *  - [STATE_LOST]      — ключ был утерян (сброс Keystore), данные не расшифровать;
 *  - отсутствует       — файл ещё не зашифрован (совместимо с апгрейдом).
 *
 * Keystore-ключ device-specific, поэтому в резервную копию кладётся сырой ключ
 * (см. SettingsBackup), иначе восстановление на другом устройстве невозможно.
 */
object MmkvKeyStore {

    /**
     * ID всех MMKV-хранилищ приложения. Единый список нужен, чтобы корректно
     * повторить шифрование/переключить ключ во всех уже открытых инстансах.
     * ВАЖНО: держать в синхроне с приватными ID_* в [MmkvManager].
     */
    val STORAGE_IDS = listOf(
        "MAIN",
        "PROFILE_FULL_CONFIG",
        "SERVER_RAW",
        "SERVER_AFF",
        "SUB",
        "ASSET",
        "SETTING",
    )

    const val STATE_ENCRYPTED = "enc"
    const val STATE_LOST = "lost"

    private const val TAG = BuildConfig.APPLICATION_ID
    private const val PREFS_NAME = "byebox_secure"
    private const val KEY_WRAPPED = "mmkv_raw_key_wrapped"
    private const val STATE_PREFIX = "mmkv_state_"
    private const val LOCK_FILE_NAME = "byebox_mmkv_key.lock"

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEYSTORE_ALIAS = "byebox_mmkv_wrapper"
    private const val WRAP_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128
    private const val WRAP_KEY_SIZE = 256

    /** Длина сырого MMKV-ключа (MMKV принимает ключ не длиннее 16 байт). */
    const val RAW_KEY_LENGTH = 16

    /** Алфавит сырого ключа: печатные ASCII, ровно 1 байт на символ. */
    private const val KEY_ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

    @Volatile
    private var cachedRawKey: String? = null
    private val memLock = Any()

    //region key

    /**
     * Возвращает сырой MMKV-ключ, при необходимости создавая его.
     *
     * @return ключ (16 ASCII-символов) либо null, если Keystore недоступен
     *         (тогда вызывающий код обязан работать без шифрования).
     */
    fun rawKeyOrNull(context: Context): String? {
        cachedRawKey?.let { return it }
        synchronized(memLock) {
            cachedRawKey?.let { return it }
            val key = withProcessLock(context) { loadOrCreateKeyLocked(context) }
            if (key != null) {
                cachedRawKey = key
            }
            return key
        }
    }

    /** Ключ, если шифрование уже активно в этом процессе, иначе null. */
    fun currentRawKeyOrNull(context: Context): String? = rawKeyOrNull(context)

    /** Проверка, что строка действительно является сырым MMKV-ключом. */
    fun isValidRawKey(rawKey: String): Boolean =
        rawKey.length == RAW_KEY_LENGTH && rawKey.all { it in KEY_ALPHABET }

    /** Есть ли хотя бы одно зашифрованное хранилище. */
    fun hasEncryptedStorage(context: Context): Boolean =
        STORAGE_IDS.any { storageState(context, it) == STATE_ENCRYPTED }

    /** Список ID зашифрованных хранилищ (для резервной копии). */
    fun encryptedStorageIds(context: Context): List<String> =
        STORAGE_IDS.filter { storageState(context, it) == STATE_ENCRYPTED }

    /**
     * Читает/создаёт ключ, удерживая межпроцессную блокировку (вызывать только
     * внутри [withProcessLock]).
     *
     * Если сохранённый ключ не удаётся расшифровать (Keystore сброшен),
     * генерируется новый, а ранее зашифрованные хранилища помечаются
     * [STATE_LOST] — их данные восстановить нельзя, это неизбежно.
     */
    private fun loadOrCreateKeyLocked(context: Context): String? {
        val prefs = prefs(context)
        val stored = prefs.getString(KEY_WRAPPED, null)
        if (!stored.isNullOrEmpty()) {
            val unwrapped = unwrapKey(stored)
            if (unwrapped != null) {
                return unwrapped
            }
            Log.e(TAG, "MMKV: сохранённый ключ не расшифрован, ключ будет пересоздан")
            markAllEncryptedAsLost(prefs)
        }

        val newKey = generateRawKey()
        if (!wrapAndStore(prefs, newKey)) {
            Log.e(TAG, "MMKV: Keystore недоступен, шифрование временно отключено")
            return null
        }
        return newKey
    }

    private fun generateRawKey(): String {
        val random = SecureRandom()
        val builder = StringBuilder(RAW_KEY_LENGTH)
        repeat(RAW_KEY_LENGTH) {
            builder.append(KEY_ALPHABET[random.nextInt(KEY_ALPHABET.length)])
        }
        return builder.toString()
    }

    /** Оборачивает ключ Keystore-ключом и сохраняет base64(iv + ciphertext). */
    private fun wrapAndStore(prefs: SharedPreferences, rawKey: String): Boolean {
        return try {
            val secret = getOrCreateKeystoreKey() ?: return false
            val cipher = Cipher.getInstance(WRAP_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secret)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(rawKey.toByteArray(Charsets.UTF_8))
            val blob = ByteArray(iv.size + ciphertext.size)
            System.arraycopy(iv, 0, blob, 0, iv.size)
            System.arraycopy(ciphertext, 0, blob, iv.size, ciphertext.size)
            val encoded = Base64.encodeToString(blob, Base64.NO_WRAP)
            prefs.edit().putString(KEY_WRAPPED, encoded).commit()
        } catch (t: Throwable) {
            Log.e(TAG, "MMKV: не удалось зашифровать ключ", t)
            false
        }
    }

    /** Расшифровывает ключ Keystore-ключом; null при любой ошибке. */
    private fun unwrapKey(stored: String): String? {
        return try {
            val blob = Base64.decode(stored, Base64.NO_WRAP)
            if (blob.size <= GCM_IV_LENGTH) {
                return null
            }
            val iv = blob.copyOfRange(0, GCM_IV_LENGTH)
            val ciphertext = blob.copyOfRange(GCM_IV_LENGTH, blob.size)
            val secret = getExistingKeystoreKey() ?: return null
            val cipher = Cipher.getInstance(WRAP_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secret, GCMParameterSpec(GCM_TAG_BITS, iv))
            val rawKey = String(cipher.doFinal(ciphertext), Charsets.UTF_8)
            rawKey.takeIf { isValidRawKey(it) }
        } catch (t: Throwable) {
            Log.w(TAG, "MMKV: не удалось расшифровать ключ из Keystore", t)
            null
        }
    }

    private fun getOrCreateKeystoreKey(): SecretKey? {
        getExistingKeystoreKey()?.let { return it }
        return try {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            generator.init(
                KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(WRAP_KEY_SIZE)
                    .build()
            )
            generator.generateKey()
        } catch (t: Throwable) {
            Log.e(TAG, "MMKV: не удалось создать Keystore-ключ", t)
            null
        }
    }

    private fun getExistingKeystoreKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            (keyStore.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
        } catch (t: Throwable) {
            Log.w(TAG, "MMKV: Keystore недоступен", t)
            null
        }
    }

    //endregion

    //region state

    /** Состояние хранилища: [STATE_ENCRYPTED], [STATE_LOST] или null. */
    fun storageState(context: Context, id: String): String? =
        prefs(context).getString(STATE_PREFIX + id, null)

    /** Помечает хранилище как зашифрованное. Вызывать только после успешного reKey. */
    fun markEncrypted(context: Context, id: String): Boolean =
        prefs(context).edit().putString(STATE_PREFIX + id, STATE_ENCRYPTED).commit()

    /**
     * Сбрасывает конкретное хранилище: удаляет его файл, чтобы создать заново
     * с новым ключом. Используется только когда Keystore-ключ утерян и данные
     * всё равно не расшифровать.
     */
    fun resetStorage(context: Context, id: String) {
        try {
            MMKV.removeStorage(id)
        } catch (t: Throwable) {
            Log.e(TAG, "MMKV: не удалось сбросить хранилище '$id'", t)
        }
        prefs(context).edit().remove(STATE_PREFIX + id).commit()
    }

    private fun markAllEncryptedAsLost(prefs: SharedPreferences) {
        val editor = prefs.edit()
        STORAGE_IDS.forEach { id ->
            if (prefs.getString(STATE_PREFIX + id, null) == STATE_ENCRYPTED) {
                editor.putString(STATE_PREFIX + id, STATE_LOST)
            }
        }
        editor.commit()
    }

    //endregion

    //region backup import

    /**
     * Импортирует сырой ключ из архива: переоборачивает его под локальный
     * Keystore и выставляет состояния хранилищ из [encryptedIds].
     *
     * @return true при успехе; false — если ключ некорректен или Keystore недоступен
     *         (тогда импорт нужно прервать, чтобы не повредить данные).
     */
    fun importKey(context: Context, rawKey: String, encryptedIds: Set<String>): Boolean {
        if (!isValidRawKey(rawKey)) {
            return false
        }
        synchronized(memLock) {
            val prefs = prefs(context)
            if (!wrapAndStore(prefs, rawKey)) {
                return false
            }
            val editor = prefs.edit()
            STORAGE_IDS.forEach { id ->
                if (id in encryptedIds) {
                    editor.putString(STATE_PREFIX + id, STATE_ENCRYPTED)
                } else {
                    editor.remove(STATE_PREFIX + id)
                }
            }
            if (!editor.commit()) {
                return false
            }
            cachedRawKey = rawKey
            return true
        }
    }

    /**
     * Подготовка к импорту старого архива без ключа: данные в нём не зашифрованы,
     * поэтому состояния сбрасываются — при следующем запуске MMKV зашифруется
     * текущим локальным ключом.
     */
    fun preparePlaintextImport(context: Context) {
        synchronized(memLock) {
            val editor = prefs(context).edit()
            STORAGE_IDS.forEach { editor.remove(STATE_PREFIX + it) }
            editor.commit()
            // cachedRawKey не сбрасываем: ключ ещё понадобится при повторном шифровании.
        }
    }

    /**
     * Приводит уже открытые в процессе MMKV-инстансы к ключу [cryptKey]
     * (null — снять шифрование). Делается ДО восстановления файлов, иначе
     * MMKV перечитает новые данные старым ключом и потеряет их.
     */
    fun alignLiveInstances(cryptKey: String?, encryptedIds: Set<String>) {
        STORAGE_IDS.forEach { id ->
            try {
                val kv = MMKV.mmkvWithID(id, MMKV.MULTI_PROCESS_MODE)
                kv.checkReSetCryptKey(if (id in encryptedIds) cryptKey else null)
            } catch (t: Throwable) {
                Log.w(TAG, "MMKV: не удалось переключить ключ для '$id'", t)
            }
        }
    }

    //endregion

    //region helpers

    @Suppress("DEPRECATION")
    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE or Context.MODE_MULTI_PROCESS,
        )

    /**
     * Межпроцессная блокировка на время чтения/создания ключа, чтобы два
     * процесса не сгенерировали разные ключи. При недоступности блокировки
     * продолжаем (внутрипроцессный [memLock] всё равно удерживается).
     */
    private fun <T> withProcessLock(context: Context, block: () -> T): T {
        val lockFile = File(context.filesDir, LOCK_FILE_NAME)
        val file = try {
            RandomAccessFile(lockFile, "rw")
        } catch (t: Throwable) {
            Log.w(TAG, "MMKV: файловая блокировка ключа недоступна", t)
            return block()
        }
        return try {
            file.use { raf ->
                val lock = try {
                    raf.channel.lock()
                } catch (t: Throwable) {
                    // Не удалось взять межпроцессную блокировку — работаем под memLock.
                    Log.w(TAG, "MMKV: не удалось взять файловую блокировку ключа", t)
                    return@use block()
                }
                try {
                    block()
                } finally {
                    lock.release()
                }
            }
        } catch (t: Throwable) {
            // Ошибка уже внутри block() — не повторяем его, только сообщаем.
            Log.e(TAG, "MMKV: ошибка при работе с ключом", t)
            throw t
        }
    }

    //endregion
}
