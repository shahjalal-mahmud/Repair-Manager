// app/src/main/java/com/appriyo/repairmanager/data/repository/EmployeeNotesRepository.kt
package com.appriyo.repairmanager.data.repository

import com.appriyo.repairmanager.data.model.EmployeeNote
import com.appriyo.repairmanager.data.model.EmployeeNotesFirestorePaths
import com.appriyo.repairmanager.presentation.utils.LedgerDateUtils
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
import java.util.Date

/**
 * Repository for the "employeeNotes" collection - the Daily Work Ledger's
 * data source. Each entry is a manual repair job tagged with who did it
 * (A = Shop Owner, B = Employee), its payment, and its profit.
 *
 * **Data isolation:** Lives at users/{uid}/employeeNotes so each Google
 * account only ever sees its own entries, while multiple devices signed into
 * the same account continue to sync in real time.
 */
class EmployeeNotesRepository(
    private val firestore: FirebaseFirestore,
    private val userProvider: FirestoreUserProvider
) {

    private val employeeNotesCollection
        get() = userProvider.currentUserDocument()
            .collection(EmployeeNotesFirestorePaths.EMPLOYEE_NOTES_COLLECTION)

    suspend fun createEmployeeNote(
        title: String,
        description: String,
        totalPayment: Double,
        profit: Double,
        workerType: String,
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
                "workerType" to workerType,
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
                    workerType = workerType,
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
            Result.failure(Exception("Failed to save employee note: ${e.localizedMessage ?: "Unknown error"}", e))
        }
    }

    suspend fun updateEmployeeNote(
        noteId: String,
        title: String,
        description: String,
        totalPayment: Double,
        profit: Double,
        workerType: String
    ): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any?>(
                "title" to title,
                "description" to description,
                "totalPayment" to totalPayment,
                "profit" to profit,
                "workerType" to workerType,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            employeeNotesCollection.document(noteId).update(updates).await()
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception(mapFirestoreException(e), e))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Network error. Please check your internet connection and try again.", e))
        } catch (e: NotAuthenticatedException) {
            Result.failure(e)
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
        } catch (e: NotAuthenticatedException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete employee note: ${e.localizedMessage ?: "Unknown error"}", e))
        }
    }

    fun observeEmployeeNotes(): Flow<List<EmployeeNote>> = callbackFlow {
        val registration: ListenerRegistration
        try {
            registration = employeeNotesCollection
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
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        awaitClose { registration.remove() }
    }

    // ---------------------------------------------------------------------
    // Local, in-memory filtering helpers.
    //
    // observeEmployeeNotes() already streams every note, newest first, from a
    // single realtime listener. Rather than issuing a new composite Firestore
    // query (and needing new indexes) every time the shop owner switches the
    // date filter, we filter that one stream locally. This keeps the realtime
    // listener simple while still giving instant, efficient filtering for
    // Today / Yesterday / Week / Month / Custom date / Custom range, entirely
    // in memory, with no extra network round-trips.
    //
    // A note whose createdAt hasn't been resolved by the server yet (a very
    // recent local write, still pending) is treated as "now" so it shows up
    // immediately in Today/current-period views instead of vanishing until
    // the server timestamp round-trips back.
    // ---------------------------------------------------------------------

    fun filterForToday(notes: List<EmployeeNote>): List<EmployeeNote> =
        filterForDate(notes, Date())

    fun filterForDate(notes: List<EmployeeNote>, date: Date): List<EmployeeNote> =
        filterForRange(notes, date, date)

    fun filterForRange(notes: List<EmployeeNote>, startDate: Date, endDate: Date): List<EmployeeNote> {
        val startMillis = LedgerDateUtils.startOfDay(startDate).time
        val endMillis = LedgerDateUtils.endOfDay(endDate).time
        return notes.filter { note ->
            val time = (note.createdAt ?: Date()).time
            time in startMillis..endMillis
        }
    }

    fun filterForWeek(notes: List<EmployeeNote>, anchor: Date = Date()): List<EmployeeNote> =
        filterForRange(notes, LedgerDateUtils.startOfWeek(anchor), LedgerDateUtils.endOfWeek(anchor))

    fun filterForMonth(notes: List<EmployeeNote>, anchor: Date = Date()): List<EmployeeNote> =
        filterForRange(notes, LedgerDateUtils.startOfMonth(anchor), LedgerDateUtils.endOfMonth(anchor))

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