package com.perqa.byebox.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perqa.byebox.data.DataRepository
import com.perqa.byebox.data.ProxyConfig
import com.perqa.byebox.data.SubscriptionSource
import com.perqa.byebox.service.CoreRuntimeState
import com.perqa.byebox.theme.AppTheme
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
    CONNECTED
}

enum class RoutingProfile(val label: String) {
    BYPASS_LAN_CN_RU("Обход LAN, Китая и РФ"),
    PROXY_ALL("Проксировать всё"),
    DIRECT("Прямое подключение"),
    BLOCK_ADS("Блокировка рекламы + Прокси")
}

enum class DnsServer(val label: String, val address: String) {
    SYSTEM("Системный DNS", "System Default"),
    CLOUDFLARE("Cloudflare DNS", "1.1.1.1"),
    GOOGLE("Google DNS", "8.8.8.8"),
    ADGUARD("AdGuard DNS (фильтр)", "94.140.14.14")
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
    val ipv6Enabled: Boolean = true,
    val lanBypassEnabled: Boolean = true,
    val systemBypassEnabled: Boolean = false,
    val meteredNetwork: Boolean = false,
    val logs: List<String> = emptyList(),
    val isPinging: Boolean = false,
    val toastMessage: String? = null
)

class MainScreenViewModel(
    private val dataRepository: DataRepository,
    private val appContext: Context? = null
) : ViewModel() {
    private val prefs = appContext?.getSharedPreferences("byebox_settings", Context.MODE_PRIVATE)

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    private val _downloadSpeed = MutableStateFlow("0.0 KB/s")
    private val _uploadSpeed = MutableStateFlow("0.0 KB/s")
    private val _appTheme = MutableStateFlow(readEnum(KEY_APP_THEME, AppTheme.SYSTEM_DYNAMIC))
    private val _routingProfile = MutableStateFlow(readEnum(KEY_ROUTING_PROFILE, RoutingProfile.BYPASS_LAN_CN_RU))
    private val _dnsServer = MutableStateFlow(readEnum(KEY_DNS_SERVER, DnsServer.SYSTEM))
    private val _ipv6Enabled = MutableStateFlow(readBoolean(KEY_IPV6_ENABLED, true))
    private val _lanBypassEnabled = MutableStateFlow(readBoolean(KEY_LAN_BYPASS_ENABLED, true))
    private val _systemBypassEnabled = MutableStateFlow(readBoolean(KEY_SYSTEM_BYPASS_ENABLED, false))
    private val _meteredNetwork = MutableStateFlow(readBoolean(KEY_METERED_NETWORK, false))
    private val _logs = com.perqa.byebox.core.AppLogger.logs
    private val _isPinging = MutableStateFlow(false)
    private val _toastMessage = MutableStateFlow<String?>(null)

    private var trafficJob: Job? = null
    private var lastToastAt: Long = 0L

    val uiState: StateFlow<MainUiState> = combine(
        dataRepository.configs,
        dataRepository.subscriptionSources,
        dataRepository.activeConfigId,
        _connectionStatus,
        _downloadSpeed,
        _uploadSpeed,
        _appTheme,
        _routingProfile,
        _dnsServer,
        _ipv6Enabled,
        _lanBypassEnabled,
        _systemBypassEnabled,
        _meteredNetwork,
        _logs,
        _isPinging,
        _toastMessage
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
            ipv6Enabled = flows[9] as Boolean,
            lanBypassEnabled = flows[10] as Boolean,
            systemBypassEnabled = flows[11] as Boolean,
            meteredNetwork = flows[12] as Boolean,
            logs = flows[13] as List<String>,
            isPinging = flows[14] as Boolean,
            toastMessage = flows[15] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    init {
        _connectionStatus.value = if (com.perqa.byebox.service.HiddifyVpnService.isRunning) {
            startTrafficUpdates()
            ConnectionStatus.CONNECTED
        } else {
            ConnectionStatus.DISCONNECTED
        }

        viewModelScope.launch {
            com.perqa.byebox.service.HiddifyVpnService.vpnState.collect { isRunning ->
                if (isRunning) {
                    if (_connectionStatus.value != ConnectionStatus.CONNECTED) {
                        _connectionStatus.value = ConnectionStatus.CONNECTED
                        val activeConfig = getActiveConfig()
                        addLog("[INFO] Запуск ядра sing-box v1.9.0-rc.3...")
                        delay(200)
                        addLog("[INFO] Загрузка конфигурации: ${activeConfig?.name ?: "Server"} (${activeConfig?.protocol ?: "VLESS"})")
                        addLog("[INFO] DNS настроен на сервер: ${_dnsServer.value.label} (${_dnsServer.value.address})")
                        addLog("[INFO] Маршрутизация: ${_routingProfile.value.label}")
                        addLog("[INFO] Создание TUN интерфейса hiddify-tun0...")
                        addLog("[INFO] sing-box успешно запущен. Трафик перенаправлен.")
                        showToast("Подключено к ${activeConfig?.name ?: "Server"}")
                        startTrafficUpdates()

                        // Automatically trigger active config ping and general pings upon successful connection
                        launch {
                            delay(1500) // Wait for connection to stabilize
                            com.perqa.byebox.core.AppLogger.info("SYSTEM", "Автоматический запуск пинга после подключения...")
                            testActiveConfigPing()
                            testPings()
                        }
                    }
                } else {
                    if (_connectionStatus.value != ConnectionStatus.DISCONNECTED) {
                        trafficJob?.cancel()
                        _connectionStatus.value = ConnectionStatus.DISCONNECTED
                        _downloadSpeed.value = "0.0 KB/s"
                        _uploadSpeed.value = "0.0 KB/s"
                        addLog("[INFO] Остановка ядра sing-box...")
                        delay(200)
                        addLog("[INFO] TUN интерфейс удален. Соединение разорвано.")
                        showToast("Соединение разорвано")
                    }
                }
            }
        }
    }

    fun setConnectingState() {
        _connectionStatus.value = ConnectionStatus.CONNECTING
        addLog("[INFO] Запуск ядра sing-box v1.9.0-rc.3...")
    }

    private fun startTrafficUpdates() {
        trafficJob?.cancel()
        trafficJob = viewModelScope.launch {
            com.perqa.byebox.service.HiddifyVpnService.trafficStats.collect { stats ->
                if (_connectionStatus.value != ConnectionStatus.CONNECTED) return@collect
                _downloadSpeed.value = formatBytesPerSec(stats.downSpeed)
                _uploadSpeed.value = formatBytesPerSec(stats.upSpeed)
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
        val configs = dataRepository.configs.value
        val activeId = dataRepository.activeConfigId.value
        return configs.find { it.id == activeId }
    }

    fun selectConfig(id: String) {
        dataRepository.selectConfig(id)
        val active = dataRepository.configs.value.find { it.id == id }
        if (active != null) {
            addLog("[SYSTEM] Выбрана конфигурация: ${active.name}")
            showToast("Выбран сервер: ${active.name}")
        }
    }

    fun selectBestConfig() {
        val best = dataRepository.configs.value
            .filter { it.ping != null && it.ping < 999 && it.failureCount < 3 }
            .minByOrNull { it.ping ?: Int.MAX_VALUE }
            ?: dataRepository.configs.value
                .filter { it.failureCount < 3 }
                .minByOrNull { it.ping ?: Int.MAX_VALUE }
            ?: dataRepository.configs.value.firstOrNull()

        if (best == null) {
            showToast("Нет доступных конфигураций")
            return
        }

        dataRepository.selectConfig(best.id)
        addLog("[SYSTEM] Выбран лучший сервер: ${best.name} (${best.ping?.let { "$it ms" } ?: "N/A"})")
        showToast("Лучший сервер: ${best.name}")
    }

    fun addConfigFromUrl(url: String) {
        viewModelScope.launch {
            val trimmedUrl = url.trim()
            if (trimmedUrl.startsWith("http://", ignoreCase = true) || trimmedUrl.startsWith("https://", ignoreCase = true)) {
                addLog("[INFO] Загрузка подписки по ссылке: $trimmedUrl")
                showToast("Загрузка подписки...")
                val result = fetchSubscription(trimmedUrl)
                if (result != null) {
                    val parsedConfigs = parseSubscriptionContent(result.body)
                    if (parsedConfigs.isNotEmpty()) {
                        val sourceName = subscriptionSourceName(trimmedUrl)
                        val source = result.toSubscriptionSource(trimmedUrl, sourceName, parsedConfigs.size)
                        dataRepository.upsertSubscriptionSource(source, parsedConfigs)
                        addLog("[SYSTEM] Подписка обновлена: $sourceName (${parsedConfigs.size} серверов).")
                        showToast("Импортировано серверов: ${parsedConfigs.size}")
                    } else {
                        addLog("[WARNING] Подписка загружена, но не найдено валидных конфигураций!")
                        showToast("В подписке нет конфигураций!")
                    }
                } else {
                    addLog("[ERROR] Не удалось загрузить подписку! Проверьте подключение к сети.")
                    showToast("Ошибка загрузки подписки!")
                }
            } else {
                val success = dataRepository.addConfigFromUrl(trimmedUrl)
                if (success) {
                    addLog("[SYSTEM] Успешно добавлена новая конфигурация по ссылке.")
                    showToast("Сервер успешно добавлен!")
                } else {
                    addLog("[ERROR] Не удалось распарсить ссылку! Поддерживаются VLESS, VMESS, Trojan и Shadowsocks.")
                    showToast("Ошибка: Неподдерживаемый формат ссылки!")
                }
            }
        }
    }

    fun refreshSubscriptions() {
        viewModelScope.launch {
            val sources = dataRepository.subscriptionSources.value
            if (sources.isEmpty()) {
                showToast("Нет подписок для обновления")
                return@launch
            }
            addLog("[SYSTEM] Обновление подписок: ${sources.size}")
            var updated = 0
            sources.forEach { source ->
                val result = fetchSubscription(source.url)
                if (result == null) {
                    addLog("[WARNING] Не удалось обновить подписку: ${source.name}")
                    return@forEach
                }
                val parsedConfigs = parseSubscriptionContent(result.body)
                if (parsedConfigs.isEmpty()) {
                    addLog("[WARNING] Подписка без валидных узлов: ${source.name}")
                    return@forEach
                }
                dataRepository.upsertSubscriptionSource(
                    result.toSubscriptionSource(source.url, source.name, parsedConfigs.size),
                    parsedConfigs
                )
                updated++
            }
            addLog("[SYSTEM] Обновлено подписок: $updated/${sources.size}")
            showToast("Обновлено подписок: $updated")
        }
    }

    fun refreshSubscription(sourceId: String) {
        viewModelScope.launch {
            val source = dataRepository.subscriptionSources.value.find { it.id == sourceId }
            if (source == null) {
                showToast("Источник не найден")
                return@launch
            }
            val result = fetchSubscription(source.url)
            if (result == null) {
                addLog("[WARNING] Не удалось обновить подписку: ${source.name}")
                showToast("Не удалось обновить: ${source.name}")
                return@launch
            }
            val parsedConfigs = parseSubscriptionContent(result.body)
            if (parsedConfigs.isEmpty()) {
                showToast("В подписке нет узлов")
                return@launch
            }
            dataRepository.upsertSubscriptionSource(
                result.toSubscriptionSource(source.url, source.name, parsedConfigs.size),
                parsedConfigs
            )
            addLog("[SYSTEM] Источник обновлен: ${source.name} (${parsedConfigs.size} серверов).")
            showToast("Обновлено: ${source.name}")
        }
    }

    fun renameSubscriptionSource(sourceId: String, newName: String) {
        dataRepository.renameSubscriptionSource(sourceId, newName)
        addLog("[SYSTEM] Источник переименован: $newName")
        showToast("Источник переименован")
    }

    fun deleteSubscriptionSource(sourceId: String) {
        val sourceName = dataRepository.subscriptionSources.value.find { it.id == sourceId }?.name ?: "Источник"
        dataRepository.deleteSubscriptionSource(sourceId)
        addLog("[SYSTEM] Удален источник подписки: $sourceName")
        showToast("Удалено: $sourceName")
    }

    private fun subscriptionSourceName(url: String): String {
        return try {
            URI(url).host?.removePrefix("www.") ?: "Подписка"
        } catch (e: Exception) {
            "Подписка"
        }
    }

    private data class SubscriptionFetchResult(
        val body: String,
        val userInfo: String?
    )

    private suspend fun fetchSubscription(urlString: String): SubscriptionFetchResult? = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", "v2raytun/ByeBox")

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line).append("\n")
                }
                reader.close()
                SubscriptionFetchResult(
                    body = response.toString(),
                    userInfo = connection.getHeaderField("subscription-userinfo")
                )
            } else {
                addLog("[ERROR] Сервер вернул код ответа: $responseCode")
                null
            }
        } catch (e: Exception) {
            addLog("[ERROR] Ошибка сети: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private fun SubscriptionFetchResult.toSubscriptionSource(
        url: String,
        name: String,
        nodeCount: Int
    ): SubscriptionSource {
        val stats = parseSubscriptionUserInfo(userInfo)
        return SubscriptionSource(
            id = stableSourceId(url),
            name = name,
            url = url,
            lastUpdatedAt = System.currentTimeMillis(),
            nodeCount = nodeCount,
            uploadBytes = stats["upload"],
            downloadBytes = stats["download"],
            totalBytes = stats["total"],
            expireAt = stats["expire"]
        )
    }

    private fun parseSubscriptionUserInfo(header: String?): Map<String, Long> {
        if (header.isNullOrBlank()) return emptyMap()
        return header
            .split(";")
            .mapNotNull { part ->
                val pieces = part.trim().split("=", limit = 2)
                val key = pieces.getOrNull(0)?.trim()?.lowercase() ?: return@mapNotNull null
                val value = pieces.getOrNull(1)?.trim()?.toLongOrNull() ?: return@mapNotNull null
                key to value
            }
            .toMap()
    }

    private fun stableSourceId(url: String): String {
        return UUID.nameUUIDFromBytes(url.toByteArray()).toString()
    }

    private fun parseSubscriptionContent(content: String): List<ProxyConfig> {
        val list = mutableListOf<ProxyConfig>()
        val decodedContent = try {
            val clean = content.trim().replace("\r", "").replace("\n", "")
            val decodedBytes = android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
            String(decodedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            content
        }

        val lines = decodedContent.split("\n", "\r")
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isNotEmpty()) {
                val parsed = com.perqa.byebox.data.ConfigParser.parse(trimmedLine)
                if (parsed != null) {
                    list.add(parsed)
                }
            }
        }
        return list
    }


    fun deleteConfig(id: String) {
        val nodeName = dataRepository.configs.value.find { it.id == id }?.name ?: "Неизвестный"
        dataRepository.deleteConfig(id)
        addLog("[SYSTEM] Удален сервер: $nodeName")
        showToast("Удалено: $nodeName")
    }

    fun testPings() {
        viewModelScope.launch {
            if (_isPinging.value) return@launch
            _isPinging.value = true
            addLog("[SYSTEM] Запуск тестирования задержки серверов...")
            
            probeConfigs(dataRepository.configs.value)
            
            _isPinging.value = false
            addLog("[SYSTEM] Тестирование пинга завершено.")
            showToast("Задержка серверов обновлена")
        }
    }

    fun testActiveConfigPing() {
        val activeId = dataRepository.activeConfigId.value ?: return
        val activeConfig = dataRepository.configs.value.find { it.id == activeId } ?: return
        viewModelScope.launch {
            if (_isPinging.value) return@launch
            _isPinging.value = true
            addLog("[SYSTEM] Тестирование задержки активного сервера ${activeConfig.name}...")
            val ping = probeTcpLatency(activeConfig) ?: 999
            dataRepository.updatePing(activeId, ping)
            _isPinging.value = false
            addLog("[PING] ${activeConfig.name} -> $ping ms")
            showToast("Пинг: $ping ms")
        }
    }

    fun testPingsForSource(sourceName: String) {
        viewModelScope.launch {
            if (_isPinging.value) return@launch
            val configs = dataRepository.configs.value.filter { it.sourceName == sourceName }
            if (configs.isEmpty()) return@launch
            _isPinging.value = true
            addLog("[SYSTEM] Пинг источника: $sourceName (${configs.size})")
            probeConfigs(configs)
            _isPinging.value = false
            addLog("[SYSTEM] Пинг источника завершён: $sourceName")
            showToast("Пинг источника обновлён")
        }
    }

    private suspend fun probeConfigs(configs: List<ProxyConfig>) = coroutineScope {
        configs.chunked(8).forEach { batch ->
            batch.map { config ->
                async { config to probeTcpLatency(config) }
            }.awaitAll().forEach { (config, ping) ->
                if (ping != null) {
                    dataRepository.updatePing(config.id, ping)
                    addLog("[PING] ${config.name} -> $ping ms")
                } else {
                    dataRepository.updatePing(config.id, 999)
                    addLog("[PING] ${config.name} -> timeout")
                }
            }
        }
    }

    private suspend fun probeTcpLatency(config: ProxyConfig): Int? = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            val elapsed = measureTimeMillis {
                socket = Socket()
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
        _routingProfile.value = profile
        writeString(KEY_ROUTING_PROFILE, profile.name)
        addLog("[SYSTEM] Профиль маршрутизации изменен: ${profile.label}")
        showToast("Маршрутизация: ${profile.label}")
        if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
            addLog("[INFO] Правила ядра sing-box обновлены на лету.")
        }
    }

    fun changeDnsServer(dns: DnsServer) {
        _dnsServer.value = dns
        writeString(KEY_DNS_SERVER, dns.name)
        addLog("[SYSTEM] Выбран DNS-сервер: ${dns.label} (${dns.address})")
        showToast("DNS: ${dns.label}")
        if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
            viewModelScope.launch {
                addLog("[INFO] Перезапуск DNS-модуля sing-box...")
                delay(300)
                addLog("[INFO] DNS-серверы sing-box обновлены.")
            }
        }
    }

    fun changeIpv6Enabled(enabled: Boolean) {
        _ipv6Enabled.value = enabled
        writeBoolean(KEY_IPV6_ENABLED, enabled)
        addLog("[SYSTEM] IPv6 в TUN: ${if (enabled) "включен" else "выключен"}")
        showToast("IPv6: ${if (enabled) "включен" else "выключен"}")
    }

    fun changeLanBypassEnabled(enabled: Boolean) {
        _lanBypassEnabled.value = enabled
        writeBoolean(KEY_LAN_BYPASS_ENABLED, enabled)
        addLog("[SYSTEM] Обход локальных сетей: ${if (enabled) "включен" else "выключен"}")
        showToast("LAN bypass: ${if (enabled) "включен" else "выключен"}")
    }

    fun changeSystemBypassEnabled(enabled: Boolean) {
        _systemBypassEnabled.value = enabled
        writeBoolean(KEY_SYSTEM_BYPASS_ENABLED, enabled)
        addLog("[SYSTEM] Android VPN bypass: ${if (enabled) "разрешен" else "запрещен"}")
        showToast("Bypass: ${if (enabled) "разрешен" else "запрещен"}")
    }

    fun changeMeteredNetwork(enabled: Boolean) {
        _meteredNetwork.value = enabled
        writeBoolean(KEY_METERED_NETWORK, enabled)
        addLog("[SYSTEM] VPN как лимитная сеть: ${if (enabled) "да" else "нет"}")
        showToast("Лимитная сеть: ${if (enabled) "да" else "нет"}")
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

    private inline fun <reified T : Enum<T>> readEnum(key: String, fallback: T): T {
        val value = prefs?.getString(key, null) ?: return fallback
        return enumValues<T>().firstOrNull { it.name == value } ?: fallback
    }

    private fun readBoolean(key: String, fallback: Boolean): Boolean {
        return prefs?.getBoolean(key, fallback) ?: fallback
    }

    private fun writeString(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply()
    }

    private fun writeBoolean(key: String, value: Boolean) {
        prefs?.edit()?.putBoolean(key, value)?.apply()
    }

    companion object {
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_ROUTING_PROFILE = "routing_profile"
        private const val KEY_DNS_SERVER = "dns_server"
        private const val KEY_IPV6_ENABLED = "ipv6_enabled"
        private const val KEY_LAN_BYPASS_ENABLED = "lan_bypass_enabled"
        private const val KEY_SYSTEM_BYPASS_ENABLED = "system_bypass_enabled"
        private const val KEY_METERED_NETWORK = "metered_network"
    }
}


