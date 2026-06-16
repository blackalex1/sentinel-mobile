package com.xprox.sentinel.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.xprox.sentinel.MainActivity

object ThreatNotificationHelper {
    private const val TAG = "ThreatNotificationHelper"
    private const val CHANNEL_ID = "sentinel_threat_channel"
    private const val NOTIFICATION_ID = 9988

    /**
     * Creates and triggers a high-priority system security notification warning about the virus.
     */
    fun showSecurityAlertNotification(context: Context, appName: String, packageName: String) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Уведомления службы безопасности Sentinel",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Предупреждения об обнаружении вирусов и блокировках сети"
                    enableVibration(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                manager.createNotificationChannel(channel)
            }

            val pendingIntent = PendingIntent.getActivity(
                context, 1,
                Intent(context, MainActivity::class.java).apply {
                    putExtra("navigate_to", "settings")
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(context)
            }

            val notification = builder
                .setContentTitle("⚠️ Угроза заблокирована!")
                .setContentText("Приложение '$appName' помещено в тотальный блекхол сети.")
                .setSmallIcon(com.xprox.sentinel.R.drawable.ic_shield_status) // uses standard shield drawable
                .setCategory(Notification.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to show security alert notification", e)
        }
    }

    fun showSystemSecurityAlertNotification(context: Context, appName: String, packageName: String, port: Int) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Уведомления службы безопасности Sentinel",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Предупреждения об обнаружении вирусов и блокировках сети"
                    enableVibration(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                manager.createNotificationChannel(channel)
            }

            val pendingIntent = PendingIntent.getActivity(
                context, 2,
                Intent(context, MainActivity::class.java).apply {
                    putExtra("navigate_to", "settings")
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(context)
            }

            val notification = builder
                .setContentTitle("⚠️ Системная угроза!")
                .setContentText("Служба '$appName' совершает атаки на порт $port. Сеть сохранена.")
                .setSmallIcon(com.xprox.sentinel.R.drawable.ic_shield_status)
                .setCategory(Notification.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            manager.notify(NOTIFICATION_ID + 1, notification)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to show system security alert notification", e)
        }
    }
}
