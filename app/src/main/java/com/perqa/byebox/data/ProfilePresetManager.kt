package com.perqa.byebox.data

import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@Serializable
data class SettingsProfileData(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val assignedConfigId: String? = null,
    val routingProfile: String = "INHERIT",
    val dnsServer: String = "INHERIT",
    val customDnsServer: String? = null,
    val appRoutingMode: String = "INHERIT",
    val tunStack: String = "INHERIT",
    val fakeDnsEnabled: Boolean? = null,
    val fragmentEnabled: Boolean? = null,
    val muxEnabled: Boolean? = null,
    val sniffingEnabled: Boolean? = null,
    val customDirectRules: String? = null,
    val customProxyRules: String? = null,
    val appRoutingPackages: Set<String>? = null
) {
    fun toJson(): JSONObject {
        return JSONObject(profileJson.encodeToString(this))
    }

    companion object {
        private val profileJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            coerceInputValues = true
        }

        fun fromJson(json: JSONObject): SettingsProfileData {
            return profileJson.decodeFromString(json.toString())
        }
    }
}

object ProfilePresetManager {
    fun loadProfiles(context: Context): List<SettingsProfileData> {
        val prefs = context.getSharedPreferences("byebox_settings", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("pref_dynamic_profiles", null)

        val baseProfile = loadBaseProfile(context)

        val customProfiles = if (jsonStr.isNullOrBlank()) {
            val defaults = createDefaultProfiles()
            saveProfiles(context, defaults)
            defaults
        } else {
            try {
                val arr = JSONArray(jsonStr)
                val list = mutableListOf<SettingsProfileData>()
                for (i in 0 until arr.length()) {
                    val p = SettingsProfileData.fromJson(arr.getJSONObject(i))
                    if (p.id != "base") {
                        list.add(p)
                    }
                }
                list
            } catch (_: Exception) {
                createDefaultProfiles()
            }
        }
        return listOf(baseProfile) + customProfiles
    }

    fun saveProfiles(context: Context, list: List<SettingsProfileData>) {
        val prefs = context.getSharedPreferences("byebox_settings", Context.MODE_PRIVATE)
        list.find { it.id == "base" }?.let { saveBaseProfile(context, it) }
        val arr = JSONArray()
        list.filter { it.id != "base" }.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("pref_dynamic_profiles", arr.toString()).apply()
    }

    fun loadBaseProfile(context: Context): SettingsProfileData {
        val prefs = context.getSharedPreferences("byebox_settings", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("pref_base_profile_data", null)
        if (!jsonStr.isNullOrBlank()) {
            try {
                return SettingsProfileData.fromJson(JSONObject(jsonStr))
            } catch (_: Exception) {}
        }
        return SettingsProfileData(
            id = "base",
            name = "По умолчанию",
            assignedConfigId = "LAST_ACTIVE",
            routingProfile = "INHERIT",
            dnsServer = "INHERIT",
            appRoutingMode = "INHERIT",
            tunStack = "INHERIT"
        )
    }

    fun saveBaseProfile(context: Context, profile: SettingsProfileData) {
        val prefs = context.getSharedPreferences("byebox_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("pref_base_profile_data", profile.toJson().toString()).apply()
    }

    fun switchActiveProfile(context: Context, id: String) {
        val prefs = context.getSharedPreferences("byebox_settings", Context.MODE_PRIVATE)
        val previousId = prefs.getString("pref_active_profile_id", "base") ?: "base"
        val currentServer = MmkvManager.getSelectServer()
        if (!currentServer.isNullOrBlank()) {
            prefs.edit().putString("last_selected_server_profile_$previousId", currentServer).apply()
        }
        prefs.edit().putString("pref_active_profile_id", id).apply()
        val list = loadProfiles(context)
        val profile = list.find { it.id == id } ?: return
        applyProfile(context, profile)
    }

    fun getActiveProfileId(context: Context): String {
        val prefs = context.getSharedPreferences("byebox_settings", Context.MODE_PRIVATE)
        val list = loadProfiles(context)
        val firstId = list.firstOrNull()?.id ?: "base"
        return prefs.getString("pref_active_profile_id", firstId) ?: firstId
    }

    fun setActiveProfileId(context: Context, id: String) {
        val prefs = context.getSharedPreferences("byebox_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("pref_active_profile_id", id).apply()
    }

    private fun createDefaultProfiles(): List<SettingsProfileData> {
        return listOf(
            SettingsProfileData(
                id = "preset-work",
                name = "Работа",
                routingProfile = "BYPASS_LAN_CN_RU",
                dnsServer = "CLOUDFLARE",
                appRoutingMode = "ONLY_SELECTED"
            ),
            SettingsProfileData(
                id = "preset-streaming",
                name = "Стриминг",
                routingProfile = "PROXY_ALL",
                dnsServer = "GOOGLE",
                muxEnabled = true
            ),
            SettingsProfileData(
                id = "preset-security",
                name = "Безопасность",
                routingProfile = "PROXY_ALL",
                dnsServer = "ADGUARD",
                tunStack = "GVISOR",
                fragmentEnabled = true
            ),
            SettingsProfileData(
                id = "preset-regional",
                name = "Обход блокировок",
                routingProfile = "BYPASS_LAN_CN_RU",
                dnsServer = "SYSTEM",
                appRoutingMode = "BYPASS_SELECTED",
                tunStack = "GVISOR",
                fragmentEnabled = true
            )
        )
    }

    fun applyProfile(context: Context, profile: SettingsProfileData) {
        try {
            val prefs = context.getSharedPreferences("byebox_settings", Context.MODE_PRIVATE)
            applyRoutingSettings(context, prefs, profile)
            applyDnsSettings(context, prefs, profile)
            applyAppRoutingSettings(context, prefs, profile)
            applyTunStackSettings(prefs, profile)
            applyFakeDnsSettings(prefs, profile)
            applyFragmentSettings(prefs, profile)
            applyMuxSettings(prefs, profile)
            applySniffingSettings(prefs, profile)
            applyCustomRulesSettings(prefs, profile)
            applyServerAssignment(prefs, profile)
        } catch (e: Exception) {
            com.v2ray.ang.util.LogUtil.e(AppConfig.TAG, "Failed to apply profile preset: ${e.message}", e)
        }
    }

    private fun resolveBase(prefs: android.content.SharedPreferences, key: String, default: String): String {
        return prefs.getString(key, default) ?: default
    }

    private fun resolveBaseBool(prefs: android.content.SharedPreferences, key: String, default: Boolean): Boolean {
        return prefs.getBoolean(key, default)
    }

    private fun applyRoutingSettings(context: Context, prefs: android.content.SharedPreferences, profile: SettingsProfileData) {
        val effective = if (profile.id == "base" || profile.routingProfile == "INHERIT") {
            resolveBase(prefs, "base_routing_profile", "BYPASS_LAN_CN_RU")
        } else {
            profile.routingProfile
        }
        val index = when (effective) {
            "BYPASS_LAN_CN_RU" -> 4
            "PROXY_ALL" -> 2
            else -> 0
        }
        prefs.edit().putString("routing_profile", effective).apply()
        SettingsManager.resetRoutingRulesetsFromPresets(context, index)
    }

    private fun applyDnsSettings(context: Context, prefs: android.content.SharedPreferences, profile: SettingsProfileData) {
        val effectiveServer = if (profile.id == "base" || profile.dnsServer == "INHERIT") {
            resolveBase(prefs, "base_dns_server", "SYSTEM")
        } else {
            profile.dnsServer
        }
        val effectiveCustom = if (profile.id == "base" || profile.customDnsServer == null) {
            resolveBase(prefs, "base_custom_dns", "")
        } else {
            profile.customDnsServer
        }
        val dns = when (effectiveServer) {
            "CLOUDFLARE" -> "1.1.1.1"
            "GOOGLE" -> "8.8.8.8"
            "ADGUARD" -> "94.140.14.14"
            "CUSTOM" -> effectiveCustom.ifBlank { "1.1.1.1" }
            else -> "1.1.1.1"
        }
        prefs.edit().putString("dns_server", effectiveServer).apply()
        MmkvManager.encodeSettings(AppConfig.PREF_REMOTE_DNS, dns)
        MmkvManager.encodeSettings(AppConfig.PREF_VPN_DNS, if (effectiveServer != "SYSTEM") dns else AppConfig.DNS_VPN)
    }

    private fun applyAppRoutingSettings(context: Context, prefs: android.content.SharedPreferences, profile: SettingsProfileData) {
        val effectiveMode = if (profile.id == "base" || profile.appRoutingMode == "INHERIT") {
            resolveBase(prefs, "base_app_routing_mode", "OFF")
        } else {
            profile.appRoutingMode
        }
        val effectivePackages = if (profile.id == "base" || profile.appRoutingPackages == null) {
            prefs.getStringSet("base_app_routing_packages", emptySet()) ?: emptySet()
        } else {
            profile.appRoutingPackages
        }
        prefs.edit().putString("app_routing_mode", effectiveMode).apply()
        if (effectiveMode == "OFF") {
            MmkvManager.encodeSettings(AppConfig.PREF_PER_APP_PROXY, false)
        } else {
            MmkvManager.encodeSettings(AppConfig.PREF_PER_APP_PROXY, true)
            MmkvManager.encodeSettings(AppConfig.PREF_BYPASS_APPS, effectiveMode == "BYPASS_SELECTED")
            MmkvManager.encodeSettings(AppConfig.PREF_PER_APP_PROXY_SET, effectivePackages.toMutableSet())
        }
    }

    private fun applyTunStackSettings(prefs: android.content.SharedPreferences, profile: SettingsProfileData) {
        val effective = if (profile.id == "base" || profile.tunStack == "INHERIT") {
            resolveBase(prefs, "base_tun_stack", "SYSTEM")
        } else {
            profile.tunStack
        }
        prefs.edit().putString("tun_stack", effective).apply()
        MmkvManager.encodeSettings(AppConfig.PREF_USE_HEV_TUNNEL, effective == "GVISOR")
    }

    private fun applyFakeDnsSettings(prefs: android.content.SharedPreferences, profile: SettingsProfileData) {
        val effective = if (profile.id == "base" || profile.fakeDnsEnabled == null) {
            resolveBaseBool(prefs, "base_fake_dns_enabled", true)
        } else {
            profile.fakeDnsEnabled
        }
        MmkvManager.encodeSettings(AppConfig.PREF_FAKE_DNS_ENABLED, effective)
    }

    private fun applyFragmentSettings(prefs: android.content.SharedPreferences, profile: SettingsProfileData) {
        val effective = if (profile.id == "base" || profile.fragmentEnabled == null) {
            resolveBaseBool(prefs, "base_fragment_enabled", false)
        } else {
            profile.fragmentEnabled
        }
        MmkvManager.encodeSettings(AppConfig.PREF_FRAGMENT_ENABLED, effective)
    }

    private fun applyMuxSettings(prefs: android.content.SharedPreferences, profile: SettingsProfileData) {
        val effective = if (profile.id == "base" || profile.muxEnabled == null) {
            resolveBaseBool(prefs, "base_mux_enabled", false)
        } else {
            profile.muxEnabled
        }
        MmkvManager.encodeSettings(AppConfig.PREF_MUX_ENABLED, effective)
    }

    private fun applySniffingSettings(prefs: android.content.SharedPreferences, profile: SettingsProfileData) {
        val effective = if (profile.id == "base" || profile.sniffingEnabled == null) {
            resolveBaseBool(prefs, "base_sniffing_enabled", true)
        } else {
            profile.sniffingEnabled
        }
        MmkvManager.encodeSettings(AppConfig.PREF_SNIFFING_ENABLED, effective)
    }

    private fun applyCustomRulesSettings(prefs: android.content.SharedPreferences, profile: SettingsProfileData) {
        val effectiveDirect = if (profile.id == "base" || profile.customDirectRules == null) {
            resolveBase(prefs, "base_custom_direct_rules", "")
        } else {
            profile.customDirectRules
        }
        val effectiveProxy = if (profile.id == "base" || profile.customProxyRules == null) {
            resolveBase(prefs, "base_custom_proxy_rules", "")
        } else {
            profile.customProxyRules
        }
        MmkvManager.encodeSettings("pref_custom_direct_rules", effectiveDirect)
        MmkvManager.encodeSettings("pref_custom_proxy_rules", effectiveProxy)
    }

    private fun applyServerAssignment(prefs: android.content.SharedPreferences, profile: SettingsProfileData) {
        if (profile.assignedConfigId == "LAST_ACTIVE") {
            val savedServerId = prefs.getString("last_selected_server_profile_${profile.id}", null)
            if (!savedServerId.isNullOrBlank()) {
                MmkvManager.setSelectServer(savedServerId)
            }
        } else if (profile.assignedConfigId != null) {
            MmkvManager.setSelectServer(profile.assignedConfigId)
        }
    }
}
