package com.perqa.byebox.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay

fun tabMeta(id: String, language: String): Pair<String, ImageVector> = when (id) {
    "main" -> Loc.get("title_main", language) to Icons.Default.Refresh
    "proxies" -> Loc.get("title_proxies", language) to Icons.Default.List
    else -> Loc.get("title_settings", language) to Icons.Default.Settings
}

fun isTabVisible(id: String, mode: String): Boolean = when (mode) {
    "tgws" -> id != "proxies"
    else -> true
}

@Composable
fun MainTabBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    scaleFactor: Float = 0.90f,
    glassmorphic: Boolean = true,
    maxBlurEnabled: Boolean = true,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    language: String = "ru",
    order: List<String> = listOf("main", "proxies", "settings"),
    modifier: Modifier = Modifier
) {
    val tactileFeedback = rememberTactileFeedback(scaleFactor)
    val containerColor = if (glassmorphic) {
        val alpha = if (maxBlurEnabled) 0.62f else 0.9f
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val containerShape = RoundedCornerShape(Dimens.tabBarShape)

    // Per-tab target widths: the active tab grows to fit icon+label, inactive
    // tabs stay at a fixed compact width. Widths animate to these final values
    // (instead of tracking content) which removes the accordion effect.
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val activeWidths = remember(order, language, density) {
        order.associateWith { tabId ->
            val label = tabMeta(tabId, language).first
            val labelLayout = textMeasurer.measure(
                text = AnnotatedString(label),
                style = TextStyle(
                    fontSize = Dimens.tabLabelFontSize,
                    fontWeight = FontWeight.Black
                ),
                softWrap = false,
                maxLines = 1,
                constraints = Constraints(maxWidth = 10000)
            )
            with(density) { labelLayout.size.width.toDp() }
                .let { labelDp ->
                    (Dimens.tabIconActive + Dimens.tabLabelPadding + labelDp * 1.2f + Dimens.tabPillPaddingH + Dimens.tabPillPaddingH)
                        .coerceAtLeast(Dimens.tabInactiveWidth)
                }
        }
    }
    val containerTarget = order.fold(Dimens.tabPillInsetV) { acc, id ->
        acc + (if (id == selectedTab) activeWidths[id] ?: Dimens.tabInactiveWidth else Dimens.tabInactiveWidth)
    } + Dimens.tabPillInsetV
    val containerWidth by animateDpAsState(
        targetValue = containerTarget,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "dockWidth"
    )

    Box(
        modifier = modifier
            .width(containerWidth)
            .height(Dimens.tabBarHeight)
            .then(
                if (glassmorphic && maxBlurEnabled) {
                    Modifier
                } else {
                    Modifier.shadow(
                        elevation = if (glassmorphic) 6.dp else 14.dp,
                        shape = containerShape,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.18f),
                        spotColor = Color.Black.copy(alpha = 0.28f)
                    )
                }
            )
    ) {
        // Blurred background layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(containerShape)
                .background(containerColor)
                .then(
                    if (glassmorphic && maxBlurEnabled && hazeState != null) {
                        Modifier.hazeChild(
                            state = hazeState,
                            shape = containerShape,
                            style = HazeDefaults.style(
                                backgroundColor = Color.Transparent,
                                blurRadius = 32.dp,
                                noiseFactor = 0.05f
                            )
                        )
                    } else {
                        Modifier
                    }
                )
                .then(
                    if (glassmorphic && !maxBlurEnabled) {
                        Modifier.border(
                            BorderStroke(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                                    )
                                )
                            ),
                            containerShape
                        )
                    } else Modifier
                )
        )

        // Foreground content layer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.tabBarHeight)
                .padding(vertical = Dimens.tabPillInsetV),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = order.map { tabMeta(it, language) }

            tabs.forEachIndexed { index, tab ->
                val id = order[index]
                val active = selectedTab == id
                val activeContainer = when (index) {
                    0 -> MaterialTheme.colorScheme.primaryContainer
                    1 -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.tertiaryContainer
                }
                val activeOnContainer = when (index) {
                    0 -> MaterialTheme.colorScheme.onPrimaryContainer
                    1 -> MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.onTertiaryContainer
                }
                val activeContentColor by animateColorAsState(
                    targetValue = if (active) activeOnContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                    label = "tabContent"
                )
                val activeBgColor by animateColorAsState(
                    targetValue = if (active) activeContainer else Color.Transparent,
                    label = "tabBg"
                )
                val pillWidth by animateDpAsState(
                    targetValue = if (active) activeWidths[id] ?: Dimens.tabInactiveWidth else Dimens.tabInactiveWidth,
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                    label = "pillWidth"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(Dimens.tabHeight)
                        .width(pillWidth)
                        .clip(RoundedCornerShape(Dimens.tabBarShape))
                        .background(activeBgColor)
                        .clickable {
                            if (!active) {
                                tactileFeedback()
                            }
                            onTabSelected(id)
                        }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = Dimens.tabPillPaddingH)
                    ) {
                        Icon(
                            imageVector = tab.second,
                            contentDescription = tab.first,
                            tint = activeContentColor,
                            modifier = Modifier.size(if (active) Dimens.tabIconActive else Dimens.tabIconInactive)
                        )
                        AnimatedVisibility(visible = active) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(modifier = Modifier.width(Dimens.tabLabelPadding))
                                Text(
                                    text = tab.first,
                                    fontSize = Dimens.tabLabelFontSize,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Black,
                                    color = activeContentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CopyIcon(modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val strokePx = 1.5.dp.toPx()
        // Back card outline
        drawRoundRect(
            color = tint.copy(alpha = 0.5f),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.1f),
            size = androidx.compose.ui.geometry.Size(w * 0.55f, h * 0.55f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            style = Stroke(width = strokePx)
        )
        // Front card outline
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.35f),
            size = androidx.compose.ui.geometry.Size(w * 0.55f, h * 0.55f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            style = Stroke(width = strokePx)
        )
    }
}

@Composable
fun ImportBubble(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    index: Int,
    scaleFactor: Float = 0.90f,
    glassmorphic: Boolean = true,
    maxBlurEnabled: Boolean = true
) {
    val tactileFeedback = rememberTactileFeedback(scaleFactor)
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val delayTime = when (index) {
            2 -> 0L
            1 -> 30L
            else -> 60L
        }
        delay(delayTime)
        visible = true
    }

    val animatedOffsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else {
            when (index) {
                2 -> 40.dp
                1 -> 48.dp
                else -> 96.dp
            }
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bubbleOffsetY"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bubbleAlpha"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scalePressed by animateFloatAsState(
        targetValue = if (isPressed) scaleFactor else 1f,
        label = "bubbleScalePressed"
    )

    val density = LocalDensity.current
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val contentColor = MaterialTheme.colorScheme.onSurface
    val bubbleShape = RoundedCornerShape(Dimens.bubbleShape)

    val finalModifier = Modifier
        .graphicsLayer {
            alpha = animatedAlpha
            translationY = with(density) { animatedOffsetY.toPx() }
            scaleX = scalePressed
            scaleY = scalePressed
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                tactileFeedback()
                onClick()
            }
        )

    Surface(
        modifier = finalModifier,
        shape = bubbleShape,
        color = containerColor,
        border = if (glassmorphic && !maxBlurEnabled) {
            BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                    )
                )
            )
        } else null,
        tonalElevation = if (glassmorphic && !maxBlurEnabled) 4.dp else 0.dp,
        shadowElevation = if (glassmorphic && !maxBlurEnabled) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
