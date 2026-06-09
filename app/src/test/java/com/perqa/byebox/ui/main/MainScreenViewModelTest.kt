package com.perqa.byebox.ui.main

import com.perqa.byebox.data.DataRepository
import com.perqa.byebox.data.ProxyConfig
import com.perqa.byebox.data.SubscriptionSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainScreenViewModelTest {
  @Test
  fun uiState_initiallyLoadsConfigs() = runTest {
    val repository = FakeMyModelRepository()
    val viewModel = MainScreenViewModel(repository)
    val state = viewModel.uiState.value
    assertNotNull(state)
    assertEquals(4, state.configs.size)
  }

  @Test
  fun uiState_onConnectionToggle_statusChanges() = runTest {
    val repository = FakeMyModelRepository()
    val viewModel = MainScreenViewModel(repository)
    assertEquals(ConnectionStatus.DISCONNECTED, viewModel.uiState.value.connectionStatus)
  }
}

private class FakeMyModelRepository : DataRepository {
    private val _configs = MutableStateFlow<List<ProxyConfig>>(
        listOf(
            ProxyConfig(id = "1", name = "DE", protocol = "VLESS", address = "addr", port = 443, uuid = "uuid", ping = 10),
            ProxyConfig(id = "2", name = "US", protocol = "VMESS", address = "addr", port = 80, uuid = "uuid", ping = 20),
            ProxyConfig(id = "3", name = "NL", protocol = "Trojan", address = "addr", port = 443, uuid = "uuid", ping = 30),
            ProxyConfig(id = "4", name = "RU", protocol = "Shadowsocks", address = "addr", port = 1080, uuid = "uuid", ping = 40)
        )
    )
    override val configs: StateFlow<List<ProxyConfig>> = _configs.asStateFlow()
    override val activeConfigId: StateFlow<String?> = MutableStateFlow("1")
    override val subscriptionSources: StateFlow<List<SubscriptionSource>> = MutableStateFlow(emptyList())

    override fun addConfig(config: ProxyConfig) {}
    override fun addConfigFromUrl(url: String): Boolean = true
    override fun upsertSubscriptionSource(source: SubscriptionSource, configs: List<ProxyConfig>) {}
    override fun renameSubscriptionSource(sourceId: String, newName: String) {}
    override fun deleteSubscriptionSource(sourceId: String) {}
    override fun selectConfig(id: String) {}
    override fun deleteConfig(id: String) {}
    override fun updatePing(id: String, ping: Int) {}
}


