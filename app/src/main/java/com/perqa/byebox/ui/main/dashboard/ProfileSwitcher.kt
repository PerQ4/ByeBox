package com.perqa.byebox.ui.main.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.perqa.byebox.data.ProxyConfig
import com.perqa.byebox.data.SettingsProfileData
import com.perqa.byebox.ui.main.DnsServer
import com.perqa.byebox.ui.main.InstalledAppInfo
import com.perqa.byebox.ui.main.Loc
import com.perqa.byebox.ui.main.MainScreenViewModel
import com.perqa.byebox.ui.main.MainUiState
import com.perqa.byebox.ui.main.SegmentedSelector
import com.perqa.byebox.ui.main.SettingsActionRow
import com.perqa.byebox.ui.main.SettingsChoiceRow
import com.perqa.byebox.ui.main.SettingsGroup
import com.perqa.byebox.ui.main.SettingsRowSurface
import com.perqa.byebox.ui.main.SettingsRowText
import com.perqa.byebox.ui.main.TunStack
import com.perqa.byebox.ui.main.AppPickerSheet
import com.perqa.byebox.ui.main.rememberTactileFeedback
import androidx.compose.foundation.basicMarquee

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
                        contentDescription = Loc.get("back_cd", state.language),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.rotate(-90f)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = Loc.get("quick_switch_profiles", state.language),
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
                    val newProfile = SettingsProfileData(name = Loc.get("new_profile", state.language))
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
                    contentDescription = Loc.get("add_cd", state.language),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Text(
            text = Loc.get("profile_sort_hint", state.language),
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
                                contentDescription = Loc.get("drag_handle", state.language),
                                tint = contentColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

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
                                            text = Loc.get("active_label", state.language),
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
                                text = assignedServer?.let { "${it.countryFlag} ${it.name}" } ?: Loc.get("no_assigned_server", state.language),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = contentColor.copy(alpha = 0.6f)
                                ),
                                maxLines = 1,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .basicMarquee(iterations = Int.MAX_VALUE)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    tactileFeedback()
                                    onEditTriggered(profile)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = Loc.get("edit_cd", state.language),
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
                                        contentDescription = Loc.get("delete_cd", state.language),
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

@OptIn(ExperimentalMaterial3Api::class)
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
            title = { Text(text = Loc.get("unsaved_changes_title", state.language)) },
            text = { Text(text = Loc.get("unsaved_changes_msg", state.language)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirm = false
                        onBack()
                    }
                ) {
                    Text(Loc.get("discard_btn", state.language))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text(Loc.get("cancel_btn", state.language))
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
                    contentDescription = Loc.get("back_cd", state.language),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.rotate(-90f)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = Loc.get("profile_settings", state.language),
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
            SettingsGroup(title = Loc.get("basic_settings", state.language)) {
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
                            label = { Text(Loc.get("profile_name_label", state.language)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Loc.get("profile_name_hint", state.language),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                val selectedServerName = when (assignedConfigId) {
                    "LAST_ACTIVE" -> Loc.get("last_active_server", state.language)
                    null -> Loc.get("dont_change_server", state.language)
                    else -> assignedServer?.let { "${it.countryFlag} ${it.name} (${it.protocol})" } ?: Loc.get("use_current_server", state.language)
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
                            title = Loc.get("assigned_server", state.language),
                            subtitle = selectedServerName
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Loc.get("assigned_server_hint", state.language),
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

            SettingsGroup(title = Loc.get("dns_servers_title", state.language)) {
                SettingsChoiceRow(
                    title = Loc.get("inherit_dns", state.language),
                    subtitle = Loc.get("inherit_dns_sub", state.language),
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
                        "SYSTEM" -> Loc.get("dns_system_desc", state.language)
                        "GOOGLE" -> Loc.get("dns_google_desc", state.language)
                        "CLOUDFLARE" -> Loc.get("dns_cloudflare_desc", state.language)
                        "ADGUARD" -> Loc.get("dns_adguard_desc", state.language)
                        "CUSTOM" -> customDnsServer.orEmpty().ifBlank { Loc.get("press_to_enter_ip", state.language) }
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
                            placeholder = { Text(Loc.get("example_dns", state.language)) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            }

            SettingsGroup(title = Loc.get("tun_stack_title", state.language)) {
                SettingsChoiceRow(
                    title = Loc.get("tun_inherit", state.language),
                    subtitle = Loc.get("tun_inherit_sub", state.language),
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
                        "GVISOR" -> Loc.get("tun_gvisor_desc", state.language)
                        "SYSTEM" -> Loc.get("tun_system_desc", state.language)
                        "MIXED" -> Loc.get("tun_mixed_desc", state.language)
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

            SettingsGroup(title = Loc.get("xray_core_features", state.language)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val binaryOptions = listOf(
                        null to Loc.get("option_inherit", state.language),
                        true to Loc.get("option_enabled", state.language),
                        false to Loc.get("option_disabled", state.language)
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
                        label = Loc.get("xray_sniffing", state.language),
                        description = Loc.get("xray_sniffing_desc", state.language),
                        selected = sniffingEnabled,
                        onSelected = { sniffingEnabled = it }
                    )

                    XrayOption(
                        label = "Fragment",
                        description = Loc.get("xray_fragment_desc", state.language),
                        selected = fragmentEnabled,
                        onSelected = { fragmentEnabled = it }
                    )

                    XrayOption(
                        label = "Mux",
                        description = Loc.get("xray_mux_desc", state.language),
                        selected = muxEnabled,
                        onSelected = { muxEnabled = it }
                    )

                    XrayOption(
                        label = "Fake DNS",
                        description = Loc.get("xray_fake_dns_desc", state.language),
                        selected = fakeDnsEnabled,
                        onSelected = { fakeDnsEnabled = it }
                    )
                }
            }

            SettingsGroup(title = Loc.get("routing_title", state.language)) {
                SettingsChoiceRow(
                    title = Loc.get("routing_inherit", state.language),
                    subtitle = Loc.get("routing_inherit_sub", state.language),
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
                    title = Loc.get("routing_bypass", state.language),
                    subtitle = Loc.get("routing_bypass_sub", state.language),
                    selected = routingProfile == "BYPASS_LAN_CN_RU",
                    top = false,
                    bottom = false,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = { routingProfile = "BYPASS_LAN_CN_RU" }
                )
                SettingsChoiceRow(
                    title = Loc.get("routing_proxy_all", state.language),
                    subtitle = Loc.get("routing_proxy_all_sub", state.language),
                    selected = routingProfile == "PROXY_ALL",
                    top = false,
                    bottom = false,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = { routingProfile = "PROXY_ALL" }
                )
                SettingsChoiceRow(
                    title = Loc.get("routing_direct", state.language),
                    subtitle = Loc.get("routing_direct_sub", state.language),
                    selected = routingProfile == "DIRECT",
                    top = false,
                    bottom = true,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = { routingProfile = "DIRECT" }
                )
            }

            SettingsGroup(title = Loc.get("app_routing_title", state.language)) {
                SettingsChoiceRow(
                    title = Loc.get("app_routing_inherit", state.language),
                    subtitle = Loc.get("app_routing_inherit_sub", state.language),
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
                    title = Loc.get("app_routing_all", state.language),
                    subtitle = Loc.get("app_routing_all_sub", state.language),
                    selected = appRoutingMode == "OFF",
                    top = false,
                    bottom = false,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = { appRoutingMode = "OFF" }
                )
                SettingsChoiceRow(
                    title = Loc.get("app_routing_selected", state.language),
                    subtitle = Loc.get("app_routing_selected_sub", state.language),
                    selected = appRoutingMode == "ONLY_SELECTED",
                    top = false,
                    bottom = false,
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = { appRoutingMode = "ONLY_SELECTED" }
                )
                SettingsChoiceRow(
                    title = Loc.get("app_routing_bypass", state.language),
                    subtitle = Loc.get("app_routing_bypass_sub", state.language),
                    selected = appRoutingMode == "BYPASS_SELECTED",
                    top = false,
                    bottom = appRoutingMode == "OFF" || appRoutingMode == "INHERIT",
                    scaleFactor = state.tapImpactScale,
                    cornerRoundness = state.cornerRoundness,
                    onClick = { appRoutingMode = "BYPASS_SELECTED" }
                )
                if (appRoutingMode != "OFF" && appRoutingMode != "INHERIT") {
                    SettingsActionRow(
                        title = Loc.get("app_routing_select_title", state.language),
                        subtitle = String.format(Loc.get("app_routing_select_sub", state.language), appRoutingPackages.size),
                        button = Loc.get("app_routing_select_btn", state.language),
                        enabled = true,
                        scaleFactor = state.tapImpactScale,
                        cornerRoundness = state.cornerRoundness,
                        onClick = { showAppPicker = true },
                        bottom = true
                    )
                }
            }

            SettingsGroup(title = Loc.get("custom_rules_title", state.language)) {
                SettingsChoiceRow(
                    title = Loc.get("custom_rules_inherit_direct", state.language),
                    subtitle = Loc.get("custom_rules_inherit_direct_sub", state.language),
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
                    title = Loc.get("custom_rules_own_direct", state.language),
                    subtitle = Loc.get("custom_rules_own_direct_sub", state.language),
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

                SettingsChoiceRow(
                    title = Loc.get("custom_rules_inherit_proxy", state.language),
                    subtitle = Loc.get("custom_rules_inherit_proxy_sub", state.language),
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
                    title = Loc.get("custom_rules_own_proxy", state.language),
                    subtitle = Loc.get("custom_rules_own_proxy_sub", state.language),
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
            Text(Loc.get("save_changes", state.language), fontWeight = FontWeight.Bold)
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
            onDismiss = { showAppPicker = false },
            language = state.language
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
            onDismiss = { showServerPicker = false },
            language = state.language
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileServerPickerSheet(
    configs: List<ProxyConfig>,
    selectedConfigId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
    language: String
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
                text = Loc.get("select_server", language),
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
                            .clickable { onSelect(null) },
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
                                text = "\uD83C\uDFF3\uFE0F",
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                text = Loc.get("dont_change_server_picker", language),
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
                            .clickable { onSelect("LAST_ACTIVE") },
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
                                text = "\uD83D\uDD04",
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                text = Loc.get("last_active_server_picker", language),
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
                            .clickable { onSelect(config.id) },
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
