package com.perqa.byebox.ui.main

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.routingDataStore: DataStore<Preferences> by preferencesDataStore(name = "routing_prefs")

object RoutingKeys {
    val DEFAULT_CHANNEL = stringPreferencesKey("routing_default_channel")
    val APP_CHANNELS = stringSetPreferencesKey("routing_app_channels")
}

enum class RouteChannel(val value: String) {
    DEFAULT("default"),
    VPN("vpn"),
    DIRECT("direct");

    companion object {
        fun from(v: String): RouteChannel = values().firstOrNull { it.value == v } ?: DEFAULT
    }
}

class RoutingStore(private val context: Context) {

    val defaultChannel: Flow<String> = context.routingDataStore.data.map { prefs ->
        prefs[RoutingKeys.DEFAULT_CHANNEL] ?: RouteChannel.VPN.value
    }

    val appChannels: Flow<Map<String, String>> = context.routingDataStore.data.map { prefs ->
        (prefs[RoutingKeys.APP_CHANNELS] ?: emptySet()).mapNotNull { entry ->
            val idx = entry.indexOf(':')
            if (idx <= 0) null else entry.substring(0, idx) to entry.substring(idx + 1)
        }.toMap()
    }

    suspend fun setDefaultChannel(channel: String) {
        context.routingDataStore.edit { it[RoutingKeys.DEFAULT_CHANNEL] = channel }
    }

    suspend fun setAppChannel(packageName: String, channel: String) {
        context.routingDataStore.edit { prefs ->
            val set = (prefs[RoutingKeys.APP_CHANNELS] ?: emptySet()).toMutableSet()
            set.removeAll { it.startsWith("$packageName:") }
            if (channel != RouteChannel.DEFAULT.value) set.add("$packageName:$channel")
            prefs[RoutingKeys.APP_CHANNELS] = set
        }
    }

    suspend fun resetAllAppChannels() {
        context.routingDataStore.edit { it[RoutingKeys.APP_CHANNELS] = emptySet() }
    }
}
