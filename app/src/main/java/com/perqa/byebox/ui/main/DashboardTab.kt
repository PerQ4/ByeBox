package com.perqa.byebox.ui.main

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.basicMarquee
import androidx.compose.material.icons.filled.ArrowBack
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perqa.byebox.MainActivity
import com.perqa.byebox.data.ProxyConfig
import com.perqa.byebox.data.SettingsProfileData
import com.perqa.byebox.findActivity
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardTab(
    state: MainUiState,
    viewModel: MainScreenViewModel,
    onTabSelected: (Int) -> Unit,
    onShowBottomBar: (Boolean) -> Unit
) {
    var activeScreen by remember { mutableStateOf("dashboard") }
    var editingProfile by remember { mutableStateOf<SettingsProfileData?>(null) }
    var editorReferrer by remember { mutableStateOf("manager") }
    var pendingProfileDeleteId by remember { mutableStateOf<String?>(null) }

    val activeConfig = state.configs.find { it.id == state.activeConfigId }
    val activeProfileName = state.profiles.find { it.id == state.activeProfileId }?.name ?: "По умолчанию"
    val context = LocalContext.current
    val activity = context.findActivity() as? MainActivity

    LaunchedEffect(activeScreen) {
        onShowBottomBar(activeScreen == "dashboard")
    }

    BackHandler(enabled = activeScreen != "dashboard") {
        if (activeScreen == "editor") {
            activeScreen = editorReferrer
            editingProfile = null
        } else if (activeScreen == "manager") {
            activeScreen = "dashboard"
        }
    }

    if (pendingProfileDeleteId != null) {
        val deleteProfileName = state.profiles.find { it.id == pendingProfileDeleteId }?.name ?: ""
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingProfileDeleteId = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = Loc.get("delete_profile_confirm_title", state.language),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                val msg = Loc.get("delete_profile_confirm_msg", state.language).replace("{name}", deleteProfileName)
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        viewModel.deleteProfile(pendingProfileDeleteId!!)
                        pendingProfileDeleteId = null
                    }
                ) {
                    Text(
                        text = Loc.get("delete_btn", state.language),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingProfileDeleteId = null }) {
                    Text(
                        text = Loc.get("cancel_btn", state.language),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    when (activeScreen) {
        "dashboard" -> {
            DashboardScreen(
                state = state,
                viewModel = viewModel,
                activeConfig = activeConfig,
                activeProfileName = activeProfileName,
                activity = activity,
                onNavigateToManager = { activeScreen = "manager" },
                onNavigateToProxy = { onTabSelected(1) },
                onEditProfile = { profile ->
                    editingProfile = profile
                    editorReferrer = "dashboard"
                    activeScreen = "editor"
                }
            )
        }
        "manager" -> {
            QuickSwitchManagerScreen(
                profiles = state.profiles,
                activeProfileId = state.activeProfileId,
                configs = state.configs,
                viewModel = viewModel,
                onBack = { activeScreen = "dashboard" },
                onSelect = { viewModel.changeActiveProfileId(it) },
                onAdd = { newProfile ->
                    viewModel.addProfile(newProfile)
                },
                onDelete = { id ->
                    if (state.confirmRemoveEnabled) {
                        pendingProfileDeleteId = id
                    } else {
                        viewModel.deleteProfile(id)
                    }
                },
                onEditTriggered = { profile ->
                    editingProfile = profile
                    editorReferrer = "manager"
                    activeScreen = "editor"
                },
                onReorder = { viewModel.reorderProfiles(it) },
                state = state
            )
        }
        "editor" -> {
            if (editingProfile != null) {
                QuickSwitchEditScreen(
                    profile = editingProfile!!,
                    configs = state.configs,
                    apps = state.installedApps,
                    state = state,
                    onBack = {
                        editingProfile = null
                        activeScreen = editorReferrer
                    },
                    onSave = { updated ->
                        viewModel.updateProfile(updated)
                        editingProfile = null
                        activeScreen = editorReferrer
                    }
                )
            } else {
                activeScreen = "manager"
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    state: MainUiState,
    viewModel: MainScreenViewModel,
    activeConfig: ProxyConfig?,
    activeProfileName: String,
    activity: MainActivity?,
    onNavigateToManager: () -> Unit,
    onNavigateToProxy: () -> Unit,
    onEditProfile: (SettingsProfileData) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // 1. Status Overview Card
        StatusOverviewCard(
            status = state.connectionStatus,
            activeConfig = activeConfig,
            routingProfile = state.routingProfile,
            dnsServer = state.dnsServer,
            activeProfileName = activeProfileName,
            downloadSpeed = state.downloadSpeed,
            uploadSpeed = state.uploadSpeed,
            language = state.language,
            onPingRefresh = { viewModel.testActiveConfigPing() },
            onNavigateToProxy = onNavigateToProxy
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Connection Button (placed directly on the screen)
        ConnectionButton(
            status = state.connectionStatus,
            pulseEnabled = state.pulseEnabled,
            onClick = {
                if (activity != null) {
                    val shouldConnect = state.connectionStatus == ConnectionStatus.DISCONNECTED
                    if (shouldConnect) {
                        val transitioned = viewModel.setConnectingState()
                        if (!transitioned) {
                            viewModel.showToast(Loc.get("no_active_config", state.language))
                            return@ConnectionButton
                        }
                    }
                    activity.handleVpnToggle(shouldConnect)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Profiles Switcher Section wrapped in a Card substrate
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Профили настроек",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    IconButton(
                        onClick = onNavigateToManager,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Управление профилями",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(modifier = Modifier.width(16.dp))
                    state.profiles.forEach { profile ->
                        val isActive = profile.id == state.activeProfileId
                        val assignedServer = state.configs.find { it.id == profile.assignedConfigId }
                        Card(
                            modifier = Modifier
                                .width(140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .combinedClickable(
                                    onClick = { viewModel.changeActiveProfileId(profile.id) },
                                    onLongClick = {
                                        onEditProfile(profile)
                                    }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = profile.name,
                                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = assignedServer?.countryFlag ?: "🏳️",
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = assignedServer?.name ?: "Текущий",
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .basicMarquee(iterations = Int.MAX_VALUE),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            fontWeight = FontWeight.Medium
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6. Quick Action Console Grid
        QuickActionsCard(
            onBestServer = { viewModel.selectBestConfig() },
            onShare = { activity?.shareActiveConfig() },
            onVpnSettings = { activity?.openSystemVpnSettings() },
            onAddTile = { activity?.requestQuickSettingsTile() },
            language = state.language
        )

        // Bottom spacer to account for the bottom bar pill overlay
        Spacer(
            modifier = Modifier
                .height(130.dp)
                .navigationBarsPadding()
        )
    }
}

@Composable
fun TelemetrySpeedCard(
    label: String,
    value: String,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
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
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            )
        }
    }
}

@Composable
fun CockpitServerCard(
    activeConfig: ProxyConfig,
    onPingRefresh: () -> Unit,
    onNavigateToProxy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable { onNavigateToProxy() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = activeConfig.countryFlag,
            fontSize = 32.sp,
            modifier = Modifier.padding(end = 12.dp)
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
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = activeConfig.protocol,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${activeConfig.address}:${activeConfig.port}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Ping actions
        IconButton(
            onClick = {
                onPingRefresh()
            },
            modifier = Modifier
                .padding(end = 6.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Пинг",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }

        // Delay status pill
        val ping = activeConfig.ping
        val pillBg = when {
            ping == null -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            ping < 60 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ping < 120 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
            else -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
        }
        val pillColor = when {
            ping == null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            ping < 60 -> MaterialTheme.colorScheme.primary
            ping < 120 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.error
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(pillBg)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (ping != null) "${ping} ms" else "N/A",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = pillColor,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            )
        }
    }
}

@Composable
fun StatusOverviewCard(
    status: ConnectionStatus,
    activeConfig: ProxyConfig?,
    routingProfile: RoutingProfile,
    dnsServer: DnsServer,
    activeProfileName: String,
    downloadSpeed: String,
    uploadSpeed: String,
    language: String = "ru",
    modifier: Modifier = Modifier,
    onPingRefresh: () -> Unit,
    onNavigateToProxy: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = when (status) {
            ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
            ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.tertiaryContainer
            ConnectionStatus.RECONNECTING -> MaterialTheme.colorScheme.errorContainer
            ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "statusOverviewColor"
    )
    val contentColor by animateColorAsState(
        targetValue = when (status) {
            ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.onPrimaryContainer
            ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.onTertiaryContainer
            ConnectionStatus.RECONNECTING -> MaterialTheme.colorScheme.onErrorContainer
            ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.onSurface
        },
        label = "statusOverviewContentColor"
    )
    val contentColorVariant = contentColor.copy(alpha = 0.6f)
    val shape = RoundedCornerShape(topStart = 34.dp, topEnd = 18.dp, bottomEnd = 34.dp, bottomStart = 18.dp)

    Card(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .clip(shape),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = when (status) {
                    ConnectionStatus.CONNECTED -> Loc.get("status_connected", language)
                    ConnectionStatus.CONNECTING -> Loc.get("status_connecting", language)
                    ConnectionStatus.RECONNECTING -> Loc.get("status_reconnecting", language)
                    ConnectionStatus.DISCONNECTED -> Loc.get("status_disconnected", language)
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = contentColor
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(text = "Профиль: $activeProfileName", textColor = contentColor, modifier = Modifier.weight(1f))
                InfoChip(text = routingProfile.label, textColor = contentColor, modifier = Modifier.weight(1f))
                InfoChip(text = dnsServer.label, textColor = contentColor, modifier = Modifier.weight(1f))
            }

            // Speed row
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.28f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = "↑ $uploadSpeed",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                    Text(
                        text = "↓ $downloadSpeed",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    )
                }
            }

            if (activeConfig != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(contentColor.copy(alpha = 0.12f))
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToProxy() }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = activeConfig.countryFlag,
                        fontSize = 32.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeConfig.name,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(contentColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = activeConfig.protocol,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = contentColor
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${activeConfig.address}:${activeConfig.port}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = contentColorVariant,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            onPingRefresh()
                        },
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Пинг",
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    val ping = activeConfig.ping
                    val pillBg = when {
                        ping == null -> contentColor.copy(alpha = 0.08f)
                        ping < 60 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ping < 120 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    }
                    val pillColor = when {
                        ping == null -> contentColorVariant
                        ping < 60 -> MaterialTheme.colorScheme.primary
                        ping < 120 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(pillBg)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (ping != null) "${ping} ms" else "N/A",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = pillColor,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = Loc.get("status_no_server", language),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = contentColorVariant,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.48f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = textColor
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
    onAddTile: () -> Unit,
    language: String = "ru"
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(28.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Быстрые утилиты",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickActionButton(Loc.get("quick_best", language), Icons.Default.Search, onBestServer, Modifier.weight(1f))
                QuickActionButton(Loc.get("quick_share", language), Icons.Default.Add, onShare, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickActionButton(Loc.get("quick_vpn_settings", language), Icons.Default.Settings, onVpnSettings, Modifier.weight(1f))
                QuickActionButton(Loc.get("quick_tile", language), Icons.Default.Info, onAddTile, Modifier.weight(1f))
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
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun ConnectionButton(
    status: ConnectionStatus,
    pulseEnabled: Boolean = true,
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
            ConnectionStatus.RECONNECTING -> MaterialTheme.colorScheme.error
            ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.surface
        },
        label = "buttonColor"
    )

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    
    val path1 = remember { Path() }
    val path2 = remember { Path() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(200.dp)
            .padding(10.dp)
    ) {
        if (status == ConnectionStatus.CONNECTED) {
            val auraAlpha1 by infiniteTransition.animateFloat(
                initialValue = 0.12f,
                targetValue = 0.38f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "auraAlpha1"
            )
            val auraScale1 by infiniteTransition.animateFloat(
                initialValue = 0.90f,
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "auraScale1"
            )
            val auraAlpha2 by infiniteTransition.animateFloat(
                initialValue = 0.04f,
                targetValue = 0.22f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "auraAlpha2"
            )
            val auraScale2 by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.45f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "auraScale2"
            )

            // Outer Aura Glow
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .graphicsLayer {
                        scaleX = auraScale2
                        scaleY = auraScale2
                        alpha = auraAlpha2
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                buttonBgColor,
                                buttonBgColor.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Inner Aura Glow
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .graphicsLayer {
                        scaleX = auraScale1
                        scaleY = auraScale1
                        alpha = auraAlpha1
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                buttonBgColor,
                                buttonBgColor.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = center
            val baseRadius = 80.dp.toPx()
            
            if (status == ConnectionStatus.CONNECTING || status == ConnectionStatus.RECONNECTING) {
                path1.reset()
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

                path2.reset()
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
                if (pulseEnabled) {
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
                    drawCircle(
                        color = buttonBgColor.copy(alpha = 0.16f),
                        radius = baseRadius,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            } else {
                drawCircle(
                    color = onSurfaceColor.copy(alpha = 0.08f),
                    radius = baseRadius,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(150.dp)
                .shadow(
                    elevation = if (status != ConnectionStatus.DISCONNECTED) 12.dp else 4.dp,
                    shape = CircleShape
                )
                .clip(CircleShape)
                .background(buttonBgColor)
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
                        .rotate(if (status == ConnectionStatus.CONNECTING || status == ConnectionStatus.RECONNECTING) progressRotate else 0f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (status) {
                        ConnectionStatus.CONNECTED -> "ВКЛЮЧЕНО"
                        ConnectionStatus.CONNECTING -> "ПОИСК..."
                        ConnectionStatus.RECONNECTING -> "ОТМЕНА"
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
fun QuickSwitchManagerScreen(
    profiles: List<SettingsProfileData>,
    activeProfileId: String,
    configs: List<ProxyConfig>,
    viewModel: MainScreenViewModel,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onAdd: (SettingsProfileData) -> Unit,
    onDelete: (String) -> Unit,
    onEditTriggered: (SettingsProfileData) -> Unit,
    onReorder: (List<SettingsProfileData>) -> Unit,
    state: MainUiState
) {
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    val currentProfiles by rememberUpdatedState(profiles)
    val currentDraggedIndex by rememberUpdatedState(draggedIndex)
    val tactileFeedback = rememberTactileFeedback()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Назад",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.rotate(-90f)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Профили быстрого свитчера",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    tactileFeedback()
                    val newProfile = SettingsProfileData(name = "Новый профиль")
                    onAdd(newProfile)
                    onEditTriggered(newProfile)
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить профиль",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Text(
            text = "Зажмите рукоятку для сортировки. Нажмите на плитку профиля, чтобы изменить его параметры.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(profiles, key = { it.id }) { profile ->
                val index = profiles.indexOfFirst { it.id == profile.id }
                val isActive = profile.id == activeProfileId
                val isDragged = draggedIndex == index
                val currentIndex by rememberUpdatedState(index)
                val assignedServer = configs.find { it.id == profile.assignedConfigId }

                val containerColor = when {
                    isDragged -> MaterialTheme.colorScheme.surfaceContainerHighest
                    isActive -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                }

                val contentColor = when {
                    isActive -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragged) 50f else 1f)
                        .graphicsLayer {
                            if (isDragged) {
                                translationY = dragOffsetY
                                scaleX = 1.03f
                                scaleY = 1.03f
                                alpha = 1.0f
                            } else {
                                translationY = 0f
                                scaleX = 1f
                                scaleY = 1f
                                alpha = 1.0f
                            }
                        }
                        .shadow(
                            elevation = if (isDragged) 16.dp else 2.dp,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            tactileFeedback()
                            onSelect(profile.id)
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = containerColor,
                        contentColor = contentColor
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Drag Handle
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(contentColor.copy(alpha = 0.08f))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggedIndex = currentIndex
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y
                                            
                                            val itemHeightPx = 86.dp.toPx()
                                            val currIndex = currentDraggedIndex
                                            if (currIndex != null) {
                                                val list = currentProfiles
                                                if (dragOffsetY > itemHeightPx / 2 && currIndex < list.lastIndex) {
                                                    val next = currIndex + 1
                                                    val mutable = list.toMutableList()
                                                    val item = mutable.removeAt(currIndex)
                                                    mutable.add(next, item)
                                                    onReorder(mutable)
                                                    draggedIndex = next
                                                    dragOffsetY -= itemHeightPx
                                                } else if (dragOffsetY < -itemHeightPx / 2 && currIndex > 0) {
                                                    val prev = currIndex - 1
                                                    val mutable = list.toMutableList()
                                                    val item = mutable.removeAt(currIndex)
                                                    mutable.add(prev, item)
                                                    onReorder(mutable)
                                                    draggedIndex = prev
                                                    dragOffsetY += itemHeightPx
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            draggedIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggedIndex = null
                                            dragOffsetY = 0f
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Reorder,
                                contentDescription = "Перетащить",
                                tint = contentColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Info
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .basicMarquee(iterations = Int.MAX_VALUE)
                                )
                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "АКТИВЕН",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = assignedServer?.let { "${it.countryFlag} ${it.name}" } ?: "Не назначен (Текущий)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = contentColor.copy(alpha = 0.6f)
                                ),
                                maxLines = 1,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .basicMarquee(iterations = Int.MAX_VALUE)
                            )
                        }

                        // Select / Delete actions
                        // Select / Delete actions
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    tactileFeedback()
                                    onEditTriggered(profile)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Редактировать",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (profile.id != "base" && profiles.size > 1) {
                                IconButton(
                                    onClick = {
                                        tactileFeedback()
                                        onDelete(profile.id)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Удалить",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(30.dp).navigationBarsPadding())
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun QuickSwitchEditScreen(
    profile: SettingsProfileData,
    configs: List<ProxyConfig>,
    apps: List<InstalledAppInfo>,
    state: MainUiState,
    onBack: () -> Unit,
    onSave: (SettingsProfileData) -> Unit
) {
    var name by remember(profile) { mutableStateOf(profile.name) }
    var assignedConfigId by remember(profile) { mutableStateOf(profile.assignedConfigId) }
    var routingProfile by remember(profile) { mutableStateOf(profile.routingProfile) }
    var dnsServer by remember(profile) { mutableStateOf(profile.dnsServer) }
    var customDnsServer by remember(profile) { mutableStateOf(profile.customDnsServer) }
    var appRoutingMode by remember(profile) { mutableStateOf(profile.appRoutingMode) }
    var tunStack by remember(profile) { mutableStateOf(profile.tunStack) }
    var fakeDnsEnabled by remember(profile) { mutableStateOf(profile.fakeDnsEnabled) }
    var fragmentEnabled by remember(profile) { mutableStateOf(profile.fragmentEnabled) }
    var muxEnabled by remember(profile) { mutableStateOf(profile.muxEnabled) }
    var sniffingEnabled by remember(profile) { mutableStateOf(profile.sniffingEnabled) }
    var customDirectRules by remember(profile) { mutableStateOf(profile.customDirectRules) }
    var customProxyRules by remember(profile) { mutableStateOf(profile.customProxyRules) }
    var appRoutingPackages by remember(profile) { mutableStateOf(profile.appRoutingPackages ?: emptySet()) }

    var showServerPicker by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    val assignedServer = configs.find { it.id == assignedConfigId }
    val scrollState = rememberScrollState()
    val tactileFeedback = rememberTactileFeedback()

    val hasChanges by remember(profile) {
        derivedStateOf<Boolean> {
            name != profile.name ||
            assignedConfigId != profile.assignedConfigId ||
            routingProfile != profile.routingProfile ||
            dnsServer != profile.dnsServer ||
            customDnsServer != profile.customDnsServer ||
            appRoutingMode != profile.appRoutingMode ||
            tunStack != profile.tunStack ||
            fakeDnsEnabled != profile.fakeDnsEnabled ||
            fragmentEnabled != profile.fragmentEnabled ||
            muxEnabled != profile.muxEnabled ||
            sniffingEnabled != profile.sniffingEnabled ||
            customDirectRules != profile.customDirectRules ||
            customProxyRules != profile.customProxyRules ||
            appRoutingPackages != (profile.appRoutingPackages ?: emptySet<String>())
        }
    }

    BackHandler(enabled = showAppPicker || showServerPicker || showDiscardConfirm || hasChanges) {
        if (showAppPicker) {
            showAppPicker = false
        } else if (showServerPicker) {
            showServerPicker = false
        } else if (showDiscardConfirm) {
            showDiscardConfirm = false
        } else if (hasChanges) {
            showDiscardConfirm = true
        }
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text(text = "Несохраненные изменения") },
            text = { Text(text = "Несохраненные изменения будут потеряны. Вы действительно хотите выйти?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirm = false
                        onBack()
                    }
                ) {
                    Text("Выйти")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (hasChanges) {
                        showDiscardConfirm = true
                    } else {
                        onBack()
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Назад",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.rotate(-90f)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Настройки профиля",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Основные настройки
            SettingsGroup(title = "ОСНОВНЫЕ НАСТРОЙКИ") {
                SettingsRowSurface(
                    top = true,
                    bottom = false,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Название профиля") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Укажите запоминающееся имя для этого набора настроек",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                val selectedServerName = when (assignedConfigId) {
                    "LAST_ACTIVE" -> "Предыдущий активный (автосохранение)"
                    null -> "Не изменять (текущий сервер)"
                    else -> assignedServer?.let { "${it.countryFlag} ${it.name} (${it.protocol})" } ?: "Использовать текущий сервер"
                }
                SettingsRowSurface(
                    top = false,
                    bottom = true,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = { showServerPicker = true }
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        SettingsRowText(
                            title = "Привязанный сервер",
                            subtitle = selectedServerName
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Выберите сервер для автоматического переключения при активации профиля.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(180f)
                    )
                }
            }

            // 2. DNS Сервера
            SettingsGroup(title = "DNS СЕРВЕРА") {
                SettingsChoiceRow(
                    title = "Наследовать от базовых настроек",
                    subtitle = "Глобальные настройки DNS из раздела настроек",
                    selected = dnsServer == "INHERIT",
                    top = true,
                    bottom = false,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = {
                        tactileFeedback()
                        dnsServer = "INHERIT"
                    }
                )
                DnsServer.values().forEachIndexed { index, dns ->
                    val isLast = index == DnsServer.values().lastIndex
                    val dnsSub = when (dns.name) {
                        "SYSTEM" -> "Использовать системные DNS-адреса вашего Android-устройства"
                        "GOOGLE" -> "Безопасные и быстрые DNS от Google (8.8.8.8, 8.8.4.4)"
                        "CLOUDFLARE" -> "Конфиденциальные DNS от Cloudflare (1.1.1.1, 1.0.0.1)"
                        "ADGUARD" -> "Фильтрация рекламы, трекеров и фишинговых сайтов"
                        "CUSTOM" -> customDnsServer.orEmpty().ifBlank { "Нажмите для ввода IP" }
                        else -> dns.address
                    }
                    SettingsChoiceRow(
                        title = dns.label,
                        subtitle = dnsSub,
                        selected = dnsServer == dns.name,
                        top = false,
                        bottom = isLast && dnsServer != "CUSTOM",
                        scaleFactor = state.tapImpactScale,
                        cornerRoundness = state.cornerRoundness,
                        onClick = {
                            tactileFeedback()
                            dnsServer = dns.name
                        }
                    )
                }
                if (dnsServer == "CUSTOM") {
                    SettingsRowSurface(
                        bottom = true,
                        scaleFactor = state.tapImpactScale,
                        cornerRoundness = state.cornerRoundness
                    ) {
                        OutlinedTextField(
                            value = customDnsServer ?: "",
                            onValueChange = { customDnsServer = it },
                            placeholder = { Text("Например: 8.8.4.4") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            }

            // 3. TUN Стек
            SettingsGroup(title = "TUN СТЕК") {
                SettingsChoiceRow(
                    title = "Наследовать от базовых настроек",
                    subtitle = "Глобальный стек TUN из раздела настроек",
                    selected = tunStack == "INHERIT",
                    top = true,
                    bottom = false,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = {
                        tactileFeedback()
                        tunStack = "INHERIT"
                    }
                )
                TunStack.values().forEachIndexed { index, stack ->
                    val stackSub = when (stack.name) {
                        "GVISOR" -> "Стек на базе gVisor (по умолчанию, высокая стабильность)"
                        "SYSTEM" -> "Системный сетевой стек Android"
                        "MIXED" -> "Смешанный сетевой стек для оптимизации маршрутизации"
                        else -> stack.description
                    }
                    SettingsChoiceRow(
                        title = stack.label,
                        subtitle = stackSub,
                        selected = tunStack == stack.name,
                        top = false,
                        bottom = index == TunStack.values().lastIndex,
                        scaleFactor = state.tapImpactScale,
                        cornerRoundness = state.cornerRoundness,
                        onClick = { tunStack = stack.name }
                    )
                }
            }

            // 4. Функции ядра Xray
            SettingsGroup(title = "ФУНКЦИИ ЯДРА XRAY") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val binaryOptions = listOf(
                        null to "Наследовать",
                        true to "Включено",
                        false to "Выключено"
                    )

                    @Composable
                    fun XrayOption(
                        label: String,
                        description: String,
                        selected: Boolean?,
                        onSelected: (Boolean?) -> Unit
                    ) {
                        Column {
                            SegmentedSelector(
                                label = label,
                                options = binaryOptions,
                                selected = selected,
                                onSelected = onSelected
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    XrayOption(
                        label = "Сниффинг трафика",
                        description = "Автоматическое определение доменных имен для точной фильтрации и обхода блокировок.",
                        selected = sniffingEnabled,
                        onSelected = { sniffingEnabled = it }
                    )

                    XrayOption(
                        label = "Фрагментация (Fragment)",
                        description = "Разбиение пакетов для обхода глубокого анализа пакетов (DPI) провайдерами.",
                        selected = fragmentEnabled,
                        onSelected = { fragmentEnabled = it }
                    )

                    XrayOption(
                        label = "Мультиплексирование (Mux)",
                        description = "Объединение TCP-соединений в один канал для снижения задержек установления связи.",
                        selected = muxEnabled,
                        onSelected = { muxEnabled = it }
                    )

                    XrayOption(
                        label = "Fake DNS",
                        description = "Подмена реальных DNS-ответов фейковыми IP-адресами для ускорения резолва и повышения анонимности.",
                        selected = fakeDnsEnabled,
                        onSelected = { fakeDnsEnabled = it }
                    )
                }
            }

            // 5. Маршрутизация трафика
            SettingsGroup(title = "МАРШРУТИЗАЦИЯ ТРАФИКА") {
                SettingsChoiceRow(
                    title = "Наследовать от базовых настроек",
                    subtitle = "Глобальный профиль маршрутизации",
                    selected = routingProfile == "INHERIT",
                    top = true,
                    bottom = false,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = {
                        tactileFeedback()
                        routingProfile = "INHERIT"
                    }
                )
                SettingsChoiceRow(
                    title = "Обход LAN, Китая и РФ",
                    subtitle = "Ресурсы локальной сети и региональные сайты (РФ, Китай) направляются напрямую, остальные — через прокси.",
                    selected = routingProfile == "BYPASS_LAN_CN_RU",
                    top = false,
                    bottom = false,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = { routingProfile = "BYPASS_LAN_CN_RU" }
                )
                SettingsChoiceRow(
                    title = "Проксировать всё",
                    subtitle = "Абсолютно весь трафик устройства направляется через VPN туннель.",
                    selected = routingProfile == "PROXY_ALL",
                    top = false,
                    bottom = false,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = { routingProfile = "PROXY_ALL" }
                )
                SettingsChoiceRow(
                    title = "Прямое подключение",
                    subtitle = "Все соединения идут напрямую в обход VPN проксирования.",
                    selected = routingProfile == "DIRECT",
                    top = false,
                    bottom = true,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = { routingProfile = "DIRECT" }
                )
            }

            // 6. Туннелирование приложений
            SettingsGroup(title = "ТУННЕЛИРОВАНИЕ ПРИЛОЖЕНИЙ") {
                SettingsChoiceRow(
                    title = "Наследовать от базовых настроек",
                    subtitle = "Глобальные настройки туннелирования приложений",
                    selected = appRoutingMode == "INHERIT",
                    top = true,
                    bottom = false,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = {
                        tactileFeedback()
                        appRoutingMode = "INHERIT"
                    }
                )
                SettingsChoiceRow(
                    title = "Все приложения",
                    subtitle = "Проксировать трафик абсолютно всех установленных приложений.",
                    selected = appRoutingMode == "OFF",
                    top = false,
                    bottom = false,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = { appRoutingMode = "OFF" }
                )
                SettingsChoiceRow(
                    title = "Только выбранные приложения",
                    subtitle = "VPN туннелирование будет работать исключительно для выбранных в списке приложений.",
                    selected = appRoutingMode == "ONLY_SELECTED",
                    top = false,
                    bottom = false,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = { appRoutingMode = "ONLY_SELECTED" }
                )
                SettingsChoiceRow(
                    title = "Обход выбранных приложений",
                    subtitle = "Выбранные приложения будут работать напрямую в обход VPN, остальные — через прокси.",
                    selected = appRoutingMode == "BYPASS_SELECTED",
                    top = false,
                    bottom = appRoutingMode == "OFF" || appRoutingMode == "INHERIT",
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = { appRoutingMode = "BYPASS_SELECTED" }
                )
                if (appRoutingMode != "OFF" && appRoutingMode != "INHERIT") {
                    SettingsActionRow(
                        title = "Выбранные приложения",
                        subtitle = "${appRoutingPackages.size} приложений выбрано",
                        button = "Выбрать",
                        enabled = true,
                        scaleFactor = state.tapImpactScale,
                        cornerRoundness = state.cornerRoundness,
                        onClick = { showAppPicker = true },
                        bottom = true
                    )
                }
            }

            // 7. Пользовательские правила
            SettingsGroup(title = "ПОЛЬЗОВАТЕЛЬСКИЕ ПРАВИЛА") {
                // Direct Rules
                SettingsChoiceRow(
                    title = "Наследовать правила DIRECT",
                    subtitle = "Использовать глобальный список прямого подключения",
                    selected = customDirectRules == null,
                    top = true,
                    bottom = false,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = {
                        tactileFeedback()
                        customDirectRules = null
                    }
                )
                SettingsChoiceRow(
                    title = "Собственные правила DIRECT",
                    subtitle = "Задать индивидуальный список доменов и IP для прямого подключения",
                    selected = customDirectRules != null,
                    top = false,
                    bottom = customDirectRules == null,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = {
                        tactileFeedback()
                        if (customDirectRules == null) {
                            customDirectRules = ""
                        }
                    }
                )
                if (customDirectRules != null) {
                    SettingsRowSurface(
                        bottom = true,
                        scaleFactor = state.tapImpactScale,
                        cornerRoundness = state.cornerRoundness
                    ) {
                        OutlinedTextField(
                            value = customDirectRules ?: "",
                            onValueChange = { customDirectRules = it },
                            placeholder = { Text("domain:yandex.ru, geoip:private, 192.168.1.0/24") },
                            singleLine = false,
                            maxLines = 4,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Proxy Rules
                SettingsChoiceRow(
                    title = "Наследовать правила PROXY",
                    subtitle = "Использовать глобальный список проксирования",
                    selected = customProxyRules == null,
                    top = true,
                    bottom = false,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = {
                        tactileFeedback()
                        customProxyRules = null
                    }
                )
                SettingsChoiceRow(
                    title = "Собственные правила PROXY",
                    subtitle = "Задать индивидуальный список доменов и IP для проксирования",
                    selected = customProxyRules != null,
                    top = false,
                    bottom = customProxyRules == null,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = {
                        tactileFeedback()
                        if (customProxyRules == null) {
                            customProxyRules = ""
                        }
                    }
                )
                if (customProxyRules != null) {
                    SettingsRowSurface(
                        bottom = true,
                        scaleFactor = state.tapImpactScale,
                        cornerRoundness = state.cornerRoundness
                    ) {
                        OutlinedTextField(
                            value = customProxyRules ?: "",
                            onValueChange = { customProxyRules = it },
                            placeholder = { Text("domain:youtube.com, domain:instagram.com") },
                            singleLine = false,
                            maxLines = 4,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                tactileFeedback()
                onSave(profile.copy(
                    name = name,
                    assignedConfigId = assignedConfigId,
                    routingProfile = routingProfile,
                    dnsServer = dnsServer,
                    customDnsServer = customDnsServer,
                    appRoutingMode = appRoutingMode,
                    tunStack = tunStack,
                    fakeDnsEnabled = fakeDnsEnabled,
                    fragmentEnabled = fragmentEnabled,
                    muxEnabled = muxEnabled,
                    sniffingEnabled = sniffingEnabled,
                    customDirectRules = customDirectRules,
                    customProxyRules = customProxyRules,
                    appRoutingPackages = if (appRoutingMode == "INHERIT") null else appRoutingPackages
                ))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Сохранить изменения", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp).navigationBarsPadding())
    }

    if (showAppPicker) {
        AppPickerSheet(
            apps = apps,
            selectedPackages = appRoutingPackages,
            onSave = {
                appRoutingPackages = it
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }

    if (showServerPicker) {
        ProfileServerPickerSheet(
            configs = configs,
            selectedConfigId = assignedConfigId,
            onSelect = {
                assignedConfigId = it
                showServerPicker = false
            },
            onDismiss = { showServerPicker = false }
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ProfileServerPickerSheet(
    configs: List<ProxyConfig>,
    selectedConfigId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Выбор сервера",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(null)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedConfigId == null) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🏳️",
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                text = "Не изменять (использовать текущий сервер)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedConfigId == null) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect("LAST_ACTIVE")
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedConfigId == "LAST_ACTIVE") MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔄",
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                text = "Предыдущий активный (автосохранение)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedConfigId == "LAST_ACTIVE") MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
                
                items(configs) { config ->
                    val isSelected = config.id == selectedConfigId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(config.id)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = config.countryFlag,
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = config.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${config.protocol} · ${config.address}:${config.port}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

