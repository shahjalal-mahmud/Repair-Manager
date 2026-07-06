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
     * Reads a contact URI returned by [androidx.activity.result.contract.ActivityResultContracts.PickContact]
     * and returns the contact's display name + phone number.
     *
     * IMPORTANT - the picker URI's shape is **not** stable across Android
     * versions and OEM skins. Concretely, the URI handed back is usually
     *   content://com.android.contacts/contacts/lookup/<lookupKey>/<contactId>
     * Querying that URI directly with [ContactsContract.CommonDataKinds.Phone]
     * columns (NUMBER, CONTACT_ID, ...) silently returns an empty cursor on
     * AOSP Android 10+ and most OEMs - those columns simply aren't exposed
     * through the lookup URI. That's why a naive query always returned
     * "no phone number" even though the contact definitely has one.
     *
     * The reliable approach is two queries:
     *   1. Read [ContactsContract.Contacts._ID] (and DISPLAY_NAME) from the
     *      picker URI. The lookup URI always exposes `_ID` and `DISPLAY_NAME`.
     *   2. With that contactId, query
     *      [ContactsContract.CommonDataKinds.Phone.CONTENT_URI] filtered by
     *      `CONTACT_ID = ?` to fetch the contact's actual phone numbers.
     *
     * Returns `null` only if both queries fail to resolve a name or a number.
     */
    fun queryPickedContact(context: Context, contactUri: Uri): PickedContact? {
        if (contactUri == Uri.EMPTY) return null

        // ---------- 1. Resolve contactId + display name from the picker URI ----------
        // The lookup URI reliably exposes Contacts._ID and DISPLAY_NAME even
        // when the phone-specific columns aren't there.
        var contactId: Long? = null
        var displayName: String? = null

        runCatching {
            context.contentResolver.query(
                contactUri,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME
                ),
                null, null, null
            )
        }.getOrNull()?.use { c ->
            if (c.moveToFirst()) {
                val idIdx = c.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIdx = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                if (idIdx >= 0 && !c.isNull(idIdx)) contactId = c.getLong(idIdx)
                if (nameIdx >= 0) displayName = c.getString(nameIdx)
            }
        }

        // Some picker implementations don't even expose _ID reliably. As a
        // last-ditch effort, try parsing the contactId from the URI path:
        //   content://.../lookup/<lookupKey>/<contactId>
        // The numeric trailing segment IS the contactId on AOSP.
        if (contactId == null) {
            contactUri.lastPathSegment?.toLongOrNull()?.let { contactId = it }
        }

        // ---------- 2. With the resolved contactId, query the phone table ----------
        var phone: String? = null
        val resolvedId = contactId
        if (resolvedId != null) {
            runCatching {
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY,
                        ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
                        ContactsContract.CommonDataKinds.Phone.TYPE
                    ),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(resolvedId.toString()),
                    // Prefer super-primary -> primary -> any, mobile type first.
                    "${ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY} DESC, " +
                            "${ContactsContract.CommonDataKinds.Phone.IS_PRIMARY} DESC, " +
                            "${ContactsContract.CommonDataKinds.Phone.TYPE} ASC"
                )
            }.getOrNull()?.use { pc ->
                if (pc.moveToFirst()) {
                    val n = pc.getString(0)
                    if (!n.isNullOrBlank()) phone = n
                }
            }
        }

        val finalName = displayName?.takeIf { it.isNotBlank() } ?: return null
        val finalPhone = phone?.takeIf { it.isNotBlank() } ?: return null
        return PickedContact(finalName, finalPhone)
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