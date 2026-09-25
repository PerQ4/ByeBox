package com.perqa.byebox

import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import com.tencent.mmkv.MMKV
import com.v2ray.ang.handler.SettingsManager

class ByeBoxApplication : MultiDexApplication(), Configuration.Provider {
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
        MMKV.initialize(this)
        SettingsManager.initApp(this)
        SettingsManager.initAssets(this, assets)

        // Disable HevSocks5Tunnel so that v2rayNG's core service establishes a native TUN inbound
        MMKV.mmkvWithID("SETTING", MMKV.MULTI_PROCESS_MODE)
            .encode(com.v2ray.ang.AppConfig.PREF_USE_HEV_TUNNEL, false)

        // Применяем выбранный язык к строковым ресурсам TGWS-сервиса
        // (уведомление и плитка быстрых настроек).
        com.perqa.byebox.core.TgwsLang.sync(this)
    }
}