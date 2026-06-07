package com.perqa.byebox

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import android.app.StatusBarManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.perqa.byebox.data.DefaultDataRepository
import com.perqa.byebox.service.HiddifyVpnService
import com.perqa.byebox.theme.HiddifyExpressiveTheme
import com.perqa.byebox.ui.main.ConnectionStatus
import com.perqa.byebox.ui.main.MainScreenViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val dataRepository by lazy { DefaultDataRepository(applicationContext) }
    private val viewModel by viewModels<MainScreenViewModel> {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MainScreenViewModel(dataRepository, applicationContext) as T
            }
        }
    }

    private var lastRuntimeSignature: String? = null

    private val vpnPrepareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnServiceInternal()
        } else {
            Toast.makeText(this, "Разрешение на VPN отклонено", Toast.LENGTH_SHORT).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Доступ к уведомлениям отклонен. Вы не увидите статус VPN.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.perqa.byebox.core.AppLogger.init(applicationContext)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                val activeConfig = state.configs.find { it.id == state.activeConfigId }
                if (activeConfig != null) {
                    if (HiddifyVpnService.isRunning) {
                        val runtimeSignature = state.runtimeSignature()
                        val serviceConfigJson = getSharedPreferences(HiddifyVpnService.PREFS_NAME, Context.MODE_PRIVATE)
                            .getString(HiddifyVpnService.PREF_CONFIG_JSON, null)
                        val activeConfigJson = activeConfig.toJson().toString()
                        if (lastRuntimeSignature == null) {
                            lastRuntimeSignature = runtimeSignature
                            if (serviceConfigJson != null && serviceConfigJson != activeConfigJson) {
                                startVpnServiceInternal()
                            }
                        } else if (lastRuntimeSignature != runtimeSignature) {
                            lastRuntimeSignature = runtimeSignature
                            startVpnServiceInternal()
                        }
                    } else {
                        lastRuntimeSignature = null
                    }
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            HiddifyExpressiveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(viewModel = viewModel)
                }
            }
        }

        // Handle deep links on cold start
        handleDeepLinkIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        val scheme = uri.scheme?.lowercase() ?: return
        val supportedSchemes = setOf(
            "vless", "vmess", "trojan", "ss", "tuic",
            "hysteria2", "hy2", "wg", "hiddify", "sing-box"
        )
        if (scheme !in supportedSchemes) return
        val url = uri.toString()
        viewModel.addConfigFromUrl(url)
    }

    fun handleVpnToggle(connect: Boolean) {
        if (connect) {
            val intent = VpnService.prepare(this)
            if (intent != null) {
                vpnPrepareLauncher.launch(intent)
            } else {
                startVpnServiceInternal()
            }
        } else {
            stopVpnServiceInternal()
        }
    }

    fun openSystemVpnSettings() {
        runCatching {
            startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
        }.onFailure {
            Toast.makeText(this, "Не удалось открыть настройки VPN", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestQuickSettingsTile() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val statusBarManager = getSystemService(StatusBarManager::class.java)
            val componentName = android.content.ComponentName(this, com.perqa.byebox.service.ByeBoxTileService::class.java)
            statusBarManager.requestAddTileService(
                componentName,
                getString(com.perqa.byebox.R.string.app_name),
                Icon.createWithResource(this, com.perqa.byebox.R.drawable.ic_notification),
                mainExecutor
            ) { result ->
                val message = when (result) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> "Плитка добавлена"
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> "Плитка уже добавлена"
                    else -> "Плитку можно добавить из быстрых настроек"
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Добавьте плитку ByeBox из панели быстрых настроек", Toast.LENGTH_LONG).show()
        }
    }

    fun shareActiveConfig() {
        val state = viewModel.uiState.value
        val activeConfig = state.configs.find { it.id == state.activeConfigId }
        val link = activeConfig?.toConfigLink().orEmpty()
        if (link.isBlank()) {
            Toast.makeText(this, "Нет активной конфигурации для отправки", Toast.LENGTH_SHORT).show()
            return
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "ByeBox: ${activeConfig?.name ?: "VPN config"}")
            putExtra(Intent.EXTRA_TEXT, link)
        }
        startActivity(Intent.createChooser(sendIntent, "Поделиться конфигурацией"))
    }

    private fun startVpnServiceInternal() {
        val state = viewModel.uiState.value
        val activeConfig = state.configs.find { it.id == state.activeConfigId }
        val dnsAddress = state.dnsServer.address
        val routingProfile = state.routingProfile.name

        if (activeConfig == null || activeConfig.address.isBlank() || activeConfig.port <= 0) {
            viewModel.showToast("Нет выбранной рабочей конфигурации")
            return
        }

        lastRuntimeSignature = state.runtimeSignature()

        val configJson = activeConfig.toJson().toString()

        val intent = Intent(this, HiddifyVpnService::class.java).apply {
            action = HiddifyVpnService.ACTION_CONNECT
            putExtra(HiddifyVpnService.EXTRA_CONFIG_JSON, configJson)
            putExtra(HiddifyVpnService.EXTRA_DNS_ADDRESS, dnsAddress)
            putExtra(HiddifyVpnService.EXTRA_ROUTING_PROFILE, routingProfile)
            putExtra(HiddifyVpnService.EXTRA_IPV6_ENABLED, state.ipv6Enabled)
            putExtra(HiddifyVpnService.EXTRA_LAN_BYPASS_ENABLED, state.lanBypassEnabled)
            putExtra(HiddifyVpnService.EXTRA_SYSTEM_BYPASS_ENABLED, state.systemBypassEnabled)
            putExtra(HiddifyVpnService.EXTRA_METERED_NETWORK, state.meteredNetwork)
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopVpnServiceInternal() {
        val intent = Intent(this, HiddifyVpnService::class.java).apply {
            action = HiddifyVpnService.ACTION_DISCONNECT
        }
        try {
            startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

private fun com.perqa.byebox.ui.main.MainUiState.runtimeSignature(): String {
    return listOf(
        activeConfigId.orEmpty(),
        dnsServer.name,
        routingProfile.name,
        ipv6Enabled,
        lanBypassEnabled,
        systemBypassEnabled,
        meteredNetwork
    ).joinToString("|")
}

fun Context.findActivity(): ComponentActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is ComponentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}


