package com.v2ray.ang.handler

import android.app.Application
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Регрессии по дедупликации узлов подписки: панель может прислать один и тот
 * же узел дважды (например, auto/policy-group и прямой), а раньше это давало
 * два GUID и два одинаковых пункта в списке.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class ConfigSignatureTest {

    private fun vless(
        server: String = "de.example.com",
        port: String = "443",
        password: String = "ac94bf12-9518-57e3-9433-06e1d3fcab1d",
        remarks: String = "Node",
        security: String? = "reality",
        sni: String? = "microsoft.com",
        flow: String? = "xtls-rprx-vision",
        path: String? = null,
        network: String? = "tcp"
    ) = ProfileItem(
        configType = EConfigType.VLESS,
        remarks = remarks,
        description = "hidden",
        server = server,
        serverPort = port,
        password = password,
        security = security,
        sni = sni,
        flow = flow,
        path = path,
        network = network
    )

    @Test
    fun signatureIgnoresMetadataButKeepsIdentity() {
        val first = vless(remarks = "Name A")
        val second = vless(remarks = "Name B").apply {
            description = "another"
            subscriptionId = "sub-2"
            addedTime = 123L
        }

        assertEquals(
            "метаданные (имя/описание/subid/время) не должны влиять на сигнатуру",
            AngConfigManager.configSignature(first),
            AngConfigManager.configSignature(second)
        )
    }

    @Test
    fun signatureNormalizesServerCaseAndWhitespace() {
        val a = vless(server = "DE.Example.com ")
        val b = vless(server = "de.example.com")
        assertEquals(AngConfigManager.configSignature(a), AngConfigManager.configSignature(b))
    }

    @Test
    fun differentTransportParametersProduceDifferentSignatures() {
        val base = AngConfigManager.configSignature(vless())
        assertNotNull(base)

        val variants = listOf(
            vless(security = "tls"),
            vless(sni = "other.example.com"),
            vless(flow = null),
            vless(path = "/ws"),
            vless(network = "ws"),
            vless(password = "another-uuid"),
            vless(port = "8443"),
        )

        variants.forEach { variant ->
            assertNotEquals(
                "разные узлы не должны схлопываться: $variant",
                base,
                AngConfigManager.configSignature(variant)
            )
        }
    }

    @Test
    fun deduplicateKeepsFirstOccurrenceAndOrder() {
        val a = vless(remarks = "A", server = "a.example.com")
        val b = vless(remarks = "B", server = "b.example.com")
        val aDuplicate = vless(remarks = "A duplicate", server = "a.example.com")

        val result = AngConfigManager.deduplicateConfigs(listOf(a, b, aDuplicate))

        assertEquals(2, result.size)
        assertEquals("A", result[0].remarks)
        assertEquals("B", result[1].remarks)
    }

    @Test
    fun configsWithoutSignatureAreNotCollapsed() {
        val customOne = ProfileItem(configType = EConfigType.CUSTOM)
        val customTwo = ProfileItem(configType = EConfigType.CUSTOM)

        assertNull(AngConfigManager.configSignature(customOne))
        assertEquals(
            "custom-конфиги без server/port нельзя схлопывать вслепую",
            2,
            AngConfigManager.deduplicateConfigs(listOf(customOne, customTwo)).size
        )
    }

    @Test
    fun customConfigsOnSameHostAreNotDedupedByEndpointOnly() {
        val first = ProfileItem(
            configType = EConfigType.CUSTOM,
            remarks = "Node A",
            server = "same.example.com",
            serverPort = "443"
        )
        val second = ProfileItem(
            configType = EConfigType.CUSTOM,
            remarks = "Node B",
            server = "same.example.com",
            serverPort = "443"
        )

        val firstKey = AngConfigManager.customConfigDedupKey(first, """{"remarks":"Node A"}""")
        val secondKey = AngConfigManager.customConfigDedupKey(second, """{"remarks":"Node B"}""")

        assertNotEquals(
            "разные CUSTOM-конфиги на одном хосте нельзя схлопывать по host+port",
            firstKey,
            secondKey
        )
    }

    @Test
    fun defaultUaUsesFallbackChainButCustomUaDoesNot() {
        assertEquals(
            listOf("Happ/2.5.0", "v2rayNG/2.3.6", "v2rayNG/1.8.5"),
            AngConfigManager.subscriptionUserAgentCandidates(null)
        )
        assertEquals(
            listOf("Happ/2.5.0", "v2rayNG/2.3.6", "v2rayNG/1.8.5"),
            AngConfigManager.subscriptionUserAgentCandidates("  ")
        )
        assertEquals(
            "сохранённый Happ — это тоже дефолт (старый пресет), fallback нужен",
            listOf("Happ/2.5.0", "v2rayNG/2.3.6", "v2rayNG/1.8.5"),
            AngConfigManager.subscriptionUserAgentCandidates("Happ/2.5.0")
        )
        assertEquals(
            "свой UA не должен порождать лишних запросов",
            listOf("MyClient/1.0"),
            AngConfigManager.subscriptionUserAgentCandidates(" MyClient/1.0 ")
        )
    }
}
