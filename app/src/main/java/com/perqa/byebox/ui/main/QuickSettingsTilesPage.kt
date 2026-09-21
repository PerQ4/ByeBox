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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    val subtitle: String
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
                    subtitle = Loc.get("tiles_toggle_sub", language)
                )
            )
            add(
                TileDef(
                    id = "cycle",
                    service = ByeBoxProfileTileService::class.java,
                    qsLabel = "ByeBox Presets",
                    title = Loc.get("tiles_cycle", language),
                    subtitle = Loc.get("tiles_cycle_sub", language)
                )
            )
            if (showTgws) {
                add(
                    TileDef(
                        id = "tgws",
                        service = ProxyTileService::class.java,
                        qsLabel = "Telegram WS Proxy",
                        title = Loc.get("tiles_tgws", language),
                        subtitle = Loc.get("tiles_tgws_sub", language)
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
        Text(
            text = Loc.get("tiles_title", language),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black)
        )
        Text(
            text = Loc.get("tiles_desc", language),
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )

        tiles.forEach { tile ->
            val isAdded = added[tile.id] ?: false
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = {
                        if (!supported) {
                            Toast.makeText(context, Loc.get("tiles_unsupported", language), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        controller?.let { c ->
                            c.requestAdd(tile.service, tile.qsLabel) { ok -> if (ok) added[tile.id] = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(if (cornerRoundness == "expressive") 26.dp else 14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAdded) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.primary,
                        contentColor = if (isAdded) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = if (isAdded) Loc.get("tiles_added", language) else Loc.get("tiles_add", language),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                    )
                }
                Text(
                    text = tile.title.plus("\n").plus(tile.subtitle),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        Text(
            text = if (supported) Loc.get("tiles_note", language) else Loc.get("tiles_unsupported", language),
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Text(
            text = Loc.get("tiles_longpress", language),
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}