package com.perqa.byebox.core

import com.perqa.byebox.data.ProxyConfig
import org.json.JSONArray
import org.json.JSONObject

data class XrayOptions(
    val dnsAddress: String,
    val routingProfile: String,
    val lanBypassEnabled: Boolean,
    val appRoutingMode: String = "OFF",
    val appRoutingPackages: List<String> = emptyList(),
    val mtu: Int = 1500,
    val tunStack: String = "gvisor"
)

object XrayConfigGenerator {
    private val PRIVATE_IP_CIDRS = listOf(
        "10.0.0.0/8",
        "100.64.0.0/10",
        "127.0.0.0/8",
        "169.254.0.0/16",
        "172.16.0.0/12",
        "192.168.0.0/16",
        "fd00::/8",
        "fe80::/10",
        "::1/128",
        "127.0.0.1/32"
    )

    fun generate(activeConfig: ProxyConfig, options: XrayOptions): String {
        val config = JSONObject()
            .put("log", logSection())
            .put("dns", dnsSection(options))
            .put("inbounds", JSONArray()
                .put(tunInbound(options))
            )
            .put("outbounds", outbounds(activeConfig))
            .put("routing", routingSection(options))
            .put("stats", JSONObject())
            .put("policy", JSONObject().put("system", JSONObject()
                .put("statsOutboundUplink", true)
                .put("statsOutboundDownlink", true)
            ))

        return config.toString(2)
    }

    private fun logSection(): JSONObject {
        return JSONObject().put("loglevel", "warning")
    }

    private fun dnsSection(options: XrayOptions): JSONObject {
        val dnsServer = if (options.dnsAddress == "System Default" || options.dnsAddress.isBlank()) {
            "8.8.8.8"
        } else {
            options.dnsAddress
        }
        return JSONObject()
            .put("servers", JSONArray().put(dnsServer))
            .put("queryStrategy", "UseIP")
            .put("disableFallback", false)
            .put("disableCache", false)
    }

    private fun tunInbound(options: XrayOptions): JSONObject {
        val settings = JSONObject()
            .put("name", "tun0")
            .put("MTU", options.mtu)

        return JSONObject()
            .put("protocol", "tun")
            .put("tag", "tun-in")
            .put("settings", settings)
            .put("sniffing", JSONObject()
                .put("enabled", true)
                .put("destOverride", JSONArray().put("http").put("tls"))
                .put("routeOnly", true)
            )
    }

    private fun outbounds(config: ProxyConfig): JSONArray {
        val list = JSONArray()
        list.put(proxyOutbound(config))
        list.put(JSONObject().put("protocol", "freedom").put("tag", "direct"))
        list.put(JSONObject().put("protocol", "blackhole").put("tag", "block"))
        list.put(JSONObject().put("protocol", "dns").put("tag", "dns-out"))
        return list
    }

    private fun proxyOutbound(config: ProxyConfig): JSONObject {
        return when (config.protocol.uppercase()) {
            "VLESS" -> vlessOutbound(config)
            "VMESS" -> vmessOutbound(config)
            "TROJAN" -> trojanOutbound(config)
            "SHADOWSOCKS" -> shadowsocksOutbound(config)
            else -> JSONObject()
                .put("protocol", "blackhole")
                .put("tag", "proxy")
                .put("settings", JSONObject().put("response", JSONObject().put("type", "none")))
        }
    }

    private fun vlessOutbound(config: ProxyConfig): JSONObject {
        val vnext = JSONObject()
            .put("address", config.address)
            .put("port", config.port)
            .put("users", JSONArray().put(
                JSONObject()
                    .put("id", config.uuid)
                    .put("encryption", "none")
                    .apply {
                        val flowValue = config.flow ?: if (config.security == "reality") "xtls-rprx-vision" else null
                        flowValue?.let { put("flow", it) }
                    }
            ))

        val settings = JSONObject().put("vnext", JSONArray().put(vnext))

        val streamSettings = JSONObject()
        putStreamSettings(streamSettings, config)

        return JSONObject()
            .put("protocol", "vless")
            .put("tag", "proxy")
            .put("settings", settings)
            .put("streamSettings", streamSettings)
    }

    private fun vmessOutbound(config: ProxyConfig): JSONObject {
        val vnext = JSONObject()
            .put("address", config.address)
            .put("port", config.port)
            .put("users", JSONArray().put(
                JSONObject()
                    .put("id", config.uuid)
                    .put("security", config.security?.takeIf { it != "tls" } ?: "auto")
            ))

        val settings = JSONObject().put("vnext", JSONArray().put(vnext))

        val streamSettings = JSONObject()
        putStreamSettings(streamSettings, config)

        return JSONObject()
            .put("protocol", "vmess")
            .put("tag", "proxy")
            .put("settings", settings)
            .put("streamSettings", streamSettings)
    }

    private fun trojanOutbound(config: ProxyConfig): JSONObject {
        val server = JSONObject()
            .put("address", config.address)
            .put("port", config.port)
            .put("password", config.uuid)

        val settings = JSONObject().put("servers", JSONArray().put(server))

        val streamSettings = JSONObject()
        putStreamSettings(streamSettings, config, forceTls = true)

        return JSONObject()
            .put("protocol", "trojan")
            .put("tag", "proxy")
            .put("settings", settings)
            .put("streamSettings", streamSettings)
    }

    private fun shadowsocksOutbound(config: ProxyConfig): JSONObject {
        val parts = config.uuid.split(":", limit = 2)
        val method = parts.getOrNull(0)?.ifBlank { "chacha20-ietf-poly1305" } ?: "chacha20-ietf-poly1305"
        val password = parts.getOrNull(1).orEmpty()

        val server = JSONObject()
            .put("address", config.address)
            .put("port", config.port)
            .put("method", method)
            .put("password", password)

        val settings = JSONObject().put("servers", JSONArray().put(server))

        return JSONObject()
            .put("protocol", "shadowsocks")
            .put("tag", "proxy")
            .put("settings", settings)
    }

    private fun putStreamSettings(stream: JSONObject, config: ProxyConfig, forceTls: Boolean = false) {
        val networkType = config.network?.lowercase() ?: "tcp"
        stream.put("network", networkType)

        val hasTls = forceTls || config.security == "tls" || config.security == "reality" || config.sni != null
        if (hasTls) {
            val securityType = if (config.security == "reality") "reality" else "tls"
            stream.put("security", securityType)

            if (securityType == "reality") {
                val realitySettings = JSONObject()
                    .put("fingerprint", "chrome")
                    .put("serverName", config.sni ?: config.address)
                config.pbk?.let { realitySettings.put("publicKey", it) }
                config.sid?.let { realitySettings.put("shortId", it) }
                stream.put("realitySettings", realitySettings)
            } else {
                val tlsSettings = JSONObject()
                    .put("fingerprint", "chrome")
                    .put("serverName", config.sni ?: config.address)
                stream.put("tlsSettings", tlsSettings)
            }
        }

        when (networkType) {
            "ws" -> {
                val wsSettings = JSONObject()
                    .apply { config.wsPath?.let { put("path", it) } }
                    .apply {
                        if (config.wsHost != null) {
                            put("headers", JSONObject().put("Host", config.wsHost))
                        }
                    }
                stream.put("wsSettings", wsSettings)
            }
            "grpc" -> {
                val grpcSettings = JSONObject()
                    .apply { config.grpcServiceName?.let { put("serviceName", it) } }
                stream.put("grpcSettings", grpcSettings)
            }
            "http", "h2" -> {
                val httpSettings = JSONObject()
                    .apply { config.wsPath?.let { put("path", it) } }
                    .apply {
                        if (config.wsHost != null) {
                            put("host", JSONArray().put(config.wsHost))
                        }
                    }
                stream.put("httpSettings", httpSettings)
            }
        }
    }

    private fun routingSection(options: XrayOptions): JSONObject {
        val rules = JSONArray()

        rules.put(
            JSONObject()
                .put("type", "field")
                .put("port", "53")
                .put("network", "udp")
                .put("outboundTag", "dns-out")
        )

        if (options.lanBypassEnabled) {
            rules.put(
                JSONObject()
                    .put("type", "field")
                    .put("ip", JSONArray(PRIVATE_IP_CIDRS))
                    .put("outboundTag", "direct")
            )
        }

        if (options.routingProfile == "BYPASS_LAN_CN_RU") {
            rules.put(
                JSONObject()
                    .put("type", "field")
                    .put("domain", JSONArray(regionalBypassDomains()))
                    .put("outboundTag", "direct")
            )
        }

        rules.put(
            JSONObject()
                .put("type", "field")
                .put("inboundTag", JSONArray().put("tun-in"))
                .put("outboundTag", "proxy")
        )

        return JSONObject()
            .put("domainStrategy", "AsIs")
            .put("rules", rules)
    }

    private fun regionalBypassDomains(): List<String> = listOf(
        "regexp:\\.ru$",
        "regexp:\\.xn--p1ai$",
        "regexp:\\.su$",
        "regexp:\\.cn$",
        "domain:gosuslugi.ru",
        "domain:yandex.ru",
        "domain:vk.com",
        "domain:mail.ru",
        "domain:ok.ru"
    )
}
