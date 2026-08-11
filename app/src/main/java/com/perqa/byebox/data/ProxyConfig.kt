package com.perqa.byebox.data

import android.util.Base64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject

private val proxyConfigJsonAliases = mapOf(
    "desc" to "description",
    "remarks" to "description",
    "remark" to "description",
    "wsPath" to "ws_path",
    "wsHost" to "ws_host",
    "grpcServiceName" to "grpc_service_name",
    "failureCount" to "failure_count",
    "lastFailureAt" to "last_failure_at",
    "sourceName" to "source_name",
    "sourceUrl" to "source_url",
    "countryFlag" to "country_flag",
)

private val proxyJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
    coerceInputValues = true
}

@Serializable
data class ProxyConfig(
    val id: String,
    val name: String,
    val description: String? = null,
    val protocol: String,
    val address: String,
    val port: Int,
    val uuid: String,
    val flow: String? = null,
    val security: String? = null,
    val sni: String? = null,
    val pbk: String? = null,
    val sid: String? = null,
    val network: String? = null,
    @SerialName("ws_path")
    val wsPath: String? = null,
    @SerialName("ws_host")
    val wsHost: String? = null,
    @SerialName("grpc_service_name")
    val grpcServiceName: String? = null,
    val ping: Int? = null,
    @SerialName("failure_count")
    val failureCount: Int = 0,
    @SerialName("last_failure_at")
    val lastFailureAt: Long? = null,
    @SerialName("source_name")
    val sourceName: String = "Local Configs",
    @SerialName("source_url")
    val sourceUrl: String? = null,
    @SerialName("country_flag")
    val countryFlag: String = "🌍"
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
        val raw = proxyJson.encodeToString(this)
        return JSONObject(raw)
    }

    companion object {
        fun fromJson(json: JSONObject): ProxyConfig {
            val resolved = JSONObject()
            for (key in json.keys()) {
                resolved.put(proxyConfigJsonAliases.getOrDefault(key, key), json.get(key))
            }
            return proxyJson.decodeFromString(resolved.toString())
        }
    }
}
