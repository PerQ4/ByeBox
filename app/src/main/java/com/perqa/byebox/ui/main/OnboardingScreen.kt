package com.perqa.byebox.ui.main

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.perqa.byebox.data.ProxyConfig
import com.perqa.byebox.data.SettingsProfileData
import com.perqa.byebox.theme.DarkThemeStyle
import com.perqa.byebox.ui.main.dashboard.QuickSwitchEditScreen
import kotlinx.coroutines.launch

data class OnboardingResult(
    val appMode: String,
    val language: String,
    val routingChannel: String,
    val routingProfile: String,
    val perAppChannels: Map<String, String>,
    val customDnsServer: String,
    val dnsServer: String,
    val tunStack: String,
    val cornerRoundness: String,
    val darkThemeStyle: String,
    val glassmorphicBar: Boolean,
    val subscriptionUrl: String,
    val subscriptionImported: Boolean,
    val presets: List<SettingsProfileData>
)

private enum class SetupMode { None, Quick, Advanced }

private enum class OnbStep { Welcome, Services, Channel, Routing, PerApp, Dns, Tun, Appearance, Subscription, Preset, Tiles, Done }

private sealed class SubImportState {
    object Idle : SubImportState()
    object Checking : SubImportState()
    data class Done(val count: Int) : SubImportState()
    object Failed : SubImportState()
}

@Composable
fun OnboardingScreen(
    language: String,
    cornerRoundness: String,
    configs: List<ProxyConfig>,
    uiState: MainUiState,
    onImportSubscription: suspend (String) -> ImportOutcome,
    onFinish: (OnboardingResult) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var setupMode by remember { mutableStateOf(SetupMode.None) }
    var lang by remember { mutableStateOf(language) }
    var services by remember { mutableStateOf(setOf("vpn", "tgws")) }
    var routingChannel by remember { mutableStateOf(RouteChannel.VPN.value) }
    var routingProfile by remember { mutableStateOf(RoutingProfile.BYPASS_LAN_CN_RU.name) }
    var perAppChannels by remember { mutableStateOf(mapOf<String, String>()) }
    var perAppEnabled by remember { mutableStateOf(false) }
    var customDns by remember { mutableStateOf("") }
    var dnsServer by remember { mutableStateOf(DnsServer.SYSTEM.name) }
    var tunStack by remember { mutableStateOf(TunStack.GVISOR.name) }
    var roundness by remember { mutableStateOf(cornerRoundness) }
    var darkStyle by remember { mutableStateOf(DarkThemeStyle.STANDARD.name) }
    var glassBar by remember { mutableStateOf(true) }
    var subscriptionUrl by remember { mutableStateOf("") }
    var subscriptionImported by remember { mutableStateOf(false) }
    var subState by remember { mutableStateOf<SubImportState>(SubImportState.Idle) }
    val scope = rememberCoroutineScope()
    var presetDrafts by remember { mutableStateOf(listOf<SettingsProfileData>()) }
    var editingPreset by remember { mutableStateOf<SettingsProfileData?>(null) }
    var showAppPicker by remember { mutableStateOf(false) }

    val isExpressive = roundness == "expressive"

    val hasVpn = services.contains("vpn")
    val hasTgws = services.contains("tgws")
    val isAdvanced = setupMode == SetupMode.Advanced

    val steps = buildList<OnbStep> {
        add(OnbStep.Welcome)
        when (setupMode) {
            SetupMode.None -> {}
            SetupMode.Quick -> {
                add(OnbStep.Services)
                if (hasVpn) {
                    add(OnbStep.Channel)
                    add(OnbStep.PerApp)
                }
                add(OnbStep.Tiles)
                add(OnbStep.Done)
            }
            SetupMode.Advanced -> {
                add(OnbStep.Services)
                if (hasVpn) {
                    add(OnbStep.Channel)
                    add(OnbStep.Routing)
                    add(OnbStep.PerApp)
                    add(OnbStep.Dns)
                    add(OnbStep.Tun)
                }
                add(OnbStep.Appearance)
                add(OnbStep.Subscription)
                add(OnbStep.Preset)
                add(OnbStep.Tiles)
                add(OnbStep.Done)
            }
        }
    }
    val safeStep = step.coerceIn(0, (steps.size - 1).coerceAtLeast(0))
    val currentStep = steps.getOrNull(safeStep) ?: OnbStep.Welcome

    fun derivedAppMode(): String = when {
        hasVpn && hasTgws -> "both"
        hasVpn -> "vpn"
        hasTgws -> "tgws"
        else -> "both"
    }

    fun finish() {
        onFinish(
            OnboardingResult(
                appMode = derivedAppMode(),
                language = lang,
                routingChannel = routingChannel,
                routingProfile = routingProfile,
                perAppChannels = perAppChannels,
                customDnsServer = customDns.trim(),
                dnsServer = dnsServer,
                tunStack = tunStack,
                cornerRoundness = roundness,
                darkThemeStyle = darkStyle,
                glassmorphicBar = glassBar,
                subscriptionUrl = subscriptionUrl,
                subscriptionImported = subscriptionImported,
                presets = presetDrafts
            )
        )
    }

    val ctx = LocalContext.current
    val pm = ctx.packageManager
    val apps = remember {
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map { InstalledAppInfo(pm.getApplicationLabel(it).toString(), it.packageName, false) }
            .sortedBy { it.label.lowercase() }
    }

    if (showAppPicker) {
        RoutingAppPickerSheet(
            apps = apps,
            channels = perAppChannels.mapValues { RouteChannel.from(it.value) },
            onSave = { newMap -> perAppChannels = newMap.mapValues { it.value.value }; showAppPicker = false },
            onDismiss = { showAppPicker = false },
            language = lang
        )
    }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val editing = editingPreset
        if (editing != null) {
            QuickSwitchEditScreen(
                profile = editing,
                configs = configs,
                apps = apps,
                state = uiState,
                onBack = { editingPreset = null },
                onSave = { saved ->
                    if (saved.name.isNotBlank()) {
                        val idx = presetDrafts.indexOfFirst { it.id == saved.id }
                        presetDrafts = if (idx >= 0) {
                            presetDrafts.mapIndexed { i, p -> if (i == idx) saved else p }
                        } else {
                            presetDrafts + saved
                        }
                        editingPreset = null
                    }
                }
            )
            return@Dialog
        }
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    repeat(steps.size) { index ->
                        val active = index == safeStep
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (active) 28.dp else 10.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Crossfade(targetState = currentStep, label = "onboarding_step") { stepEnum ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            when (stepEnum) {
                                OnbStep.Welcome -> WelcomeStep(
                                    language = lang,
                                    isExpressive = isExpressive,
                                    onDefault = { finish() },
                                    onQuick = { setupMode = SetupMode.Quick; step = 1 },
                                    onAdvanced = { setupMode = SetupMode.Advanced; step = 1 },
                                    onLanguageChange = { lang = it }
                                )
                                OnbStep.Services -> ServicesStep(lang, isExpressive, services) { services = it }
                                OnbStep.Channel -> ChannelStep(lang, isExpressive, routingChannel) { routingChannel = it }
                                OnbStep.Routing -> RoutingProfileStep(lang, isExpressive, routingProfile) { routingProfile = it }
                                OnbStep.PerApp -> PerAppStep(
                                    language = lang,
                                    isExpressive = isExpressive,
                                    enabled = perAppEnabled,
                                    channels = perAppChannels.mapValues { RouteChannel.from(it.value) },
                                    apps = apps,
                                    onSelect = { enabled ->
                                        perAppEnabled = enabled
                                        if (!enabled) perAppChannels = emptyMap()
                                    },
                                    onOpenPicker = { showAppPicker = true }
                                )
                                OnbStep.Dns -> DnsStep(
                                    language = lang,
                                    isExpressive = isExpressive,
                                    selected = dnsServer,
                                    customDns = customDns,
                                    onSelect = { dnsServer = it },
                                    onCustomDnsChange = { customDns = it }
                                )
                                OnbStep.Tun -> TunStep(lang, isExpressive, tunStack) { tunStack = it }
                                OnbStep.Appearance -> AppearanceStep(
                                    language = lang,
                                    isExpressive = isExpressive,
                                    roundness = roundness,
                                    darkStyle = darkStyle,
                                    glassBar = glassBar,
                                    onRoundnessChange = { roundness = it },
                                    onDarkStyleChange = { darkStyle = it.name },
                                    onGlassChange = { glassBar = it }
                                )
                                OnbStep.Subscription -> SubscriptionStep(
                                    language = lang,
                                    isExpressive = isExpressive,
                                    url = subscriptionUrl,
                                    onUrlChange = { newUrl ->
                                        subscriptionUrl = newUrl
                                        if (subState !is SubImportState.Idle && subState !is SubImportState.Checking) {
                                            subState = SubImportState.Idle
                                        }
                                    },
                                    state = subState,
                                    onImport = {
                                        val trimmed = subscriptionUrl.trim()
                                        if (trimmed.isNotBlank()) {
                                            subState = SubImportState.Checking
                                            scope.launch {
                                                val outcome = onImportSubscription(trimmed)
                                                if (outcome.count > 0) {
                                                    subState = SubImportState.Done(outcome.count)
                                                    subscriptionImported = true
                                                } else {
                                                    subState = SubImportState.Failed
                                                }
                                            }
                                        }
                                    }
                                )
                                OnbStep.Preset -> PresetStep(
                                    language = lang,
                                    isExpressive = isExpressive,
                                    drafts = presetDrafts,
                                    configs = configs,
                                    onCreate = {
                                        editingPreset = SettingsProfileData(
                                            name = "",
                                            assignedConfigId = "LAST_ACTIVE"
                                        )
                                    },
                                    onEdit = { preset -> editingPreset = preset },
                                    onDelete = { id ->
                                        presetDrafts = presetDrafts.filter { it.id != id }
                                    }
                                )
                                OnbStep.Tiles -> QuickSettingsTilesPage(
                                    scaleFactor = 1f,
                                    cornerRoundness = if (isExpressive) "expressive" else "18dp",
                                    language = lang,
                                    showTgws = hasTgws
                                )
                                OnbStep.Done -> SummaryStep(
                                    language = lang,
                                    isExpressive = isExpressive,
                                    advanced = isAdvanced,
                                    hasVpn = hasVpn,
                                    languages = lang,
                                    appMode = derivedAppMode(),
                                    channel = routingChannel,
                                    profile = remember(routingProfile) { RoutingProfile.entries.firstOrNull { it.name == routingProfile } ?: RoutingProfile.BYPASS_LAN_CN_RU },
                                    appCount = perAppChannels.size,
                                    customDns = customDns,
                                    dns = remember(dnsServer) { DnsServer.entries.firstOrNull { it.name == dnsServer } ?: DnsServer.SYSTEM },
                                    tun = remember(tunStack) { TunStack.entries.firstOrNull { it.name == tunStack } ?: TunStack.GVISOR },
                                    roundness = roundness,
                                    dark = remember(darkStyle) { DarkThemeStyle.entries.firstOrNull { it.name == darkStyle } ?: DarkThemeStyle.STANDARD },
                                    glass = glassBar,
                                    subscription = subscriptionUrl,
                                    presetCount = presetDrafts.size
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (currentStep != OnbStep.Welcome) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (safeStep > 0) {
                            Button(
                                onClick = { step = (safeStep - 1).coerceAtLeast(0) },
                                modifier = Modifier.height(52.dp),
                                shape = RoundedCornerShape(if (isExpressive) 26.dp else 14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(Loc.get("onboarding_back", lang), fontWeight = FontWeight.Bold)
                            }
                        }
                        Button(
                            onClick = {
                                if (safeStep < steps.size - 1) {
                                    step = (safeStep + 1).coerceAtMost(steps.size - 1)
                                } else {
                                    finish()
                                }
                            },
                            modifier = Modifier
                                .height(52.dp)
                                .weight(1f),
                            shape = RoundedCornerShape(if (isExpressive) 26.dp else 14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                if (safeStep < steps.size - 1) Loc.get("onboarding_next", lang)
                                else Loc.get("onboarding_finish", lang),
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun stepHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}

@Composable
private fun ChoiceTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    isExpressive: Boolean,
    onClick: () -> Unit
) {
    ExpressiveTile(
        title = title,
        subtitle = subtitle,
        icon = icon,
        cornerRoundness = if (isExpressive) "expressive" else "18dp",
        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        onClick = onClick
    )
}

@Composable
private fun LanguageChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(if (selected) 24.dp else 22.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                maxLines = 1,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
private fun WelcomeStep(
    language: String,
    isExpressive: Boolean,
    onDefault: () -> Unit,
    onQuick: () -> Unit,
    onAdvanced: () -> Unit,
    onLanguageChange: (String) -> Unit
) {
    val tilesRoundness = if (isExpressive) "expressive" else "18dp"
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(modifier = Modifier.height(6.dp))
        stepHeader(
            Loc.get("onboarding_title", language),
            Loc.get("onboarding_main_q", language)
        )
        ExpressiveTile(
            title = Loc.get("onboarding_default", language),
            subtitle = Loc.get("onboarding_default_sub", language),
            icon = Icons.Default.CheckCircle,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            cornerRoundness = tilesRoundness,
            onClick = onDefault
        )
        ExpressiveTile(
            title = Loc.get("onboarding_quick_setup", language),
            subtitle = Loc.get("onboarding_quick_sub", language),
            icon = Icons.Default.AutoAwesome,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            cornerRoundness = tilesRoundness,
            onClick = onQuick
        )
        ExpressiveTile(
            title = Loc.get("onboarding_advanced", language),
            subtitle = Loc.get("onboarding_advanced_sub", language),
            icon = Icons.Default.Tune,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            cornerRoundness = tilesRoundness,
            onClick = onAdvanced
        )

        Spacer(modifier = Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                Loc.get("onboarding_language_title", language),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(start = 4.dp)
            )
            val options = listOf(
                "system" to Loc.get("onboarding_lbl_auto", language),
                "ru" to Loc.get("lang_ru", language),
                "en" to Loc.get("lang_en", language),
                "zh" to Loc.get("lang_zh", language)
            )
            options.chunked(2).forEach { rowOptions ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowOptions.forEach { (code, label) ->
                        LanguageChip(
                            label = label,
                            selected = language == code,
                            modifier = Modifier.weight(1f),
                            onClick = { onLanguageChange(code) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServicesStep(language: String, isExpressive: Boolean, selected: Set<String>, onSelect: (Set<String>) -> Unit) {
    val toggle = { key: String ->
        val set = if (selected.contains(key)) selected - key else selected + key
        onSelect(set)
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        stepHeader(
            Loc.get("onboarding_services_title", language),
            Loc.get("onboarding_services_q", language)
        )
        Spacer(modifier = Modifier.height(2.dp))
        ChoiceTile(Loc.get("service_vpn", language), Loc.get("service_vpn_sub", language), Icons.AutoMirrored.Filled.CompareArrows, selected.contains("vpn"), isExpressive) { toggle("vpn") }
        ChoiceTile(Loc.get("service_tgws", language), Loc.get("service_tgws_sub", language), Icons.Default.Send, selected.contains("tgws"), isExpressive) { toggle("tgws") }
    }
}

@Composable
private fun ChannelStep(language: String, isExpressive: Boolean, channel: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        stepHeader(
            Loc.get("onboarding_channel_title", language),
            Loc.get("onboarding_channel_q", language)
        )
        Spacer(modifier = Modifier.height(2.dp))
        ChoiceTile(
            Loc.get("routing_channel_vpn", language),
            Loc.get("onboarding_default_vpn", language),
            Icons.AutoMirrored.Filled.CompareArrows,
            channel == RouteChannel.VPN.value,
            isExpressive
        ) { onSelect(RouteChannel.VPN.value) }
        ChoiceTile(
            Loc.get("routing_channel_direct", language),
            Loc.get("onboarding_default_direct", language),
            Icons.Default.OpenInBrowser,
            channel == RouteChannel.DIRECT.value,
            isExpressive
        ) { onSelect(RouteChannel.DIRECT.value) }
    }
}

@Composable
private fun RoutingProfileStep(language: String, isExpressive: Boolean, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        stepHeader(
            Loc.get("onboarding_routing_title", language),
            Loc.get("onboarding_routing_q", language)
        )
        Spacer(modifier = Modifier.height(2.dp))
        RoutingProfile.entries.forEach { profile ->
            val icon = when (profile) {
                RoutingProfile.BYPASS_LAN_CN_RU -> Icons.Default.AltRoute
                RoutingProfile.PROXY_ALL -> Icons.Default.Language
                RoutingProfile.DIRECT -> Icons.Default.Navigation
            }
            val subtitle = when (profile) {
                RoutingProfile.BYPASS_LAN_CN_RU -> Loc.get("routing_bypass_desc", language)
                RoutingProfile.PROXY_ALL -> Loc.get("routing_proxy_all_desc", language)
                RoutingProfile.DIRECT -> Loc.get("routing_direct_desc", language)
            }
            ChoiceTile(
                profile.localizedName(language),
                subtitle,
                icon,
                selected == profile.name,
                isExpressive
            ) { onSelect(profile.name) }
        }
    }
}

@Composable
private fun PerAppStep(
    language: String,
    isExpressive: Boolean,
    enabled: Boolean,
    channels: Map<String, RouteChannel>,
    apps: List<InstalledAppInfo>,
    onSelect: (Boolean) -> Unit,
    onOpenPicker: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        stepHeader(
            Loc.get("onboarding_perapp_ask_title", language),
            Loc.get("onboarding_perapp_ask_q", language)
        )
        Spacer(modifier = Modifier.height(2.dp))
        ChoiceTile(Loc.get("perapp_yes", language), "", Icons.Default.CheckCircle, enabled, isExpressive) { onSelect(true) }
        ChoiceTile(Loc.get("perapp_no", language), "", Icons.Default.OpenInBrowser, !enabled, isExpressive) { onSelect(false) }
        if (enabled) {
            Spacer(modifier = Modifier.height(2.dp))
            SelectedAppsPickerSection(language, isExpressive, channels, apps, onOpenPicker)
        }
    }
}

@Composable
private fun SelectedAppsPickerSection(
    language: String,
    isExpressive: Boolean,
    channels: Map<String, RouteChannel>,
    apps: List<InstalledAppInfo>,
    onOpenPicker: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SettingsActionRow(
            title = Loc.get("selected_apps", language),
            subtitle = String.format(Loc.get("packages_selected", language), channels.size),
            button = Loc.get("select_btn", language),
            enabled = true,
            cornerRoundness = if (isExpressive) "expressive" else "18dp",
            onClick = onOpenPicker
        )
        val selectedApps = apps.filter { channels.containsKey(it.packageName) }
            .map { RouteApp(it.packageName, it.label) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            selectedApps.forEach { app ->
                val ch = channels[app.pkg] ?: RouteChannel.DEFAULT
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val painter = rememberAppIconPainter(app.pkg)
                    if (painter != null) {
                        Image(
                            painter,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(app.label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text(app.pkg, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(
                            routeChannelLabel(ch, language),
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DnsStep(
    language: String,
    isExpressive: Boolean,
    selected: String,
    customDns: String,
    onSelect: (String) -> Unit,
    onCustomDnsChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        stepHeader(
            Loc.get("onboarding_dns_title", language),
            Loc.get("onboarding_dns_q", language)
        )
        Spacer(modifier = Modifier.height(2.dp))
        DnsServer.entries.forEach { dns ->
            val icon = when (dns) {
                DnsServer.SYSTEM -> Icons.Default.Dns
                DnsServer.CLOUDFLARE -> Icons.Default.Cloud
                DnsServer.GOOGLE -> Icons.Default.Language
                DnsServer.ADGUARD -> Icons.Default.Shield
                DnsServer.CUSTOM -> Icons.Default.Settings
            }
            val subtitle = if (dns == DnsServer.CUSTOM) {
                customDns.ifBlank { Loc.get("press_to_enter_ip", language) }
            } else {
                dns.localizedAddress(language)
            }
            ChoiceTile(
                dns.localizedName(language),
                subtitle,
                icon,
                selected == dns.name,
                isExpressive
            ) { onSelect(dns.name) }
        }
        if (selected == DnsServer.CUSTOM.name) {
            OutlinedTextField(
                value = customDns,
                onValueChange = onCustomDnsChange,
                placeholder = { Text(Loc.get("example_dns", language)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(if (isExpressive) 24.dp else 14.dp)
            )
        }
    }
}

@Composable
private fun TunStep(language: String, isExpressive: Boolean, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        stepHeader(
            Loc.get("onboarding_tun_title", language),
            Loc.get("onboarding_tun_q", language)
        )
        Spacer(modifier = Modifier.height(2.dp))
        TunStack.entries.forEach { stack ->
            val icon = when (stack) {
                TunStack.GVISOR -> Icons.Default.Shield
                TunStack.SYSTEM -> Icons.Default.Build
            }
            ChoiceTile(
                stack.localizedName(language),
                stack.localizedDescription(language),
                icon,
                selected == stack.name,
                isExpressive
            ) { onSelect(stack.name) }
        }
    }
}

@Composable
private fun AppearanceStep(
    language: String,
    isExpressive: Boolean,
    roundness: String,
    darkStyle: String,
    glassBar: Boolean,
    onRoundnessChange: (String) -> Unit,
    onDarkStyleChange: (DarkThemeStyle) -> Unit,
    onGlassChange: (Boolean) -> Unit
) {
    val rowRoundness = if (isExpressive) "expressive" else "18dp"
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        stepHeader(
            Loc.get("onboarding_appearance_title", language),
            Loc.get("onboarding_appearance_q", language)
        )
        Text(
            Loc.get("roundness_title", language),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        ChoiceTile(
            Loc.get("corner_std", language),
            Loc.get("corner_std_sub", language),
            Icons.Default.CropSquare,
            roundness == "standard",
            isExpressive
        ) { onRoundnessChange("standard") }
        ChoiceTile(
            Loc.get("corner_expr", language),
            Loc.get("corner_expr_sub", language),
            Icons.Default.Category,
            roundness == "expressive",
            isExpressive
        ) { onRoundnessChange("expressive") }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            Loc.get("dark_theme_style_title", language),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        val darkLabel = remember(darkStyle) { DarkThemeStyle.entries.firstOrNull { it.name == darkStyle } ?: DarkThemeStyle.STANDARD }
        DarkThemeStyle.entries.forEach { style ->
            ChoiceTile(
                when (style) {
                    DarkThemeStyle.STANDARD -> Loc.get("dark_standard", language)
                    DarkThemeStyle.DEEP_SLATE -> Loc.get("dark_deep_slate", language)
                    DarkThemeStyle.MIDNIGHT_NAVY -> Loc.get("dark_midnight_navy", language)
                    DarkThemeStyle.PURE_BLACK -> Loc.get("dark_pure_black", language)
                },
                when (style) {
                    DarkThemeStyle.STANDARD -> Loc.get("dark_standard_sub", language)
                    DarkThemeStyle.DEEP_SLATE -> Loc.get("dark_deep_slate_sub", language)
                    DarkThemeStyle.MIDNIGHT_NAVY -> Loc.get("dark_midnight_navy_sub", language)
                    DarkThemeStyle.PURE_BLACK -> Loc.get("dark_pure_black_sub", language)
                },
                Icons.Default.CheckCircle,
                darkLabel == style,
                isExpressive
            ) { onDarkStyleChange(style) }
        }

        Spacer(modifier = Modifier.height(6.dp))

        SettingsSwitchRow(
            title = Loc.get("glass_bar", language),
            subtitle = Loc.get("glass_bar_sub", language),
            checked = glassBar,
            icon = Icons.Default.Share,
            top = true,
            bottom = true,
            cornerRoundness = rowRoundness,
            onCheckedChange = onGlassChange
        )
    }
}

@Composable
private fun SubscriptionStep(
    language: String,
    isExpressive: Boolean,
    url: String,
    onUrlChange: (String) -> Unit,
    state: SubImportState,
    onImport: () -> Unit
) {
    val fieldRoundness = if (isExpressive) 24.dp else 14.dp
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        stepHeader(
            Loc.get("onboarding_sub_title", language),
            Loc.get("onboarding_sub_q", language)
        )
        Spacer(modifier = Modifier.height(2.dp))
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            placeholder = { Text(Loc.get("onboarding_sub_placeholder", language)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(fieldRoundness)
        )
        Button(
            onClick = onImport,
            enabled = url.isNotBlank() && state != SubImportState.Checking,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(if (isExpressive) 26.dp else 14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (state == SubImportState.Checking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                if (state == SubImportState.Checking) Loc.get("onboarding_sub_importing", language)
                else Loc.get("onboarding_sub_import_btn", language),
                fontWeight = FontWeight.Black
            )
        }
        when (state) {
            is SubImportState.Done -> Text(
                String.format(Loc.get("onboarding_sub_success", language), state.count),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            SubImportState.Failed -> Text(
                Loc.get("onboarding_sub_fail", language),
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.error)
            )
            SubImportState.Idle, SubImportState.Checking -> {}
        }
    }
}

@Composable
private fun PresetStep(
    language: String,
    isExpressive: Boolean,
    drafts: List<SettingsProfileData>,
    configs: List<ProxyConfig>,
    onCreate: () -> Unit,
    onEdit: (SettingsProfileData) -> Unit,
    onDelete: (String) -> Unit
) {
    val rowRoundness = if (isExpressive) "expressive" else "18dp"
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        stepHeader(
            Loc.get("onboarding_preset_title", language),
            Loc.get("onboarding_preset_q", language)
        )
        Spacer(modifier = Modifier.height(2.dp))

        if (drafts.isEmpty()) {
            Text(
                Loc.get("onboarding_preset_empty", language),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        } else {
            drafts.forEach { draft ->
                val serverText = when (draft.assignedConfigId) {
                    "FASTEST" -> "\u26A1 ${Loc.get("fastest_server", language)}"
                    "LAST_ACTIVE" -> "\uD83D\uDD04 ${Loc.get("last_active_server", language)}"
                    null -> Loc.get("dont_change_server", language)
                    else -> configs.find { it.id == draft.assignedConfigId }?.name
                        ?: Loc.get("no_assigned_server", language)
                }
                SettingsRowSurface(cornerRoundness = rowRoundness) {
                    SettingsRowText(
                        title = draft.name,
                        subtitle = serverText,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onEdit(draft) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = Loc.get("edit_cd", language),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { onDelete(draft.id) }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = Loc.get("delete_cd", language),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Button(
            onClick = onCreate,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(if (isExpressive) 26.dp else 14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text(Loc.get("onboarding_preset_add", language), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SummaryStep(
    language: String,
    isExpressive: Boolean,
    advanced: Boolean,
    hasVpn: Boolean,
    languages: String,
    appMode: String,
    channel: String,
    profile: RoutingProfile,
    appCount: Int,
    customDns: String,
    dns: DnsServer,
    tun: TunStack,
    roundness: String,
    dark: DarkThemeStyle,
    glass: Boolean,
    subscription: String,
    presetCount: Int
) {
    val rowRoundness = if (isExpressive) "expressive" else "18dp"

    fun languageValue(): String = when (languages) {
        "system" -> Loc.get("onboarding_lbl_auto", language)
        "ru" -> Loc.get("lang_ru", language)
        "en" -> Loc.get("lang_en", language)
        else -> Loc.get("lang_zh", language)
    }

    fun purposeValue(): String = when (appMode) {
        "vpn" -> Loc.get("mode_vpn", language)
        "tgws" -> Loc.get("mode_tgws", language)
        else -> Loc.get("mode_both", language)
    }

    fun channelValue(): String = when (channel) {
        RouteChannel.DIRECT.value -> Loc.get("channel_direct", language)
        else -> Loc.get("channel_vpn", language)
    }

    val rows = buildList<Pair<String, String>> {
        add(Loc.get("onboarding_lbl_language", language) to languageValue())
        add(Loc.get("onboarding_lbl_purpose", language) to purposeValue())
        if (hasVpn) {
            add(Loc.get("onboarding_channel_title", language) to channelValue())
            if (advanced) {
                add(Loc.get("onboarding_lbl_routing_profile", language) to profile.localizedName(language))
            }
            val appsValue = if (appCount == 0) {
                Loc.get("perapp_no", language)
            } else {
                String.format(Loc.get("packages_selected", language), appCount)
            }
            add(Loc.get("onboarding_lbl_apps", language) to appsValue)
        }
        if (advanced) {
            if (hasVpn) {
                add(Loc.get("onboarding_dns_title", language) to
                    (if (dns == DnsServer.CUSTOM) customDns.ifBlank { dns.localizedName(language) } else dns.localizedName(language)))
                add(Loc.get("onboarding_tun_title", language) to tun.localizedName(language))
            }
            add(Loc.get("roundness_title", language) to
                (if (roundness == "standard") Loc.get("corner_std", language) else Loc.get("corner_expr", language)))
            add(Loc.get("onboarding_lbl_dark", language) to when (dark) {
                DarkThemeStyle.STANDARD -> Loc.get("dark_standard", language)
                DarkThemeStyle.DEEP_SLATE -> Loc.get("dark_deep_slate", language)
                DarkThemeStyle.MIDNIGHT_NAVY -> Loc.get("dark_midnight_navy", language)
                DarkThemeStyle.PURE_BLACK -> Loc.get("dark_pure_black", language)
            })
            add(Loc.get("onboarding_lbl_glass", language) to
                (if (glass) Loc.get("onboarding_value_on", language) else Loc.get("onboarding_value_off", language)))
        }
        if (subscription.isNotBlank()) {
            add(Loc.get("onboarding_lbl_subscription", language) to subscription.trim())
        }
        if (presetCount > 0) {
            add(Loc.get("onboarding_lbl_presets", language) to "$presetCount")
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        stepHeader(
            Loc.get("onboarding_summary_title", language),
            Loc.get("onboarding_summary_q", language)
        )
        Spacer(modifier = Modifier.height(2.dp))
        rows.forEach { (label, value) ->
            SettingsRowSurface(cornerRoundness = rowRoundness) {
                SettingsRowText(title = label, subtitle = "", modifier = Modifier.weight(1f))
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}