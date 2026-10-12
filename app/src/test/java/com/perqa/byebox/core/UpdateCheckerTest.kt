package com.perqa.byebox.core

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты апдейтера. Публичного API у [UpdateChecker] немного: сетевой
 * `check()` без правок прод-кода и без сети не проверить, поэтому основной
 * упор — на чистое [SemVer]-сравнение, [UpdateChannel] и вычисляемые поля
 * [UpdateInfo]. Самая рискованная приватная логика (`extractVersionCode`,
 * `pickApkAsset`) вызывается через рефлексию: прод-код не меняется.
 */
class UpdateCheckerTest {

    private fun sampleInfo(
        latestVersion: String = "1.2.0",
        stage: String = "",
        stageNumber: Int = 0,
        prerelease: Boolean = false,
    ) = UpdateInfo(
        latestVersion = latestVersion,
        latestVersionCode = 100,
        apkUrl = "https://example.com/app.apk",
        apkSha256 = "",
        apkSizeBytes = 0L,
        downloadUrl = "https://example.com/release",
        releaseNotes = "",
        publishedAt = "",
        prerelease = prerelease,
        stage = stage,
        stageNumber = stageNumber,
    )

    // --- UpdateInfo: вычисляемые свойства ----------------------------------

    @Test
    fun `stable release is not a pre-release and shows a clean version`() {
        val info = sampleInfo()
        assertFalse(info.isPreRelease)
        assertEquals("1.2.0", info.displayVersion)
    }

    @Test
    fun `stage release is a pre-release and shows an English stage label`() {
        val info = sampleInfo(latestVersion = "1.3.0-beta.2", stage = "beta", stageNumber = 2, prerelease = true)
        assertTrue(info.isPreRelease)
        assertEquals("1.3.0-beta.2 (Beta 2)", info.displayVersion)
    }

    @Test
    fun `prerelease flag alone marks pre-release but keeps the plain version`() {
        val info = sampleInfo(prerelease = true)
        assertTrue(info.isPreRelease)
        assertEquals("1.2.0", info.displayVersion)
    }

    @Test
    fun `rc stage is rendered capitalized`() {
        val info = sampleInfo(latestVersion = "2.0.0-rc.1", stage = "rc", stageNumber = 1)
        assertEquals("2.0.0-rc.1 (Rc 1)", info.displayVersion)
    }

    // --- UpdateChannel.fromPreference --------------------------------------

    @Test
    fun `update channel preference is case-insensitive and defaults to stable`() {
        assertEquals(UpdateChannel.STABLE, UpdateChannel.fromPreference(null))
        assertEquals(UpdateChannel.STABLE, UpdateChannel.fromPreference(""))
        assertEquals(UpdateChannel.STABLE, UpdateChannel.fromPreference("STABLE"))
        assertEquals(UpdateChannel.STABLE, UpdateChannel.fromPreference("stable"))
        assertEquals(UpdateChannel.DEV, UpdateChannel.fromPreference("DEV"))
        assertEquals(UpdateChannel.DEV, UpdateChannel.fromPreference("dev"))
        assertEquals(UpdateChannel.STABLE, UpdateChannel.fromPreference("garbage"))
    }

    // --- SemVer: пробелы, которые не покрыты SemVerTest --------------------

    @Test
    fun `numeric pre-release identifiers sort below alphanumeric ones`() {
        // SemVer §11: числовой идентификатор ниже буквенного.
        assertTrue(SemVer.parse("1.0.0-alpha.1")!! < SemVer.parse("1.0.0-alpha.beta")!!)
    }

    @Test
    fun `more pre-release identifiers win when the prefix is equal`() {
        assertTrue(SemVer.parse("1.0.0-alpha")!! < SemVer.parse("1.0.0-alpha.1")!!)
    }

    @Test
    fun `unknown or missing parts parse predictably`() {
        assertEquals(SemVer(1, 0, 0), SemVer.parse("1"))
        assertEquals(SemVer(1, 2, 0), SemVer.parse("v1.2"))
        assertNull(SemVer.parse("v"))
        assertNull(SemVer.parse("version-one"))
        // stage распознаётся только для известных стадий
        assertEquals("", SemVer.parse("1.0.0-snapshot.1")!!.stage)
        assertEquals(0, SemVer.parse("1.0.0-snapshot.abc")!!.stageNumber)
        // Второй идентификатор читается как stageNumber, даже если стадия неизвестна.
        assertEquals(1, SemVer.parse("1.0.0-snapshot.1")!!.stageNumber)
    }

    @Test
    fun `equal versions compare as equal regardless of the v prefix`() {
        assertEquals(0, SemVer.parse("1.2.0")!!.compareTo(SemVer.parse("v1.2.0")!!))
    }

    // --- Приватные помощники через рефлексию -------------------------------

    private val extractVersionCodeMethod =
        UpdateChecker::class.java.getDeclaredMethod("extractVersionCode", JSONObject::class.java)
            .apply { isAccessible = true }

    private val pickApkAssetMethod =
        UpdateChecker::class.java.getDeclaredMethod("pickApkAsset", JSONObject::class.java)
            .apply { isAccessible = true }

    private fun extractVersionCode(body: String, name: String = ""): Int? =
        extractVersionCodeMethod.invoke(UpdateChecker, JSONObject().put("body", body).put("name", name)) as Int?

    private fun pickApkAsset(vararg names: String): String? {
        val assets = JSONArray()
        names.forEach { assets.put(JSONObject().put("name", it)) }
        val release = JSONObject().put("assets", assets)
        val asset = pickApkAssetMethod.invoke(UpdateChecker, release) as JSONObject?
        return asset?.optString("name")
    }

    @Test
    fun `extractVersionCode reads the body marker`() {
        assertEquals(123, extractVersionCode("Release notes\n<!-- byebox:versionCode=123 -->\nmore"))
        assertEquals(0, extractVersionCode("<!-- byebox:versionCode=0 -->"))
        assertEquals(7, extractVersionCode("<!-- byebox:versionCode = 7 -->"))
    }

    @Test
    fun `extractVersionCode falls back to the release name`() {
        assertEquals(456, extractVersionCode("no marker here", "ByeBox 1.2.0 (456)"))
    }

    @Test
    fun `extractVersionCode prefers the body marker over the name`() {
        assertEquals(789, extractVersionCode("<!-- byebox:versionCode=789 -->", "ByeBox 1.2.0 (456)"))
    }

    @Test
    fun `extractVersionCode returns null without any marker`() {
        assertNull(extractVersionCode("just notes", "ByeBox 1.2.0"))
        assertNull(extractVersionCode("", ""))
    }

    @Test
    fun `pickApkAsset prefers universal then arm64 then any apk`() {
        assertEquals("universal.apk", pickApkAsset("x86.apk", "universal.apk", "arm64-v8a.apk"))
        assertEquals("arm64-v8a.apk", pickApkAsset("x86.apk", "arm64-v8a.apk"))
        assertEquals("fallback.apk", pickApkAsset("fallback.apk", "notes.txt"))
    }

    @Test
    fun `pickApkAsset matches the extension case-insensitively`() {
        assertEquals("APP.APK", pickApkAsset("APP.APK"))
    }

    @Test
    fun `pickApkAsset returns null without apk assets`() {
        assertNull(pickApkAsset())
        assertNull(pickApkAsset("notes.txt", "checksums.txt"))
    }
}
