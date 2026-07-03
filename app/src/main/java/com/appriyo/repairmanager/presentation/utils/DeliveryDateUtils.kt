package com.appriyo.repairmanager.presentation.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Reorder/extend this list to match whatever pattern your date picker in
 * AddRepairScreen actually writes to `expectedDeliveryDate`. First match wins.
 */
object DeliveryDateUtils {

    @RequiresApi(Build.VERSION_CODES.O)
    private val SUPPORTED_FORMATS = listOf(
        "dd/MM/yyyy",
        "yyyy-MM-dd",
        "MM/dd/yyyy",
        "dd-MM-yyyy",
        "dd MMM yyyy"
    ).map { DateTimeFormatter.ofPattern(it) }

    @RequiresApi(Build.VERSION_CODES.O)
    fun parseOrNull(rawDate: String): LocalDate? {
        if (rawDate.isBlank()) return null
        val trimmed = rawDate.trim()
        for (formatter in SUPPORTED_FORMATS) {
            try {
                return LocalDate.parse(trimmed, formatter)
            } catch (_: DateTimeParseException) { /* try next */ }
        }
        return try {
            LocalDate.parse(trimmed) // ISO_LOCAL_DATE fallback
        } catch (_: DateTimeParseException) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun isToday(rawDate: String, today: LocalDate = LocalDate.now()): Boolean =
        parseOrNull(rawDate) == today

    @RequiresApi(Build.VERSION_CODES.O)
    fun isTomorrow(rawDate: String, today: LocalDate = LocalDate.now()): Boolean =
        parseOrNull(rawDate) == today.plusDays(1)

    @RequiresApi(Build.VERSION_CODES.O)
    fun isOverdue(rawDate: String, today: LocalDate = LocalDate.now()): Boolean {
        val date = parseOrNull(rawDate) ?: return false
        return date.isBefore(today)
    }
}