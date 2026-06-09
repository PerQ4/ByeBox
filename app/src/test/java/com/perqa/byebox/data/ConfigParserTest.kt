package com.perqa.byebox.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ConfigParserTest {

    // ── VLESS ──────────────────────────────────────────────────────

    @Test
    fun parseVless_basic() {
        val url = "vless://uuid-1234@de.hiddify.express:443#Germany"
        val config = ConfigParser.parse(url)!!
        assertEquals("VLESS", config.protocol)
        assertEquals("uuid-1234", config.uuid)
        assertEquals("de.hiddify.express", config.address)
        assertEquals(443, config.port)
        assertEquals("Germany", config.name)
        assertEquals("🇩🇪", config.countryFlag)
    }

    @Test
    fun parseVless_withReality() {
        val url = "vless://uuid@server.com:443?security=reality&sni=microsoft.com&pbk=ABC123&sid=short123&flow=xtls-rprx-vision#MyNode"
        val config = ConfigParser.parse(url)!!
        assertEquals("reality", config.security)
        assertEquals("microsoft.com", config.sni)
        assertEquals("ABC123", config.pbk)
        assertEquals("short123", config.sid)
        assertEquals("xtls-rprx-vision", config.flow)
    }

    @Test
    fun parseVless_withWebSocket() {
        val url = "vless://uuid@server.com:443?type=ws&path=/mypath&host=example.com#WS"
        val config = ConfigParser.parse(url)!!
        assertEquals("ws", config.network)
        assertEquals("/mypath", config.wsPath)
        assertEquals("example.com", config.wsHost)
    }

    @Test
    fun parseVless_withGrpc() {
        val url = "vless://uuid@server.com:443?type=grpc&serviceName=myService#Grpc"
        val config = ConfigParser.parse(url)!!
        assertEquals("grpc", config.network)
        assertEquals("myService", config.grpcServiceName)
    }

    @Test
    fun parseVless_withTls() {
        val url = "vless://uuid@server.com:443?security=tls&sni=cdn.example.com#TLS"
        val config = ConfigParser.parse(url)!!
        assertEquals("tls", config.security)
        assertEquals("cdn.example.com", config.sni)
    }

    @Test
    fun parseVless_noHashUsesDefaultName() {
        val url = "vless://uuid@server.com:443"
        val config = ConfigParser.parse(url)!!
        assertEquals("VLESS Server", config.name)
    }

    // ── VMESS ──────────────────────────────────────────────────────

    @Test
    fun parseVmess_basic() {
        val vmessJson = """{"v":"2","ps":"Tokyo VMess","add":"jp.server.com","port":80,"id":"abc-def","scy":"auto","net":"tcp","type":"none"}"""
        val encoded = android.util.Base64.encodeToString(vmessJson.toByteArray(), android.util.Base64.NO_WRAP)
        val url = "vmess://$encoded"
        val config = ConfigParser.parse(url)!!
        assertEquals("VMESS", config.protocol)
        assertEquals("Tokyo VMess", config.name)
        assertEquals("jp.server.com", config.address)
        assertEquals(80, config.port)
        assertEquals("abc-def", config.uuid)
        assertEquals("auto", config.security)
    }

    @Test
    fun parseVmess_withWs() {
        val vmessJson = """{"v":"2","ps":"WS Node","add":"ws.server.com","port":443,"id":"id123","scy":"auto","net":"ws","type":"none","path":"/ws","host":"ws.example.com"}"""
        val encoded = android.util.Base64.encodeToString(vmessJson.toByteArray(), android.util.Base64.NO_WRAP)
        val config = ConfigParser.parse("vmess://$encoded")!!
        assertEquals("ws", config.network)
        assertEquals("/ws", config.wsPath)
        assertEquals("ws.example.com", config.wsHost)
    }

    @Test
    fun parseVmess_withTls() {
        val vmessJson = """{"v":"2","ps":"TLS Node","add":"tls.server.com","port":443,"id":"id123","scy":"auto","net":"tcp","type":"none","tls":"tls","sni":"sni.example.com"}"""
        val encoded = android.util.Base64.encodeToString(vmessJson.toByteArray(), android.util.Base64.NO_WRAP)
        val config = ConfigParser.parse("vmess://$encoded")!!
        assertEquals("tls", config.security)
        assertEquals("sni.example.com", config.sni)
    }

    // ── Trojan ─────────────────────────────────────────────────────

    @Test
    fun parseTrojan_basic() {
        val url = "trojan://mypassword@nl.server.com:8443#Netherlands"
        val config = ConfigParser.parse(url)!!
        assertEquals("Trojan", config.protocol)
        assertEquals("mypassword", config.uuid)
        assertEquals("nl.server.com", config.address)
        assertEquals(8443, config.port)
        assertEquals("Netherlands", config.name)
        assertEquals("🇳🇱", config.countryFlag)
    }

    @Test
    fun parseTrojan_withSni() {
        val url = "trojan://pass@server.com:443?sni=cdn.example.com#TrojanTLS"
        val config = ConfigParser.parse(url)!!
        assertEquals("cdn.example.com", config.sni)
    }

    @Test
    fun parseTrojan_withTransport() {
        val url = "trojan://pass@server.com:443?type=ws&path=/tws&host=t.example.com&serviceName=grpcSvc#Mixed"
        val config = ConfigParser.parse(url)!!
        assertEquals("ws", config.network)
        assertEquals("/tws", config.wsPath)
        assertEquals("t.example.com", config.wsHost)
        assertEquals("grpcSvc", config.grpcServiceName)
    }

    // ── Shadowsocks ────────────────────────────────────────────────

    @Test
    fun parseShadowsocks_basic() {
        val userinfo = android.util.Base64.encodeToString("chacha20-ietf-poly1305:secret123".toByteArray(), android.util.Base64.NO_WRAP)
        val url = "ss://$userinfo@ss.server.com:1080#SSNode"
        val config = ConfigParser.parse(url)!!
        assertEquals("Shadowsocks", config.protocol)
        assertEquals("chacha20-ietf-poly1305:secret123", config.uuid)
        assertEquals("ss.server.com", config.address)
        assertEquals(1080, config.port)
        assertEquals("SSNode", config.name)
    }

    // ── TUIC ───────────────────────────────────────────────────────

    @Test
    fun parseTuic_basic() {
        val url = "tuic://uuid123:mypassword@tuic.server.com:443#TUICNode"
        val config = ConfigParser.parse(url)!!
        assertEquals("TUIC", config.protocol)
        assertEquals("uuid123", config.uuid)
        assertEquals("mypassword", config.password)
        assertEquals("tuic.server.com", config.address)
        assertEquals(443, config.port)
    }

    @Test
    fun parseTuic_withParams() {
        val url = "tuic://uuid:pass@server.com:443?congestion_control=bbr&udp_relay_mode=native&alpn=h3,h2&sni=sni.example.com&zero_rtt_handshake=true#TUIC"
        val config = ConfigParser.parse(url)!!
        assertEquals("bbr", config.congestionControl)
        assertEquals("native", config.udpRelayMode)
        assertEquals("h3,h2", config.alpn)
        assertEquals("sni.example.com", config.sni)
        assertTrue(config.zeroRttHandshake)
    }

    @Test
    fun parseTuic_noPasswordUsesUuid() {
        val url = "tuic://uuid123@server.com:443#NoPass"
        val config = ConfigParser.parse(url)!!
        assertEquals("uuid123", config.uuid)
        assertNull(config.password)
    }

    // ── Hysteria2 ──────────────────────────────────────────────────

    @Test
    fun parseHysteria2_basic() {
        val url = "hysteria2://secretpass@hy2.server.com:443#Hy2Node"
        val config = ConfigParser.parse(url)!!
        assertEquals("Hysteria2", config.protocol)
        assertEquals("secretpass", config.password)
        assertEquals("hy2.server.com", config.address)
        assertEquals(443, config.port)
        assertEquals("Hy2Node", config.name)
    }

    @Test
    fun parseHy2_withParams() {
        val url = "hy2://pass@server.com:443?obfs=salamander&obfs-password=obfspass&sni=sni.hy2.com&up=100&down=200#Hy2"
        val config = ConfigParser.parse(url)!!
        assertEquals("salamander", config.obfsType)
        assertEquals("obfspass", config.obfsPassword)
        assertEquals("sni.hy2.com", config.sni)
        assertEquals(100, config.upMbps)
        assertEquals(200, config.downMbps)
    }

    @Test
    fun parseHysteria2_noObfs() {
        val url = "hysteria2://pass@server.com:443#NoObfs"
        val config = ConfigParser.parse(url)!!
        assertNull(config.obfsType)
    }

    // ── WireGuard ──────────────────────────────────────────────────

    @Test
    fun parseWireGuard_basic() {
        val url = "wg://publicKey123@wg.server.com:51820?private_key=privKey&address=10.0.0.2/32&dns=1.1.1.1&allowed_ips=0.0.0.0/0&reserved=1,2,3#WG"
        val config = ConfigParser.parse(url)!!
        assertEquals("WireGuard", config.protocol)
        assertEquals("publicKey123", config.publicKey)
        assertEquals("wg.server.com", config.address)
        assertEquals(51820, config.port)
        assertEquals("privKey", config.privateKey)
        assertEquals("10.0.0.2/32", config.wgAddress)
        assertEquals("1.1.1.1", config.wgDns)
        assertEquals("0.0.0.0/0", config.wgAllowedIps)
        assertEquals("1,2,3", config.reserved)
    }

    // ── Country flags ──────────────────────────────────────────────

    @Test
    fun countryFlag_russia() {
        val url = "vless://uuid@ru.server.com:443#Moscow RU"
        val config = ConfigParser.parse(url)!!
        assertEquals("🇷🇺", config.countryFlag)
    }

    @Test
    fun countryFlag_usa() {
        val url = "vless://uuid@us.server.com:443#New York US"
        val config = ConfigParser.parse(url)!!
        assertEquals("🇺🇸", config.countryFlag)
    }

    @Test
    fun countryFlag_germany() {
        val url = "vless://uuid@de.server.com:443#Frankfurt DE"
        val config = ConfigParser.parse(url)!!
        assertEquals("🇩🇪", config.countryFlag)
    }

    @Test
    fun countryFlag_unknown() {
        val url = "vless://uuid@server.com:443#RandomNode"
        val config = ConfigParser.parse(url)!!
        assertEquals("🌐", config.countryFlag)
    }

    // ── Edge cases ─────────────────────────────────────────────────

    @Test
    fun parse_unknownScheme_returnsNull() {
        assertNull(ConfigParser.parse("unknown://something"))
    }

    @Test
    fun parse_emptyString_returnsNull() {
        assertNull(ConfigParser.parse(""))
    }

    @Test
    fun parse_garbage_returnsNull() {
        assertNull(ConfigParser.parse("not a url at all"))
    }

    @Test
    fun parseVless_urlEncodedName() {
        val url = "vless://uuid@server.com:443#My%20Server%20%E2%9C%93"
        val config = ConfigParser.parse(url)!!
        assertEquals("My Server ✓", config.name)
    }

    @Test
    fun parseVless_highPort() {
        val url = "vless://uuid@server.com:65535#HighPort"
        val config = ConfigParser.parse(url)!!
        assertEquals(65535, config.port)
    }
}
