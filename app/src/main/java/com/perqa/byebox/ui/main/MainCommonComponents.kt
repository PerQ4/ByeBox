package com.perqa.byebox.ui.main

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perqa.byebox.core.HapticFeedbackUtil
import com.perqa.byebox.core.HapticType
import com.perqa.byebox.findActivity

@Composable
fun PlainDragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(vertical = 10.dp)
            .size(width = 32.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
    )
}

fun interface TactileFeedbackPlayer {
    operator fun invoke(type: HapticType)
    operator fun invoke() {
        invoke(HapticType.LIGHT)
    }
}

@Composable
fun rememberTactileFeedback(scaleFactor: Float = 0.90f): TactileFeedbackPlayer {
    val context = LocalContext.current
    return remember(context, scaleFactor) {
        TactileFeedbackPlayer { type ->
            HapticFeedbackUtil.play(context, type, scaleFactor)
        }
    }
}

fun smoothStep(value: Float): Float {
    val x = value.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
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
    onDismiss: () -> Unit,
    language: String = "ru"
) {
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
                text = Loc.get("app_picker_title", language),
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
                            text = String.format(Loc.get("app_picker_count", language), localSelected.size),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = String.format(Loc.get("app_picker_found", language), filteredApps.size),
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
                            text = Loc.get("app_picker_hide_system", language),
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
                placeholder = { Text(Loc.get("app_picker_search", language)) },
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
                                contentDescription = Loc.get("app_picker_clear", language),
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
                    val ctx = LocalContext.current
                    val iconPainter = remember(app.packageName) {
                        val pm = ctx.packageManager
                        try {
                            val drawable = pm.getApplicationIcon(app.packageName)
                            val bitmap = Bitmap.createBitmap(
                                drawable.intrinsicWidth.coerceAtLeast(1),
                                drawable.intrinsicHeight.coerceAtLeast(1),
                                Bitmap.Config.ARGB_8888
                            )
                            Canvas(bitmap).apply {
                                drawable.setBounds(0, 0, width, height)
                                drawable.draw(this)
                            }
                            BitmapPainter(bitmap.asImageBitmap())
                        } catch (_: Exception) { null }
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
                Text(Loc.get("app_picker_save", language), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun SettingsChoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    top: Boolean = false,
    bottom: Boolean = false,
    scaleFactor: Float = 0.90f,
    cornerRoundness: String = "expressive",
    onClick: () -> Unit
) {
    SettingsRowSurface(top = top, bottom = bottom, selected = selected, scaleFactor = scaleFactor, cornerRoundness = cornerRoundness, onClick = onClick) {
        if (icon != null) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }
        SettingsRowText(title = title, subtitle = subtitle, modifier = Modifier.weight(1f))
        RadioButton(
            selected = selected,
            onClick = null
        )
    }
}

@Composable
fun SettingsSwitchRow(
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
fun SettingsActionRow(
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
fun SettingsInputRow(
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
fun SettingsRowSurface(
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
fun SettingsRowText(
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
fun SettingsGroup(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
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
fun SettingsHealthRow(
    value: String,
    onValueChange: (String) -> Unit,
    onTest: () -> Unit,
    scaleFactor: Float = 0.90f,
    cornerRoundness: String = "expressive",
    top: Boolean = false,
    bottom: Boolean = false,
    language: String = "ru"
) {
    SettingsRowSurface(top = top, bottom = bottom, scaleFactor = scaleFactor, cornerRoundness = cornerRoundness) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(Loc.get("health_url_label", language)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp)
        )
        IconButton(onClick = onTest) {
            Icon(Icons.Default.Search, contentDescription = Loc.get("health_check_cd", language))
        }
    }
}
