package com.xprox.sentinel.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.xprox.sentinel.MainActivity
import com.xprox.sentinel.data.LanguageManager

object VpnNotificationHelper {
    const val CHANNEL_ID = "vpn_service_channel_v4"
    const val NOTIFICATION_ID = 8876

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Delete legacy notification channels so system OS drops previous alert/sound configurations
            try {
                manager.deleteNotificationChannel("vpn_service_channel")
                manager.deleteNotificationChannel("vpn_service_channel_v2")
                manager.deleteNotificationChannel("vpn_service_channel_v3")
            } catch (e: Exception) {
                // Ignore
            }

            val channel = NotificationChannel(
                CHANNEL_ID,
                LanguageManager.getString("notification_channel_name"),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = LanguageManager.getString("notification_channel_desc")
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun createNotification(
        context: Context,
        profileName: String,
        profileAddress: String,
        socksPort: Int,
        speedText: String = ""
    ): Notification {
        val pendingIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val isCapturing = ThreatDetectionManager.isAnyAppCapturingPcap()

        val disconnectPendingIntent = if (isCapturing) {
            val disconnectIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("EXTRA_SHOW_DISCONNECT_CONFIRM", true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            PendingIntent.getActivity(
                context, 1,
                disconnectIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            val disconnectIntent = Intent(context, VpnManagerService::class.java).apply {
                action = VpnManagerService.ACTION_DISCONNECT
            }
            PendingIntent.getService(
                context, 1,
                disconnectIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setColor(0xFF8B5CF6.toInt())

        val notificationTitle = if (profileName.isNotEmpty() && profileAddress.isNotEmpty()) {
            profileName
        } else {
            LanguageManager.getString("notification_title")
        }

        val warningPrefix = if (isCapturing) {
            if (LanguageManager.currentLanguage.value.code == "ru") {
                "⚠️ ИДЕТ СБОР PCAP ДАМПА УГРОЗ! | "
            } else {
                "⚠️ ACTIVE PCAP CAPTURE IN PROGRESS! | "
            }
        } else ""

        val contentText = if (speedText.isNotEmpty()) {
            "$warningPrefix$speedText"
        } else {
            "$warningPrefix${LanguageManager.getString("notification_socks_protected")} $socksPort"
        }

        val disconnectAction = NotificationCompat.Action.Builder(
            0,
            LanguageManager.getString("btn_disconnect"),
            disconnectPendingIntent
        ).build()

        builder.addAction(disconnectAction)

        return builder
            .setContentTitle(notificationTitle)
            .setContentText(contentText)
            .setSmallIcon(com.xprox.sentinel.R.drawable.ic_shield_status)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setLocalOnly(true)
            .setOnlyAlertOnce(true)
            .build()
    }
}
