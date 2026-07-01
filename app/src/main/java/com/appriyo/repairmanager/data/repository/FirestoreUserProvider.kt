// app/src/main/java/com/appriyo/repairmanager/data/repository/FirestoreUserProvider.kt
package com.appriyo.repairmanager.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Thrown by [FirestoreUserProvider.currentUserDocument] when no Firebase user
 * is currently signed in. Repositories catch this (via their existing generic
 * Exception handlers) and turn it into a meaningful [Result.failure] instead
 * of crashing.
 */
class NotAuthenticatedException :
    Exception("No authenticated user. Please sign in again.")

/**
 * Single source of truth for resolving the current user's private Firestore
 * document: users/{uid}
 *
 * Every repository must obtain its collection references through this class
 * instead of touching FirebaseAuth directly. This guarantees:
 *  - No repository ever duplicates UID-lookup logic.
 *  - Every read/write is automatically isolated to the signed-in Google
 *    account's own data.
 *  - Multiple devices signed into the SAME account resolve to the SAME
 *    users/{uid} document and therefore sync in real time.
 */
class FirestoreUserProvider(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    companion object {
        const val USERS_COLLECTION = "users"
    }

    /**
     * Returns the DocumentReference for users/{currentUid}.
     *
     * @throws NotAuthenticatedException if there is no signed-in user. This
     * is a regular checked-by-convention exception (not a crash) - it is
     * intended to be caught by the calling repository's existing try/catch
     * blocks and converted into a Result.failure.
     */
    fun currentUserDocument(): DocumentReference {
        val uid = auth.currentUser?.uid ?: throw NotAuthenticatedException()
        return firestore.collection(USERS_COLLECTION).document(uid)
    }

    /** Returns the current UID, or null if signed out. Never throws. */
    fun currentUserIdOrNull(): String? = auth.currentUser?.uid
}