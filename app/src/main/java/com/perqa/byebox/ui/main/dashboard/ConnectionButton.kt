package com.perqa.byebox.ui.main.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perqa.byebox.theme.LocalByeBoxMotion
import com.perqa.byebox.ui.main.ConnectionStatus
import com.perqa.byebox.ui.main.Loc
import com.perqa.byebox.ui.main.rememberTactileFeedback

/**
 * Мягкая аура вокруг кнопки: два радиальных слоя без жёстких колец.
 * Пульсация — только в активных состояниях; в DISCONNECTED статичная.
 */
@Composable
private fun ButtonAura(color: Color, pulse: Boolean) {
    if (pulse) {
        val transition = rememberInfiniteTransition(label = "buttonAura")
        val scale by transition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.18f,
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "auraScale"
        )
        val alpha by transition.animateFloat(
            initialValue = 0.16f,
            targetValue = 0.34f,
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "auraAlpha"
        )
        AuraLayers(color = color, scale = scale, alpha = alpha)
    } else {
        AuraLayers(color = color, scale = 0.96f, alpha = 0.20f)
    }
}

@Composable
private fun AuraLayers(color: Color, scale: Float, alpha: Float) {
    Box(
        modifier = Modifier
            .size(190.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.55f),
                        color.copy(alpha = 0.18f),
                        Color.Transparent
                    )
                ),
                shape = CircleShape
            )
    )
    Box(
        modifier = Modifier
            .size(160.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.45f), Color.Transparent)
                ),
                shape = CircleShape
            )
    )
}

@Composable
fun ConnectionButton(
    status: ConnectionStatus,
    pulseEnabled: Boolean = true,
    language: String = "ru",
    centerContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val tactileFeedback = rememberTactileFeedback()
    val motion = LocalByeBoxMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    // Лёгкое «продавливание» кнопки: expressive-отклик без перерисовки экрана.
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.955f else 1f,
        animationSpec = motion.spatialFast(),
        label = "connectPressScale"
    )

    val isBusy = status == ConnectionStatus.CONNECTING || status == ConnectionStatus.RECONNECTING

    val statusLabel = when (status) {
        ConnectionStatus.CONNECTED -> Loc.get("btn_connected", language)
        ConnectionStatus.CONNECTING -> Loc.get("btn_connecting", language)
        ConnectionStatus.RECONNECTING -> Loc.get("btn_reconnecting", language)
        ConnectionStatus.DISCONNECTED -> Loc.get("btn_disconnected", language)
    }
    val contentColor = when (status) {
        ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.onPrimary
        ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.onTertiary
        ConnectionStatus.RECONNECTING -> MaterialTheme.colorScheme.onError
        ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.onSurface
    }
    val iconTint = if (status == ConnectionStatus.DISCONNECTED) {
        MaterialTheme.colorScheme.primary
    } else {
        contentColor
    }
    val auraColor = when (status) {
        ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primary
        ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.tertiary
        ConnectionStatus.RECONNECTING -> MaterialTheme.colorScheme.error
        ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.primary
    }
    val buttonBrush = when (status) {
        ConnectionStatus.CONNECTED -> Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primaryContainer
            )
        )
        ConnectionStatus.CONNECTING -> Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.tertiaryContainer
            )
        )
        ConnectionStatus.RECONNECTING -> Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.error,
                MaterialTheme.colorScheme.errorContainer
            )
        )
        ConnectionStatus.DISCONNECTED -> Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.colorScheme.surfaceContainer
            )
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(200.dp)
    ) {
        ButtonAura(
            color = auraColor,
            pulse = pulseEnabled && status != ConnectionStatus.DISCONNECTED
        )

        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(188.dp),
                color = auraColor,
                trackColor = auraColor.copy(alpha = 0.12f),
                strokeWidth = 3.dp,
                strokeCap = StrokeCap.Round
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(150.dp)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .shadow(
                    elevation = if (status != ConnectionStatus.DISCONNECTED) 18.dp else 6.dp,
                    shape = CircleShape,
                    ambientColor = auraColor.copy(alpha = 0.45f),
                    spotColor = auraColor.copy(alpha = 0.45f)
                )
                .clip(CircleShape)
                .background(brush = buttonBrush)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Button,
                    onClickLabel = statusLabel
                ) {
                    tactileFeedback()
                    onClick()
                }
        ) {
            if (centerContent != null) {
                centerContent()
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isBusy) {
                        // Анимация вращения создаётся только в busy-состоянии.
                        val busyRotation by rememberInfiniteTransition(label = "busyRotate").animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "rotate"
                        )
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(36.dp).rotate(busyRotation)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = contentColor
                        )
                    )
                }
            }
        }
    }
}