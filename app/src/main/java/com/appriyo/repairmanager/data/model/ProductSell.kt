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
 * **Free-text money / warranty fields:** [productPrice], [paymentAmount],
 * and [warrantyMonths] are intentionally plain [String] fields, not
 * numbers. This screen is only used to produce a printed invoice, so the
 * shopkeeper can type whatever they want in these fields - digits, Bangla
 * digits, words like "Free" or "Negotiable", etc. Nothing is parsed or
 * validated as a number; whatever was typed is what gets printed.
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

    /**
     * Selling price of the product, exactly as typed by the shopkeeper
     * (e.g. "1500", "১৫০০", "Negotiable"). Free text - printed as-is.
     */
    @get:PropertyName("productPrice") @set:PropertyName("productPrice")
    var productPrice: String = "",

    /**
     * Amount paid by the customer, exactly as typed by the shopkeeper.
     * Free text - printed as-is.
     */
    @get:PropertyName("paymentAmount") @set:PropertyName("paymentAmount")
    var paymentAmount: String = "",

    /**
     * Warranty period, exactly as typed by the shopkeeper (e.g. "6",
     * "৬ মাস", "No warranty"). Free text - printed as-is.
     */
    @get:PropertyName("warrantyMonths") @set:PropertyName("warrantyMonths")
    var warrantyMonths: String = "",

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