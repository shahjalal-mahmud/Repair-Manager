// app/src/main/java/com/appriyo/repairmanager/data/repository/SecurityRepository.kt
package com.appriyo.repairmanager.data.repository

import com.appriyo.repairmanager.data.model.SecurityPin
import com.appriyo.repairmanager.domain.security.PinValidator
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Manages the owner PIN for each Google account (one PIN per user document).
 *
 * Firestore path: users/{uid}/security/pin
 *
 * The plain-text PIN is never persisted. Every read or write goes through
 * [PinValidator] which hashes the value with a per-user random salt using
 * SHA-256 before storage.
 *
 * On first read for a new user, [getOrSeedPin] writes the default PIN
 * "000000" so the rest of the app can immediately use it without any extra
 * setup step.
 */
class SecurityRepository(
    private val firestore: FirebaseFirestore,
    private val userProvider: FirestoreUserProvider
) {

    companion object {
        /** Default PIN assigned to a fresh user document on first read. */
        const val DEFAULT_PIN = "000000"
        private const val SECURITY_COLLECTION = "security"
        private const val PIN_DOCUMENT = "pin"
    }

    private val pinDocRef
        get() = userProvider.currentUserDocument()
            .collection(SECURITY_COLLECTION)
            .document(PIN_DOCUMENT)

    /**
     * Returns the current SecurityPin for the signed-in user. If no document
     * exists yet (fresh user), seeds the default PIN "000000" with a fresh
     * random salt and returns the freshly-written record.
     */
    suspend fun getOrSeedPin(): Result<SecurityPin> {
        return try {
            val snapshot = pinDocRef.get().await()
            val existing = snapshot.toObject(SecurityPin::class.java)
            if (existing != null && existing.pinHash.isNotBlank() && existing.salt.isNotBlank()) {
                Result.success(existing)
            } else {
                val seeded = writeDefaultPin()
                Result.success(seeded)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Compares [input] against the stored PIN. Always reloads from Firestore
     * to keep the comparison fresh (e.g. immediately after the owner changed
     * the PIN from the Settings screen).
     */
    suspend fun verifyPin(input: String): Result<Boolean> {
        return try {
            if (!PinValidator.isWellFormed(input)) {
                Result.success(false)
            } else {
                getOrSeedPin().fold(
                    onSuccess = { stored ->
                        val expectedHash = PinValidator.hashPin(input, stored.salt)
                        Result.success(PinValidator.constantTimeEquals(expectedHash, stored.pinHash))
                    },
                    onFailure = { Result.failure(it) }
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifies [currentInput] and, if correct, writes [newPin] as the new
     * owner PIN with a freshly generated salt. Returns:
     *  - success(Unit) on a successful change
     *  - success(false) when current PIN is wrong
     *  - failure on Firestore errors
     *
     * Callers should treat the inner boolean (success) as a separate signal.
     */
    suspend fun setPin(currentInput: String, newPin: String): Result<Boolean> {
        if (!PinValidator.isWellFormed(newPin)) {
            return Result.failure(IllegalArgumentException("New PIN must be exactly 6 digits."))
        }
        return try {
            val verifyResult = verifyPin(currentInput)
            verifyResult.fold(
                onSuccess = { ok ->
                    if (!ok) {
                        Result.success(false)
                    } else {
                        val newSalt = PinValidator.generateSalt()
                        val newHash = PinValidator.hashPin(newPin, newSalt)
                        val data = hashMapOf<String, Any?>(
                            "pinHash" to newHash,
                            "salt" to newSalt,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        pinDocRef.set(data, SetOptions.merge()).await()
                        Result.success(true)
                    }
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun writeDefaultPin(): SecurityPin {
        val salt = PinValidator.generateSalt()
        val hash = PinValidator.hashPin(DEFAULT_PIN, salt)
        val data = hashMapOf<String, Any?>(
            "pinHash" to hash,
            "salt" to salt,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        pinDocRef.set(data).await()
        return SecurityPin(pinHash = hash, salt = salt, updatedAt = null)
    }
}