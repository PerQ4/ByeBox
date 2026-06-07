package com.perqa.byebox.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

interface DataRepository {
    val configs: StateFlow<List<ProxyConfig>>
    val activeConfigId: StateFlow<String?>
    val subscriptionSources: StateFlow<List<SubscriptionSource>>
    fun addConfig(config: ProxyConfig)
    fun addConfigFromUrl(url: String): Boolean
    fun upsertSubscriptionSource(source: SubscriptionSource, configs: List<ProxyConfig>)
    fun renameSubscriptionSource(sourceId: String, newName: String)
    fun deleteSubscriptionSource(sourceId: String)
    fun selectConfig(id: String)
    fun deleteConfig(id: String)
    fun updatePing(id: String, ping: Int)
}

class DefaultDataRepository(private val context: Context? = null) : DataRepository {
    private val _configs = MutableStateFlow<List<ProxyConfig>>(emptyList())
    override val configs: StateFlow<List<ProxyConfig>> = _configs.asStateFlow()

    private val _activeConfigId = MutableStateFlow<String?>(null)
    override val activeConfigId: StateFlow<String?> = _activeConfigId.asStateFlow()
    private val _subscriptionSources = MutableStateFlow<List<SubscriptionSource>>(emptyList())
    override val subscriptionSources: StateFlow<List<SubscriptionSource>> = _subscriptionSources.asStateFlow()
    private val prefs by lazy {
        context?.getSharedPreferences("byebox_data", Context.MODE_PRIVATE)
    }

    init {
        val restored = restoreConfigs()
        _subscriptionSources.value = restoreSources()
        if (restored.isNotEmpty()) {
            _configs.value = restored
            _activeConfigId.value = prefs?.getString(KEY_ACTIVE_CONFIG_ID, null)
                ?.takeIf { id -> restored.any { it.id == id } }
                ?: restored.firstOrNull()?.id
        } else {
            val defaultNodes = defaultConfigs()
            _configs.value = defaultNodes
            _activeConfigId.value = "1"
            persist()
        }
    }

    private fun defaultConfigs(): List<ProxyConfig> {
        return listOf(
            ProxyConfig(
                id = "1",
                name = "DE - Frankfurt Reality Fast",
                protocol = "VLESS",
                address = "de.hiddify.express",
                port = 443,
                uuid = UUID.randomUUID().toString(),
                security = "reality",
                flow = "xtls-rprx-vision",
                ping = 54,
                sourceName = "Hiddify Demo",
                countryFlag = "🇩🇪"
            ),
            ProxyConfig(
                id = "2",
                name = "US - New York Core VMess",
                protocol = "VMESS",
                address = "us.hiddify.express",
                port = 80,
                uuid = UUID.randomUUID().toString(),
                security = "none",
                ping = 112,
                sourceName = "Hiddify Demo",
                countryFlag = "🇺🇸"
            ),
            ProxyConfig(
                id = "3",
                name = "NL - Amsterdam Trojan Secure",
                protocol = "Trojan",
                address = "nl.hiddify.express",
                port = 8443,
                uuid = "securepass123",
                ping = 62,
                sourceName = "Hiddify Demo",
                countryFlag = "🇳🇱"
            ),
            ProxyConfig(
                id = "4",
                name = "RU - Moscow Xray Direct",
                protocol = "Shadowsocks",
                address = "ru.hiddify.express",
                port = 1080,
                uuid = "chacha20-ietf-poly1305:mypassword",
                ping = 21,
                sourceName = "Manual",
                countryFlag = "🇷🇺"
            )
        )
    }

    override fun addConfig(config: ProxyConfig) {
        _configs.value = dedupeConfigs(_configs.value + config)
        if (_activeConfigId.value == null) {
            _activeConfigId.value = config.id
        }
        persist()
    }

    override fun upsertSubscriptionSource(source: SubscriptionSource, configs: List<ProxyConfig>) {
        val stampedSource = source.copy(
            lastUpdatedAt = source.lastUpdatedAt ?: System.currentTimeMillis(),
            nodeCount = configs.size
        )
        _subscriptionSources.value = (_subscriptionSources.value.filter { it.id != stampedSource.id } + stampedSource)
            .sortedBy { it.name.lowercase() }

        val stampedConfigs = configs.map {
            it.copy(sourceName = stampedSource.name, sourceUrl = stampedSource.url)
        }
        _configs.value = dedupeConfigs(
            _configs.value.filterNot { it.sourceUrl == stampedSource.url } + stampedConfigs
        )
        if (_activeConfigId.value == null || _configs.value.none { it.id == _activeConfigId.value }) {
            _activeConfigId.value = _configs.value.firstOrNull()?.id
        }
        persist()
    }

    override fun deleteSubscriptionSource(sourceId: String) {
        val source = _subscriptionSources.value.find { it.id == sourceId } ?: return
        _subscriptionSources.value = _subscriptionSources.value.filterNot { it.id == sourceId }
        _configs.value = _configs.value.filterNot { it.sourceUrl == source.url }
        if (_configs.value.none { it.id == _activeConfigId.value }) {
            _activeConfigId.value = _configs.value.firstOrNull()?.id
        }
        persist()
    }

    override fun renameSubscriptionSource(sourceId: String, newName: String) {
        val cleanName = newName.trim()
        if (cleanName.isBlank()) return
        val source = _subscriptionSources.value.find { it.id == sourceId } ?: return
        _subscriptionSources.value = _subscriptionSources.value.map {
            if (it.id == sourceId) it.copy(name = cleanName) else it
        }.sortedBy { it.name.lowercase() }
        _configs.value = _configs.value.map {
            if (it.sourceUrl == source.url) it.copy(sourceName = cleanName) else it
        }
        persist()
    }

    override fun addConfigFromUrl(url: String): Boolean {
        val parsed = ConfigParser.parse(url)
        return if (parsed != null) {
            addConfig(parsed.copy(sourceName = "Manual", sourceUrl = null))
            true
        } else {
            false
        }
    }

    override fun selectConfig(id: String) {
        if (_configs.value.any { it.id == id }) {
            _activeConfigId.value = id
            persist()
        }
    }

    override fun deleteConfig(id: String) {
        _configs.value = _configs.value.filter { it.id != id }
        if (_activeConfigId.value == id) {
            _activeConfigId.value = _configs.value.firstOrNull()?.id
        }
        persist()
    }

    override fun updatePing(id: String, ping: Int) {
        _configs.value = _configs.value.map {
            if (it.id == id) {
                if (ping >= 999) {
                    it.copy(
                        ping = ping,
                        failureCount = (it.failureCount + 1).coerceAtMost(99),
                        lastFailureAt = System.currentTimeMillis()
                    )
                } else {
                    it.copy(
                        ping = ping,
                        failureCount = 0,
                        lastFailureAt = null
                    )
                }
            } else {
                it
            }
        }
        persist()
    }

    private fun persist() {
        val prefs = prefs ?: return
        val configsJson = JSONArray()
        _configs.value.forEach { configsJson.put(it.toJson()) }
        val sourcesJson = JSONArray()
        _subscriptionSources.value.forEach { sourcesJson.put(it.toJson()) }
        prefs.edit()
            .putString(KEY_CONFIGS, configsJson.toString())
            .putString(KEY_SOURCES, sourcesJson.toString())
            .putString(KEY_ACTIVE_CONFIG_ID, _activeConfigId.value)
            .apply()
    }

    private fun restoreSources(): List<SubscriptionSource> {
        val raw = prefs?.getString(KEY_SOURCES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getJSONObject(index).toSubscriptionSource())
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun dedupeConfigs(configs: List<ProxyConfig>): List<ProxyConfig> {
        return configs
            .distinctBy { "${it.protocol}|${it.address}|${it.port}|${it.uuid}|${it.name}" }
            .sortedWith(compareBy<ProxyConfig> { it.sourceName.lowercase() }.thenBy { it.name.lowercase() })
    }

    private fun restoreConfigs(): List<ProxyConfig> {
        val raw = prefs?.getString(KEY_CONFIGS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(ProxyConfig.fromJson(array.getJSONObject(index)))
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun SubscriptionSource.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("name", name)
            .put("url", url)
            .put("lastUpdatedAt", lastUpdatedAt)
            .put("nodeCount", nodeCount)
            .put("uploadBytes", uploadBytes)
            .put("downloadBytes", downloadBytes)
            .put("totalBytes", totalBytes)
            .put("expireAt", expireAt)
    }

    private fun JSONObject.toSubscriptionSource(): SubscriptionSource {
        return SubscriptionSource(
            id = optString("id"),
            name = optString("name"),
            url = optString("url"),
            lastUpdatedAt = optNullableLong("lastUpdatedAt"),
            nodeCount = optInt("nodeCount"),
            uploadBytes = optNullableLong("uploadBytes"),
            downloadBytes = optNullableLong("downloadBytes"),
            totalBytes = optNullableLong("totalBytes"),
            expireAt = optNullableLong("expireAt")
        )
    }

    private fun JSONObject.optNullableLong(name: String): Long? {
        return if (isNull(name)) null else optLong(name)
    }

    companion object {
        private const val KEY_CONFIGS = "configs"
        private const val KEY_SOURCES = "subscription_sources"
        private const val KEY_ACTIVE_CONFIG_ID = "active_config_id"
    }
}


