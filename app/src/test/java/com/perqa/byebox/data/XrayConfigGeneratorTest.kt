package com.perqa.byebox.data

import com.perqa.byebox.core.XrayConfigGenerator
import com.perqa.byebox.core.XrayOptions
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class XrayConfigGeneratorTest {

    private val baseVless = ProxyConfig(
        id = "1",
        name = "Test VLESS",
        protocol = "VLESS",
        address = "de.xray.express",
        port = 443,
        uuid = "ac94bf12-9518-57e3-9433-06e1d3fcab1d",
        security = "reality",
        flow = "xtls-rprx-vision",
        sni = "microsoft.com",
        pbk = "Ovu-MEOWU1tUI8ppfuaGosmbiQLFaVY8YwZLKfMDhF4",
        sid = "2c0157f1"
    )

    private fun defaultOptions() = XrayOptions(
        dnsAddress = "System Default",
        routingProfile = "BYPASS_LAN_CN_RU",
        lanBypassEnabled = true
    )

    @Test
    fun generate_containsRequiredSections() {
        val json = JSONObject(XrayConfigGenerator.generate(baseVless, defaultOptions()))
        assertTrue(json.has("log"))
        assertTrue(json.has("dns"))
        assertTrue(json.has("inbounds"))
        assertTrue(json.has("outbounds"))
        assertTrue(json.has("routing"))
    }

    @Test
    fun generate_logSection() {
        val json = JSONObject(XrayConfigGenerator.generate(baseVless, defaultOptions()))
        val log = json.getJSONObject("log")
        assertEquals("warning", log.getString("loglevel"))
    }

    @Test
    fun generate_dnsGoogle() {
        val opts = defaultOptions().copy(dnsAddress = "8.8.8.8")
        val json = JSONObject(XrayConfigGenerator.generate(baseVless, opts))
        val servers = json.getJSONObject("dns").getJSONArray("servers")
        assertEquals("8.8.8.8", servers.getString(0))
    }

    @Test
    fun generate_tunInbound_basic() {
        val json = JSONObject(XrayConfigGenerator.generate(baseVless, defaultOptions()))
        val inbounds = json.getJSONArray("inbounds")
        assertEquals(1, inbounds.length())
        val inbound = inbounds.getJSONObject(0)
        assertEquals("tun", inbound.getString("protocol"))
        assertEquals("tun-in", inbound.getString("tag"))
        val settings = inbound.getJSONObject("settings")
        assertEquals("tun0", settings.getString("name"))
        assertEquals(1500, settings.getInt("MTU"))
        val sniffing = inbound.getJSONObject("sniffing")
        assertTrue(sniffing.getBoolean("enabled"))
        assertTrue(sniffing.getBoolean("routeOnly"))
    }

    @Test
    fun generate_outbounds_containProxyDirectBlockDns() {
        val json = JSONObject(XrayConfigGenerator.generate(baseVless, defaultOptions()))
        val outbounds = json.getJSONArray("outbounds")
        assertEquals(4, outbounds.length())
        val tags = (0 until outbounds.length()).map { outbounds.getJSONObject(it).getString("tag") }
        assertTrue(tags.contains("proxy"))
        assertTrue(tags.contains("direct"))
        assertTrue(tags.contains("block"))
        assertTrue(tags.contains("dns-out"))
    }

    @Test
    fun generate_vlessOutbound() {
        val json = JSONObject(XrayConfigGenerator.generate(baseVless, defaultOptions()))
        val proxy = findProxyOutbound(json)
        assertEquals("vless", proxy.getString("protocol"))
        
        val settings = proxy.getJSONObject("settings")
        val vnext = settings.getJSONArray("vnext").getJSONObject(0)
        assertEquals("de.xray.express", vnext.getString("address"))
        assertEquals(443, vnext.getInt("port"))
        
        val user = vnext.getJSONArray("users").getJSONObject(0)
        assertEquals("ac94bf12-9518-57e3-9433-06e1d3fcab1d", user.getString("id"))
        assertEquals("none", user.getString("encryption"))
        assertEquals("xtls-rprx-vision", user.getString("flow"))
    }

    @Test
    fun generate_vlessOutbound_tlsReality() {
        val json = JSONObject(XrayConfigGenerator.generate(baseVless, defaultOptions()))
        val proxy = findProxyOutbound(json)
        val streamSettings = proxy.getJSONObject("streamSettings")
        assertEquals("tcp", streamSettings.getString("network"))
        assertEquals("reality", streamSettings.getString("security"))
        
        val reality = streamSettings.getJSONObject("realitySettings")
        assertEquals("chrome", reality.getString("fingerprint"))
        assertEquals("microsoft.com", reality.getString("serverName"))
        assertEquals("Ovu-MEOWU1tUI8ppfuaGosmbiQLFaVY8YwZLKfMDhF4", reality.getString("publicKey"))
        assertEquals("2c0157f1", reality.getString("shortId"))
    }

    @Test
    fun generate_vmessOutbound() {
        val vmess = baseVless.copy(protocol = "VMESS", security = "auto")
        val json = JSONObject(XrayConfigGenerator.generate(vmess, defaultOptions()))
        val proxy = findProxyOutbound(json)
        assertEquals("vmess", proxy.getString("protocol"))
        
        val settings = proxy.getJSONObject("settings")
        val vnext = settings.getJSONArray("vnext").getJSONObject(0)
        val user = vnext.getJSONArray("users").getJSONObject(0)
        assertEquals("auto", user.getString("security"))
    }

    @Test
    fun generate_trojanOutbound() {
        val trojan = baseVless.copy(protocol = "Trojan", uuid = "trojanpass", sni = "sni.tr.com")
        val json = JSONObject(XrayConfigGenerator.generate(trojan, defaultOptions()))
        val proxy = findProxyOutbound(json)
        assertEquals("trojan", proxy.getString("protocol"))
        
        val settings = proxy.getJSONObject("settings")
        val server = settings.getJSONArray("servers").getJSONObject(0)
        assertEquals("trojanpass", server.getString("password"))
        
        val streamSettings = proxy.getJSONObject("streamSettings")
        assertEquals("tls", streamSettings.getString("security"))
    }

    @Test
    fun generate_shadowsocksOutbound() {
        val ss = baseVless.copy(protocol = "Shadowsocks", uuid = "aes-128-gcm:mypass")
        val json = JSONObject(XrayConfigGenerator.generate(ss, defaultOptions()))
        val proxy = findProxyOutbound(json)
        assertEquals("shadowsocks", proxy.getString("protocol"))
        
        val settings = proxy.getJSONObject("settings")
        val server = settings.getJSONArray("servers").getJSONObject(0)
        assertEquals("aes-128-gcm", server.getString("method"))
        assertEquals("mypass", server.getString("password"))
    }

    private fun findProxyOutbound(json: JSONObject): JSONObject {
        val outbounds = json.getJSONArray("outbounds")
        return (0 until outbounds.length())
            .map { outbounds.getJSONObject(it) }
            .first { it.getString("tag") == "proxy" }
    }
}
