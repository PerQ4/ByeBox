package com.perqa.byebox.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Единая дизайн-система настроек.
 *
 * Все компоненты страницы настроек используют общую шкалу скруглений и единый
 * вид иконок в рядах, чтобы оформление было одинаковым в каждом подменю.
 * Ключ скругления: "expressive" (крупные радиуса) или "standard" (сдержанные).
 */
private fun radiusValue(roundness: String, expressive: Dp, standard: Dp): Dp =
    if (roundness == "expressive") expressive else standard

/** Радиус карточек, больших блоков и крайних строк группы. */
fun settingsCardRadius(roundness: String): Dp = radiusValue(roundness, 28.dp, 14.dp)

/** Радиус внутренних (не крайних) строк группы. */
fun settingsInnerRadius(roundness: String): Dp = radiusValue(roundness, 10.dp, 4.dp)

/** Радиус контролов: чипы иконок, кнопки, текстовые поля, плитки. */
fun settingsControlRadius(roundness: String): Dp = radiusValue(roundness, 16.dp, 8.dp)

/** Единый цвет подписей (subtitle) для всех компонентов настроек. */
val MaterialTheme.settingsSubtitleColor: Color
    @Composable get() = colorScheme.onSurface.copy(alpha = 0.62f)

/**
 * Единый чип иконки для рядов настроек: одинаковые размер, скругление и фон
 * во всех компонентах (переключатели, выбор опций, карточки категорий).
 */
@Composable
fun SettingsRowIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    containerAlpha: Float = 0.18f,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    cornerRoundness: String = "expressive"
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(settingsControlRadius(cornerRoundness)))
            .background(containerColor.copy(alpha = containerAlpha)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
    }
}