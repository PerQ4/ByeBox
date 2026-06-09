package com.perqa.byebox.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.net.TrafficStats as AndroidTrafficStats
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.perqa.byebox.core.BoxService
import com.perqa.byebox.core.SingBoxConfigGenerator
import com.perqa.byebox.core.SingBoxOptions
import com.perqa.byebox.core.TrafficStats
import com.perqa.byebox.data.ProxyConfig
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

enum class CoreRuntimeState {
    IDLE, MISSING, CONFIG_READY, STARTING, RUNNING, RECONNECTING, FAILED, STOPPED
}

class HiddifyVpnService : VpnService(), Runnable {
    private var vpnThread: Thread? = null
    var vpnInterface: ParcelFileDescriptor? = null
    private var dnsAddress: String = "8.8.8.8"
    var serverName: String = "Server"
    private var serverEndpoint: String = ""
    private var protocol: String = "PROXY"
    private var routingProfile: String = "BYPASS_LAN_CN_RU"
    var ipv6Enabled: Boolean = false
    private var lanBypassEnabled: Boolean = true
    var systemBypassEnabled: Boolean = false
    var meteredNetwork: Boolean = false
    private var appRoutingMode: String = "OFF"
    private var appRoutingPackages: List<String> = emptyList()
    private var tunStack: String = "mixed"
    var httpProxyEnabled: Boolean = false
        private set
    private var activeConfig: ProxyConfig? = null
    private var boxService: BoxService? = null
    private var statsThread: Thread? = null
    private var statsEnabled: Boolean = false
    private var statsPort: Int = 0
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    @Volatile
    private var userStopRequested = false

    @Volatile
    private var networkChanged = false

    private data class TrafficSnapshot(
        val downBytes: Long,
        val upBytes: Long,
        val source: String
    )

    companion object {
        private val _vpnState = MutableStateFlow(false)
        val vpnState: StateFlow<Boolean> = _vpnState.asStateFlow()
        private val _coreState = MutableStateFlow(CoreRuntimeState.IDLE)
        val coreState: StateFlow<CoreRuntimeState> = _coreState.asStateFlow()
        private val _trafficStats = MutableStateFlow(TrafficStats())
        val trafficStats: StateFlow<TrafficStats> = _trafficStats.asStateFlow()

        @JvmStatic
        var isRunning = false
            set(value) {
                field = value
                _vpnState.value = value
            }

        private fun setCoreState(state: CoreRuntimeState) {
            _coreState.value = state
            com.perqa.byebox.core.AppLogger.core("Состояние ядра изменилось на: $state")
        }

        private fun appendCoreLog(message: String) {
            com.perqa.byebox.core.AppLogger.core(message)
        }

        private var lastTrafficDown: Long = 0L
        private var lastTrafficUp: Long = 0L
        private var sessionStartDown: Long = 0L
        private var sessionStartUp: Long = 0L
        private var lastTrafficTime: Long = 0L

        private fun resetTrafficBaseline(down: Long = 0L, up: Long = 0L) {
            lastTrafficDown = down
            lastTrafficUp = up
            sessionStartDown = down
            sessionStartUp = up
            lastTrafficTime = System.currentTimeMillis()
            _trafficStats.value = TrafficStats(downBytes = 0L, upBytes = 0L)
        }

        private fun updateTraffic(down: Long, up: Long): TrafficStats {
            val now = System.currentTimeMillis()
            val dt = (now - lastTrafficTime).coerceAtLeast(1)
            val prevDown = lastTrafficDown
            val prevUp = lastTrafficUp
            lastTrafficDown = down
            lastTrafficUp = up
            lastTrafficTime = now
            val stats = TrafficStats(
                downBytes = (down - sessionStartDown).coerceAtLeast(0L),
                upBytes = (up - sessionStartUp).coerceAtLeast(0L),
                downSpeed = ((down - prevDown) * 1000L / dt).coerceAtLeast(0),
                upSpeed = ((up - prevUp) * 1000L / dt).coerceAtLeast(0)
            )
            _trafficStats.value = stats
            return stats
        }

        const val ACTION_CONNECT = "com.perqa.byebox.service.CONNECT"
        const val ACTION_DISCONNECT = "com.perqa.byebox.service.DISCONNECT"
        const val EXTRA_CONFIG_JSON = "config_json"
        const val EXTRA_DNS_ADDRESS = "dns_address"
        const val EXTRA_ROUTING_PROFILE = "routing_profile"
        const val EXTRA_IPV6_ENABLED = "ipv6_enabled"
        const val EXTRA_LAN_BYPASS_ENABLED = "lan_bypass_enabled"
        const val EXTRA_SYSTEM_BYPASS_ENABLED = "system_bypass_enabled"
        const val EXTRA_METERED_NETWORK = "metered_network"
        const val EXTRA_APP_ROUTING_MODE = "app_routing_mode"
        const val EXTRA_APP_ROUTING_PACKAGES = "app_routing_packages"
        const val EXTRA_TUN_STACK = "tun_stack"
        const val EXTRA_HTTP_PROXY_ENABLED = "http_proxy_enabled"

        const val PREFS_NAME = "byebox_vpn"
        const val PREF_SERVER_NAME = "server_name"
        const val PREF_CONFIG_JSON = "config_json"
        const val PREF_DNS_ADDRESS = "dns_address"
        const val PREF_ROUTING_PROFILE = "routing_profile"
        const val PREF_IPV6_ENABLED = "ipv6_enabled"
        const val PREF_LAN_BYPASS_ENABLED = "lan_bypass_enabled"
        const val PREF_SYSTEM_BYPASS_ENABLED = "system_bypass_enabled"
        const val PREF_METERED_NETWORK = "metered_network"
        const val PREF_APP_ROUTING_MODE = "app_routing_mode"
        const val PREF_APP_ROUTING_PACKAGES = "app_routing_packages"
        const val PREF_TUN_STACK = "tun_stack"
        const val PREF_HTTP_PROXY_ENABLED = "http_proxy_enabled"

        val PUBLIC_IPV4_ROUTES = listOf(
            "1.0.0.0" to 8, "2.0.0.0" to 7, "4.0.0.0" to 6, "8.0.0.0" to 7,
            "11.0.0.0" to 8, "12.0.0.0" to 6, "16.0.0.0" to 4, "32.0.0.0" to 3,
            "64.0.0.0" to 2, "128.0.0.0" to 3, "160.0.0.0" to 5, "168.0.0.0" to 6,
            "172.0.0.0" to 12, "172.32.0.0" to 11, "172.64.0.0" to 10, "172.128.0.0" to 9,
            "173.0.0.0" to 8, "174.0.0.0" to 7, "176.0.0.0" to 4, "192.0.0.0" to 9,
            "192.128.0.0" to 11, "192.160.0.0" to 13, "192.169.0.0" to 16, "192.170.0.0" to 15,
            "192.172.0.0" to 14, "192.176.0.0" to 12, "192.192.0.0" to 10, "193.0.0.0" to 8,
            "194.0.0.0" to 7, "196.0.0.0" to 6, "200.0.0.0" to 5, "208.0.0.0" to 4
        )
    }

    override fun onCreate() {
        super.onCreate()
        com.perqa.byebox.core.AppLogger.init(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            if (ACTION_CONNECT == action) {
                val configJson = intent.getStringExtra(EXTRA_CONFIG_JSON) ?: "{}"
                val config = try {
                    ProxyConfig.fromJson(JSONObject(configJson))
                } catch (e: Exception) {
                    Log.e("HiddifyVpnService", "Failed to parse config: ${e.message}")
                    null
                }
                val dnsAddr = intent.getStringExtra(EXTRA_DNS_ADDRESS) ?: "8.8.8.8"
                val routing = intent.getStringExtra(EXTRA_ROUTING_PROFILE) ?: "BYPASS_LAN_CN_RU"
                val ipv6 = intent.getBooleanExtra(EXTRA_IPV6_ENABLED, false)
                val lanBypass = intent.getBooleanExtra(EXTRA_LAN_BYPASS_ENABLED, true)
                val systemBypass = intent.getBooleanExtra(EXTRA_SYSTEM_BYPASS_ENABLED, false)
                val metered = intent.getBooleanExtra(EXTRA_METERED_NETWORK, false)
                val appMode = intent.getStringExtra(EXTRA_APP_ROUTING_MODE) ?: "OFF"
                val appPackages = intent.getStringExtra(EXTRA_APP_ROUTING_PACKAGES).orEmpty()
                val tunStackVal = intent.getStringExtra(EXTRA_TUN_STACK) ?: "mixed"
                val httpProxy = intent.getBooleanExtra(EXTRA_HTTP_PROXY_ENABLED, false)
                startVpn(config, dnsAddr, routing, ipv6, lanBypass, systemBypass, metered, appMode, appPackages, tunStackVal, httpProxy)
            } else if (ACTION_DISCONNECT == action) {
                stopVpn()
            }
        } else {
            // Always-On VPN: Android restarts the service with null intent.
            // Load last saved connection from SharedPreferences.
            Log.i("HiddifyVpnService", "Always-On restart: loading saved config")
            startVpnFromSavedPrefs()
        }
        return START_STICKY
    }

    private fun startVpnFromSavedPrefs() {
        val vpnPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val configJson = vpnPrefs.getString(PREF_CONFIG_JSON, null) ?: run {
            Log.w("HiddifyVpnService", "No saved config — cannot restart")
            stopSelf()
            return
        }
        val config = try {
            ProxyConfig.fromJson(JSONObject(configJson))
        } catch (e: Exception) {
            Log.e("HiddifyVpnService", "Failed to parse saved config on always-on restart", e)
            stopSelf()
            return
        }
        if (prepare(this) != null) {
            Log.w("HiddifyVpnService", "VPN permission missing on always-on restart")
            stopSelf()
            return
        }
        val dnsAddr = vpnPrefs.getString(PREF_DNS_ADDRESS, "8.8.8.8") ?: "8.8.8.8"
        val routing = vpnPrefs.getString(PREF_ROUTING_PROFILE, "BYPASS_LAN_CN_RU") ?: "BYPASS_LAN_CN_RU"
        val ipv6 = vpnPrefs.getBoolean(PREF_IPV6_ENABLED, false)
        val lanBypass = vpnPrefs.getBoolean(PREF_LAN_BYPASS_ENABLED, true)
        val systemBypass = vpnPrefs.getBoolean(PREF_SYSTEM_BYPASS_ENABLED, false)
        val metered = vpnPrefs.getBoolean(PREF_METERED_NETWORK, false)
        val appMode = vpnPrefs.getString(PREF_APP_ROUTING_MODE, "OFF") ?: "OFF"
        val appPackages = vpnPrefs.getString(PREF_APP_ROUTING_PACKAGES, "") ?: ""
        val tunStackVal = vpnPrefs.getString(PREF_TUN_STACK, "mixed") ?: "mixed"
        val httpProxy = vpnPrefs.getBoolean(PREF_HTTP_PROXY_ENABLED, false)
        startVpn(config, dnsAddr, routing, ipv6, lanBypass, systemBypass, metered, appMode, appPackages, tunStackVal, httpProxy)
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    override fun onRevoke() {
        userStopRequested = true
        stopVpn()
        super.onRevoke()
    }

    private fun startVpn(
        config: ProxyConfig?,
        dnsAddr: String,
        routing: String,
        ipv6: Boolean,
        lanBypass: Boolean,
        systemBypass: Boolean,
        metered: Boolean,
        appMode: String,
        appPackages: String,
        tunStackVal: String = "mixed",
        httpProxy: Boolean = false
    ) {
        if (config == null || config.address.isBlank() || config.port <= 0) {
            setCoreState(CoreRuntimeState.FAILED)
            appendCoreLog("No valid active config; VPN start aborted")
            Log.e("HiddifyVpnService", "No valid active config; VPN start aborted")
            stopSelf()
            return
        }
        if (prepare(this) != null) {
            setCoreState(CoreRuntimeState.FAILED)
            appendCoreLog("VPN permission is missing; start aborted")
            Log.e("HiddifyVpnService", "VPN permission is missing; start aborted")
            stopSelf()
            return
        }
        stopVpn()
        userStopRequested = false
        isRunning = false
        this.activeConfig = config
        this.serverName = config.name
        this.serverEndpoint = "${config.address}:${config.port}"
        this.protocol = config.protocol
        this.dnsAddress = dnsAddr
        this.routingProfile = routing
        this.ipv6Enabled = ipv6
        this.lanBypassEnabled = lanBypass
        this.systemBypassEnabled = systemBypass
        this.meteredNetwork = metered
        this.appRoutingMode = appMode
        this.appRoutingPackages = parsePackageList(appPackages)
        this.tunStack = tunStackVal
        this.httpProxyEnabled = httpProxy
        saveLastConnection(config, dnsAddr, routing, false, lanBypass, systemBypass, metered, appMode, appPackages, tunStackVal, httpProxy)

        createVpnNotification()
        val notification = buildVpnNotification(isConnecting = true)

        try {
            VpnNotification.startForeground(this, notification)
        } catch (e: Exception) {
            setCoreState(CoreRuntimeState.FAILED)
            appendCoreLog("startForeground failed: ${e.message}")
            Log.e("HiddifyVpnService", "startForeground failed", e)
            stopSelf()
            return
        }

        vpnThread = Thread(this, "HiddifyVPNThread").apply { start() }
        registerNetworkCallback()
        updateTile()
    }

    private fun registerNetworkCallback() {
        unregisterNetworkCallback()
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (isRunning) {
                        Log.i("HiddifyVpnService", "Network available: $network — triggering reconnect")
                        networkChanged = true
                    }
                }

                override fun onLost(network: Network) {
                    if (isRunning) {
                        Log.i("HiddifyVpnService", "Network lost: $network — triggering reconnect")
                        networkChanged = true
                    }
                }

                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    if (isRunning) {
                        Log.d("HiddifyVpnService", "Network capabilities changed: $network")
                        networkChanged = true
                    }
                }
            }
            cm.registerNetworkCallback(request, networkCallback!!)
            Log.i("HiddifyVpnService", "Network callback registered")
        } catch (e: Exception) {
            Log.e("HiddifyVpnService", "Failed to register network callback", e)
        }
    }

    private fun unregisterNetworkCallback() {
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            networkCallback?.let { cm.unregisterNetworkCallback(it) }
        } catch (_: Exception) {}
        networkCallback = null
    }

    private fun saveLastConnection(
        config: ProxyConfig?,
        dnsAddr: String,
        routing: String,
        ipv6: Boolean,
        lanBypass: Boolean,
        systemBypass: Boolean,
        metered: Boolean,
        appMode: String,
        appPackages: String,
        tunStackVal: String = "mixed",
        httpProxy: Boolean = false
    ) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_CONFIG_JSON, config?.toJson()?.toString() ?: "{}")
            .putString(PREF_DNS_ADDRESS, dnsAddr)
            .putString(PREF_ROUTING_PROFILE, routing)
            .putBoolean(PREF_IPV6_ENABLED, ipv6)
            .putBoolean(PREF_LAN_BYPASS_ENABLED, lanBypass)
            .putBoolean(PREF_SYSTEM_BYPASS_ENABLED, systemBypass)
            .putBoolean(PREF_METERED_NETWORK, metered)
            .putString(PREF_APP_ROUTING_MODE, appMode)
            .putString(PREF_APP_ROUTING_PACKAGES, appPackages)
            .putString(PREF_TUN_STACK, tunStackVal)
            .putBoolean(PREF_HTTP_PROXY_ENABLED, httpProxy)
            .apply()
    }

    private fun startStatsPolling() {
        stopStatsPolling()
        readTrafficSnapshot()?.let {
            HiddifyVpnService.resetTrafficBaseline(it.downBytes, it.upBytes)
        } ?: HiddifyVpnService.resetTrafficBaseline()
        statsThread = Thread({
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val snapshot = readTrafficSnapshot()
                    if (snapshot != null) {
                        val stats = HiddifyVpnService.updateTraffic(snapshot.downBytes, snapshot.upBytes)
                        updateTrafficNotification(stats)
                    }
                } catch (e: Exception) {
                    Log.d("HiddifyVpnService", "Traffic polling failed: ${e.message}")
                }
                try {
                    Thread.sleep(1000)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }, "SingBoxStats").apply { isDaemon = true; start() }
    }

    private fun readTrafficSnapshot(): TrafficSnapshot? {
        return readClashConnectionTotals()
            ?: readTunInterfaceTotals()
            ?: readDeviceTrafficTotals()
    }

    private fun readClashConnectionTotals(): TrafficSnapshot? {
        if (!statsEnabled) return null
        var conn: HttpURLConnection? = null
        return try {
            val url = URL("http://127.0.0.1:$statsPort/connections")
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 700
                readTimeout = 700
                requestMethod = "GET"
            }
            if (conn.responseCode != 200) return null
            val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val json = JSONObject(body)
            val down = json.optLong("downloadTotal", 0L)
            val up = json.optLong("uploadTotal", 0L)
            if (down > 0L || up > 0L) TrafficSnapshot(down, up, "clash") else null
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun readTunInterfaceTotals(): TrafficSnapshot? {
        return try {
            File("/proc/net/dev")
                .readLines()
                .asSequence()
                .mapNotNull { line ->
                    val separator = line.indexOf(':')
                    if (separator <= 0) return@mapNotNull null
                    val iface = line.substring(0, separator).trim()
                    if (!iface.startsWith("tun")) return@mapNotNull null
                    val fields = line.substring(separator + 1)
                        .trim()
                        .split(Regex("\\s+"))
                    if (fields.size < 16) return@mapNotNull null
                    val down = fields[0].toLongOrNull() ?: return@mapNotNull null
                    val up = fields[8].toLongOrNull() ?: return@mapNotNull null
                    if (down <= 0L && up <= 0L) return@mapNotNull null
                    TrafficSnapshot(down, up, iface)
                }
                .maxByOrNull { it.downBytes + it.upBytes }
        } catch (_: Exception) {
            null
        }
    }

    private fun readDeviceTrafficTotals(): TrafficSnapshot? {
        val down = AndroidTrafficStats.getTotalRxBytes()
        val up = AndroidTrafficStats.getTotalTxBytes()
        return if (down >= 0L && up >= 0L) TrafficSnapshot(down, up, "device") else null
    }

    private fun stopStatsPolling() {
        statsThread?.interrupt()
        statsThread = null
    }

    private fun updateTrafficNotification(stats: TrafficStats) {
        val trafficLine = "${formatSpeed(stats.downSpeed)} down  ${formatSpeed(stats.upSpeed)} up  |  ${formatBytes(stats.downBytes)} down  ${formatBytes(stats.upBytes)} up"
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(VpnNotification.NOTIFICATION_ID, buildVpnNotification(trafficLine))
        } catch (e: Exception) {
            Log.d("HiddifyVpnService", "Traffic notification update failed: ${e.message}")
        }
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        val value = when {
            bytesPerSec >= 1_048_576L -> String.format(java.util.Locale.US, "%.1f MB/s", bytesPerSec / 1_048_576.0)
            bytesPerSec >= 1024L -> String.format(java.util.Locale.US, "%.1f KB/s", bytesPerSec / 1024.0)
            else -> "$bytesPerSec B/s"
        }
        return value
    }

    private fun formatBytes(bytes: Long): String {
        val value = when {
            bytes >= 1_073_741_824L -> String.format(java.util.Locale.US, "%.2f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576L -> String.format(java.util.Locale.US, "%.1f MB", bytes / 1_048_576.0)
            bytes >= 1024L -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
        return value
    }

    private fun stopVpn() {
        userStopRequested = true
        isRunning = false
        cleanupRuntimeResources()
        setCoreState(CoreRuntimeState.STOPPED)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            Log.d("HiddifyVpnService", "stopForeground failed: ${e.message}")
        }
        val thread = vpnThread
        if (thread != null && thread != Thread.currentThread()) {
            thread.interrupt()
        }
        vpnThread = null
        updateTile()
    }

    private fun cleanupRuntimeResources() {
        stopStatsPolling()
        unregisterNetworkCallback()
        boxService?.close()
        boxService = null
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.d("HiddifyVpnService", "VPN interface close failed: ${e.message}")
        }
        vpnInterface = null
    }

    private fun updateTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                android.service.quicksettings.TileService.requestListeningState(
                    this,
                    android.content.ComponentName(this, ByeBoxTileService::class.java)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createVpnNotification() {
        VpnNotification.createChannel(this)
    }

    private fun buildVpnNotification(trafficLine: String? = null, isConnecting: Boolean = false): Notification {
        val disconnectIntent = Intent(this, HiddifyVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this, 1, disconnectIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (isConnecting) "🟡 ByeBox VPN · $serverName (Подключение)" else "🟢 ByeBox VPN · $serverName"
        val content = trafficLine ?: if (isConnecting) "Соединение..." else "Соединение установлено · Защищено"
        val subText = listOfNotNull(protocol, serverEndpoint, routingLabel(routingProfile))
            .filter { it.isNotBlank() }.joinToString(" · ")

        return VpnNotification.build(this, title, content, subText, disconnectPendingIntent)
    }

    override fun run() {
        var retryCount = 0
        val maxRetries = 5
        val maxRetryDelay = 30000L
        var retryDelay = 2000L
        var configLogged = false

        while (!Thread.currentThread().isInterrupted && !userStopRequested) {
            try {
                val config = activeConfig ?: run {
                    Log.e("HiddifyVpnService", "No active config — cannot start VPN")
                    isRunning = false
                    updateTile()
                    return
                }

                setCoreState(CoreRuntimeState.CONFIG_READY)

                val configJson = SingBoxConfigGenerator.generate(
                    activeConfig = config,
                    options = SingBoxOptions(
                        dnsAddress = dnsAddress,
                        routingProfile = routingProfile,
                        ipv6Enabled = ipv6Enabled,
                        lanBypassEnabled = lanBypassEnabled,
                        appRoutingMode = appRoutingMode,
                        appRoutingPackages = appRoutingPackages,
                        statsEnabled = false,
                        tunStack = tunStack,
                        httpProxyEnabled = httpProxyEnabled
                    )
                )
                statsEnabled = false
                statsPort = 0
                if (!configLogged) {
                    appendCoreLog("sing-box config generated")
                    com.perqa.byebox.core.AppLogger.info("HiddifyVpnService", "sing-box config:\n$configJson")
                    configLogged = true
                } else {
                    appendCoreLog("sing-box config regenerated")
                }

                setCoreState(CoreRuntimeState.STARTING)
                appendCoreLog("Starting sing-box via libbox...")

                val svc = BoxService(this)
                boxService = svc
                svc.setup()

                val result = svc.start(configJson, this)
                if (result.isSuccess) {
                    isRunning = true
                    setCoreState(CoreRuntimeState.RUNNING)
                    appendCoreLog("sing-box running (libbox)")
                    startStatsPolling()

                    // Reset reconnect delay on successful start
                    retryDelay = 2000L
                    retryCount = 0

                    // Wait until interrupted, service stops, or network changes
                    while (!Thread.currentThread().isInterrupted && !userStopRequested && isRunning) {
                        if (networkChanged) {
                            networkChanged = false
                            Log.i("HiddifyVpnService", "Network changed — restarting VPN")
                            appendCoreLog("Сеть изменилась — перезапуск VPN...")
                            break
                        }
                        try {
                            Thread.sleep(1000)
                        } catch (e: InterruptedException) {
                            break
                        }
                    }
                } else {
                    val ex = result.exceptionOrNull()
                    setCoreState(CoreRuntimeState.FAILED)
                    val msg = "sing-box failed: ${ex?.message ?: "unknown error"}"
                    appendCoreLog(msg)
                    Log.e("HiddifyVpnService", msg, ex)
                    isRunning = false
                    updateTile()
                }

            } catch (e: InterruptedException) {
                Log.d("HiddifyVpnService", "VPN thread interrupted")
                break
            } catch (e: Exception) {
                val msg = "VPN error: ${e.javaClass.simpleName}: ${e.message}"
                setCoreState(CoreRuntimeState.FAILED)
                Log.e("HiddifyVpnService", msg, e)
                appendCoreLog(msg)
            } finally {
                // Cleanup current attempt resources
                cleanupRuntimeResources()
            }

            if (userStopRequested) {
                break
            }

            if (retryCount >= maxRetries) {
                setCoreState(CoreRuntimeState.FAILED)
                appendCoreLog("Reconnect stopped after $maxRetries failed attempts")
                break
            }

            // Wait for OS to release the port (e.g. clash_api :9090) before retrying
            try {
                Thread.sleep(1500)
            } catch (_: InterruptedException) {
                break
            }

            // Exponential backoff
            val retryDelaySec = retryDelay / 1000
            setCoreState(CoreRuntimeState.RECONNECTING)
            appendCoreLog("Connection interrupted. Reconnect ${retryCount + 1}/$maxRetries in $retryDelaySec sec...")
            try {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(VpnNotification.NOTIFICATION_ID, buildVpnNotification("Reconnect ${retryCount + 1}/$maxRetries in $retryDelaySec sec...", isConnecting = true))
            } catch (e: Exception) {
                Log.d("HiddifyVpnService", "Reconnect notification update failed: ${e.message}")
            }

            try {
                Thread.sleep(retryDelay)
            } catch (e: InterruptedException) {
                break
            }
            retryDelay = (retryDelay * 1.5).toLong().coerceAtMost(maxRetryDelay)
            retryCount++
        }

        vpnThread = null
        if (!userStopRequested) {
            isRunning = false
            setCoreState(CoreRuntimeState.STOPPED)
            updateTile()
            stopSelf()
        }
    }

    private fun routingLabel(routing: String): String {
        return when (routing) {
            "BYPASS_LAN_CN_RU" -> "LAN bypass"
            "PROXY_ALL" -> "Full tunnel"
            "DIRECT" -> "Direct"
            "BLOCK_ADS" -> "AdGuard DNS"
            else -> routing
        }
    }

    private fun parsePackageList(value: String): List<String> {
        return value
            .split(',', '\n', '\r', ';', ' ', '\t')
            .map { it.trim() }
            .filter { it.isNotBlank() && it != packageName }
            .distinct()
    }
}
