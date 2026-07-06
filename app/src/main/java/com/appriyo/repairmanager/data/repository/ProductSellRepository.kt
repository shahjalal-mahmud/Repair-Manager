// app/src/main/java/com/appriyo/repairmanager/data/repository/ProductSellRepository.kt
package com.appriyo.repairmanager.data.repository

import com.appriyo.repairmanager.data.model.ProductSell
import com.appriyo.repairmanager.data.model.ProductSellFirestorePaths
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
 * Repository for managing product sell / invoice records in Firestore.
 *
 * **Data isolation:** All collections live under users/{uid}, just like the
 * repair / notes / cashbox repositories, so each Google account only ever
 * reads/writes its own data while multiple devices on the same account
 * continue to share and sync the same underlying documents.
 *
 * **Key features:**
 * - Transactional serial number generation (prevents duplicate invoice IDs)
 * - Real-time sync across devices via Flow APIs
 *
 * @author RepairManager Team
 * @since 1.5.0
 */
class ProductSellRepository(
    private val firestore: FirebaseFirestore,
    private val userProvider: FirestoreUserProvider
) {

    // ======================== COLLECTION REFERENCES ========================

    private val productSellsCollection
        get() = userProvider.currentUserDocument()
            .collection(ProductSellFirestorePaths.PRODUCT_SELLS_COLLECTION)

    private val counterDocRef
        get() = userProvider.currentUserDocument()
            .collection(ProductSellFirestorePaths.PRODUCT_SELL_COUNTER_COLLECTION)
            .document(ProductSellFirestorePaths.PRODUCT_SELL_COUNTER_DOC)

    // ======================== CRUD OPERATIONS ========================

    /**
     * Creates a new product sell record with an automatically generated
     * serial number in a single Firestore transaction. This guarantees
     * consistent serial numbers even with concurrent writes from multiple
     * devices signed into the same account.
     */
    suspend fun createProductSell(
        productName: String,
        productPrice: Double,
        paymentAmount: Double,
        warrantyMonths: Int,
        warrantyStartDate: String,
        productSerial: String,
        warrantyDetails: String,
        notes: String,
        createdBy: String
    ): Result<ProductSell> = runCatching {
        val newDocRef = productSellsCollection.document()
        val counterRef = counterDocRef

        val generatedSerial = firestore.runTransaction { transaction ->
            val counterSnapshot = transaction.get(counterRef)
            val lastSerial = counterSnapshot.getLong(ProductSellFirestorePaths.LAST_SERIAL_FIELD) ?: 0L
            val newSerialNumber = lastSerial + 1

            transaction.set(
                counterRef,
                mapOf(ProductSellFirestorePaths.LAST_SERIAL_FIELD to newSerialNumber)
            )

            val formattedSerial = formatSerialNumber(newSerialNumber)

            val data = hashMapOf<String, Any?>(
                "id" to newDocRef.id,
                "serialNumber" to formattedSerial,
                "productName" to productName.trim(),
                "productPrice" to productPrice,
                "paymentAmount" to paymentAmount,
                "warrantyMonths" to warrantyMonths,
                "warrantyStartDate" to warrantyStartDate.trim(),
                "productSerial" to productSerial.trim(),
                "warrantyDetails" to warrantyDetails.trim(),
                "notes" to notes.trim(),
                "createdAt" to FieldValue.serverTimestamp(),
                "createdBy" to createdBy
            )

            transaction.set(newDocRef, data)
            formattedSerial
        }.await()

        ProductSell(
            id = newDocRef.id,
            serialNumber = generatedSerial,
            productName = productName.trim(),
            productPrice = productPrice,
            paymentAmount = paymentAmount,
            warrantyMonths = warrantyMonths,
            warrantyStartDate = warrantyStartDate.trim(),
            productSerial = productSerial.trim(),
            warrantyDetails = warrantyDetails.trim(),
            notes = notes.trim(),
            createdAt = null,
            createdBy = createdBy
        )
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { handleException(it) }
    )

    /**
     * Deletes a product sell record. Currently used by the list screen for
     * cleanup / corrections. Note: this only removes the Firestore
     * document; it cannot un-print a previously printed invoice.
     */
    suspend fun deleteProductSell(productSellId: String): Result<Unit> = runCatching {
        productSellsCollection.document(productSellId).delete().await()
        Result.success(Unit)
    }.fold(
        onSuccess = { it },
        onFailure = { handleException(it) }
    )

    // ======================== QUERY OPERATIONS ========================

    /** One-time fetch of all product sells (used for export/backups). */
    suspend fun getAllProductSellsOnce(): Result<List<ProductSell>> = runCatching {
        val snapshot = productSellsCollection.get().await()
        snapshot.documents.mapNotNull { it.toObject(ProductSell::class.java) }
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { handleException(it) }
    )

    // ======================== REALTIME STREAMS ========================

    /**
     * Provides a real-time stream of all product sell records, newest
     * first (ordered by createdAt descending).
     */
    fun observeProductSells(): Flow<List<ProductSell>> = callbackFlow {
        val registration: ListenerRegistration
        try {
            registration = productSellsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val items = snapshot?.documents
                        ?.mapNotNull { it.toObject(ProductSell::class.java) }
                        ?: emptyList()
                    trySend(items)
                }
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        awaitClose { registration.remove() }
    }

    // ======================== PRIVATE HELPERS ========================

    /**
     * Formats a raw counter value into the PS-XXXXXX serial format.
     */
    private fun formatSerialNumber(serial: Long): String {
        return String.format(Locale.US, "PS-%06d", serial)
    }

    /**
     * Maps low-level Firestore exceptions (and the local
     * [NotAuthenticatedException]) to user-friendly error messages.
     */
    private fun <T> handleException(e: Throwable): Result<T> {
        return when (e) {
            is FirebaseFirestoreException -> {
                val message = when (e.code) {
                    FirebaseFirestoreException.Code.UNAVAILABLE ->
                        "Unable to reach the server. Please check your internet connection and try again."
                    FirebaseFirestoreException.Code.ABORTED ->
                        "Could not save the sale because of a conflicting update. Please try again."
                    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        "You don't have permission to perform this action."
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                        "The request took too long. Please check your connection and try again."
                    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                        "Service is temporarily busy. Please try again in a moment."
                    FirebaseFirestoreException.Code.NOT_FOUND ->
                        "This sale record could not be found."
                    else ->
                        "Operation failed (${e.code}). Please try again."
                }
                Result.failure(Exception(message, e))
            }
            is FirebaseNetworkException -> {
                Result.failure(Exception("Network error. Please check your internet connection and try again.", e))
            }
            is NotAuthenticatedException -> {
                Result.failure(e)
            }
            else -> {
                Result.failure(Exception("Failed to complete operation: ${e.localizedMessage ?: "Unknown error"}", e))
            }
        }
    }
}
