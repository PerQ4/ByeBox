package com.perqa.byebox.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

data class ProxyConfig(
    val id: String,
    val name: String,
    val description: String? = null,
    val protocol: String, // VLESS, VMESS, Trojan, Shadowsocks
    val address: String,
    val port: Int,
    val uuid: String,
    val flow: String? = null,
    val security: String? = null,
    val sni: String? = null,
    val pbk: String? = null, // reality public key
    val sid: String? = null, // reality short id

    // Transport layer (WS, gRPC, HTTP/2)
    val network: String? = null,
    val wsPath: String? = null,
    val wsHost: String? = null,
    val grpcServiceName: String? = null,

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
                    if (network != null) params.add("type=$network")
                    if (wsPath != null) params.add("path=$wsPath")
                    if (wsHost != null) params.add("host=$wsHost")
                    if (grpcServiceName != null) params.add("serviceName=$grpcServiceName")
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
                        put("net", network ?: "tcp")
                        put("type", "none")
                        if (security == "tls") put("tls", "tls")
                        if (sni != null) put("sni", sni)
                        if (grpcServiceName != null) {
                            put("path", grpcServiceName)
                        } else if (wsPath != null) {
                            put("path", wsPath)
                        }
                        if (wsHost != null) put("host", wsHost)
                    }
                    val base64 = Base64.encodeToString(json.toString().toByteArray(), Base64.NO_WRAP)
                    "vmess://$base64"
                }
                "Trojan" -> {
                    val params = mutableListOf<String>()
                    if (sni != null) params.add("sni=$sni")
                    if (network != null) params.add("type=$network")
                    if (wsPath != null) params.add("path=$wsPath")
                    if (wsHost != null) params.add("host=$wsHost")
                    if (grpcServiceName != null) params.add("serviceName=$grpcServiceName")
                    val query = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
                    "trojan://$uuid@$address:$port$query#$name"
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


