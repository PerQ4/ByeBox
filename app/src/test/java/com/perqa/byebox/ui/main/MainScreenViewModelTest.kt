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
            ProxyConfig("1", "DE", "VLESS", "addr", 443, "uuid", ping = 10),
            ProxyConfig("2", "US", "VMESS", "addr", 80, "uuid", ping = 20),
            ProxyConfig("3", "NL", "Trojan", "addr", 443, "uuid", ping = 30),
            ProxyConfig("4", "RU", "Shadowsocks", "addr", 1080, "uuid", ping = 40)
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


