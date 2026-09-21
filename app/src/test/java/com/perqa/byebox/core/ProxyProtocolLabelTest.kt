package com.perqa.byebox.core

import android.app.Application
import com.v2ray.ang.enums.EConfigType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class ProxyProtocolLabelTest {

    @Test
    fun rawJson_vlessOutbound_resolvesToVless() {
        val raw = """{"outbounds":[{"protocol":"vless","settings":{}}]}"""
        assertEquals("VLESS", ProxyProtocolLabel.fromRawJson(raw))
    }

    @Test
    fun rawJson_skipsNonProxyOutbounds() {
        val raw = """
            {
              "outbounds": [
                {"protocol": "freedom", "tag": "direct"},
                {"protocol": "blackhole", "tag": "block"},
                {"protocol": "dns", "tag": "dns-out"},
                {"protocol": "trojan", "settings": {}}
              ]
            }
        """.trimIndent()
        assertEquals("Trojan", ProxyProtocolLabel.fromRawJson(raw))
    }

    @Test
    fun rawJson_protocolIsCaseInsensitive() {
        val raw = """{"outbounds":[{"protocol":"Shadowsocks"}]}"""
        assertEquals("Shadowsocks", ProxyProtocolLabel.fromRawJson(raw))
    }

    @Test
    fun rawJson_unknownOrMissingOutbound_returnsNull() {
        assertNull(ProxyProtocolLabel.fromRawJson("""{"outbounds":[{"protocol":"freedom"}]}"""))
        assertNull(ProxyProtocolLabel.fromRawJson("""{"inbounds":[]}"""))
        assertNull(ProxyProtocolLabel.fromRawJson("not json at all"))
        assertNull(ProxyProtocolLabel.fromRawJson(""))
        assertNull(ProxyProtocolLabel.fromRawJson(null))
    }

    @Test
    fun configTypeLabels_matchExistingDisplayNames() {
        assertEquals("VMESS", ProxyProtocolLabel.forConfigType(EConfigType.VMESS))
        assertEquals("VLESS", ProxyProtocolLabel.forConfigType(EConfigType.VLESS))
        assertEquals("Trojan", ProxyProtocolLabel.forConfigType(EConfigType.TROJAN))
        assertEquals("Shadowsocks", ProxyProtocolLabel.forConfigType(EConfigType.SHADOWSOCKS))
        assertEquals("SOCKS", ProxyProtocolLabel.forConfigType(EConfigType.SOCKS))
        assertEquals("WireGuard", ProxyProtocolLabel.forConfigType(EConfigType.WIREGUARD))
        assertEquals("Hysteria2", ProxyProtocolLabel.forConfigType(EConfigType.HYSTERIA2))
        assertEquals("HTTP", ProxyProtocolLabel.forConfigType(EConfigType.HTTP))
    }
}
