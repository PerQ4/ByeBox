package com.perqa.byebox

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.drawable.Icon
import android.graphics.Color
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
import androidx.core.view.WindowCompat
import com.perqa.byebox.theme.ByeBoxTheme
import com.perqa.byebox.ui.main.ConnectionStatus
import com.perqa.byebox.ui.main.Loc
import com.perqa.byebox.ui.main.MainScreenViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.perqa.byebox.R
import com.perqa.byebox.core.AppLogger
import com.perqa.byebox.core.UpdateCheckScheduler
import com.perqa.byebox.service.ByeBoxTileService
import com.perqa.byebox.ui.main.MainUiState
import com.perqa.byebox.ui.main.effectiveShareLink
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.handler.MmkvManager
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

class MainActivity : ComponentActivity() {

    companion object {
        const val ACTION_TOGGLE_VPN = "com.perqa.byebox.action.TOGGLE_VPN"
        const val ACTION_CHECK_UPDATES = "com.perqa.byebox.action.CHECK_UPDATES"

        // MainActivity экспортирована (LAUNCHER + deep-link), поэтому управляющие
        // action обрабатываем только при наличии внутреннего токена, который
        // добавляем сами при создании шорткатов. Иначе любое приложение могло бы
        // переключать VPN явным интентом.
        private const val EXTRA_INTERNAL_TOKEN = "com.perqa.byebox.extra.INTERNAL_TOKEN"
        private const val INTERNAL_TOKEN = "byebox:internal:9f3c1a7d"
    }

    private val viewModel by viewModels<MainScreenViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainScreenViewModel(applicationContext) as T
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
            Toast.makeText(
                this,
                Loc.get("vpn_permission_denied", viewModel.uiState.value.language),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this,
                Loc.get("notif_permission_denied", viewModel.uiState.value.language),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.init(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val activeConfig = state.configs.find { it.id == state.activeConfigId }
                    if (activeConfig != null) {
                        val isServiceRunning = state.connectionStatus == ConnectionStatus.CONNECTED ||
                                MmkvManager.decodeSettingsBool(AppConfig.PREF_TILE_VPN_RUNNING, false)
                        if (isServiceRunning) {
                            val runtimeSignature = state.runtimeSignature()
                            if (lastRuntimeSignature == null) {
                                lastRuntimeSignature = runtimeSignature
                            } else if (lastRuntimeSignature != runtimeSignature) {
                                lastRuntimeSignature = runtimeSignature
                                CoreServiceManager.stopVService(this@MainActivity)
                                delay(500)
                                requestStartVpn()
                            }
                        } else {
                            lastRuntimeSignature = null
                        }
                    }
                }
            }
        }

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            ByeBoxTheme(appTheme = state.appTheme, darkThemeStyle = state.darkThemeStyle) {
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
        handleShortcutIntent(intent)
        publishShortcuts()
        UpdateCheckScheduler.sync(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLinkIntent(intent)
        handleShortcutIntent(intent)
    }

    /**
     * App icon long-press shortcuts (dynamic shortcuts). Publishing is best
     * effort: fails silently on devices that do not support them.
     */
    private fun publishShortcuts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        try {
            val language = viewModel.uiState.value.language
            ShortcutManagerCompat.addDynamicShortcuts(
                this,
                listOf(
                    ShortcutInfoCompat.Builder(this, "toggle_vpn")
                        .setShortLabel(Loc.get("shortcut_toggle_vpn", language))
                        .setLongLabel(Loc.get("shortcut_toggle_vpn", language))
                        .setIcon(IconCompat.createWithResource(this, R.drawable.ic_notification_on))
                        .setIntent(
                            Intent(this, MainActivity::class.java).apply {
                                action = ACTION_TOGGLE_VPN
                                putExtra(EXTRA_INTERNAL_TOKEN, INTERNAL_TOKEN)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                        )
                        .build(),
                    ShortcutInfoCompat.Builder(this, "check_updates")
                        .setShortLabel(Loc.get("shortcut_check_updates", language))
                        .setLongLabel(Loc.get("shortcut_check_updates", language))
                        .setIcon(IconCompat.createWithResource(this, R.drawable.ic_notification_on))
                        .setIntent(
                            Intent(this, MainActivity::class.java).apply {
                                action = ACTION_CHECK_UPDATES
                                putExtra(EXTRA_INTERNAL_TOKEN, INTERNAL_TOKEN)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                        )
                        .build()
                )
            )
        } catch (_: Exception) {
            // Shortcuts are optional; never crash on unsupported devices
        }
    }

    private fun isInternalControlIntent(intent: Intent?): Boolean {
        if (intent == null) return false
        return intent.getStringExtra(EXTRA_INTERNAL_TOKEN) == INTERNAL_TOKEN
    }

    private fun handleShortcutIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_TOGGLE_VPN -> {
                // Игнорируем управляющие интенты извне пакета (MainActivity exported).
                if (!isInternalControlIntent(intent)) {
                    AppLogger.warn("MainActivity", "Ignored external ACTION_TOGGLE_VPN intent")
                    return
                }
                val isRunning = MmkvManager.decodeSettingsBool(AppConfig.PREF_TILE_VPN_RUNNING, false)
                handleVpnToggle(connect = !isRunning)
            }
            ACTION_CHECK_UPDATES -> {
                if (!isInternalControlIntent(intent)) {
                    AppLogger.warn("MainActivity", "Ignored external ACTION_CHECK_UPDATES intent")
                    return
                }
                viewModel.checkForUpdates()
            }
        }
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        val scheme = uri.scheme?.lowercase() ?: return
        val supportedSchemes = setOf(
            "vless", "vmess", "trojan", "ss", "hiddify", "sing-box"
        )
        if (scheme !in supportedSchemes) return
        val url = uri.toString()
        viewModel.addConfigFromUrl(url)
    }

    fun handleVpnToggle(connect: Boolean) {
        if (connect) {
            requestStartVpn()
        } else {
            stopVpnServiceInternal()
        }
    }

    private fun requestStartVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPrepareLauncher.launch(intent)
        } else {
            startVpnServiceInternal()
        }
    }

    fun openSystemVpnSettings() {
        runCatching {
            startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
        }.onFailure {
            Toast.makeText(
                this,
                Loc.get("toast_vpn_settings_fail", viewModel.uiState.value.language),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun requestQuickSettingsTile() {
        val language = viewModel.uiState.value.language
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val statusBarManager = getSystemService(StatusBarManager::class.java)
            val componentName = ComponentName(this, ByeBoxTileService::class.java)
            statusBarManager.requestAddTileService(
                componentName,
                getString(R.string.app_name),
                Icon.createWithResource(this, R.drawable.ic_notification_on),
                mainExecutor
            ) { result ->
                val message = when (result) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> Loc.get("tile_added", language)
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> Loc.get("tile_already_added", language)
                    else -> Loc.get("tile_add_hint", language)
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, Loc.get("tile_add_manual", language), Toast.LENGTH_LONG).show()
        }
    }

    fun shareActiveConfig() {
        val state = viewModel.uiState.value
        val activeConfig = state.configs.find { it.id == state.activeConfigId }
        val link = activeConfig?.effectiveShareLink().orEmpty()
        if (link.isBlank()) {
            Toast.makeText(this, Loc.get("share_no_active", state.language), Toast.LENGTH_SHORT).show()
            return
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "ByeBox: ${activeConfig?.name ?: "VPN config"}")
            putExtra(Intent.EXTRA_TEXT, link)
        }
        startActivity(Intent.createChooser(sendIntent, Loc.get("share_config_chooser", state.language)))
    }

    private fun startVpnServiceInternal() {
        val state = viewModel.uiState.value
        val activeConfig = state.configs.find { it.id == state.activeConfigId }

        if (activeConfig == null || activeConfig.address.isBlank() || activeConfig.port <= 0) {
            viewModel.showToast(Loc.get("no_active_config", state.language))
            viewModel.resetConnectionState()
            return
        }

        lastRuntimeSignature = state.runtimeSignature()

        try {
            CoreServiceManager.startVService(this, activeConfig.id)
        } catch (e: Exception) {
            e.printStackTrace()
            viewModel.showToast(String.format(Loc.get("toast_vpn_start_fail", state.language), e.message ?: ""))
        }
    }

    private fun stopVpnServiceInternal() {
        try {
            CoreServiceManager.stopVService(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

private fun MainUiState.runtimeSignature(): String {
    return listOf(
        activeConfigId.orEmpty(),
        dnsServer.name,
        // Свои DNS и кастомные правила участвуют в сборке конфига ядра, поэтому
        // их смена тоже должна перезапускать туннель (propagateActiveProfile
        // обновляет конфиг, но работающее ядро старое до рестарта).
        customDnsServer,
        customDirectRules,
        customProxyRules,
        routingDomainStrategy,
        outboundDomainResolveMethod,
        routingProfile.name,
        lanBypassEnabled,
        appRoutingMode.name,
        appRoutingPackages,
        tunStack.name,
        vpnModeEnabled,
        socksPort,
        proxySharingEnabled,
        muxEnabled,
        fakeDnsEnabled,
        fragmentEnabled,
        ipv6Enabled,
        startOnBootEnabled,
        sniffingEnabled,
        preferIpv6Enabled,
        logLevel,
        blockingEnabled
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


