// ============================================================================
// REPAIR REPOSITORY
// ============================================================================

package com.appriyo.repairmanager.data.repository

import com.appriyo.repairmanager.data.model.FirestorePaths
import com.appriyo.repairmanager.data.model.Repair
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.DocumentChange
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
 * Repository for managing repair records in Firestore.
 *
 * **Key Features:**
 * - Transactional serial number generation (prevents duplicate IDs)
 * - Real-time sync across devices via Flow APIs
 * - Immutable fields: id, serialNumber, createdAt, createdBy
 * - Optimized status updates (partial updates)
 *
 * **Collections Used:**
 * - "repairs": Main collection for repair records
 * - "counters": Single document for atomic serial number generation
 *
 * **Thread Safety:** All operations are suspend functions and should be called
 * from a coroutine scope (e.g., ViewModelScope).
 *
 * @author RepairManager Team
 * @since 1.0.0
 */
class RepairRepository(
    private val firestore: FirebaseFirestore
) {

    // ======================== COLLECTION REFERENCES ========================

    private val repairsCollection
        get() = firestore.collection(FirestorePaths.REPAIRS_COLLECTION)

    private val counterDocRef
        get() = firestore
            .collection(FirestorePaths.COUNTERS_COLLECTION)
            .document(FirestorePaths.REPAIR_COUNTER_DOC)

    // ======================== CRUD OPERATIONS ========================

    /**
     * Creates a new repair record with an automatically generated serial number.
     *
     * **Atomicity:** Serial generation and document creation happen in a single
     * Firestore transaction, ensuring consistency even with concurrent writes
     * from multiple devices.
     *
     * @param customerName Full name of the customer
     * @param phoneNumber Contact phone number
     * @param deviceModel Model of the device
     * @param problemDescription Description of the issue
     * @param expectedDeliveryDate Estimated completion date
     * @param paymentInfo Payment details
     * @param additionalDetails Supplementary notes
     * @param boxNumber Storage box identifier
     * @param securityType Lock screen security type
     * @param password Device password (if applicable)
     * @param pattern Device pattern lock (if applicable)
     * @param batteryIncluded Whether battery was included
     * @param simIncluded Whether SIM card was included
     * @param memoryCardIncluded Whether memory card was included
     * @param simTrayIncluded Whether SIM tray was included
     * @param backCoverIncluded Whether back cover was included
     * @param deadPhonePermission Permission to work on dead phone
     * @param status Initial repair status
     * @param createdBy User identifier who created this record
     * @return Result containing the created Repair object or an error
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
    ): Result<Repair> = runCatching {
        val newRepairRef = repairsCollection.document()

        val generatedSerial = firestore.runTransaction { transaction ->
            val counterSnapshot = transaction.get(counterDocRef)
            val lastSerial = counterSnapshot.getLong(FirestorePaths.LAST_SERIAL_FIELD) ?: 0L
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

        Repair(
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
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { handleException(it) }
    )

    /**
     * Updates an existing repair record.
     *
     * **Immutable Fields:** id, serialNumber, createdAt, createdBy cannot be updated.
     *
     * @param repairId The ID of the repair to update
     * @param customerName Updated customer name
     * @param phoneNumber Updated phone number
     * @param deviceModel Updated device model
     * @param problemDescription Updated problem description
     * @param expectedDeliveryDate Updated delivery date
     * @param paymentInfo Updated payment info
     * @param additionalDetails Updated additional notes
     * @param boxNumber Updated box number
     * @param securityType Updated security type
     * @param password Updated password
     * @param pattern Updated pattern
     * @param batteryIncluded Updated battery inclusion status
     * @param simIncluded Updated SIM inclusion status
     * @param memoryCardIncluded Updated memory card inclusion status
     * @param simTrayIncluded Updated SIM tray inclusion status
     * @param backCoverIncluded Updated back cover inclusion status
     * @param deadPhonePermission Updated dead phone permission
     * @param status Updated status
     * @return Result indicating success or failure
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
    ): Result<Unit> = runCatching {
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
    }.fold(
        onSuccess = { it },
        onFailure = { handleException(it) }
    )

    /**
     * Updates only the status of a repair record.
     *
     * This lightweight operation is optimized for quick status changes from
     * list or detail screens without sending the entire record over the wire.
     *
     * @param repairId The ID of the repair to update
     * @param newStatus The new status value
     * @return Result indicating success or failure
     */
    suspend fun updateStatus(repairId: String, newStatus: String): Result<Unit> = runCatching {
        repairsCollection.document(repairId).update(
            mapOf(
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    }.fold(
        onSuccess = { it },
        onFailure = { handleException(it) }
    )

    /**
     * Deletes a repair record from Firestore.
     *
     * **Warning:** This operation is irreversible. Consider archiving instead.
     *
     * @param repairId The ID of the repair to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteRepair(repairId: String): Result<Unit> = runCatching {
        repairsCollection.document(repairId).delete().await()
        Result.success(Unit)
    }.fold(
        onSuccess = { it },
        onFailure = { handleException(it) }
    )

    // ======================== QUERY OPERATIONS ========================

    /**
     * Fetches a single repair record by ID.
     *
     * **Note:** This is a one-time fetch. For real-time updates, use [observeRepair].
     *
     * @param repairId The ID of the repair to fetch
     * @return Result containing the Repair object or null if not found
     */
    suspend fun getRepair(repairId: String): Result<Repair?> = runCatching {
        val snapshot = repairsCollection.document(repairId).get().await()
        snapshot.toObject(Repair::class.java)
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { handleException(it) }
    )

    // ======================== REALTIME STREAMS ========================

    /**
     * Provides a real-time stream of all repair records.
     *
     * **Usage:** Subscribe in the Customer List screen for automatic updates
     * across all signed-in devices. Changes appear without manual refresh.
     *
     * **Ordering:** Newest records first (ordered by createdAt descending).
     *
     * @return Flow emitting the complete list of repairs on every change
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
     * Provides a real-time stream for a single repair record.
     *
     * **Usage:** Subscribe in the Customer Details screen to reflect status
     * changes made on any device.
     *
     * @param repairId The ID of the repair to observe
     * @return Flow emitting the repair record or null if not found
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
     * Provides a real-time stream of newly added or modified repairs.
     *
     * **Usage:** Used exclusively by the SMS auto-send system to detect changes
     * without scanning the entire collection on every snapshot.
     *
     * @return Flow emitting changed repair records
     */
    fun observeRepairChanges(): Flow<List<Repair>> = callbackFlow {
        val registration: ListenerRegistration = repairsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val changed = snapshot?.documentChanges
                    ?.filter { it.type == DocumentChange.Type.ADDED || it.type == DocumentChange.Type.MODIFIED }
                    ?.mapNotNull { it.document.toObject(Repair::class.java) }
                    ?: emptyList()
                if (changed.isNotEmpty()) {
                    trySend(changed)
                }
            }

        awaitClose { registration.remove() }
    }

    // ======================== PRIVATE HELPERS ========================

    /**
     * Formats a raw counter value into the RM-XXXXXX serial format.
     *
     * @param serial The numeric serial value
     * @return Formatted serial string (e.g., "RM-000001")
     */
    private fun formatSerialNumber(serial: Long): String {
        return String.format(Locale.US, "RM-%06d", serial)
    }

    /**
     * Maps low-level Firestore exceptions to user-friendly error messages.
     *
     * @param e The caught exception
     * @return Result containing an error message
     */
    private fun <T> handleException(e: Throwable): Result<T> {
        return when (e) {
            is FirebaseFirestoreException -> {
                val message = when (e.code) {
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
                Result.failure(Exception(message, e))
            }
            is FirebaseNetworkException -> {
                Result.failure(Exception("Network error. Please check your internet connection and try again.", e))
            }
            else -> {
                Result.failure(Exception("Failed to complete operation: ${e.localizedMessage ?: "Unknown error"}", e))
            }
        }
    }
}