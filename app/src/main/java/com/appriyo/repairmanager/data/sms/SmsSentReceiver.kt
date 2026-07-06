package com.appriyo.repairmanager.data.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import com.appriyo.repairmanager.data.repository.SmsLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.get

/**
 * Receives the SMS_SENT broadcast after [SmsManager.sendTextMessage] has
 * handed the message off to the radio. We use this signal (instead of just
 * catching exceptions) so we can detect Android 13+ silent drops: the
 * `RESULT_ERROR_GENERIC_FAILURE` / `RESULT_ERROR_NO_SERVICE` codes fire
 * through here even when the framework call returns normally.
 *
 * The receiver updates the corresponding `smsLogs/{repairId}_{status}` doc
 * with the actual outcome and a short error description, so the
 * retry-with-backoff in [SmsLogRepository] can decide whether to re-attempt.
 *
 * This file does not introduce any new permissions or dependencies — it
 * only consumes the PendingIntents that [SmsSender] now creates.
 */
class SmsSentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val repairId = intent.getStringExtra(EXTRA_REPAIR_ID).orEmpty()
        val status = intent.getStringExtra(EXTRA_STATUS).orEmpty()
        if (repairId.isBlank() || status.isBlank()) return

        val resultCode = resultCode
        val (success, errorMessage) = when (resultCode) {
            Activity.RESULT_OK -> true to null
            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> false to "generic_failure"
            SmsManager.RESULT_ERROR_NO_SERVICE -> false to "no_service"
            SmsManager.RESULT_ERROR_NULL_PDU -> false to "null_pdu"
            SmsManager.RESULT_ERROR_RADIO_OFF -> false to "radio_off"
            // RESULT_ERROR_LIMIT_EXCEEDED and any other value: treat as failure
            // but allow retry (transient codes).
            else -> false to "code_$resultCode"
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repo: SmsLogRepository = get(SmsLogRepository::class.java)
                repo.recordAttemptResult(
                    repairId = repairId,
                    status = status,
                    success = success,
                    errorMessage = errorMessage
                )
            } catch (_: Exception) {
                // Swallow: receiver-level failure should never crash the broadcast dispatcher.
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SMS_SENT = "com.appriyo.repairmanager.SMS_SENT"
        const val EXTRA_REPAIR_ID = "extra_repair_id"
        const val EXTRA_STATUS = "extra_status"
    }
}
