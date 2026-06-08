package com.perqa.byebox.core

import com.perqa.byebox.data.ProxyConfig
import org.json.JSONArray
import org.json.JSONObject

data class SingBoxOptions(
    val dnsAddress: String,
    val routingProfile: String,
    val ipv6Enabled: Boolean,
    val lanBypassEnabled: Boolean,
    val mtu: Int = 1500,
    val statsEnabled: Boolean = false,
    val statsPort: Int = 9090
)

object SingBoxConfigGenerator {
    fun generate(activeConfig: ProxyConfig, options: SingBoxOptions): String {
        val config = JSONObject()
            .put("log", logSection())
            .put("dns", dnsSection(options))
            .put("inbounds", JSONArray().put(tunInbound(options)))
            .put("outbounds", outbounds(activeConfig))
            .put("route", routeSection(options))

        if (options.statsEnabled) {
            config.put("experimental", JSONObject().put(
                "clash_api",
                JSONObject()
                    .put("external_controller", "127.0.0.1:${options.statsPort}")
                    .put("external_ui", "")
                    .put("secret", "")
                    .put("default_mode", "rule")
            ))
        }

        return config.toString(2)
    }

    private fun logSection(): JSONObject {
        return JSONObject()
            .put("level", "info")
            .put("timestamp", true)
    }

    private fun dnsSection(options: SingBoxOptions): JSONObject {
        val usesSystemDns = options.routingProfile != "BLOCK_ADS" &&
            (options.dnsAddress == "System Default" || options.dnsAddress.isBlank())
        val dnsAddress = when {
            options.routingProfile == "BLOCK_ADS" -> "94.140.14.14"
            usesSystemDns -> "1.1.1.1"
            else -> options.dnsAddress
        }

        val dnsRules = JSONArray()
            .put(
                JSONObject()
                    .put("domain", JSONArray().put("localhost"))
                    .put("domain_suffix", JSONArray().put(".local").put(".lan"))
                    .put("server", "local")
            )
            .put(
                JSONObject()
                    .put("ip_is_private", true)
                    .put("server", "local")
            )

        return JSONObject()
            .put(
                "servers",
                JSONArray()
                    .put(
                        JSONObject()
                            .put("type", "udp")
                            .put("tag", "remote")
                            .put("server", dnsAddress)
                            .put("detour", "proxy")
                    )
                    .put(
                        JSONObject()
                            // "local" tag uses system/DHCP DNS — does NOT go through TUN
                            // This is the real physical network DNS, not through proxy
                            .put("type", "local")
                            .put("tag", "local")
                            .put("detour", "direct")
                    )
            )
            .put("rules", dnsRules)
            .put("final", if (usesSystemDns) "local" else "remote")
            .put("strategy", if (options.ipv6Enabled) "prefer_ipv4" else "ipv4_only")
    }

    private fun tunInbound(options: SingBoxOptions): JSONObject {
        val addresses = JSONArray().put("10.8.0.2/24")
        if (options.ipv6Enabled) {
            addresses.put("fd7a:115c:a1e0::2/64")
        }

        return JSONObject()
            .put("type", "tun")
            .put("tag", "tun-in")
            .put("interface_name", "tun0")
            .put("address", addresses)
            .put("mtu", options.mtu)
            .put("auto_route", true)
            .put("strict_route", true)
            .put("stack", "mixed")
            .put("endpoint_independent_nat", true)
    }

    private fun outbounds(config: ProxyConfig): JSONArray {
        return JSONArray()
            .put(proxyOutbound(config))
            .put(JSONObject().put("type", "direct").put("tag", "direct"))
    }

    private fun proxyOutbound(config: ProxyConfig): JSONObject {
        return when (config.protocol.uppercase()) {
            "VLESS" -> vlessOutbound(config)
            "VMESS" -> vmessOutbound(config)
            "TROJAN" -> trojanOutbound(config)
            "SHADOWSOCKS" -> shadowsocksOutbound(config)
            "TUIC" -> tuicOutbound(config)
            "HYSTERIA2" -> hysteria2Outbound(config)
            "WIREGUARD" -> wireGuardOutbound(config)
            else -> JSONObject().put("type", "direct").put("tag", "proxy")
        }
    }

    private fun vlessOutbound(config: ProxyConfig): JSONObject {
        return JSONObject()
            .put("type", "vless")
            .put("tag", "proxy")
            .put("server", config.address)
            .put("server_port", config.port)
            .put("uuid", config.uuid)
            .apply {
                config.flow?.let { put("flow", it) }
                putTlsIfNeeded(config)
                putTransportIfNeeded(config)
            }
    }

    private fun vmessOutbound(config: ProxyConfig): JSONObject {
        return JSONObject()
            .put("type", "vmess")
            .put("tag", "proxy")
            .put("server", config.address)
            .put("server_port", config.port)
            .put("uuid", config.uuid)
            .put("security", config.security?.takeIf { it != "tls" } ?: "auto")
            .apply {
                putTlsIfNeeded(config)
                putTransportIfNeeded(config)
            }
    }

    private fun trojanOutbound(config: ProxyConfig): JSONObject {
        return JSONObject()
            .put("type", "trojan")
            .put("tag", "proxy")
            .put("server", config.address)
            .put("server_port", config.port)
            .put("password", config.uuid)
            .apply { putTlsIfNeeded(config, force = true) }
    }

    private fun shadowsocksOutbound(config: ProxyConfig): JSONObject {
        val methodAndPassword = config.uuid.split(":", limit = 2)
        return JSONObject()
            .put("type", "shadowsocks")
            .put("tag", "proxy")
            .put("server", config.address)
            .put("server_port", config.port)
            .put("method", methodAndPassword.getOrNull(0)?.ifBlank { "chacha20-ietf-poly1305" } ?: "chacha20-ietf-poly1305")
            .put("password", methodAndPassword.getOrNull(1).orEmpty())
    }

    private fun tuicOutbound(config: ProxyConfig): JSONObject {
        return JSONObject()
            .put("type", "tuic")
            .put("tag", "proxy")
            .put("server", config.address)
            .put("server_port", config.port)
            .put("uuid", config.uuid)
            .put("password", config.password ?: config.uuid)
            .apply {
                config.congestionControl?.let { put("congestion_control", it) }
                config.udpRelayMode?.let { put("udp_relay_mode", it) }
                config.alpn?.let { alpn -> put("tls", JSONObject().put("enabled", true).put("server_name", config.sni ?: config.address).put("alpn", JSONArray(alpn.split(",").map { it.trim() }))) }
                config.sni?.let { if (!config.alpn.isNullOrEmpty()) return@let; put("tls", JSONObject().put("enabled", true).put("server_name", it)) }
                if (config.zeroRttHandshake) put("zero_rtt_handshake", true)
                if (config.sni != null || config.alpn != null) return@apply
                put("tls", JSONObject().put("enabled", true).put("server_name", config.address))
            }
    }

    private fun hysteria2Outbound(config: ProxyConfig): JSONObject {
        return JSONObject()
            .put("type", "hysteria2")
            .put("tag", "proxy")
            .put("server", config.address)
            .put("server_port", config.port)
            .put("password", config.password ?: config.uuid)
            .apply {
                if (config.obfsType != null && config.obfsType != "none") {
                    val obfs = JSONObject().put("type", config.obfsType)
                    config.obfsPassword?.let { obfs.put("password", it) }
                    put("obfs", obfs)
                }
                config.upMbps?.let { put("up_mbps", it) }
                config.downMbps?.let { put("down_mbps", it) }
                put("tls", JSONObject()
                    .put("enabled", true)
                    .put("server_name", config.sni ?: config.address))
            }
    }

    private fun wireGuardOutbound(config: ProxyConfig): JSONObject {
        val localAddress = if (config.wgAddress != null) {
            JSONArray().put(config.wgAddress)
        } else {
            JSONArray().put("10.0.0.2/32")
        }
        val allowedIps = if (config.wgAllowedIps != null) {
            JSONArray(config.wgAllowedIps.split(",").map { it.trim() })
        } else {
            JSONArray().put("0.0.0.0/0")
        }
        return JSONObject()
            .put("type", "wireguard")
            .put("tag", "proxy")
            .put("server", config.address)
            .put("server_port", config.port)
            .put("local_address", localAddress)
            .put("private_key", config.privateKey ?: "")
            .put("peer_public_key", config.publicKey ?: "")
            .put("allowed_ips", allowedIps)
            .apply {
                config.wgDns?.let { put("dns", it) }
                config.reserved?.let {
                    val parts = it.split(",").mapNotNull { s -> s.trim().toIntOrNull() }
                    if (parts.size == 3) put("reserved", JSONArray(parts))
                }
            }
    }

    private fun JSONObject.putTlsIfNeeded(config: ProxyConfig, force: Boolean = false) {
        val hasTls = force || config.security == "tls" || config.security == "reality" || config.sni != null
        if (!hasTls) return

        val tls = JSONObject()
            .put("enabled", true)
            .put("server_name", config.sni ?: config.address)

        // Enable uTLS to mimic a browser fingerprint (essential for bypassing censorship and Reality)
        val utls = JSONObject()
            .put("enabled", true)
            .put("fingerprint", "chrome")
        tls.put("utls", utls)

        if (config.security == "reality") {
            val reality = JSONObject()
                .put("enabled", true)
            config.pbk?.let { reality.put("public_key", it) }
            config.sid?.let { reality.put("short_id", it) }
            tls.put("reality", reality)
        }

        put("tls", tls)
    }

    private fun JSONObject.putTransportIfNeeded(config: ProxyConfig) {
        val network = config.network?.lowercase() ?: return
        when (network) {
            "ws" -> {
                put("transport", JSONObject()
                    .put("type", "ws")
                    .apply { config.wsPath?.let { put("path", it) } }
                    .apply {
                        if (config.wsHost != null) {
                            put("headers", JSONObject().put("Host", config.wsHost))
                        }
                    }
                )
            }
            "grpc" -> {
                put("transport", JSONObject()
                    .put("type", "grpc")
                    .apply {
                        config.grpcServiceName?.let {
                            put("service", it)
                        }
                    }
                )
            }
            "h2", "http" -> {
                put("transport", JSONObject()
                    .put("type", "http")
                    .apply {
                        config.wsPath?.let { put("path", it) }
                        if (config.wsHost != null) {
                            put("host", JSONArray().put(config.wsHost))
                        }
                    }
                )
            }
        }
    }

    private fun routeSection(options: SingBoxOptions): JSONObject {
        val rules = JSONArray()
            .put(
                JSONObject()
                    .put("inbound", "tun-in")
                    .put("action", "sniff")
                    .put("sniffer", JSONArray(listOf("http", "tls", "quic", "stun")))
            )
            .put(
                JSONObject()
                    .put("protocol", "dns")
                    .put("action", "hijack-dns")
            )

        if (options.routingProfile == "BLOCK_ADS") {
            rules.put(
                JSONObject()
                    .put("domain_suffix", JSONArray(adBlockDomainSuffixes()))
                    .put("action", "reject")
            )
        }

        if (options.lanBypassEnabled) {
            rules.put(JSONObject().put("ip_is_private", true).put("outbound", "direct"))
        }

        if (options.routingProfile == "BYPASS_LAN_CN_RU") {
            rules.put(
                JSONObject()
                    .put("domain_suffix", JSONArray(regionalBypassDomainSuffixes()))
                    .put("outbound", "direct")
            )
        }

        return JSONObject()
            .put("rules", rules)
            .put(
                "final",
                when (options.routingProfile) {
                    "DIRECT" -> "direct"
                    else -> "proxy"
                }
            )
            // NOTE: auto_detect_interface is intentionally REMOVED.
            // We use usePlatformAutoDetectInterfaceControl()=true in PlatformInterfaceImpl
            // which calls VpnService.protect(fd) on each outbound socket.
            // Having auto_detect_interface=true here causes sing-box to try procfs
            // interface detection (which fails on Android) instead of using the platform.
    }

    private fun regionalBypassDomainSuffixes(): List<String> = listOf(
        "ru",
        "xn--p1ai",
        "su",
        "cn",
        "gov.ru",
        "gosuslugi.ru",
        "yandex.ru",
        "vk.com",
        "mail.ru",
        "ok.ru"
    )

    private fun adBlockDomainSuffixes(): List<String> = listOf(
        "ads",
        "adservice.google.com",
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "adnxs.com",
        "adsrvr.org",
        "appsflyer.com",
        "facebook.com",
        "scorecardresearch.com",
        "taboola.com",
        "outbrain.com"
    )
}
