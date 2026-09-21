package com.perqa.byebox.ui.main.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perqa.byebox.ui.main.ConnectionStatus
import com.perqa.byebox.ui.main.Loc
import com.perqa.byebox.ui.main.rememberTactileFeedback
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun ConnectionButton(
    status: ConnectionStatus,
    pulseEnabled: Boolean = true,
    language: String = "ru",
    centerContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val tactileFeedback = rememberTactileFeedback()
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val progressRotate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val isBusy = status == ConnectionStatus.CONNECTING || status == ConnectionStatus.RECONNECTING

    val buttonBgColor by animateColorAsState(
        targetValue = when (status) {
            ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primary
            ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.tertiary
            ConnectionStatus.RECONNECTING -> MaterialTheme.colorScheme.error
            ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.surface
        },
        label = "buttonColor"
    )
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
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(200.dp)
    ) {
        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(188.dp),
                color = buttonBgColor,
                trackColor = buttonBgColor.copy(alpha = 0.12f),
                strokeWidth = 3.dp,
                strokeCap = StrokeCap.Round
            )
        }

        if (status == ConnectionStatus.CONNECTED) {
            val auraAlpha1 by infiniteTransition.animateFloat(
                initialValue = 0.12f,
                targetValue = 0.38f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "auraAlpha1"
            )
            val auraScale1 by infiniteTransition.animateFloat(
                initialValue = 0.90f,
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "auraScale1"
            )
            val auraAlpha2 by infiniteTransition.animateFloat(
                initialValue = 0.04f,
                targetValue = 0.22f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "auraAlpha2"
            )
            val auraScale2 by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.45f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "auraScale2"
            )

            Box(
                modifier = Modifier
                    .size(150.dp)
                    .graphicsLayer {
                        scaleX = auraScale2
                        scaleY = auraScale2
                        alpha = auraAlpha2
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                buttonBgColor,
                                buttonBgColor.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Box(
                modifier = Modifier
                    .size(150.dp)
                    .graphicsLayer {
                        scaleX = auraScale1
                        scaleY = auraScale1
                        alpha = auraAlpha1
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                buttonBgColor,
                                buttonBgColor.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        Canvas(modifier = Modifier.size(188.dp)) {
            val center = center
            val baseRadius = 80.dp.toPx()
            val ringRadius = 88.dp.toPx()
            val ringStroke = 3.dp.toPx()

            if (status == ConnectionStatus.CONNECTED) {
                if (pulseEnabled) {
                    val pulseScale = 1f + 0.04f * sin(wavePhase.toDouble()).toFloat()
                    drawCircle(
                        color = buttonBgColor.copy(alpha = 0.16f),
                        radius = baseRadius * pulseScale,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawCircle(
                        color = buttonBgColor.copy(alpha = 0.08f),
                        radius = baseRadius * (pulseScale + 0.06f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                } else {
                    drawCircle(
                        color = buttonBgColor.copy(alpha = 0.16f),
                        radius = baseRadius,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
                val breatheAlpha = 0.10f + 0.12f * ((sin(wavePhase.toDouble()).toFloat() + 1f) * 0.5f)
                val breatheRadius = ringRadius + 1.5.dp.toPx() * sin(wavePhase.toDouble()).toFloat()
                drawCircle(
                    color = buttonBgColor.copy(alpha = breatheAlpha),
                    radius = breatheRadius,
                    style = Stroke(width = ringStroke)
                )
            } else if (status == ConnectionStatus.DISCONNECTED) {
                drawCircle(
                    color = onSurfaceColor.copy(alpha = 0.08f),
                    radius = baseRadius,
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = onSurfaceColor.copy(alpha = 0.16f),
                    radius = ringRadius,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(150.dp)
                .shadow(
                    elevation = if (status != ConnectionStatus.DISCONNECTED) 12.dp else 4.dp,
                    shape = CircleShape
                )
                .clip(CircleShape)
                .background(buttonBgColor)
                .clickable {
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
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier
                            .size(36.dp)
                            .rotate(if (isBusy) progressRotate else 0f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (status) {
                            ConnectionStatus.CONNECTED -> Loc.get("btn_connected", language)
                            ConnectionStatus.CONNECTING -> Loc.get("btn_connecting", language)
                            ConnectionStatus.RECONNECTING -> Loc.get("btn_reconnecting", language)
                            ConnectionStatus.DISCONNECTED -> Loc.get("btn_disconnected", language)
                        },
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