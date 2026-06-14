package com.perqa.byebox

import androidx.multidex.MultiDexApplication
import com.tencent.mmkv.MMKV
import com.v2ray.ang.handler.SettingsManager

class ByeBoxApplication : MultiDexApplication() {
    companion object {
        lateinit var instance: ByeBoxApplication
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        MMKV.initialize(this)
        SettingsManager.initApp(this)
        SettingsManager.initAssets(this, assets)
        
        // Disable HevSocks5Tunnel so that v2rayNG's core service establishes a native TUN inbound
        MMKV.mmkvWithID("SETTING", MMKV.MULTI_PROCESS_MODE)
            .encode(com.v2ray.ang.AppConfig.PREF_USE_HEV_TUNNEL, false)
    }
}
