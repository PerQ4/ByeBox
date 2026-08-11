package com.perqa.byebox.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsProfileDataTest {

    private fun optIsEmpty(json: JSONObject, key: String): Boolean {
        return !json.has(key) || json.optString(key, "").isEmpty()
    }

    @Test
    fun `toJson and fromJson round-trip preserves all fields`() {
        val original = SettingsProfileData(
            id = "profile-1",
            name = "Work Profile",
            assignedConfigId = "config-abc",
            routingProfile = "BYPASS_LAN_CN_RU",
            dnsServer = "CLOUDFLARE",
            customDnsServer = "1.1.1.1",
            appRoutingMode = "ONLY_SELECTED",
            tunStack = "GVISOR",
            fakeDnsEnabled = true,
            fragmentEnabled = false,
            muxEnabled = true,
            sniffingEnabled = true,
            customDirectRules = "yandex.ru\nvk.com",
            customProxyRules = "instagram.com",
            appRoutingPackages = setOf("com.example.app", "com.test.app")
        )

        val json = original.toJson()
        val restored = SettingsProfileData.fromJson(json)

        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.assignedConfigId, restored.assignedConfigId)
        assertEquals(original.routingProfile, restored.routingProfile)
        assertEquals(original.dnsServer, restored.dnsServer)
        assertEquals(original.customDnsServer, restored.customDnsServer)
        assertEquals(original.appRoutingMode, restored.appRoutingMode)
        assertEquals(original.tunStack, restored.tunStack)
        assertEquals(original.fakeDnsEnabled, restored.fakeDnsEnabled)
        assertEquals(original.fragmentEnabled, restored.fragmentEnabled)
        assertEquals(original.muxEnabled, restored.muxEnabled)
        assertEquals(original.sniffingEnabled, restored.sniffingEnabled)
        assertEquals(original.customDirectRules, restored.customDirectRules)
        assertEquals(original.customProxyRules, restored.customProxyRules)
        assertNotNull(restored.appRoutingPackages)
        assertEquals(2, restored.appRoutingPackages!!.size)
        assertTrue(restored.appRoutingPackages!!.contains("com.example.app"))
    }

    @Test
    fun `fromJson handles minimal fields`() {
        val json = JSONObject()
            .put("name", "Minimal Profile")

        val restored = SettingsProfileData.fromJson(json)
        assertNotNull(restored.id)
        assertEquals("Minimal Profile", restored.name)
        assertNull(restored.assignedConfigId)
        assertEquals("INHERIT", restored.routingProfile)
        assertEquals("INHERIT", restored.dnsServer)
        assertEquals("INHERIT", restored.appRoutingMode)
        assertEquals("INHERIT", restored.tunStack)
        assertNull(restored.fakeDnsEnabled)
        assertNull(restored.fragmentEnabled)
        assertNull(restored.customDirectRules)
        assertNull(restored.appRoutingPackages)
    }

    @Test
    fun `toJson omits null optionals`() {
        val profile = SettingsProfileData(
            id = "p1",
            name = "Simple"
        )
        val json = profile.toJson()
        assertTrue(optIsEmpty(json, "assignedConfigId"))
        assertTrue(optIsEmpty(json, "customDnsServer"))
        assertNull(json.opt("fakeDnsEnabled"))
        assertTrue(!json.has("appRoutingPackages") || json.optJSONArray("appRoutingPackages")?.length() == 0)
    }

    @Test
    fun `appRoutingPackages round-trip preserves set`() {
        val packages = setOf("a.a", "b.b", "c.c.c")
        val profile = SettingsProfileData(
            id = "pkg-test",
            name = "Package Test",
            appRoutingPackages = packages
        )
        val json = profile.toJson()
        val arr = json.getJSONArray("appRoutingPackages")
        assertEquals(3, arr.length())

        val restored = SettingsProfileData.fromJson(json)
        assertEquals(packages, restored.appRoutingPackages)
    }
}
