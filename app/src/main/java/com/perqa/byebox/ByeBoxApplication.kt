package com.perqa.byebox

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import com.perqa.byebox.data.PreferenceKeys
import com.perqa.byebox.data.TunStackMapping
import com.tencent.mmkv.MMKV
import com.v2ray.ang.AppConfig
import com.v2ray.ang.enums.NotificationChannelType
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.util.LogUtil

class ByeBoxApplication : Application(), Configuration.Provider {
    companion object {
        lateinit var instance: ByeBoxApplication
    }

    /**
     * WorkManagerInitializer is removed from the manifest (multiprocess
     * WorkManager setup, see AndroidManifest.xml), so the app must provide
     * its own configuration. Without this, any WorkManager / RemoteWorkManager
     * call in the main process throws IllegalStateException at startup.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Каналы уведомлений берут имена из строковых ресурсов; Application
        // отдаёт контекст для их резолва (см. NotificationChannelType).
        NotificationChannelType.init(this)
        MMKV.initialize(this)
        SettingsManager.initApp(this)
        SettingsManager.initAssets(this, assets)

        // ByeBox всегда использует нативный TUN-инбаунд ядра Xray, поэтому
        // внешний туннель HevSocks5Tunnel принудительно выключен.
        // Это НЕ выбор сетевого стека: gVisor/system задаётся отдельно,
        // ключом PREF_TUN_STACK, и выбирается пользователем в настройках.
        // Пишем через MmkvManager, а не напрямую в MMKV: хранилище SETTING
        // зашифровано, и прямое открытие без ключа сломало бы чтение.
        MmkvManager.encodeSettings(AppConfig.PREF_USE_HEV_TUNNEL, false)

        migrateTunStackOnce()

        // Применяем выбранный язык к строковым ресурсам TGWS-сервиса
        // (уведомление и плитка быстрых настроек).
        com.perqa.byebox.core.TgwsLang.sync(this)
    }

    /**
     * Перенос выбора стека TUN в MMKV для установок, обновляемых с 1.5.3 и старше.
     * До 1.5.4 настройка писалась в SharedPreferences ("base_tun_stack") и в
     * PREF_USE_HEV_TUNNEL, из-за чего выбор не доходил до конфига ядра вовсе.
     *
     * Значение по умолчанию — "system": именно так фактически работало ядро
     * до этого релиза (поле noKernelTun не выставлялось вовсе).
     */
    private fun migrateTunStackOnce() {
        val key = AppConfig.PREF_TUN_STACK
        if (MmkvManager.decodeSettingsString(key).isNullOrBlank()) {
            val legacy = getSharedPreferences(
                PreferenceKeys.BYEBOX_SETTINGS,
                Context.MODE_PRIVATE
            ).getString(PreferenceKeys.BASE_TUN_STACK, null)
            // Явный выбор пользователя уважаем; если выбора не было (null),
            // toXrayValue вернёт system — фактическое поведение до 1.5.4.
            val value = TunStackMapping.toXrayValue(legacy.orEmpty())
            MmkvManager.encodeSettings(key, value)
            LogUtil.i(AppConfig.TAG, "Migrated TUN stack setting to MMKV: $value")
        }
    }
}
