// app/src/main/java/com/appriyo/repairmanager/data/repository/ProductSellRepository.kt
package com.appriyo.repairmanager.data.repository

import com.appriyo.repairmanager.data.model.ProductSell
import com.appriyo.repairmanager.data.model.ProductSellFirestorePaths
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.Locale

/**
 * Repository for managing product sell / invoice records in Firestore.
 *
 * **Data isolation:** All collections live under users/{uid}, just like
 * the repair / notes / cashbox repositories, so each Google account only
 * ever reads/writes its own data while multiple devices on the same
 * account continue to share and sync the same underlying documents.
 *
 * **Key features:**
 * - Transactional serial number generation (prevents duplicate invoice IDs)
 * - Real-time sync across devices via Flow APIs
 * - Price / payment / warranty are stored as free text (see [ProductSell])
 *   since this feature only needs to produce a printed invoice, not do
 *   numeric accounting.
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
     *
     * [productPrice], [paymentAmount], and [warrantyMonths] are stored
     * exactly as typed (free text) - no numeric parsing is performed.
     */
    suspend fun createProductSell(
        productName: String,
        productPrice: String,
        paymentAmount: String,
        warrantyMonths: String,
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
                "productPrice" to productPrice.trim(),
                "paymentAmount" to paymentAmount.trim(),
                "warrantyMonths" to warrantyMonths.trim(),
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
            productPrice = productPrice.trim(),
            paymentAmount = paymentAmount.trim(),
            warrantyMonths = warrantyMonths.trim(),
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
        snapshot.documents.mapNotNull { it.toProductSellOrNull() }
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
                        ?.mapNotNull { it.toProductSellOrNull() }
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

/**
 * Manual [ProductSell] mapper for a [DocumentSnapshot].
 *
 * Replaces Firestore's reflection-based `toObject(ProductSell::class.java)`,
 * which used to crash when an older document had `productPrice`,
 * `paymentAmount`, or `warrantyMonths` stored as numeric values (Long /
 * Double) — Firestore's mapper auto-coerces Long → Int / Long → Double /
 * Double → Int / etc., but it does **not** coerce Long → String and throws
 * `Failed to convert value of type java.lang.Long to String` instead.
 *
 * Since those three fields are now free-text on the model anyway
 * (see [ProductSell] docs), the safest fix is to read each field manually
 * and coerce any numeric / timestamp-shaped value into a display string.
 * Old numeric Firestore values and new string values are both handled, so
 * no data migration is required and no crash occurs on mixed collections.
 *
 * Other fields (id, serialNumber, productName, dates, optional
 * serial/details/notes) keep their original types and are read via plain
 * `getString(...)` — a single missing/mismatched field for those would only
 * produce `null`/empty defaults, never a crash.
 */
private fun DocumentSnapshot.toProductSellOrNull(): ProductSell? {
    // Use the document id as a fallback - even if every other field read
    // returns null we want this snapshot to surface as an empty entry
    // rather than silently dropping it.
    val fallbackId = id

    fun asFlexibleText(value: Any?): String = when (value) {
        null -> ""
        is String -> value
        is Number -> value.toString()
        is Boolean -> value.toString()
        // Firestore Timestamps do come back as Timestamp when the field is
        // `@ServerTimestamp` or typed as Timestamp server-side, but a defensive
        // toString() keeps us safe against any other odd shape (e.g. Date).
        is Timestamp -> value.toDate().time.let { Date(it).toString() }
        else -> value.toString()
    }

    return ProductSell(
        id = getString("id") ?: fallbackId,
        serialNumber = getString("serialNumber").orEmpty(),
        productName = getString("productName").orEmpty(),
        productPrice = asFlexibleText(get("productPrice")),
        paymentAmount = asFlexibleText(get("paymentAmount")),
        warrantyMonths = asFlexibleText(get("warrantyMonths")),
        warrantyStartDate = getString("warrantyStartDate").orEmpty(),
        productSerial = getString("productSerial").orEmpty(),
        warrantyDetails = getString("warrantyDetails").orEmpty(),
        notes = getString("notes").orEmpty(),
        // createdAt can be null (newly created doc, server timestamp pending),
        // a Timestamp, or, after manual edits in the console, sometimes a
        // String - handle all three without throwing.
        createdAt = when (val raw = get("createdAt")) {
            is Timestamp -> raw.toDate()
            is Date -> raw
            else -> null
        },
        createdBy = getString("createdBy").orEmpty()
    )
}