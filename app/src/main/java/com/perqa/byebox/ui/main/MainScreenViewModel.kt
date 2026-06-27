package com.perqa.byebox.ui.main

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perqa.byebox.data.ProxyConfig
import com.perqa.byebox.data.SubscriptionSource
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.isActive
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.SubscriptionCache
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.util.MessageUtil
import com.perqa.byebox.theme.AppTheme
import com.perqa.byebox.theme.DarkThemeStyle
import com.perqa.byebox.data.SettingsProfileData
import com.perqa.byebox.data.ProfilePresetManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.net.URL

import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import kotlin.system.measureTimeMillis

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    RECONNECTING,
    CONNECTED
}

enum class RoutingProfile(val label: String) {
    BYPASS_LAN_CN_RU("Обход LAN, Китая и РФ"),
    PROXY_ALL("Проксировать всё"),
    DIRECT("Прямое подключение")
}

enum class DnsServer(val label: String, val address: String) {
    SYSTEM("Системный DNS", "System Default"),
    CLOUDFLARE("Cloudflare DNS", "1.1.1.1"),
    GOOGLE("Google DNS", "8.8.8.8"),
    ADGUARD("AdGuard DNS (фильтр)", "94.140.14.14"),
    CUSTOM("Свой DNS", "custom")
}

enum class AppRoutingMode(val label: String, val description: String) {
    OFF("Все приложения", "VPN работает для всего трафика устройства"),
    ONLY_SELECTED("Только выбранные", "Через VPN идут только пакеты из списка"),
    BYPASS_SELECTED("Обход выбранных", "Приложения из списка идут напрямую, остальные через VPN")
}

enum class TunStack(val label: String, val description: String, val xrayValue: String) {
    GVISOR("gVisor", "Изоляция стека — максимальная совместимость", "gvisor"),
    SYSTEM("System", "Системный стек — максимальная скорость", "system")
}

data class MainUiState(
    val configs: List<ProxyConfig> = emptyList(),
    val subscriptionSources: List<SubscriptionSource> = emptyList(),
    val activeConfigId: String? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val downloadSpeed: String = "0.0 KB/s",
    val uploadSpeed: String = "0.0 KB/s",
    val appTheme: AppTheme = AppTheme.SYSTEM_DYNAMIC,
    val routingProfile: RoutingProfile = RoutingProfile.BYPASS_LAN_CN_RU,
    val dnsServer: DnsServer = DnsServer.SYSTEM,
    val lanBypassEnabled: Boolean = true,
    val appRoutingMode: AppRoutingMode = AppRoutingMode.OFF,
    val appRoutingPackages: String = "",
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val healthCheckUrl: String = "https://www.gstatic.com/generate_204",
    val logs: List<String> = emptyList(),
    val isPinging: Boolean = false,
    val toastMessage: String? = null,
    val tunStack: TunStack = TunStack.GVISOR,
    val vpnModeEnabled: Boolean = true,
    val socksPort: String = "10808",
    val proxySharingEnabled: Boolean = false,
    val muxEnabled: Boolean = false,
    val fakeDnsEnabled: Boolean = false,
    val fragmentEnabled: Boolean = false,
    val ipv6Enabled: Boolean = false,
    val startOnBootEnabled: Boolean = false,
    val logLevel: String = "warning",
    val blockingEnabled: Boolean = false,
    val sniffingEnabled: Boolean = true,
    val confirmRemoveEnabled: Boolean = true,
    val preferIpv6Enabled: Boolean = false,
    val tapImpactScale: Float = 0.90f,
    val cornerRoundness: String = "expressive",
    val pulseEnabled: Boolean = true,
    val glassmorphicBar: Boolean = true,
    val maxBlurEnabled: Boolean = true,
    val language: String = "system",
    val darkThemeStyle: DarkThemeStyle = DarkThemeStyle.STANDARD,
    val profiles: List<SettingsProfileData> = emptyList(),
    val activeProfileId: String = "",
    val compactLayoutEnabled: Boolean = false,
    val showFlagsEnabled: Boolean = true,
    val customDnsServer: String = "",
    val customDirectRules: String = "",
    val customProxyRules: String = "",
    val routingDomainStrategy: String = "AsIs",
    val outboundDomainResolveMethod: String = "0"
)

data class InstalledAppInfo(
    val label: String,
    val packageName: String,
    val isSystem: Boolean,
    val icon: android.graphics.Bitmap? = null
)

class MainScreenViewModel(
    private val appContext: Context? = null
) : ViewModel() {
    private val prefs = appContext?.getSharedPreferences("byebox_settings", Context.MODE_PRIVATE)

    private val _configs = MutableStateFlow<List<ProxyConfig>>(emptyList())
    private val _subscriptionSources = MutableStateFlow<List<SubscriptionSource>>(emptyList())
    private val _activeConfigId = MutableStateFlow<String?>(null)

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    private val _downloadSpeed = MutableStateFlow("0.0 KB/s")
    private val _uploadSpeed = MutableStateFlow("0.0 KB/s")
    private val _appTheme = MutableStateFlow(readEnum(KEY_APP_THEME, AppTheme.SYSTEM_DYNAMIC))
    private val _routingProfile = MutableStateFlow(readEnum(KEY_ROUTING_PROFILE, RoutingProfile.BYPASS_LAN_CN_RU))
    private val _dnsServer = MutableStateFlow(readEnum(KEY_DNS_SERVER, DnsServer.SYSTEM))
    private val _lanBypassEnabled = MutableStateFlow(MmkvManager.decodeSettingsString(AppConfig.PREF_VPN_BYPASS_LAN) != "2")
    private val _appRoutingMode = MutableStateFlow(readEnum(KEY_APP_ROUTING_MODE, AppRoutingMode.OFF))
    private val _appRoutingPackages = MutableStateFlow(readString(KEY_APP_ROUTING_PACKAGES, ""))
    private val _tunStack = MutableStateFlow(readEnum(KEY_TUN_STACK, TunStack.GVISOR))
    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val _healthCheckUrl = MutableStateFlow(MmkvManager.decodeSettingsString(AppConfig.PREF_DELAY_TEST_URL) ?: "https://www.gstatic.com/generate_204")
    private val _vpnModeEnabled = MutableStateFlow(MmkvManager.decodeSettingsString(AppConfig.PREF_MODE, "VPN") == "VPN")
    private val _socksPort = MutableStateFlow(MmkvManager.decodeSettingsString(AppConfig.PREF_SOCKS_PORT, "10808") ?: "10808")
    private val _proxySharingEnabled = MutableStateFlow(MmkvManager.decodeSettingsBool(AppConfig.PREF_PROXY_SHARING, false))
    private val _muxEnabled = MutableStateFlow(MmkvManager.decodeSettingsBool(AppConfig.PREF_MUX_ENABLED, false))
    private val _fakeDnsEnabled = MutableStateFlow(MmkvManager.decodeSettingsBool(AppConfig.PREF_FAKE_DNS_ENABLED, false))
    private val _fragmentEnabled = MutableStateFlow(MmkvManager.decodeSettingsBool(AppConfig.PREF_FRAGMENT_ENABLED, false))
    private val _ipv6Enabled = MutableStateFlow(MmkvManager.decodeSettingsBool(AppConfig.PREF_IPV6_ENABLED, false))
    private val _startOnBootEnabled = MutableStateFlow(MmkvManager.decodeStartOnBoot())
    private val _logLevel = MutableStateFlow(MmkvManager.decodeSettingsString(AppConfig.PREF_LOGLEVEL, "warning") ?: "warning")
    private val _blockingEnabled = MutableStateFlow(MmkvManager.decodeSettingsBool("pref_blocking", false))
    private val _sniffingEnabled = MutableStateFlow(MmkvManager.decodeSettingsBool(AppConfig.PREF_SNIFFING_ENABLED, true))
    private val _confirmRemoveEnabled = MutableStateFlow(MmkvManager.decodeSettingsBool(AppConfig.PREF_CONFIRM_REMOVE, true))
    private val _preferIpv6Enabled = MutableStateFlow(MmkvManager.decodeSettingsBool(AppConfig.PREF_PREFER_IPV6, false))
    private val _tapImpactScale = MutableStateFlow(0.90f)
    private val _cornerRoundness = MutableStateFlow("expressive")
    private val _pulseEnabled = MutableStateFlow(true)
    private val _glassmorphicBar = MutableStateFlow(true)
    private val _maxBlurEnabled = MutableStateFlow(true)
    private val _language = MutableStateFlow("system")
    private val _darkThemeStyle = MutableStateFlow(DarkThemeStyle.STANDARD)
    private val _profiles = MutableStateFlow<List<SettingsProfileData>>(emptyList())
    private val _activeProfileId = MutableStateFlow("")
    private var isApplyingProfilePreset = false
    private val _compactLayoutEnabled = MutableStateFlow(false)
    private val _showFlagsEnabled = MutableStateFlow(true)
    private val _customDnsServer = MutableStateFlow("")
    private val _customDirectRules = MutableStateFlow("")
    private val _customProxyRules = MutableStateFlow("")
    private val _routingDomainStrategy = MutableStateFlow("AsIs")
    private val _outboundDomainResolveMethod = MutableStateFlow("0")
    private val _logs = com.perqa.byebox.core.AppLogger.logs
    private val _isPinging = MutableStateFlow(false)
    private val _toastMessage = MutableStateFlow<String?>(null)

    private var trafficJob: Job? = null
    private var lastToastAt: Long = 0L

    private val mMsgReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val key = intent.getIntExtra("key", 0)
            val content = intent.getStringExtra("content") ?: ""
            when (key) {
                AppConfig.MSG_STATE_RUNNING,
                AppConfig.MSG_STATE_START_SUCCESS -> {
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    startTrafficUpdates()
                    loadDataFromMmkv()
                    triggerHaptic(HapticType.SUCCESS)
                }
                AppConfig.MSG_STATE_NOT_RUNNING,
                AppConfig.MSG_STATE_STOP_SUCCESS -> {
                    _connectionStatus.value = ConnectionStatus.DISCONNECTED
                    trafficJob?.cancel()
                    _downloadSpeed.value = "0.0 KB/s"
                    _uploadSpeed.value = "0.0 KB/s"
                    loadDataFromMmkv()
                    triggerHaptic(HapticType.MEDIUM)
                }
                AppConfig.MSG_STATE_START_FAILURE -> {
                    _connectionStatus.value = ConnectionStatus.DISCONNECTED
                    trafficJob?.cancel()
                    _downloadSpeed.value = "0.0 KB/s"
                    _uploadSpeed.value = "0.0 KB/s"
                    addLog("[ERROR] Сбой подключения: $content")
                    showToast("Сбой подключения")
                    loadDataFromMmkv()
                    triggerHaptic(HapticType.ERROR)
                }
            }
        }
    }

    val uiState: StateFlow<MainUiState> = combine(
        _configs,
        _subscriptionSources,
        _activeConfigId,
        _connectionStatus,
        _downloadSpeed,
        _uploadSpeed,
        _appTheme,
        _routingProfile,
        _dnsServer,
        _lanBypassEnabled,
        _appRoutingMode,
        _appRoutingPackages,
        _installedApps,
        _healthCheckUrl,
        _logs,
        _isPinging,
        _toastMessage,
        _tunStack,
        _vpnModeEnabled,
        _socksPort,
        _proxySharingEnabled,
        _muxEnabled,
        _fakeDnsEnabled,
        _fragmentEnabled,
        _ipv6Enabled,
        _startOnBootEnabled,
        _logLevel,
        _blockingEnabled,
        _sniffingEnabled,
        _confirmRemoveEnabled,
        _preferIpv6Enabled,
        _tapImpactScale,
        _cornerRoundness,
        _pulseEnabled,
        _glassmorphicBar,
        _maxBlurEnabled,
        _language,
        _darkThemeStyle,
        _profiles,
        _activeProfileId,
        _compactLayoutEnabled,
        _showFlagsEnabled,
        _customDnsServer,
        _customDirectRules,
        _customProxyRules,
        _routingDomainStrategy,
        _outboundDomainResolveMethod
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        MainUiState(
            configs = flows[0] as List<ProxyConfig>,
            subscriptionSources = flows[1] as List<SubscriptionSource>,
            activeConfigId = flows[2] as String?,
            connectionStatus = flows[3] as ConnectionStatus,
            downloadSpeed = flows[4] as String,
            uploadSpeed = flows[5] as String,
            appTheme = flows[6] as AppTheme,
            routingProfile = flows[7] as RoutingProfile,
            dnsServer = flows[8] as DnsServer,
            lanBypassEnabled = flows[9] as Boolean,
            appRoutingMode = flows[10] as AppRoutingMode,
            appRoutingPackages = flows[11] as String,
            installedApps = flows[12] as List<InstalledAppInfo>,
            healthCheckUrl = flows[13] as String,
            logs = flows[14] as List<String>,
            isPinging = flows[15] as Boolean,
            toastMessage = flows[16] as String?,
            tunStack = flows[17] as TunStack,
            vpnModeEnabled = flows[18] as Boolean,
            socksPort = flows[19] as String,
            proxySharingEnabled = flows[20] as Boolean,
            muxEnabled = flows[21] as Boolean,
            fakeDnsEnabled = flows[22] as Boolean,
            fragmentEnabled = flows[23] as Boolean,
            ipv6Enabled = flows[24] as Boolean,
            startOnBootEnabled = flows[25] as Boolean,
            logLevel = flows[26] as String,
            blockingEnabled = flows[27] as Boolean,
            sniffingEnabled = flows[28] as Boolean,
            confirmRemoveEnabled = flows[29] as Boolean,
            preferIpv6Enabled = flows[30] as Boolean,
            tapImpactScale = flows[31] as Float,
            cornerRoundness = flows[32] as String,
            pulseEnabled = flows[33] as Boolean,
            glassmorphicBar = flows[34] as Boolean,
            maxBlurEnabled = flows[35] as Boolean,
            language = flows[36] as String,
            darkThemeStyle = flows[37] as DarkThemeStyle,
            profiles = flows[38] as List<SettingsProfileData>,
            activeProfileId = flows[39] as String,
            compactLayoutEnabled = flows[40] as Boolean,
            showFlagsEnabled = flows[41] as Boolean,
            customDnsServer = flows[42] as String,
            customDirectRules = flows[43] as String,
            customProxyRules = flows[44] as String,
            routingDomainStrategy = flows[45] as String,
            outboundDomainResolveMethod = flows[46] as String
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    private val prefsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "pref_dynamic_profiles" || key == "pref_active_profile_id" || key == KEY_DARK_THEME_STYLE ||
            key?.startsWith("base_") == true) {
            loadDataFromMmkv()
        }
    }

    init {
        migrateBaseSettingsIfNeeded()
        prefs?.registerOnSharedPreferenceChangeListener(prefsListener)
        loadInstalledApps()
        loadDataFromMmkv()

        val context = appContext
        if (context != null) {
            val filter = android.content.IntentFilter(AppConfig.BROADCAST_ACTION_ACTIVITY)
            context.registerReceiver(mMsgReceiver, filter, Context.RECEIVER_EXPORTED)
            MessageUtil.sendMsg2Service(context, AppConfig.MSG_REGISTER_CLIENT, "")
        }

        _connectionStatus.value = if (CoreServiceManager.isRunning()) {
            startTrafficUpdates()
            ConnectionStatus.CONNECTED
        } else {
            ConnectionStatus.DISCONNECTED
        }
    }

    fun setConnectingState(): Boolean {
        val active = getActiveConfig()
        if (active == null || active.address.isBlank() || active.port <= 0) {
            return false
        }
        _connectionStatus.value = ConnectionStatus.CONNECTING
        addLog("[INFO] Запуск ядра Xray...")
        return true
    }

    fun resetConnectionState() {
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
    }

    private fun startTrafficUpdates() {
        trafficJob?.cancel()
        trafficJob = viewModelScope.launch(Dispatchers.IO) {
            var lastQueryTime = System.currentTimeMillis()
            while (isActive) {
                delay(1000)
                if (_connectionStatus.value != ConnectionStatus.CONNECTED) continue
                
                val queryTime = System.currentTimeMillis()
                val sinceLastQuery = queryTime - lastQueryTime
                if (sinceLastQuery <= 0) continue
                val seconds = sinceLastQuery / 1000.0
                
                var proxyUplink = 0L
                var proxyDownlink = 0L
                
                CoreServiceManager.queryAllOutboundTrafficStats().forEach { stat ->
                    if (stat.tag.startsWith(AppConfig.TAG_PROXY)) {
                        when (stat.direction) {
                            AppConfig.UPLINK -> proxyUplink += stat.value
                            AppConfig.DOWNLINK -> proxyDownlink += stat.value
                        }
                    }
                }
                
                lastQueryTime = queryTime
                
                withContext(Dispatchers.Main) {
                    _downloadSpeed.value = formatBytesPerSec((proxyDownlink / seconds).toLong())
                    _uploadSpeed.value = formatBytesPerSec((proxyUplink / seconds).toLong())
                }
            }
        }
    }

    private fun formatBytesPerSec(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1_048_576L -> String.format(Locale.US, "%.1f MB/s", bytesPerSec / 1_048_576.0)
            bytesPerSec >= 1024L -> String.format(Locale.US, "%.1f KB/s", bytesPerSec / 1024.0)
            else -> "${bytesPerSec} B/s"
        }
    }

    private fun getActiveConfig(): ProxyConfig? {
        val configs = _configs.value
        val activeId = _activeConfigId.value
        return configs.find { it.id == activeId }
    }

    fun selectConfig(id: String) {
        MmkvManager.setSelectServer(id)
        _activeConfigId.value = id
        
        // Autosave the selected server for the active profile preset
        val context = appContext
        if (context != null) {
            val activeId = ProfilePresetManager.getActiveProfileId(context)
            val prefs = context.getSharedPreferences("byebox_settings", Context.MODE_PRIVATE)
            prefs.edit().putString("last_selected_server_profile_$activeId", id).apply()
        }

        val active = _configs.value.find { it.id == id }
        if (active != null) {
            addLog("[SYSTEM] Выбрана конфигурация: ${active.name}")
            showToast("Выбран сервер: ${active.name}")
        }
    }

    fun selectBestConfig() {
        val best = _configs.value
            .filter { it.ping != null && it.ping < 999 && it.failureCount < 3 }
            .minByOrNull { it.ping ?: Int.MAX_VALUE }
            ?: _configs.value
                .filter { it.failureCount < 3 }
                .minByOrNull { it.ping ?: Int.MAX_VALUE }
            ?: _configs.value.firstOrNull()

        if (best == null) {
            showToast("Нет доступных конфигураций")
            return
        }

        selectConfig(best.id)
        addLog("[SYSTEM] Выбран лучший сервер: ${best.name} (${best.ping?.let { "$it ms" } ?: "N/A"})")
        showToast("Лучший сервер: ${best.name}")
    }

    fun addConfigFromUrl(url: String) {
        viewModelScope.launch {
            val trimmedUrl = url.trim()
            if (trimmedUrl.startsWith("http://", ignoreCase = true) || trimmedUrl.startsWith("https://", ignoreCase = true)) {
                addLog("[INFO] Добавление подписки по ссылке: $trimmedUrl")
                showToast("Добавление подписки...")
                
                val result = withContext(Dispatchers.IO) {
                    val subscriptions = MmkvManager.decodeSubscriptions()
                    var existingSub = subscriptions.find { it.subscription.url == trimmedUrl }
                    val subId = existingSub?.guid ?: com.v2ray.ang.util.Utils.getUuid()
                    
                    if (existingSub == null) {
                        val uri = try { java.net.URI(com.v2ray.ang.util.Utils.fixIllegalUrl(trimmedUrl)) } catch(e: Exception) { null }
                        val host = uri?.host ?: "Подписка"
                        val remarks = uri?.fragment ?: host
                        
                        val subItem = com.v2ray.ang.dto.entities.SubscriptionItem().apply {
                            this.remarks = remarks
                            this.url = trimmedUrl
                            this.enabled = true
                        }
                        MmkvManager.encodeSubscription(subId, subItem)
                        existingSub = com.v2ray.ang.dto.entities.SubscriptionCache(subId, subItem)
                    }
                    
                    AngConfigManager.updateConfigViaSub(existingSub)
                }
                
                if (result.configCount > 0) {
                    addLog("[SYSTEM] Подписка успешно обновлена. Импортировано узлов: ${result.configCount}")
                    showToast("Импортировано серверов: ${result.configCount}")
                    loadDataFromMmkv()
                } else {
                    addLog("[ERROR] Не удалось загрузить сервера из подписки.")
                    showToast("Загружено 0 серверов")
                    loadDataFromMmkv()
                }
            } else {
                val (configCount, _) = withContext(Dispatchers.IO) {
                    AngConfigManager.importBatchConfig(trimmedUrl, "", true)
                }
                if (configCount > 0) {
                    addLog("[SYSTEM] Успешно добавлена новая конфигурация по ссылке.")
                    showToast("Сервер успешно добавлен!")
                    loadDataFromMmkv()
                } else {
                    addLog("[ERROR] Не удалось распарсить ссылку!")
                    showToast("Ошибка: Неподдерживаемый формат ссылки!")
                }
            }
        }
    }

    fun refreshSubscriptions() {
        viewModelScope.launch {
            addLog("[SYSTEM] Обновление подписок...")
            showToast("Обновление подписок...")
            val result = withContext(Dispatchers.IO) {
                AngConfigManager.updateConfigViaSubAll()
            }
            addLog("[SYSTEM] Обновлено конфигураций: ${result.configCount}, Успешно: ${result.successCount}, Ошибок: ${result.failureCount}")
            showToast("Обновлено серверов: ${result.configCount}")
            loadDataFromMmkv()
        }
    }

    fun refreshSubscription(sourceId: String) {
        viewModelScope.launch {
            val sub = MmkvManager.decodeSubscription(sourceId)
            if (sub == null) {
                showToast("Источник не найден")
                return@launch
            }
            addLog("[SYSTEM] Обновление подписки: ${sub.remarks}...")
            showToast("Обновление подписки...")
            val result = withContext(Dispatchers.IO) {
                val cache = SubscriptionCache(sourceId, sub)
                AngConfigManager.updateConfigViaSub(cache)
            }
            addLog("[SYSTEM] Обновлено конфигураций: ${result.configCount}, Ошибок: ${result.failureCount}")
            showToast("Обновлено серверов: ${result.configCount}")
            loadDataFromMmkv()
        }
    }

    fun renameSubscriptionSource(sourceId: String, newName: String) {
        val cleanName = newName.trim()
        if (cleanName.isBlank()) return
        val sub = MmkvManager.decodeSubscription(sourceId) ?: return
        sub.remarks = cleanName
        MmkvManager.encodeSubscription(sourceId, sub)
        loadDataFromMmkv()
        addLog("[SYSTEM] Источник переименован: $cleanName")
        showToast("Источник переименован")
    }

    fun deleteSubscriptionSource(sourceId: String) {
        val sub = MmkvManager.decodeSubscription(sourceId)
        val name = sub?.remarks ?: "Источник"
        MmkvManager.removeSubscription(sourceId)
        loadDataFromMmkv()
        addLog("[SYSTEM] Удален источник подписки: $name")
        showToast("Удалено: $name")
    }

    fun deleteConfig(id: String) {
        val nodeName = _configs.value.find { it.id == id }?.name ?: "Неизвестный"
        MmkvManager.removeServer(id)
        loadDataFromMmkv()
        addLog("[SYSTEM] Удален сервер: $nodeName")
        showToast("Удалено: $nodeName")
    }

    fun updateConfig(updatedConfig: ProxyConfig) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val profile = MmkvManager.decodeServerConfig(updatedConfig.id) ?: return@withContext
                profile.remarks = updatedConfig.name
                profile.server = updatedConfig.address
                profile.serverPort = updatedConfig.port.toString()
                profile.password = updatedConfig.uuid
                profile.sni = updatedConfig.sni
                profile.publicKey = updatedConfig.pbk
                profile.shortId = updatedConfig.sid
                profile.flow = updatedConfig.flow
                profile.security = updatedConfig.security
                profile.network = updatedConfig.network
                profile.path = updatedConfig.wsPath
                profile.host = updatedConfig.wsHost
                profile.serviceName = updatedConfig.grpcServiceName
                
                MmkvManager.encodeServerConfig(updatedConfig.id, profile)
            }
            loadDataFromMmkv()
            showToast("Конфигурация обновлена")
        }
    }

    fun testPings() {
        viewModelScope.launch {
            if (_isPinging.value) return@launch
            _isPinging.value = true
            try {
                addLog("[SYSTEM] Запуск тестирования задержки серверов...")
                val summary = probeConfigs(_configs.value)
                addLog("[SYSTEM] Тестирование пинга завершено: ${summary.ok} ok, ${summary.failed} timeout.")
                showToast("Пинг обновлён: ${summary.ok}/${summary.total}")
                loadDataFromMmkv()
                triggerHaptic(HapticType.SUCCESS)
            } finally {
                _isPinging.value = false
            }
        }
    }

    fun testActiveConfigPing() {
        val activeId = _activeConfigId.value ?: return
        val activeConfig = _configs.value.find { it.id == activeId } ?: return
        viewModelScope.launch {
            if (_isPinging.value) return@launch
            _isPinging.value = true
            try {
                addLog("[SYSTEM] Тестирование задержки активного сервера ${activeConfig.name}...")
                val ping = probeTcpLatency(activeConfig) ?: 999
                MmkvManager.encodeServerTestDelayMillis(activeId, ping.toLong())
                addLog("[PING] ${activeConfig.name} -> $ping ms")
                showToast("Пинг: $ping ms")
                loadDataFromMmkv()
                triggerHaptic(if (ping < 999) HapticType.SUCCESS else HapticType.ERROR)
            } finally {
                _isPinging.value = false
            }
        }
    }

    fun testPingsForSource(sourceName: String) {
        viewModelScope.launch {
            if (_isPinging.value) return@launch
            val configs = _configs.value.filter { it.sourceName == sourceName }
            if (configs.isEmpty()) return@launch
            _isPinging.value = true
            try {
                addLog("[SYSTEM] Пинг источника: $sourceName (${configs.size})")
                val summary = probeConfigs(configs)
                addLog("[SYSTEM] Пинг источника завершён: $sourceName, ${summary.ok} ok, ${summary.failed} timeout")
                showToast("Пинг источника: ${summary.ok}/${summary.total}")
                loadDataFromMmkv()
                triggerHaptic(HapticType.SUCCESS)
            } finally {
                _isPinging.value = false
            }
        }
    }

    private data class ProbeSummary(val total: Int, val ok: Int, val failed: Int)

    private suspend fun probeConfigs(configs: List<ProxyConfig>): ProbeSummary = coroutineScope {
        var ok = 0
        var failed = 0
        val healthUrl = _healthCheckUrl.value.trim()
        val strictHealthCheck = false
        configs.chunked(8).forEach { batch ->
            batch.map { config ->
                async { config to probeConfigLatency(config, healthUrl, strictHealthCheck) }
            }.awaitAll().forEach { (config, ping) ->
                if (ping != null) {
                    ok += 1
                    MmkvManager.encodeServerTestDelayMillis(config.id, ping.toLong())
                    addLog("[PING] ${config.name} -> $ping ms")
                } else {
                    failed += 1
                    MmkvManager.encodeServerTestDelayMillis(config.id, 999L)
                    addLog("[PING] ${config.name} -> timeout")
                }
            }
        }
        ProbeSummary(configs.size, ok, failed)
    }

    private suspend fun probeConfigLatency(config: ProxyConfig, healthUrl: String, strictHealthCheck: Boolean): Int? {
        val ping = probeTcpLatency(config) ?: return null
        if (!strictHealthCheck) return ping
        return if (probeResource(healthUrl)) ping else null
    }

    private suspend fun probeResource(rawUrl: String): Boolean = withContext(Dispatchers.IO) {
        if (rawUrl.isBlank()) return@withContext true
        val normalizedUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) rawUrl else "https://$rawUrl"
        val connection = try {
            (URL(normalizedUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 3500
                readTimeout = 3500
                instanceFollowRedirects = false
            }
        } catch (_: Exception) {
            return@withContext false
        }
        try {
            val code = connection.responseCode
            code in 200..399 || code == 204 || code == 405
        } catch (_: Exception) {
            false
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun probeTcpLatency(config: ProxyConfig): Int? = withContext(Dispatchers.IO) {
        if (config.address.isBlank() || config.port <= 0) return@withContext null
        var socket: Socket? = null
        try {
            val elapsed = measureTimeMillis {
                socket = Socket()
                socket.soTimeout = 2500
                socket.connect(InetSocketAddress(config.address, config.port), 2500)
            }
            elapsed.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        } catch (_: Exception) {
            null
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {
            }
        }
    }

    fun changeTheme(theme: AppTheme) {
        _appTheme.value = theme
        writeString(KEY_APP_THEME, theme.name)
        addLog("[SYSTEM] Смена темы оформления: $theme")
        showToast("Тема изменена: ${theme.name.replace("_", " ")}")
    }

    fun changeRoutingProfile(profile: RoutingProfile) {
        resetProfileToCustom()
        _routingProfile.value = profile
        writeString("base_routing_profile", profile.name)
        propagateActiveProfile()
        addLog("[SYSTEM] Профиль маршрутизации изменен: ${profile.label}")
        showToast("Маршрутизация: ${profile.label}")
    }

    fun changeDnsServer(dns: DnsServer) {
        resetProfileToCustom()
        _dnsServer.value = dns
        writeString("base_dns_server", dns.name)
        propagateActiveProfile()
        addLog("[SYSTEM] Выбран DNS-сервер: ${dns.label}")
        showToast("DNS: ${dns.label}")
    }

    fun changeLanBypassEnabled(enabled: Boolean) {
        resetProfileToCustom()
        _lanBypassEnabled.value = enabled
        writeBoolean(KEY_LAN_BYPASS_ENABLED, enabled)
        
        MmkvManager.encodeSettings(AppConfig.PREF_VPN_BYPASS_LAN, if (enabled) "1" else "2")
        addLog("[SYSTEM] Обход локальных сетей: ${if (enabled) "включен" else "выключен"}")
        showToast("LAN bypass: ${if (enabled) "включен" else "выключен"}")
    }

    fun changeVpnModeEnabled(enabled: Boolean) {
        resetProfileToCustom()
        _vpnModeEnabled.value = enabled
        MmkvManager.encodeSettings(AppConfig.PREF_MODE, if (enabled) "VPN" else "PROXY")
        addLog("[SYSTEM] Режим VPN: ${if (enabled) "включен" else "локальный прокси"}")
        showToast(if (enabled) "Режим VPN" else "Режим локального прокси")
    }

    fun changeSocksPort(port: String) {
        val cleanPort = port.filter { it.isDigit() }
        if (cleanPort.isNotEmpty()) {
            _socksPort.value = cleanPort
            MmkvManager.encodeSettings(AppConfig.PREF_SOCKS_PORT, cleanPort)
        }
    }

    fun changeProxySharingEnabled(enabled: Boolean) {
        resetProfileToCustom()
        _proxySharingEnabled.value = enabled
        MmkvManager.encodeSettings(AppConfig.PREF_PROXY_SHARING, enabled)
        addLog("[SYSTEM] LAN Sharing: ${if (enabled) "разрешен" else "запрещен"}")
        showToast("LAN Sharing: ${if (enabled) "включен" else "выключен"}")
    }

    fun changeMuxEnabled(enabled: Boolean) {
        resetProfileToCustom()
        _muxEnabled.value = enabled
        writeBoolean("base_mux_enabled", enabled)
        propagateActiveProfile()
        addLog("[SYSTEM] Мультиплексирование (Mux): ${if (enabled) "включено" else "выключено"}")
        showToast("Mux: ${if (enabled) "включен" else "выключен"}")
    }

    fun changeAppRoutingMode(mode: AppRoutingMode) {
        resetProfileToCustom()
        _appRoutingMode.value = mode
        writeString("base_app_routing_mode", mode.name)
        propagateActiveProfile()
        addLog("[SYSTEM] Профиль приложений VPN: ${mode.label}")
        showToast("Приложения: ${mode.label}")
    }

    fun changeAppRoutingPackages(value: String) {
        val normalized = normalizePackageText(value)
        _appRoutingPackages.value = normalized
        writeString("base_app_routing_packages_str", normalized)
        
        val set = parsePackageText(normalized).toSet()
        prefs?.edit()?.putStringSet("base_app_routing_packages", set)?.apply()
        propagateActiveProfile()
    }

    fun toggleAppRoutingPackage(packageName: String) {
        val packages = parsePackageText(_appRoutingPackages.value).toMutableSet()
        if (!packages.add(packageName)) {
            packages.remove(packageName)
        }
        val normalized = packages.sorted().joinToString("\n")
        _appRoutingPackages.value = normalized
        writeString("base_app_routing_packages_str", normalized)
        
        prefs?.edit()?.putStringSet("base_app_routing_packages", packages)?.apply()
        propagateActiveProfile()
    }

    fun clearAppRoutingPackages() {
        _appRoutingPackages.value = ""
        writeString("base_app_routing_packages_str", "")
        prefs?.edit()?.putStringSet("base_app_routing_packages", emptySet())?.apply()
        propagateActiveProfile()
        showToast("Список приложений очищен")
    }

    fun changeTunStack(stack: TunStack) {
        resetProfileToCustom()
        _tunStack.value = stack
        writeString("base_tun_stack", stack.name)
        propagateActiveProfile()
        addLog("[SYSTEM] TUN стек: ${stack.label}")
        showToast("TUN стек: ${stack.label}")
    }

    fun changeHealthCheckUrl(value: String) {
        _healthCheckUrl.value = value
        writeString(KEY_HEALTH_CHECK_URL, value)
        MmkvManager.encodeSettings(AppConfig.PREF_DELAY_TEST_URL, value)
    }

    fun changeFakeDnsEnabled(enabled: Boolean) {
        resetProfileToCustom()
        _fakeDnsEnabled.value = enabled
        writeBoolean("base_fake_dns_enabled", enabled)
        propagateActiveProfile()
        addLog("[SYSTEM] Fake DNS: ${if (enabled) "включен" else "выключен"}")
        showToast("Fake DNS: ${if (enabled) "включен" else "выключен"}")
    }

    fun changeFragmentEnabled(enabled: Boolean) {
        resetProfileToCustom()
        _fragmentEnabled.value = enabled
        writeBoolean("base_fragment_enabled", enabled)
        propagateActiveProfile()
        addLog("[SYSTEM] Фрагментация (Fragment): ${if (enabled) "включена" else "выключена"}")
        showToast("Fragment: ${if (enabled) "включен" else "выключен"}")
    }

    fun changeIpv6Enabled(enabled: Boolean) {
        _ipv6Enabled.value = enabled
        MmkvManager.encodeSettings(AppConfig.PREF_IPV6_ENABLED, enabled)
        addLog("[SYSTEM] Поддержка IPv6: ${if (enabled) "включена" else "выключена"}")
        showToast("IPv6: ${if (enabled) "включен" else "выключен"}")
    }

    fun changeStartOnBootEnabled(enabled: Boolean) {
        _startOnBootEnabled.value = enabled
        MmkvManager.encodeStartOnBoot(enabled)
        addLog("[SYSTEM] Автозапуск при загрузке: ${if (enabled) "включен" else "выключен"}")
        showToast("Автозапуск: ${if (enabled) "включен" else "выключен"}")
    }

    fun changeBlockingEnabled(enabled: Boolean) {
        _blockingEnabled.value = enabled
        MmkvManager.encodeSettings("pref_blocking", enabled)
        addLog("[SYSTEM] Блокировка без VPN (Kill Switch): ${if (enabled) "включена" else "выключена"}")
        showToast("Kill Switch: ${if (enabled) "включен" else "выключен"}")
    }

    fun changeSniffingEnabled(enabled: Boolean) {
        resetProfileToCustom()
        _sniffingEnabled.value = enabled
        writeBoolean("base_sniffing_enabled", enabled)
        propagateActiveProfile()
        addLog("[SYSTEM] Сниффинг трафика: ${if (enabled) "включен" else "выключен"}")
        showToast("Сниффинг: ${if (enabled) "включен" else "выключен"}")
    }

    fun changeConfirmRemoveEnabled(enabled: Boolean) {
        _confirmRemoveEnabled.value = enabled
        MmkvManager.encodeSettings(AppConfig.PREF_CONFIRM_REMOVE, enabled)
    }

    fun changePreferIpv6Enabled(enabled: Boolean) {
        _preferIpv6Enabled.value = enabled
        MmkvManager.encodeSettings(AppConfig.PREF_PREFER_IPV6, enabled)
        addLog("[SYSTEM] Предпочитать IPv6: ${if (enabled) "включено" else "выключено"}")
        showToast("Предпочитать IPv6: ${if (enabled) "включено" else "выключено"}")
    }

    fun changeTapImpactScale(scale: Float) {
        _tapImpactScale.value = scale
        prefs?.edit()?.putFloat("pref_tap_impact_scale", scale)?.apply()
    }

    fun changeRoutingDomainStrategy(strategy: String) {
        _routingDomainStrategy.value = strategy
        MmkvManager.encodeSettings(AppConfig.PREF_ROUTING_DOMAIN_STRATEGY, strategy)
        addLog("[SYSTEM] Domain Strategy изменена: $strategy")
        showToast("Domain Strategy: $strategy")
    }

    fun changeOutboundDomainResolveMethod(method: String) {
        _outboundDomainResolveMethod.value = method
        MmkvManager.encodeSettings(AppConfig.PREF_OUTBOUND_DOMAIN_RESOLVE_METHOD, method)
        addLog("[SYSTEM] Outbound Resolve Method изменён: $method")
        showToast("Outbound Resolve: $method")
    }

    fun changeCornerRoundness(roundness: String) {
        _cornerRoundness.value = roundness
        prefs?.edit()?.putString("pref_corner_roundness", roundness)?.apply()
    }

    fun changePulseEnabled(enabled: Boolean) {
        _pulseEnabled.value = enabled
        prefs?.edit()?.putBoolean("pref_pulse_enabled", enabled)?.apply()
    }

    fun changeGlassmorphicBar(enabled: Boolean) {
        _glassmorphicBar.value = enabled
        prefs?.edit()?.putBoolean("pref_glassmorphic_bar", enabled)?.apply()
    }

    fun changeMaxBlurEnabled(enabled: Boolean) {
        _maxBlurEnabled.value = enabled
        prefs?.edit()?.putBoolean("pref_max_blur", enabled)?.apply()
    }

    fun changeLanguage(lang: String) {
        _language.value = lang
        prefs?.edit()?.putString("pref_language", lang)?.apply()
    }

    private fun resetProfileToCustom() {
        // No-op in dynamic profile system
    }

    fun changeDarkThemeStyle(style: DarkThemeStyle) {
        _darkThemeStyle.value = style
        writeString(KEY_DARK_THEME_STYLE, style.name)
    }

    fun changeActiveProfileId(id: String) {
        val context = appContext ?: return
        val list = _profiles.value
        val profile = list.find { it.id == id } ?: return

        isApplyingProfilePreset = true
        try {
            ProfilePresetManager.switchActiveProfile(context, id)
            loadDataFromMmkv()

            // If service is running, toggle it to apply new Xray settings/server config
            val isVpnRunning = MmkvManager.decodeSettingsBool(AppConfig.PREF_TILE_VPN_RUNNING, false)
            if (isVpnRunning) {
                com.v2ray.ang.util.MessageUtil.sendMsg2Service(context, AppConfig.MSG_STATE_RESTART, "")
            }
        } finally {
            isApplyingProfilePreset = false
        }
        addLog("[SYSTEM] Активирован профиль настроек: ${profile.name}")
        showToast("Профиль: ${profile.name}")
    }

    fun addProfile(profile: SettingsProfileData) {
        val context = appContext ?: return
        val newList = _profiles.value + profile
        _profiles.value = newList
        ProfilePresetManager.saveProfiles(context, newList)
        addLog("[SYSTEM] Создан новый профиль: ${profile.name}")
        showToast("Создан профиль: ${profile.name}")
    }

    fun updateProfile(updated: SettingsProfileData) {
        val context = appContext ?: return
        val newList = _profiles.value.map { if (it.id == updated.id) updated else it }
        _profiles.value = newList
        ProfilePresetManager.saveProfiles(context, newList)
        
        // If updating the currently active profile, apply its updated settings
        if (updated.id == _activeProfileId.value) {
            isApplyingProfilePreset = true
            try {
                ProfilePresetManager.applyProfile(context, updated)
                loadDataFromMmkv()
                val isVpnRunning = MmkvManager.decodeSettingsBool(AppConfig.PREF_TILE_VPN_RUNNING, false)
                if (isVpnRunning) {
                    com.v2ray.ang.util.MessageUtil.sendMsg2Service(context, AppConfig.MSG_STATE_RESTART, "")
                }
            } finally {
                isApplyingProfilePreset = false
            }
        }
        showToast("Профиль сохранен")
    }

    fun deleteProfile(id: String) {
        val context = appContext ?: return
        val list = _profiles.value
        if (list.size <= 1) {
            showToast("Нельзя удалить единственный профиль")
            return
        }
        val target = list.find { it.id == id } ?: return
        val newList = list.filter { it.id != id }
        _profiles.value = newList
        ProfilePresetManager.saveProfiles(context, newList)
        
        // If we deleted the active profile, switch to the first remaining profile
        if (id == _activeProfileId.value) {
            val nextId = newList.firstOrNull()?.id ?: ""
            changeActiveProfileId(nextId)
        }
        addLog("[SYSTEM] Удален профиль: ${target.name}")
        showToast("Удален профиль: ${target.name}")
    }

    fun reorderProfiles(reordered: List<SettingsProfileData>) {
        val context = appContext ?: return
        _profiles.value = reordered
        ProfilePresetManager.saveProfiles(context, reordered)
    }

    fun changeCompactLayoutEnabled(enabled: Boolean) {
        _compactLayoutEnabled.value = enabled
        prefs?.edit()?.putBoolean("pref_compact_layout", enabled)?.apply()
    }

    fun changeShowFlagsEnabled(enabled: Boolean) {
        _showFlagsEnabled.value = enabled
        prefs?.edit()?.putBoolean("pref_show_flags", enabled)?.apply()
    }

    fun changeCustomDnsServer(server: String) {
        val trimmed = server.trim()
        if (trimmed.isNotBlank() && !com.v2ray.ang.util.Utils.isPureIpAddress(trimmed)) {
            showToast("Некорректный IP-адрес DNS")
            return
        }
        _customDnsServer.value = trimmed
        writeString("base_custom_dns", trimmed)
        propagateActiveProfile()
        addLog("[SYSTEM] Свой DNS: $trimmed")
    }

    fun changeCustomDirectRules(rules: String) {
        _customDirectRules.value = rules
        writeString("base_custom_direct_rules", rules)
        propagateActiveProfile()
        addLog("[SYSTEM] Кастомные правила DIRECT: $rules")
    }

    fun changeCustomProxyRules(rules: String) {
        _customProxyRules.value = rules
        writeString("base_custom_proxy_rules", rules)
        propagateActiveProfile()
        addLog("[SYSTEM] Кастомные правила PROXY: $rules")
    }

    fun changeLogLevel(level: String) {
        _logLevel.value = level
        MmkvManager.encodeSettings(AppConfig.PREF_LOGLEVEL, level)
        com.v2ray.ang.util.LogUtil.refreshLogLevel()
        addLog("[SYSTEM] Уровень логов: $level")
        showToast("Уровень логов: $level")
    }

    fun testHealthCheckUrl() {
        val url = _healthCheckUrl.value
        viewModelScope.launch {
            val ok = probeResource(url)
            addLog("[SYSTEM] Проверка ресурса $url: ${if (ok) "ok" else "failed"}")
            showToast(if (ok) "Ресурс доступен" else "Ресурс недоступен")
        }
    }

    fun clearLogs() {
        com.perqa.byebox.core.AppLogger.clearLogs()
    }

    private fun addLog(message: String) {
        val tag = "ViewModel"
        when {
            message.contains("[ERROR]") -> com.perqa.byebox.core.AppLogger.error(tag, message.replace("[ERROR]", "").trim())
            message.contains("[WARNING]") -> com.perqa.byebox.core.AppLogger.warn(tag, message.replace("[WARNING]", "").trim())
            else -> com.perqa.byebox.core.AppLogger.info(tag, message.replace("[SYSTEM]", "").replace("[INFO]", "").trim())
        }
    }

    fun exportLogs(context: Context) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val logFile = java.io.File(context.filesDir, "box_log.txt")
                    if (!logFile.exists() || logFile.length() == 0L) {
                        return@withContext "Логи пусты"
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val resolver = context.contentResolver
                        val contentValues = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "byebox_log_${System.currentTimeMillis()}.txt")
                            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                        }
                        val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                            ?: return@withContext "Не удалось создать файл в Downloads"
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            logFile.inputStream().use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        "Логи сохранены в папку Downloads"
                    } else {
                        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                        if (!downloadsDir.exists()) downloadsDir.mkdirs()
                        val targetFile = java.io.File(downloadsDir, "byebox_log_${System.currentTimeMillis()}.txt")
                        logFile.inputStream().use { inputStream ->
                            targetFile.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        "Логи сохранены в: ${targetFile.absolutePath}"
                    }
                } catch (e: Exception) {
                    "Ошибка экспорта: ${e.message}"
                }
            }
            showToast(result)
        }
    }

    fun showToast(message: String) {
        val now = System.currentTimeMillis()
        if (now - lastToastAt < 1800L) return
        lastToastAt = now
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun triggerHaptic(type: HapticType) {
        val context = appContext ?: return
        val scale = _tapImpactScale.value
        runCatching {
            val multiplier = when {
                scale >= 1.00f -> 0.0f
                scale >= 0.95f -> 0.6f
                scale >= 0.90f -> 1.0f
                else -> 1.6f
            }
            if (multiplier <= 0.0f) return

            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }

            if (vibrator?.hasVibrator() == true) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    when (type) {
                        HapticType.LIGHT -> {
                            val dur = (8 * multiplier).toLong().coerceAtLeast(1)
                            val amp = (30 * multiplier).toInt().coerceIn(1, 255)
                            vibrator.vibrate(android.os.VibrationEffect.createOneShot(dur, amp))
                        }
                        HapticType.MEDIUM -> {
                            val dur = (15 * multiplier).toLong().coerceAtLeast(1)
                            val amp = (70 * multiplier).toInt().coerceIn(1, 255)
                            vibrator.vibrate(android.os.VibrationEffect.createOneShot(dur, amp))
                        }
                        HapticType.HEAVY -> {
                            val dur = (30 * multiplier).toLong().coerceAtLeast(1)
                            val amp = (140 * multiplier).toInt().coerceIn(1, 255)
                            vibrator.vibrate(android.os.VibrationEffect.createOneShot(dur, amp))
                        }
                        HapticType.SUCCESS -> {
                            val timings = longArrayOf(0, (10 * multiplier).toLong().coerceAtLeast(1), 40, (20 * multiplier).toLong().coerceAtLeast(1))
                            val amplitudes = intArrayOf(0, (120 * multiplier).toInt().coerceIn(1, 255), 0, (140 * multiplier).toInt().coerceIn(1, 255))
                            vibrator.vibrate(android.os.VibrationEffect.createWaveform(timings, amplitudes, -1))
                        }
                        HapticType.ERROR -> {
                            val timings = longArrayOf(0, (30 * multiplier).toLong().coerceAtLeast(1), 30, (40 * multiplier).toLong().coerceAtLeast(1))
                            val amplitudes = intArrayOf(0, (160 * multiplier).toInt().coerceIn(1, 255), 0, (200 * multiplier).toInt().coerceIn(1, 255))
                            vibrator.vibrate(android.os.VibrationEffect.createWaveform(timings, amplitudes, -1))
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    when (type) {
                        HapticType.LIGHT -> vibrator.vibrate((8 * multiplier).toLong().coerceAtLeast(1))
                        HapticType.MEDIUM -> vibrator.vibrate((15 * multiplier).toLong().coerceAtLeast(1))
                        HapticType.HEAVY -> vibrator.vibrate((30 * multiplier).toLong().coerceAtLeast(1))
                        HapticType.SUCCESS -> vibrator.vibrate(longArrayOf(0, (10 * multiplier).toLong().coerceAtLeast(1), 40, (20 * multiplier).toLong().coerceAtLeast(1)), -1)
                        HapticType.ERROR -> vibrator.vibrate(longArrayOf(0, (30 * multiplier).toLong().coerceAtLeast(1), 30, (40 * multiplier).toLong().coerceAtLeast(1)), -1)
                    }
                }
            }
        }
    }

    private inline fun <reified T : Enum<T>> readEnum(key: String, fallback: T): T {
        val value = prefs?.getString(key, null) ?: return fallback
        return enumValues<T>().firstOrNull { it.name == value } ?: fallback
    }

    private fun readBoolean(key: String, fallback: Boolean): Boolean {
        return prefs?.getBoolean(key, fallback) ?: fallback
    }

    private fun readString(key: String, fallback: String): String {
        return prefs?.getString(key, fallback) ?: fallback
    }

    private fun loadInstalledApps() {
        val context = appContext ?: return
        viewModelScope.launch {
            val pm = context.packageManager
            val launcherIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = withContext(Dispatchers.IO) {
                pm.queryIntentActivities(launcherIntent, 0)
                    .filter { it.activityInfo?.packageName != context.packageName }
                    .distinctBy { it.activityInfo?.packageName }
            }
            val apps = resolveInfos.map { ri ->
                val packageName = ri.activityInfo?.packageName.orEmpty()
                val label = ri.loadLabel(pm).toString().ifBlank { packageName }
                val appInfo = runCatching { pm.getApplicationInfo(packageName, 0) }.getOrNull()
                val isSystem = appInfo?.flags?.and(android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                val icon = loadAppIcon(pm, packageName)
                InstalledAppInfo(label = label, packageName = packageName, isSystem = isSystem, icon = icon)
            }.sortedWith(
                compareBy<InstalledAppInfo> { it.label.lowercase(Locale.getDefault()) }
                    .thenBy { it.packageName }
            )
            _installedApps.value = apps
        }
    }

    private fun loadAppIcon(pm: android.content.pm.PackageManager, packageName: String): android.graphics.Bitmap? {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val drawable = appInfo.loadIcon(pm)
            val size = 128
            val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
            val w = drawable.minimumWidth.coerceIn(1, size)
            val h = drawable.minimumHeight.coerceIn(1, size)
            val left = (size - w) / 2
            val top = (size - h) / 2
            drawable.mutate()
            drawable.setBounds(left, top, left + w, top + h)
            drawable.draw(canvas)
            val pixel = bitmap.getPixel(size / 2, size / 2)
            if (pixel == 0) {
                drawable.setBounds(0, 0, size, size)
                drawable.draw(canvas)
            }
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("ByeBox", "loadAppIcon failed for $packageName", e)
            null
        }
    }

    private fun normalizePackageText(value: String): String {
        return parsePackageText(value).joinToString("\n")
    }

    private fun parsePackageText(value: String): List<String> {
        return value
            .split(',', '\n', '\r', ';', ' ', '\t')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    private fun writeString(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply()
    }

    private fun writeBoolean(key: String, value: Boolean) {
        prefs?.edit()?.putBoolean(key, value)?.apply()
    }

    fun loadDataFromMmkv() {
        val serverGuids = MmkvManager.decodeAllServerList()
        val proxyConfigs = serverGuids.mapNotNull { guid ->
            MmkvManager.decodeServerConfig(guid)?.toProxyConfig(guid)
        }
        _configs.value = proxyConfigs

        val subsList = MmkvManager.decodeSubscriptions()
        _subscriptionSources.value = subsList.map { subCache ->
            SubscriptionSource(
                id = subCache.guid,
                name = subCache.subscription.remarks,
                url = subCache.subscription.url,
                lastUpdatedAt = subCache.subscription.lastUpdated,
                nodeCount = MmkvManager.decodeServerList(subCache.guid).size,
                uploadBytes = subCache.subscription.uploadBytes,
                downloadBytes = subCache.subscription.downloadBytes,
                totalBytes = subCache.subscription.totalBytes,
                expireAt = subCache.subscription.expireAt,
                description = subCache.subscription.description
            )
        }

        _activeConfigId.value = MmkvManager.getSelectServer()

        _vpnModeEnabled.value = MmkvManager.decodeSettingsString(AppConfig.PREF_MODE, "VPN") == "VPN"
        _socksPort.value = MmkvManager.decodeSettingsString(AppConfig.PREF_SOCKS_PORT, "10808") ?: "10808"
        _proxySharingEnabled.value = MmkvManager.decodeSettingsBool(AppConfig.PREF_PROXY_SHARING, false)
        _muxEnabled.value = prefs?.getBoolean("base_mux_enabled", false) ?: false
        _fakeDnsEnabled.value = prefs?.getBoolean("base_fake_dns_enabled", true) ?: true
        _fragmentEnabled.value = prefs?.getBoolean("base_fragment_enabled", false) ?: false
        _ipv6Enabled.value = MmkvManager.decodeSettingsBool(AppConfig.PREF_IPV6_ENABLED, false)
        _startOnBootEnabled.value = MmkvManager.decodeStartOnBoot()
        _lanBypassEnabled.value = MmkvManager.decodeSettingsString(AppConfig.PREF_VPN_BYPASS_LAN) != "2"
        _healthCheckUrl.value = MmkvManager.decodeSettingsString(AppConfig.PREF_DELAY_TEST_URL) ?: "https://www.gstatic.com/generate_204"
        _blockingEnabled.value = MmkvManager.decodeSettingsBool("pref_blocking", false)
        _sniffingEnabled.value = prefs?.getBoolean("base_sniffing_enabled", true) ?: true
        _confirmRemoveEnabled.value = MmkvManager.decodeSettingsBool(AppConfig.PREF_CONFIRM_REMOVE, true)
        _preferIpv6Enabled.value = MmkvManager.decodeSettingsBool(AppConfig.PREF_PREFER_IPV6, false)
        _tapImpactScale.value = prefs?.getFloat("pref_tap_impact_scale", 0.90f) ?: 0.90f
        _cornerRoundness.value = prefs?.getString("pref_corner_roundness", "expressive") ?: "expressive"
        _pulseEnabled.value = prefs?.getBoolean("pref_pulse_enabled", true) ?: true
        _glassmorphicBar.value = prefs?.getBoolean("pref_glassmorphic_bar", true) ?: true
        _maxBlurEnabled.value = prefs?.getBoolean("pref_max_blur", true) ?: true
        _language.value = prefs?.getString("pref_language", "system") ?: "system"
        val context = appContext
        if (context != null) {
            _profiles.value = ProfilePresetManager.loadProfiles(context)
            _activeProfileId.value = ProfilePresetManager.getActiveProfileId(context)
        }
        _darkThemeStyle.value = readEnum(KEY_DARK_THEME_STYLE, DarkThemeStyle.STANDARD)
        _compactLayoutEnabled.value = prefs?.getBoolean("pref_compact_layout", false) ?: false
        _showFlagsEnabled.value = prefs?.getBoolean("pref_show_flags", true) ?: true
        _customDnsServer.value = prefs?.getString("base_custom_dns", "") ?: ""
        _customDirectRules.value = prefs?.getString("base_custom_direct_rules", "") ?: ""
        _customProxyRules.value = prefs?.getString("base_custom_proxy_rules", "") ?: ""
        
        _routingProfile.value = readEnum("base_routing_profile", RoutingProfile.BYPASS_LAN_CN_RU)
        _dnsServer.value = readEnum("base_dns_server", DnsServer.SYSTEM)
        _appRoutingMode.value = readEnum("base_app_routing_mode", AppRoutingMode.OFF)
        _appRoutingPackages.value = readString("base_app_routing_packages_str", "")
        _tunStack.value = readEnum("base_tun_stack", TunStack.GVISOR)
        _routingDomainStrategy.value = MmkvManager.decodeSettingsString(AppConfig.PREF_ROUTING_DOMAIN_STRATEGY) ?: "AsIs"
        _outboundDomainResolveMethod.value = MmkvManager.decodeSettingsString(AppConfig.PREF_OUTBOUND_DOMAIN_RESOLVE_METHOD) ?: "0"
    }

    override fun onCleared() {
        super.onCleared()
        prefs?.unregisterOnSharedPreferenceChangeListener(prefsListener)
        appContext?.unregisterReceiver(mMsgReceiver)
        trafficJob?.cancel()
    }

    private fun migrateBaseSettingsIfNeeded() {
        val context = appContext ?: return
        val prefs = context.getSharedPreferences("byebox_settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("base_settings_migrated_v2", false)) return

        val editor = prefs.edit()
        
        // 1. routing_profile
        if (!prefs.contains("base_routing_profile")) {
            val oldVal = prefs.getString("routing_profile", "BYPASS_LAN_CN_RU")
            editor.putString("base_routing_profile", oldVal)
        }
        // 2. dns_server
        if (!prefs.contains("base_dns_server")) {
            val oldVal = prefs.getString("dns_server", "SYSTEM")
            editor.putString("base_dns_server", oldVal)
        }
        // 3. custom dns
        if (!prefs.contains("base_custom_dns")) {
            val oldVal = MmkvManager.decodeSettingsString("pref_custom_dns", "") ?: ""
            editor.putString("base_custom_dns", oldVal)
        }
        // 4. app_routing_mode
        if (!prefs.contains("base_app_routing_mode")) {
            val oldVal = prefs.getString("app_routing_mode", "OFF")
            editor.putString("base_app_routing_mode", oldVal)
        }
        // 5. app_routing_packages
        if (!prefs.contains("base_app_routing_packages_str")) {
            val oldVal = prefs.getString("app_routing_packages", "")
            editor.putString("base_app_routing_packages_str", oldVal)
        }
        // 6. tun_stack
        if (!prefs.contains("base_tun_stack")) {
            val oldVal = prefs.getString("tun_stack", "SYSTEM")
            editor.putString("base_tun_stack", oldVal)
        }
        // 7. fake dns
        if (!prefs.contains("base_fake_dns_enabled")) {
            val oldVal = MmkvManager.decodeSettingsBool(AppConfig.PREF_FAKE_DNS_ENABLED, true)
            editor.putBoolean("base_fake_dns_enabled", oldVal)
        }
        // 8. fragment
        if (!prefs.contains("base_fragment_enabled")) {
            val oldVal = MmkvManager.decodeSettingsBool(AppConfig.PREF_FRAGMENT_ENABLED, false)
            editor.putBoolean("base_fragment_enabled", oldVal)
        }
        // 9. mux
        if (!prefs.contains("base_mux_enabled")) {
            val oldVal = MmkvManager.decodeSettingsBool(AppConfig.PREF_MUX_ENABLED, false)
            editor.putBoolean("base_mux_enabled", oldVal)
        }
        // 10. sniffing
        if (!prefs.contains("base_sniffing_enabled")) {
            val oldVal = MmkvManager.decodeSettingsBool(AppConfig.PREF_SNIFFING_ENABLED, true)
            editor.putBoolean("base_sniffing_enabled", oldVal)
        }
        // 11. custom rules
        if (!prefs.contains("base_custom_direct_rules")) {
            val oldVal = MmkvManager.decodeSettingsString("pref_custom_direct_rules", "") ?: ""
            editor.putString("base_custom_direct_rules", oldVal)
        }
        if (!prefs.contains("base_custom_proxy_rules")) {
            val oldVal = MmkvManager.decodeSettingsString("pref_custom_proxy_rules", "") ?: ""
            editor.putString("base_custom_proxy_rules", oldVal)
        }
        
        val packagesSet = MmkvManager.decodeSettingsStringSet(AppConfig.PREF_PER_APP_PROXY_SET) ?: emptySet()
        editor.putStringSet("base_app_routing_packages", packagesSet)

        editor.putBoolean("base_settings_migrated_v2", true)
        editor.apply()
    }

    private fun propagateActiveProfile() {
        val context = appContext ?: return
        val activeId = ProfilePresetManager.getActiveProfileId(context)
        val profiles = _profiles.value.ifEmpty { ProfilePresetManager.loadProfiles(context) }
        val activeProfile = profiles.find { it.id == activeId } ?: return
        
        isApplyingProfilePreset = true
        try {
            ProfilePresetManager.applyProfile(context, activeProfile)
        } finally {
            isApplyingProfilePreset = false
        }
    }

    companion object {
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_ROUTING_PROFILE = "routing_profile"
        private const val KEY_DNS_SERVER = "dns_server"
        private const val KEY_LAN_BYPASS_ENABLED = "lan_bypass_enabled"
        private const val KEY_SYSTEM_BYPASS_ENABLED = "system_bypass_enabled"
        private const val KEY_METERED_NETWORK = "metered_network"
        private const val KEY_APP_ROUTING_MODE = "app_routing_mode"
        private const val KEY_APP_ROUTING_PACKAGES = "app_routing_packages"
        private const val KEY_HEALTH_CHECK_URL = "health_check_url"
        private const val KEY_STRICT_HEALTH_CHECK = "strict_health_check"
        private const val KEY_TUN_STACK = "tun_stack"
        private const val KEY_SETTINGS_PROFILE = "settings_profile"
        private const val KEY_DARK_THEME_STYLE = "pref_dark_theme_style"
    }
}

private fun ProfileItem.toProxyConfig(guid: String): ProxyConfig {
    val protocolStr = when (this.configType) {
        EConfigType.VMESS -> "VMESS"
        EConfigType.VLESS -> "VLESS"
        EConfigType.TROJAN -> "Trojan"
        EConfigType.SHADOWSOCKS -> "Shadowsocks"
        else -> this.configType.name
    }
    
    val subRemarks = this.subscriptionId.let { subId ->
        MmkvManager.decodeSubscription(subId)?.remarks
    }.orEmpty().ifBlank { "Локальные конфигурации" }
    
    val subUrl = this.subscriptionId.let { subId ->
        MmkvManager.decodeSubscription(subId)?.url
    }
    
    val pingVal = MmkvManager.decodeServerAffiliationInfo(guid)?.testDelayMillis?.toInt()
    val pingResult = if (pingVal != null && pingVal > 0) pingVal else null

    return ProxyConfig(
        id = guid,
        name = this.remarks,
        description = this.description,
        protocol = protocolStr,
        address = this.server.orEmpty(),
        port = this.serverPort?.toIntOrNull() ?: 0,
        uuid = this.password.orEmpty(),
        flow = this.flow,
        security = this.security,
        sni = this.sni,
        pbk = this.publicKey,
        sid = this.shortId,
        network = this.network,
        wsPath = this.path,
        wsHost = this.host,
        grpcServiceName = this.serviceName,
        ping = pingResult,
        failureCount = 0,
        lastFailureAt = null,
        sourceName = subRemarks,
        sourceUrl = subUrl,
        countryFlag = inferCountryFlag(this.server.orEmpty(), this.remarks)
    )
}

private fun countryCodeToFlag(code: String): String? {
    if (code.length != 2) return null
    val uppercased = code.uppercase()
    val firstChar = uppercased[0].code
    val secondChar = uppercased[1].code
    if (firstChar !in 'A'.code..'Z'.code || secondChar !in 'A'.code..'Z'.code) return null
    val flagCodepoints = intArrayOf(0x1F1E6 - 'A'.code + firstChar, 0x1F1E6 - 'A'.code + secondChar)
    return String(flagCodepoints, 0, 2)
}

private val tldToCountryCode = mapOf(
    "jp" to "JP", "ru" to "RU", "de" to "DE", "fr" to "FR",
    "uk" to "GB", "us" to "US", "sg" to "SG", "hk" to "HK",
    "tw" to "TW", "kr" to "KR", "au" to "AU", "ca" to "CA",
    "br" to "BR", "in" to "IN", "it" to "IT", "es" to "ES",
    "nl" to "NL", "se" to "SE", "no" to "NO", "fi" to "FI",
    "dk" to "DK", "pl" to "PL", "cz" to "CZ", "at" to "AT",
    "ch" to "CH", "be" to "BE", "ie" to "IE", "nz" to "NZ",
    "za" to "ZA", "mx" to "MX", "ar" to "AR", "cl" to "CL",
    "co" to "CO", "pe" to "PE", "vn" to "VN", "th" to "TH",
    "my" to "MY", "ph" to "PH", "id" to "ID", "tr" to "TR",
    "ae" to "AE", "sa" to "SA", "il" to "IL", "cn" to "CN",
    "mo" to "MO", "ro" to "RO", "bg" to "BG", "hu" to "HU",
    "gr" to "GR", "pt" to "PT", "ua" to "UA", "kz" to "KZ",
    "ir" to "IR", "pk" to "PK", "vn" to "VN", "eg" to "EG"
)

private fun inferCountryFlag(address: String, remarks: String): String {
    val flagRegex = Regex("[\\uD83C\\uDDE6-\\uD83C\\uDDFF]{2}")
    val existingFlag = flagRegex.find(remarks)
    if (existingFlag != null) return existingFlag.value

    val ccRegex = Regex("""(?:^|\s|\[|\()([A-Za-z]{2})(?:\s|]|\)|_|-)""")
    ccRegex.findAll(remarks).forEach { match ->
        val code = match.groupValues[1].uppercase()
        countryCodeToFlag(code)?.let { return it }
    }

    val tld = address.trimEnd('.').substringAfterLast('.').lowercase()
    if (tld.length == 2 || tld.length == 3) {
        val mapped = tldToCountryCode[tld]
        if (mapped != null) {
            countryCodeToFlag(mapped)?.let { return it }
        }
        countryCodeToFlag(tld.uppercase())?.let { return it }
    }

    return "🏳️"
}


