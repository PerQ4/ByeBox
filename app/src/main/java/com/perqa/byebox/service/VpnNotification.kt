package com.perqa.byebox.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.perqa.byebox.R

object VpnNotification {
    const val CHANNEL_ID = "byebox_vpn_channel"
    const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ByeBox VPN Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Displays active VPN connection status"
                setShowBadge(false)
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    fun build(
        context: Context,
        title: String,
        content: String,
        subText: String? = null,
        disconnectPendingIntent: PendingIntent? = null
    ): Notification {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, com.perqa.byebox.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        val clickPendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_on)
            .setContentTitle(title)
            .setContentText(content)
            .apply { if (subText != null) setSubText(subText) }
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(clickPendingIntent)
            .apply {
                if (disconnectPendingIntent != null) {
                    addAction(R.drawable.ic_notification_on, "Отключить", disconnectPendingIntent)
                }
            }
            .build()
    }

    fun startForeground(service: android.app.Service, notification: Notification) {
        createChannel(service)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            service.startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            service.startForeground(NOTIFICATION_ID, notification)
        }
    }

    fun updateNotification(context: Context, notification: Notification) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, notification)
    }
}
