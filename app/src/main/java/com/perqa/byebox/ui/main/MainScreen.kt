package com.perqa.byebox.ui.main

import com.perqa.byebox.MainActivity
import com.perqa.byebox.findActivity
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.blur
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
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .padding(bottom = 118.dp)
                ) {
                    AppHeader(
                        status = state.connectionStatus,
                        onLogsClick = { selectedTab = 3 }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

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
                                0 -> DashboardTab(state = state, viewModel = viewModel)
                                1 -> ProxyTab(state = state, viewModel = viewModel)
                                2 -> SettingsTab(state = state, viewModel = viewModel)
                                3 -> LogsTab(state = state, viewModel = viewModel)
                            }
                        }
                    }
                }

                BottomNavFade(
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                MainTabBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
fun AppHeader(
    status: ConnectionStatus,
    onLogsClick: () -> Unit
) {
    val statusColor by animateColorAsState(
        targetValue = when (status) {
            ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primary
            ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.tertiary
            ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        },
        label = "statusColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "BYEBOX",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (status) {
                        ConnectionStatus.CONNECTED -> "ПОДКЛЮЧЕНО"
                        ConnectionStatus.CONNECTING -> "ПОДКЛЮЧЕНИЕ..."
                        ConnectionStatus.DISCONNECTED -> "ОТКЛЮЧЕНО"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onLogsClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Логи",
                    tint = MaterialTheme.colorScheme.primary
                )
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
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
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        RoundedCornerShape(28.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
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

        Spacer(modifier = Modifier.height(20.dp))
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
            ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)
            ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
            ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f)
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
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
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
    val containerColor = when (icon) {
        Icons.Default.Search -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        Icons.Default.Add -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
        Icons.Default.Settings -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when (icon) {
        Icons.Default.Search -> MaterialTheme.colorScheme.onPrimaryContainer
        Icons.Default.Add -> MaterialTheme.colorScheme.onSecondaryContainer
        Icons.Default.Settings -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Button(
        onClick = onClick,
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

    val buttonBgColor by animateColorAsState(
        targetValue = when (status) {
            ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primary
            ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.tertiary
            ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.surface
        },
        label = "buttonColor"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(200.dp)
            .padding(10.dp)
    ) {
        // Simple outer circular border, flat and clean
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.dp.toPx()
            drawCircle(
                color = when (status) {
                    ConnectionStatus.CONNECTED -> buttonBgColor.copy(alpha = 0.2f)
                    else -> Color.White.copy(alpha = 0.05f)
                },
                style = Stroke(width = strokeWidth)
            )
        }

        // Connecting loader arc
        if (status == ConnectionStatus.CONNECTING) {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(progressRotate),
                color = MaterialTheme.colorScheme.tertiary,
                strokeWidth = 3.dp
            )
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
                .clickable { onClick() }
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
            .clip(RoundedCornerShape(24.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                RoundedCornerShape(24.dp)
        ),
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
fun ProxyTab(
    state: MainUiState,
    viewModel: MainScreenViewModel
) {
    var importUrl by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(NodeSortMode.SOURCE) }
    val sourceGroups = remember(state.configs, sortMode) {
        state.configs
            .groupBy { it.sourceName.ifBlank { "Локальные конфигурации" } }
            .mapValues { (_, configs) -> configs.sortedFor(sortMode) }
            .toList()
            .let { groups ->
                if (sortMode == NodeSortMode.SOURCE) groups.sortedBy { it.first.lowercase() } else groups
            }
    }
    val sourcesByName = remember(state.subscriptionSources) {
        state.subscriptionSources.associateBy { it.name }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 18.dp, bottomEnd = 32.dp, bottomStart = 18.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
            )
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
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "${sourceGroups.size} источника · ${state.configs.size} узлов",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    if (state.isPinging) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = importUrl,
                    onValueChange = { importUrl = it },
                    placeholder = {
                        Text(
                            "Вставьте vless://, vmess://, ss://, trojan://",
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.48f)
                    ),
                    maxLines = 1
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
                            viewModel.refreshSubscriptions()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.secondary
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
                            viewModel.testPings()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.tertiary
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
                            if (importUrl.isNotBlank()) {
                                viewModel.addConfigFromUrl(importUrl)
                                importUrl = ""
                            } else {
                                viewModel.showToast("Вставьте ссылку в поле!")
                            }
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
                SortModeBar(
                    selected = sortMode,
                    onSelected = { sortMode = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 138.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            sourceGroups.forEach { (sourceName, configs) ->
                item(key = "source-$sourceName") {
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
                        showConfigs = false
                    )
                }
                items(configs, key = { it.id }) { config ->
                    ServerItemCard(
                        config = config,
                        isActive = config.id == state.activeConfigId,
                        onSelect = { viewModel.selectConfig(config.id) },
                        onDelete = { viewModel.deleteConfig(config.id) }
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
fun SortModeBar(
    selected: NodeSortMode,
    onSelected: (NodeSortMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.48f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        NodeSortMode.values().forEach { mode ->
            val active = selected == mode
            Button(
                onClick = { onSelected(mode) },
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (active) {
                        when (mode) {
                            NodeSortMode.SOURCE -> MaterialTheme.colorScheme.primaryContainer
                            NodeSortMode.PING -> MaterialTheme.colorScheme.tertiaryContainer
                            NodeSortMode.NAME -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    } else {
                        Color.Transparent
                    },
                    contentColor = if (active) {
                        when (mode) {
                            NodeSortMode.SOURCE -> MaterialTheme.colorScheme.onPrimaryContainer
                            NodeSortMode.PING -> MaterialTheme.colorScheme.onTertiaryContainer
                            NodeSortMode.NAME -> MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                ),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(mode.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
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
    showConfigs: Boolean = true
) {
    val sourceUrl = configs.firstOrNull { it.sourceUrl != null }?.sourceUrl
    val averagePing = configs.mapNotNull { it.ping }.takeIf { it.isNotEmpty() }?.average()?.toInt()
    val activeCount = configs.count { it.id == activeConfigId }
    val groupShape = RoundedCornerShape(topStart = 28.dp, topEnd = 16.dp, bottomEnd = 28.dp, bottomStart = 16.dp)
    var isRenaming by remember(source?.id) { mutableStateOf(false) }
    var editedName by remember(source?.id, sourceName) { mutableStateOf(source?.name ?: sourceName) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(groupShape),
        colors = CardDefaults.cardColors(
            containerColor = if (activeCount > 0) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.18f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
            }
        )
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
            }

            source?.let {
                Button(
                    onClick = onPingSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .padding(bottom = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Пинг узлов источника", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SourceActionButton("Обновить", Icons.Default.Refresh, { onRefreshSource(it.id) }, Modifier.weight(1f))
                    SourceActionButton("Имя", Icons.Default.Settings, { isRenaming = !isRenaming }, Modifier.weight(1f))
                    SourceActionButton("Удалить", Icons.Default.Delete, { onDeleteSource(it.id) }, Modifier.weight(1f), destructive = true)
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
    val containerColor = when {
        destructive -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
        icon == Icons.Default.Refresh -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        icon == Icons.Default.Settings -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        destructive -> MaterialTheme.colorScheme.onErrorContainer
        icon == Icons.Default.Refresh -> MaterialTheme.colorScheme.onSecondaryContainer
        icon == Icons.Default.Settings -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Button(
        onClick = onClick,
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
    onDelete: () -> Unit
) {
    val activeBorderColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "activeBorder"
    )

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val asymmetricShape = RoundedCornerShape(topStart = 20.dp, bottomEnd = 20.dp, topEnd = 10.dp, bottomStart = 10.dp)
    val protocolDetails = remember(config) { config.protocolSummary() }
    val endpointDetails = remember(config) { config.endpointSummary() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(asymmetricShape)
            .border(
                1.5.dp,
                activeBorderColor,
                asymmetricShape
            )
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = config.countryFlag,
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 10.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
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
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                PingPill(config.ping)
                Spacer(modifier = Modifier.width(4.dp))

                // Quick Copy Link Button
                IconButton(
                    onClick = {
                        val link = config.toConfigLink()
                        if (link.isNotBlank()) {
                            clipboardManager.setText(AnnotatedString(link))
                            Toast.makeText(context, "Ссылка скопирована!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    CopyIcon(modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
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
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Theme Selection Card
        SettingsSectionCard(
            title = "Тема оформления (Material 3)",
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
        ) {
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

        // Routing Rules Selection Card
        SettingsSectionCard(
            title = "Правила маршрутизации",
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RoutingProfile.values().forEach { profile ->
                    val isSelected = state.routingProfile == profile
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                                }
                            )
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.changeRoutingProfile(profile) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .border(
                                    2.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    CircleShape
                                )
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = profile.label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }

        // DNS configuration Card
        SettingsSectionCard(
            title = "Выбор DNS сервера",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DnsServer.values().forEach { dns ->
                    val isSelected = state.dnsServer == dns
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                                }
                            )
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.changeDnsServer(dns) }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = dns.label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = dns.address,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        SettingsSectionCard(
            title = "TUN и Android VPN",
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToggleSettingRow(
                    title = "IPv6 в туннеле",
                    subtitle = "Временно выключено: Android TUN сейчас стабилизирован в IPv4-only",
                    checked = false,
                    enabled = false,
                    onCheckedChange = {}
                )
                ToggleSettingRow(
                    title = "Обход локальных сетей",
                    subtitle = "Не забирает RFC1918, loopback и link-local IPv4 сети в VPN",
                    checked = state.lanBypassEnabled,
                    onCheckedChange = viewModel::changeLanBypassEnabled
                )
                ToggleSettingRow(
                    title = "Разрешить Android bypass",
                    subtitle = "Позволяет приложениям обходить VPN через системный API",
                    checked = state.systemBypassEnabled,
                    onCheckedChange = viewModel::changeSystemBypassEnabled
                )
                ToggleSettingRow(
                    title = "VPN как лимитная сеть",
                    subtitle = "Android будет считать туннель metered-соединением",
                    checked = state.meteredNetwork,
                    onCheckedChange = viewModel::changeMeteredNetwork
                )
                ToggleSettingRow(
                    title = "Автозапуск после загрузки",
                    subtitle = "Поднимает последний рабочий профиль после перезагрузки устройства",
                    checked = state.autostartEnabled,
                    onCheckedChange = viewModel::changeAutostartEnabled
                )
            }
        }

        SettingsSectionCard(
            title = "Профиль приложений",
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.18f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppRoutingMode.values().forEach { mode ->
                    val isSelected = state.appRoutingMode == mode
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.changeAppRoutingMode(mode) }
                            .padding(14.dp)
                    ) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = mode.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f)
                            )
                        )
                    }
                }
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
                        shape = RoundedCornerShape(18.dp),
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
                    shape = RoundedCornerShape(18.dp)
                )
            }
        }

        SettingsSectionCard(
            title = "Фильтр проверки узлов",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.18f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        shape = RoundedCornerShape(18.dp)
                    )
                    Button(
                        onClick = viewModel::testHealthCheckUrl,
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
                ToggleSettingRow(
                    title = "Строгий фильтр",
                    subtitle = "При проверке узлов помечает timeout, если ресурс недоступен",
                    checked = state.strictHealthCheck,
                    onCheckedChange = viewModel::changeStrictHealthCheck
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun AppPickerDialog(
    apps: List<InstalledAppInfo>,
    selectedPackages: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
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
                    items(filteredApps, key = { it.packageName }) { app ->
                        val checked = app.packageName in selectedPackages
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
                                )
                                .clickable { onToggle(app.packageName) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { onToggle(app.packageName) }
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
fun ToggleSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val rowScale by animateFloatAsState(
        targetValue = if (checked && enabled) 1.01f else 1f,
        label = "toggleRowScale"
    )
    val rowColor by animateColorAsState(
        targetValue = when {
            checked -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
        },
        label = "toggleRowColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
        label = "toggleRowBorder"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(rowScale)
            .clip(RoundedCornerShape(16.dp))
            .background(rowColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp)),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )
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
    val buttonScale by animateFloatAsState(
        targetValue = if (active) 1.03f else 1f,
        label = "themeButtonScale"
    )
    val buttonColor by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.46f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
        },
        label = "themeButtonColor"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(56.dp)
            .scale(buttonScale)
            .clip(RoundedCornerShape(18.dp))
            .background(buttonColor)
            .border(
                1.5.dp,
                if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Поиск в логах...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
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

            Button(
                onClick = { viewModel.exportLogs(context) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Text("Экспорт", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { viewModel.clearLogs() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Text("Очистить", fontWeight = FontWeight.Bold)
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
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
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
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredLogs) { log ->
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
    }
}

@Composable
fun BottomNavFade(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(122.dp)
            .blur(18.dp)
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.42f to MaterialTheme.colorScheme.background.copy(alpha = 0.58f),
                    1f to MaterialTheme.colorScheme.background.copy(alpha = 0.96f)
                )
            )
    )
}

@Composable
fun MainTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 64.dp, end = 64.dp, top = 22.dp, bottom = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 26.dp,
                    shape = RoundedCornerShape(30.dp),
                    clip = false,
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    spotColor = Color.Black.copy(alpha = 0.38f)
                )
                .clip(RoundedCornerShape(30.dp))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f),
                    RoundedCornerShape(30.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
            )
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
                        targetValue = if (active) 1.52f else 0.78f,
                        label = "tabWeight"
                    )
                    val tabScale by animateFloatAsState(
                        targetValue = if (active) 1.04f else 1f,
                        label = "tabScale"
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
                            .height(48.dp)
                            .scale(tabScale)
                            .clip(RoundedCornerShape(24.dp))
                            .background(activeBgColor)
                            .clickable { onTabSelected(index) }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Icon(
                                imageVector = tab.second,
                                contentDescription = tab.first,
                                tint = activeContentColor,
                                modifier = Modifier.size(if (active) 21.dp else 20.dp)
                            )
                            AnimatedVisibility(visible = active) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(modifier = Modifier.width(6.dp))
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


