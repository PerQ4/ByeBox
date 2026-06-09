package com.perqa.byebox.data

import com.perqa.byebox.core.SingBoxConfigGenerator
import com.perqa.byebox.core.SingBoxOptions
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SingBoxConfigGeneratorTest {

    private val baseVless = ProxyConfig(
        id = "1",
        name = "Test VLESS",
        protocol = "VLESS",
        address = "de.hiddify.express",
        port = 443,
        uuid = "ac94bf12-9518-57e3-9433-06e1d3fcab1d",
        security = "reality",
        flow = "xtls-rprx-vision",
        sni = "microsoft.com",
        pbk = "Ovu-MEOWU1tUI8ppfuaGosmbiQLFaVY8YwZLKfMDhF4",
        sid = "2c0157f1"
    )

    private fun defaultOptions() = SingBoxOptions(
        dnsAddress = "System Default",
        routingProfile = "BYPASS_LAN_CN_RU",
        ipv6Enabled = false,
        lanBypassEnabled = true
    )

    // ── Structure ──────────────────────────────────────────────────

    @Test
    fun generate_containsRequiredSections() {
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, defaultOptions()))
        assertTrue(json.has("log"))
        assertTrue(json.has("dns"))
        assertTrue(json.has("inbounds"))
        assertTrue(json.has("outbounds"))
        assertTrue(json.has("route"))
    }

    @Test
    fun generate_logSection() {
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, defaultOptions()))
        val log = json.getJSONObject("log")
        assertEquals("info", log.getString("level"))
        assertTrue(log.getBoolean("timestamp"))
    }

    // ── DNS ────────────────────────────────────────────────────────

    @Test
    fun generate_dnsSystemDefault_usesLocalFinal() {
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, defaultOptions()))
        val dns = json.getJSONObject("dns")
        assertEquals("local", dns.getString("final"))
    }

    @Test
    fun generate_dnsCloudflare_usesRemoteFinal() {
        val opts = defaultOptions().copy(dnsAddress = "1.1.1.1")
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, opts))
        val dns = json.getJSONObject("dns")
        assertEquals("remote", dns.getString("final"))
    }

    @Test
    fun generate_dnsGoogle() {
        val opts = defaultOptions().copy(dnsAddress = "8.8.8.8")
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, opts))
        val servers = json.getJSONObject("dns").getJSONArray("servers")
        val remote = servers.getJSONObject(0)
        assertEquals("8.8.8.8", remote.getString("server"))
    }

    @Test
    fun generate_dnsStripProtocol() {
        val opts = defaultOptions().copy(dnsAddress = "https://dns.google/dns-query")
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, opts))
        val remote = json.getJSONObject("dns").getJSONArray("servers").getJSONObject(0)
        assertEquals("dns.google", remote.getString("server"))
    }

    @Test
    fun generate_dnsBlockAds_usesAdGuard() {
        val opts = defaultOptions().copy(routingProfile = "BLOCK_ADS")
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, opts))
        val remote = json.getJSONObject("dns").getJSONArray("servers").getJSONObject(0)
        assertEquals("94.140.14.14", remote.getString("server"))
    }

    @Test
    fun generate_dnsIpv6Strategy() {
        val opts = defaultOptions().copy(ipv6Enabled = true)
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, opts))
        val dns = json.getJSONObject("dns")
        assertEquals("prefer_ipv4", dns.getString("strategy"))
    }

    @Test
    fun generate_dnsIpv4OnlyStrategy() {
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, defaultOptions()))
        val dns = json.getJSONObject("dns")
        assertEquals("ipv4_only", dns.getString("strategy"))
    }

    // ── TUN Inbound ────────────────────────────────────────────────

    @Test
    fun generate_tunInbound_basic() {
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, defaultOptions()))
        val inbound = json.getJSONArray("inbounds").getJSONObject(0)
        assertEquals("tun", inbound.getString("type"))
        assertEquals("tun-in", inbound.getString("tag"))
        assertTrue(inbound.getBoolean("auto_route"))
        assertTrue(inbound.getBoolean("strict_route"))
        assertEquals("mixed", inbound.getString("stack"))
    }

    @Test
    fun generate_tunInbound_ipv4Address() {
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, defaultOptions()))
        val inbound = json.getJSONArray("inbounds").getJSONObject(0)
        val addresses = inbound.getJSONArray("address")
        assertEquals("10.8.0.2/24", addresses.getString(0))
        assertEquals(1, addresses.length())
    }

    @Test
    fun generate_tunInbound_ipv6Addresses() {
        val opts = defaultOptions().copy(ipv6Enabled = true)
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, opts))
        val inbound = json.getJSONArray("inbounds").getJSONObject(0)
        val addresses = inbound.getJSONArray("address")
        assertEquals(2, addresses.length())
        assertEquals("fd7a:115c:a1e0::2/64", addresses.getString(1))
    }

    @Test
    fun generate_tunInbound_customMtu() {
        val opts = defaultOptions().copy(mtu = 9000)
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, opts))
        val inbound = json.getJSONArray("inbounds").getJSONObject(0)
        assertEquals(9000, inbound.getInt("mtu"))
    }

    @Test
    fun generate_tunInbound_perAppOnlySelected() {
        val opts = defaultOptions().copy(
            appRoutingMode = "ONLY_SELECTED",
            appRoutingPackages = listOf("com.telegram.messenger", "org.mozilla.firefox")
        )
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, opts))
        val inbound = json.getJSONArray("inbounds").getJSONObject(0)
        val include = inbound.getJSONArray("include_package")
        assertEquals(2, include.length())
        assertTrue(include.toList().contains("com.telegram.messenger"))
    }

    @Test
    fun generate_tunInbound_perAppBypassSelected() {
        val opts = defaultOptions().copy(
            appRoutingMode = "BYPASS_SELECTED",
            appRoutingPackages = listOf("com.whatsapp")
        )
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, opts))
        val inbound = json.getJSONArray("inbounds").getJSONObject(0)
        val exclude = inbound.getJSONArray("exclude_package")
        assertEquals(1, exclude.length())
        assertEquals("com.whatsapp", exclude.getString(0))
    }

    @Test
    fun generate_tunInbound_perAppOff_noPackages() {
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, defaultOptions()))
        val inbound = json.getJSONArray("inbounds").getJSONObject(0)
        assertFalse(inbound.has("include_package"))
        assertFalse(inbound.has("exclude_package"))
    }

    // ── Outbounds ──────────────────────────────────────────────────

    @Test
    fun generate_outbounds_containProxyDirectBlock() {
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, defaultOptions()))
        val outbounds = json.getJSONArray("outbounds")
        assertEquals(3, outbounds.length())
        val tags = (0 until outbounds.length()).map { outbounds.getJSONObject(it).getString("tag") }
        assertTrue(tags.contains("proxy"))
        assertTrue(tags.contains("direct"))
        assertTrue(tags.contains("block"))
    }

    // ── VLESS outbound ─────────────────────────────────────────────

    @Test
    fun generate_vlessOutbound() {
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, defaultOptions()))
        val proxy = findProxyOutbound(json)
        assertEquals("vless", proxy.getString("type"))
        assertEquals("de.hiddify.express", proxy.getString("server"))
        assertEquals(443, proxy.getInt("server_port"))
        assertEquals("ac94bf12-9518-57e3-9433-06e1d3fcab1d", proxy.getString("uuid"))
        assertEquals("xtls-rprx-vision", proxy.getString("flow"))
    }

    @Test
    fun generate_vlessOutbound_tlsReality() {
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, defaultOptions()))
        val proxy = findProxyOutbound(json)
        val tls = proxy.getJSONObject("tls")
        assertTrue(tls.getBoolean("enabled"))
        assertEquals("microsoft.com", tls.getString("server_name"))
        val utls = tls.getJSONObject("utls")
        assertTrue(utls.getBoolean("enabled"))
        assertEquals("chrome", utls.getString("fingerprint"))
        val reality = tls.getJSONObject("reality")
        assertTrue(reality.getBoolean("enabled"))
        assertEquals("Ovu-MEOWU1tUI8ppfuaGosmbiQLFaVY8YwZLKfMDhF4", reality.getString("public_key"))
        assertEquals("2c0157f1", reality.getString("short_id"))
    }

    // ── VMess outbound ─────────────────────────────────────────────

    @Test
    fun generate_vmessOutbound() {
        val vmess = baseVless.copy(protocol = "VMESS", security = "auto")
        val json = JSONObject(SingBoxConfigGenerator.generate(vmess, defaultOptions()))
        val proxy = findProxyOutbound(json)
        assertEquals("vmess", proxy.getString("type"))
        assertEquals("auto", proxy.getString("security"))
    }

    @Test
    fun generate_vmessOutbound_withWs() {
        val vmess = baseVless.copy(protocol = "VMESS", network = "ws", wsPath = "/vmws", wsHost = "ws.h.com")
        val json = JSONObject(SingBoxConfigGenerator.generate(vmess, defaultOptions()))
        val proxy = findProxyOutbound(json)
        val transport = proxy.getJSONObject("transport")
        assertEquals("ws", transport.getString("type"))
        assertEquals("/vmws", transport.getString("path"))
        assertEquals("ws.h.com", transport.getJSONObject("headers").getString("Host"))
    }

    @Test
    fun generate_vmessOutbound_withGrpc() {
        val vmess = baseVless.copy(protocol = "VMESS", network = "grpc", grpcServiceName = "myGrpc")
        val json = JSONObject(SingBoxConfigGenerator.generate(vmess, defaultOptions()))
        val proxy = findProxyOutbound(json)
        val transport = proxy.getJSONObject("transport")
        assertEquals("grpc", transport.getString("type"))
        assertEquals("myGrpc", transport.getString("service"))
    }

    // ── Trojan outbound ────────────────────────────────────────────

    @Test
    fun generate_trojanOutbound() {
        val trojan = baseVless.copy(protocol = "Trojan", uuid = "trojanpass", sni = "sni.tr.com")
        val json = JSONObject(SingBoxConfigGenerator.generate(trojan, defaultOptions()))
        val proxy = findProxyOutbound(json)
        assertEquals("trojan", proxy.getString("type"))
        assertEquals("trojanpass", proxy.getString("password"))
        assertTrue(proxy.has("tls"))
    }

    // ── Shadowsocks outbound ───────────────────────────────────────

    @Test
    fun generate_shadowsocksOutbound() {
        val ss = baseVless.copy(protocol = "Shadowsocks", uuid = "chacha20-ietf-poly1305:mypass")
        val json = JSONObject(SingBoxConfigGenerator.generate(ss, defaultOptions()))
        val proxy = findProxyOutbound(json)
        assertEquals("shadowsocks", proxy.getString("type"))
        assertEquals("chacha20-ietf-poly1305", proxy.getString("method"))
        assertEquals("mypass", proxy.getString("password"))
    }

    // ── TUIC outbound ──────────────────────────────────────────────

    @Test
    fun generate_tuicOutbound() {
        val tuic = baseVless.copy(
            protocol = "TUIC", uuid = "tu-uuid", password = "tu-pass",
            congestionControl = "bbr", udpRelayMode = "native",
            alpn = "h3", sni = "sni.tu.com"
        )
        val json = JSONObject(SingBoxConfigGenerator.generate(tuic, defaultOptions()))
        val proxy = findProxyOutbound(json)
        assertEquals("tuic", proxy.getString("type"))
        assertEquals("tu-uuid", proxy.getString("uuid"))
        assertEquals("tu-pass", proxy.getString("password"))
        assertEquals("bbr", proxy.getString("congestion_control"))
        assertEquals("native", proxy.getString("udp_relay_mode"))
        assertTrue(proxy.has("tls"))
    }

    // ── Hysteria2 outbound ─────────────────────────────────────────

    @Test
    fun generate_hysteria2Outbound() {
        val hy2 = baseVless.copy(
            protocol = "Hysteria2", password = "hy2pass",
            obfsType = "salamander", obfsPassword = "obfspass",
            sni = "sni.hy2.com", upMbps = 100, downMbps = 200
        )
        val json = JSONObject(SingBoxConfigGenerator.generate(hy2, defaultOptions()))
        val proxy = findProxyOutbound(json)
        assertEquals("hysteria2", proxy.getString("type"))
        assertEquals("hy2pass", proxy.getString("password"))
        assertEquals(100, proxy.getInt("up_mbps"))
        assertEquals(200, proxy.getInt("down_mbps"))
        val obfs = proxy.getJSONObject("obfs")
        assertEquals("salamander", obfs.getString("type"))
        assertEquals("obfspass", obfs.getString("password"))
    }

    // ── WireGuard outbound ─────────────────────────────────────────

    @Test
    fun generate_wireGuardOutbound() {
        val wg = baseVless.copy(
            protocol = "WireGuard", address = "wg.server.com", port = 51820,
            privateKey = "privkey", publicKey = "pubkey",
            wgAddress = "10.0.0.2/32", wgDns = "1.1.1.1",
            wgAllowedIps = "0.0.0.0/0", reserved = "1,2,3"
        )
        val json = JSONObject(SingBoxConfigGenerator.generate(wg, defaultOptions()))
        val proxy = findProxyOutbound(json)
        assertEquals("wireguard", proxy.getString("type"))
        assertEquals("wg.server.com", proxy.getString("server"))
        assertEquals(51820, proxy.getInt("server_port"))
        assertEquals("privkey", proxy.getString("private_key"))
        assertEquals("pubkey", proxy.getString("peer_public_key"))
        val localAddr = proxy.getJSONArray("local_address")
        assertEquals("10.0.0.2/32", localAddr.getString(0))
        val reserved = proxy.getJSONArray("reserved")
        assertEquals(3, reserved.length())
    }

    // ── Route rules ────────────────────────────────────────────────

    @Test
    fun generate_routeSniff() {
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, defaultOptions()))
        val rules = json.getJSONObject("route").getJSONArray("rules")
        val sniffRule = (0 until rules.length()).map { rules.getJSONObject(it) }
            .firstOrNull { it.has("action") && it.getString("action") == "sniff" }
        assertNotNull(sniffRule)
        val sniffer = sniffRule!!.getJSONArray("sniffer")
        assertTrue(sniffer.toList().contains("http"))
        assertTrue(sniffer.toList().contains("tls"))
    }

    @Test
    fun generate_routeDnsHijack() {
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, defaultOptions()))
        val rules = json.getJSONObject("route").getJSONArray("rules")
        val dnsRule = (0 until rules.length()).map { rules.getJSONObject(it) }
            .firstOrNull { it.has("action") && it.getString("action") == "hijack-dns" }
        assertNotNull(dnsRule)
        assertEquals("dns", dnsRule!!.getString("protocol"))
    }

    @Test
    fun generate_routeLanBypass() {
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, defaultOptions()))
        val rules = json.getJSONObject("route").getJSONArray("rules")
        val lanRule = (0 until rules.length()).map { rules.getJSONObject(it) }
            .firstOrNull { it.has("ip_is_private") && it.optBoolean("ip_is_private") == true }
        assertNotNull(lanRule)
        assertEquals("direct", lanRule!!.getString("outbound"))
    }

    @Test
    fun generate_routeRegionalBypass() {
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, defaultOptions()))
        val rules = json.getJSONObject("route").getJSONArray("rules")
        val regionalRule = (0 until rules.length()).map { rules.getJSONObject(it) }
            .firstOrNull { it.has("domain_suffix") && it.has("outbound") && it.getString("outbound") == "direct" }
        assertNotNull(regionalRule)
        val suffixes = regionalRule!!.getJSONArray("domain_suffix").toList()
        assertTrue(suffixes.contains("ru"))
        assertTrue(suffixes.contains("cn"))
        assertTrue(suffixes.contains("vk.com"))
    }

    @Test
    fun generate_routeAdBlock() {
        val opts = defaultOptions().copy(routingProfile = "BLOCK_ADS")
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, opts))
        val rules = json.getJSONObject("route").getJSONArray("rules")
        val adRule = (0 until rules.length()).map { rules.getJSONObject(it) }
            .firstOrNull { it.has("action") && it.getString("action") == "reject" }
        assertNotNull(adRule)
        val suffixes = adRule!!.getJSONArray("domain_suffix").toList()
        assertTrue(suffixes.contains("doubleclick.net"))
        assertTrue(suffixes.contains("ads.google.com"))
        assertFalse(suffixes.contains("ads"))
    }

    @Test
    fun generate_routeFinalProxy() {
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, defaultOptions()))
        assertEquals("proxy", json.getJSONObject("route").getString("final"))
    }

    @Test
    fun generate_routeFinalDirect() {
        val opts = defaultOptions().copy(routingProfile = "DIRECT")
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, opts))
        assertEquals("direct", json.getJSONObject("route").getString("final"))
    }

    // ── Experimental (clash_api) ───────────────────────────────────

    @Test
    fun generate_noClashApiByDefault() {
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, defaultOptions()))
        assertFalse(json.has("experimental"))
    }

    @Test
    fun generate_clashApiWhenEnabled() {
        val opts = defaultOptions().copy(statsEnabled = true, statsPort = 9090)
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, opts))
        assertTrue(json.has("experimental"))
        val clash = json.getJSONObject("experimental").getJSONObject("clash_api")
        assertEquals("127.0.0.1:9090", clash.getString("external_controller"))
    }

    @Test
    fun tun_defaultMixed() {
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, defaultOptions()))
        val tun = json.getJSONArray("inbounds").getJSONObject(0)
        assertEquals("mixed", tun.getString("stack"))
    }

    @Test
    fun tun_gvisorStack() {
        val opts = defaultOptions().copy(tunStack = "gvisor")
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, opts))
        val tun = json.getJSONArray("inbounds").getJSONObject(0)
        assertEquals("gvisor", tun.getString("stack"))
    }

    @Test
    fun tun_systemStack() {
        val opts = defaultOptions().copy(tunStack = "system")
        val json = JSONObject(SingBoxConfigGenerator.generate(baseVless, opts))
        val tun = json.getJSONArray("inbounds").getJSONObject(0)
        assertEquals("system", tun.getString("stack"))
    }

    // ── helpers ────────────────────────────────────────────────────

    private fun findProxyOutbound(json: JSONObject): JSONObject {
        val outbounds = json.getJSONArray("outbounds")
        return (0 until outbounds.length())
            .map { outbounds.getJSONObject(it) }
            .first { it.getString("tag") == "proxy" }
    }

    private fun JSONArray.toList(): List<String> {
        return (0 until length()).map { getString(it) }
    }
}
