package com.appriyo.repairmanager.data.model

/**
 * Firestore path segments for the TaliKhata feature.
 *
 * Full paths (always resolved through FirestoreUserProvider, never a
 * top-level collection):
 *   users/{uid}/talikhata
 *   users/{uid}/talikhata/{entryId}/history
 */
object TaliKhataFirestorePaths {
    const val TALIKHATA_COLLECTION = "talikhata"
    const val HISTORY_SUBCOLLECTION = "history"
}