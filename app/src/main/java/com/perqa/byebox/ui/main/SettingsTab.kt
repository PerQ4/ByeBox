package com.perqa.byebox.ui.main

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueryBuilder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perqa.byebox.MainActivity
import com.perqa.byebox.findActivity
import com.perqa.byebox.theme.AppTheme
import com.perqa.byebox.ui.main.dashboard.InfoChip
import com.perqa.byebox.theme.DarkThemeStyle

enum class SettingsSubMenu {
    CONNECTION,
    ROUTING,
    APPEARANCE,
    SYSTEM,
    LOGS
}

@Composable
fun SettingsCategoryCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    scaleFactor: Float = 0.90f,
    cornerRoundness: String = "expressive",
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val isExpressive = cornerRoundness == "expressive"
    val baseRadius = if (isExpressive) 24.dp else 12.dp
    val targetRadius = if (isPressed) baseRadius + 6.dp else baseRadius
    val cornerRadius by animateDpAsState(targetValue = targetRadius, label = "categoryCardCornerRadius")
    val shape = RoundedCornerShape(cornerRadius)
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleFactor else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "categoryCardScale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier
                    .size(24.dp)
                    .rotate(90f)
            )
        }
    }
}

@Composable
fun SettingsTab(
    state: MainUiState,
    viewModel: MainScreenViewModel,
    onShowBottomBar: (Boolean) -> Unit
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

    var selectedSubMenu by remember { mutableStateOf<SettingsSubMenu?>(null) }
    
    LaunchedEffect(selectedSubMenu) {
        onShowBottomBar(selectedSubMenu == null)
    }

    BackHandler(enabled = selectedSubMenu != null) {
        selectedSubMenu = null
    }

    if (showAppPicker) {
        AppPickerSheet(
            apps = state.installedApps,
            selectedPackages = selectedAppPackages,
            onSave = { newPackages ->
                val joined = newPackages.sorted().joinToString("\n")
                viewModel.changeAppRoutingPackages(joined)
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (selectedSubMenu == null) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = Loc.get("title_settings", state.language),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                
                SettingsHeroCard(
                    status = state.connectionStatus,
                    routingProfile = state.routingProfile,
                    dnsServer = state.dnsServer,
                    appRoutingMode = state.appRoutingMode,
                    language = state.language
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                SettingsCategoryCard(
                    title = Loc.get("submenu_connection", state.language),
                    description = Loc.get("conn_params", state.language),
                    icon = Icons.Default.Build,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = {
                        tactileFeedback()
                        selectedSubMenu = SettingsSubMenu.CONNECTION
                    }
                )
                
                SettingsCategoryCard(
                    title = Loc.get("submenu_rules", state.language),
                    description = Loc.get("rules_params", state.language),
                    icon = Icons.Default.List,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = {
                        tactileFeedback()
                        selectedSubMenu = SettingsSubMenu.ROUTING
                    }
                )
                
                SettingsCategoryCard(
                    title = Loc.get("submenu_appearance", state.language),
                    description = Loc.get("appearance_params", state.language),
                    icon = Icons.Default.Star,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = {
                        tactileFeedback()
                        selectedSubMenu = SettingsSubMenu.APPEARANCE
                    }
                )
                
                SettingsCategoryCard(
                    title = Loc.get("submenu_system", state.language),
                    description = Loc.get("system_params", state.language),
                    icon = Icons.Default.Settings,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = {
                        tactileFeedback()
                        selectedSubMenu = SettingsSubMenu.SYSTEM
                    }
                )

                SettingsCategoryCard(
                    title = Loc.get("title_logs", state.language),
                    description = Loc.get("logs_params", state.language),
                    icon = Icons.Default.List,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = {
                        tactileFeedback()
                        selectedSubMenu = SettingsSubMenu.LOGS
                    }
                )

                val updateInfo = state.updateInfo
                if (updateInfo != null) {
                    UpdateBanner(
                        updateInfo = updateInfo,
                        language = state.language,
                        scaleFactor = state.tapImpactScale,
                        cornerRoundness = state.cornerRoundness,
                    )
                }

                Spacer(
                    modifier = Modifier
                        .height(130.dp)
                        .navigationBarsPadding()
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { selectedSubMenu = null },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = Loc.get("back", state.language),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(-90f)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = when (selectedSubMenu) {
                        SettingsSubMenu.CONNECTION -> Loc.get("submenu_connection", state.language)
                        SettingsSubMenu.ROUTING -> Loc.get("submenu_rules", state.language)
                        SettingsSubMenu.APPEARANCE -> Loc.get("submenu_appearance", state.language)
                        SettingsSubMenu.SYSTEM -> Loc.get("submenu_system", state.language)
                        SettingsSubMenu.LOGS -> Loc.get("title_logs", state.language)
                        else -> ""
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            
            if (selectedSubMenu == SettingsSubMenu.LOGS) {
                // Logs has its own LazyColumn - render directly without wrapping scroll
                LogsTab(state = state, viewModel = viewModel, embedMode = true)
            } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedSubMenu) {
                    SettingsSubMenu.CONNECTION -> {
                        SettingsGroup(title = Loc.get("submenu_connection", state.language)) {
                            SettingsSwitchRow(
                                title = Loc.get("ipv6_tunnel", state.language),
                                subtitle = Loc.get("ipv6_tunnel_sub", state.language),
                                checked = state.ipv6Enabled,
                                icon = Icons.Default.Build,
                                top = true,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeIpv6Enabled
                            )
                            SettingsSwitchRow(
                                title = Loc.get("prefer_ipv6", state.language),
                                subtitle = Loc.get("prefer_ipv6_sub", state.language),
                                checked = state.preferIpv6Enabled,
                                icon = Icons.Default.Build,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changePreferIpv6Enabled
                            )
                            SettingsSwitchRow(
                                title = Loc.get("lan_bypass", state.language),
                                subtitle = Loc.get("lan_bypass_sub", state.language),
                                checked = state.lanBypassEnabled,
                                icon = Icons.Default.Home,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeLanBypassEnabled
                            )
                            SettingsSwitchRow(
                                title = Loc.get("blocking_vpn", state.language),
                                subtitle = Loc.get("blocking_vpn_sub", state.language),
                                checked = state.blockingEnabled,
                                icon = Icons.Default.Lock,
                                bottom = true,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeBlockingEnabled
                            )
                        }

                        SettingsGroup(title = Loc.get("dns_servers", state.language)) {
                            DnsServer.values().forEachIndexed { index, dns ->
                                val isLast = index == DnsServer.values().lastIndex
                                val dnsIcon = when (dns) {
                                    DnsServer.SYSTEM -> Icons.Default.Dns
                                    DnsServer.CLOUDFLARE -> Icons.Default.Cloud
                                    DnsServer.GOOGLE -> Icons.Default.Language
                                    DnsServer.ADGUARD -> Icons.Default.Shield
                                    DnsServer.CUSTOM -> Icons.Default.Settings
                                }
                                SettingsChoiceRow(
                                    title = dns.label,
                                    subtitle = if (dns == DnsServer.CUSTOM) state.customDnsServer.ifBlank { Loc.get("press_to_enter_ip", state.language) } else dns.address,
                                    selected = state.dnsServer == dns,
                                    icon = dnsIcon,
                                    top = index == 0,
                                    bottom = isLast && state.dnsServer != DnsServer.CUSTOM,
                                    scaleFactor = state.tapImpactScale,
                                    cornerRoundness = state.cornerRoundness,
                                    onClick = {
                                        tactileFeedback()
                                        viewModel.changeDnsServer(dns)
                                    }
                                )
                            }
                            if (state.dnsServer == DnsServer.CUSTOM) {
                                SettingsRowSurface(
                                    bottom = true,
                                    scaleFactor = state.tapImpactScale,
                                    cornerRoundness = state.cornerRoundness
                                ) {
                                    OutlinedTextField(
                                        value = state.customDnsServer,
                                        onValueChange = viewModel::changeCustomDnsServer,
                                        placeholder = { Text(Loc.get("example_dns", state.language)) },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                }
                            }
                        }

                        SettingsGroup(title = Loc.get("local_proxy", state.language)) {
                            SettingsSwitchRow(
                                title = Loc.get("vpn_mode", state.language),
                                subtitle = Loc.get("vpn_mode_sub", state.language),
                                checked = state.vpnModeEnabled,
                                icon = Icons.Default.Lock,
                                top = true,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeVpnModeEnabled
                            )
                            SettingsSwitchRow(
                                title = Loc.get("lan_sharing", state.language),
                                subtitle = Loc.get("lan_sharing_sub", state.language),
                                checked = state.proxySharingEnabled,
                                icon = Icons.Default.Share,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeProxySharingEnabled
                            )
                            SettingsRowSurface(
                                bottom = true,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = Loc.get("socks_port_title", state.language),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = Loc.get("socks_port_desc", state.language),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    )
                                }
                                OutlinedTextField(
                                    value = state.socksPort,
                                    onValueChange = viewModel::changeSocksPort,
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.width(90.dp),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                        }

                        SettingsGroup(title = Loc.get("tun_stack_title", state.language)) {
                            TunStack.values().forEachIndexed { index, stack ->
                                val stackIcon = when (stack) {
                                    TunStack.GVISOR -> Icons.Default.Shield
                                    TunStack.SYSTEM -> Icons.Default.Build
                                }
                                SettingsChoiceRow(
                                    title = stack.label,
                                    subtitle = stack.description,
                                    selected = state.tunStack == stack,
                                    icon = stackIcon,
                                    top = index == 0,
                                    bottom = index == TunStack.values().lastIndex,
                                    scaleFactor = state.tapImpactScale,
                                    cornerRoundness = state.cornerRoundness,
                                    onClick = { viewModel.changeTunStack(stack) }
                                )
                            }
                        }
                    }

                    SettingsSubMenu.ROUTING -> {
                        SettingsGroup(title = Loc.get("routing_rules", state.language)) {
                            RoutingProfile.values().forEachIndexed { index, profile ->
                                val profileIcon = when (profile) {
                                    RoutingProfile.BYPASS_LAN_CN_RU -> Icons.Default.AltRoute
                                    RoutingProfile.PROXY_ALL -> Icons.Default.Language
                                    RoutingProfile.DIRECT -> Icons.Default.Navigation
                                }
                                SettingsChoiceRow(
                                    title = profile.label,
                                    subtitle = when (profile) {
                                        RoutingProfile.BYPASS_LAN_CN_RU -> Loc.get("routing_bypass_desc", state.language)
                                        RoutingProfile.PROXY_ALL -> Loc.get("routing_proxy_all_desc", state.language)
                                        RoutingProfile.DIRECT -> Loc.get("routing_direct_desc", state.language)
                                    },
                                    selected = state.routingProfile == profile,
                                    icon = profileIcon,
                                    top = index == 0,
                                    bottom = index == RoutingProfile.values().lastIndex,
                                    scaleFactor = state.tapImpactScale,
                                    cornerRoundness = state.cornerRoundness,
                                    onClick = {
                                        tactileFeedback()
                                        viewModel.changeRoutingProfile(profile)
                                    }
                                )
                            }
                        }

                        SettingsGroup(title = Loc.get("custom_rules", state.language)) {
                            SettingsRowSurface(
                                top = true,
                                bottom = false,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text(
                                        text = Loc.get("custom_direct_title", state.language),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = Loc.get("custom_direct_desc", state.language),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        ),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    OutlinedTextField(
                                        value = state.customDirectRules,
                                        onValueChange = viewModel::changeCustomDirectRules,
                                        placeholder = { Text(Loc.get("example_direct", state.language)) },
                                        singleLine = false,
                                        maxLines = 3,
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                }
                            }
                            SettingsRowSurface(
                                top = false,
                                bottom = true,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text(
                                        text = Loc.get("custom_proxy_title", state.language),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = Loc.get("custom_proxy_desc", state.language),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        ),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    OutlinedTextField(
                                        value = state.customProxyRules,
                                        onValueChange = viewModel::changeCustomProxyRules,
                                        placeholder = { Text(Loc.get("example_proxy", state.language)) },
                                        singleLine = false,
                                        maxLines = 3,
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                }
                            }
                        }

                        SettingsGroup(title = Loc.get("app_profile", state.language)) {
                            AppRoutingMode.values().forEachIndexed { index, mode ->
                                val modeIcon = when (mode) {
                                    AppRoutingMode.OFF -> Icons.Default.Language
                                    AppRoutingMode.ONLY_SELECTED -> Icons.Default.CheckCircle
                                    AppRoutingMode.BYPASS_SELECTED -> Icons.Default.DoNotDisturbOn
                                }
                                SettingsChoiceRow(
                                    title = mode.label,
                                    subtitle = mode.description,
                                    selected = state.appRoutingMode == mode,
                                    icon = modeIcon,
                                    top = index == 0,
                                    bottom = false,
                                    scaleFactor = state.tapImpactScale,
                                    cornerRoundness = state.cornerRoundness,
                                    onClick = {
                                        tactileFeedback()
                                        viewModel.changeAppRoutingMode(mode)
                                    }
                                )
                            }
                            SettingsActionRow(
                                title = Loc.get("selected_apps", state.language),
                                subtitle = String.format(Loc.get("packages_selected", state.language), selectedAppPackages.size),
                                button = Loc.get("select_btn", state.language),
                                enabled = state.appRoutingMode != AppRoutingMode.OFF,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onClick = {
                                    tactileFeedback()
                                    showAppPicker = true
                                },
                                bottom = true
                            )
                        }

                        SettingsGroup(title = Loc.get("xray_features", state.language)) {
                            SettingsSwitchRow(
                                title = Loc.get("sniffing", state.language),
                                subtitle = Loc.get("sniffing_desc", state.language),
                                checked = state.sniffingEnabled,
                                icon = Icons.Default.Search,
                                top = true,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeSniffingEnabled
                            )
                            SettingsSwitchRow(
                                title = Loc.get("fragment", state.language),
                                subtitle = Loc.get("fragment_desc", state.language),
                                checked = state.fragmentEnabled,
                                icon = Icons.Default.Build,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeFragmentEnabled
                            )
                            SettingsSwitchRow(
                                title = Loc.get("mux", state.language),
                                subtitle = Loc.get("mux_desc", state.language),
                                checked = state.muxEnabled,
                                icon = Icons.Default.Settings,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeMuxEnabled
                            )
                            SettingsSwitchRow(
                                title = Loc.get("fake_dns", state.language),
                                subtitle = Loc.get("fake_dns_desc", state.language),
                                checked = state.fakeDnsEnabled,
                                icon = Icons.Default.Settings,
                                bottom = true,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeFakeDnsEnabled
                            )
                        }

                        // Domain Strategy
                        val domainStrategyOptions = listOf(
                            "AsIs" to (Loc.get("domain_strategy_as_is", state.language) to Loc.get("domain_strategy_as_is_desc", state.language)),
                            "IPOnDemand" to (Loc.get("domain_strategy_ip_on_demand", state.language) to Loc.get("domain_strategy_ip_on_demand_desc", state.language)),
                            "IPIfNonMatch" to (Loc.get("domain_strategy_ip_if_non_match", state.language) to Loc.get("domain_strategy_ip_if_non_match_desc", state.language))
                        )
                        SettingsGroup(title = Loc.get("routing_domain_strategy", state.language)) {
                            domainStrategyOptions.forEachIndexed { index, (value, labels) ->
                                val strategyIcon = when (value) {
                                    "AsIs" -> Icons.Default.Forward
                                    "IPOnDemand" -> Icons.Default.QueryBuilder
                                    else -> Icons.Default.CompareArrows
                                }
                                SettingsChoiceRow(
                                    title = labels.first,
                                    subtitle = labels.second,
                                    selected = state.routingDomainStrategy == value,
                                    icon = strategyIcon,
                                    top = index == 0,
                                    bottom = index == domainStrategyOptions.lastIndex,
                                    scaleFactor = state.tapImpactScale,
                                    cornerRoundness = state.cornerRoundness,
                                    onClick = {
                                        tactileFeedback()
                                        viewModel.changeRoutingDomainStrategy(value)
                                    }
                                )
                            }
                        }

                        // Outbound Resolve Method
                        val outboundResolveOptions = listOf(
                            "0" to (Loc.get("outbound_resolve_default", state.language) to Loc.get("outbound_resolve_default_desc", state.language)),
                            "1" to (Loc.get("outbound_resolve_use_ip", state.language) to Loc.get("outbound_resolve_use_ip_desc", state.language)),
                            "2" to (Loc.get("outbound_resolve_replace", state.language) to Loc.get("outbound_resolve_replace_desc", state.language))
                        )
                        SettingsGroup(title = Loc.get("outbound_domain_resolve_method", state.language)) {
                            outboundResolveOptions.forEachIndexed { index, (value, labels) ->
                                val resolveIcon = when (value) {
                                    "0" -> Icons.Default.Dns
                                    "1" -> Icons.Default.Tag
                                    else -> Icons.Default.SwapHoriz
                                }
                                SettingsChoiceRow(
                                    title = labels.first,
                                    subtitle = labels.second,
                                    selected = state.outboundDomainResolveMethod == value,
                                    icon = resolveIcon,
                                    top = index == 0,
                                    bottom = index == outboundResolveOptions.lastIndex,
                                    scaleFactor = state.tapImpactScale,
                                    cornerRoundness = state.cornerRoundness,
                                    onClick = {
                                        tactileFeedback()
                                        viewModel.changeOutboundDomainResolveMethod(value)
                                    }
                                )
                            }
                        }
                    }

                    SettingsSubMenu.APPEARANCE -> {
                        SettingsGroup(title = Loc.get("theme_title", state.language)) {
                            SettingsThemeGrid(state = state, viewModel = viewModel)
                        }
                        
                        SettingsGroup(title = Loc.get("haptics_title", state.language)) {
                            val options = listOf(
                                1.00f to Loc.get("tap_impact_none", state.language),
                                0.95f to Loc.get("tap_impact_light", state.language),
                                0.90f to Loc.get("tap_impact_medium", state.language),
                                0.85f to Loc.get("tap_impact_deep", state.language)
                            )
                            options.forEachIndexed { index, (value, label) ->
                                SettingsChoiceRow(
                                    title = label,
                                    subtitle = Loc.get("tap_impact_sub", state.language),
                                    selected = state.tapImpactScale == value,
                                    icon = Icons.Default.Vibration,
                                    top = index == 0,
                                    bottom = index == options.lastIndex,
                                    scaleFactor = state.tapImpactScale,
                                    cornerRoundness = state.cornerRoundness,
                                    onClick = { viewModel.changeTapImpactScale(value) }
                                )
                            }
                        }
                        
                        SettingsGroup(title = Loc.get("roundness_title", state.language)) {
                            val roundnessOptions = listOf(
                                "standard" to Loc.get("corner_std", state.language),
                                "expressive" to Loc.get("corner_expr", state.language)
                            )
                            roundnessOptions.forEachIndexed { index, (value, label) ->
                                val roundnessIcon = when (value) {
                                    "standard" -> Icons.Default.CropSquare
                                    else -> Icons.Default.Category
                                }
                                SettingsChoiceRow(
                                    title = label,
                                    subtitle = if (value == "expressive") Loc.get("corner_expr_sub", state.language) else Loc.get("corner_std_sub", state.language),
                                    selected = state.cornerRoundness == value,
                                    icon = roundnessIcon,
                                    top = index == 0,
                                    bottom = index == roundnessOptions.lastIndex,
                                    scaleFactor = state.tapImpactScale,
                                    cornerRoundness = state.cornerRoundness,
                                    onClick = { viewModel.changeCornerRoundness(value) }
                                )
                            }
                        }
                        
                        SettingsGroup(title = Loc.get("effects_title", state.language)) {
                            SettingsSwitchRow(
                                title = Loc.get("pulse_btn", state.language),
                                subtitle = Loc.get("pulse_btn_sub", state.language),
                                checked = state.pulseEnabled,
                                icon = Icons.Default.Star,
                                top = true,
                                bottom = false,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changePulseEnabled
                            )
                            SettingsSwitchRow(
                                title = Loc.get("glass_bar", state.language),
                                subtitle = Loc.get("glass_bar_sub", state.language),
                                checked = state.glassmorphicBar,
                                icon = Icons.Default.Share,
                                top = false,
                                bottom = false,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeGlassmorphicBar
                            )
                            SettingsSwitchRow(
                                title = Loc.get("max_blur", state.language),
                                subtitle = Loc.get("max_blur_sub", state.language),
                                checked = state.maxBlurEnabled,
                                icon = Icons.Default.Lock,
                                top = false,
                                bottom = true,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeMaxBlurEnabled
                            )
                        }

                        SettingsGroup(title = Loc.get("dark_theme_style_title", state.language)) {
                            DarkThemeStyle.values().forEachIndexed { index, style ->
                                val isFirst = index == 0
                                val isLast = index == DarkThemeStyle.values().lastIndex
                                SettingsChoiceRow(
                                    title = when (style) {
                                    DarkThemeStyle.STANDARD -> Loc.get("dark_standard", state.language)
                                    DarkThemeStyle.DEEP_SLATE -> Loc.get("dark_deep_slate", state.language)
                                    DarkThemeStyle.MIDNIGHT_NAVY -> Loc.get("dark_midnight_navy", state.language)
                                    DarkThemeStyle.PURE_BLACK -> Loc.get("dark_pure_black", state.language)
                                    },
                                    subtitle = when (style) {
                                        DarkThemeStyle.STANDARD -> Loc.get("dark_standard_sub", state.language)
                                        DarkThemeStyle.DEEP_SLATE -> Loc.get("dark_deep_slate_sub", state.language)
                                        DarkThemeStyle.MIDNIGHT_NAVY -> Loc.get("dark_midnight_navy_sub", state.language)
                                        DarkThemeStyle.PURE_BLACK -> Loc.get("dark_pure_black_sub", state.language)
                                    },
                                    selected = state.darkThemeStyle == style,
                                    top = isFirst,
                                    bottom = isLast,
                                    scaleFactor = state.tapImpactScale,
                                    cornerRoundness = state.cornerRoundness,
                                    onClick = { viewModel.changeDarkThemeStyle(style) }
                                )
                            }
                        }

                        SettingsGroup(title = Loc.get("display_params_title", state.language)) {
                            SettingsSwitchRow(
                                title = Loc.get("compact_proxy_list", state.language),
                                subtitle = Loc.get("compact_proxy_list_sub", state.language),
                                checked = state.compactLayoutEnabled,
                                icon = Icons.Default.List,
                                top = true,
                                bottom = false,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeCompactLayoutEnabled
                            )
                            SettingsSwitchRow(
                                title = Loc.get("show_flags", state.language),
                                subtitle = Loc.get("show_flags_sub", state.language),
                                checked = state.showFlagsEnabled,
                                icon = Icons.Default.Star,
                                top = false,
                                bottom = true,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeShowFlagsEnabled
                            )
                        }
                    }

                    SettingsSubMenu.SYSTEM -> {
                        SettingsGroup(title = Loc.get("language", state.language)) {
                            val languages = listOf(
                                "system" to Loc.get("lang_system", state.language),
                                "ru" to Loc.get("lang_ru", state.language),
                                "en" to Loc.get("lang_en", state.language),
                                "zh" to Loc.get("lang_zh", state.language)
                            )
                            languages.forEachIndexed { index, (value, label) ->
                                val subtitle = when (value) {
                                    "system" -> {
                                        val sysLang = java.util.Locale.getDefault().language
                                        val systemResolved = when {
                                            sysLang.startsWith("ru") || sysLang.startsWith("be") || sysLang.startsWith("uk") || sysLang.startsWith("kk") || sysLang.startsWith("ky") -> Loc.get("lang_ru", state.language)
                                            sysLang.startsWith("zh") -> Loc.get("lang_zh", state.language)
                                            else -> Loc.get("lang_en", state.language)
                                        }
                                        "${Loc.get("lang_system", state.language)} ($systemResolved)"
                                    }
                                    "ru" -> Loc.get("lang_ru", state.language)
                                    "en" -> Loc.get("lang_en", state.language)
                                    else -> Loc.get("lang_zh", state.language)
                                }
                                SettingsChoiceRow(
                                    title = label,
                                    subtitle = subtitle,
                                    selected = state.language == value,
                                    top = index == 0,
                                    bottom = index == languages.lastIndex,
                                    scaleFactor = state.tapImpactScale,
                                    cornerRoundness = state.cornerRoundness,
                                    onClick = {
                                        tactileFeedback()
                                        viewModel.changeLanguage(value)
                                    }
                                )
                            }
                        }

                        SettingsGroup(title = Loc.get("logging", state.language)) {
                            val logLevels = listOf("debug" to "Debug", "info" to "Info", "warning" to "Warning", "error" to "Error")
                            logLevels.forEachIndexed { index, (value, label) ->
                                SettingsChoiceRow(
                                    title = label,
                                    subtitle = when (value) {
                                        "debug" -> Loc.get("log_debug_desc", state.language)
                                        "info" -> Loc.get("log_info_desc", state.language)
                                        "warning" -> Loc.get("log_warn_desc", state.language)
                                        else -> Loc.get("log_err_desc", state.language)
                                    },
                                    selected = state.logLevel == value,
                                    top = index == 0,
                                    bottom = index == logLevels.lastIndex,
                                    scaleFactor = state.tapImpactScale,
                                    cornerRoundness = state.cornerRoundness,
                                    onClick = {
                                        tactileFeedback()
                                        viewModel.changeLogLevel(value)
                                    }
                                )
                            }
                        }

                        SettingsGroup(title = Loc.get("sys_settings", state.language)) {
                            SettingsSwitchRow(
                                title = Loc.get("start_on_boot", state.language),
                                subtitle = Loc.get("start_on_boot_sub", state.language),
                                checked = state.startOnBootEnabled,
                                icon = Icons.Default.PlayArrow,
                                top = true,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeStartOnBootEnabled
                            )
                            SettingsSwitchRow(
                                title = Loc.get("confirm_remove", state.language),
                                subtitle = Loc.get("confirm_remove_sub", state.language),
                                checked = state.confirmRemoveEnabled,
                                icon = Icons.Default.Delete,
                                bottom = true,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeConfirmRemoveEnabled
                            )
                        }

                        SettingsGroup(title = Loc.get("check_servers", state.language)) {
                            SettingsHealthRow(
                                value = state.healthCheckUrl,
                                onValueChange = viewModel::changeHealthCheckUrl,
                                onTest = viewModel::testHealthCheckUrl,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                top = true,
                                bottom = true,
                                language = state.language
                            )
                        }
                    }
                    SettingsSubMenu.LOGS -> { /* handled above */ }
                    null -> {}
                }

                Spacer(
                    modifier = Modifier
                        .height(30.dp)
                        .navigationBarsPadding()
                )
            }
            } // end of if/else LOGS
        }
    }
}

@Composable
private fun SettingsHeroCard(
    status: ConnectionStatus,
    routingProfile: RoutingProfile,
    dnsServer: DnsServer,
    appRoutingMode: AppRoutingMode,
    language: String = "ru"
) {
    val color by animateColorAsState(
        targetValue = when (status) {
            ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
            ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.tertiaryContainer
            ConnectionStatus.RECONNECTING -> MaterialTheme.colorScheme.errorContainer
            ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "settingsHeroColor"
    )
    val contentColor by animateColorAsState(
        targetValue = when (status) {
            ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.onPrimaryContainer
            ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.onTertiaryContainer
            ConnectionStatus.RECONNECTING -> MaterialTheme.colorScheme.onErrorContainer
            ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.onSurface
        },
        label = "settingsHeroContentColor"
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = color
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(text = routingProfile.label, textColor = contentColor, modifier = Modifier.weight(1f))
                InfoChip(text = dnsServer.label, textColor = contentColor, modifier = Modifier.weight(1f))
            }
            InfoChip(text = appRoutingMode.label, textColor = contentColor, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SettingsThemeGrid(
    state: MainUiState,
    viewModel: MainScreenViewModel
) {
    val themes = listOf(
        Triple("System", AppTheme.SYSTEM_DYNAMIC, MaterialTheme.colorScheme.primary),
        Triple("Slate", AppTheme.MIDNIGHT_AURORA, Color(0xFFB4C6E7)),
        Triple("Desert", AppTheme.SOLAR_FLARE, Color(0xFFE2B697)),
        Triple("Sage", AppTheme.FOREST_CYBER, Color(0xFFA3B899))
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        themes.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (label, theme, accent) ->
                    ThemeButton(
                        label = label,
                        theme = theme,
                        active = state.appTheme == theme,
                        accentColor = accent,
                        onClick = { viewModel.changeTheme(theme) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
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
private fun UpdateBanner(
    updateInfo: com.perqa.byebox.core.UpdateInfo,
    language: String,
    scaleFactor: Float,
    cornerRoundness: String,
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp)),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse(updateInfo.downloadUrl)
                    }
                    context.startActivity(intent)
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Доступно обновление ${updateInfo.latestVersion}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                if (updateInfo.releaseNotes.isNotBlank()) {
                    Text(
                        text = updateInfo.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
