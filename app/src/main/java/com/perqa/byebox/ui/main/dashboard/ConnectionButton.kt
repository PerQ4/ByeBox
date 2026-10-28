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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
        CookieAura(
            color = accent,
            pulse = pulseEnabled && status != ConnectionStatus.DISCONNECTED
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
 * Мягкое свечение: радиальный градиент, обрезанный по Cookie12Sided, с blur.
 * Пульсирует только в активных состояниях; в покое — статичное и тише.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CookieAura(color: Color, pulse: Boolean) {
    if (pulse) {
        val transition = rememberInfiniteTransition(label = "cookieAura")
        val scale by transition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(2600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "auraScale"
        )
        val alpha by transition.animateFloat(
            initialValue = 0.38f,
            targetValue = 0.68f,
            animationSpec = infiniteRepeatable(
                animation = tween(2600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "auraAlpha"
        )
        CookieGlow(color = color, scale = scale, alpha = alpha)
    } else {
        CookieGlow(color = color, scale = 1f, alpha = 0.42f)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CookieGlow(color: Color, scale: Float, alpha: Float) {
    // Аура — идеальный круг: Cookie12Sided в квадратном боксе выглядел
    // сплющенным овалом. Кнопка остаётся Cookie9Sided.
    val shape: androidx.compose.ui.graphics.Shape = CircleShape
    Box(
        modifier = Modifier
            .size(205.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            // Сглаживает края формы, чтобы это читалось как свечение, а не
            // как второй «блин» за кнопкой. На API < 31 blur игнорируется.
            .blur(30.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.9f),
                        color.copy(alpha = 0.35f),
                        Color.Transparent
                    )
                ),
                shape = shape
            )
    )
}
