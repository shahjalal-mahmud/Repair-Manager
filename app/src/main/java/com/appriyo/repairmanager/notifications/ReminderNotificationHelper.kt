package com.appriyo.repairmanager.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.appriyo.repairmanager.MainActivity
import com.appriyo.repairmanager.R
import com.appriyo.repairmanager.data.model.Repair

object ReminderNotificationHelper {

    const val CHANNEL_ID = "daily_delivery_reminders"
    const val EXTRA_NAVIGATE_ROUTE = "extra_navigate_route"
    private const val NOTIFICATION_ID = 5001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID, "Delivery Reminders", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Daily 9 AM reminder for devices due today"
            enableVibration(true)
            setSound(soundUri, audioAttributes)
        }
        manager.createNotificationChannel(channel)
    }

    fun showTodayReminder(context: Context, repairs: List<Repair>) {
        if (repairs.isEmpty()) return
        createChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_ROUTE, "delivery_list/today")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, NOTIFICATION_ID, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val inboxStyle = NotificationCompat.InboxStyle()
        repairs.take(5).forEach { repair ->
            inboxStyle.addLine("${repair.customerName} • ${repair.deviceModel.ifBlank { "Unknown device" }}")
        }
        if (repairs.size > 5) inboxStyle.setSummaryText("+${repairs.size - 5} more")

        val contentText = if (repairs.size == 1) {
            val problem = repairs.first().problemDescription
            "${repairs.first().customerName} — ${problem.ifBlank { "No description" }}"
        } else {
            "${repairs.size} devices due for delivery today"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // swap for a dedicated monochrome icon later
            .setContentTitle("Today's deliveries (${repairs.size})")
            .setContentText(contentText)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}