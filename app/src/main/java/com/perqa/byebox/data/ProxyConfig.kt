package com.perqa.byebox.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

data class ProxyConfig(
    val id: String,
    val name: String,
    val description: String? = null,
    val protocol: String, // VLESS, VMESS, Trojan, Shadowsocks, TUIC, Hysteria2, WireGuard
    val address: String,
    val port: Int,
    val uuid: String,
    val flow: String? = null,
    val security: String? = null,
    val sni: String? = null,
    val pbk: String? = null, // reality public key
    val sid: String? = null, // reality short id

    // Transport layer (WS, gRPC, HTTP/2)
    val network: String? = null,      // "tcp", "ws", "grpc", "http"
    val wsPath: String? = null,       // WebSocket path
    val wsHost: String? = null,       // WebSocket Host header
    val grpcServiceName: String? = null, // gRPC service name

    // Shared: password for TUIC, Hysteria2, SSH, ShadowTLS
    val password: String? = null,
    // TUIC-specific
    val congestionControl: String? = null, // bbr, cubic, new_reno
    val alpn: String? = null,              // h3,h2
    val udpRelayMode: String? = null,      // native, quic
    val zeroRttHandshake: Boolean = false,
    // Hysteria2-specific
    val obfsType: String? = null,          // salamander, none
    val obfsPassword: String? = null,
    val upMbps: Int? = null,
    val downMbps: Int? = null,
    // WireGuard-specific
    val privateKey: String? = null,
    val publicKey: String? = null,
    val wgAddress: String? = null,
    val wgDns: String? = null,
    val wgAllowedIps: String? = null,
    val reserved: String? = null,          // WireGuard reserved bytes hex

    val ping: Int? = null,
    val failureCount: Int = 0,
    val lastFailureAt: Long? = null,
    val sourceName: String = "Локальные конфигурации",
    val sourceUrl: String? = null,
    val countryFlag: String = "🏳️"
) {
    fun toConfigLink(): String {
        return try {
            when (protocol) {
                "VLESS" -> {
                    val params = mutableListOf<String>()
                    if (security != null) params.add("security=$security")
                    if (sni != null) params.add("sni=$sni")
                    if (pbk != null) params.add("pbk=$pbk")
                    if (sid != null) params.add("sid=$sid")
                    if (flow != null) params.add("flow=$flow")
                    val query = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
                    "vless://$uuid@$address:$port$query#$name"
                }
                "VMESS" -> {
                    val json = JSONObject().apply {
                        put("v", "2")
                        put("ps", name)
                        put("add", address)
                        put("port", port)
                        put("id", uuid)
                        put("scy", security ?: "auto")
                        put("net", "tcp")
                        put("type", "none")
                    }
                    val base64 = Base64.encodeToString(json.toString().toByteArray(), Base64.NO_WRAP)
                    "vmess://$base64"
                }
                "Trojan" -> {
                    val query = if (sni != null) "?sni=$sni" else ""
                    "trojan://$uuid@$address:$port$query#$name"
                }
                "TUIC" -> {
                    val params = mutableListOf<String>()
                    if (congestionControl != null) params.add("congestion_control=$congestionControl")
                    if (udpRelayMode != null) params.add("udp_relay_mode=$udpRelayMode")
                    if (zeroRttHandshake) params.add("zero_rtt_handshake=true")
                    if (alpn != null) params.add("alpn=$alpn")
                    if (sni != null) params.add("sni=$sni")
                    val pass = password ?: uuid
                    val query = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
                    "tuic://$uuid:$pass@$address:$port$query#$name"
                }
                "Hysteria2" -> {
                    val params = mutableListOf<String>()
                    if (obfsType != null && obfsType != "none") params.add("obfs=$obfsType")
                    if (obfsPassword != null) params.add("obfs-password=$obfsPassword")
                    if (sni != null) params.add("sni=$sni")
                    if (upMbps != null) params.add("up=$upMbps")
                    if (downMbps != null) params.add("down=$downMbps")
                    val pass = password ?: uuid
                    val query = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
                    "hysteria2://$pass@$address:$port$query#$name"
                }
                "WireGuard" -> {
                    val params = mutableListOf<String>()
                    if (privateKey != null) params.add("private_key=$privateKey")
                    if (wgAddress != null) params.add("address=$wgAddress")
                    if (wgDns != null) params.add("dns=$wgDns")
                    if (wgAllowedIps != null) params.add("allowed_ips=$wgAllowedIps")
                    if (reserved != null) params.add("reserved=$reserved")
                    val pk = publicKey ?: ""
                    val query = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
                    "wg://$pk@$address:$port$query#$name"
                }
                else -> {
                    val base64 = Base64.encodeToString(uuid.toByteArray(), Base64.NO_WRAP)
                    "ss://$base64@$address:$port#$name"
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            description?.let { put("description", it) }
            put("protocol", protocol)
            put("address", address)
            put("port", port)
            put("uuid", uuid)
            flow?.let { put("flow", it) }
            security?.let { put("security", it) }
            sni?.let { put("sni", it) }
            pbk?.let { put("pbk", it) }
            sid?.let { put("sid", it) }
            network?.let { put("network", it) }
            wsPath?.let { put("ws_path", it) }
            wsHost?.let { put("ws_host", it) }
            grpcServiceName?.let { put("grpc_service_name", it) }
            password?.let { put("password", it) }
            congestionControl?.let { put("congestion_control", it) }
            alpn?.let { put("alpn", it) }
            udpRelayMode?.let { put("udp_relay_mode", it) }
            put("zero_rtt_handshake", zeroRttHandshake)
            obfsType?.let { put("obfs_type", it) }
            obfsPassword?.let { put("obfs_password", it) }
            upMbps?.let { put("up_mbps", it) }
            downMbps?.let { put("down_mbps", it) }
            privateKey?.let { put("private_key", it) }
            publicKey?.let { put("public_key", it) }
            wgAddress?.let { put("wg_address", it) }
            wgDns?.let { put("wg_dns", it) }
            wgAllowedIps?.let { put("wg_allowed_ips", it) }
            reserved?.let { put("reserved", it) }
            ping?.let { put("ping", it) }
            put("failure_count", failureCount)
            lastFailureAt?.let { put("last_failure_at", it) }
            put("source_name", sourceName)
            sourceUrl?.let { put("source_url", it) }
            put("country_flag", countryFlag)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): ProxyConfig {
            fun optNullableString(vararg keys: String): String? {
                for (key in keys) {
                    if (!json.has(key) || json.isNull(key)) continue
                    val value = json.optString(key, "").trim()
                    if (value.isNotEmpty() && value != "null") return value
                }
                return null
            }

            return ProxyConfig(
                id = json.optString("id", ""),
                name = json.optString("name", ""),
                description = optNullableString("description", "desc", "remarks", "remark"),
                protocol = json.optString("protocol", ""),
                address = json.optString("address", ""),
                port = json.optInt("port", 0),
                uuid = json.optString("uuid", ""),
                flow = optNullableString("flow"),
                security = optNullableString("security"),
                sni = optNullableString("sni"),
                pbk = optNullableString("pbk"),
                sid = optNullableString("sid"),
                network = optNullableString("network"),
                wsPath = optNullableString("ws_path", "wsPath"),
                wsHost = optNullableString("ws_host", "wsHost"),
                grpcServiceName = optNullableString("grpc_service_name", "grpcServiceName"),
                password = optNullableString("password"),
                congestionControl = optNullableString("congestion_control", "congestionControl"),
                alpn = optNullableString("alpn"),
                udpRelayMode = optNullableString("udp_relay_mode", "udpRelayMode"),
                zeroRttHandshake = json.optBoolean("zero_rtt_handshake", false),
                obfsType = optNullableString("obfs_type", "obfsType"),
                obfsPassword = optNullableString("obfs_password", "obfsPassword"),
                upMbps = if (json.has("up_mbps")) json.getInt("up_mbps") else null,
                downMbps = if (json.has("down_mbps")) json.getInt("down_mbps") else null,
                privateKey = optNullableString("private_key", "privateKey"),
                publicKey = optNullableString("public_key", "publicKey"),
                wgAddress = optNullableString("wg_address", "wgAddress"),
                wgDns = optNullableString("wg_dns", "wgDns"),
                wgAllowedIps = optNullableString("wg_allowed_ips", "wgAllowedIps"),
                reserved = optNullableString("reserved"),
                ping = if (json.has("ping")) json.getInt("ping") else null,
                failureCount = json.optInt("failure_count", json.optInt("failureCount", 0)),
                lastFailureAt = if (json.has("last_failure_at")) json.getLong("last_failure_at")
                    else if (json.has("lastFailureAt")) json.getLong("lastFailureAt") else null,
                sourceName = json.optString("source_name", json.optString("sourceName", "Локальные конфигурации")),
                sourceUrl = optNullableString("source_url", "sourceUrl"),
                countryFlag = json.optString("country_flag", json.optString("countryFlag", "🏳️"))
            )
        }
    }
}


