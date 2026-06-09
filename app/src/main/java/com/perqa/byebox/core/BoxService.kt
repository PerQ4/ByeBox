package com.perqa.byebox.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import android.os.Process
import android.system.OsConstants
import android.util.Log
import com.perqa.byebox.service.HiddifyVpnService
import io.nekohasekai.libbox.CommandServer
import io.nekohasekai.libbox.CommandServerHandler
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.PlatformInterface
import io.nekohasekai.libbox.TunOptions
import io.nekohasekai.libbox.StringIterator
import io.nekohasekai.libbox.OverrideOptions
import io.nekohasekai.libbox.WIFIState
import io.nekohasekai.libbox.NetworkInterfaceIterator
import io.nekohasekai.libbox.NetworkInterface
import io.nekohasekai.libbox.Notification
import io.nekohasekai.libbox.ConnectionOwner
import io.nekohasekai.libbox.Int32Iterator
import io.nekohasekai.libbox.InterfaceUpdateListener
import io.nekohasekai.libbox.NeighborUpdateListener
import io.nekohasekai.libbox.RoutePrefixIterator
import io.nekohasekai.libbox.ShellSession
import io.nekohasekai.libbox.PlatformUser
import io.nekohasekai.libbox.LocalDNSTransport
import io.nekohasekai.libbox.SystemProxyStatus
import io.nekohasekai.libbox.SetupOptions
import java.io.File
import java.net.InetSocketAddress
import java.net.NetworkInterface as JavaNetInterface

class MyStringIterator(private val list: List<String>) : StringIterator {
    private var index = 0
    override fun hasNext(): Boolean = index < list.size
    override fun len(): Int = list.size
    override fun next(): String {
        if (!hasNext()) throw NoSuchElementException()
        return list[index++]
    }
}

class MyNetworkInterfaceIterator(private val list: List<NetworkInterface>) : NetworkInterfaceIterator {
    private var index = 0
    override fun hasNext(): Boolean = index < list.size
    override fun next(): NetworkInterface {
        if (!hasNext()) throw NoSuchElementException()
        return list[index++]
    }
}

class BoxService(private val context: Context) {
    companion object {
        private const val TAG = "BoxService"
        private var loaded = false

        fun loadNativeLibrary() {
            if (!loaded) {
                try {
                    System.loadLibrary("box")
                    loaded = true
                    Log.i(TAG, "libbox.so loaded successfully")
                } catch (e: UnsatisfiedLinkError) {
                    Log.e(TAG, "Failed to load libbox.so: ${e.message}", e)
                    throw e
                }
            }
        }
    }

    private var commandServer: CommandServer? = null
    private var platformImpl: PlatformInterfaceImpl? = null
    private val basePath: File by lazy {
        context.getDir("box_base", Context.MODE_PRIVATE).also { it.mkdirs() }
    }
    private val workingDir: File by lazy {
        File(basePath, "work").also { it.mkdirs() }
    }
    private val tempDir: File by lazy {
        File(context.cacheDir, "box_temp").also { it.mkdirs() }
    }

    fun setup() {
        System.setProperty("GODEBUG", "efence=1,stacktraceback=2")
        System.setProperty("GOGC", "off")
        loadNativeLibrary()
        runCatching { Libbox.prepareCrashSignalHandlers() }
        Libbox.setup(SetupOptions().apply {
            this.basePath = this@BoxService.basePath.absolutePath
            workingPath = this@BoxService.workingDir.absolutePath
            tempPath = this@BoxService.tempDir.absolutePath
            fixAndroidStack = true
            crashReportSource = "ByeBox Android"
            oomKillerEnabled = true
            logMaxLines = 600
            // debug=false: debug mode floods writeDebugMessage from Go goroutines,
            // which are NOT attached to JVM — calling Java/Kotlin from them causes SIGABRT
            debug = false
        })
        runCatching { Libbox.reinstallCrashSignalHandlers() }
        Log.i(TAG, "Libbox.setup completed, basePath=${basePath.absolutePath}")
    }

    fun start(configContent: String, vpnService: VpnService?): Result<Unit> = runCatching {
        if (commandServer != null) {
            Log.w(TAG, "CommandServer already running, closing first")
            close()
        }

        val platform = PlatformInterfaceImpl(context, vpnService)
        platformImpl = platform

        val handler = CommandServerHandlerImpl(context)
        val server = CommandServer(handler, platform)
        commandServer = server

        Libbox.checkConfig(configContent)
        Log.i(TAG, "sing-box config validated")

        server.start()
        Log.i(TAG, "CommandServer started")

        val options = OverrideOptions()
        server.startOrReloadService(configContent, options)
        Log.i(TAG, "sing-box service started")
    }

    fun close() {
        try {
            commandServer?.closeService()
        } catch (e: Exception) {
            Log.w(TAG, "closeService error: ${e.message}")
        }
        try {
            commandServer?.close()
        } catch (e: Exception) {
            Log.w(TAG, "close error: ${e.message}")
        }
        commandServer = null
        platformImpl = null
        Log.i(TAG, "CommandServer closed")
    }

    private class CommandServerHandlerImpl(private val context: Context) : CommandServerHandler {
        @Throws(Exception::class)
        override fun serviceStop() {
            Log.i(TAG, "serviceStop called")
        }

        @Throws(Exception::class)
        override fun serviceReload() {
            Log.i(TAG, "serviceReload called")
        }

        @Throws(Exception::class)
        override fun getSystemProxyStatus(): SystemProxyStatus {
            val status = SystemProxyStatus()
            status.enabled = false
            status.available = false
            return status
        }

        @Throws(Exception::class)
        override fun setSystemProxyEnabled(enabled: Boolean) {
            Log.i(TAG, "setSystemProxyEnabled: $enabled")
        }

        @Throws(Exception::class)
        override fun triggerNativeCrash() {
            Log.w(TAG, "triggerNativeCrash called")
        }

        override fun writeDebugMessage(message: String) {
            // Called from Go goroutine threads (NOT attached to JVM).
            // Only use android.util.Log which is safe from any thread via native logging.
            // Do NOT call AppLogger here — it touches StateFlow/FileWriter which requires JVM.
            Log.d("sing-box", message)
        }

        @Throws(Exception::class)
        override fun connectSSHAgent(): Int {
            return -1
        }
    }

    private class PlatformInterfaceImpl(
        private val context: Context,
        private val vpnService: VpnService?
    ) : PlatformInterface {
        private val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        private var defaultNetworkCallback: ConnectivityManager.NetworkCallback? = null

        override fun localDNSTransport(): LocalDNSTransport? = null

        override fun usePlatformAutoDetectInterfaceControl(): Boolean = true

        override fun autoDetectInterfaceControl(fd: Int) {
            // Called from Go goroutine threads — only use thread-safe Log, NOT AppLogger
            val success = vpnService?.protect(fd) ?: false
            if (!success) {
                Log.w(TAG, "autoDetectInterfaceControl: protect(fd=$fd) failed")
            }
        }

        override fun openTun(options: TunOptions): Int {
            Log.i(TAG, "openTun called")
            val service = vpnService as? HiddifyVpnService
                ?: throw Exception("VpnService is not HiddifyVpnService")

            var hasPermission = false
            var permissionChecks = 0
            while (permissionChecks < 20) {
                if (VpnService.prepare(service) == null) {
                    hasPermission = true
                    break
                }
                permissionChecks++
                Thread.sleep(50)
            }
            if (!hasPermission) {
                throw Exception("android: missing vpn permission")
            }

            val builder = service.Builder()
                .setSession("ByeBox: ${service.serverName}")
                .setMtu(if (options.getMTU() > 0) options.getMTU() else 9000)
            
            // Add Addresses
            val inet4Addr = options.getInet4Address()
            if (inet4Addr != null) {
                while (inet4Addr.hasNext()) {
                    val prefix = inet4Addr.next()
                    builder.addAddress(prefix.address(), prefix.prefix())
                }
            }
            val inet6Addr = options.getInet6Address()
            if (inet6Addr != null) {
                while (inet6Addr.hasNext()) {
                    val prefix = inet6Addr.next()
                    builder.addAddress(prefix.address(), prefix.prefix())
                }
            }
            
            // Add Routes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val inet4RouteAddress = options.getInet4RouteAddress()
                if (inet4RouteAddress != null && inet4RouteAddress.hasNext()) {
                    while (inet4RouteAddress.hasNext()) {
                        val prefix = inet4RouteAddress.next()
                        try {
                            builder.addRoute(android.net.IpPrefix(java.net.InetAddress.getByName(prefix.address()), prefix.prefix()))
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to add IPv4 route: ${prefix.address()}/${prefix.prefix()}", e)
                        }
                    }
                } else {
                    builder.addRoute("0.0.0.0", 0)
                }

                val inet6RouteAddress = options.getInet6RouteAddress()
                if (inet6RouteAddress != null && inet6RouteAddress.hasNext()) {
                    while (inet6RouteAddress.hasNext()) {
                        val prefix = inet6RouteAddress.next()
                        try {
                            builder.addRoute(android.net.IpPrefix(java.net.InetAddress.getByName(prefix.address()), prefix.prefix()))
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to add IPv6 route: ${prefix.address()}/${prefix.prefix()}", e)
                        }
                    }
                } else if (service.ipv6Enabled) {
                    builder.addRoute("::", 0)
                }

                val inet4RouteExcludeAddress = options.getInet4RouteExcludeAddress()
                if (inet4RouteExcludeAddress != null) {
                    while (inet4RouteExcludeAddress.hasNext()) {
                        val prefix = inet4RouteExcludeAddress.next()
                        try {
                            builder.excludeRoute(android.net.IpPrefix(java.net.InetAddress.getByName(prefix.address()), prefix.prefix()))
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to exclude IPv4 route: ${prefix.address()}/${prefix.prefix()}", e)
                        }
                    }
                }

                val inet6RouteExcludeAddress = options.getInet6RouteExcludeAddress()
                if (inet6RouteExcludeAddress != null) {
                    while (inet6RouteExcludeAddress.hasNext()) {
                        val prefix = inet6RouteExcludeAddress.next()
                        try {
                            builder.excludeRoute(android.net.IpPrefix(java.net.InetAddress.getByName(prefix.address()), prefix.prefix()))
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to exclude IPv6 route: ${prefix.address()}/${prefix.prefix()}", e)
                        }
                    }
                }
            } else {
                val inet4Routes = options.getInet4RouteRange()
                if (inet4Routes != null && inet4Routes.hasNext()) {
                    while (inet4Routes.hasNext()) {
                        val prefix = inet4Routes.next()
                        builder.addRoute(prefix.address(), prefix.prefix())
                    }
                } else {
                    builder.addRoute("0.0.0.0", 0)
                }

                val inet6Routes = options.getInet6RouteRange()
                if (inet6Routes != null && inet6Routes.hasNext()) {
                    while (inet6Routes.hasNext()) {
                        val prefix = inet6Routes.next()
                        builder.addRoute(prefix.address(), prefix.prefix())
                    }
                } else if (service.ipv6Enabled) {
                    builder.addRoute("::", 0)
                }
            }

            // Add DNS Servers
            try {
                val dnsServers = options.getDNSServerAddress()
                if (dnsServers != null) {
                    while (dnsServers.hasNext()) {
                        val dns = dnsServers.next()
                        if (dns.isNotBlank()) {
                            try {
                                builder.addDnsServer(dns)
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to add DNS server: $dns", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read DNS servers from TunOptions", e)
            }
            
            // Add Packages
            var hasAllowed = false
            val includePackages = options.getIncludePackage()
            if (includePackages != null) {
                while (includePackages.hasNext()) {
                    val pkg = includePackages.next()
                    if (pkg.isNotBlank()) {
                        try {
                            builder.addAllowedApplication(pkg)
                            hasAllowed = true
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to add allowed application: $pkg", e)
                        }
                    }
                }
            }
            if (!hasAllowed) {
                val excludePackages = options.getExcludePackage()
                if (excludePackages != null) {
                    while (excludePackages.hasNext()) {
                        val pkg = excludePackages.next()
                        if (pkg.isNotBlank()) {
                            try {
                                builder.addDisallowedApplication(pkg)
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to add disallowed application: $pkg", e)
                            }
                        }
                    }
                }
                // Always exclude our own package to prevent loop
                try {
                    builder.addDisallowedApplication(service.packageName)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to exclude own package: ${e.message}")
                }
            }
            
            // Platform options
            if (service.systemBypassEnabled) {
                builder.allowBypass()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(service.meteredNetwork)
            }
            
            val pfd = builder.establish() ?: throw Exception("Failed to establish VPN interface")
            service.vpnInterface = pfd
            Log.i(TAG, "VPN interface established, fd=${pfd.fd}")
            return pfd.fd
        }

        override fun useProcFS(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

        override fun findConnectionOwner(
            ipProtocol: Int,
            sourceAddress: String?,
            sourcePort: Int,
            destinationAddress: String?,
            destinationPort: Int
        ): ConnectionOwner? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                sourceAddress.isNullOrBlank() ||
                destinationAddress.isNullOrBlank()
            ) {
                return null
            }
            return try {
                val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val uid = connectivity.getConnectionOwnerUid(
                    ipProtocol,
                    InetSocketAddress(sourceAddress, sourcePort),
                    InetSocketAddress(destinationAddress, destinationPort)
                )
                ConnectionOwner().apply {
                    userId = uid
                    if (uid != Process.INVALID_UID) {
                        val packageName = context.packageManager.getPackagesForUid(uid)?.firstOrNull().orEmpty()
                        userName = packageName
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "findConnectionOwner failed", e)
                null
            }
        }

        override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener?) {
            Log.d(TAG, "startDefaultInterfaceMonitor disabled")
            // The libbox callback is invoked from Go-owned threads. On recent Android 16
            // builds, pushing interface updates through this JNI listener can abort the
            // process before TUN is established. Hiddify keeps this path conservative;
            // socket protection still happens through autoDetectInterfaceControl(fd).
            defaultNetworkCallback = null
        }

        override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener?) {
            Log.d(TAG, "closeDefaultInterfaceMonitor")
            val callback = defaultNetworkCallback
            if (callback != null) {
                try {
                    connectivity.unregisterNetworkCallback(callback)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to unregister network callback", e)
                }
                defaultNetworkCallback = null
            }
        }

        override fun getInterfaces(): NetworkInterfaceIterator? {
            // Return null — getInterfaces() is called from Go goroutine threads that are NOT
            // attached to the JVM. Creating Java objects (NetworkInterface, MyStringIterator)
            // from unattached threads is JNI-unsafe and causes Go runtime SIGABRT.
            // Socket protection for outbound connections is handled by
            // usePlatformAutoDetectInterfaceControl()=true + autoDetectInterfaceControl(fd)
            // which calls VpnService.protect(fd) safely from the main thread.
            return try {
                val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val javaInterfaces = JavaNetInterface.getNetworkInterfaces().toList()
                val interfaces = mutableListOf<NetworkInterface>()
                connectivity.allNetworks.forEach { network ->
                    val linkProperties = connectivity.getLinkProperties(network) ?: return@forEach
                    val capabilities = connectivity.getNetworkCapabilities(network) ?: return@forEach
                    val interfaceName = linkProperties.interfaceName ?: return@forEach
                    val javaInterface = javaInterfaces.find { it.name == interfaceName } ?: return@forEach
                    val boxInterface = NetworkInterface()
                    boxInterface.name = interfaceName
                    boxInterface.index = javaInterface.index
                    boxInterface.mtu = runCatching { javaInterface.mtu }.getOrDefault(1500)
                    boxInterface.type = when {
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Libbox.InterfaceTypeWIFI
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Libbox.InterfaceTypeCellular
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Libbox.InterfaceTypeEthernet
                        else -> Libbox.InterfaceTypeOther
                    }
                    var flags = 0
                    if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                        flags = flags or OsConstants.IFF_UP or OsConstants.IFF_RUNNING
                    }
                    if (javaInterface.isLoopback) flags = flags or OsConstants.IFF_LOOPBACK
                    if (javaInterface.isPointToPoint) flags = flags or OsConstants.IFF_POINTOPOINT
                    if (javaInterface.supportsMulticast()) flags = flags or OsConstants.IFF_MULTICAST
                    boxInterface.flags = flags
                    boxInterface.metered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    boxInterface.dnsServer = MyStringIterator(linkProperties.dnsServers.mapNotNull { it.hostAddress })
                    boxInterface.addresses = MyStringIterator(
                        javaInterface.interfaceAddresses.mapNotNull { address ->
                            address.address?.hostAddress?.let { "${it}/${address.networkPrefixLength}" }
                        }
                    )
                    interfaces.add(boxInterface)
                }
                MyNetworkInterfaceIterator(interfaces)
            } catch (e: Exception) {
                Log.e(TAG, "getInterfaces failed", e)
                null
            }
        }

        override fun underNetworkExtension(): Boolean = false

        override fun includeAllNetworks(): Boolean = false

        override fun readWIFIState(): WIFIState? = null

        override fun systemCertificates(): StringIterator? = null

        override fun clearDNSCache() {}

        override fun sendNotification(notification: Notification?) {}

        override fun startNeighborMonitor(listener: NeighborUpdateListener?) {}

        override fun closeNeighborMonitor(listener: NeighborUpdateListener?) {}

        override fun registerMyInterface(name: String?) {}

        override fun usePlatformShell(): Boolean = false

        override fun checkPlatformShell() {}

        override fun openShellSession(
            user: PlatformUser?,
            command: String?,
            environ: StringIterator?,
            term: String?,
            rows: Int,
            cols: Int
        ): ShellSession? = null

        override fun lookupUser(username: String?): PlatformUser? = null

        override fun lookupSFTPServer(): String? = null

        override fun readSystemSSHHostKey(): String? = null

        override fun tailscaleHostname(): String = ""
    }
}
