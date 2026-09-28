package com.perqa.byebox.ui.main

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
            Icon.createWithResource(activity, R.drawable.ic_tile_telegram),
            activity.mainExecutor
        ) { result ->
            onResult(
                result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
                    result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
            )
        }
    }

    /**
     * Реальное состояние плитки у системы (добавлена ли она в шторку).
     * getTileServiceState — скрытый @SystemApi, поэтому вызывается через рефлексию;
     * при недоступности (заблокировано политикой hidden API) возвращает null,
     * и страница опирается на сохранённый в SharedPreferences результат.
     */
    fun isTileAdded(service: Class<*>): Boolean? = runCatching {
        val method = StatusBarManager::class.java.getMethod(
            "getTileServiceState",
            ComponentName::class.java
        )
        val state = method.invoke(sbm, ComponentName(activity, service)) as? Int
            ?: return@runCatching null
        state == Tile.STATE_ACTIVE || state == Tile.STATE_INACTIVE
    }.getOrNull()
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
    val prefs = remember { context.getSharedPreferences("qs_tiles", Context.MODE_PRIVATE) }
    val added = remember { mutableStateMapOf<String, Boolean>() }

    // При открытии страницы сверяемся с системой; если скрытый API недоступен —
    // берём из SharedPreferences, чтобы плитка не «забывала», что уже добавлена.
    LaunchedEffect(controller, tiles) {
        tiles.forEach { tile ->
            val system = controller?.isTileAdded(tile.service)
            added[tile.id] = if (system != null) {
                prefs.edit().putBoolean(tile.id, system).apply()
                system
            } else {
                prefs.getBoolean(tile.id, false)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingsGroup(title = Loc.get("tiles_group", language)) {
            tiles.forEachIndexed { index, tile ->
                val isAdded = added[tile.id] ?: false
                TileAddRow(
                    tile = tile,
                    isAdded = isAdded,
                    top = index == 0,
                    bottom = index == tiles.lastIndex,
                    scaleFactor = scaleFactor,
                    cornerRoundness = cornerRoundness,
                    language = language,
                    supported = supported,
                    context = context,
                    onRequestAdd = {
                        controller?.let { c ->
                            c.requestAdd(tile.service, tile.qsLabel) { ok ->
                                if (ok) {
                                    added[tile.id] = true
                                    prefs.edit().putBoolean(tile.id, true).apply()
                                }
                            }
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

/**
 * Ряд добавления плитки: кнопка-бейдж стоит в линию с заголовком,
 * а описание занимает всю ширину ряда и всегда читается целиком.
 */
@Composable
private fun TileAddRow(
    tile: TileDef,
    isAdded: Boolean,
    top: Boolean,
    bottom: Boolean,
    scaleFactor: Float,
    cornerRoundness: String,
    language: String,
    supported: Boolean,
    context: Context,
    onRequestAdd: () -> Unit
) {
    val onClick = {
        if (!supported) {
            Toast.makeText(context, Loc.get("tiles_unsupported", language), Toast.LENGTH_SHORT).show()
        } else {
            onRequestAdd()
        }
    }

    SettingsRowSurface(
        top = top,
        bottom = bottom,
        selected = isAdded,
        scaleFactor = scaleFactor,
        cornerRoundness = cornerRoundness,
        onClick = onClick
    ) {
        SettingsRowIcon(
            icon = tile.icon,
            containerAlpha = if (isAdded) 0.42f else 0.18f,
            contentColor = if (isAdded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            cornerRoundness = cornerRoundness
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(settingsControlRadius(cornerRoundness)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isAdded) Loc.get("tiles_added", language) else Loc.get("tiles_add_btn", language),
                        maxLines = 1
                    )
                }
            }
            Text(
                text = tile.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.settingsSubtitleColor
                ),
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}