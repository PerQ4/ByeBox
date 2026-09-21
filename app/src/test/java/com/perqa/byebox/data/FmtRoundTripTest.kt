package com.perqa.byebox.data

import android.app.Application
import com.v2ray.ang.AppConfig
import com.v2ray.ang.fmt.Hysteria2Fmt
import com.v2ray.ang.fmt.ShadowsocksFmt
import com.v2ray.ang.fmt.SocksFmt
import com.v2ray.ang.fmt.TrojanFmt
import com.v2ray.ang.fmt.VlessFmt
import com.v2ray.ang.fmt.VmessFmt
import com.v2ray.ang.fmt.WireguardFmt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Round-trip tests for every subscription link parser: parse(link) -> ProfileItem,
 * then ProfileItem -> toUri() must serialize back the essential fields.
 *
 * The Fmt objects serialize scheme-less URIs (like MmkvManager's export path),
 * so the scheme is prepended here exactly like the app does.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class FmtRoundTripTest {

    private val testUuid = "ac94bf12-9518-57e3-9433-06e1d3fcab1d"

    @Test
    fun vlessRealityRoundTrip_preservesCoreFields() {
        val link = "vless://$testUuid@de.xray.express:443" +
            "?security=reality&flow=xtls-rprx-vision&sni=microsoft.com" +
            "&pbk=Ovu-MEOWU1tUI8ppfuaGosmbiQLFaVY8YwZLKfMDhF4&sid=2c0157f1&fp=chrome&type=tcp" +
            "#Test%20VLESS"

        val first = VlessFmt.parse(link)
        assertNotNull(first)
        assertEquals("de.xray.express", first!!.server)
        assertEquals("443", first.serverPort)
        assertEquals(testUuid, first.password)
        assertEquals("reality", first.security)
        assertEquals("microsoft.com", first.sni)
        assertEquals("Ovu-MEOWU1tUI8ppfuaGosmbiQLFaVY8YwZLKfMDhF4", first.publicKey)
        assertEquals("2c0157f1", first.shortId)
        assertEquals("xtls-rprx-vision", first.flow)
        assertEquals("Test VLESS", first.remarks)

        val serialized = AppConfig.VLESS + VlessFmt.toUri(first)
        val reparsed = VlessFmt.parse(serialized)
        assertNotNull(reparsed)
        assertEquals("de.xray.express", reparsed!!.server)
        assertEquals("443", reparsed.serverPort)
        assertEquals(testUuid, reparsed.password)
        assertEquals("reality", reparsed.security)
        assertEquals("microsoft.com", reparsed.sni)
        assertEquals("Ovu-MEOWU1tUI8ppfuaGosmbiQLFaVY8YwZLKfMDhF4", reparsed.publicKey)
        assertEquals("2c0157f1", reparsed.shortId)
        assertEquals("xtls-rprx-vision", reparsed.flow)
    }

    @Test
    fun vmessLegacyRoundTrip_preservesCoreFields() {
        val base64Json = "eyJ2IjoiMiIsInBzIjoiVGVzdCBWTUVTUyIsImFkZCI6InZtLnNlcnZlci5jb20iLCJwb3J0IjoiNDQzIiwiaWQiOiJhYzk0YmYxMi05NTE4LTU3ZTMtOTQzMy0wNmUxZDNmY2FiMWQiLCJhaWQiOiIwIiwibmV0Ijoid3MiLCJ0eXBlIjoibm9uZSIsImhvc3QiOiJ3cy1ob3N0LmNvbSIsInBhdGgiOiIvd3MiLCJ0bHMiOiJ0bHMiLCJzbmkiOiJzbmkudm0uY29tIiwiZnAiOiJjaHJvbWUifQ=="
        val link = "vmess://$base64Json"

        val first = VmessFmt.parse(link)
        assertNotNull(first)
        assertEquals("vm.server.com", first!!.server)
        assertEquals("443", first.serverPort)
        assertEquals(testUuid, first.password)
        assertEquals("ws", first.network)
        assertEquals("ws-host.com", first.host)
        assertEquals("/ws", first.path)
        assertEquals("tls", first.security)
        assertEquals("sni.vm.com", first.sni)
        assertEquals("Test VMESS", first.remarks)

        val serialized = AppConfig.VMESS + VmessFmt.toUri(first)
        val reparsed = VmessFmt.parse(serialized)
        assertNotNull(reparsed)
        assertEquals("vm.server.com", reparsed!!.server)
        assertEquals("443", reparsed.serverPort)
        assertEquals(testUuid, reparsed.password)
    }

    @Test
    fun trojanRoundTrip_preservesCoreFields() {
        val link = "trojan://trojan-pass@sni.tr.com:443?sni=sni.tr.com&type=tcp#Trojan%20Server"

        val first = TrojanFmt.parse(link)
        assertEquals("sni.tr.com", first.server)
        assertEquals("443", first.serverPort)
        assertEquals("trojan-pass", first.password)
        assertEquals("tls", first.security)
        assertEquals("tcp", first.network)

        val reparsed = TrojanFmt.parse(AppConfig.TROJAN + TrojanFmt.toUri(first))
        assertEquals(first.server, reparsed.server)
        assertEquals(first.serverPort, reparsed.serverPort)
        assertEquals(first.password, reparsed.password)
        assertEquals(first.sni, reparsed.sni)
    }

    @Test
    fun shadowsocksSip002RoundTrip_preservesMethodAndPassword() {
        val userInfo = "YWVzLTI1Ni1nY206cGFzc3dvcmQxMjM="
        val link = "ss://$userInfo@ss.example.com:8388#SS%20Server"

        val first = ShadowsocksFmt.parse(link)
        assertNotNull(first)
        assertEquals("ss.example.com", first!!.server)
        assertEquals("8388", first.serverPort)
        assertEquals("aes-256-gcm", first.method)
        assertEquals("password123", first.password)

        val reparsed = ShadowsocksFmt.parse(AppConfig.SHADOWSOCKS + ShadowsocksFmt.toUri(first))
        assertNotNull(reparsed)
        assertEquals(first.method, reparsed!!.method)
        assertEquals(first.password, reparsed.password)
        assertEquals(first.server, reparsed.server)
        assertEquals(first.serverPort, reparsed.serverPort)
    }

    @Test
    fun hysteria2RoundTrip_preservesPasswordAndSecurity() {
        val link = "hysteria2://hy2-pass@hy2.example.com:8443?insecure=1&sni=hy2.sni.com#Hysteria2"

        val first = Hysteria2Fmt.parse(link)
        assertEquals("hy2.example.com", first.server)
        assertEquals("8443", first.serverPort)
        assertEquals("hy2-pass", first.password)
        assertEquals("tls", first.security)

        val reparsed = Hysteria2Fmt.parse(AppConfig.HYSTERIA2 + Hysteria2Fmt.toUri(first))
        assertEquals(first.server, reparsed.server)
        assertEquals(first.serverPort, reparsed.serverPort)
        assertEquals(first.password, reparsed.password)
        assertEquals(first.security, reparsed.security)
    }

    @Test
    fun wireguardRoundTrip_preservesKeysAndAddress() {
        val secret = "c2VjcmV0S2V5MDEyMzQ1Njc4OWFiY2RlZg=="
        val publicKey = "cHVibGljS2V5MDEyMzQ1Njc4OWFiY2RlZw=="
        val link = "wireguard://$secret@wg.example.com:51820" +
            "?publickey=$publicKey&address=10.0.0.2/24&mtu=1420&reserved=0,0,0#WireGuard"

        val first = WireguardFmt.parse(link)
        assertNotNull(first)
        assertEquals("wg.example.com", first!!.server)
        assertEquals("51820", first.serverPort)
        assertEquals(secret, first.secretKey)
        assertEquals(publicKey, first.publicKey)
        assertEquals("10.0.0.2/24", first.localAddress)
        assertEquals(1420, first.mtu)
        assertEquals("0,0,0", first.reserved)

        val reparsed = WireguardFmt.parse(AppConfig.WIREGUARD + WireguardFmt.toUri(first))
        assertNotNull(reparsed)
        assertEquals(first.secretKey, reparsed!!.secretKey)
        assertEquals(first.publicKey, reparsed.publicKey)
        assertEquals(first.localAddress, reparsed.localAddress)
        assertEquals(first.mtu, reparsed.mtu)
    }

    @Test
    fun socksRoundTrip_preservesCredentials() {
        val link = "socks://user:pass@127.0.0.1:1080#Socks5"

        val first = SocksFmt.parse(link)
        assertNotNull(first)
        assertEquals("127.0.0.1", first!!.server)
        assertEquals("1080", first.serverPort)
        assertEquals("user", first.username)
        assertEquals("pass", first.password)

        val reparsed = SocksFmt.parse(AppConfig.SOCKS + SocksFmt.toUri(first))
        assertNotNull(reparsed)
        assertEquals(first.username, reparsed!!.username)
        assertEquals(first.password, reparsed.password)
    }

    @Test
    fun parseRejectsLinksMissingRequiredParts() {
        assertNull(VlessFmt.parse("vless://nodata"))
        assertNull(ShadowsocksFmt.parse("ss://server.example.com"))
        assertNull(SocksFmt.parse("socks://server.example.com"))
    }
}