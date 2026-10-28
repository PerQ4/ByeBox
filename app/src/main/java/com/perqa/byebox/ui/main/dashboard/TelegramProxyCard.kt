package com.perqa.byebox.ui.main.dashboard

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amurcanov.tgwsproxy.ProxyController
import com.amurcanov.tgwsproxy.ProxyService
import com.amurcanov.tgwsproxy.R as TgwsR
import com.amurcanov.tgwsproxy.SettingsStore
import com.amurcanov.tgwsproxy.ui.openTelegram
import com.perqa.byebox.theme.ByeBoxExpressiveTheme
import com.perqa.byebox.theme.ByeBoxShapes
import com.perqa.byebox.theme.LocalByeBoxMotion
import com.perqa.byebox.ui.main.Loc
import com.perqa.byebox.ui.main.ModeChip
import com.perqa.byebox.ui.main.ProxyStatusPanel
import com.perqa.byebox.ui.main.SettingsRowSurface
import com.perqa.byebox.ui.main.applyToTelegramPackages
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    isExpanded: Boolean = false,
    onToggle: () -> Unit = {},
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
                val started = withContext(Dispatchers.IO) {
                    ProxyController.startFromSavedSettings(context, showInvalidPortToast = true)
                }
                if (!started) isStarting = false
            }
        }
    }
    val disconnectAction = {
        if (isRunning || isStarting) ProxyController.stop(context)
    }
    val isActiveVisual = isRunning || isStarting

    val motion = LocalByeBoxMotion.current
    val expressive = cornerRoundness == "expressive"
    val cardShape = ByeBoxShapes.card(expressive)

    ByeBoxExpressiveTheme {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                // clip ДО clickable: иначе ripple рисуется прямоугольником и не
                // учитывает скругление карточки. Вложенные интерактивные элементы
                // (пилюля, шеврон, кнопки контента) перехватывают свои нажатия сами.
                .clip(cardShape)
                .clickable(
                    role = Role.Button,
                    onClickLabel = if (isExpanded) {
                        Loc.get("collapse", language)
                    } else {
                        Loc.get("expand", language)
                    }
                ) { onToggle() },
            shape = cardShape,
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
                    isExpanded = isExpanded,
                    language = language,
                    onToggle = onToggle,
                    onToggleService = { if (isActiveVisual) disconnectAction() else connectAction() },
                    expressive = expressive
                )

                AnimatedVisibility(
                    visible = isExpanded,
                    // Spring без отскока: высота раскрытия не должна выходить за
                    // контент, иначе внутри карточки появляется пустая полоса.
                    enter = expandVertically(animationSpec = motion.expand()) +
                        fadeIn(animationSpec = motion.effectsDefault()),
                    exit = shrinkVertically(animationSpec = motion.expand()) +
                        fadeOut(animationSpec = motion.effectsFast())
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
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TelegramHeader(
    title: String,
    statusText: String,
    isActive: Boolean,
    isVerified: Boolean,
    isExpanded: Boolean,
    language: String,
    onToggle: () -> Unit,
    onToggleService: () -> Unit,
    expressive: Boolean = true
) {
    val motion = LocalByeBoxMotion.current
    val accent by animateColorAsState(
        targetValue = when {
            isActive && isVerified -> MaterialTheme.colorScheme.primary
            isActive -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = motion.effectsDefault(),
        label = "tgwsAccent"
    )
    val accentContent by animateColorAsState(
        targetValue = when {
            isActive && isVerified -> MaterialTheme.colorScheme.onPrimary
            isActive -> MaterialTheme.colorScheme.onTertiary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = motion.effectsDefault(),
        label = "tgwsAccentContent"
    )
    val logoShape = MaterialShapes.Cookie6Sided.toShape()
    val leadingShape = RoundedCornerShape(
        topStart = CornerSize(22.dp),
        bottomStart = CornerSize(22.dp),
        topEnd = CornerSize(8.dp),
        bottomEnd = CornerSize(8.dp)
    )
    val trailingShape = RoundedCornerShape(
        topStart = CornerSize(8.dp),
        bottomStart = CornerSize(8.dp),
        topEnd = CornerSize(22.dp),
        bottomEnd = CornerSize(22.dp)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // В выключенном состоянии accent — тёмный surfaceVariant, поэтому бейдж
        // брал почти невидимый фон. Для логотипа используем отдельный акцент.
        val logoAccent = if (isActive) {
            accent
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(logoShape)
                .background(logoAccent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = TgwsR.drawable.ic_telegram_logo),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
                alpha = if (isActive) 1f else 0.85f
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = (if (expressive) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.titleMedium
            }).copy(
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        // Ручной split: SplitButtonLayout растягивает ведущую кнопку и съедает
        // заголовок, поэтому собираем две connected-кнопки фиксированного размера.
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Surface(
                onClick = onToggleService,
                shape = leadingShape,
                color = accent,
                contentColor = accentContent,
                modifier = Modifier.height(44.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(MaterialShapes.Cookie9Sided.toShape())
                            .background(accentContent)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }
            }
            Surface(
                onClick = onToggle,
                shape = trailingShape,
                color = accent,
                contentColor = accentContent,
                modifier = Modifier.size(44.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = if (isExpanded) {
                            Loc.get("collapse", language)
                        } else {
                            Loc.get("expand", language)
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
                .height(52.dp),
            // Обычная кнопка со скруглёнными углами.
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                Loc.get("tgws_apply_in_telegram", language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Сегментированный переключатель в общей капсуле.
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ModeChip(
                    label = Loc.get("tgws_packages", language),
                    selected = applyMode == "packages",
                    modifier = Modifier.weight(1f).height(44.dp)
                ) { onApplyModeChange("packages") }
                ModeChip(
                    label = Loc.get("tgws_link", language),
                    selected = applyMode == "link",
                    modifier = Modifier.weight(1f).height(44.dp)
                ) { onApplyModeChange("link") }
            }
        }

        ProxyStatusPanel(
            cfEnabled = cfEnabled,
            poolSize = poolSize,
            port = port,
            language = language
        )

        Surface(
            onClick = {
                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(android.content.ClipData.newPlainText("Proxy", proxyUrl))
                Toast.makeText(context, Loc.get("tgws_copied", language), Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(MaterialShapes.Cookie9Sided.toShape())
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = Loc.get("tgws_copy", language),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = proxyUrl,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}