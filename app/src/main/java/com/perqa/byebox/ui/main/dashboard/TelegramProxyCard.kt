package com.perqa.byebox.ui.main.dashboard

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amurcanov.tgwsproxy.ProxyController
import com.amurcanov.tgwsproxy.ProxyService
import com.amurcanov.tgwsproxy.R as TgwsR
import com.amurcanov.tgwsproxy.SettingsStore
import com.amurcanov.tgwsproxy.ui.openTelegram
import com.perqa.byebox.ui.main.Loc
import com.perqa.byebox.ui.main.ModeChip
import com.perqa.byebox.ui.main.ProxyStatusPanel
import com.perqa.byebox.ui.main.SettingsRowSurface
import com.perqa.byebox.ui.main.applyToTelegramPackages
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Telegram WS proxy card for the main dashboard.
 *
 * Header mirrors SourceGroupCard: title + status subtitle + trailing badges
 * (pool, port) plus a connect dot selector. Expanded content reuses the
 * standalone tgws screen pieces (ProxyStatusPanel, ModeChip, copy row).
 */
@Composable
fun TelegramProxyCard(
    language: String = "ru",
    cornerRoundness: String = "expressive",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }

    val isRunning by ProxyService.isRunning.collectAsStateWithLifecycle()
    val isVerifiedRunning by ProxyService.isVerifiedRunning.collectAsStateWithLifecycle()
    val cfEnabled by settingsStore.cfproxyEnabled.collectAsStateWithLifecycle(initialValue = true)
    val poolSize by settingsStore.poolSize.collectAsStateWithLifecycle(initialValue = 4)
    val port by settingsStore.port.collectAsStateWithLifecycle(initialValue = "1443")
    val bindIp by settingsStore.bindIp.collectAsStateWithLifecycle(initialValue = "127.0.0.1")
    val secretKey by settingsStore.secretKey.collectAsStateWithLifecycle(initialValue = "")

    var isStarting by remember { mutableStateOf(false) }
    var applyMode by remember { mutableStateOf("packages") }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (settingsStore.secretKey.first().isBlank()) {
            val bytes = ByteArray(16)
            java.security.SecureRandom().nextBytes(bytes)
            settingsStore.saveSecretKey(bytes.joinToString("") { "%02x".format(it) })
        }
    }

    val statusText = when {
        isVerifiedRunning -> Loc.get("tgws_status_connected", language)
        isStarting || isRunning -> Loc.get("tgws_status_connecting", language)
        else -> Loc.get("tgws_status_disconnected", language)
    }

    LaunchedEffect(isRunning, isVerifiedRunning) {
        if (isVerifiedRunning || !isRunning) isStarting = false
    }

    val connectAction = {
        if (!isRunning && !isStarting) {
            isStarting = true
            scope.launch {
                if (!ProxyController.startFromSavedSettings(context, showInvalidPortToast = true)) isStarting = false
            }
        }
    }
    val disconnectAction = {
        if (isRunning || isStarting) ProxyController.stop(context)
    }
    val isActiveVisual = isRunning || isStarting

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (cornerRoundness == "expressive") 28.dp else 16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TelegramHeader(
                title = Loc.get("title_telegram", language),
                statusText = statusText,
                isActive = isActiveVisual,
                isVerified = isVerifiedRunning,
                isExpanded = expanded,
                language = language,
                onToggle = { expanded = !expanded },
                onToggleService = { if (isActiveVisual) disconnectAction() else connectAction() }
            )

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                TelegramExpandedContent(
                    context = context,
                    language = language,
                    isVerifiedRunning = isVerifiedRunning,
                    isStarting = isStarting,
                    isRunning = isRunning,
                    cfEnabled = cfEnabled,
                    poolSize = poolSize,
                    port = port,
                    bindIp = bindIp,
                    secretKey = secretKey,
                    applyMode = applyMode,
                    onApplyModeChange = { applyMode = it },
                    cornerRoundness = cornerRoundness
                )
            }
        }
    }
}

@Composable
internal fun TelegramHeader(
    title: String,
    statusText: String,
    isActive: Boolean,
    isVerified: Boolean,
    isExpanded: Boolean,
    language: String,
    onToggle: () -> Unit,
    onToggleService: () -> Unit
) {
    val pillColor = when {
        isActive && isVerified -> MaterialTheme.colorScheme.primary
        isActive -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val pillContentColor = when {
        isActive && isVerified -> MaterialTheme.colorScheme.onPrimary
        isActive -> MaterialTheme.colorScheme.onTertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val pillText = statusText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = TgwsR.drawable.ic_telegram_logo),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
                    alpha = if (isActive) 1f else 0.6f
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = pillColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onToggleService() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(pillContentColor)
                    )
                    Text(
                        text = pillText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = pillContentColor
                        )
                    )
                }
            }

            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) Loc.get("collapse", language) else Loc.get("expand", language),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
internal fun TelegramExpandedContent(
    context: Context,
    language: String = "ru",
    isVerifiedRunning: Boolean,
    isStarting: Boolean,
    isRunning: Boolean,
    cfEnabled: Boolean,
    poolSize: Int,
    port: String,
    bindIp: String,
    secretKey: String,
    applyMode: String,
    onApplyModeChange: (String) -> Unit,
    cornerRoundness: String
) {
    val portInt = port.toIntOrNull() ?: 1443
    val secretForUrl = secretKey.trim().takeIf { it.isNotEmpty() } ?: "0".repeat(32)
    val host = bindIp.trim().takeIf { it.isNotEmpty() } ?: "127.0.0.1"
    val proxyUrl = "https://t.me/proxy?server=$host&port=$portInt&secret=dd$secretForUrl"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = {
                if (applyMode == "packages") applyToTelegramPackages(context, proxyUrl, language)
                else openTelegram(context, proxyUrl)
            },
            enabled = isRunning,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(if (cornerRoundness == "expressive") 24.dp else 14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        ) {
            Text(
                stringResource(TgwsR.string.apply_in_telegram),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeChip(
                label = "Пакеты",
                selected = applyMode == "packages",
                modifier = Modifier.weight(1f).height(44.dp)
            ) { onApplyModeChange("packages") }
            ModeChip(
                label = "Ссылка",
                selected = applyMode == "link",
                modifier = Modifier.weight(1f).height(44.dp)
            ) { onApplyModeChange("link") }
        }

        ProxyStatusPanel(
            cfEnabled = cfEnabled,
            poolSize = poolSize,
            port = port
        )

        SettingsRowSurface(
            cornerRoundness = cornerRoundness,
            onClick = {
                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(android.content.ClipData.newPlainText("Proxy", proxyUrl))
                Toast.makeText(context, TgwsR.string.copied, Toast.LENGTH_SHORT).show()
            }
        ) {
            Text(
                text = proxyUrl,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = stringResource(TgwsR.string.copy),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}