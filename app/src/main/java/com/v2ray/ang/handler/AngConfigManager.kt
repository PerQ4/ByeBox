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
import android.util.Base64

object AngConfigManager {

    // Remembers which server GUIDs were replaced by new ones during the last
    // subscription refresh (old guideline -> new GUID). Consumed by callers to
    // re-map profile server assignments so they survive subscription updates.
    @Volatile
    private var serverReplacementMap: Map<String, String> = emptyMap()

    fun consumeServerReplacementMap(): Map<String, String> {
        val consumed = serverReplacementMap
        serverReplacementMap = emptyMap()
        return consumed
    }

    /**
     * After a subscription refresh, re-maps profile server assignments that
     * referenced replaced GUIDs onto the new ones and re-applies the active
     * profile so the currently selected server survives the update.
     */
    private fun reconcileProfileAssignmentsAfterRefresh() {
        val replacementMap = consumeServerReplacementMap()
        if (replacementMap.isEmpty()) return
        try {
            val context = com.perqa.byebox.ByeBoxApplication.instance
            val changed = com.perqa.byebox.data.ProfilePresetManager.reconcileAssignments(
                context,
                replacementMap,
                MmkvManager.decodeAllServerList().toSet()
            )
            if (changed) {
                val manager = com.perqa.byebox.data.ProfilePresetManager
                val activeId = manager.getActiveProfileId(context)
                val activeProfile = manager.loadProfiles(context).find { it.id == activeId }
                if (activeProfile != null) {
                    manager.applyProfile(context, activeProfile)
                }
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to reconcile profile assignments after refresh: ${e.message}", e)
        }
    }

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
     * Builds a shareable link/URI for a config using the protocol-specific
     * format classes (v2rayNG exporters). The UI copy/share/QR entry points
     * must use this instead of ad-hoc link builders, which produce broken
     * links for Shadowsocks/SOCKS/WireGuard/Hysteria2 and friends.
     *
     * @param guid The GUID of the configuration.
     * @return The share URI string ("" when the config cannot be shared as a link).
     */
    fun getShareLink(guid: String?): String {
        if (guid.isNullOrBlank()) return ""
        val link = shareConfig(guid)
        // Some types (CUSTOM, HTTP, HYSTERIA, unknown) have no exporter:
        // shareConfig returns just the empty/partial scheme then — treat as unsupported.
        return if (link.isBlank() || link.endsWith("://")) "" else link
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

                // Re-map profile server assignments whose GUIDs were replaced
                // during this refresh, and re-apply the active profile if needed.
                reconcileProfileAssignmentsAfterRefresh()

                // Возвращаем число реально сохранённых узлов: дубли внутри
                // ответа панели уже схлопнуты batch-функциями.
                return keyToProfile.size
            }

            return 0
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

        deduplicateConfigs(configs).forEach { config ->
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
        val oldProfiles = oldServerList.mapNotNull { key ->
            MmkvManager.decodeServerConfig(key)?.let { key to it }
        }
        val oldBySignature = mutableMapOf<String, String>()
        oldProfiles.forEach { (key, old) ->
            configSignature(old)?.let { sig -> oldBySignature.putIfAbsent(sig, key) }
        }

        val keyToProfile = mutableMapOf<String, ProfileItem>()
        val newServerList = mutableListOf<String>()
        val keptKeys = mutableSetOf<String>()

        // Reuse existing GUIDs for configs that still match; mint new ones for new servers.
        // Дубли внутри одного ответа панели (например, один и тот же узел из
        // auto/policy-group и напрямую) схлопываем по сигнатуре: порядок и
        // стабильность GUID при этом сохраняются.
        deduplicateConfigs(configs).forEach { config ->
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

        // Record old GUID -> new GUID for replaced servers so profile
        // server assignments can be re-mapped after the refresh.
        serverReplacementMap = buildServerReplacementMap(oldProfiles, keptKeys, keyToProfile)

        return keyToProfile
    }

    /**
     * Replaces a subscription's custom (JSON/wireguard) servers while preserving
     * stable GUIDs for configs that still match, mirroring batchReplaceConfigs.
     * Also keeps the raw payload per server.
     *
     * @param configs Pairs of parsed ProfileItem and their raw payload.
     * @param subid The subscription ID.
     * @return Map of generated keys to their corresponding ProfileItem.
     */
    private fun batchReplaceCustomConfigs(
        configs: List<Pair<ProfileItem, String>>,
        subid: String
    ): Map<String, ProfileItem> {
        val oldServerList = MmkvManager.decodeServerList(subid)
        val oldProfiles = oldServerList.mapNotNull { key ->
            MmkvManager.decodeServerConfig(key)?.let { key to it }
        }
        val oldBySignature = mutableMapOf<String, String>()
        oldProfiles.forEach { (key, old) ->
            configSignature(old)?.let { sig -> oldBySignature.putIfAbsent(sig, key) }
        }

        val keyToProfile = mutableMapOf<String, ProfileItem>()
        val newServerList = mutableListOf<String>()
        val keptKeys = mutableSetOf<String>()

        // Схлопываем дубли внутри одного batch (тот же узел, пришедший дважды).
        // Для CUSTOM-конфигов configSignature содержит только адрес/порт
        // (legacy CustomFmt.parse не извлекает остальное), поэтому там
        // сравниваем сырой JSON: иначе разные узлы на одном хосте схлопнутся.
        val deduplicated = mutableListOf<Pair<ProfileItem, String>>()
        val seenSignatures = mutableSetOf<String>()
        configs.forEach { pair ->
            val signature = customConfigDedupKey(pair.first, pair.second)
            if (signature == null || seenSignatures.add(signature)) {
                deduplicated.add(pair)
            }
        }

        deduplicated.forEach { (config, raw) ->
            val signature = configSignature(config)
            var key = signature?.let { oldBySignature[it] }.takeIf { it != null && it !in keptKeys }
            if (key == null) {
                key = Utils.getUuid()
            }
            keptKeys += key

            MmkvManager.encodeProfileDirect(key, JsonUtil.toJson(config))
            if (raw.isNotBlank()) {
                MmkvManager.encodeServerRaw(key, raw)
            }

            if (!newServerList.contains(key)) {
                newServerList.add(0, key)
            }
            keyToProfile[key] = config
        }

        MmkvManager.removeServerViaSubidNotIn(subid, keptKeys)
        MmkvManager.encodeServerList(newServerList, subid)

        val selected = MmkvManager.getSelectServer()
        if (selected == null || selected.isBlank()) {
            newServerList.firstOrNull()?.let { MmkvManager.setSelectServer(it) }
        }

        serverReplacementMap = buildServerReplacementMap(oldProfiles, keptKeys, keyToProfile)

        return keyToProfile
    }

    /**
     * Builds a mapping from old server GUIDs (that were dropped during a
     * subscription refresh) to the new GUIDs that now represent the same
     * server. Only exact server+port matches are considered replacements, so a
     * profile assignment is never silently pointed at a different endpoint.
     */
    private fun buildServerReplacementMap(
        oldProfiles: List<Pair<String, ProfileItem>>,
        keptKeys: Set<String>,
        keyToProfile: Map<String, ProfileItem>
    ): Map<String, String> {
        val byServerPort = mutableMapOf<Pair<String, String>, String>()
        keyToProfile.forEach { (key, cfg) ->
            val server = cfg.server?.trim()?.lowercase()
            val port = cfg.serverPort?.trim()
            if (server != null && server.isNotEmpty() && port != null && port.isNotEmpty()) {
                byServerPort.putIfAbsent(server to port, key)
            }
        }
        if (byServerPort.isEmpty()) return emptyMap()

        val used = mutableSetOf<String>()
        val replacement = mutableMapOf<String, String>()
        oldProfiles.forEach { (oldKey, old) ->
            if (oldKey in keptKeys) return@forEach
            val server = old.server?.trim()?.lowercase()
            val port = old.serverPort?.trim()
            if (server == null || server.isEmpty() || port == null || port.isEmpty()) return@forEach
            val newKey = byServerPort[server to port]?.takeIf { it !in used }
            if (newKey != null) {
                replacement[oldKey] = newKey
                used += newKey
            }
        }
        return replacement
    }

    /**
     * Returns a stable signature identifying a server's identity.
     *
     * Раньше сигнатура была только `address|port|password`, из-за чего разные
     * узлы на одном сервере (например, с разным транспортом/SNI/flow) считались
     * одним и тем же, а один и тот же узел с разными метаданными — разными.
     * Теперь в сигнатуру входят все поля, влияющие на подключение, но НЕ
     * входят remarks/description/subscriptionId/addedTime (они не меняют узел).
     */
    internal fun configSignature(config: ProfileItem): String? {
        val server = config.server?.trim().orEmpty()
        val port = config.serverPort?.trim().orEmpty()
        if (server.isEmpty() && port.isEmpty()) return null
        fun norm(value: String?): String = value?.trim().orEmpty()
        fun normLower(value: String?): String = value?.trim()?.lowercase().orEmpty()
        fun normInt(value: Int?): String = value?.toString().orEmpty()
        return listOf(
            config.configType.name,
            normLower(config.server),
            port,
            norm(config.password),
            normLower(config.method),
            normLower(config.flow),
            norm(config.username),
            normLower(config.network),
            normLower(config.headerType),
            normLower(config.host),
            norm(config.path),
            norm(config.serviceName),
            normLower(config.mode),
            normLower(config.authority),
            normLower(config.xhttpMode),
            norm(config.xhttpExtra),
            norm(config.finalMask),
            normLower(config.security),
            normLower(config.sni),
            norm(config.alpn),
            norm(config.fingerPrint),
            if (config.insecure == true) "1" else "",
            norm(config.echConfigList),
            norm(config.verifyPeerCertByName),
            norm(config.pinnedCA256),
            norm(config.publicKey),
            norm(config.shortId),
            norm(config.spiderX),
            norm(config.mldsa65Verify),
            norm(config.secretKey),
            norm(config.preSharedKey),
            norm(config.localAddress),
            norm(config.reserved),
            normInt(config.mtu),
            norm(config.obfsPassword),
            norm(config.portHopping),
            norm(config.portHoppingInterval),
            norm(config.pinSHA256),
            normLower(config.quicSecurity),
            norm(config.quicKey),
            norm(config.seed),
            normInt(config.kcpMtu),
            normInt(config.kcpTti),
            norm(config.proxyChainProfiles),
            normLower(config.browserDialerMode),
            normLower(config.policyGroupType),
            norm(config.policyGroupSubscriptionId),
            norm(config.policyGroupFilter),
        ).joinToString("\u0001")
    }

    /**
     * Убирает повторяющиеся конфиги внутри одного пакета (batch), сохраняя
     * порядок первого вхождения. Конфиги без сигнатуры (custom) не схлопываем:
     * для них нет надёжного ключа идентичности.
     */
    internal fun deduplicateConfigs(configs: List<ProfileItem>): List<ProfileItem> {
        val seen = mutableSetOf<String>()
        return configs.filter { config ->
            val signature = configSignature(config)
            signature == null || seen.add(signature)
        }
    }

    /**
     * Ключ дедупликации для custom/wireguard-пакетов, где есть сырой payload.
     * Для CUSTOM/POLICYGROUP берём сам JSON (configSignature у них слишком
     * грубая: только host+port), для остальных — обычную сигнатуру.
     */
    internal fun customConfigDedupKey(config: ProfileItem, raw: String): String? {
        if (config.configType == EConfigType.CUSTOM || config.configType == EConfigType.POLICYGROUP) {
            val payload = raw.trim()
            if (payload.isNotEmpty()) return "raw:$payload"
        }
        return configSignature(config)
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
                    val configs = mutableListOf<Pair<ProfileItem, String>>()
                    for (srv in serverList.reversed()) {
                        val config = CustomFmt.parse(JsonUtil.toJson(srv)) ?: continue
                        config.subscriptionId = subid
                        config.description = generateDescription(config)
                        configs.add(config to (JsonUtil.toJsonPretty(srv) ?: ""))
                    }
                    if (configs.isNotEmpty()) {
                        val keyToProfile = if (append) {
                            val ktp = mutableMapOf<String, ProfileItem>()
                            configs.forEach { (config, raw) ->
                                val key = MmkvManager.encodeServerConfig("", config)
                                if (raw.isNotBlank()) {
                                    MmkvManager.encodeServerRaw(key, raw)
                                }
                                ktp[key] = config
                            }
                            ktp
                        } else {
                            batchReplaceCustomConfigs(configs, subid)
                        }
                        val matchKey = findMatchedProfileKey(keyToProfile, removedSelected)
                        matchKey?.let { MmkvManager.setSelectServer(it) }
                        reconcileProfileAssignmentsAfterRefresh()
                        return keyToProfile.size
                    }
                    return 0
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
                    batchReplaceCustomConfigs(listOf(config to server), subid)
                } else {
                    val key = MmkvManager.encodeServerConfig("", config)
                    MmkvManager.encodeServerRaw(key, server)
                }
                reconcileProfileAssignmentsAfterRefresh()
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
                    batchReplaceCustomConfigs(listOf(config to server), subid)
                } else {
                    val key = MmkvManager.encodeServerConfig("", config)
                    MmkvManager.encodeServerRaw(key, server)
                }
                reconcileProfileAssignmentsAfterRefresh()
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
    fun updateConfigViaSubAll(quick: Boolean = false): SubscriptionUpdateResult {
        return try {
            val subscriptions = MmkvManager.decodeSubscriptions()
            subscriptions.fold(SubscriptionUpdateResult()) { acc, subscription ->
                acc + updateConfigViaSub(
                    subscription,
                    retries = if (quick) 1 else 4
                )
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
                    "B" -> String(Base64.decode(encoded, Base64.NO_WRAP), Charset.forName(charset))
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
        // Subscription headers commonly report expire in epoch SECONDS,
        // while the rest of the app works with epoch milliseconds.
        val normalize = { v: Long? -> v?.let { if (it < 100_000_000_000L) it * 1000L else it } }
        return Userinfo(normalize(upload), normalize(download), normalize(total), normalize(expire))
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
     * True when a label is a meaningless numeric/tech token (e.g. panels that
     * put just an id into content-disposition's filename="213"). Such tokens
     * should not override a human-readable name or the host-derived fallback.
     */
    private fun isNumericLabel(value: String): Boolean {
        val v = value.trim()
        return v.isEmpty() || v.all { it.isDigit() }
    }

    /**
     * Updates the configuration via a subscription.
     *
     * @param it The subscription item.
     * @return Subscription update result.
     */
    /**
     * Загружает подписку с ретраями.
     *
     * Шлюзы панелей нестабильны: один и тот же запрос может вернуть 200, 503
     * или 504 (чередуется). Ретраим только транзиентные ошибки (5xx/429/сеть)
     * с экспоненциальной паузой. Вызывается на IO-потоке, поэтому используем
     * Thread.sleep, а не delay (функция не suspend).
     */
    private fun fetchSubscriptionWithRetries(
        url: String,
        userAgent: String?,
        hwid: String?,
        viaProxy: Boolean,
        attempts: Int,
        proxyUsername: String?,
        proxyPassword: String?
    ): Pair<String, Map<String, String>>? {
        var backoffMs = 600L
        repeat(attempts) { attempt ->
            try {
                val httpPort = if (viaProxy) SettingsManager.getHttpPort() else 0
                val pair = HttpUtil.getUrlContentAndHeaders(
                    UrlContentRequest(
                        url = url,
                        userAgent = userAgent,
                        hwid = hwid,
                        timeout = 15000,
                        httpPort = httpPort,
                        proxyUsername = proxyUsername,
                        proxyPassword = proxyPassword
                    )
                )
                if (pair.first.isNotEmpty()) {
                    if (attempt > 0) {
                        LogUtil.i(AppConfig.TAG, "Subscription fetched on attempt ${attempt + 1}")
                    }
                    return pair
                }
            } catch (e: Exception) {
                val code = Regex("""status code (\d+)""")
                    .find(e.message.orEmpty())
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toIntOrNull()
                val transient = code?.let { it >= 500 || it == 429 } ?: true
                LogUtil.w(
                    AppConfig.TAG,
                    "Subscription fetch attempt ${attempt + 1}/$attempts failed" +
                        (if (code != null) " (HTTP $code)" else "") + ": ${e.message}"
                )
                if (!transient) return null
            }
            if (attempt < attempts - 1) {
                try {
                    Thread.sleep(backoffMs)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return null
                }
                backoffMs *= 2
            }
        }
        return null
    }

    /**
     * Возвращает UA, которые нужно перебрать при загрузке подписки.
     *
     * Если пользователь задал свой UA — перебираем только его (никаких лишних
     * запросов). Для «по умолчанию» (null/blank или явно сохранённый Happ,
     * который раньше был отдельным пресетом) — Happ, затем v2rayNG: некоторые
     * панели (например, shukalove.site) отвечают 500 на Happ.
     */
    internal fun subscriptionUserAgentCandidates(userAgent: String?): List<String> {
        val trimmed = userAgent?.trim().orEmpty()
        val isDefaultUa = trimmed.isEmpty() ||
            trimmed.equals(AppConfig.DEFAULT_SUBSCRIPTION_USER_AGENT, ignoreCase = true)
        return if (isDefaultUa) {
            listOf(AppConfig.DEFAULT_SUBSCRIPTION_USER_AGENT) + AppConfig.SUBSCRIPTION_USER_AGENT_FALLBACKS
        } else {
            listOf(trimmed)
        }
    }

    fun updateConfigViaSub(it: SubscriptionCache, retries: Int = 4): SubscriptionUpdateResult {
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
            // HWID: auto-generate once and persist, so Remnawave/XTLS panels see a
            // stable device id across updates instead of a fresh one every time.
            var hwid = it.subscription.hwid
            if (hwid.isNullOrBlank()) {
                hwid = java.util.UUID.randomUUID().toString()
                it.subscription.hwid = hwid
                MmkvManager.encodeSubscription(it.guid, it.subscription)
                LogUtil.i(AppConfig.TAG, "HWID auto-generated for ${it.subscription.remarks}: $hwid")
            }
            val proxyUsername = SettingsManager.getSocksUsername()
            val proxyPassword = SettingsManager.getSocksPassword()

            // Шлюзы панелей нестабильны: один и тот же запрос может вернуть
            // 200, 503 или 504 (чередуется). Поэтому несколько попыток с
            // экспоненциальной паузой, а не одна.
            //
            // UA-цепочка: если пользователь задал свой UA — используем только
            // его и не делаем лишних запросов. Иначе основной Happ, а при
            // отказе — v2rayNG (некоторые панели отвечают 500 на Happ).
            val defaultUa = AppConfig.DEFAULT_SUBSCRIPTION_USER_AGENT
            val userAgentCandidates = subscriptionUserAgentCandidates(userAgent)

            var responsePair: Pair<String, Map<String, String>>? = null
            var successfulUserAgent = userAgent?.takeIf { it.isNotBlank() } ?: defaultUa
            for ((index, candidate) in userAgentCandidates.withIndex()) {
                if (index == 0) {
                    // Основной UA сначала пробуем через локальный прокси, как
                    // раньше; запасные — сразу напрямую, чтобы не терять время.
                    responsePair = fetchSubscriptionWithRetries(
                        url = url,
                        userAgent = candidate,
                        hwid = hwid,
                        viaProxy = true,
                        attempts = 1,
                        proxyUsername = proxyUsername,
                        proxyPassword = proxyPassword
                    )
                }
                if (responsePair == null || responsePair.first.isEmpty()) {
                    responsePair = fetchSubscriptionWithRetries(
                        url = url,
                        userAgent = candidate,
                        hwid = hwid,
                        viaProxy = false,
                        attempts = retries,
                        proxyUsername = proxyUsername,
                        proxyPassword = proxyPassword
                    )
                }
                val fetched = responsePair
                if (fetched != null && fetched.first.isNotEmpty()) {
                    successfulUserAgent = candidate
                    if (index > 0) {
                        LogUtil.i(AppConfig.TAG, "Subscription fetched with fallback UA: $candidate")
                    }
                    break
                }
            }
            val fetchedSubscription = responsePair
            if (fetchedSubscription == null || fetchedSubscription.first.isEmpty()) {
                return SubscriptionUpdateResult(failureCount = 1)
            }

            var configText = fetchedSubscription.first
            var headers = fetchedSubscription.second
            var parsedCount = 0

            // Remnawave/XTLS HWID: a panel with HWID Device Limit reports
            // x-hwid-max-devices-reached: true and returns dummy nodes instead of
            // real configs. Don't overwrite existing servers with those dummies.
            // Проверяем ДО expanded-запроса и парсинга.
            if (isHwidLimitReached(headers)) {
                it.subscription.announce = hwidLimitMessage(configText)
                LogUtil.e(
                    AppConfig.TAG,
                    "Update subscription: HWID device limit reached for ${it.subscription.url} - ${it.subscription.announce}"
                )
                return SubscriptionUpdateResult(failureCount = 1)
            }

            // QRATOR/Mantaray panels report subscriptions-expand-now: 1 to signal that a
            // full-content (JSON config array) form is available. Request it with an UA that
            // the panel maps to an expanded-fetch client (e.g. " Happ"), so all profiles
            // (including auto/policy-group nodes) are imported, not just the URI lines.
            val expandNow = headers.entries
                .firstOrNull { it.key.equals("subscriptions-expand-now", ignoreCase = true) }
                ?.value
                ?.trim()
            if (!expandNow.isNullOrEmpty() && expandNow == "1") {
                // Если UA уже содержит Happ (например, дефолтный Happ/2.5.0),
                // не добавляем второй раз: «Happ/2.5.0 Happ» панель может
                // интерпретировать как неизвестного клиента.
                val expandedUserAgent = if (successfulUserAgent.contains("Happ", ignoreCase = true)) {
                    successfulUserAgent
                } else {
                    "$successfulUserAgent Happ"
                }
                val expandedResponse = try {
                    val httpPort = SettingsManager.getHttpPort()
                    HttpUtil.getUrlContentAndHeaders(
                        UrlContentRequest(
                            url = url,
                            userAgent = expandedUserAgent,
                            hwid = hwid,
                            timeout = 15000,
                            httpPort = httpPort,
                            proxyUsername = proxyUsername,
                            proxyPassword = proxyPassword
                        )
                    )
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.ANG_PACKAGE, "Update subscription: expanded fetch proxy not ready", e)
                    null
                } ?: try {
                    HttpUtil.getUrlContentAndHeaders(
                        UrlContentRequest(
                            url = url,
                            userAgent = expandedUserAgent,
                            hwid = hwid,
                            timeout = 15000
                        )
                    )
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "Update subscription: Failed to fetch expanded content", e)
                    null
                }
                if (expandedResponse != null && expandedResponse.first.isNotEmpty()) {
                    if (isHwidLimitReached(expandedResponse.second)) {
                        it.subscription.announce = hwidLimitMessage(expandedResponse.first)
                        LogUtil.e(
                            AppConfig.TAG,
                            "Update subscription: HWID device limit reached (expanded) for ${it.subscription.url} - ${it.subscription.announce}"
                        )
                        return SubscriptionUpdateResult(failureCount = 1)
                    }
                    LogUtil.i(AppConfig.TAG, "Subscription expanded: ${expandedResponse.first.length} bytes")
                    parsedCount = parseConfigViaSub(expandedResponse.first, it.guid, false)
                    if (parsedCount > 0) {
                        // Expanded-ответ успешно распарсен — исходный URI-список
                        // больше НЕ парсим, иначе узлы задвоятся.
                        configText = expandedResponse.first
                        headers = mergeHeaders(headers, expandedResponse.second)
                    } else {
                        // Expanded не распарсился (пустой/битый) — падаем
                        // обратно на исходный текст, ничего не потеряв.
                        LogUtil.w(
                            AppConfig.TAG,
                            "Subscription expanded response parsed no configs, falling back to original"
                        )
                    }
                }
            }

            val count = if (parsedCount > 0) {
                parsedCount
            } else {
                parseConfigViaSub(configText, it.guid, false)
            }
            if (count > 0) {
                it.subscription.lastUpdated = System.currentTimeMillis()

                // 1. Header metadata (Happ/XTLS): subscription-userinfo, profile-title,
                // profile-update-interval, support-url, web-page-url, announce, ...
                var titleAppliedFromHeader = false
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
                    if (key == "profile-update-interval") {
                        val hours = valStr.trim().toLongOrNull()
                        if (hours != null && hours > 0L && it.subscription.updateInterval == 1440L) {
                            // Happ reports the interval in hours; stored value is minutes.
                            // Only apply when the user hasn't customized the interval.
                            it.subscription.updateInterval = hours * 60L
                        }
                    }
                    if (key == "profile-title" || key == "subscription-title") {
                        val newTitle = decodeHeaderValue(valStr)
                        if (newTitle.isNotBlank() && shouldUseRemoteTitle(it.subscription.remarks, it.subscription.url)) {
                            it.subscription.remarks = newTitle
                            titleAppliedFromHeader = true
                        }
                    }
                    if (key == "content-disposition") {
                        val filename = parseContentDispositionFilename(valStr)
                        if (filename != null && filename.isNotBlank() &&
                            !titleAppliedFromHeader && !isNumericLabel(filename) &&
                            shouldUseRemoteTitle(it.subscription.remarks, it.subscription.url)
                        ) {
                            it.subscription.remarks = filename
                        }
                    }
                    if (key == "profile-description" || key == "subscription-description") {
                        val desc = decodeHeaderValue(valStr)
                        if (desc.isNotBlank()) {
                            it.subscription.description = desc
                        }
                    }
                    if (key == "announce") {
                        val announceText = decodeHeaderValue(valStr)
                        if (announceText.isNotBlank()) {
                            // Announcement text comes from the panel (subscription), not the app.
                            it.subscription.announce = announceText
                            // QRATOR/Happ panels sometimes ship the subscription information
                            // in the `announce` header without sending profile-description.
                            // In that case show the text as the description block (the announce
                            // itself is hidden when it equals the description).
                            if (it.subscription.description.isNullOrBlank()) {
                                it.subscription.description = announceText
                            }
                        }
                    }
                    if (key == "support-url" || key == "profile-web-page-url" || key == "announce-url") {
                        val link = valStr.trim()
                        if (link.isNotBlank()) {
                            when (key) {
                                "support-url" -> it.subscription.supportUrl = link
                                "profile-web-page-url" -> it.subscription.webPageUrl = link
                                "announce-url" -> it.subscription.announceUrl = link
                            }
                        }
                    }
                }

                // 2. Parse from body comments as fallback/enrichment
                runCatching {
                    // Happ/XTLS panels may ship metadata as comment lines in the
                    // subscription body (#profile-title, #subscription-userinfo, ...).
                    configText.lines().take(20).forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.startsWith("#") || trimmed.startsWith("//")) {
                            val commentContent = trimmed.substring(if (trimmed.startsWith("#")) 1 else 2).trim()
                            val colonIndex = commentContent.indexOf(':')
                            if (colonIndex > 0) {
                                val commentKey = commentContent.substring(0, colonIndex).trim().lowercase()
                                val commentVal = commentContent.substring(colonIndex + 1).trim()
                                if (commentVal.isNotBlank()) {
                                    val decodedVal = decodeHeaderValue(commentVal)
                                    when {
                                        commentKey == "profile-title" || commentKey == "subscription-title" ->
                                            if (shouldUseRemoteTitle(it.subscription.remarks, it.subscription.url)) {
                                                it.subscription.remarks = decodedVal
                                            }

                                        commentKey == "profile-description" || commentKey == "subscription-description" ->
                                            if (it.subscription.description.isNullOrBlank()) {
                                                it.subscription.description = decodedVal
                                            }

                                        commentKey == "subscription-userinfo" || commentKey == "x-subscription-userinfo" -> {
                                            if (it.subscription.uploadBytes == null && it.subscription.downloadBytes == null &&
                                                it.subscription.totalBytes == null && it.subscription.expireAt == null
                                            ) {
                                                val userinfo = parseSubscriptionUserinfo(commentVal)
                                                it.subscription.uploadBytes = userinfo.upload
                                                it.subscription.downloadBytes = userinfo.download
                                                it.subscription.totalBytes = userinfo.total
                                                it.subscription.expireAt = userinfo.expire
                                            }
                                        }

                                        commentKey == "profile-update-interval" -> {
                                            if (it.subscription.updateInterval == 1440L) {
                                                val hours = commentVal.toLongOrNull()
                                                if (hours != null && hours > 0L) {
                                                    it.subscription.updateInterval = hours * 60L
                                                }
                                            }
                                        }

                                        commentKey == "support-url" -> if (it.subscription.supportUrl.isNullOrBlank()) {
                                            it.subscription.supportUrl = decodedVal
                                        }

                                        commentKey == "profile-web-page-url" || commentKey == "web-page-url" ->
                                            if (it.subscription.webPageUrl.isNullOrBlank()) {
                                                it.subscription.webPageUrl = decodedVal
                                            }

                                        commentKey == "announce-url" -> if (it.subscription.announceUrl.isNullOrBlank()) {
                                            it.subscription.announceUrl = decodedVal
                                        }

                                        commentKey == "announce" -> if (it.subscription.announce.isNullOrBlank()) {
                                            // Announcement text comes from the panel (subscription), not the app.
                                            it.subscription.announce = decodedVal
                                            // Same fallback as with the header: when the panel does not
                                            // send a profile-description, use the announce text as the
                                            // description block.
                                            if (it.subscription.description.isNullOrBlank()) {
                                                it.subscription.description = decodedVal
                                            }
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

                // Если панель прислала один и тот же текст и в profile-description,
                // и в announce — не показываем в UI два одинаковых блока.
                // Оставляем description, дублирующий announce убираем.
                val descriptionText = it.subscription.description?.trim()
                val announceText = it.subscription.announce?.trim()
                if (descriptionText != null && descriptionText.isNotEmpty() &&
                    announceText != null && announceText.isNotEmpty() &&
                    descriptionText.equals(announceText, ignoreCase = true)
                ) {
                    it.subscription.announce = null
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
        // Happ/XTLS subscriptions may carry a title in the URL fragment
        // (#Title or #Title?installid=...) — strip params and decode it.
        subItem.remarks = uri.fragment
            ?.substringBefore('?')
            ?.let { runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "import sub"
        subItem.url = url
        MmkvManager.encodeSubscription("", subItem)
        return 1
    }

    /**
     * True when the local subscription label is still a placeholder/derived value
     * (template text, URL, host-derived domain, or a raw base64 summary), so a
     * proper remote title from the panel should replace it.
     */
    private fun shouldUseRemoteTitle(remarks: String?, url: String?): Boolean {
        val current = remarks.orEmpty()
        if (current == "import sub" || current.startsWith("http") || current == "Подписка") {
            return true
        }
        if (current.startsWith("base64:") || current.contains("%3D%3D") || current.contains("8J+Q") || current.contains("eyJ")) {
            return true
        }
        val host = runCatching { URI(url.orEmpty()).host?.removePrefix("www.") }.getOrNull()
        return host != null && current == host
    }

    /**
     * True when the panel reported that the HWID device limit is reached and
     * the body contains dummy nodes instead of real configs.
     */
    private fun isHwidLimitReached(headers: Map<String, String>): Boolean {
        return headers.entries
            .firstOrNull { it.key.equals("x-hwid-max-devices-reached", ignoreCase = true) }
            ?.value
            ?.trim() == "true"
    }

    /** Human-readable message for the HWID device limit case. */
    private fun hwidLimitMessage(configText: String): String {
        return extractHwidDummyRemark(configText)?.takeIf { it.isNotBlank() }
            ?: "Слишком много устройств: освободите слот в боте («Настройка» → «Сессии») или докупите устройства"
    }

    /**
     * Merges subscription headers case-insensitively. Values from [override]
     * win over [base] with the same header name in any letter case.
     */
    private fun mergeHeaders(
        base: Map<String, String>,
        override: Map<String, String>
    ): Map<String, String> {
        if (override.isEmpty()) return base
        val merged = LinkedHashMap<String, String>(base)
        override.forEach { (key, value) ->
            merged.keys.firstOrNull { it.equals(key, ignoreCase = true) }?.let { merged.remove(it) }
            merged[key] = value
        }
        return merged
    }

    /**
     * Extracts the first human-readable remark from an HWID-limit dummy payload
     * (JSON config array "remarks" field, or a URL-encoded fragment of the first
     * vless line in a base64 batch). Returns null when the payload has no text.
     */
    private fun extractHwidDummyRemark(configText: String): String? {
        return try {
            val candidate: String? = if (configText.trimStart().startsWith("[")) {
                val arr = JsonUtil.fromJson(configText, Array<Any>::class.java) ?: return null
                val first = arr.firstOrNull()
                if (first is Map<*, *>) first["remarks"]?.toString() else null
            } else {
                // base64 batch: first "xxx://...#<urlencoded name>" line
                val decoded = String(Base64.decode(configText, Base64.NO_WRAP), Charsets.UTF_8)
                val line = decoded.lineSequence().firstOrNull { it.contains("#") }
                line?.substringAfter("#", "")?.let { java.net.URLDecoder.decode(it, "UTF-8") }
            }
            candidate?.trim()?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeHeaderValue(value: String): String {
        val trimmed = value.trim()
            .removePrefix("\"")
            .removeSuffix("\"")
            .trim()
        if (trimmed.startsWith("base64:", ignoreCase = true)) {
            val payload = trimmed.substring("base64:".length).trim()
            return try {
                String(Base64.decode(payload, Base64.NO_WRAP), Charsets.UTF_8).trim()
            } catch (_: Exception) {
                trimmed
            }
        }
        val decodedRfc = decodeRfc2047(trimmed)
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

        val addrPart = Utils.maskServerAddress(server)

        return "$addrPart : ${port ?: ""}"
    }
}
