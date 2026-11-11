package com.perqa.byebox.ui.main.dashboard

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.perqa.byebox.ui.main.smoothStep
import com.perqa.byebox.ui.main.stickySwipeDisplay
import com.perqa.byebox.ui.main.stickySwipeFraction

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
    onManageClick: () -> Unit
) {
    var swipingId by remember { mutableStateOf<String?>(null) }
    var swipingDragPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

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

    // Если строку удалили свайпом, её id мог остаться в swipingId.
    LaunchedEffect(displayConfigs) {
        val id = swipingId
        if (id != null && displayConfigs.none { it.id == id }) {
            swipingId = null
            swipingDragPx = 0f
        }
    }

    val activeProfileName = displayConfigs.firstOrNull { it.id == state.activeProfileId }?.name
    val firstConfigActive = displayConfigs.firstOrNull()?.let { it.id == state.activeProfileId } ?: false
    val firstConfigSwiping = displayConfigs.firstOrNull()?.id == swipingId
    val expressiveCorner = if (state.cornerRoundness == "expressive") 32.dp else 28.dp
    val headerBottomCorner = if (
        displayConfigs.isEmpty() || firstConfigActive || firstConfigSwiping
    ) {
        expressiveCorner
    } else {
        6.dp
    }
    val headerShape = RoundedCornerShape(
        topStart = expressiveCorner,
        topEnd = expressiveCorner,
        bottomStart = headerBottomCorner,
        bottomEnd = headerBottomCorner
    )

    // Соседи видят то же «липкое» смещение, что и сама карточка.
    val presetThresholdPx = with(density) { 140.dp.toPx() }
    val presetDetachStartPx = with(density) { 64.dp.toPx() }
    val presetDetachEndPx = with(density) { 96.dp.toPx() }
    val stickyDragPx = stickySwipeDisplay(
        raw = swipingDragPx,
        detachStartPx = presetDetachStartPx,
        detachEndPx = presetDetachEndPx
    )
    val jellyDragPx by animateFloatAsState(
        targetValue = stickyDragPx,
        animationSpec = if (swipingId != null) {
            spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessHigh)
        } else {
            spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)
        },
        label = "presetsJellyDrag"
    )
    val swipingIndex = if (swipingId == null) {
        -1
    } else {
        displayConfigs.indexOfFirst { it.id == swipingId }
    }

    // Статичный блок: все пресеты всегда видны, без peek-высоты,
    // внутреннего скролла и авто-сворачивания.
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PresetsHeader(
            title = Loc.get("profile_presets", state.language),
            subtitle = activeProfileName?.let {
                String.format(Loc.get("presets_active_fmt", state.language), it)
            },
            shape = headerShape,
            onManageClick = onManageClick,
            language = state.language
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            displayConfigs.forEachIndexed { index, config ->
                val isActive = config.id == state.activeProfileId
                val prevIsActive = index > 0 && displayConfigs[index - 1].id == state.activeProfileId
                val nextIsActive = index < displayConfigs.lastIndex && displayConfigs[index + 1].id == state.activeProfileId
                val isLast = index == displayConfigs.lastIndex

                val baseTopCorner = when {
                    isActive -> expressiveCorner
                    prevIsActive -> expressiveCorner
                    else -> 6.dp
                }
                val baseBottomCorner = when {
                    isActive -> expressiveCorner
                    nextIsActive -> expressiveCorner
                    isLast -> expressiveCorner
                    else -> 6.dp
                }

                val prevIsSwipingNeighbor = index > 0 && displayConfigs[index - 1].id == swipingId
                val nextIsSwipingNeighbor = index < displayConfigs.lastIndex && displayConfigs[index + 1].id == swipingId
                val neighborFollowProgress = smoothStep((kotlin.math.abs(jellyDragPx) / 96f).coerceIn(0f, 1f))
                val neighborRoundnessProgress = stickySwipeFraction(jellyDragPx, presetThresholdPx)
                val effectiveTopCorner = if (prevIsSwipingNeighbor) {
                    lerp(baseTopCorner, expressiveCorner, neighborRoundnessProgress)
                } else {
                    baseTopCorner
                }
                val effectiveBottomCorner = if (nextIsSwipingNeighbor) {
                    lerp(baseBottomCorner, expressiveCorner, neighborRoundnessProgress)
                } else {
                    baseBottomCorner
                }
                // Цепочка: первый сосед идёт на полную, второй — на 45%.
                val neighborLevel = if (swipingIndex >= 0) {
                    kotlin.math.abs(index - swipingIndex)
                } else {
                    Int.MAX_VALUE
                }
                val isNeighbor = neighborLevel in 1..2
                val levelFactor = when (neighborLevel) {
                    1 -> 1f
                    2 -> 0.45f
                    else -> 0f
                }
                val neighborOffsetDp = if (isNeighbor) {
                    kotlin.math.sign(jellyDragPx) *
                        8.dp.value *
                        neighborFollowProgress *
                        levelFactor
                } else 0f
                val neighborJelly = if (isNeighbor) {
                    neighborFollowProgress * levelFactor
                } else 0f

                val profile = profiles.find { it.id == config.id }
                key(config.id) {
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
                        neighborJelly = neighborJelly,
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
        }
    }
}

/**
 * Шапка блока пресетов. Обычные скруглённые кнопки (не expressive-формы):
 * иконка в бейдже, отдельная кнопка управления и шеврон раскрытия.
 */
@Composable
private fun PresetsHeader(
    title: String,
    subtitle: String?,
    shape: androidx.compose.ui.graphics.Shape,
    onManageClick: () -> Unit,
    language: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = onManageClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = Loc.get("manage_cd", language),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
