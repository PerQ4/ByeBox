package com.perqa.byebox.data

import android.util.Base64
import java.net.URLDecoder
import java.util.UUID
import org.json.JSONObject

object ConfigParser {
    private val descriptionKeys = setOf("remarks", "remark", "desc", "description")

    fun parse(url: String): ProxyConfig? {
        val trimmed = url.trim()
        return try {
            when {
                trimmed.startsWith("vless://", ignoreCase = true) -> parseVless(trimmed)
                trimmed.startsWith("vmess://", ignoreCase = true) -> parseVmess(trimmed)
                trimmed.startsWith("trojan://", ignoreCase = true) -> parseTrojan(trimmed)
                trimmed.startsWith("ss://", ignoreCase = true) -> parseShadowsocks(trimmed)
                trimmed.startsWith("tuic://", ignoreCase = true) -> parseTuic(trimmed)
                trimmed.startsWith("hysteria2://", ignoreCase = true) -> parseHysteria2(trimmed)
                trimmed.startsWith("hy2://", ignoreCase = true) -> parseHysteria2(trimmed)
                trimmed.startsWith("wg://", ignoreCase = true) -> parseWireGuard(trimmed)
                trimmed.startsWith("wireguard://", ignoreCase = true) -> parseWireGuard(trimmed)
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseVless(url: String): ProxyConfig {
        val cleanUrl = url.substring(8)
        val hashIndex = cleanUrl.indexOf('#')
        val name = if (hashIndex != -1) {
            URLDecoder.decode(cleanUrl.substring(hashIndex + 1), "UTF-8")
        } else {
            "VLESS Server"
        }
        val mainPart = if (hashIndex != -1) cleanUrl.substring(0, hashIndex) else cleanUrl

        val atIndex = mainPart.indexOf('@')
        val uuid = mainPart.substring(0, atIndex)
        val rest = mainPart.substring(atIndex + 1)

        val colonIndex = rest.indexOf(':')
        val queryStartIndex = rest.indexOf('?')
        
        val address = rest.substring(0, colonIndex)
        val portStr = if (queryStartIndex != -1) {
            rest.substring(colonIndex + 1, queryStartIndex)
        } else {
            rest.substring(colonIndex + 1)
        }
        val port = portStr.toIntOrNull() ?: 443

        var security: String? = null
        var sni: String? = null
        var pbk: String? = null
        var sid: String? = null
        var flow: String? = null
        var network: String? = null
        var wsPath: String? = null
        var wsHost: String? = null
        var description: String? = null

        if (queryStartIndex != -1) {
            val query = rest.substring(queryStartIndex + 1)
            val params = query.split('&')
            for (param in params) {
                val keyValue = param.split('=', limit = 2)
                if (keyValue.size == 2) {
                    val key = keyValue[0].lowercase()
                    val value = URLDecoder.decode(keyValue[1], "UTF-8")
                    when (key) {
                        "security" -> security = value
                        "sni" -> sni = value
                        "pbk" -> pbk = value
                        "sid" -> sid = value
                        "flow" -> flow = value
                        "type" -> network = value
                        "path" -> wsPath = value
                        "host" -> wsHost = value
                        "servicename" -> grpcServiceName = value
                        in descriptionKeys -> description = value
                    }
                }
            }
        }

        val flag = getFlagForName(name)

        return ProxyConfig(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            protocol = "VLESS",
            address = address,
            port = port,
            uuid = uuid,
            flow = flow,
            security = security,
            sni = sni,
            pbk = pbk,
            sid = sid,
            network = network,
            wsPath = wsPath,
            wsHost = wsHost,
            countryFlag = flag
        )
    }

    private fun parseVmess(url: String): ProxyConfig? {
        val cleanUrl = url.substring(8)
        val decodedBytes = Base64.decode(cleanUrl, Base64.DEFAULT)
        val decodedStr = String(decodedBytes, Charsets.UTF_8)
        
        val json = JSONObject(decodedStr)
        val name = json.optString("ps", "VMESS Server")
        val address = json.optString("add", "")
        val port = json.optInt("port", 443)
        val uuid = json.optString("id", "")
        val security = json.optString("scy", "auto")
        val sni = json.optString("sni", "")
        val network = json.optString("net", "tcp")
        val wsPath = json.optString("path", "").trim().takeIf { it.isNotEmpty() }
        val wsHost = json.optString("host", "").trim().takeIf { it.isNotEmpty() }
        val description = listOf("remarks", "remark", "desc", "description")
            .firstNotNullOfOrNull { key ->
                json.optString(key, "").trim().takeIf { it.isNotEmpty() }
            }

        val flag = getFlagForName(name)

        return ProxyConfig(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            protocol = "VMESS",
            address = address,
            port = port,
            uuid = uuid,
            security = if (json.optString("tls") == "tls") "tls" else security,
            sni = sni,
            network = network,
            wsPath = wsPath,
            wsHost = wsHost,
            countryFlag = flag
        )
    }

    private fun parseTrojan(url: String): ProxyConfig {
        val cleanUrl = url.substring(9)
        val hashIndex = cleanUrl.indexOf('#')
        val name = if (hashIndex != -1) {
            URLDecoder.decode(cleanUrl.substring(hashIndex + 1), "UTF-8")
        } else {
            "Trojan Server"
        }
        val mainPart = if (hashIndex != -1) cleanUrl.substring(0, hashIndex) else cleanUrl

        val atIndex = mainPart.indexOf('@')
        val password = mainPart.substring(0, atIndex)
        val rest = mainPart.substring(atIndex + 1)

        val colonIndex = rest.indexOf(':')
        val queryStartIndex = rest.indexOf('?')
        
        val address = rest.substring(0, colonIndex)
        val portStr = if (queryStartIndex != -1) {
            rest.substring(colonIndex + 1, queryStartIndex)
        } else {
            rest.substring(colonIndex + 1)
        }
        val port = portStr.toIntOrNull() ?: 443

        var sni: String? = null
        var network: String? = null
        var wsPath: String? = null
        var wsHost: String? = null
        var grpcServiceName: String? = null
        var description: String? = null
        if (queryStartIndex != -1) {
            val query = rest.substring(queryStartIndex + 1)
            val params = query.split('&')
            for (param in params) {
                val keyValue = param.split('=', limit = 2)
                if (keyValue.size == 2) {
                    val key = keyValue[0].lowercase()
                    val value = URLDecoder.decode(keyValue[1], "UTF-8")
                    when (key) {
                        "sni" -> sni = value
                        "type" -> network = value
                        "path" -> wsPath = value
                        "host" -> wsHost = value
                        "servicename" -> grpcServiceName = value
                        in descriptionKeys -> description = value
                    }
                }
            }
        }

        val flag = getFlagForName(name)

        return ProxyConfig(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            protocol = "Trojan",
            address = address,
            port = port,
            uuid = password,
            sni = sni,
            network = network,
            wsPath = wsPath,
            wsHost = wsHost,
            grpcServiceName = grpcServiceName,
            countryFlag = flag
        )
    }

    private fun parseShadowsocks(url: String): ProxyConfig {
        val cleanUrl = url.substring(5)
        val hashIndex = cleanUrl.indexOf('#')
        val name = if (hashIndex != -1) {
            URLDecoder.decode(cleanUrl.substring(hashIndex + 1), "UTF-8")
        } else {
            "Shadowsocks Server"
        }
        val mainPart = if (hashIndex != -1) cleanUrl.substring(0, hashIndex) else cleanUrl

        val atIndex = mainPart.indexOf('@')
        var userinfo = ""
        var rest = ""
        if (atIndex != -1) {
            val encodedUserinfo = mainPart.substring(0, atIndex)
            rest = mainPart.substring(atIndex + 1)
            userinfo = try {
                String(Base64.decode(encodedUserinfo, Base64.DEFAULT), Charsets.UTF_8)
            } catch (e: Exception) {
                encodedUserinfo
            }
        } else {
            rest = mainPart
        }

        val colonIndex = rest.indexOf(':')
        val queryStartIndex = rest.indexOf('?')
        val addressEnd = when {
            colonIndex != -1 -> colonIndex
            queryStartIndex != -1 -> queryStartIndex
            else -> rest.length
        }
        val address = rest.substring(0, addressEnd)
        val portPart = if (colonIndex != -1) {
            if (queryStartIndex != -1) rest.substring(colonIndex + 1, queryStartIndex) else rest.substring(colonIndex + 1)
        } else {
            "1080"
        }
        val portStr = portPart
        val port = portStr.toIntOrNull() ?: 1080
        val description = if (queryStartIndex != -1) {
            rest.substring(queryStartIndex + 1)
                .split('&')
                .firstNotNullOfOrNull { param ->
                    val keyValue = param.split('=', limit = 2)
                    if (keyValue.size == 2 && keyValue[0].lowercase() in descriptionKeys) {
                        URLDecoder.decode(keyValue[1], "UTF-8")
                    } else {
                        null
                    }
                }
        } else {
            null
        }

        val flag = getFlagForName(name)

        return ProxyConfig(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            protocol = "Shadowsocks",
            address = address,
            port = port,
            uuid = userinfo,
            countryFlag = flag
        )
    }

    private fun parseTuic(url: String): ProxyConfig {
        val cleanUrl = url.substring(7)
        val hashIndex = cleanUrl.indexOf('#')
        val name = if (hashIndex != -1) {
            URLDecoder.decode(cleanUrl.substring(hashIndex + 1), "UTF-8")
        } else {
            "TUIC Server"
        }
        val mainPart = if (hashIndex != -1) cleanUrl.substring(0, hashIndex) else cleanUrl

        val atIndex = mainPart.indexOf('@')
        val userPart = mainPart.substring(0, atIndex)
        val rest = mainPart.substring(atIndex + 1)

        val colonIndex = rest.indexOf(':')
        val queryStartIndex = rest.indexOf('?')

        val address = rest.substring(0, colonIndex)
        val portStr = if (queryStartIndex != -1) {
            rest.substring(colonIndex + 1, queryStartIndex)
        } else {
            rest.substring(colonIndex + 1)
        }
        val port = portStr.toIntOrNull() ?: 443

        // userPart = "uuid:password" or just "uuid"
        val userColon = userPart.indexOf(':')
        val uuid = if (userColon != -1) userPart.substring(0, userColon) else userPart
        val password = if (userColon != -1) userPart.substring(userColon + 1) else null

        var congestionControl: String? = null
        var alpn: String? = null
        var udpRelayMode: String? = null
        var zeroRttHandshake = false
        var sni: String? = null
        var description: String? = null

        if (queryStartIndex != -1) {
            val query = rest.substring(queryStartIndex + 1)
            val params = query.split('&')
            for (param in params) {
                val keyValue = param.split('=', limit = 2)
                if (keyValue.size == 2) {
                    val key = keyValue[0].lowercase()
                    val value = URLDecoder.decode(keyValue[1], "UTF-8")
                    when (key) {
                        "congestion_control" -> congestionControl = value
                        "alpn" -> alpn = value
                        "udp_relay_mode" -> udpRelayMode = value
                        "zero_rtt_handshake" -> zeroRttHandshake = value.toBoolean()
                        "sni" -> sni = value
                        in descriptionKeys -> description = value
                    }
                }
            }
        }

        return ProxyConfig(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            protocol = "TUIC",
            address = address,
            port = port,
            uuid = uuid,
            password = password,
            congestionControl = congestionControl,
            alpn = alpn,
            udpRelayMode = udpRelayMode,
            zeroRttHandshake = zeroRttHandshake,
            sni = sni,
            countryFlag = getFlagForName(name)
        )
    }

    private fun parseHysteria2(url: String): ProxyConfig {
        val prefix = if (url.startsWith("hy2://", ignoreCase = true)) 6 else 12
        val cleanUrl = url.substring(prefix)
        val hashIndex = cleanUrl.indexOf('#')
        val name = if (hashIndex != -1) {
            URLDecoder.decode(cleanUrl.substring(hashIndex + 1), "UTF-8")
        } else {
            "Hysteria2 Server"
        }
        val mainPart = if (hashIndex != -1) cleanUrl.substring(0, hashIndex) else cleanUrl

        val atIndex = mainPart.indexOf('@')
        val passwordPart = if (atIndex != -1) mainPart.substring(0, atIndex) else ""
        val rest = if (atIndex != -1) mainPart.substring(atIndex + 1) else mainPart

        val colonIndex = rest.indexOf(':')
        val queryStartIndex = rest.indexOf('?')

        val address = rest.substring(0, colonIndex)
        val portStr = if (queryStartIndex != -1) {
            rest.substring(colonIndex + 1, queryStartIndex)
        } else {
            rest.substring(colonIndex + 1)
        }
        val port = portStr.toIntOrNull() ?: 443
        val password = URLDecoder.decode(passwordPart, "UTF-8")

        var obfsType: String? = null
        var obfsPassword: String? = null
        var sni: String? = null
        var upMbps: Int? = null
        var downMbps: Int? = null
        var description: String? = null

        if (queryStartIndex != -1) {
            val query = rest.substring(queryStartIndex + 1)
            val params = query.split('&')
            for (param in params) {
                val keyValue = param.split('=', limit = 2)
                if (keyValue.size == 2) {
                    val key = keyValue[0].lowercase()
                    val value = URLDecoder.decode(keyValue[1], "UTF-8")
                    when (key) {
                        "obfs" -> obfsType = value
                        "obfs-password" -> obfsPassword = value
                        "sni" -> sni = value
                        "up" -> upMbps = value.toIntOrNull()
                        "down" -> downMbps = value.toIntOrNull()
                        in descriptionKeys -> description = value
                    }
                }
            }
        }

        return ProxyConfig(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            protocol = "Hysteria2",
            address = address,
            port = port,
            uuid = "",
            password = password,
            obfsType = obfsType,
            obfsPassword = obfsPassword,
            sni = sni,
            upMbps = upMbps,
            downMbps = downMbps,
            countryFlag = getFlagForName(name)
        )
    }

    private fun parseWireGuard(url: String): ProxyConfig {
        val prefix = if (url.startsWith("wireguard://", ignoreCase = true)) 12 else 5
        val cleanUrl = url.substring(prefix)
        val hashIndex = cleanUrl.indexOf('#')
        val name = if (hashIndex != -1) {
            URLDecoder.decode(cleanUrl.substring(hashIndex + 1), "UTF-8")
        } else {
            "WireGuard Server"
        }
        val mainPart = if (hashIndex != -1) cleanUrl.substring(0, hashIndex) else cleanUrl

        val atIndex = mainPart.indexOf('@')
        val publicKey = if (atIndex != -1) mainPart.substring(0, atIndex) else ""
        val rest = if (atIndex != -1) mainPart.substring(atIndex + 1) else mainPart

        val colonIndex = rest.indexOf(':')
        val queryStartIndex = rest.indexOf('?')

        val address = rest.substring(0, colonIndex)
        val portStr = if (queryStartIndex != -1) {
            rest.substring(colonIndex + 1, queryStartIndex)
        } else {
            rest.substring(colonIndex + 1)
        }
        val port = portStr.toIntOrNull() ?: 51820

        var privateKey: String? = null
        var wgAddress: String? = null
        var wgDns: String? = null
        var wgAllowedIps: String? = null
        var reserved: String? = null
        var mtu: String? = null
        var description: String? = null

        if (queryStartIndex != -1) {
            val query = rest.substring(queryStartIndex + 1)
            val params = query.split('&')
            for (param in params) {
                val keyValue = param.split('=', limit = 2)
                if (keyValue.size == 2) {
                    val key = keyValue[0].lowercase()
                    val value = URLDecoder.decode(keyValue[1], "UTF-8")
                    when (key) {
                        "private_key" -> privateKey = value
                        "address" -> wgAddress = value
                        "dns" -> wgDns = value
                        "allowed_ips" -> wgAllowedIps = value
                        "reserved" -> reserved = value
                        "mtu" -> mtu = value
                        in descriptionKeys -> description = value
                    }
                }
            }
        }

        return ProxyConfig(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            protocol = "WireGuard",
            address = address,
            port = port,
            uuid = "",
            privateKey = privateKey,
            publicKey = publicKey,
            wgAddress = wgAddress,
            wgDns = wgDns,
            wgAllowedIps = wgAllowedIps,
            reserved = reserved,
            countryFlag = getFlagForName(name)
        )
    }

    private fun getFlagForName(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("ru") || lower.contains("russia") || lower.contains("россия") -> "🇷🇺"
            lower.contains("us") || lower.contains("usa") || lower.contains("united states") || lower.contains("сша") -> "🇺🇸"
            lower.contains("de") || lower.contains("germany") || lower.contains("германия") -> "🇩🇪"
            lower.contains("nl") || lower.contains("netherlands") || lower.contains("нидерланды") -> "🇳🇱"
            lower.contains("gb") || lower.contains("uk") || lower.contains("london") || lower.contains("великобритания") -> "🇬🇧"
            lower.contains("fr") || lower.contains("france") || lower.contains("франция") -> "🇫🇷"
            lower.contains("fi") || lower.contains("finland") || lower.contains("финляндия") -> "🇫🇮"
            lower.contains("sg") || lower.contains("singapore") || lower.contains("сингапур") -> "🇸🇬"
            lower.contains("tr") || lower.contains("turkey") || lower.contains("турция") -> "🇹🇷"
            lower.contains("pl") || lower.contains("poland") || lower.contains("польша") -> "🇵🇱"
            lower.contains("hk") || lower.contains("hong kong") || lower.contains("гонконг") -> "🇭🇰"
            lower.contains("jp") || lower.contains("japan") || lower.contains("япония") -> "🇯🇵"
            else -> "🌐"
        }
    }
}

