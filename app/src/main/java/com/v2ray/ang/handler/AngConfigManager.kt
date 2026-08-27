package com.v2ray.ang.handler

import android.content.Context
import android.graphics.Bitmap
import android.text.TextUtils
import com.v2ray.ang.AppConfig
import com.perqa.byebox.R
import com.v2ray.ang.core.CoreConfigManager
import com.v2ray.ang.dto.SubscriptionUpdateResult
import com.v2ray.ang.dto.UrlContentRequest
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.SubscriptionCache
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.extension.isNotNullEmpty
import com.v2ray.ang.fmt.CustomFmt
import com.v2ray.ang.fmt.Hysteria2Fmt
import com.v2ray.ang.fmt.ShadowsocksFmt
import com.v2ray.ang.fmt.SocksFmt
import com.v2ray.ang.fmt.TrojanFmt
import com.v2ray.ang.fmt.VlessFmt
import com.v2ray.ang.fmt.VmessFmt
import com.v2ray.ang.fmt.WireguardFmt
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.QRCodeDecoder
import com.v2ray.ang.util.Utils
import java.net.URI
import java.nio.charset.Charset
import java.util.Base64

object AngConfigManager {

    // Parser mapping for different config types (lazy initialized)
    private val configFmtParsers: Map<String, (String) -> ProfileItem?> by lazy {
        mapOf(
            EConfigType.VMESS.protocolScheme to VmessFmt::parse,
            EConfigType.SHADOWSOCKS.protocolScheme to ShadowsocksFmt::parse,
            EConfigType.SOCKS.protocolScheme to SocksFmt::parse,
            AppConfig.SOCKS4 to SocksFmt::parse,
            AppConfig.SOCKS5 to SocksFmt::parse,
            EConfigType.TROJAN.protocolScheme to TrojanFmt::parse,
            EConfigType.VLESS.protocolScheme to VlessFmt::parse,
            EConfigType.WIREGUARD.protocolScheme to WireguardFmt::parse,
            EConfigType.HYSTERIA2.protocolScheme to Hysteria2Fmt::parse,
            AppConfig.HY2 to Hysteria2Fmt::parse
        )
    }

    /**
     * Shares the configuration to the clipboard.
     *
     * @param context The context.
     * @param guid The GUID of the configuration.
     * @return The result code.
     */
    fun share2Clipboard(context: Context, guid: String): Int {
        try {
            val conf = shareConfig(guid)
            if (TextUtils.isEmpty(conf)) {
                return -1
            }

            Utils.setClipboard(context, conf)

        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to share config to clipboard", e)
            return -1
        }
        return 0
    }

    /**
     * Shares non-custom configurations to the clipboard.
     *
     * @param context The context.
     * @param serverList The list of server GUIDs.
     * @return The number of configurations shared.
     */
    fun shareNonCustomConfigsToClipboard(context: Context, serverList: List<String>): Int {
        try {
            val sb = StringBuilder()
            var count = 0
            for (guid in serverList) {
                val url = shareConfig(guid)
                if (TextUtils.isEmpty(url)) {
                    continue
                }
                sb.append(url)
                sb.appendLine()
                count++
            }
            if (count > 0) {
                Utils.setClipboard(context, sb.toString())
            }
            return count
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to share non-custom configs to clipboard", e)
            return -1
        }
    }

    /**
     * Shares the configuration as a QR code.
     *
     * @param guid The GUID of the configuration.
     * @return The QR code bitmap.
     */
    fun share2QRCode(guid: String): Bitmap? {
        try {
            val conf = shareConfig(guid)
            if (TextUtils.isEmpty(conf)) {
                return null
            }
            return QRCodeDecoder.createQRCode(conf)

        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to share config as QR code", e)
            return null
        }
    }

    /**
     * Shares the full content of the configuration to the clipboard.
     *
     * @param context The context.
     * @param guid The GUID of the configuration.
     * @return The result code.
     */
    fun shareFullContent2Clipboard(context: Context, guid: String?): Int {
        try {
            if (guid == null) return -1
            val result = CoreConfigManager.getV2rayConfig(context, guid)
            if (result.status) {
                Utils.setClipboard(context, result.content)
            } else {
                return -1
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to share full content to clipboard", e)
            return -1
        }
        return 0
    }

    /**
     * Shares the configuration.
     *
     * @param guid The GUID of the configuration.
     * @return The configuration string.
     */
    private fun shareConfig(guid: String): String {
        try {
            val config = MmkvManager.decodeServerConfig(guid) ?: return ""

            return config.configType.protocolScheme + when (config.configType) {
                EConfigType.VMESS -> VmessFmt.toUri(config)
                EConfigType.SHADOWSOCKS -> ShadowsocksFmt.toUri(config)
                EConfigType.SOCKS -> SocksFmt.toUri(config)
                EConfigType.VLESS -> VlessFmt.toUri(config)
                EConfigType.TROJAN -> TrojanFmt.toUri(config)
                EConfigType.WIREGUARD -> WireguardFmt.toUri(config)
                EConfigType.HYSTERIA2 -> Hysteria2Fmt.toUri(config)
                else -> {}
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to share config for GUID: $guid", e)
            return ""
        }
    }

    /**
     * Imports a batch of configurations.
     *
     * @param server The server string.
     * @param subid The subscription ID.
     * @param append Whether to append the configurations.
     * @return A pair containing the number of configurations and subscriptions imported.
     */
    fun importBatchConfig(server: String?, subid: String, append: Boolean): Pair<Int, Int> {
        var count = parseBatchConfig(Utils.decode(server), subid, append)
        if (count <= 0) {
            count = parseBatchConfig(server, subid, append)
        }
        if (count <= 0) {
            count = parseCustomConfigServer(server, subid, append)
        }

        var countSub = parseBatchSubscription(server)
        if (countSub <= 0) {
            countSub = parseBatchSubscription(Utils.decode(server))
        }
        if (countSub > 0) {
            updateConfigViaSubAll()
        }

        return count to countSub
    }

    /**
     * Parses a batch of subscriptions.
     *
     * @param servers The servers string.
     * @return The number of subscriptions parsed.
     */
    private fun parseBatchSubscription(servers: String?): Int {
        try {
            if (servers == null) {
                return 0
            }

            var count = 0
            servers.lines()
                .distinct()
                .forEach { str ->
                    if (Utils.isValidSubUrl(str)) {
                        count += importUrlAsSubscription(str)
                    }
                }
            return count
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to parse batch subscription", e)
        }
        return 0
    }

    /**
     * Parses a batch of configurations.
     *
     * @param servers The servers string.
     * @param subid The subscription ID.
     * @param append Whether to append the configurations.
     * @return The number of configurations parsed.
     */
    private fun parseBatchConfig(servers: String?, subid: String, append: Boolean): Int {
        try {
            if (servers == null) {
                return 0
            }
            // Find the currently selected server that belongs to the same subscription before replacement.
            val removedSelected = getRemovedSelectedProfile(subid, append)

            val subItem = MmkvManager.decodeSubscription(subid)

            // Parse all configs first (no I/O during parsing)
            val configs = mutableListOf<ProfileItem>()
            servers.lines()
                .distinct()
                .reversed()
                .forEach {
                    val config = parseConfig(it, subid, subItem)
                    if (config != null) {
                        configs.add(config)
                    }
                }

            // Batch save all parsed configs (only one serverList read/write)
            if (configs.isNotEmpty()) {
                val keyToProfile = if (append) {
                    batchSaveConfigs(configs, subid)
                } else {
                    batchReplaceConfigs(configs, subid)
                }
                val matchKey = findMatchedProfileKey(keyToProfile, removedSelected)
                matchKey?.let { MmkvManager.setSelectServer(it) }
            }

            return configs.size
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to parse batch config", e)
        }
        return 0
    }

    /**
     * Batch save configurations to reduce serverList read/write operations.
     * Reads serverList once, saves all configs, then writes serverList once.
     *
     * @param configs The list of ProfileItem to save.
     * @param subid The subscription ID.
     * @return Map of generated keys to their corresponding ProfileItem.
     */
    private fun batchSaveConfigs(configs: List<ProfileItem>, subid: String): Map<String, ProfileItem> {
        val keyToProfile = mutableMapOf<String, ProfileItem>()

        // Read serverList once
        val serverList = MmkvManager.decodeServerList(subid)

        configs.forEach { config ->
            val key = Utils.getUuid()
            // Save profile directly without updating serverList
            MmkvManager.encodeProfileDirect(key, JsonUtil.toJson(config))

            if (!serverList.contains(key)) {
                serverList.add(0, key)
            }
            keyToProfile[key] = config
        }

        // Write serverList once
        MmkvManager.encodeServerList(serverList, subid)
        return keyToProfile
    }

    /**
     * Replaces a subscription's servers while preserving stable GUIDs for configs
     * that still match (by server+port+password). This keeps profile references
     * (selected server per profile) valid across subscription updates.
     *
     * @param configs The list of ProfileItem to save.
     * @param subid The subscription ID.
     * @return Map of generated keys to their corresponding ProfileItem.
     */
    private fun batchReplaceConfigs(configs: List<ProfileItem>, subid: String): Map<String, ProfileItem> {
        // Read existing server list + configs once
        val oldServerList = MmkvManager.decodeServerList(subid)
        val oldBySignature = mutableMapOf<String, String>()
        oldServerList.forEach { key ->
            val old = MmkvManager.decodeServerConfig(key)
            if (old != null) {
                configSignature(old)?.let { sig -> oldBySignature.putIfAbsent(sig, key) }
            }
        }

        val keyToProfile = mutableMapOf<String, ProfileItem>()
        val newServerList = mutableListOf<String>()
        val keptKeys = mutableSetOf<String>()

        // Reuse existing GUIDs for configs that still match; mint new ones for new servers
        configs.forEach { config ->
            val signature = configSignature(config)
            var key = signature?.let { oldBySignature[it] }.takeIf { it != null && it !in keptKeys }
            if (key == null) {
                key = Utils.getUuid()
            }
            keptKeys += key

            MmkvManager.encodeProfileDirect(key, JsonUtil.toJson(config))

            if (!newServerList.contains(key)) {
                newServerList.add(0, key)
            }
            keyToProfile[key] = config
        }

        // Remove stale servers that no longer exist in the subscription
        MmkvManager.removeServerViaSubidNotIn(subid, keptKeys)

        // Replace the list with the new ordering
        MmkvManager.encodeServerList(newServerList, subid)

        // Restore a selected server if the old one was dropped
        val selected = MmkvManager.getSelectServer()
        if (selected == null || selected.isBlank()) {
            newServerList.firstOrNull()?.let { MmkvManager.setSelectServer(it) }
        }

        return keyToProfile
    }

    /**
     * Returns a stable signature identifying a server's identity:
     * address + port + password (uuid). Two configs with the same signature
     * are considered the "same server" across subscription refreshes.
     */
    private fun configSignature(config: ProfileItem): String? {
        val server = config.server?.trim().orEmpty()
        val port = config.serverPort?.trim().orEmpty()
        val password = config.password?.trim().orEmpty()
        if (server.isEmpty() && port.isEmpty()) return null
        return "${server.lowercase()}|$port|$password"
    }

    /**
     * Finds a matched profile key from the given key-profile map using multi-level matching.
     * Matching priority (from highest to lowest):
     * 1. Exact match: server + port + password
     * 2. Match by remarks (exact match)
     * 3. Match by server + port
     * 4. Match by server only
     *
     * @param keyToProfile Map of server keys to their ProfileItem
     * @param target Target profile to match
     * @return Matched key or null
     */
    private fun findMatchedProfileKey(keyToProfile: Map<String, ProfileItem>, target: ProfileItem?): String? {
        if (keyToProfile.isEmpty()) return null
        if (target == null) return null

        // Level 0: Full match (remarks + server + port + password)
        if (target.remarks.isNotBlank()) {
            keyToProfile.entries.firstOrNull { (_, saved) ->
                isSameText(saved.remarks, target.remarks) &&
                        isSameText(saved.server, target.server) &&
                        isSameText(saved.serverPort, target.serverPort) &&
                        isSameText(saved.password, target.password)
            }?.key?.let { return it }
        }

        // Level 1: Match by remarks
        if (target.remarks.isNotBlank()) {
            keyToProfile.entries.firstOrNull { (_, saved) ->
                isSameText(saved.remarks, target.remarks)
            }?.key?.let { return it }
        }

        // Level 2: Exact match (server + port + password)
        keyToProfile.entries.firstOrNull { (_, saved) ->
            isSameText(saved.server, target.server) &&
                    isSameText(saved.serverPort, target.serverPort) &&
                    isSameText(saved.password, target.password)
        }?.key?.let { return it }

        // Level 3: Match by server + port
        keyToProfile.entries.firstOrNull { (_, saved) ->
            isSameText(saved.server, target.server) &&
                    isSameText(saved.serverPort, target.serverPort)
        }?.key?.let { return it }

        // Level 4: Match by server only
        keyToProfile.entries.firstOrNull { (_, saved) ->
            isSameText(saved.server, target.server)
        }?.key?.let { return it }

        // If old selected node cannot be matched, fall back to the first imported config.
        return keyToProfile.keys.firstOrNull()
    }

    /**
     * Returns the currently selected profile if it belongs to the target subscription and will be replaced.
     */
    private fun getRemovedSelectedProfile(subid: String, append: Boolean): ProfileItem? {
        if (subid.isBlank() || append) return null

        return MmkvManager.getSelectServer()
            .takeIf { it?.isNotBlank() == true }
            ?.let { MmkvManager.decodeServerConfig(it) }
            ?.takeIf { it.subscriptionId == subid }
    }

    /**
     * Case-insensitive trimmed string comparison.
     *
     * @param left First string
     * @param right Second string
     * @return True if both are non-empty and equal (case-insensitive, trimmed)
     */
    private fun isSameText(left: String?, right: String?): Boolean {
        if (left.isNullOrBlank() || right.isNullOrBlank()) return false
        return left.trim().equals(right.trim(), ignoreCase = true)
    }

    /**
     * Parses a custom configuration server.
     *
     * @param server The server string.
     * @param subid The subscription ID.
     * @param append Whether to append the configurations.
     * @return The number of configurations parsed.
     */
    private fun parseCustomConfigServer(server: String?, subid: String, append: Boolean): Int {
        if (server == null) {
            return 0
        }
        if (server.contains("inbounds")
            && server.contains("outbounds")
            && server.contains("routing")
        ) {
            try {
                val serverList: Array<Any> =
                    JsonUtil.fromJson(server, Array<Any>::class.java) ?: arrayOf()

                if (serverList.isNotEmpty()) {
                    val removedSelected = getRemovedSelectedProfile(subid, append)
                    if (!append) {
                        MmkvManager.removeServerViaSubid(subid)
                    }
                    var count = 0
                    val keyToProfile = mutableMapOf<String, ProfileItem>()
                    for (srv in serverList.reversed()) {
                        val config = CustomFmt.parse(JsonUtil.toJson(srv)) ?: continue
                        config.subscriptionId = subid
                        config.description = generateDescription(config)
                        val key = MmkvManager.encodeServerConfig("", config)
                        MmkvManager.encodeServerRaw(key, JsonUtil.toJsonPretty(srv) ?: "")
                        keyToProfile[key] = config
                        count += 1
                    }
                    if (count > 0) {
                        val matchKey = findMatchedProfileKey(keyToProfile, removedSelected)
                        matchKey?.let { MmkvManager.setSelectServer(it) }
                    }
                    return count
                }
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to parse custom config server JSON array", e)
            }

            try {
                // For compatibility
                val config = CustomFmt.parse(server) ?: return 0
                config.subscriptionId = subid
                config.description = generateDescription(config)
                if (!append) {
                    MmkvManager.removeServerViaSubid(subid)
                }
                val key = MmkvManager.encodeServerConfig("", config)
                MmkvManager.encodeServerRaw(key, server)
                return 1
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to parse custom config server as single config", e)
            }
            return 0
        } else if (server.startsWith("[Interface]") && server.contains("[Peer]")) {
            try {
                val config = WireguardFmt.parseWireguardConfFile(server) ?: return 0
                config.description = generateDescription(config)
                if (!append) {
                    MmkvManager.removeServerViaSubid(subid)
                }
                val key = MmkvManager.encodeServerConfig("", config)
                MmkvManager.encodeServerRaw(key, server)
                return 1
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to parse WireGuard config file", e)
            }
            return 0
        } else {
            return 0
        }
    }

    /**
     * Parses the configuration from a QR code or string.
     * Only parses and returns ProfileItem, does not save.
     *
     * @param str The configuration string.
     * @param subid The subscription ID.
     * @param subItem The subscription item.
     * @return The parsed ProfileItem or null if parsing fails or filtered out.
     */
    private fun parseConfig(
        str: String?,
        subid: String,
        subItem: SubscriptionItem?
    ): ProfileItem? {
        try {
            if (str == null || TextUtils.isEmpty(str)) {
                return null
            }

            val config = configFmtParsers.firstNotNullOfOrNull { (scheme, parser) ->
                if (str.startsWith(scheme)) parser(str) else null
            }

            if (config == null) {
                return null
            }

            // Apply filter
            if (subItem?.filter.isNotNullEmpty() && config.remarks.isNotNullEmpty()) {
                val matched = Regex(pattern = subItem?.filter.orEmpty())
                    .containsMatchIn(input = config.remarks)
                if (!matched) return null
            }

            config.subscriptionId = subid
            config.description = generateDescription(config)

            return config
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to parse config", e)
            return null
        }
    }

    /**
     * Updates the configuration via all subscriptions.
     *
     * @return Detailed result of the subscription update operation.
     */
    fun updateConfigViaSubAll(): SubscriptionUpdateResult {
        return try {
            val subscriptions = MmkvManager.decodeSubscriptions()
            subscriptions.fold(SubscriptionUpdateResult()) { acc, subscription ->
                acc + updateConfigViaSub(subscription)
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to update config via all subscriptions", e)
            SubscriptionUpdateResult()
        }
    }

    private fun decodeRfc2047(headerValue: String): String {
        val regex = Regex("""=\?([^?]+)\?([BbQq])\?([^?]*)\?=""")
        return regex.replace(headerValue) { match ->
            val charset = match.groupValues[1]
            val encoding = match.groupValues[2].uppercase()
            val encoded = match.groupValues[3]
            try {
                when (encoding) {
                    "B" -> String(Base64.getDecoder().decode(encoded), Charset.forName(charset))
                    "Q" -> {
                        val decoded = encoded
                            .replace('_', ' ')
                            .replace(Regex("=([0-9A-Fa-f]{2})")) {
                                Integer.parseInt(it.groupValues[1], 16).toChar().toString()
                            }
                        decoded
                    }
                    else -> match.value
                }
            } catch (_: Exception) {
                match.value
            }
        }.trim()
    }

    private fun extractDomainFromUrl(url: String): String? {
        return try {
            val uri = URI(url)
            uri.host?.removePrefix("www.")
        } catch (_: Exception) {
            null
        }
    }

    private fun parseSubscriptionUserinfo(headerValue: String): Userinfo {
        var upload: Long? = null
        var download: Long? = null
        var total: Long? = null
        var expire: Long? = null
        headerValue.split(';').forEach { part ->
            val kv = part.split('=')
            if (kv.size == 2) {
                val key = kv[0].trim().lowercase()
                val value = kv[1].trim().toLongOrNull()
                if (value != null) {
                    when (key) {
                        "upload" -> upload = value
                        "download" -> download = value
                        "total" -> total = value
                        "expire" -> expire = value
                    }
                }
            }
        }
        return Userinfo(upload, download, total, expire)
    }

    private data class Userinfo(
        val upload: Long?,
        val download: Long?,
        val total: Long?,
        val expire: Long?
    )

    private fun parseContentDispositionFilename(headerValue: String): String? {
        val utf8Match = Regex("""filename\*=UTF-8''([^;\n]+)""", RegexOption.IGNORE_CASE).find(headerValue)
        if (utf8Match != null) {
            val encoded = utf8Match.groupValues[1]
            return runCatching {
                java.net.URLDecoder.decode(encoded, "UTF-8")
            }.getOrNull()
        }
        val normalMatch = Regex("""filename="([^"]+)"""", RegexOption.IGNORE_CASE).find(headerValue)
        if (normalMatch != null) {
            return normalMatch.groupValues[1]
        }
        val simpleMatch = Regex("""filename=([^;\n]+)""", RegexOption.IGNORE_CASE).find(headerValue)
        if (simpleMatch != null) {
            return simpleMatch.groupValues[1].trim()
        }
        return null
    }

    /**
     * Updates the configuration via a subscription.
     *
     * @param it The subscription item.
     * @return Subscription update result.
     */
    fun updateConfigViaSub(it: SubscriptionCache): SubscriptionUpdateResult {
        try {
            // Check if disabled
            if (!it.subscription.enabled) {
                return SubscriptionUpdateResult(skipCount = 1)
            }

            // Validate subscription info
            if (TextUtils.isEmpty(it.guid)
                || TextUtils.isEmpty(it.subscription.remarks)
                || TextUtils.isEmpty(it.subscription.url)
            ) {
                return SubscriptionUpdateResult(skipCount = 1)
            }

            val url = HttpUtil.toIdnUrl(it.subscription.url)
            if (!Utils.isValidUrl(url)) {
                return SubscriptionUpdateResult(failureCount = 1)
            }
            if (!it.subscription.allowInsecureUrl) {
                if (!Utils.isValidSubUrl(url)) {
                    return SubscriptionUpdateResult(failureCount = 1)
                }
            }
            LogUtil.i(AppConfig.TAG, url)
            val userAgent = it.subscription.userAgent
            val proxyUsername = SettingsManager.getSocksUsername()
            val proxyPassword = SettingsManager.getSocksPassword()

            var responsePair = try {
                val httpPort = SettingsManager.getHttpPort()
                HttpUtil.getUrlContentAndHeaders(
                    UrlContentRequest(
                        url = url,
                        userAgent = userAgent,
                        timeout = 15000,
                        httpPort = httpPort,
                        proxyUsername = proxyUsername,
                        proxyPassword = proxyPassword
                    )
                )
            } catch (e: Exception) {
                LogUtil.e(AppConfig.ANG_PACKAGE, "Update subscription: proxy not ready or other error", e)
                null
            }
            if (responsePair == null || responsePair.first.isEmpty()) {
                responsePair = try {
                    HttpUtil.getUrlContentAndHeaders(
                        UrlContentRequest(
                            url = url,
                            userAgent = userAgent
                        )
                    )
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "Update subscription: Failed to get URL content with user agent", e)
                    null
                }
            }
            if (responsePair == null || responsePair.first.isEmpty()) {
                return SubscriptionUpdateResult(failureCount = 1)
            }
            val configText = responsePair.first
            val headers = responsePair.second

            val count = parseConfigViaSub(configText, it.guid, false)
            if (count > 0) {
                it.subscription.lastUpdated = System.currentTimeMillis()
                
                headers.entries.forEach { entry ->
                    val key = entry.key.lowercase()
                    val valStr = entry.value
                    if (key == "subscription-userinfo" || key == "x-subscription-userinfo") {
                        val userinfo = parseSubscriptionUserinfo(valStr)
                        it.subscription.uploadBytes = userinfo.upload
                        it.subscription.downloadBytes = userinfo.download
                        it.subscription.totalBytes = userinfo.total
                        it.subscription.expireAt = userinfo.expire
                    }
                    if (key == "profile-title") {
                        val newTitle = decodeHeaderValue(valStr)
                        if (newTitle.isNotBlank() && (it.subscription.remarks == "import sub" || it.subscription.remarks.startsWith("http") || it.subscription.remarks == "Подписка")) {
                            it.subscription.remarks = newTitle
                        }
                    }
                    if (key == "content-disposition") {
                        val filename = parseContentDispositionFilename(valStr)
                        if (filename != null && filename.isNotBlank() && (it.subscription.remarks == "import sub" || it.subscription.remarks.startsWith("http") || it.subscription.remarks == "Подписка")) {
                            it.subscription.remarks = filename
                        }
                    }
                    if (key == "profile-description" || key == "subscription-description") {
                        val desc = decodeHeaderValue(valStr)
                        if (desc.isNotBlank()) {
                            it.subscription.description = desc
                        }
                    }
                }

                // 2. Parse from body comments as fallback/enrichment
                runCatching {
                    configText.lines().take(15).forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.startsWith("#") || trimmed.startsWith("//")) {
                            val commentContent = trimmed.substring(if (trimmed.startsWith("#")) 1 else 2).trim()
                            val colonIndex = commentContent.indexOf(':')
                            if (colonIndex > 0) {
                                val commentKey = commentContent.substring(0, colonIndex).trim().lowercase()
                                val commentVal = commentContent.substring(colonIndex + 1).trim()
                                if (commentVal.isNotBlank()) {
                                    val decodedVal = try {
                                        java.net.URLDecoder.decode(decodeRfc2047(commentVal), "UTF-8").trim()
                                    } catch (_: Exception) {
                                        decodeRfc2047(commentVal).trim()
                                    }
                                    if (commentKey == "profile-title" || commentKey == "subscription-title") {
                                        if (it.subscription.remarks == "import sub" || it.subscription.remarks.startsWith("http") || it.subscription.remarks == "Подписка") {
                                            it.subscription.remarks = decodedVal
                                        }
                                    }
                                    if (commentKey == "profile-description" || commentKey == "subscription-description") {
                                        if (it.subscription.description.isNullOrBlank()) {
                                            it.subscription.description = decodedVal
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Fallback: derive name from domain if still a template/URL name
                val remarks = it.subscription.remarks
                if (remarks == "import sub" || remarks.startsWith("http") || remarks == "Подписка") {
                    extractDomainFromUrl(it.subscription.url)?.let { domain ->
                        it.subscription.remarks = domain
                    }
                }
                
                MmkvManager.encodeSubscription(it.guid, it.subscription)
                LogUtil.i(AppConfig.TAG, "Subscription updated: ${it.subscription.remarks}, $count configs")
                return SubscriptionUpdateResult(
                    configCount = count,
                    successCount = 1
                )
            } else {
                // Got response but no valid configs parsed
                return SubscriptionUpdateResult(failureCount = 1)
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to update config via subscription", e)
            return SubscriptionUpdateResult(failureCount = 1)
        }
    }

    /**
     * Parses the configuration via a subscription.
     *
     * @param server The server string.
     * @param subid The subscription ID.
     * @param append Whether to append the configurations.
     * @return The number of configurations parsed.
     */
    private fun parseConfigViaSub(server: String?, subid: String, append: Boolean): Int {
        var count = parseBatchConfig(Utils.decode(server), subid, append)
        if (count <= 0) {
            count = parseBatchConfig(server, subid, append)
        }
        if (count <= 0) {
            count = parseCustomConfigServer(server, subid, append)
        }
        return count
    }

    /**
     * Imports a URL as a subscription.
     *
     * @param url The URL.
     * @return The number of subscriptions imported.
     */
    private fun importUrlAsSubscription(url: String): Int {
        val subscriptions = MmkvManager.decodeSubscriptions()
        subscriptions.forEach {
            if (it.subscription.url == url) {
                return 0
            }
        }
        val uri = URI(Utils.fixIllegalUrl(url))
        val subItem = SubscriptionItem()
        subItem.remarks = uri.fragment ?: "import sub"
        subItem.url = url
        MmkvManager.encodeSubscription("", subItem)
        return 1
    }

    private fun decodeHeaderValue(value: String): String {
        val decodedRfc = decodeRfc2047(value)
        return try {
            java.net.URLDecoder.decode(decodedRfc, "UTF-8").trim()
        } catch (_: Exception) {
            decodedRfc.trim()
        }
    }

    /** Generates a description for the profile.
     *
     * @param profile The profile item.
     * @return The generated description.
     */
    fun generateDescription(profile: ProfileItem): String {
        // Hide xxx:xxx:***/xxx.xxx.xxx.***
        val server = profile.server
        val port = profile.serverPort
        if (server.isNullOrBlank() && port.isNullOrBlank()) return ""

        val addrPart = server?.let {
            if (it.contains(":"))
                it.split(":").take(2).joinToString(":", postfix = ":***")
            else
                it.split('.').dropLast(1).joinToString(".", postfix = ".***")
        } ?: ""

        return "$addrPart : ${port ?: ""}"
    }
}
