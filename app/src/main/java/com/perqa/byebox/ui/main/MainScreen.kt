package com.perqa.byebox.ui.main

import com.perqa.byebox.MainActivity
import com.perqa.byebox.findActivity
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Divider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.perqa.byebox.data.DefaultDataRepository
import com.perqa.byebox.data.ProxyConfig
import com.perqa.byebox.theme.AppTheme
import com.perqa.byebox.theme.HiddifyExpressiveTheme
import kotlinx.coroutines.delay

enum class NodeSortMode(val label: String) {
    SOURCE("Источник"),
    PING("Пинг"),
    NAME("Имя")
}

@Composable
private fun rememberTactileFeedback(): () -> Unit {
    val context = LocalContext.current
    return remember(context) {
        {
            runCatching {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    manager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

                if (vibrator?.hasVibrator() == true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                8L,
                                32
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(8L)
                    }
                }
            }
        }
    }
}

private fun smoothStep(value: Float): Float {
    val x = value.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(DefaultDataRepository()) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Dynamic M3 theme overriding inside MainScreen based on state.appTheme
    HiddifyExpressiveTheme(appTheme = state.appTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            var selectedTab by remember { mutableIntStateOf(0) }

            Box(modifier = modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when (state.connectionStatus) {
                            ConnectionStatus.CONNECTED -> "ПОДКЛЮЧЕНО"
                            ConnectionStatus.CONNECTING -> "ПОДКЛЮЧЕНИЕ..."
                            ConnectionStatus.DISCONNECTED -> "ОТКЛЮЧЕНО"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.42f),
                            letterSpacing = 0.8.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Crossfade(
                            targetState = selectedTab,
                            label = "mainTabCrossfade"
                        ) { tab ->
                            when (tab) {
                                0 -> DashboardTab(
                                    state = state,
                                    viewModel = viewModel
                                )
                                1 -> ProxyTab(
                                    state = state,
                                    viewModel = viewModel
                                )
                                2 -> SettingsTab(
                                    state = state,
                                    viewModel = viewModel
                                )
                                3 -> LogsTab(
                                    state = state,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }

                BottomEdgeFade(
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp, start = 8.dp, end = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MainTabBar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    FloatingContextAction(
                        selectedTab = selectedTab,
                        onClick = {
                            when (selectedTab) {
                                0 -> viewModel.selectBestConfig()
                                1 -> viewModel.testPings()
                                2 -> (context.findActivity() as? MainActivity)?.openSystemVpnSettings()
                                3 -> viewModel.exportLogs(context)
                            }
                        }
                    )
                }
            }
        }
    }
}



@Composable
fun DashboardTab(
    state: MainUiState,
    viewModel: MainScreenViewModel
) {
    val activeConfig = state.configs.find { it.id == state.activeConfigId }
    val context = LocalContext.current
    val activity = context.findActivity() as? MainActivity
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        StatusOverviewCard(
            status = state.connectionStatus,
            activeConfig = activeConfig,
            routingProfile = state.routingProfile,
            dnsServer = state.dnsServer
        )

        Spacer(modifier = Modifier.height(14.dp))

        ConnectionButton(
            status = state.connectionStatus,
            onClick = {
                if (activity != null) {
                    val isConnecting = state.connectionStatus == ConnectionStatus.DISCONNECTED
                    if (isConnecting) {
                        viewModel.setConnectingState()
                    }
                    activity.handleVpnToggle(isConnecting)
                }
            }
        )


        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SpeedCard(
                label = "Скачивание",
                value = state.downloadSpeed,
                iconColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.46f),
                modifier = Modifier.weight(1f)
            )
            SpeedCard(
                label = "Загрузка",
                value = state.uploadSpeed,
                iconColor = MaterialTheme.colorScheme.secondary,
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.46f),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        QuickActionsCard(
            onBestServer = { viewModel.selectBestConfig() },
            onShare = { activity?.shareActiveConfig() },
            onVpnSettings = { activity?.openSystemVpnSettings() },
            onAddTile = { activity?.requestQuickSettingsTile() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (activeConfig != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(28.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeConfig.countryFlag,
                        fontSize = 36.sp,
                        modifier = Modifier.padding(end = 16.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeConfig.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeConfig.protocol,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${activeConfig.address}:${activeConfig.port}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = activeConfig.sourceName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Quick Active Server Ping Button
                    IconButton(
                        onClick = { viewModel.testActiveConfigPing() },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Быстрый пинг",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Ping delay pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    activeConfig.ping == null -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    activeConfig.ping < 60 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    activeConfig.ping < 120 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (activeConfig.ping != null) "${activeConfig.ping} ms" else "N/A",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    activeConfig.ping == null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    activeConfig.ping < 60 -> MaterialTheme.colorScheme.primary
                                    activeConfig.ping < 120 -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.error
                                }
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(180.dp))
    }
}

@Composable
fun StatusOverviewCard(
    status: ConnectionStatus,
    activeConfig: ProxyConfig?,
    routingProfile: RoutingProfile,
    dnsServer: DnsServer
) {
    val containerColor by animateColorAsState(
        targetValue = when (status) {
            ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
            ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.tertiaryContainer
            ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "statusOverviewColor"
    )
    val shape = RoundedCornerShape(topStart = 34.dp, topEnd = 18.dp, bottomEnd = 34.dp, bottomStart = 18.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(shape),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = when (status) {
                    ConnectionStatus.CONNECTED -> "VPN защищает трафик"
                    ConnectionStatus.CONNECTING -> "Поднимаем VPN-туннель"
                    ConnectionStatus.DISCONNECTED -> "VPN отключен"
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = activeConfig?.name ?: "Сервер не выбран",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(text = routingProfile.label, modifier = Modifier.weight(1f))
                InfoChip(text = dnsServer.label, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun InfoChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.48f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun QuickActionsCard(
    onBestServer: () -> Unit,
    onShare: () -> Unit,
    onVpnSettings: () -> Unit,
    onAddTile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(28.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickActionButton("Лучший", Icons.Default.Search, onBestServer, Modifier.weight(1f))
                QuickActionButton("Поделиться", Icons.Default.Add, onShare, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickActionButton("VPN Android", Icons.Default.Settings, onVpnSettings, Modifier.weight(1f))
                QuickActionButton("Плитка", Icons.Default.Info, onAddTile, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tactileFeedback = rememberTactileFeedback()
    val containerColor = when (icon) {
        Icons.Default.Search -> MaterialTheme.colorScheme.primaryContainer
        Icons.Default.Add -> MaterialTheme.colorScheme.secondaryContainer
        Icons.Default.Settings -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when (icon) {
        Icons.Default.Search -> MaterialTheme.colorScheme.onPrimaryContainer
        Icons.Default.Add -> MaterialTheme.colorScheme.onSecondaryContainer
        Icons.Default.Settings -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Button(
        onClick = {
            tactileFeedback()
            onClick()
        },
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun ConnectionButton(
    status: ConnectionStatus,
    onClick: () -> Unit
) {
    val tactileFeedback = rememberTactileFeedback()
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val progressRotate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val buttonBgColor by animateColorAsState(
        targetValue = when (status) {
            ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primary
            ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.tertiary
            ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.surface
        },
        label = "buttonColor"
    )

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(200.dp)
            .padding(10.dp)
    ) {
        // Animated polar waveform on Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = center
            val baseRadius = 80.dp.toPx()
            
            if (status == ConnectionStatus.CONNECTING) {
                // Wave 1: 5 crests, rotating forward
                val path1 = androidx.compose.ui.graphics.Path()
                val steps = 100
                for (i in 0..steps) {
                    val angle = (i.toFloat() / steps) * 2f * Math.PI.toFloat()
                    val r = baseRadius + 6.dp.toPx() * kotlin.math.sin(5 * angle - wavePhase)
                    val x = center.x + r * kotlin.math.cos(angle)
                    val y = center.y + r * kotlin.math.sin(angle)
                    if (i == 0) path1.moveTo(x, y) else path1.lineTo(x, y)
                }
                path1.close()
                drawPath(
                    path = path1,
                    color = buttonBgColor.copy(alpha = 0.25f),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Wave 2: 7 crests, rotating backward, slightly smaller amplitude
                val path2 = androidx.compose.ui.graphics.Path()
                for (i in 0..steps) {
                    val angle = (i.toFloat() / steps) * 2f * Math.PI.toFloat()
                    val r = baseRadius + 4.dp.toPx() * kotlin.math.sin(7 * angle + wavePhase + 1.2f)
                    val x = center.x + r * kotlin.math.cos(angle)
                    val y = center.y + r * kotlin.math.sin(angle)
                    if (i == 0) path2.moveTo(x, y) else path2.lineTo(x, y)
                }
                path2.close()
                drawPath(
                    path = path2,
                    color = buttonBgColor.copy(alpha = 0.4f),
                    style = Stroke(width = 2.dp.toPx())
                )
            } else if (status == ConnectionStatus.CONNECTED) {
                // Breathing pulse when connected
                val pulseScale = 1f + 0.04f * kotlin.math.sin(wavePhase.toDouble()).toFloat()
                drawCircle(
                    color = buttonBgColor.copy(alpha = 0.16f),
                    radius = baseRadius * pulseScale,
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = buttonBgColor.copy(alpha = 0.08f),
                    radius = baseRadius * (pulseScale + 0.06f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            } else {
                // Static neat ring when disconnected
                drawCircle(
                    color = onSurfaceColor.copy(alpha = 0.08f),
                    radius = baseRadius,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Central trigger button (flat design with soft elevation and no neon shadows)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(150.dp)
                .shadow(
                    elevation = if (status != ConnectionStatus.DISCONNECTED) 8.dp else 2.dp,
                    shape = CircleShape
                )
                .clip(CircleShape)
                .background(buttonBgColor)
                .border(
                    width = 1.dp,
                    color = when (status) {
                        ConnectionStatus.CONNECTED -> Color.Transparent
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    },
                    shape = CircleShape
                )
                .clickable {
                    tactileFeedback()
                    onClick()
                }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = if (status == ConnectionStatus.DISCONNECTED) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .rotate(if (status == ConnectionStatus.CONNECTING) progressRotate else 0f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (status) {
                        ConnectionStatus.CONNECTED -> "ВКЛЮЧЕНО"
                        ConnectionStatus.CONNECTING -> "ПОИСК..."
                        ConnectionStatus.DISCONNECTED -> "СТАРТ"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = if (status == ConnectionStatus.DISCONNECTED) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        }
                    )
                )
            }
        }
    }
}

@Composable
fun SpeedCard(
    label: String,
    value: String,
    iconColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp)),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(iconColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
fun ProxyTab(
    state: MainUiState,
    viewModel: MainScreenViewModel
) {
    var importUrl by remember { mutableStateOf("") }
    var importUrlError by remember { mutableStateOf(false) }
    var nodeSearchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(NodeSortMode.SOURCE) }
    var showProxyToolsSheet by remember { mutableStateOf(false) }
    var controlPanelExpanded by remember { mutableStateOf(true) }
    var controlPanelManuallyExpanded by remember { mutableStateOf(false) }
    var collapsedGroupKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var swipingConfigId by remember { mutableStateOf<String?>(null) }
    var swipingDragPx by remember { mutableFloatStateOf(0f) }
    var configDetails by remember { mutableStateOf<ProxyConfig?>(null) }
    val listState = rememberLazyListState()
    val sourceGroups = remember(state.configs, sortMode, nodeSearchQuery) {
        val query = nodeSearchQuery.trim().lowercase()
        state.configs
            .asSequence()
            .filter { config ->
                query.isEmpty() ||
                    config.name.lowercase().contains(query) ||
                    config.sourceName.lowercase().contains(query) ||
                    config.address.lowercase().contains(query) ||
                    config.protocol.lowercase().contains(query) ||
                    config.countryFlag.lowercase().contains(query) ||
                    (config.description?.lowercase()?.contains(query) == true) ||
                    (config.sni?.lowercase()?.contains(query) == true) ||
                    (config.network?.lowercase()?.contains(query) == true) ||
                    (config.security?.lowercase()?.contains(query) == true)
            }
            .toList()
            .groupBy { it.sourceName.ifBlank { "Локальные конфигурации" } }
            .mapValues { (_, configs) -> configs.sortedFor(sortMode) }
            .toList()
            .sortedBy { it.first.lowercase() }
    }
    LaunchedEffect(sourceGroups) {
        val visibleGroupKeys = sourceGroups.map { it.first }.toSet()
        collapsedGroupKeys = collapsedGroupKeys.intersect(visibleGroupKeys)
    }
    val sourcesByName = remember(state.subscriptionSources) {
        state.subscriptionSources.associateBy { it.name }
    }
    val autoControlPanelCollapsed by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 48 }
    }
    val controlPanelCollapsed = !controlPanelExpanded || (autoControlPanelCollapsed && !controlPanelManuallyExpanded)

    configDetails?.let { config ->
        ModalBottomSheet(
            onDismissRequest = { configDetails = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            ConfigDetailsSheet(
                config = config,
                onDismiss = { configDetails = null }
            )
        }
    }

    if (showProxyToolsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProxyToolsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            ProxyToolsSheet(
                sortMode = sortMode,
                onSortModeSelected = {
                    sortMode = it
                    showProxyToolsSheet = false
                },
                searchQuery = nodeSearchQuery,
                onSearchQueryChange = { nodeSearchQuery = it },
                resultCount = sourceGroups.sumOf { it.second.size },
                totalCount = state.configs.size,
                onClearSearch = { nodeSearchQuery = "" }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        ProxyControlPanel(
            sourceCount = sourceGroups.size,
            configCount = state.configs.size,
            isPinging = state.isPinging,
            importUrl = importUrl,
            onImportUrlChange = {
                importUrl = it
                importUrlError = false
            },
            importUrlError = importUrlError,
            searchQuery = nodeSearchQuery,
            onSearchQueryChange = { nodeSearchQuery = it },
            collapsed = controlPanelCollapsed,
            onToggleExpanded = {
                if (controlPanelCollapsed) {
                    controlPanelExpanded = true
                    controlPanelManuallyExpanded = true
                } else {
                    controlPanelExpanded = false
                    controlPanelManuallyExpanded = false
                }
            },
            sortMode = sortMode,
            onOpenFilters = { showProxyToolsSheet = true },
            onRefreshSubscriptions = { viewModel.refreshSubscriptions() },
            onPingAll = { viewModel.testPings() },
            onAddConfig = {
                if (importUrl.isNotBlank()) {
                    viewModel.addConfigFromUrl(importUrl)
                    importUrl = ""
                    importUrlError = false
                } else {
                    importUrlError = true
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 190.dp)
        ) {
            if (sourceGroups.isEmpty()) {
                item(key = "proxy-empty", contentType = "empty") {
                    ProxyEmptyState(
                        hasSearch = nodeSearchQuery.isNotBlank(),
                        onClearSearch = { nodeSearchQuery = "" }
                    )
                }
            }
            sourceGroups.forEachIndexed { groupIndex, (sourceName, configs) ->
                val groupCollapsed = sourceName in collapsedGroupKeys

                if (groupIndex > 0) {
                    item(key = "spacer-group-$sourceName") {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                val firstConfigActive = configs.firstOrNull()?.let { it.id == state.activeConfigId } ?: false
                val firstConfigSwiping = configs.firstOrNull()?.id == swipingConfigId
                val headerBottomCorner = if (groupCollapsed || configs.isEmpty() || firstConfigActive || firstConfigSwiping) 28.dp else 6.dp
                val headerShape = RoundedCornerShape(
                    topStart = 28.dp,
                    topEnd = 28.dp,
                    bottomStart = headerBottomCorner,
                    bottomEnd = headerBottomCorner
                )

                stickyHeader(key = "source-$sourceName", contentType = "source") {
                    SourceGroupCard(
                        sourceName = sourceName,
                        source = sourcesByName[sourceName],
                        configs = configs,
                        activeConfigId = state.activeConfigId,
                        onSelect = { viewModel.selectConfig(it) },
                        onDelete = { viewModel.deleteConfig(it) },
                        onRefreshSource = { viewModel.refreshSubscription(it) },
                        onRenameSource = { sourceId, name -> viewModel.renameSubscriptionSource(sourceId, name) },
                        onDeleteSource = { viewModel.deleteSubscriptionSource(it) },
                        onPingSource = { viewModel.testPingsForSource(sourceName) },
                        expanded = !groupCollapsed,
                        onToggleExpanded = {
                            collapsedGroupKeys = if (groupCollapsed) {
                                collapsedGroupKeys - sourceName
                            } else {
                                collapsedGroupKeys + sourceName
                            }
                        },
                        showConfigs = false,
                        shape = headerShape
                    )
                }

                if (!groupCollapsed) {
                    configs.forEachIndexed { index, config ->
                        val isActive = config.id == state.activeConfigId
                        val prevIsActive = index > 0 && configs[index - 1].id == state.activeConfigId
                        val nextIsActive = index < configs.lastIndex && configs[index + 1].id == state.activeConfigId
                        val isLast = index == configs.lastIndex

                        val baseTopCorner = when {
                            isActive -> 28.dp
                            prevIsActive -> 28.dp
                            else -> 6.dp
                        }
                        val baseBottomCorner = when {
                            isActive -> 28.dp
                            nextIsActive -> 28.dp
                            isLast -> 28.dp
                            else -> 6.dp
                        }

                        val prevIsSwipingNeighbor = index > 0 && configs[index - 1].id == swipingConfigId
                        val nextIsSwipingNeighbor = index < configs.lastIndex && configs[index + 1].id == swipingConfigId
                        val neighborFollowProgress = smoothStep((kotlin.math.abs(swipingDragPx) / 96f).coerceIn(0f, 1f))
                        val neighborRoundnessProgress = smoothStep((kotlin.math.abs(swipingDragPx) / 140f).coerceIn(0f, 1f))
                        val effectiveTopCorner = if (prevIsSwipingNeighbor) lerp(baseTopCorner, 28.dp, neighborRoundnessProgress) else baseTopCorner
                        val effectiveBottomCorner = if (nextIsSwipingNeighbor) lerp(baseBottomCorner, 28.dp, neighborRoundnessProgress) else baseBottomCorner
                        val neighborOffsetPx = if (prevIsSwipingNeighbor || nextIsSwipingNeighbor) {
                            kotlin.math.sign(swipingDragPx) * 3.dp.value * neighborFollowProgress
                        } else {
                            0f
                        }

                        item(key = "config-${config.id}", contentType = "server") {
                            ServerItemCard(
                                config = config,
                                isActive = isActive,
                                onSelect = { viewModel.selectConfig(config.id) },
                                onDelete = { viewModel.deleteConfig(config.id) },
                                onOpenSettings = { configDetails = config },
                                topCorner = effectiveTopCorner,
                                bottomCorner = effectiveBottomCorner,
                                neighborOffsetDp = neighborOffsetPx.dp,
                                onSwipeOffsetChanged = { offset ->
                                    if (swipingConfigId == config.id) {
                                        val crossesDeleteThreshold = kotlin.math.abs(offset) >= 140f && kotlin.math.abs(swipingDragPx) < 140f
                                        val returnsHome = offset == 0f
                                        val movedEnough = kotlin.math.abs(offset - swipingDragPx) >= 14f
                                        if (returnsHome || crossesDeleteThreshold || movedEnough) {
                                            swipingDragPx = offset
                                        }
                                    }
                                },
                                onSwipingChanged = { isSwiping ->
                                    if (isSwiping) {
                                        swipingConfigId = config.id
                                    } else if (swipingConfigId == config.id) {
                                        swipingConfigId = null
                                        swipingDragPx = 0f
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProxyControlPanel(
    sourceCount: Int,
    configCount: Int,
    isPinging: Boolean,
    importUrl: String,
    onImportUrlChange: (String) -> Unit,
    importUrlError: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    collapsed: Boolean,
    onToggleExpanded: () -> Unit,
    sortMode: NodeSortMode,
    onOpenFilters: () -> Unit,
    onRefreshSubscriptions: () -> Unit,
    onPingAll: () -> Unit,
    onAddConfig: () -> Unit
) {
    val tactileFeedback = rememberTactileFeedback()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp)),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Конфигурации",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "$sourceCount источника · $configCount узлов",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                if (isPinging) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                } else {
                    TextButton(
                        onClick = {
                            tactileFeedback()
                            onToggleExpanded()
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (collapsed) "Раскрыть" else "Скрыть",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isPinging) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }

            AnimatedVisibility(visible = !collapsed) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    ProxySearchField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        onOpenFilters = {
                            tactileFeedback()
                            onOpenFilters()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importUrl,
                        onValueChange = onImportUrlChange,
                        placeholder = {
                            Text(
                                "Вставьте vless://, vmess://, ss://, trojan://",
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        maxLines = 1,
                        isError = importUrlError,
                        supportingText = if (importUrlError) {
                            { Text("Вставьте ссылку на конфиг или подписку") }
                        } else {
                            null
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    tactileFeedback()
                                    onRefreshSubscriptions()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(40.dp)
                                    .weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Обн.", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    tactileFeedback()
                                    onPingAll()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(40.dp)
                                    .weight(0.72f)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Пинг", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    tactileFeedback()
                                    onAddConfig()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(40.dp)
                                    .weight(1.05f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Добавить", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SortSummaryBar(
                        selected = sortMode,
                        searchQuery = searchQuery,
                        onOpenFilters = {
                            tactileFeedback()
                            onOpenFilters()
                        }
                    )
                }
            }
        }
    }
}

private fun List<ProxyConfig>.sortedFor(mode: NodeSortMode): List<ProxyConfig> {
    return when (mode) {
        NodeSortMode.SOURCE -> sortedWith(compareBy<ProxyConfig> { it.sourceName.lowercase() }.thenBy { it.name.lowercase() })
        NodeSortMode.PING -> sortedWith(compareBy<ProxyConfig> { it.ping ?: Int.MAX_VALUE }.thenBy { it.failureCount }.thenBy { it.name.lowercase() })
        NodeSortMode.NAME -> sortedBy { it.name.lowercase() }
    }
}

@Composable
fun ProxySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onOpenFilters: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = "Поиск узлов, стран, протоколов",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                textStyle = MaterialTheme.typography.bodyMedium,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            IconButton(
                onClick = onOpenFilters,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Сортировка и фильтры",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ProxyEmptyState(
    hasSearch: Boolean,
    onClearSearch: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = if (hasSearch) "Ничего не найдено" else "Нет конфигураций",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                textAlign = TextAlign.Center
            )
            Text(
                text = if (hasSearch) {
                    "Попробуйте изменить запрос или сбросить фильтр."
                } else {
                    "Добавьте ссылку на конфиг или подписку выше."
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )
            if (hasSearch) {
                TextButton(onClick = onClearSearch) {
                    Text("Сбросить поиск", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProxyToolsSheet(
    sortMode: NodeSortMode,
    onSortModeSelected: (NodeSortMode) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    resultCount: Int,
    totalCount: Int,
    onClearSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Поиск и сортировка",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
        )
        Text(
            text = "$resultCount из $totalCount узлов",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = if (searchQuery.isNotBlank()) {
                {
                    TextButton(onClick = onClearSearch) {
                        Text("Сброс")
                    }
                }
            } else {
                null
            },
            placeholder = { Text("Имя, страна, адрес, протокол") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        )

        Text(
            text = "Сортировка",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            NodeSortMode.values().forEachIndexed { index, mode ->
                val selected = sortMode == mode
                val shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
                    NodeSortMode.values().lastIndex -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                    else -> RoundedCornerShape(6.dp)
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .clickable { onSortModeSelected(mode) },
                    shape = shape,
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SortSummaryBar(
    selected: NodeSortMode,
    searchQuery: String,
    onOpenFilters: () -> Unit
) {
    val label = if (searchQuery.isBlank()) {
        "Сортировка: ${selected.label}"
    } else {
        "Фильтр: $searchQuery"
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenFilters),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.48f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Открыть фильтры",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(90f)
            )
        }
    }
}

@Composable
fun SourceGroupCard(
    sourceName: String,
    source: com.perqa.byebox.data.SubscriptionSource?,
    configs: List<ProxyConfig>,
    activeConfigId: String?,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRefreshSource: (String) -> Unit,
    onRenameSource: (String, String) -> Unit,
    onDeleteSource: (String) -> Unit,
    onPingSource: () -> Unit,
    expanded: Boolean = true,
    onToggleExpanded: () -> Unit = {},
    showConfigs: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(22.dp)
) {
    val tactileFeedback = rememberTactileFeedback()
    val sourceUrl = configs.firstOrNull { it.sourceUrl != null }?.sourceUrl
    val averagePing = configs.mapNotNull { it.ping }.takeIf { it.isNotEmpty() }?.average()?.toInt()
    val activeCount = configs.count { it.id == activeConfigId }
    var isRenaming by remember(source?.id) { mutableStateOf(false) }
    var editedName by remember(source?.id, sourceName) { mutableStateOf(source?.name ?: sourceName) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (activeCount > 0) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 2.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (isRenaming && source != null) {
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        onRenameSource(source.id, editedName)
                                        isRenaming = false
                                    }
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Сохранить")
                                }
                            }
                        )
                    } else {
                        Text(
                            text = sourceName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = sourceSubtitle(sourceUrl, source),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    source?.description?.takeIf { it.isNotBlank() }?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.46f)
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${configs.size} узл.",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )
                    )
                    Text(
                        text = averagePing?.let { "~$it ms" } ?: "N/A",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    source?.let {
                        Text(
                            text = trafficSubtitle(it),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(
                    onClick = {
                        tactileFeedback()
                        onToggleExpanded()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle group",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }


            if (showConfigs) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    configs.forEach { config ->
                        ServerItemCard(
                            config = config,
                            isActive = config.id == activeConfigId,
                            onSelect = { onSelect(config.id) },
                            onDelete = { onDelete(config.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SourceActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false
) {
    val tactileFeedback = rememberTactileFeedback()
    val containerColor = when {
        destructive -> MaterialTheme.colorScheme.errorContainer
        icon == Icons.Default.Refresh -> MaterialTheme.colorScheme.secondaryContainer
        icon == Icons.Default.Settings -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = when {
        destructive -> MaterialTheme.colorScheme.onErrorContainer
        icon == Icons.Default.Refresh -> MaterialTheme.colorScheme.onSecondaryContainer
        icon == Icons.Default.Settings -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Button(
        onClick = {
            tactileFeedback()
            onClick()
        },
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 11.sp)
    }
}

@Composable
fun ConfigDetailsSheet(
    config: ProxyConfig,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = config.countryFlag,
                    fontSize = 23.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
                Text(
                    text = config.protocolSummary(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Закрыть")
            }
        }

        config.description?.takeIf { it.isNotBlank() && it != config.name }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ConfigDetailLine("Протокол", config.protocolSummary())
                ConfigDetailLine("Адрес", "${config.address}:${config.port}")
                ConfigDetailLine("Транспорт", config.network ?: "tcp")
                config.security?.let { ConfigDetailLine("TLS", it) }
                config.sni?.let { ConfigDetailLine("SNI", it) }
                config.flow?.let { ConfigDetailLine("Flow", it) }
                config.wsPath?.let { ConfigDetailLine("Path", it) }
                config.grpcServiceName?.let { ConfigDetailLine("gRPC", it) }
                ConfigDetailLine("Источник", config.sourceName)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("Готово", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("Настройки", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ConfigDetailLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

private fun sourceSubtitle(sourceUrl: String?, source: com.perqa.byebox.data.SubscriptionSource?): String {
    val updated = source?.lastUpdatedAt?.let { timestamp ->
        val formatter = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault())
        " · обновлено ${formatter.format(java.util.Date(timestamp))}"
    }.orEmpty()
    return "${sourceUrl ?: "Локальный импорт"}$updated"
}

private fun trafficSubtitle(source: com.perqa.byebox.data.SubscriptionSource): String {
    val total = source.totalBytes ?: return ""
    val used = (source.uploadBytes ?: 0L) + (source.downloadBytes ?: 0L)
    return "${formatBytes(used)} / ${formatBytes(total)}"
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    return String.format(java.util.Locale.US, "%.1f %s", value, units[unitIndex])
}

@Composable
fun ServerItemCard(
    config: ProxyConfig,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onOpenSettings: () -> Unit = {},
    topCorner: androidx.compose.ui.unit.Dp = 6.dp,
    bottomCorner: androidx.compose.ui.unit.Dp = 6.dp,
    neighborOffsetDp: androidx.compose.ui.unit.Dp = 0.dp,
    onSwipeOffsetChanged: (Float) -> Unit = {},
    onSwipingChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val tactileFeedback = rememberTactileFeedback()
    val scope = rememberCoroutineScope()
    val protocolDetails = remember(config) { config.protocolSummary() }
    val endpointDetails = remember(config) { config.endpointSummary() }
    val density = androidx.compose.ui.platform.LocalDensity.current

    var swipeOffsetX by remember(config.id) { mutableFloatStateOf(0f) }
    var actionThresholdFeedbackSent by remember(config.id) { mutableStateOf(false) }
    val actionThresholdPx = remember(density) { with(density) { 140.dp.toPx() } }
    val detachStartPx = remember(density) { with(density) { 14.dp.toPx() } }
    val detachEndPx = remember(density) { with(density) { 58.dp.toPx() } }
    val detachProgress by remember {
        derivedStateOf {
            smoothStep(((kotlin.math.abs(swipeOffsetX) - detachStartPx) / (detachEndPx - detachStartPx)).coerceIn(0f, 1f))
        }
    }
    val displayOffsetX by remember {
        derivedStateOf {
            val resisted = swipeOffsetX * 0.48f
            resisted + (swipeOffsetX - resisted) * detachProgress
        }
    }
    val swipeFraction by remember {
        derivedStateOf { (-displayOffsetX / actionThresholdPx).coerceIn(0f, 1f) }
    }
    val settingsFraction by remember {
        derivedStateOf { (displayOffsetX / actionThresholdPx).coerceIn(0f, 1f) }
    }
    val roundnessProgress by remember {
        derivedStateOf {
            smoothStep((kotlin.math.abs(displayOffsetX) / actionThresholdPx).coerceIn(0f, 1f))
        }
    }

    val animTopCorner by animateDpAsState(
        targetValue = if (isActive) 28.dp else topCorner,
        animationSpec = tween(260),
        label = "topCorner"
    )
    val animBottomCorner by animateDpAsState(
        targetValue = if (isActive) 28.dp else bottomCorner,
        animationSpec = tween(260),
        label = "bottomCorner"
    )

    val cardTopCorner = lerp(animTopCorner, 28.dp, roundnessProgress)
    val cardBottomCorner = lerp(animBottomCorner, 28.dp, roundnessProgress)
    val shape = RoundedCornerShape(
        topStart = cardTopCorner,
        topEnd = cardTopCorner,
        bottomStart = cardBottomCorner,
        bottomEnd = cardBottomCorner
    )

    val containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
                         else MaterialTheme.colorScheme.surfaceContainer
    val errorContainer = MaterialTheme.colorScheme.errorContainer
    val onErrorContainer = MaterialTheme.colorScheme.onErrorContainer

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = settingsFraction)),
            contentAlignment = Alignment.CenterStart
        ) {
            if (settingsFraction > 0.08f) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Настройки",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = (settingsFraction * 2.5f).coerceIn(0f, 1f)),
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .size(22.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(errorContainer.copy(alpha = swipeFraction)),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (swipeFraction > 0.08f) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = onErrorContainer.copy(alpha = (swipeFraction * 2.5f).coerceIn(0f, 1f)),
                    modifier = Modifier
                        .padding(end = 20.dp)
                        .size(22.dp)
                )
            }
        }

        // Card with sticky offset
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = with(density) { neighborOffsetDp.toPx() }
                }
                .offset { IntOffset(displayOffsetX.toInt(), 0) }
                .clip(shape)
                .pointerInput(config.id) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            actionThresholdFeedbackSent = false
                            tactileFeedback()
                            onSwipingChanged(true)
                            onSwipeOffsetChanged(swipeOffsetX)
                        },
                        onDragEnd = {
                            scope.launch {
                                if (-displayOffsetX >= actionThresholdPx) {
                                    tactileFeedback()
                                    val anim = Animatable(swipeOffsetX)
                                    anim.animateTo(
                                        -size.width.toFloat(),
                                        animationSpec = tween(260)
                                    ) {
                                        swipeOffsetX = value
                                        onSwipeOffsetChanged(value)
                                    }
                                    onDelete()
                                    swipeOffsetX = 0f
                                    onSwipeOffsetChanged(0f)
                                    onSwipingChanged(false)
                                } else if (displayOffsetX >= actionThresholdPx) {
                                    tactileFeedback()
                                    val anim = Animatable(swipeOffsetX)
                                    anim.animateTo(
                                        0f,
                                        animationSpec = tween(220)
                                    ) {
                                        swipeOffsetX = value
                                        onSwipeOffsetChanged(value)
                                    }
                                    onSwipingChanged(false)
                                    onOpenSettings()
                                } else {
                                    val anim = Animatable(swipeOffsetX)
                                    anim.animateTo(
                                        0f,
                                        animationSpec = tween(260)
                                    ) {
                                        swipeOffsetX = value
                                        onSwipeOffsetChanged(value)
                                    }
                                    onSwipingChanged(false)
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                val anim = Animatable(swipeOffsetX)
                                anim.animateTo(0f, animationSpec = tween(220)) {
                                    swipeOffsetX = value
                                    onSwipeOffsetChanged(value)
                                }
                            }
                            onSwipingChanged(false)
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = (swipeOffsetX + dragAmount)
                                .coerceIn(-size.width.toFloat(), size.width.toFloat())
                            val displayNewOffset = run {
                                val progress = smoothStep(((kotlin.math.abs(newOffset) - detachStartPx) / (detachEndPx - detachStartPx)).coerceIn(0f, 1f))
                                val resisted = newOffset * 0.48f
                                resisted + (newOffset - resisted) * progress
                            }
                            if (kotlin.math.abs(displayNewOffset) >= actionThresholdPx && !actionThresholdFeedbackSent) {
                                actionThresholdFeedbackSent = true
                                tactileFeedback()
                            }
                            swipeOffsetX = newOffset
                            onSwipeOffsetChanged(newOffset)
                        }
                    )
                }
                .clickable {
                    tactileFeedback()
                    onSelect()
                },
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                ) {
                    Text(
                        text = config.countryFlag,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = config.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ProtocolPill(protocolDetails)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = endpointDetails,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    config.description?.takeIf { it.isNotBlank() && it != config.name }?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    PingPill(config.ping)
                    IconButton(
                        onClick = {
                            tactileFeedback()
                            val link = config.toConfigLink()
                            if (link.isNotBlank()) {
                                clipboardManager.setText(AnnotatedString(link))
                                Toast.makeText(context, "Ссылка скопирована!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        CopyIcon(
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProtocolPill(protocol: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = protocol,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        )
    }
}

private fun ProxyConfig.protocolSummary(): String {
    return listOfNotNull(
        protocol.uppercase(),
        security?.takeIf { it.isNotBlank() && it != "none" }?.uppercase(),
        network?.takeIf { it.isNotBlank() && it != "tcp" }?.uppercase(),
        flow?.takeIf { it.isNotBlank() }?.replace("xtls-rprx-", "", ignoreCase = true)?.uppercase()
    ).joinToString(" / ").ifBlank { protocol.uppercase() }
}

private fun ProxyConfig.endpointSummary(): String {
    val host = sni?.takeIf { it.isNotBlank() && it != address } ?: address
    val transport = when {
        wsPath?.isNotBlank() == true -> wsPath
        grpcServiceName?.isNotBlank() == true -> grpcServiceName
        else -> null
    }
    return listOfNotNull("$host:$port", transport).joinToString(" · ")
}

@Composable
fun PingPill(ping: Int?) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    ping == null -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    ping < 60 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ping < 120 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)
                    else -> MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                }
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = ping?.let { "$it ms" } ?: "N/A",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                ping == null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                ping < 60 -> MaterialTheme.colorScheme.primary
                ping < 120 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
        )
    }
}

@Composable
fun SettingsTab(
    state: MainUiState,
    viewModel: MainScreenViewModel
) {
    var showAppPicker by remember { mutableStateOf(false) }
    val selectedAppPackages = remember(state.appRoutingPackages) {
        state.appRoutingPackages
            .split(',', '\n', '\r', ';', ' ', '\t')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    val scrollState = rememberScrollState()
    val tactileFeedback = rememberTactileFeedback()

    if (showAppPicker) {
        AppPickerDialog(
            apps = state.installedApps,
            selectedPackages = selectedAppPackages,
            onToggle = viewModel::toggleAppRoutingPackage,
            onDismiss = { showAppPicker = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Theme Selection Group
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Тема оформления",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeButton(
                        label = "System",
                        theme = AppTheme.SYSTEM_DYNAMIC,
                        active = state.appTheme == AppTheme.SYSTEM_DYNAMIC,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = { viewModel.changeTheme(AppTheme.SYSTEM_DYNAMIC) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeButton(
                        label = "Slate",
                        theme = AppTheme.MIDNIGHT_AURORA,
                        active = state.appTheme == AppTheme.MIDNIGHT_AURORA,
                        accentColor = Color(0xFFB4C6E7),
                        onClick = { viewModel.changeTheme(AppTheme.MIDNIGHT_AURORA) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeButton(
                        label = "Desert",
                        theme = AppTheme.SOLAR_FLARE,
                        active = state.appTheme == AppTheme.SOLAR_FLARE,
                        accentColor = Color(0xFFE2B697),
                        onClick = { viewModel.changeTheme(AppTheme.SOLAR_FLARE) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeButton(
                        label = "Sage",
                        theme = AppTheme.FOREST_CYBER,
                        active = state.appTheme == AppTheme.FOREST_CYBER,
                        accentColor = Color(0xFFA3B899),
                        onClick = { viewModel.changeTheme(AppTheme.FOREST_CYBER) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Routing Rules Group
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Правила маршрутизации",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val profiles = RoutingProfile.values()
                profiles.forEachIndexed { index, profile ->
                    val isSelected = state.routingProfile == profile
                    val shape = when {
                        profiles.size == 1 -> RoundedCornerShape(24.dp)
                        index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        index == profiles.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                        else -> RoundedCornerShape(4.dp)
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .clickable {
                                if (!isSelected) {
                                    tactileFeedback()
                                }
                                viewModel.changeRoutingProfile(profile)
                            },
                        shape = shape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val primaryColor = MaterialTheme.colorScheme.primary
                            val onSurfaceColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
                            Canvas(modifier = Modifier.size(20.dp)) {
                                val radius = size.minDimension / 2
                                if (isSelected) {
                                    drawCircle(color = primaryColor, radius = radius)
                                    drawCircle(color = onPrimaryColor, radius = radius / 2)
                                } else {
                                    drawCircle(
                                        color = onSurfaceColor,
                                        radius = radius - 1.dp.toPx(),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = profile.label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }
        }

        // DNS Server Group
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Выбор DNS сервера",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val dnsList = DnsServer.values()
                dnsList.forEachIndexed { index, dns ->
                    val isSelected = state.dnsServer == dns
                    val shape = when {
                        dnsList.size == 1 -> RoundedCornerShape(24.dp)
                        index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        index == dnsList.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                        else -> RoundedCornerShape(4.dp)
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .clickable {
                                if (!isSelected) {
                                    tactileFeedback()
                                }
                                viewModel.changeDnsServer(dns)
                            },
                        shape = shape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val primaryColor = MaterialTheme.colorScheme.primary
                                val onSurfaceColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
                                Canvas(modifier = Modifier.size(20.dp)) {
                                    val radius = size.minDimension / 2
                                    if (isSelected) {
                                        drawCircle(color = primaryColor, radius = radius)
                                        drawCircle(color = onPrimaryColor, radius = radius / 2)
                                    } else {
                                        drawCircle(
                                            color = onSurfaceColor,
                                            radius = radius - 1.dp.toPx(),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = dns.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = dns.address,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // TUN settings Group
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "TUN и Android VPN",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ToggleSettingCard(
                    title = "IPv6 в туннеле",
                    subtitle = "Временно выключено: Android TUN сейчас стабилизирован в IPv4-only",
                    checked = false,
                    enabled = false,
                    onCheckedChange = {},
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                )
                ToggleSettingCard(
                    title = "Обход локальных сетей",
                    subtitle = "Не забирает RFC1918, loopback и link-local IPv4 сети в VPN",
                    checked = state.lanBypassEnabled,
                    onCheckedChange = viewModel::changeLanBypassEnabled,
                    shape = RoundedCornerShape(4.dp)
                )
                ToggleSettingCard(
                    title = "Разрешить Android bypass",
                    subtitle = "Позволяет приложениям обходить VPN через системный API",
                    checked = state.systemBypassEnabled,
                    onCheckedChange = viewModel::changeSystemBypassEnabled,
                    shape = RoundedCornerShape(4.dp)
                )
                ToggleSettingCard(
                    title = "VPN как лимитная сеть",
                    subtitle = "Android будет считать туннель metered-соединением",
                    checked = state.meteredNetwork,
                    onCheckedChange = viewModel::changeMeteredNetwork,
                    shape = RoundedCornerShape(4.dp)
                )
                ToggleSettingCard(
                    title = "Автозапуск после загрузки",
                    subtitle = "Поднимает последний рабочий профиль после перезагрузки устройства",
                    checked = state.autostartEnabled,
                    onCheckedChange = viewModel::changeAutostartEnabled,
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                )
            }
        }

        // App Profile Settings Group
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Профиль приложений",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AppRoutingMode.values().forEachIndexed { index, mode ->
                    val isSelected = state.appRoutingMode == mode
                    val shape = when (index) {
                        0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        else -> RoundedCornerShape(4.dp)
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .clickable {
                                if (!isSelected) {
                                    tactileFeedback()
                                }
                                viewModel.changeAppRoutingMode(mode)
                            },
                        shape = shape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val primaryColor = MaterialTheme.colorScheme.primary
                                val onSurfaceColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
                                Canvas(modifier = Modifier.size(20.dp)) {
                                    val radius = size.minDimension / 2
                                    if (isSelected) {
                                        drawCircle(color = primaryColor, radius = radius)
                                        drawCircle(color = onPrimaryColor, radius = radius / 2)
                                    } else {
                                        drawCircle(
                                            color = onSurfaceColor,
                                            radius = radius - 1.dp.toPx(),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.padding(start = 34.dp)
                            )
                        }
                    }
                }

                val pickerShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                Card(
                    modifier = Modifier.fillMaxWidth().clip(pickerShape),
                    shape = pickerShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Выбрано: ${selectedAppPackages.size}",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Button(
                                onClick = { showAppPicker = true },
                                enabled = state.appRoutingMode != AppRoutingMode.OFF,
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Выбрать")
                            }
                            TextButton(
                                onClick = viewModel::clearAppRoutingPackages,
                                enabled = selectedAppPackages.isNotEmpty()
                            ) {
                                Text("Очистить")
                            }
                        }
                        OutlinedTextField(
                            value = state.appRoutingPackages,
                            onValueChange = viewModel::changeAppRoutingPackages,
                            label = { Text("Package names вручную") },
                            placeholder = { Text("org.telegram.messenger\ncom.discord") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.appRoutingMode != AppRoutingMode.OFF,
                            minLines = 2,
                            maxLines = 5,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }
        }

        // Health Check Settings Group
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Фильтр проверки узлов",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val inputShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                Card(
                    modifier = Modifier.fillMaxWidth().clip(inputShape),
                    shape = inputShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.healthCheckUrl,
                            onValueChange = viewModel::changeHealthCheckUrl,
                            label = { Text("URL ресурса") },
                            placeholder = { Text("https://www.gstatic.com/generate_204") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                        Button(
                            onClick = viewModel::testHealthCheckUrl,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                ToggleSettingCard(
                    title = "Строгий фильтр",
                    subtitle = "При проверке узлов помечает timeout, если ресурс недоступен",
                    checked = state.strictHealthCheck,
                    onCheckedChange = viewModel::changeStrictHealthCheck,
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                )
            }
        }

        Spacer(modifier = Modifier
            .height(180.dp)
            .navigationBarsPadding())
    }
}

@Composable
fun AppPickerDialog(
    apps: List<InstalledAppInfo>,
    selectedPackages: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val tactileFeedback = rememberTactileFeedback()
    var query by remember { mutableStateOf("") }
    val filteredApps = remember(apps, query) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            apps
        } else {
            apps.filter {
                it.label.contains(cleanQuery, ignoreCase = true) ||
                    it.packageName.contains(cleanQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Выбор приложений",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Поиск приложения или package") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                )
                Text(
                    text = "${selectedPackages.size} выбрано · ${filteredApps.size} найдено",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f),
                        fontWeight = FontWeight.Bold
                    )
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = filteredApps,
                        key = { it.packageName },
                        contentType = { "app" }
                    ) { app ->
                        val checked = app.packageName in selectedPackages
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
                                )
                                .clickable {
                                    tactileFeedback()
                                    onToggle(app.packageName)
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    tactileFeedback()
                                    onToggle(app.packageName)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = app.packageName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            if (app.isSystem) {
                                Text(
                                    text = "SYS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Готово")
            }
        }
    )
}

@Composable
fun ToggleSettingCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp)
) {
    val tactileFeedback = rememberTactileFeedback()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(enabled = enabled) {
                tactileFeedback()
                onCheckedChange(!checked)
            },
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (checked && enabled) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = if (checked && enabled) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.52f)
                        )
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.48f else 0.36f)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = {
                    tactileFeedback()
                    onCheckedChange(it)
                },
                enabled = enabled
            )
        }
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconContainerColor: Color,
    iconContentColor: Color,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconContainerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconContentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            content()
        }
    }
}

@Composable
fun ThemeButton(
    label: String,
    theme: AppTheme,
    active: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tactileFeedback = rememberTactileFeedback()
    val buttonColor by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        label = "themeButtonColor"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(buttonColor)
            .clickable {
                if (!active) {
                    tactileFeedback()
                }
                onClick()
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Canvas(modifier = Modifier.size(12.dp)) {
                drawCircle(color = accentColor, radius = size.minDimension / 2)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun LogsTab(
    state: MainUiState,
    viewModel: MainScreenViewModel
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val filteredLogs = remember(searchQuery, state.logs) {
        if (searchQuery.isBlank()) {
            state.logs
        } else {
            state.logs.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Поиск в логах...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                maxLines = 1,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.exportLogs(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .height(42.dp)
                        .weight(1f)
                ) {
                    Text("Экспорт", fontWeight = FontWeight.Bold, maxLines = 1)
                }

                Button(
                    onClick = { viewModel.clearLogs() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .height(42.dp)
                        .weight(1f)
                ) {
                    Text("Очистить", fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Logs Terminal View
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(14.dp)
        ) {
            if (filteredLogs.isEmpty()) {
                Text(
                    text = "Записей не найдено.",
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = filteredLogs,
                        contentType = { "log" }
                    ) { log ->
                        val textColor = when {
                            log.contains("[ERROR]", ignoreCase = true) -> Color(0xFFEF4444)
                            log.contains("[WARNING]", ignoreCase = true) -> Color(0xFFFBBF24)
                            log.contains("[INFO]", ignoreCase = true) -> Color(0xFF60A5FA)
                            log.contains("[DEBUG]", ignoreCase = true) -> Color(0xFF10B981)
                            else -> Color(0xFFE2E8F0)
                        }
                        Text(
                            text = log,
                            color = textColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier
                .navigationBarsPadding()
                .height(80.dp)
        )
    }
}

@Composable
fun BottomEdgeFade(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.52f to MaterialTheme.colorScheme.background.copy(alpha = 0.16f),
                    1f to MaterialTheme.colorScheme.background.copy(alpha = 0.58f)
                )
            )
    )
}

@Composable
fun FloatingContextAction(
    selectedTab: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tactileFeedback = rememberTactileFeedback()
    val icon = when (selectedTab) {
        0 -> Icons.Default.Search
        1 -> Icons.Default.Refresh
        2 -> Icons.Default.Settings
        else -> Icons.Default.Add
    }
    val containerColor by animateColorAsState(
        targetValue = when (selectedTab) {
            0 -> MaterialTheme.colorScheme.primaryContainer
            1 -> MaterialTheme.colorScheme.tertiaryContainer
            2 -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.errorContainer
        },
        label = "floatingActionColor"
    )
    val contentColor = when (selectedTab) {
        0 -> MaterialTheme.colorScheme.onPrimaryContainer
        1 -> MaterialTheme.colorScheme.onTertiaryContainer
        2 -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onErrorContainer
    }
    val buttonShape = RoundedCornerShape(16.dp)

    Surface(
        modifier = modifier
            .size(54.dp)
            .shadow(
                elevation = 12.dp,
                shape = buttonShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.18f),
                spotColor = Color.Black.copy(alpha = 0.24f)
            )
            .clip(buttonShape)
            .clickable {
                tactileFeedback()
                onClick()
            },
        shape = buttonShape,
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun MainTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tactileFeedback = rememberTactileFeedback()

    Surface(
        modifier = modifier
            .width(240.dp)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(30.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.18f),
                spotColor = Color.Black.copy(alpha = 0.28f)
            ),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Pair("Главная", Icons.Default.Refresh),
                Pair("Прокси", Icons.Default.List),
                Pair("Настройки", Icons.Default.Settings),
                Pair("Логи", Icons.Default.Info)
            )

            tabs.forEachIndexed { index, tab ->
                val active = selectedTab == index
                val activeContainer = when (index) {
                    0 -> MaterialTheme.colorScheme.primaryContainer
                    1 -> MaterialTheme.colorScheme.secondaryContainer
                    2 -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val activeOnContainer = when (index) {
                    0 -> MaterialTheme.colorScheme.onPrimaryContainer
                    1 -> MaterialTheme.colorScheme.onSecondaryContainer
                    2 -> MaterialTheme.colorScheme.onTertiaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val tabWeight by animateFloatAsState(
                    targetValue = if (active) 1.38f else 0.82f,
                    label = "tabWeight"
                )
                val activeBgColor by animateColorAsState(
                    targetValue = if (active) activeContainer else Color.Transparent,
                    label = "tabBg"
                )
                val activeContentColor by animateColorAsState(
                    targetValue = if (active) activeOnContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                    label = "tabContent"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(tabWeight)
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(activeBgColor)
                        .clickable {
                            if (!active) {
                                tactileFeedback()
                            }
                            onTabSelected(index)
                        }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 7.dp)
                    ) {
                        Icon(
                            imageVector = tab.second,
                            contentDescription = tab.first,
                            tint = activeContentColor,
                            modifier = Modifier.size(if (active) 19.dp else 18.dp)
                        )
                        AnimatedVisibility(visible = active) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = tab.first,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Black,
                                    color = activeContentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CopyIcon(modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val strokePx = 1.5.dp.toPx()
        // Back card outline
        drawRoundRect(
            color = tint.copy(alpha = 0.5f),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.1f),
            size = androidx.compose.ui.geometry.Size(w * 0.55f, h * 0.55f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            style = Stroke(width = strokePx)
        )
        // Front card outline
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.35f),
            size = androidx.compose.ui.geometry.Size(w * 0.55f, h * 0.55f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            style = Stroke(width = strokePx)
        )
    }
}


