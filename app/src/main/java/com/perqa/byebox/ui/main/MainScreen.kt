package com.perqa.byebox.ui.main

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Home
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
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
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Divider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SheetValue
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
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
import com.perqa.byebox.data.ProxyConfig
import com.perqa.byebox.theme.AppTheme
import com.perqa.byebox.theme.ByeBoxTheme
import kotlinx.coroutines.delay
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter

enum class NodeSortMode(val label: String) {
    DEFAULT("По умолчанию"),
    SOURCE("Источник"),
    PING("Пинг"),
    NAME("Имя")
}

enum class SettingsSubMenu {
    CONNECTION,
    ROUTING,
    APPEARANCE,
    SYSTEM
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
private fun PlainDragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(vertical = 10.dp)
            .size(width = 32.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
    )
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
    onItemClick: (androidx.navigation3.runtime.NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel? = null
) {
    val context = LocalContext.current
    val viewModel: MainScreenViewModel = viewModel ?: viewModel { MainScreenViewModel(context.applicationContext) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Dynamic M3 theme overriding inside MainScreen based on state.appTheme
    ByeBoxTheme(appTheme = state.appTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            var selectedTab by remember { mutableIntStateOf(0) }
            var showImportDialog by remember { mutableStateOf(false) }
            var speedDialExpanded by remember { mutableStateOf(false) }

            val density = androidx.compose.ui.platform.LocalDensity.current
            val configuration = LocalConfiguration.current
            val screenWidthDp = configuration.screenWidthDp.dp
            var fabRightOffsetFromEndDp by remember { mutableStateOf(0.dp) }
            val hazeState = remember { HazeState() }

            val qrLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val url = result.data?.getStringExtra("SCAN_RESULT")
                if (url != null) {
                    showImportDialog = false
                    viewModel.addConfigFromUrl(url)
                }
            }

            BackHandler(enabled = speedDialExpanded) {
                speedDialExpanded = false
            }

            BackHandler(enabled = !speedDialExpanded && selectedTab != 0) {
                selectedTab = 0
            }

            if (showImportDialog) {
                ImportConfigDialog(
                    onDismiss = { showImportDialog = false },
                    onImport = { url ->
                        viewModel.addConfigFromUrl(url)
                    },
                    language = state.language
                )
            }

            Box(modifier = modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().haze(hazeState)) {
                    // 1. Main Content Tab Container
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))
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

                    // 2. Bottom Fade
                    BottomEdgeFade(
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )

                    // 3. Scrim overlay and bubbles for Speed Dial (transparent dismiss-on-tap detector)
                    AnimatedVisibility(
                        visible = speedDialExpanded && selectedTab == 1,
                        enter = androidx.compose.animation.fadeIn(animationSpec = tween(200)),
                        exit = androidx.compose.animation.fadeOut(animationSpec = tween(200)),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    speedDialExpanded = false
                                }
                        ) {
                            val clipboardManager = LocalClipboardManager.current
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .navigationBarsPadding()
                                    .padding(bottom = 86.dp, end = fabRightOffsetFromEndDp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                ImportBubble(
                                    label = Loc.get("import_link", state.language),
                                    icon = Icons.Default.Share,
                                    index = 0,
                                    scaleFactor = state.tapImpactScale,
                                    glassmorphic = state.glassmorphicBar,
                                    maxBlurEnabled = state.maxBlurEnabled,
                                    onClick = {
                                        speedDialExpanded = false
                                        showImportDialog = true
                                    }
                                )
                                ImportBubble(
                                    label = Loc.get("import_qr", state.language),
                                    icon = Icons.Default.Search,
                                    index = 1,
                                    scaleFactor = state.tapImpactScale,
                                    glassmorphic = state.glassmorphicBar,
                                    maxBlurEnabled = state.maxBlurEnabled,
                                    onClick = {
                                        speedDialExpanded = false
                                        qrLauncher.launch(
                                            Intent(context, QrScanActivity::class.java)
                                        )
                                    }
                                )
                                ImportBubble(
                                    label = Loc.get("import_clipboard", state.language),
                                    icon = Icons.Default.List,
                                    index = 2,
                                    scaleFactor = state.tapImpactScale,
                                    glassmorphic = state.glassmorphicBar,
                                    maxBlurEnabled = state.maxBlurEnabled,
                                    onClick = {
                                        speedDialExpanded = false
                                        val clip = clipboardManager.getText()?.toString()?.trim()
                                        if (!clip.isNullOrBlank()) {
                                            viewModel.addConfigFromUrl(clip)
                                        } else {
                                            Toast.makeText(context, Loc.get("clipboard_empty", state.language), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // 4. Bottom Row containing TabBar and FAB
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
                        onTabSelected = { selectedTab = it },
                        scaleFactor = state.tapImpactScale,
                        glassmorphic = state.glassmorphicBar,
                        maxBlurEnabled = state.maxBlurEnabled,
                        hazeState = hazeState,
                        language = state.language
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    FloatingContextAction(
                        selectedTab = selectedTab,
                        scaleFactor = state.tapImpactScale,
                        cornerRoundness = state.cornerRoundness,
                        onClick = {
                            when (selectedTab) {
                                0 -> viewModel.selectBestConfig()
                                1 -> speedDialExpanded = !speedDialExpanded
                                2 -> (context.findActivity() as? MainActivity)?.openSystemVpnSettings()
                                3 -> viewModel.exportLogs(context)
                            }
                        },
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            val rightX = coordinates.positionInRoot().x + coordinates.size.width
                            val rightXDp = with(density) { rightX.toDp() }
                            fabRightOffsetFromEndDp = screenWidthDp - rightXDp
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
            dnsServer = state.dnsServer,
            language = state.language
        )

        Spacer(modifier = Modifier.height(14.dp))

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


        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SpeedCard(
                label = Loc.get("download", state.language),
                value = state.downloadSpeed,
                iconColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.46f),
                modifier = Modifier.weight(1f)
            )
            SpeedCard(
                label = Loc.get("upload", state.language),
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
            onAddTile = { activity?.requestQuickSettingsTile() },
            language = state.language
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
    dnsServer: DnsServer,
    language: String = "ru"
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
                    ConnectionStatus.CONNECTED -> Loc.get("status_connected", language)
                    ConnectionStatus.CONNECTING -> Loc.get("status_connecting", language)
                    ConnectionStatus.RECONNECTING -> Loc.get("status_reconnecting", language)
                    ConnectionStatus.DISCONNECTED -> Loc.get("status_disconnected", language)
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = activeConfig?.name ?: Loc.get("status_no_server", language),
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
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
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(28.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
    
    // Performance optimization: cache path allocations to prevent object churn in drawing frames
    val path1 = remember { androidx.compose.ui.graphics.Path() }
    val path2 = remember { androidx.compose.ui.graphics.Path() }

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
            
            if (status == ConnectionStatus.CONNECTING || status == ConnectionStatus.RECONNECTING) {
                // Wave 1: 5 crests, rotating forward
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

                // Wave 2: 7 crests, rotating backward, slightly smaller amplitude
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
                    // Static neat ring when connected but pulse is disabled
                    drawCircle(
                        color = buttonBgColor.copy(alpha = 0.16f),
                        radius = baseRadius,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
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
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
fun ProxyTab(
    state: MainUiState,
    viewModel: MainScreenViewModel
) {
    val tactileFeedback = rememberTactileFeedback()
    var importUrl by remember { mutableStateOf("") }
    var importUrlError by remember { mutableStateOf(false) }
    var nodeSearchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(NodeSortMode.DEFAULT) }
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
        ConfigDetailsSheet(
            config = config,
            onDismiss = { configDetails = null },
            viewModel = viewModel
        )
    }

    if (showProxyToolsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProxyToolsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            dragHandle = { PlainDragHandle() }
        ) {
            ProxyToolsSheet(
                sortMode = sortMode,
                onSortModeSelected = {
                    sortMode = it
                    showProxyToolsSheet = false
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Конфигурации",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "${sourceGroups.size} источника · ${state.configs.size} узлов",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.isPinging) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).padding(4.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = {
                            tactileFeedback()
                            viewModel.testPings()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Тест пинга всех",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(
                    onClick = {
                        tactileFeedback()
                        viewModel.refreshSubscriptions()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Обновить подписки",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        ProxySearchField(
            value = nodeSearchQuery,
            onValueChange = { nodeSearchQuery = it },
            onOpenFilters = {
                tactileFeedback()
                showProxyToolsSheet = true
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

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
        NodeSortMode.DEFAULT -> this
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
    onSortModeSelected: (NodeSortMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Сортировка узлов",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
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

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = if (activeCount > 0) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        enabled = !isRenaming,
        onClick = {
            tactileFeedback()
            onToggleExpanded()
        }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
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
                    source?.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 3,
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
                        val expireColor = subscriptionExpireColor(it)
                        Text(
                            text = trafficSubtitle(it),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = expireColor ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp).padding(2.dp)
                )
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
fun <T> SegmentedSelector(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(start = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { (value, title) ->
                val isSelected = value == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .clickable { onSelected(value) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ImportConfigDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
    language: String = "ru"
) {
    var text by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var clipboardUrl by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        val clip = clipboardManager.getText()?.toString()
        if (clip != null) {
            val trimmed = clip.trim()
            if (trimmed.startsWith("vless://") || trimmed.startsWith("vmess://") ||
                trimmed.startsWith("ss://") || trimmed.startsWith("trojan://") ||
                trimmed.startsWith("hysteria2://") || trimmed.startsWith("hysteria://") ||
                trimmed.startsWith("http://") || trimmed.startsWith("https://") ||
                trimmed.startsWith("socks://") || trimmed.startsWith("tuic://")
            ) {
                clipboardUrl = trimmed
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = Loc.get("import_config", language),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val description = if (language == "ru") {
                    "Введите ссылку на прокси-сервер (vless, vmess, ss, trojan) или ссылку на подписку (http/https):"
                } else if (language == "zh") {
                    "请输入代理服务器链接 (vless, vmess, ss, trojan) 或订阅链接 (http/https)："
                } else {
                    "Enter proxy server link (vless, vmess, ss, trojan) or subscription URL (http/https):"
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        isError = false
                    },
                    label = { Text(Loc.get("config_url_label", language)) },
                    placeholder = { Text("vless://..., vmess://..., http://...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = false,
                    maxLines = 4,
                    isError = isError,
                    trailingIcon = {
                        if (text.isNotEmpty()) {
                            IconButton(onClick = { text = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = Loc.get("clear", language))
                            }
                        }
                    },
                    supportingText = {
                        val hint = when {
                            text.startsWith("vless://") || text.startsWith("vmess://") ||
                            text.startsWith("ss://") || text.startsWith("trojan://") ||
                            text.startsWith("hysteria2://") || text.startsWith("hysteria://") ||
                            text.startsWith("socks://") || text.startsWith("tuic://") ->
                                Loc.get("direct_link", language)
                            text.startsWith("http://") || text.startsWith("https://") ->
                                Loc.get("sub_link", language)
                            text.isNotEmpty() -> Loc.get("unknown_format", language)
                            else -> null
                        }
                        if (hint != null) {
                            Text(
                                hint,
                                color = if (hint == Loc.get("unknown_format", language)) MaterialTheme.colorScheme.error
                                       else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else if (isError) {
                            Text(Loc.get("unknown_format", language), color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                if (clipboardUrl != null && text != clipboardUrl) {
                    SuggestionChip(
                        onClick = { text = clipboardUrl!! },
                        label = { Text(Loc.get("clipboard_chip", language)) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = text.trim()
                    if (trimmed.isNotEmpty()) {
                        onImport(trimmed)
                        onDismiss()
                    } else {
                        isError = true
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(Loc.get("import_link", language), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(Loc.get("back", language), fontWeight = FontWeight.SemiBold)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigDetailsSheet(
    config: ProxyConfig,
    onDismiss: () -> Unit,
    viewModel: MainScreenViewModel
) {
    var isEditing by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    var name by remember(config) { mutableStateOf(config.name) }
    var address by remember(config) { mutableStateOf(config.address) }
    var portString by remember(config) { mutableStateOf(config.port.toString()) }
    var uuid by remember(config) { mutableStateOf(config.uuid) }
    var sni by remember(config) { mutableStateOf(config.sni ?: "") }
    var flow by remember(config) { mutableStateOf(config.flow ?: "") }
    var security by remember(config) { mutableStateOf(config.security ?: "") }
    var network by remember(config) { mutableStateOf(config.network ?: "") }
    var wsPath by remember(config) { mutableStateOf(config.wsPath ?: "") }
    var wsHost by remember(config) { mutableStateOf(config.wsHost ?: "") }
    var grpcServiceName by remember(config) { mutableStateOf(config.grpcServiceName ?: "") }
    var pbk by remember(config) { mutableStateOf(config.pbk ?: "") }
    var sid by remember(config) { mutableStateOf(config.sid ?: "") }

    val hasChanges by remember {
        derivedStateOf {
            isEditing && (
                name != config.name ||
                address != config.address ||
                portString != config.port.toString() ||
                uuid != config.uuid ||
                sni != (config.sni ?: "") ||
                flow != (config.flow ?: "") ||
                security != (config.security ?: "") ||
                network != (config.network ?: "") ||
                wsPath != (config.wsPath ?: "") ||
                wsHost != (config.wsHost ?: "") ||
                grpcServiceName != (config.grpcServiceName ?: "") ||
                pbk != (config.pbk ?: "") ||
                sid != (config.sid ?: "")
            )
        }
    }

    fun attemptDismiss() {
        if (showExitDialog) return
        if (hasChanges) {
            showExitDialog = true
        } else {
            onDismiss()
        }
    }

    BackHandler(enabled = !showExitDialog) { attemptDismiss() }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    text = "Несохраненные изменения",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
            },
            text = {
                Text("У вас есть несохраненные изменения. Выйти без сохранения?")
            },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    isEditing = false
                    onDismiss()
                }) {
                    Text("Выйти", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Остаться")
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    val scrollState = rememberScrollState()
    val tactileFeedback = rememberTactileFeedback()
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = { attemptDismiss() },
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { newValue ->
                if (newValue == SheetValue.Hidden && hasChanges) {
                    showExitDialog = true
                    false
                } else {
                    true
                }
            }
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { PlainDragHandle() }
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
                    text = if (isEditing) "Редактирование" else config.name,
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

        if (isEditing) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название (Remarks)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Адрес (IP/Host)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    OutlinedTextField(
                        value = portString,
                        onValueChange = { portString = it.filter { char -> char.isDigit() } },
                        label = { Text("Порт") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(18.dp)
                    )
                }

                OutlinedTextField(
                    value = uuid,
                    onValueChange = { uuid = it },
                    label = { Text("UUID / Пароль") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                )

                OutlinedTextField(
                    value = sni,
                    onValueChange = { sni = it },
                    label = { Text("SNI") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                )

                SegmentedSelector(
                    label = "Безопасность (security)",
                    options = listOf(
                        "none" to "None",
                        "tls" to "TLS",
                        "reality" to "Reality"
                    ),
                    selected = security.lowercase().ifBlank { "none" },
                    onSelected = { security = it }
                )

                SegmentedSelector(
                    label = "Транспорт (network)",
                    options = listOf(
                        "tcp" to "TCP",
                        "ws" to "WebSocket",
                        "grpc" to "gRPC"
                    ),
                    selected = network.lowercase().ifBlank { "tcp" },
                    onSelected = { network = it }
                )

                if (security == "reality" || pbk.isNotBlank() || sid.isNotBlank()) {
                    OutlinedTextField(
                        value = pbk,
                        onValueChange = { pbk = it },
                        label = { Text("PublicKey (Reality)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    )
                    OutlinedTextField(
                        value = sid,
                        onValueChange = { sid = it },
                        label = { Text("ShortId (Reality)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    )
                }

                if (network == "ws" || wsPath.isNotBlank() || wsHost.isNotBlank()) {
                    OutlinedTextField(
                        value = wsPath,
                        onValueChange = { wsPath = it },
                        label = { Text("WS Path") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    )
                    OutlinedTextField(
                        value = wsHost,
                        onValueChange = { wsHost = it },
                        label = { Text("WS Host") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    )
                }

                if (network == "grpc" || grpcServiceName.isNotBlank()) {
                    OutlinedTextField(
                        value = grpcServiceName,
                        onValueChange = { grpcServiceName = it },
                        label = { Text("gRPC Service Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    )
                }
            }
        } else {
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
                    config.security?.takeIf { it.isNotBlank() }?.let { ConfigDetailLine("Безопасность", it) }
                    config.sni?.takeIf { it.isNotBlank() }?.let { ConfigDetailLine("SNI", it) }
                    config.flow?.takeIf { it.isNotBlank() }?.let { ConfigDetailLine("Flow", it) }
                    config.wsPath?.takeIf { it.isNotBlank() }?.let { ConfigDetailLine("WS Path", it) }
                    config.grpcServiceName?.takeIf { it.isNotBlank() }?.let { ConfigDetailLine("gRPC Service", it) }
                    ConfigDetailLine("Источник", config.sourceName)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isEditing) {
                Button(
                    onClick = {
                        tactileFeedback()
                        isEditing = false
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Отмена", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        tactileFeedback()
                        val parsedPort = portString.toIntOrNull()
                        if (parsedPort == null || parsedPort !in 1..65535) {
                            Toast.makeText(context, "Некорректный порт (1..65535)", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (address.isBlank() || name.isBlank() || uuid.isBlank()) {
                            Toast.makeText(context, "Имя, адрес и пароль не могут быть пустыми", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val updatedConfig = config.copy(
                            name = name,
                            address = address,
                            port = parsedPort,
                            uuid = uuid,
                            sni = sni.takeIf { it.isNotBlank() },
                            flow = flow.takeIf { it.isNotBlank() },
                            security = security.takeIf { it.isNotBlank() },
                            network = network.takeIf { it.isNotBlank() },
                            wsPath = wsPath.takeIf { it.isNotBlank() },
                            wsHost = wsHost.takeIf { it.isNotBlank() },
                            grpcServiceName = grpcServiceName.takeIf { it.isNotBlank() },
                            pbk = pbk.takeIf { it.isNotBlank() },
                            sid = sid.takeIf { it.isNotBlank() }
                        )

                        viewModel.updateConfig(updatedConfig)
                        isEditing = false
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("Сохранить", fontWeight = FontWeight.Bold)
                }
            } else {
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
                    onClick = {
                        tactileFeedback()
                        isEditing = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("Изменить", fontWeight = FontWeight.Bold)
                }
            }
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
    val parts = mutableListOf<String>()
    val url = sourceUrl ?: "Локальный импорт"
    parts.add(url)
    source?.lastUpdatedAt?.let { timestamp ->
        val formatter = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault())
        parts.add(formatter.format(java.util.Date(timestamp)))
    }
    return parts.joinToString(" · ")
}

private fun trafficSubtitle(source: com.perqa.byebox.data.SubscriptionSource): String {
    val parts = mutableListOf<String>()
    val total = source.totalBytes
    if (total != null && total > 0L) {
        val used = (source.uploadBytes ?: 0L) + (source.downloadBytes ?: 0L)
        parts.add("${formatBytes(used)} / ${formatBytes(total)}")
    }
    source.expireAt?.let { epochMillis ->
        if (epochMillis > 0L) {
            val now = System.currentTimeMillis()
            val daysLeft = (epochMillis - now) / (1000L * 60 * 60 * 24)
            when {
                daysLeft < 0 -> parts.add("истекла")
                daysLeft == 0L -> parts.add("сегодня")
                daysLeft == 1L -> parts.add("1 день")
                daysLeft in 2L..4L -> parts.add("$daysLeft дня")
                daysLeft <= 30L -> parts.add("$daysLeft дн.")
                else -> {
                    val formatter = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                    parts.add("до ${formatter.format(java.util.Date(epochMillis))}")
                }
            }
        }
    }
    return parts.joinToString(" · ")
}

@Composable
private fun subscriptionExpireColor(source: com.perqa.byebox.data.SubscriptionSource): androidx.compose.ui.graphics.Color? {
    val epochMillis = source.expireAt ?: return null
    if (epochMillis <= 0L) return null
    val now = System.currentTimeMillis()
    val daysLeft = (epochMillis - now) / (1000L * 60 * 60 * 24)
    return when {
        daysLeft < 0 -> MaterialTheme.colorScheme.error
        daysLeft <= 3L -> MaterialTheme.colorScheme.error
        daysLeft <= 7L -> MaterialTheme.colorScheme.tertiary
        else -> null
    }
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

    var selectedSubMenu by remember { mutableStateOf<SettingsSubMenu?>(null) }
    
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
                    appRoutingMode = state.appRoutingMode
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
                
                Spacer(
                    modifier = Modifier
                        .height(120.dp)
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
                        else -> ""
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedSubMenu) {
                    SettingsSubMenu.CONNECTION -> {
                        SettingsGroup(title = "Параметры VPN") {
                            SettingsSwitchRow(
                                title = "IPv6 в туннеле",
                                subtitle = "Включить IPv6-маршрутизацию в виртуальном интерфейсе",
                                checked = state.ipv6Enabled,
                                icon = Icons.Default.Build,
                                top = true,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeIpv6Enabled
                            )
                            SettingsSwitchRow(
                                title = "Предпочитать IPv6",
                                subtitle = "Запрашивать IPv6-адреса (AAAA) при резолве доменов",
                                checked = state.preferIpv6Enabled,
                                icon = Icons.Default.Build,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changePreferIpv6Enabled
                            )
                            SettingsSwitchRow(
                                title = "Обход локальных сетей",
                                subtitle = "LAN, loopback и link-local сети не попадают в VPN",
                                checked = state.lanBypassEnabled,
                                icon = Icons.Default.Home,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeLanBypassEnabled
                            )
                            SettingsSwitchRow(
                                title = "Блокировка трафика без VPN",
                                subtitle = "Kill Switch — блокировать интернет-соединение, если VPN отключен или разорван",
                                checked = state.blockingEnabled,
                                icon = Icons.Default.Lock,
                                bottom = true,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeBlockingEnabled
                            )
                        }

                        SettingsGroup(title = "DNS Сервера") {
                            DnsServer.values().forEachIndexed { index, dns ->
                                SettingsChoiceRow(
                                    title = dns.label,
                                    subtitle = dns.address,
                                    selected = state.dnsServer == dns,
                                    top = index == 0,
                                    bottom = index == DnsServer.values().lastIndex,
                                    scaleFactor = state.tapImpactScale,
                                    cornerRoundness = state.cornerRoundness,
                                    onClick = {
                                        tactileFeedback()
                                        viewModel.changeDnsServer(dns)
                                    }
                                )
                            }
                        }

                        SettingsGroup(title = "Локальный прокси") {
                            SettingsSwitchRow(
                                title = "Режим VPN",
                                subtitle = "VPN перехватывает весь трафик устройства, иначе работает локальный прокси",
                                checked = state.vpnModeEnabled,
                                icon = Icons.Default.Lock,
                                top = true,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeVpnModeEnabled
                            )
                            SettingsSwitchRow(
                                title = "Доступ из LAN",
                                subtitle = "Разрешить устройствам в локальной сети использовать ваш прокси",
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
                                Text(
                                    text = "Локальный SOCKS порт",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.weight(1f)
                                )
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

                        SettingsGroup(title = "TUN Стек") {
                            TunStack.values().forEachIndexed { index, stack ->
                                SettingsChoiceRow(
                                    title = stack.label,
                                    subtitle = stack.description,
                                    selected = state.tunStack == stack,
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
                        SettingsGroup(title = "Правила маршрутизации") {
                            RoutingProfile.values().forEachIndexed { index, profile ->
                                SettingsChoiceRow(
                                    title = profile.label,
                                    subtitle = when (profile) {
                                        RoutingProfile.BYPASS_LAN_CN_RU -> "Локальные сети и популярные региональные диапазоны идут напрямую"
                                        RoutingProfile.PROXY_ALL -> "Весь трафик устройства проходит через выбранный узел"
                                        RoutingProfile.DIRECT -> "Туннель не забирает трафик, удобно для диагностики"
                                    },
                                    selected = state.routingProfile == profile,
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

                        SettingsGroup(title = "Профиль приложений") {
                            AppRoutingMode.values().forEachIndexed { index, mode ->
                                SettingsChoiceRow(
                                    title = mode.label,
                                    subtitle = mode.description,
                                    selected = state.appRoutingMode == mode,
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
                                title = "Выбранные приложения",
                                subtitle = "${selectedAppPackages.size} пакетов",
                                button = "Выбрать",
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

                        SettingsGroup(title = "Функции ядра Xray") {
                            SettingsSwitchRow(
                                title = "Сниффинг трафика",
                                subtitle = "Сниффить HTTP/TLS запросы для извлечения доменного имени",
                                checked = state.sniffingEnabled,
                                icon = Icons.Default.Search,
                                top = true,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeSniffingEnabled
                            )
                            SettingsSwitchRow(
                                title = "Фрагментация (Fragment)",
                                subtitle = "Разбивать TLS-пакеты для обхода DPI/ТСПУ систем фильтрации",
                                checked = state.fragmentEnabled,
                                icon = Icons.Default.Build,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeFragmentEnabled
                            )
                            SettingsSwitchRow(
                                title = "Мультиплексирование (Mux)",
                                subtitle = "Объединять соединения в одно для снижения накладных расходов",
                                checked = state.muxEnabled,
                                icon = Icons.Default.Settings,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeMuxEnabled
                            )
                            SettingsSwitchRow(
                                title = "Fake DNS",
                                subtitle = "Кэшировать DNS-запросы на устройстве для ускорения загрузки",
                                checked = state.fakeDnsEnabled,
                                icon = Icons.Default.Settings,
                                bottom = true,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeFakeDnsEnabled
                            )
                        }
                    }

                    SettingsSubMenu.APPEARANCE -> {
                        SettingsGroup(title = "Тема оформления") {
                            SettingsThemeGrid(state = state, viewModel = viewModel)
                        }
                        
                        SettingsGroup(title = "Степень отклика") {
                            val options = listOf(
                                1.00f to "Без сжатия (1.00)",
                                0.95f to "Легкий отклик (0.95)",
                                0.90f to "Средний (0.90)",
                                0.85f to "Глубокий отклик (0.85)"
                            )
                            options.forEachIndexed { index, (value, label) ->
                                SettingsChoiceRow(
                                    title = label,
                                    subtitle = "Физическое сжатие интерфейса при нажатии",
                                    selected = state.tapImpactScale == value,
                                    top = index == 0,
                                    bottom = index == options.lastIndex,
                                    scaleFactor = state.tapImpactScale,
                                    cornerRoundness = state.cornerRoundness,
                                    onClick = { viewModel.changeTapImpactScale(value) }
                                )
                            }
                        }
                        
                        SettingsGroup(title = "Стиль скруглений") {
                            val roundnessOptions = listOf(
                                "standard" to "Стандартный (M3)",
                                "expressive" to "Выразительный (M3 Expressive)"
                            )
                            roundnessOptions.forEachIndexed { index, (value, label) ->
                                SettingsChoiceRow(
                                    title = label,
                                    subtitle = if (value == "expressive") "Крупные скругления углов и выразительная форма элементов" else "Стандартный строгий дизайн Material Design",
                                    selected = state.cornerRoundness == value,
                                    top = index == 0,
                                    bottom = index == roundnessOptions.lastIndex,
                                    scaleFactor = state.tapImpactScale,
                                    cornerRoundness = state.cornerRoundness,
                                    onClick = { viewModel.changeCornerRoundness(value) }
                                )
                            }
                        }
                        
                        SettingsGroup(title = "Эффекты и анимация") {
                            SettingsSwitchRow(
                                title = "Пульсация кнопки",
                                subtitle = "Анимированная пульсация центральной кнопки при активном подключении",
                                checked = state.pulseEnabled,
                                icon = Icons.Default.Star,
                                top = true,
                                bottom = false,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changePulseEnabled
                            )
                            SettingsSwitchRow(
                                title = "Стеклянный эффект",
                                subtitle = "Эффект полупрозрачности (Glassmorphism) для нижней панели вкладок",
                                checked = state.glassmorphicBar,
                                icon = Icons.Default.Share,
                                top = false,
                                bottom = false,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeGlassmorphicBar
                            )
                            SettingsSwitchRow(
                                title = "Максимальное размытие",
                                subtitle = "Плотный матовый эффект без обводки (стиль Android 17, по умолчанию включен)",
                                checked = state.maxBlurEnabled,
                                icon = Icons.Default.Lock,
                                top = false,
                                bottom = true,
                                scaleFactor = state.tapImpactScale,
                                cornerRoundness = state.cornerRoundness,
                                onCheckedChange = viewModel::changeMaxBlurEnabled
                            )
                        }
                    }

                    SettingsSubMenu.SYSTEM -> {
                        SettingsGroup(title = Loc.get("language", state.language)) {
                            val languages = listOf(
                                "ru" to Loc.get("lang_ru", state.language),
                                "en" to Loc.get("lang_en", state.language),
                                "zh" to Loc.get("lang_zh", state.language)
                            )
                            languages.forEachIndexed { index, (value, label) ->
                                SettingsChoiceRow(
                                    title = label,
                                    subtitle = if (value == "ru") "Русский интерфейс" else if (value == "en") "English interface" else "中文界面",
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
                                bottom = true
                            )
                        }
                    }
                    else -> {}
                }
                
                Spacer(
                    modifier = Modifier
                        .height(190.dp)
                        .navigationBarsPadding()
                )
            }
        }
    }
}

@Composable
private fun SettingsChoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    top: Boolean = false,
    bottom: Boolean = false,
    scaleFactor: Float = 0.90f,
    cornerRoundness: String = "expressive",
    onClick: () -> Unit
) {
    SettingsRowSurface(top = top, bottom = bottom, selected = selected, scaleFactor = scaleFactor, cornerRoundness = cornerRoundness, onClick = onClick) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val radius = size.minDimension / 2
            if (selected) {
                drawCircle(color = Color.White.copy(alpha = 0.92f), radius = radius)
                drawCircle(color = Color.Black.copy(alpha = 0.28f), radius = radius / 2.6f)
            } else {
                drawCircle(color = Color.White.copy(alpha = 0.38f), radius = radius - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
            }
        }
        SettingsRowText(title = title, subtitle = subtitle, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Settings,
    enabled: Boolean = true,
    top: Boolean = false,
    bottom: Boolean = false,
    scaleFactor: Float = 0.90f,
    cornerRoundness: String = "expressive",
    onCheckedChange: (Boolean) -> Unit
) {
    val tactileFeedback = rememberTactileFeedback()
    SettingsRowSurface(
        top = top,
        bottom = bottom,
        selected = checked && enabled,
        enabled = enabled,
        scaleFactor = scaleFactor,
        cornerRoundness = cornerRoundness,
        onClick = {
            tactileFeedback()
            onCheckedChange(!checked)
        }
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = if (checked && enabled) 0.42f else 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        SettingsRowText(title = title, subtitle = subtitle, modifier = Modifier.weight(1f), enabled = enabled)
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    button: String,
    enabled: Boolean,
    scaleFactor: Float = 0.90f,
    cornerRoundness: String = "expressive",
    onClick: () -> Unit,
    bottom: Boolean = false
) {
    SettingsRowSurface(onClick = onClick, enabled = enabled, bottom = bottom, scaleFactor = scaleFactor, cornerRoundness = cornerRoundness) {
        SettingsRowText(title = title, subtitle = subtitle, modifier = Modifier.weight(1f), enabled = enabled)
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(button, maxLines = 1)
        }
    }
}

@Composable
private fun SettingsInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    scaleFactor: Float = 0.90f,
    cornerRoundness: String = "expressive",
    bottom: Boolean
) {
    SettingsRowSurface(bottom = bottom, enabled = enabled, scaleFactor = scaleFactor, cornerRoundness = cornerRoundness) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            enabled = enabled,
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun SettingsHealthRow(
    value: String,
    onValueChange: (String) -> Unit,
    onTest: () -> Unit,
    scaleFactor: Float = 0.90f,
    cornerRoundness: String = "expressive",
    top: Boolean = false,
    bottom: Boolean = false
) {
    SettingsRowSurface(top = top, bottom = bottom, scaleFactor = scaleFactor, cornerRoundness = cornerRoundness) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("URL ресурса") },
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp)
        )
        IconButton(onClick = onTest) {
            Icon(Icons.Default.Search, contentDescription = "Проверить")
        }
    }
}

@Composable
private fun SettingsRowSurface(
    top: Boolean = false,
    bottom: Boolean = false,
    selected: Boolean = false,
    enabled: Boolean = true,
    scaleFactor: Float = 0.90f,
    cornerRoundness: String = "expressive",
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val isExpressive = cornerRoundness == "expressive"
    val largeRadius = if (isExpressive) 28.dp else 14.dp
    val smallRadius = if (isExpressive) 10.dp else 4.dp
    
    val targetTop = if (top) {
        if (isPressed && enabled && onClick != null) (largeRadius + 4.dp) else largeRadius
    } else {
        if (isPressed && enabled && onClick != null) (smallRadius + 4.dp) else smallRadius
    }
    val targetBottom = if (bottom) {
        if (isPressed && enabled && onClick != null) (largeRadius + 4.dp) else largeRadius
    } else {
        if (isPressed && enabled && onClick != null) (smallRadius + 4.dp) else smallRadius
    }
    
    val animatedTop by animateDpAsState(targetValue = targetTop, label = "settingsRowTopCorner")
    val animatedBottom by animateDpAsState(targetValue = targetBottom, label = "settingsRowBottomCorner")
    
    val shape = RoundedCornerShape(
        topStart = animatedTop,
        topEnd = animatedTop,
        bottomStart = animatedBottom,
        bottomEnd = animatedBottom
    )
    
    val targetColor = when {
        selected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val color by animateColorAsState(targetValue = targetColor, label = "settingsRowColor")
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && onClick != null) scaleFactor else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "settingsRowScale"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(color.copy(alpha = if (enabled) 1f else 0.52f))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = androidx.compose.foundation.LocalIndication.current,
                        enabled = enabled,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun SettingsRowText(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.48f),
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = subtitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.62f else 0.36f)
            )
        )
    }
}

@Composable
private fun SettingsHeroCard(
    status: ConnectionStatus,
    routingProfile: RoutingProfile,
    dnsServer: DnsServer,
    appRoutingMode: AppRoutingMode
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = color
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = when (status) {
                    ConnectionStatus.CONNECTED -> "VPN подключен"
                    ConnectionStatus.CONNECTING -> "Поднимаем туннель"
                    ConnectionStatus.RECONNECTING -> "Идёт переподключение"
                    ConnectionStatus.DISCONNECTED -> "VPN отключен"
                },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(text = routingProfile.label, modifier = Modifier.weight(1f))
                InfoChip(text = dnsServer.label, modifier = Modifier.weight(1f))
            }
            InfoChip(text = appRoutingMode.label, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black
            )
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp), content = content)
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
    scaleFactor: Float = 0.90f,
    cornerRoundness: String = "expressive",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tactileFeedback = rememberTactileFeedback()
    val icon = when (selectedTab) {
        0 -> Icons.Default.Star
        1 -> Icons.Default.Add
        2 -> Icons.Default.Settings
        else -> Icons.Default.Share
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
    val isExpressive = cornerRoundness == "expressive"
    val baseRadius = if (isExpressive) 20.dp else 12.dp
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val targetRadius = if (isPressed) baseRadius + 4.dp else baseRadius
    val cornerRadius by animateDpAsState(targetValue = targetRadius, label = "fabCornerRadius")
    val buttonShape = RoundedCornerShape(cornerRadius)
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleFactor else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fabScale"
    )

    Surface(
        modifier = modifier
            .size(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 12.dp,
                shape = buttonShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.18f),
                spotColor = Color.Black.copy(alpha = 0.24f)
            )
            .clip(buttonShape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current
            ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerSheet(
    apps: List<InstalledAppInfo>,
    selectedPackages: Set<String>,
    onSave: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val tactileFeedback = rememberTactileFeedback()
    var query by remember { mutableStateOf("") }
    var hideSystem by remember { mutableStateOf(true) }
    var localSelected by remember { mutableStateOf(selectedPackages) }
    var showExitDialog by remember { mutableStateOf(false) }
    val hasChanges by remember {
        derivedStateOf { localSelected != selectedPackages }
    }

    val filteredApps = remember(apps, query, hideSystem) {
        val cleanQuery = query.trim()
        apps.filter { app ->
            val matchesQuery = cleanQuery.isBlank() ||
                app.label.contains(cleanQuery, ignoreCase = true) ||
                app.packageName.contains(cleanQuery, ignoreCase = true)
            val matchesSystem = !hideSystem || !app.isSystem
            matchesQuery && matchesSystem
        }
    }

    fun attemptDismiss() {
        if (showExitDialog) return
        if (hasChanges) {
            showExitDialog = true
        } else {
            onDismiss()
        }
    }

    BackHandler(enabled = !showExitDialog) { attemptDismiss() }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    text = "Несохраненные изменения",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
            },
            text = {
                Text("У вас есть несохраненные изменения. Выйти без сохранения?")
            },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onDismiss()
                }) {
                    Text("Выйти", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Остаться")
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    ModalBottomSheet(
        onDismissRequest = { attemptDismiss() },
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { newValue ->
                if (newValue == SheetValue.Hidden && hasChanges) {
                    showExitDialog = true
                    false
                } else {
                    true
                }
            }
        ),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { PlainDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Выбор приложений",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${localSelected.size} выбрано",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "Найдено: ${filteredApps.size}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Скрыть системные",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Switch(
                            checked = hideSystem,
                            onCheckedChange = { hideSystem = it }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Поиск по имени или пакету...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Очистить",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
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
                    val checked = app.packageName in localSelected
                    val iconPainter = remember(app.icon) {
                        app.icon?.let { BitmapPainter(it.asImageBitmap()) }
                    }

                    val itemCornerRadius by animateDpAsState(
                        targetValue = if (checked) 24.dp else 12.dp,
                        label = "appItemCornerRadius"
                    )
                    val itemBgColor by animateColorAsState(
                        targetValue = if (checked) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
                        },
                        label = "appItemBgColor"
                    )
                    val itemContentColor by animateColorAsState(
                        targetValue = if (checked) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        label = "appItemContentColor"
                    )
                    val itemShape = RoundedCornerShape(itemCornerRadius)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(itemShape)
                            .background(itemBgColor)
                            .clickable {
                                tactileFeedback()
                                localSelected = if (checked) {
                                    localSelected - app.packageName
                                } else {
                                    localSelected + app.packageName
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp)),
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
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = app.label.take(1).uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = itemContentColor
                                )
                            )
                            Text(
                                text = app.packageName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = itemContentColor.copy(alpha = 0.65f)
                                )
                            )
                        }
                        if (app.isSystem) {
                            Text(
                                text = "SYS",
                                modifier = Modifier.padding(end = 6.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = itemContentColor.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                tactileFeedback()
                                localSelected = if (checked) {
                                    localSelected - app.packageName
                                } else {
                                    localSelected + app.packageName
                                }
                            }
                        )
                    }
                }
            }

            Button(
                onClick = { onSave(localSelected) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                enabled = hasChanges,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                Text("Сохранить изменения", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun MainTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    scaleFactor: Float = 0.90f,
    glassmorphic: Boolean = true,
    maxBlurEnabled: Boolean = true,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    language: String = "ru",
    modifier: Modifier = Modifier
) {
    val tactileFeedback = rememberTactileFeedback()
    val containerColor = if (glassmorphic) {
        val alpha = if (maxBlurEnabled) 0.48f else 0.86f
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val containerShape = RoundedCornerShape(30.dp)

    Box(
        modifier = modifier
            .width(240.dp)
            .then(
                if (glassmorphic && maxBlurEnabled) {
                    Modifier
                } else {
                    Modifier.shadow(
                        elevation = if (glassmorphic) 6.dp else 14.dp,
                        shape = containerShape,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.18f),
                        spotColor = Color.Black.copy(alpha = 0.28f)
                    )
                }
            )
    ) {
        // Blurred background layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(containerShape)
                .background(containerColor)
                .then(
                    if (glassmorphic && maxBlurEnabled && hazeState != null) {
                        Modifier.hazeChild(
                            state = hazeState,
                            shape = containerShape,
                            style = HazeDefaults.style(
                                backgroundColor = Color.Transparent,
                                blurRadius = 32.dp,
                                noiseFactor = 0.05f
                            )
                        )
                    } else {
                        Modifier
                    }
                )
                .then(
                    if (glassmorphic && !maxBlurEnabled) {
                        Modifier.border(
                            BorderStroke(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                                    )
                                )
                            ),
                            containerShape
                        )
                    } else Modifier
                )
        )

        // Foreground content layer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Pair(Loc.get("title_main", language), Icons.Default.Refresh),
                Pair(Loc.get("title_proxies", language), Icons.Default.List),
                Pair(Loc.get("title_settings", language), Icons.Default.Settings),
                Pair(Loc.get("title_logs", language), Icons.Default.Info)
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
                
                val tabRadius by animateDpAsState(
                    targetValue = if (active) 21.dp else 10.dp,
                    label = "tabCornerRadius"
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

@Composable
fun ImportBubble(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    index: Int,
    scaleFactor: Float = 0.90f,
    glassmorphic: Boolean = true,
    maxBlurEnabled: Boolean = true
) {
    val tactileFeedback = rememberTactileFeedback()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val delayTime = when (index) {
            2 -> 0L
            1 -> 30L
            else -> 60L
        }
        delay(delayTime)
        visible = true
    }

    val animatedOffsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else {
            when (index) {
                2 -> 40.dp
                1 -> 48.dp
                else -> 96.dp
            }
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bubbleOffsetY"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bubbleAlpha"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scalePressed by animateFloatAsState(
        targetValue = if (isPressed) scaleFactor else 1f,
        label = "bubbleScalePressed"
    )

    val density = LocalDensity.current
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val contentColor = MaterialTheme.colorScheme.onSurface
    val bubbleShape = RoundedCornerShape(20.dp)

    val finalModifier = Modifier
        .graphicsLayer {
            alpha = animatedAlpha
            translationY = with(density) { animatedOffsetY.toPx() }
            scaleX = scalePressed
            scaleY = scalePressed
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                tactileFeedback()
                onClick()
            }
        )

    Surface(
        modifier = finalModifier,
        shape = bubbleShape,
        color = containerColor,
        border = if (glassmorphic && !maxBlurEnabled) {
            BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                    )
                )
            )
        } else null,
        tonalElevation = if (glassmorphic && !maxBlurEnabled) 4.dp else 0.dp,
        shadowElevation = if (glassmorphic && !maxBlurEnabled) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

object Loc {
    private val ru = mapOf(
        "title_main" to "Главная",
        "title_proxies" to "Прокси",
        "title_settings" to "Настройки",
        "title_logs" to "Логи",
        "status_connected" to "VPN защищает трафик",
        "status_connecting" to "Поднимаем VPN-туннель",
        "status_reconnecting" to "Связь потеряна, переподключаемся",
        "status_disconnected" to "VPN отключен",
        "status_no_server" to "Сервер не выбран",
        "download" to "Скачивание",
        "upload" to "Загрузка",
        "quick_best" to "Лучший",
        "quick_share" to "Поделиться",
        "quick_vpn_settings" to "VPN Android",
        "quick_tile" to "Плитка",
        "no_active_config" to "Нет выбранной рабочей конфигурации",
        "conn_params" to "VPN параметры, DNS сервера, TUN стек, локальный прокси",
        "rules_params" to "Маршрутизация, профиль приложений, функции ядра Xray",
        "appearance_params" to "Тема оформления, степень отклика, стиль скруглений, анимация",
        "system_params" to "Логирование, автозапуск, проверка серверов, сброс настроек",
        "submenu_connection" to "Соединение",
        "submenu_rules" to "Правила",
        "submenu_appearance" to "Внешний вид",
        "submenu_system" to "Система",
        "back" to "Назад",
        "language" to "Язык",
        "lang_ru" to "Русский",
        "lang_en" to "English",
        "lang_zh" to "简体中文",
        "theme" to "Тема оформления",
        "tap_impact" to "Степень отклика",
        "tap_impact_none" to "Без сжатия (1.00)",
        "tap_impact_light" to "Легкий отклик (0.95)",
        "tap_impact_medium" to "Средний (0.90)",
        "tap_impact_deep" to "Глубокий отклик (0.85)",
        "tap_impact_sub" to "Физическое сжатие интерфейса при нажатии",
        "corner_roundness" to "Стиль скруглений",
        "corner_std" to "Стандартный (M3)",
        "corner_expr" to "Выразительный (M3 Expressive)",
        "corner_std_sub" to "Стандартный строгий дизайн Material Design",
        "corner_expr_sub" to "Крупные скругления углов и выразительная форма элементов",
        "effects_anim" to "Эффекты и анимация",
        "pulse_btn" to "Пульсация кнопки",
        "pulse_btn_sub" to "Анимированная пульсация центральной кнопки при активном подключении",
        "glass_bar" to "Стеклянный эффект",
        "glass_bar_sub" to "Эффект полупрозрачности (Glassmorphism) для нижней панели вкладок",
        "max_blur" to "Максимальное размытие",
        "max_blur_sub" to "Плотный матовый эффект без обводки (стиль Android 17, по умолчанию включен)",
        "logging" to "Логирование",
        "log_debug_desc" to "Все события, включая детали подключений",
        "log_info_desc" to "Основные события и изменения состояния",
        "log_warn_desc" to "Только предупреждения и ошибки",
        "log_err_desc" to "Только критические ошибки",
        "sys_settings" to "Системные настройки",
        "start_on_boot" to "Автозапуск при старте",
        "start_on_boot_sub" to "Автоматически поднимать VPN после перезагрузки устройства",
        "confirm_remove" to "Подтверждение удаления",
        "confirm_remove_sub" to "Спрашивать подтверждение перед удалением сервера из списка",
        "check_servers" to "Проверка серверов",
        "check_resource" to "Проверка ресурса",
        "resource_ok" to "Ресурс доступен",
        "resource_fail" to "Ресурс недоступен",
        "import_config" to "Добавить конфигурацию",
        "import_link" to "Ссылка",
        "import_qr" to "QR-код",
        "import_clipboard" to "Буфер обмена",
        "clipboard_empty" to "Буфер обмена пуст!",
        "clipboard_chip" to "Из буфера",
        "clear" to "Очистить",
        "unknown_format" to "Неизвестный формат",
        "direct_link" to "Прямая ссылка на сервер",
        "sub_link" to "Ссылка на подписку",
        "config_url_label" to "Ссылка или URL подписки"
    )

    private val en = mapOf(
        "title_main" to "Main",
        "title_proxies" to "Proxies",
        "title_settings" to "Settings",
        "title_logs" to "Logs",
        "status_connected" to "VPN is protecting traffic",
        "status_connecting" to "Establishing VPN tunnel",
        "status_reconnecting" to "Connection lost, reconnecting",
        "status_disconnected" to "VPN disconnected",
        "status_no_server" to "No server selected",
        "download" to "Download",
        "upload" to "Upload",
        "quick_best" to "Best Server",
        "quick_share" to "Share Config",
        "quick_vpn_settings" to "VPN Android",
        "quick_tile" to "Add Tile",
        "no_active_config" to "No working configuration selected",
        "conn_params" to "VPN settings, DNS servers, TUN stack, local proxy",
        "rules_params" to "Routing, app routing, Xray core features",
        "appearance_params" to "Theme, click feedback, corner roundness, animations",
        "system_params" to "Logging, auto start, health check, reset",
        "submenu_connection" to "Connection",
        "submenu_rules" to "Rules",
        "submenu_appearance" to "Appearance",
        "submenu_system" to "System",
        "back" to "Back",
        "language" to "Language",
        "lang_ru" to "Русский",
        "lang_en" to "English",
        "lang_zh" to "简体中文",
        "theme" to "Theme",
        "tap_impact" to "Click Haptics",
        "tap_impact_none" to "No Haptics (1.00)",
        "tap_impact_light" to "Light (0.95)",
        "tap_impact_medium" to "Medium (0.90)",
        "tap_impact_deep" to "Deep (0.85)",
        "tap_impact_sub" to "UI scale squeeze on press",
        "corner_roundness" to "Corner Style",
        "corner_std" to "Standard (M3)",
        "corner_expr" to "Expressive (M3 Expressive)",
        "corner_std_sub" to "Standard strict Material Design",
        "corner_expr_sub" to "Larger corner radiuses and expressive shapes",
        "effects_anim" to "Effects & Animations",
        "pulse_btn" to "Pulse Button",
        "pulse_btn_sub" to "Pulse animation on central button when connected",
        "glass_bar" to "Glassmorphism",
        "glass_bar_sub" to "Translucent glass effect for bottom tab bar",
        "max_blur" to "Maximum Blur",
        "max_blur_sub" to "Dense matte background without outline (Android 17 style)",
        "logging" to "Logging",
        "log_debug_desc" to "All events, including connection details",
        "log_info_desc" to "Major events and state changes",
        "log_warn_desc" to "Warnings and errors only",
        "log_err_desc" to "Critical errors only",
        "sys_settings" to "System Settings",
        "start_on_boot" to "Start on Boot",
        "start_on_boot_sub" to "Automatically start VPN on device boot",
        "confirm_remove" to "Confirm Delete",
        "confirm_remove_sub" to "Show confirmation before deleting a server",
        "check_servers" to "Health Check",
        "check_resource" to "Test Resource",
        "resource_ok" to "Resource is reachable",
        "resource_fail" to "Resource is unreachable",
        "import_config" to "Add Configuration",
        "import_link" to "Link",
        "import_qr" to "QR Code",
        "import_clipboard" to "Clipboard",
        "clipboard_empty" to "Clipboard is empty!",
        "clipboard_chip" to "From clipboard",
        "clear" to "Clear",
        "unknown_format" to "Unknown format",
        "direct_link" to "Direct link to server",
        "sub_link" to "Subscription link",
        "config_url_label" to "Link or subscription URL"
    )

    private val zh = mapOf(
        "title_main" to "主页",
        "title_proxies" to "代理",
        "title_settings" to "设置",
        "title_logs" to "日志",
        "status_connected" to "VPN 正在保护流量",
        "status_connecting" to "正在建立 VPN 隧道",
        "status_reconnecting" to "连接已断开，正在重新连接",
        "status_disconnected" to "VPN 已断开",
        "status_no_server" to "未选择服务器",
        "download" to "下载",
        "upload" to "上传",
        "quick_best" to "最佳服务器",
        "quick_share" to "分享配置",
        "quick_vpn_settings" to "系统 VPN",
        "quick_tile" to "添加瓷贴",
        "no_active_config" to "未选择有效的配置",
        "conn_params" to "VPN 参数、DNS 服务器、TUN 栈、本地代理",
        "rules_params" to "路由、应用代理、Xray 内核功能",
        "appearance_params" to "主题风格、触觉反馈、圆角样式、动画",
        "system_params" to "日志记录、开机自启、服务器检查、重置设置",
        "submenu_connection" to "连接",
        "submenu_rules" to "规则",
        "submenu_appearance" to "外观",
        "submenu_system" to "系统",
        "back" to "返回",
        "language" to "语言",
        "lang_ru" to "Русский",
        "lang_en" to "English",
        "lang_zh" to "简体中文",
        "theme" to "主题",
        "tap_impact" to "按压触觉反馈",
        "tap_impact_none" to "无缩放 (1.00)",
        "tap_impact_light" to "轻微 (0.95)",
        "tap_impact_medium" to "中等 (0.90)",
        "tap_impact_deep" to "强烈 (0.85)",
        "tap_impact_sub" to "按压时界面物理缩放挤压效果",
        "corner_roundness" to "圆角风格",
        "corner_std" to "标准 (M3)",
        "corner_expr" to "丰富 (M3 Expressive)",
        "corner_std_sub" to "标准的 Material Design 严谨设计",
        "corner_expr_sub" to "更大的圆角和更具表现力的形状",
        "effects_anim" to "特效与动画",
        "pulse_btn" to "按钮呼吸灯",
        "pulse_btn_sub" to "连接成功时中心按钮的呼吸动画效果",
        "glass_bar" to "毛玻璃效果",
        "glass_bar_sub" to "底部导航栏的半透明毛玻璃效果",
        "max_blur" to "最大模糊",
        "max_blur_sub" to "无描边的紧密磨砂效果（Android 17 风格，默认开启）",
        "logging" to "日志记录",
        "log_debug_desc" to "所有事件，包括连接的详细信息",
        "log_info_desc" to "主要事件和状态更改",
        "log_warn_desc" to "仅限警告和错误",
        "log_err_desc" to "仅限严重错误",
        "sys_settings" to "系统设置",
        "start_on_boot" to "开机自启",
        "start_on_boot_sub" to "设备开机后自动启动 VPN 连接",
        "confirm_remove" to "删除确认",
        "confirm_remove_sub" to "从列表中删除服务器前询问确认",
        "check_servers" to "服务器连接性检查",
        "check_resource" to "测试资源地址",
        "resource_ok" to "测试成功，资源可达",
        "resource_fail" to "测试失败，资源不可达",
        "import_config" to "添加配置",
        "import_link" to "链接导入",
        "import_qr" to "扫码导入",
        "import_clipboard" to "从剪贴板导入",
        "clipboard_empty" to "剪贴板为空！",
        "clipboard_chip" to "剪贴板",
        "clear" to "清除",
        "unknown_format" to "未知格式",
        "direct_link" to "服务器直连配置链接",
        "sub_link" to "订阅链接",
        "config_url_label" to "链接或订阅 URL"
    )

    fun get(key: String, lang: String): String {
        val dict = when (lang) {
            "en" -> en
            "zh" -> zh
            else -> ru
        }
        return dict[key] ?: ru[key] ?: key
    }
}


