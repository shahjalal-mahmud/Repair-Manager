// app/src/main/java/com/appriyo/repairmanager/data/repository/SmsLogRepository.kt
package com.appriyo.repairmanager.data.repository

import com.appriyo.repairmanager.data.model.SmsFirestorePaths
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Handles users/{uid}/smsLogs/{repairId}_{status} - the duplicate-prevention
 * mechanism.
 *
 * **Data isolation:** SMS logs now live under the signed-in account's own
 * document, so two different repair shops (Google accounts) never see or
 * collide with each other's SMS history. Multiple devices signed into the
 * SAME account still share this collection, so the transactional claim below
 * continues to prevent duplicate sends across devices exactly as before.
 *
 * Two-phase to avoid races across listener re-fires:
 *  1) tryClaimLog: transactionally creates the log doc IF AND ONLY IF it
 *     doesn't exist yet OR the previous attempt failed and is eligible for
 *     retry. Once a log is marked `success = true` it is permanent - that
 *     status will never be sent again.
 *  2) recordAttemptResult: called from [com.appriyo.repairmanager.data.sms.SmsSentReceiver]
 *     with the framework's reported outcome. Records success/failure and,
 *     on failure, schedules the next retry window using exponential backoff.
 *
 * **Why retries exist:** On Android 13+, SmsManager.sendTextMessage for a
 * non-default SMS app is silently dropped on some OEMs. The fix is to allow
 * transient failures to be retried on the next eligible snapshot rather than
 * bricked by a permanent tombstone.
 */
class SmsLogRepository(
    private val firestore: FirebaseFirestore,
    private val userProvider: FirestoreUserProvider
) {

    private val logsCollection
        get() = userProvider.currentUserDocument()
            .collection(SmsFirestorePaths.SMS_LOGS_COLLECTION)

    private fun logId(repairId: String, status: String) = "${repairId}_$status"

    /**
     * Returns true if this call claimed (or reclaimed) the log - i.e. caller
     * should proceed to send.
     *
     * A claim succeeds when:
     *  - The doc doesn't exist yet (first send), OR
     *  - The doc exists with `success = false`, `nextEligibleAt <= now`, and
     *    `attemptCount < MAX_ATTEMPTS` (retry-after-backoff).
     *
     * Once `success = true` is set, the claim permanently returns false.
     */
    suspend fun tryClaimLog(
        repairId: String,
        status: String,
        phoneNumber: String,
        message: String,
        deviceId: String
    ): Result<Boolean> {
        val id = logId(repairId, status)
        return try {
            val logRef = logsCollection.document(id)
            val claimed = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(logRef)
                if (!snapshot.exists()) {
                    // First attempt ever for this (repairId, status).
                    val data = hashMapOf<String, Any?>(
                        "id" to id,
                        "repairId" to repairId,
                        "status" to status,
                        "phoneNumber" to phoneNumber,
                        "message" to message,
                        "sentByDeviceId" to deviceId,
                        "attemptCount" to 0,
                        "success" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "nextEligibleAt" to null
                    )
                    transaction.set(logRef, data)
                    true
                } else {
                    val existingSuccess = snapshot.getBoolean("success") == true
                    if (existingSuccess) {
                        false  // already permanently sent
                    } else {
                        val attemptCount = snapshot.getLong("attemptCount")?.toInt() ?: 0
                        if (attemptCount >= MAX_ATTEMPTS) {
                            false  // give up after MAX_ATTEMPTS to avoid infinite loops
                        } else {
                            val now = System.currentTimeMillis()
                            val nextEligible = snapshot.getTimestamp("nextEligibleAt")
                                ?.toDate()?.time ?: 0L
                            if (nextEligible > now) {
                                false  // backoff window not yet elapsed
                            } else {
                                // Eligible for retry. Update metadata but keep
                                // the original createdAt - only reset
                                // attemptCount? No: increment it.
                                val newCount = attemptCount + 1
                                transaction.update(
                                    logRef,
                                    mapOf(
                                        "attemptCount" to newCount,
                                        "lastAttemptAt" to FieldValue.serverTimestamp(),
                                        "phoneNumber" to phoneNumber,
                                        "message" to message,
                                        "sentByDeviceId" to deviceId,
                                        "nextEligibleAt" to null
                                    )
                                )
                                true
                            }
                        }
                    }
                }
            }.await()
            Result.success(claimed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Called from [com.appriyo.repairmanager.data.sms.SmsSentReceiver] when
     * the framework reports the outcome of a send via the SMS_SENT broadcast.
     *
     * On success: marks the log `success = true` permanently.
     * On failure: records `lastError`, increments `attemptCount`, and sets
     * `nextEligibleAt` to now + exponential-backoff window so the next
     * snapshot will retry it once the backoff has elapsed.
     */
    suspend fun recordAttemptResult(
        repairId: String,
        status: String,
        success: Boolean,
        errorMessage: String?
    ): Result<Unit> {
        return try {
            val logRef = logsCollection.document(logId(repairId, status))
            val snapshot = logRef.get().await()
            if (!snapshot.exists()) {
                // Lost race with a deletion or the doc never got created. Nothing to update.
                return Result.success(Unit)
            }
            val attemptCount = snapshot.getLong("attemptCount")?.toInt() ?: 0
            val updates = mutableMapOf<String, Any?>(
                "lastAttemptAt" to FieldValue.serverTimestamp()
            )
            if (success) {
                updates["success"] = true
                updates["lastError"] = null
                updates["nextEligibleAt"] = null
            } else {
                val newCount = attemptCount + 1
                updates["attemptCount"] = newCount
                updates["lastError"] = errorMessage
                updates["nextEligibleAt"] = computeNextEligible(newCount)
            }
            logRef.set(updates.toMap(), SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Backoff schedule: 30s, 2m, 10m, 1h, 6h (max). After MAX_ATTEMPTS the
     * doc becomes permanent-failure (tryClaimLog returns false), preventing
     * unbounded retries that would drain the radio and battery.
     */
    private fun computeNextEligible(attemptCount: Int): com.google.firebase.Timestamp {
        val delayMs = when (attemptCount) {
            1 -> TimeUnit.SECONDS.toMillis(30)
            2 -> TimeUnit.MINUTES.toMillis(2)
            3 -> TimeUnit.MINUTES.toMillis(10)
            4 -> TimeUnit.HOURS.toMillis(1)
            else -> TimeUnit.HOURS.toMillis(6)
        }
        val target = System.currentTimeMillis() + delayMs
        return com.google.firebase.Timestamp(java.util.Date(target))
    }

    companion object {
        /** Maximum number of send attempts before the doc becomes permanent-failure. */
        private const val MAX_ATTEMPTS = 6
    }
}
