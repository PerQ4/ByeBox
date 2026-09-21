package com.perqa.byebox.ui.main

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class RouteApp(val pkg: String, val label: String)

@Composable
fun RoutingScreen(
    scaleFactor: Float,
    cornerRoundness: String,
    language: String,
    viewModel: MainScreenViewModel
) {
    val ctx = LocalContext.current
    val pm = ctx.packageManager
    val store = remember { RoutingStore(ctx) }
    val defaultStr by store.defaultChannel.collectAsState(RouteChannel.VPN.value)
    val defaultChannel = RouteChannel.from(defaultStr)
    val appChannelsStr by store.appChannels.collectAsState(emptyMap())
    val appChannels = appChannelsStr.mapValues { RouteChannel.from(it.value) }
    var search by remember { mutableStateOf("") }
    val cr = cornerRoundness.toIntOrNull() ?: 16

    val apps = remember {
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map { RouteApp(it.packageName, pm.getApplicationLabel(it).toString()) }
            .sortedBy { it.label.lowercase() }
    }

    val filtered = if (search.isBlank()) apps else apps.filter {
        it.label.contains(search, true) || it.pkg.contains(search, true)
    }

    Column(modifier = Modifier.fillMaxWidth()    ) {
        SettingsGroup(title = Loc.get("routing_default", language)) {
            SegmentedChannelSelector(
                selected = defaultChannel,
                onSelect = { ch ->
                    CoroutineScope(Dispatchers.IO).launch { store.setDefaultChannel(ch.value) }
                },
                cornerRoundness = cr,
                language = language
            )
        }

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(Loc.get("search_apps", language)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(cr.dp)
        )

        Spacer(Modifier.height(14.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filtered.forEach { app ->
                val current = appChannels[app.pkg] ?: defaultChannel
                val isOverride = appChannelsStr.containsKey(app.pkg)
                AppRoutingItem(
                    app = app,
                    current = current,
                    isOverride = isOverride,
                    onSelect = { ch ->
                        CoroutineScope(Dispatchers.IO).launch { store.setAppChannel(app.pkg, ch.value) }
                    },
                    onReset = {
                        CoroutineScope(Dispatchers.IO).launch { store.setAppChannel(app.pkg, RouteChannel.DEFAULT.value) }
                    },
                    cornerRoundness = cr,
                    language = language
                )
            }
        }
    }
}

@Composable
fun SegmentedChannelSelector(
    selected: RouteChannel,
    onSelect: (RouteChannel) -> Unit,
    cornerRoundness: Int,
    language: String,
    modifier: Modifier = Modifier
) {
    val options = listOf(RouteChannel.VPN, RouteChannel.DIRECT)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { ch ->
            val sel = selected == ch
            val label = when (ch) {
                RouteChannel.VPN -> Loc.get("channel_vpn", language)
                else -> Loc.get("channel_direct", language)
            }
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(cornerRoundness.dp))
                    .clickable { onSelect(ch) }
                    .background(
                        if (sel) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (sel) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun AppRoutingItem(
    app: RouteApp,
    current: RouteChannel,
    isOverride: Boolean,
    onSelect: (RouteChannel) -> Unit,
    onReset: () -> Unit,
    cornerRoundness: Int,
    language: String
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRoundness.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.pkg,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val chLabel = when (current) {
                RouteChannel.VPN -> Loc.get("channel_vpn", language)
                RouteChannel.DIRECT -> Loc.get("channel_direct", language)
                else -> Loc.get("channel_default", language)
            }
            Text(
                text = if (isOverride) chLabel else "${Loc.get("channel_default", language)}: $chLabel",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(top = 10.dp)) {
                SegmentedChannelSelector(
                    selected = current,
                    onSelect = onSelect,
                    cornerRoundness = cornerRoundness,
                    language = language
                )
                if (isOverride) {
                    Text(
                        text = Loc.get("routing_reset", language),
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable { onReset() },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
