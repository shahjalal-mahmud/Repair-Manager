// app/src/main/java/com/appriyo/repairmanager/util/ContactsHelper.kt
package com.appriyo.repairmanager.util

import android.content.ContentProviderOperation
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Thin wrapper around [ContactsContract] for the Add Repair phonebook feature.
 *
 * Two responsibilities:
 *  1. Parse the URI returned by the system contact picker into a
 *     [PickedContact] (display name + phone number).
 *  2. Insert a new raw contact into the device's Contacts app via
 *     `applyBatch`, requiring the `WRITE_CONTACTS` runtime permission.
 *
 * No state, no caching - every method is a one-shot call against the
 * ContentResolver.
 */
object ContactsHelper {

    private const val TAG = "ContactsHelper"

    /**
     * Result of parsing a URI returned by the system contact picker.
     *
     * @param displayName the contact's primary display name
     * @param phoneNumber the contact's first (or primary) phone number,
     *                    already stripped by the ContentResolver
     */
    data class PickedContact(
        val displayName: String,
        val phoneNumber: String
    )

    /**
     * Reads a phone-data URI returned by an ACTION_PICK intent typed to
     * [ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE].
     *
     * Unlike the generic Contacts picker, this URI IS a phone data row
     * (content://com.android.contacts/data/<id>), so DISPLAY_NAME and NUMBER
     * are queryable directly - no CONTACT_ID lookup, no sub-path permission
     * assumptions. Requires READ_CONTACTS to be granted before the picker
     * is launched.
     */
    fun queryPickedContact(context: Context, phoneDataUri: Uri): PickedContact? {
        if (phoneDataUri == Uri.EMPTY) return null

        return runCatching {
            context.contentResolver.query(
                phoneDataUri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null, null
            )
        }.getOrNull()?.use { c ->
            if (!c.moveToFirst()) return null
            val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val name = if (nameIdx >= 0) c.getString(nameIdx) else null
            val number = if (numIdx >= 0) c.getString(numIdx) else null
            if (!name.isNullOrBlank() && !number.isNullOrBlank()) {
                PickedContact(name, number)
            } else null
        }
    }

    /**
     * Inserts a single raw contact (display name + mobile phone) into the
     * device's Contacts app via a single [ContactsContract.applyBatch] call.
     *
     * Uses local-only storage (`ACCOUNT_TYPE = null`, `ACCOUNT_NAME = null`)
     * so no Google account sync is required.
     *
     * Must be called from a coroutine - runs on [Dispatchers.IO] so it never
     * blocks the UI thread.
     *
     * Returns `false` on blank input or on any thrown exception (logged).
     * Callers should still consider the surrounding operation (e.g. a
     * Firestore repair save) successful if this returns false - the
     * phonebook is a side effect, not the source of truth.
     */
    suspend fun saveToPhonebook(
        context: Context,
        displayName: String,
        phoneNumber: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (displayName.isBlank() || phoneNumber.isBlank()) return@withContext false

        val ops = ArrayList<ContentProviderOperation>()

        // Op 1 - create the raw contact row. withValueBackReference below
        // binds to the index of this op (0) to get the new RAW_CONTACT_ID.
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
        )

        // Op 2 - structured name row.
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                )
                .withValue(
                    ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                    displayName
                )
                .build()
        )

        // Op 3 - mobile phone row.
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                )
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                .withValue(
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                )
                .build()
        )

        try {
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert contact '$displayName' / '$phoneNumber'", e)
            false
        }
    }
}