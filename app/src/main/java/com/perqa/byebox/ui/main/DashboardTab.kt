package com.perqa.byebox.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import com.perqa.byebox.MainActivity
import com.perqa.byebox.data.SettingsProfileData
import com.perqa.byebox.findActivity
import com.perqa.byebox.ui.main.dashboard.DashboardScreen
import com.perqa.byebox.ui.main.dashboard.QuickSwitchEditScreen
import com.perqa.byebox.ui.main.dashboard.QuickSwitchManagerScreen
import androidx.compose.runtime.LaunchedEffect

@Composable
fun DashboardTab(
    state: MainUiState,
    viewModel: MainScreenViewModel,
    onTabSelected: (Int) -> Unit,
    onShowBottomBar: (Boolean) -> Unit
) {
    var activeScreen by remember { mutableStateOf("dashboard") }
    var editingProfile by remember { mutableStateOf<SettingsProfileData?>(null) }
    var editorReferrer by remember { mutableStateOf("manager") }
    var pendingProfileDeleteId by remember { mutableStateOf<String?>(null) }

    val activeConfig = state.configs.find { it.id == state.activeConfigId }
    val activeProfileName = state.profiles.find { it.id == state.activeProfileId }?.name ?: Loc.get("default_profile", state.language)
    val context = LocalContext.current
    val activity = context.findActivity() as? MainActivity

    LaunchedEffect(activeScreen) {
        onShowBottomBar(activeScreen == "dashboard")
    }

    BackHandler(enabled = activeScreen != "dashboard") {
        if (activeScreen == "editor") {
            activeScreen = editorReferrer
            editingProfile = null
        } else if (activeScreen == "manager") {
            activeScreen = "dashboard"
        }
    }

    if (pendingProfileDeleteId != null) {
        val deleteProfileName = state.profiles.find { it.id == pendingProfileDeleteId }?.name ?: ""
        AlertDialog(
            onDismissRequest = { pendingProfileDeleteId = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = Loc.get("delete_profile_confirm_title", state.language),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                val msg = Loc.get("delete_profile_confirm_msg", state.language).replace("{name}", deleteProfileName)
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProfile(pendingProfileDeleteId!!)
                        pendingProfileDeleteId = null
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
                TextButton(onClick = { pendingProfileDeleteId = null }) {
                    Text(
                        text = Loc.get("cancel_btn", state.language),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    when (activeScreen) {
        "dashboard" -> {
            DashboardScreen(
                state = state,
                viewModel = viewModel,
                activeConfig = activeConfig,
                activeProfileName = activeProfileName,
                activity = activity,
                onNavigateToManager = { activeScreen = "manager" },
                onNavigateToProxy = { onTabSelected(1) },
                onEditProfile = { profile ->
                    editingProfile = profile
                    editorReferrer = "dashboard"
                    activeScreen = "editor"
                }
            )
        }
        "manager" -> {
            QuickSwitchManagerScreen(
                profiles = state.profiles,
                activeProfileId = state.activeProfileId,
                configs = state.configs,
                viewModel = viewModel,
                onBack = { activeScreen = "dashboard" },
                onSelect = { viewModel.changeActiveProfileId(it) },
                onAdd = { newProfile -> viewModel.addProfile(newProfile) },
                onDelete = { id ->
                    if (state.confirmRemoveEnabled) {
                        pendingProfileDeleteId = id
                    } else {
                        viewModel.deleteProfile(id)
                    }
                },
                onEditTriggered = { profile ->
                    editingProfile = profile
                    editorReferrer = "manager"
                    activeScreen = "editor"
                },
                onReorder = { viewModel.reorderProfiles(it) },
                state = state
            )
        }
        "editor" -> {
            if (editingProfile != null) {
                QuickSwitchEditScreen(
                    profile = editingProfile!!,
                    configs = state.configs,
                    apps = state.installedApps,
                    state = state,
                    onBack = {
                        editingProfile = null
                        activeScreen = editorReferrer
                    },
                    onSave = { updated ->
                        viewModel.updateProfile(updated)
                        editingProfile = null
                        activeScreen = editorReferrer
                    }
                )
            } else {
                activeScreen = "manager"
            }
        }
    }
}
