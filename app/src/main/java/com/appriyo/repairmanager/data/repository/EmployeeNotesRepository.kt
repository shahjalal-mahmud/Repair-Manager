// app/src/main/java/com/appriyo/repairmanager/data/repository/EmployeeNotesRepository.kt
package com.appriyo.repairmanager.data.repository

import com.appriyo.repairmanager.data.model.EmployeeNote
import com.appriyo.repairmanager.data.model.EmployeeNotesFirestorePaths
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for the "employeeNotes" collection - a manual notebook of repair
 * jobs and their payment/profit. Not an employee-management system.
 */
class EmployeeNotesRepository(
    private val firestore: FirebaseFirestore
) {

    private val employeeNotesCollection
        get() = firestore.collection(EmployeeNotesFirestorePaths.EMPLOYEE_NOTES_COLLECTION)

    suspend fun createEmployeeNote(
        title: String,
        description: String,
        totalPayment: Double,
        profit: Double,
        createdBy: String
    ): Result<EmployeeNote> {
        return try {
            val newNoteRef = employeeNotesCollection.document()

            val noteData = hashMapOf<String, Any?>(
                "id" to newNoteRef.id,
                "title" to title,
                "description" to description,
                "totalPayment" to totalPayment,
                "profit" to profit,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "createdBy" to createdBy
            )

            newNoteRef.set(noteData).await()

            Result.success(
                EmployeeNote(
                    id = newNoteRef.id,
                    title = title,
                    description = description,
                    totalPayment = totalPayment,
                    profit = profit,
                    createdAt = null,
                    updatedAt = null,
                    createdBy = createdBy
                )
            )
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception(mapFirestoreException(e), e))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Network error. Please check your internet connection and try again.", e))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save employee note: ${e.localizedMessage ?: "Unknown error"}", e))
        }
    }

    suspend fun updateEmployeeNote(
        noteId: String,
        title: String,
        description: String,
        totalPayment: Double,
        profit: Double
    ): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any?>(
                "title" to title,
                "description" to description,
                "totalPayment" to totalPayment,
                "profit" to profit,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            employeeNotesCollection.document(noteId).update(updates).await()
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception(mapFirestoreException(e), e))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Network error. Please check your internet connection and try again.", e))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update employee note: ${e.localizedMessage ?: "Unknown error"}", e))
        }
    }

    suspend fun deleteEmployeeNote(noteId: String): Result<Unit> {
        return try {
            employeeNotesCollection.document(noteId).delete().await()
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception(mapFirestoreException(e), e))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Network error. Please check your internet connection and try again.", e))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete employee note: ${e.localizedMessage ?: "Unknown error"}", e))
        }
    }

    fun observeEmployeeNotes(): Flow<List<EmployeeNote>> = callbackFlow {
        val registration: ListenerRegistration = employeeNotesCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val notes = snapshot?.documents?.mapNotNull { it.toObject(EmployeeNote::class.java) }
                    ?: emptyList()
                trySend(notes)
            }

        awaitClose { registration.remove() }
    }

    private fun mapFirestoreException(e: FirebaseFirestoreException): String {
        return when (e.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Unable to reach the server. Please check your internet connection and try again."
            FirebaseFirestoreException.Code.ABORTED ->
                "Could not save the entry because of a conflicting update. Please try again."
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "You don't have permission to perform this action. Please contact the administrator."
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                "The request took too long. Please check your connection and try again."
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                "Service is temporarily busy. Please try again in a moment."
            FirebaseFirestoreException.Code.NOT_FOUND ->
                "This entry could not be found. It may have been deleted."
            else ->
                "Operation failed (${e.code}). Please try again."
        }
    }
}