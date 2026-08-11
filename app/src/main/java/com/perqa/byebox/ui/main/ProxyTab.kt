package com.perqa.byebox.ui.main

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.basicMarquee
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.perqa.byebox.MainActivity
import com.perqa.byebox.data.ProxyConfig
import com.perqa.byebox.data.SubscriptionSource
import com.perqa.byebox.findActivity
import kotlinx.coroutines.launch

enum class NodeSortMode {
    DEFAULT, SOURCE, PING, NAME;

    fun label(language: String): String = Loc.get("sort_${name.lowercase()}", language)
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
    var pendingConfigDeleteId by remember { mutableStateOf<String?>(null) }
    var pendingSourceDeleteId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    // Deletion confirmation dialogs
    if (pendingConfigDeleteId != null) {
        val configName = state.configs.find { it.id == pendingConfigDeleteId }?.name ?: ""
        AlertDialog(
            onDismissRequest = { pendingConfigDeleteId = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = Loc.get("delete_config_confirm_title", state.language),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = Loc.get("delete_config_confirm_msg", state.language).replace("{name}", configName),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteConfig(pendingConfigDeleteId!!)
                        pendingConfigDeleteId = null
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
                TextButton(onClick = { pendingConfigDeleteId = null }) {
                    Text(
                        text = Loc.get("cancel_btn", state.language),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    if (pendingSourceDeleteId != null) {
        val sourceName = state.subscriptionSources.find { it.id == pendingSourceDeleteId }?.name ?: ""
        AlertDialog(
            onDismissRequest = { pendingSourceDeleteId = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = Loc.get("delete_source_confirm_title", state.language),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = Loc.get("delete_source_confirm_msg", state.language).replace("{name}", sourceName),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSubscriptionSource(pendingSourceDeleteId!!)
                        pendingSourceDeleteId = null
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
                TextButton(onClick = { pendingSourceDeleteId = null }) {
                    Text(
                        text = Loc.get("cancel_btn", state.language),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

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
            .groupBy { it.sourceName.ifBlank { Loc.get("local_configs", state.language) } }
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
            viewModel = viewModel,
            language = state.language
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
                },
                language = state.language
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
                    text = Loc.get("configurations", state.language),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = String.format(Loc.get("sources_nodes_fmt", state.language), sourceGroups.size, state.configs.size),
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
                            contentDescription = Loc.get("test_ping_all", state.language),
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
                        contentDescription = Loc.get("refresh_subs", state.language),
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
            },
            language = state.language
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
                        onClearSearch = { nodeSearchQuery = "" },
                        language = state.language
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
                        onDelete = { id ->
                            if (state.confirmRemoveEnabled) {
                                pendingConfigDeleteId = id
                            } else {
                                viewModel.deleteConfig(id)
                            }
                        },
                        onRefreshSource = { viewModel.refreshSubscription(it) },
                        onRenameSource = { sourceId, name -> viewModel.renameSubscriptionSource(sourceId, name) },
                        onDeleteSource = { id ->
                            if (state.confirmRemoveEnabled) {
                                pendingSourceDeleteId = id
                            } else {
                                viewModel.deleteSubscriptionSource(id)
                            }
                        },
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
                        shape = headerShape,
                        compactMode = state.compactLayoutEnabled,
                        showFlags = state.showFlagsEnabled,
                        language = state.language
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
                                onDelete = {
                                    if (state.confirmRemoveEnabled) {
                                        pendingConfigDeleteId = config.id
                                    } else {
                                        viewModel.deleteConfig(config.id)
                                    }
                                },
                                onOpenSettings = { configDetails = config },
                                topCorner = effectiveTopCorner,
                                bottomCorner = effectiveBottomCorner,
                                neighborOffsetDp = neighborOffsetPx.dp,
                                compactMode = state.compactLayoutEnabled,
                                showFlags = state.showFlagsEnabled,
                                language = state.language,
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
    language: String = "ru",
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
                        text = Loc.get("configurations", language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = String.format(Loc.get("sources_nodes_fmt", language), sourceCount, configCount),
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
                            text = if (collapsed) Loc.get("expand", language) else Loc.get("collapse", language),
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
                        },
                        language = language
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importUrl,
                        onValueChange = onImportUrlChange,
                        placeholder = {
                            Text(
                                Loc.get("import_placeholder", language),
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
                            { Text(Loc.get("import_error_hint", language)) }
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
                                Text(Loc.get("refresh_short", language), fontWeight = FontWeight.Bold)
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
                                Text(Loc.get("ping", language), fontWeight = FontWeight.Bold)
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
                                Text(Loc.get("add", language), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SortSummaryBar(
                        selected = sortMode,
                        searchQuery = searchQuery,
                        language = language,
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
    onOpenFilters: () -> Unit,
    language: String = "ru"
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
                        text = Loc.get("search_nodes", language),
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
                    contentDescription = Loc.get("sort_and_filters", language),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ProxyEmptyState(
    hasSearch: Boolean,
    onClearSearch: () -> Unit,
    language: String = "ru"
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
                text = if (hasSearch) Loc.get("nothing_found", language) else Loc.get("no_configs", language),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                textAlign = TextAlign.Center
            )
            Text(
                text = if (hasSearch) {
                    Loc.get("search_try_again", language)
                } else {
                    Loc.get("add_config_hint", language)
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )
            if (hasSearch) {
                TextButton(onClick = onClearSearch) {
                    Text(Loc.get("clear_search", language), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProxyToolsSheet(
    sortMode: NodeSortMode,
    onSortModeSelected: (NodeSortMode) -> Unit,
    language: String = "ru"
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = Loc.get("sort_nodes", language),
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
                            text = mode.label(language),
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
    onOpenFilters: () -> Unit,
    language: String = "ru"
) {
    val label = if (searchQuery.isBlank()) {
        String.format(Loc.get("sort_by_fmt", language), selected.label(language))
    } else {
        String.format(Loc.get("filter_by_fmt", language), searchQuery)
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
                contentDescription = Loc.get("open_filters_cd", language),
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
    source: SubscriptionSource?,
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
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(22.dp),
    compactMode: Boolean = false,
    showFlags: Boolean = true,
    language: String = "ru"
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
                                    Icon(Icons.Default.Check, contentDescription = Loc.get("save_cd", language))
                                }
                            }
                        )
                    } else {
                        Text(
                            text = sourceName,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1
                        )
                    }
                    Text(
                        text = sourceSubtitle(sourceUrl, source, language),
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
                        text = String.format(Loc.get("nodes_count_fmt", language), configs.size),
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
                        val expireColor = subscriptionExpireColor(it, MaterialTheme.colorScheme)
                        Text(
                            text = trafficSubtitle(it, language),
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
                    contentDescription = if (expanded) Loc.get("collapse_cd", language) else Loc.get("expand_cd", language),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp).padding(2.dp)
                )
            }

            if (expanded && source != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SourceActionButton(
                        label = Loc.get("ping", language),
                        icon = Icons.Default.Search,
                        onClick = onPingSource,
                        modifier = Modifier.weight(1f)
                    )
                    SourceActionButton(
                        label = Loc.get("refresh", language),
                        icon = Icons.Default.Refresh,
                        onClick = { onRefreshSource(source.id) },
                        modifier = Modifier.weight(1f)
                    )
                    SourceActionButton(
                        label = Loc.get("rename", language),
                        icon = Icons.Default.Edit,
                        onClick = { isRenaming = true },
                        modifier = Modifier.weight(1f)
                    )
                    SourceActionButton(
                        label = Loc.get("delete", language),
                        icon = Icons.Default.Delete,
                        onClick = { onDeleteSource(source.id) },
                        destructive = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            AnimatedVisibility(
                visible = showConfigs,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    configs.forEach { config ->
                        ServerItemCard(
                            config = config,
                            isActive = config.id == activeConfigId,
                            onSelect = { onSelect(config.id) },
                            onDelete = { onDelete(config.id) },
                            compactMode = compactMode,
                            showFlags = showFlags,
                            language = language
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

@OptIn(ExperimentalMaterial3Api::class)
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
    val tactileFeedback = rememberTactileFeedback()

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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

            val description = Loc.get("import_desc", language)
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

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(Loc.get("back", language), fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        tactileFeedback()
                        val trimmed = text.trim()
                        if (trimmed.isNotEmpty()) {
                            onImport(trimmed)
                            onDismiss()
                        } else {
                            isError = true
                        }
                    },
                    modifier = Modifier.weight(1.5f).height(48.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(Loc.get("import_link", language), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigDetailsSheet(
    config: ProxyConfig,
    onDismiss: () -> Unit,
    viewModel: MainScreenViewModel,
    language: String = "ru"
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
                        text = Loc.get("config_details_unsaved_title", language),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                    )
                },
                text = {
                    Text(Loc.get("config_details_unsaved_msg", language))
                },
                confirmButton = {
                    TextButton(onClick = {
                        showExitDialog = false
                        isEditing = false
                        onDismiss()
                    }) {
                        Text(Loc.get("config_details_exit", language), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text(Loc.get("config_details_stay", language))
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
                        text = if (isEditing) Loc.get("config_details_editing", language) else config.name,
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
                IconButton(onClick = ::attemptDismiss) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = Loc.get("config_details_close", language))
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
                        label = { Text(Loc.get("remark_label", language)) },
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
                            label = { Text(Loc.get("address_label", language)) },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        OutlinedTextField(
                            value = portString,
                            onValueChange = { portString = it.filter { char -> char.isDigit() } },
                            label = { Text(Loc.get("port_label", language)) },
                            singleLine = true,
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(18.dp)
                        )
                    }

                    OutlinedTextField(
                        value = uuid,
                        onValueChange = { uuid = it },
                        label = { Text(Loc.get("config_details_uuid", language)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    )

                    OutlinedTextField(
                        value = sni,
                        onValueChange = { sni = it },
                        label = { Text(Loc.get("config_details_sni", language)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    )

                    SegmentedSelector(
                        label = Loc.get("config_details_security", language),
                        options = listOf(
                            "none" to "None",
                            "tls" to "TLS",
                            "reality" to "Reality"
                        ),
                        selected = security.lowercase().ifBlank { "none" },
                        onSelected = { security = it }
                    )

                    SegmentedSelector(
                        label = Loc.get("config_details_transport", language),
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
                        ConfigDetailLine(Loc.get("config_details_protocol", language), config.protocolSummary())
                        ConfigDetailLine(Loc.get("config_details_address", language), "${config.address}:${config.port}")
                        ConfigDetailLine(Loc.get("config_details_transport_val", language), config.network ?: "tcp")
                        config.security?.takeIf { it.isNotBlank() }?.let { ConfigDetailLine(Loc.get("config_details_security_val", language), it) }
                        config.sni?.takeIf { it.isNotBlank() }?.let { ConfigDetailLine("SNI", it) }
                        config.flow?.takeIf { it.isNotBlank() }?.let { ConfigDetailLine("Flow", it) }
                        config.wsPath?.takeIf { it.isNotBlank() }?.let { ConfigDetailLine("WS Path", it) }
                        config.grpcServiceName?.takeIf { it.isNotBlank() }?.let { ConfigDetailLine("gRPC Service", it) }
                        ConfigDetailLine(Loc.get("config_details_source", language), config.sourceName)
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
                        Text(Loc.get("config_details_cancel", language), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            tactileFeedback()
                            val parsedPort = portString.toIntOrNull()
                            if (parsedPort == null || parsedPort !in 1..65535) {
                                Toast.makeText(context, Loc.get("invalid_port", language), Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (address.isBlank() || name.isBlank() || uuid.isBlank()) {
                                Toast.makeText(context, Loc.get("empty_fields", language), Toast.LENGTH_SHORT).show()
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
                        Text(Loc.get("config_details_save", language), fontWeight = FontWeight.Bold)
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
                        Text(Loc.get("config_details_done", language), fontWeight = FontWeight.Bold)
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
                        Text(Loc.get("config_details_change", language), fontWeight = FontWeight.Bold)
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

private fun sourceSubtitle(sourceUrl: String?, source: SubscriptionSource?, language: String = "ru"): String {
    val parts = mutableListOf<String>()
    val url = sourceUrl ?: Loc.get("local_import", language)
    parts.add(url)
    source?.lastUpdatedAt?.let { timestamp ->
        val formatter = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault())
        parts.add(formatter.format(java.util.Date(timestamp)))
    }
    return parts.joinToString(" · ")
}

private fun trafficSubtitle(source: SubscriptionSource, language: String = "ru"): String {
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
                daysLeft < 0 -> parts.add(Loc.get("expired", language))
                daysLeft == 0L -> parts.add(Loc.get("today", language))
                daysLeft == 1L -> parts.add(Loc.get("day_1", language))
                daysLeft in 2L..4L -> parts.add(String.format(Loc.get("day_2_4", language), daysLeft))
                daysLeft <= 30L -> parts.add(String.format(Loc.get("day_5_30", language), daysLeft))
                else -> {
                    val formatter = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                    parts.add("${String.format(Loc.get("until_date", language), formatter.format(java.util.Date(epochMillis)))}")
                }
            }
        }
    }
    return parts.joinToString(" · ")
}

private fun subscriptionExpireColor(source: SubscriptionSource, colorScheme: androidx.compose.material3.ColorScheme): Color? {
    val epochMillis = source.expireAt ?: return null
    if (epochMillis <= 0L) return null
    val now = System.currentTimeMillis()
    val daysLeft = (epochMillis - now) / (1000L * 60 * 60 * 24)
    return when {
        daysLeft < 0 -> colorScheme.error
        daysLeft <= 3L -> colorScheme.error
        daysLeft <= 7L -> colorScheme.tertiary
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
    onSwipingChanged: (Boolean) -> Unit = {},
    compactMode: Boolean = false,
    showFlags: Boolean = true,
    language: String = "ru"
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
        targetValue = if (isActive) 40.dp else topCorner,
        animationSpec = tween(260),
        label = "topCorner"
    )
    val animBottomCorner by animateDpAsState(
        targetValue = if (isActive) 40.dp else bottomCorner,
        animationSpec = tween(260),
        label = "bottomCorner"
    )

    val cardTopCorner = lerp(animTopCorner, 40.dp, roundnessProgress)
    val cardBottomCorner = lerp(animBottomCorner, 40.dp, roundnessProgress)
    val shape = RoundedCornerShape(
        topStart = cardTopCorner,
        topEnd = cardTopCorner,
        bottomStart = cardBottomCorner,
        bottomEnd = cardBottomCorner
    )

    val bgShape = RoundedCornerShape(40.dp)

    val containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
                         else MaterialTheme.colorScheme.surfaceContainer
    val errorContainer = MaterialTheme.colorScheme.errorContainer
    val onErrorContainer = MaterialTheme.colorScheme.onErrorContainer

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(bgShape)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = settingsFraction)),
            contentAlignment = Alignment.CenterStart
        ) {
            if (settingsFraction > 0.08f) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = Loc.get("settings_cd", language),
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
                .clip(bgShape)
                .background(errorContainer.copy(alpha = swipeFraction)),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (swipeFraction > 0.08f) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = Loc.get("delete_cd", language),
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
                    .padding(horizontal = 12.dp, vertical = if (compactMode) 6.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showFlags) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(if (compactMode) 32.dp else 40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                    ) {
                        Text(
                            text = config.countryFlag,
                            fontSize = if (compactMode) 16.sp else 20.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = config.name,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1
                    )
                    if (!compactMode) {
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
                                Toast.makeText(context, Loc.get("link_copied", language), Toast.LENGTH_SHORT).show()
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
