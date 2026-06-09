package com.perqa.byebox.service

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.VpnService
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.os.Build

class ByeBoxTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val context = applicationContext
        val isServiceRunning = HiddifyVpnService.isRunning

        if (isServiceRunning) {
            val intent = Intent(context, HiddifyVpnService::class.java).apply {
                action = HiddifyVpnService.ACTION_DISCONNECT
            }
            startServiceCompat(context, intent)
            updateTileState(active = false)
        } else {
            if (VpnService.prepare(context) != null) {
                openMainAndCollapse()
                updateTileState(active = false)
                return
            }
            val prefs = context.getSharedPreferences(HiddifyVpnService.PREFS_NAME, Context.MODE_PRIVATE)
            val configJson = prefs.getString(HiddifyVpnService.PREF_CONFIG_JSON, null)
            if (configJson.isNullOrBlank() || configJson == "{}") {
                openMainAndCollapse()
                updateTileState(active = false)
                return
            }
            val dnsAddr = prefs.getString(HiddifyVpnService.PREF_DNS_ADDRESS, "8.8.8.8") ?: "8.8.8.8"
            val routingProfile = prefs.getString(HiddifyVpnService.PREF_ROUTING_PROFILE, "BYPASS_LAN_CN_RU") ?: "BYPASS_LAN_CN_RU"
            val ipv6 = prefs.getBoolean(HiddifyVpnService.PREF_IPV6_ENABLED, false)
            val lanBypass = prefs.getBoolean(HiddifyVpnService.PREF_LAN_BYPASS_ENABLED, true)
            val systemBypass = prefs.getBoolean(HiddifyVpnService.PREF_SYSTEM_BYPASS_ENABLED, false)
            val metered = prefs.getBoolean(HiddifyVpnService.PREF_METERED_NETWORK, false)
            val appRoutingMode = prefs.getString(HiddifyVpnService.PREF_APP_ROUTING_MODE, "OFF") ?: "OFF"
            val appRoutingPackages = prefs.getString(HiddifyVpnService.PREF_APP_ROUTING_PACKAGES, "") ?: ""

            val intent = Intent(context, HiddifyVpnService::class.java).apply {
                action = HiddifyVpnService.ACTION_CONNECT
                putExtra(HiddifyVpnService.EXTRA_CONFIG_JSON, configJson)
                putExtra(HiddifyVpnService.EXTRA_DNS_ADDRESS, dnsAddr)
                putExtra(HiddifyVpnService.EXTRA_ROUTING_PROFILE, routingProfile)
                putExtra(HiddifyVpnService.EXTRA_IPV6_ENABLED, ipv6)
                putExtra(HiddifyVpnService.EXTRA_LAN_BYPASS_ENABLED, lanBypass)
                putExtra(HiddifyVpnService.EXTRA_SYSTEM_BYPASS_ENABLED, systemBypass)
                putExtra(HiddifyVpnService.EXTRA_METERED_NETWORK, metered)
                putExtra(HiddifyVpnService.EXTRA_APP_ROUTING_MODE, appRoutingMode)
                putExtra(HiddifyVpnService.EXTRA_APP_ROUTING_PACKAGES, appRoutingPackages)
                putExtra(HiddifyVpnService.EXTRA_TUN_STACK, prefs.getString(HiddifyVpnService.PREF_TUN_STACK, "mixed") ?: "mixed")
                putExtra(HiddifyVpnService.EXTRA_HTTP_PROXY_ENABLED, prefs.getBoolean(HiddifyVpnService.PREF_HTTP_PROXY_ENABLED, false))
            }
            startServiceCompat(context, intent)
            updateTileState(active = true)
        }
    }

    private fun updateTileState(active: Boolean = HiddifyVpnService.isRunning) {
        val tile = qsTile ?: return
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "ByeBox VPN"
        tile.icon = Icon.createWithResource(this, com.perqa.byebox.R.drawable.ic_notification)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (active) "Подключено" else "Отключено"
        }

        tile.updateTile()
    }

    private fun startServiceCompat(context: Context, intent: Intent) {
        if (intent.action == HiddifyVpnService.ACTION_DISCONNECT) {
            context.startService(intent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun openMainAndCollapse() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } ?: Intent(this, com.perqa.byebox.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivityAndCollapse(launchIntent)
    }
}
