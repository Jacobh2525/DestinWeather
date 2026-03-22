package com.destinweather.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.destinweather.MainActivity
import com.destinweather.R

object NotificationHelper {

    private const val CHANNEL_ID = "weather_alerts"
    private const val CHANNEL_NAME = "Weather Alerts"
    private const val CHANNEL_DESC = "Severe weather alerts and notifications"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showSevereAlertNotification(
        context: Context,
        alertId: String,
        event: String,
        severity: String,
        headline: String?
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val color = when (severity) {
            "Extreme" -> 0xFFD32F2F.toInt()
            "Severe" -> 0xFFF57C00.toInt()
            else -> 0xFFFBC02D.toInt()
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚠️ $event")
            .setContentText(headline ?: "Check for details")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(headline ?: "Tap to view alert details"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(color)
            .setVibrate(longArrayOf(0, 500, 200, 500))

        try {
            NotificationManagerCompat.from(context).notify(alertId.hashCode(), builder.build())
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }
}
