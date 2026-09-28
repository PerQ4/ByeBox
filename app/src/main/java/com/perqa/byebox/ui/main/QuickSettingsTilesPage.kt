package com.perqa.byebox.ui.main

import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.amurcanov.tgwsproxy.ProxyTileService
import com.perqa.byebox.R
import com.perqa.byebox.findActivity
import com.perqa.byebox.service.ByeBoxProfileTileService
import com.perqa.byebox.service.ByeBoxTileService

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class QsTileController(private val activity: ComponentActivity) {
    private val sbm = activity.getSystemService(StatusBarManager::class.java)

    fun requestAdd(service: Class<*>, label: String, onResult: (Boolean) -> Unit) {
        sbm?.requestAddTileService(
            ComponentName(activity, service),
            label,
            Icon.createWithResource(activity, R.drawable.ic_notification_on),
            activity.mainExecutor
        ) { result ->
            onResult(
                result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
                    result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
            )
        }
    }
}

private data class TileDef(
    val id: String,
    val service: Class<*>,
    val qsLabel: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
fun QuickSettingsTilesPage(
    scaleFactor: Float,
    cornerRoundness: String,
    language: String,
    showTgws: Boolean
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    val tiles = remember(language, showTgws) {
        buildList {
            add(
                TileDef(
                    id = "toggle",
                    service = ByeBoxTileService::class.java,
                    qsLabel = "ByeBox VPN",
                    title = Loc.get("tiles_toggle", language),
                    subtitle = Loc.get("tiles_toggle_sub", language),
                    icon = Icons.Filled.PowerSettingsNew
                )
            )
            add(
                TileDef(
                    id = "cycle",
                    service = ByeBoxProfileTileService::class.java,
                    qsLabel = "ByeBox Presets",
                    title = Loc.get("tiles_cycle", language),
                    subtitle = Loc.get("tiles_cycle_sub", language),
                    icon = Icons.Filled.Repeat
                )
            )
            if (showTgws) {
                add(
                    TileDef(
                        id = "tgws",
                        service = ProxyTileService::class.java,
                        qsLabel = "Telegram WS Proxy",
                        title = Loc.get("tiles_tgws", language),
                        subtitle = Loc.get("tiles_tgws_sub", language),
                        icon = Icons.AutoMirrored.Filled.Send
                    )
                )
            }
        }
    }

    val controller = remember(activity, supported) {
        if (supported) activity?.let { QsTileController(it) } else null
    }
    val added = remember { mutableStateMapOf<String, Boolean>() }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingsGroup(title = Loc.get("tiles_group", language)) {
            tiles.forEachIndexed { index, tile ->
                val isAdded = added[tile.id] ?: false
                SettingsActionRow(
                    title = tile.title,
                    subtitle = tile.subtitle,
                    button = if (isAdded) Loc.get("tiles_added", language) else Loc.get("tiles_add", language),
                    enabled = true,
                    icon = tile.icon,
                    selected = isAdded,
                    top = index == 0,
                    bottom = index == tiles.lastIndex,
                    scaleFactor = scaleFactor,
                    cornerRoundness = cornerRoundness,
                    onClick = {
                        if (!supported) {
                            Toast.makeText(context, Loc.get("tiles_unsupported", language), Toast.LENGTH_SHORT).show()
                            return@SettingsActionRow
                        }
                        controller?.let { c ->
                            c.requestAdd(tile.service, tile.qsLabel) { ok -> if (ok) added[tile.id] = true }
                        }
                    }
                )
            }
        }

        Text(
            text = if (supported) Loc.get("tiles_note", language) else Loc.get("tiles_unsupported", language),
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.settingsSubtitleColor),
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Text(
            text = Loc.get("tiles_longpress", language),
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.settingsSubtitleColor),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}