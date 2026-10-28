package com.perqa.byebox.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Единые спеки движения для блоков главной и TGWS — нативные M3 Expressive
 * ([MotionScheme]) из Material3 1.5.x.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Immutable
class ByeBoxMotion internal constructor(private val scheme: MotionScheme) {

    /** Основное пространственное движение: смена формы/положения. */
    fun <T> spatialDefault(): FiniteAnimationSpec<T> = scheme.defaultSpatialSpec()

    /** Быстрое пространственное движение: отклик на нажатие. */
    fun <T> spatialFast(): FiniteAnimationSpec<T> = scheme.fastSpatialSpec()

    /** Цвет/прозрачность: без отскока. */
    fun <T> effectsDefault(): FiniteAnimationSpec<T> = scheme.defaultEffectsSpec()

    fun <T> effectsFast(): FiniteAnimationSpec<T> = scheme.fastEffectsSpec()

    /**
     * Раскрытие/сворачивание по высоте. Намеренно critically damped, а НЕ
     * expressive-spring: любой отскок даёт высоту больше контента, из-за чего
     * появляется пустая полоса, а замеры высот (панель пресетов) «плавают».
     */
    fun <T> expand(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val StandardMotion = ByeBoxMotion(MotionScheme.standard())

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val ExpressiveMotion = ByeBoxMotion(MotionScheme.expressive())

val LocalByeBoxMotion = staticCompositionLocalOf { StandardMotion }

/**
 * Локальная expressive-обёртка: только спеки движения. Цветовую схему НЕ
 * переопределяем — иначе MaterialExpressiveTheme без явного colorScheme
 * подставляет expressiveLightColorScheme() и блоки становятся светлыми
 * на тёмной теме.
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
