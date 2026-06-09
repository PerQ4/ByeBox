package com.perqa.byebox.core

import android.net.ConnectivityManager
import android.net.Network
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LocalResolverTest {

    private lateinit var mockConnectivityManager: ConnectivityManager
    private lateinit var mockNetwork: Network

    @Before
    fun setUp() {
        mockConnectivityManager = mock(ConnectivityManager::class.java)
        mockNetwork = mock(Network::class.java)
        LocalResolver.connectivityManager = mockConnectivityManager
        LocalResolver.defaultNetwork = mockNetwork
    }

    @Test
    fun raw_returnsTrueOnAndroidQ() {
        assertEquals(true, LocalResolver.raw())
    }

    @Test
    fun defaultNetwork_isSet() {
        assertNotNull(LocalResolver.defaultNetwork)
        assertEquals(mockNetwork, LocalResolver.defaultNetwork)
    }

    @Test
    fun defaultNetwork_canBeNulled() {
        LocalResolver.defaultNetwork = null
        assertNull(LocalResolver.defaultNetwork)
        LocalResolver.defaultNetwork = mockNetwork
    }

    @Test
    fun connectivityManager_isSet() {
        assertNotNull(LocalResolver.connectivityManager)
        assertEquals(mockConnectivityManager, LocalResolver.connectivityManager)
    }
}
