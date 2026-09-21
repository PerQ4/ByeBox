package com.perqa.byebox.ui.main

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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amurcanov.tgwsproxy.ProxyController
import com.amurcanov.tgwsproxy.ProxyService
import com.amurcanov.tgwsproxy.R as TgwsR
import com.amurcanov.tgwsproxy.SettingsStore
import kotlinx.coroutines.launch

private fun generateRandomSecret(): String {
    val bytes = ByteArray(16)
    java.security.SecureRandom().nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
}

private val tgClientPackages = listOf(
    "org.telegram.messenger",
    "com.radolyn.ayugram",
    "com.exteragram.messenger",
    "org.telegram.plus",
    "ir.ilmili.telegraph",
    "org.telegram.BifToGram",
    "tw.nekomimi.nekogram",
    "xyz.nextalone.nagram",
    "uz.unnarsx.cherrygram",
    "org.telegram.mdgram",
    "org.forkclient.messenger.beta",
    "app.nicegram",
    "top.qwq2333.nullgram",
    "com.iMe.android",
    "ru.dahl.messenger",
    "com.scriptsaz.litegram",
    "org.thunderdog.challegram"
)

@Composable
private fun TgwsPortRow(
    value: String,
    onValueChange: (String) -> Unit,
    top: Boolean = false,
    bottom: Boolean = false,
    scaleFactor: Float,
    cornerRoundness: String,
    language: String
) {
    SettingsRowSurface(top = top, bottom = bottom, scaleFactor = scaleFactor, cornerRoundness = cornerRoundness) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(TgwsR.string.port),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = Loc.get("tgws_port_help", language),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.filter { c -> c.isDigit() }.take(5)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(120.dp),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun TgwsTextFieldRow(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    help: String = "",
    top: Boolean = false,
    bottom: Boolean = false,
    scaleFactor: Float,
    cornerRoundness: String
) {
    SettingsRowSurface(top = top, bottom = bottom, scaleFactor = scaleFactor, cornerRoundness = cornerRoundness) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (help.isNotEmpty()) {
                Text(
                    text = help,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                )
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier.width(200.dp),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun TgwsPoolRow(
    value: Int,
    onValueChange: (Int) -> Unit,
    top: Boolean = false,
    bottom: Boolean = false,
    scaleFactor: Float,
    cornerRoundness: String
) {
    SettingsRowSurface(top = top, bottom = bottom, scaleFactor = scaleFactor, cornerRoundness = cornerRoundness) {
        Text(
            text = stringResource(TgwsR.string.pool_short, value),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = { onValueChange((value - 1).coerceAtLeast(1)) }) {
                Text("−", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { onValueChange((value + 1).coerceAtMost(16)) }) {
                Text("+", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun TgwsSecretRow(
    value: String,
    onValueChange: (String) -> Unit,
    top: Boolean = false,
    bottom: Boolean = false,
    scaleFactor: Float,
    cornerRoundness: String
) {
    SettingsRowSurface(top = top, bottom = bottom, scaleFactor = scaleFactor, cornerRoundness = cornerRoundness) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(TgwsR.string.secret_key),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(TgwsR.string.help_secret_key_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
            )
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun TgwsClientRow(
    packageName: String,
    label: String,
    onClick: () -> Unit,
    top: Boolean = false,
    bottom: Boolean = false,
    scaleFactor: Float,
    cornerRoundness: String
) {
    SettingsRowSurface(top = top, bottom = bottom, scaleFactor = scaleFactor, cornerRoundness = cornerRoundness, onClick = onClick) {
        val iconPainter = rememberAppIconPainter(packageName)
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (iconPainter != null) {
                Image(
                    painter = iconPainter,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp))
                )
            } else {
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label.take(1).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = packageName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TgWsProxySettingsContent(
    scaleFactor: Float = 0.90f,
    cornerRoundness: String = "expressive",
    language: String = "ru",
    viewModel: MainScreenViewModel? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }

    val isDcAuto by settingsStore.isDcAuto.collectAsStateWithLifecycle(initialValue = true)
    val cfEnabled by settingsStore.cfproxyEnabled.collectAsStateWithLifecycle(initialValue = true)
    val experimental by settingsStore.isExperimentalMode.collectAsStateWithLifecycle(initialValue = false)
    val autostart by settingsStore.autoStartOnBoot.collectAsStateWithLifecycle(initialValue = false)
    val port by settingsStore.port.collectAsStateWithLifecycle(initialValue = "1443")
    val poolSize by settingsStore.poolSize.collectAsStateWithLifecycle(initialValue = 4)
    val secretKey by settingsStore.secretKey.collectAsStateWithLifecycle(initialValue = "")
    val bypassEnabled by settingsStore.bypassClientEnabled.collectAsStateWithLifecycle(initialValue = false)
    val bypassPkg by settingsStore.bypassClientPackage.collectAsStateWithLifecycle(initialValue = "")

    var showPicker by remember { mutableStateOf(false) }
    var poolExpanded by remember { mutableStateOf(false) }
    val clientApps = remember(viewModel) {
        (viewModel?.installedApps ?: emptyList()).filter { it.packageName in tgClientPackages }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingsGroup(title = stringResource(TgwsR.string.connection)) {
            SettingsSwitchRow(
                title = stringResource(TgwsR.string.autostart),
                subtitle = stringResource(TgwsR.string.help_autostart_text),
                checked = autostart,
                icon = Icons.Default.PowerSettingsNew,
                top = true,
                scaleFactor = scaleFactor,
                cornerRoundness = cornerRoundness,
                onCheckedChange = { scope.launch { settingsStore.saveAutoStartOnBoot(it) } }
            )
            TgwsPortRow(
                value = port,
                onValueChange = { scope.launch { settingsStore.savePort(it) } },
                top = false,
                bottom = false,
                scaleFactor = scaleFactor,
                cornerRoundness = cornerRoundness,
                language = language
            )
            TgwsPoolRow(
                value = poolSize,
                onValueChange = { scope.launch { settingsStore.savePoolSize(it) } },
                scaleFactor = scaleFactor,
                cornerRoundness = cornerRoundness
            )
            SettingsRowSurface(onClick = { poolExpanded = !poolExpanded }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (poolExpanded) Loc.get("tgws_hide", language) else Loc.get("tgws_more", language),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (poolExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            AnimatedVisibility(visible = poolExpanded) {
                Text(
                    text = stringResource(TgwsR.string.help_ws_pool_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 2.dp)
                )
            }
            SettingsSwitchRow(
                title = stringResource(TgwsR.string.auto_cf_enabled),
                subtitle = stringResource(TgwsR.string.help_cloudflare_text),
                checked = cfEnabled,
                icon = Icons.Default.Cloud,
                scaleFactor = scaleFactor,
                cornerRoundness = cornerRoundness,
                onCheckedChange = { scope.launch { settingsStore.saveCfproxyEnabled(it) } }
            )
            SettingsSwitchRow(
                title = stringResource(TgwsR.string.help_auto_dc_title),
                subtitle = stringResource(TgwsR.string.help_auto_dc_text),
                checked = isDcAuto,
                icon = Icons.Default.Language,
                scaleFactor = scaleFactor,
                cornerRoundness = cornerRoundness,
                onCheckedChange = { scope.launch { settingsStore.saveIsDcAuto(it) } }
            )
            SettingsSwitchRow(
                title = stringResource(TgwsR.string.experimental_mode),
                subtitle = stringResource(TgwsR.string.help_experimental_text),
                checked = experimental,
                icon = Icons.Default.Build,
                bottom = true,
                scaleFactor = scaleFactor,
                cornerRoundness = cornerRoundness,
                onCheckedChange = { scope.launch { settingsStore.saveIsExperimentalMode(it) } }
                )
            }

        if (experimental) {
            SettingsGroup(title = Loc.get("tgws_dc_title", language)) {
                val dcNames = listOf("dc1", "dc2", "dc3", "dc4", "dc5", "dc203")
                dcNames.forEachIndexed { i, name ->
                    val v by settingsStore.dcByName(name).collectAsStateWithLifecycle(initialValue = "")
                    TgwsTextFieldRow(
                        title = name.uppercase(),
                        value = v,
                        onValueChange = { scope.launch { settingsStore.saveDc(name, it.trim()) } },
                        top = i == 0,
                        bottom = i == dcNames.lastIndex,
                        scaleFactor = scaleFactor,
                        cornerRoundness = cornerRoundness
                    )
                }
            }
            SettingsGroup(title = Loc.get("tgws_bind_ip", language)) {
                val bindIpVal by settingsStore.bindIp.collectAsStateWithLifecycle(initialValue = "127.0.0.1")
                TgwsTextFieldRow(
                    title = Loc.get("tgws_bind_ip", language),
                    value = bindIpVal,
                    onValueChange = { scope.launch { settingsStore.saveBindIp(it.trim()) } },
                    top = true,
                    bottom = true,
                    scaleFactor = scaleFactor,
                    cornerRoundness = cornerRoundness
                )
            }
        }


        SettingsGroup(title = Loc.get("tgws_bypass", language)) {
            val selectedClient = clientApps.firstOrNull { it.packageName == bypassPkg }
            SettingsSwitchRow(
                title = Loc.get("tgws_bypass", language),
                subtitle = Loc.get("tgws_bypass_sub", language),
                checked = bypassEnabled,
                icon = Icons.Default.Language,
                top = true,
                bottom = !bypassEnabled,
                scaleFactor = scaleFactor,
                cornerRoundness = cornerRoundness,
                onCheckedChange = { on ->
                    scope.launch {
                        settingsStore.saveBypassClientEnabled(on)
                        val pkg = if (on && bypassPkg.isBlank()) clientApps.firstOrNull()?.packageName.orEmpty() else bypassPkg
                        if (on && pkg.isNotBlank()) settingsStore.saveBypassClientPackage(pkg)
                        viewModel?.setTgwsBypass(on, pkg)
                    }
                }
            )
            if (bypassEnabled) {
                TgwsClientRow(
                    packageName = selectedClient?.packageName ?: bypassPkg,
                    label = selectedClient?.label ?: bypassPkg,
                    top = false,
                    bottom = true,
                    scaleFactor = scaleFactor,
                    cornerRoundness = cornerRoundness,
                    onClick = { showPicker = true }
                )
            }
        }

        SettingsGroup(title = stringResource(TgwsR.string.secret_key)) {
            TgwsSecretRow(
                value = secretKey,
                onValueChange = { scope.launch { settingsStore.saveSecretKey(it) } },
                top = true,
                bottom = false,
                scaleFactor = scaleFactor,
                cornerRoundness = cornerRoundness
            )
            SettingsActionRow(
                title = Loc.get("tgws_regen_title", language),
                subtitle = stringResource(TgwsR.string.help_secret_key_text),
                button = Loc.get("tgws_regenerate", language),
                enabled = true,
                scaleFactor = scaleFactor,
                cornerRoundness = cornerRoundness,
                bottom = true,
                onClick = { scope.launch { settingsStore.saveSecretKey(generateRandomSecret()) } }
            )
        }

        Spacer(modifier = Modifier.height(120.dp))
    }

    if (showPicker) {
        AppPickerSheet(
            apps = clientApps,
            selectedPackages = if (bypassPkg.isNotBlank()) setOf(bypassPkg) else emptySet(),
            onSave = { selected ->
                val pkg = selected.firstOrNull()
                if (pkg != null) {
                    scope.launch {
                        settingsStore.saveBypassClientEnabled(true)
                        settingsStore.saveBypassClientPackage(pkg)
                        viewModel?.setTgwsBypass(true, pkg)
                    }
                }
                showPicker = false
            },
            onDismiss = { showPicker = false },
            language = language
        )
    }
}
