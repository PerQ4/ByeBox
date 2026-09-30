package com.perqa.byebox.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Регрессия на баг 1.5.4: выбор «TUN Стек» не доходил до конфига ядра.
 *
 * До 1.5.4 настройка писалась в PREF_USE_HEV_TUNNEL (переключатель внешнего
 * туннеля HevSocks5Tunnel) и тут же перетиралась при старте приложения, а поле
 * TUN-инбаунда в реальном конфиге отсутствовало — выбор просто ничего не делал.
 */
class TunStackMappingTest {

    @Test
    fun `explicit names map to their core values case-insensitively`() {
        assertEquals(TunStackMapping.SYSTEM, TunStackMapping.toXrayValue("SYSTEM"))
        assertEquals(TunStackMapping.SYSTEM, TunStackMapping.toXrayValue("system"))
        assertEquals(TunStackMapping.SYSTEM, TunStackMapping.toXrayValue("System"))
        assertEquals(TunStackMapping.GVISOR, TunStackMapping.toXrayValue("GVISOR"))
        assertEquals(TunStackMapping.GVISOR, TunStackMapping.toXrayValue("gvisor"))
    }

    @Test
    fun `unknown values fall back to system, not gvisor`() {
        // Ключевое свойство: неизвестное значение не должно молча переводить
        // пользователя на userspace-стек. До 1.5.4 ядро работало на системном
        // стеке, и дефолт обязан сохранять это поведение.
        assertEquals(TunStackMapping.SYSTEM, TunStackMapping.toXrayValue("INHERIT"))
        assertEquals(TunStackMapping.SYSTEM, TunStackMapping.toXrayValue(""))
        assertEquals(TunStackMapping.SYSTEM, TunStackMapping.toXrayValue("ЧТО-ТО"))
        // Отсутствие прежней настройки при миграции — тоже system.
        assertEquals(TunStackMapping.SYSTEM, TunStackMapping.toXrayValue("".orEmpty()))
    }

    @Test
    fun `noKernelTun is inverted relative to stack name`() {
        // Ядро Xray трактует поле буквально: true = не использовать ядро, то есть gVisor.
        assertTrue(TunStackMapping.toNoKernelTun(TunStackMapping.GVISOR))
        assertFalse(TunStackMapping.toNoKernelTun(TunStackMapping.SYSTEM))
    }

    @Test
    fun `unknown stack name yields kernel tun`() {
        // Неизвестный стек не должен приводить к noKernelTun = true.
        assertFalse(TunStackMapping.toNoKernelTun(TunStackMapping.toXrayValue("INHERIT")))
    }

    @Test
    fun `round-trip through both steps is stable`() {
        for (name in listOf("GVISOR", "SYSTEM")) {
            val value = TunStackMapping.toXrayValue(name)
            assertEquals(value, TunStackMapping.toXrayValue(value))
        }
    }

    @Test
    fun `only gvisor is preserved, everything else collapses to system`() {
        val values = listOf("GVISOR", "gvisor", "SYSTEM", "INHERIT", "", "x")
            .map(TunStackMapping::toXrayValue)
            .toSet()
        assertEquals(setOf(TunStackMapping.GVISOR, TunStackMapping.SYSTEM), values)
    }

    @Test
    fun `normalizeCoreValue keeps gvisor and nulls out to system`() {
        assertEquals(TunStackMapping.GVISOR, TunStackMapping.normalizeCoreValue("gvisor"))
        assertEquals(TunStackMapping.GVISOR, TunStackMapping.normalizeCoreValue("GVISOR"))
        assertEquals(TunStackMapping.SYSTEM, TunStackMapping.normalizeCoreValue("system"))
        assertEquals(TunStackMapping.SYSTEM, TunStackMapping.normalizeCoreValue("SYSTEM"))
        // Отсутствующее значение после миграции/правки — тоже system.
        assertEquals(TunStackMapping.SYSTEM, TunStackMapping.normalizeCoreValue(null))
        assertEquals(TunStackMapping.SYSTEM, TunStackMapping.normalizeCoreValue(""))
        assertEquals(TunStackMapping.SYSTEM, TunStackMapping.normalizeCoreValue("мусор"))
    }

    @Test
    fun `normalizeCoreValue is idempotent`() {
        for (raw in listOf("gvisor", "system", "GVISOR", "", "x", "null")) {
            val once = TunStackMapping.normalizeCoreValue(raw)
            assertEquals(once, TunStackMapping.normalizeCoreValue(once))
        }
    }

    @Test
    fun `noKernelTun agrees with normalizeCoreValue`() {
        // SettingsManager.getTunStack() нормализует, CoreConfigManager ставит поле
        // по noKernelTun — эти два пути обязаны сходиться.
        for (raw in listOf("gvisor", "system", "", "x", null)) {
            val normalized = TunStackMapping.normalizeCoreValue(raw)
            assertEquals(
                normalized == TunStackMapping.GVISOR,
                TunStackMapping.toNoKernelTun(normalized)
            )
        }
    }
}
