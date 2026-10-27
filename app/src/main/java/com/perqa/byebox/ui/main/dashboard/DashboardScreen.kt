package com.perqa.byebox.ui.main.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.perqa.byebox.MainActivity
import com.perqa.byebox.data.ProxyConfig
import com.perqa.byebox.data.SettingsProfileData
import com.perqa.byebox.theme.ByeBoxExpressiveTheme
import com.perqa.byebox.theme.LocalByeBoxMotion
import com.perqa.byebox.ui.main.ConnectionStatus
import com.perqa.byebox.ui.main.Loc
import com.perqa.byebox.ui.main.MainScreenViewModel
import com.perqa.byebox.ui.main.MainUiState

@Composable
fun DashboardScreen(
    state: MainUiState,
    viewModel: MainScreenViewModel,
    activeConfig: ProxyConfig?,
    activity: MainActivity?,
    onNavigateToManager: () -> Unit,
    onNavigateToProxy: () -> Unit,
    onEditProfile: (SettingsProfileData) -> Unit,
    onDeleteProfile: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val appMode by viewModel.appMode.collectAsStateWithLifecycle()
    val showVpn = appMode != "tgws"
    val showTelegram = appMode != "vpn"
    val fullTgws = appMode == "tgws"

    var viewportHeightPx by remember { mutableFloatStateOf(0f) }
    var tgwsCollapsedHeightPx by remember { mutableFloatStateOf(0f) }
    var aboveContentHeightPx by remember { mutableFloatStateOf(0f) }
    var bottomReserveHeightPx by remember { mutableFloatStateOf(0f) }
    // Пресеты и TGWS-карточка не должны быть развёрнуты одновременно: оба блока
    // занимают много места, поэтому разворот одного сворачивает другой.
    var presetsExpanded by remember { mutableStateOf(false) }
    var tgwsExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // onGloballyPositioned обязан стоять ДО verticalScroll. Если поставить
            // его после, он измеряет высоту прокручиваемого КОНТЕНТА, а не
            // вьюпорта: viewportHeightPx начинает «дышать» вместе с содержимым
            // (в логах 2879 -> 2779 без изменения экрана), и высота списка
            // пресетов уезжает — панель TGWS плавает.
            .onGloballyPositioned { viewportHeightPx = it.size.height.toFloat() }
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showVpn) {
            ByeBoxExpressiveTheme {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { aboveContentHeightPx = it.size.height.toFloat() }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Трафик читается в изолированном composable, поэтому тик раз
                        // в секунду перекомпоновывает только индикатор и карточку
                        // сессии, а не весь экран.
                        LiveTrafficStatus(
                            state = state,
                            viewModel = viewModel
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        ConnectionButton(
                            status = state.connectionStatus,
                            pulseEnabled = state.pulseEnabled,
                            language = state.language,
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

                        Spacer(modifier = Modifier.height(18.dp))

                        if (activeConfig != null) {
                            CockpitServerCard(
                                activeConfig = activeConfig,
                                onPingRefresh = { viewModel.testActiveConfigPing() },
                                onNavigateToProxy = onNavigateToProxy,
                                language = state.language,
                                cornerRoundness = state.cornerRoundness
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            ByeBoxExpressiveTheme {
                ProfilePresetsCard(
                    state = state,
                    isExpanded = presetsExpanded,
                    onToggleExpanded = {
                        presetsExpanded = !presetsExpanded
                        if (presetsExpanded) tgwsExpanded = false
                    },
                    singleRowPreview = tgwsExpanded,
                    onSelectProfile = { viewModel.changeActiveProfileId(it) },
                    onDeleteProfile = onDeleteProfile,
                    onEditProfile = onEditProfile,
                    onManageClick = onNavigateToManager,
                    viewportHeightPx = viewportHeightPx,
                    reservedCollapsedAbovePx = if (aboveContentHeightPx > 0f) aboveContentHeightPx else null,
                    reservedCollapsedBelowPx = if (showTelegram) tgwsCollapsedHeightPx else null,
                    reservedCollapsedBottomPx = if (bottomReserveHeightPx > 0f) bottomReserveHeightPx else null
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (showTelegram) {
            if (fullTgws) {
                ByeBoxExpressiveTheme {
                    TgwsFullPage(
                        language = state.language,
                        cornerRoundness = state.cornerRoundness,
                        viewModel = viewModel
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned {
                            // Резерв должен отражать высоту именно СВЁРНУТОЙ
                            // карточки. Раньше min() навсегда фиксировал первое
                            // (свёрнутое) значение, и если свёрнутая высота
                            // менялась (язык, длина статуса), резерв оставался
                            // устаревшим. В развёрнутом виде не измеряем, иначе
                            // в резерв уйдёт раздутая высота.
                            if (!tgwsExpanded) {
                                tgwsCollapsedHeightPx = it.size.height.toFloat()
                            }
                        }
                ) {
                    TelegramProxyCard(
                        language = state.language,
                        cornerRoundness = state.cornerRoundness,
                        isExpanded = tgwsExpanded,
                        onToggle = {
                            tgwsExpanded = !tgwsExpanded
                            if (tgwsExpanded) presetsExpanded = false
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { bottomReserveHeightPx = it.size.height.toFloat() }
        ) {
            Spacer(
                modifier = Modifier
                    .height(130.dp)
                    .navigationBarsPadding()
            )
        }
    }
}

/**
 * Индикатор трафика и карточка длительности сессии. Единственная точка, где
 * читается [MainScreenViewModel.liveTraffic]: тик раз в секунду обновляет только
 * этот участок композиции, не инвалидируя весь DashboardScreen.
 */
@Composable
private fun LiveTrafficStatus(
    state: MainUiState,
    viewModel: MainScreenViewModel
) {
    val liveTraffic by viewModel.liveTraffic.collectAsStateWithLifecycle()
    val motion = LocalByeBoxMotion.current

    StatusPillBar(
        status = state.connectionStatus,
        downloadSpeed = liveTraffic.downloadSpeed,
        uploadSpeed = liveTraffic.uploadSpeed,
        dnsServer = state.dnsServer,
        language = state.language,
        cornerRoundness = state.cornerRoundness,
        sessionUpload = liveTraffic.sessionUpload,
        sessionDownload = liveTraffic.sessionDownload
    )

    if (state.connectionStatus == ConnectionStatus.CONNECTED) {
        // Пружинное появление карточки сессии. Специально не анимируем высоту:
        // aboveContentHeightPx должен меняться так же мгновенно, как раньше,
        // иначе компенсация панели пресетов начинает «догонять» анимацию.
        var appeared by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { appeared = true }
        val appearAlpha by animateFloatAsState(
            targetValue = if (appeared) 1f else 0f,
            animationSpec = motion.effectsDefault(),
            label = "sessionAppearAlpha"
        )
        val appearScale by animateFloatAsState(
            targetValue = if (appeared) 1f else 0.94f,
            animationSpec = motion.spatialDefault(),
            label = "sessionAppearScale"
        )
        Column(
            modifier = Modifier.graphicsLayer {
                alpha = appearAlpha
                scaleX = appearScale
                scaleY = appearScale
            }
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            SessionDurationCard(
                duration = liveTraffic.sessionDuration,
                language = state.language,
                cornerRoundness = state.cornerRoundness
            )
        }
    }
}
