package com.appriyo.repairmanager.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.appriyo.repairmanager.data.sms.SmsAutoSendManager
import com.appriyo.repairmanager.data.sms.SmsSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.java.KoinJavaComponent.get

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            if (AlarmScheduler.canScheduleExactAlarms(context)) {
                AlarmScheduler.scheduleDailyReminder(context)
            }

            // Re-arm the SMS auto-send service if this device is the designated
            // sender and SMS permission is still granted. The check is best-effort:
            // SmsAutoSendManager.start() will early-out (no service start) if the
            // device-id no longer matches in Firestore.
            try {
                val smsSender: SmsSender = get(SmsSender::class.java)
                if (smsSender.hasSendPermission()) {
                    val manager: SmsAutoSendManager = get(SmsAutoSendManager::class.java)
                    manager.start(
                        CoroutineScope(SupervisorJob() + Dispatchers.Default)
                    )
                }
            } catch (_: Exception) {
                // Don't let SMS re-arm failures crash the boot dispatcher.
            }
        }
    }
}
