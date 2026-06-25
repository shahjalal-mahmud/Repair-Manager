// app/src/main/java/com/appriyo/repairmanager/data/model/StoreInfo.kt
package com.appriyo.repairmanager.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents the shop's display information stored in Firestore.
 *
 * Collection : storeInfo
 * Document   : main
 *
 * You populate this document manually in the Firebase console.
 * The app reads it at print time to build the invoice header.
 */
data class StoreInfo(
    @get:PropertyName("storeName") @set:PropertyName("storeName")
    var storeName: String = "",

    @get:PropertyName("address") @set:PropertyName("address")
    var address: String = "",

    @get:PropertyName("phone") @set:PropertyName("phone")
    var phone: String = "",

    @ServerTimestamp
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Date? = null
)