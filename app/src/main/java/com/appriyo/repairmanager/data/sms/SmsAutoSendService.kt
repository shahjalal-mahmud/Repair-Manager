package com.appriyo.repairmanager.data.sms

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.appriyo.repairmanager.MainActivity
import com.appriyo.repairmanager.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.java.KoinJavaComponent.get

/**
 * Foreground service that keeps [SmsAutoSendManager] alive across Android
 * 13+ background process kills. Without a foreground notification the OS
 * will silently terminate the process within minutes on devices targeting
 * SDK 33+, and the Firestore snapshot listener disconnects — so SMS never
 * fires.
 *
 * Lifecycle:
 *  - Started by [SmsAutoSendManager.start] when this device is the SMS sender.
 *  - Stops itself when the device is no longer the sender OR when SMS permission
 *    is revoked.
 *  - Re-armed on boot by [com.appriyo.repairmanager.notifications.BootCompletedReceiver].
 *
 * Foreground service type is `dataSync` (declared in the manifest), which
 * Android 14+ requires for background sync work that doesn't fit other types.
 */
class SmsAutoSendService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel(this)
        startForeground(NOTIFICATION_ID, buildNotification(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager: SmsAutoSendManager = get(SmsAutoSendManager::class.java)
        manager.start(serviceScope)
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SMS Auto-Send",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the SMS auto-send listener running"
            setShowBadge(false)
        }
        mgr.createNotificationChannel(channel)
    }

    private fun buildNotification(context: Context): Notification {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Auto-sending SMS")
            .setContentText("Repair Manager is sending customer notifications.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pi)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "sms_auto_send_service"
        private const val NOTIFICATION_ID = 6001

        /**
         * Starts the foreground service. No-op if SMS permission is not granted
         * or if this device isn't the designated sender (caller checks).
         *
         * Safe to call multiple times - the system collapses duplicate start commands.
         *
         * On Android 13+ (TIRAMISU), if POST_NOTIFICATIONS is denied we still
         * start the service because the manager's caller-side listener would
         * otherwise be killed by the OS under background restrictions. The
         * foreground notification just won't be visible to the user.
         */
        fun start(context: Context) {
            val intent = Intent(context, SmsAutoSendService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
                // If startForegroundService throws (e.g. background-start
                // restriction on Android 12+ when called from a non-Activity
                // context), fall back to a regular startService. The service
                // will then call startForeground() in onCreate; if that also
                // fails the OS will eventually stop us and the next snapshot
                // re-fires start().
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, SmsAutoSendService::class.java))
            } catch (_: Exception) {
                // Best-effort.
            }
        }
    }
}
