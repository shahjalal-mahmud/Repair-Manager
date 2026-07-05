// app/src/main/java/com/appriyo/repairmanager/data/repository/NotesRepository.kt
package com.appriyo.repairmanager.data.repository

import com.appriyo.repairmanager.data.model.Note
import com.appriyo.repairmanager.data.model.NotesFirestorePaths
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
 * Repository responsible for all Firestore operations related to general
 * business notes.
 *
 * **Data isolation:** Notes now live at users/{uid}/notes so each Google
 * account (repair shop) only ever sees its own notes, while multiple devices
 * signed into the same account continue to sync in real time.
 *
 * Mirrors the structure of RepairRepository: Result-wrapped suspend functions
 * for writes, and a callbackFlow-backed snapshot listener for realtime reads.
 */
class NotesRepository(
    private val firestore: FirebaseFirestore,
    private val userProvider: FirestoreUserProvider
) {

    private val notesCollection
        get() = userProvider.currentUserDocument()
            .collection(NotesFirestorePaths.NOTES_COLLECTION)

    suspend fun createNote(
        title: String,
        description: String,
        category: String,
        createdBy: String
    ): Result<Note> {
        return try {
            val newNoteRef = notesCollection.document()

            val noteData = hashMapOf<String, Any?>(
                "id" to newNoteRef.id,
                "title" to title,
                "description" to description,
                "category" to category,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "createdBy" to createdBy
            )

            newNoteRef.set(noteData).await()

            Result.success(
                Note(
                    id = newNoteRef.id,
                    title = title,
                    description = description,
                    category = category,
                    createdAt = null,
                    updatedAt = null,
                    createdBy = createdBy
                )
            )
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception(mapFirestoreException(e), e))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Network error. Please check your internet connection and try again.", e))
        } catch (e: NotAuthenticatedException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save note: ${e.localizedMessage ?: "Unknown error"}", e))
        }
    }

    suspend fun updateNote(
        noteId: String,
        title: String,
        description: String,
        category: String
    ): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any?>(
                "title" to title,
                "description" to description,
                "category" to category,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            notesCollection.document(noteId).update(updates).await()
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception(mapFirestoreException(e), e))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Network error. Please check your internet connection and try again.", e))
        } catch (e: NotAuthenticatedException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update note: ${e.localizedMessage ?: "Unknown error"}", e))
        }
    }

    suspend fun deleteNote(noteId: String): Result<Unit> {
        return try {
            notesCollection.document(noteId).delete().await()
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception(mapFirestoreException(e), e))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Network error. Please check your internet connection and try again.", e))
        } catch (e: NotAuthenticatedException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete note: ${e.localizedMessage ?: "Unknown error"}", e))
        }
    }

    /**
     * Realtime stream of all notes for the signed-in account, newest first.
     * Backs the Notes screen so changes made on any device signed into the
     * same Google account appear automatically.
     */
    fun observeNotes(): Flow<List<Note>> = callbackFlow {
        val registration: ListenerRegistration
        try {
            registration = notesCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val notes = snapshot?.documents?.mapNotNull { it.toObject(Note::class.java) }
                        ?: emptyList()
                    trySend(notes)
                }
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        awaitClose { registration.remove() }
    }

    private fun mapFirestoreException(e: FirebaseFirestoreException): String {
        return when (e.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Unable to reach the server. Please check your internet connection and try again."
            FirebaseFirestoreException.Code.ABORTED ->
                "Could not save the note because of a conflicting update. Please try again."
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "You don't have permission to perform this action. Please contact the administrator."
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                "The request took too long. Please check your connection and try again."
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                "Service is temporarily busy. Please try again in a moment."
            FirebaseFirestoreException.Code.NOT_FOUND ->
                "This note could not be found. It may have been deleted."
            else ->
                "Operation failed (${e.code}). Please try again."
        }
    }
}