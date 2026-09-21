package com.perqa.byebox.ui.main.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.perqa.byebox.data.ProxyConfig
import com.perqa.byebox.data.SettingsProfileData
import com.perqa.byebox.ui.main.Loc
import com.perqa.byebox.ui.main.MainUiState
import com.perqa.byebox.ui.main.ServerItemCard
import com.perqa.byebox.ui.main.SourceGroupCard
import com.perqa.byebox.ui.main.smoothStep

/**
 * Profile preset block that reuses the subscription group look as-is: a distinct
 * rounded header (SourceGroupCard) glued to profile rows (ServerItemCard). It
 * keeps the same expand/fade collapse, neighbor swipe-shift, settings-swipe-to-edit
 * and delete-swipe gestures the proxy list has. The manage gear opens the manager.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ProfilePresetsCard(
    state: MainUiState,
    onSelectProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onEditProfile: (SettingsProfileData) -> Unit,
    onManageClick: () -> Unit,
    viewportHeightPx: Float? = null,
    reservedCollapsedAbovePx: Float? = null,
    reservedCollapsedBelowPx: Float? = null,
    reservedCollapsedBottomPx: Float? = null
) {
    var collapsed by remember { mutableStateOf(true) }
    var swipingId by remember { mutableStateOf<String?>(null) }
    var swipingDragPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    var contentHeightPx by remember { mutableFloatStateOf(0f) }
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    val bottomPad = 64.dp
    val bottomPadPx = with(density) { bottomPad.toPx() }
    val staticPeekHeight = 168.dp
    // Высота свёрнутого списка считается ОДИН раз — когда измерены все резервы страницы —
    // и затем замораживается. Перекомпоновка нижележащих блоков (например, раскрытие TGWS)
    // не должна менять высоту свёрнутого списка пресетов, иначе он «разворачивается» сам.
    val allReservesMeasured = viewportHeightPx != null &&
        (reservedCollapsedAbovePx ?: 0f) > 0f &&
        (reservedCollapsedBelowPx ?: 0f) > 0f &&
        (reservedCollapsedBottomPx ?: 0f) > 0f &&
        headerHeightPx > 0f
    val dynamicPeekHeight: Dp = remember(allReservesMeasured) {
        if (allReservesMeasured) {
            // Свободное место на экране: экран минус контент над пресетами, сам хедер,
            // снизу TGWS, нижний запас страницы и небольшой зазор
            // (14dp между блоками + 20dp запаса).
            val interAndReservePx = with(density) { 34.dp.toPx() }
            val minPeekPx = with(density) { 110.dp.toPx() }
            val maxPeekPx = with(density) { 360.dp.toPx() }
            val availablePx = viewportHeightPx!! -
                reservedCollapsedAbovePx!! -
                headerHeightPx -
                reservedCollapsedBelowPx!! -
                reservedCollapsedBottomPx!! -
                interAndReservePx
            with(density) { availablePx.coerceIn(minPeekPx, maxPeekPx).toDp() }
        } else {
            staticPeekHeight
        }
    }
    val peekHeight = dynamicPeekHeight
    val peekHeightPx = with(density) { peekHeight.toPx() }
    val overflowsCollapsed = collapsed && scrollState.maxValue > 0f

    // Внутренний список должен прокручиваться сам, не увлекая за собой страницу:
    // пока список свёрнут и имеет запас прокрутки, «съедаем» остаток жеста,
    // чтобы внешняя вертикальная прокрутка страницы не срабатывала.
    val blockParentScroll = collapsed && scrollState.maxValue > 0f
    val parentScrollBlocker = remember(blockParentScroll) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset =
                if (blockParentScroll && available.y != 0f) {
                    Offset(0f, available.y)
                } else {
                    Offset.Zero
                }
        }
    }

    val displayConfigs = remember(state.profiles, state.configs, state.language) {
        state.profiles.map { profile ->
            when (profile.assignedConfigId) {
                "FASTEST" -> ProxyConfig(
                    id = profile.id,
                    name = profile.localizedProfileName(state.language),
                    protocol = "",
                    address = "",
                    port = 0,
                    uuid = "",
                    ping = null,
                    countryFlag = "\u26A1"
                )
                "LAST_ACTIVE" -> ProxyConfig(
                    id = profile.id,
                    name = profile.localizedProfileName(state.language),
                    protocol = "",
                    address = "",
                    port = 0,
                    uuid = "",
                    ping = null,
                    countryFlag = "\uD83D\uDD04"
                )
                else -> {
                    val server = state.configs.find { it.id == profile.assignedConfigId }
                    ProxyConfig(
                        id = profile.id,
                        name = profile.localizedProfileName(state.language),
                        protocol = server?.protocol ?: "",
                        address = server?.address ?: "",
                        port = server?.port ?: 0,
                        uuid = server?.uuid ?: "",
                        ping = server?.ping,
                        countryFlag = server?.countryFlag ?: "\uD83C\uDFF3\uFE0F"
                    )
                }
            }
        }
    }
    val profiles = state.profiles

    val firstConfigActive = displayConfigs.firstOrNull()?.let { it.id == state.activeProfileId } ?: false
    val firstConfigSwiping = displayConfigs.firstOrNull()?.id == swipingId
    val headerBottomCorner = if (collapsed || displayConfigs.isEmpty() || firstConfigActive || firstConfigSwiping) 28.dp else 6.dp
    val headerShape = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 28.dp,
        bottomStart = headerBottomCorner,
        bottomEnd = headerBottomCorner
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { headerHeightPx = it.size.height.toFloat() }
        ) {
            SourceGroupCard(
                sourceName = Loc.get("profile_presets", state.language),
                source = null,
                configs = displayConfigs,
                activeConfigId = state.activeProfileId,
                onSelect = onSelectProfile,
                onDelete = onDeleteProfile,
                onRefreshSource = {},
                onRenameSource = { _, _ -> },
                onDeleteSource = {},
                onPingSource = {},
                expanded = !collapsed,
                onToggleExpanded = { collapsed = !collapsed },
                showConfigs = false,
                shape = headerShape,
                language = state.language,
                onInfo = onManageClick,
                headerActionIcon = Icons.Default.Tune,
                showNodeCount = false,
                showAvgPing = false
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = tween(320))
        ) {
            Column(
                modifier = if (collapsed) {
                    Modifier
                        .fillMaxWidth()
                        .height(peekHeight)
                        .verticalScroll(scrollState)
                        .nestedScroll(parentScrollBlocker)
                } else {
                    Modifier
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { contentHeightPx = it.size.height.toFloat() },
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    displayConfigs.forEachIndexed { index, config ->
                        val isActive = config.id == state.activeProfileId
                        val prevIsActive = index > 0 && displayConfigs[index - 1].id == state.activeProfileId
                        val nextIsActive = index < displayConfigs.lastIndex && displayConfigs[index + 1].id == state.activeProfileId
                        val isLast = index == displayConfigs.lastIndex

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

                        val prevIsSwipingNeighbor = index > 0 && displayConfigs[index - 1].id == swipingId
                        val nextIsSwipingNeighbor = index < displayConfigs.lastIndex && displayConfigs[index + 1].id == swipingId
                        val neighborFollowProgress = smoothStep((kotlin.math.abs(swipingDragPx) / 96f).coerceIn(0f, 1f))
                        val neighborRoundnessProgress = smoothStep((kotlin.math.abs(swipingDragPx) / 140f).coerceIn(0f, 1f))
                        val effectiveTopCorner = if (prevIsSwipingNeighbor) lerp(baseTopCorner, 28.dp, neighborRoundnessProgress) else baseTopCorner
                        val effectiveBottomCorner = if (nextIsSwipingNeighbor) lerp(baseBottomCorner, 28.dp, neighborRoundnessProgress) else baseBottomCorner
                        val neighborOffsetDp = if (prevIsSwipingNeighbor || nextIsSwipingNeighbor) {
                            kotlin.math.sign(swipingDragPx) * 3.dp.value * neighborFollowProgress
                        } else 0f

                        val profile = profiles.find { it.id == config.id }
                        ServerItemCard(
                            config = config,
                            isActive = isActive,
                            isPinging = false,
                            onSelect = { onSelectProfile(config.id) },
                            onDelete = { onDeleteProfile(config.id) },
                            onOpenSettings = { profile?.let(onEditProfile) },
                            topCorner = effectiveTopCorner,
                            bottomCorner = effectiveBottomCorner,
                            neighborOffsetDp = neighborOffsetDp.dp,
                            language = state.language,
                            statusText = when (profile?.assignedConfigId) {
                                "FASTEST" -> "\u26A1 ${Loc.get("fastest_server", state.language)}"
                                "LAST_ACTIVE" -> "\uD83D\uDD04 ${Loc.get("last_active_server", state.language)}"
                                else -> null
                            },
                            onSwipeOffsetChanged = { offset ->
                                if (swipingId == config.id) {
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
                                    swipingId = config.id
                                } else if (swipingId == config.id) {
                                    swipingId = null
                                    swipingDragPx = 0f
                                }
                            }
                        )
                    }
                }
                if (collapsed) {
                    Spacer(modifier = Modifier.height(bottomPad))
                }
            }

            if (overflowsCollapsed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                1f to MaterialTheme.colorScheme.background
                            )
                        )
                )

                val contentH = (contentHeightPx + bottomPadPx).coerceAtLeast(1f)
                val progress by remember {
                    derivedStateOf {
                        if (scrollState.maxValue > 0) {
                            scrollState.value.toFloat() / scrollState.maxValue.toFloat()
                        } else 0f
                    }
                }
                val thumbH = (peekHeightPx * peekHeightPx / contentH).coerceAtLeast(with(density) { 16.dp.toPx() })
                val thumbY = (peekHeightPx - thumbH) * progress.coerceIn(0f, 1f)
                val thumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .height(peekHeight)
                        .padding(end = 6.dp)
                        .width(3.dp)
                ) {
                    drawRoundRect(
                        color = thumbColor,
                        topLeft = Offset(0f, thumbY),
                        size = Size(size.width, thumbH),
                        cornerRadius = CornerRadius(1.5.dp.toPx())
                    )
                }
            }
        }
    }
}