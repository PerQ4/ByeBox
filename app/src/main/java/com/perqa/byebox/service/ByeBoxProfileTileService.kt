package com.perqa.byebox.service

import android.content.Context
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.os.Build
import com.perqa.byebox.data.SettingsProfileData
import com.perqa.byebox.data.ProfilePresetManager
import com.perqa.byebox.data.PreferenceKeys
import com.perqa.byebox.ui.main.Loc
import com.perqa.byebox.R
import com.v2ray.ang.core.CoreServiceManager

class ByeBoxProfileTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val context = applicationContext
        val list = ProfilePresetManager.loadProfiles(context)
        if (list.isEmpty()) return
        
        val activeId = ProfilePresetManager.getActiveProfileId(context)
        val currentIndex = list.indexOfFirst { it.id == activeId }
        val nextIndex = if (currentIndex == -1 || currentIndex == list.lastIndex) 0 else currentIndex + 1
        val nextProfile = list[nextIndex]

        ProfilePresetManager.switchActiveProfile(context, nextProfile.id)
        updateTileState(nextProfile)

        // Restart VPN if it is currently running to apply new configurations instantly
        val active = isVpnServiceRunning(context)
        if (active) {
            com.v2ray.ang.util.MessageUtil.sendMsg2Service(context, com.v2ray.ang.AppConfig.MSG_STATE_RESTART, "")
        }
    }

    private fun updateTileState(profileOverride: SettingsProfileData? = null) {
        val tile = qsTile ?: return
        val context = applicationContext
        // Иконка приходит из манифеста (@drawable/ic_quick_switch), но уже добавленная
        // плитка хранит закэшированную иконку — возвращаем стрелки явно.
        tile.icon = Icon.createWithResource(this, R.drawable.ic_quick_switch)
        val list = ProfilePresetManager.loadProfiles(context)
        if (list.isEmpty()) return
        
        val activeId = ProfilePresetManager.getActiveProfileId(context)
        val profile = profileOverride ?: list.find { it.id == activeId } ?: list.first()

        val lang = currentLanguage()
        tile.state = Tile.STATE_INACTIVE
        tile.label = String.format(Loc.get("tile_profile_label", lang), profile.name)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val desc = String.format(Loc.get("tile_profile_dns", lang), profile.dnsServer)
            tile.subtitle = desc.take(20) + if (desc.length > 20) "…" else ""
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.stateDescription = String.format(Loc.get("tile_profile_state_desc", lang), profile.name)
        }

        tile.updateTile()
    }

    private fun isVpnServiceRunning(context: Context): Boolean {
        return com.v2ray.ang.handler.MmkvManager.decodeSettingsBool(com.v2ray.ang.AppConfig.PREF_TILE_VPN_RUNNING, false)
    }

    /** Язык интерфейса из настроек приложения (для текстов плитки). */
    private fun currentLanguage(): String {
        return getSharedPreferences(PreferenceKeys.BYEBOX_SETTINGS, MODE_PRIVATE)
            .getString(PreferenceKeys.LANGUAGE, "system") ?: "system"
    }
}
