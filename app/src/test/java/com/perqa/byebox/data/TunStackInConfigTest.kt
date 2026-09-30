package com.perqa.byebox.data

import com.google.gson.Gson
import com.perqa.byebox.data.TunStackMapping
import com.v2ray.ang.dto.V2rayConfig
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Регрессия на баг 1.5.4: поле стека TUN не попадало в конфиг, который
 * уходит в ядро Xray, поэтому выбор «gVisor / System» ничего не менял.
 *
 * Тест проверяет именно сериализацию — тот JSON, который `startLoop()`
 * получает как `result.content`.
 */
class TunStackInConfigTest {

    private fun buildTunInbound(stackName: String): V2rayConfig.InboundBean {
        val stackValue = TunStackMapping.toXrayValue(stackName)
        return V2rayConfig.InboundBean(
            tag = "tun",
            port = null,
            protocol = "tun",
            settings = V2rayConfig.InboundBean.InSettingsBean(
                name = "xray0",
                mtu = 1500,
                noKernelTun = TunStackMapping.toNoKernelTun(stackValue)
            )
        )
    }

    private fun serialize(inbound: V2rayConfig.InboundBean): JSONObject =
        JSONObject(Gson().toJson(inbound))

    @Test
    fun `gvisor produces noKernelTun true in config json`() {
        val json = serialize(buildTunInbound("GVISOR"))
        assertTrue("noKernelTun должен присутствовать в конфиге", json.has("settings"))
        assertTrue(
            "gVisor = noKernelTun true",
            json.getJSONObject("settings").getBoolean("noKernelTun")
        )
    }

    @Test
    fun `system produces noKernelTun false in config json`() {
        val json = serialize(buildTunInbound("SYSTEM"))
        assertFalse(
            "system = noKernelTun false",
            json.getJSONObject("settings").getBoolean("noKernelTun")
        )
    }

    @Test
    fun `noKernelTun is serialized next to mtu and keeps camelCase spelling`() {
        val json = serialize(buildTunInbound("GVISOR"))
        val settings = json.getJSONObject("settings")
        assertEquals(1500, settings.getInt("mtu"))
        // Ядро читает поле по точному имени — переименование сломает конфиг.
        assertTrue(
            "Поле должно называться ровно noKernelTun",
            settings.has("noKernelTun")
        )
    }

    @Test
    fun `null noKernelTun is omitted so existing behavior is preserved`() {
        val inbound = V2rayConfig.InboundBean(
            tag = "tun",
            port = null,
            protocol = "tun",
            settings = V2rayConfig.InboundBean.InSettingsBean(name = "xray0", mtu = 1500)
        )
        val json = serialize(inbound)
        assertFalse(
            "Поле не должно появляться, если стек не задан",
            json.getJSONObject("settings").has("noKernelTun")
        )
    }
}
