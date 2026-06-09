package com.perqa.byebox.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.util.Log
import com.perqa.byebox.data.ProxyConfig
import org.json.JSONObject

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
        const val PREFS_NAME = "byebox_settings"
        const val KEY_AUTOSTART_ENABLED = "autostart_enabled"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != "com.htc.intent.action.QUICKBOOT_POWERON"
        ) return

        Log.d(TAG, "Boot completed — checking autostart setting")

        val settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val autostartEnabled = settings.getBoolean(KEY_AUTOSTART_ENABLED, false)
        if (!autostartEnabled) {
            Log.d(TAG, "Autostart disabled — skipping")
            return
        }

        // Check that VPN permission was already granted (prepare returns null if granted)
        val vpnPrepareIntent = VpnService.prepare(context)
        if (vpnPrepareIntent != null) {
            Log.d(TAG, "VPN permission not granted — cannot autostart")
            return
        }

        // Load last connection settings
        val vpnPrefs = context.getSharedPreferences(HiddifyVpnService.PREFS_NAME, Context.MODE_PRIVATE)
        val configJson = vpnPrefs.getString(HiddifyVpnService.PREF_CONFIG_JSON, null) ?: run {
            Log.d(TAG, "No saved config — skipping autostart")
            return
        }
        val config = try {
            ProxyConfig.fromJson(JSONObject(configJson))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse saved config", e)
            return
        }

        val dnsAddress = vpnPrefs.getString(HiddifyVpnService.PREF_DNS_ADDRESS, "8.8.8.8") ?: "8.8.8.8"
        val routingProfile = vpnPrefs.getString(HiddifyVpnService.PREF_ROUTING_PROFILE, "BYPASS_LAN_CN_RU") ?: "BYPASS_LAN_CN_RU"
        val ipv6Enabled = vpnPrefs.getBoolean(HiddifyVpnService.PREF_IPV6_ENABLED, false)
        val lanBypass = vpnPrefs.getBoolean(HiddifyVpnService.PREF_LAN_BYPASS_ENABLED, true)
        val systemBypass = vpnPrefs.getBoolean(HiddifyVpnService.PREF_SYSTEM_BYPASS_ENABLED, false)
        val metered = vpnPrefs.getBoolean(HiddifyVpnService.PREF_METERED_NETWORK, false)
        val appRoutingMode = vpnPrefs.getString(HiddifyVpnService.PREF_APP_ROUTING_MODE, "OFF") ?: "OFF"
        val appRoutingPackages = vpnPrefs.getString(HiddifyVpnService.PREF_APP_ROUTING_PACKAGES, "") ?: ""

        Log.d(TAG, "Autostarting VPN: ${config.name}")

        val serviceIntent = Intent(context, HiddifyVpnService::class.java).apply {
            setAction(HiddifyVpnService.ACTION_CONNECT)
            putExtra(HiddifyVpnService.EXTRA_CONFIG_JSON, configJson)
            putExtra(HiddifyVpnService.EXTRA_DNS_ADDRESS, dnsAddress)
            putExtra(HiddifyVpnService.EXTRA_ROUTING_PROFILE, routingProfile)
            putExtra(HiddifyVpnService.EXTRA_IPV6_ENABLED, ipv6Enabled)
            putExtra(HiddifyVpnService.EXTRA_LAN_BYPASS_ENABLED, lanBypass)
            putExtra(HiddifyVpnService.EXTRA_SYSTEM_BYPASS_ENABLED, systemBypass)
            putExtra(HiddifyVpnService.EXTRA_METERED_NETWORK, metered)
            putExtra(HiddifyVpnService.EXTRA_APP_ROUTING_MODE, appRoutingMode)
            putExtra(HiddifyVpnService.EXTRA_APP_ROUTING_PACKAGES, appRoutingPackages)
            putExtra(HiddifyVpnService.EXTRA_TUN_STACK, vpnPrefs.getString(HiddifyVpnService.PREF_TUN_STACK, "mixed") ?: "mixed")
            putExtra(HiddifyVpnService.EXTRA_HTTP_PROXY_ENABLED, vpnPrefs.getBoolean(HiddifyVpnService.PREF_HTTP_PROXY_ENABLED, false))
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN service on boot", e)
        }
    }
}
