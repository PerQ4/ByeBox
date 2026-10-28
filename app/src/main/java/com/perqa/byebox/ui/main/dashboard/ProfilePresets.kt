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
    isExpanded: Boolean = false,
    onToggleExpanded: () -> Unit = {},
    singleRowPreview: Boolean = false,
    onSelectProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onEditProfile: (SettingsProfileData) -> Unit,
    onManageClick: () -> Unit,
    viewportHeightPx: Float? = null,
    reservedCollapsedAbovePx: Float? = null,
    reservedCollapsedBelowPx: Float? = null,
    reservedCollapsedBottomPx: Float? = null
) {
    val collapsed = !isExpanded
    val singleRowMode = collapsed && singleRowPreview
    var swipingId by remember { mutableStateOf<String?>(null) }
    var swipingDragPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    var contentHeightPx by remember { mutableFloatStateOf(0f) }
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    // Последняя полная высота списка, измеренная вне single-row режима, и флаг,
    // что последнее измерение контента сделано в single-row (т.е. это одна
    // строка). Когда блок выходит из single-row (закрыли TGWS), на первом кадре
    // contentHeightPx всё ещё хранит высоту одной строки — по ней нельзя судить,
    // обрезается ли список, — поэтому оценка canCrop опирается на последнюю
    // полную высоту, пока измерение не догонит.
    var lastFullContentHeightPx by remember { mutableFloatStateOf(0f) }
    var lastMeasuredSingleRow by remember { mutableStateOf(false) }
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
    val dynamicPeekHeight: Dp = remember(
        allReservesMeasured,
        reservedCollapsedAbovePx
    ) {
        if (allReservesMeasured) {
            // Свободное место на экране: экран минус контент над пресетами, сам хедер,
            // снизу TGWS, нижний запас страницы и небольшой зазор
            // (14dp между блоками + 20dp запаса).
            val interAndReservePx = with(density) { 34.dp.toPx() }
            // Нижняя граница = примерно одна строка пресета. ВАЖНО: при
            // подключённом VPN сверху появляется карточка сессии (+~58dp), и
            // свободного места остаётся ~64dp. Если минимум выше этого, список
            // не может сократиться на всю высоту карточки — остаток уходит вниз
            // и панель TGWS съезжает. 56dp позволяет компенсации быть точной,
            // а список всё равно остаётся прокручиваемым (видна строка).
            val minPeekPx = with(density) { 56.dp.toPx() }
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
    // Нельзя опираться на scrollState.maxValue: она зависит от высоты
    // контейнера, а высота контейнера выводится из контента — получался
    // самоусиливающийся цикл (нижний спейсер раздувает контейнер -> maxValue>0
    // -> спейсер остаётся). Сравниваем измеренный контент с peek напрямую: это
    // чистая функция от уже известных величин и не создаёт обратной связи.
    val overflowsCollapsed = collapsed && contentHeightPx > peekHeightPx
    // Сворачивание имеет смысл только когда в свёрнутом виде список реально
    // обрезается (контент выше peek). Если весь список и так помещается
    // (пресетов меньше, чем нужно для обрезания), переключатель не нужен:
    // вместо шеврона в хедере показывается нейтральная полосочка.
    // На кадре выхода из single-row измеренная высота ещё равна высоте одной
    // строки (remeasure происходит в следующем layout) — для свёрнутого
    // состояния берём последнюю полную высоту списка, иначе стрелка мигает,
    // исчезая на пару миллисекунд при каждом закрытии Telegram.
    val effectiveCropHeightPx = if (collapsed && lastMeasuredSingleRow) {
        lastFullContentHeightPx
    } else {
        contentHeightPx
    }
    val canCrop = effectiveCropHeightPx > 0f && effectiveCropHeightPx > peekHeightPx
    // Переключатель показываем, когда сворачивание/разворачивание имеет смысл:
    //  - singleRowPreview (блок сжат до одной строки из-за открытого TGWS) — всегда,
    //    иначе не вернуть блок обратно;
    //  - блок раскрыт — всегда, свернуть можно всегда;
    //  - блок свёрнут — только если контент реально обрезается (canCrop).
    // Важно: для раскрытого состояния НЕ используем canCrop — на первом кадре
    // раскрытия contentHeightPx ещё хранит высоту одной строки, и стрелка
    // мигает, исчезая на пару миллисекунд. Для свёрнутого состояния canCrop
    // тоже устойчив к этому лагу (см. lastFullContentHeightPx выше) — стрелка
    // не пропадает и при закрытии Telegram через тап по нему.
    val isExpandable = when {
        !collapsed -> true
        collapsed && singleRowPreview -> true
        else -> canCrop
    }

    // Внутренний список должен прокручиваться сам, не увлекая за собой страницу:
    // пока список свёрнут и имеет запас прокрутки, «съедаем» остаток жеста,
    // чтобы внешняя вертикальная прокрутка страницы не срабатывала. НО когда
    // внутренний список уже на краю (вверху при движении вниз / внизу при
    // движении вверх), гасить жест нельзя — остаток нужно отдать родительской
    // странице, иначе до карточки Telegram, лежащей под пресетами, не добраться.
    val blockParentScroll = collapsed && scrollState.maxValue > 0f
    val parentScrollBlocker = remember(blockParentScroll) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!blockParentScroll || available.y == 0f) return Offset.Zero
                // available.y > 0 — жест/контент движется вниз (к началу списка),
                // available.y < 0 — вверх (к концу списка).
                val atTopEdge = scrollState.value <= 0 && available.y > 0f
                val atBottomEdge = scrollState.value >= scrollState.maxValue && available.y < 0f
                // На краю внутреннему списку некуда скроллить — пропускаем остаток
                // в родителя. Иначе гасим жест, пока список прокручивается сам.
                return if (atTopEdge || atBottomEdge) {
                    Offset.Zero
                } else {
                    Offset(0f, available.y)
                }
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

    // Если строку удалили свайпом, её id мог остаться в swipingId: тогда
    // последующие свайпы больше не обновляют состояние. Сбрасываем, как только
    // удалённого элемента больше нет в списке.
    LaunchedEffect(displayConfigs) {
        val id = swipingId
        if (id != null && displayConfigs.none { it.id == id }) {
            swipingId = null
            swipingDragPx = 0f
        }
    }

    // При singleRowPreview (например, когда открыт блок Telegram) свёрнутый блок
    // показывает только первую строку — «один элемент», без скролла и пустой зоны.
    val visibleConfigs = if (collapsed && singleRowPreview) displayConfigs.take(1) else displayConfigs

    val activeProfileName = displayConfigs.firstOrNull { it.id == state.activeProfileId }?.name
    val firstConfigActive = displayConfigs.firstOrNull()?.let { it.id == state.activeProfileId } ?: false
    val firstConfigSwiping = displayConfigs.firstOrNull()?.id == swipingId
    // Expressive-радиус берём из тех же настроек, что и остальные карточки,
    // чтобы заголовок и строки списка жили в одной форме.
    val expressiveCorner = if (state.cornerRoundness == "expressive") 32.dp else 28.dp
    val headerBottomCorner = if (collapsed || displayConfigs.isEmpty() || firstConfigActive || firstConfigSwiping) expressiveCorner else 6.dp
    val headerShape = RoundedCornerShape(
        topStart = expressiveCorner,
        topEnd = expressiveCorner,
        bottomStart = headerBottomCorner,
        bottomEnd = headerBottomCorner
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { headerHeightPx = it.size.height.toFloat() }
        ) {
            PresetsHeader(
                title = Loc.get("profile_presets", state.language),
                subtitle = activeProfileName?.let {
                    String.format(Loc.get("presets_active_fmt", state.language), it)
                },
                expanded = isExpanded,
                expandable = isExpandable,
                shape = headerShape,
                onToggleExpanded = onToggleExpanded,
                onManageClick = onManageClick,
                language = state.language
            )
        }

        // Высоту тела списка анимируем ТОЛЬКО на переходе развернуть/свернуть.
        // Изменение peek (например, из-за появления/исчезновения карточки
        // сессии) применяем мгновенно: если анимировать и его, список
        // «доигрывает» прежнюю высоту 320 мс, и панель TGWS под ним плывёт.
        val expandedBodyPx = if (contentHeightPx > 0f) contentHeightPx else peekHeightPx
        val collapsedBodyPx = if (contentHeightPx > 0f) {
            minOf(contentHeightPx, peekHeightPx)
        } else {
            peekHeightPx
        }
        val collapseProgress by animateFloatAsState(
            targetValue = if (collapsed) 1f else 0f,
            // Пружина без отскока: высота тела не выходит за контент. Любой
            // bounce здесь даёт overshoot и лишнюю пустую полосу под списком.
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "presetsCollapse"
        )
        val bodyHeightPx = expandedBodyPx + (collapsedBodyPx - expandedBodyPx) * collapseProgress

        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { bodyHeightPx.toDp() })
                    // verticalScroll держим всегда: он измеряет контент без
                    // ограничения по высоте, поэтому contentHeightPx всегда
                    // равен настоящей высоте списка, а не высоте контейнера.
                    // Когда список помещается целиком, прокрутки нет (maxValue 0).
                    .verticalScroll(scrollState)
                    .nestedScroll(parentScrollBlocker)
            ) {
                // Соседи видят то же «липкое» смещение, что и сама карточка,
                // и догоняют его пружиной — поэтому и скругление, и сдвиг
                // соседей привязаны к липкости, а не к сырому пальцу.
                val presetThresholdPx = with(density) { 140.dp.toPx() }
                val presetDetachStartPx = with(density) { 22.dp.toPx() }
                val presetDetachEndPx = with(density) { 46.dp.toPx() }
                val stickyDragPx = stickySwipeDisplay(
                    raw = swipingDragPx,
                    detachStartPx = presetDetachStartPx,
                    detachEndPx = presetDetachEndPx
                )
                val jellyDragPx by animateFloatAsState(
                    targetValue = stickyDragPx,
                    animationSpec = spring(
                        dampingRatio = 0.45f,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "presetsJellyDrag"
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            contentHeightPx = coords.size.height.toFloat()
                            if (!singleRowMode) {
                                lastFullContentHeightPx = coords.size.height.toFloat()
                                lastMeasuredSingleRow = false
                            } else {
                                lastMeasuredSingleRow = true
                            }
                        },
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    visibleConfigs.forEachIndexed { index, config ->
                        val isActive = config.id == state.activeProfileId
                        val prevIsActive = index > 0 && visibleConfigs[index - 1].id == state.activeProfileId
                        val nextIsActive = index < visibleConfigs.lastIndex && visibleConfigs[index + 1].id == state.activeProfileId
                        val isLast = index == visibleConfigs.lastIndex

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

                        val prevIsSwipingNeighbor = index > 0 && visibleConfigs[index - 1].id == swipingId
                        val nextIsSwipingNeighbor = index < visibleConfigs.lastIndex && visibleConfigs[index + 1].id == swipingId
                        val neighborFollowProgress = smoothStep((kotlin.math.abs(jellyDragPx) / 96f).coerceIn(0f, 1f))
                        val neighborRoundnessProgress = stickySwipeFraction(jellyDragPx, presetThresholdPx)
                        val effectiveTopCorner = if (prevIsSwipingNeighbor) lerp(baseTopCorner, expressiveCorner, neighborRoundnessProgress) else baseTopCorner
                        val effectiveBottomCorner = if (nextIsSwipingNeighbor) lerp(baseBottomCorner, expressiveCorner, neighborRoundnessProgress) else baseBottomCorner
                        val isNeighbor = prevIsSwipingNeighbor || nextIsSwipingNeighbor
                        val neighborOffsetDp = if (isNeighbor) {
                            kotlin.math.sign(jellyDragPx) * 7.dp.value * neighborFollowProgress
                        } else 0f
                        val neighborJelly = if (isNeighbor) neighborFollowProgress else 0f

                        val profile = profiles.find { it.id == config.id }
                        // Ключ по id: при удалении через свайп Compose не должен
                        // переиспользовать состояние (offset) соседней строки —
                        // иначе последующие свайпы «ломаются».
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
                // Нижний запас под скролл добавляем только когда список реально
                // переполняет свёрнутый контейнер (иначе при 1-2 пресетах остаётся
                // большая пустая зона, и блок выглядит разложенным).
                if (collapsed && overflowsCollapsed) {
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

/**
 * Шапка блока пресетов. Обычные скруглённые кнопки (не expressive-формы):
 * иконка в бейдже, отдельная кнопка управления и шеврон раскрытия.
 */
@Composable
private fun PresetsHeader(
    title: String,
    subtitle: String?,
    expanded: Boolean,
    expandable: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    onToggleExpanded: () -> Unit,
    onManageClick: () -> Unit,
    language: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(enabled = expandable) { onToggleExpanded() },
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
            if (expandable) {
                Icon(
                    imageVector = if (expanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = if (expanded) {
                        Loc.get("collapse_cd", language)
                    } else {
                        Loc.get("expand_cd", language)
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
