package com.perqa.byebox.core

import android.net.ConnectivityManager
import android.net.DnsResolver
import android.net.Network
import android.os.Build
import android.os.CancellationSignal
import android.system.ErrnoException
import android.util.Log
import io.nekohasekai.libbox.ExchangeContext
import io.nekohasekai.libbox.LocalDNSTransport
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Native DNS resolver using Android's DnsResolver API (API Q+).
 * Ported from Hiddify's LocalResolver.kt.
 *
 * Instead of routing DNS through an external server (Cloudflare, Google, etc.),
 * this uses the system's own DNS resolver — the same one the OS uses for
 * normal network requests. This eliminates DNS leaks and is faster.
 */
object LocalResolver : LocalDNSTransport {
    private const val TAG = "LocalResolver"
    private const val RCODE_NXDOMAIN = 3

    var connectivityManager: ConnectivityManager? = null
    var defaultNetwork: Network? = null

    /**
     * Enable raw DNS message exchange (requires API Q+).
     * When true, sing-box will send raw DNS packets through exchange().
     */
    override fun raw(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /**
     * Forward a raw DNS message to the system resolver.
     * Used when sing-box wants to resolve DNS through the platform.
     */
    override fun exchange(ctx: ExchangeContext, message: ByteArray) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ctx.errorCode(RCODE_NXDOMAIN)
            return
        }
        val network = defaultNetwork ?: run {
            Log.w(TAG, "exchange: no default network available")
            ctx.errorCode(RCODE_NXDOMAIN)
            return
        }
        val cm = connectivityManager ?: run {
            Log.w(TAG, "exchange: no ConnectivityManager")
            ctx.errorCode(RCODE_NXDOMAIN)
            return
        }

        val signal = CancellationSignal()
        ctx.onCancel(signal::cancel)

        DnsResolver.getInstance().rawQuery(
            network,
            message,
            DnsResolver.FLAG_NO_RETRY,
            { it.run() }, // Execute on the calling thread (Go goroutine)
            signal,
            object : DnsResolver.Callback<ByteArray> {
                override fun onAnswer(answer: ByteArray, rcode: Int) {
                    if (rcode == 0) {
                        ctx.rawSuccess(answer)
                    } else {
                        ctx.errorCode(rcode)
                    }
                }

                override fun onError(error: DnsResolver.DnsException) {
                    val cause = error.cause
                    if (cause is ErrnoException) {
                        ctx.errnoCode(cause.errno)
                    } else {
                        Log.e(TAG, "exchange error", error)
                        ctx.errorCode(RCODE_NXDOMAIN)
                    }
                }
            }
        )
    }

    /**
     * Resolve a domain name to IP addresses using the system resolver.
     * network param can end with "4" (IPv4 only) or "6" (IPv6 only).
     */
    override fun lookup(ctx: ExchangeContext, network: String, domain: String) {
        val androidNetwork = defaultNetwork ?: run {
            Log.w(TAG, "lookup: no default network available")
            ctx.errorCode(RCODE_NXDOMAIN)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cm = connectivityManager ?: run {
                ctx.errorCode(RCODE_NXDOMAIN)
                return
            }
            val signal = CancellationSignal()
            ctx.onCancel(signal::cancel)

            // Determine DNS query type based on network suffix
            val type = when {
                network.endsWith("4") -> DnsResolver.TYPE_A
                network.endsWith("6") -> DnsResolver.TYPE_AAAA
                else -> 0 // No type filter — returns both A and AAAA
            }

            val callback = object : DnsResolver.Callback<Collection<InetAddress>> {
                override fun onAnswer(answer: Collection<InetAddress>, rcode: Int) {
                    if (rcode == 0) {
                        val addresses = answer.mapNotNull { it.hostAddress }
                        ctx.success(addresses.joinToString("\n"))
                    } else {
                        ctx.errorCode(rcode)
                    }
                }

                override fun onError(error: DnsResolver.DnsException) {
                    val cause = error.cause
                    if (cause is ErrnoException) {
                        ctx.errnoCode(cause.errno)
                    } else {
                        Log.e(TAG, "lookup error for $domain", error)
                        ctx.errorCode(RCODE_NXDOMAIN)
                    }
                }
            }

            if (type != 0) {
                DnsResolver.getInstance().query(
                    androidNetwork,
                    domain,
                    type,
                    DnsResolver.FLAG_NO_RETRY,
                    { it.run() },
                    signal,
                    callback
                )
            } else {
                DnsResolver.getInstance().query(
                    androidNetwork,
                    domain,
                    DnsResolver.FLAG_NO_RETRY,
                    { it.run() },
                    signal,
                    callback
                )
            }
        } else {
            // Pre-Q fallback: use InetAddress directly
            try {
                val addresses = androidNetwork.getAllByName(domain)
                ctx.success(addresses.mapNotNull { it.hostAddress }.joinToString("\n"))
            } catch (e: UnknownHostException) {
                ctx.errorCode(RCODE_NXDOMAIN)
            }
        }
    }
}
