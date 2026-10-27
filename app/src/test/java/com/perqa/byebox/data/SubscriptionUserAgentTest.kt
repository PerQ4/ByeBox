package com.perqa.byebox.data

import com.perqa.byebox.BuildConfig
import com.v2ray.ang.AppConfig
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Регрессия: в User-Agent для подписок подставлялась версия ByeBox
 * (`v2rayNG/${BuildConfig.VERSION_NAME}`). Панели подписок фильтруют клиентов
 * по UA и отклоняют неизвестные версии (например, «v2rayNG/1.5.5-beta.2»
 * получал 503), из-за чего подписки переставали парситься. В UA должна быть
 * реальная версия v2rayNG.
 */
class SubscriptionUserAgentTest {

    private val canonicalV2rayNg = Regex("""v2rayNG/\d+\.\d+\.\d+""")

    @Test
    fun defaultSubscriptionUserAgentIsCanonicalV2rayNg() {
        assertTrue(
            "default UA must look like v2rayNG/x.y.z: ${AppConfig.DEFAULT_SUBSCRIPTION_USER_AGENT}",
            canonicalV2rayNg.matches(AppConfig.DEFAULT_SUBSCRIPTION_USER_AGENT)
        )
    }

    @Test
    fun fallbackSubscriptionUserAgentIsCanonicalV2rayNg() {
        assertTrue(
            "fallback UA must look like v2rayNG/x.y.z: ${AppConfig.FALLBACK_SUBSCRIPTION_USER_AGENT}",
            canonicalV2rayNg.matches(AppConfig.FALLBACK_SUBSCRIPTION_USER_AGENT)
        )
    }

    @Test
    fun defaultUserAgentDoesNotUseByeBoxVersion() {
        assertNotEquals(
            "UA must not embed ByeBox's own version",
            "v2rayNG/${BuildConfig.VERSION_NAME}",
            AppConfig.DEFAULT_SUBSCRIPTION_USER_AGENT
        )
    }
}
