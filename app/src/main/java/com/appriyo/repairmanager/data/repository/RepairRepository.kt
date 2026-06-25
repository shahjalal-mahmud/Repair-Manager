// app/src/main/java/com/appriyo/repairmanager/data/repository/RepairRepository.kt
package com.appriyo.repairmanager.data.repository

import com.appriyo.repairmanager.data.model.FirestorePaths
import com.appriyo.repairmanager.data.model.Repair
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Locale

/**
 * Repository responsible for all Firestore operations related to repair records.
 *
 * Collections used (Spark/free tier friendly, no extras):
 *  - "repairs"  : one document per repair record
 *  - "counters" : a single "repairCounter" document used to generate sequential serials
 *
 * Serial generation + repair-document creation happen inside ONE Firestore transaction,
 * exactly as before - this guarantees the counter and the repair records can never drift
 * apart, even with multiple shop devices writing concurrently.
 *
 * Realtime sync across devices is provided by [observeRepairs] and [observeRepair], which
 * wrap Firestore snapshot listeners as cold [Flow]s. No extra collections or polling are used.
 */
class RepairRepository(
    private val firestore: FirebaseFirestore
) {

    private val repairsCollection
        get() = firestore.collection(FirestorePaths.REPAIRS_COLLECTION)

    private val counterDocRef
        get() = firestore
            .collection(FirestorePaths.COUNTERS_COLLECTION)
            .document(FirestorePaths.REPAIR_COUNTER_DOC)

    /**
     * Creates a new repair record with a transactionally-generated sequential serial number.
     */
    suspend fun createRepair(
        customerName: String,
        phoneNumber: String,
        deviceModel: String,
        problemDescription: String,
        expectedDeliveryDate: String,
        paymentInfo: String,
        additionalDetails: String,
        boxNumber: String,
        securityType: String,
        password: String,
        pattern: String,
        batteryIncluded: Boolean,
        simIncluded: Boolean,
        memoryCardIncluded: Boolean,
        simTrayIncluded: Boolean,
        backCoverIncluded: Boolean,
        deadPhonePermission: Boolean,
        status: String,
        createdBy: String
    ): Result<Repair> {
        return try {
            // Pre-generate the document reference so we know the ID before committing.
            val newRepairRef = repairsCollection.document()

            val generatedSerial = firestore.runTransaction { transaction ->
                val counterSnapshot = transaction.get(counterDocRef)

                val lastSerial: Long = if (counterSnapshot.exists()) {
                    counterSnapshot.getLong(FirestorePaths.LAST_SERIAL_FIELD) ?: 0L
                } else {
                    0L
                }

                val newSerialNumber = lastSerial + 1

                transaction.set(
                    counterDocRef,
                    mapOf(FirestorePaths.LAST_SERIAL_FIELD to newSerialNumber)
                )

                val formattedSerial = formatSerialNumber(newSerialNumber)

                val repairData = hashMapOf<String, Any?>(
                    "id" to newRepairRef.id,
                    "serialNumber" to formattedSerial,
                    "customerName" to customerName,
                    "phoneNumber" to phoneNumber,
                    "deviceModel" to deviceModel,
                    "problemDescription" to problemDescription,
                    "expectedDeliveryDate" to expectedDeliveryDate,
                    "paymentInfo" to paymentInfo,
                    "additionalDetails" to additionalDetails,
                    "boxNumber" to boxNumber,
                    "status" to status,
                    "securityType" to securityType,
                    "password" to password,
                    "pattern" to pattern,
                    "batteryIncluded" to batteryIncluded,
                    "simIncluded" to simIncluded,
                    "memoryCardIncluded" to memoryCardIncluded,
                    "simTrayIncluded" to simTrayIncluded,
                    "backCoverIncluded" to backCoverIncluded,
                    "deadPhonePermission" to deadPhonePermission,
                    "photoCount" to 0,
                    "videoCount" to 0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "createdBy" to createdBy
                )

                transaction.set(newRepairRef, repairData)

                formattedSerial
            }.await()

            val createdRepair = Repair(
                id = newRepairRef.id,
                serialNumber = generatedSerial,
                customerName = customerName,
                phoneNumber = phoneNumber,
                deviceModel = deviceModel,
                problemDescription = problemDescription,
                expectedDeliveryDate = expectedDeliveryDate,
                paymentInfo = paymentInfo,
                additionalDetails = additionalDetails,
                boxNumber = boxNumber,
                status = status,
                securityType = securityType,
                password = password,
                pattern = pattern,
                batteryIncluded = batteryIncluded,
                simIncluded = simIncluded,
                memoryCardIncluded = memoryCardIncluded,
                simTrayIncluded = simTrayIncluded,
                backCoverIncluded = backCoverIncluded,
                deadPhonePermission = deadPhonePermission,
                photoCount = 0,
                videoCount = 0,
                createdAt = null,
                updatedAt = null,
                createdBy = createdBy
            )

            Result.success(createdRepair)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception(mapFirestoreException(e), e))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Network error. Please check your internet connection and try again.", e))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save repair record: ${e.localizedMessage ?: "Unknown error"}", e))
        }
    }

    /**
     * Updates an existing repair record. Deliberately excludes id, serialNumber,
     * createdAt and createdBy - those are immutable once a record is created.
     */
    suspend fun updateRepair(
        repairId: String,
        customerName: String,
        phoneNumber: String,
        deviceModel: String,
        problemDescription: String,
        expectedDeliveryDate: String,
        paymentInfo: String,
        additionalDetails: String,
        boxNumber: String,
        securityType: String,
        password: String,
        pattern: String,
        batteryIncluded: Boolean,
        simIncluded: Boolean,
        memoryCardIncluded: Boolean,
        simTrayIncluded: Boolean,
        backCoverIncluded: Boolean,
        deadPhonePermission: Boolean,
        status: String
    ): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any?>(
                "customerName" to customerName,
                "phoneNumber" to phoneNumber,
                "deviceModel" to deviceModel,
                "problemDescription" to problemDescription,
                "expectedDeliveryDate" to expectedDeliveryDate,
                "paymentInfo" to paymentInfo,
                "additionalDetails" to additionalDetails,
                "boxNumber" to boxNumber,
                "securityType" to securityType,
                "password" to password,
                "pattern" to pattern,
                "batteryIncluded" to batteryIncluded,
                "simIncluded" to simIncluded,
                "memoryCardIncluded" to memoryCardIncluded,
                "simTrayIncluded" to simTrayIncluded,
                "backCoverIncluded" to backCoverIncluded,
                "deadPhonePermission" to deadPhonePermission,
                "status" to status,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            repairsCollection.document(repairId).update(updates).await()
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception(mapFirestoreException(e), e))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Network error. Please check your internet connection and try again.", e))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update repair record: ${e.localizedMessage ?: "Unknown error"}", e))
        }
    }

    /**
     * Lightweight, dedicated status-only update so quick status changes from the
     * list or details screens don't need to ship the entire record over the wire.
     */
    suspend fun updateStatus(repairId: String, newStatus: String): Result<Unit> {
        return try {
            repairsCollection.document(repairId).update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception(mapFirestoreException(e), e))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Network error. Please check your internet connection and try again.", e))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update status: ${e.localizedMessage ?: "Unknown error"}", e))
        }
    }

    suspend fun deleteRepair(repairId: String): Result<Unit> {
        return try {
            repairsCollection.document(repairId).delete().await()
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception(mapFirestoreException(e), e))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Network error. Please check your internet connection and try again.", e))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete repair record: ${e.localizedMessage ?: "Unknown error"}", e))
        }
    }

    /**
     * One-time fetch of a single repair, used by the Edit screen to load initial values.
     */
    suspend fun getRepair(repairId: String): Result<Repair?> {
        return try {
            val snapshot = repairsCollection.document(repairId).get().await()
            Result.success(snapshot.toObject(Repair::class.java))
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception(mapFirestoreException(e), e))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Network error. Please check your internet connection and try again.", e))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to load repair record: ${e.localizedMessage ?: "Unknown error"}", e))
        }
    }

    /**
     * Realtime stream of all repairs, newest first. Backs the Customer List screen so
     * changes made on any signed-in device appear automatically, with no manual refresh
     * and no extra reads beyond what Firestore's listener already needs.
     */
    fun observeRepairs(): Flow<List<Repair>> = callbackFlow {
        val registration: ListenerRegistration = repairsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val repairs = snapshot?.documents?.mapNotNull { it.toObject(Repair::class.java) }
                    ?: emptyList()
                trySend(repairs)
            }

        awaitClose { registration.remove() }
    }

    /**
     * Realtime stream for a single repair document. Backs the Customer Details screen
     * so status changes (made here or on another device) reflect immediately.
     */
    fun observeRepair(repairId: String): Flow<Repair?> = callbackFlow {
        val registration: ListenerRegistration = repairsCollection.document(repairId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(Repair::class.java))
            }

        awaitClose { registration.remove() }
    }

    /**
     * Formats a raw long counter value into the "RM-000001" style serial number.
     */
    private fun formatSerialNumber(serial: Long): String {
        return String.format(Locale.US, "RM-%06d", serial)
    }

    /**
     * Converts low-level Firestore exceptions into meaningful, user-facing messages.
     */
    private fun mapFirestoreException(e: FirebaseFirestoreException): String {
        return when (e.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Unable to reach the server. Please check your internet connection and try again."
            FirebaseFirestoreException.Code.ABORTED ->
                "Could not save the repair record because of a conflicting update. Please try again."
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "You don't have permission to perform this action. Please contact the administrator."
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                "The request took too long. Please check your connection and try again."
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                "Service is temporarily busy. Please try again in a moment."
            FirebaseFirestoreException.Code.NOT_FOUND ->
                "This repair record could not be found. It may have been deleted."
            else ->
                "Operation failed (${e.code}). Please try again."
        }
    }
}