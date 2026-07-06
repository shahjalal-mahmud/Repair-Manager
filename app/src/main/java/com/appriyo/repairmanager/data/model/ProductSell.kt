// app/src/main/java/com/appriyo/repairmanager/data/model/ProductSell.kt
package com.appriyo.repairmanager.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents a single product sell record / invoice entry.
 *
 * Each record corresponds to one transaction where the shop sold a product
 * (with optional warranty) to a customer. The same record backs the
 * printable POS invoice.
 *
 * **Data isolation:** Stored under users/{uid}/productSells, just like
 * every other per-account collection in this app, so that each Google
 * account only ever reads/writes its own data.
 *
 * @author RepairManager Team
 * @since 1.5.0
 */
data class ProductSell(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    /** Human-readable serial / invoice number (e.g. "PS-000001"). */
    @get:PropertyName("serialNumber") @set:PropertyName("serialNumber")
    var serialNumber: String = "",

    /** Product name entered by the shopkeeper. */
    @get:PropertyName("productName") @set:PropertyName("productName")
    var productName: String = "",

    /** Selling price of the product (BDT). */
    @get:PropertyName("productPrice") @set:PropertyName("productPrice")
    var productPrice: Double = 0.0,

    /** Amount actually paid by the customer for this sale. */
    @get:PropertyName("paymentAmount") @set:PropertyName("paymentAmount")
    var paymentAmount: Double = 0.0,

    /**
     * Warranty period in months (0 = no warranty). Used to compute the
     * warranty expiry date when the warranty start date is provided.
     */
    @get:PropertyName("warrantyMonths") @set:PropertyName("warrantyMonths")
    var warrantyMonths: Int = 0,

    /** Date when the warranty starts (typically the sell date), formatted dd/MM/yyyy. */
    @get:PropertyName("warrantyStartDate") @set:PropertyName("warrantyStartDate")
    var warrantyStartDate: String = "",

    /**
     * Optional serial / IMEI of the sold product. Helps identify the
     * specific unit when honouring a warranty claim.
     */
    @get:PropertyName("productSerial") @set:PropertyName("productSerial")
    var productSerial: String = "",

    /** Optional warranty terms / conditions (free text, printed on the invoice). */
    @get:PropertyName("warrantyDetails") @set:PropertyName("warrantyDetails")
    var warrantyDetails: String = "",

    /** Optional additional notes (e.g. accessories included, colour, etc.). */
    @get:PropertyName("notes") @set:PropertyName("notes")
    var notes: String = "",

    @ServerTimestamp
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Date? = null,

    @get:PropertyName("createdBy") @set:PropertyName("createdBy")
    var createdBy: String = ""
)

/**
 * Collection path constants used UNDER users/{uid} for product sell records.
 */
object ProductSellFirestorePaths {
    const val PRODUCT_SELLS_COLLECTION = "productSells"
    const val PRODUCT_SELL_COUNTER_COLLECTION = "productSellCounter"
    const val PRODUCT_SELL_COUNTER_DOC = "counter"
    const val LAST_SERIAL_FIELD = "lastSerial"
}
