package com.perqa.byebox.data

/**
 * Маппинг выбора «TUN Стек» на параметры конфига ядра Xray.
 *
 * Ядро не знает поля `stack`: выбор делается полем `noKernelTun` у TUN-инбаунда
 * (`json:"noKernelTun"` в Xray-core). Логика обратная:
 *  - `gvisor` — трафик обрабатывает gVisor в userspace → `noKernelTun = true`;
 *  - `system` — обработку берёт ядро Android → `noKernelTun = false`.
 *
 * Вынесено отдельно, чтобы обе точки записи (UI и пресеты профилей)
 * и место сборки конфига считали одинаково, а логика покрывалась тестами.
 */
object TunStackMapping {

    const val GVISOR = "gvisor"
    const val SYSTEM = "system"

    /**
     * Имя стека из настройки профиля ("GVISOR"/"SYSTEM"/"INHERIT", регистронезависимо)
     * в значение, которое понимает ядро.
     *
     * Только явный выбор gVisor даёт gvisor; всё остальное — system. Это
     * намеренно «безопасно по умолчанию»: до 1.5.4 поле `noKernelTun` в конфиг
     * не попадало вовсе, то есть фактически работал системный стек ядра.
     * Неизвестное или пустое значение не должно молча переводить пользователя
     * на userspace-стек.
     */
    fun toXrayValue(stackName: String): String =
        if (stackName.equals(GVISOR, ignoreCase = true)) GVISOR else SYSTEM

    /**
     * Значение поля `noKernelTun` для указанного стека.
     */
    fun toNoKernelTun(xrayValue: String): Boolean =
        !xrayValue.equals(SYSTEM, ignoreCase = true)

    /**
     * Приведение уже сохранённого значения ядра ("gvisor"/"system", может быть
     * null или мусором) к валидному. Неизвестное значение — `system`.
     *
     * Отдельно от [toXrayValue], потому что здесь на входе уже формат ядра,
     * а не имя стека из настроек профиля.
     */
    fun normalizeCoreValue(raw: String?): String =
        if (raw.equals(GVISOR, ignoreCase = true)) GVISOR else SYSTEM
}
