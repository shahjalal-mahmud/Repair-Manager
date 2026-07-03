package com.appriyo.repairmanager.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.appriyo.repairmanager.data.repository.RepairRepository
import com.appriyo.repairmanager.domain.delivery.DeliveryFilter
import com.appriyo.repairmanager.domain.delivery.DeliveryFilterUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.get

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Reschedule first so a failure below can't silently break future reminders.
        AlarmScheduler.scheduleDailyReminder(context)

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (FirebaseAuth.getInstance().currentUser == null) return@launch

                val repairRepository: RepairRepository = get(RepairRepository::class.java)
                repairRepository.getAllRepairsOnce().onSuccess { repairs ->
                    val todayRepairs = DeliveryFilterUtils.filter(repairs, DeliveryFilter.TODAY)
                    ReminderNotificationHelper.showTodayReminder(context, todayRepairs)
                }
            } catch (e: Exception) {
                // A missed reminder shouldn't crash the broadcast dispatch.
            } finally {
                pendingResult.finish()
            }
        }
    }
}