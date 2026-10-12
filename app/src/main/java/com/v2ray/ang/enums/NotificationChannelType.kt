package com.v2ray.ang.enums

import android.content.Context
import androidx.annotation.StringRes
import com.perqa.byebox.R

/**
 * Enum defining different notification channels.
 * Each channel has a unique channelId, notificationId, and a localized name.
 *
 * Имя канала берётся из строковых ресурсов (values / values-ru / values-zh),
 * а не из жёстко зашитой строки, поэтому отображается на языке системы.
 * Контекст приложения устанавливается один раз в [init] из
 * `ByeBoxApplication.onCreate`; `NotificationHelper` читает [channelName]
 * при создании канала.
 */
enum class NotificationChannelType(
    val channelId: String,
    @StringRes val channelNameRes: Int,
    val notificationId: Int
) {
    SUBSCRIPTION_UPDATE(
        channelId = "subscription_update_channel",
        channelNameRes = R.string.notif_channel_subscription_update,
        notificationId = 13
    ),
    CORE_TEST(
        channelId = "core_test_channel",
        channelNameRes = R.string.notif_channel_core_test,
        notificationId = 12
    );

    /** Локализованное имя канала; `channelId` — безопасный фолбэк. */
    val channelName: String
        get() = appContext?.getString(channelNameRes) ?: channelId

    companion object {
        private var appContext: Context? = null

        /** Задаёт контекст приложения для резолва строк. Вызывается из Application. */
        fun init(context: Context) {
            appContext = context.applicationContext
        }
    }
}
