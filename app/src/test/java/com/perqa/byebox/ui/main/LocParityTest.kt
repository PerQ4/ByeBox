package com.perqa.byebox.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Паритет ключей локализации.
 *
 * Словари в [Loc] объявлены как `private val`, поэтому достаём их через
 * рефлексию — прод-код при этом не меняется. Тест требует, чтобы наборы
 * ключей ru/en/zh совпадали: ключ, добавленный только в один словарь, ловится
 * сразу (для него Loc.get() отдаст не перевод, а fallback/сам ключ).
 */
class LocParityTest {

    private fun dict(field: String): Map<String, String> {
        val f = Loc::class.java.getDeclaredField(field)
        f.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return f.get(Loc) as Map<String, String>
    }

    @Test
    fun `all language dictionaries share the same key set`() {
        val ru = dict("ru")
        val en = dict("en")
        val zh = dict("zh")

        assertTrue("ru dictionary should not be empty", ru.isNotEmpty())
        assertEquals("en misses ru keys: ${ru.keys - en.keys}", emptySet<String>(), ru.keys - en.keys)
        assertEquals("en adds keys absent from ru: ${en.keys - ru.keys}", emptySet<String>(), en.keys - ru.keys)
        assertEquals("zh misses ru keys: ${ru.keys - zh.keys}", emptySet<String>(), ru.keys - zh.keys)
        assertEquals("zh adds keys absent from ru: ${zh.keys - ru.keys}", emptySet<String>(), zh.keys - ru.keys)
    }

    @Test
    fun `known keys resolve per language and unknown keys do not throw`() {
        val ru = dict("ru")
        val en = dict("en")
        val key = ru.keys.first()
        assertEquals(ru[key], Loc.get(key, "ru"))
        assertEquals(en[key], Loc.get(key, "en"))
        // Неизвестный ключ не должен приводить к исключению или пустой строке.
        assertTrue(Loc.get("definitely-not-a-key", "en").isNotEmpty())
    }
}
