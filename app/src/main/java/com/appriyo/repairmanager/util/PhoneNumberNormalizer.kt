// app/src/main/java/com/appriyo/repairmanager/util/PhoneNumberNormalizer.kt
package com.appriyo.repairmanager.util

/**
 * Pure, side-effect-free normalizer for Bangladeshi phone numbers.
 *
 * Accepts the messy formats real users type and the messy formats the
 * Android Contacts app hands us, and produces the canonical local form:
 *
 *     01XXXXXXXXX     // 11 digits, leading 0, mobile prefix 013-019
 *
 * Examples of accepted inputs:
 *
 *   "01712345678"            -> "01712345678"
 *   "+8801712345678"         -> "01712345678"
 *   "8801712345678"          -> "01712345678"
 *   "01712-345678"           -> "01712345678"
 *   "01712 345678"           -> "01712345678"
 *   "01712 345 678"          -> "01712345678"
 *   "(880) 1712-345678"      -> "01712345678"
 *
 * Returns `null` (or `""` via [normalizeOrEmpty]) for inputs that cannot
 * be normalized to a valid 11-digit BD mobile number.
 *
 * The validation rule is intentionally restricted to mobile numbers
 * (013-019 prefixes). BD landlines have a different shape and aren't
 * useful for the SMS / customer-search flows in this app.
 */
object PhoneNumberNormalizer {

    /**
     * Final canonical format: 11 digits starting with `01`, second digit
     * `3-9` (i.e. mobile prefixes 013 / 014 / 015 / 016 / 017 / 018 / 019).
     */
    private val BD_MOBILE = Regex("^01[3-9]\\d{8}$")

    /**
     * Normalize a raw phone string into the canonical local BD form.
     *
     * @param rawInput Anything the user (or the Contacts provider) might
     *                 hand us — leading/trailing whitespace, dashes,
     *                 spaces, parentheses, and `+880` / `880` prefixes
     *                 are all handled.
     * @return The normalized 11-digit local number, or `null` when the
     *         input is blank or cannot be resolved to a valid BD mobile.
     */
    fun normalize(rawInput: String?): String? {
        val trimmed = rawInput?.trim().orEmpty()
        if (trimmed.isEmpty()) return null

        // Keep only digits. Spaces, dashes, parentheses, '+', letters - all dropped.
        val digits = buildString {
            for (ch in trimmed) if (ch.isDigit()) append(ch)
        }
        if (digits.isEmpty()) return null

        // International form: 880 + 10 digits -> drop the 880, prepend 0
        val prefixed = if (digits.startsWith("880") && digits.length == 13) {
            "0" + digits.substring(3)
        } else if (digits.length == 10 && digits.startsWith("1")) {
            // Some contact apps export the trunk-zero number without the
            // leading 0; restore it.
            "0$digits"
        } else {
            digits
        }

        return if (BD_MOBILE.matches(prefixed)) prefixed else null
    }

    /**
     * Convenience wrapper: same as [normalize] but returns `""` for
     * blank / null input instead of `null`. Useful when the caller has
     * a non-nullable field and just wants the normalized string back
     * (e.g. when populating a TextField after a contact pick).
     */
    fun normalizeOrEmpty(rawInput: String?): String =
        normalize(rawInput).orEmpty()

    /**
     * True when the input is either blank OR resolves to a valid BD
     * mobile. This is the predicate the form uses to decide whether
     * to block Save on the phone field.
     */
    fun isValidOrBlank(rawInput: String?): Boolean {
        val trimmed = rawInput?.trim().orEmpty()
        if (trimmed.isEmpty()) return true
        return normalize(trimmed) != null
    }
}