package com.perqa.byebox.ui.main.dashboard

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perqa.byebox.MainActivity
import com.perqa.byebox.data.ProxyConfig
import com.perqa.byebox.data.SettingsProfileData
import com.perqa.byebox.ui.main.ConnectionStatus
import com.perqa.byebox.ui.main.Loc
import com.perqa.byebox.ui.main.MainScreenViewModel
import com.perqa.byebox.ui.main.MainUiState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.basicMarquee

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    state: MainUiState,
    viewModel: MainScreenViewModel,
    activeConfig: ProxyConfig?,
    activeProfileName: String,
    activity: MainActivity?,
    onNavigateToManager: () -> Unit,
    onNavigateToProxy: () -> Unit,
    onEditProfile: (SettingsProfileData) -> Unit
) {
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
            activeProfileName = activeProfileName,
            downloadSpeed = state.downloadSpeed,
            uploadSpeed = state.uploadSpeed,
            language = state.language,
            onPingRefresh = { viewModel.testActiveConfigPing() },
            onNavigateToProxy = onNavigateToProxy
        )

        Spacer(modifier = Modifier.height(12.dp))

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

        Spacer(modifier = Modifier.height(16.dp))
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Loc.get("profile_presets", state.language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    IconButton(
                        onClick = onNavigateToManager,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = Loc.get("manage_profiles", state.language),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(modifier = Modifier.width(16.dp))
                    state.profiles.forEach { profile ->
                        val isActive = profile.id == state.activeProfileId
                        val assignedServer = state.configs.find { it.id == profile.assignedConfigId }
                        Card(
                            modifier = Modifier
                                .width(140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .combinedClickable(
                                    onClick = { viewModel.changeActiveProfileId(profile.id) },
                                    onLongClick = { onEditProfile(profile) }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = profile.name,
                                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = assignedServer?.countryFlag ?: "\uD83C\uDFF3\uFE0F",
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = assignedServer?.name ?: Loc.get("current_server", state.language),
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .basicMarquee(iterations = Int.MAX_VALUE),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            fontWeight = FontWeight.Medium
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        QuickActionsCard(
            onBestServer = { viewModel.selectBestConfig() },
            onShare = { activity?.shareActiveConfig() },
            onVpnSettings = { activity?.openSystemVpnSettings() },
            onAddTile = { activity?.requestQuickSettingsTile() },
            language = state.language
        )

        Spacer(
            modifier = Modifier
                .height(130.dp)
                .navigationBarsPadding()
        )
    }
}
