package com.perqa.byebox.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.net.VpnService
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.os.Build
import androidx.core.content.ContextCompat
import com.perqa.byebox.MainActivity
import com.perqa.byebox.R
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.util.Utils

class ByeBoxTileService : TileService() {
    private var mMsgReceive: BroadcastReceiver? = null
    private var currentState = 0

    private val mMsgReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.getIntExtra("key", 0)) {
                AppConfig.MSG_STATE_RUNNING,
                AppConfig.MSG_STATE_START_SUCCESS -> updateTileState(active = true)
                AppConfig.MSG_STATE_NOT_RUNNING,
                AppConfig.MSG_STATE_STOP_SUCCESS,
                AppConfig.MSG_STATE_START_FAILURE -> updateTileState(active = false)
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        val running = MmkvManager.decodeSettingsBool(AppConfig.PREF_TILE_VPN_RUNNING, false)
        updateTileState(active = running)

        val mFilter = IntentFilter(AppConfig.BROADCAST_ACTION_ACTIVITY)
        ContextCompat.registerReceiver(
            applicationContext,
            mMsgReceiver,
            mFilter,
            Utils.receiverFlags()
        )
        mMsgReceive = mMsgReceiver
    }

    override fun onStopListening() {
        super.onStopListening()
        try {
            mMsgReceive?.let { applicationContext.unregisterReceiver(it) }
        } catch (_: Exception) {
        }
        mMsgReceive = null
    }

    override fun onClick() {
        super.onClick()
        val context = applicationContext
        val tile = qsTile ?: return
        if (currentState == 1) return

        when (tile.state) {
            Tile.STATE_ACTIVE -> {
                CoreServiceManager.stopVService(context)
                updateTileState(active = false)
            }
            Tile.STATE_INACTIVE -> {
                if (VpnService.prepare(context) != null) {
                    openMainAndCollapse()
                    return
                }
                val started = CoreServiceManager.startVServiceFromToggle(context)
                if (started) {
                    updateTileState(connecting = true)
                } else {
                    updateTileState(active = false)
                }
            }
        }
    }

    fun updateTileState(active: Boolean = CoreServiceManager.isRunning(), connecting: Boolean = false) {
        val tile = qsTile ?: return
        currentState = when {
            connecting -> 1
            active -> 2
            else -> 0
        }

        tile.state = when {
            connecting -> Tile.STATE_UNAVAILABLE
            active -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.label = "ByeBox"

        tile.icon = if (active) {
            Icon.createWithResource(this, R.drawable.ic_notification_on)
        } else {
            Icon.createWithResource(this, R.drawable.ic_notification_off)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                connecting -> "Подключение…"
                active -> {
                    val guid = MmkvManager.getSelectServer()
                    val config = guid?.let { MmkvManager.decodeServerConfig(it) }
                    config?.remarks.orEmpty().ifBlank { "Подключено" }
                }
                else -> "Отключено"
            }
            tile.stateDescription = when {
                connecting -> "VPN подключается"
                active -> "VPN активен"
                else -> "VPN выключен"
            }
        }
        tile.updateTile()
    }

    private fun openMainAndCollapse() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } ?: Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivityAndCollapse(launchIntent)
    }
}
