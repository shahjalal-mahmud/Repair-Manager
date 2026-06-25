// app/src/main/java/com/appriyo/repairmanager/data/repository/AppSettingsRepository.kt
package com.appriyo.repairmanager.data.repository

import com.appriyo.repairmanager.data.model.AppSettings
import com.appriyo.repairmanager.data.model.SmsFirestorePaths
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Manages the single shared appSettings/global document that designates the
 * SMS-sending device and its preferred SIM slot.
 */
class AppSettingsRepository(private val firestore: FirebaseFirestore) {

    private val settingsDocRef
        get() = firestore
            .collection(SmsFirestorePaths.APP_SETTINGS_COLLECTION)
            .document(SmsFirestorePaths.GLOBAL_SETTINGS_DOC)

    fun observeSettings(): Flow<AppSettings?> = callbackFlow {
        val registration: ListenerRegistration = settingsDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(AppSettings::class.java))
        }
        awaitClose { registration.remove() }
    }

    suspend fun getSettingsOnce(): Result<AppSettings?> {
        return try {
            val snapshot = settingsDocRef.get().await()
            Result.success(snapshot.toObject(AppSettings::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Marks the calling device as the one responsible for sending SMS, or clears it (pass blank ids). */
    suspend fun setSmsSenderDevice(deviceId: String, deviceName: String): Result<Unit> {
        return try {
            val data = hashMapOf<String, Any?>(
                "smsSenderDeviceId" to deviceId,
                "smsSenderDeviceName" to deviceName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            settingsDocRef.set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setSimSlotIndex(simSlotIndex: Int): Result<Unit> {
        return try {
            val data = hashMapOf<String, Any?>(
                "simSlotIndex" to simSlotIndex,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            settingsDocRef.set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}