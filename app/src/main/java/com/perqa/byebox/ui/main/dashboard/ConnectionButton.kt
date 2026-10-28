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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
 * Кнопка подключения в духе M3 Expressive: форма Cookie9Sided и мягкое
 * радиальное свечение Cookie12Sided (с blur), без градиентов и жёстких колец.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
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
        ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val iconTint = contentColor
    val accent = when (status) {
        ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primary
        ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.tertiary
        ConnectionStatus.RECONNECTING -> MaterialTheme.colorScheme.error
        ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.primary
    }
    val buttonColor = when (status) {
        ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primary
        ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.tertiary
        ConnectionStatus.RECONNECTING -> MaterialTheme.colorScheme.error
        ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.primaryContainer
    }
    val buttonShape = MaterialShapes.Cookie9Sided.toShape()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(210.dp)
    ) {
        DynamicBlob(
            color = accent,
            status = status,
            animate = pulseEnabled
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(146.dp)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .clip(buttonShape)
                .background(buttonColor)
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
                        val rotation by rememberInfiniteTransition(label = "busyRotate").animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "rotate"
                        )
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier
                                .size(38.dp)
                                .rotate(rotation)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(38.dp)
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

/**
 * «Жидкое» динамическое пятно за кнопкой: два перекрывающихся cookie-слоя,
 * вращающихся в разные стороны, с breathing + squash/stretch. Поведение
 * зависит от состояния; альфа достаточно высокая, чтобы пятно было видно.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DynamicBlob(color: Color, status: ConnectionStatus, animate: Boolean) {
    val shapeA = when (status) {
        ConnectionStatus.DISCONNECTED -> MaterialShapes.Cookie6Sided
        ConnectionStatus.CONNECTED -> MaterialShapes.Cookie9Sided
        ConnectionStatus.CONNECTING -> MaterialShapes.Cookie9Sided
        ConnectionStatus.RECONNECTING -> MaterialShapes.Cookie12Sided
    }.toShape()
    val shapeB = when (status) {
        ConnectionStatus.RECONNECTING -> MaterialShapes.Cookie9Sided
        else -> MaterialShapes.Cookie6Sided
    }.toShape()
    val baseAlpha = when (status) {
        ConnectionStatus.DISCONNECTED -> 0.34f
        ConnectionStatus.CONNECTED -> 0.70f
        ConnectionStatus.CONNECTING -> 0.80f
        ConnectionStatus.RECONNECTING -> 0.85f
    }
    val breatheMs = when (status) {
        ConnectionStatus.CONNECTED -> 3200
        ConnectionStatus.CONNECTING -> 1500
        else -> 1100
    }
    val rotateMs = when (status) {
        ConnectionStatus.CONNECTED -> 14000
        ConnectionStatus.CONNECTING -> 3600
        else -> 2600
    }

    if (!animate || status == ConnectionStatus.DISCONNECTED) {
        BlobLayer(
            shape = shapeA,
            color = color,
            rotation = 0f,
            scale = 1f,
            alpha = baseAlpha,
            squash = 0f
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "blob")
    val rotationA by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(rotateMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blobRotationA"
    )
    val rotationB by transition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween((rotateMs * 1.6f).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blobRotationB"
    )
    val breath by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(breatheMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blobBreath"
    )
    val squash by transition.animateFloat(
        initialValue = -0.07f,
        targetValue = 0.07f,
        animationSpec = infiniteRepeatable(
            animation = tween((breatheMs * 0.7f).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blobSquash"
    )

    BlobLayer(
        shape = shapeA,
        color = color,
        rotation = rotationA,
        scale = breath,
        alpha = baseAlpha,
        squash = squash
    )
    BlobLayer(
        shape = shapeB,
        color = color,
        rotation = rotationB,
        scale = breath * 0.92f,
        alpha = baseAlpha * 0.7f,
        squash = -squash
    )
}

@Composable
private fun BlobLayer(
    shape: androidx.compose.ui.graphics.Shape,
    color: Color,
    rotation: Float,
    scale: Float,
    alpha: Float,
    squash: Float
) {
    Box(
        modifier = Modifier
            .size(198.dp)
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale * (1f + squash)
                scaleY = scale * (1f - squash)
            }
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = alpha),
                        color.copy(alpha = alpha * 0.55f),
                        Color.Transparent
                    )
                ),
                shape = shape
            )
    )
}
