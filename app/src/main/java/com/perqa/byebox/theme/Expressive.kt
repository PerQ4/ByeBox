package com.perqa.byebox.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Единые спеки движения для блоков главной и TGWS.
 *
 * Проверка Compose BOM 2026.03.01 (Material3 1.4.0): expressive-API в артефакте
 * есть, но `MaterialExpressiveTheme`, `MotionScheme` и
 * `ExperimentalMaterial3ExpressiveApi` помечены `internal` в Kotlin-метаданных
 * и недоступны коду приложения. Поэтому здесь локальный эквивалент на spring:
 * формы — в [ByeBoxShapes], движение — ниже, без новых зависимостей.
 */
@Immutable
class ByeBoxMotion internal constructor(private val expressive: Boolean) {

    /** Основное пространственное движение: смена формы/положения. */
    fun <T> spatialDefault(): FiniteAnimationSpec<T> = if (expressive) {
        spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
    } else {
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    }

    /** Быстрое пространственное движение: отклик на нажатие. */
    fun <T> spatialFast(): FiniteAnimationSpec<T> = if (expressive) {
        spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMedium)
    } else {
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
    }

    /** Цвет/прозрачность: без отскока. */
    fun <T> effectsDefault(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    fun <T> effectsFast(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)

    /**
     * Раскрытие/сворачивание по высоте. Намеренно critically damped: любая
     * пересборка с отскоком даёт высоту больше контента, из-за чего появляется
     * пустая полоса, а замеры высот (панель пресетов) начинают «плавать».
     */
    fun <T> expand(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
}

private val StandardMotion = ByeBoxMotion(expressive = false)
private val ExpressiveMotion = ByeBoxMotion(expressive = true)

val LocalByeBoxMotion = staticCompositionLocalOf { StandardMotion }

/**
 * Локальная expressive-обёртка. Глобальную [ByeBoxTheme] не трогаем: цвета,
 * shapes и типографика остаются текущими, для переданного участка включаются
 * только expressive-спеки движения.
 */
@Composable
fun ByeBoxExpressiveTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalByeBoxMotion provides ExpressiveMotion, content = content)
}

/**
 * Выразительные формы для карточек главной/TGWS. Держим их в одном месте,
 * чтобы блоки не разъезжались по радиусу и не дублировали магические числа.
 */
object ByeBoxShapes {
    val cardExpressive = RoundedCornerShape(32.dp)
    val cardStandard = RoundedCornerShape(20.dp)
    val innerExpressive = RoundedCornerShape(28.dp)
    val innerStandard = RoundedCornerShape(16.dp)

    fun card(expressive: Boolean) = if (expressive) cardExpressive else cardStandard
    fun inner(expressive: Boolean) = if (expressive) innerExpressive else innerStandard
}
