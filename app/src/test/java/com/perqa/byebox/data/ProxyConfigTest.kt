package com.perqa.byebox.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProxyConfigTest {

    // ── toConfigLink: VLESS ────────────────────────────────────────

    @Test
    fun vlessToConfigLink_basic() {
        val config = baseConfig().copy(protocol = "VLESS", uuid = "abc-123")
        val link = config.toConfigLink()
        assertTrue(link.startsWith("vless://abc-123@server.com:443"))
        assertTrue(link.contains("#Test"))
    }

    @Test
    fun vlessToConfigLink_withReality() {
        val config = baseConfig().copy(
            protocol = "VLESS",
            uuid = "uuid",
            security = "reality",
            sni = "microsoft.com",
            pbk = "PUBKEY",
            sid = "SID123",
            flow = "xtls-rprx-vision"
        )
        val link = config.toConfigLink()
        assertTrue(link.contains("security=reality"))
        assertTrue(link.contains("sni=microsoft.com"))
        assertTrue(link.contains("pbk=PUBKEY"))
        assertTrue(link.contains("sid=SID123"))
        assertTrue(link.contains("flow=xtls-rprx-vision"))
    }

    @Test
    fun vlessToConfigLink_withWs() {
        val config = baseConfig().copy(
            protocol = "VLESS",
            network = "ws",
            wsPath = "/mypath",
            wsHost = "host.example.com"
        )
        val link = config.toConfigLink()
        assertTrue(link.contains("type=ws"))
        assertTrue(link.contains("path=/mypath"))
        assertTrue(link.contains("host=host.example.com"))
    }

    @Test
    fun vlessToConfigLink_withGrpc() {
        val config = baseConfig().copy(
            protocol = "VLESS",
            network = "grpc",
            grpcServiceName = "myGrpc"
        )
        val link = config.toConfigLink()
        assertTrue(link.contains("type=grpc"))
        assertTrue(link.contains("serviceName=myGrpc"))
    }

    @Test
    fun vlessToConfigLink_withH2() {
        val config = baseConfig().copy(
            protocol = "VLESS",
            network = "http",
            wsPath = "/h2path",
            wsHost = "h2.example.com"
        )
        val link = config.toConfigLink()
        assertTrue(link.contains("type=http"))
        assertTrue(link.contains("path=/h2path"))
    }

    @Test
    fun vlessToConfigLink_noTransportParams() {
        val config = baseConfig().copy(protocol = "VLESS")
        val link = config.toConfigLink()
        assertFalse(link.contains("type="))
        assertFalse(link.contains("path="))
        assertFalse(link.contains("host="))
    }

    // ── toConfigLink: VMess ────────────────────────────────────────

    @Test
    fun vmessToConfigLink_basic() {
        val config = baseConfig().copy(protocol = "VMESS", uuid = "vmess-id")
        val link = config.toConfigLink()
        assertTrue(link.startsWith("vmess://"))
        val encoded = link.removePrefix("vmess://")
        val decoded = String(android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP))
        val json = JSONObject(decoded)
        assertEquals("vmess-id", json.getString("id"))
        assertEquals("server.com", json.getString("add"))
        assertEquals(443, json.getInt("port"))
        assertEquals("Test", json.getString("ps"))
        assertEquals("auto", json.getString("scy"))
        assertEquals("tcp", json.getString("net"))
    }

    @Test
    fun vmessToConfigLink_withTls() {
        val config = baseConfig().copy(protocol = "VMESS", security = "tls", sni = "sni.example.com")
        val link = config.toConfigLink()
        val decoded = String(android.util.Base64.decode(link.removePrefix("vmess://"), android.util.Base64.NO_WRAP))
        val json = JSONObject(decoded)
        assertEquals("tls", json.getString("tls"))
        assertEquals("sni.example.com", json.getString("sni"))
    }

    @Test
    fun vmessToConfigLink_withWs() {
        val config = baseConfig().copy(protocol = "VMESS", network = "ws", wsPath = "/vmws", wsHost = "ws.example.com")
        val link = config.toConfigLink()
        val decoded = String(android.util.Base64.decode(link.removePrefix("vmess://"), android.util.Base64.NO_WRAP))
        val json = JSONObject(decoded)
        assertEquals("ws", json.getString("net"))
        assertEquals("/vmws", json.getString("path"))
        assertEquals("ws.example.com", json.getString("host"))
    }

    @Test
    fun vmessToConfigLink_withGrpc() {
        val config = baseConfig().copy(protocol = "VMESS", network = "grpc", grpcServiceName = "grpcSvc")
        val link = config.toConfigLink()
        val decoded = String(android.util.Base64.decode(link.removePrefix("vmess://"), android.util.Base64.NO_WRAP))
        val json = JSONObject(decoded)
        assertEquals("grpc", json.getString("net"))
        assertEquals("grpcSvc", json.getString("path"))
    }

    // ── toConfigLink: Trojan ───────────────────────────────────────

    @Test
    fun trojanToConfigLink_basic() {
        val config = baseConfig().copy(protocol = "Trojan", uuid = "trojanpass")
        val link = config.toConfigLink()
        assertTrue(link.startsWith("trojan://trojanpass@server.com:443"))
    }

    @Test
    fun trojanToConfigLink_withSniAndTransport() {
        val config = baseConfig().copy(
            protocol = "Trojan",
            uuid = "pass",
            sni = "sni.tr.com",
            network = "ws",
            wsPath = "/tws",
            wsHost = "tr.example.com",
            grpcServiceName = "trGrpc"
        )
        val link = config.toConfigLink()
        assertTrue(link.contains("sni=sni.tr.com"))
        assertTrue(link.contains("type=ws"))
        assertTrue(link.contains("path=/tws"))
        assertTrue(link.contains("host=tr.example.com"))
        assertTrue(link.contains("serviceName=trGrpc"))
    }

    // ── toConfigLink: Shadowsocks ──────────────────────────────────

    @Test
    fun shadowsocksToConfigLink() {
        val config = baseConfig().copy(protocol = "Shadowsocks", uuid = "chacha20-ietf-poly1305:mypassword")
        val link = config.toConfigLink()
        assertTrue(link.startsWith("ss://"))
        assertTrue(link.contains("@server.com:443"))
        assertTrue(link.contains("#Test"))
    }

    // ── toConfigLink: roundtrip ────────────────────────────────────

    @Test
    fun vlessRoundtrip_parseThenGenerate() {
        val original = baseConfig().copy(
            protocol = "VLESS",
            uuid = "roundtrip-uuid",
            security = "reality",
            sni = "sni.example.com",
            pbk = "pbk123",
            sid = "sid456",
            flow = "xtls-rprx-vision",
            network = "ws",
            wsPath = "/rt",
            wsHost = "rt.example.com"
        )
        val link = original.toConfigLink()
        val parsed = ConfigParser.parse(link)!!
        assertEquals(original.uuid, parsed.uuid)
        assertEquals(original.address, parsed.address)
        assertEquals(original.port, parsed.port)
        assertEquals(original.security, parsed.security)
        assertEquals(original.sni, parsed.sni)
        assertEquals(original.pbk, parsed.pbk)
        assertEquals(original.sid, parsed.sid)
        assertEquals(original.flow, parsed.flow)
        assertEquals(original.network, parsed.network)
        assertEquals(original.wsPath, parsed.wsPath)
        assertEquals(original.wsHost, parsed.wsHost)
    }

    @Test
    fun trojanRoundtrip_parseThenGenerate() {
        val original = baseConfig().copy(
            protocol = "Trojan",
            uuid = "trojan-pass",
            sni = "sni.t.com",
            network = "ws",
            wsPath = "/tp",
            wsHost = "tp.example.com"
        )
        val link = original.toConfigLink()
        val parsed = ConfigParser.parse(link)!!
        assertEquals(original.uuid, parsed.uuid)
        assertEquals(original.sni, parsed.sni)
        assertEquals(original.network, parsed.network)
        assertEquals(original.wsPath, parsed.wsPath)
        assertEquals(original.wsHost, parsed.wsHost)
    }

    // ── toJson / fromJson roundtrip ────────────────────────────────

    @Test
    fun jsonRoundtrip_preservesAllFields() {
        val original = baseConfig().copy(
            protocol = "VLESS",
            uuid = "json-uuid",
            security = "reality",
            sni = "sni.json.com",
            pbk = "pbk-json",
            sid = "sid-json",
            flow = "xtls-rprx-vision",
            network = "ws",
            wsPath = "/jp",
            wsHost = "json.example.com",
            grpcServiceName = "grpcJson",
            ping = 42,
            failureCount = 3,
            sourceName = "TestSource",
            countryFlag = "🇩🇪"
        )
        val json = original.toJson()
        val restored = ProxyConfig.fromJson(json)
        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.protocol, restored.protocol)
        assertEquals(original.address, restored.address)
        assertEquals(original.port, restored.port)
        assertEquals(original.uuid, restored.uuid)
        assertEquals(original.security, restored.security)
        assertEquals(original.sni, restored.sni)
        assertEquals(original.pbk, restored.pbk)
        assertEquals(original.sid, restored.sid)
        assertEquals(original.flow, restored.flow)
        assertEquals(original.network, restored.network)
        assertEquals(original.wsPath, restored.wsPath)
        assertEquals(original.wsHost, restored.wsHost)
        assertEquals(original.grpcServiceName, restored.grpcServiceName)
        assertEquals(original.ping, restored.ping)
        assertEquals(original.failureCount, restored.failureCount)
        assertEquals(original.sourceName, restored.sourceName)
        assertEquals(original.countryFlag, restored.countryFlag)
    }

    private fun baseConfig() = ProxyConfig(
        id = "test-id",
        name = "Test",
        protocol = "VLESS",
        address = "server.com",
        port = 443,
        uuid = "test-uuid"
    )
}
