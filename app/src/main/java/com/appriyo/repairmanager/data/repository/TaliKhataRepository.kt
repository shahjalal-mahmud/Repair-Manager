package com.appriyo.repairmanager.data.repository

import com.appriyo.repairmanager.data.model.TaliKhataEntry
import com.appriyo.repairmanager.data.model.TaliKhataFirestorePaths
import com.appriyo.repairmanager.data.model.TaliKhataHistoryEntry
import com.appriyo.repairmanager.data.model.TaliKhataHistoryOperation
import com.appriyo.repairmanager.data.model.TaliKhataType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Manages users/{uid}/talikhata and its per-entry
 * users/{uid}/talikhata/{entryId}/history subcollection.
 *
 * Follows the same realtime-listener + Result<T> pattern as
 * EmployeeNotesRepository / SmsLogRepository. Never touches FirebaseAuth
 * directly - every path is resolved through [FirestoreUserProvider], so data
 * is automatically isolated per signed-in account.
 */
class TaliKhataRepository(
    private val firestore: FirebaseFirestore,
    private val userProvider: FirestoreUserProvider
) {

    private val collection
        get() = userProvider.currentUserDocument()
            .collection(TaliKhataFirestorePaths.TALIKHATA_COLLECTION)

    private fun historyCollection(entryId: String) =
        collection.document(entryId).collection(TaliKhataFirestorePaths.HISTORY_SUBCOLLECTION)

    /**
     * Client-side generated Firestore document id. Generated with no network
     * call, so the UI can use it as the MediaStorageManager draft id (via
     * "TALIKHATA_{id}") for photos attached *before* the entry is saved.
     */
    fun generateEntryId(): String = collection.document().id

    /** Realtime listener for all entries, newest first (ViewModel re-sorts/filters as needed). */
    fun observeEntries(): Flow<Result<List<TaliKhataEntry>>> = callbackFlow {
        val registration: ListenerRegistration = collection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val entries = snapshot?.documents
                    ?.mapNotNull { it.toObject(TaliKhataEntry::class.java) }
                    ?: emptyList()
                trySend(Result.success(entries))
            }
        awaitClose { registration.remove() }
    }

    /** Realtime listener for one entry's history, oldest first (chronological). */
    fun observeHistory(entryId: String): Flow<Result<List<TaliKhataHistoryEntry>>> = callbackFlow {
        val registration: ListenerRegistration = historyCollection(entryId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val history = snapshot?.documents
                    ?.mapNotNull { it.toObject(TaliKhataHistoryEntry::class.java) }
                    ?: emptyList()
                trySend(Result.success(history))
            }
        awaitClose { registration.remove() }
    }

    /**
     * Creates a new entry at the given (pre-generated) [entryId] with an
     * initial "Created" history record.
     */
    suspend fun createEntry(
        entryId: String,
        personName: String,
        phoneNumber: String,
        details: String,
        initialBalance: Double,
        type: TaliKhataType,
        createdBy: String
    ): Result<Unit> {
        return try {
            val data = hashMapOf(
                "personName" to personName,
                "phoneNumber" to phoneNumber,
                "details" to details,
                "balance" to initialBalance,
                "type" to type.firestoreValue,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "createdBy" to createdBy
            )
            collection.document(entryId).set(data).await()
            addHistoryRecord(
                entryId = entryId,
                operation = TaliKhataHistoryOperation.CREATED,
                amount = initialBalance,
                balanceAfter = initialBalance
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Updates identity fields (name/phone/details/type) - never touches balance directly. */
    suspend fun updateDetails(
        entryId: String,
        personName: String,
        phoneNumber: String,
        details: String,
        type: TaliKhataType
    ): Result<Unit> {
        return try {
            collection.document(entryId).update(
                mapOf(
                    "personName" to personName,
                    "phoneNumber" to phoneNumber,
                    "details" to details,
                    "type" to type.firestoreValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Applies a balance adjustment (+ or -) transactionally, rejects it if it
     * would push the balance negative, and writes an auditable history
     * record with the resulting balance.
     */
    suspend fun adjustBalance(
        entryId: String,
        adjustmentAmount: Double,
        isIncrease: Boolean
    ): Result<Double> {
        return try {
            val entryRef = collection.document(entryId)
            val newBalance = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(entryRef)
                val currentBalance = snapshot.getDouble("balance") ?: 0.0
                val updated = if (isIncrease) currentBalance + adjustmentAmount
                else currentBalance - adjustmentAmount
                if (updated < 0) {
                    throw IllegalArgumentException("Adjustment exceeds current balance.")
                }
                transaction.update(
                    entryRef,
                    mapOf(
                        "balance" to updated,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                updated
            }.await()

            addHistoryRecord(
                entryId = entryId,
                operation = if (isIncrease) TaliKhataHistoryOperation.INCREASE else TaliKhataHistoryOperation.DECREASE,
                amount = if (isIncrease) adjustmentAmount else -adjustmentAmount,
                balanceAfter = newBalance
            )
            Result.success(newBalance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Deletes an entry and its full history subcollection. Photos are untouched (MediaStore-only, no cascade needed for Firestore). */
    suspend fun deleteEntry(entryId: String): Result<Unit> {
        return try {
            val historyDocs = historyCollection(entryId).get().await()
            for (doc in historyDocs.documents) {
                doc.reference.delete().await()
            }
            collection.document(entryId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun addHistoryRecord(
        entryId: String,
        operation: TaliKhataHistoryOperation,
        amount: Double,
        balanceAfter: Double
    ) {
        val historyRef = historyCollection(entryId).document()
        val data = hashMapOf(
            "operation" to operation.firestoreValue,
            "amount" to amount,
            "balanceAfter" to balanceAfter,
            "timestamp" to FieldValue.serverTimestamp()
        )
        historyRef.set(data).await()
    }
}