package com.perqa.byebox.ui.main.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amurcanov.tgwsproxy.ProxyController
import com.amurcanov.tgwsproxy.ProxyService
import com.amurcanov.tgwsproxy.R as TgwsR
import com.amurcanov.tgwsproxy.SettingsStore
import com.perqa.byebox.ui.main.ConnectionStatus
import com.perqa.byebox.ui.main.Loc
import com.perqa.byebox.ui.main.MainScreenViewModel
import com.perqa.byebox.ui.main.TgWsProxySettingsContent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Full TGWS page rendered when the app runs in "tgws-only" mode. Mirrors the
 * VPN dashboard: a status pill bar, a big circular connect button, an
 * always-expanded "apply to Telegram" card, and the full TGWS settings inline.
 */
@Composable
fun TgwsFullPage(
    language: String = "ru",
    cornerRoundness: String = "expressive",
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel? = null
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

    LaunchedEffect(Unit) {
        if (settingsStore.secretKey.first().isBlank()) {
            val bytes = ByteArray(16)
            java.security.SecureRandom().nextBytes(bytes)
            settingsStore.saveSecretKey(bytes.joinToString("") { "%02x".format(it) })
        }
    }

    LaunchedEffect(isRunning, isVerifiedRunning) {
        if (isVerifiedRunning || !isRunning) isStarting = false
    }

    val status = when {
        isVerifiedRunning -> ConnectionStatus.CONNECTED
        isStarting || isRunning -> ConnectionStatus.CONNECTING
        else -> ConnectionStatus.DISCONNECTED
    }
    val statusText = when {
        isVerifiedRunning -> Loc.get("tgws_status_connected", language)
        isStarting || isRunning -> Loc.get("tgws_status_connecting", language)
        else -> Loc.get("tgws_status_disconnected", language)
    }
    val isActiveVisual = isRunning || isStarting
    val isBusyVisual = isStarting || (isRunning && !isVerifiedRunning)

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

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        TgwsStatusPillBar(
            statusText = statusText,
            isActive = isActiveVisual,
            isVerified = isVerifiedRunning,
            isBusy = isBusyVisual,
            poolSize = poolSize,
            port = port,
            cornerRoundness = cornerRoundness
        )

        ConnectionButton(
            status = status,
            pulseEnabled = true,
            language = language,
            centerContent = {
                val contentColor = when (status) {
                    ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.onPrimary
                    ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.onTertiary
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = TgwsR.drawable.ic_telegram_logo),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
                        alpha = if (isActiveVisual) 1f else 0.6f
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = contentColor
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            onClick = { if (isActiveVisual) disconnectAction() else connectAction() }
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(if (cornerRoundness == "expressive") 28.dp else 16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = TgwsR.drawable.ic_telegram_logo),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
                        alpha = if (isActiveVisual) 1f else 0.6f
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Loc.get("title_telegram", language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (isActiveVisual) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isVerifiedRunning) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.tertiary
                                )
                        )
                    }
                }

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

        TgWsProxySettingsContent(
            scaleFactor = 0.90f,
            cornerRoundness = cornerRoundness,
            language = language,
            viewModel = viewModel
        )
    }
}

@Composable
private fun TgwsStatusPillBar(
    statusText: String,
    isActive: Boolean,
    isVerified: Boolean,
    isBusy: Boolean,
    poolSize: Int,
    port: String,
    cornerRoundness: String
) {
    val statusColor = when {
        isActive && isVerified -> MaterialTheme.colorScheme.primary
        isActive -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val dotPulse = rememberInfiniteTransition(label = "tgwsDotPulse")
    val dotAlpha by dotPulse.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tgwsDotAlpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (cornerRoundness == "expressive") 32.dp else 20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .graphicsLayer { alpha = if (isBusy) dotAlpha else 1f }
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = statusColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                )
                Text(
                    text = stringResource(TgwsR.string.pool_short, poolSize),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = stringResource(TgwsR.string.port_short, port),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}