package com.perqa.byebox.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

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

    @Test @Ignore("requires android.util.Base64 (Android SDK)")
    fun `toConfigLink for VMESS produces base64`() {
        val vmess = baseConfig.copy(protocol = "VMESS", security = "tls", network = "ws", wsPath = "/ws", wsHost = "host.com")
        val link = vmess.toConfigLink()
        assertTrue("VMESS link should start with vmess://", link.startsWith("vmess://"))
        assertTrue("VMESS link should be longer than 20 chars", link.length > 20)
    }

    @Test
    fun `toConfigLink for Trojan`() {
        val trojan = baseConfig.copy(protocol = "Trojan", uuid = "trojan-pass", security = "tls", sni = "sni.tr.com")
        val link = trojan.toConfigLink()
        assertTrue(link.startsWith("trojan://"))
        assertTrue(link.contains("trojan-pass"))
        assertTrue(link.contains("sni=sni.tr.com"))
    }

    @Test @Ignore("requires android.util.Base64 (Android SDK)")
    fun `toConfigLink for Shadowsocks`() {
        val ss = baseConfig.copy(protocol = "Shadowsocks", uuid = "mypassword")
        val link = ss.toConfigLink()
        assertTrue("Shadowsocks link should start with ss://", link.startsWith("ss://"))
        assertTrue(link.contains("@server.example.com:443"))
    }

    @Test
    fun `toConfigLink returns empty for broken config`() {
        val broken = ProxyConfig(
            id = "bad", name = "", protocol = "", address = "", port = 0, uuid = ""
        )
        val link = broken.toConfigLink()
        assertTrue("empty config without Base64 should return empty", link.isEmpty())
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
