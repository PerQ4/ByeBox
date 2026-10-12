package com.perqa.byebox.data

import android.app.Application
import android.util.Base64
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Раньше класс работал в «голом» JUnit, где android.util.Base64 бросал
 * RuntimeException. Ветки VMESS/Shadowsocks из-за этого были помечены
 * @Ignore, а тест на «сломанный» конфиг проходил лишь потому, что
 * ProxyConfig.toConfigLink() глотает все исключения и возвращает "".
 * Robolectric даёт настоящий android.util.Base64, поэтому все ветки теперь
 * проверяются детерминированно.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class ProxyConfigTest {

    private val baseConfig = ProxyConfig(
        id = "test-id",
        name = "Test Server",
        description = "A test server",
        protocol = "VLESS",
        address = "server.example.com",
        port = 443,
        uuid = "ac94bf12-9518-57e3-9433-06e1d3fcab1d",
        flow = "xtls-rprx-vision",
        security = "reality",
        sni = "microsoft.com",
        pbk = "Ovu-MEOWU1tUI8ppfuaGosmbiQLFaVY8YwZLKfMDhF4",
        sid = "2c0157f1",
        network = "tcp",
        ping = 42,
        failureCount = 0,
        sourceName = "My Subscription",
        countryFlag = "🇩🇪"
    )

    @Test
    fun `toJson and fromJson round-trip preserves all fields`() {
        val json = baseConfig.toJson()
        val restored = ProxyConfig.fromJson(json)

        assertEquals(baseConfig.id, restored.id)
        assertEquals(baseConfig.name, restored.name)
        assertEquals(baseConfig.description, restored.description)
        assertEquals(baseConfig.protocol, restored.protocol)
        assertEquals(baseConfig.address, restored.address)
        assertEquals(baseConfig.port, restored.port)
        assertEquals(baseConfig.uuid, restored.uuid)
        assertEquals(baseConfig.flow, restored.flow)
        assertEquals(baseConfig.security, restored.security)
        assertEquals(baseConfig.sni, restored.sni)
        assertEquals(baseConfig.pbk, restored.pbk)
        assertEquals(baseConfig.sid, restored.sid)
        assertEquals(baseConfig.network, restored.network)
        assertEquals(baseConfig.ping, restored.ping)
        assertEquals(baseConfig.failureCount, restored.failureCount)
        assertEquals(baseConfig.sourceName, restored.sourceName)
        assertEquals(baseConfig.countryFlag, restored.countryFlag)
    }

    @Test
    fun `fromJson handles missing optional fields`() {
        val json = JSONObject()
            .put("id", "min-id")
            .put("name", "Minimal")
            .put("protocol", "VLESS")
            .put("address", "x.com")
            .put("port", 443)
            .put("uuid", "00001111-2222-3333-4444-555566667777")

        val config = ProxyConfig.fromJson(json)
        assertEquals("min-id", config.id)
        assertEquals("Minimal", config.name)
        assertEquals("VLESS", config.protocol)
        assertEquals("x.com", config.address)
        assertEquals(443, config.port)
        assertNull(config.flow)
        assertNull(config.security)
        assertNull(config.sni)
        assertNull(config.pbk)
        assertNull(config.sid)
        assertNull(config.network)
        assertNull(config.ping)
        assertEquals(0, config.failureCount)
        assertEquals("Local Configs", config.sourceName)
        assertEquals("🌍", config.countryFlag)
    }

    @Test
    fun `fromJson handles alternative field names`() {
        val json = JSONObject()
            .put("id", "alt-id")
            .put("name", "Alt")
            .put("remarks", "Alternative name")
            .put("protocol", "VMESS")
            .put("address", "alt.com")
            .put("port", 8443)
            .put("uuid", "aaaabbbb-cccc-dddd-eeee-ffff00001111")
            .put("wsPath", "/ws")
            .put("wsHost", "example.com")
            .put("countryFlag", "🇯🇵")
            .put("failureCount", 2)

        val config = ProxyConfig.fromJson(json)
        assertEquals("Alternative name", config.description)
        assertEquals("/ws", config.wsPath)
        assertEquals("example.com", config.wsHost)
        assertEquals("🇯🇵", config.countryFlag)
        assertEquals(2, config.failureCount)
    }

    @Test
    fun `toConfigLink for VLESS with reality`() {
        val link = baseConfig.toConfigLink()
        assertTrue(link.startsWith("vless://"))
        assertTrue(link.contains("ac94bf12-9518-57e3-9433-06e1d3fcab1d"))
        assertTrue(link.contains("security=reality"))
        assertTrue(link.contains("sni=microsoft.com"))
        assertTrue(link.contains("pbk=Ovu-MEOWU1tUI8ppfuaGosmbiQLFaVY8YwZLKfMDhF4"))
        assertTrue(link.contains("sid=2c0157f1"))
        assertTrue(link.contains("flow=xtls-rprx-vision"))
        assertTrue(link.contains("type=tcp"))
        assertTrue(link.contains("#Test Server") || link.contains("#Test%20Server"))
    }

    @Test
    fun `toConfigLink for VMESS produces base64 JSON with core fields`() {
        val vmess = baseConfig.copy(
            protocol = "VMESS",
            security = "tls",
            network = "ws",
            wsPath = "/ws",
            wsHost = "host.com"
        )
        val link = vmess.toConfigLink()
        assertTrue("VMESS link should start with vmess://", link.startsWith("vmess://"))

        // Полезная нагрузка — это JSON, закодированный в base64 без переносов.
        val decoded = String(Base64.decode(link.removePrefix("vmess://"), Base64.NO_WRAP))
        val json = JSONObject(decoded)
        assertEquals("2", json.getString("v"))
        assertEquals("Test Server", json.getString("ps"))
        assertEquals("server.example.com", json.getString("add"))
        assertEquals(443, json.getInt("port"))
        assertEquals(baseConfig.uuid, json.getString("id"))
        assertEquals("tls", json.getString("tls"))
        assertEquals("tls", json.getString("scy"))
        assertEquals("ws", json.getString("net"))
        assertEquals("none", json.getString("type"))
        assertEquals("/ws", json.getString("path"))
        assertEquals("host.com", json.getString("host"))
        assertEquals("microsoft.com", json.getString("sni"))
    }

    @Test
    fun `toConfigLink for Trojan`() {
        val trojan = baseConfig.copy(protocol = "Trojan", uuid = "trojan-pass", security = "tls", sni = "sni.tr.com")
        val link = trojan.toConfigLink()
        assertTrue(link.startsWith("trojan://"))
        assertTrue(link.contains("trojan-pass"))
        assertTrue(link.contains("sni=sni.tr.com"))
    }

    @Test
    fun `toConfigLink for Shadowsocks base64-encodes the password`() {
        val ss = baseConfig.copy(protocol = "Shadowsocks", uuid = "mypassword")
        val link = ss.toConfigLink()
        assertTrue("Shadowsocks link should start with ss://", link.startsWith("ss://"))
        assertTrue(link.contains("@server.example.com:443"))

        val userInfo = link.removePrefix("ss://").substringBefore('@')
        assertEquals("mypassword", String(Base64.decode(userInfo, Base64.NO_WRAP)))
    }

    @Test
    fun `toConfigLink for empty protocol falls back to the ss branch`() {
        // Пустой протокол не бросает исключение: он попадает в ветку else
        // (Shadowsocks-подобная ссылка). Тест фиксирует реальное поведение и
        // больше не зависит от RuntimeException внутри android.util.Base64.
        val broken = ProxyConfig(
            id = "bad", name = "", protocol = "", address = "", port = 0, uuid = ""
        )
        val link = broken.toConfigLink()
        assertTrue("empty protocol should fall back to the ss:// branch", link.startsWith("ss://"))
    }

    @Test
    fun `default countryFlag is globe emoji`() {
        val config = ProxyConfig(
            id = "1", name = "n", protocol = "VLESS", address = "a", port = 1, uuid = "u"
        )
        assertEquals("🌍", config.countryFlag)
    }

    @Test
    fun `toJson does not include null optionals`() {
        val minimal = ProxyConfig(
            id = "m", name = "m", protocol = "SS", address = "a", port = 1, uuid = "u"
        )
        val json = minimal.toJson()
        assertNull(json.opt("description"))
        assertNull(json.opt("flow"))
        assertNull(json.opt("security"))
        assertNull(json.opt("ping"))
        assertNull(json.opt("ws_path"))
    }
}
