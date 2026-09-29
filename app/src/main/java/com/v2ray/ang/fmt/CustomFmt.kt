package com.v2ray.ang.fmt

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.v2ray.ang.core.CoreOutboundBuilder
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.util.JsonUtil

object CustomFmt : FmtBase() {
    /**
     * Parses a JSON string into a ProfileItem object.
     *
     * Legacy entry point used when importing raw JSON servers: keeps the config
     * type as CUSTOM and extracts at least the server address/port so the node
     * becomes usable. The full field extraction lives in [parseFull].
     *
     * @param str the JSON string to parse
     * @return the parsed ProfileItem object
     */
    fun parse(str: String): ProfileItem {
        val config = ProfileItem.create(EConfigType.CUSTOM)

        val fullConfig = JsonUtil.fromJson(str, V2rayConfig::class.java)
        val outbound = fullConfig?.getProxyOutbound()

        config.remarks = fullConfig?.remarks ?: System.currentTimeMillis().toString()
        config.server = outbound?.getServerAddress()
        config.serverPort = outbound?.getServerPort()?.toString()

        return config
    }

    /**
     * Fully parses a V2ray JSON config into a ProfileItem, extracting every
     * field the UI supports: protocol type, credentials, transport settings,
     * TLS/REALITY settings and QUIC/Hysteria specific fields.
     *
     * The returned ProfileItem gets the matching [EConfigType] so the node can
     * be re-used as a typed profile (not only as CUSTOM).
     *
     * @param str the full V2ray JSON config string
     * @return the parsed ProfileItem, or null when the JSON is not a valid config
     */
    fun parseFull(str: String): ProfileItem? {
        val fullConfig = JsonUtil.fromJson(str, V2rayConfig::class.java) ?: return null
        val outbound = fullConfig.getProxyOutbound() ?: return null

        val configType = detectConfigType(outbound.protocol) ?: EConfigType.CUSTOM
        val config = ProfileItem.create(configType)
        config.remarks = fullConfig.remarks
            ?: outbound.tag.takeIf { it.isNotBlank() }
            ?: System.currentTimeMillis().toString()
        config.server = outbound.getServerAddress()
        config.serverPort = outbound.getServerPort()?.toString()

        // Credentials / protocol users
        outbound.settings?.vnext?.firstOrNull()?.users?.firstOrNull()?.let { user ->
            config.password = user.id.takeIf { it.isNotBlank() }
            config.method = user.security?.takeIf { it.isNotBlank() }
            config.flow = user.flow?.takeIf { it.isNotBlank() }
        }
        outbound.settings?.servers?.firstOrNull()?.let { server ->
            config.password = server.password?.takeIf { it.isNotBlank() }
            config.method = server.method?.takeIf { it.isNotBlank() }
            config.flow = server.flow?.takeIf { it.isNotBlank() }
        }

        val stream = outbound.streamSettings ?: return config
        config.network = stream.network.takeIf { it.isNotBlank() } ?: "tcp"

        // TCP header hijacking
        stream.tcpSettings?.header?.let { header ->
            header.type.takeIf { it.isNotBlank() && it != "none" }?.let { config.headerType = it }
        }

        // WebSocket
        stream.wsSettings?.let { ws ->
            config.path = ws.path?.takeIf { it.isNotBlank() }
            config.host = ws.headers?.get("Host")?.takeIf { it.isNotBlank() }
                ?: ws.host?.takeIf { it.isNotBlank() }
        }

        // HTTPUpgrade
        stream.httpupgradeSettings?.let { hup ->
            config.path = hup.path?.takeIf { it.isNotBlank() }
            config.host = hup.host?.takeIf { it.isNotBlank() }
        }

        // XHTTP
        stream.xhttpSettings?.let { xhttp ->
            config.path = xhttp.path?.takeIf { it.isNotBlank() }
            config.host = xhttp.host?.takeIf { it.isNotBlank() }
            config.xhttpMode = xhttp.mode?.takeIf { it.isNotBlank() }
            xhttp.extra?.let { extra -> config.xhttpExtra = JsonUtil.toJson(extra) }
        }

        // gRPC
        stream.grpcSettings?.let { grpc ->
            config.serviceName = grpc.serviceName.takeIf { it.isNotBlank() }
            config.authority = grpc.authority?.takeIf { it.isNotBlank() }
        }

        // QUIC
        stream.quicSettings?.let { quic ->
            config.quicSecurity = quic.security.takeIf { it.isNotBlank() && it != "none" }
            config.quicKey = quic.key.takeIf { it.isNotBlank() }
        }

        // Hysteria auth
        stream.hysteriaSettings?.let { hy ->
            config.password = hy.auth?.takeIf { it.isNotBlank() } ?: config.password
        }

        // Security / TLS / REALITY
        config.security = stream.security?.takeIf { it.isNotBlank() }
        val tls = stream.tlsSettings ?: stream.realitySettings
        tls?.let {
            config.sni = it.serverName?.takeIf { s -> s.isNotBlank() }
            config.alpn = it.alpn?.takeIf { a -> a.isNotEmpty() }?.joinToString(",")
            config.fingerPrint = it.fingerprint?.takeIf { f -> f.isNotBlank() }
            config.insecure = it.allowInsecure.takeIf { insecure -> insecure }
            config.echConfigList = it.echConfigList?.takeIf { e -> e.isNotBlank() }
            config.verifyPeerCertByName = it.verifyPeerCertByName?.takeIf { v -> v.isNotBlank() }
            config.pinnedCA256 = it.pinnedPeerCertSha256?.takeIf { p -> p.isNotBlank() }
            config.publicKey = it.publicKey?.takeIf { p -> p.isNotBlank() }
            config.shortId = it.shortId?.takeIf { s -> s.isNotBlank() }
            config.spiderX = it.spiderX?.takeIf { s -> s.isNotBlank() }
            config.mldsa65Verify = it.mldsa65Verify?.takeIf { m -> m.isNotBlank() }
        }

        return config
    }

    /**
     * Builds an editable JSON document for a typed (non-CUSTOM) profile.
     * Typed profiles usually have no stored raw JSON, so the outbound is
     * reconstructed via [CoreOutboundBuilder] and wrapped in an `outbounds`
     * array that [parseFull] can read back.
     *
     * @param profile the typed profile to serialize
     * @return pretty-printed JSON, or null when the profile cannot be converted
     */
    fun buildEditableJson(profile: ProfileItem): String? {
        val outbound = CoreOutboundBuilder.convert(profile) ?: return null
        val root = JsonObject()
        val array = JsonArray()
        array.add(Gson().toJsonTree(outbound))
        root.add("outbounds", array)
        root.addProperty("remarks", profile.remarks)
        return GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
            .toJson(root)
    }

    private fun detectConfigType(protocol: String): EConfigType? = when (protocol.lowercase()) {
        "vmess" -> EConfigType.VMESS
        "vless" -> EConfigType.VLESS
        "trojan" -> EConfigType.TROJAN
        "shadowsocks", "ss" -> EConfigType.SHADOWSOCKS
        "socks" -> EConfigType.SOCKS
        "http" -> EConfigType.HTTP
        "wireguard" -> EConfigType.WIREGUARD
        "hysteria2" -> EConfigType.HYSTERIA2
        "hysteria" -> EConfigType.HYSTERIA
        else -> null
    }
}