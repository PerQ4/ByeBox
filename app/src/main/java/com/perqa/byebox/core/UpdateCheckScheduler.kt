package com.perqa.byebox.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteWorkManager
import com.perqa.byebox.BuildConfig
import com.perqa.byebox.MainActivity
import com.perqa.byebox.R
import com.perqa.byebox.ByeBoxApplication
import com.perqa.byebox.ui.main.Loc
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil
import java.util.concurrent.TimeUnit

/**
 * Background update check: periodically asks GitHub whether a newer build is
 * out and posts a system notification when it is. This covers the case where
 * the app is not opened for a long time — the in-app check only runs while the
 * main screen's view model is alive.
 *
 * The worker honors the same preferences as the in-app updater:
 * - `auto_check_updates` (SharedPreferences `byebox_settings`) — master switch;
 * - `remind_later_until` — do not nag again while the user asked to be reminded
 *   later;
 * - `skipped_version_code` — never suggest a release the user skipped.
 */
object UpdateCheckScheduler {
    private const val TASK_NAME = "byebox_update_check"
    private const val INTERVAL_HOURS = 24L

    const val CHANNEL_ID = "byebox_updates_channel"
    const val NOTIFICATION_ID = 14

    private const val KEY_AUTO_CHECK_UPDATES = "auto_check_updates"
    private const val KEY_REMIND_LATER_UNTIL = "remind_later_until"
    private const val KEY_SKIPPED_VERSION_CODE = "skipped_version_code"

    /**
     * Schedule (or cancel) the background update check based on the current
     * `auto_check_updates` preference. Call from Application/MainActivity
     * startup and from the settings toggle handler.
     */
    fun sync(context: Context = ByeBoxApplication.instance) {
        val enabled = prefs(context).getBoolean(KEY_AUTO_CHECK_UPDATES, true)
        val rw = RemoteWorkManager.getInstance(context)
        if (!enabled) {
            rw.cancelUniqueWork(TASK_NAME)
            LogUtil.i(AppConfig.TAG, "UpdateCheckScheduler: background check disabled, cancelled")
            return
        }
        val request = PeriodicWorkRequestBuilder<UpdateCheckTask>(INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        rw.enqueueUniquePeriodicWork(TASK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        LogUtil.i(AppConfig.TAG, "UpdateCheckScheduler: background check scheduled every ${INTERVAL_HOURS}h")
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences("byebox_settings", Context.MODE_PRIVATE)

    class UpdateCheckTask(context: Context, params: WorkerParameters) :
        CoroutineWorker(context, params) {

        override suspend fun doWork(): Result {
            val ctx = applicationContext
            val shared = ctx.getSharedPreferences("byebox_settings", Context.MODE_PRIVATE)
            if (!shared.getBoolean(KEY_AUTO_CHECK_UPDATES, true)) {
                LogUtil.i(AppConfig.TAG, "UpdateCheckTask: disabled by preference")
                return Result.success()
            }

            val remindUntil = shared.getLong(KEY_REMIND_LATER_UNTIL, 0L)
            if (System.currentTimeMillis() < remindUntil) {
                LogUtil.i(AppConfig.TAG, "UpdateCheckTask: user asked to be reminded later")
                return Result.success()
            }

            val result = UpdateChecker.check()
            if (result !is UpdateCheckResult.Available) {
                LogUtil.i(AppConfig.TAG, "UpdateCheckTask: no newer release found")
                return Result.success()
            }

            if (result.info.latestVersionCode <= shared.getInt(KEY_SKIPPED_VERSION_CODE, 0)) {
                LogUtil.i(AppConfig.TAG, "UpdateCheckTask: latest release was skipped by the user")
                return Result.success()
            }

            notifyUpdate(ctx, result.info)
            return Result.success()
        }

        private fun notifyUpdate(context: Context, info: UpdateInfo) {
            ensureChannel(context)

            val language = context.getSharedPreferences("byebox_settings", Context.MODE_PRIVATE)
                .getString("pref_language", "system") ?: "system"
            val title = String.format(
                Loc.get("notif_update_available", language),
                info.displayVersion
            )
            val content = Loc.get("notif_update_open", language)

            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            val contentIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_on)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            val nm = context.getSystemService(NotificationManager::class.java)
            nm.notify(NOTIFICATION_ID, notification)
            LogUtil.i(AppConfig.TAG, "UpdateCheckTask: posted update notification ${info.displayVersion}")
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ByeBox Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "New ByeBox versions"
            }
            nm.createNotificationChannel(channel)
        }
    }
}