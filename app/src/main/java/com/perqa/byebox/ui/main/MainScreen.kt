package com.perqa.byebox.ui.main

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.perqa.byebox.MainActivity
import com.perqa.byebox.findActivity
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
            var selectedTab by remember { mutableIntStateOf(0) }
            var showBottomBar by remember { mutableStateOf(true) }
            var showImportDialog by remember { mutableStateOf(false) }
            var speedDialExpanded by remember { mutableStateOf(false) }

            LaunchedEffect(selectedTab) {
                showBottomBar = true
            }

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
                            androidx.compose.animation.AnimatedVisibility(
                                visible = selectedTab == 0,
                                enter = fadeIn(animationSpec = tween(300)),
                                exit = fadeOut(animationSpec = tween(300))
                            ) {
                                DashboardTab(
                                    state = state,
                                    viewModel = viewModel,
                                    onTabSelected = { selectedTab = it },
                                    onShowBottomBar = { showBottomBar = it }
                                )
                            }
                            androidx.compose.animation.AnimatedVisibility(
                                visible = selectedTab == 1,
                                enter = fadeIn(animationSpec = tween(300)),
                                exit = fadeOut(animationSpec = tween(300))
                            ) {
                                ProxyTab(
                                    state = state,
                                    viewModel = viewModel
                                )
                            }
                            androidx.compose.animation.AnimatedVisibility(
                                visible = selectedTab == 2,
                                enter = fadeIn(animationSpec = tween(300)),
                                exit = fadeOut(animationSpec = tween(300))
                            ) {
                                SettingsTab(
                                    state = state,
                                    viewModel = viewModel,
                                    onShowBottomBar = { showBottomBar = it }
                                )
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
                if (showBottomBar) {
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

                        // FAB is hidden on Settings tab — TabBar centers naturally
                        androidx.compose.animation.AnimatedVisibility(
                            visible = selectedTab != 2,
                            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn() + androidx.compose.animation.expandHorizontally(
                                expandFrom = Alignment.Start,
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                )
                            ),
                            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut() + androidx.compose.animation.shrinkHorizontally(
                                shrinkTowards = Alignment.Start,
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                )
                            )
                        ) {
                            Row {
                                Spacer(modifier = Modifier.width(10.dp))
                                FloatingContextAction(
                                    selectedTab = selectedTab,
                                    scaleFactor = state.tapImpactScale,
                                    cornerRoundness = state.cornerRoundness,
                                    onClick = {
                                        when (selectedTab) {
                                            0 -> viewModel.selectBestConfig()
                                            1 -> speedDialExpanded = !speedDialExpanded
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
    }
}
