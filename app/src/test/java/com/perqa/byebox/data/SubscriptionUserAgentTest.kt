package com.perqa.byebox.data

import com.perqa.byebox.BuildConfig
import com.v2ray.ang.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Регрессии по User-Agent подписок:
 *  - дефолт должен быть реальным клиентским UA (Happ), а не версией ByeBox;
 *  - пресет «По умолчанию» не должен расходиться с AppConfig;
 *  - fallback-цепочка должна пробовать v2rayNG после Happ.
 */
class SubscriptionUserAgentTest {

    private val canonicalHapp = Regex("""Happ/\d+\.\d+\.\d+""")
    private val canonicalV2rayNg = Regex("""v2rayNG/\d+\.\d+\.\d+""")

    @Test
    fun defaultSubscriptionUserAgentIsHapp() {
        assertTrue(
            "default UA must look like Happ/x.y.z: ${AppConfig.DEFAULT_SUBSCRIPTION_USER_AGENT}",
            canonicalHapp.matches(AppConfig.DEFAULT_SUBSCRIPTION_USER_AGENT)
        )
    }

    @Test
    fun defaultPresetMatchesAppConfig() {
        assertEquals(
            "«По умолчанию» в UI должно означать тот же UA, что уходит в запрос",
            AppConfig.DEFAULT_SUBSCRIPTION_USER_AGENT,
            UaPresets.defaultPreset.value
        )
        assertEquals("дефолтный пресет должен быть первым", UaPresets.defaultPreset, UaPresets.all.first())
    }

    @Test
    fun fallbackChainIsV2rayNg() {
        val fallbacks = AppConfig.SUBSCRIPTION_USER_AGENT_FALLBACKS
        assertEquals(
            listOf("v2rayNG/2.3.6", "v2rayNG/1.8.5"),
            fallbacks
        )
        assertEquals(AppConfig.FALLBACK_SUBSCRIPTION_USER_AGENT, fallbacks.first())
        fallbacks.forEach { ua ->
            assertTrue("fallback UA must look like v2rayNG/x.y.z: $ua", canonicalV2rayNg.matches(ua))
        }
    }

    @Test
    fun defaultUserAgentDoesNotUseByeBoxVersion() {
        assertNotEquals(
            "UA must not embed ByeBox's own version",
            "v2rayNG/${BuildConfig.VERSION_NAME}",
            AppConfig.DEFAULT_SUBSCRIPTION_USER_AGENT
        )
        assertFalse(
            "default UA must not be the ByeBox version",
            AppConfig.DEFAULT_SUBSCRIPTION_USER_AGENT.contains(BuildConfig.VERSION_NAME)
        )
    }
}
