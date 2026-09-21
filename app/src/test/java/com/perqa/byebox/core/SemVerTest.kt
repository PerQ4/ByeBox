package com.perqa.byebox.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SemVerTest {

    @Test
    fun parsesFullAndPartialVersions() {
        assertEquals(SemVer(1, 2, 0), SemVer.parse("1.2.0"))
        assertEquals(SemVer(1, 2, 0), SemVer.parse("v1.2.0"))
        // legacy two-part tags still parse
        assertEquals(SemVer(9, 0, 0), SemVer.parse("9.0"))
        assertEquals(SemVer(0, 7, 1, listOf("Beta")), SemVer.parse("v0.7.1-Beta"))
        // build metadata is ignored
        assertEquals(SemVer(1, 2, 0, listOf("beta", "1")), SemVer.parse("1.2.0-beta.1+260921.21"))
        assertNull(SemVer.parse(""))
        assertNull(SemVer.parse("not-a-version"))
    }

    @Test
    fun ordersByCoreNumbers() {
        assertTrue(SemVer.parse("1.2.0")!! > SemVer.parse("1.1.9")!!)
        assertTrue(SemVer.parse("1.2.1")!! > SemVer.parse("1.2.0")!!)
        assertTrue(SemVer.parse("2.0.0")!! > SemVer.parse("1.99.99")!!)
    }

    @Test
    fun releaseOutranksPreRelease() {
        assertTrue(SemVer.parse("1.2.0")!! > SemVer.parse("1.2.0-rc.1")!!)
        assertTrue(SemVer.parse("1.2.0-rc.1")!! > SemVer.parse("1.2.0-beta.2")!!)
        assertTrue(SemVer.parse("1.2.0-beta.2")!! > SemVer.parse("1.2.0-beta.1")!!)
        assertTrue(SemVer.parse("1.2.0-beta.1")!! > SemVer.parse("1.2.0-alpha.5")!!)
    }

    @Test
    fun extractsStageAndNumber() {
        val beta = SemVer.parse("1.3.0-beta.2")!!
        assertEquals("beta", beta.stage)
        assertEquals(2, beta.stageNumber)

        val stable = SemVer.parse("1.3.0")!!
        assertEquals("", stable.stage)
        assertEquals(0, stable.stageNumber)
    }

    @Test
    fun toStringIsSemVerCompatible() {
        assertEquals("1.2.0", SemVer(1, 2, 0).toString())
        assertEquals("1.3.0-beta.1", SemVer(1, 3, 0, listOf("beta", "1")).toString())
    }
}
