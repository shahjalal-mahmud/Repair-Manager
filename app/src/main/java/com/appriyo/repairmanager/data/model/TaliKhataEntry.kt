package com.appriyo.repairmanager.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * A single TaliKhata ledger entry, stored at users/{uid}/talikhata/{id}.
 *
 * Photos/proof attachments are NEVER stored here or anywhere in Firestore -
 * they live only in MediaStore via MediaStorageManager/MediaRepository,
 * keyed by draft id "TALIKHATA_{id}".
 *
 * [type] is stored as a raw Firestore string ([TaliKhataType.firestoreValue])
 * and exposed as a typed enum via [typeEnum] - this class never leaks a raw
 * string into the rest of the app.
 */
data class TaliKhataEntry(
    @DocumentId
    var id: String = "",
    val personName: String = "",
    val phoneNumber: String = "",
    val details: String = "",
    val balance: Double = 0.0,
    val type: String = TaliKhataType.THEY_OWE_YOU.firestoreValue,
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null,
    val createdBy: String = ""
) {
    val typeEnum: TaliKhataType
        get() = TaliKhataType.fromFirestoreValue(type)

    /** Draft id used for all MediaStorageManager/MediaRepository calls for this entry's photos. */
    val mediaDraftId: String
        get() = "TALIKHATA_$id"
}