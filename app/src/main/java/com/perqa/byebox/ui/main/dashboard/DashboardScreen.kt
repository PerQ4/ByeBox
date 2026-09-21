package com.perqa.byebox.ui.main.dashboard

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.perqa.byebox.MainActivity
import com.perqa.byebox.data.ProxyConfig
import com.perqa.byebox.data.SettingsProfileData
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .onGloballyPositioned { viewportHeightPx = it.size.height.toFloat() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showVpn) {
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

                    StatusPillBar(
                        status = state.connectionStatus,
                        downloadSpeed = state.downloadSpeed,
                        uploadSpeed = state.uploadSpeed,
                        dnsServer = state.dnsServer,
                        language = state.language,
                        cornerRoundness = state.cornerRoundness
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

            ProfilePresetsCard(
                state = state,
                onSelectProfile = { viewModel.changeActiveProfileId(it) },
                onDeleteProfile = onDeleteProfile,
                onEditProfile = onEditProfile,
                onManageClick = onNavigateToManager,
                viewportHeightPx = viewportHeightPx,
                reservedCollapsedAbovePx = if (aboveContentHeightPx > 0f) aboveContentHeightPx else null,
                reservedCollapsedBelowPx = if (showTelegram) tgwsCollapsedHeightPx else null,
                reservedCollapsedBottomPx = if (bottomReserveHeightPx > 0f) bottomReserveHeightPx else null
            )

            Spacer(modifier = Modifier.height(14.dp))
        }

        if (showTelegram) {
            if (fullTgws) {
                TgwsFullPage(
                    language = state.language,
                    cornerRoundness = state.cornerRoundness,
                    viewModel = viewModel
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned {
                            tgwsCollapsedHeightPx = kotlin.math.min(
                                tgwsCollapsedHeightPx,
                                it.size.height.toFloat()
                            ).let { current ->
                                if (current <= 0f) it.size.height.toFloat() else current
                            }
                        }
                ) {
                    TelegramProxyCard(
                        language = state.language,
                        cornerRoundness = state.cornerRoundness
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
