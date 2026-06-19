// app/src/main/java/com/appriyo/repairmanager/data/repository/RepairRepository.kt
package com.appriyo.repairmanager.data.repository

import com.appriyo.repairmanager.data.model.FirestorePaths
import com.appriyo.repairmanager.data.model.Repair
import com.appriyo.repairmanager.data.model.RepairStatus
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import java.util.Locale

/**
 * Repository responsible for all Firestore operations related to repair records.
 *
 * Responsibilities:
 *  - Generate a unique, sequential, human-readable serial number (RM-000001, RM-000002, ...)
 *    using a Firestore transaction so that concurrent writes from multiple devices
 *    never produce duplicate serial numbers.
 *  - Persist the repair record document in the "repairs" collection.
 *
 * Both the serial generation AND the repair document creation happen inside a
 * SINGLE Firestore transaction. This guarantees that if the counter increments,
 * the repair document is also written - and vice versa - which keeps the counter
 * and the actual repair records perfectly consistent, even under concurrent access
 * from multiple shop devices, and works entirely within the Firebase Free (Spark) tier.
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
     * Creates a new repair record:
     *  1. Pre-generates a new document reference (so we have an ID before the transaction).
     *  2. Inside a single Firestore transaction:
     *     a. Reads the current counter value (creating it on first use if missing).
     *     b. Increments it.
     *     c. Writes the new counter value.
     *     d. Writes the new repair document with the generated serial number.
     *  3. Returns the fully created [Repair] object on success.
     *
     * @return [Result] wrapping the created [Repair] on success, or a meaningful
     * exception with a user-friendly message on failure.
     */
    suspend fun createRepair(
        customerName: String,
        phoneNumber: String,
        deviceName: String,
        problem: String,
        expectedDeliveryDate: String,
        paymentInfo: String,
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

                // Update (or create, on first run) the counter document.
                transaction.set(
                    counterDocRef,
                    mapOf(FirestorePaths.LAST_SERIAL_FIELD to newSerialNumber)
                )

                val formattedSerial = formatSerialNumber(newSerialNumber)

                val repairData = hashMapOf(
                    "id" to newRepairRef.id,
                    "serialNumber" to formattedSerial,
                    "customerName" to customerName,
                    "phoneNumber" to phoneNumber,
                    "deviceName" to deviceName,
                    "problem" to problem,
                    "expectedDeliveryDate" to expectedDeliveryDate,
                    "paymentInfo" to paymentInfo,
                    "status" to RepairStatus.PENDING,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "createdBy" to createdBy
                )

                transaction.set(newRepairRef, repairData)

                // Return the serial number as the transaction's result.
                formattedSerial
            }.await()

            val createdRepair = Repair(
                id = newRepairRef.id,
                serialNumber = generatedSerial,
                customerName = customerName,
                phoneNumber = phoneNumber,
                deviceName = deviceName,
                problem = problem,
                expectedDeliveryDate = expectedDeliveryDate,
                paymentInfo = paymentInfo,
                status = RepairStatus.PENDING,
                createdAt = null, // populated server-side; not needed immediately on success
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
     * Formats a raw long counter value into the "RM-000001" style serial number.
     * Example: 1 -> RM-000001, 152 -> RM-000152, 9999 -> RM-009999
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
                "You don't have permission to save repair records. Please contact the administrator."
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                "The request took too long. Please check your connection and try again."
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                "Service is temporarily busy. Please try again in a moment."
            else ->
                "Failed to save repair record (${e.code}). Please try again."
        }
    }
}