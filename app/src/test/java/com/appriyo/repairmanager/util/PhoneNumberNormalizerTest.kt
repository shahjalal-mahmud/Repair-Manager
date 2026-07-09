// app/src/test/java/com/appriyo/repairmanager/util/PhoneNumberNormalizerTest.kt
package com.appriyo.repairmanager.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberNormalizerTest {

    // ── blank / null inputs ────────────────────────────────────────────────

    @Test
    fun normalize_null_returns_null() {
        assertNull(PhoneNumberNormalizer.normalize(null))
    }

    @Test
    fun normalize_empty_returns_null() {
        assertNull(PhoneNumberNormalizer.normalize(""))
    }

    @Test
    fun normalize_whitespace_returns_null() {
        assertNull(PhoneNumberNormalizer.normalize("   "))
    }

    @Test
    fun normalizeOrEmpty_blank_returns_empty_string() {
        assertEquals("", PhoneNumberNormalizer.normalizeOrEmpty(""))
        assertEquals("", PhoneNumberNormalizer.normalizeOrEmpty("   "))
        assertEquals("", PhoneNumberNormalizer.normalizeOrEmpty(null))
    }

    // ── canonical 11-digit local format ───────────────────────────────────

    @Test
    fun normalize_canonical_passes_through() {
        assertEquals("01712345678", PhoneNumberNormalizer.normalize("01712345678"))
    }

    @Test
    fun normalize_canonical_with_whitespace_around() {
        assertEquals(
            "01712345678",
            PhoneNumberNormalizer.normalize("  01712345678  ")
        )
    }

    // ── should-save: international / formatted inputs ──────────────────────

    @Test
    fun normalize_plus880_is_stripped() {
        assertEquals("01712345678", PhoneNumberNormalizer.normalize("+8801712345678"))
    }

    @Test
    fun normalize_bare_880_prefix_is_stripped() {
        assertEquals("01712345678", PhoneNumberNormalizer.normalize("8801712345678"))
    }

    @Test
    fun normalize_dashed_local_format() {
        assertEquals("01712345678", PhoneNumberNormalizer.normalize("01712-345678"))
    }

    @Test
    fun normalize_spaced_local_format_compact() {
        assertEquals("01712345678", PhoneNumberNormalizer.normalize("01712 345678"))
    }

    @Test
    fun normalize_spaced_local_format_split() {
        assertEquals("01712345678", PhoneNumberNormalizer.normalize("01712 345 678"))
    }

    @Test
    fun normalize_parentheses_and_country_code() {
        assertEquals("01712345678", PhoneNumberNormalizer.normalize("(880) 1712-345678"))
    }

    @Test
    fun normalize_bare_trunk_zero_form() {
        // Some contact providers export "1712345678" without the leading 0.
        assertEquals("01712345678", PhoneNumberNormalizer.normalize("1712345678"))
    }

    // ── should-fail: clearly invalid inputs ────────────────────────────────

    @Test
    fun normalize_short_number_returns_null() {
        assertNull(PhoneNumberNormalizer.normalize("12345"))
    }

    @Test
    fun normalize_alpha_returns_null() {
        assertNull(PhoneNumberNormalizer.normalize("abc123"))
    }

    @Test
    fun normalize_short_mobile_prefix_returns_null() {
        assertNull(PhoneNumberNormalizer.normalize("019999"))
    }

    @Test
    fun normalize_too_long_returns_null() {
        assertNull(PhoneNumberNormalizer.normalize("0171234567890"))
    }

    @Test
    fun normalize_country_code_too_short_returns_null() {
        // +880 prefix + only 9 digits -> not a valid BD mobile.
        assertNull(PhoneNumberNormalizer.normalize("+880171234567"))
    }

    @Test
    fun normalize_country_code_too_long_returns_null() {
        // +880 prefix + 11 digits after -> too long.
        assertNull(PhoneNumberNormalizer.normalize("+880171234567899"))
    }

    @Test
    fun normalize_only_dashes_returns_null() {
        assertNull(PhoneNumberNormalizer.normalize("---"))
    }

    @Test
    fun normalize_unknown_country_code_returns_null() {
        // Not a BD country code.
        assertNull(PhoneNumberNormalizer.normalize("+12345678901"))
    }

    // ── isValidOrBlank ─────────────────────────────────────────────────────

    @Test
    fun isValidOrBlank_true_for_blank() {
        assertTrue(PhoneNumberNormalizer.isValidOrBlank(""))
        assertTrue(PhoneNumberNormalizer.isValidOrBlank("   "))
        assertTrue(PhoneNumberNormalizer.isValidOrBlank(null))
    }

    @Test
    fun isValidOrBlank_true_for_canonical() {
        assertTrue(PhoneNumberNormalizer.isValidOrBlank("01712345678"))
    }

    @Test
    fun isValidOrBlank_true_for_normalized_form() {
        assertTrue(PhoneNumberNormalizer.isValidOrBlank("+8801712345678"))
        assertTrue(PhoneNumberNormalizer.isValidOrBlank("01712-345678"))
    }

    @Test
    fun isValidOrBlank_false_for_garbage() {
        assertFalse(PhoneNumberNormalizer.isValidOrBlank("12345"))
        assertFalse(PhoneNumberNormalizer.isValidOrBlank("abc123"))
        assertFalse(PhoneNumberNormalizer.isValidOrBlank("0171234567890"))
    }
}