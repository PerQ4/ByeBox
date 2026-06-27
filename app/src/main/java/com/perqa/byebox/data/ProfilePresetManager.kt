package com.perqa.byebox.data

import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

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
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("assignedConfigId", assignedConfigId ?: "")
            put("routingProfile", routingProfile)
            put("dnsServer", dnsServer)
            if (customDnsServer != null) put("customDnsServer", customDnsServer)
            put("appRoutingMode", appRoutingMode)
            put("tunStack", tunStack)
            if (fakeDnsEnabled != null) put("fakeDnsEnabled", fakeDnsEnabled)
            if (fragmentEnabled != null) put("fragmentEnabled", fragmentEnabled)
            if (muxEnabled != null) put("muxEnabled", muxEnabled)
            if (sniffingEnabled != null) put("sniffingEnabled", sniffingEnabled)
            if (customDirectRules != null) put("customDirectRules", customDirectRules)
            if (customProxyRules != null) put("customProxyRules", customProxyRules)
            if (appRoutingPackages != null) {
                val pkgsArray = JSONArray()
                appRoutingPackages.forEach { pkgsArray.put(it) }
                put("appRoutingPackages", pkgsArray)
            }
        }
    }

    companion object {
        fun fromJson(json: JSONObject): SettingsProfileData {
            val packagesSet = if (json.has("appRoutingPackages")) {
                val s = mutableSetOf<String>()
                val arr = json.optJSONArray("appRoutingPackages")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        s.add(arr.optString(i))
                    }
                }
                s
            } else {
                null
            }
            return SettingsProfileData(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", "Профиль"),
                assignedConfigId = json.optString("assignedConfigId", "").takeIf { it.isNotEmpty() },
                routingProfile = json.optString("routingProfile", "INHERIT"),
                dnsServer = json.optString("dnsServer", "INHERIT"),
                customDnsServer = if (json.has("customDnsServer")) json.optString("customDnsServer") else null,
                appRoutingMode = json.optString("appRoutingMode", "INHERIT"),
                tunStack = json.optString("tunStack", "INHERIT"),
                fakeDnsEnabled = if (json.has("fakeDnsEnabled")) json.optBoolean("fakeDnsEnabled") else null,
                fragmentEnabled = if (json.has("fragmentEnabled")) json.optBoolean("fragmentEnabled") else null,
                muxEnabled = if (json.has("muxEnabled")) json.optBoolean("muxEnabled") else null,
                sniffingEnabled = if (json.has("sniffingEnabled")) json.optBoolean("sniffingEnabled") else null,
                customDirectRules = if (json.has("customDirectRules")) json.optString("customDirectRules") else null,
                customProxyRules = if (json.has("customProxyRules")) json.optString("customProxyRules") else null,
                appRoutingPackages = packagesSet
            )
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
            
            // 1. Routing profile
            val effectiveRoutingProfile = if (profile.id == "base" || profile.routingProfile == "INHERIT") {
                prefs.getString("base_routing_profile", "BYPASS_LAN_CN_RU") ?: "BYPASS_LAN_CN_RU"
            } else {
                profile.routingProfile
            }
            val routingIndex = when (effectiveRoutingProfile) {
                "BYPASS_LAN_CN_RU" -> 4 // BYPASS_LAN_CN_RU (WHITE_RUSSIA)
                "PROXY_ALL" -> 2 // PROXY_ALL (GLOBAL)
                else -> 0 // DIRECT
            }
            prefs.edit().putString("routing_profile", effectiveRoutingProfile).apply()
            SettingsManager.resetRoutingRulesetsFromPresets(context, routingIndex)

            // 2. DNS
            val effectiveDnsServer = if (profile.id == "base" || profile.dnsServer == "INHERIT") {
                prefs.getString("base_dns_server", "SYSTEM") ?: "SYSTEM"
            } else {
                profile.dnsServer
            }
            val effectiveCustomDnsServer = if (profile.id == "base" || profile.customDnsServer == null) {
                prefs.getString("base_custom_dns", "") ?: ""
            } else {
                profile.customDnsServer
            }
            val dns = when (effectiveDnsServer) {
                "CLOUDFLARE" -> "1.1.1.1"
                "GOOGLE" -> "8.8.8.8"
                "ADGUARD" -> "94.140.14.14"
                "CUSTOM" -> effectiveCustomDnsServer.ifBlank { "1.1.1.1" }
                else -> "1.1.1.1" // SYSTEM
            }
            prefs.edit().putString("dns_server", effectiveDnsServer).apply()
            MmkvManager.encodeSettings(AppConfig.PREF_REMOTE_DNS, dns)
            if (effectiveDnsServer != "SYSTEM") {
                MmkvManager.encodeSettings(AppConfig.PREF_VPN_DNS, dns)
            } else {
                MmkvManager.encodeSettings(AppConfig.PREF_VPN_DNS, AppConfig.DNS_VPN)
            }

            // 3. App routing mode
            val effectiveAppRoutingMode = if (profile.id == "base" || profile.appRoutingMode == "INHERIT") {
                prefs.getString("base_app_routing_mode", "OFF") ?: "OFF"
            } else {
                profile.appRoutingMode
            }
            val effectiveAppRoutingPackages = if (profile.id == "base" || profile.appRoutingPackages == null) {
                prefs.getStringSet("base_app_routing_packages", emptySet()) ?: emptySet()
            } else {
                profile.appRoutingPackages
            }
            prefs.edit().putString("app_routing_mode", effectiveAppRoutingMode).apply()
            if (effectiveAppRoutingMode == "OFF") {
                MmkvManager.encodeSettings(AppConfig.PREF_PER_APP_PROXY, false)
            } else {
                MmkvManager.encodeSettings(AppConfig.PREF_PER_APP_PROXY, true)
                MmkvManager.encodeSettings(AppConfig.PREF_BYPASS_APPS, effectiveAppRoutingMode == "BYPASS_SELECTED")
                MmkvManager.encodeSettings(AppConfig.PREF_PER_APP_PROXY_SET, effectiveAppRoutingPackages.toMutableSet())
            }

            // 4. TUN stack
            val effectiveTunStack = if (profile.id == "base" || profile.tunStack == "INHERIT") {
                prefs.getString("base_tun_stack", "SYSTEM") ?: "SYSTEM"
            } else {
                profile.tunStack
            }
            prefs.edit().putString("tun_stack", effectiveTunStack).apply()
            MmkvManager.encodeSettings(AppConfig.PREF_USE_HEV_TUNNEL, effectiveTunStack == "GVISOR")

            // 5. Fake DNS
            val effectiveFakeDns = if (profile.id == "base" || profile.fakeDnsEnabled == null) {
                prefs.getBoolean("base_fake_dns_enabled", true)
            } else {
                profile.fakeDnsEnabled
            }
            MmkvManager.encodeSettings(AppConfig.PREF_FAKE_DNS_ENABLED, effectiveFakeDns)

            // 6. Fragment
            val effectiveFragment = if (profile.id == "base" || profile.fragmentEnabled == null) {
                prefs.getBoolean("base_fragment_enabled", false)
            } else {
                profile.fragmentEnabled
            }
            MmkvManager.encodeSettings(AppConfig.PREF_FRAGMENT_ENABLED, effectiveFragment)

            // 7. Mux
            val effectiveMux = if (profile.id == "base" || profile.muxEnabled == null) {
                prefs.getBoolean("base_mux_enabled", false)
            } else {
                profile.muxEnabled
            }
            MmkvManager.encodeSettings(AppConfig.PREF_MUX_ENABLED, effectiveMux)

            // 8. Sniffing
            val effectiveSniffing = if (profile.id == "base" || profile.sniffingEnabled == null) {
                prefs.getBoolean("base_sniffing_enabled", true)
            } else {
                profile.sniffingEnabled
            }
            MmkvManager.encodeSettings(AppConfig.PREF_SNIFFING_ENABLED, effectiveSniffing)

            // 9. Custom rules
            val effectiveDirectRules = if (profile.id == "base" || profile.customDirectRules == null) {
                prefs.getString("base_custom_direct_rules", "") ?: ""
            } else {
                profile.customDirectRules
            }
            val effectiveProxyRules = if (profile.id == "base" || profile.customProxyRules == null) {
                prefs.getString("base_custom_proxy_rules", "") ?: ""
            } else {
                profile.customProxyRules
            }
            MmkvManager.encodeSettings("pref_custom_direct_rules", effectiveDirectRules)
            MmkvManager.encodeSettings("pref_custom_proxy_rules", effectiveProxyRules)

            // 10. Selected VPN Config
            if (profile.assignedConfigId == "LAST_ACTIVE") {
                val savedServerId = prefs.getString("last_selected_server_profile_${profile.id}", null)
                if (!savedServerId.isNullOrBlank()) {
                    MmkvManager.setSelectServer(savedServerId)
                }
            } else if (profile.assignedConfigId != null) {
                MmkvManager.setSelectServer(profile.assignedConfigId)
            }
        } catch (e: Exception) {
            com.v2ray.ang.util.LogUtil.e(AppConfig.TAG, "Failed to apply profile preset: ${e.message}", e)
        }
    }
}
