package com.perqa.byebox.core

import com.v2ray.ang.enums.EConfigType
import org.json.JSONObject

/**
 * Resolves the user-facing protocol label for a proxy config.
 *
 * Subscription panels that support "expanded" delivery return full JSON configs
 * (see the `subscriptions-expand-now` handling in [com.v2ray.ang.handler.AngConfigManager]),
 * which are imported as [EConfigType.CUSTOM]. Those configs have no protocol field on
 * [com.v2ray.ang.dto.entities.ProfileItem], so before this helper every expanded node was
 * displayed as "CUSTOM" in the profile block, the server list and the status card.
 *
 * For custom configs the real protocol is read from the outbound in the stored raw JSON.
 */
object ProxyProtocolLabel {

    /** Human-readable label for a typed (link-parsed) config. */
    fun forConfigType(type: EConfigType): String = when (type) {
        EConfigType.VMESS -> "VMESS"
        EConfigType.VLESS -> "VLESS"
        EConfigType.TROJAN -> "Trojan"
        EConfigType.SHADOWSOCKS -> "Shadowsocks"
        EConfigType.SOCKS -> "SOCKS"
        EConfigType.WIREGUARD -> "WireGuard"
        EConfigType.HYSTERIA2 -> "Hysteria2"
        EConfigType.HYSTERIA -> "Hysteria"
        EConfigType.HTTP -> "HTTP"
        else -> type.name
    }

    /**
     * Protocol label inferred from a raw full-JSON config, or null when the
     * payload has no recognisable proxy outbound. "freedom", "blackhole" and
     * "dns" outbounds are skipped, so mixed configs still resolve correctly.
     */
    fun fromRawJson(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return try {
            val outbounds = JSONObject(raw).optJSONArray("outbounds") ?: return null
            for (i in 0 until outbounds.length()) {
                val protocol = outbounds.optJSONObject(i)?.optString("protocol").orEmpty()
                labels[protocol.lowercase()]?.let { return it }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private val labels = mapOf(
        "vless" to "VLESS",
        "vmess" to "VMESS",
        "trojan" to "Trojan",
        "shadowsocks" to "Shadowsocks",
        "socks" to "SOCKS",
        "http" to "HTTP",
        "wireguard" to "WireGuard",
        "hysteria2" to "Hysteria2",
        "hysteria" to "Hysteria"
    )
}
