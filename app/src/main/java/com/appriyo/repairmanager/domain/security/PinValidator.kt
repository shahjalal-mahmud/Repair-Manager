// app/src/main/java/com/appriyo/repairmanager/domain/security/PinValidator.kt
package com.appriyo.repairmanager.domain.security

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Pure helper functions for hashing and salting 6-digit PINs.
 *
 * The PIN is never stored in plain text. A per-user random salt is mixed into
 * the hash so two users who happen to pick the same PIN ("000000" by default)
 * still produce different hashes on disk.
 */
object PinValidator {

    /** Length of every PIN accepted by the app. */
    const val PIN_LENGTH = 6

    private const val SALT_LENGTH = 16
    private const val SALT_ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

    /** Returns true if [pin] is exactly 6 decimal digits (e.g. "000000"). */
    fun isWellFormed(pin: String): Boolean =
        pin.length == PIN_LENGTH && pin.all { it.isDigit() }

    /** Generates a new cryptographically random salt. */
    fun generateSalt(): String {
        val rng = SecureRandom()
        val out = StringBuilder(SALT_LENGTH)
        repeat(SALT_LENGTH) {
            out.append(SALT_ALPHABET[rng.nextInt(SALT_ALPHABET.length)])
        }
        return out.toString()
    }

    /** SHA-256 hash of (salt + pin), returned as a lowercase hex string. */
    fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((salt + pin).toByteArray(Charsets.UTF_8))
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }

    /** Constant-time string comparison. Avoids leaking how many leading bytes matched. */
    fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}