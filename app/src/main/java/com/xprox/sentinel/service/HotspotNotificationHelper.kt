package com.xprox.sentinel.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.xprox.sentinel.MainActivity
import com.xprox.sentinel.R

object HotspotNotificationHelper {
    private const val TAG = "HotspotNotificationHelper"
    private const val CHANNEL_ID = "sentinel_hotspot_channel"
    private const val PAIRING_CHANNEL_ID = "sentinel_pairing_request_channel"
    private const val NOTIFICATION_ID = 7766
    private const val PAIRING_NOTIFICATION_ID = 7767
    private const val SYNC_NOTIFICATION_ID = 7768

    fun showPairingAttemptNotification(context: Context, clientName: String, pinCode: String) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    PAIRING_CHANNEL_ID,
                    "Запросы сопряжения Sentinel",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Уведомления о входящих запросах сопряжения с ПК"
                    enableVibration(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                manager.createNotificationChannel(channel)
            }

            val pendingIntent = PendingIntent.getActivity(
                context, 101,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val title = "🔐 Запрос сопряжения с ПК"
            val text = "ПК \"$clientName\" запрашивает сопряжение (PIN: $pinCode). Нажмите для подтверждения."

            val notification = NotificationCompat.Builder(context, PAIRING_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_shield_status)
                .setColor(0xFFC084FC.toInt())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            manager.notify(PAIRING_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show pairing attempt notification", e)
        }
    }

    fun dismissPairingAttemptNotification(context: Context) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            manager.cancel(PAIRING_NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dismiss pairing attempt notification", e)
        }
    }

    fun showSyncNotification(context: Context, clientName: String?) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Раздача прокси и Hotspot",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Уведомления о статусе проксирования точки доступа для ПК и клиентов"
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                manager.createNotificationChannel(channel)
            }

            val pendingIntent = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val client = clientName ?: "Sentinel Desktop"
            val title = "🔄 Синхронизация точки доступа"
            val text = "ПК \"$client\" успешно синхронизировал параметры прокси."

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_shield_status)
                .setColor(0xFF38BDF8.toInt())
                .setAutoCancel(true)
                .setTimeoutAfter(8000)
                .setContentIntent(pendingIntent)
                .build()

            manager.notify(SYNC_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show sync notification", e)
        }
    }

    fun showHotspotProxyingNotification(context: Context, clientName: String?, port: Int) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Раздача прокси и Hotspot",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Уведомления о статусе проксирования точки доступа для ПК и клиентов"
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                manager.createNotificationChannel(channel)
            }

            val pendingIntent = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val title = "🌐 Проксирование точки доступа активно"
            val text = if (!clientName.isNullOrEmpty()) {
                "Подключен ПК-клиент: $clientName (SOCKS5 :$port)"
            } else {
                "Точка доступа проксируется через Sentinel (SOCKS5 :$port)"
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_shield_status)
                .setColor(0xFF38BDF8.toInt())
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .build()

            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show hotspot notification", e)
        }
    }

    fun dismissHotspotProxyingNotification(context: Context) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            manager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dismiss hotspot notification", e)
        }
    }
}
