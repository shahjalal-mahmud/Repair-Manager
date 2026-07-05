// app/src/main/java/com/appriyo/repairmanager/data/model/SecurityPin.kt
package com.appriyo.repairmanager.data.model

import com.google.firebase.firestore.PropertyName
import java.util.Date

/**
 * Firestore document: users/{uid}/security/pin
 *
 * Stores the hashed owner PIN plus its per-user random salt. The plain-text
 * PIN is never persisted - only [pinHash] (SHA-256 of salt + pin, hex) lives
 * in Firestore.
 *
 * The default PIN is "000000" and is seeded automatically the first time the
 * app needs to read the PIN for a given user.
 */
data class SecurityPin(
    @get:PropertyName("pinHash") @set:PropertyName("pinHash")
    var pinHash: String = "",

    @get:PropertyName("salt") @set:PropertyName("salt")
    var salt: String = "",

    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Date? = null
)