package com.perqa.byebox.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RegionProxyListsTest {

    @Test
    fun fetch_cn_include_returnsPackages() = runBlocking {
        val packages = RegionProxyLists.fetch(
            RegionProxyLists.Region.CN,
            RegionProxyLists.AppProxyMode.INCLUDE
        )
        // Should return a non-empty set from the remote list
        assertNotNull("Package list should not be null", packages)
        if (packages != null) {
            assert(packages.isNotEmpty()) { "Package list should not be empty" }
            // All entries should look like Android package names
            packages.forEach { pkg ->
                assert(pkg.contains('.')) { "Package name should contain dot: $pkg" }
            }
        }
    }

    @Test
    fun fetch_invalidRegion_returnsNull() = runBlocking {
        // "OTHER" has no proxy_cn/direct_cn file on GitHub
        val packages = RegionProxyLists.fetch(
            RegionProxyLists.Region.OTHER,
            RegionProxyLists.AppProxyMode.INCLUDE
        )
        assertNull("OTHER region should return null", packages)
    }

    @Test
    fun fetch_both_returnsPair() = runBlocking {
        val (include, exclude) = RegionProxyLists.fetchBoth(RegionProxyLists.Region.CN)
        // Both should be non-null for CN
        assertNotNull("Include list should not be null", include)
        assertNotNull("Exclude list should not be null", exclude)
    }
}
