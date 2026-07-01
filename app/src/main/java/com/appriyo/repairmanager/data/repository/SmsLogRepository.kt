// app/src/main/java/com/appriyo/repairmanager/data/repository/SmsLogRepository.kt
package com.appriyo.repairmanager.data.repository

import com.appriyo.repairmanager.data.model.SmsFirestorePaths
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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
 *  1) tryClaimLog: transactionally creates the log doc IF AND ONLY IF it doesn't exist yet.
 *     If it already exists, that status was already handled - caller must not send again.
 *  2) updateLogResult: after the actual SMS send attempt, records whether it succeeded.
 */
class SmsLogRepository(
    private val firestore: FirebaseFirestore,
    private val userProvider: FirestoreUserProvider
) {

    private val logsCollection
        get() = userProvider.currentUserDocument()
            .collection(SmsFirestorePaths.SMS_LOGS_COLLECTION)

    private fun logId(repairId: String, status: String) = "${repairId}_$status"

    /** Returns true if this call claimed (created) the log - i.e. caller should proceed to send. */
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
                if (snapshot.exists()) {
                    false
                } else {
                    val data = hashMapOf<String, Any?>(
                        "id" to id,
                        "repairId" to repairId,
                        "status" to status,
                        "phoneNumber" to phoneNumber,
                        "message" to message,
                        "sentAt" to FieldValue.serverTimestamp(),
                        "sentByDeviceId" to deviceId,
                        "success" to false
                    )
                    transaction.set(logRef, data)
                    true
                }
            }.await()
            Result.success(claimed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLogResult(repairId: String, status: String, success: Boolean): Result<Unit> {
        return try {
            logsCollection.document(logId(repairId, status))
                .update("success", success)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}