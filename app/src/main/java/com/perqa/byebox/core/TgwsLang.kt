package com.perqa.byebox.core

import android.content.Context
import com.amurcanov.tgwsproxy.TProxyLocale
import java.util.Locale

/**
 * Синхронизирует выбранный язык приложения с TGWS-сервисом: уведомление и плитка
 * быстрых настроек резолвят свои строки по этой локали, а не по системной.
 */
object TgwsLang {
    fun sync(context: Context) {
        val lang = context.getSharedPreferences("byebox_settings", Context.MODE_PRIVATE)
            .getString("pref_language", "system") ?: "system"
        TProxyLocale.overrideLocale = when (lang) {
            "ru" -> Locale.forLanguageTag("ru")
            "en" -> Locale.ENGLISH
            "zh" -> Locale.CHINA
            else -> null
        }
    }
}